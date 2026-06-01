package com.jjk.test;

import com.jjk.data.Migrator;
import com.jjk.data.PlayerData;
import com.jjk.data.PlayerRepository;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MigratorTest {

    private Connection newMemoryDb() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite::memory:");
    }

    @Test
    void testFreshInstall() throws Exception {
        Connection conn = newMemoryDb();
        new Migrator().migrate(conn);

        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, "player_data", null)) {
            assertTrue(rs.next(), "player_data 테이블이 생성되어야 함");
        }
        try (ResultSet rs = meta.getTables(null, null, "audit_log", null)) {
            assertTrue(rs.next(), "audit_log 테이블이 생성되어야 함");
        }
    }

    @Test
    void testSchemaVersion() throws Exception {
        Connection conn = newMemoryDb();
        new Migrator().migrate(conn);

        boolean found = false;
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, "player_data", "schema_version")) {
            found = rs.next();
        }
        assertTrue(found, "player_data 테이블에 schema_version 컬럼이 있어야 함");
    }

    @Test
    void testCreateDefault() throws Exception {
        Connection conn = newMemoryDb();
        new Migrator().migrate(conn);

        PlayerRepository repo = new PlayerRepository(conn);
        UUID uuid = UUID.randomUUID();
        PlayerData d = PlayerData.createDefault(uuid);
        repo.saveImmediate(d);

        PlayerData loaded = repo.load(uuid);
        assertEquals(1,    loaded.schemaVersion,          "schemaVersion");
        assertEquals(-1L,  loaded.bindingVowDeclaredTick, "bindingVowDeclaredTick");
        assertEquals("4급",  loaded.grade,                "grade");
        assertEquals("IDLE", loaded.trialState,           "trialState");
    }

    @Test
    void testV3CopyTechniqueColumnsExist() throws Exception {
        Connection conn = newMemoryDb();
        new Migrator().migrate(conn);

        DatabaseMetaData meta = conn.getMetaData();
        for (String col : new String[]{
                "last_received_skill_id", "last_received_base_damage",
                "last_received_ce_cost", "last_received_cooldown_ticks",
                "last_received_is_domain"}) {
            try (ResultSet rs = meta.getColumns(null, null, "player_data", col)) {
                assertTrue(rs.next(), col + " 컬럼이 player_data에 있어야 함");
            }
        }
    }

    @Test
    void testVersionMismatch() throws Exception {
        Connection conn = newMemoryDb();
        new Migrator().migrate(conn);

        // 미래 버전으로 강제 설정
        conn.createStatement().execute("PRAGMA user_version = 99");

        // migrate()는 99 >= CURRENT_VERSION(1) 이므로 no-op; 예외 없어야 함
        assertDoesNotThrow(() -> new Migrator().migrate(conn));
    }
}
