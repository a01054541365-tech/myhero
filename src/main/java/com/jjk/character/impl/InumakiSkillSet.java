package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.combat.IDamageSource;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.CooldownManager;
import com.jjk.combat.DamageContext;
import com.jjk.combat.HitValidator;
import com.jjk.data.PlayerData;
import com.jjk.effect.EffectManager;
import com.jjk.effect.EffectType;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.stream.Collectors;

public class InumakiSkillSet implements ISkillSet {

    // 吏쟊OCK: techniques.json ??륂뒄 ???袁⑹벥 癰궰野?疫뀀뜆?
    private static final float BD_SF = 70f;

    private static final int CE_F  = 120, CD_F  = 10, ANIM_F  = 50;
    private static final int CE_SF = 280, CD_SF = 24, ANIM_SF = 51;
    private static final int CE_SR = 240, CD_SR = 30, ANIM_SR = 52;
    private static final int CE_V  = 150, CD_V  = 18, ANIM_V  = 53;

    // 筌?쑵????쎄텢 ??됱뵠???귐됱퍩: 40??2????1??    private static final int CHAT_RATELIMIT_TICKS = 40;
    private static final String RATELIMIT_KEY = "inumaki_chat_ratelimit";

    // ?봔??筌앹빓???(?봔??野껊슣?좑쭪?)
    private static final int BURDEN_STOP    = 30;
    private static final int BURDEN_EXPLODE = 50;
    private static final int BURDEN_SLEEP   = 40;

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        // keyId 筌욊낯???紐꾪뀱 ??揶쎛??揶쎛繹먮슣???怨몄뱽 ???怨몄몵嚥?        List<ServerPlayerEntity> enemies = HitValidator.getNearby(player, 10.0).stream()
                .filter(t -> JJKMod.getTeamManager().isEnemy(player, t))
                .collect(Collectors.toList());
        ServerPlayerEntity target = enemies.isEmpty() ? null : enemies.get(0);

