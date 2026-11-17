package com.sorrowed.entity;

import com.sorrowed.net.ModNetwork;
import com.sorrowed.registry.ModItems;
import com.sorrowed.story.Thoughts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;

/**
 * He is still trying to finish it. He walks in a straight line and lays a plank bridge under his
 * own feet, across the gaps in the Unfinished. Being near him is the only calm thing left.
 * Hurt him and he stops, says one thing, and leaves you a Rain Tear.
 */
public class MasonEntity extends PathfinderMob {
    private Direction dir = Direction.EAST;
    private int life = 4800;
    private boolean greeted;

    public MasonEntity(EntityType<? extends MasonEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        Level lvl = this.level();
        if (lvl.isClientSide) return;
        if (--life <= 0) {
            this.discard();
            return;
        }

        Player near = lvl.getNearestPlayer(this, 12.0D);
        if (!greeted && near instanceof ServerPlayer sp) {
            greeted = true;
            ModNetwork.thought(sp, Thoughts.f(Thoughts.MASON_SEEN));
        }

        if (this.tickCount % 12 != 0) return;
        if (this.random.nextInt(30) == 0) {
            dir = this.random.nextBoolean() ? dir.getClockWise() : dir.getCounterClockWise();
        }

        BlockPos fwd = this.blockPosition().relative(dir);
        boolean wallAhead = lvl.getBlockState(fwd).canOcclude() && lvl.getBlockState(fwd.above()).canOcclude();
        if (wallAhead) {
            dir = dir.getClockWise();
            return;
        }

        // Finish the bridge before stepping onto it.
        BlockPos below = fwd.below();
        if (lvl.getBlockState(below).isAir() || !lvl.getBlockState(below).getFluidState().isEmpty()) {
            lvl.setBlock(below, Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
            lvl.playSound(null, below, SoundType.WOOD.getPlaceSound(), SoundSource.BLOCKS, 0.8F, 0.9F);
        }
        this.getMoveControl().setWantedPosition(fwd.getX() + 0.5D, fwd.getY(), fwd.getZ() + 0.5D, 1.0D);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && source.getEntity() instanceof ServerPlayer sp) {
            ModNetwork.thought(sp, Thoughts.f(Thoughts.MASON_HURT));
            this.spawnAtLocation(new ItemStack(ModItems.RAIN_TEAR.get()));
            this.discard();
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
