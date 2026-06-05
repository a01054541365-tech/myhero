package com.jjk.npc;

import com.jjk.JJKMod;
import com.jjk.api.skill.SkillResult;
import com.jjk.data.PlayerData;
import com.jjk.economy.CursedStoneManager;
import com.jjk.network.c2s.NpcServiceC2SPacket;
import com.jjk.network.s2c.NpcOpenGuiS2CPacket;
import com.jjk.team.TeamManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class GojoShiyuService {

    public static SkillResult handle(NpcServiceC2SPacket packet, PlayerData data,
                                      ServerPlayerEntity player, long tick) {
        return handle(packet, data, player, tick, JJKMod.getCursedStoneManager());
    }

    public static SkillResult handle(NpcServiceC2SPacket packet, PlayerData data,
                                      ServerPlayerEntity player, long tick,
                                      CursedStoneManager csm) {
        if (csm == null) return SkillResult.FAIL;
        if (TeamManager.getTeam(data.characterId) != TeamManager.Team.CURSED_SPIRIT) {
            return SkillResult.FAIL;
        }
        return switch (packet.action()) {
            case "settle_bounty" -> settleBounty(data, player, csm);
            case "buy_tracker"   -> buyTracker(data, player, tick, csm);
            case "buy_buff"      -> buyBuff(data, player, tick, csm);
            case "buy_info"      -> buyInfo(data, player, csm);
            default              -> SkillResult.FAIL;
        };
    }

    private static SkillResult settleBounty(PlayerData data, ServerPlayerEntity player,
                                             CursedStoneManager csm) {
        long settled = csm.settleBounty(data, player);
        return settled > 0 ? SkillResult.SUCCESS : SkillResult.FAIL;
    }

    private static SkillResult buyTracker(PlayerData data, ServerPlayerEntity player,
                                           long tick, CursedStoneManager csm) {
        if (!csm.spend(data, 2000L, "tracker", player)) {
            return SkillResult.CE_INSUFFICIENT;
        }
        data.cooldowns.put("tracker_active", tick + 600L);
        JJKMod.getPlayerRepository().save(data);
        return SkillResult.SUCCESS;
    }

    private static SkillResult buyBuff(PlayerData data, ServerPlayerEntity player,
                                        long tick, CursedStoneManager csm) {
        if (!csm.spend(data, 1500L, "faction_buff", player)) {
            return SkillResult.CE_INSUFFICIENT;
        }
        data.cooldowns.put("faction_buff_until", tick + 12000L);
        JJKMod.getPlayerRepository().save(data);
        return SkillResult.SUCCESS;
    }

    private static SkillResult buyInfo(PlayerData data, ServerPlayerEntity player,
                                        CursedStoneManager csm) {
        if (!csm.spend(data, 500L, "info", player)) {
            return SkillResult.CE_INSUFFICIENT;
        }
        String topPlayer = JJKMod.getPlayerRepository().findTopGradeSorcerer();
        if (player != null) {
            player.sendMessage(Text.literal("§e최강 주술사: §f" + topPlayer), true);
        }
        return SkillResult.SUCCESS;
    }

    private GojoShiyuService() {}
}
