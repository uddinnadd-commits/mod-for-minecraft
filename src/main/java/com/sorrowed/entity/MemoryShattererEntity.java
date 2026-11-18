package com.sorrowed.entity;

import com.sorrowed.item.BorrowedSwordItem;
import com.sorrowed.mind.Scheduler;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.registry.ModEntities;
import com.sorrowed.story.BossFight;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Memory Shatterer. What he turns into when you try to finish it: everything he would not let
 * go of, in his shape, twice his size, coming apart. It hovers, it blinks behind you, it throws
 * pieces of itself, and later it brings the sky down and slams the ground.
 *
 * Four stages, a quarter of its life each. No single blow can carry it past a stage, and while it
 * changes nothing touches it. Only the sword from the sky hurts it properly; anything else does a
 * third as much, and fire, falls and other creatures do nothing at all.
 */
public class MemoryShattererEntity extends Monster {
    public static final int FORM_TICKS = 70;
    public static final int DEATH_TICKS = 80;
    public static final String FRAGMENT_TAG = "sorrowed_fragment";
    private static final float[] SPEED = {0.20F, 0.24F, 0.28F, 0.34F};
    private static final float[] MELEE = {4.0F, 5.0F, 6.0F, 7.0F};
    private static final DustParticleOptions MARK = new DustParticleOptions(new Vector3f(0.85F, 0.08F, 0.12F), 1.4F);

