package com.sorrowed;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** World-wide bookkeeping: has the grave been placed, where are the hub gates. */
public class SorrowedData extends SavedData {
    private static final String NAME = "sorrowed";

    public boolean gravePlaced;
    private CompoundTag gates = new CompoundTag();

    public static SorrowedData load(CompoundTag tag) {
        SorrowedData data = new SorrowedData();
        data.gravePlaced = tag.getBoolean("gravePlaced");
        data.gates = tag.getCompound("gates");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("gravePlaced", gravePlaced);
        tag.put("gates", gates);
        return tag;
    }

    public static SorrowedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(SorrowedData::load, SorrowedData::new, NAME);
    }

    public Integer getY(String key) {
        return gates.contains(key, Tag.TAG_INT) ? Integer.valueOf(gates.getInt(key)) : null;
    }

    public void setY(String key, int y) {
        gates.putInt(key, y);
        setDirty();
    }

    public boolean isBuilt(String key) {
        return gates.getBoolean("built_" + key);
    }

    public void setBuilt(String key) {
        gates.putBoolean("built_" + key, true);
        setDirty();
    }
}
