package com.jjk.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Migrator {

    private static final int CURRENT_DB_VERSION = 5;

    public void migrate(Connection conn) throws SQLException {
        int version = getDbVersion(conn);
        while (version < CURRENT_DB_VERSION) {
            applyMigration(conn, ++version);
            setDbVersion(conn, version);
        }
    }

    private int getDbVersion(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA user_version")) {
            return rs.getInt(1);
        }
    }

    private void setDbVersion(Connection conn, int version) throws SQLException {
        // PRAGMA user_version does not support ? placeholders
        conn.createStatement().execute("PRAGMA user_version = " + version);
    }

    private void applyMigration(Connection conn, int targetVersion) throws SQLException {
        switch (targetVersion) {
            case 1 -> { /* initial schema is created by init() DDL — no ALTER needed */ }
            case 2 -> {
                String[] alters = {
                    "ALTER TABLE player_data ADD COLUMN jackpot_active INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE player_data ADD COLUMN jackpot_end_tick INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE player_data ADD COLUMN last_jackpot_attempt_tick INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE player_data ADD COLUMN has_execution_sword INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE player_data ADD COLUMN infinity_active INTEGER NOT NULL DEFAULT 0"
                };
                for (String sql : alters) {
                    try { conn.createStatement().execute(sql); } catch (SQLException ignored) {}
                }
            }
            case 3 -> {
                try {
                    conn.createStatement().execute(
                        "ALTER TABLE player_data ADD COLUMN overtime_work INTEGER NOT NULL DEFAULT 0"
                    );
                } catch (SQLException ignored) {}
            }
            case 4 -> {
                String[] alters = {
                    "ALTER TABLE player_data ADD COLUMN falling_blossom_active INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE player_data ADD COLUMN falling_blossom_until INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE player_data ADD COLUMN simple_barrier_active INTEGER NOT NULL DEFAULT 0"
                };
                for (String sql : alters) {
                    try { conn.createStatement().execute(sql); } catch (SQLException ignored) {}
                }
            }
            case 5 -> {
                // curtain_active was missing from all previous migrations
                addColumnIfMissing(conn, "curtain_active",          "INTEGER NOT NULL DEFAULT 0");
                addColumnIfMissing(conn, "infinity_active",         "INTEGER NOT NULL DEFAULT 0");
                addColumnIfMissing(conn, "overtime_work",           "INTEGER NOT NULL DEFAULT 0");
                addColumnIfMissing(conn, "falling_blossom_active",  "INTEGER NOT NULL DEFAULT 0");
                addColumnIfMissing(conn, "falling_blossom_until",   "INTEGER NOT NULL DEFAULT 0");
                addColumnIfMissing(conn, "simple_barrier_active",   "INTEGER NOT NULL DEFAULT 0");
            }
        }
    }

    private void addColumnIfMissing(Connection conn,
            String column, String definition) throws SQLException {
        try {
            conn.createStatement().execute(
                "ALTER TABLE player_data ADD COLUMN " + column + " " + definition);
        } catch (SQLException ignored) {
            // column already exists — idempotent
        }
    }
}
