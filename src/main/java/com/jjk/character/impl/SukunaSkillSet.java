package com.jjk.character.impl;

import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import net.minecraft.server.network.ServerPlayerEntity;

// Phase 3 ??쎈???5揶???쎄텢 筌뤴뫀紐?NOT_IMPLEMENTED
public class SukunaSkillSet implements ISkillSet {

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return SkillResult.NOT_IMPLEMENTED;
    }

    @Override public boolean canUse(ServerPlayerEntity player, int keyId) { return false; }
    @Override public int getCooldownTicks(int keyId) { return 0; }
    @Override public int getCeCost(int keyId) { return 0; }
    @Override public String getSkillName(int keyId) { return "not_implemented"; }
}
