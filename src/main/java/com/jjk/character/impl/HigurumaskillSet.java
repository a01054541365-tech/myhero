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

public class HigurumaskillSet implements ISkillSet {

    private static final int CE_0 = 120, CD_0 = 15, ANIM_0 = 38;
    private static final int CE_3 = 600, CD_3 = 90, ANIM_3 = 55;
    private static final int ANIM_4 = 39;

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useSubmitEvidence(player);
            case 1, 2 -> SkillResult.NOT_IMPLEMENTED;
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
            case 3 -> CD_3;
            default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) {
        return switch (keyId) {
            case 0 -> CE_0;
            case 3 -> CE_3;
            default -> 0;
        };
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "submit_evidence";
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
        return SkillResult.NOT_IMPLEMENTED;
    }

    @Override
    public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick) {
        return SkillResult.NOT_IMPLEMENTED;
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
