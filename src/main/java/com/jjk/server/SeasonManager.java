package com.jjk.server;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public final class SeasonManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-season");
    private static final String SEASON_FILE = "jjk/season_last_reset.txt";

    public static void checkAndReset(MinecraftServer server) {
        LocalDate lastReset = loadLastResetDate(server);
        LocalDate now = LocalDate.now();

        if (lastReset.getMonthValue() != now.getMonthValue()
                || lastReset.getYear() != now.getYear()) {
            performSeasonReset(server);
            saveLastResetDate(server, now);
        }
    }

    private static void performSeasonReset(MinecraftServer server) {
        List<PlayerData> all = JJKMod.getPlayerRepository().getAllPlayerData();
        int count = 0;
        for (PlayerData data : all) {
            data.seasonXp = 0;
            JJKMod.getPlayerRepository().saveAsync(data);
            count++;
        }
        server.getPlayerManager().broadcast(
            Text.literal("§6[JJK] 새 시즌이 시작되었습니다! 랭킹이 초기화되었습니다.§r"),
            false);
        LOGGER.info("[JJK] 시즌 초기화 완료. 대상 플레이어 수={}", count);
    }

    private static LocalDate loadLastResetDate(MinecraftServer server) {
        try {
            Path file = server.getSavePath(WorldSavePath.ROOT).resolve(SEASON_FILE);
            if (!Files.exists(file)) return LocalDate.of(2000, 1, 1);
            String content = Files.readString(file, StandardCharsets.UTF_8).trim();
            return LocalDate.parse(content);
        } catch (Exception e) {
            return LocalDate.of(2000, 1, 1);
        }
    }

    private static void saveLastResetDate(MinecraftServer server, LocalDate date) {
        try {
            Path file = server.getSavePath(WorldSavePath.ROOT).resolve(SEASON_FILE);
            Files.createDirectories(file.getParent());
            Files.writeString(file, date.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("[JJK] 시즌 날짜 저장 실패", e);
        }
    }

    private SeasonManager() {}
}
