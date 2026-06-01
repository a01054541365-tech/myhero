package com.jjk.effect;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

/** 화염 상태이상 공통 유틸. 매 틱 데미지는 TickScheduler에서 tickFire 호출로 처리. */
public final class FireEffectManager {
    private FireEffectManager() {}

    private static final String KEY = "status_fire";

    /** 화염 상태 적용 — 매 틱 데미지는 이 메서드에서 직접 처리하지 않음 */
    public static void apply(PlayerData target, int durationTicks, long currentTick) {
        target.cooldowns.put(KEY, currentTick + durationTicks);
    }

    public static boolean isOnFire(PlayerData target, long currentTick) {
        return target.cooldowns.getOrDefault(KEY, 0L) > currentTick;
    }

    /** TickScheduler에서 호출. CombatPipeline 경유 없이 HP 직접 차감 (도트 데미지). */
    public static void tickFire(PlayerData target, long currentTick, float damagePerTick) {
        if (!isOnFire(target, currentTick)) return;
        target.hpCurrent = Math.max(0f, target.hpCurrent - damagePerTick);
    }

    /** TickScheduler Consumer<ServerPlayerEntity> 진입점. */
    public static void tickFirePlayer(ServerPlayerEntity player) {
        long currentTick = player.getWorld().getTime();
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (!isOnFire(data, currentTick)) return;
        tickFire(data, currentTick, 1.0f);
        JJKMod.getPlayerRepository().save(data);
    }
}
