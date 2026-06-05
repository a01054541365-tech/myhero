package com.jjk.npc;

import com.jjk.JJKMod;
import com.jjk.api.skill.SkillResult;
import com.jjk.data.PlayerData;
import com.jjk.economy.CursedStoneManager;
import com.jjk.network.c2s.NpcServiceC2SPacket;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;

public final class ShokoService {

    // action → [비용, 쿨타임틱]
    private static final Map<String, long[]> SERVICES = Map.of(
        "heal_half",    new long[]{200L, 6000L},
        "heal_full",    new long[]{600L, 36000L},
        "ce_fill",      new long[]{300L, 12000L},
        "full_package", new long[]{800L, 72000L},
        "status_clear", new long[]{400L, 0L}
    );

    public static SkillResult handle(NpcServiceC2SPacket packet, PlayerData data,
                                      ServerPlayerEntity player, long tick) {
        return handle(packet, data, player, tick, JJKMod.getCursedStoneManager());
    }

    public static SkillResult handle(NpcServiceC2SPacket packet, PlayerData data,
                                      ServerPlayerEntity player, long tick,
                                      CursedStoneManager csm) {
        if (csm == null) return SkillResult.FAIL;
        String action = packet.action();
        long[] sv = SERVICES.get(action);
        if (sv == null) return SkillResult.FAIL;

        long cost    = sv[0];
        long cdTicks = sv[1];
        String cdKey = "npc_shoko_" + action;

        if (cdTicks > 0) {
            Long expiry = data.cooldowns.get(cdKey);
            if (expiry != null && expiry > tick) return SkillResult.ON_COOLDOWN;
        }

        if (!csm.spend(data, cost, "shoko_" + action, player)) {
            return SkillResult.CE_INSUFFICIENT;
        }

        switch (action) {
            case "heal_half"    -> data.hpCurrent = data.hpMax * 0.5f;
            case "heal_full"    -> data.hpCurrent = data.hpMax;
            case "ce_fill"      -> data.ceCurrent = data.ceMax;
            case "full_package" -> { data.hpCurrent = data.hpMax; data.ceCurrent = data.ceMax; }
            case "status_clear" -> { data.cooldowns.remove("skill_seal"); data.burden = 0; }
        }

        if (cdTicks > 0) data.cooldowns.put(cdKey, tick + cdTicks);

        JJKMod.getPlayerRepository().saveImmediate(data);
        return SkillResult.SUCCESS;
    }

    private ShokoService() {}
}
