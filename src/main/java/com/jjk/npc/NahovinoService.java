package com.jjk.npc;

import com.jjk.JJKMod;
import com.jjk.api.skill.SkillResult;
import com.jjk.data.PlayerData;
import com.jjk.grade.GradeManager.Grade;
import com.jjk.economy.CursedStoneManager;
import com.jjk.network.c2s.NpcServiceC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;

public final class NahovinoService {

    public static SkillResult handle(NpcServiceC2SPacket packet, PlayerData data,
                                      ServerPlayerEntity player, long tick) {
        return handle(packet, data, player, tick, JJKMod.getCursedStoneManager());
    }

    public static SkillResult handle(NpcServiceC2SPacket packet, PlayerData data,
                                      ServerPlayerEntity player, long tick,
                                      CursedStoneManager csm) {
        if (csm == null) return SkillResult.FAIL;
        return switch (packet.action()) {
            case "train_atk"  -> trainStat(data, player, "atk", 800L,
                                     "stat_atk_bonus", 5, 2, Grade.GRADE_1.rank, csm);
            case "train_def"  -> trainStat(data, player, "def", 800L,
                                     "stat_def_bonus", 5, 2, Grade.GRADE_1.rank, csm);
            case "train_spd"  -> trainStat(data, player, "spd", 600L,
                                     "stat_spd_bonus", 5, 1, Grade.GRADE_2.rank, csm);
            case "reset_stats"-> resetStats(data, player, csm);
            default           -> SkillResult.FAIL;
        };
    }

    private static SkillResult trainStat(PlayerData data, ServerPlayerEntity player,
                                          String statType, long cost,
                                          String bonusKey, int maxCount,
                                          int bonusPerTrain, int requiredGrade,
                                          CursedStoneManager csm) {
        if (data.grade == null || data.grade.ordinal() < requiredGrade) {
            return SkillResult.FAIL;
        }

        int current = data.cooldowns.getOrDefault(bonusKey, 0L).intValue();
        if (current >= maxCount) return SkillResult.FAIL;

        if (!csm.spend(data, cost, "train_" + statType, player)) {
            return SkillResult.CE_INSUFFICIENT;
        }

        switch (statType) {
            case "atk" -> data.attackStat  += bonusPerTrain;
            case "def" -> data.defenseStat += bonusPerTrain;
            case "spd" -> data.speedStat   += bonusPerTrain;
        }
        data.cooldowns.put(bonusKey, (long) (current + 1));
        JJKMod.getPlayerRepository().saveImmediate(data);
        return SkillResult.SUCCESS;
    }

    private static SkillResult resetStats(PlayerData data, ServerPlayerEntity player,
                                           CursedStoneManager csm) {
        if (data.grade == null || data.grade != com.jjk.data.Grade.SPECIAL) {
            return SkillResult.FAIL;
        }
        if (!csm.spend(data, 3000L, "stat_reset", player)) {
            return SkillResult.CE_INSUFFICIENT;
        }
        // 기본값 복원 (createDefault 기준: 10)
        data.attackStat  = 10;
        data.defenseStat = 10;
        data.cooldowns.remove("stat_atk_bonus");
        data.cooldowns.remove("stat_def_bonus");
        data.cooldowns.remove("stat_spd_bonus");
        JJKMod.getPlayerRepository().saveImmediate(data);
        return SkillResult.SUCCESS;
    }

    private NahovinoService() {}
}
