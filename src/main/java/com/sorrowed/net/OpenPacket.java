package com.sorrowed.net;

import com.sorrowed.client.ClientStory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Opens something on the player's screen: "chat", "console", "laptop", "ending", or "page:&lt;id&gt;". */
public class OpenPacket {
    private final String what;

    public OpenPacket(String what) {
        this.what = what;
    }

    public static void encode(OpenPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.what, 64);
    }

    public static OpenPacket decode(FriendlyByteBuf buf) {
        return new OpenPacket(buf.readUtf(64));
    }

    public static void handle(OpenPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientStory.open(msg.what)));
        ctx.get().setPacketHandled(true);
    }
}
