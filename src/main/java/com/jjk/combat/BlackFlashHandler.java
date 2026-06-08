package com.jjk.combat;

import com.jjk.JJKMod;
import com.jjk.security.MacroDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 클라이언트가 보고하는 흑섬 입력 타이밍을 매크로 탐지기에 연결하고,
 * 매크로 누적 시 흑섬 Perfect 판정을 완전히 차단(일반 흑섬으로 강등)한다.
 */
public class BlackFlashHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-blackflash-handler");
    private static final int DOWNGRADE_MACRO_THRESHOLD = 5;

    private final MacroDetector macroDetector = new MacroDetector();
    private final Set<UUID> downgraded = ConcurrentHashMap.newKeySet();

    /** 클라이언트의 흑섬 입력 타이밍 보고 패킷 처리. */
    public void onTimingReport(UUID playerId, long packetTimestampMs) {
        if (!macroDetector.checkMacro(playerId, packetTimestampMs)) return;

        AntiAbuseManager aam = JJKMod.getAntiAbuseManager();
        int total = aam != null ? aam.flagMacro(playerId) : 0;
        if (total >= DOWNGRADE_MACRO_THRESHOLD) {
            downgradeToNormal(playerId);
        }
    }

    /** Perfect 판정 완전 차단 — 이후 흑섬은 일반(Great)으로만 발동. */
    public void downgradeToNormal(UUID playerId) {
        if (downgraded.add(playerId)) {
            LOGGER.warn("[JJK-AC] 흑섬 매크로 누적 5회 — Perfect 판정 강등. player={}", playerId);
        }
    }

    public boolean isDowngraded(UUID playerId) {
        return downgraded.contains(playerId);
    }

    public void clear(UUID playerId) {
        macroDetector.clear(playerId);
        downgraded.remove(playerId);
    }
}
