package com.sorrowed.net;

import com.sorrowed.client.DistortionOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: pull the picture apart (in ticks), hold it dark, then let it settle. */
public class DistortPacket {
    private final int in;
    private final int hold;
    private final int out;

    public DistortPacket(int in, int hold, int out) {
        this.in = in;
        this.hold = hold;
        this.out = out;
    }

    public static void encode(DistortPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.in);
        buf.writeVarInt(msg.hold);
        buf.writeVarInt(msg.out);
    }

    public static DistortPacket decode(FriendlyByteBuf buf) {
        return new DistortPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(DistortPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DistortionOverlay.start(msg.in, msg.hold, msg.out)));
        ctx.get().setPacketHandled(true);
    }
}
