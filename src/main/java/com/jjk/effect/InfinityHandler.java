package com.jjk.effect;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// ?얜똾釉??怨밴묶????뺤쒔 ????????λ뜃由?遺얜┷???紐껋컭筌뤴뫀???袁⑹뒠 ?怨밴묶.
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
