package com.sorrowed.entity;

import com.sorrowed.mind.Sorrow;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.story.Thoughts;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Carrier. A figure with a television for a head, wandering the static. Looking at the
 * screen hurts: the longer you watch, the worse you feel, and when you have watched long
 * enough it is suddenly behind you instead. After the third time it is gone.
 */
public class CarrierEntity extends Monster {
    private int idle;
    private int stare;
    private int hops;

    public CarrierEntity(EntityType<? extends CarrierEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    public static boolean checkSpawn(EntityType<CarrierEntity> type, ServerLevelAccessor level,
                                     MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && Mob.checkMobSpawnRules(type, level, reason, pos, random)
                && random.nextInt(14) == 0
                && level.getEntitiesOfClass(CarrierEntity.class, new AABB(pos).inflate(64.0D)).isEmpty();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        Player p = this.level().getNearestPlayer(this, 32.0D);
        if (p == null || p.isCreative() || p.isSpectator()) {
            if (++idle > 1200) this.discard();
            return;
        }
        idle = 0;

        if (!ArchivistEntity.watched(p, this, 0.6D)) {
            if (stare > 0) stare--;
            return;
        }

        stare++;
        if (stare % 5 == 0 && p instanceof ServerPlayer sp) {
            ModNetwork.statik(sp, Math.min(1.0F, 0.15F + stare / 100.0F));
        }
        if (stare % 40 == 1) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.HOSTILE, 1.2F, 0.5F);
        }
        if (stare % 20 == 0 && p instanceof ServerPlayer sp) {
            Sorrow.add(sp, 1.5F);
        }
        if (stare < 100) return;

        stare = 0;
        hops++;
        Vec3 look = p.getLookAngle();
        Vec3 target = p.position().subtract(look.x * 7.0D, 0.0D, look.z * 7.0D);
        BlockPos spot = standable(BlockPos.containing(target));
        if (spot != null) {
            this.teleportTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D);
            this.getNavigation().stop();
        }
        if (p instanceof ServerPlayer sp) {
            ModNetwork.thought(sp, Thoughts.f(hops >= 3 ? Thoughts.CARRIER_GONE : Thoughts.CARRIER_MOVED));
        }
        if (hops >= 3) this.discard();
    }

    private BlockPos standable(BlockPos guess) {
        for (int dy = 3; dy >= -4; dy--) {
            BlockPos pos = guess.above(dy);
            if (this.level().getBlockState(pos).isAir()
                    && this.level().getBlockState(pos.above()).isAir()
                    && !this.level().getBlockState(pos.below()).isAir()) {
                return pos;
            }
        }
        return null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
