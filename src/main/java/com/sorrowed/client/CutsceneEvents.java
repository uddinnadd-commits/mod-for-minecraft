package com.sorrowed.client;

import com.sorrowed.Sorrowed;
import net.minecraft.client.player.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** While a cutscene runs: lock the controls, hide the HUD and hand, and drive FOV and roll. */
@Mod.EventBusSubscriber(modid = Sorrowed.MODID, value = Dist.CLIENT)
public class CutsceneEvents {
    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) CutsceneClient.tick();
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) CutsceneClient.frame(event.renderTickTime);
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        if (!CutsceneClient.isActive()) return;
        Input in = event.getInput();
        in.up = false;
        in.down = false;
        in.left = false;
        in.right = false;
        in.jumping = false;
        in.shiftKeyDown = false;
        in.forwardImpulse = 0.0F;
        in.leftImpulse = 0.0F;
    }

    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        if (!CutsceneClient.isActive()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Pre event) {
        if (!CutsceneClient.isActive()) return;
        if (!event.getOverlay().id().getNamespace().equals(Sorrowed.MODID)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent event) {
        if (CutsceneClient.isActive()) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (CutsceneClient.isActive()) event.setFOV(CutsceneClient.fov());
    }

    @SubscribeEvent
    public static void onAngles(ViewportEvent.ComputeCameraAngles event) {
        if (CutsceneClient.isActive()) event.setRoll(CutsceneClient.roll());
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        CutsceneClient.abort();
    }
}
