package com.jjk.awakening;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;
import com.jjk.network.s2c.AwakeningS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

// §LOCK: HP 30% / 160틱 / 2400틱 — 변경 시 AwakeningManagerTest 깨짐, 절대 수정 금지
public class AwakeningManager {

    private static final int AWAKENING_DURATION_TICKS = 160;
    private static final int AWAKENING_COOLDOWN_TICKS = 2400;

    private final JjkConfig config;

    public AwakeningManager(JjkConfig config) {
        this.config = config;
    }

    public void tickCheck(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long currentTick = player.getWorld().getTime();

        if (data.awakeningActive && currentTick >= data.awakeningEndTick) {
            deactivate(data);
            JJKMod.getPlayerRepository().save(data);
            ServerPlayNetworking.send(player, new AwakeningS2CPacket(false));
        }
    }

    public boolean checkAndActivate(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        long currentTick = player.getWorld().getTime();

        if (data.jackpotActive) return false;
        if (data.awakeningActive) return false;
        if (currentTick < data.awakeningCooldownUntil) return false;

        float hpRatio = player.getHealth() / player.getMaxHealth();
        if (hpRatio > config.awakeningHpThreshold()) return false;

        data.awakeningActive = true;
        data.awakeningEndTick = currentTick + AWAKENING_DURATION_TICKS;
        // §3-7: set cooldown at activation to close the race window (H-4)
        data.awakeningCooldownUntil = currentTick + AWAKENING_COOLDOWN_TICKS;
        JJKMod.getPlayerRepository().save(data);
        ServerPlayNetworking.send(player, new AwakeningS2CPacket(true));
        return true;
    }

    private void deactivate(PlayerData data) {
        data.awakeningActive = false;
        data.awakeningEndTick = 0;
    }

    // ─── Pure-data overloads (CombatPipeline.processData 및 테스트용) ──────────

    // §LOCK 상수 — 테스트에서 직접 참조 가능
    public static final int   DURATION_TICKS      = AWAKENING_DURATION_TICKS; // 160
    public static final int   COOLDOWN_TICKS_CONST = AWAKENING_COOLDOWN_TICKS; // 2400

    /**
     * §3-7: 순수 PlayerData 기반 각성 발동 판정. MC 패킷 미전송.
     * CombatPipeline 5단계에서만 호출할 것 (CLAUDE.md §E 규정).
     */
    public void checkAndActivate(PlayerData data, float currentHp, float maxHp, long tick) {
        if (data.jackpotActive) return;
        float threshold = (JJKMod.getInstance() != null)
                ? JJKMod.getConfig().awakeningHpThreshold() : config.awakeningHpThreshold();
        if (currentHp > maxHp * threshold) return;
        if (tick < data.awakeningCooldownUntil) return;
        if (data.awakeningActive) return;
        data.awakeningActive = true;
        data.awakeningEndTick = tick + AWAKENING_DURATION_TICKS;   // §LOCK: 160
        data.awakeningCooldownUntil = tick + AWAKENING_COOLDOWN_TICKS; // §LOCK: 2400
    }

    /**
     * §3-7: 각성 만료 tick 체크 (순수 PlayerData). MC 패킷 미전송.
     * ServerPlayerEntityMixin tick at=TAIL 에서 호출 (TickScheduler 경유).
     */
    public void tickCheck(PlayerData data, long tick) {
        if (data.awakeningActive && tick >= data.awakeningEndTick) {
            data.awakeningActive = false;
            data.awakeningEndTick = 0;
        }
    }
}
