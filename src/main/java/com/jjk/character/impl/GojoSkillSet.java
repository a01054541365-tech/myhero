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
import net.minecraft.util.math.Vec3d;

import java.util.List;

import java.util.List;
import java.util.Optional;

public class GojoSkillSet implements ISkillSet {

    // key 0: blue, 1: red, 2: purple, 3: unlimited_void, 4: infinity_toggle, 5: curtain_toggle
    // 수치는 techniques.json 단일 기준 (2026-06-11 데이터 주도 전환). key 5(커튼)는 techniques.json 미등재 — 상수 유지.
    private static final String CHAR_ID = "gojo";
    private static final int CE_5 = 200,  CD_5 = 60;

    private static int ce(int keyId) { return (int) com.jjk.combat.TechniqueLoader.getCeCost(CHAR_ID, keyId); }
    private static int cd(int keyId) { return (int) com.jjk.combat.TechniqueLoader.getCooldownTicks(CHAR_ID, keyId); }

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
            case 0, 1, 2, 3, 4 -> cd(keyId); case 5 -> CD_5; default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) {
        return switch (keyId) {
            case 0, 1, 2, 3, 4 -> ce(keyId); case 5 -> CE_5; default -> 0;
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

    private SkillResult checkAndConsume(ServerPlayerEntity player,
            PlayerData data, long tick, int keyId) {
        if (!CooldownManager.isReady(data, cdKey(keyId), tick))
            return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, getCeCost(keyId)))
            return SkillResult.CE_INSUFFICIENT;
        return null;
    }

    private boolean recentlyUsed(PlayerData data, long tick, int keyId) {
        Long expiry = data.cooldowns.get(cdKey(keyId));
        if (expiry == null) return false;
        long lastUseTick = expiry - getCooldownTicks(keyId);
        return (tick - lastUseTick) <= 100;
    }

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

    private SkillResult useBlue(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        SkillResult check = checkAndConsume(player, data, tick, 0);
        if (check != null) return check;

        List<LivingEntity> targets = HitValidator.getNearby(player, 5.0);
        Vec3d casterPos = player.getPos();
        for (LivingEntity target : targets) {
            Vec3d pull = casterPos.subtract(target.getPos()).normalize().multiply(1.5);
            target.addVelocity(pull.x, pull.y, pull.z);
            target.velocityModified = true;
        }

        JJKMod.getCEManager().consume(player, ce(0));
        CooldownManager.set(data, cdKey(0), tick, cd(0));
        JJKMod.getPlayerRepository().save(data);
        player.getServerWorld().getPlayers().forEach(p ->
                ServerPlayNetworking.send(p, new AnimationTriggerS2CPacket(player.getUuid(), (byte) 1)));
        return SkillResult.SUCCESS;
    }

    private SkillResult useRed(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        SkillResult check = checkAndConsume(player, data, tick, 1);
        if (check != null) return check;

        List<LivingEntity> targets = HitValidator.getNearby(player, 5.0);
        Vec3d casterPos = player.getPos();
        for (LivingEntity target : targets) {
            Vec3d push = target.getPos().subtract(casterPos).normalize().multiply(3.0);
            target.addVelocity(push.x, push.y, push.z);
            target.velocityModified = true;
        }

        JJKMod.getCEManager().consume(player, ce(1));
        CooldownManager.set(data, cdKey(1), tick, cd(1));
        JJKMod.getPlayerRepository().save(data);
        player.getServerWorld().getPlayers().forEach(p ->
                ServerPlayNetworking.send(p, new AnimationTriggerS2CPacket(player.getUuid(), (byte) 2)));
        return SkillResult.SUCCESS;
    }

    private SkillResult usePurple(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        SkillResult check = checkAndConsume(player, data, tick, 2);
        if (check != null) return check;

        if (!recentlyUsed(data, tick, 0) && !recentlyUsed(data, tick, 1))
            return SkillResult.FAIL;

        JJKMod.getCEManager().consume(player, ce(2));
        CooldownManager.set(data, cdKey(2), tick, cd(2));
        JJKMod.getPlayerRepository().save(data);
        player.getServerWorld().getPlayers().forEach(p ->
                ServerPlayNetworking.send(p, new AnimationTriggerS2CPacket(player.getUuid(), (byte) 3)));
        return SkillResult.SUCCESS;
    }

    private SkillResult useUnlimitedVoid(ServerPlayerEntity player) {
        boolean deployed = JJKMod.getDomainManager()
                .deployDomain("gojo_unlimited_void", player);
        if (!deployed) return SkillResult.FAIL;
        player.getServerWorld().getPlayers().forEach(p ->
                ServerPlayNetworking.send(p, new AnimationTriggerS2CPacket(player.getUuid(), (byte) 4)));
        return SkillResult.SUCCESS;
    }
    private SkillResult toggleInfinity(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(4), tick)) return SkillResult.ON_COOLDOWN;
        // §6-2: 토글 ON 시만 최소 보유량(ce(4)) 확인. OFF는 항상 허용.
        // 실제 CE 소모는 CEManager.regenTick() 드레인(cePerTick)으로만 처리.
        if (!data.infinityActive && !JJKMod.getCEManager().canAfford(player, ce(4))) {
            return SkillResult.CE_INSUFFICIENT;
        }
        data.infinityActive = !data.infinityActive;
        CooldownManager.set(data, cdKey(4), tick, cd(4));
        JJKMod.getPlayerRepository().save(data);
        return SkillResult.SUCCESS;
    }

    // ─── 순수 PlayerData 경로 (ISkillSet §default 오버라이드) ──────────────────
    // player==null 허용: 테스트에서 MC 없이 호출. null 시 기본 성공 반환.
    @Override public SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : useBlue(player); }
    @Override public SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) { return player == null ? SkillResult.SUCCESS : useRed(player); }
    @Override public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : usePurple(player); }
    @Override public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) { return player == null ? SkillResult.SUCCESS : useUnlimitedVoid(player); }
    @Override public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : toggleInfinity(player); }
}
