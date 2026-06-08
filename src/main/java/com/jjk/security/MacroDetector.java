package com.jjk.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 흑섬 입력 타이밍 패킷의 간격을 추적해 매크로(기계적으로 균일한 입력)를 탐지한다.
 * 표준편차가 임계값 미만으로 지나치게 균일하면 매크로로 판정.
 */
public class MacroDetector {

    private static final int SAMPLE_SIZE = 5;
    private static final double UNIFORMITY_STDDEV_THRESHOLD_MS = 5.0;

    private final Map<UUID, Long> lastTimestampMs = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> intervalHistory = new ConcurrentHashMap<>();

    /** 새 타이밍 패킷 수신 시 호출. 최근 SAMPLE_SIZE개 간격이 비정상적으로 균일하면 true. */
    public boolean checkMacro(UUID playerId, long packetTimestampMs) {
        Long last = lastTimestampMs.put(playerId, packetTimestampMs);
        if (last == null) return false;

        long interval = packetTimestampMs - last;
        if (interval <= 0) return false;

        Deque<Long> history = intervalHistory.computeIfAbsent(playerId, k -> new ArrayDeque<>());
        history.addLast(interval);
        while (history.size() > SAMPLE_SIZE) {
            history.removeFirst();
        }
        if (history.size() < SAMPLE_SIZE) return false;

        return isSuspiciouslyUniform(history);
    }

    private boolean isSuspiciouslyUniform(Deque<Long> intervals) {
        double mean = intervals.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double variance = intervals.stream()
                .mapToDouble(v -> (v - mean) * (v - mean))
                .average().orElse(0.0);
        return Math.sqrt(variance) < UNIFORMITY_STDDEV_THRESHOLD_MS;
    }

    public void clear(UUID playerId) {
        lastTimestampMs.remove(playerId);
        intervalHistory.remove(playerId);
    }
}
