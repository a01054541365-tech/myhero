package com.jjk.npc;

import com.jjk.JJKMod;
import com.jjk.api.skill.SkillResult;
import com.jjk.audit.AuditLogger;
import com.jjk.data.PlayerData;
import com.jjk.economy.CursedStoneManager;
import com.jjk.network.c2s.NpcServiceC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;

public final class KusakabeService {

    private static long getTrainingCost(int currentMastery) {
        if (currentMastery <= 3) return 500L;
        if (currentMastery <= 6) return 1200L;
        return 2500L;
    }

    private static long getResetCost(int resetCount) {
        if (resetCount == 0) return 0L;
        if (resetCount == 1) return 1500L;
        return 5000L;
    }

    public static SkillResult handle(NpcServiceC2SPacket packet, PlayerData data,
                                      ServerPlayerEntity player, long tick) {
        return handle(packet, data, player, tick, JJKMod.getCursedStoneManager());
    }

    public static SkillResult handle(NpcServiceC2SPacket packet, PlayerData data,
                                      ServerPlayerEntity player, long tick,
                                      CursedStoneManager csm) {
        if (csm == null) return SkillResult.FAIL;
        return switch (packet.action()) {
            case "train" -> train(data, player, csm);
            case "reset" -> reset(data, player, csm);
            default      -> SkillResult.FAIL;
        };
    }

    private static SkillResult train(PlayerData data, ServerPlayerEntity player,
                                      CursedStoneManager csm) {
        if (data.mastery >= 10) return SkillResult.FAIL;

        long cost = getTrainingCost(data.mastery);
        if (!csm.spend(data, cost, "mastery_train", player)) {
            return SkillResult.CE_INSUFFICIENT;
        }
        data.mastery++;
        JJKMod.getPlayerRepository().saveImmediate(data);
        return SkillResult.SUCCESS;
    }

    private static SkillResult reset(PlayerData data, ServerPlayerEntity player,
                                      CursedStoneManager csm) {
        long cost = getResetCost(data.masteryResetCount);
        if (cost > 0 && !csm.spend(data, cost, "mastery_reset", player)) {
            return SkillResult.CE_INSUFFICIENT;
        }
        data.mastery = 0;
        data.masteryResetCount++;
        JJKMod.getPlayerRepository().saveImmediate(data);
        AuditLogger auditLogger = JJKMod.getAuditLogger();
        if (auditLogger != null) {
            auditLogger.logEvent("mastery_reset", data.uuid,
                String.format("{\"resetCount\":%d}", data.masteryResetCount), 0L);
        }
        return SkillResult.SUCCESS;
    }

    private KusakabeService() {}
}
