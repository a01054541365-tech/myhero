package com.jjk.client.hud;

import com.jjk.client.JjkClientState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** 영역 전개 인디케이터 — 진입 메시지 페이드 + 충돌 경고 */
@Environment(EnvType.CLIENT)
public final class DomainIndicator {

    private static final int FADE_TICKS   = 60;  // 3초 후 페이드아웃 시작
    private static final int HOLD_TICKS   = 40;  // 2초 표시 후 페이드 시작

    private String  displayDomainName = null;
    private String  displayOwnerName  = null;
    private long    entryDisplayStart  = 0L;
    private float   fadeAlpha          = 0f;

    public void onEnter(String domainId, long worldTick) {
        displayDomainName = formatDomainName(domainId);
        displayOwnerName  = formatOwnerName(domainId);
        entryDisplayStart = worldTick;
        fadeAlpha = 1f;
    }

    public void onExit(long worldTick) {
        // 페이드아웃은 tick()에서 처리
    }

    public void tick(long worldTick) {
        if (displayDomainName == null) return;
        long elapsed = worldTick - entryDisplayStart;
        if (elapsed < HOLD_TICKS) {
            fadeAlpha = 1f;
        } else {
            float fadeProgress = (float)(elapsed - HOLD_TICKS) / FADE_TICKS;
            fadeAlpha = Math.max(0f, 1f - fadeProgress);
            if (fadeAlpha <= 0f) displayDomainName = null;
        }
    }

    private static final int DOMAIN_VIGNETTE_COLOR = 0x303B1E5E; // 옅은 보라
    private static final int DOMAIN_VIGNETTE_SIZE  = 12;

    public void render(DrawContext context, MinecraftClient client) {
        if (client.player == null) return;
        int screenW = client.getWindow().getScaledWidth();

        // 영역 내부에 있는 동안 화면 가장자리 미세 비네팅
        if (JjkClientState.isInDomain()) {
            int screenH = client.getWindow().getScaledHeight();
            int s = DOMAIN_VIGNETTE_SIZE;
            context.fill(0, 0, s, screenH, DOMAIN_VIGNETTE_COLOR);
            context.fill(screenW - s, 0, screenW, screenH, DOMAIN_VIGNETTE_COLOR);
            context.fill(s, 0, screenW - s, s, DOMAIN_VIGNETTE_COLOR);
            context.fill(s, screenH - s, screenW - s, screenH, DOMAIN_VIGNETTE_COLOR);
        }

        // 영역 진입 메시지 (상단 중앙, 페이드아웃) — 영역 이름 + 소유자 이름
        if (displayDomainName != null && fadeAlpha > 0f) {
            int alpha = (int)(fadeAlpha * 255) << 24;
            int textColor = (0x00FFFFFF & 0xFFFFFF) | alpha;
            context.drawCenteredTextWithShadow(client.textRenderer,
                    Text.literal("§b[영역] " + displayDomainName
                        + (displayOwnerName != null ? " §7— " + displayOwnerName : "")),
                    screenW / 2, 20,
                    textColor);
        }

        // 영역 충돌 경고 (좌상단)
        if (JjkClientState.getActiveDomainCount() >= 2) {
            int remaining = 0;
            if (JjkClientState.isInDomain() && client.world != null) {
                long entered = JjkClientState.getDomainEnteredTick();
                long elapsed = client.world.getTime() - entered;
                remaining = Math.max(0, (int)((200 - elapsed) / 20));
            }
            context.drawTextWithShadow(client.textRenderer,
                    Text.literal("§c[영역 충돌 중] 잔여: " + remaining + "초"),
                    4, 4,
                    0xFFFF4444);
        }
    }

    private static String formatDomainName(String domainId) {
        return switch (domainId) {
            case "gojo_unlimited_void"     -> "무량공처";
            case "sukuna_malevolent_shrine"-> "복마어주자";
            case "megumi_chimera_shadow"   -> "개관흉흉즉사";
            case "mahito_self_embodiment"  -> "자폐원돈과";
            case "hakari_idle_death_gamble"-> "잭팟";
            case "itadori_unnamed"         -> "확장 영역";
            default                        -> domainId;
        };
    }

    private static String formatOwnerName(String domainId) {
        return switch (domainId) {
            case "gojo_unlimited_void"     -> "고죠 사토루";
            case "sukuna_malevolent_shrine"-> "료멘 스쿠나";
            case "megumi_chimera_shadow"   -> "후시구로 메구미";
            case "mahito_self_embodiment"  -> "마히토";
            case "hakari_idle_death_gamble"-> "하카리 킨토키";
            case "itadori_unnamed"         -> "이타도리 유지";
            case "cursed_spirit_domain"    -> "주령";
            default                        -> null;
        };
    }
}
