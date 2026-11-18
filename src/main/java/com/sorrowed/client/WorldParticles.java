package com.sorrowed.client;

import com.sorrowed.SorrowedConfig;
import com.sorrowed.registry.Dimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * What hangs in the air around you. Every world has its own: dust in the hallway and the ward,
 * ash in the hush, snow in the deep water, rain on the street, noise in the static. In his world
 * the motes do not move at all, until the very end, when it is the day after and things drift.
 * Also the faint light rising from whatever you are looking for.
 */
public final class WorldParticles {
    private WorldParticles() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        Player player = mc.player;
        if (level == null || player == null || mc.isPaused()) return;
        ParticleStatus status = mc.options.particles().get();
        if (status == ParticleStatus.MINIMAL) return;
        boolean less = status == ParticleStatus.DECREASED;
        RandomSource r = level.random;
        long time = level.getGameTime();

        if (SorrowedConfig.AMBIENT_PARTICLES.get() && !(less && (time & 1L) == 0L)) {
            ambient(level, player, r);
        }
        // Something of his is still warm here.
        if ((time % 3L) == 0L && !CutsceneClient.isActive()) {
            for (BlockPos t : ClientStory.targets) {
                double d2 = player.distanceToSqr(t.getX() + 0.5D, t.getY() + 0.5D, t.getZ() + 0.5D);
                // Close enough to touch it: no more sparks in your face.
                if (d2 > 160.0D * 160.0D || d2 < 2.5D * 2.5D) continue;
                level.addAlwaysVisibleParticle(ParticleTypes.END_ROD, true,
                        t.getX() + 0.5D + (r.nextDouble() - 0.5D) * 0.8D, t.getY() + 0.3D + r.nextDouble() * 0.8D,
                        t.getZ() + 0.5D + (r.nextDouble() - 0.5D) * 0.8D, 0.0D, 0.015D + r.nextDouble() * 0.02D, 0.0D);
            }
        }
    }

    private static void ambient(ClientLevel level, Player p, RandomSource r) {
        ResourceKey<Level> dim = level.dimension();
        if (dim.equals(Dimensions.BACKUP)) {
            if (ClientStory.stage != ClientStory.PLAYING) {
                // The day after: petals coming down on the wind.
                for (int i = 0; i < 2; i++) around(level, p, r, ParticleTypes.CHERRY_LEAVES, 16.0D, 4.0D, 12.0D, 0.0D, 0.0D, 0.0D);
            } else if (r.nextInt(2) == 0) {
                // Dust that hangs where it is. Nothing here moves.
                around(level, p, r, ParticleTypes.WHITE_ASH, 9.0D, 0.0D, 4.0D, 0.0D, 0.0D, 0.0D);
            }
        } else if (dim.equals(Dimensions.HUSH)) {
            around(level, p, r, ParticleTypes.ASH, 12.0D, 2.0D, 8.0D, 0.0D, -0.02D, 0.0D);
            if (r.nextInt(30) == 0) around(level, p, r, ParticleTypes.SOUL, 10.0D, 0.0D, 3.0D, 0.0D, 0.01D, 0.0D);
        } else if (dim.equals(Dimensions.DROWNED)) {
            if (p.isUnderWater()) {
                for (int i = 0; i < 4; i++) around(level, p, r, ParticleTypes.UNDERWATER, 8.0D, -3.0D, 6.0D, 0.0D, 0.0D, 0.0D);
                if (r.nextInt(6) == 0) around(level, p, r, ParticleTypes.BUBBLE, 4.0D, -2.0D, 2.0D, 0.0D, 0.05D, 0.0D);
            }
        } else if (dim.equals(Dimensions.HALLWAY) || dim.equals(Dimensions.WARD) || dim.equals(Dimensions.ARCHIVE)) {
            if (r.nextInt(2) == 0) around(level, p, r, ParticleTypes.WHITE_ASH, 7.0D, 0.0D, 3.5D, 0.0D, 0.0D, 0.0D);
            if (dim.equals(Dimensions.ARCHIVE) && r.nextInt(12) == 0) {
                around(level, p, r, ParticleTypes.SMALL_FLAME, 6.0D, 1.0D, 3.0D, 0.0D, 0.01D, 0.0D);
            }
        } else if (dim.equals(Dimensions.STREET)) {
            // Rain on the road: drops falling and breaking on the ground.
            for (int i = 0; i < 3; i++) around(level, p, r, ParticleTypes.FALLING_WATER, 10.0D, 4.0D, 6.0D, 0.0D, 0.0D, 0.0D);
            for (int i = 0; i < 2; i++) {
                double x = p.getX() + (r.nextDouble() - 0.5D) * 16.0D;
                double z = p.getZ() + (r.nextDouble() - 0.5D) * 16.0D;
                BlockPos top = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(x, 0, z));
                if (Math.abs(top.getY() - p.getY()) < 8.0D) level.addParticle(ParticleTypes.RAIN, x, top.getY() + 0.05D, z, 0.0D, 0.0D, 0.0D);
            }
        } else if (dim.equals(Dimensions.STATIC)) {
            for (int i = 0; i < 2; i++) {
                around(level, p, r, r.nextBoolean() ? ParticleTypes.WHITE_ASH : ParticleTypes.ASH, 10.0D, 0.0D, 5.0D,
                        (r.nextDouble() - 0.5D) * 0.2D, 0.0D, (r.nextDouble() - 0.5D) * 0.2D);
            }
            if (r.nextInt(25) == 0) around(level, p, r, ParticleTypes.ELECTRIC_SPARK, 6.0D, 0.5D, 2.5D, 0.0D, 0.0D, 0.0D);
        } else if (dim.equals(Dimensions.ORCHARD)) {
            if (r.nextInt(2) == 0) around(level, p, r, ParticleTypes.ASH, 10.0D, 1.0D, 5.0D, 0.0D, -0.01D, 0.0D);
        } else if (dim.equals(Dimensions.SERVER)) {
            if (r.nextInt(3) == 0) around(level, p, r, ParticleTypes.WHITE_ASH, 7.0D, 0.0D, 3.0D, 0.0D, 0.0D, 0.0D);
            if (r.nextInt(14) == 0) around(level, p, r, ParticleTypes.ELECTRIC_SPARK, 5.0D, 0.5D, 2.0D, 0.0D, 0.0D, 0.0D);
        } else if (dim.equals(Level.OVERWORLD) && ClientStory.stage == ClientStory.ENDED && level.isDay() && r.nextInt(4) == 0) {
            // After: the air is clear and there is light in it.
            around(level, p, r, ParticleTypes.WHITE_ASH, 10.0D, 1.0D, 5.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** One particle somewhere in a box around the player: 'spread' wide, 'up' to 'up + height' above the feet. */
    private static void around(ClientLevel level, Player p, RandomSource r, ParticleOptions type, double spread,
                               double up, double height, double vx, double vy, double vz) {
        double x = p.getX() + (r.nextDouble() - 0.5D) * spread * 2.0D;
        double y = p.getY() + up + r.nextDouble() * height;
        double z = p.getZ() + (r.nextDouble() - 0.5D) * spread * 2.0D;
        BlockPos at = BlockPos.containing(x, y, z);
        if (level.getBlockState(at).isSolidRender(level, at)) return;
        level.addParticle(type, x, y, z, vx, vy, vz);
    }
}
