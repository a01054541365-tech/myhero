package com.jjk.grade;

import com.jjk.JJKMod;
import com.jjk.advancement.AdvancementTriggerManager;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class MasterySystem {

    private static final int MASTERY_MILESTONE = 10;
    private static final float CE_CONTROL_BONUS = 0.10f;
    private static final float CE_CONTROL_MAX   = 2.0f;

    /**
     * 숙련도 증가. Lv.10 달성 시 ceControl +0.10 보너스.
     * 호출자가 saveImmediate를 대신할 경우 saveNow=false로 지정 가능.
     */
    public static void awardMastery(PlayerData data, int amount, ServerPlayerEntity player) {
        if (data == null || amount <= 0) return;
        int before = data.mastery;
        data.mastery += amount;
        if (before < MASTERY_MILESTONE && data.mastery >= MASTERY_MILESTONE) {
            data.ceControl = Math.min(data.ceControl + CE_CONTROL_BONUS, CE_CONTROL_MAX);
            if (player != null) {
                player.sendMessage(
                    Text.literal("[JJK] 술식 완전 숙달! 주력 조작 수치가 증가했습니다. ("
                        + String.format("%.2f", data.ceControl) + ")"), false);
                AdvancementTriggerManager.onMasteryReach(player, data.mastery);
            }
        }
        if (player != null) {
            JJKMod.getPlayerRepository().saveImmediate(data);
        }
    }

    private MasterySystem() {}
}
