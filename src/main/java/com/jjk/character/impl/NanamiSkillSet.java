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

import java.util.Comparator;
import java.util.List;

public class NanamiSkillSet implements ISkillSet {

    // §LOCK: techniques.json 수치와 동일 — 변경 금지
    private static final float BD_F   = 50f;
    private static final float BD_SR  = 86f;
    private static final float BD_SF  = 60f;   // 무장해체
    private static final float BD_V   = 78f;   // 십: 분
    private static final int   CE_F   = 110, CD_F   = 6,  ANIM_F   = 1;
    private static final int   CE_SR  = 240, CD_SR  = 20, ANIM_SR  = 54;
    private static final int   CE_SF  = 160, CD_SF  = 10, ANIM_SF  = 60; // 무장해체
    private static final int   CD_R   = 8,               ANIM_R   = 61; // 극한초과 (CE 없음)
    private static final int   CE_V   = 200, CD_V   = 14, ANIM_V   = 62; // 십: 분
    private static final int   DISMANTLE_CD_DELAY = 40;                    // 무장해체 쿨타임 지연

    // 약점 배율 — spec §6-10 명시값, config 불필요
    private static final float WEAKNESS_MULT      = 1.5f;
    private static final float PUNCTURE_WEAK_MULT = 1.30f; // 십: 분 약점 배율
    // 경계선 방어 관통 — spec §6-10 명시값 (30% 관통 = 0.7f 배율)
    private static final float BOUNDARY_DEF_MULT  = 0.7f;
    private static final float PUNCTURE_DEF_MULT  = 0.7f;  // 십: 분 방어 관통 30%
    // 극한초과: 오후 6시 기준 월드 타임 (13000틱 이상) — 테스트 접근용 public
    public static final long  OVERTIME_TIME_THRESHOLD = 13000L;
    public static final float OVERTIME_MULTIPLIER     = 2.50f;

