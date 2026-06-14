package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.combat.IDamageSource;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.CooldownManager;
import com.jjk.combat.DamageContext;
import com.jjk.combat.HitValidator;
import com.jjk.data.PlayerData;
import com.jjk.entity.CeProjectileEntity;
import com.jjk.entity.CursedSpiritEntityTypes;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

// §6 쵸소 스킬셋 — 혈도조술 캐릭터
// key 0=천혈, 1=적린약동, 2=사혈, 3=혈도폭쇄, 4=혈도이동
public class ChosoSkillSet implements ISkillSet {

    // 수치는 techniques.json 단일 기준 (2026-06-11 데이터 주도 전환)
    private static final String CHAR_ID = "choso";
    private static final int ANIM_0 = 60, ANIM_1 = 61, ANIM_2 = 62, ANIM_3 = 66, ANIM_4 = 63;

    // 혈도폭쇄(key 3): 전방 원뿔(폭 45°, 깊이 4블록) + 명중 시 SLOW 40틱(2초)
    private static final double HYEOLDO_CONE_DEPTH = 4.0;
    private static final float  HYEOLDO_CONE_ANGLE = 45f;
    private static final long   SLOW_DURATION_TICKS = 40L;

    private static float bd(int keyId) { return com.jjk.combat.TechniqueLoader.getBaseDamage(CHAR_ID, keyId); }
    private static int   ce(int keyId) { return (int) com.jjk.combat.TechniqueLoader.getCeCost(CHAR_ID, keyId); }
    private static int   cd(int keyId) { return (int) com.jjk.combat.TechniqueLoader.getCooldownTicks(CHAR_ID, keyId); }

