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

import java.util.Comparator;
import java.util.List;

public class SukunaSkillSet implements ISkillSet {

    // §LOCK: 운영자 확정 2026-05-28 — 변경 금지
    private static final float BD_F  = 8f;
    private static final float BD_SF = 18f;
    private static final float BD_R  = 30f;
    private static final float BD_SR = 50f;

    private static final int CE_F  = 10,  CD_F  = 10,  ANIM_F  = 21;
    private static final int CE_SF = 25,  CD_SF = 40,  ANIM_SF = 22;
    private static final int CE_R  = 50,  CD_R  = 120, ANIM_R  = 23;
    private static final int CE_SR = 100, CD_SR = 240, ANIM_SR = 24;
    private static final int CE_V  = 200, CD_V  = 360, ANIM_V  = 7;

    // §LOCK: 필살참 HP 비례 추가 피해 — 변경 금지
    private static final float HP_BONUS_RATIO = 0.20f;

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
            case 0 -> "해체(解體)"; case 1 -> "필살참(捌)"; case 2 -> "개(開)·화염";
            case 3 -> "세계절단참"; case 4 -> "복마어주자"; default -> "unknown";
        };
    }

    // ── onX PlayerData 경로 ──────────────────────────────────────────────────────

    /** F — 해체(解體): 직선 참격 투사체 (EffectDeferQueue 4틱 지연) */
    @Override
    public SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("0", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_F) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        List<LivingEntity> nearby = HitValidator.getNearby(player, 15.0);
        LivingEntity target = nearby.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(player))).orElse(null);
        if (target == null) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= CE_F;
        JJKMod.getEffectDeferQueue().schedule(
                target.getBlockPos(), BD_F, 4, player.getUuid(), tick);
        data.cooldowns.put("0", tick + CD_F);
        broadcastAnim(player, ANIM_F);
        return SkillResult.SUCCESS;
    }

    /** Shift+F — 필살참(捌): baseDamage + target.hpCurrent × 0.20f */
    @Override
    public SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("1", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_SF) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        List<LivingEntity> nearby = HitValidator.getNearby(player, 10.0);
        LivingEntity target = nearby.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(player))).orElse(null);
        if (target == null) return SkillResult.FAIL_NO_TARGET;

        float bonusDamage = target.getHealth() * HP_BONUS_RATIO; // §LOCK: 0.20f
        float totalDamage = BD_SF + bonusDamage;

        data.ceCurrent -= CE_SF;
        DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, totalDamage)
                .skillName("cleave").keyId(1).build();
        JJKMod.getCombatPipeline().process(ctx);
        data.cooldowns.put("1", tick + CD_SF);
        broadcastAnim(player, ANIM_SF);
        return SkillResult.SUCCESS;
    }

    /** R — 개(開)·화염: 반경 4블록 범위 폭발 + 화상 80틱 */
    @Override
    public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("2", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_R) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        List<LivingEntity> targets = HitValidator.getNearby(player, 4.0);
        if (targets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= CE_R;
        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_R)
                    .skillName("fire_arrow").keyId(2).build();
            JJKMod.getCombatPipeline().process(ctx);
            if (target instanceof ServerPlayerEntity targetPlayer) {
                PlayerData targetData = JJKMod.getPlayerRepository().load(targetPlayer.getUuid());
                FireEffectManager.apply(targetData, 80, tick);
                JJKMod.getPlayerRepository().save(targetData);
            }
        }
        data.cooldowns.put("2", tick + CD_R);
        broadcastAnim(player, ANIM_R);
        return SkillResult.SUCCESS;
    }

    /** Shift+R — 세계절단참: isSoulDirect=true, bypassRCT=true (방어·RCT 무시) */
    @Override
    public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("3", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_SR) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        List<LivingEntity> nearby = HitValidator.getNearby(player, 12.0);
        LivingEntity target = nearby.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(player))).orElse(null);
        if (target == null) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= CE_SR;
        DamageContext ctx = DamageContext.builder(player, target, IDamageSource.SOUL_DIRECT, BD_SR)
                .soulDirect().bypassRCT().skillName("dismantle").keyId(3).build();
        JJKMod.getCombatPipeline().process(ctx);
        data.cooldowns.put("3", tick + CD_SR);
        broadcastAnim(player, ANIM_SR);
        return SkillResult.SUCCESS;
    }

    /** V — 복마어주자(伏魔御廚子): 개방형 영역 전개 */
    @Override
    public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.domainCooldownUntil > tick) return SkillResult.FAIL_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_V) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_CONDITION;

        boolean deployed = JJKMod.getDomainManager().deployDomain("sukuna_malevolent_shrine", player);
        if (!deployed) return SkillResult.FAIL_CONDITION;
        broadcastAnim(player, ANIM_V);
        return SkillResult.SUCCESS;
    }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        var pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }
}
