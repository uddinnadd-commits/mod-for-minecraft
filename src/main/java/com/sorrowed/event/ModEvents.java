package com.sorrowed.event;

import com.sorrowed.Sorrowed;
import com.sorrowed.entity.ArchivistEntity;
import com.sorrowed.entity.CarrierEntity;
import com.sorrowed.entity.CopyEntity;
import com.sorrowed.entity.EchoEntity;
import com.sorrowed.entity.HollowEntity;
import com.sorrowed.entity.ListenerEntity;
import com.sorrowed.entity.MasonEntity;
import com.sorrowed.entity.NeighbourEntity;
import com.sorrowed.entity.OrderlyEntity;
import com.sorrowed.entity.ScarecrowEntity;
import com.sorrowed.entity.ReflectionEntity;
import com.sorrowed.registry.ModEntities;
import com.sorrowed.registry.ModItems;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Sorrowed.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {
    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ECHO.get(), EchoEntity.createAttributes().build());
        event.put(ModEntities.HOLLOW.get(), HollowEntity.createAttributes().build());
        event.put(ModEntities.LISTENER.get(), ListenerEntity.createAttributes().build());
        event.put(ModEntities.REFLECTION.get(), ReflectionEntity.createAttributes().build());
        event.put(ModEntities.MASON.get(), MasonEntity.createAttributes().build());
        event.put(ModEntities.ARCHIVIST.get(), ArchivistEntity.createAttributes().build());
        event.put(ModEntities.NEIGHBOUR.get(), NeighbourEntity.createAttributes().build());
        event.put(ModEntities.CARRIER.get(), CarrierEntity.createAttributes().build());
        event.put(ModEntities.ORDERLY.get(), OrderlyEntity.createAttributes().build());
        event.put(ModEntities.SCARECROW.get(), ScarecrowEntity.createAttributes().build());
        event.put(ModEntities.COPY.get(), CopyEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void spawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(ModEntities.HOLLOW.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HollowEntity::checkSpawn,
                SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.LISTENER.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.ARCHIVIST.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ArchivistEntity::checkSpawn,
                SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.NEIGHBOUR.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, NeighbourEntity::checkSpawn,
                SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.CARRIER.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CarrierEntity::checkSpawn,
                SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.ORDERLY.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, OrderlyEntity::checkSpawn,
                SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.SCARECROW.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ScarecrowEntity::checkSpawn,
                SpawnPlacementRegisterEvent.Operation.OR);
        event.register(ModEntities.COPY.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CopyEntity::checkSpawn,
                SpawnPlacementRegisterEvent.Operation.OR);
    }

    @SubscribeEvent
    public static void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.RAIN_TEAR.get());
            event.accept(ModItems.PAPERS.get());
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.HUSH_DOOR.get());
            event.accept(ModItems.DROWNED_DOOR.get());
            event.accept(ModItems.HALLWAY_DOOR.get());
            event.accept(ModItems.BACKUP_DOOR.get());
            event.accept(ModItems.ARCHIVE_DOOR.get());
            event.accept(ModItems.STREET_DOOR.get());
            event.accept(ModItems.STATIC_DOOR.get());
            event.accept(ModItems.ORCHARD_DOOR.get());
            event.accept(ModItems.WARD_DOOR.get());
            event.accept(ModItems.SERVER_DOOR.get());
            event.accept(ModItems.GRAVE_MARKER.get());
            event.accept(ModItems.LOOSE_PAPERS.get());
        }
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.ECHO_SPAWN_EGG.get());
            event.accept(ModItems.HOLLOW_SPAWN_EGG.get());
            event.accept(ModItems.LISTENER_SPAWN_EGG.get());
            event.accept(ModItems.MASON_SPAWN_EGG.get());
            event.accept(ModItems.ARCHIVIST_SPAWN_EGG.get());
            event.accept(ModItems.NEIGHBOUR_SPAWN_EGG.get());
            event.accept(ModItems.CARRIER_SPAWN_EGG.get());
            event.accept(ModItems.ORDERLY_SPAWN_EGG.get());
            event.accept(ModItems.SCARECROW_SPAWN_EGG.get());
            event.accept(ModItems.COPY_SPAWN_EGG.get());
        }
    }
}