    private static final float POISON_DMG_PER_HIT = 8f; // 사혈 DoT (techniques.json baseDamage=0)
    private static final int   BLOOD_RESOURCE_MAX  = 5;

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useCheonHyeol(player);
            case 1 -> useJeoklInYakDong(player);
            case 2 -> useSaHyeol(player);
            case 3 -> useHyeoldoPokswae(
                    JJKMod.getPlayerRepository().load(player.getUuid()), player, player.getWorld().getTime());
            case 4 -> useHyeoldoMove(player);
            default -> SkillResult.NOT_IMPLEMENTED;
        };
    }

    @Override
    public boolean canUse(ServerPlayerEntity player, int keyId) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(keyId), tick)) return false;
        int ceCost = getCeCost(keyId);
        return ceCost == 0 || JJKMod.getCEManager().canAfford(player, ceCost);
    }

    @Override
    public int getCooldownTicks(int keyId) {
        return switch (keyId) {
            case 0 -> cd(0); case 1 -> cd(1); case 2 -> cd(2); case 3 -> cd(3); case 4 -> cd(4); default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) {
        return switch (keyId) {
            case 0 -> ce(0); case 1 -> ce(1); case 2 -> ce(2); case 3 -> ce(3); case 4 -> ce(4); default -> 0;
        };
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "천혈"; case 1 -> "적린약동"; case 2 -> "사혈"; case 3 -> "혈도폭쇄"; case 4 -> "혈도_이동";
            default -> "unknown";
        };
    }

    private static String cdKey(int keyId) { return "cd_choso_" + keyId; }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        var pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }

    // ─── key 0: 천혈 — 전방 8블록 혈액 발사체 ──────────────────────────────────
    private SkillResult useCheonHyeol(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(0), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, ce(0))) return SkillResult.CE_INSUFFICIENT;

        CeProjectileEntity projectile = new CeProjectileEntity(
                CursedSpiritEntityTypes.CE_PROJECTILE, player.getServerWorld());
        projectile.setOwner(player);
        projectile.setPosition(player.getX(), player.getEyeY() - 0.1, player.getZ());
        Vec3d vel = player.getRotationVec(1.0f).multiply(1.5);
        projectile.setVelocity(vel.x, vel.y, vel.z);
        projectile.setDamage(bd(0));
        player.getServerWorld().spawnEntity(projectile);

        JJKMod.getCEManager().consume(player, ce(0));
        CooldownManager.set(data, cdKey(0), tick, cd(0));
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_0);
        return SkillResult.SUCCESS;
    }

    // ─── key 1: 적린약동 — 혈액 자원 1 소모, 전방 12블록 부채꼴 ─────────────────
    private SkillResult useJeoklInYakDong(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(1), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, ce(1))) return SkillResult.CE_INSUFFICIENT;
        if (data.bloodResource <= 0) return SkillResult.FAIL_CONDITION;

        Vec3d facing = player.getRotationVec(1.0f);
        double spreadCos = Math.cos(Math.toRadians(30.0)); // ±30° 부채꼴
        List<LivingEntity> targets = HitValidator.getNearby(player, 12.0).stream()
                .filter(e -> {
                    Vec3d toTarget = e.getPos().subtract(player.getPos()).normalize();
                    return toTarget.dotProduct(facing) >= spreadCos;
                })
                .toList();

        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target,
                            IDamageSource.NORMAL_TECHNIQUE, bd(1))
                    .keyId(1)
                    .skillName("적린약동")
                    .build();
            JJKMod.getCombatPipeline().process(ctx);
        }

        data.bloodResource--;
        JJKMod.getCEManager().consume(player, ce(1));
        CooldownManager.set(data, cdKey(1), tick, cd(1));
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_1);
        return SkillResult.SUCCESS;
    }

    // ─── key 2: 사혈 — 근접 대상에게 독 데미지 예약 (20틱 간격 × 3회) ───────────
    private SkillResult useSaHyeol(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(2), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, ce(2))) return SkillResult.CE_INSUFFICIENT;

        List<LivingEntity> nearby = HitValidator.getNearby(player, 4.0);
        if (nearby.isEmpty()) return SkillResult.FAIL_NO_TARGET;

        LivingEntity target = nearby.get(0);
        for (int i = 1; i <= 3; i++) {
            JJKMod.getEffectDeferQueue().schedule(
                    target.getBlockPos(), POISON_DMG_PER_HIT, 20 * i, player.getUuid(), tick);
        }

        JJKMod.getCEManager().consume(player, ce(2));
        CooldownManager.set(data, cdKey(2), tick, cd(2));
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_2);
        return SkillResult.SUCCESS;
    }

    // ─── key 3: 혈도폭쇄 — 전방 원뿔(45°, 4블록) 피해 + 명중 시 SLOW 40틱 ───────
    // data.ceCurrent 직접 경로 (player=null 허용, 테스트 가능). 수치는 techniques.json 로드.
    private SkillResult useHyeoldoPokswae(PlayerData data, ServerPlayerEntity player, long tick) {
        // 2단계: 쿨타임 검증 (decisions §3-1)
        if (!CooldownManager.isReady(data, cdKey(3), tick)) return SkillResult.ON_COOLDOWN;
        // CE 검증 — 부족 시 차감 없이 즉시 반환
        if (data.ceCurrent < ce(3)) return SkillResult.CE_INSUFFICIENT;

        // 발동 확정: CE 소모 + 쿨타임 설정 (범위 내 대상 없어도 발동 성공으로 처리)
        data.ceCurrent -= ce(3);
        CooldownManager.set(data, cdKey(3), tick, cd(3));

        if (player != null) {
            // 전방 원뿔: 같은 진영 제외는 HitValidator.getNearbyArc(TeamManager) 내부에서 처리
            List<LivingEntity> targets =
                    HitValidator.getNearbyArc(player, HYEOLDO_CONE_DEPTH, HYEOLDO_CONE_ANGLE);
            for (LivingEntity target : targets) {
                DamageContext ctx = DamageContext.builder(player, target,
                                IDamageSource.NORMAL_TECHNIQUE, bd(3))
                        .keyId(3)
                        .skillName("혈도폭쇄")
                        .build();
                JJKMod.getCombatPipeline().process(ctx);
                // 명중 시 SLOW 40틱 — PlayerData 보유 대상(플레이어)에만 부여
                if (target instanceof ServerPlayerEntity tp) {
                    applySlow(JJKMod.getPlayerRepository().load(tp.getUuid()), tick);
                }
            }
            broadcastAnim(player, ANIM_3);
        }
        return SkillResult.SUCCESS;
    }

    // 명중 대상에게 SLOW 상태이상 부여 — cooldowns "status_slow" 만료 틱 = tick + 40
    public static void applySlow(PlayerData target, long tick) {
        target.cooldowns.put("status_slow", tick + SLOW_DURATION_TICKS);
    }

    // ─── key 4: 혈도 이동 — 전방 돌진 + 혈액 자원 +1 ──────────────────────────
    private SkillResult useHyeoldoMove(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(4), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, ce(4))) return SkillResult.CE_INSUFFICIENT;

        Vec3d forward = player.getRotationVec(1.0f);
        player.setVelocity(forward.multiply(1.5));
        player.velocityModified = true;

        data.bloodResource = Math.min(data.bloodResource + 1, BLOOD_RESOURCE_MAX);
        JJKMod.getCEManager().consume(player, ce(4));
        CooldownManager.set(data, cdKey(4), tick, cd(4));
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_4);
        return SkillResult.SUCCESS;
    }

    // ─── 순수 PlayerData 경로 (ISkillSet default 오버라이드) ──────────────────
    @Override public SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick) {
        return player == null ? SkillResult.SUCCESS : useCheonHyeol(player);
    }
    @Override public SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (player == null) {
            if (data.bloodResource <= 0) return SkillResult.FAIL_CONDITION;
            data.bloodResource--;
            return SkillResult.SUCCESS;
        }
        return useJeoklInYakDong(player);
    }
    @Override public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick) {
        return player == null ? SkillResult.SUCCESS : useSaHyeol(player);
    }
    @Override public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) {
        return useHyeoldoPokswae(data, player, tick);
    }
    @Override public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick) {
        if (player == null) {
            data.bloodResource = Math.min(data.bloodResource + 1, BLOOD_RESOURCE_MAX);
            return SkillResult.SUCCESS;
        }
        return useHyeoldoMove(player);
    }
}
