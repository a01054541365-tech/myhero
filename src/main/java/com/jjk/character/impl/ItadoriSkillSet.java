package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.CooldownManager;
import com.jjk.combat.HitValidator;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItadoriSkillSet implements ISkillSet {

    private static final Map<UUID, Long> blackFlashFocusTicks = new HashMap<>();

    // key 0: divergent_fist, 1: manji_kick, 2: black_flash_focus, 3: domain_startup, 4: rct
    // §6-3 이타도리 기준
    private static final int CE_0 = 80,   CD_0 = 4;    // 5→4 (§6-3 이타도리 기준)
    private static final int CE_1 = 90,   CD_1 = 7;
    private static final int CE_2 = 120,  CD_2 = 20;   // 0→120, 120→20 (§6-3 이타도리 기준)
    private static final int CE_3 = 2200, CD_3 = 300;  // 2400→2200, 480→300 (§6-3 이타도리 기준)
    private static final int CE_4 = 0,    CD_4 = 5;

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
            case 0 -> "divergent_fist"; case 1 -> "manji_kick"; case 2 -> "black_flash_focus";
            case 3 -> "domain_startup"; case 4 -> "rct"; default -> "unknown";
        };
    }

    private static String cdKey(int keyId) { return "cd_itadori_" + keyId; }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        AnimationTriggerS2CPacket pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }

    private SkillResult useDivergentFist(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(0), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_0)) return SkillResult.CE_INSUFFICIENT;

        List<LivingEntity> targets = HitValidator.getNearbyArc(player, 3.0, 120f);

        for (LivingEntity target : targets) {
            JJKMod.getEffectDeferQueue().schedule(
                    BlockPos.ofFloored(target.getPos()), 17f, 5, player.getUuid(), tick);
        }

        JJKMod.getCEManager().consume(player, CE_0);
        CooldownManager.set(data, cdKey(0), tick, CD_0);
        JJKMod.getPlayerRepository().save(data);
        return SkillResult.SUCCESS;
    }

    private SkillResult useManjiKick(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(1), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_1)) return SkillResult.CE_INSUFFICIENT;

        Vec3d dir = player.getRotationVec(1.0f);
        player.setVelocity(dir.multiply(1.5));
        player.velocityModified = true;

        List<LivingEntity> targets = HitValidator.getNearby(player, 3.0);
        if (!targets.isEmpty()) {
            JJKMod.getComboTracker().recordHit(player.getUuid(), tick);
        }

        JJKMod.getCEManager().consume(player, CE_1);
        CooldownManager.set(data, cdKey(1), tick, CD_1);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, 14);
        return SkillResult.SUCCESS;
    }

    private SkillResult useBlackFlashFocus(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(2), tick)) return SkillResult.ON_COOLDOWN;

        blackFlashFocusTicks.put(player.getUuid(), tick);
        CooldownManager.set(data, cdKey(2), tick, CD_2);
        JJKMod.getPlayerRepository().save(data);
        return SkillResult.SUCCESS;
    }
    private SkillResult useDomainStartup(ServerPlayerEntity player) {
        boolean deployed = JJKMod.getDomainManager()
                .deployDomain("itadori_unnamed", player);
        if (!deployed) return SkillResult.FAIL;
        broadcastAnim(player, 59);
        return SkillResult.SUCCESS;
    }

    private SkillResult useRCT(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(4), tick)) return SkillResult.ON_COOLDOWN;

        data.healingActive = true;
        CooldownManager.set(data, cdKey(4), tick, CD_4);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, 15);
        return SkillResult.SUCCESS;
    }

    // ─── 순수 PlayerData 경로 (ISkillSet §default 오버라이드) ──────────────────
    @Override public SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : useDivergentFist(player); }
    @Override public SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) { return player == null ? SkillResult.SUCCESS : useManjiKick(player); }
    @Override public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : useBlackFlashFocus(player); }
    @Override public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) { return player == null ? SkillResult.SUCCESS : useDomainStartup(player); }
    @Override public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : useRCT(player); }
}
