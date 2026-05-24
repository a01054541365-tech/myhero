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
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class MahitoSkillSet implements ISkillSet {

    // 吏쟊OCK: techniques.json ??륂뒄 ???袁⑹벥 癰궰野?疫뀀뜆?
    private static final float BD_F  = 26f;
    private static final float BD_SF = 48f;
    private static final float BD_SR = 52f;

    private static final int CE_F  = 140,  CD_F  = 8,   ANIM_F  = 33;
    private static final int CE_SF = 220,  CD_SF = 18,  ANIM_SF = 43;
    private static final int CE_R  = 120,  CD_R  = 15,  ANIM_R  = 34;
    private static final int CE_SR = 190,  CD_SR = 12,  ANIM_SR = 44;
    private static final int CE_V  = 3150, CD_V  = 360, ANIM_V  = 35;

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useIdleTransfiguration(player);
            case 1 -> usePolymorphicSoulIsomer(player);
            case 2 -> useSoulDefense(player);
            case 3 -> useBladeTransfiguration(player);
            case 4 -> useSelfEmbodiment(player);
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
            case 0 -> "idle_transfiguration";
            case 1 -> "polymorphic_soul_isomer";
            case 2 -> "soul_defense";
            case 3 -> "blade_transfiguration";
            case 4 -> "self_embodiment_of_perfection";
            default -> "unknown";
        };
    }

    // F ??idle_transfiguration: 域뱀눘??1.5?됰뗀以? isSoulDirect=true
    private SkillResult useIdleTransfiguration(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_mahito_0";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_F)) return SkillResult.CE_INSUFFICIENT;

        List<ServerPlayerEntity> targets = HitValidator.getNearby(player, 1.5);
        if (targets.isEmpty()) return SkillResult.FAIL;

        ServerPlayerEntity target = targets.get(0);
        DamageContext ctx = DamageContext.builder(player, target, IDamageSource.SOUL_DIRECT, BD_F)
                .soulDirect()
                .skillName("idle_transfiguration")
                .build();
        JJKMod.getCombatPipeline().process(ctx);

        JJKMod.getCEManager().consume(player, CE_F);
        CooldownManager.set(data, cdKey, tick, CD_F);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_F);
        return SkillResult.SUCCESS;
    }

    // SF ??polymorphic_soul_isomer: 3筌??브쑴肉? MultiHitDampener ??.0/??.85/??.70
    private SkillResult usePolymorphicSoulIsomer(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_mahito_1";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_SF)) return SkillResult.CE_INSUFFICIENT;

        List<ServerPlayerEntity> targets = HitValidator.getNearby(player, 10.0).stream()
                .filter(t -> JJKMod.getTeamManager().isEnemy(player, t))
                .sorted(Comparator.comparingDouble(t -> t.squaredDistanceTo(player)))
                .limit(3)
                .collect(Collectors.toList());

        float[] dampeners = {1.0f, 0.85f, 0.70f};
        for (int i = 0; i < targets.size(); i++) {
            float dmg = BD_SF * dampeners[i];
            DamageContext ctx = DamageContext.builder(player, targets.get(i),
                    IDamageSource.NORMAL_TECHNIQUE, dmg)
                    .skillName("polymorphic_soul_isomer")
                    .build();
            JJKMod.getCombatPipeline().process(ctx);
        }

        JJKMod.getCEManager().consume(player, CE_SF);
        CooldownManager.set(data, cdKey, tick, CD_SF);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SF);
        return SkillResult.SUCCESS;
    }

    // R ??soul_defense: 80??soul_resist 甕곌쑵遊?(isSoulDirect ?怨?筌왖 50% 揶쏅Ŋ??
    private SkillResult useSoulDefense(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_mahito_2";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_R)) return SkillResult.CE_INSUFFICIENT;

        data.cooldowns.put("status_soul_resist", tick + 80);
        JJKMod.getCEManager().consume(player, CE_R);
        CooldownManager.set(data, cdKey, tick, CD_R);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_R);
        return SkillResult.SUCCESS;
    }

    // SR ??blade_transfiguration: ?袁④컩 90??arc 2?됰뗀以? isSoulDirect=true
    private SkillResult useBladeTransfiguration(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_mahito_3";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_SR)) return SkillResult.CE_INSUFFICIENT;

        List<ServerPlayerEntity> targets = HitValidator.getNearbyArc(player, 2.0, 90f);
        for (ServerPlayerEntity target : targets) {
            DamageContext ctx = DamageContext.builder(player, target, IDamageSource.SOUL_DIRECT, BD_SR)
                    .soulDirect()
                    .skillName("blade_transfiguration")
                    .build();
            JJKMod.getCombatPipeline().process(ctx);
        }

        JJKMod.getCEManager().consume(player, CE_SR);
        CooldownManager.set(data, cdKey, tick, CD_SR);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_SR);
        return SkillResult.SUCCESS;
    }

    // V ???癒곕짃?癒?즷?? DomainManager ?袁⑹뿫. ?臾먭텊??tick 筌ｌ꼶???DomainManager?癒?퐣.
    private SkillResult useSelfEmbodiment(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        String cdKey = "cd_mahito_4";
        if (!CooldownManager.isReady(data, cdKey, tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_V)) return SkillResult.CE_INSUFFICIENT;

        JJKMod.getDomainManager().deployDomain("mahito_domain", player);

        JJKMod.getCEManager().consume(player, CE_V);
        CooldownManager.set(data, cdKey, tick, CD_V);
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, ANIM_V);
        return SkillResult.SUCCESS;
    }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        var pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }
}
