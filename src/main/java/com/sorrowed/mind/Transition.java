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
 * How you move between worlds: the picture comes apart, goes dark, and settles again somewhere
 * else. The teleport happens while the screen is fully dark.
 */
public final class Transition {
    private Transition() {}

    public static final int IN = 70;
    public static final int HOLD = 16;
    public static final int OUT = 60;
    private static final int TELEPORT_AT = IN + 6;
    private static final String TRANSIT = "sorrowed_transit_until";

    public static boolean inTransit(ServerPlayer p) {
        return p.getPersistentData().getLong(TRANSIT) > p.level().getGameTime();
    }

    public static void go(ServerPlayer p, ServerLevel dest, Vec3 pos, float yaw) {
        if (inTransit(p)) return;
        MinecraftServer server = p.getServer();
        if (server == null) return;
        p.getPersistentData().putLong(TRANSIT, p.level().getGameTime() + IN + HOLD + OUT);
        ModNetwork.distort(p, IN, HOLD, OUT);
        p.setPortalCooldown();
        Scheduler.after(server, TELEPORT_AT, () -> {
            if (p.isRemoved()) return;
            p.setPortalCooldown();
            p.changeDimension(dest, new DoorTeleporter(pos, yaw));
            p.setPortalCooldown();
        });
    }

    /** Taken to Alex's world from wherever you stand. The gate at the end of his path brings you back here. */
    public static void toBackup(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        ServerLevel dest = server.getLevel(Dimensions.BACKUP);
        if (dest == null || p.serverLevel().dimension().equals(Dimensions.BACKUP)) return;
        p.getPersistentData().putString("sorrowed_return_" + Dimensions.BACKUP.location().getPath(),
                p.serverLevel().dimension().location() + "|" + p.getX() + "|" + p.getY() + "|" + p.getZ());
        go(p, dest, HubBuilder.arrival(dest, Dimensions.BACKUP), 0.0F);
    }
}
