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

    // Overworld <-> Alex's world (the Backup). Every other doorway returns there.
    public static final RegistryObject<Block> BACKUP_DOOR = BLOCKS.register("backup_door",
            () -> new DoorwayBlock(Dimensions.BACKUP, Level.OVERWORLD));
    public static final RegistryObject<Block> HUSH_DOOR = BLOCKS.register("hush_door",
            () -> new DoorwayBlock(Dimensions.HUSH, Dimensions.BACKUP));
    public static final RegistryObject<Block> DROWNED_DOOR = BLOCKS.register("drowned_door",
            () -> new DoorwayBlock(Dimensions.DROWNED, Dimensions.BACKUP));
    public static final RegistryObject<Block> HALLWAY_DOOR = BLOCKS.register("hallway_door",
            () -> new DoorwayBlock(Dimensions.HALLWAY, Dimensions.BACKUP));
    public static final RegistryObject<Block> ARCHIVE_DOOR = BLOCKS.register("archive_door",
            () -> new DoorwayBlock(Dimensions.ARCHIVE, Dimensions.BACKUP));
    public static final RegistryObject<Block> STREET_DOOR = BLOCKS.register("street_door",
            () -> new DoorwayBlock(Dimensions.STREET, Dimensions.BACKUP));
    public static final RegistryObject<Block> STATIC_DOOR = BLOCKS.register("static_door",
            () -> new DoorwayBlock(Dimensions.STATIC, Dimensions.BACKUP));
    public static final RegistryObject<Block> ORCHARD_DOOR = BLOCKS.register("orchard_door",
            () -> new DoorwayBlock(Dimensions.ORCHARD, Dimensions.BACKUP));
    public static final RegistryObject<Block> WARD_DOOR = BLOCKS.register("ward_door",
            () -> new DoorwayBlock(Dimensions.WARD, Dimensions.BACKUP));
    public static final RegistryObject<Block> SERVER_DOOR = BLOCKS.register("server_door",
            () -> new DoorwayBlock(Dimensions.SERVER, Dimensions.BACKUP));

    public static final RegistryObject<Block> GRAVE_MARKER = BLOCKS.register("grave_marker",
            GraveMarkerBlock::new);
}
