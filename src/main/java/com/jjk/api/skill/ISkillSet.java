package com.jjk.api.skill;

import net.minecraft.server.network.ServerPlayerEntity;

public interface ISkillSet {
    SkillResult use(ServerPlayerEntity player, int keyId);
    boolean canUse(ServerPlayerEntity player, int keyId);
    int getCooldownTicks(int keyId);
    int getCeCost(int keyId);
    String getSkillName(int keyId);
}
