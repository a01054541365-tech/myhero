package com.jjk.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 틱 처리 구간(예: CombatPipeline)의 소요 시간을 측정해 예산 초과 시 경고 로그를 남긴다. */
public class TickBudgetMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-tick-budget");

    private static final double WARNING_MS = 15.0;
    private static final double DANGER_MS  = 25.0;

    private long startNanos = 0L;

    /** 측정 구간 시작. */
    public void startTick() {
        startNanos = System.nanoTime();
    }

    /** 측정 구간 종료 + 소요 시간(ms) 반환. 예산 초과 시 WARN 로그 출력 (틱 차단 금지). */
    public double usedMs(String label) {
        double elapsedMs = (System.nanoTime() - startNanos) / 1_000_000.0;
        if (elapsedMs >= DANGER_MS) {
            LOGGER.warn("[JJK] {} 처리 시간 위험 수준: {}ms (기준 {}ms)", label, String.format("%.2f", elapsedMs), DANGER_MS);
        } else if (elapsedMs >= WARNING_MS) {
            LOGGER.warn("[JJK] {} 처리 시간 경고: {}ms (기준 {}ms)", label, String.format("%.2f", elapsedMs), WARNING_MS);
        }
        return elapsedMs;
    }
}
