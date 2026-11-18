package com.sorrowed.client;

import com.sorrowed.registry.Dimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/**
 * The sounds of each place, played client-side somewhere just out of sight: pages turning in
 * the Archive, thunder two streets over, a door shutting down the hall, the hiss of the static.
 * Plus the dust and drips that drift past you.
 */
public final class AmbienceEvents {
    private AmbienceEvents() {}

    private record Cue(SoundEvent sound, float volume, float pitchLo, float pitchHi, int rarity) {}

    private static Cue cue(SoundEvent s, float v, float lo, float hi, int rarity) {
        return new Cue(s, v, lo, hi, rarity);
    }

    private static Cue[] cuesFor(ResourceKey<Level> dim) {
        if (dim.equals(Dimensions.ORCHARD)) return new Cue[] {
                cue(SoundEvents.PHANTOM_AMBIENT, 0.25F, 0.3F, 0.5F, 1800),
                cue(SoundEvents.WOODEN_DOOR_OPEN, 0.3F, 0.3F, 0.5F, 900),
                cue(SoundEvents.WOOD_HIT, 0.4F, 0.5F, 0.7F, 600),
                cue(SoundEvents.GRASS_STEP, 0.3F, 0.5F, 0.7F, 700)};
        if (dim.equals(Dimensions.WARD)) return new Cue[] {
                cue(SoundEvents.NOTE_BLOCK_BIT.value(), 0.25F, 1.8F, 1.9F, 70),
                cue(SoundEvents.BEACON_AMBIENT, 0.3F, 0.6F, 0.8F, 400),
                cue(SoundEvents.IRON_TRAPDOOR_OPEN, 0.3F, 0.4F, 0.6F, 900),
                cue(SoundEvents.WOODEN_DOOR_CLOSE, 0.3F, 0.5F, 0.7F, 1500),
                cue(SoundEvents.PLAYER_BREATH, 0.5F, 0.6F, 0.8F, 1300)};
        if (dim.equals(Dimensions.HUSH)) return new Cue[] {
                cue(SoundEvents.WOLF_HOWL, 0.25F, 0.5F, 0.7F, 2200),
                cue(SoundEvents.AMBIENT_CAVE.value(), 0.5F, 0.7F, 0.9F, 1600),
                cue(SoundEvents.GRASS_STEP, 0.4F, 0.7F, 0.9F, 500)};
        if (dim.equals(Dimensions.DROWNED)) return new Cue[] {
                cue(SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, 0.6F, 0.8F, 1.2F, 260),
                cue(SoundEvents.ELDER_GUARDIAN_AMBIENT, 0.2F, 0.4F, 0.6F, 3200),
                cue(SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 0.3F, 0.8F, 1.0F, 900)};
        if (dim.equals(Dimensions.HALLWAY)) return new Cue[] {
                cue(SoundEvents.REDSTONE_TORCH_BURNOUT, 0.3F, 0.5F, 0.7F, 700),
                cue(SoundEvents.WOODEN_DOOR_CLOSE, 0.35F, 0.7F, 0.9F, 1800),
                cue(SoundEvents.WOOL_STEP, 0.3F, 0.6F, 0.8F, 1100)};
        if (dim.equals(Dimensions.BACKUP)) return new Cue[] {
                cue(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.3F, 1.4F, 1.9F, 2400),
                cue(SoundEvents.AMBIENT_CAVE.value(), 0.15F, 1.0F, 1.2F, 5000)};
        if (dim.equals(Dimensions.ARCHIVE)) return new Cue[] {
                cue(SoundEvents.BOOK_PAGE_TURN, 0.5F, 0.6F, 0.9F, 500),
                cue(SoundEvents.WOODEN_TRAPDOOR_CLOSE, 0.3F, 0.4F, 0.6F, 1500),
                cue(SoundEvents.CANDLE_AMBIENT, 0.4F, 0.8F, 1.0F, 400),
                cue(SoundEvents.WOOD_STEP, 0.3F, 0.5F, 0.7F, 1000)};
        if (dim.equals(Dimensions.STREET)) return new Cue[] {
                cue(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.3F, 0.5F, 0.7F, 2200),
                cue(SoundEvents.WOODEN_DOOR_CLOSE, 0.3F, 0.6F, 0.8F, 1600),
                cue(SoundEvents.IRON_TRAPDOOR_CLOSE, 0.25F, 0.8F, 1.0F, 2400),
                cue(SoundEvents.STONE_STEP, 0.3F, 0.6F, 0.8F, 1000)};
        if (dim.equals(Dimensions.STATIC)) return new Cue[] {
                cue(SoundEvents.CONDUIT_AMBIENT_SHORT, 0.5F, 0.4F, 0.6F, 500),
                cue(SoundEvents.BEACON_DEACTIVATE, 0.3F, 0.3F, 0.5F, 1800),
                cue(SoundEvents.REDSTONE_TORCH_BURNOUT, 0.3F, 0.3F, 0.5F, 900)};
        return null;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        ResourceKey<Level> dim = mc.level.dimension();
        Cue[] cues = cuesFor(dim);
        if (cues == null) return;
        RandomSource r = mc.level.random;

        for (Cue c : cues) {
            if (r.nextInt(c.rarity()) != 0) continue;
            double a = r.nextDouble() * Math.PI * 2.0D;
            double d = 6.0D + r.nextDouble() * 14.0D;
            double x = mc.player.getX() + Math.cos(a) * d;
            double z = mc.player.getZ() + Math.sin(a) * d;
            double y = mc.player.getY() + r.nextDouble() * 3.0D - 1.0D;
            float pitch = c.pitchLo() + r.nextFloat() * (c.pitchHi() - c.pitchLo());
            mc.level.playLocalSound(x, y, z, c.sound(), SoundSource.AMBIENT, c.volume(), pitch, false);
        }

        // What drifts past you.
        if (dim.equals(Dimensions.ARCHIVE) && r.nextInt(3) == 0) {
            mc.level.addParticle(ParticleTypes.ASH, mc.player.getX() + r.nextGaussian() * 6.0D,
                    mc.player.getEyeY() + r.nextGaussian() * 2.0D, mc.player.getZ() + r.nextGaussian() * 6.0D, 0.0D, -0.004D, 0.0D);
        } else if (dim.equals(Dimensions.HALLWAY) && r.nextInt(20) == 0) {
            mc.level.addParticle(ParticleTypes.MYCELIUM, mc.player.getX() + r.nextGaussian() * 5.0D,
                    mc.player.getEyeY() + r.nextGaussian() * 1.5D, mc.player.getZ() + r.nextGaussian() * 5.0D, 0.0D, 0.0D, 0.0D);
        } else if (dim.equals(Dimensions.DROWNED) && mc.player.isUnderWater() && r.nextInt(4) == 0) {
            mc.level.addParticle(ParticleTypes.BUBBLE, mc.player.getX() + r.nextGaussian() * 4.0D,
                    mc.player.getY() + r.nextDouble() * 2.0D, mc.player.getZ() + r.nextGaussian() * 4.0D, 0.0D, 0.05D, 0.0D);
        } else if (dim.equals(Dimensions.HUSH) && r.nextInt(6) == 0) {
            mc.level.addParticle(ParticleTypes.ASH, mc.player.getX() + r.nextGaussian() * 8.0D,
                    mc.player.getEyeY() + r.nextGaussian() * 3.0D, mc.player.getZ() + r.nextGaussian() * 8.0D, 0.0D, -0.01D, 0.0D);
        } else if (dim.equals(Dimensions.ORCHARD) && r.nextInt(5) == 0) {
            mc.level.addParticle(ParticleTypes.MYCELIUM, mc.player.getX() + r.nextGaussian() * 7.0D,
                    mc.player.getEyeY() + r.nextGaussian() * 2.0D, mc.player.getZ() + r.nextGaussian() * 7.0D, 0.0D, -0.02D, 0.0D);
        } else if (dim.equals(Dimensions.WARD) && r.nextInt(30) == 0) {
            mc.level.addParticle(ParticleTypes.SMOKE, mc.player.getX() + r.nextGaussian() * 5.0D,
                    mc.player.getEyeY() + 1.0D, mc.player.getZ() + r.nextGaussian() * 5.0D, 0.0D, 0.01D, 0.0D);
        } else if (dim.equals(Dimensions.STATIC) && r.nextInt(2) == 0) {
            mc.level.addParticle(ParticleTypes.WHITE_ASH, mc.player.getX() + r.nextGaussian() * 8.0D,
                    mc.player.getEyeY() + r.nextGaussian() * 3.0D, mc.player.getZ() + r.nextGaussian() * 8.0D,
                    r.nextGaussian() * 0.02D, 0.0D, r.nextGaussian() * 0.02D);
        }
    }
}
