package com.jjk.character.impl;

import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import net.minecraft.server.network.ServerPlayerEntity;

public class MegumiSkillSet implements ISkillSet {

    // key 0: nue, 1: divine_dog, 2: mahoraga_adaptation, 3: chimera_shadow_garden, 4: shadow_move

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useNue(player);
            case 1 -> useDivineDog(player);
            case 2 -> useMaharagaAdaptation(player);
            case 3 -> useChimeraShadowGarden(player);
            case 4 -> useShadowMove(player);
            default -> SkillResult.FAIL;
        };
    }

    @Override public boolean canUse(ServerPlayerEntity player, int keyId) { return true; }
    @Override public int getCooldownTicks(int keyId) { return 0; }
    @Override public int getCeCost(int keyId) { return 0; }
    @Override public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "nue"; case 1 -> "divine_dog"; case 2 -> "mahoraga_adaptation";
            case 3 -> "chimera_shadow_garden"; case 4 -> "shadow_move"; default -> "unknown";
        };
    }

    private SkillResult useNue(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useDivineDog(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useMaharagaAdaptation(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useChimeraShadowGarden(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useShadowMove(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
}
