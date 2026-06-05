package com.jjk.entity.npc;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class NpcRegistry {

    public static EntityType<SimpleNpcEntity> ZENIN_STORAGE;
    public static EntityType<SimpleNpcEntity> KUSAKABE;
    public static EntityType<SimpleNpcEntity> SHOKO;
    public static EntityType<SimpleNpcEntity> GOJO_SHIYU;
    public static EntityType<SimpleNpcEntity> IJICHI;
    public static EntityType<SimpleNpcEntity> YAGA;
    public static EntityType<SimpleNpcEntity> NAHOBINO;

    public static void register() {
        ZENIN_STORAGE = registerNpc("zenin_storage", "젠인 창고지기");
        KUSAKABE      = registerNpc("kusakabe",      "쿠사카베 아츠야");
        SHOKO         = registerNpc("shoko",         "이에이리 쇼코");
        GOJO_SHIYU    = registerNpc("gojo_shiyu",    "공시우");
        IJICHI        = registerNpc("ijichi",        "이치지 키요타카");
        YAGA          = registerNpc("yaga",          "야가 마사모토");
        NAHOBINO      = registerNpc("nahobino",      "나호비노 아오이");
    }

    private static EntityType<SimpleNpcEntity> registerNpc(String id, String displayName) {
        EntityType<SimpleNpcEntity> type = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("jjk", id),
            EntityType.Builder.<SimpleNpcEntity>create(
                    (t, w) -> new SimpleNpcEntity(t, w, id, displayName),
                    SpawnGroup.MISC)
                .dimensions(0.6f, 1.8f)
                .maxTrackingRange(10)
                .build()
        );
        FabricDefaultAttributeRegistry.register(type, NpcEntity.createAttributes().build());
        return type;
    }

    private NpcRegistry() {}
}
