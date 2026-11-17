package com.sorrowed.entity;

import com.sorrowed.mind.Sorrow;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.story.Thoughts;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * The Neighbour. Someone in a coat and hat, standing under a streetlight, looking at you.
 * Walk toward them and they walk away, never hurrying, never letting you get close.
 * Run at them and you hear a door close somewhere, and they are gone.
 */
public class NeighbourEntity extends Monster {
    private int idle;
    private boolean warned;

    public NeighbourEntity(EntityType<? extends NeighbourEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    /** Only ever on the pavement or the lawns, never many at once. */
    public static boolean checkSpawn(EntityType<NeighbourEntity> type, ServerLevelAccessor level,
                                     MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && Mob.checkMobSpawnRules(type, level, reason, pos, random)
                && level.canSeeSky(pos)
                && random.nextInt(4) == 0;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        Player p = this.level().getNearestPlayer(this, 40.0D);
        if (p == null || p.isCreative() || p.isSpectator()) {
            this.getNavigation().stop();
            if (++idle > 1200) this.discard();
            return;
        }
        idle = 0;

        this.getLookControl().setLookAt(p, 30.0F, 30.0F);
        double d = this.distanceTo(p);

        if (!warned && d < 28.0D && p.hasLineOfSight(this) && p instanceof ServerPlayer sp) {
            warned = true;
            ModNetwork.thought(sp, Thoughts.f(Thoughts.NEIGHBOUR_SEEN));
        }

        if (p.isSprinting() && d < 7.0D) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.HOSTILE, 1.0F, 0.8F);
            if (p instanceof ServerPlayer sp) {
                Sorrow.add(sp, 8.0F);
                ModNetwork.thought(sp, Thoughts.f(Thoughts.NEIGHBOUR_GONE));
            }
            this.discard();
            return;
        }

        if (d < 11.0D) {
            if (this.tickCount % 10 == 0) {
                Vec3 away = new Vec3(this.getX() - p.getX(), 0.0D, this.getZ() - p.getZ());
                if (away.lengthSqr() < 1.0E-4D) away = new Vec3(1.0D, 0.0D, 0.0D);
                away = away.normalize().scale(9.0D);
                this.getNavigation().moveTo(this.getX() + away.x, this.getY(), this.getZ() + away.z, 0.9D);
            }
        } else if (d > 16.0D) {
            this.getNavigation().stop();
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }
}
