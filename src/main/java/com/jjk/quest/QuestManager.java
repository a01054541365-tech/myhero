package com.jjk.quest;

import com.jjk.JJKMod;
import com.jjk.audit.AuditLogger;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.UUID;


public final class QuestManager {

    private static final List<QuestDef> DAILY_POOL = List.of(
        new QuestDef("patrol",   "주령 5마리 처치",    "kill_spirit", 5,  150L, 50,  false),
        new QuestDef("training", "흑섬 3회 발동",      "black_flash", 3,  100L, 30,  false),
        new QuestDef("intel",    "PvP 전투 1회",       "pvp",         1,   80L, 20,  false),
        new QuestDef("tool_use", "주구 장착 전투 3승", "tool_win",    3,  120L, 40,  false)
    );

    private static final List<QuestDef> WEEKLY_POOL = List.of(
        new QuestDef("grade_up",  "1등급 상승",            "grade_up",    1,  500L, 200, true),
        new QuestDef("boss_kill", "특급 주령 처치 1회",    "kill_special",1,  800L, 300, true),
        new QuestDef("ce_spend",  "CE 1,000,000 누적 소모","ce_spend",1000000,400L,150, true),
        new QuestDef("top_fight", "최고 등급과 전투",      "top_pvp",     1,  600L, 250, true)
    );

    public QuestDef getDailyQuest(UUID playerUuid, int epochDay) {
        int idx = Math.abs(playerUuid.hashCode() ^ epochDay) % DAILY_POOL.size();
        return DAILY_POOL.get(idx);
    }

    public QuestDef getWeeklyQuest(UUID playerUuid, int epochWeek) {
        int idx = Math.abs(playerUuid.hashCode() ^ epochWeek) % WEEKLY_POOL.size();
        return WEEKLY_POOL.get(idx);
    }

    public void progress(PlayerData data, String type, ServerPlayerEntity player, long tick) {
        int epochDay = (int) (System.currentTimeMillis() / 86400000L);
        long doneDay = data.cooldowns.getOrDefault("quest_daily_done", -1L);
        if (doneDay == epochDay) return;

        QuestDef quest = getDailyQuest(data.uuid, epochDay);
        if (!quest.type().equals(type)) return;

        String progKey = "quest_daily_prog";
        long current = data.cooldowns.getOrDefault(progKey, 0L);
        long newProg = current + 1;
        data.cooldowns.put(progKey, newProg);

        if (newProg >= quest.target()) {
            completeDailyQuest(data, quest, player);
        } else {
            JJKMod.getPlayerRepository().save(data);
        }
    }

    // ── 주간 퀘스트 ──────────────────────────────────────────────────────────────

    public static int currentEpochWeek() {
        return (int)(System.currentTimeMillis() / (86400000L * 7));
    }

    public void progressWeekly(PlayerData data, String type, ServerPlayerEntity player) {
        int thisWeek = currentEpochWeek();
        if (data.weeklyQuestDone == thisWeek) return;

        QuestDef quest = getWeeklyQuest(data.uuid, thisWeek);
        if (!quest.type().equals(type)) return;

        String progKey = "quest_weekly_prog";

        if ("ce_spend".equals(type)) {
            long total = data.cooldowns.getOrDefault("ce_spend_total", 0L);
            if (total >= quest.target()) {
                completeWeeklyQuest(data, quest, player);
            }
            return;
        }

        long current = data.cooldowns.getOrDefault(progKey, 0L);
        long newProg = current + 1;
        data.cooldowns.put(progKey, newProg);

        if (newProg >= quest.target()) {
            completeWeeklyQuest(data, quest, player);
        } else {
            JJKMod.getPlayerRepository().save(data);
        }
    }

    private void completeWeeklyQuest(PlayerData data, QuestDef quest,
                                      ServerPlayerEntity player) {
        data.cooldowns.remove("quest_weekly_prog");
        data.weeklyQuestDone = currentEpochWeek();

        if (JJKMod.getCursedStoneManager() != null) {
            JJKMod.getCursedStoneManager()
                .give(data, quest.stoneReward(), "weekly_" + quest.questId(), player);
        }
        if (JJKMod.getGradeManager() != null) {
            JJKMod.getGradeManager().addXp(data, quest.xpReward(), player);
        }

        AuditLogger auditLogger = JJKMod.getAuditLogger();
        if (auditLogger != null) {
            auditLogger.logEvent("weekly_quest_complete", data.uuid,
                String.format("{\"questId\":\"%s\"}", quest.questId()), 0L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void completeDailyQuest(PlayerData data, QuestDef quest,
                                     ServerPlayerEntity player) {
        data.cooldowns.remove("quest_daily_prog");
        data.cooldowns.put("quest_daily_done",
            (long) (System.currentTimeMillis() / 86400000L));

        if (JJKMod.getCursedStoneManager() != null) {
            JJKMod.getCursedStoneManager()
                .give(data, quest.stoneReward(), "quest_" + quest.questId(), player);
        }
        if (JJKMod.getGradeManager() != null) {
            JJKMod.getGradeManager().addXp(data, quest.xpReward(), player);
        }

        AuditLogger auditLogger = JJKMod.getAuditLogger();
        if (auditLogger != null) {
            auditLogger.logEvent("quest_complete", data.uuid,
                String.format("{\"questId\":\"%s\"}", quest.questId()), 0L);
        }
    }
}
