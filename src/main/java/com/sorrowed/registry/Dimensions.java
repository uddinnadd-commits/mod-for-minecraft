package com.sorrowed.registry;

import com.sorrowed.Sorrowed;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class Dimensions {
    private static ResourceKey<Level> key(String name) {
        return ResourceKey.create(Registries.DIMENSION, new ResourceLocation(Sorrowed.MODID, name));
    }

    /** Long floating chunks: ribbons, monoliths and slabs hanging in a pale sky. The hub. */
    public static final ResourceKey<Level> UNFINISHED = key("the_unfinished");
    /** The midnight forest that nobody built. */
    public static final ResourceKey<Level> HUSH = key("the_hush");
    /** A flooded world. His house is still down there, and it still has air in it. */
    public static final ResourceKey<Level> DROWNED = key("the_drowned");
    /** An endless carpeted maze of rooms that all feel like somewhere you've lived. */
    public static final ResourceKey<Level> HALLWAY = key("the_hallway");
    /** The world exactly as it was. Perfect, bright, and nothing in it moves. */
    public static final ResourceKey<Level> BACKUP = key("the_backup");
    /** An endless library of everything he never said. The pages are blank. */
    public static final ResourceKey<Level> ARCHIVE = key("the_archive");
    /** The road he grew up on, at night, in the rain. One window is lit. */
    public static final ResourceKey<Level> STREET = key("the_street");
    /** A plain of black and white noise. A channel nobody is broadcasting on. */
    public static final ResourceKey<Level> STATIC = key("the_static");
}
