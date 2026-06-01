package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.combat.IDamageSource;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.DamageContext;
import com.jjk.combat.HitValidator;
import com.jjk.data.PlayerData;
import com.jjk.effect.FireEffectManager;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;

public class JogoSkillSet implements ISkillSet {

    // §LOCK: techniques.json 수치와 동일 — 변경 금지
    private static final float BD_F  = 46f;
    private static final float BD_SF = 80f;
    private static final float BD_R  = 120f;
    private static final float BD_SR = 54f;

    private static final int CE_F  = 130,  CD_F  = 6,   ANIM_F  = 30;
    private static final int CE_SF = 280,  CD_SF = 20,  ANIM_SF = 46;
    private static final int CE_R  = 600,  CD_R  = 75,  ANIM_R  = 1;
    private static final int CE_SR = 230,  CD_SR = 16,  ANIM_SR = 47;
    private static final int CE_V  = 2700, CD_V  = 360, ANIM_V  = 7;

    // ── Legacy API ────────────────────────────────────────────────────────────

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        return dispatch(keyId, data, player, tick);
    }

    @Override
    public boolean canUse(ServerPlayerEntity player, int keyId) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        return data.cooldowns.getOrDefault(String.valueOf(keyId), 0L) <= tick
                && data.ceCurrent >= getCeCost(keyId);
    }

    @Override
    public int getCooldownTicks(int keyId) {
        return switch (keyId) {
            case 0 -> CD_F; case 1 -> CD_SF; case 2 -> CD_R;
            case 3 -> CD_SR; case 4 -> CD_V; default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) {
        return switch (keyId) {
            case 0 -> CE_F; case 1 -> CE_SF; case 2 -> CE_R;
            case 3 -> CE_SR; case 4 -> CE_V; default -> 0;
        };
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "화산탄"; case 1 -> "개관"; case 2 -> "운석";
            case 3 -> "불꽃의 고리"; case 4 -> "개관철위산"; default -> "unknown";
        };
    }

    // ── onX 경로 ─────────────────────────────────────────────────────────────

    /** F — 화산탄: 전방 12블록 단일 대상, 화염 80틱 */
    @Override
    public SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("0", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_F) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        // CE 소모
        data.ceCurrent -= CE_F;

        // 전방 12블록 이내 가장 가까운 적
        List<LivingEntity> targets = HitValidator.getNearby(player, 12.0);
        LivingEntity target = targets.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(player)))
                .orElse(null);
        if (target == null) {
            data.ceCurrent += CE_F;
            return SkillResult.FAIL_NO_TARGET;
        }

        // 우천 시 -20% 패널티 (DamageCalculator externalBuffMult 경로)
        float rainMult = player.getServerWorld().isRaining() ? 0.80f : 1.0f;
        DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_F)
                .skillName("화산탄").keyId(0).externalBuffMult(rainMult).build();
        JJKMod.getCombatPipeline().process(ctx);

        // 화염 상태이상 80틱
        if (target instanceof ServerPlayerEntity targetPlayer) {
            PlayerData targetData = JJKMod.getPlayerRepository().load(targetPlayer.getUuid());
            FireEffectManager.apply(targetData, 80, tick);
            JJKMod.getPlayerRepository().save(targetData);
        }

        data.cooldowns.put("0", tick + CD_F);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_F);
        return SkillResult.SUCCESS;
    }

    /** Shift+F — 개관: 전방 20블록 직선 (폭 1.5블록), 화염 60틱 */
    @Override
    public SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("1", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_SF) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        Vec3d facing = player.getRotationVec(1.0f);
        Vec3d origin = player.getEyePos();
        List<LivingEntity> nearby = HitValidator.getNearby(player, 20.0);
        List<LivingEntity> lineTargets = nearby.stream()
                .filter(e -> isOnLine(origin, facing, e, 20.0, 1.5))
                .toList();

        if (lineTargets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= CE_SF;
        float rainMult = player.getServerWorld().isRaining() ? 0.80f : 1.0f;

        for (LivingEntity target : lineTargets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_SF)
                    .skillName("개관").keyId(1).externalBuffMult(rainMult).build();
            JJKMod.getCombatPipeline().process(ctx);

            if (target instanceof ServerPlayerEntity targetPlayer) {
                PlayerData targetData = JJKMod.getPlayerRepository().load(targetPlayer.getUuid());
                FireEffectManager.apply(targetData, 60, tick);
                JJKMod.getPlayerRepository().save(targetData);
            }
        }

        data.cooldowns.put("1", tick + CD_SF);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SF);
        return SkillResult.SUCCESS;
    }

    /** R — 운석: 전방 15블록 지점, 40틱 후 광역 피해 (EffectDeferQueue) */
    @Override
    public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("2", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_R) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= CE_R;

        // 전방 15블록 낙하 지점 예약 (40틱 후 실행)
        Vec3d dir = player.getRotationVec(1.0f);
        Vec3d strikePos = player.getPos().add(dir.multiply(15.0));
        JJKMod.getEffectDeferQueue().schedule(
                BlockPos.ofFloored(strikePos), BD_R, 40, player.getUuid(), tick);

        data.cooldowns.put("2", tick + CD_R);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_R);
        return SkillResult.SUCCESS;
    }

    /** Shift+R — 불꽃의 고리: 반경 6블록 범위, 화염 40틱 */
    @Override
    public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("3", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_SR) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        List<LivingEntity> targets = HitValidator.getNearby(player, 6.0);
        if (targets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= CE_SR;
        float rainMult = player.getServerWorld().isRaining() ? 0.80f : 1.0f;

        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_SR)
                    .skillName("불꽃의 고리").keyId(3).externalBuffMult(rainMult).build();
            JJKMod.getCombatPipeline().process(ctx);

            if (target instanceof ServerPlayerEntity targetPlayer) {
                PlayerData targetData = JJKMod.getPlayerRepository().load(targetPlayer.getUuid());
                FireEffectManager.apply(targetData, 40, tick);
                JJKMod.getPlayerRepository().save(targetData);
            }
        }

        data.cooldowns.put("3", tick + CD_SR);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SR);
        return SkillResult.SUCCESS;
    }

    /** V — 개관철위산 (영역 전개) */
    @Override
    public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick) {
        // domainCooldownUntil — decisions §3-2: cooldowns Map "domain" 키 사용 금지
        if (data.domainCooldownUntil > tick) return SkillResult.FAIL_COOLDOWN;
        if (data.ceCurrent < CE_V) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        boolean deployed = JJKMod.getDomainManager().deployDomain("jogo_volcano_domain", player);
        if (!deployed) return SkillResult.FAIL_COOLDOWN;

        broadcastAnim(player, ANIM_V);
        return SkillResult.SUCCESS;
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private static boolean isOnLine(Vec3d origin, Vec3d facing,
                                    LivingEntity target, double maxDist, double maxLateral) {
        Vec3d toTarget = target.getPos().subtract(origin);
        double forward = toTarget.dotProduct(facing);
        if (forward <= 0 || forward > maxDist) return false;
        return toTarget.subtract(facing.multiply(forward)).length() <= maxLateral;
    }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        var pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }
}
