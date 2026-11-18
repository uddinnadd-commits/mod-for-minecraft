package com.sorrowed.net;

import com.sorrowed.client.CutsceneClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> client: start cutscene 'id'. If anchored, the camera is relative to (x,y,z) instead of the player. */
public class CutscenePacket {
    private final String id;
    private final boolean anchored;
    private final double x;
    private final double y;
    private final double z;

    public CutscenePacket(String id, boolean anchored, double x, double y, double z) {
        this.id = id;
        this.anchored = anchored;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(CutscenePacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.id, 64);
        buf.writeBoolean(m.anchored);
        buf.writeDouble(m.x);
        buf.writeDouble(m.y);
        buf.writeDouble(m.z);
    }

    public static CutscenePacket decode(FriendlyByteBuf buf) {
        return new CutscenePacket(buf.readUtf(64), buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(CutscenePacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                        CutsceneClient.start(m.id, m.anchored, m.x, m.y, m.z)));
        ctx.get().setPacketHandled(true);
    }
}
