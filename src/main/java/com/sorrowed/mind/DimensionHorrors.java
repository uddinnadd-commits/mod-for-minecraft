package com.sorrowed.mind;

import com.sorrowed.SorrowedConfig;
import com.sorrowed.entity.CarrierEntity;
import com.sorrowed.entity.ScarecrowEntity;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.registry.Dimensions;
import com.sorrowed.registry.ModEntities;
import com.sorrowed.story.Thoughts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * What each world does to you while you are in it. Something small happens now and then; the
 * longer you stay, the more often, and after a while the world starts telling you to leave:
 * a heartbeat that is not yours, the dark closing in. The exits are always somewhere close.
 */
public final class DimensionHorrors {
    private DimensionHorrors() {}

    private record Stay(ResourceKey<Level> dim, long since) {}

    private static final Map<UUID, Stay> STAYS = new HashMap<>();
    private static final long LEAVE_AFTER = 20L * 60L * 7L;   // seven minutes

    public static void forget(UUID id) {
        STAYS.remove(id);
    }

    /** Called once a second per player. */
    public static void tick(ServerPlayer p) {
        if (!SorrowedConfig.EVENTS.get()) return;
        if (p.isCreative() || p.isSpectator()) return;
        ServerLevel level = p.serverLevel();
        ResourceKey<Level> dim = level.dimension();
        if (dim.equals(Level.OVERWORLD) || dim.equals(Dimensions.BACKUP) || !dim.location().getNamespace().equals("sorrowed")) {
            STAYS.remove(p.getUUID());
            return;
        }
        long now = level.getGameTime();
        Stay stay = STAYS.get(p.getUUID());
        if (stay == null || !stay.dim().equals(dim)) {
            stay = new Stay(dim, now);
            STAYS.put(p.getUUID(), stay);
        }
        long here = now - stay.since();
        if (here < 20L * 30L) return;   // half a minute's grace when you arrive
        RandomSource r = level.random;

        // The longer you stay, the closer together they come: about every 90 s at first, every 25 s by the end.
        int minutes = (int) (here / 1200L);
        int odds = Math.max(25, 90 - minutes * 10);
        if (r.nextInt(odds) == 0) {
            happen(p, level, dim, r);
        }

        // Past seven minutes, the world wants you gone.
        if (here > LEAVE_AFTER && here % 600L < 20L) {
            sound(p, SoundEvents.WARDEN_HEARTBEAT, p.position(), 1.0F, 0.8F);
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0, false, false));
            if (here - LEAVE_AFTER < 20L) ModNetwork.thought(p, Thoughts.f(Thoughts.STAYED_TOO_LONG));
        }
    }

    private static Vec3 behind(ServerPlayer p, double dist) {
        Vec3 look = p.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0.0D, -look.z);
        if (back.lengthSqr() < 1.0E-4D) back = new Vec3(0.0D, 0.0D, 1.0D);
        return p.position().add(back.normalize().scale(dist));
    }

    private static Vec3 around(ServerPlayer p, RandomSource r, double dist) {
        double a = r.nextDouble() * Math.PI * 2.0D;
        return p.position().add(Math.cos(a) * dist, 0.0D, Math.sin(a) * dist);
    }

    private static void sound(ServerPlayer p, SoundEvent s, Vec3 at, float volume, float pitch) {
        p.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(s),
                SoundSource.AMBIENT, at.x, at.y, at.z, volume, pitch, p.getRandom().nextLong()));
    }

    private static void repeat(ServerPlayer p, SoundEvent s, Vec3 at, float volume, float pitch, int times, int every) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        for (int i = 0; i < times; i++) {
            Scheduler.after(server, i * every, () -> {
                if (!p.isRemoved()) sound(p, s, at, volume, pitch);
            });
        }
    }

    private static void think(ServerPlayer p, String[] pool, RandomSource r) {
        ModNetwork.thought(p, Thoughts.f(pool[r.nextInt(pool.length)]));
    }

    private static void happen(ServerPlayer p, ServerLevel level, ResourceKey<Level> dim, RandomSource r) {
        int pick = r.nextInt(3);
        if (dim.equals(Dimensions.HALLWAY)) {
            if (pick == 0) {
                // The lights die around you, a panel at a time, and stay dead.
                killLights(p, level, 6, Blocks.GLOWSTONE.defaultBlockState(), Blocks.REDSTONE_LAMP.defaultBlockState());
                sound(p, SoundEvents.REDSTONE_TORCH_BURNOUT, p.position().add(0, 6, 0), 1.0F, 0.5F);
                think(p, Thoughts.HORROR_HALLWAY, r);
            } else if (pick == 1) {
                sound(p, SoundEvents.WOODEN_DOOR_CLOSE, behind(p, 10), 1.2F, 0.6F);
                ModNetwork.thought(p, Thoughts.f(Thoughts.HORROR_DOOR));
            } else {
                repeat(p, SoundEvents.WOOL_STEP, behind(p, 6), 0.9F, 0.7F, 5, 6);
            }
        } else if (dim.equals(Dimensions.DROWNED)) {
            if (pick == 0 && p.isInWater()) {
                // Something vast looks at you through the water.
                p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.GUARDIAN_ELDER_EFFECT, 1.0F));
                think(p, Thoughts.HORROR_DROWNED, r);
            } else if (pick == 1) {
                sound(p, SoundEvents.ELDER_GUARDIAN_AMBIENT, around(p, r, 30), 2.0F, 0.4F);
            } else {
                level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, p.getX(), p.getY() - 2.0D, p.getZ(), 40, 0.6D, 1.0D, 0.6D, 0.1D);
                sound(p, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, p.position(), 1.0F, 0.6F);
            }
        } else if (dim.equals(Dimensions.ARCHIVE)) {
            if (pick == 0) {
                // The candles go out near you, one after another.
                int n = 0;
                for (BlockPos pos : BlockPos.betweenClosed(p.blockPosition().offset(-10, -2, -10), p.blockPosition().offset(10, 6, 10))) {
                    BlockState st = level.getBlockState(pos);
                    if (st.getBlock() instanceof CandleBlock && st.getValue(CandleBlock.LIT)) {
                        level.setBlock(pos, st.setValue(CandleBlock.LIT, false), 3);
                        level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
                        if (++n >= 6) break;
                    }
                }
                think(p, Thoughts.HORROR_ARCHIVE, r);
            } else if (pick == 1) {
                repeat(p, SoundEvents.BOOK_PAGE_TURN, behind(p, 4), 1.0F, 0.5F, 6, 3);
            } else {
                sound(p, SoundEvents.CHISELED_BOOKSHELF_PICKUP, around(p, r, 8), 1.0F, 0.6F);
                ModNetwork.thought(p, Thoughts.f(Thoughts.HORROR_NAME));
            }
        } else if (dim.equals(Dimensions.STREET)) {
            if (pick == 0) {
                // The one lit window in the street goes dark while you look at it.
                BlockPos lit = find(level, p.blockPosition(), 28, Blocks.GLOWSTONE.defaultBlockState());
                if (lit != null) {
                    level.setBlock(lit, Blocks.OAK_PLANKS.defaultBlockState(), 3);
                    ModNetwork.thought(p, Thoughts.f(Thoughts.HORROR_WINDOW));
                }
            } else if (pick == 1) {
                repeat(p, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, around(p, r, 12), 0.7F, 1.1F, 3, 12);
            } else {
                sound(p, SoundEvents.WOLF_GROWL, around(p, r, 14), 1.0F, 0.6F);
            }
        } else if (dim.equals(Dimensions.STATIC)) {
            ModNetwork.statik(p, 0.9F);
            if (pick == 0 && level.getEntitiesOfClass(CarrierEntity.class, p.getBoundingBox().inflate(48)).isEmpty()) {
                Vec3 at = behind(p, 18);
                CarrierEntity c = ModEntities.CARRIER.get().create(level);
                if (c != null) {
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) at.x, (int) at.z);
                    c.moveTo(at.x, y, at.z, p.getYRot(), 0.0F);
                    level.addFreshEntity(c);
                }
            }
            think(p, Thoughts.HORROR_STATIC, r);
        } else if (dim.equals(Dimensions.ORCHARD)) {
            if (pick == 0) {
                // The scarecrow you passed is now behind you.
                for (ScarecrowEntity s : level.getEntitiesOfClass(ScarecrowEntity.class, p.getBoundingBox().inflate(48))) {
                    if (s.isDown()) continue;
                    Vec3 at = behind(p, 10);
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) at.x, (int) at.z);
                    s.teleportTo(at.x, y, at.z);
                    sound(p, SoundEvents.WOOD_BREAK, at, 0.6F, 0.5F);
                    ModNetwork.thought(p, Thoughts.f(Thoughts.HORROR_SCARECROW));
                    break;
                }
            } else if (pick == 1) {
                sound(p, SoundEvents.STONE_FALL, behind(p, 3), 1.0F, 0.6F);
                think(p, Thoughts.HORROR_ORCHARD, r);
            } else {
                repeat(p, SoundEvents.PHANTOM_FLAP, around(p, r, 10), 0.8F, 0.5F, 4, 5);
            }
        } else if (dim.equals(Dimensions.WARD)) {
            if (pick == 0) {
                // The corridor lamps go out in a wave toward you.
                killLights(p, level, 14, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true),
                        Blocks.REDSTONE_LAMP.defaultBlockState());
                sound(p, SoundEvents.BEACON_DEACTIVATE, p.position(), 1.0F, 0.5F);
                think(p, Thoughts.HORROR_WARD, r);
            } else if (pick == 1) {
                repeat(p, SoundEvents.IRON_TRAPDOOR_OPEN, behind(p, 14), 0.6F, 0.4F, 6, 7);
            } else {
                // The monitor flatlines.
                repeat(p, SoundEvents.NOTE_BLOCK_BIT.value(), around(p, r, 6), 0.8F, 1.9F, 20, 1);
                ModNetwork.thought(p, Thoughts.f(Thoughts.HORROR_FLATLINE));
            }
        } else if (dim.equals(Dimensions.SERVER)) {
            if (pick == 0) {
                // The fans spin down, all at once, and it is very quiet.
                sound(p, SoundEvents.BEACON_DEACTIVATE, p.position(), 1.5F, 0.4F);
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));
                think(p, Thoughts.HORROR_SERVER, r);
            } else if (pick == 1) {
                // A line in chat, from him, in his way of typing.
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal("<" + Thoughts.f("{friend}").toLowerCase() + "> "
                        + Thoughts.FRIEND_CHAT[r.nextInt(Thoughts.FRIEND_CHAT.length)]));
            } else {
                repeat(p, SoundEvents.STONE_BUTTON_CLICK_ON, around(p, r, 5), 0.6F, 1.6F, 8, 2);
            }
        } else if (dim.equals(Dimensions.HUSH)) {
            if (pick == 0) {
                sound(p, SoundEvents.WOLF_HOWL, around(p, r, 12), 1.2F, 0.5F);
            } else if (pick == 1) {
                repeat(p, SoundEvents.WOOD_HIT, around(p, r, 9), 1.0F, 0.6F, 3, 10);
                ModNetwork.thought(p, Thoughts.f(Thoughts.HORROR_KNOCK));
            } else {
                level.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1.0D, p.getZ(), 20, 3.0D, 1.0D, 3.0D, 0.01D);
                think(p, Thoughts.HORROR_HUSH, r);
            }
        }
    }

    private static void killLights(ServerPlayer p, ServerLevel level, int radius, BlockState from, BlockState to) {
        BlockPos c = p.blockPosition();
        MinecraftServer server = p.getServer();
        int n = 0;
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-radius, 0, -radius), c.offset(radius, 10, radius))) {
            if (level.getBlockState(pos) != from) continue;
            BlockPos at = pos.immutable();
            int delay = 3 + (int) Math.sqrt(at.distSqr(c)) * 2;   // a wave, outward from where you stand
            if (server != null) Scheduler.after(server, delay, () -> level.setBlock(at, to, 2));
            if (++n >= 24) break;
        }
    }

    private static BlockPos find(ServerLevel level, BlockPos c, int radius, BlockState what) {
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-radius, -2, -radius), c.offset(radius, 8, radius))) {
            if (level.getBlockState(pos) == what) return pos.immutable();
        }
        return null;
    }
}
