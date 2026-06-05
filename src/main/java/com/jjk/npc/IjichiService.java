package com.jjk.npc;

import com.jjk.JJKMod;
import com.jjk.api.skill.SkillResult;
import com.jjk.data.PlayerData;
import com.jjk.economy.CursedStoneManager;
import com.jjk.network.c2s.NpcServiceC2SPacket;
import com.jjk.network.s2c.NpcOpenGuiS2CPacket;
import com.jjk.quest.QuestDef;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class IjichiService {

    public static SkillResult handle(NpcServiceC2SPacket packet, PlayerData data,
                                      ServerPlayerEntity player, long tick) {
        return handle(packet, data, player, tick, JJKMod.getCursedStoneManager());
    }

    public static SkillResult handle(NpcServiceC2SPacket packet, PlayerData data,
                                      ServerPlayerEntity player, long tick,
                                      CursedStoneManager csm) {
        if (csm == null) return SkillResult.FAIL;
        return switch (packet.action()) {
            case "get_quest"        -> getQuest(data, player);
            case "get_weekly_quest" -> getWeeklyQuest(data, player);
            case "teleport"         -> teleport(packet.param(), data, player, csm);
            default                 -> SkillResult.FAIL;
        };
    }

    private static SkillResult getQuest(PlayerData data, ServerPlayerEntity player) {
        int today = (int) (System.currentTimeMillis() / 86400000L);
        long doneDay = data.cooldowns.getOrDefault("quest_daily_done", -1L);
        if (doneDay == today) return SkillResult.FAIL;

        QuestDef quest = JJKMod.getQuestManager().getDailyQuest(data.uuid, today);

        if (player != null) {
            ServerPlayNetworking.send(player,
                new NpcOpenGuiS2CPacket("ijichi",
                    String.format("{\"quest\":\"%s\",\"target\":%d}",
                        quest.description(), quest.target())));
        }
        return SkillResult.SUCCESS;
    }

    private static SkillResult getWeeklyQuest(PlayerData data, ServerPlayerEntity player) {
        int thisWeek = com.jjk.quest.QuestManager.currentEpochWeek();
        if (data.weeklyQuestDone == thisWeek) return SkillResult.FAIL;

        QuestDef quest = JJKMod.getQuestManager().getWeeklyQuest(data.uuid, thisWeek);
        int prog = data.cooldowns.getOrDefault("quest_weekly_prog", 0L).intValue();

        if (player != null) {
            ServerPlayNetworking.send(player,
                new NpcOpenGuiS2CPacket("ijichi",
                    String.format("{\"stones\":%d,\"questDesc\":\"%s\","
                        + "\"questTarget\":%d,\"questProg\":%d,"
                        + "\"weeklyQuestDesc\":\"%s\",\"weeklyTarget\":%d,"
                        + "\"weeklyProg\":%d,\"weeklyDone\":%b}",
                        data.cursedStones,
                        JJKMod.getQuestManager().getDailyQuest(data.uuid,
                            (int)(System.currentTimeMillis() / 86400000L)).description(),
                        JJKMod.getQuestManager().getDailyQuest(data.uuid,
                            (int)(System.currentTimeMillis() / 86400000L)).target(),
                        data.cooldowns.getOrDefault("quest_daily_prog", 0L).intValue(),
                        quest.description(), quest.target(), prog,
                        data.weeklyQuestDone == thisWeek)));
        }
        return SkillResult.SUCCESS;
    }

    private static SkillResult teleport(String dest, PlayerData data,
                                         ServerPlayerEntity player,
                                         CursedStoneManager csm) {
        if (!csm.spend(data, 300L, "teleport_" + dest, player)) {
            return SkillResult.CE_INSUFFICIENT;
        }
        Vec3d pos = switch (dest != null ? dest : "") {
            case "training"  -> JJKMod.getConfig().trainingRoomPos();
            case "infirmary" -> JJKMod.getConfig().infirmaryPos();
            case "storage"   -> JJKMod.getConfig().storagePos();
            case "entrance"  -> JJKMod.getConfig().entrancePos();
            default          -> null;
        };
        if (pos == null) {
            csm.give(data, 300L, "teleport_refund", null);
            return SkillResult.FAIL;
        }
        if (player != null) {
            player.requestTeleport(pos.x, pos.y, pos.z);
        }
        return SkillResult.SUCCESS;
    }

    private IjichiService() {}
}
