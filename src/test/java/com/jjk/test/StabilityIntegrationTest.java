package com.jjk.test;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.combat.AntiAbuseManager;
import com.jjk.combat.BlackFlashHandler;
import com.jjk.economy.CursedStoneManager;
import com.jjk.data.Migrator;
import com.jjk.data.PlayerRepository;
import com.jjk.data.backup.RotatingBackup;
import com.jjk.discord.DiscordWebhook;
import com.jjk.domain.DomainBlockHistoryDao;
import com.jjk.domain.DomainBlockQueue;
import com.jjk.effect.EffectDeferQueue;
import com.jjk.performance.TPSGuard;
import com.jjk.security.MacroDetector;
import net.minecraft.server.MinecraftServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// TASK J-6-1: 안정성/보안 시스템 통합 테스트
class StabilityIntegrationTest {

    @BeforeEach
    void setUp() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        new Migrator().migrate(conn);
        PlayerRepository repo = new PlayerRepository(conn);
        CursedStoneManager csm = new CursedStoneManager(repo);
        JJKMod.initForTest(csm, repo);
    }

    private static Object invokePrivate(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(target, args);
    }

    private static DomainBlockQueue newBlockQueue() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        return new DomainBlockQueue(new DomainBlockHistoryDao(conn));
    }

    // ─── 1~3. TPSGuard ──────────────────────────────────────────────────────

    @Test
    void testTpsGuard_tps14_warningHalvesParticleRate() throws Exception {
        EffectDeferQueue effectQueue = new EffectDeferQueue();
        TPSGuard guard = new TPSGuard(effectQueue, newBlockQueue());

        Object level = invokePrivate(guard, "resolveLevel", new Class<?>[]{float.class}, 14f);
        assertEquals("WARNING", level.toString(), "TPS 14는 WARNING 단계로 분류되어야 함");

        invokePrivate(guard, "applyWarning", new Class<?>[]{MinecraftServer.class}, (Object) null);
        assertEquals(0.5f, effectQueue.getParticleRate(), 0.0001f,
                "WARNING 단계 진입 시 파티클 송출 비율이 절반(0.5)으로 줄어야 함");
    }

    @Disabled("MinecraftServer 실서버 필요 — applyCritical()이 server.getPlayerManager()로 시야 거리 저장/축소 (testWeaknessZone과 동일 사유)")
    @Test
    void testTpsGuard_tps9_criticalStopsParticlesAndBlocksDeployments() {
        // 실서버 통합 테스트 대상:
        //   1. 평균 틱 시간이 ~111ms(TPS≈9)인 실서버로 TPSGuard.tick(server) 호출
        //   2. EffectDeferQueue.getParticleRate() == 0.0f 확인
        //   3. DomainBlockQueue.blockNewDeployments(true) 적용(newDeploymentsBlocked=true) 확인
    }

    @Test
    void testTpsGuard_tps17_recoveryRestoresNormalRate() throws Exception {
        EffectDeferQueue effectQueue = new EffectDeferQueue();
        effectQueue.setParticleRate(0.0f); // CRITICAL 단계였다고 가정한 사전 상태
        TPSGuard guard = new TPSGuard(effectQueue, newBlockQueue());

        Object level = invokePrivate(guard, "resolveLevel", new Class<?>[]{float.class}, 17f);
        assertEquals("NORMAL", level.toString(), "TPS 17은 NORMAL 단계로 분류되어야 함");

        invokePrivate(guard, "applyRecovery", new Class<?>[]{MinecraftServer.class}, (Object) null);
        assertEquals(1.0f, effectQueue.getParticleRate(), 0.0001f,
                "NORMAL 복원 시 파티클 송출 비율이 정상(1.0)으로 복원되어야 함");
    }

    // ─── 4~5. MovementValidator ─────────────────────────────────────────────

    @Disabled("ServerPlayerEntity 실엔티티 필요 — validate()가 player.getPos()/teleport() 등 실엔티티 API 직접 호출 (testWeaknessZone과 동일 사유, mock 없이 순수 로직 테스트 원칙)")
    @Test
    void testMovementValidator_abnormalMovement_rollsBackToLastValidPosition() {
        // 실서버 통합 테스트 대상:
        //   1. lastValidPos 대비 비정상 거리(MAX_DISTANCE_PER_TICK=8.0 초과) 이동을 3회 반복
        //   2. count==3에서 player.teleport()로 마지막 정상 위치로 롤백되는지 확인
    }

    @Disabled("ServerPlayerEntity 실엔티티 필요 — validate()가 player.getPos()/teleport() 등 실엔티티 API 직접 호출 (testWeaknessZone과 동일 사유, mock 없이 순수 로직 테스트 원칙)")
    @Test
    void testMovementValidator_markTeleportPending_allowsLargeJumpWithoutRollback() {
        // 실서버 통합 테스트 대상:
        //   1. markTeleportPending(uuid, currentTick)으로 nonce 발급(만료=currentTick+2)
        //   2. nonce 만료 전에 큰 폭의 이동 발생 → 텔레포트 예외로 허용, 위반 카운트 증가 없음
        //   3. violationCounts에 누적되지 않아 count==3 롤백이 발생하지 않는지 확인
    }

    // ─── 6. MacroDetector ───────────────────────────────────────────────────

    @Test
    void testMacroDetector_uniformOneTickIntervals_detectedAsMacro() {
        MacroDetector detector = new MacroDetector();
        UUID uuid = UUID.randomUUID();

        // 1틱(50ms) ±2ms 패턴 5회 — 표준편차 약 1.41ms (< UNIFORMITY_STDDEV_THRESHOLD_MS=5.0)
        long[] intervalsMs = {50, 49, 51, 48, 52};
        long t = 10_000_000L;
        boolean detected = detector.checkMacro(uuid, t); // 최초 호출 — 기준 타임스탬프 등록(false)
        assertFalse(detected, "최초 패킷은 비교 대상이 없어 매크로로 판정되지 않아야 함");

        for (long interval : intervalsMs) {
            t += interval;
            detected = detector.checkMacro(uuid, t);
        }

        assertTrue(detected, "1틱 ±2ms 균일 패턴 5회 누적 시 checkMacro()는 true를 반환해야 함");
    }

    // ─── 7. AntiAbuseManager / BlackFlashHandler ────────────────────────────

    @Test
    void testFlagMacro_fiveAccumulations_triggersDowngradeToNormal() {
        UUID uuid = UUID.randomUUID();
        BlackFlashHandler handler = new BlackFlashHandler();

        // 균일 간격 패킷을 충분히 보내 checkMacro()가 5회 이상 true를 반환하도록 한다
        // (flagMacro 누적 5회 → DOWNGRADE_MACRO_THRESHOLD 도달 → downgradeToNormal 호출)
        long[] intervalsMs = {50, 49, 51, 48, 52, 50, 49, 51, 48, 52, 50, 49};
        long t = 20_000_000L;
        handler.onTimingReport(uuid, t);
        for (long interval : intervalsMs) {
            t += interval;
            handler.onTimingReport(uuid, t);
        }

        assertTrue(handler.isDowngraded(uuid),
                "매크로 의심 누적이 5회에 도달하면 downgradeToNormal()이 호출되어 흑섬 Perfect 판정이 강등되어야 함");
        assertTrue(JJKMod.getAntiAbuseManager().getMacroFlags(uuid) >= 5,
                "AntiAbuseManager.flagMacro 누적 횟수가 5 이상이어야 함");
    }

    // ─── 8~9. RotatingBackup ────────────────────────────────────────────────

    @Test
    void testRotatingBackup_copyAtomic_createsBakFile(@TempDir Path tempDir) throws Exception {
        Path source = tempDir.resolve("player_data.db");
        Files.writeString(source, "dummy-db-snapshot");
        Path dest = tempDir.resolve("player_data_2026-06-07_00-00.db.bak");

        invokePrivate(new RotatingBackup(), "copyAtomic", new Class<?>[]{Path.class, Path.class}, source, dest);

        assertTrue(Files.exists(dest), "백업 실행 후 .bak 파일이 생성되어야 함");
        assertEquals("dummy-db-snapshot", Files.readString(dest), "백업된 내용이 원본 DB와 일치해야 함");
        assertFalse(Files.exists(dest.resolveSibling(dest.getFileName().toString() + ".tmp")),
                "원자적 복사 완료 후 임시(.tmp) 파일이 남아있지 않아야 함");
    }

    @Test
    void testRotatingBackup_purgeByPrefix_keepCount24RemovesOldest25th(@TempDir Path tempDir) throws Exception {
        String prefix = "player_data_";
        String suffix = ".db.bak";
        for (int i = 1; i <= 25; i++) {
            String name = String.format("%s2026-01-%02d_00-00%s", prefix, i, suffix);
            Files.writeString(tempDir.resolve(name), "x");
        }

        invokePrivate(new RotatingBackup(), "purgeByPrefix",
                new Class<?>[]{Path.class, String.class, int.class}, tempDir, prefix, 24);

        try (var stream = Files.list(tempDir)) {
            List<String> remaining = stream.map(p -> p.getFileName().toString()).sorted().toList();
            assertEquals(24, remaining.size(), "keepCount=24 초과분(가장 오래된 1개)이 삭제되어야 함");
            assertFalse(remaining.contains(prefix + "2026-01-01_00-00" + suffix),
                    "가장 오래된(25번째) 백업 파일이 삭제되어야 함");
            assertTrue(remaining.contains(prefix + "2026-01-25_00-00" + suffix),
                    "가장 최신 백업 파일은 보존되어야 함");
        }
    }

    // ─── 10. DiscordWebhook ─────────────────────────────────────────────────

    @Test
    void testDiscordWebhook_urlNotConfigured_sendAsyncDoesNotThrow() {
        JJKMod.getConfig().discordWebhookUrl = "";
        assertDoesNotThrow(() -> DiscordWebhook.sendAsync("[테스트] discordWebhookUrl 미설정 — 전송 스킵 확인"),
                "discordWebhookUrl 미설정 시 sendAsync()는 NPE 없이 조용히 스킵되어야 함");
    }
}
