package com.sorrowed.entity;

import com.sorrowed.mind.Sorrow;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.story.Thoughts;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
 * The Copy. It is you, a few seconds after you were backed up. When it sees you it locks to you
 * like a reflection in a mirror you cannot see: step toward it and it steps toward you, turn
 * left and it turns right. If you meet in the middle, the newer one is deleted.
 */
public class CopyEntity extends Monster {
    private int idle;
    private Vec3 mirrorPoint;
    private Vec3 mirrorNormal;
    private ServerPlayer locked;

    public CopyEntity(EntityType<? extends CopyEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    public static boolean checkSpawn(EntityType<CopyEntity> type, ServerLevelAccessor level,
                                     MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && Mob.checkMobSpawnRules(type, level, reason, pos, random)
                && random.nextInt(14) == 0
                && level.getEntitiesOfClass(CopyEntity.class, new AABB(pos).inflate(96.0D)).isEmpty();
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        if (locked == null) {
            Player near = this.level().getNearestPlayer(this, 28.0D);
            if (near instanceof ServerPlayer sp && !sp.isCreative() && !sp.isSpectator() && sp.hasLineOfSight(this)) {
                // The mirror is the plane halfway between you, facing you.
                locked = sp;
                mirrorPoint = sp.position().add(this.position()).scale(0.5D);
                Vec3 n = this.position().subtract(sp.position());
                mirrorNormal = new Vec3(n.x, 0.0D, n.z).normalize();
                ModNetwork.thought(sp, Thoughts.f(Thoughts.COPY_SEEN));
                this.level().playSound(null, this.blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.0F, 0.6F);
            } else {
                if (++idle > 1200) this.discard();
                return;
            }
        }
        if (locked.isRemoved() || locked.level() != this.level() || locked.distanceTo(this) > 64.0F) {
            this.discard();
            return;
        }

        // Reflect the player through the plane.
        Vec3 pp = locked.position();
        double d = pp.subtract(mirrorPoint).dot(mirrorNormal);
        Vec3 target = pp.subtract(mirrorNormal.scale(2.0D * d));
        BlockPos feet = BlockPos.containing(target.x, pp.y, target.z);
        boolean clear = this.level().getBlockState(feet).getCollisionShape(this.level(), feet).isEmpty()
                && this.level().getBlockState(feet.above()).getCollisionShape(this.level(), feet.above()).isEmpty();
        if (clear) {
            this.setPos(target.x, pp.y, target.z);
        }
        // Mirror the facing as well: reflect the look direction through the plane's normal.
        Vec3 look = locked.getLookAngle();
        Vec3 mlook = look.subtract(mirrorNormal.scale(2.0D * look.dot(mirrorNormal)));
        float yaw = (float) (Mth.atan2(mlook.z, mlook.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
        this.setXRot(locked.getXRot());
        this.setDeltaMovement(Vec3.ZERO);

        if (this.distanceTo(locked) < 1.6F) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.0F, 0.5F);
            locked.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));
            locked.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            Sorrow.add(locked, 15.0F);
            ModNetwork.thought(locked, Thoughts.f(Thoughts.COPY_MET));
            this.discard();
        }
    }

    @Override
    public boolean isNoGravity() {
        return locked != null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
