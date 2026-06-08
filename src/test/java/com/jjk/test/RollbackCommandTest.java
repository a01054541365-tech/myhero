package com.jjk.test;

import com.jjk.data.Migrator;
import com.jjk.data.PlayerData;
import com.jjk.data.PlayerRepository;
import com.jjk.domain.DomainBlockHistoryDao;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// TASK J-4-2: /jj rollback 검증
class RollbackCommandTest {

    // ─── 1. /jj rollback player — 백업 DB에서 PlayerData UUID 추출 ─────────────

    @Test
    void testLoadFromBackup_extractsPlayerDataByUuid(@TempDir Path tempDir) throws Exception {
        Path backupPath = tempDir.resolve("player_data_backup.db");
        UUID uuid = UUID.randomUUID();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + backupPath.toAbsolutePath())) {
            new Migrator().migrate(conn);
            PlayerRepository setup = new PlayerRepository(conn);
            PlayerData d = PlayerData.createDefault(uuid);
            d.characterId = "itadori";
            d.grade = "1급";
            setup.saveImmediate(d);
        }

        PlayerRepository repo = new PlayerRepository();
        PlayerData restored = repo.loadFromBackup(backupPath, uuid);

        assertNotNull(restored, "백업 파일에서 PlayerData를 추출해야 함");
        assertEquals(uuid, restored.uuid, "UUID가 요청한 값과 일치해야 함");
        assertEquals("itadori", restored.characterId, "characterId가 백업 값과 일치해야 함");
        assertEquals("1급", restored.grade, "grade가 백업 값과 일치해야 함");
    }

    @Test
    void testLoadFromBackup_unknownUuidReturnsNull(@TempDir Path tempDir) throws Exception {
        Path backupPath = tempDir.resolve("empty_backup.db");
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + backupPath.toAbsolutePath())) {
            new Migrator().migrate(conn);
        }

        PlayerRepository repo = new PlayerRepository();
        assertNull(repo.loadFromBackup(backupPath, UUID.randomUUID()),
                "백업 파일에 없는 UUID는 null을 반환해야 함");
    }

    // ─── 2. /jj rollback domain — 미복구 블록 전체 복구 후 recovered=1 ─────────

    @Test
    void testDomainRollback_markAllRecoveredSetsRecoveredFlag() throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        createBlockHistoryTable(conn);
        DomainBlockHistoryDao dao = new DomainBlockHistoryDao(conn);

        String domainId = "test_domain";
        String worldKey = "minecraft:overworld";
        insertHistoryRow(conn, domainId, worldKey, 0, 64, 0, "minecraft:stone", "minecraft:bedrock");
        insertHistoryRow(conn, domainId, worldKey, 1, 64, 0, "minecraft:dirt", "minecraft:bedrock");
        // 다른 도메인 — 영향 받지 않아야 함
        insertHistoryRow(conn, "other_domain", worldKey, 5, 64, 5, "minecraft:grass_block", "minecraft:bedrock");

        assertEquals(3, dao.loadUnrecovered().size(), "복구 전 미복구 항목 3개");

        dao.markAllRecovered(domainId);
        waitUntil(() -> countUnrecovered(conn, domainId) == 0, 2000);

        assertEquals(0, countUnrecovered(conn, domainId), "rollback domain 후 해당 도메인의 모든 항목이 recovered=1 이어야 함");
        assertEquals(1, dao.loadUnrecovered().size(), "다른 도메인의 미복구 항목은 영향받지 않아야 함");
    }

    // ─── 3. /jj rollback chunk — 미로드 청크 에러 메시지 ───────────────────────

    @Disabled("ServerWorld 실월드 필요 — 단위 테스트 환경에서 isChunkLoaded 검증 불가 (testInumakiScatterEntityInteraction과 동일 사유)")
    @Test
    void testChunkRollback_unloadedChunkReturnsErrorMessage() {
        // 실서버 통합 테스트 대상:
        //   1. world.isChunkLoaded(x, z) == false 인 좌표로 /jj rollback chunk 실행
        //   2. DomainBlockQueue.rollbackChunk() == -1 반환 확인
        //   3. 명령 출력 메시지 "청크가 로드되지 않았습니다. 해당 위치로 이동 후 재시도." 확인
    }

    // ─── 헬퍼 ─────────────────────────────────────────────────────────────────

    private static void createBlockHistoryTable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS domain_block_history (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    domain_id      TEXT    NOT NULL,
                    world_key      TEXT    NOT NULL,
                    pos_x          INTEGER NOT NULL,
                    pos_y          INTEGER NOT NULL,
                    pos_z          INTEGER NOT NULL,
                    original_state TEXT    NOT NULL,
                    changed_state  TEXT    NOT NULL,
                    recovered      INTEGER NOT NULL DEFAULT 0,
                    created_at     INTEGER NOT NULL
                )
                """);
        }
    }

    private static void insertHistoryRow(Connection conn, String domainId, String worldKey,
                                          int x, int y, int z,
                                          String originalState, String changedState) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO domain_block_history
                (domain_id, world_key, pos_x, pos_y, pos_z, original_state, changed_state, recovered, created_at)
                VALUES (?,?,?,?,?,?,?,0,?)
                """)) {
            ps.setString(1, domainId);
            ps.setString(2, worldKey);
            ps.setInt(3, x);
            ps.setInt(4, y);
            ps.setInt(5, z);
            ps.setString(6, originalState);
            ps.setString(7, changedState);
            ps.setLong(8, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    private static int countUnrecovered(Connection conn, String domainId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM domain_block_history WHERE domain_id=? AND recovered=0")) {
            ps.setString(1, domainId);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) return;
            Thread.sleep(10);
        }
        fail("비동기 작업이 제한 시간 내에 완료되지 않음");
    }
}
