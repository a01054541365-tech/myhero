package com.jjk.quest;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import com.jjk.item.CursedCrystalItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * QuestRegistry 기반 퀘스트 진행 추적.
 * QuestManager.progress() 에서 호출 — 별도 이벤트 훅 불필요.
 */
public final class QuestTracker {

    private static final int EPOCH_WEEK = 7;

    /** conditionType 이벤트 발생 시 호출. 보상은 자동 지급. */
    public static void track(PlayerData data, String conditionType, ServerPlayerEntity player) {
        long today = System.currentTimeMillis() / 86400000L;

        // 일일 초기화
        if (data.lastQuestResetDay != today) {
            data.questProgress.clear();
            data.completedDailyQuests.clear();
            data.lastQuestResetDay = today;
        }

        long thisWeek = System.currentTimeMillis() / (86400000L * EPOCH_WEEK);
        boolean weekChanged = !data.completedWeeklyQuests.isEmpty()
            && data.lastQuestResetDay / EPOCH_WEEK != thisWeek;
        if (weekChanged) {
            data.completedWeeklyQuests.clear();
        }

        for (QuestDefinition quest : QuestRegistry.getAll()) {
            if (!quest.condition().type().equals(conditionType)) continue;

            if (quest.type() == QuestType.DAILY) {
                if (data.completedDailyQuests.contains(quest.id())) continue;
                int prog = data.questProgress.getOrDefault(quest.id(), 0) + 1;
                data.questProgress.put(quest.id(), prog);
                if (prog >= quest.condition().target()) {
                    data.completedDailyQuests.add(quest.id());
                    grantReward(quest, data, player);
                }
            } else {
                if (data.completedWeeklyQuests.contains(quest.id())) continue;
                int prog = data.questProgress.getOrDefault(quest.id(), 0) + 1;
                data.questProgress.put(quest.id(), prog);
                if (prog >= quest.condition().target()) {
                    data.completedWeeklyQuests.add(quest.id());
                    grantReward(quest, data, player);
                }
            }
        }
    }

    private static void grantReward(QuestDefinition quest, PlayerData data,
                                     ServerPlayerEntity player) {
        if (JJKMod.getGradeManager() != null) {
            JJKMod.getGradeManager().addXp(data, quest.rewardXp(), player);
        }
        if (player != null && CursedCrystalItem.INSTANCE != null && quest.rewardCrystals() > 0) {
            ItemStack crystals = new ItemStack(CursedCrystalItem.INSTANCE, quest.rewardCrystals());
            if (!player.getInventory().insertStack(crystals)) {
                player.dropItem(crystals, false);
            }
            player.sendMessage(Text.literal("[JJK] 퀘스트 완료: " + quest.name()
                + " (XP +" + quest.rewardXp() + ", 결정체 +" + quest.rewardCrystals() + ")"), false);
        }
        JJKMod.getPlayerRepository().save(data);
    }

    private QuestTracker() {}
}
