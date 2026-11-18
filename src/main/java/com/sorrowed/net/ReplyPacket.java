package com.sorrowed.net;

import com.sorrowed.event.StoryEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** From a screen back to the server: what the player typed to him, or that they watched it through. */
public class ReplyPacket {
    private final String what;
    private final String text;

    public ReplyPacket(String what, String text) {
        this.what = what;
        this.text = text;
    }

    public static void encode(ReplyPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.what, 32);
        buf.writeUtf(msg.text, 256);
    }

    public static ReplyPacket decode(FriendlyByteBuf buf) {
        return new ReplyPacket(buf.readUtf(32), buf.readUtf(256));
    }

    public static void handle(ReplyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        ctx.get().enqueueWork(() -> {
            if (sender != null) StoryEvents.reply(sender, msg.what, msg.text);
        });
        ctx.get().setPacketHandled(true);
    }
}
