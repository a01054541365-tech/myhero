package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.CooldownManager;
import com.jjk.data.PlayerData;
import com.jjk.entity.ShikigamiEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;

import java.util.UUID;

public class MegumiSkillSet implements ISkillSet {

    private static final String SHIKIGAMI_SILKWORM   = "silkworm";
    private static final String SHIKIGAMI_WHITE_DOG  = "white_dog";
    private static final String SHIKIGAMI_RABBIT_MASK = "rabbit_mask";

    private float shikigamiDmgBoost = 1.0f;

    // key 0: nue, 1: divine_dog, 2: mahoraga_adaptation, 3: chimera_shadow_garden, 4: shadow_move
    private static final int CE_0 = 200,  CD_0 = 20;
    private static final int CE_1 = 180,  CD_1 = 16;
    private static final int CE_2 = 0,    CD_2 = 300;
    private static final int CE_3 = 3000, CD_3 = 600;
    private static final int CE_4 = 100,  CD_4 = 8;

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

    @Override
    public boolean canUse(ServerPlayerEntity player, int keyId) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        return CooldownManager.isReady(data, cdKey(keyId), tick)
                && JJKMod.getCEManager().canAfford(player, getCeCost(keyId));
    }

    @Override
    public int getCooldownTicks(int keyId) {
        return switch (keyId) {
            case 0 -> CD_0; case 1 -> CD_1; case 2 -> CD_2;
            case 3 -> CD_3; case 4 -> CD_4; default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) {
        return switch (keyId) {
            case 0 -> CE_0; case 1 -> CE_1; case 2 -> CE_2;
            case 3 -> CE_3; case 4 -> CE_4; default -> 0;
        };
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "nue"; case 1 -> "divine_dog"; case 2 -> "mahoraga_adaptation";
            case 3 -> "chimera_shadow_garden"; case 4 -> "shadow_move"; default -> "unknown";
        };
    }

    public void onWhiteDogDeath() {
        shikigamiDmgBoost = 1.5f;
    }

    public float getShikigamiDmgBoost() { return shikigamiDmgBoost; }

    private static String cdKey(int keyId) { return "cd_megumi_" + keyId; }

    private SkillResult useNue(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data.deadShikigamiIds.contains(SHIKIGAMI_SILKWORM)) return SkillResult.FAIL;
        if (countActiveShikigami(player) >= 3) return SkillResult.FAIL;
        return SkillResult.NOT_IMPLEMENTED;
    }

    private SkillResult useDivineDog(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data.deadShikigamiIds.contains(SHIKIGAMI_WHITE_DOG)) return SkillResult.FAIL;
        if (countActiveShikigami(player) >= 3) return SkillResult.FAIL;
        return SkillResult.NOT_IMPLEMENTED;
    }

    private int countActiveShikigami(ServerPlayerEntity player) {
        UUID ownerUuid = player.getUuid();
        Box box = player.getBoundingBox().expand(64);
        return player.getServerWorld()
            .getEntitiesByClass(ShikigamiEntity.class, box,
                e -> ownerUuid.equals(e.getOwnerUuid()))
            .size();
    }
    private SkillResult useMaharagaAdaptation(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useChimeraShadowGarden(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useShadowMove(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
}
