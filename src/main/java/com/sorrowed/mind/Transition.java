package com.sorrowed.mind;

import com.sorrowed.block.DoorTeleporter;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.registry.Dimensions;
import com.sorrowed.worldgen.HubBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * How you move between worlds: a scribbled mess covers the screen, and when it is scratched
 * away you are somewhere else. The teleport happens while the screen is fully covered.
 */
public final class Transition {
    private Transition() {}

    public static final int IN = 50;
    public static final int HOLD = 12;
    public static final int OUT = 40;
    private static final int TELEPORT_AT = IN + 4;
    private static final String TRANSIT = "sorrowed_transit_until";

    public static boolean inTransit(ServerPlayer p) {
        return p.getPersistentData().getLong(TRANSIT) > p.level().getGameTime();
    }

    public static void go(ServerPlayer p, ServerLevel dest, Vec3 pos, float yaw) {
        if (inTransit(p)) return;
        MinecraftServer server = p.getServer();
        if (server == null) return;
        p.getPersistentData().putLong(TRANSIT, p.level().getGameTime() + IN + HOLD + OUT);
        ModNetwork.scribble(p, IN, HOLD, OUT);
        p.setPortalCooldown();
        Scheduler.after(server, TELEPORT_AT, () -> {
            if (p.isRemoved()) return;
            p.setPortalCooldown();
            p.changeDimension(dest, new DoorTeleporter(pos, yaw));
            p.setPortalCooldown();
        });
    }

    /** Taken to the Unfinished from wherever you stand. The hub gate brings you back here. */
    public static void toUnfinished(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        ServerLevel dest = server.getLevel(Dimensions.UNFINISHED);
        if (dest == null || p.serverLevel().dimension().equals(Dimensions.UNFINISHED)) return;
        p.getPersistentData().putString("sorrowed_return_" + Dimensions.UNFINISHED.location().getPath(),
                p.serverLevel().dimension().location() + "|" + p.getX() + "|" + p.getY() + "|" + p.getZ());
        go(p, dest, HubBuilder.arrival(dest, Dimensions.UNFINISHED), 180.0F);
    }
}
