package com.sorrowed.entity;

import com.sorrowed.mind.Sorrow;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.story.Thoughts;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Scarecrow. It hangs on its pole with its arms out and its head turned to you, always.
 * Come close without a light in your hand and it climbs down. It wants what you are holding;
 * when it gets it, it carries it off into the rows and goes back up on a pole somewhere else.
 */
public class ScarecrowEntity extends Monster {
    private int idle;
    private boolean down;
    private boolean warned;

    public ScarecrowEntity(EntityType<? extends ScarecrowEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.33D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    public static boolean checkSpawn(EntityType<ScarecrowEntity> type, ServerLevelAccessor level,
                                     MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && Mob.checkMobSpawnRules(type, level, reason, pos, random)
                && random.nextInt(8) == 0
                && level.getEntitiesOfClass(ScarecrowEntity.class, new AABB(pos).inflate(48.0D)).isEmpty();
    }

    public boolean isDown() {
        return down;
    }

    @Override
    protected void registerGoals() {
    }

    private static boolean carriesLight(Player p) {
        return isLight(p.getMainHandItem()) || isLight(p.getOffhandItem());
    }

    private static boolean isLight(ItemStack s) {
        return s.is(Items.TORCH) || s.is(Items.LANTERN) || s.is(Items.SOUL_LANTERN) || s.is(Items.SOUL_TORCH);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        Player p = this.level().getNearestPlayer(this, 32.0D);
        if (p == null || p.isCreative() || p.isSpectator()) {
            this.getNavigation().stop();
            if (++idle > 1200) this.discard();
            return;
        }
        idle = 0;
        this.getLookControl().setLookAt(p, 60.0F, 60.0F);
        double d = this.distanceTo(p);

        if (!warned && d < 18.0D && p instanceof ServerPlayer sp) {
            warned = true;
            ModNetwork.thought(sp, Thoughts.f(Thoughts.SCARECROW_SEEN));
        }

        if (!down) {
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            if (d < 16.0D && !carriesLight(p)) {
                down = true;
                this.level().playSound(null, this.blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 1.0F, 0.5F);
                if (p instanceof ServerPlayer sp) ModNetwork.thought(sp, Thoughts.f(Thoughts.SCARECROW_DOWN));
            }
            return;
        }

        if (carriesLight(p)) {
            this.getNavigation().stop();
            return;
        }
        if (this.tickCount % 5 == 0) this.getNavigation().moveTo(p, 1.3D);
        if (this.tickCount % 9 == 0) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.GRASS_STEP, SoundSource.HOSTILE, 0.6F, 0.5F);
        }

        if (d < 2.0D && p instanceof ServerPlayer sp) {
            sp.hurt(this.damageSources().mobAttack(this), 4.0F);
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            ItemStack held = sp.getMainHandItem();
            if (!held.isEmpty()) {
                ItemStack taken = held.copy();
                held.setCount(0);
                Vec3 away = this.position().subtract(sp.position()).normalize().scale(20.0D);
                ItemEntity drop = new ItemEntity(this.level(), this.getX() + away.x, this.getY() + 1.0D, this.getZ() + away.z, taken);
                drop.setDefaultPickUpDelay();
                this.level().addFreshEntity(drop);
                ModNetwork.thought(sp, Thoughts.f(Thoughts.SCARECROW_TOOK));
            } else {
                ModNetwork.thought(sp, Thoughts.f(Thoughts.SCARECROW_TOUCH));
            }
            Sorrow.add(sp, 10.0F);
            this.level().playSound(null, this.blockPosition(), SoundEvents.WOOL_BREAK, SoundSource.HOSTILE, 1.0F, 0.4F);
            this.discard();
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
