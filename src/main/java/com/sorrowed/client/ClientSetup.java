package com.sorrowed.client;

import com.sorrowed.Sorrowed;
import com.sorrowed.registry.ModBlockEntities;
import com.sorrowed.registry.ModEntities;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Sorrowed.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    private static ResourceLocation tex(String name) {
        return new ResourceLocation(Sorrowed.MODID, "textures/entity/" + name + ".png");
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModels.ECHO, ModModels::echoLayer);
        event.registerLayerDefinition(ModModels.HOLLOW, ModModels::hollowLayer);
        event.registerLayerDefinition(ModModels.LISTENER, ModModels::listenerLayer);
        event.registerLayerDefinition(ModModels.MASON, ModModels::masonLayer);
        event.registerLayerDefinition(ModModels.ARCHIVIST, ModModels::archivistLayer);
        event.registerLayerDefinition(ModModels.NEIGHBOUR, ModModels::neighbourLayer);
        event.registerLayerDefinition(ModModels.CARRIER, ModModels::carrierLayer);
        event.registerLayerDefinition(ModModels.ORDERLY, ModModels::orderlyLayer);
        event.registerLayerDefinition(ModModels.SCARECROW, ModModels::scarecrowLayer);
        event.registerLayerDefinition(ModModels.COPY, ModModels::copyLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ECHO.get(),
                ctx -> new SorrowMobRenderer<>(ctx, ModModels.ECHO, tex("echo"), 0.4F, ModModels.ECHO_STYLE, true));
        event.registerEntityRenderer(ModEntities.HOLLOW.get(),
                ctx -> new SorrowMobRenderer<>(ctx, ModModels.HOLLOW, tex("hollow"), 0.5F, ModModels.HOLLOW_STYLE, false));
        event.registerEntityRenderer(ModEntities.LISTENER.get(),
                ctx -> new SorrowMobRenderer<>(ctx, ModModels.LISTENER, tex("listener"), 0.5F, ModModels.LISTENER_STYLE, false));
        event.registerEntityRenderer(ModEntities.MASON.get(),
                ctx -> new SorrowMobRenderer<>(ctx, ModModels.MASON, tex("mason"), 0.5F, ModModels.MASON_STYLE, false));
        event.registerEntityRenderer(ModEntities.ARCHIVIST.get(),
                ctx -> new SorrowMobRenderer<>(ctx, ModModels.ARCHIVIST, tex("archivist"), 0.4F, ModModels.ARCHIVIST_STYLE, false));
        event.registerEntityRenderer(ModEntities.NEIGHBOUR.get(),
                ctx -> new SorrowMobRenderer<>(ctx, ModModels.NEIGHBOUR, tex("neighbour"), 0.5F, ModModels.NEIGHBOUR_STYLE, false));
        event.registerEntityRenderer(ModEntities.CARRIER.get(),
                ctx -> new SorrowMobRenderer<>(ctx, ModModels.CARRIER, tex("carrier"), 0.5F, ModModels.CARRIER_STYLE, false));
        event.registerEntityRenderer(ModEntities.ORDERLY.get(),
                ctx -> new SorrowMobRenderer<>(ctx, ModModels.ORDERLY, tex("orderly"), 0.6F, ModModels.ORDERLY_STYLE, false));
        event.registerEntityRenderer(ModEntities.SCARECROW.get(),
                ctx -> new SorrowMobRenderer<>(ctx, ModModels.SCARECROW, tex("scarecrow"), 0.4F, ModModels.SCARECROW_STYLE, false));
        event.registerEntityRenderer(ModEntities.COPY.get(),
                ctx -> new SorrowMobRenderer<>(ctx, ModModels.COPY, tex("copy"), 0.4F, ModModels.COPY_STYLE, false));
        event.registerEntityRenderer(ModEntities.REFLECTION.get(), ReflectionRenderer::new);
        event.registerEntityRenderer(ModEntities.FRIEND.get(), FriendRenderer::new);
        event.registerEntityRenderer(ModEntities.MEMORY_SHATTERER.get(), ShattererRenderer::new);
        event.registerEntityRenderer(ModEntities.MEMORY_SHARD.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.4F, true));
        event.registerBlockEntityRenderer(ModBlockEntities.LIGHTHOUSE_LAMP.get(), LighthouseBeamRenderer::new);
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent event) {
        // The look of each world sits with the vanilla vignette, under the hotbar.
        event.registerAbove(VanillaGuiOverlay.VIGNETTE.id(), "screen_effects", (gui, graphics, partialTick, width, height) ->
                ScreenEffects.render(graphics, width, height));
        // Order matters: static, the objective, then letterbox/fades, then the typewriter text, then the distortion over all of it.
        event.registerAboveAll("static", (gui, graphics, partialTick, width, height) ->
                StaticOverlay.render(graphics, width, height));
        event.registerAboveAll("objective", (gui, graphics, partialTick, width, height) ->
                ObjectiveHud.render(graphics, width, height));
        event.registerAboveAll("boss", (gui, graphics, partialTick, width, height) ->
                BossClient.render(graphics, width, height));
        event.registerAboveAll("cutscene", (gui, graphics, partialTick, width, height) ->
                CutsceneOverlay.render(graphics, width, height));
        event.registerAboveAll("thoughts", (gui, graphics, partialTick, width, height) ->
                ThoughtOverlay.render(graphics, width, height));
        event.registerAboveAll("distortion", (gui, graphics, partialTick, width, height) ->
                DistortionOverlay.render(graphics, width, height));
    }

    @SubscribeEvent
    public static void dimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(new ResourceLocation(Sorrowed.MODID, "hush"), new SorrowEffects(true));
        event.register(new ResourceLocation(Sorrowed.MODID, "hallway"), new SorrowEffects(false));
        event.register(new ResourceLocation(Sorrowed.MODID, "archive"), new SorrowEffects(false));
        event.register(new ResourceLocation(Sorrowed.MODID, "static"), new SorrowEffects(true));
        event.register(new ResourceLocation(Sorrowed.MODID, "ward"), new SorrowEffects(false));
        event.register(new ResourceLocation(Sorrowed.MODID, "server"), new SorrowEffects(false));
    }
}
