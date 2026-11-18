package com.sorrowed.registry;

import com.sorrowed.Sorrowed;
import com.sorrowed.item.PapersItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Sorrowed.MODID);

    public static final RegistryObject<Item> RAIN_TEAR = ITEMS.register("rain_tear",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PAPERS = ITEMS.register("papers",
            () -> new PapersItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> HUSH_DOOR = ITEMS.register("hush_door",
            () -> new BlockItem(ModBlocks.HUSH_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> DROWNED_DOOR = ITEMS.register("drowned_door",
            () -> new BlockItem(ModBlocks.DROWNED_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> HALLWAY_DOOR = ITEMS.register("hallway_door",
            () -> new BlockItem(ModBlocks.HALLWAY_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> BACKUP_DOOR = ITEMS.register("backup_door",
            () -> new BlockItem(ModBlocks.BACKUP_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> ARCHIVE_DOOR = ITEMS.register("archive_door",
            () -> new BlockItem(ModBlocks.ARCHIVE_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> STREET_DOOR = ITEMS.register("street_door",
            () -> new BlockItem(ModBlocks.STREET_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> STATIC_DOOR = ITEMS.register("static_door",
            () -> new BlockItem(ModBlocks.STATIC_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> ORCHARD_DOOR = ITEMS.register("orchard_door",
            () -> new BlockItem(ModBlocks.ORCHARD_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> WARD_DOOR = ITEMS.register("ward_door",
            () -> new BlockItem(ModBlocks.WARD_DOOR.get(), new Item.Properties()));
    public static final RegistryObject<Item> GRAVE_MARKER = ITEMS.register("grave_marker",
            () -> new BlockItem(ModBlocks.GRAVE_MARKER.get(), new Item.Properties()));

    public static final RegistryObject<Item> ECHO_SPAWN_EGG = ITEMS.register("echo_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ECHO, 0xC9D6E0, 0x4A5668, new Item.Properties()));
    public static final RegistryObject<Item> HOLLOW_SPAWN_EGG = ITEMS.register("hollow_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.HOLLOW, 0x16161C, 0x6E7A8C, new Item.Properties()));
    public static final RegistryObject<Item> LISTENER_SPAWN_EGG = ITEMS.register("listener_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LISTENER, 0xAAAAB0, 0x202024, new Item.Properties()));
    public static final RegistryObject<Item> MASON_SPAWN_EGG = ITEMS.register("mason_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MASON, 0x6E4C2C, 0xC8A082, new Item.Properties()));
    public static final RegistryObject<Item> ARCHIVIST_SPAWN_EGG = ITEMS.register("archivist_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ARCHIVIST, 0x1E2A22, 0xD8CDB0, new Item.Properties()));
    public static final RegistryObject<Item> NEIGHBOUR_SPAWN_EGG = ITEMS.register("neighbour_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.NEIGHBOUR, 0x6B5A3E, 0x2A2A30, new Item.Properties()));
    public static final RegistryObject<Item> CARRIER_SPAWN_EGG = ITEMS.register("carrier_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.CARRIER, 0x3A3A3E, 0xDCDCDC, new Item.Properties()));
    public static final RegistryObject<Item> ORDERLY_SPAWN_EGG = ITEMS.register("orderly_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ORDERLY, 0xF0F0F0, 0x5A8A7A, new Item.Properties()));
    public static final RegistryObject<Item> SCARECROW_SPAWN_EGG = ITEMS.register("scarecrow_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SCARECROW, 0x8A6A3A, 0x2A2A2A, new Item.Properties()));
}
