package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.advancement.AdvancementTriggerManager;
import com.jjk.api.combat.IDamageSource;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.DamageContext;
import com.jjk.combat.HitValidator;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import com.jjk.network.s2c.SkillResultS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class HakariSkillSet implements ISkillSet {

    // 수치는 techniques.json 단일 기준 (2026-06-11 데이터 주도 전환)
    private static final String CHAR_ID = "hakari";
    private static final int ANIM_F = 36, ANIM_SF = 48, ANIM_R = 1, ANIM_SR = 49, ANIM_V = 37;

    private static float bd(int keyId) { return com.jjk.combat.TechniqueLoader.getBaseDamage(CHAR_ID, keyId); }
    private static int   ce(int keyId) { return (int) com.jjk.combat.TechniqueLoader.getCeCost(CHAR_ID, keyId); }
    private static int   cd(int keyId) { return (int) com.jjk.combat.TechniqueLoader.getCooldownTicks(CHAR_ID, keyId); }

    // 동시 잭팟 상한 (P2-3): TPS 보호
    private static final AtomicInteger activeJackpots = new AtomicInteger(0);
    private static final int MAX_CONCURRENT_JACKPOTS = 2;

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
            case 0 -> "jackpot_activate";
            case 1 -> "power_output";
            case 2 -> "reroll";
            case 3 -> "indeterminate_domain";
            case 4 -> "jackpot_domain";
            default -> "unknown";
        };
    }

    // ── onX PlayerData 경로 ──────────────────────────────────────────────────────

    @Override
    public SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("0", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < ce(0)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_CONDITION;

        data.ceCurrent -= ce(0);
        boolean jackpot = JackpotStateMachine.tryJackpot(data, tick, JJKMod.getConfig());
        if (jackpot) {
            if (activeJackpots.get() >= MAX_CONCURRENT_JACKPOTS) {
                data.jackpotActive = false;
                data.jackpotEndTick = 0L;
                jackpot = false;
            } else {
                activeJackpots.incrementAndGet();
                long streak = data.cooldowns.getOrDefault("adv_jackpot_streak", 0L) + 1L;
                data.cooldowns.put("adv_jackpot_streak", streak);
                AdvancementTriggerManager.onHakariJackpotStreak(player, (int) streak);
            }
        }
        if (!jackpot) {
            data.ceCurrent += ce(0);
            data.cooldowns.put("adv_jackpot_streak", 0L);
        }
        data.cooldowns.put("0", tick + cd(0));
        ServerPlayNetworking.send(player,
                new SkillResultS2CPacket(0, jackpot ? "jackpot_success" : "jackpot_fail", 0f));
        broadcastAnim(player, ANIM_F);
        return SkillResult.SUCCESS;
    }

    @Override
    public SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("1", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        List<LivingEntity> nearby = HitValidator.getNearby(player, 10.0);
        LivingEntity target = nearby.stream()
                .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(player))).orElse(null);
        if (target == null) return SkillResult.FAIL_NO_TARGET;

        float damage = JackpotStateMachine.isJackpotActive(data, tick) ? bd(1) * 1.5f : bd(1);
        DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, damage)
                .skillName("power_output").keyId(1).build();
        JJKMod.getCombatPipeline().process(ctx);
        data.cooldowns.put("1", tick + cd(1));
        broadcastAnim(player, ANIM_SF);
        return SkillResult.SUCCESS;
    }

    @Override
    public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("2", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.cooldowns.getOrDefault("jackpot_retry_used", 0L) > 0L) return SkillResult.FAIL_CONDITION;
        if (data.ceCurrent < ce(2)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_CONDITION;

        data.ceCurrent -= ce(2);
        JackpotStateMachine.tryJackpot(data, tick, JJKMod.getConfig());
        data.cooldowns.put("jackpot_retry_used", tick + 1);
        data.cooldowns.put("2", tick + cd(2));
        broadcastAnim(player, ANIM_R);
        return SkillResult.SUCCESS;
    }

    @Override
    public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("3", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < ce(3)) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= ce(3);
        int roll = ThreadLocalRandom.current().nextInt(3);
        switch (roll) {
            case 0 -> applyUncertainShockwave(player);
            case 1 -> applyUncertainCEAbsorb(player, data);
            case 2 -> applyUncertainSlowZone(player);
        }
        data.cooldowns.put("3", tick + cd(3));
        ServerPlayNetworking.send(player,
                new SkillResultS2CPacket(3, "hakari_shift_r_" + roll, 0f));
        broadcastAnim(player, ANIM_SR);
        return SkillResult.SUCCESS;
    }

    @Override
    public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.domainCooldownUntil > tick) return SkillResult.FAIL_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        // CE 검증·차감은 DomainManager(domains.json ceCost)가 단일 수행
        if (player == null) return SkillResult.FAIL_CONDITION;

        boolean deployed = JJKMod.getDomainManager().deployDomain("hakari_jackpot_domain", player);
        if (!deployed) return SkillResult.FAIL_CONDITION;
        broadcastAnim(player, ANIM_V);
        return SkillResult.SUCCESS;
    }

    // ── 불확정 영역(Shift+R) 효과 헬퍼 ───────────────────────────────────────────

    private void applyUncertainShockwave(ServerPlayerEntity player) {
        List<LivingEntity> targets = HitValidator.getNearby(player, 8.0);
        for (LivingEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, 45f)
                    .skillName("uncertain_domain")
                    .build();
            JJKMod.getCombatPipeline().process(ctx);
            net.minecraft.util.math.Vec3d dir = target.getPos().subtract(player.getPos()).normalize();
            target.setVelocity(dir.multiply(2.0));
            target.velocityModified = true;
        }
    }

    private void applyUncertainCEAbsorb(ServerPlayerEntity player, PlayerData playerData) {
        List<LivingEntity> targets = HitValidator.getNearby(player, 6.0);
        float totalAbsorbed = 0f;
        for (LivingEntity target : targets) {
            if (!(target instanceof ServerPlayerEntity)) continue; // CE only from player targets
            PlayerData targetData = JJKMod.getPlayerRepository().load(target.getUuid());
            float absorb = targetData.ceMax * 0.15f;
            targetData.ceCurrent = Math.max(0, targetData.ceCurrent - absorb);
            JJKMod.getPlayerRepository().save(targetData);
            totalAbsorbed += absorb;
        }
        playerData.ceCurrent = Math.min(playerData.ceCurrent + totalAbsorbed, playerData.ceMax);
    }

    private void applyUncertainSlowZone(ServerPlayerEntity player) {
        List<LivingEntity> targets = HitValidator.getNearby(player, 5.0);
        for (LivingEntity target : targets) {
            if (!(target instanceof ServerPlayerEntity)) continue; // slow status only for player targets
            PlayerData targetData = JJKMod.getPlayerRepository().load(target.getUuid());
            targetData.cooldowns.put("status_speed_down_until", player.getWorld().getTime() + 60);
            JJKMod.getPlayerRepository().save(targetData);
        }
    }

    // 잭팟 종료 처리: CE/HP 회복 + 능력치 버프 해제는 JJKMod.onInitialize()에서 등록한 틱 핸들러가 처리
    public static void tickJackpot(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (!"hakari".equals(data.characterId)) return;
        if (!data.jackpotActive) return;

        long currentTick = player.getWorld().getTime();
        if (currentTick >= data.jackpotEndTick) {
            // 잭팟 종료 → 600틱 CD 시작, 이후에만 재시도 가능
            data.jackpotActive = false;
            data.jackpotEndTick = 0L;
            data.lastJackpotAttemptTick = currentTick;
            activeJackpots.updateAndGet(v -> Math.max(0, v - 1));
            JJKMod.getPlayerRepository().save(data);
        }
    }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        var pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }
}
