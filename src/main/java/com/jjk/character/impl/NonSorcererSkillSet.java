package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.combat.IDamageSource;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.CooldownManager;
import com.jjk.combat.DamageContext;
import com.jjk.combat.HitValidator;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.List;

// 비술사(천여주박) — CE 술식 없이 신체 능력만으로 싸우는 전사 진영. characterId="todo" (표시이름: 비술사)
public class NonSorcererSkillSet implements ISkillSet {

    public static final Identifier BURST_SPEED_MODIFIER_ID =
            Identifier.of("jjk", "non_sorcerer_burst_speed");

    // key 0: 강화주먹, 1: 강화질주, 2: 파쇄격, 3: 부기우기, 4: 불굴
    private static final float BD_F = 28f;   // 강화주먹
    private static final float BD_R = 40f;   // 파쇄격

    private static final int CD_F  = 3,    ANIM_F  = 64;
    private static final int CD_SF = 40,   ANIM_SF = 64;
    private static final int CD_R  = 60,   ANIM_R  = 64;
    private static final int CD_SR = 100,  ANIM_SR = 64;
    private static final int CD_V  = 300,  ANIM_V  = 64;

    private static final long  BURST_DURATION_TICKS  = 600L;
    private static final float BURST_ATK_MULT        = 1.6f;
    private static final float BURST_DEF_MULT        = 1.4f;
    private static final long  SHIELD_DURATION_TICKS = 100L;
    private static final float KNOCKBACK_STRENGTH    = 2.0f;

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useEnhancedFist(player);
            case 1 -> useEnhancedDash(player);
            case 2 -> useShatteringStrike(player);
            case 3 -> useBoogieWoogie(player);
            case 4 -> useIndomitable(player);
            default -> SkillResult.FAIL;
        };
    }

    @Override
    public boolean canUse(ServerPlayerEntity player, int keyId) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        return CooldownManager.isReady(data, cdKey(keyId), tick);
    }

    @Override
    public int getCooldownTicks(int keyId) {
        return switch (keyId) {
            case 0 -> CD_F; case 1 -> CD_SF; case 2 -> CD_R;
            case 3 -> CD_SR; case 4 -> CD_V; default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) { return 0; }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "enhanced_fist"; case 1 -> "enhanced_dash"; case 2 -> "shattering_strike";
            case 3 -> "boogie_woogie"; case 4 -> "indomitable"; default -> "unknown";
        };
    }

    private static String cdKey(int keyId) { return "cd_todo_" + keyId; }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        AnimationTriggerS2CPacket pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }

    // 강화주먹 — 전방 3블록 근접 타격. CE 소모 없음
    private SkillResult useEnhancedFist(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(0), tick)) return SkillResult.ON_COOLDOWN;

        List<LivingEntity> targets = HitValidator.getNearbyArc(player, 3.0, 120f);
        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_F)
                    .skillName("enhanced_fist")
                    .keyId(0)
                    .build();
            JJKMod.getCombatPipeline().process(ctx);
        }

        CooldownManager.set(data, cdKey(0), tick, CD_F);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_F);
        return SkillResult.SUCCESS;
    }

    // 강화질주 — 전방 8블록 순간이동. 장애물(고체 블록) 있으면 충돌 직전 위치까지만 이동
    private SkillResult useEnhancedDash(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(1), tick)) return SkillResult.ON_COOLDOWN;

        Vec3d start = player.getEyePos();
        Vec3d dir   = player.getRotationVec(1.0f);
        Vec3d wanted = start.add(dir.multiply(8.0));

        BlockHitResult hit = player.getWorld().raycast(new RaycastContext(
                start, wanted, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player));
        Vec3d destEye = hit.getType() == HitResult.Type.BLOCK ? hit.getPos() : wanted;
        Vec3d dest = destEye.subtract(0, player.getStandingEyeHeight(), 0);

        player.teleport(player.getServerWorld(), dest.x, dest.y, dest.z, player.getYaw(), player.getPitch());

        CooldownManager.set(data, cdKey(1), tick, CD_SF);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SF);
        return SkillResult.SUCCESS;
    }

    // 파쇄격 — 전방 4블록 단일 타겟 강타 + 넉백 3블록
    private SkillResult useShatteringStrike(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(2), tick)) return SkillResult.ON_COOLDOWN;

        List<LivingEntity> targets = HitValidator.getNearbyArc(player, 4.0, 90f);
        if (!targets.isEmpty()) {
            LivingEntity target = targets.get(0);
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_R)
                    .skillName("shattering_strike")
                    .keyId(2)
                    .build();
            JJKMod.getCombatPipeline().process(ctx);

            Vec3d dir = target.getPos().subtract(player.getPos()).normalize();
            target.setVelocity(dir.multiply(KNOCKBACK_STRENGTH));
            target.velocityModified = true;
        }

        CooldownManager.set(data, cdKey(2), tick, CD_R);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_R);
        return SkillResult.SUCCESS;
    }

    // 부기우기 — 6블록 이내 가장 가까운 엔티티와 위치 교체
    private SkillResult useBoogieWoogie(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(3), tick)) return SkillResult.ON_COOLDOWN;

        Box box = player.getBoundingBox().expand(6.0);
        List<LivingEntity> nearby = player.getServerWorld().getEntitiesByClass(
                LivingEntity.class, box, e -> e != player && e.isAlive());
        if (nearby.isEmpty()) {
            player.sendMessage(Text.literal("§c[부기우기] 범위 내 대상이 없습니다."), false);
            return SkillResult.FAIL_NO_TARGET;
        }
        LivingEntity target = nearby.stream()
                .min(java.util.Comparator.comparingDouble(e -> e.squaredDistanceTo(player)))
                .orElseThrow();

        Vec3d playerPos = player.getPos();
        Vec3d targetPos = target.getPos();
        player.teleport(player.getServerWorld(), targetPos.x, targetPos.y, targetPos.z,
                player.getYaw(), player.getPitch());
        if (target instanceof ServerPlayerEntity tp) {
            tp.teleport(tp.getServerWorld(), playerPos.x, playerPos.y, playerPos.z,
                    tp.getYaw(), tp.getPitch());
        } else {
            target.refreshPositionAndAngles(playerPos.x, playerPos.y, playerPos.z,
                    target.getYaw(), target.getPitch());
        }

        CooldownManager.set(data, cdKey(3), tick, CD_SR);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SR);
        return SkillResult.SUCCESS;
    }

    // 불굴 — 100틱간 받는 피해 ×0.70 + 사망 방지 1회
    private SkillResult useIndomitable(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(4), tick)) return SkillResult.ON_COOLDOWN;

        data.nsShieldExpireTick = tick + SHIELD_DURATION_TICKS;
        data.nsDeathPreventUsed = false;

        CooldownManager.set(data, cdKey(4), tick, CD_V);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_V);
        return SkillResult.SUCCESS;
    }

    // ─── 순수 PlayerData 경로 (ISkillSet §default 오버라이드) ──────────────────
    @Override public SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : useEnhancedFist(player); }
    @Override public SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) { return player == null ? SkillResult.SUCCESS : useEnhancedDash(player); }
    @Override public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : useShatteringStrike(player); }
    @Override public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) { return player == null ? SkillResult.SUCCESS : useBoogieWoogie(player); }
    @Override public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : useIndomitable(player); }
}
