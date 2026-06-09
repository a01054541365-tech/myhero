package com.jjk.effect;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 무한(Infinity) 명중 무효화 판정 처리 — CombatPipeline 6단계(방어·저항 처리)에서 호출
public class InfinityHandler {

    private final Map<UUID, Boolean> infinityState = new HashMap<>();

    public boolean isInfinityActive(ServerPlayerEntity player) {
        return infinityState.getOrDefault(player.getUuid(), false);
    }

    // §22-1: infinity is nullified when the attacker stands inside an active domain,
    // or when megumi's mahoraga_hit_count reaches maharagaThreshold.
    public boolean canNeutralize(ServerPlayerEntity attacker, ServerPlayerEntity target) {
        if (!isInfinityActive(target)) return false;
        if (JJKMod.getDomainManager().getDomainAt(attacker.getBlockPos()).isPresent()) return true;

        // 마허라가 의식 적응 — 메구미 전용
        PlayerData attackerData = JJKMod.getPlayerRepository().load(attacker.getUuid());
        if ("megumi".equals(attackerData.characterId)) {
            int maharagaCount = attackerData.cooldowns.getOrDefault("mahoraga_hit_count", 0L).intValue();
            if (maharagaCount >= JJKMod.getConfig().maharagaThreshold) return true;
        }
        return false;
    }

    public void setInfinity(ServerPlayerEntity player, boolean active) {
        if (active) {
            infinityState.put(player.getUuid(), true);
        } else {
            infinityState.remove(player.getUuid());
        }
    }
}
