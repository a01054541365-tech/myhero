package com.jjk.discord;

import com.jjk.JJKMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class DiscordWebhook {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-discord");
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2))
        .build();

    /** discordWebhookUrl이 비어있으면 무시. */
    public static void send(String content) {
        if (JJKMod.getInstance() == null) return;
        String url = JJKMod.getConfig().discordWebhookUrl;
        if (url == null || url.isBlank()) return;

        String safe = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        String json = "{\"content\":\"" + safe + "\"}";
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HTTP.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                .exceptionally(ex -> { LOGGER.warn("[JJK] Discord 전송 실패: {}", ex.getMessage()); return null; });
        } catch (Exception e) {
            LOGGER.warn("[JJK] Discord 요청 생성 실패: {}", e.getMessage());
        }
    }

    /** TPSGuard 등 성능 경보용 — 2초 타임아웃, 실패해도 서버 틱을 차단하지 않는다. */
    public static void sendAsync(String content) {
        if (JJKMod.getInstance() == null) return;
        String url = JJKMod.getConfig().discordWebhookUrl;
        if (url == null || url.isBlank()) return;

        String safe = content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        String json = "{\"content\":\"" + safe + "\"}";
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(2))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            HTTP.sendAsync(req, HttpResponse.BodyHandlers.discarding())
                .exceptionally(ex -> { LOGGER.warn("[JJK] Discord 알림 전송 실패: {}", ex.getMessage()); return null; });
        } catch (Exception e) {
            LOGGER.warn("[JJK] Discord 알림 요청 생성 실패: {}", e.getMessage());
        }
    }

    private DiscordWebhook() {}
}
