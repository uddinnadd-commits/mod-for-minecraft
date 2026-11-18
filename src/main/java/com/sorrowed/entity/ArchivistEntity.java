package com.sorrowed.entity;

import com.sorrowed.item.PapersItem;
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
import net.minecraft.world.damagesource.DamageSource;
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
 * The Archivist. It stands in the aisles reading something it never turns the page of. It only
 * moves while you are not looking at it, and when it finally reaches you it presses a page into
 * your hand and is gone. You can never read what is on the page.
 */
public class ArchivistEntity extends Monster {
    private int idle;
    private boolean warned;

    public ArchivistEntity(EntityType<? extends ArchivistEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    /** Anywhere it can stand in the stacks; the lanterns do not keep it away. */
    public static boolean checkSpawn(EntityType<ArchivistEntity> type, ServerLevelAccessor level,
                                     MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && Mob.checkMobSpawnRules(type, level, reason, pos, random)
                && random.nextInt(9) == 0
                && level.getEntitiesOfClass(ArchivistEntity.class, new AABB(pos).inflate(64.0D)).isEmpty();
    }

    @Override
    protected void registerGoals() {
    }

    /** True while 'p' has this in roughly the middle of their view and nothing in the way. */
    static boolean watched(Player p, Mob mob, double cone) {
        Vec3 look = p.getViewVector(1.0F).normalize();
        Vec3 to = new Vec3(mob.getX() - p.getX(), mob.getEyeY() - p.getEyeY(), mob.getZ() - p.getZ());
        if (to.lengthSqr() < 1.0E-4D) return true;
        return look.dot(to.normalize()) > cone && p.hasLineOfSight(mob);
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

        boolean seen = watched(p, this, 0.55D);
        if (seen) {
            // Frozen mid-step, book up, not looking at you. Never looking at you.
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            if (!warned && this.distanceTo(p) < 20.0F && p instanceof ServerPlayer sp) {
                warned = true;
                ModNetwork.thought(sp, Thoughts.f(Thoughts.ARCHIVIST_SEEN));
            }
            return;
        }

        if (this.tickCount % 5 == 0) {
            this.getNavigation().moveTo(p, 1.35D);
        }
        if (this.distanceToSqr(p) < 3.2D && p instanceof ServerPlayer sp) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.HOSTILE, 1.0F, 0.6F);
            sp.hurt(this.damageSources().mobAttack(this), 5.0F);
            sp.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0));
            Sorrow.add(sp, 14.0F);
            ModNetwork.thought(sp, Thoughts.f(Thoughts.ARCHIVIST_TOUCH));
            if (!sp.getInventory().add(PapersItem.of("archivist"))) this.spawnAtLocation(PapersItem.of("archivist"));
            this.discard();
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BOOK_PAGE_TURN;
    }
}
