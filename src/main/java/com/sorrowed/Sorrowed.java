package com.sorrowed;

import com.mojang.logging.LogUtils;
import com.sorrowed.net.ModNetwork;
import com.sorrowed.registry.ModBlockEntities;
import com.sorrowed.registry.ModBlocks;
import com.sorrowed.registry.ModChunkGenerators;
import com.sorrowed.registry.ModEntities;
import com.sorrowed.registry.ModFeatures;
import com.sorrowed.registry.ModItems;
import com.sorrowed.registry.ModSounds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Sorrowed.MODID)
public class Sorrowed {
    public static final String MODID = "sorrowed";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Sorrowed() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModBlockEntities.BLOCK_ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModSounds.SOUNDS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModFeatures.FEATURES.register(bus);
        ModChunkGenerators.GENERATORS.register(bus);
        bus.addListener(this::commonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SorrowedConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
