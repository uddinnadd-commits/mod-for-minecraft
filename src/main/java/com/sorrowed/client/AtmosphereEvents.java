package com.sorrowed.client;

import com.sorrowed.Sorrowed;
import com.sorrowed.registry.Dimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Per-dimension fog: the Hush closes in, the Hallway hazes yellow, the Drowned Hall is murk, the Backup is bright. */
@Mod.EventBusSubscriber(modid = Sorrowed.MODID, value = Dist.CLIENT)
public class AtmosphereEvents {
    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ResourceKey<Level> dim = mc.level.dimension();
        FogType type = event.getType();

        if (dim.equals(Dimensions.HUSH) && type == FogType.NONE) {
            event.setNearPlaneDistance(0.0F);
            event.setFarPlaneDistance(40.0F);
            event.setCanceled(true);
        } else if (dim.equals(Dimensions.HALLWAY) && type == FogType.NONE) {
            event.setNearPlaneDistance(16.0F);
            event.setFarPlaneDistance(72.0F);
            event.setCanceled(true);
        } else if (dim.equals(Dimensions.DROWNED)) {
            if (type == FogType.NONE) {
                event.setNearPlaneDistance(0.0F);
                event.setFarPlaneDistance(110.0F);
                event.setCanceled(true);
            } else if (type == FogType.WATER) {
                event.setNearPlaneDistance(0.0F);
                event.setFarPlaneDistance(28.0F);
                event.setCanceled(true);
            }
        } else if (dim.equals(Dimensions.ARCHIVE) && type == FogType.NONE) {
            event.setNearPlaneDistance(6.0F);
            event.setFarPlaneDistance(48.0F);
            event.setCanceled(true);
        } else if (dim.equals(Dimensions.STREET) && type == FogType.NONE) {
            event.setNearPlaneDistance(12.0F);
            event.setFarPlaneDistance(90.0F);
            event.setCanceled(true);
        } else if (dim.equals(Dimensions.ORCHARD) && type == FogType.NONE) {
            event.setNearPlaneDistance(8.0F);
            event.setFarPlaneDistance(44.0F);
            event.setCanceled(true);
        } else if (dim.equals(Dimensions.SERVER) && type == FogType.NONE) {
            event.setNearPlaneDistance(4.0F);
            event.setFarPlaneDistance(40.0F);
            event.setCanceled(true);
        } else if (dim.equals(Dimensions.WARD) && type == FogType.NONE) {
            event.setNearPlaneDistance(10.0F);
            event.setFarPlaneDistance(56.0F);
            event.setCanceled(true);
        } else if (dim.equals(Dimensions.STATIC) && type == FogType.NONE) {
            // The static breathes: the fog edge crawls in and out a little every second.
            float t = (mc.level.getGameTime() + (float) event.getPartialTick()) * 0.35F;
            float wobble = (float) Math.sin(t) * 6.0F;
            event.setNearPlaneDistance(0.0F);
            event.setFarPlaneDistance(34.0F + wobble);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ResourceKey<Level> dim = mc.level.dimension();
        if (dim.equals(Dimensions.HUSH)) {
            set(event, 0.03F, 0.05F, 0.07F);
        } else if (dim.equals(Dimensions.HALLWAY)) {
            set(event, 0.52F, 0.46F, 0.16F);
        } else if (dim.equals(Dimensions.DROWNED)) {
            set(event, 0.02F, 0.18F, 0.20F);
        } else if (dim.equals(Dimensions.BACKUP)) {
            if (ClientStory.stage != ClientStory.PLAYING) {
                // The day after: the light has gone gold.
                set(event, 1.0F, 0.84F, 0.62F);
            } else {
                set(event, 1.0F, 0.96F, 0.85F);
            }
        } else if (dim.equals(Dimensions.ARCHIVE)) {
            set(event, 0.16F, 0.12F, 0.07F);
        } else if (dim.equals(Dimensions.STREET)) {
            set(event, 0.05F, 0.05F, 0.09F);
        } else if (dim.equals(Dimensions.ORCHARD)) {
            set(event, 0.56F, 0.57F, 0.55F);
        } else if (dim.equals(Dimensions.WARD)) {
            set(event, 0.80F, 0.86F, 0.82F);
        } else if (dim.equals(Dimensions.SERVER)) {
            set(event, 0.02F, 0.04F, 0.06F);
        } else if (dim.equals(Dimensions.STATIC)) {
            // Flicker between greys, never quite the same twice.
            long t = mc.level.getGameTime();
            float g = 0.62F + 0.06F * (float) Math.sin(t * 0.9D) + 0.04F * (float) Math.sin(t * 2.3D);
            set(event, g, g, g);
        }
    }

    private static void set(ViewportEvent.ComputeFogColor event, float r, float g, float b) {
        event.setRed(r);
        event.setGreen(g);
        event.setBlue(b);
    }
}
