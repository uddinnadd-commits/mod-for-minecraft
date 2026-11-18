package com.sorrowed.net;

import com.sorrowed.Sorrowed;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class ModNetwork {
    private static final String PROTOCOL = "6";

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
        CHANNEL.registerMessage(3, DistortPacket.class,
                DistortPacket::encode, DistortPacket::decode, DistortPacket::handle);
        CHANNEL.registerMessage(4, StaticPacket.class,
                StaticPacket::encode, StaticPacket::decode, StaticPacket::handle);
        CHANNEL.registerMessage(5, ObjectivesPacket.class,
                ObjectivesPacket::encode, ObjectivesPacket::decode, ObjectivesPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(6, OpenPacket.class,
                OpenPacket::encode, OpenPacket::decode, OpenPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(7, ReplyPacket.class,
                ReplyPacket::encode, ReplyPacket::decode, ReplyPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void objectives(ServerPlayer player, ObjectivesPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void open(ServerPlayer player, String what) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenPacket(what));
    }

    public static void ending(ServerPlayer player) {
        open(player, "ending");
    }

    /** Client side: send what was typed into one of the screens. */
    public static void reply(String what, String text) {
        CHANNEL.sendToServer(new ReplyPacket(what, text));
    }

    public static void distort(ServerPlayer player, int in, int hold, int out) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new DistortPacket(in, hold, out));
    }

    public static void statik(ServerPlayer player, float strength) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new StaticPacket(strength));
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
