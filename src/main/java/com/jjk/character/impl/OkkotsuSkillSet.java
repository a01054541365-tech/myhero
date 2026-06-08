package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.combat.IDamageSource;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.CooldownManager;
import com.jjk.combat.DamageContext;
import com.jjk.combat.TechniqueLoader;
import com.jjk.combat.HitValidator;
import com.jjk.data.PlayerData;
import com.jjk.entity.RikaEntity;
import com.jjk.entity.ShikigamiEntityTypes;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.List;

// §6-5 옷코츠 유타 스킬셋
public class OkkotsuSkillSet implements ISkillSet {

    // key 0: rika_summon, 1: sword_slash, 2: copy_technique, 3: okkotsu_true_mutual_love, 4: rct
    // CE/CD 수치: jjk_spec_v5.md §6-5
    private static final int CE_0 = 260,  CD_0 = 30,  ANIM_0 = 50;
    private static final int CE_1 = 180,  CD_1 = 12,  ANIM_1 = 40;
    private static final int CE_2 = 300,  CD_2 = 45,  ANIM_2 = 52;
    private static final int CE_3 = 3000, CD_3 = 480, ANIM_3 = 41;  // 진판상애절단 영역
    private static final int CD_4 = 8;   // RCT: 업프런트 CE 없음, 드레인 방식

