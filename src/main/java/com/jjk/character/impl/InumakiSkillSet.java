package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.combat.IDamageSource;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.burden.BurdenManager;
import com.jjk.combat.DamageContext;
import com.jjk.combat.HitValidator;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import com.jjk.network.s2c.SkillEffectS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;

public class InumakiSkillSet implements ISkillSet {

    // 수치는 techniques.json 단일 기준 (2026-06-11 데이터 주도 전환)
    private static final String CHAR_ID = "inumaki";
    private static final int ANIM_F = 50, ANIM_SF = 51, ANIM_R = 65, ANIM_SR = 52, ANIM_V = 53;

    private static float bd(int keyId) { return com.jjk.combat.TechniqueLoader.getBaseDamage(CHAR_ID, keyId); }
    private static int   ce(int keyId) { return (int) com.jjk.combat.TechniqueLoader.getCeCost(CHAR_ID, keyId); }
    private static int   cd(int keyId) { return (int) com.jjk.combat.TechniqueLoader.getCooldownTicks(CHAR_ID, keyId); }

    private static final double SCATTER_RADIUS = 6.0;
    private static final double SCATTER_KNOCKBACK = 1.8;
    private static final double SCATTER_LIFT = 0.4;

    // 부담 수치 — spec §6-9 명시값
    private static final float BURDEN_F  = 15f;
    private static final float BURDEN_SF = 30f;
    private static final float BURDEN_R  = 20f;
    private static final float BURDEN_SR = 25f;
    private static final float BURDEN_V  = 10f;

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
        return !BurdenManager.isSealed(data, tick)
                && data.cooldowns.getOrDefault(String.valueOf(keyId), 0L) <= tick
                && data.ceCurrent >= getCeCost(keyId);
    }

    @Override
    public int getCooldownTicks(int keyId) {
        return (keyId >= 0 && keyId <= 4) ? cd(keyId) : 0;
    }

    @Override
    public int getCeCost(int keyId) {
        return (keyId >= 0 && keyId <= 4) ? ce(keyId) : 0;
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "!멈춰"; case 1 -> "!터져"; case 2 -> "!흩어져"; case 3 -> "!잠들어"; case 4 -> "!달려";
            default -> "not_implemented";
        };
    }

    // ── onX 경로 ─────────────────────────────────────────────────────────────

    /** F — !멈춰: 전방 10블록 단일 대상, status_freeze 40틱 */
    @Override
    public SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (BurdenManager.isSealed(data, tick)) return SkillResult.FAIL_SKILL_SEALED;
        if (data.cooldowns.getOrDefault("0", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.ceCurrent < ce(0)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        List<LivingEntity> targets = HitValidator.getNearby(player, 10.0);
        LivingEntity target = targets.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(player)))
                .orElse(null);
        if (target == null) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= ce(0);

        if (target instanceof ServerPlayerEntity tp) {
            PlayerData td = JJKMod.getPlayerRepository().load(tp.getUuid());
            td.cooldowns.put("status_freeze", tick + 40);
            JJKMod.getPlayerRepository().save(td);
        }

        BurdenManager.addBurden(data, BURDEN_F, tick, JJKMod.getConfig());
        data.cooldowns.put("0", tick + cd(0));
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_F);
        return SkillResult.SUCCESS;
    }

    /** Shift+F — !터져: 반경 4블록, 폭발 피해 70 */
    @Override
    public SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (BurdenManager.isSealed(data, tick)) return SkillResult.FAIL_SKILL_SEALED;
        if (data.cooldowns.getOrDefault("1", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.ceCurrent < ce(1)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        List<LivingEntity> targets = HitValidator.getNearby(player, 4.0);
        if (targets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= ce(1);

        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, bd(1))
                    .skillName("!터져").keyId(1).build();
            JJKMod.getCombatPipeline().process(ctx);
        }

        BurdenManager.addBurden(data, BURDEN_SF, tick, JJKMod.getConfig());
        data.cooldowns.put("1", tick + cd(1));
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SF);
        return SkillResult.SUCCESS;
    }

    /** R — !흩어져: 반경 6블록 내 적 전원 외부 방향 넉백 (군중 제어, 부담 소모형) */
    @Override
    public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (BurdenManager.isSealed(data, tick)) return SkillResult.FAIL_SKILL_SEALED;
        if (data.cooldowns.getOrDefault("2", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.ceCurrent < ce(2)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        List<LivingEntity> targets = HitValidator.getNearby(player, SCATTER_RADIUS);
        if (targets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= ce(2);

        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayerEntity tp) {
                PlayerData td = JJKMod.getPlayerRepository().load(tp.getUuid());
                if (JJKMod.getTeamManager().isSameTeam(data, td)) continue;
            }
            Vec3d dir = target.getPos().subtract(player.getPos()).normalize();
            target.addVelocity(dir.x * SCATTER_KNOCKBACK, SCATTER_LIFT, dir.z * SCATTER_KNOCKBACK);
            target.velocityModified = true;
        }

        BurdenManager.addBurden(data, BURDEN_R, tick, JJKMod.getConfig());
        data.cooldowns.put("2", tick + cd(2));
        JJKMod.getPlayerRepository().save(data);

        Vec3d pos = player.getPos();
        SkillEffectS2CPacket pkt = SkillEffectS2CPacket.of(
                "inumaki_scatter", player.getUuid(), pos.x, pos.y, pos.z);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));

        broadcastAnim(player, ANIM_R);
        return SkillResult.SUCCESS;
    }

    /** Shift+R — !잠들어: 전방 10블록 단일 대상, status_sleep 100틱 */
    @Override
    public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (BurdenManager.isSealed(data, tick)) return SkillResult.FAIL_SKILL_SEALED;
        if (data.cooldowns.getOrDefault("3", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.ceCurrent < ce(3)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        List<LivingEntity> targets = HitValidator.getNearby(player, 10.0);
        LivingEntity target = targets.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(player)))
                .orElse(null);
        if (target == null) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= ce(3);

        if (target instanceof ServerPlayerEntity tp) {
            PlayerData td = JJKMod.getPlayerRepository().load(tp.getUuid());
            td.cooldowns.put("status_sleep", tick + 100);
            JJKMod.getPlayerRepository().save(td);
        }

        BurdenManager.addBurden(data, BURDEN_SR, tick, JJKMod.getConfig());
        data.cooldowns.put("3", tick + cd(3));
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SR);
        return SkillResult.SUCCESS;
    }

    /** V — !달려: 반경 10블록 아군 + 자신, status_speed 80틱 (S2C로만 처리) */
    @Override
    public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick) {
        if (BurdenManager.isSealed(data, tick)) return SkillResult.FAIL_SKILL_SEALED;
        if (data.cooldowns.getOrDefault("4", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.ceCurrent < ce(4)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= ce(4);

        // 자신 포함
        data.cooldowns.put("status_speed", tick + 80);

        // 반경 10블록 아군
        List<LivingEntity> nearby = HitValidator.getNearby(player, 10.0);
        for (LivingEntity entity : nearby) {
            if (!(entity instanceof ServerPlayerEntity ally)) continue;
            PlayerData allyData = JJKMod.getPlayerRepository().load(ally.getUuid());
            if (!JJKMod.getTeamManager().isSameTeam(data, allyData)) continue;
            allyData.cooldowns.put("status_speed", tick + 80);
            JJKMod.getPlayerRepository().save(allyData);
        }

        BurdenManager.addBurden(data, BURDEN_V, tick, JJKMod.getConfig());
        data.cooldowns.put("4", tick + cd(4));
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_V);
        return SkillResult.SUCCESS;
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        var pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }
}
