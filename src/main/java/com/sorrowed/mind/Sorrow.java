package com.sorrowed.mind;

import com.sorrowed.net.ModNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sorrow: a 0-100 meter stored on the player. Rain, the dark and the wrong dimensions raise it;
 * graves, shelter and the Mason lower it. It decides which hallucinations and events can happen.
 */
public final class Sorrow {
    private Sorrow() {}

    private static final String PERSIST = "PlayerPersisted";
    private static final String KEY = "sorrowed_sorrow";
    private static final Map<UUID, Integer> LAST_SYNCED = new HashMap<>();

    public static float get(ServerPlayer p) {
        return p.getPersistentData().getCompound(PERSIST).getFloat(KEY);
    }

    public static void set(ServerPlayer p, float value) {
        float v = Math.max(0.0F, Math.min(100.0F, value));
        CompoundTag root = p.getPersistentData();
        CompoundTag saved = root.getCompound(PERSIST);
        saved.putFloat(KEY, v);
        root.put(PERSIST, saved);

        int rounded = Math.round(v);
        Integer last = LAST_SYNCED.get(p.getUUID());
        if (last == null || last != rounded) {
            LAST_SYNCED.put(p.getUUID(), rounded);
            ModNetwork.sorrow(p, rounded);
        }
    }

    public static void add(ServerPlayer p, float delta) {
        set(p, get(p) + delta);
    }

    /** 0 calm, 1 uneasy, 2 haunted, 3 breaking. */
    public static int tier(ServerPlayer p) {
        float v = get(p);
        if (v >= 80.0F) return 3;
        if (v >= 55.0F) return 2;
        if (v >= 30.0F) return 1;
        return 0;
    }

    public static void resync(ServerPlayer p) {
        LAST_SYNCED.remove(p.getUUID());
        set(p, get(p));
    }

    public static void forget(UUID id) {
        LAST_SYNCED.remove(id);
    }
}
