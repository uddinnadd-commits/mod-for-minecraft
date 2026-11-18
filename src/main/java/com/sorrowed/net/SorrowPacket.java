package com.sorrowed.net;

import com.sorrowed.client.ClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: the player's current Sorrow (0-100), drives the vignette and text glitching. */
public class SorrowPacket {
    private final int value;

    public SorrowPacket(int value) {
        this.value = value;
    }

    public static void encode(SorrowPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.value);
    }

    public static SorrowPacket decode(FriendlyByteBuf buf) {
        return new SorrowPacket(buf.readVarInt());
    }

    public static void handle(SorrowPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientState.sorrow = msg.value));
        ctx.get().setPacketHandled(true);
    }
}
