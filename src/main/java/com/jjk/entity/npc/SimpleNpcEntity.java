package com.jjk.entity.npc;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import com.jjk.grade.GradeManager.Grade;
import com.jjk.item.CursedToolRegistry;
import com.jjk.item.CursedToolItem;
import com.jjk.network.s2c.NpcOpenGuiS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public class SimpleNpcEntity extends NpcEntity {

    public SimpleNpcEntity(EntityType<? extends NpcEntity> type, World world,
                            String npcId, String displayName) {
        super(type, world, npcId);
        setCustomName(Text.literal(displayName));
        setCustomNameVisible(true);
    }

    @Override
    protected void onInteract(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        String payload = buildPayload(getNpcId(), data, player);
        ServerPlayNetworking.send(player, new NpcOpenGuiS2CPacket(getNpcId(), payload));
    }

    private static String buildPayload(String npcId, PlayerData data,
                                        ServerPlayerEntity player) {
        int gradeRank = Grade.fromLabel(data.grade).rank;
        int today = (int)(System.currentTimeMillis() / 86400000L);
        return switch (npcId) {
            case "zenin_storage" -> String.format(
                "{\"stones\":%d,\"gradeRank\":%d}",
                data.cursedStones, gradeRank);
            case "kusakabe" -> String.format(
                "{\"stones\":%d,\"mastery\":%d,\"resetCount\":%d}",
                data.cursedStones, data.mastery, data.masteryResetCount);
            case "shoko" -> String.format(
                "{\"stones\":%d,\"hp\":%.1f,\"maxHp\":%.1f,\"ce\":%.1f,\"maxCe\":%.1f}",
                data.cursedStones, data.hpCurrent, data.hpMax,
                data.ceCurrent, data.ceMax);
            case "gojo_shiyu" -> String.format(
                "{\"stones\":%d,\"bounty\":%d,\"isCursedSpirit\":%b}",
                data.cursedStones, data.bounty,
                JJKMod.getTeamManager().isCursedSpirit(data));
            case "ijichi" -> buildIjichiPayload(data, today);
            case "yaga" -> buildYagaPayload(data, player);
            case "nahobino" -> String.format(
                "{\"stones\":%d,\"gradeRank\":%d," +
                "\"atkBonus\":%d,\"defBonus\":%d,\"spdBonus\":%d," +
                "\"baseAtk\":%d,\"baseDef\":%d}",
                data.cursedStones, gradeRank,
                data.cooldowns.getOrDefault("stat_atk_bonus", 0L).intValue(),
                data.cooldowns.getOrDefault("stat_def_bonus", 0L).intValue(),
                data.cooldowns.getOrDefault("stat_spd_bonus", 0L).intValue(),
                data.attackStat, data.defenseStat);
            default -> "{}";
        };
    }

    private static String buildIjichiPayload(PlayerData data, int today) {
        var quest = JJKMod.getQuestManager().getDailyQuest(data.uuid, today);
        boolean done = data.cooldowns.getOrDefault("quest_daily_done", -1L) == today;
        int prog = data.cooldowns.getOrDefault("quest_daily_prog", 0L).intValue();
        return String.format(
            "{\"stones\":%d,\"questDesc\":\"%s\",\"questTarget\":%d," +
            "\"questProg\":%d,\"questDone\":%b}",
            data.cursedStones, quest.description(), quest.target(), prog, done);
    }

    private static String buildYagaPayload(PlayerData data, ServerPlayerEntity player) {
        String toolId = getMainHandToolId(player);
        int enhLevel = data.cooldowns.getOrDefault("tool_enhance_" + toolId, 0L).intValue();
        return String.format("{\"stones\":%d,\"toolId\":\"%s\",\"enhLevel\":%d}",
            data.cursedStones, toolId, enhLevel);
    }

    private static String getMainHandToolId(ServerPlayerEntity player) {
        if (player == null) return "";
        ItemStack stack = player.getMainHandStack();
        if (stack.getItem() == CursedToolRegistry.CURSED_DAGGER)    return "cursed_dagger";
        if (stack.getItem() == CursedToolRegistry.THOUSAND_SPEAR)   return "thousand_spear";
        if (stack.getItem() == CursedToolRegistry.PLAYFUL_CLOUD)    return "playful_cloud";
        if (stack.getItem() == CursedToolRegistry.INVERTED_SPEAR)   return "inverted_spear";
        if (stack.getItem() == CursedToolRegistry.SPLIT_SOUL_BLADE) return "split_soul_blade";
        return "";
    }
}
