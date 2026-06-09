package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.advancement.AdvancementTriggerManager;
import com.jjk.api.combat.IDamageSource;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.CooldownManager;
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
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class HakariSkillSet implements ISkillSet {

    // §LOCK: techniques.json 기준 baseDamage 값 — 임의 변경 금지
    private static final float BD_SF = 66f;

    private static final int CE_F  = 600,  CD_F  = 400, ANIM_F  = 36;
    private static final int CE_SF = 0,    CD_SF = 8,   ANIM_SF = 48;
    private static final int CE_R  = 180,  CD_R  = 30,  ANIM_R  = 1;
    private static final int CE_SR = 450,  CD_SR = 20,  ANIM_SR = 49;
    private static final int CE_V  = 2500, CD_V  = 360, ANIM_V  = 37;

    // §LOCK: jackpot probability 1/239
    private static final int JACKPOT_ODDS = 239;
    // 잭팟 종료 후 재시도 대기시간 (최소 = 600틱)
    private static final int POST_JACKPOT_CD = 600;

    // 동시 잭팟 상한 (P2-3): TPS 보호
    private static final AtomicInteger activeJackpots = new AtomicInteger(0);
    private static final int MAX_CONCURRENT_JACKPOTS = 2;

    private static final Random RANDOM = new Random();

    // 잭팟 관련 쿨다운 키 (PlayerData.cooldowns 맵에 저장 — DB 영속화 됨)
    private static final String KEY_JACKPOT_UNTIL  = "hakari_jackpot_until";
    private static final String KEY_LAST_ATTEMPT   = "hakari_last_attempt";

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useJackpotActivate(player);
            case 1 -> usePowerOutput(player);
            case 2 -> useReroll(player);
            case 3 -> useUncertainDomain(player);
            case 4 -> useDomainDeploy(player);
            default -> SkillResult.FAIL;
        };
    }

    @Override public boolean canUse(ServerPlayerEntity player, int keyId) { return true; }

    @Override
    public int getCooldownTicks(int keyId) {
        return switch (keyId) {
            case 0 -> CD_F; case 1 -> CD_SF; case 2 -> CD_R;
            case 3 -> CD_SR; case 4 -> CD_V; default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) {
        return switch (keyId) {
            case 0 -> CE_F; case 1 -> CE_SF; case 2 -> CE_R;
            case 3 -> CE_SR; case 4 -> CE_V; default -> 0;
        };
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "jackpot_activate";
            case 1 -> "power_output";
            case 2 -> "reroll";
            case 3 -> "uncertain_domain";
            case 4 -> "domain_deploy";
            default -> "unknown";
        };
    }

    // ── onX PlayerData 경로 ──────────────────────────────────────────────────────

    @Override
    public SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("0", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_F) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_CONDITION;

        data.ceCurrent -= CE_F;
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
                if (player != null) AdvancementTriggerManager.onHakariJackpotStreak(player, (int) streak);
            }
        }
        if (!jackpot) {
            data.ceCurrent += CE_F;
            data.cooldowns.put("adv_jackpot_streak", 0L);
        }
        data.cooldowns.put("0", tick + 20);
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

        float damage = JackpotStateMachine.isJackpotActive(data, tick) ? BD_SF * 1.5f : BD_SF;
        DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, damage)
                .skillName("power_output").keyId(1).build();
        JJKMod.getCombatPipeline().process(ctx);
        data.cooldowns.put("1", tick + CD_SF);
        broadcastAnim(player, ANIM_SF);
        return SkillResult.SUCCESS;
    }

    @Override
    public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("2", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.cooldowns.getOrDefault("jackpot_retry_used", 0L) > 0L) return SkillResult.FAIL_CONDITION;
        if (data.ceCurrent < CE_R) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_CONDITION;

        data.ceCurrent -= CE_R;
        JackpotStateMachine.tryJackpot(data, tick, JJKMod.getConfig());
        data.cooldowns.put("jackpot_retry_used", tick + 1);
        data.cooldowns.put("2", tick + CD_R);
        broadcastAnim(player, ANIM_R);
        return SkillResult.SUCCESS;
    }

    @Override
    public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.cooldowns.getOrDefault("3", 0L) > tick) return SkillResult.ON_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_SR) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_NO_TARGET;

        data.ceCurrent -= CE_SR;
        int roll = ThreadLocalRandom.current().nextInt(3);
        switch (roll) {
            case 0 -> applyUncertainShockwave(player);
            case 1 -> applyUncertainCEAbsorb(player, data);
            case 2 -> applyUncertainSlowZone(player);
        }
        data.cooldowns.put("3", tick + CD_SR);
        ServerPlayNetworking.send(player,
                new SkillResultS2CPacket(3, "hakari_shift_r_" + roll, 0f));
        broadcastAnim(player, ANIM_SR);
        return SkillResult.SUCCESS;
    }

    @Override
    public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick) {
        if (data.domainCooldownUntil > tick) return SkillResult.FAIL_COOLDOWN;
        if (data.cooldowns.getOrDefault("skill_seal", 0L) > tick) return SkillResult.FAIL_SKILL_SEALED;
        if (data.ceCurrent < CE_V) return SkillResult.FAIL_CE_INSUFFICIENT;
        if (player == null) return SkillResult.FAIL_CONDITION;

        boolean deployed = JJKMod.getDomainManager().deployDomain("hakari_jackpot_domain", player);
        if (!deployed) return SkillResult.FAIL_CONDITION;
        broadcastAnim(player, ANIM_V);
        return SkillResult.SUCCESS;
    }

    // F — jackpot_activate: 1/239 확률, §LOCK jackpotDurationTicks는 config에서 참조함.
    private SkillResult useJackpotActivate(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();

        // 잭팟 발동 시도 — 600틱 CD (jackpotCooldownUntil과는 별개)
        if (!CooldownManager.isReady(data, "cd_hakari_0", tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_F)) return SkillResult.CE_INSUFFICIENT;

        data.cooldowns.put(KEY_LAST_ATTEMPT, tick);

        boolean canJackpot = (tick - data.lastJackpotAttemptTick) >= POST_JACKPOT_CD;
        boolean jackpot = canJackpot && RANDOM.nextInt(JACKPOT_ODDS) == 0;
        if (jackpot && activeJackpots.get() >= MAX_CONCURRENT_JACKPOTS) {
            player.sendMessage(
                    net.minecraft.text.Text.literal("[JJK] 현재 잭팟이 최대 동시 발동 중입니다."), true);
            jackpot = false;
        }
        if (jackpot) {
            // §LOCK: jackpotDurationTicks는 config에서 참조, 기본값 상한 251틱
            activeJackpots.incrementAndGet();
            int duration = JJKMod.getConfig().jackpotDurationTicks;
            data.jackpotActive = true;
            data.jackpotEndTick = tick + duration;
        }

        JJKMod.getCEManager().consume(player, CE_F);
        CooldownManager.set(data, "cd_hakari_0", tick, CD_F);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_F);
        return SkillResult.SUCCESS;
    }

    // SF — power_output: 공격력 1.5배 버프 적용. ceCost=0이므로 canAfford 생략.
    private SkillResult usePowerOutput(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_hakari_1";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        // CE_SF=0 이므로 canAfford/consume 생략

        boolean jackpotActive = isJackpotActive(data, tick);
        float mult = jackpotActive ? 1.5f : 1.0f;

        List<LivingEntity> targets = HitValidator.getNearby(player, 3.0);
        if (targets.isEmpty()) return SkillResult.FAIL;

        LivingEntity target = targets.get(0);
        DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_SF)
                .externalBuffMult(mult)
                .skillName("power_output")
                .build();
        JJKMod.getCombatPipeline().process(ctx);

        CooldownManager.set(data, cdKey, tick, CD_SF);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SF);
        return SkillResult.SUCCESS;
    }

    // R: reroll
    private SkillResult useReroll(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_hakari_2";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_R)) return SkillResult.CE_INSUFFICIENT;

        if (isJackpotActive(data, tick)) return SkillResult.FAIL; // 이미 잭팟 활성 중이면 재시도 불가
        long lastAttempt = data.cooldowns.getOrDefault(KEY_LAST_ATTEMPT, 0L);
        if (tick - lastAttempt > 20) return SkillResult.FAIL; // 20틱 내 재시도 불가
        data.cooldowns.put(KEY_LAST_ATTEMPT, tick);
        boolean jackpot = RANDOM.nextInt(JACKPOT_ODDS) == 0;
        if (jackpot) {
            int duration = JJKMod.getConfig().jackpotDurationTicks;
            data.jackpotActive = true;
            data.jackpotEndTick = tick + duration;
        }

        JJKMod.getCEManager().consume(player, CE_R);
        CooldownManager.set(data, cdKey, tick, CD_R);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_R);
        return SkillResult.SUCCESS;
    }

    // SR — uncertain_domain: Random.nextInt(3)으로 효과 랜덤 선택, 결과는 SkillResultS2CPacket으로 전송
    private SkillResult useUncertainDomain(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_hakari_3";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_SR)) return SkillResult.CE_INSUFFICIENT;

        int effect = RANDOM.nextInt(3);
        switch (effect) {
            case 0 -> applyUncertainShockwave(player);
            case 1 -> applyUncertainCEAbsorb(player, data);
            case 2 -> applyUncertainSlowZone(player);
        }

        // 클라이언트에 효과 종류를 알려주기 위해 결과 패킷 전송
        ServerPlayNetworking.send(player, new SkillResultS2CPacket(3, "uncertain_domain:" + effect, 0f));

        JJKMod.getCEManager().consume(player, CE_SR);
        CooldownManager.set(data, cdKey, tick, CD_SR);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SR);
        return SkillResult.SUCCESS;
    }

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

    // V — domain_deploy: DomainManager 영역 전개
    private SkillResult useDomainDeploy(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_hakari_4";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_V)) return SkillResult.CE_INSUFFICIENT;

        JJKMod.getDomainManager().deployDomain("hakari_domain", player);

        JJKMod.getCEManager().consume(player, CE_V);
        CooldownManager.set(data, cdKey, tick, CD_V);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_V);
        return SkillResult.SUCCESS;
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

    private static boolean isJackpotActive(PlayerData data, long currentTick) {
        return data.jackpotActive && currentTick < data.jackpotEndTick;
    }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        var pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }
}
