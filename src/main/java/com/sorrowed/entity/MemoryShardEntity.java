package com.sorrowed.entity;

import com.sorrowed.registry.ModEntities;
import com.sorrowed.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A piece of a memory, thrown. Volley shards fly straight; rain shards fall out of the dark and
 * break where they land, hurting anyone standing on the spot that was marked for them.
 */
public class MemoryShardEntity extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Boolean> DATA_FALLING =
            SynchedEntityData.defineId(MemoryShardEntity.class, EntityDataSerializers.BOOLEAN);

    private float damage = 4.0F;
    private int life;

    public MemoryShardEntity(EntityType<? extends MemoryShardEntity> type, Level level) {
        super(type, level);
    }

    public MemoryShardEntity(Level level, LivingEntity owner, float damage, boolean falling) {
        super(ModEntities.MEMORY_SHARD.get(), owner, level);
        this.damage = damage;
        this.entityData.set(DATA_FALLING, falling);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FALLING, false);
    }

    private boolean falling() {
        return this.entityData.get(DATA_FALLING);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.MEMORY_SHARD.get();
    }

    @Override
    protected float getGravity() {
        return falling() ? 0.06F : 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.1D, this.getZ(), 0.0D, 0.0D, 0.0D);
        } else if (++life > 140) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        return super.canHitEntity(e) && !(e instanceof MemoryShattererEntity) && !(e instanceof MemoryShardEntity)
                && !(e instanceof HollowEntity);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide) return;
        Entity owner = this.getOwner();
        result.getEntity().hurt(this.damageSources().mobProjectile(this, owner instanceof LivingEntity l ? l : null), damage);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(this.level() instanceof ServerLevel level)) return;
        if (falling() && result.getType() == HitResult.Type.BLOCK) {
            // The spot was marked: whoever is still standing on it gets hurt.
            Entity owner = this.getOwner();
            for (Player p : level.getEntitiesOfClass(Player.class, new AABB(this.blockPosition()).inflate(1.6D, 1.5D, 1.6D))) {
                if (p.isSpectator() || p.isCreative()) continue;
                p.hurt(this.damageSources().mobProjectile(this, owner instanceof LivingEntity l ? l : null), damage);
            }
            level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.2D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 10, 0.2D, 0.2D, 0.2D, 0.08D);
        level.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.7F,
                1.1F + this.random.nextFloat() * 0.5F);
        this.discard();
    }
}
