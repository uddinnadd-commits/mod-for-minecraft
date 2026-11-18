package com.sorrowed.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sorrowed.Sorrowed;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** J opens the journal. */
@Mod.EventBusSubscriber(modid = Sorrowed.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyBindings {
    public static final KeyMapping JOURNAL = new KeyMapping("key.sorrowed.journal", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories.sorrowed");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(JOURNAL);
    }
}
