package com.sorrowed.client;

import com.sorrowed.Sorrowed;
import com.sorrowed.SorrowedConfig;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Puts the mod's title screen in place of the vanilla one, unless turned off in the config. */
@Mod.EventBusSubscriber(modid = Sorrowed.MODID, value = Dist.CLIENT)
public class MenuEvents {
    @SubscribeEvent
    public static void onOpen(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof TitleScreen)) return;
        if (!SorrowedConfig.TITLE_SCREEN.get()) return;
        event.setNewScreen(new SorrowedTitleScreen());
    }
}
