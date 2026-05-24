package com.jjk.effect;

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

    public boolean canNeutralize(ServerPlayerEntity attacker, ServerPlayerEntity target) {
        if (!isInfinityActive(target)) return false;
        // TODO: check neutralization conditions (special grade hit, binding vow timeout, etc.)
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
