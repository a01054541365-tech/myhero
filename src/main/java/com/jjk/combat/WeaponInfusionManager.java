package com.jjk.combat;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.WeaponInfusionS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.server.network.ServerPlayerEntity;

public class WeaponInfusionManager {

    public enum InfusionResult {
        SUCCESS, DISABLED, NOT_CHARACTER, NOT_SORCERER, INVALID_WEAPON, NO_CE
    }

    private static final float[] CE_COST = {80f, 120f, 180f, 260f, 360f, 480f};
    private static final int[]   DURATION = {100, 160, 240, 320, 400, 500};
    private static final float[] DMG_MULT = {1.15f, 1.25f, 1.35f, 1.45f, 1.55f, 1.65f};

    private final JjkConfig config;

    public WeaponInfusionManager(JjkConfig config) {
        this.config = config;
    }

    public InfusionResult tryInfuse(ServerPlayerEntity player) {
        if (!config.weaponInfusionEnabled) return InfusionResult.DISABLED;

        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());

        if (data.characterId == null) return InfusionResult.NOT_CHARACTER;
        if (data.ceMax <= 0f) return InfusionResult.NOT_SORCERER;

        boolean hasValidWeapon = player.getMainHandStack().getItem() instanceof SwordItem
                || player.getMainHandStack().getItem() instanceof AxeItem;
        if (!hasValidWeapon) return InfusionResult.INVALID_WEAPON;

        int gradeOrdinal = gradeOrdinal(data);
        float cost = CE_COST[gradeOrdinal];
        boolean consumed = JJKMod.getCEManager().consumeCE(data, cost);
        if (!consumed) return InfusionResult.NO_CE;

        int duration = DURATION[gradeOrdinal];
        data.infusionEndTick = player.getWorld().getTime() + duration;
        JJKMod.getPlayerRepository().save(data);
        ServerPlayNetworking.send(player, new WeaponInfusionS2CPacket(duration, DMG_MULT[gradeOrdinal]));
        return InfusionResult.SUCCESS;
    }

    public boolean isActive(PlayerData data, long currentTick) {
        return data.infusionEndTick > 0 && currentTick < data.infusionEndTick;
    }

    public float getMultiplier(PlayerData data, long currentTick) {
        if (!isActive(data, currentTick)) return 1.0f;
        return DMG_MULT[gradeOrdinal(data)];
    }

    public void forceOn(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        int g = gradeOrdinal(data);
        data.infusionEndTick = player.getWorld().getTime() + 6000; // 강제 5분
        JJKMod.getPlayerRepository().saveImmediate(data);
        ServerPlayNetworking.send(player, new WeaponInfusionS2CPacket(6000, DMG_MULT[g]));
    }

    public void forceOff(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        data.infusionEndTick = -1L;
        JJKMod.getPlayerRepository().saveImmediate(data);
    }

    private static int gradeOrdinal(PlayerData data) {
        if (data.grade == null) return 0;
        return Math.min(data.grade.ordinal(), DMG_MULT.length - 1);
    }
}
