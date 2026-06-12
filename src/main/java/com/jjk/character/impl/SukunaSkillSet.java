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

    // 수치는 techniques.json 단일 기준 (2026-06-11 데이터 주도 전환)
    private static final String CHAR_ID = "sukuna";
    private static final int ANIM_F = 21, ANIM_SF = 22, ANIM_R = 23, ANIM_SR = 24, ANIM_V = 7;

    private static float bd(int keyId) { return com.jjk.combat.TechniqueLoader.getBaseDamage(CHAR_ID, keyId); }
    private static int   ce(int keyId) { return (int) com.jjk.combat.TechniqueLoader.getCeCost(CHAR_ID, keyId); }
    private static int   cd(int keyId) { return (int) com.jjk.combat.TechniqueLoader.getCooldownTicks(CHAR_ID, keyId); }

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
        if (keyId == 4) return tick >= data.domainCooldownUntil; // CE는 DomainManager(domains.json)가 검증
        return data.cooldowns.getOrDefault(String.valueOf(keyId), 0L) <= tick
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
        if (data.ceCurrent < ce(0)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        // 전방 직선 참격: 폭 3블록 × 길이 15블록
        List<LivingEntity> targets = HitValidator.getNearbyBox(player, 3.0, 15.0);
        if (targets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= ce(0);
        data.cooldowns.put("0", tick + applyCdReduction(data, cd(0)));

        for (LivingEntity target : targets) {
            // 방어 관통 20%: defenseMultiplier=0.80 (calculatePure에서 effectiveDefense × 0.80)
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, bd(0))
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
        if (data.ceCurrent < ce(1)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        // 전방 직선 레이캐스트 관통: 사거리 20블록
        Vec3d start = player.getEyePos();
        Vec3d end   = start.add(player.getRotationVec(1.0f).multiply(20.0));
        List<LivingEntity> targets = HitValidator.getRaycastPiercing(player, start, end, 0.5);
        if (targets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= ce(1);
        data.cooldowns.put("1", tick + applyCdReduction(data, cd(1)));

        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, bd(1))
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
        if (data.ceCurrent < ce(2)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        // 손가락 10개 이상: 범위 ×1.3
        double radius = data.fingerCount >= 10 ? 8.0 * 1.3 : 8.0;
        List<LivingEntity> targets = HitValidator.getNearbyArc(player, radius, 120f);
        if (targets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= ce(2);
        data.cooldowns.put("2", tick + applyCdReduction(data, cd(2)));

        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, bd(2))
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
        if (data.ceCurrent < ce(3)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        // 원형 반경 6블록, isSoulDirect=true → 방어 무시
        List<LivingEntity> targets = HitValidator.getNearby(player, 6.0);
        if (targets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= ce(3);
        data.cooldowns.put("3", tick + applyCdReduction(data, cd(3)));

        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.SOUL_DIRECT, bd(3))
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
        // CE 검증·차감은 DomainManager(domains.json ceCost, 개방형 ×2)가 단일 수행
        if (player == null) return SkillResult.FAIL_CONDITION;

        boolean deployed = JJKMod.getDomainManager()
                .deployDomain("sukuna_malevolent_shrine", player);
        if (!deployed) return SkillResult.FAIL_CONDITION;
        broadcastAnim(player, ANIM_V);
        return SkillResult.SUCCESS;
    }
}
