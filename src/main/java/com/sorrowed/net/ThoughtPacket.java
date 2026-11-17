package com.sorrowed.net;

import com.sorrowed.client.ThoughtOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: show this line in the typewriter overlay. */
public class ThoughtPacket {
    private final String text;

    public ThoughtPacket(String text) {
        this.text = text;
    }

    public static void encode(ThoughtPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.text, 400);
    }

    public static ThoughtPacket decode(FriendlyByteBuf buf) {
        return new ThoughtPacket(buf.readUtf(400));
    }

    public static void handle(ThoughtPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ThoughtOverlay.push(msg.text)));
        ctx.get().setPacketHandled(true);
    }
}
