package com.sorrowed.net;

import com.sorrowed.client.StaticOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: television static over the view, 0..1 strong. Decays on its own. */
public class StaticPacket {
    private final float strength;

    public StaticPacket(float strength) {
        this.strength = strength;
    }

    public static void encode(StaticPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.strength);
    }

    public static StaticPacket decode(FriendlyByteBuf buf) {
        return new StaticPacket(buf.readFloat());
    }

    public static void handle(StaticPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> StaticOverlay.push(msg.strength)));
        ctx.get().setPacketHandled(true);
    }
}
