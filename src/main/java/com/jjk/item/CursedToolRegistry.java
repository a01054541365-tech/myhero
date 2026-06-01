package com.jjk.item;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import com.jjk.grade.GradeManager;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class CursedToolRegistry {

    public static CursedToolItem CURSED_DAGGER;
    public static CursedToolItem THOUSAND_SPEAR;
    public static CursedToolItem PLAYFUL_CLOUD;
    public static CursedToolItem INVERTED_SPEAR;
    public static CursedToolItem SPLIT_SOUL_BLADE;

    private static final Map<String, CursedToolEffect> EFFECTS = new HashMap<>();

    static {
        // JJKMod가 초기화되기 전에도 테스트에서 접근 가능하도록 기본값 사용
        JjkConfig cfg = JJKMod.getInstance() != null ? JJKMod.getConfig() : new JjkConfig();
        initEffects(cfg);
    }

    static void initEffects(JjkConfig cfg) {
        EFFECTS.put("cursed_dagger", new CursedToolEffect(
            cfg.cursedToolAttackBonus_dagger(),
            0f, 0f, 0f, 0, 0, false,
            GradeManager.Grade.GRADE_4.rank));

        EFFECTS.put("thousand_spear", new CursedToolEffect(
            cfg.cursedToolAttackBonus_spear(),
            cfg.cursedToolCeReduction_spear(),
            0f, 0f, 0, 0, false,
            GradeManager.Grade.GRADE_1.rank));

        EFFECTS.put("playful_cloud", new CursedToolEffect(
            cfg.cursedToolAttackBonus_cloud(),
            0f, cfg.cursedToolRangeBonus_cloud(),
            0f, 0, 0, false,
            GradeManager.Grade.SEMI_SPECIAL.rank));

        EFFECTS.put("inverted_spear", new CursedToolEffect(
            cfg.cursedToolAttackBonus_inverted(),
            0f, 0f, cfg.cursedToolDefPenetration_inverted(),
            0, cfg.cursedToolSealCooldown_inverted(), false,
            GradeManager.Grade.SPECIAL.rank));

        EFFECTS.put("split_soul_blade", new CursedToolEffect(
            cfg.cursedToolAttackBonus_soul(),
            0f, 0f, 0f,
            cfg.cursedToolBlackFlashBonus_soul(),
            0, true,
            GradeManager.Grade.SPECIAL.rank));
    }

    /** JJKMod.onInitialize()에서 호출 — Registries에 아이템 등록 */
    public static void registerItems() {
        CURSED_DAGGER    = registerItem("cursed_dagger");
        THOUSAND_SPEAR   = registerItem("thousand_spear");
        PLAYFUL_CLOUD    = registerItem("playful_cloud");
        INVERTED_SPEAR   = registerItem("inverted_spear");
        SPLIT_SOUL_BLADE = registerItem("split_soul_blade");
        // config 재적용 (런타임 config 반영)
        if (JJKMod.getInstance() != null) {
            EFFECTS.clear();
            initEffects(JJKMod.getConfig());
        }
    }

    private static CursedToolItem registerItem(String id) {
        return Registry.register(
            Registries.ITEM,
            Identifier.of("jjk", id),
            new CursedToolItem(id, new net.minecraft.item.Item.Settings().maxCount(1)));
    }

    public static CursedToolEffect getEffect(String toolId) {
        return EFFECTS.getOrDefault(toolId, CursedToolEffect.NONE);
    }

    /**
     * 플레이어 현재 장착 주구 효과 반환.
     * 메인핸드 우선, 등급 조건 검사 포함. 조건 미달이면 NONE.
     */
    public static CursedToolEffect getActiveEffect(ServerPlayerEntity player, PlayerData data) {
        ItemStack main = player.getMainHandStack();
        ItemStack off  = player.getOffHandStack();
        for (ItemStack stack : new ItemStack[]{main, off}) {
            if (stack.getItem() instanceof CursedToolItem tool) {
                if (tool.isEligible(data)) {
                    return getEffect(tool.getToolId());
                }
            }
        }
        return CursedToolEffect.NONE;
    }

    private CursedToolRegistry() {}
}
