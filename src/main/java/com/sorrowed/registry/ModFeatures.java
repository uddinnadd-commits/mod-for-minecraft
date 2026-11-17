package com.sorrowed.registry;

import com.sorrowed.Sorrowed;
import com.sorrowed.worldgen.BusStopFeature;
import com.sorrowed.worldgen.GateFeature;
import com.sorrowed.worldgen.GraveFeature;
import com.sorrowed.worldgen.HouseFeature;
import com.sorrowed.worldgen.LighthouseFeature;
import com.sorrowed.worldgen.StandingStonesFeature;
import com.sorrowed.worldgen.VigilRingFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, Sorrowed.MODID);

    public static final RegistryObject<Feature<?>> GATE = FEATURES.register("gate", GateFeature::new);
    public static final RegistryObject<Feature<?>> HOUSE = FEATURES.register("house", HouseFeature::new);
    public static final RegistryObject<Feature<?>> GRAVE = FEATURES.register("grave", GraveFeature::new);
    public static final RegistryObject<Feature<?>> LIGHTHOUSE = FEATURES.register("lighthouse", LighthouseFeature::new);
    public static final RegistryObject<Feature<?>> VIGIL_RING = FEATURES.register("vigil_ring", VigilRingFeature::new);
    public static final RegistryObject<Feature<?>> BUS_STOP = FEATURES.register("bus_stop", BusStopFeature::new);
    public static final RegistryObject<Feature<?>> STANDING_STONES = FEATURES.register("standing_stones", StandingStonesFeature::new);
}
