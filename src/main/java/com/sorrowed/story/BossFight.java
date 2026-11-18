package com.sorrowed.story;

import com.sorrowed.cutscene.Cutscene;
import com.sorrowed.cutscene.CutsceneLibrary;
import com.sorrowed.cutscene.CutsceneServer;
import com.sorrowed.entity.FriendEntity;
import com.sorrowed.entity.MemoryShattererEntity;
import com.sorrowed.item.BorrowedSwordItem;
import com.sorrowed.mind.Scheduler;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.registry.Dimensions;
import com.sorrowed.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The twist. You reach him at the pond, he starts to say goodbye, and then he will not let you
 * finish it: he comes apart into the Memory Shatterer. Something falls out of the sky for you to
 * fight it with. Win and he comes back down, himself again; die or walk away and he is sitting on
 * the dock again, waiting for you to try.
 */
public final class BossFight {
    private BossFight() {}

    /** Ticks into each cutscene: when he turns, and when the sword is dropped. */
    private static final int TWIST_TURN = 290;
    private static final int TWIST_SWORD = 405;
    private static final int REMATCH_TURN = 40;
    private static final int REMATCH_SWORD = 95;

    public static boolean isFighting(ServerPlayer p) {
        return Progress.stage(p) == Progress.FIGHTING;
    }

    /** You reached him. He talks, and then he turns. */
    public static void begin(ServerPlayer p, FriendEntity him) {
        MinecraftServer server = p.getServer();
        if (server == null) return;
        boolean again = Progress.flag(p, "twisted");
        String id = again ? "rematch" : "twist";
        Cutscene scene = CutsceneLibrary.get(id);
        boolean played = scene != null && CutsceneServer.play(p, id, Finale.SEAT, null);
        int turn;
        int sword;
        int dormant;
        if (played) {
            turn = again ? REMATCH_TURN : TWIST_TURN;
            sword = again ? REMATCH_SWORD : TWIST_SWORD;
            dormant = scene.duration() - turn + 10;
        } else {
            // No cutscenes: the same thing, said where you stand.
            ModNetwork.thought(p, Thoughts.f(again ? "you came back." : "...{friend}? hey. you took so long."));
            ModNetwork.thought(p, again ? "STAY." : "you can't take them. they're all i've got left. STAY.");
            turn = 50;
            sword = 70;
            dormant = 120;
        }
        Progress.setFlag(p, "twisted");
        Scheduler.after(server, turn, () -> {
            if (!p.isRemoved()) turn(p, him, dormant);
        });
        Scheduler.after(server, sword, () -> {
            if (!p.isRemoved() && isFighting(p)) dropSword(p);
        });
    }

