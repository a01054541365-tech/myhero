package com.jjk.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AuditLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-audit");

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS audit_log (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp   INTEGER NOT NULL,
                event_type  TEXT    NOT NULL,
                player_uuid TEXT    NOT NULL,
                detail      TEXT
            )""";

    private static final String INSERT =
            "INSERT INTO audit_log (timestamp, event_type, player_uuid, detail) VALUES (?,?,?,?)";

    private final Connection conn;

    // Test constructor: uses provided connection (table created by Migrator)
    public AuditLogger(Connection conn) {
        this.conn = conn;
    }

    // Production factory: opens own connection to the DB file
    public static AuditLogger open(Path dbPath) {
        try {
            Files.createDirectories(dbPath.getParent());
            Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
            try (Statement st = c.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute(DDL);
            }
            return new AuditLogger(c);
        } catch (Exception e) {
            throw new RuntimeException("Failed to open audit_log DB at " + dbPath, e);
        }
    }

    /**
     * eventType 허용값: "death"|"finger_drop"|"execution_sword"|"char_change"|
     *                   "admin_cmd"|"domain_start"|"domain_end"
     */
    public void logEvent(String eventType, UUID playerUuid, String detailJson, long tick) {
        CompletableFuture.runAsync(() -> {
            synchronized (conn) {
                try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
                    ps.setLong(1, tick);
                    ps.setString(2, eventType);
                    ps.setString(3, playerUuid.toString());
                    ps.setString(4, detailJson);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    LOGGER.error("AuditLogger.logEvent failed: type={} player={}", eventType, playerUuid, e);
                }
            }
        });
    }

    public void close() {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException e) {
            LOGGER.error("AuditLogger close failed", e);
        }
    }
}
