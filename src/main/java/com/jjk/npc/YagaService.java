package com.jjk.npc;

import com.jjk.JJKMod;
import com.jjk.api.skill.SkillResult;
import com.jjk.audit.AuditLogger;
import com.jjk.data.PlayerData;
import com.jjk.economy.CursedStoneManager;
import com.jjk.network.c2s.NpcServiceC2SPacket;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;

public final class YagaService {

    private static final Map<Integer, Long> ENHANCE_COSTS = Map.of(
        1, 1000L,
        2, 2500L,
        3, 5000L
    );

    public static SkillResult handle(NpcServiceC2SPacket packet, PlayerData data,
                                      ServerPlayerEntity player, long tick) {
        return handle(packet, data, player, tick, JJKMod.getCursedStoneManager());
    }

    public static SkillResult handle(NpcServiceC2SPacket packet, PlayerData data,
                                      ServerPlayerEntity player, long tick,
                                      CursedStoneManager csm) {
        if (csm == null) return SkillResult.FAIL;
        return switch (packet.action()) {
            case "enhance"        -> enhance(packet.param(), data, player, csm);
            case "craft_ce_potion"-> craftCePotion(data, player, csm);
            default               -> SkillResult.FAIL;
        };
    }

    private static SkillResult enhance(String toolId, PlayerData data,
                                        ServerPlayerEntity player,
                                        CursedStoneManager csm) {
        if (toolId == null || toolId.isEmpty()) return SkillResult.FAIL;
        String enhKey = "tool_enhance_" + toolId;
        int currentLevel = data.cooldowns.getOrDefault(enhKey, 0L).intValue();

        if (currentLevel >= 3) return SkillResult.FAIL;

        Long cost = ENHANCE_COSTS.get(currentLevel + 1);
        if (cost == null) return SkillResult.FAIL;

        if (!csm.spend(data, cost, "enhance_" + toolId, player)) {
            return SkillResult.CE_INSUFFICIENT;
        }

        data.cooldowns.put(enhKey, (long) (currentLevel + 1));
        JJKMod.getPlayerRepository().save(data);

        AuditLogger auditLogger = JJKMod.getAuditLogger();
        if (auditLogger != null) {
            auditLogger.logEvent("tool_enhanced", data.uuid,
                String.format("{\"toolId\":\"%s\",\"level\":%d}", toolId, currentLevel + 1), 0L);
        }
        return SkillResult.SUCCESS;
    }

    private static SkillResult craftCePotion(PlayerData data, ServerPlayerEntity player,
                                              CursedStoneManager csm) {
        if (!csm.spend(data, 500L, "craft_ce_potion", player)) {
            return SkillResult.CE_INSUFFICIENT;
        }
        ItemStack potion = ZeninService.createCePotion();
        if (player != null && !player.getInventory().insertStack(potion)) {
            player.dropItem(potion, false);
        }
        return SkillResult.SUCCESS;
    }

    private YagaService() {}
}
