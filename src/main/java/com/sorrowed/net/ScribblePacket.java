package com.sorrowed.net;

import com.sorrowed.client.ScribbleOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: scribble over the screen (in ticks), hold it black, then scratch it away. */
public class ScribblePacket {
    private final int in;
    private final int hold;
    private final int out;

    public ScribblePacket(int in, int hold, int out) {
        this.in = in;
        this.hold = hold;
        this.out = out;
    }

    public static void encode(ScribblePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.in);
        buf.writeVarInt(msg.hold);
        buf.writeVarInt(msg.out);
    }

    public static ScribblePacket decode(FriendlyByteBuf buf) {
        return new ScribblePacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(ScribblePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ScribbleOverlay.start(msg.in, msg.hold, msg.out)));
        ctx.get().setPacketHandled(true);
    }
}
