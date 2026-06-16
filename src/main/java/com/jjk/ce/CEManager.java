package com.jjk.ce;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

public class CEManager {

    /** 날씨 패시브 보너스 (WeatherPassiveManager가 설정). §LOCK 수치 외부 보너스. */
    public static float weatherRegenBonus = 0.0f;

    private final JjkConfig config;
    private final CEPool pool = new CEPool();

    public CEManager(JjkConfig config) {
        this.config = config;
    }

    public void regenTick(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data.characterId == null) return;

        long currentTick = player.getWorld().getTime();
        CERegenRule rule = pool.getRule(data.characterId);
        float rate = rule.regenPerTick(data, currentTick, config) + weatherRegenBonus;

        // CE 재생 디버프 체크 (SmallpoxDeityEntity 등 — -50% 재생)
        Long ceDebuffUntil = data.cooldowns.get("ce_regen_debuff_until");
        if (ceDebuffUntil != null && currentTick < ceDebuffUntil) {
            rate *= 0.5f;
        }

        float before = data.ceCurrent;
        data.ceCurrent = Math.min(data.ceCurrent + rate, data.ceMax);

        // §6-2 고죠 무한 유지비: 30/s = 1.5/틱, CE 부족 시 자동 해제
        if (data.infinityActive) {
            if (data.ceCurrent < 1.5f) {
                data.infinityActive = false;
            } else {
                data.ceCurrent -= 1.5f;
            }
        }

        // 수동 방어 CE 소모: max_ce × shieldCeDrainRatio / 틱
        if (data.shieldActive) {
            float drain = data.ceMax * config.shieldCeDrainRatio();
            data.ceCurrent = Math.max(0f, data.ceCurrent - drain);
            if (data.ceCurrent <= 0f) data.shieldActive = false;
        }

        // 간이영역 CE 유지 소모: max_ce × simpleBarrierCostPerSecond / 20틱
        if (data.simpleBarrierActive) {
            float drain = data.ceMax * config.simpleBarrierCostPerSecond() / 20.0f;
            data.ceCurrent = Math.max(0f, data.ceCurrent - drain);
            if (data.ceCurrent <= 0f) {
                data.simpleBarrierActive = false;
                data.fallingBlossomActive = false;
            }
        }

        // 영역전연 CE 유지 소모: simpleBarrierCostPerSecond와 동일 비율 적용
        if (data.domainAmplificationActive) {
            float drain = data.ceMax * config.simpleBarrierCostPerSecond() / 20.0f;
            data.ceCurrent = Math.max(0f, data.ceCurrent - drain);
            if (data.ceCurrent <= 0f) data.domainAmplificationActive = false;
        }

        // 천여주박: CE 0 → 신체능력 버프 활성. CE 1 이상 회복 → 버프 해제.
        if (data.ceCurrent <= 0f && !data.tenShadowsActive) {
            data.tenShadowsActive = true;
        } else if (data.ceCurrent >= 1f && data.tenShadowsActive) {
            data.tenShadowsActive = false;
        }

        if (data.ceCurrent != before) {
            JJKMod.getPlayerRepository().save(data);
        }
    }

    /** CE 0이면 스킬 사용 불가. */
    public boolean canUseSkill(PlayerData data) {
        return data.ceCurrent > 0f;
    }

    public boolean canAfford(ServerPlayerEntity player, float amount) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        return data.ceCurrent >= amount;
    }

    public boolean consume(ServerPlayerEntity player, float amount) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data.ceCurrent < amount) return false;
        data.ceCurrent -= amount;
        JJKMod.getPlayerRepository().save(data);
        return true;
    }

    // Pure-data overloads (CombatPipeline.processData 및 테스트용)
    public boolean consumeCE(PlayerData data, float amount) {
        // 잭팟 중 CE CAP 체크 우회 — 차감은 항상 진행, 음수 보호만 유지
        if (!data.jackpotActive && data.ceCurrent < amount) return false;
        data.ceCurrent = Math.max(0f, data.ceCurrent - amount);
        return true;
    }

    public void refundCE(PlayerData data, float amount) {
        data.ceCurrent = Math.min(data.ceCurrent + amount, data.ceMax);
    }

    /** SmallpoxDeityEntity 전용: CE 재생 -50% 디버프 (durationTicks 동안 유지). */
    public void applyCeRegenDebuff(ServerPlayerEntity target, int durationTicks) {
        PlayerData data = JJKMod.getPlayerRepository().load(target.getUuid());
        long expiry = target.getWorld().getTime() + durationTicks;
        data.cooldowns.merge("ce_regen_debuff_until", expiry, Math::max);
        JJKMod.getPlayerRepository().save(data);
    }
}
