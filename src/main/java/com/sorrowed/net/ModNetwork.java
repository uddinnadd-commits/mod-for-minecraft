package com.sorrowed.net;

import com.sorrowed.Sorrowed;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL = "3";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Sorrowed.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, ThoughtPacket.class,
                ThoughtPacket::encode, ThoughtPacket::decode, ThoughtPacket::handle);
        CHANNEL.registerMessage(1, SorrowPacket.class,
                SorrowPacket::encode, SorrowPacket::decode, SorrowPacket::handle);
        CHANNEL.registerMessage(2, CutscenePacket.class,
                CutscenePacket::encode, CutscenePacket::decode, CutscenePacket::handle);
    }

    public static void thought(ServerPlayer player, String text) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ThoughtPacket(text));
    }

    public static void sorrow(ServerPlayer player, int value) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SorrowPacket(value));
    }

    public static void cutscene(ServerPlayer player, String id, Vec3 anchor) {
        CutscenePacket packet = anchor == null
                ? new CutscenePacket(id, false, 0, 0, 0)
                : new CutscenePacket(id, true, anchor.x, anchor.y, anchor.z);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
