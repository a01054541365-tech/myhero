package com.jjk.entity;

import com.jjk.entity.cursed.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

// TASK B-1·B-2 엔티티 통합 등록
// 렌더러 등록: PHASE D에서 처리 (src/client/ EntityRendererRegistry 스텁 예정)
public final class JJKEntities {

    public static EntityType<MukiEntity>                  MUKI;
    public static EntityType<KotsibakuEntity>             KOTSIBAKU;
    public static EntityType<KotsibakuProjectileEntity>   KOTSIBAKU_PROJECTILE;
    public static EntityType<HomurakuiEntity>             HOMURAKU;
    public static EntityType<JogoNpcEntity>               JOGO_NPC;
    public static EntityType<HannamiNpcEntity>            HANNAMI_NPC;
    public static EntityType<JuugoNpcEntity>              JUUGO_NPC;

    public static void register() {
        MUKI = registerHostile("muki",
            MukiEntity::new, MukiEntity.createAttributes(), 0.6f, 1.8f);

        KOTSIBAKU = registerHostile("kotsibaku",
            KotsibakuEntity::new, KotsibakuEntity.createAttributes(), 0.6f, 1.9f);

        KOTSIBAKU_PROJECTILE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("jjk", "kotsibaku_projectile"),
            EntityType.Builder.<KotsibakuProjectileEntity>create(
                    KotsibakuProjectileEntity::new, SpawnGroup.MISC)
                .dimensions(0.25f, 0.25f)
                .build());

        HOMURAKU = registerHostile("homuraku",
            HomurakuiEntity::new, HomurakuiEntity.createAttributes(), 0.7f, 2.0f);

        JOGO_NPC = registerHostile("jogo_npc",
            JogoNpcEntity::new, JogoNpcEntity.createAttributes(), 0.6f, 1.8f);

        HANNAMI_NPC = registerHostile("hannami_npc",
            HannamiNpcEntity::new, HannamiNpcEntity.createAttributes(), 0.6f, 1.8f);

        JUUGO_NPC = registerHostile("juugo_npc",
            JuugoNpcEntity::new, JuugoNpcEntity.createAttributes(), 0.6f, 1.8f);
    }

    private static <T extends HostileEntity> EntityType<T> registerHostile(
            String id, EntityType.EntityFactory<T> factory,
            net.minecraft.entity.attribute.DefaultAttributeContainer.Builder attrs,
            float width, float height) {
        EntityType<T> type = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("jjk", id),
            EntityType.Builder.<T>create(factory, SpawnGroup.MONSTER)
                .dimensions(width, height)
                .build());
        FabricDefaultAttributeRegistry.register(type, attrs.build());
        return type;
    }

    private JJKEntities() {}
}
