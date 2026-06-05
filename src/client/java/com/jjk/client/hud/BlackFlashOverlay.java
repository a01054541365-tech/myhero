package com.jjk.client.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/**
 * 흑섬 히트/발동 시 화면 flash 오버레이.
 * HudRenderCallback 에 등록하여 매 프레임 render() 호출.
 */
@Environment(EnvType.CLIENT)
public final class BlackFlashOverlay {

    public static final BlackFlashOverlay INSTANCE = new BlackFlashOverlay();
    private BlackFlashOverlay() {}

    // 피격 flash (검은) — 5틱 지속 + 5틱 fadeOut
    private int hitBlackTick = 0;
    // 발동 flash (흰) — 5틱 지속 + 5틱 fadeOut
    private int hitWhiteTick = 0;

    private static final int DURATION  = 5;
    private static final int FADE_TICKS = 5;
    private static final int TOTAL     = DURATION + FADE_TICKS;

    public void triggerHitFlash()   { hitBlackTick = TOTAL; }
    public void triggerActivateFlash() { hitWhiteTick = TOTAL; }

    public void render(DrawContext ctx, RenderTickCounter tickCounter) {
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();

        if (hitBlackTick > 0) {
            int alpha = calcAlpha(hitBlackTick, 0xAA);
            ctx.fill(0, 0, sw, sh, (alpha << 24) | 0x000000);
            hitBlackTick--;
        }

        if (hitWhiteTick > 0) {
            int alpha = calcAlpha(hitWhiteTick, 0x44);
            ctx.fill(0, 0, sw, sh, (alpha << 24) | 0xFFFFFF);
            hitWhiteTick--;
        }
    }

    private static int calcAlpha(int remaining, int baseAlpha) {
        if (remaining > FADE_TICKS) return baseAlpha;
        // 선형 페이드아웃
        return (int) (baseAlpha * remaining / (float) FADE_TICKS);
    }
}
