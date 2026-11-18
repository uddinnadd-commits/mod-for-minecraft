package com.sorrowed;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** World-wide bookkeeping: has the grave been placed, where are the hub gates, has the story ended. */
public class SorrowedData extends SavedData {
    private static final String NAME = "sorrowed";

    public boolean gravePlaced;
    /** Somebody finished it. The rain is allowed to stop. */
    public boolean ended;
    private CompoundTag gates = new CompoundTag();
    private CompoundTag places = new CompoundTag();

    public static SorrowedData load(CompoundTag tag) {
        SorrowedData data = new SorrowedData();
        data.gravePlaced = tag.getBoolean("gravePlaced");
        data.ended = tag.getBoolean("ended");
        data.gates = tag.getCompound("gates");
        data.places = tag.getCompound("places");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("gravePlaced", gravePlaced);
        tag.putBoolean("ended", ended);
        tag.put("gates", gates);
        tag.put("places", places);
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

    /** A remembered block position (the grave, the thing to find in each world), or null. */
    public BlockPos getPos(String key) {
        return places.contains(key, Tag.TAG_LONG) ? BlockPos.of(places.getLong(key)) : null;
    }

    public void setPos(String key, BlockPos pos) {
        places.putLong(key, pos.asLong());
        setDirty();
    }

    public void setEnded() {
        if (!ended) {
            ended = true;
            setDirty();
        }
    }
}
