package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.combat.IDamageSource;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.DamageContext;
import com.jjk.combat.HitValidator;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * 스쿠나 스킬셋 — Phase 3 확정 수치 적용.
 * 손가락 연동: 10개 이상=역팔지 범위 ×1.3, 20개=전 스킬 CD ×0.80
 */
public class SukunaSkillSet implements ISkillSet {

    // 확정 수치 (Phase 3)
    private static final float BD_F  = 72f;
    private static final float BD_SF = 55f;
    private static final float BD_R  = 95f;
    private static final float BD_SR = 110f;

    private static final int CE_F  = 180, BASE_CD_F  = 6,   ANIM_F  = 21;
    private static final int CE_SF = 140, BASE_CD_SF = 5,   ANIM_SF = 22;
    private static final int CE_R  = 400, BASE_CD_R  = 40,  ANIM_R  = 23;
    private static final int CE_SR = 500, BASE_CD_SR = 50,  ANIM_SR = 24;
    private static final int CE_V  = 3000, BASE_CD_V = 360, ANIM_V  = 7;

    // ── ISkillSet 인터페이스 ──────────────────────────────────────────────────

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        SkillResult result = dispatch(keyId, data, player, tick);
        JJKMod.getPlayerRepository().save(data);
        return result;
    }

    @Override
    public boolean canUse(ServerPlayerEntity player, int keyId) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (keyId == 4) return data.ceCurrent >= CE_V && tick >= data.domainCooldownUntil;
        return data.cooldowns.getOrDefault(String.valueOf(keyId), 0L) <= tick
                && data.ceCurrent >= getCeCost(keyId);
    }

    @Override
    public int getCooldownTicks(int keyId) {
        return switch (keyId) {
            case 0 -> BASE_CD_F; case 1 -> BASE_CD_SF; case 2 -> BASE_CD_R;
            case 3 -> BASE_CD_SR; case 4 -> BASE_CD_V; default -> 0;
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
            case 0 -> "dismantle"; case 1 -> "arrow";
            case 2 -> "reverse_eight_handled"; case 3 -> "cleave";
            case 4 -> "malevolent_shrine"; default -> "unknown";
        };
    }

    // ── 공통 헬퍼 ────────────────────────────────────────────────────────────

    /** 손가락 20개 만재 시 CD × 0.80 적용. */
    private int applyCdReduction(PlayerData data, int baseCd) {
        return data.fingerCount >= 20 ? (int)(baseCd * 0.80f) : baseCd;
    }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        AnimationTriggerS2CPacket pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }

    // ── F: 해체(Dismantle) ────────────────────────────────────────────────────

    @Override
    public SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("0", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_F) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        // 전방 직선 참격: 폭 3블록 × 길이 15블록
        List<LivingEntity> targets = HitValidator.getNearbyBox(player, 3.0, 15.0);
        if (targets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= CE_F;
        data.cooldowns.put("0", tick + applyCdReduction(data, BASE_CD_F));

        for (LivingEntity target : targets) {
            // 방어 관통 20%: defenseMultiplier=0.80 (calculatePure에서 effectiveDefense × 0.80)
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_F)
                    .defenseMultiplier(0.80f).skillName("dismantle").keyId(0).build();
            JJKMod.getCombatPipeline().process(ctx);
        }
        broadcastAnim(player, ANIM_F);
        return SkillResult.SUCCESS;
    }

    // ── Shift+F: 화살(Arrow) ──────────────────────────────────────────────────

    @Override
    public SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("1", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_SF) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        // 전방 직선 레이캐스트 관통: 사거리 20블록
        Vec3d start = player.getEyePos();
        Vec3d end   = start.add(player.getRotationVec(1.0f).multiply(20.0));
        List<LivingEntity> targets = HitValidator.getRaycastPiercing(player, start, end, 0.5);
        if (targets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= CE_SF;
        data.cooldowns.put("1", tick + applyCdReduction(data, BASE_CD_SF));

        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_SF)
                    .skillName("arrow").keyId(1).build();
            JJKMod.getCombatPipeline().process(ctx);
        }
        broadcastAnim(player, ANIM_SF);
        return SkillResult.SUCCESS;
    }

    // ── R: 역팔지 ─────────────────────────────────────────────────────────────

    @Override
    public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("2", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_R) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        // 손가락 10개 이상: 범위 ×1.3
        double radius = data.fingerCount >= 10 ? 8.0 * 1.3 : 8.0;
        List<LivingEntity> targets = HitValidator.getNearbyArc(player, radius, 120f);
        if (targets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= CE_R;
        data.cooldowns.put("2", tick + applyCdReduction(data, BASE_CD_R));

        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_R)
                    .skillName("reverse_eight_handled").keyId(2).build();
            JJKMod.getCombatPipeline().process(ctx);
            // 넉백 4블록
            Vec3d kb = target.getPos().subtract(player.getPos()).normalize().multiply(4.0);
            target.addVelocity(kb.x, 0.3, kb.z);
        }
        broadcastAnim(player, ANIM_R);
        return SkillResult.SUCCESS;
    }

    // ── Shift+R: 수파(Cleave) ─────────────────────────────────────────────────

    @Override
    public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("3", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_SR) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        // 원형 반경 6블록, isSoulDirect=true → 방어 무시
        List<LivingEntity> targets = HitValidator.getNearby(player, 6.0);
        if (targets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= CE_SR;
        data.cooldowns.put("3", tick + applyCdReduction(data, BASE_CD_SR));

        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.SOUL_DIRECT, BD_SR)
                    .soulDirect().skillName("cleave").keyId(3).build();
            JJKMod.getCombatPipeline().process(ctx);
        }
        broadcastAnim(player, ANIM_SR);
        return SkillResult.SUCCESS;
    }

    // ── V: 복마어주자 전개 ────────────────────────────────────────────────────

    @Override
    public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.domainCooldownUntil > tick) return SkillResult.FAIL_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_V) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_CONDITION;

        boolean deployed = JJKMod.getDomainManager()
                .deployDomain("sukuna_malevolent_shrine", player);
        if (!deployed) return SkillResult.FAIL_CONDITION;
        broadcastAnim(player, ANIM_V);
        return SkillResult.SUCCESS;
    }
}
