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

public class HigurumaSkillSet implements ISkillSet {

    private static final int CE_0 = 180, CD_0 = 15, ANIM_0 = 38;
    private static final int CE_1 = 250, CD_1 = 30, ANIM_1 = 63; // 검사 논고
    private static final int CE_2 = 180, CD_2 = 60, ANIM_2 = 64; // 증거 인멸
    private static final int CE_3 = 600, CD_3 = 90, ANIM_3 = 55;
    private static final int ANIM_4 = 39;

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useSubmitEvidence(player);
            case 1 -> useProsecutorArgument(player);
            case 2 -> useEvidenceDestruction(player);
            case 3 -> useJury(player);
            case 4 -> useExecutionerSword(player);
            default -> SkillResult.FAIL;
        };
    }

    @Override
    public boolean canUse(ServerPlayerEntity player, int keyId) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (keyId == 4) {
            return data.hasExecutionSword;
        }
        long tick = player.getWorld().getTime();
        return data.cooldowns.getOrDefault("skill_" + keyId, 0L) <= tick;
    }

    @Override
    public int getCooldownTicks(int keyId) {
        return switch (keyId) {
            case 0 -> CD_0;
            case 1 -> CD_1;
            case 2 -> CD_2;
            case 3 -> CD_3;
            default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) {
        return switch (keyId) {
            case 0 -> CE_0;
            case 1 -> CE_1;
            case 2 -> CE_2;
            case 3 -> CE_3;
            default -> 0;
        };
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "submit_evidence";
            case 1 -> "prosecutor_argument";
            case 2 -> "evidence_destruction";
            case 3 -> "jury";
            case 4 -> "executioner_sword";
            default -> "not_implemented";
        };
    }

    // ── onX PlayerData 경로 ──────────────────────────────────────────────────────

    @Override
    public SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("0", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_0) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        LivingEntity found = findAimedTarget(player, 8.0);
        if (!(found instanceof ServerPlayerEntity target)) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= CE_0;
        JJKMod.getTrialManager().addEvidence(player, target);
        data.cooldowns.put("0", tick + CD_0);
        broadcastAnim(player, ANIM_0);
        return SkillResult.SUCCESS;
    }

    @Override
    public SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("1", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_1) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        List<LivingEntity> targets = HitValidator.getNearbyArc(player, 10.0, 60f);
        data.ceCurrent -= CE_1;
        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayerEntity tp) {
                com.jjk.data.PlayerData td = JJKMod.getPlayerRepository().load(tp.getUuid());
                td.cooldowns.replaceAll((k, v) -> v + 60L);
                JJKMod.getPlayerRepository().save(td);
            }
        }
        data.cooldowns.put("1", tick + CD_1);
        broadcastAnim(player, ANIM_1);
        return SkillResult.SUCCESS;
    }

    @Override
    public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("2", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_2) return SkillResult.FAIL_CE_INSUFFICIENT;

        data.ceCurrent -= CE_2;
        data.cooldowns.put("trial_bonus_until", tick + 60L);
        data.cooldowns.put("2", tick + CD_2);

        if (player != null) {
            broadcastAnim(player, ANIM_2);
        }
        JJKMod.getPlayerRepository().save(data);
        return SkillResult.SUCCESS;
    }

    @Override
    public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("3", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_3) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        LivingEntity found = findAimedTarget(player, 8.0);
        if (!(found instanceof ServerPlayerEntity target)) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= CE_3;
        JJKMod.getTrialManager().startTrial(player, target);
        data.cooldowns.put("3", tick + CD_3);
        broadcastAnim(player, ANIM_3);
        return SkillResult.SUCCESS;
    }

    @Override
    public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick) {
        if (!data.hasExecutionSword) return SkillResult.FAIL_CONDITION;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        LivingEntity target = findAimedTarget(player, 5.0);
        if (target == null) return SkillResult.FAIL_NO_TARGET;

        DamageContext ctx = DamageContext.builder(player, target, IDamageSource.EXECUTIONER_SWORD, 999f)
                .skillName("executioner_sword").keyId(4).build();
        JJKMod.getCombatPipeline().process(ctx);
        data.hasExecutionSword = false;
        JJKMod.getPlayerRepository().saveImmediate(data);
        broadcastAnim(player, ANIM_4);
        return SkillResult.SUCCESS;
    }

    // Shift+F — 검사 논고: 전방 대상 쿨타임 +60틱
    private SkillResult useProsecutorArgument(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (data.cooldowns.getOrDefault("skill_1", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_1)) return SkillResult.CE_INSUFFICIENT;

        List<LivingEntity> targets = HitValidator.getNearbyArc(player, 10.0, 60f);
        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayerEntity tp) {
                com.jjk.data.PlayerData td = JJKMod.getPlayerRepository().load(tp.getUuid());
                td.cooldowns.replaceAll((k, v) -> v + 60L);
                JJKMod.getPlayerRepository().save(td);
            }
        }

        JJKMod.getCEManager().consume(player, CE_1);
        data.cooldowns.put("skill_1", tick + CD_1);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_1);
        return SkillResult.SUCCESS;
    }

    // R — 증거 인멸: 다음 재판 성공률 +20%
    private SkillResult useEvidenceDestruction(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (data.cooldowns.getOrDefault("skill_2", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_2)) return SkillResult.CE_INSUFFICIENT;

        JJKMod.getCEManager().consume(player, CE_2);
        data.cooldowns.put("trial_bonus_until", tick + 60L);
        data.cooldowns.put("skill_2", tick + CD_2);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_2);
        return SkillResult.SUCCESS;
    }

    // F — 증거 제출
    private SkillResult useSubmitEvidence(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (data.cooldowns.getOrDefault("skill_0", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_0)) return SkillResult.CE_INSUFFICIENT;

        LivingEntity found = findAimedTarget(player, 8.0);
        if (!(found instanceof ServerPlayerEntity target)) return SkillResult.FAIL;

        JJKMod.getTrialManager().addEvidence(player, target);

        JJKMod.getCEManager().consume(player, CE_0);
        data.cooldowns.put("skill_0", tick + CD_0);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_0);
        return SkillResult.SUCCESS;
    }

    // H/Shift+R — 배심원 소환
    private SkillResult useJury(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (data.cooldowns.getOrDefault("skill_3", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_3)) return SkillResult.CE_INSUFFICIENT;

        LivingEntity found = findAimedTarget(player, 8.0);
        if (!(found instanceof ServerPlayerEntity target)) return SkillResult.FAIL;

        JJKMod.getTrialManager().startTrial(player, target);

        JJKMod.getCEManager().consume(player, CE_3);
        data.cooldowns.put("skill_3", tick + CD_3);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_3);
        return SkillResult.SUCCESS;
    }

    // V — 처형검 (판결 후 1회성)
    private SkillResult useExecutionerSword(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (!data.hasExecutionSword) return SkillResult.FAIL;

        LivingEntity target = findAimedTarget(player, 8.0);
        if (target == null) return SkillResult.FAIL;

        DamageContext ctx = DamageContext.builder(player, target, IDamageSource.EXECUTIONER_SWORD, 999f)
                .skillName("executioner_sword")
                .build();
        JJKMod.getCombatPipeline().process(ctx);

        data.hasExecutionSword = false;
        JJKMod.getPlayerRepository().saveImmediate(data);
        broadcastAnim(player, ANIM_4);
        return SkillResult.SUCCESS;
    }

    private static LivingEntity findAimedTarget(ServerPlayerEntity player, double range) {
        List<LivingEntity> candidates = HitValidator.getNearbyArc(player, range, 30f);
        return candidates.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(player)))
                .orElse(null);
    }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        var pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }
}
