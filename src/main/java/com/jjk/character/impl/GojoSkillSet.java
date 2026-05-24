package com.jjk.character.impl;

import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import net.minecraft.server.network.ServerPlayerEntity;

public class GojoSkillSet implements ISkillSet {

    // key 0: blue, 1: red, 2: purple, 3: unlimited_void, 4: infinity_toggle

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useBlue(player);
            case 1 -> useRed(player);
            case 2 -> usePurple(player);
            case 3 -> useUnlimitedVoid(player);
            case 4 -> toggleInfinity(player);
            default -> SkillResult.FAIL;
        };
    }

    @Override
    public boolean canUse(ServerPlayerEntity player, int keyId) {
        // TODO: CE check + cooldown check
        return true;
    }

    @Override public int getCooldownTicks(int keyId) { return 0; /* TODO: read from config */ }
    @Override public int getCeCost(int keyId) { return 0; /* TODO: read from config */ }
    @Override public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "blue"; case 1 -> "red"; case 2 -> "purple";
            case 3 -> "unlimited_void"; case 4 -> "infinity_toggle"; default -> "unknown";
        };
    }

    private SkillResult useBlue(ServerPlayerEntity player) { /* TODO */ return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useRed(ServerPlayerEntity player) { /* TODO */ return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult usePurple(ServerPlayerEntity player) { /* TODO */ return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useUnlimitedVoid(ServerPlayerEntity player) { /* TODO */ return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult toggleInfinity(ServerPlayerEntity player) { /* TODO */ return SkillResult.NOT_IMPLEMENTED; }
}
