package com.jjk.combat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// 흑섬 Perfect 발동 틱 추적 (ItadoriSkillSet shrine 흑섬 콤보 윈도우 판정용).
// 서버 재시작 시 초기화됨 — 윈도우 20틱(1초)이므로 영속성 불필요.
public class BlackFlashPerfectTracker {

    private static final Map<UUID, Long> lastPerfectTick = new ConcurrentHashMap<>();

    public static void record(UUID playerId, long tick) {
        lastPerfectTick.put(playerId, tick);
    }

    public static boolean isWithinWindow(UUID playerId, long currentTick, int windowTicks) {
        Long last = lastPerfectTick.get(playerId);
        return last != null && (currentTick - last) <= windowTicks;
    }
}
