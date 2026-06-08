package com.jjk.data.backup;

import com.jjk.JJKMod;
import com.jjk.discord.DiscordWebhook;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * player_data.db 회전식 백업.
 * 1시간마다 스냅샷(최대 backupKeepCount개), 자정마다 daily 백업(최대 backupDailyKeepCount일치)을 보관한다.
 * 비동기로 실행해 메인 스레드를 차단하지 않는다.
 */
public class RotatingBackup {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-rotating-backup");

    private static final String BACKUP_DIR_NAME   = "backups";
    private static final String BACKUP_SUBDIR     = "jjk";
    private static final String HOURLY_PREFIX     = "player_data_";
    private static final String DAILY_PREFIX      = "daily_";
    private static final String SUFFIX            = ".db.bak";
    private static final DateTimeFormatter HOURLY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");
    private static final DateTimeFormatter DAILY_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private LocalDate lastDailyBackupDate;

    /** 백업 디렉터리: <run>/backups/jjk/ */
    public static Path backupDir(MinecraftServer server) {
        return server.getRunDirectory().resolve(BACKUP_DIR_NAME).resolve(BACKUP_SUBDIR);
    }

    /** 비동기로 player_data.db 스냅샷을 생성한다. 메인 스레드 차단 금지. */
    public void backup(Path dbPath) {
        MinecraftServer server = JJKMod.getServer();
        if (server == null || !Files.exists(dbPath)) return;

        CompletableFuture.runAsync(() -> {
            try {
                Path dir = backupDir(server);
                Files.createDirectories(dir);

                LocalDateTime now = LocalDateTime.now();
                String hourlyName = HOURLY_PREFIX + now.format(HOURLY_FMT) + SUFFIX;
                copyAtomic(dbPath, dir.resolve(hourlyName));
                LOGGER.info("[JJK] 백업 완료: {}", hourlyName);
                purgeOld(JJKMod.getConfig().backupKeepCount);

                LocalDate today = now.toLocalDate();
                if (!today.equals(lastDailyBackupDate)) {
                    String dailyName = DAILY_PREFIX + today.format(DAILY_FMT) + SUFFIX;
                    copyAtomic(dbPath, dir.resolve(dailyName));
                    LOGGER.info("[JJK] 일일 백업 완료: {}", dailyName);
                    purgeByPrefix(dir, DAILY_PREFIX, JJKMod.getConfig().backupDailyKeepCount);
                    lastDailyBackupDate = today;
                }
            } catch (IOException e) {
                LOGGER.error("[JJK] 백업 실패: {}", dbPath, e);
                DiscordWebhook.sendAsync("[백업 실패] player_data.db 백업 중 오류: " + e.getMessage());
            }
        });
    }

    /** 임시파일(.tmp)로 먼저 쓰고 rename — 복사 중 크래시에도 손상된 백업이 남지 않도록. */
    private void copyAtomic(Path source, Path dest) throws IOException {
        Path tmp = dest.resolveSibling(dest.getFileName().toString() + ".tmp");
        Files.copy(source, tmp, StandardCopyOption.REPLACE_EXISTING);
        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /** 시간별 스냅샷(player_data_*) 중 keepCount를 초과하는 오래된 항목을 삭제한다. */
    public void purgeOld(int keepCount) {
        MinecraftServer server = JJKMod.getServer();
        if (server == null) return;
        purgeByPrefix(backupDir(server), HOURLY_PREFIX, keepCount);
    }

    private void purgeByPrefix(Path dir, String prefix, int keepCount) {
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> backups = stream
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return name.startsWith(prefix) && name.endsWith(SUFFIX);
                    })
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .toList();
            for (int i = keepCount; i < backups.size(); i++) {
                Files.deleteIfExists(backups.get(i));
            }
        } catch (IOException e) {
            LOGGER.error("[JJK] 오래된 백업 정리 실패: prefix={}", prefix, e);
        }
    }

    /** TickScheduler(주기) + 서버 종료 시 최종 백업 등록. */
    public void register(MinecraftServer server) {
        if (!JJKMod.getConfig().backupEnabled) return;

        Path dbPath = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                .resolve("jjk").resolve("player_data.db");

        JJKMod.getTickScheduler().registerServerTask(
                sv -> backup(dbPath), JJKMod.getConfig().backupIntervalTicks);

        ServerLifecycleEvents.SERVER_STOPPING.register(sv -> backup(dbPath));
    }
}
