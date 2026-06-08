package com.jjk.advancement;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * 서버 사이드 업적 트리거 관리자.
 * 모든 grant() 호출은 서버 스레드에서만 발생해야 함.
 */
public final class AdvancementTriggerManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk");

    private static final Set<String> ALL_CHARACTERS = Set.of(
        "gojo", "itadori", "megumi", "okkotsu", "sukuna",
        "mahito", "jogo", "hakari", "inumaki", "nanami",
        "higuruma", "choso"
    );

    private AdvancementTriggerManager() {}

    // ── core grant ───────────────────────────────────────────────────────────

    private static void grant(ServerPlayerEntity player, String advancementId) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        AdvancementEntry entry = server.getAdvancementLoader().get(Identifier.of("jjk", advancementId));
        if (entry == null) {
            LOGGER.warn("[JJK] advancement not found: {}", advancementId);
            return;
        }
        var tracker = player.getAdvancementTracker();
        if (tracker.getProgress(entry).isDone()) return;
        tracker.grantCriterion(entry, "impossible");
    }

    // ── event hooks ──────────────────────────────────────────────────────────

    /** 흑섬 명중 시. perfectBlackFlash=true면 완벽한 흑섬. */
    public static void onBlackFlash(ServerPlayerEntity player, boolean perfectBlackFlash) {
        grant(player, "combat/first_black_flash");

        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (perfectBlackFlash) {
            long count = data.cooldowns.getOrDefault("adv_perfect_bf", 0L) + 1L;
            data.cooldowns.put("adv_perfect_bf", count);
            JJKMod.getPlayerRepository().save(data);
            if (count >= 10) {
                grant(player, "combat/perfect_black_flash_10");
            }
        }
    }

    /** 영역 전개 성공 시. */
    public static void onDomainDeploy(ServerPlayerEntity player) {
        grant(player, "combat/first_domain");
    }

    /** 영역 충돌 승리 시. */
    public static void onDomainCollisionWin(ServerPlayerEntity player) {
        grant(player, "combat/domain_collision_win");
    }

    /** 특급 주령 처치 시 (SPECIAL 또는 SEMI_SPECIAL). */
    public static void onSpecialGradeKill(ServerPlayerEntity player) {
        grant(player, "combat/kill_special_grade");
    }

    /** 손가락 획득 시. totalCount = 취득 후 총 개수. */
    public static void onFingerCollect(ServerPlayerEntity player, int totalCount) {
        if (totalCount >= 5)  grant(player, "collect/finger_5");
        if (totalCount >= 10) grant(player, "collect/finger_10");
        if (totalCount >= 20) grant(player, "collect/finger_20");
    }

    /** 주령 포획 시. totalCaptured = 포획 후 총 누적 횟수. */
    public static void onCaptureSpirit(ServerPlayerEntity player, int totalCaptured) {
        if (totalCaptured >= 10) grant(player, "collect/capture_10");
    }

    /** 등급 달성 시. gradeInt: SPECIAL=0, SEMI_SPECIAL=1, ... */
    public static void onGradeReach(ServerPlayerEntity player, int gradeInt) {
        if (gradeInt == 0) grant(player, "growth/reach_special_grade");
    }

    /** 숙련도 달성 시. level: 현재 mastery 값. */
    public static void onMasteryReach(ServerPlayerEntity player, int level) {
        if (level >= 10) grant(player, "growth/mastery_max");
    }

    /** 속박 서약 파훼 성공 시. */
    public static void onBindingVowBreak(ServerPlayerEntity player) {
        grant(player, "growth/break_binding_vow");
    }

    /** 캐릭터 선택 시. 모든 12개 캐릭터를 한 번씩 선택하면 업적 달성. */
    public static void onCharacterTried(ServerPlayerEntity player, String characterId) {
        if (!ALL_CHARACTERS.contains(characterId)) return;

        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        data.cooldowns.put("adv_tried_" + characterId, 1L);

        boolean allTried = ALL_CHARACTERS.stream()
            .allMatch(c -> data.cooldowns.getOrDefault("adv_tried_" + c, 0L) > 0L);
        JJKMod.getPlayerRepository().save(data);

        if (allTried) {
            grant(player, "collect/all_characters");
        }
    }

    /** 이누마키가 특급 주령을 처치했을 때. */
    public static void onInumakiKillSpecialGrade(ServerPlayerEntity player) {
        grant(player, "secret/inumaki_kill_special");
    }

    /** 나나미 7:3 약점 명중 횟수. totalCount = 명중 후 누적 횟수. */
    public static void onNanamiRatioHit(ServerPlayerEntity player, int totalCount) {
        if (totalCount >= 100) grant(player, "secret/nanami_ratio_100");
    }

    /** 하카리 잭팟 연속 발동 횟수. streak = 현재 연속 잭팟 횟수. */
    public static void onHakariJackpotStreak(ServerPlayerEntity player, int streak) {
        if (streak >= 5) grant(player, "secret/hakari_jackpot_5");
    }
}
