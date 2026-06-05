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
    private static final int CE_0 = 360,  CD_0 = 4;
    private static final int CE_1 = 360,  CD_1 = 8;
    private static final int CE_2 = 520,  CD_2 = 60;
    private static final int CE_3 = 6000, CD_3 = 360;
    private static final int CE_4 = 30,   CD_4 = 1;    // 50→30, 5→1 (§6-2 고죠 기준) | CE_4는 토글 ON 최소 보유량 기준; 실제 소모는 CEManager 드레인(1.5/틱)으로 처리
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

        JJKMod.getCEManager().consume(player, CE_0);
        CooldownManager.set(data, cdKey(0), tick, CD_0);
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

        JJKMod.getCEManager().consume(player, CE_1);
        CooldownManager.set(data, cdKey(1), tick, CD_1);
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

        JJKMod.getCEManager().consume(player, CE_2);
        CooldownManager.set(data, cdKey(2), tick, CD_2);
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
        // §6-2: 토글 ON 시만 최소 보유량(CE_4=30) 확인. OFF는 항상 허용.
        // 실제 CE 소모는 CEManager.regenTick() 드레인(1.5/틱)으로만 처리.
        if (!data.infinityActive && !JJKMod.getCEManager().canAfford(player, CE_4)) {
            return SkillResult.CE_INSUFFICIENT;
        }
        data.infinityActive = !data.infinityActive;
        CooldownManager.set(data, cdKey(4), tick, CD_4);
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