    /** He comes apart. */
    private static void turn(ServerPlayer p, @Nullable FriendEntity him, int dormant) {
        Finale.forget(p.getUUID());
        ServerLevel level = p.serverLevel();
        if (!level.dimension().equals(Dimensions.BACKUP) || Progress.stage(p) != Progress.READY) return;
        Vec3 at = him != null && him.isAlive() ? him.position() : Finale.SEAT;
        for (FriendEntity f : level.getEntitiesOfClass(FriendEntity.class, new AABB(BlockPos.containing(Finale.SEAT)).inflate(8.0D))) {
            f.discard();
        }
        MemoryShattererEntity boss = ModEntities.MEMORY_SHATTERER.get().create(level);
        if (boss == null) return;
        boss.moveTo(at.x, at.y, at.z, Finale.SEAT_YAW, 0.0F);
        boss.awaken(p.getUUID(), dormant);
        level.addFreshEntity(boss);
        Progress.setStage(p, Progress.FIGHTING);
        Progress.sync(p);
        BlockPos where = BlockPos.containing(at);
        level.playSound(null, where, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.5F, 0.8F);
        level.playSound(null, where, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.playSound(null, where, SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0F, 0.6F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1.0D, at.z, 150, 0.6D, 1.0D, 0.6D, 0.6D);
        ModNetwork.open(p, "boss:turn");
    }

    /** A sword, falling out of the dark, lands at your feet and is in your hand. */
    private static void dropSword(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        MinecraftServer server = level.getServer();
        double x = p.getX();
        double z = p.getZ();
        ItemEntity item = new ItemEntity(level, x, p.getY() + 34.0D, z, BorrowedSwordItem.create());
        item.setNeverPickUp();
        item.setUnlimitedLifetime();
        item.setGlowingTag(true);
        item.setDeltaMovement(0.0D, -0.8D, 0.0D);
        level.addFreshEntity(item);
        level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4F, 1.6F);
        for (int t = 1; t <= 80; t++) {
            final int tick = t;
            Scheduler.after(server, t, () -> {
                if (item.isRemoved()) return;
                if (tick % 2 == 0) {
                    level.sendParticles(ParticleTypes.END_ROD, item.getX(), item.getY() + 1.5D, item.getZ(), 3, 0.05D, 1.5D, 0.05D, 0.0D);
                }
                boolean landed = item.onGround() || item.getY() <= p.getY() + 0.3D || tick == 80;
                if (!landed) return;
                level.sendParticles(ParticleTypes.END_ROD, item.getX(), item.getY() + 0.3D, item.getZ(), 40, 0.3D, 0.3D, 0.3D, 0.15D);
                level.playSound(null, item.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2.0F, 0.8F);
                item.discard();
                if (!p.isRemoved() && isFighting(p)) {
                    giveSword(p);
                    ModNetwork.thought(p, "A sword. Out of the... sky?");
                }
            });
        }
    }

    /** Puts the sword in your hand, moving whatever was there into your bag (or onto the ground). */
    public static void giveSword(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof BorrowedSwordItem) return;
        }
        int slot = -1;
        if (inv.getItem(inv.selected).isEmpty()) {
            slot = inv.selected;
        } else {
            for (int i = 0; i < 9; i++) {
                if (inv.getItem(i).isEmpty()) {
                    slot = i;
                    break;
                }
            }
        }
        if (slot < 0) {
            ItemStack held = inv.getItem(inv.selected);
            inv.setItem(inv.selected, ItemStack.EMPTY);
            if (!inv.add(held)) p.drop(held, false);
            slot = inv.selected;
        }
        inv.setItem(slot, BorrowedSwordItem.create());
        inv.selected = slot;
        p.connection.send(new ClientboundSetCarriedItemPacket(slot));
        p.inventoryMenu.broadcastChanges();
    }

    /** It was only lent. */
    public static void takeSword(ServerPlayer p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof BorrowedSwordItem) inv.setItem(i, ItemStack.EMPTY);
        }
        if (p.containerMenu.getCarried().getItem() instanceof BorrowedSwordItem) p.containerMenu.setCarried(ItemStack.EMPTY);
        p.inventoryMenu.broadcastChanges();
    }

    /** You won. The dark goes, the sword goes back up, and then he comes down. */
    public static void defeated(ServerLevel level, @Nullable UUID owner) {
        ServerPlayer p = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (p == null || !isFighting(p)) return;
        Progress.setStage(p, Progress.FAREWELL);
        Progress.sync(p);
        ModNetwork.open(p, "boss:dead");
        MinecraftServer server = level.getServer();
        Scheduler.after(server, 50, () -> {
            if (p.isRemoved()) return;
            takeSword(p);
            p.serverLevel().sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0D, p.getZ(), 40, 0.2D, 2.5D, 0.2D, 0.05D);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.2F, 1.4F);
            ModNetwork.thought(p, "The sword's gone. Back up, I think. It was only lent.");
        });
        Scheduler.after(server, 110, () -> {
            if (!p.isRemoved()) Finale.farewell(p);
        });
    }

    /** The fight is off: you died, left his world, or logged out. He is back on the dock, waiting. */
    public static void reset(ServerPlayer p) {
        if (!isFighting(p)) return;
        takeSword(p);
        Finale.forget(p.getUUID());
        Progress.setStage(p, Progress.READY);
        Progress.sync(p);
    }
}
