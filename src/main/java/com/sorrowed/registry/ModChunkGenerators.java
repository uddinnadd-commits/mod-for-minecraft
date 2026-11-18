package com.sorrowed.registry;

import com.mojang.serialization.Codec;
import com.sorrowed.Sorrowed;
import com.sorrowed.worldgen.LiminalGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModChunkGenerators {
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Sorrowed.MODID);

    public static final RegistryObject<Codec<LiminalGenerator>> LIMINAL =
            GENERATORS.register("liminal", () -> LiminalGenerator.CODEC);
}
