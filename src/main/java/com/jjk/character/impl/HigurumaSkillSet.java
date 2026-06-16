package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.combat.IDamageSource;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.DamageContext;
import com.jjk.combat.HitValidator;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import com.jjk.network.s2c.SealedSkillSyncS2CPacket;
import com.jjk.network.s2c.SkillEffectS2CPacket;
import com.jjk.trial.TrialManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.List;

public class HigurumaSkillSet implements ISkillSet {

    // 수치는 techniques.json 단일 기준 (2026-06-11 데이터 주도 전환)
    private static final String CHAR_ID = "higuruma";
    private static final int ANIM_0 = 38, ANIM_1 = 38, ANIM_2 = 38, ANIM_3 = 55, ANIM_4 = 39;

    private static float bd(int keyId) { return com.jjk.combat.TechniqueLoader.getBaseDamage(CHAR_ID, keyId); }
    private static int   ce(int keyId) { return (int) com.jjk.combat.TechniqueLoader.getCeCost(CHAR_ID, keyId); }
    private static int   cd(int keyId) { return (int) com.jjk.combat.TechniqueLoader.getCooldownTicks(CHAR_ID, keyId); }

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        return switch (keyId) {
            case 0 -> useSubmitEvidence(player);
            case 1 -> onShiftF(data, player, tick);
            case 2 -> onR(data, player, tick);
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
        return (keyId >= 0 && keyId <= 3) ? cd(keyId) : 0;
    }

    @Override
    public int getCeCost(int keyId) {
        return (keyId >= 0 && keyId <= 3) ? ce(keyId) : 0;
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "submit_evidence";
            case 1 -> "culpable_hit";
            case 2 -> "evidence_amplify";
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
        if (data.ceCurrent < ce(0)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        LivingEntity found = findAimedTarget(player, 8.0);
        if (!(found instanceof ServerPlayerEntity target)) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= ce(0);
        JJKMod.getTrialManager().addEvidence(player, target);
        data.cooldowns.put("0", tick + cd(0));
        broadcastAnim(player, ANIM_0);
        return SkillResult.SUCCESS;
    }

    /** Shift+F — 심판의 일격: 증거 1개 소모, 명중 시 55데미지 + 피격자 마지막 사용 스킬 봉인 */
    @Override
    public SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("1", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < ce(1)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        LivingEntity found = findAimedTarget(player, 8.0);
        if (!(found instanceof ServerPlayerEntity targetPlayer)) return SkillResult.FAIL_NO_TARGET;

        PlayerData targetData = JJKMod.getPlayerRepository().load(targetPlayer.getUuid());
        if (TrialManager.getEvidenceCount(targetData, tick) < 1) {
            player.sendMessage(Text.literal("§c[히구루마] 증거가 부족합니다."), false);
            return SkillResult.FAIL_CONDITION;
        }

        data.ceCurrent -= ce(1);
        TrialManager.consumeEvidence(targetData, tick);

        DamageContext ctx = DamageContext.builder(player, targetPlayer, IDamageSource.NORMAL_TECHNIQUE, bd(1))
                .skillName("culpable_hit").keyId(1).build();
        JJKMod.getCombatPipeline().process(ctx);

        String sealedSkillId = targetData.lastUsedSkillId;
        if (sealedSkillId != null && !sealedSkillId.isEmpty()) {
            targetData.sealedSkills.add(sealedSkillId);
            targetData.sealExpireTick = tick + JJKMod.getConfig().sealDurationTicks;

            var pkt = new SealedSkillSyncS2CPacket(targetPlayer.getUuid(), sealedSkillId, targetData.sealExpireTick);
            for (ServerPlayerEntity p : player.getServer().getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(p, pkt);
            }
        }
        JJKMod.getPlayerRepository().saveImmediate(targetData);

        data.cooldowns.put("1", tick + cd(1));
        JJKMod.getPlayerRepository().save(data);

        sendEffect(player, "higuruma_culpable_hit");
        broadcastAnim(player, ANIM_1);
        return SkillResult.SUCCESS;
    }

    /** R — 증거 강화: 조준 대상에 증거 1개 추가 부여 + 다음 jury 판결 성공률 +30%(1회) */
    @Override
    public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("2", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < ce(2)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (data.evidenceAmplifyActive) return SkillResult.FAIL_CONDITION;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        LivingEntity found = findAimedTarget(player, 8.0);
        if (!(found instanceof ServerPlayerEntity targetPlayer)) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= ce(2);
        data.evidenceAmplifyActive = true;
        JJKMod.getTrialManager().addEvidence(player, targetPlayer);
        data.cooldowns.put("2", tick + cd(2));
        JJKMod.getPlayerRepository().save(data);

        player.sendMessage(Text.literal("§e[히구루마] 증거를 강화했습니다. 다음 재판 성공률이 상승합니다."), false);
        sendEffect(player, "higuruma_evidence_amplify");
        broadcastAnim(player, ANIM_2);
        return SkillResult.SUCCESS;
    }

    @Override
    public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("3", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < ce(3)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        LivingEntity found = findAimedTarget(player, 8.0);
        if (!(found instanceof ServerPlayerEntity target)) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= ce(3);
        JJKMod.getTrialManager().startTrial(player, target);
        data.cooldowns.put("3", tick + cd(3));
        broadcastAnim(player, ANIM_3);
        return SkillResult.SUCCESS;
    }

    @Override
    public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick) {
        if (!data.hasExecutionSword) return SkillResult.FAIL_CONDITION;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        LivingEntity target = findAimedTarget(player, 5.0);
        if (target == null) return SkillResult.FAIL_NO_TARGET;

        DamageContext ctx = DamageContext.builder(player, target, IDamageSource.EXECUTIONER_SWORD, bd(4))
                .skillName("executioner_sword").keyId(4).bypassPvpCap().build();
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
        if (!JJKMod.getCEManager().canAfford(player, ce(0))) return SkillResult.CE_INSUFFICIENT;

        LivingEntity found = findAimedTarget(player, 8.0);
        if (!(found instanceof ServerPlayerEntity target)) return SkillResult.FAIL;

        JJKMod.getTrialManager().addEvidence(player, target);

        JJKMod.getCEManager().consume(player, ce(0));
        data.cooldowns.put("skill_0", tick + cd(0));
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_0);
        return SkillResult.SUCCESS;
    }

    // H/Shift+R — 배심원 소환
    private SkillResult useJury(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (data.cooldowns.getOrDefault("skill_3", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, ce(3))) return SkillResult.CE_INSUFFICIENT;

        LivingEntity found = findAimedTarget(player, 8.0);
        if (!(found instanceof ServerPlayerEntity target)) return SkillResult.FAIL;

        JJKMod.getTrialManager().startTrial(player, target);

        JJKMod.getCEManager().consume(player, ce(3));
        data.cooldowns.put("skill_3", tick + cd(3));
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

        DamageContext ctx = DamageContext.builder(player, target, IDamageSource.EXECUTIONER_SWORD, bd(4))
                .skillName("executioner_sword").bypassPvpCap()
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

    private static void sendEffect(ServerPlayerEntity player, String effectType) {
        var pos = player.getPos();
        SkillEffectS2CPacket pkt = SkillEffectS2CPacket.of(effectType, player.getUuid(), pos.x, pos.y, pos.z);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }
}
