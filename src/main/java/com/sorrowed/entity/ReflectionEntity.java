package com.sorrowed.entity;

import com.sorrowed.mind.PathHistory;
import com.sorrowed.mind.Sorrow;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.story.Thoughts;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * You, ten seconds ago. It wears your skin and walks the exact path you walked, ten seconds behind.
 * Stop moving and it catches up. Stare at it too long, or let it get close, and it is gone.
 */
public class ReflectionEntity extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(ReflectionEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public static final int DELAY_TICKS = 200;

    private int age;
    private int stareTicks;

    public ReflectionEntity(EntityType<? extends ReflectionEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER, Optional.empty());
    }

    public void setOwner(UUID id) {
        this.entityData.set(OWNER, Optional.of(id));
    }

    public Optional<UUID> getOwnerId() {
        return this.entityData.get(OWNER);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        UUID id = getOwnerId().orElse(null);
        Player owner = id == null ? null : this.level().getPlayerByUUID(id);
        if (!(owner instanceof ServerPlayer sp) || sp.level() != this.level() || ++age > 1800) {
            this.discard();
            return;
        }

        PathHistory.Sample s = PathHistory.get(id, DELAY_TICKS);
        if (s == null) return;
        this.moveTo(s.x(), s.y(), s.z(), s.yaw(), 0.0F);
        this.setYHeadRot(s.yaw());
        this.yBodyRot = s.yaw();

        if (age > 40 && this.distanceTo(sp) < 3.5F) {
            vanish(sp);
            return;
        }

        // Staring at it for three seconds.
        Vec3 toMe = this.position().subtract(sp.getEyePosition()).normalize();
        boolean looking = sp.getLookAngle().dot(toMe) > 0.97D && sp.hasLineOfSight(this);
        if (looking) {
            if (++stareTicks == 1) ModNetwork.thought(sp, Thoughts.f(Thoughts.REFLECTION));
            if (stareTicks > 60) vanish(sp);
        } else if (stareTicks > 0) {
            stareTicks--;
        }
    }

    private void vanish(ServerPlayer player) {
        if (this.isRemoved()) return;
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ASH, getX(), getY() + 1.0D, getZ(), 40, 0.3D, 0.8D, 0.3D, 0.02D);
        }
        ModNetwork.thought(player, Thoughts.f(Thoughts.REFLECTION_VANISH));
        Sorrow.add(player, 8.0F);
        this.discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide) return false;
        if (source.getEntity() instanceof ServerPlayer sp) {
            vanish(sp);
        }
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
