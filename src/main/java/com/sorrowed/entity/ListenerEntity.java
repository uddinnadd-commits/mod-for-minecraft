package com.sorrowed.entity;

import com.sorrowed.mind.Sorrow;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.story.Thoughts;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Blind and still. It only moves toward noise: sprinting, jumping, swinging a tool, using an item.
 * Stand perfectly quiet and it will stand next to you and do nothing. Make a sound next to it
 * and it hits hard, and what it hits stays with you.
 */
public class ListenerEntity extends Monster {
    private int heard;
    private int cooldown;
    private int idle;
    private boolean warned;

    public ListenerEntity(EntityType<? extends ListenerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    private static boolean makesNoise(Player p) {
        return p.isSprinting()
                || p.swinging
                || p.isUsingItem()
                || (!p.onGround() && !p.isInWater() && p.getDeltaMovement().y > 0.1D);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        Player p = this.level().getNearestPlayer(this, 48.0D);
        if (p == null || p.isCreative() || p.isSpectator()) {
            this.getNavigation().stop();
            if (++idle > 1200) this.discard();
            return;
        }
        idle = 0;

        if (makesNoise(p)) heard = 30;
        if (cooldown > 0) cooldown--;

        if (heard > 0) {
            heard--;
            this.getNavigation().moveTo(p, 1.25D);
        } else {
            this.getNavigation().stop();
        }

        if (!warned && p instanceof ServerPlayer sp && this.distanceTo(p) < 8.0F) {
            warned = true;
            ModNetwork.thought(sp, Thoughts.f(Thoughts.LISTENER_NEAR));
        }

        // It only strikes while it can hear you.
        if (heard > 0 && cooldown == 0 && this.distanceToSqr(p) < 2.8D) {
            p.hurt(this.damageSources().mobAttack(this), 5.0F);
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            cooldown = 40;
            if (p instanceof ServerPlayer sp) {
                Sorrow.add(sp, 20.0F);
                ModNetwork.thought(sp, Thoughts.f(Thoughts.LISTENER_HIT));
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
