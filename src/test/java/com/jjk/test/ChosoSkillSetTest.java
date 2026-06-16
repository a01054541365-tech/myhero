package com.jjk.test;

import com.jjk.anim.AnimationRegistry;
import com.jjk.api.skill.SkillResult;
import com.jjk.character.impl.ChosoSkillSet;
import com.jjk.combat.TechniqueLoader;
import com.jjk.data.PlayerData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// TASK-1: 쵸소 Shift+R(keyId 3) "혈도폭쇄" — 수치는 techniques.json 단일 기준
class ChosoSkillSetTest {

    private static final int KEY = 3;

    @BeforeAll
    static void loadTechniques() {
        TechniqueLoader.load(Path.of("run/config/jjk/techniques.json"));
    }

    private static ChosoSkillSet skill() {
        return new ChosoSkillSet();
    }

    private static PlayerData chosoData(float ce) {
        PlayerData d = PlayerData.createDefault(UUID.randomUUID());
        d.characterId = "choso";
        d.ceCurrent   = ce;
        d.ceMax       = 2200f;
        return d;
    }

    // ── 0. 수치는 JSON에서 로드 (하드코딩 아님) ──────────────────────────────
    @Test
    void values_loadedFromJson() {
        assertEquals(320, skill().getCeCost(KEY),       "ceCost는 techniques.json에서 로드");
        assertEquals(40,  skill().getCooldownTicks(KEY), "cooldownTicks는 techniques.json에서 로드");
        assertEquals("혈도폭쇄", skill().getSkillName(KEY), "skillName");
        assertEquals(34f, TechniqueLoader.getBaseDamage("choso", KEY), 0.001f, "baseDamage");
    }

    // ── 0b. animId 66 등록 확인 ──────────────────────────────────────────────
    @Test
    void animId66_registered() {
        assertTrue(AnimationRegistry.has(66), "animId 66 등록됨");
        assertEquals("choso_shift_r", AnimationRegistry.get(66), "animId 66 → choso_shift_r");
    }

    // ── 1. CE 충분 → SUCCESS, CE 차감, 쿨타임 설정 ──────────────────────────
    @Test
    void ceSufficient_returnsSuccessAndConsumesCe() {
        PlayerData data = chosoData(500f);
        long tick = 0L;

        SkillResult result = skill().onShiftR(data, null, tick);

        assertNotEquals(SkillResult.NOT_IMPLEMENTED, result, "더 이상 NOT_IMPLEMENTED 아님");
        assertEquals(SkillResult.SUCCESS, result, "발동 성공 (범위 내 대상 없어도 성공)");
        assertEquals(180f, data.ceCurrent, 0.001f, "CE 320 차감 → 500-320=180");
        assertEquals(tick + 40L, data.cooldowns.get("cd_choso_3"), "쿨타임 40틱 설정");
    }

    // ── 2. CE 부족 → CE_INSUFFICIENT, CE 차감 없음 ──────────────────────────
    @Test
    void ceInsufficient_noConsumption() {
        PlayerData data = chosoData(100f);
        long tick = 0L;

        SkillResult result = skill().onShiftR(data, null, tick);

        assertEquals(SkillResult.CE_INSUFFICIENT, result, "CE 부족");
        assertEquals(100f, data.ceCurrent, 0.001f, "CE 차감 없음");
        assertFalse(data.cooldowns.containsKey("cd_choso_3"), "쿨타임 설정 안 됨");
    }

    // ── 3. 명중 시 SLOW 40틱 부여 (status_slow 만료 틱 = tick + 40) ──────────
    @Test
    void hit_appliesSlowStatus40Ticks() {
        PlayerData target = PlayerData.createDefault(UUID.randomUUID());
        long tick = 1000L;

        ChosoSkillSet.applySlow(target, tick);

        assertEquals(tick + 40L, target.cooldowns.get("status_slow"), "SLOW 만료 틱 = tick + 40");
    }
}
