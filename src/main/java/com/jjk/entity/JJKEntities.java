package com.jjk.entity;

import com.jjk.entity.cursed.*;
import com.jjk.entity.npc.SorcererNPCEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

// TASK B-1·B-2 엔티티 통합 등록
// 렌더러 등록: PHASE D에서 처리 (src/client/ EntityRendererRegistry 스텁 예정)
public final class JJKEntities {

    public static EntityType<MukiEntity>                    MUKI;
    public static EntityType<KotsibakuEntity>               KOTSIBAKU;
    public static EntityType<KotsibakuProjectileEntity>     KOTSIBAKU_PROJECTILE;
    public static EntityType<CeProjectileEntity>            CE_PROJECTILE;
    public static EntityType<HomurakuiEntity>               HOMURAKU;
    public static EntityType<JogoNpcEntity>                 JOGO_NPC;
    public static EntityType<HannamiNpcEntity>              HANNAMI_NPC;
    public static EntityType<JuugoNpcEntity>                JUUGO_NPC;
    // 9종 신규 주령
    public static EntityType<SyoutoEntity>                  SYOUTO;
    public static EntityType<MoleEntity>                    MOLE_CURSED_SPIRIT;
    public static EntityType<FingerBearerEntity>            FINGER_BEARER;
    public static EntityType<PlantCursedSpiritEntity>       PLANT_CURSED_SPIRIT;
    public static EntityType<WaterCursedSpiritEntity>       WATER_CURSED_SPIRIT;
    public static EntityType<SmallpoxDeityEntity>           SMALLPOX_DEITY;
    public static EntityType<CursedEnergyAbsorberEntity>    CE_ABSORBER;
    public static EntityType<SplittingCursedSpiritEntity>   SPLITTING_CURSED_SPIRIT;
    public static EntityType<ShadowCursedSpiritEntity>      SHADOW_CURSED_SPIRIT;
    // 주술사 NPC 4개 등급
    public static EntityType<SorcererNPCEntity> SORCERER_NPC_4;
    public static EntityType<SorcererNPCEntity> SORCERER_NPC_3;
    public static EntityType<SorcererNPCEntity> SORCERER_NPC_2;
    public static EntityType<SorcererNPCEntity> SORCERER_NPC_1;

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

        CE_PROJECTILE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("jjk", "ce_projectile"),
            EntityType.Builder.<CeProjectileEntity>create(
                    CeProjectileEntity::new, SpawnGroup.MISC)
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

        // ── 9종 신규 주령 등록 ────────────────────────────────────────────────
        SYOUTO = registerMob("syouto",
            SyoutoEntity::new, SyoutoEntity.createAttributes(), 0.3f, 0.3f);

        MOLE_CURSED_SPIRIT = registerMob("mole_cursed_spirit",
            MoleEntity::new, MoleEntity.createAttributes(), 0.6f, 0.6f);

        FINGER_BEARER = registerMob("finger_bearer",
            FingerBearerEntity::new, FingerBearerEntity.createAttributes(), 1.2f, 2.4f);

        PLANT_CURSED_SPIRIT = registerMob("plant_cursed_spirit",
            PlantCursedSpiritEntity::new, PlantCursedSpiritEntity.createAttributes(), 1.0f, 2.0f);

        WATER_CURSED_SPIRIT = registerMob("water_cursed_spirit",
            WaterCursedSpiritEntity::new, WaterCursedSpiritEntity.createAttributes(), 1.0f, 1.8f);

        SMALLPOX_DEITY = registerMob("smallpox_deity",
            SmallpoxDeityEntity::new, SmallpoxDeityEntity.createAttributes(), 0.8f, 1.6f);

        CE_ABSORBER = registerMob("ce_absorber",
            CursedEnergyAbsorberEntity::new, CursedEnergyAbsorberEntity.createAttributes(), 0.8f, 1.6f);

        SPLITTING_CURSED_SPIRIT = registerMob("splitting_cursed_spirit",
            SplittingCursedSpiritEntity::new, SplittingCursedSpiritEntity.createAttributes(), 0.7f, 1.4f);

        SHADOW_CURSED_SPIRIT = registerMob("shadow_cursed_spirit",
            ShadowCursedSpiritEntity::new, ShadowCursedSpiritEntity.createAttributes(), 0.6f, 1.6f);

        // ── 주술사 NPC 4개 등급 등록 ──────────────────────────────────────────────
        SorcererNPCEntity.SorcererNPCGrade g4 = SorcererNPCEntity.SorcererNPCGrade.GRADE_4;
        SorcererNPCEntity.SorcererNPCGrade g3 = SorcererNPCEntity.SorcererNPCGrade.GRADE_3;
        SorcererNPCEntity.SorcererNPCGrade g2 = SorcererNPCEntity.SorcererNPCGrade.GRADE_2;
        SorcererNPCEntity.SorcererNPCGrade g1 = SorcererNPCEntity.SorcererNPCGrade.GRADE_1;
        SORCERER_NPC_4 = registerMob("sorcerer_npc_4",
            (t, w) -> new SorcererNPCEntity(t, w, g4),
            SorcererNPCEntity.createAttributes(g4), 0.6f, 1.8f);
        SORCERER_NPC_3 = registerMob("sorcerer_npc_3",
            (t, w) -> new SorcererNPCEntity(t, w, g3),
            SorcererNPCEntity.createAttributes(g3), 0.6f, 1.8f);
        SORCERER_NPC_2 = registerMob("sorcerer_npc_2",
            (t, w) -> new SorcererNPCEntity(t, w, g2),
            SorcererNPCEntity.createAttributes(g2), 0.6f, 1.8f);
        SORCERER_NPC_1 = registerMob("sorcerer_npc_1",
            (t, w) -> new SorcererNPCEntity(t, w, g1),
            SorcererNPCEntity.createAttributes(g1), 0.6f, 1.8f);
    }

    private static <T extends HostileEntity> EntityType<T> registerHostile(
            String id, EntityType.EntityFactory<T> factory,
            DefaultAttributeContainer.Builder attrs,
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

    private static <T extends PathAwareEntity> EntityType<T> registerMob(
            String id, EntityType.EntityFactory<T> factory,
            DefaultAttributeContainer.Builder attrs,
            float width, float height) {
        EntityType<T> type = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("jjk", id),
            EntityType.Builder.<T>create(factory, SpawnGroup.MONSTER)
                .dimensions(width, height)
                .maxTrackingRange(48)
                .build());
        FabricDefaultAttributeRegistry.register(type, attrs.build());
        return type;
    }

    private JJKEntities() {}
}
