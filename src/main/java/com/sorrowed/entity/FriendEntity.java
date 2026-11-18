package com.sorrowed.entity;

import com.sorrowed.event.StoryEvents;
import com.sorrowed.story.Finale;
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
 */
public class FriendEntity extends PathfinderMob {
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
        if (this.level().isClientSide) return;
        Player near = this.level().getNearestPlayer(this, 14.0D);
        if (near != null) {
            this.getLookControl().setLookAt(near, 20.0F, 20.0F);
        } else {
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
        if (!this.level().isClientSide && player instanceof ServerPlayer sp) {
            StoryEvents.meet(sp, this);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }
}
