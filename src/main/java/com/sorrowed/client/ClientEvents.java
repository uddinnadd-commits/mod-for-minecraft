package com.sorrowed.client;

import com.sorrowed.Sorrowed;
import com.sorrowed.SorrowedConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Sorrowed.MODID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (SorrowedConfig.PROGRAMMER_ART.get()) {
            Minecraft.getInstance().execute(ProgrammerArt::enable);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ThoughtOverlay.clear();
        DistortionOverlay.clear();
        StaticOverlay.clear();
        ScreenEffects.clear();
        ClientStory.clear();
        ClientState.sorrow = 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ThoughtOverlay.tick();
            DistortionOverlay.tick();
            StaticOverlay.tick();
            AmbienceEvents.tick();
            WorldParticles.tick();
            Minecraft mc = Minecraft.getInstance();
            while (KeyBindings.JOURNAL.consumeClick()) {
                if (mc.screen == null && mc.player != null && !CutsceneClient.isActive()) JournalScreen.open();
            }
        }
    }

    /** The journal, from the pause menu as well. */
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        event.addListener(Button.builder(Component.literal("Journal"), b -> JournalScreen.open())
                .bounds(8, 8, 74, 20).build());
    }
}
