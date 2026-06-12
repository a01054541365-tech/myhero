package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.combat.IDamageSource;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.BlackFlashPerfectTracker;
import com.jjk.combat.CooldownManager;
import com.jjk.combat.DamageContext;
import com.jjk.combat.HitValidator;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import com.jjk.network.s2c.SkillEffectS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItadoriSkillSet implements ISkillSet {

    private static final Map<UUID, Long> blackFlashFocusTicks = new HashMap<>();

    // key 0: divergent_fist, 1: manji_kick, 2: black_flash_focus, 3: domain_startup, 4: rct
    // key 5 (extended): shrine
    // 수치는 techniques.json 단일 기준 (2026-06-11 데이터 주도 전환). key 5(shrine)는 extendedSkills — 상수 유지.
    private static final String CHAR_ID = "itadori";

    private static float bd(int keyId) { return com.jjk.combat.TechniqueLoader.getBaseDamage(CHAR_ID, keyId); }
    private static int   ce(int keyId) { return (int) com.jjk.combat.TechniqueLoader.getCeCost(CHAR_ID, keyId); }
    private static int   cd(int keyId) { return (int) com.jjk.combat.TechniqueLoader.getCooldownTicks(CHAR_ID, keyId); }

    // shrine (확장 슬롯 keyId=5) — extendedSkills, §H-3 밸런스 확정
    private static final int CE_SHRINE = 450, CD_SHRINE = 160, SHRINE_ANIM = 63;
    private static final int BF_COMBO_WINDOW = 20; // 흑섬 Perfect → shrine 콤보 윈도우(틱)
    private static final float[] SHRINE_DECAY = {1.0f, 0.90f, 0.75f}; // 3타 decay
    private static final float SHRINE_BF_MULT = 3.0f; // 흑섬 Perfect 콤보 배율 (첫 히트만)

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useDivergentFist(player);
            case 1 -> useManjiKick(player);
            case 2 -> useBlackFlashFocus(player);
            case 3 -> useDomainStartup(player);
            case 4 -> useRCT(player);
            case 5 -> useShrine(player);
            default -> SkillResult.FAIL;
        };
    }

    @Override
    public boolean canUse(ServerPlayerEntity player, int keyId) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        return CooldownManager.isReady(data, cdKey(keyId), tick)
                && JJKMod.getCEManager().canAfford(player, getCeCost(keyId));
    }

    @Override
    public int getCooldownTicks(int keyId) {
        return switch (keyId) {
            case 0, 1, 2, 3, 4 -> cd(keyId); case 5 -> CD_SHRINE; default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) {
        return switch (keyId) {
            case 0, 1, 2, 3, 4 -> ce(keyId); case 5 -> CE_SHRINE; default -> 0;
        };
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "divergent_fist"; case 1 -> "manji_kick"; case 2 -> "black_flash_focus";
            case 3 -> "domain_startup"; case 4 -> "rct"; case 5 -> "shrine"; default -> "unknown";
        };
    }

    private static String cdKey(int keyId) { return "cd_itadori_" + keyId; }

    private static void broadcastAnim(ServerPlayerEntity player, int animId) {
        AnimationTriggerS2CPacket pkt = new AnimationTriggerS2CPacket(player.getUuid(), (byte) animId);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));
    }

    private SkillResult useDivergentFist(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(0), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, ce(0))) return SkillResult.CE_INSUFFICIENT;

        List<LivingEntity> targets = HitValidator.getNearbyArc(player, 3.0, 120f);

        for (LivingEntity target : targets) {
            JJKMod.getEffectDeferQueue().schedule(
                    BlockPos.ofFloored(target.getPos()), bd(0), 5, player.getUuid(), tick);
        }

        JJKMod.getCEManager().consume(player, ce(0));
        CooldownManager.set(data, cdKey(0), tick, cd(0));
        JJKMod.getPlayerRepository().save(data);
        return SkillResult.SUCCESS;
    }

    private SkillResult useManjiKick(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(1), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, ce(1))) return SkillResult.CE_INSUFFICIENT;

        Vec3d dir = player.getRotationVec(1.0f);
        player.setVelocity(dir.multiply(1.5));
        player.velocityModified = true;

        List<LivingEntity> targets = HitValidator.getNearby(player, 3.0);
        if (!targets.isEmpty()) {
            JJKMod.getComboTracker().recordHit(player.getUuid(), tick);
        }

        JJKMod.getCEManager().consume(player, ce(1));
        CooldownManager.set(data, cdKey(1), tick, cd(1));
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, 14);
        return SkillResult.SUCCESS;
    }

    private SkillResult useBlackFlashFocus(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(2), tick)) return SkillResult.ON_COOLDOWN;

        blackFlashFocusTicks.put(player.getUuid(), tick);
        CooldownManager.set(data, cdKey(2), tick, cd(2));
        JJKMod.getPlayerRepository().save(data);
        return SkillResult.SUCCESS;
    }
    private SkillResult useDomainStartup(ServerPlayerEntity player) {
        boolean deployed = JJKMod.getDomainManager()
                .deployDomain("itadori_unnamed", player);
        if (!deployed) return SkillResult.FAIL;
        broadcastAnim(player, 59);
        return SkillResult.SUCCESS;
    }

    private SkillResult useRCT(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(4), tick)) return SkillResult.ON_COOLDOWN;

        data.healingActive = true;
        CooldownManager.set(data, cdKey(4), tick, cd(4));
        JJKMod.getPlayerRepository().save(data);
        broadcastAnim(player, 15);
        return SkillResult.SUCCESS;
    }

    private SkillResult useShrine(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();

        // 발동 조건: 준특급 이상 OR 각성 활성
        boolean gradeOk = data.grade != null
                && data.grade.ordinal() >= com.jjk.data.Grade.SEMI_SPECIAL.ordinal();
        if (!gradeOk && !data.awakeningActive) return SkillResult.FAIL;

        if (!CooldownManager.isReady(data, cdKey(5), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_SHRINE)) return SkillResult.CE_INSUFFICIENT;

        // 흑섬 Perfect 콤보 윈도우 확인 (20틱 이내 흑섬 Perfect 성공 시 ×3.0, 첫 히트만)
        float bfMult = BlackFlashPerfectTracker.isWithinWindow(player.getUuid(), tick, BF_COMBO_WINDOW)
                ? SHRINE_BF_MULT : 1.0f;

        // 3타 다단히트 — isSoulDirect=true (방어 관통), 각 히트 decay 사전 적용
        List<LivingEntity> targets = HitValidator.getNearby(player, 4.0);
        for (LivingEntity target : targets) {
            for (int i = 0; i < SHRINE_DECAY.length; i++) {
                float adjustedBase = 42f * SHRINE_DECAY[i] * (i == 0 ? bfMult : 1.0f);
                DamageContext ctx = DamageContext.builder(player, target,
                                IDamageSource.SOUL_DIRECT, adjustedBase)
                        .soulDirect()
                        .skillName("shrine")
                        .hitIndex(0)        // decay 사전 적용 → 기본 다단 감쇠 우회
                        .alreadyConsumedCE(true)
                        .build();
                JJKMod.getCombatPipeline().process(ctx);
            }
        }

        JJKMod.getCEManager().consume(player, CE_SHRINE);
        CooldownManager.set(data, cdKey(5), tick, CD_SHRINE);
        JJKMod.getPlayerRepository().save(data);

        Vec3d pos = player.getPos();
        SkillEffectS2CPacket pkt = SkillEffectS2CPacket.of(
                "itadori_shrine", player.getUuid(), pos.x, pos.y, pos.z);
        player.getServerWorld().getPlayers().stream()
                .filter(p -> p.squaredDistanceTo(player) <= 32 * 32)
                .forEach(p -> ServerPlayNetworking.send(p, pkt));

        broadcastAnim(player, SHRINE_ANIM);
        return SkillResult.SUCCESS;
    }

    // ─── 순수 PlayerData 경로 (ISkillSet §default 오버라이드) ──────────────────
    @Override public SkillResult onF(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : useDivergentFist(player); }
    @Override public SkillResult onShiftF(PlayerData data, ServerPlayerEntity player, long tick) { return player == null ? SkillResult.SUCCESS : useManjiKick(player); }
    @Override public SkillResult onR(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : useBlackFlashFocus(player); }
    @Override public SkillResult onShiftR(PlayerData data, ServerPlayerEntity player, long tick) { return player == null ? SkillResult.SUCCESS : useDomainStartup(player); }
    @Override public SkillResult onV(PlayerData data, ServerPlayerEntity player, long tick)      { return player == null ? SkillResult.SUCCESS : useRCT(player); }
}
