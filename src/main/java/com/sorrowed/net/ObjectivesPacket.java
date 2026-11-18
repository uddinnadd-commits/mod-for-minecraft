package com.sorrowed.net;

import com.sorrowed.client.ClientStory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The player's whole progress: stage, every objective (done, parts), what they typed back to him,
 * where to walk in the world they are in, and which objective (if any) was finished just now.
 * Objectives are sent in {@link com.sorrowed.story.Objectives#ALL} order.
 */
public class ObjectivesPacket {
    public final int stage;
    public final boolean[] done;
    public final int[] steps;
    public final String reply;
    public final List<BlockPos> targets;
    public final String justDone;

    public ObjectivesPacket(int stage, boolean[] done, int[] steps, String reply, List<BlockPos> targets, String justDone) {
        this.stage = stage;
        this.done = done;
        this.steps = steps;
        this.reply = reply;
        this.targets = targets;
        this.justDone = justDone;
    }

    public static void encode(ObjectivesPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.stage);
        buf.writeVarInt(msg.done.length);
        for (int i = 0; i < msg.done.length; i++) {
            buf.writeBoolean(msg.done[i]);
            buf.writeVarInt(msg.steps[i]);
        }
        buf.writeUtf(msg.reply, 256);
        buf.writeVarInt(msg.targets.size());
        for (BlockPos p : msg.targets) buf.writeBlockPos(p);
        buf.writeUtf(msg.justDone, 64);
    }

    public static ObjectivesPacket decode(FriendlyByteBuf buf) {
        int stage = buf.readByte();
        int n = buf.readVarInt();
        boolean[] done = new boolean[n];
        int[] steps = new int[n];
        for (int i = 0; i < n; i++) {
            done[i] = buf.readBoolean();
            steps[i] = buf.readVarInt();
        }
        String reply = buf.readUtf(256);
        int t = buf.readVarInt();
        List<BlockPos> targets = new ArrayList<>(t);
        for (int i = 0; i < t; i++) targets.add(buf.readBlockPos());
        String justDone = buf.readUtf(64);
        return new ObjectivesPacket(stage, done, steps, reply, targets, justDone);
    }

    public static void handle(ObjectivesPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientStory.update(msg)));
        ctx.get().setPacketHandled(true);
    }
}