    private static final float SWORD_SLASH_DAMAGE = 60f;

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useRikaSummon(player);
            case 1 -> useSwordSlash(player);
            case 2 -> useCopyTechnique(player);
            case 3 -> useTrueMutualLove(player);
            case 4 -> useRCT(player);
            default -> SkillResult.FAIL;
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
            case 0 -> CD_0; case 1 -> CD_1; case 2 -> CD_2;
            case 3 -> CD_3; case 4 -> CD_4; default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) {
        return switch (keyId) {
            case 0 -> CE_0; case 1 -> CE_1; case 2 -> CE_2;
            case 3 -> CE_3; case 4 -> 0; default -> 0;
        };
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "rika_summon"; case 1 -> "sword_slash"; case 2 -> "copy_technique";
            case 3 -> "okkotsu_true_mutual_love"; case 4 -> "reverse_cursed_technique"; default -> "unknown";
        };
    }

    private static String cdKey(int keyId) { return "cd_okkotsu_" + keyId; }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        var pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }

    // ─── key 0: 리카 소환 ────────────────────────────────────────────────────────
    private SkillResult useRikaSummon(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(0), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_0)) return SkillResult.CE_INSUFFICIENT;

        RikaEntity rika = new RikaEntity(ShikigamiEntityTypes.RIKA, player.getServerWorld(), player.getUuid());
        rika.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch());
        player.getServerWorld().spawnEntity(rika);

        JJKMod.getCEManager().consume(player, CE_0);
        CooldownManager.set(data, cdKey(0), tick, CD_0);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_0);
        return SkillResult.SUCCESS;
    }

    // ─── key 1: 검격 ─────────────────────────────────────────────────────────────
    private SkillResult useSwordSlash(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(1), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_1)) return SkillResult.CE_INSUFFICIENT;

        Vec3d facing = player.getRotationVec(1.0f);
        double cosHalf = Math.cos(Math.toRadians(45.0));  // ±45도 arc
        List<LivingEntity> targets = HitValidator.getNearby(player, 3.5).stream()
                .filter(e -> {
                    Vec3d toTarget = e.getPos().subtract(player.getPos()).normalize();
                    return toTarget.dotProduct(facing) >= cosHalf;
                })
                .toList();

        float burstMult = TechniqueLoader.getBurstMultiplier("okkotsu", 3);
        float externalBuff = data.burstActive ? burstMult : 1.0f;
        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target,
                            IDamageSource.NORMAL_TECHNIQUE, SWORD_SLASH_DAMAGE)
                    .externalBuffMult(externalBuff)
                    .keyId(1)
                    .skillName("sword_slash")
                    .build();
            JJKMod.getCombatPipeline().process(ctx);
        }

        JJKMod.getCEManager().consume(player, CE_1);
        CooldownManager.set(data, cdKey(1), tick, CD_1);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_1);
        return SkillResult.SUCCESS;
    }

    // ─── key 2: 복사 술식 (§6-5) ─────────────────────────────────────────────────
    private SkillResult useCopyTechnique(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(2), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_2)) return SkillResult.CE_INSUFFICIENT;

        // 복사 대상 스킬 존재 여부 확인
        if (data.lastReceivedSkillId == null) return SkillResult.FAIL_CONDITION;
        // 영역 스킬 복사 불가 — §6-5
        if (data.lastReceivedIsDomain) return SkillResult.FAIL_CONDITION;

        // 복사본 수치 (§6-5: 기본 ×0.7, CE ×1.5, CD ×1.5)
        int copiedDamage   = (int)(data.lastReceivedBaseDamage    * 0.7f);
        int copiedCeCost   = (int)(data.lastReceivedCeCost        * 1.5f);
        int copiedCooldown = (int)(data.lastReceivedCooldownTicks * 1.5f);

        // 기본 발동 CE 300 소모
        JJKMod.getCEManager().consume(player, CE_2);

        // 복사본 추가 CE 검증·소모
        if (copiedCeCost > 0) {
            if (!JJKMod.getCEManager().canAfford(player, copiedCeCost)) {
                // §6-5: 이미 소모한 300 CE는 환불하지 않음
                CooldownManager.set(data, cdKey(2), tick, CD_2);
                JJKMod.getPlayerRepository().save(data);
                return SkillResult.CE_INSUFFICIENT;
            }
            JJKMod.getCEManager().consume(player, copiedCeCost);
        }

        // 전방 3.5블록 이내 첫 번째 적에게 복사본 발동
        List<LivingEntity> nearby = HitValidator.getNearby(player, 3.5);
        if (!nearby.isEmpty()) {
            LivingEntity target = nearby.get(0);
            DamageContext ctx = DamageContext.builder(player, target,
                            IDamageSource.NORMAL_TECHNIQUE, copiedDamage)
                    .keyId(2)
                    .skillName(data.lastReceivedSkillId)
                    .ceCost(copiedCeCost)
                    .cooldownTicks(copiedCooldown)
                    .isDomainSkill(false)
                    .alreadyConsumedCE(true)
                    .build();
            JJKMod.getCombatPipeline().process(ctx);
        }

        // 사용 후 lastReceivedSkill 초기화
        data.lastReceivedSkillId = null;
        CooldownManager.set(data, cdKey(2), tick, CD_2);
        JJKMod.getPlayerRepository().save(data);
        return SkillResult.SUCCESS;
    }

    // ─── key 3: 진판상애절단 영역 전개 — 복사 술식 필중 발동 후 종료 ──────────────
    private SkillResult useTrueMutualLove(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(3), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_3)) return SkillResult.CE_INSUFFICIENT;
        if (data.lastReceivedSkillId == null) return SkillResult.FAIL_CONDITION;

        // deployDomain이 CE 차감 + domainCooldownUntil + saveImmediate 처리
        if (!JJKMod.getDomainManager().deployDomain(player, "okkotsu_true_mutual_love", player.getBlockPos()))
            return SkillResult.FAIL_CONDITION;

        // deployDomain 이후 freshLoad로 CooldownManager 설정 (CE double-deduction 방지)
        PlayerData fresh = JJKMod.getPlayerRepository().load(player.getUuid());
        CooldownManager.set(fresh, cdKey(3), tick, CD_3);
        JJKMod.getPlayerRepository().save(fresh);
        broadcastAnim(player, ANIM_3);
        return SkillResult.SUCCESS;
    }

    // ─── key 4: 반전술식 (토글, CE 드레인 방식) ──────────────────────────────────
    private SkillResult useRCT(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(4), tick)) return SkillResult.ON_COOLDOWN;
        data.healingActive = !data.healingActive;
        CooldownManager.set(data, cdKey(4), tick, CD_4);

        // 조준 대상 타인 치유 (6블록 이내 최근접 아군 우선)
        if (data.healingActive) {
            ServerPlayerEntity target = findHealTarget(player, 6.0);
            if (target != null && !target.equals(player)) {
                startHealOther(player, target, tick);
            }
        }

        JJKMod.getPlayerRepository().save(data);
        return SkillResult.SUCCESS;
    }

    private static ServerPlayerEntity findHealTarget(ServerPlayerEntity healer, double radius) {
        return healer.getServerWorld().getPlayers().stream()
                .filter(p -> !p.equals(healer) && p.squaredDistanceTo(healer) <= radius * radius)
                .findFirst()
                .orElse(null);
    }

    private static void startHealOther(ServerPlayerEntity healer, ServerPlayerEntity target, long tick) {
        PlayerData healerData = JJKMod.getPlayerRepository().load(healer.getUuid());
        PlayerData targetData = JJKMod.getPlayerRepository().load(target.getUuid());
        float healPerTick = JJKMod.getConfig().reverseHealOtherPerTick();    // 0.4
        float ceDrain     = healerData.ceMax * JJKMod.getConfig().reverseCeDrainOtherRatio(); // 0.012
        if (!JJKMod.getCEManager().consumeCE(healerData, ceDrain)) return;
        targetData.hpCurrent = Math.min(targetData.hpCurrent + healPerTick, targetData.hpMax);
        JJKMod.getPlayerRepository().save(healerData);
        JJKMod.getPlayerRepository().save(targetData);
    }

    // ─── 순수 PlayerData 경로 (ISkillSet §default 오버라이드) ──────────────────
    @Override public SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : useRikaSummon(player); }
    @Override public SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) { return player == null ? SkillResult.SUCCESS : useSwordSlash(player); }
    @Override public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : useCopyTechnique(player); }
    @Override public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (player == null) {
            data.burstActive = true;
            data.burstEndTick = tick + 200;
            return SkillResult.SUCCESS;
        }
        return useTrueMutualLove(player);
    }
    @Override public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : useRCT(player); }
}
