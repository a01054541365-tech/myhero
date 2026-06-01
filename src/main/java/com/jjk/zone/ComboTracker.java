package com.jjk.zone;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.data.PlayerData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class ComboTracker {

    private static final int JUST_FRAME_MIN = 8;
    private static final int JUST_FRAME_MAX = 12;
    private static final int RESET_TICKS = 30;

    // Per-UUID state for production (singleton ComboTracker)
    private final Map<UUID, ComboState> states = new HashMap<>();

    // Per-instance state for single-player testing (new ComboTracker() per test)
    private long instanceLastAttackTick = -1L;

    // ─── Static pure check (§LOCK: Just Frame 8~12틱) ───────────────────────

    /**
     * §LOCK: Just Frame 윈도우 8~12틱. 수치 변경 금지.
     * attackTick < 0 이면 이전 공격 없음 → false.
     */
    public static boolean checkBlackFlash(long attackTick, long currentTick) {
        if (attackTick < 0) return false;
        long diff = currentTick - attackTick;
        return diff >= JUST_FRAME_MIN && diff <= JUST_FRAME_MAX;
    }

    /**
     * 흑섬 발동 확률 롤. Just Frame 판정 통과 후 호출.
     * 기본 1%, 체력 10% 미만 +7%, Zone 중 +10%.
     */
    public static boolean rollBlackFlash(PlayerData data, float currentHp, float maxHp) {
        JjkConfig cfg = (JJKMod.getInstance() != null) ? JJKMod.getConfig() : new JjkConfig();
        int rate = cfg.blackFlashBaseRate();
        if (currentHp <= maxHp * 0.10f) {
            rate += cfg.blackFlashLowHpBonus();
        }
        if (data.zoneActive) {
            rate += cfg.blackFlashZoneBonus();
        }
        return ThreadLocalRandom.current().nextInt(100) < rate;
    }

    // ─── Per-instance methods (테스트 및 단일 플레이어 추적용) ─────────────────

    public void recordAttack(long tick) {
        this.instanceLastAttackTick = tick;
    }

    /** §LOCK: 콤보 리셋 30틱. 수치 변경 금지. */
    public void tick(long currentTick) {
        if (instanceLastAttackTick >= 0 && currentTick - instanceLastAttackTick > RESET_TICKS) {
            instanceLastAttackTick = -1L;
        }
    }

    public long getLastAttackTick() {
        return instanceLastAttackTick;
    }

    // ─── UUID-based methods for production singleton ─────────────────────────

    public boolean isJustFrame(UUID playerUuid, int ticksSinceLastHit) {
        return ticksSinceLastHit >= JUST_FRAME_MIN && ticksSinceLastHit <= JUST_FRAME_MAX;
    }

    public void recordHit(UUID playerUuid, long currentTick) {
        states.put(playerUuid, new ComboState(currentTick));
    }

    public void tick(UUID playerUuid, long currentTick) {
        ComboState state = states.get(playerUuid);
        if (state != null && currentTick - state.lastHitTick > RESET_TICKS) {
            states.remove(playerUuid);
        }
    }

    private record ComboState(long lastHitTick) {}

    // ─── Q3 콤보 시스템 (PlayerData 기반) ────────────────────────────────────

    private static final int COMBO_RESET_TICKS = 30;

    public void onHit(PlayerData attacker, UUID targetUuid, long currentTick) {
        if (targetUuid == null) return;
        if (!targetUuid.equals(attacker.comboTargetUuid)
                || currentTick - attacker.comboLastHitTick > COMBO_RESET_TICKS) {
            attacker.comboCount = 0;
        }
        attacker.comboCount++;
        attacker.comboLastHitTick = currentTick;
        attacker.comboTargetUuid  = targetUuid;
    }

    public void onHurt(PlayerData data) {
        data.comboCount       = 0;
        data.comboTargetUuid  = null;
    }

    public int getComboLevel(PlayerData data) {
        return Math.min(10, data.comboCount / 3 + 1);
    }
}
