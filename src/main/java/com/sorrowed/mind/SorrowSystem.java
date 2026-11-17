package com.sorrowed.mind;

import com.sorrowed.SorrowedConfig;
import com.sorrowed.cutscene.CutsceneServer;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.entity.ArchivistEntity;
import com.sorrowed.entity.CarrierEntity;
import com.sorrowed.entity.EchoEntity;
import com.sorrowed.entity.HollowEntity;
import com.sorrowed.entity.ListenerEntity;
import com.sorrowed.entity.MasonEntity;
import com.sorrowed.entity.NeighbourEntity;
import com.sorrowed.registry.Dimensions;
import com.sorrowed.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Once a second: what the world is doing to this player's head. */
public final class SorrowSystem {
    private SorrowSystem() {}

    public static void tick(ServerPlayer p) {
        if (!SorrowedConfig.EVENTS.get()) return;
        if (p.isCreative() || p.isSpectator()) return;

        ServerLevel level = p.serverLevel();
        ResourceKey<Level> dim = level.dimension();
        float d = 0.0F;

        if (dim.equals(Level.OVERWORLD)) {
            boolean sky = level.canSeeSky(p.blockPosition());
            if (level.isRaining() && sky) d += 0.25F;
            else if (!sky) d -= 0.25F;
            if (level.isNight()) d += 0.15F;
        } else if (dim.equals(Dimensions.HUSH)) {
            d += 0.8F;
        } else if (dim.equals(Dimensions.DROWNED)) {
            d += 0.4F;
        } else if (dim.equals(Dimensions.HALLWAY)) {
            d += 0.6F;
        } else if (dim.equals(Dimensions.UNFINISHED)) {
            d -= 0.3F;
        } else if (dim.equals(Dimensions.BACKUP)) {
            d -= 0.8F;
        } else if (dim.equals(Dimensions.ARCHIVE)) {
            d += 0.5F;
        } else if (dim.equals(Dimensions.STREET)) {
            d += 0.45F;
        } else if (dim.equals(Dimensions.STATIC)) {
            d += 0.7F;
        }

        AABB box = p.getBoundingBox().inflate(10.0D);
        if (!level.getEntitiesOfClass(HollowEntity.class, box).isEmpty()) d += 1.0F;
        if (!level.getEntitiesOfClass(ListenerEntity.class, box).isEmpty()) d += 1.2F;
        if (!level.getEntitiesOfClass(EchoEntity.class, box).isEmpty()) d += 0.4F;
        if (!level.getEntitiesOfClass(MasonEntity.class, box).isEmpty()) d -= 2.0F;
        if (!level.getEntitiesOfClass(ArchivistEntity.class, box).isEmpty()) d += 0.8F;
        if (!level.getEntitiesOfClass(NeighbourEntity.class, box).isEmpty()) d += 0.5F;
        if (!level.getEntitiesOfClass(CarrierEntity.class, box).isEmpty()) d += 0.6F;
        if (nearGrave(level, p.blockPosition())) d -= 1.5F;
        if (p.isSleeping()) d -= 1.5F;

        Sorrow.add(p, d);

        // At the top of the meter the world gives out, then lets you go.
        if (Sorrow.get(p) >= 99.5F) {
            CutsceneServer.play(p, "breakdown", null, () -> {
                Sorrow.set(p, 55.0F);
                ModNetwork.thought(p, "I think I fell asleep.");
            });
        }

        if (Sorrow.tier(p) >= 3 && !CutsceneServer.isPlaying(p)) {
            p.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 0.7F, 1.0F);
            if (level.random.nextInt(8) == 0) {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false));
            }
        }
    }

    private static boolean nearGrave(ServerLevel level, BlockPos c) {
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-4, -2, -4), c.offset(4, 2, 4))) {
            if (level.getBlockState(pos).is(ModBlocks.GRAVE_MARKER.get())) return true;
        }
        return false;
    }
}
