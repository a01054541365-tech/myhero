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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MahitoSkillSet implements ISkillSet {

    // 수치는 techniques.json 단일 기준 (2026-06-11 데이터 주도 전환)
    private static final String CHAR_ID = "mahito";
    private static final int ANIM_F = 33, ANIM_SF = 43, ANIM_R = 34, ANIM_SR = 44, ANIM_V = 35;

    private static float bd(int keyId) { return com.jjk.combat.TechniqueLoader.getBaseDamage(CHAR_ID, keyId); }
    private static int   ce(int keyId) { return (int) com.jjk.combat.TechniqueLoader.getCeCost(CHAR_ID, keyId); }
    private static int   cd(int keyId) { return (int) com.jjk.combat.TechniqueLoader.getCooldownTicks(CHAR_ID, keyId); }

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
            case 0 -> "idle_transfiguration";
            case 1 -> "polymorphic_soul_isomer";
            case 2 -> "soul_defense";
            case 3 -> "blade_transfiguration";
            case 4 -> "self_embodiment_of_perfection";
            default -> "unknown";
        };
    }

    // ── onX 경로 ─────────────────────────────────────────────────────────────

    /** F — 무위전변: 근접 4블록 최근접 단일 대상, isSoulDirect=true */
    @Override
    public SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("0", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < ce(0)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        List<LivingEntity> targets = HitValidator.getNearby(player, 4.0);
        LivingEntity target = targets.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(player))).orElse(null);
        if (target == null) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= ce(0);
        DamageContext ctx = DamageContext.builder(player, target, IDamageSource.SOUL_DIRECT, bd(0))
                .soulDirect().skillName("idle_transfiguration").keyId(0).build();
        JJKMod.getCombatPipeline().process(ctx);
        data.cooldowns.put("0", tick + cd(0));
        broadcastAnim(player, ANIM_F);
        return SkillResult.SUCCESS;
    }

    /** Shift+F — 다중체변: 최대 3타, MultiHitDampener 1.0/0.85/0.70 */
    @Override
    public SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("1", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < ce(1)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        List<LivingEntity> nearbyTargets = HitValidator.getNearby(player, 10.0).stream()
                .filter(t -> !(t instanceof ServerPlayerEntity p) || JJKMod.getTeamManager().isEnemy(player, p))
                .sorted(Comparator.comparingDouble(e -> e.squaredDistanceTo(player)))
                .limit(3)
                .collect(Collectors.toList());
        if (nearbyTargets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= ce(1);
        float[] dampeners = {1.0f, 0.85f, 0.70f};
        for (int i = 0; i < nearbyTargets.size(); i++) {
            DamageContext ctx = DamageContext.builder(player, nearbyTargets.get(i),
                    IDamageSource.NORMAL_TECHNIQUE, bd(1) * dampeners[i])
                    .skillName("polymorphic_soul_isomer").keyId(1).build();
            JJKMod.getCombatPipeline().process(ctx);
        }
        data.cooldowns.put("1", tick + cd(1));
        broadcastAnim(player, ANIM_SF);
        return SkillResult.SUCCESS;
    }

    /** R — 혼방어: 80틱 soul_resist 부여 (isSoulDirect 피격 시 데미지 50% 감소) */
    @Override
    public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("2", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < ce(2)) return SkillResult.FAIL_CE_INSUFFICIENT;

        data.ceCurrent -= ce(2);
        data.cooldowns.put("status_soul_resist", tick + 80);
        data.cooldowns.put("2", tick + cd(2));
        if (player != null) broadcastAnim(player, ANIM_R);
        return SkillResult.SUCCESS;
    }

    /** Shift+R — 체변의 칼날: 전방 90도 arc 5블록, isSoulDirect=true */
    @Override
    public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("3", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < ce(3)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        List<LivingEntity> targets = HitValidator.getNearbyArc(player, 5.0, 90f);
        if (targets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= ce(3);
        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target,
                    IDamageSource.SOUL_DIRECT, bd(3))
                    .soulDirect().skillName("blade_transfiguration").keyId(3).build();
            JJKMod.getCombatPipeline().process(ctx);
        }
        data.cooldowns.put("3", tick + cd(3));
        broadcastAnim(player, ANIM_SR);
        return SkillResult.SUCCESS;
    }

    /** V — 자기 체현 완성 (영역 전개) + 양날성(자기 피해) */
    @Override
    public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.domainCooldownUntil > tick) return SkillResult.FAIL_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        // CE 검증·차감은 DomainManager(domains.json ceCost)가 단일 수행
        if (player == null) return SkillResult.FAIL_CONDITION;

        boolean deployed = JJKMod.getDomainManager().deployDomain("mahito_self_embodiment", player);
        if (!deployed) return SkillResult.FAIL_CONDITION;

        // 양날성: 영역 전개 반동으로 사용자 자신에게도 soul-direct 피해 (bypassRCT)
        DamageContext selfCtx = DamageContext.builder(null, player, IDamageSource.SOUL_DIRECT, bd(1))
                .soulDirect().bypassRCT().skillName("self_embodiment_recoil").build();
        JJKMod.getCombatPipeline().process(selfCtx);

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
