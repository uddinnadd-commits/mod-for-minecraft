package com.sorrowed.registry;

import com.sorrowed.Sorrowed;
import com.sorrowed.entity.ArchivistEntity;
import com.sorrowed.entity.CarrierEntity;
import com.sorrowed.entity.EchoEntity;
import com.sorrowed.entity.HollowEntity;
import com.sorrowed.entity.ListenerEntity;
import com.sorrowed.entity.MasonEntity;
import com.sorrowed.entity.NeighbourEntity;
import com.sorrowed.entity.OrderlyEntity;
import com.sorrowed.entity.ScarecrowEntity;
import com.sorrowed.entity.ReflectionEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Sorrowed.MODID);

    public static final RegistryObject<EntityType<EchoEntity>> ECHO = ENTITIES.register("echo",
            () -> EntityType.Builder.<EchoEntity>of(EchoEntity::new, MobCategory.MISC)
                    .sized(0.6F, 2.3F).clientTrackingRange(10).build("echo"));

    public static final RegistryObject<EntityType<HollowEntity>> HOLLOW = ENTITIES.register("hollow",
            () -> EntityType.Builder.<HollowEntity>of(HollowEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.6F).clientTrackingRange(8).build("hollow"));

    public static final RegistryObject<EntityType<ListenerEntity>> LISTENER = ENTITIES.register("listener",
            () -> EntityType.Builder.<ListenerEntity>of(ListenerEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.7F).clientTrackingRange(10).build("listener"));

    public static final RegistryObject<EntityType<ReflectionEntity>> REFLECTION = ENTITIES.register("reflection",
            () -> EntityType.Builder.<ReflectionEntity>of(ReflectionEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F).clientTrackingRange(10).build("reflection"));

    public static final RegistryObject<EntityType<MasonEntity>> MASON = ENTITIES.register("mason",
            () -> EntityType.Builder.<MasonEntity>of(MasonEntity::new, MobCategory.MISC)
                    .sized(0.7F, 2.25F).clientTrackingRange(10).build("mason"));

    public static final RegistryObject<EntityType<ArchivistEntity>> ARCHIVIST = ENTITIES.register("archivist",
            () -> EntityType.Builder.<ArchivistEntity>of(ArchivistEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 2.7F).clientTrackingRange(10).build("archivist"));

    public static final RegistryObject<EntityType<NeighbourEntity>> NEIGHBOUR = ENTITIES.register("neighbour",
            () -> EntityType.Builder.<NeighbourEntity>of(NeighbourEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 2.0F).clientTrackingRange(12).build("neighbour"));

    public static final RegistryObject<EntityType<CarrierEntity>> CARRIER = ENTITIES.register("carrier",
            () -> EntityType.Builder.<CarrierEntity>of(CarrierEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.4F).clientTrackingRange(12).build("carrier"));

    public static final RegistryObject<EntityType<OrderlyEntity>> ORDERLY = ENTITIES.register("orderly",
            () -> EntityType.Builder.<OrderlyEntity>of(OrderlyEntity::new, MobCategory.MONSTER)
                    .sized(0.8F, 3.0F).clientTrackingRange(12).build("orderly"));

    public static final RegistryObject<EntityType<ScarecrowEntity>> SCARECROW = ENTITIES.register("scarecrow",
            () -> EntityType.Builder.<ScarecrowEntity>of(ScarecrowEntity::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.6F).clientTrackingRange(12).build("scarecrow"));
}
