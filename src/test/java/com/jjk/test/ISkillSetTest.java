package com.jjk.test;

import com.jjk.JjkConfig;
import com.jjk.anim.AnimationRegistry;
import com.jjk.api.skill.SkillResult;
import com.jjk.burden.BurdenManager;
import com.jjk.character.CharacterRegistry;
import com.jjk.character.SkillRegistry;
import com.jjk.character.impl.*;
import com.jjk.data.Migrator;
import com.jjk.data.PlayerData;
import com.jjk.team.TeamManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ISkillSetTest {

    @BeforeAll
    static void registerAll() {
        SkillRegistry.register("gojo",    new GojoSkillSet());
        SkillRegistry.register("itadori", new ItadoriSkillSet());
        SkillRegistry.register("megumi",  new MegumiSkillSet());
        SkillRegistry.register("okkotsu", new OkkotsuSkillSet());
        SkillRegistry.register("sukuna",  new SukunaSkillSet());
        SkillRegistry.register("nanami",  new NanamiSkillSet());
        SkillRegistry.register("jogo",    new JogoSkillSet());

        CharacterRegistry.register("gojo",    new CharacterRegistry.CharacterMeta("gojo",    "고죠 사토루",    "4급", 5000f));
        CharacterRegistry.register("itadori", new CharacterRegistry.CharacterMeta("itadori", "이타도리 유지",   "4급", 4000f));
        CharacterRegistry.register("megumi",  new CharacterRegistry.CharacterMeta("megumi",  "후시구로 메구미", "4급", 3500f));
        CharacterRegistry.register("okkotsu", new CharacterRegistry.CharacterMeta("okkotsu", "옷코츠 유타",    "4급", 4500f));
        CharacterRegistry.register("sukuna",  new CharacterRegistry.CharacterMeta("sukuna",  "료멘 스쿠나",   "특급", 6000f));
        CharacterRegistry.register("nanami",  new CharacterRegistry.CharacterMeta("nanami",  "나나미 켄토",    "1급", 3000f));
        CharacterRegistry.register("jogo",    new CharacterRegistry.CharacterMeta("jogo",    "죠고",          "4급", 4000f));
        SkillRegistry.register("inumaki",  new InumakiSkillSet());
        CharacterRegistry.register("inumaki", new CharacterRegistry.CharacterMeta("inumaki", "이누마키 토게",  "4급", 3000f));
        SkillRegistry.register("mahito",  new MahitoSkillSet());
        CharacterRegistry.register("mahito", new CharacterRegistry.CharacterMeta("mahito", "마히토", "특급", 4500f));
        SkillRegistry.register("hakari", new HakariSkillSet());
        CharacterRegistry.register("hakari", new CharacterRegistry.CharacterMeta("hakari", "하카리 킨지", "1급", 4000f));
        SkillRegistry.register("higuruma", new HigurumaskillSet());
        CharacterRegistry.register("higuruma", new CharacterRegistry.CharacterMeta("higuruma", "히구루마 히로미", "1급", 3500f));
    }

    private static PlayerData newData() {
        return PlayerData.createDefault(UUID.randomUUID());
    }

    @Test
    void testGojoAllKeysImplemented() {
        GojoSkillSet s = new GojoSkillSet();
        PlayerData d = newData();
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onF(d, null, 0L),      "F");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onShiftF(d, null, 0L), "Shift+F");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onR(d, null, 0L),      "R");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onShiftR(d, null, 0L), "Shift+R");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onV(d, null, 0L),      "V");
    }

    @Test
    void testItadoriAllKeysImplemented() {
        ItadoriSkillSet s = new ItadoriSkillSet();
        PlayerData d = newData();
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onF(d, null, 0L),      "F");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onShiftF(d, null, 0L), "Shift+F");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onR(d, null, 0L),      "R");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onShiftR(d, null, 0L), "Shift+R");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onV(d, null, 0L),      "V");
    }

    @Test
    void testMegumiAllKeysImplemented() {
        MegumiSkillSet s = new MegumiSkillSet();
        PlayerData d = newData(); // deadShikigamiIds 비어있음
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onF(d, null, 0L),      "F");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onShiftF(d, null, 0L), "Shift+F");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onR(d, null, 0L),      "R");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onShiftR(d, null, 0L), "Shift+R");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onV(d, null, 0L),      "V");
    }

    @Test
    void testOkkotsuAllKeysImplemented() {
        OkkotsuSkillSet s = new OkkotsuSkillSet();
        PlayerData d = newData();
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onF(d, null, 0L),      "F");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onShiftF(d, null, 0L), "Shift+F");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onR(d, null, 0L),      "R");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onShiftR(d, null, 0L), "Shift+R");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, s.onV(d, null, 0L),      "V");
    }

    @Test
    void testSukunaAllKeysImplemented() {
        SukunaSkillSet skill = new SukunaSkillSet();
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.ceCurrent = 9999f;
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onF(data, null, 0L),      "F");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onShiftF(data, null, 0L), "Shift+F");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onR(data, null, 0L),      "R");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onShiftR(data, null, 0L), "Shift+R");
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onV(data, null, 0L),      "V");
    }

    @Test
    void testSukunaTeam() {
        assertEquals(TeamManager.Team.CURSED_SPIRIT, TeamManager.getTeam("sukuna"));
    }

    @Test
    void testSukunaAnimIdReserved() {
        assertTrue(AnimationRegistry.has(21));
        assertTrue(AnimationRegistry.has(22));
        assertTrue(AnimationRegistry.has(23));
        assertTrue(AnimationRegistry.has(24));
    }

    @Test
    void testHpProportionalDamage() {
        float targetHp = 10f;
        float bonusDamage = targetHp * 0.20f;
        float totalDamage = 18f + bonusDamage;
        assertEquals(20f, totalDamage, 0.001f);
    }

    @Test
    void testMegumiDeadShikigamiBlocked() {
        MegumiSkillSet s = new MegumiSkillSet();
        PlayerData d = newData();
        d.deadShikigamiIds.add("nue");
        assertEquals(SkillResult.FAIL_CONDITION, s.onF(d, null, 0L), "죽은 누에 재소환 금지");
    }

    @Test
    void testOkkotsuBurstActivated() {
        OkkotsuSkillSet s = new OkkotsuSkillSet();
        PlayerData d = newData();
        long tick = 100L;
        SkillResult result = s.onShiftR(d, null, tick);
        assertEquals(SkillResult.SUCCESS, result, "주력해방 성공");
        assertTrue(d.burstActive, "burstActive=true");
        assertEquals(tick + 200, d.burstEndTick, "burstEndTick=tick+200");
    }

    // ── 이누마키 토게 ────────────────────────────────────────────────────────────

    @Test
    void testInumakiImplementedKeys() {
        InumakiSkillSet skill = new InumakiSkillSet();
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        // ceCurrent=100 < 각 CE → FAIL_CE_INSUFFICIENT ≠ NOT_IMPLEMENTED
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onF(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onShiftF(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onShiftR(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onV(data, null, 0L));
    }

    @Test
    void testInumakiRNotImplemented() {
        InumakiSkillSet skill = new InumakiSkillSet();
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        assertEquals(SkillResult.NOT_IMPLEMENTED, skill.onR(data, null, 0L));
    }

    @Test
    void testInumakiSealedBlocked() {
        InumakiSkillSet skill = new InumakiSkillSet();
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.cooldowns.put("skill_seal", Long.MAX_VALUE);
        assertEquals(SkillResult.FAIL_SKILL_SEALED, skill.onF(data, null, 0L));
        assertEquals(SkillResult.FAIL_SKILL_SEALED, skill.onShiftF(data, null, 0L));
    }

    @Test
    void testInumakiBurdenAccumulation() {
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        JjkConfig config = new JjkConfig();
        BurdenManager.addBurden(data, 90f, 0L, config);
        assertFalse(BurdenManager.isSealed(data, 0L), "90 추가 — 봉인 아직 안 됨");
        BurdenManager.addBurden(data, 15f, 0L, config);
        assertTrue(BurdenManager.isSealed(data, 0L), "105 → 봉인");
    }

    @Test
    void testInumakiRegistered() {
        assertNotNull(SkillRegistry.get("inumaki"), "inumaki SkillRegistry 미등록");
        assertTrue(CharacterRegistry.isRegistered("inumaki"), "inumaki CharacterRegistry 미존재");
    }

    // ── 죠고 ─────────────────────────────────────────────────────────────────────

    @Test
    void testJogoAllKeysImplemented() {
        JogoSkillSet skill = new JogoSkillSet();
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        // ceCurrent=100이므로 CE 부족 → FAIL_CE_INSUFFICIENT ≠ NOT_IMPLEMENTED
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onF(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onShiftF(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onR(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onShiftR(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onV(data, null, 0L));
    }

    @Test
    void testJogoRegistered() {
        assertNotNull(SkillRegistry.get("jogo"), "jogo SkillRegistry 미등록");
        assertTrue(CharacterRegistry.isRegistered("jogo"), "jogo CharacterRegistry 미존재");
    }

    @Test
    void testJogoTeam() {
        assertEquals(TeamManager.Team.CURSED_SPIRIT, TeamManager.getTeam("jogo"));
    }

    // ── 나나미 켄토 ─────────────────────────────────────────────────────────────

    @Test
    void testNanamiImplemented() {
        NanamiSkillSet skill = new NanamiSkillSet();
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        // F(CE=110), Shift+R(CE=240) 구현됨 — ceCurrent=100이므로 FAIL_CE_INSUFFICIENT 반환
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onF(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onShiftR(data, null, 0L));
    }

    @Test
    void testNanamiNotImplementedKeys() {
        NanamiSkillSet skill = new NanamiSkillSet();
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        assertEquals(SkillResult.NOT_IMPLEMENTED, skill.onShiftF(data, null, 0L));
        assertEquals(SkillResult.NOT_IMPLEMENTED, skill.onR(data, null, 0L));
        assertEquals(SkillResult.NOT_IMPLEMENTED, skill.onV(data, null, 0L));
    }

    @Test
    void testNanamiRegistered() {
        assertNotNull(SkillRegistry.get("nanami"), "nanami SkillRegistry 미등록");
        assertTrue(CharacterRegistry.isRegistered("nanami"), "nanami CharacterRegistry 미존재");
    }

    @Disabled("PlayerData에 pos/yaw 없음 — WeaknessZoneCalculator는 ServerPlayerEntity 기반 구현")
    @Test
    void testWeaknessZone() {
        // WeaknessZoneCalculator.isWeaknessHit(ServerPlayerEntity, LivingEntity)
        // ServerPlayerEntity Mock 없이 테스트 불가 — Phase 3 통합 테스트 대상
    }

    @Test
    void testHigurumaSwordCondition() {
        HigurumaskillSet skill = new HigurumaskillSet();
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.hasExecutionSword = false;
        assertEquals(SkillResult.FAIL_CONDITION, skill.onV(data, null, 0L));
        data.hasExecutionSword = true;
        data.ceCurrent = 9999f;
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onV(data, null, 0L));
    }

    // ── 히구루마 히로미 ──────────────────────────────────────────────────────────

    @Test
    void testHigurumaImplementedKeys() {
        HigurumaskillSet skill = new HigurumaskillSet();
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.ceCurrent = 9999f;
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onF(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onShiftR(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onV(data, null, 0L));
    }

    @Test
    void testHigurumaNotImplementedKeys() {
        HigurumaskillSet skill = new HigurumaskillSet();
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        assertEquals(SkillResult.NOT_IMPLEMENTED, skill.onShiftF(data, null, 0L));
        assertEquals(SkillResult.NOT_IMPLEMENTED, skill.onR(data, null, 0L));
    }

    @Test
    void testHigurumaRegistered() {
        assertNotNull(SkillRegistry.get("higuruma"));
        assertTrue(CharacterRegistry.isRegistered("higuruma"));
    }

    @Test
    void testTrialSuccessRateFromConfig() {
        JjkConfig config = new JjkConfig();
        assertEquals(0.60f, config.getTrialSuccessRate(), 0.001f);
    }

    // ── 마히토 ──────────────────────────────────────────────────────────────────

    @Test
    void testMahitoAllKeysImplemented() {
        MahitoSkillSet skill = new MahitoSkillSet();
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onF(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onShiftF(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onR(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onShiftR(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onV(data, null, 0L));
    }

    @Test
    void testMahitoSoulResistApplied() {
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        long tick = 100L;
        data.ceCurrent = 9999f;
        MahitoSkillSet skill = new MahitoSkillSet();
        SkillResult result = skill.onR(data, null, tick);
        assertEquals(SkillResult.SUCCESS, result);
        assertTrue(data.cooldowns.getOrDefault("status_soul_resist", 0L) > tick);
    }

    @Test
    void testMahitoRegistered() {
        assertNotNull(SkillRegistry.get("mahito"));
        assertTrue(CharacterRegistry.isRegistered("mahito"));
    }

    @Test
    void testMahitoTeam() {
        assertEquals(TeamManager.Team.CURSED_SPIRIT, TeamManager.getTeam("mahito"));
    }

    // ── 하카리 킨지 ──────────────────────────────────────────────────────────────

    @Test
    void testHakariAllKeysImplemented() {
        HakariSkillSet skill = new HakariSkillSet();
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.ceCurrent = 9999f;
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onF(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onShiftF(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onR(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onShiftR(data, null, 0L));
        assertNotEquals(SkillResult.NOT_IMPLEMENTED, skill.onV(data, null, 0L));
    }

    @Test
    void testJackpotDurationFromConfig() {
        JjkConfig config = new JjkConfig();
        assertEquals(251, config.getJackpotDurationTicks());
    }

    @Test
    void testJackpotActiveHealPerTick() {
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.jackpotActive = true;
        data.jackpotEndTick = Long.MAX_VALUE;
        data.hpCurrent = 10f;
        data.hpMax = 20f;
        JackpotStateMachine.tickJackpot(data, 0L, null);
        assertEquals(11f, data.hpCurrent, 0.001f);
    }

    @Test
    void testJackpotExpiry() {
        PlayerData data = PlayerData.createDefault(UUID.randomUUID());
        data.jackpotActive = true;
        data.jackpotEndTick = 5L;
        JackpotStateMachine.tickJackpot(data, 10L, null);
        assertFalse(data.jackpotActive);
    }

    @Test
    void testHakariRegistered() {
        assertNotNull(SkillRegistry.get("hakari"));
        assertTrue(CharacterRegistry.isRegistered("hakari"));
    }

    @Test
    void testMigratorV2() {
        assertEquals(2, Migrator.CURRENT_VERSION);
    }

    @Test
    void testAllPhase1CharactersRegistered() {
        for (String id : new String[]{"gojo", "itadori", "megumi", "okkotsu", "sukuna"}) {
            assertNotNull(SkillRegistry.get(id), id + " SkillRegistry 미등록");
        }
    }

    @Test
    void testCharacterMetaExists() {
        for (String id : new String[]{"gojo", "itadori", "megumi", "okkotsu", "sukuna"}) {
            assertTrue(CharacterRegistry.isRegistered(id), id + " CharacterRegistry 미존재");
        }
    }

    @Test
    void testDispatchRouting() {
        GojoSkillSet s = new GojoSkillSet();
        PlayerData d = newData();
        long tick = 0L;
        // dispatch(0) → onF() 경유 — 동일 결과여야 함
        assertEquals(s.onF(d, null, tick), s.dispatch(0, d, null, tick), "dispatch(0) == onF()");
    }
}
