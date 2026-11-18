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

    /** Alex's world, the day before. Perfect, bright, and nothing in it moves. The hub. */
    public static final ResourceKey<Level> BACKUP = key("the_backup");
    /** The midnight forest that nobody built. */
    public static final ResourceKey<Level> HUSH = key("the_hush");
    /** A flooded world. His house is still down there, and it still has air in it. */
    public static final ResourceKey<Level> DROWNED = key("the_drowned");
    /** An endless carpeted maze of rooms that all feel like somewhere you've lived. */
    public static final ResourceKey<Level> HALLWAY = key("the_hallway");
    /** An endless library of everything he never said. The pages are blank. */
    public static final ResourceKey<Level> ARCHIVE = key("the_archive");
    /** The road he grew up on, at night, in the rain. One window is lit. */
    public static final ResourceKey<Level> STREET = key("the_street");
    /** A plain of black and white noise. A channel nobody is broadcasting on. */
    public static final ResourceKey<Level> STATIC = key("the_static");
    /** His grandfather's orchard, dead, in grey fog. The scarecrow is looking at you. */
    public static final ResourceKey<Level> ORCHARD = key("the_orchard");
    /** The hospital. One corridor, the lamps half out, and an empty bed being pushed past. */
    public static final ResourceKey<Level> WARD = key("the_ward");
    /** Where his world was kept. The racks are still running. Something in them is still running. */
    public static final ResourceKey<Level> SERVER = key("the_server");
}
