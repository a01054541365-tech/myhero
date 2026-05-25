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
import com.jjk.network.s2c.SkillResultS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Random;

public class HakariSkillSet implements ISkillSet {

    // 吏쟊OCK: techniques.json ??륂뒄 ???袁⑹벥 癰궰野?疫뀀뜆?
    private static final float BD_SF = 66f;

    private static final int CE_F  = 600,  CD_F  = 400, ANIM_F  = 36;
    private static final int CE_SF = 0,    CD_SF = 8,   ANIM_SF = 48;
    private static final int CE_R  = 180,  CD_R  = 30,  ANIM_R  = 1;
    private static final int CE_SR = 450,  CD_SR = 20,  ANIM_SR = 49;
    private static final int CE_V  = 2500, CD_V  = 360, ANIM_V  = 37;

    // §LOCK: jackpot probability 1/239
    private static final int JACKPOT_ODDS = 239;
    // ???????묅뫀???(??λ뻻??30??= 600??
    private static final int POST_JACKPOT_CD = 600;

    private static final Random RANDOM = new Random();

    // ?????怨밴묶 ??(PlayerData.cooldowns 筌???????DB ?癒?짗 ????
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

    // F ??jackpot_activate: 1/239 ?곕뗄爰? 吏쟊OCK jackpotDurationTicks??config?癒?퐣筌???뚯벉.
    private SkillResult useJackpotActivate(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();

        // ??λ뻻?? ?????ル굝利???600??CD (jackpotCooldownUntil ?袁⑤굡 ??뽰뒠)
        if (!CooldownManager.isReady(data, "cd_hakari_0", tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_F)) return SkillResult.CE_INSUFFICIENT;

        data.cooldowns.put(KEY_LAST_ATTEMPT, tick);

        boolean canJackpot = (tick - data.lastJackpotAttemptTick) >= POST_JACKPOT_CD;
        boolean jackpot = canJackpot && RANDOM.nextInt(JACKPOT_ODDS) == 0;
        if (jackpot) {
            // 吏쟊OCK: jackpotDurationTicks??config?癒?퐣筌???뚯벉, ?꾨뗀諭???롫굡?꾨뗀逾?疫뀀뜆?
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

    // SF ??power_output: ????餓???.5 ?곕떽? ?怨?筌왖. ceCost=0???嚥?canAfford ??쎄땁.
    private SkillResult usePowerOutput(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_hakari_1";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        // CE_SF=0 ???嚥?canAfford/consume ??쎄땁

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

        if (isJackpotActive(data, tick)) return SkillResult.FAIL; // ????餓??귐됀??븍뜃?
        long lastAttempt = data.cooldowns.getOrDefault(KEY_LAST_ATTEMPT, 0L);
        if (tick - lastAttempt > 20) return SkillResult.FAIL; // 20???λ뜃??
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

    // SR ??uncertain_domain: Random.nextInt(3)??곗쨮 ??ｋ궢 野껉퀣????SkillResultS2CPacket ?袁⑸꽊
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

        // ?????곷섧?紐꾨퓠 ?醫뤾문????ｋ궢 ?紐껊쑔???袁⑸꽊
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

    // V ??domain_deploy: DomainManager ?袁⑹뿫
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

    // ??????筌ｌ꼶?? CE/HP regen + 筌띾슢利?揶쏅Ŋ? ??JJKMod.onInitialize()?癒?퐣 ?源낆쨯
    public static void tickJackpot(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (!"hakari".equals(data.characterId)) return;
        if (!data.jackpotActive) return;

        long currentTick = player.getWorld().getTime();
        if (currentTick >= data.jackpotEndTick) {
            // ?????ル굝利? 600??CD ?怨몄뒠, ?怨몃열 CD??椰꾨?諭띄뵳?? ??놁벉
            data.jackpotActive = false;
            data.jackpotEndTick = 0L;
            data.lastJackpotAttemptTick = currentTick;
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
