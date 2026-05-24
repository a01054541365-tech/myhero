package com.jjk.character.impl;

import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import net.minecraft.server.network.ServerPlayerEntity;

public class OkkotsuSkillSet implements ISkillSet {

    // key 0: rika_summon, 1: sword_slash, 2: copy_technique, 3: rika_burst, 4: reverse_cursed_technique

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useRikaSummon(player);
            case 1 -> useSwordSlash(player);
            case 2 -> useCopyTechnique(player);
            case 3 -> useRikaBurst(player);
            case 4 -> useRCT(player);
            default -> SkillResult.FAIL;
        };
    }

    @Override public boolean canUse(ServerPlayerEntity player, int keyId) { return true; }
    @Override public int getCooldownTicks(int keyId) { return 0; }
    @Override public int getCeCost(int keyId) { return 0; }
    @Override public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "rika_summon"; case 1 -> "sword_slash"; case 2 -> "copy_technique";
            case 3 -> "rika_burst"; case 4 -> "reverse_cursed_technique"; default -> "unknown";
        };
    }

    private SkillResult useRikaSummon(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useSwordSlash(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useCopyTechnique(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useRikaBurst(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useRCT(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
}
