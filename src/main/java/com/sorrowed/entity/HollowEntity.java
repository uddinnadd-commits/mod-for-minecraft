package com.sorrowed.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * A tall faceless thing that only walks in the rain. Slow, tough, and it leaves you heavy.
 * It will not attack anyone who is holding a lantern: it just stands there, facing the light.
 */
public class HollowEntity extends Monster {
    public HollowEntity(EntityType<? extends HollowEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /** Spawns on open ground while it is raining, any time of day. */
    public static boolean checkSpawn(EntityType<HollowEntity> type, ServerLevelAccessor level,
                                     MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && Mob.checkMobSpawnRules(type, level, reason, pos, random)
                && level.getLevel().isRaining()
                && level.canSeeSky(pos)
                && random.nextInt(4) == 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || this.tickCount % 10 != 0) return;
        if (this.level() instanceof ServerLevel sl) {
            BlockPos at = this.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(at.offset(-2, -1, -2), at.offset(2, 2, 2))) {
                BlockState state = sl.getBlockState(pos);
                if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.LANTERN)) {
                    sl.removeBlock(pos, false);
                    sl.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.8F, 0.6F);
                    sl.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 0.6D, pos.getZ() + 0.5D, 8, 0.1D, 0.2D, 0.1D, 0.01D);
                    return;
                }
            }
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof Player player && (isLight(player.getMainHandItem()) || isLight(player.getOffhandItem()))) {
            return false;
        }
        return super.canAttack(target);
    }

    private static boolean isLight(ItemStack stack) {
        return stack.is(Items.LANTERN) || stack.is(Items.SOUL_LANTERN);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