    private static final EntityDataAccessor<Integer> DATA_STAGE =
            SynchedEntityData.defineId(MemoryShattererEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_FORMING =
            SynchedEntityData.defineId(MemoryShattererEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SHIELDED =
            SynchedEntityData.defineId(MemoryShattererEntity.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.translatable("entity.sorrowed.memory_shatterer"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS)
            .setDarkenScreen(true).setCreateWorldFog(true);

    @Nullable
    private UUID owner;
    private int dormant;
    private int transition;
    private int windup;
    private int charging;
    private int slam;
    private int meleeCd = 20;
    private int blinkCd = 80;
    private int volleyCd = 60;
    private int rainCd = 100;
    private int slamCd = 140;
    private int pulseCd = 100;
    private boolean reported;

    public MemoryShattererEntity(EntityType<? extends MemoryShattererEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_STAGE, 0);
        this.entityData.define(DATA_FORMING, 0);
        this.entityData.define(DATA_SHIELDED, true);
    }

    /** 0 to 3: remembering, forgetting, shattering, the last memory. */
    public int stage() {
        return this.entityData.get(DATA_STAGE);
    }

    /** Ticks left of the transformation out of him. */
    public int forming() {
        return this.entityData.get(DATA_FORMING);
    }

    public boolean shielded() {
        return this.entityData.get(DATA_SHIELDED);
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    /** Called when it is made out of him: who it is for, and how long to wait (the cutscene) before it moves. */
    public void awaken(UUID owner, int dormantTicks) {
        this.owner = owner;
        this.dormant = dormantTicks;
        this.entityData.set(DATA_FORMING, FORM_TICKS);
        this.entityData.set(DATA_SHIELDED, true);
    }

    // ------------------------------------------------------------------ what it is not

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean isAffectedByFluids() {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean addEffect(MobEffectInstance effect, @Nullable Entity source) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GLASS_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 1.6F;
    }

    // ------------------------------------------------------------------ the bar at the top

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    private void tell(String what) {
        for (ServerPlayer p : this.bossEvent.getPlayers()) ModNetwork.open(p, what);
    }

    // ------------------------------------------------------------------ every tick

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.isAlive()) {
            int n = forming() > 0 ? 4 : 1;
            for (int i = 0; i < n; i++) {
                if (this.random.nextInt(3) != 0) continue;
                this.level().addParticle(this.random.nextBoolean() ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.END_ROD,
                        this.getRandomX(0.8D), this.getY() + this.random.nextDouble() * this.getBbHeight(), this.getRandomZ(0.8D),
                        0.0D, 0.02D, 0.0D);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) this.level();
        ServerPlayer target = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (target == null || target.level() != level || !BossFight.isFighting(target) || target.isDeadOrDying()) {
            vanish();
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        int forming = forming();
        if (forming > 0) this.entityData.set(DATA_FORMING, forming - 1);
        face(target);
        if (this.tickCount % 46 == 0) this.playSound(SoundEvents.WARDEN_HEARTBEAT, 1.4F, 0.6F);

        if (dormant > 0) {
            // Rising off the dock as it comes out of him.
            float risen = 1.0F - forming() / (float) FORM_TICKS;
            move(this.getX(), this.getZ(), 0.2D + 1.6D * risen, 0.0D);
            if (--dormant == 0) wake();
            return;
        }
        if (transition > 0) {
            transitionTick(level, target);
            return;
        }
        fight(level, target);
    }

    private void wake() {
        this.entityData.set(DATA_SHIELDED, false);
        meleeCd = 40;   // a breath to get your bearings
        this.playSound(SoundEvents.WARDEN_ROAR, 2.5F, 0.6F);
        tell("boss:stage:0");
    }

    private void fight(ServerLevel level, ServerPlayer t) {
        int s = stage();
        double dist = this.distanceTo(t);
        meleeCd--;
        blinkCd--;
        volleyCd--;
        rainCd--;
        slamCd--;
        pulseCd--;

        if (slam > 0) {
            slamTick(level, t);
            return;
        }
        if (charging > 0) {
            charging--;
            move(this.getX(), this.getZ(), 1.4D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 2.8D, this.getZ(), 3, 0.9D, 0.6D, 0.9D, 0.0D);
            if (charging == 0) volley(level, t);
            return;
        }
        if (windup > 0) {
            windup--;
            move(t.getX(), t.getZ(), 0.6D, SPEED[s] * 0.6D);
            if (windup == 0) {
                this.swing(InteractionHand.MAIN_HAND, true);
                if (this.distanceTo(t) < 4.0D) melee(level, t);
                else this.playSound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.4F, 0.5F);
                meleeCd = 28 - s * 5;
            }
            return;
        }

        // Come at them, stopping just out of reach unless it is swinging.
        if (dist > 2.3D) move(t.getX(), t.getZ(), 0.6D, SPEED[s]);
        else move(this.getX(), this.getZ(), 0.6D, 0.0D);

        if (dist < 3.6D && meleeCd <= 0) {
            windup = 12 - s * 2;
            this.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 0.5F);
        } else if (blinkCd <= 0 && dist > 6.0D) {
            blink(level, t);
        } else if (s >= 1 && volleyCd <= 0 && dist > 4.0D) {
            charging = 20;
            volleyCd = 110 - s * 15;
            this.playSound(SoundEvents.BEACON_POWER_SELECT, 2.0F, 1.6F);
        } else if (s >= 2 && rainCd <= 0) {
            rain(level, t);
            rainCd = 170 - s * 20;
        } else if (s >= 2 && slamCd <= 0 && dist < 10.0D) {
            slam = 1;
            slamCd = 220 - s * 30;
            this.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 2.5F, 0.8F);
        } else if (s >= 3 && pulseCd <= 0) {
            pulse(level);
            pulseCd = 200;
        }
    }

    // ------------------------------------------------------------------ moving

    private double groundY(double x, double z) {
        return this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
    }

    /** Toward (tx, tz) at up to 'speed' blocks a tick, floating 'up' above the ground with a slow bob. */
    private void move(double tx, double tz, double up, double speed) {
        double dx = tx - this.getX();
        double dz = tz - this.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        double vx = 0.0D;
        double vz = 0.0D;
        if (d > 1.0E-3D && speed > 0.0D) {
            double step = Math.min(speed, d);
            vx = dx / d * step;
            vz = dz / d * step;
        }
        double gy = groundY(this.getX() + vx, this.getZ() + vz) + up + Math.sin(this.tickCount * 0.12D) * 0.18D;
        double vy = Mth.clamp(gy - this.getY(), -0.5D, 0.4D);
        this.setDeltaMovement(vx, vy, vz);
    }

    private void face(LivingEntity t) {
        double dx = t.getX() - this.getX();
        double dz = t.getZ() - this.getZ();
        double dy = t.getEyeY() - this.getEyeY();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.setXRot(pitch);
    }

