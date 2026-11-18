package com.sorrowed.entity;

import com.sorrowed.event.StoryEvents;
import com.sorrowed.story.Finale;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Him. He sits at the end of the dock in his world, facing the water, and turns his head when you
 * come. He cannot be hurt and does not move. He is only there while someone has finished
 * everything and not yet said goodbye.
 *
 * With a halo he is the one at the very end: he comes down out of the sky (the descent is drawn by
 * the client from his age; he is really standing on the dock the whole time) and stands.
 */
public class FriendEntity extends PathfinderMob {
    public static final int DESCENT_TICKS = 170;
    public static final double DESCENT_HEIGHT = 30.0D;
    private static final EntityDataAccessor<Boolean> DATA_HALO =
            SynchedEntityData.defineId(FriendEntity.class, EntityDataSerializers.BOOLEAN);

    public FriendEntity(EntityType<? extends FriendEntity> type, Level level) {
        super(type, level);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_HALO, false);
    }

    public boolean isHalo() {
        return this.entityData.get(DATA_HALO);
    }

    public void setHalo(boolean halo) {
        this.entityData.set(DATA_HALO, halo);
    }

    /** How far down out of the sky he has come, 0 to 1. Always 1 without a halo. */
    public float descent(float partialTick) {
        if (!isHalo()) return 1.0F;
        float t = Math.min(1.0F, (this.tickCount + partialTick) / DESCENT_TICKS);
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Halo", isHalo());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHalo(tag.getBoolean("Halo"));
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
        if (this.level().isClientSide) {
            if (isHalo() && this.random.nextInt(2) == 0) {
                double up = (1.0D - descent(0.0F)) * DESCENT_HEIGHT;
                this.level().addParticle(ParticleTypes.END_ROD, this.getRandomX(0.6D), this.getY() + up + this.random.nextDouble() * 2.0D,
                        this.getRandomZ(0.6D), 0.0D, -0.02D, 0.0D);
            }
            return;
        }
        Player near = this.level().getNearestPlayer(this, 14.0D);
        if (near != null) {
            this.getLookControl().setLookAt(near, 20.0F, 20.0F);
        } else if (!isHalo()) {
            // Back to the water.
            this.getLookControl().setLookAt(this.getX() + Math.cos(Math.toRadians(this.getYRot() + 90.0F)),
                    this.getEyeY() - 0.4D, this.getZ() + Math.sin(Math.toRadians(this.getYRot() + 90.0F)));
        }
        if (this.tickCount % 40 == 0 && this.level() instanceof ServerLevel level && !Finale.anyoneWaiting(level)) {
            this.discard();
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && player instanceof ServerPlayer sp && !isHalo()) {
            StoryEvents.meet(sp, this);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }
}
