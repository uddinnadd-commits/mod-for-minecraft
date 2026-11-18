package com.sorrowed.entity;

import com.sorrowed.mind.Sorrow;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.registry.Dimensions;
import com.sorrowed.story.Thoughts;
import com.sorrowed.worldgen.HubBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Orderly. Something in a white coat pushing an empty bed down the corridor. It only moves
 * while you do: stop, and the wheels stop. If it reaches you, you wake up in the bed at the
 * start of the ward with no memory of lying down.
 */
public class OrderlyEntity extends Monster {
    private int idle;
    private boolean warned;
    private Vec3 lastPlayerPos = Vec3.ZERO;

    public OrderlyEntity(EntityType<? extends OrderlyEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    public static boolean checkSpawn(EntityType<OrderlyEntity> type, ServerLevelAccessor level,
                                     MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && Mob.checkMobSpawnRules(type, level, reason, pos, random)
                && random.nextInt(10) == 0
                && level.getEntitiesOfClass(OrderlyEntity.class, new AABB(pos).inflate(96.0D)).isEmpty();
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        Player p = this.level().getNearestPlayer(this, 64.0D);
        if (p == null || p.isCreative() || p.isSpectator()) {
            this.getNavigation().stop();
            if (++idle > 1200) this.discard();
            return;
        }
        idle = 0;
        this.getLookControl().setLookAt(p, 10.0F, 10.0F);

        boolean moving = p.position().distanceToSqr(lastPlayerPos) > 1.0E-3D;
        lastPlayerPos = p.position();
        double d = this.distanceTo(p);

        if (!warned && d < 30.0D && p.hasLineOfSight(this) && p instanceof ServerPlayer sp) {
            warned = true;
            ModNetwork.thought(sp, Thoughts.f(Thoughts.ORDERLY_SEEN));
        }

        if (moving) {
            if (this.tickCount % 5 == 0) this.getNavigation().moveTo(p, 1.0D);
            if (this.tickCount % 14 == 0) {
                this.level().playSound(null, this.blockPosition(), SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.HOSTILE, 0.5F, 0.4F);
            }
        } else {
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
        }

        if (d < 2.2D && p instanceof ServerPlayer sp && this.level() instanceof ServerLevel sl) {
            // Back to bed, and not gently.
            sp.hurt(this.damageSources().mobAttack(this), 4.0F);
            sp.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            Vec3 bed = HubBuilder.arrival(sl, Dimensions.WARD);
            sp.connection.teleport(bed.x, bed.y, bed.z, 0.0F, 0.0F);
            sl.playSound(null, BlockPos.containing(bed), SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.HOSTILE, 1.0F, 0.5F);
            Sorrow.add(sp, 12.0F);
            ModNetwork.thought(sp, Thoughts.f(Thoughts.ORDERLY_TAKEN));
            this.discard();
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
