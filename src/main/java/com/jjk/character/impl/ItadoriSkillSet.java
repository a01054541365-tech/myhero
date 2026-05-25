package com.jjk.character.impl;

import com.jjk.JJKMod;
import com.jjk.api.combat.IDamageSource;
import com.jjk.api.skill.ISkillSet;
import com.jjk.api.skill.SkillResult;
import com.jjk.combat.CooldownManager;
import com.jjk.combat.DamageContext;
import com.jjk.combat.HitValidator;
import com.jjk.data.PlayerData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItadoriSkillSet implements ISkillSet {

    private static final Map<UUID, Long> blackFlashFocusTicks = new HashMap<>();

    // key 0: divergent_fist, 1: manji_kick, 2: black_flash_focus, 3: domain_startup, 4: rct
    private static final int CE_0 = 80,   CD_0 = 5;
    private static final int CE_1 = 120,  CD_1 = 8;
    private static final int CE_2 = 0,    CD_2 = 120;
    private static final int CE_3 = 2400, CD_3 = 480;
    private static final int CE_4 = 120,  CD_4 = 25;

    @Override
    public SkillResult use(ServerPlayerEntity player, int keyId) {
        return switch (keyId) {
            case 0 -> useDivergentFist(player);
            case 1 -> useManjiKick(player);
            case 2 -> useBlackFlashFocus(player);
            case 3 -> useDomainStartup(player);
            case 4 -> useRCT(player);
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
            case 0 -> CD_0; case 1 -> CD_1; case 2 -> CD_2;
            case 3 -> CD_3; case 4 -> CD_4; default -> 0;
        };
    }

    @Override
    public int getCeCost(int keyId) {
        return switch (keyId) {
            case 0 -> CE_0; case 1 -> CE_1; case 2 -> CE_2;
            case 3 -> CE_3; case 4 -> CE_4; default -> 0;
        };
    }

    @Override
    public String getSkillName(int keyId) {
        return switch (keyId) {
            case 0 -> "divergent_fist"; case 1 -> "manji_kick"; case 2 -> "black_flash_focus";
            case 3 -> "domain_startup"; case 4 -> "rct"; default -> "unknown";
        };
    }

    private static String cdKey(int keyId) { return "cd_itadori_" + keyId; }

    private SkillResult useDivergentFist(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(0), tick)) return SkillResult.ON_COOLDOWN;
        if (!JJKMod.getCEManager().canAfford(player, CE_0)) return SkillResult.CE_INSUFFICIENT;

        List<LivingEntity> targets = HitValidator.getNearbyArc(player, 3.0, 120f);

        for (LivingEntity target : targets) {
            DamageContext ctx1 = DamageContext.builder(player, target, IDamageSource.NORMAL_TECHNIQUE, 34f)
                    .skillName("divergent_fist").hitIndex(0).keyId(0).build();
            JJKMod.getCombatPipeline().process(ctx1);
            JJKMod.getEffectDeferQueue().schedule(
                    BlockPos.ofFloored(target.getPos()), 17f, 5, player.getUuid(), tick);
        }

        JJKMod.getCEManager().consume(player, CE_0);
        CooldownManager.set(data, cdKey(0), tick, CD_0);
        JJKMod.getPlayerRepository().save(data);
        return SkillResult.SUCCESS;
    }

    private SkillResult useManjiKick(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }

    private SkillResult useBlackFlashFocus(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long tick = player.getWorld().getTime();
        if (!CooldownManager.isReady(data, cdKey(2), tick)) return SkillResult.ON_COOLDOWN;

        blackFlashFocusTicks.put(player.getUuid(), tick);
        CooldownManager.set(data, cdKey(2), tick, CD_2);
        JJKMod.getPlayerRepository().save(data);
        return SkillResult.SUCCESS;
    }
    private SkillResult useDomainStartup(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
    private SkillResult useRCT(ServerPlayerEntity player) { return SkillResult.NOT_IMPLEMENTED; }
}
