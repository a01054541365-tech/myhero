package com.jjk.character.impl;

import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import net.minecraft.server.network.ServerPlayerEntity;

public class ItadoriSkillSet implements ISkillSet {

    // key 0: divergent_fist, 1: manji_kick, 2: black_flash_focus, 3: domain_startup, 4: rct

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useDivergentFist(player);
            case 1 -> useManjiKick(player);
            case 2 -> useBlackFlashFocus(player);
            case 3 -> useDomainStartup(player);
            case 4 -> useRCT(player);
            default -> SkillResult.FAIL;
        };
    }

    @Override public boolean canUse(ServerPlayerEntity player, int keyId) { return true; }
    @Override public int getCooldownTicks(int keyId) { return 0; }
    @Override public int getCeCost(int keyId) { return 0; }
    @Override public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "divergent_fist"; case 1 -> "manji_kick"; case 2 -> "black_flash_focus";
            case 3 -> "domain_startup"; case 4 -> "rct"; default -> "unknown";
        };
    }

    private SkillResult useDivergentFist(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useManjiKick(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useBlackFlashFocus(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useDomainStartup(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useRCT(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
}