    // ------------------------------------------------------------------ what it does

    private void melee(ServerLevel level, ServerPlayer t) {
        if (t.hurt(this.damageSources().mobAttack(this), MELEE[stage()])) {
            t.knockback(0.9D, this.getX() - t.getX(), this.getZ() - t.getZ());
            t.hurtMarked = true;
        }
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4F, 0.6F);
        this.playSound(SoundEvents.GLASS_BREAK, 1.0F, 0.6F);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, t.getX(), t.getY() + 1.0D, t.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    /** Gone, and then right behind you. */
    private void blink(ServerLevel level, ServerPlayer t) {
        Vec3 look = t.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0.0D, -look.z);
        if (back.lengthSqr() < 1.0E-4D) back = new Vec3(1.0D, 0.0D, 0.0D);
        back = back.normalize().scale(3.2D);
        double nx = t.getX() + back.x;
        double nz = t.getZ() + back.z;
        double ny = groundY(nx, nz) + 0.6D;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.7D, this.getZ(), 60, 0.5D, 1.2D, 0.5D, 0.1D);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.6F, 0.5F);
        this.teleportTo(nx, ny, nz);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, nx, ny + 1.7D, nz, 60, 0.5D, 1.2D, 0.5D, 0.1D);
        level.playSound(null, nx, ny, nz, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.6F, 0.4F);
        blinkCd = 150 - stage() * 25;
        meleeCd = 0;
    }

    /** A fan of shards, straight at you. */
    private void volley(ServerLevel level, ServerPlayer t) {
        int s = stage();
        int n = 3 + s;
        Vec3 from = new Vec3(this.getX(), this.getY() + 2.8D, this.getZ());
        Vec3 aim = t.getEyePosition().subtract(from).normalize();
        double base = Math.atan2(aim.z, aim.x);
        double flat = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
        for (int i = 0; i < n; i++) {
            double a = base + Math.toRadians((i - (n - 1) / 2.0D) * 9.0D);
            MemoryShardEntity shard = new MemoryShardEntity(level, this, 3.0F + s, false);
            shard.setPos(from.x, from.y, from.z);
            shard.shoot(Math.cos(a) * flat, aim.y, Math.sin(a) * flat, 0.95F, 0.0F);
            level.addFreshEntity(shard);
        }
        this.playSound(SoundEvents.AMETHYST_CLUSTER_BREAK, 2.0F, 0.8F);
    }

    /** The sky comes down: marked spots, then shards out of the dark onto them. */
    private void rain(ServerLevel level, ServerPlayer t) {
        int s = stage();
        int n = 5 + s * 2;
        List<Vec3> spots = new ArrayList<>();
        spots.add(new Vec3(t.getX(), groundY(t.getX(), t.getZ()), t.getZ()));
        for (int i = 1; i < n; i++) {
            double a = this.random.nextDouble() * Math.PI * 2.0D;
            double r = 2.0D + this.random.nextDouble() * 5.0D;
            double x = t.getX() + Math.cos(a) * r;
            double z = t.getZ() + Math.sin(a) * r;
            spots.add(new Vec3(x, groundY(x, z), z));
        }
        MinecraftServer server = level.getServer();
        for (int k = 0; k < 22; k += 3) {
            Scheduler.after(server, k, () -> {
                if (!this.isAlive()) return;
                for (Vec3 spot : spots) {
                    for (int i = 0; i < 8; i++) {
                        double a = i * Math.PI / 4.0D;
                        level.sendParticles(MARK, spot.x + Math.cos(a) * 1.2D, spot.y + 0.1D, spot.z + Math.sin(a) * 1.2D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    }
                }
            });
        }
        Scheduler.after(server, 22, () -> {
            if (!this.isAlive() || this.isRemoved()) return;
            for (Vec3 spot : spots) {
                MemoryShardEntity shard = new MemoryShardEntity(level, this, 4.0F + s, true);
                shard.setPos(spot.x, spot.y + 14.0D, spot.z);
                shard.setDeltaMovement(0.0D, -1.1D, 0.0D);
                level.addFreshEntity(shard);
            }
        });
        this.playSound(SoundEvents.ILLUSIONER_PREPARE_MIRROR, 2.5F, 0.6F);
    }

    /** Up, over you, and down. Jump and it misses. */
    private void slamTick(ServerLevel level, ServerPlayer t) {
        slam++;
        if (slam <= 24) {
            move(t.getX(), t.getZ(), 0.6D + slam * 0.18D, 0.14D);
            if (slam % 4 == 0) {
                level.sendParticles(MARK, t.getX(), groundY(t.getX(), t.getZ()) + 0.1D, t.getZ(), 12, 2.5D, 0.0D, 2.5D, 0.0D);
            }
            return;
        }
        double ground = groundY(this.getX(), this.getZ());
        double drop = Math.min(1.4D, this.getY() - (ground + 0.2D));
        if (drop > 0.25D && slam < 44) {
            this.setDeltaMovement(0.0D, -drop, 0.0D);
            return;
        }
        slam = 0;
        double r = 6.5D;
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), ground + 0.2D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        for (int ring = 1; ring <= 3; ring++) {
            for (int i = 0; i < 24; i++) {
                double a = i * Math.PI / 12.0D;
                level.sendParticles(ParticleTypes.CLOUD, this.getX() + Math.cos(a) * ring * 2.0D, ground + 0.3D,
                        this.getZ() + Math.sin(a) * ring * 2.0D, 1, 0.0D, 0.05D, 0.0D, 0.02D);
            }
        }
        level.playSound(null, this.getX(), ground, this.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.8F, 0.6F);
        level.playSound(null, this.getX(), ground, this.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.4F, 0.8F);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(r, 4.0D, r))) {
            if (p.isSpectator() || p.isCreative()) continue;
            ModNetwork.open(p, "boss:shake:0.8");
            if (p.distanceTo(this) > r) continue;
            if (!p.onGround() && p.getY() > groundY(p.getX(), p.getZ()) + 0.35D) continue;   // jumped it
            p.hurt(this.damageSources().mobAttack(this), 6.0F + stage());
            Vec3 away = p.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            away = away.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : away.normalize();
            p.setDeltaMovement(away.x * 1.4D, 0.55D, away.z * 1.4D);
            p.hurtMarked = true;
        }
    }

    /** The last stage: a ring of shards out from it, and the dark comes in. */
    private void pulse(ServerLevel level) {
        this.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 2.0F, 0.6F);
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI / 5.0D + this.random.nextDouble() * 0.2D;
            MemoryShardEntity shard = new MemoryShardEntity(level, this, 4.0F, false);
            shard.setPos(this.getX(), this.getY() + 1.8D, this.getZ());
            shard.shoot(Math.cos(a), -0.02D, Math.sin(a), 0.7F, 0.0F);
            level.addFreshEntity(shard);
        }
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(24.0D))) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0, false, false));
            ModNetwork.open(p, "boss:shake:0.5");
        }
    }

    // ------------------------------------------------------------------ between stages

    private float floor() {
        return this.getMaxHealth() * (3 - stage()) * 0.25F;
    }

    private void beginTransition() {
        int next = stage() + 1;
        this.entityData.set(DATA_STAGE, next);
        this.entityData.set(DATA_SHIELDED, true);
        transition = 70;
        windup = 0;
        charging = 0;
        slam = 0;
        ServerLevel level = (ServerLevel) this.level();
        level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.5F, 0.7F + next * 0.1F);
        level.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.8D, this.getZ(), 120, 0.5D, 1.0D, 0.5D, 0.35D);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(8.0D))) {
            if (p.isSpectator() || p.isCreative()) continue;
            Vec3 away = p.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
            away = away.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : away.normalize();
            p.setDeltaMovement(away.x * 1.2D, 0.4D, away.z * 1.2D);
            p.hurtMarked = true;
        }
        tell("boss:stage:" + next);
    }

    private void transitionTick(ServerLevel level, ServerPlayer t) {
        transition--;
        move(this.getX(), this.getZ(), 4.0D, 0.0D);
        if (transition % 5 == 0) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.8D, this.getZ(), 20, 0.6D, 1.2D, 0.6D, 0.2D);
        }
        if (transition > 0) return;
        this.entityData.set(DATA_SHIELDED, false);
        meleeCd = 20;
        blinkCd = 40;
        volleyCd = 30;
        rainCd = 50;
        slamCd = 90;
        pulseCd = 60;
        if (stage() >= 2) summonFragments(level, t, 2);
    }

    /** The ones with no faces: everything he never said. */
    private void summonFragments(ServerLevel level, ServerPlayer t, int n) {
        for (int i = 0; i < n; i++) {
            HollowEntity h = ModEntities.HOLLOW.get().create(level);
            if (h == null) continue;
            double a = this.random.nextDouble() * Math.PI * 2.0D;
            double x = t.getX() + Math.cos(a) * 6.0D;
            double z = t.getZ() + Math.sin(a) * 6.0D;
            h.moveTo(x, groundY(x, z), z, this.random.nextFloat() * 360.0F, 0.0F);
            h.addTag(FRAGMENT_TAG);
            h.setPersistenceRequired();
            h.setTarget(t);
            level.addFreshEntity(h);
            level.sendParticles(ParticleTypes.SOUL, x, groundY(x, z) + 1.0D, z, 20, 0.3D, 0.8D, 0.3D, 0.02D);
        }
        level.playSound(null, t.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    public static void clearFragments(ServerLevel level, Vec3 around) {
        for (HollowEntity h : level.getEntitiesOfClass(HollowEntity.class, new AABB(around, around).inflate(96.0D))) {
            if (h.getTags().contains(FRAGMENT_TAG)) h.discard();
        }
    }

    // ------------------------------------------------------------------ being hurt

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (this.level().isClientSide || this.isDeadOrDying()) return false;
        if (shielded() || forming() > 0) {
            if (source.getEntity() instanceof Player) {
                this.playSound(SoundEvents.AMETHYST_BLOCK_HIT, 1.4F, 0.5F);
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY() + 1.7D, this.getZ(),
                        12, 0.6D, 0.8D, 0.6D, 0.2D);
            }
            return false;
        }
        if (!(source.getEntity() instanceof Player player)) return false;
        boolean sword = source.getDirectEntity() == player && player.getMainHandItem().getItem() instanceof BorrowedSwordItem;
        if (!sword) amount *= 0.35F;
        int s = stage();
        if (s < 3) {
            amount = Math.min(amount, this.getHealth() - floor());
            if (amount <= 0.0F) return false;
        }
        boolean hit = super.hurt(source, amount);
        if (hit) {
            if (sword) player.heal(1.5F);
            if (s < 3 && this.getHealth() <= floor() + 0.01F) beginTransition();
        }
        return hit;
    }

    // ------------------------------------------------------------------ the end of it

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.entityData.set(DATA_SHIELDED, true);
        if (this.level() instanceof ServerLevel) {
            this.bossEvent.setProgress(0.0F);
            tell("boss:dying");
        }
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        this.setDeltaMovement(0.0D, 0.015D, 0.0D);
        if (!(this.level() instanceof ServerLevel level)) return;
        if (this.deathTime % 3 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.7D, this.getZ(), 6 + this.deathTime / 6,
                    0.7D, 1.2D, 0.7D, 0.05D + this.deathTime * 0.002D);
        }
        if (this.deathTime % 8 == 0) {
            level.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.6F, 0.4F + this.deathTime / 100.0F);
        }
        if (this.deathTime >= DEATH_TICKS && !this.isRemoved()) {
            level.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY() + 1.7D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 1.7D, this.getZ(), 220, 0.4D, 0.8D, 0.4D, 0.45D);
            level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 1.2F);
            level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
            clearFragments(level, this.position());
            UUID who = owner;
            this.remove(RemovalReason.KILLED);
            if (!reported) {
                reported = true;
                BossFight.defeated(level, who);
            }
        }
    }

    /** The fight is off (you left, or died): it just comes apart. */
    private void vanish() {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1.7D, this.getZ(), 80, 0.6D, 1.2D, 0.6D, 0.1D);
            clearFragments(level, this.position());
        }
        this.discard();
    }
}
