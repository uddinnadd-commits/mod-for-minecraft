package com.sorrowed.registry;

import com.sorrowed.Sorrowed;
import com.sorrowed.block.DoorwayBlock;
import com.sorrowed.block.GraveMarkerBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Sorrowed.MODID);

    // Overworld <-> Unfinished hub. Every other doorway returns to the hub.
    public static final RegistryObject<Block> UNFINISHED_DOOR = BLOCKS.register("unfinished_door",
            () -> new DoorwayBlock(Dimensions.UNFINISHED, Level.OVERWORLD));
    public static final RegistryObject<Block> HUSH_DOOR = BLOCKS.register("hush_door",
            () -> new DoorwayBlock(Dimensions.HUSH, Dimensions.UNFINISHED));
    public static final RegistryObject<Block> DROWNED_DOOR = BLOCKS.register("drowned_door",
            () -> new DoorwayBlock(Dimensions.DROWNED, Dimensions.UNFINISHED));
    public static final RegistryObject<Block> HALLWAY_DOOR = BLOCKS.register("hallway_door",
            () -> new DoorwayBlock(Dimensions.HALLWAY, Dimensions.UNFINISHED));
    public static final RegistryObject<Block> BACKUP_DOOR = BLOCKS.register("backup_door",
            () -> new DoorwayBlock(Dimensions.BACKUP, Dimensions.UNFINISHED));
    public static final RegistryObject<Block> ARCHIVE_DOOR = BLOCKS.register("archive_door",
            () -> new DoorwayBlock(Dimensions.ARCHIVE, Dimensions.UNFINISHED));
    public static final RegistryObject<Block> STREET_DOOR = BLOCKS.register("street_door",
            () -> new DoorwayBlock(Dimensions.STREET, Dimensions.UNFINISHED));
    public static final RegistryObject<Block> STATIC_DOOR = BLOCKS.register("static_door",
            () -> new DoorwayBlock(Dimensions.STATIC, Dimensions.UNFINISHED));

    public static final RegistryObject<Block> GRAVE_MARKER = BLOCKS.register("grave_marker",
            GraveMarkerBlock::new);
}
