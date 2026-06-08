package com.jjk.server;

import com.jjk.JJKMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class BackupScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-backup");
    public static final int INTERVAL_TICKS = 72000;
    private static final int MAX_BACKUPS = 7;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static void tick(MinecraftServer server) {
        Path dbPath = server.getSavePath(WorldSavePath.ROOT).resolve("jjk/player_data.db");
        if (!Files.exists(dbPath)) return;

        Path backupDir = server.getSavePath(WorldSavePath.ROOT).resolve("jjk/backups");
        try {
            Files.createDirectories(backupDir);
            String ts = LocalDateTime.now().format(FMT);
            Path dest = backupDir.resolve("player_data_" + ts + ".db");

            // VACUUM INTO — SQLite 내장 백업 (WAL 커밋된 데이터만, 락 불필요)
            Connection conn = JJKMod.getPlayerRepository().getConnection();
            if (conn != null) {
                try (Statement st = conn.createStatement()) {
                    st.execute("VACUUM INTO '" + dest.toString().replace('\\', '/') + "'");
                }
            } else {
                Files.copy(dbPath, dest, StandardCopyOption.REPLACE_EXISTING);
            }

            // 오래된 백업 삭제 (MAX_BACKUPS 초과분)
            try (Stream<Path> stream = Files.list(backupDir)) {
                List<Path> backups = stream
                    .filter(p -> p.getFileName().toString().endsWith(".db"))
                    .sorted(Comparator.reverseOrder())
                    .toList();
                for (int i = MAX_BACKUPS; i < backups.size(); i++) {
                    Files.deleteIfExists(backups.get(i));
                }
            }

            LOGGER.info("[JJK] 자동 백업 완료: {}", dest.getFileName());
        } catch (IOException | java.sql.SQLException e) {
            LOGGER.error("[JJK] 자동 백업 실패", e);
        }
    }

    private BackupScheduler() {}
}
