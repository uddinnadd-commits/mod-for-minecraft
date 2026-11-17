package com.sorrowed.block;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

/** Puts the player at an exact spot, no vanilla portal search. */
public class DoorTeleporter implements ITeleporter {
    private final Vec3 pos;
    private final float yaw;

    public DoorTeleporter(Vec3 pos, float yaw) {
        this.pos = pos;
        this.yaw = yaw;
    }

    @Override
    public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld,
                                    Function<ServerLevel, PortalInfo> defaultPortalInfo) {
        return new PortalInfo(pos, Vec3.ZERO, yaw, entity.getXRot());
    }

    @Override
    public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceWorld, ServerLevel destWorld) {
        return false;
    }
}
