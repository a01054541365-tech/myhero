package com.jjk.grade;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.advancement.AdvancementTriggerManager;
import com.jjk.character.CharacterRegistry;
import com.jjk.combat.TechniqueLoader;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.CharacterInfoS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GradeManager {

    public enum Grade {
        GRADE_4("4급",    0),
        GRADE_3("3급",    1),
        GRADE_2("2급",    2),
        GRADE_1("1급",    3),
        SEMI_SPECIAL("준특급", 4),
        SPECIAL("특급",   5);

        public final String label;
        public final int rank;

        Grade(String label, int rank) {
            this.label = label;
            this.rank  = rank;
        }

        public static Grade fromLabel(String label) {
            if (label == null) return GRADE_4;
            for (Grade g : values()) {
                if (g.label.equals(label)) return g;
            }
            return GRADE_4;
        }

        /** 다음 등급. SPECIAL이면 null. */
        public Grade next() {
            int nextRank = rank + 1;
            for (Grade g : values()) {
                if (g.rank == nextRank) return g;
            }
            return null;
        }
    }

    // 1전투당 데미지 XP 상한 추적 (in-memory, 서버 재시작 시 초기화)
    private final Map<UUID, Integer> combatXpAccum = new HashMap<>();
    private final Map<UUID, Long>    combatStartTick = new HashMap<>();

    public void addXp(PlayerData data, int amount, ServerPlayerEntity player) {
        if (data == null || amount <= 0) return;
        data.xp += amount;
        checkAndPromote(data, player);
        if (player != null) {
            JJKMod.getPlayerRepository().saveImmediate(data);
        }
    }

    private void checkAndPromote(PlayerData data, ServerPlayerEntity player) {
        while (true) {
            Grade current = Grade.fromLabel(data.grade != null ? data.grade.display : "4급");
            Grade next = current.next();
            if (next == null) break;
            int threshold = getXpThreshold(current);
            if (data.xp < threshold) break;
            data.xp -= threshold;
            promoteGrade(data, current, next, player);
        }
    }

    private void promoteGrade(PlayerData data, Grade from, Grade to,
                               ServerPlayerEntity player) {
        data.grade = com.jjk.data.Grade.fromKey(to.label);

        if (from == Grade.GRADE_4 && to == Grade.GRADE_3 && player != null
                && JJKMod.getAchievementManager() != null) {
            JJKMod.getAchievementManager().unlock(player, "first_grade_up");
        }

        // 특급 달성 시 ceControl +0.20 (P3-2)
        if (to == Grade.SPECIAL) {
            data.ceControl = Math.min(data.ceControl + 0.20f, 2.0f);
            if (player != null) AdvancementTriggerManager.onGradeReach(player, 0);
        }

        // 등급 상승 주력석 지급
        if (player != null && JJKMod.getCursedStoneManager() != null) {
            long stoneReward = switch (to) {
                case GRADE_3      -> 100L;
                case GRADE_2      -> 150L;
                case GRADE_1      -> 200L;
                case SEMI_SPECIAL -> 350L;
                case SPECIAL      -> 500L;
                default           -> 0L;
            };
            if (stoneReward > 0L) {
                JJKMod.getCursedStoneManager()
                    .give(data, stoneReward, "grade_up_" + to.label, player);
            }
        }

        // CE_max 증가
        float ceIncrease = switch (to) {
            case GRADE_3      -> 100f;
            case GRADE_2      -> 150f;
            case GRADE_1      -> 200f;
            case SEMI_SPECIAL -> 300f;
            case SPECIAL      -> 500f;
            default           -> 0f;
        };
        data.ceMax    += ceIncrease;
        data.ceCurrent = Math.min(data.ceCurrent + ceIncrease, data.ceMax);

        // 새 등급 스킬 슬롯 해금
        unlockSkillsForGrade(data, to.label);

        // 감사 로그
        if (JJKMod.getAuditLogger() != null) {
            JJKMod.getAuditLogger().logEvent("grade_up", data.uuid,
                    "{\"from\":\"" + from.label + "\",\"to\":\"" + to.label + "\"}", 0L);
        }

        // 주간 퀘스트 연동
        if (JJKMod.getQuestManager() != null) {
            JJKMod.getQuestManager().progressWeekly(data, "grade_up", player);
        }

        // 클라이언트 알림 (채팅 메시지 + HUD 갱신 패킷)
        if (player != null) {
            player.sendMessage(Text.literal("[JJK] 등급 상승! " + from.label + " → " + to.label), false);
            ServerPlayNetworking.send(player,
                    new CharacterInfoS2CPacket(data.characterId,
                            data.grade != null ? data.grade.display : "4급",
                            data.ceMax, data.ceCurrent));
        }
    }

    /** 관리자 setgrade 등에서 해당 등급까지 모든 스킬을 해금할 때 사용. */
    public void applyGradeUnlocks(PlayerData data, String grade) {
        unlockSkillsForGrade(data, grade);
    }

    private void unlockSkillsForGrade(PlayerData data, String newGrade) {
        if (data.characterId == null) return;
        TechniqueLoader.getAllForCharacter(data.characterId).stream()
                .filter(def -> !def.notImplemented && matchesGrade(def.unlockGrade, newGrade))
                .forEach(def -> {
                    String key = data.characterId + ":" + def.keyId;
                    if (!data.unlockedSkills.contains(key)) {
                        data.unlockedSkills.add(key);
                    }
                });
    }

    private static boolean matchesGrade(String unlockGrade, String playerGrade) {
        return Grade.fromLabel(unlockGrade).rank <= Grade.fromLabel(playerGrade).rank;
    }

    public int getXpThreshold(Grade from) {
        if (JJKMod.getInstance() == null) return 9999;
        JjkConfig cfg = JJKMod.getConfig();
        return switch (from) {
            case GRADE_4      -> cfg.xpGrade4to3();
            case GRADE_3      -> cfg.xpGrade3to2();
            case GRADE_2      -> cfg.xpGrade2to1();
            case GRADE_1      -> cfg.xpGrade1toSemi();
            case SEMI_SPECIAL -> cfg.xpGradeSemiToSpecial();
            default           -> Integer.MAX_VALUE;
        };
    }

    // ─── 전투 XP 이벤트 헬퍼 ──────────────────────────────────────────────────

    /**
     * 전투 XP 이벤트. defenderGradeRank로 등급 보호 보너스 적용.
     * G-5-1: gradeDiff >= config.gradeProtectionDiff 시 XP × gradeProtectionXpMultiplier.
     */
    public void onDamageHit(PlayerData attacker, ServerPlayerEntity player, long tick, int defenderGradeRank) {
        if (JJKMod.getInstance() == null) return;
        JjkConfig cfg = JJKMod.getConfig();
        UUID uuid = attacker.uuid;
        long start = combatStartTick.getOrDefault(uuid, tick);
        if (tick - start > 200) {
            combatXpAccum.put(uuid, 0);
            combatStartTick.put(uuid, tick);
        }
        int acc = combatXpAccum.getOrDefault(uuid, 0);
        if (acc < cfg.xpOnDamageCapPerCombat()) {
            int xpAmount = cfg.xpOnDamagePerHit();
            int attackerGradeRank = attacker.grade != null ? attacker.grade.ordinal() : 0;
            int gradeDiff = Math.abs(attackerGradeRank - defenderGradeRank);
            if (gradeDiff >= cfg.gradeProtectionDiff()) {
                xpAmount = (int)(xpAmount * cfg.gradeProtectionXpMultiplier());
            }
            addXp(attacker, xpAmount, player);
            combatXpAccum.put(uuid, acc + xpAmount);
        }
    }

    public void onKill(PlayerData attacker, PlayerData target, ServerPlayerEntity player, long tick) {
        if (JJKMod.getInstance() == null) return;
        JjkConfig cfg = JJKMod.getConfig();
        addXp(attacker, cfg.xpOnKill(), player);
        // Perfect 판정: attacker가 최근 피격 없으면 추가 XP
        if (attacker.lastDamageTakenTick < attacker.lastAttackTick) {
            addXp(attacker, cfg.xpOnPerfect(), player);
        }
    }

    public void onBlackFlash(PlayerData attacker, ServerPlayerEntity player) {
        if (JJKMod.getInstance() == null) return;
        addXp(attacker, JJKMod.getConfig().xpOnBlackFlash(), player);
    }

    public void onDailyLogin(PlayerData data, ServerPlayerEntity player) {
        if (JJKMod.getInstance() == null) return;
        int today = (int) java.time.LocalDate.now().toEpochDay();
        if (data.lastLoginDay == today) return;
        data.lastLoginDay = today;
        addXp(data, JJKMod.getConfig().xpOnDailyLogin(), player);
    }
}
