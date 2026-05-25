package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.CooldownManager;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public class OkkotsuSkillSet implements ISkillSet {

    // key 0: rika_summon, 1: sword_slash, 2: copy_technique, 3: rika_burst, 4: reverse_cursed_technique
    private static final int CE_0 = 400, CD_0 = 60,  ANIM_0 = 50;
    private static final int CE_1 = 80,  CD_1 = 5,   ANIM_1 = 51;
    private static final int CE_2 = 300, CD_2 = 120,  ANIM_2 = 52;
    private static final int CE_3 = 600, CD_3 = 180,  ANIM_3 = 53;
    private static final int CE_4 = 120, CD_4 = 25,   ANIM_4 = 54;

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
            case 0 -> "rika_summon"; case 1 -> "sword_slash"; case 2 -> "copy_technique";
            case 3 -> "rika_burst"; case 4 -> "reverse_cursed_technique"; default -> "unknown";
        };
    }

    private static String cdKey(int keyId) { return "cd_okkotsu_" + keyId; }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        var pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }

    private SkillResult useRikaSummon(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useSwordSlash(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useCopyTechnique(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useRikaBurst(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useRCT(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
}
