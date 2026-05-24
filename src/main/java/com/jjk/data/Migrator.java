package com.jjk.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Migrator {

    private static final int CURRENT_DB_VERSION = 1;

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
            case 1 -> { /* initial schema is created by init() DDL ??no ALTER needed */ }
            // case 2 -> conn.createStatement().execute("ALTER TABLE player_data ADD COLUMN ...");
        }
    }
}
