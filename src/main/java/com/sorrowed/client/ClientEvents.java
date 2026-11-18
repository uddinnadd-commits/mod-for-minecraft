package com.sorrowed.client;

import com.sorrowed.Sorrowed;
import com.sorrowed.SorrowedConfig;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
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
        ClientState.sorrow = 0;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ThoughtOverlay.tick();
            DistortionOverlay.tick();
            StaticOverlay.tick();
            AmbienceEvents.tick();
        }
    }
}
