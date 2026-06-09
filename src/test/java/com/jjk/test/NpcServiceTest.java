package com.jjk.test;

import com.jjk.JJKMod;
import com.jjk.api.skill.SkillResult;
import com.jjk.data.Grade;
import com.jjk.data.Migrator;
import com.jjk.data.PlayerData;
import com.jjk.data.PlayerRepository;
import com.jjk.economy.CursedStoneManager;
import com.jjk.network.c2s.NpcServiceC2SPacket;
import com.jjk.npc.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NpcServiceTest {

    private CursedStoneManager csm;
    private PlayerRepository repo;

    @BeforeEach
    void setUp() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        new Migrator().migrate(conn);
        repo = new PlayerRepository(conn);
        csm = new CursedStoneManager(repo);
        JJKMod.initForTest(csm, repo);
    }

    private PlayerData makeSorcerer(String grade) {
        PlayerData d = PlayerData.createDefault(UUID.randomUUID());
        d.characterId = "itadori";
        d.grade = Grade.fromKey(grade);
        return d;
    }

    private PlayerData makeSpirit(String grade) {
        PlayerData d = PlayerData.createDefault(UUID.randomUUID());
        d.characterId = "mahito";
        d.grade = Grade.fromKey(grade);
        return d;
    }

    private NpcServiceC2SPacket pkt(String npcId, String action, String param) {
        return new NpcServiceC2SPacket(npcId, action, param);
    }

    // ── ZeninService ───────────────────────────────────────────────────────────

    @Test
    void zenin_buy_sufficient_grade_success() {
        PlayerData data = makeSorcerer("4급");
        data.cursedStones = 500L;
        SkillResult r = ZeninService.handle(pkt("zenin_storage", "buy", "cursed_dagger"),
            data, null, 0L, csm);
        assertEquals(SkillResult.SUCCESS, r, "잔액 충분 + 등급 충족 → 구매 성공");
        assertEquals(200L, data.cursedStones, "500 - 300 = 200");
    }

    @Test
    void zenin_buy_insufficient_stones() {
        PlayerData data = makeSorcerer("4급");
        data.cursedStones = 100L;
        SkillResult r = ZeninService.handle(pkt("zenin_storage", "buy", "cursed_dagger"),
            data, null, 0L, csm);
        assertEquals(SkillResult.CE_INSUFFICIENT, r, "잔액 부족 → CE_INSUFFICIENT");
        assertEquals(100L, data.cursedStones, "차감 없음");
    }

    @Test
    void zenin_buy_grade_insufficient() {
        PlayerData data = makeSorcerer("4급");
        data.cursedStones = 5000L;
        // 천역모(1급 이상 필요)를 4급이 구매 시도
        SkillResult r = ZeninService.handle(pkt("zenin_storage", "buy", "thousand_spear"),
            data, null, 0L, csm);
        assertEquals(SkillResult.FAIL, r, "등급 미달 → FAIL");
        assertEquals(5000L, data.cursedStones, "차감 없음");
    }

    // ── KusakabeService ────────────────────────────────────────────────────────

    @Test
    void kusakabe_reset_first_free() {
        PlayerData data = makeSorcerer("4급");
        data.mastery = 5;
        data.masteryResetCount = 0;
        SkillResult r = KusakabeService.handle(pkt("kusakabe", "reset", null),
            data, null, 0L, csm);
        assertEquals(SkillResult.SUCCESS, r, "첫 번째 초기화 무료");
        assertEquals(0, data.mastery, "mastery reset to 0");
        assertEquals(1, data.masteryResetCount);
    }

    @Test
    void kusakabe_reset_second_costs_1500() {
        PlayerData data = makeSorcerer("4급");
        data.mastery = 5;
        data.masteryResetCount = 1;
        data.cursedStones = 2000L;
        SkillResult r = KusakabeService.handle(pkt("kusakabe", "reset", null),
            data, null, 0L, csm);
        assertEquals(SkillResult.SUCCESS, r, "두 번째 초기화 1500석");
        assertEquals(500L, data.cursedStones, "2000 - 1500 = 500");
    }

    // ── ShokoService ───────────────────────────────────────────────────────────

    @Test
    void shoko_heal_full_on_cooldown() {
        PlayerData data = makeSorcerer("4급");
        data.cursedStones = 1000L;
        long tick = 1000L;
        // 쿨타임 설정
        data.cooldowns.put("npc_shoko_heal_full", tick + 100L);
        SkillResult r = ShokoService.handle(pkt("shoko", "heal_full", null),
            data, null, tick, csm);
        assertEquals(SkillResult.ON_COOLDOWN, r, "쿨타임 중 → ON_COOLDOWN");
    }

    @Test
    void shoko_status_clear_removes_seal() {
        PlayerData data = makeSorcerer("4급");
        data.cursedStones = 500L;
        data.cooldowns.put("skill_seal", 99999L);
        data.burden = 50;
        SkillResult r = ShokoService.handle(pkt("shoko", "status_clear", null),
            data, null, 0L, csm);
        assertEquals(SkillResult.SUCCESS, r);
        assertFalse(data.cooldowns.containsKey("skill_seal"), "봉인 해제");
        assertEquals(0, data.burden, "부담 초기화");
    }

    // ── GojoShiyuService ───────────────────────────────────────────────────────

    @Test
    void goshinyu_sorcerer_rejected() {
        PlayerData data = makeSorcerer("4급"); // 주술사 진영
        SkillResult r = GojoShiyuService.handle(pkt("gojo_shiyu", "settle_bounty", null),
            data, null, 0L, csm);
        assertEquals(SkillResult.FAIL, r, "주술사 진영 → FAIL");
    }

    @Test
    void goshinyu_spirit_zero_bounty_fail() {
        PlayerData data = makeSpirit("4급");
        data.bounty = 0L;
        data.cursedStones = 100L;
        SkillResult r = GojoShiyuService.handle(pkt("gojo_shiyu", "settle_bounty", null),
            data, null, 0L, csm);
        assertEquals(SkillResult.FAIL, r, "bounty=0 → 정산 실패");
    }

    @Test
    void goshinyu_spirit_bounty_settled() {
        PlayerData data = makeSpirit("4급");
        data.bounty = 100L;
        data.cursedStones = 0L;
        SkillResult r = GojoShiyuService.handle(pkt("gojo_shiyu", "settle_bounty", null),
            data, null, 0L, csm);
        assertEquals(SkillResult.SUCCESS, r, "bounty 정산 성공");
        assertEquals(100L, data.cursedStones, "bounty → cursedStones");
        assertEquals(0L, data.bounty);
    }

    // ── NahovinoService ────────────────────────────────────────────────────────

    @Test
    void nahobino_train_atk_grade_insufficient() {
        PlayerData data = makeSorcerer("3급"); // 1급 미만
        data.cursedStones = 5000L;
        SkillResult r = NahovinoService.handle(pkt("nahobino", "train_atk", null),
            data, null, 0L, csm);
        assertEquals(SkillResult.FAIL, r, "3급으로 공격 특훈 → 등급 미달 FAIL");
        assertEquals(5000L, data.cursedStones, "차감 없음");
    }

    @Test
    void nahobino_train_atk_max_exceeded() {
        PlayerData data = makeSorcerer("1급");
        data.cursedStones = 50000L;
        data.cooldowns.put("stat_atk_bonus", 5L); // 이미 최대
        SkillResult r = NahovinoService.handle(pkt("nahobino", "train_atk", null),
            data, null, 0L, csm);
        assertEquals(SkillResult.FAIL, r, "최대 횟수 초과 → FAIL");
        assertEquals(50000L, data.cursedStones, "차감 없음");
    }

    // ── YagaService ────────────────────────────────────────────────────────────

    @Test
    void yaga_enhance_max_level_fail() {
        PlayerData data = makeSorcerer("4급");
        data.cursedStones = 99999L;
        data.cooldowns.put("tool_enhance_cursed_dagger", 3L); // 최대 강화
        SkillResult r = YagaService.handle(pkt("yaga", "enhance", "cursed_dagger"),
            data, null, 0L, csm);
        assertEquals(SkillResult.FAIL, r, "3단계 강화 후 추가 강화 → FAIL");
        assertEquals(99999L, data.cursedStones, "차감 없음");
    }
}
