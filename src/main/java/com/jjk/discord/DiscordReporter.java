package com.jjk.discord;

import com.jjk.JJKMod;
import com.jjk.data.PlayerData;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class DiscordReporter {

    public static final int INTERVAL_TICKS = 36000;

    public static void sendPeriodicReport(MinecraftServer server) {
        int playerCount = server.getPlayerManager().getCurrentPlayerCount();
        float mspt = server.getAverageTickTime();
        double tps = mspt > 0 ? Math.min(20.0, 1000.0 / mspt) : 20.0;

        List<PlayerData> all = JJKMod.getPlayerRepository().getAllPlayerData();
        Map<String, Long> charCounts = all.stream()
            .filter(d -> d.characterId != null)
            .collect(Collectors.groupingBy(d -> d.characterId, Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        sb.append("**JJK 서버 현황** (30분 주기)\n");
        sb.append("접속자: ").append(playerCount).append("명\n");
        sb.append(String.format("TPS: %.1f\n", tps));
        if (!charCounts.isEmpty()) {
            sb.append("캐릭터 픽률:\n");
            charCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> sb.append("  ").append(e.getKey())
                    .append(": ").append(e.getValue()).append("명\n"));
        }

        DiscordWebhook.send(sb.toString());
    }

    private DiscordReporter() {}
}