    // ── Legacy API (use 경로) ────────────────────────────────────────────────

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
            case 0 -> CD_F;
            case 1 -> CD_SF;
            case 2 -> CD_R;
            case 3 -> CD_SR;
            case 4 -> CD_V;
            default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) {
        return switch (keyId) {
            case 0 -> CE_F;
            case 1 -> CE_SF;
            case 2 -> 0;
            case 3 -> CE_SR;
            case 4 -> CE_V;
            default -> 0;
        };
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "십획주법";
            case 1 -> "nanami_dismantle";
            case 2 -> "nanami_overtime";
            case 3 -> "경계선";
            case 4 -> "nanami_ten_puncture";
            default -> "not_implemented";
        };
    }

    // ── onX 경로 (ISkillSet — player=null 허용, 테스트용) ───────────────────

    /** F — 십획주법 (7:3 약점 판정) */
    @Override
    public SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick) {
        // 쿨타임 체크
        if (data.cooldowns.getOrDefault("0", 0L) > tick) return SkillResult.ON_COOLDOWN;
        // skill_seal 체크
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        // CE 체크
        if (data.ceCurrent < CE_F) return SkillResult.FAIL_CE_INSUFFICIENT;
        // player=null → 테스트 경로 종료
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        // CE 소모
        data.ceCurrent -= CE_F;

        // 전방 8블록 이내 가장 가까운 적 탐색
        List<LivingEntity> targets = HitValidator.getNearby(player, 8.0);
        LivingEntity target = targets.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(player)))
                .orElse(null);

        // 없으면 CE 환불
        if (target == null) {
            data.ceCurrent += CE_F;
            return SkillResult.FAIL_NO_TARGET;
        }

        // 약점 판정 (7:3 — 등 뒤 60도 범위)
        boolean isWeakness = WeaknessZoneCalculator.isWeaknessHit(player, target);
        float bd = BD_F * (isWeakness ? WEAKNESS_MULT : 1.0f);

        // 전투
        DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, bd)
                .skillName("십획주법").keyId(0).build();
        JJKMod.getCombatPipeline().process(ctx);

        // 쿨타임 세팅 + 저장 + 애니메이션
        data.cooldowns.put("0", tick + CD_F);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_F);
        return SkillResult.SUCCESS;
    }

    /** Shift+F — 무장해체 (전방 arc 5블록 × 90도, 피격 대상 쿨타임 +40틱) */
    @Override
    public SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("1", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_SF) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        // [BUG-02] LinkedHashSet으로 중복 제거 — arc 내 동일 엔티티 2회 히트 방지
        java.util.Set<LivingEntity> targets =
            new java.util.LinkedHashSet<>(HitValidator.getNearbyArc(player, 5.0, 90f));
        if (targets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= CE_SF;

        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target,
                    IDamageSource.NORMAL_TECHNIQUE, BD_SF)
                .skillName("nanami_dismantle").keyId(1).build();
            JJKMod.getCombatPipeline().process(ctx);

            // 피격 대상 쿨타임 전체 +40틱 지연
            if (target instanceof ServerPlayerEntity targetPlayer) {
                com.jjk.data.PlayerData targetData =
                    JJKMod.getPlayerRepository().load(targetPlayer.getUuid());
                targetData.cooldowns.replaceAll((k, v) -> v + DISMANTLE_CD_DELAY);
                JJKMod.getPlayerRepository().save(targetData);
            }
        }

        data.cooldowns.put("1", tick + CD_SF);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SF);
        return SkillResult.SUCCESS;
    }

    /** R — 극한초과 (오후 6시 이후 공격력 ×2.50, 60틱 유지) */
    @Override
    public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("2", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        long timeOfDay = player.getWorld().getTimeOfDay() % 24000L;
        float multiplier = getOvertimeMultiplier(timeOfDay);

        // 60틱간 overtime 배율 보존 (float→long 인코딩 ×1000)
        data.cooldowns.put("overtime_multiplier_until", tick + 60L);
        data.cooldowns.put("overtime_multiplier_value", (long)(multiplier * 1000));

        data.cooldowns.put("2", tick + CD_R);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_R);
        return SkillResult.SUCCESS;
    }

    /** Shift+R — 경계선 (전방 10블록 선분, 방어 관통 30%) */
    @Override
    public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) {
        // 쿨타임 체크
        if (data.cooldowns.getOrDefault("3", 0L) > tick) return SkillResult.ON_COOLDOWN;
        // skill_seal 체크
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        // CE 체크
        if (data.ceCurrent < CE_SR) return SkillResult.FAIL_CE_INSUFFICIENT;
        // player=null → 테스트 경로 종료
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        // 전방 10블록 선분 판정
        Vec3d facing = player.getRotationVec(1.0f);
        Vec3d origin = player.getEyePos();
        List<LivingEntity> nearby = HitValidator.getNearby(player, 10.0);
        List<LivingEntity> lineTargets = nearby.stream()
                .filter(e -> isOnLine(origin, facing, e, 10.0, 1.0))
                .toList();

        if (lineTargets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        // CE 소모
        data.ceCurrent -= CE_SR;

        // 각 대상 전투 (방어 관통 30% = defenseMultiplier 0.7f)
        for (LivingEntity target : lineTargets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_SR)
                    .skillName("경계선").keyId(3)
                    .defenseMultiplier(BOUNDARY_DEF_MULT)
                    .build();
            JJKMod.getCombatPipeline().process(ctx);
        }

        // 쿨타임 세팅 + 저장 + 애니메이션
        data.cooldowns.put("3", tick + CD_SR);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SR);
        return SkillResult.SUCCESS;
    }

    /** V — 십: 분 (7:3 약점 판정, 방어 관통 30%, 극한초과 연계) */
    @Override
    public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("4", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_V) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        List<LivingEntity> targets = HitValidator.getNearbyArc(player, 4.0, 60f);
        if (targets.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= CE_V;

        // 극한초과 배율 조회
        float overtimeMult = 1.0f;
        Long overtimeUntil = data.cooldowns.get("overtime_multiplier_until");
        if (overtimeUntil != null && overtimeUntil > tick) {
            long encoded = data.cooldowns.getOrDefault("overtime_multiplier_value", 1000L);
            overtimeMult = encoded / 1000.0f;
        }

        for (LivingEntity target : targets) {
            // 7:3 약점 판정: 시전자 눈높이 ≤ 대상 하단 30% → 약점
            boolean weak = isWeakpointHit(player, target);
            float bd = applyWeaknessBonus(BD_V, weak) * overtimeMult;

            DamageContext ctx = DamageContext.builder(player, target,
                    IDamageSource.NORMAL_TECHNIQUE, bd)
                .skillName(weak ? "nanami_ten_puncture_weak" : "nanami_ten_puncture")
                .keyId(4)
                .defenseMultiplier(PUNCTURE_DEF_MULT)
                .build();
            JJKMod.getCombatPipeline().process(ctx);
        }

        data.cooldowns.put("4", tick + CD_V);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_V);
        return SkillResult.SUCCESS;
    }

    // ── 정적 헬퍼 (테스트 가능) ──────────────────────────────────────────────

    /** 월드 시간 기준 극한초과 배율 반환. 13000틱 이상 = ×2.50, 미만 = ×1.00. */
    public static float getOvertimeMultiplier(long worldTimeOfDay) {
        return worldTimeOfDay % 24000L >= OVERTIME_TIME_THRESHOLD ? OVERTIME_MULTIPLIER : 1.0f;
    }

    /** 약점 보너스 적용 (7:3 법칙 하단 30%). */
    public static float applyWeaknessBonus(float damage, boolean isWeakpoint) {
        return isWeakpoint ? damage * PUNCTURE_WEAK_MULT : damage;
    }

    /** 시전자 눈높이가 대상 높이의 30% 이하이면 약점. */
    public static boolean isWeakpointHit(ServerPlayerEntity attacker, LivingEntity target) {
        return attacker.getEyeY() <= target.getY() + target.getHeight() * 0.30;
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────────

    /** origin 기준 facing 방향 선분 위에 target이 있는지 판정 (maxLateral = 선분 폭 반값) */
    private static boolean isOnLine(Vec3d origin, Vec3d facing,
                                    LivingEntity target, double maxDist, double maxLateral) {
        Vec3d toTarget = target.getPos().subtract(origin);
        double forward = toTarget.dotProduct(facing);
        if (forward <= 0 || forward > maxDist) return false;
        Vec3d lateral = toTarget.subtract(facing.multiply(forward));
        return lateral.length() <= maxLateral;
    }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        var pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }

    // ── JJKMod TickScheduler 등록용 정적 메서드 (V=NOT_IMPLEMENTED이므로 항상 no-op) ──

    public static void tickRCT(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (!"nanami".equals(data.characterId) || !data.healingActive) return;
        // healingActive는 현재 나나미 스킬셋에서 활성화하지 않으므로 실질 no-op
    }
}
