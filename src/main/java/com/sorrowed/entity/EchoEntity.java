package com.sorrowed.entity;

import com.sorrowed.mind.Sorrow;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.story.Thoughts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A pale figure that stands far off in the rain and watches. While it rains it keeps its distance.
 * When the rain STOPS, it walks straight toward you. It never attacks; it just arrives.
 */
public class EchoEntity extends PathfinderMob {
    private int lonely;
    private boolean spoke;

    public EchoEntity(EntityType<? extends EchoEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<Player>(this, Player.class, 9.0F, 0.9D, 1.1D) {
            @Override
            public boolean canUse() {
                return this.mob.level().isRaining() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 24.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.5D));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        Player nearest = this.level().getNearestPlayer(this, 40.0D);
        if (nearest == null) {
            if (++lonely > 600) this.discard();
            return;
        }
        lonely = 0;

        // The silence brings them closer.
        if (!this.level().isRaining() && this.tickCount % 10 == 0) {
            this.getNavigation().moveTo(nearest, 0.6D);
        }

        if (nearest instanceof ServerPlayer sp) {
            double dist = this.distanceTo(sp);
            if (!spoke && dist < 24.0D) {
                spoke = true;
                ModNetwork.thought(sp, Thoughts.f(Thoughts.ECHO_SEEN));
            }
            if (dist < 4.0D) {
                vanish(sp);
            }
        }
    }

    private void vanish(ServerPlayer player) {
        if (this.isRemoved()) return;
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.0D, getZ(), 30, 0.3D, 0.8D, 0.3D, 0.02D);
        }
        ModNetwork.thought(player, Thoughts.f(Thoughts.ECHO_VANISH));
        Sorrow.add(player, 10.0F);
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) return false;
        if (source.getEntity() instanceof ServerPlayer sp) {
            vanish(sp);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