        return switch (keyId) {
            case 0 -> useStopCurse(player, target);
            case 1 -> useExplodeCurse(player);
            case 2 -> useSleepCurse(player, target);
            case 3 -> useRunCurse(player);
            default -> SkillResult.FAIL;
        };
    }

    @Override public boolean canUse(ServerPlayerEntity player, int keyId) { return true; }

    @Override
    public int getCooldownTicks(int keyId) {
        return switch (keyId) {
            case 0 -> CD_F; case 1 -> CD_SF; case 2 -> CD_SR; case 3 -> CD_V; default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) {
        return switch (keyId) {
            case 0 -> CE_F; case 1 -> CE_SF; case 2 -> CE_SR; case 3 -> CE_V; default -> 0;
        };
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "stop_curse"; case 1 -> "explode_curse";
            case 2 -> "sleep_curse"; case 3 -> "run_curse"; default -> "unknown";
        };
    }

    // 筌?쑵??筌롫뗄?놅쭪? ???뼓 ????쎄텢 ??깆뒭?? JJKMod 筌?쑵???귐딅뮞??됰퓠???紐꾪뀱.
    public boolean handleChat(ServerPlayerEntity player, String text) {
        return switch (text.trim()) {
            case "!筌롫뜆??  -> { use(player, 0); yield true; }
            case "!?怨쀬죬"  -> { use(player, 1); yield true; }
            case "!?醫딅굶?? -> { use(player, 2); yield true; }
            case "!????  -> { use(player, 3); yield true; }
            default -> false;
        };
    }

    // F/keyId=0 ??stop_curse (!筌롫뜆??: STUN 40??    private SkillResult useStopCurse(ServerPlayerEntity player, ServerPlayerEntity target) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();

        if (!checkChatRateLimit(data, tick)) return SkillResult.ON_COOLDOWN;
        if (data.burden + BURDEN_STOP > 100) return SkillResult.SEALED;
        if (!CooldownManager.isReady(data, "cd_inumaki_0", tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_F)) return SkillResult.CE_INSUFFICIENT;
        if (target == null) return SkillResult.FAIL;

        EffectManager.apply(target, EffectType.STUN, 40);

        JJKMod.getCEManager().consume(player, CE_F);
        CooldownManager.set(data, "cd_inumaki_0", tick, CD_F);
        applyBurden(player, data, BURDEN_STOP, tick);
        setRateLimit(data, tick);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_F);
        return SkillResult.SUCCESS;
    }

    // SF/keyId=1 ??explode_curse (!?怨쀬죬): 獄쏆꼵瑗?4?됰뗀以???而??怨?筌왖 70
    private SkillResult useExplodeCurse(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();

        if (!checkChatRateLimit(data, tick)) return SkillResult.ON_COOLDOWN;
        if (data.burden + BURDEN_EXPLODE > 100) return SkillResult.SEALED;
        if (!CooldownManager.isReady(data, "cd_inumaki_1", tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_SF)) return SkillResult.CE_INSUFFICIENT;

        List<ServerPlayerEntity> targets = HitValidator.getNearby(player, 4.0);
        for (ServerPlayerEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, BD_SF)
                    .skillName("explode_curse")
                    .build();
            JJKMod.getCombatPipeline().process(ctx);
        }

        JJKMod.getCEManager().consume(player, CE_SF);
        CooldownManager.set(data, "cd_inumaki_1", tick, CD_SF);
        applyBurden(player, data, BURDEN_EXPLODE, tick);
        setRateLimit(data, tick);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SF);
        return SkillResult.SUCCESS;
    }

    // SR/keyId=2 ??sleep_curse (!?醫딅굶??: SLEEP 100?? ??④봄 ????곸젫
    private SkillResult useSleepCurse(ServerPlayerEntity player, ServerPlayerEntity target) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();

        if (!checkChatRateLimit(data, tick)) return SkillResult.ON_COOLDOWN;
        if (data.burden + BURDEN_SLEEP > 100) return SkillResult.SEALED;
        if (!CooldownManager.isReady(data, "cd_inumaki_2", tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_SR)) return SkillResult.CE_INSUFFICIENT;
        if (target == null) return SkillResult.FAIL;

        EffectManager.apply(target, EffectType.SLEEP, 100);

        JJKMod.getCEManager().consume(player, CE_SR);
        CooldownManager.set(data, "cd_inumaki_2", tick, CD_SR);
        applyBurden(player, data, BURDEN_SLEEP, tick);
        setRateLimit(data, tick);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SR);
        return SkillResult.SUCCESS;
    }

    // V/keyId=3 ??run_curse (!????: 獄쏆꼵瑗?8?됰뗀以??袁㏓럵 ??猷??얜즲 +40%, 80??    private SkillResult useRunCurse(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();

        if (!CooldownManager.isReady(data, "cd_inumaki_3", tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_V)) return SkillResult.CE_INSUFFICIENT;

        List<ServerPlayerEntity> allies = HitValidator.getNearby(player, 8.0).stream()
                .filter(t -> !JJKMod.getTeamManager().isEnemy(player, t))
                .collect(Collectors.toList());

        for (ServerPlayerEntity ally : allies) {
            var attr = ally.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            if (attr != null) {
                double boosted = attr.getBaseValue() * 1.4;
                attr.setBaseValue(boosted);
                // 80?????癒?궗?? EffectManager ?怨밴묶 ?곕뗄???곗쨮 筌ｌ꼶??                PlayerData allyData = JJKMod.getPlayerRepository().load(ally.getUuid());
                allyData.cooldowns.put("status_speed_up_until", tick + 80);
                allyData.cooldowns.put("status_speed_up_base", (long)(attr.getBaseValue() / 1.4 * 1000));
                JJKMod.getPlayerRepository().save(allyData);
            }
        }

        JJKMod.getCEManager().consume(player, CE_V);
        CooldownManager.set(data, "cd_inumaki_3", tick, CD_V);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_V);
        return SkillResult.SUCCESS;
    }

    // 筌?쑵????됱뵠???귐됱퍩 筌ｋ똾寃?(2?λ뜄??1??
    private static boolean checkChatRateLimit(PlayerData data, long tick) {
        Long limitUntil = data.cooldowns.get(RATELIMIT_KEY);
        return limitUntil == null || tick >= limitUntil;
    }

    private static void setRateLimit(PlayerData data, long tick) {
        data.cooldowns.put(RATELIMIT_KEY, tick + CHAT_RATELIMIT_TICKS);
    }

    // ?봔??野껊슣?좑쭪? 筌앹빓? + 100 ??곴맒 ??100???딅맩??    private static void applyBurden(ServerPlayerEntity player, PlayerData data, int amount, long tick) {
        JJKMod.getBurdenManager().addBurden(player, amount);
    }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        var pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }
}
