package com.jjk.entity;

import com.jjk.entity.RikaEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ShikigamiEntityTypes {

    public static EntityType<ShikigamiEntity> NUE;
    public static EntityType<ShikigamiEntity> WHITE_DOG;
    public static EntityType<ShikigamiEntity> MAHORAGA;
    public static EntityType<RikaEntity> RIKA;

    public static void register() {
        NUE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("jjk", "nue"),
            EntityType.Builder
                .<ShikigamiEntity>create(
                    (type, world) -> new ShikigamiEntity(type, world, null, "silkworm"),
                    SpawnGroup.MONSTER)
                .dimensions(0.6f, 1.8f)
                .build()
        );
        WHITE_DOG = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("jjk", "white_dog"),
            EntityType.Builder
                .<ShikigamiEntity>create(
                    (type, world) -> new ShikigamiEntity(type, world, null, "white_dog"),
                    SpawnGroup.MONSTER)
                .dimensions(0.6f, 0.85f)
                .build()
        );
        MAHORAGA = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("jjk", "mahoraga"),
            EntityType.Builder
                .<ShikigamiEntity>create(
                    (type, world) -> new ShikigamiEntity(type, world, null, "mahoraga"),
                    SpawnGroup.MONSTER)
                .dimensions(0.8f, 2.5f)
                .build()
        );
        RIKA = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("jjk", "rika"),
            EntityType.Builder
                .<RikaEntity>create(
                    (type, world) -> new RikaEntity(type, world, null),
                    SpawnGroup.MONSTER)
                .dimensions(0.8f, 2.2f)
                .build()
        );
    }
}
