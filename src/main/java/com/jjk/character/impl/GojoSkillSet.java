package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.CooldownManager;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

public class GojoSkillSet implements ISkillSet {

    // key 0: blue, 1: red, 2: purple, 3: unlimited_void, 4: infinity_toggle, 5: curtain_toggle
    private static final int CE_0 = 150,  CD_0 = 8;
    private static final int CE_1 = 200,  CD_1 = 12;
    private static final int CE_2 = 600,  CD_2 = 60;
    private static final int CE_3 = 3000, CD_3 = 600;
    private static final int CE_4 = 50,   CD_4 = 5;
    private static final int CE_5 = 200,  CD_5 = 60;

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useBlue(player);
            case 1 -> useRed(player);
            case 2 -> usePurple(player);
            case 3 -> useUnlimitedVoid(player);
            case 4 -> toggleInfinity(player);
            case 5 -> toggleCurtain(player);
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
            case 3 -> CD_3; case 4 -> CD_4; case 5 -> CD_5; default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) {
        return switch (keyId) {
            case 0 -> CE_0; case 1 -> CE_1; case 2 -> CE_2;
            case 3 -> CE_3; case 4 -> CE_4; case 5 -> CE_5; default -> 0;
        };
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "blue"; case 1 -> "red"; case 2 -> "purple";
            case 3 -> "unlimited_void"; case 4 -> "infinity_toggle"; case 5 -> "curtain_toggle"; default -> "unknown";
        };
    }

    private static String cdKey(int keyId) { return "cd_gojo_" + keyId; }

    private SkillResult toggleCurtain(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(5), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_5)) return SkillResult.CE_INSUFFICIENT;
        data.curtainActive = !data.curtainActive;
        CooldownManager.set(data, cdKey(5), tick, CD_5);
        JJKMod.getPlayerRepository().save(data);
        JJKMod.getCEManager().consume(player, CE_5);
        return SkillResult.SUCCESS;
    }

    private SkillResult useBlue(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useRed(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult usePurple(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useUnlimitedVoid(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult toggleInfinity(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(4), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_4)) return SkillResult.CE_INSUFFICIENT;
        data.infinityActive = !data.infinityActive;
        CooldownManager.set(data, cdKey(4), tick, CD_4);
        JJKMod.getPlayerRepository().save(data);
        JJKMod.getCEManager().consume(player, CE_4);
        return SkillResult.SUCCESS;
    }
}
