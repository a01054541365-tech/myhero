package com.jjk.entity;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class CursedSpiritEntityTypes {

    public static EntityType<CursedSpiritEntity> GRADE_4;
    public static EntityType<CursedSpiritEntity> GRADE_3;
    public static EntityType<CursedSpiritEntity> GRADE_2;
    public static EntityType<CursedSpiritEntity> GRADE_1;
    public static EntityType<CursedSpiritEntity> SPECIAL;
    public static EntityType<CeProjectileEntity> CE_PROJECTILE;

    public static void register() {
        GRADE_4 = registerSpirit("cursed_spirit_grade4", CursedSpiritGrade.GRADE_4);
        GRADE_3 = registerSpirit("cursed_spirit_grade3", CursedSpiritGrade.GRADE_3);
        GRADE_2 = registerSpirit("cursed_spirit_grade2", CursedSpiritGrade.GRADE_2);
        GRADE_1 = registerSpirit("cursed_spirit_grade1", CursedSpiritGrade.GRADE_1);
        SPECIAL = registerSpirit("cursed_spirit_special", CursedSpiritGrade.SPECIAL);

        CE_PROJECTILE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("jjk", "ce_projectile"),
            EntityType.Builder.<CeProjectileEntity>create(
                    CeProjectileEntity::new, SpawnGroup.MISC)
                .dimensions(0.25f, 0.25f)
                .build());

        // [66-5] 스폰 weight 조정: 바닐라 좀비(100)·스켈레톤(100) 대비 공존 가능 수준
        // GRADE_4: 80→15, GRADE_3: 30→5
        BiomeModifications.addSpawn(
            BiomeSelectors.foundInOverworld(),
            SpawnGroup.MONSTER, GRADE_4,
            15, 1, 3);
        BiomeModifications.addSpawn(
            BiomeSelectors.foundInOverworld(),
            SpawnGroup.MONSTER, GRADE_3,
            5, 1, 2);

        // [66-3 허용됨] DomainDeployGoal의 내부 cooldown int는 서버 재시작 시 초기화.
        // 특급 주령 즉시 재전개 가능 — 허용 범위 (EntityData NBT 저장 불필요 판단).
    }

    private static EntityType<CursedSpiritEntity> registerSpirit(String id, CursedSpiritGrade grade) {
        EntityType<CursedSpiritEntity> type = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of("jjk", id),
            EntityType.Builder.<CursedSpiritEntity>create(
                    (t, w) -> new CursedSpiritEntity(t, w, grade),
                    SpawnGroup.MONSTER)
                .dimensions(0.6f, 1.8f)
                .build());
        FabricDefaultAttributeRegistry.register(
            type, CursedSpiritEntity.createAttributes(grade).build());
        return type;
    }

    private CursedSpiritEntityTypes() {}
}
