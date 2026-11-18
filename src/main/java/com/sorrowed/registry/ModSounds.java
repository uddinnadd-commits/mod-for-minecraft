package com.sorrowed.registry;

import com.sorrowed.Sorrowed;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Sorrowed.MODID);

    /** The fight with the Memory Shatterer. Streamed and looped by the client while it lasts. */
    public static final RegistryObject<SoundEvent> BOSS_MUSIC = SOUNDS.register("music.memory_shatterer",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Sorrowed.MODID, "music.memory_shatterer")));
}
