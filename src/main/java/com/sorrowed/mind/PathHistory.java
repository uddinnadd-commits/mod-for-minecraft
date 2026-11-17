package com.sorrowed.mind;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Where each player has been, one sample per tick, for the last 20 seconds. The Reflection walks this path. */
public final class PathHistory {
    private PathHistory() {}

    public record Sample(double x, double y, double z, float yaw) {}

    private static final int SIZE = 400;

    private static final class Ring {
        final Sample[] data = new Sample[SIZE];
        int head;
        int count;
    }

    private static final Map<UUID, Ring> MAP = new HashMap<>();

    public static void record(ServerPlayer p) {
        Ring r = MAP.computeIfAbsent(p.getUUID(), k -> new Ring());
        r.data[r.head] = new Sample(p.getX(), p.getY(), p.getZ(), p.getYRot());
        r.head = (r.head + 1) % SIZE;
        if (r.count < SIZE) r.count++;
    }

    /** Where the player was 'ticksAgo' ticks ago, or null if we don't have that much history. */
    public static Sample get(UUID id, int ticksAgo) {
        Ring r = MAP.get(id);
        if (r == null || ticksAgo >= r.count) return null;
        return r.data[Math.floorMod(r.head - 1 - ticksAgo, SIZE)];
    }

    public static void clear(UUID id) {
        MAP.remove(id);
    }
}
