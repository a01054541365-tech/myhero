package com.jjk.client.hud;

import com.jjk.client.JjkClientState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class CEBarRenderer {

    // CE 바는 보라색 (#9B59FF) 단일 계열. 경고/위기 구간만 노랑/빨강으로 강조.
    private static final int CE_COLOR_HIGH   = 0xFF9B59FF; // CE ≥ 70% — 보라색
    private static final int CE_COLOR_MID    = 0xFF9B59FF; // CE 40–69% — 보라색
    private static final int CE_COLOR_WARN   = 0xFFE5C525; // CE 20–39% — 노란색
    private static final int CE_COLOR_LOW    = 0xFFFF2222; // CE < 20% — 빨간색
    private static final int CE_COLOR_BG     = 0xFF333333;
    private static final int VIGNETTE_COLOR  = 0x66FF0000; // semi-transparent red
    private static final int VIGNETTE_SIZE   = 20;

    private boolean ceWarningBlink = false;
    private int blinkTimer = 0;

    // CE 바 위치: 바닐라 체력바(screenH - 49) 기준 12px 위 = screenH - 61
    private static final int BAR_WIDTH  = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_OFFSET_Y = 61; // screenH - 61

    public void tick() {
        blinkTimer++;
        if (blinkTimer >= 10) {
            blinkTimer = 0;
            ceWarningBlink = !ceWarningBlink;
        }
    }

    public void render(DrawContext context, MinecraftClient client) {
        if (client.player == null) return;

        float ce    = JjkClientState.getCeCurrent();
        float ceMax = JjkClientState.getCeMax();
        if (ceMax <= 0f) return;

        float ratio = Math.max(0f, Math.min(1f, ce / ceMax));
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        int barX = screenW / 2 - BAR_WIDTH / 2;
        int barY = screenH - BAR_OFFSET_Y;

        // 배경
        context.fill(barX - 1, barY - 1, barX + BAR_WIDTH + 1, barY + BAR_HEIGHT + 1, 0xFF111111);
        context.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, CE_COLOR_BG);

        // CE 바 (색상 분기 — 보스바와 동일 구간)
        int barColor;
        if (ratio >= 0.70f) {
            barColor = CE_COLOR_HIGH;
        } else if (ratio >= 0.40f) {
            barColor = CE_COLOR_MID;
        } else if (ratio >= 0.20f) {
            barColor = CE_COLOR_WARN;
        } else {
            barColor = CE_COLOR_LOW;
        }
        // 30% 이하 경고: 점멸로 강조
        if (ratio < 0.30f && ceWarningBlink && ce > 0f) {
            barColor = CE_COLOR_BG;
        }

        int filled = (int)(BAR_WIDTH * ratio);
        if (filled > 0) {
            context.fill(barX, barY, barX + filled, barY + BAR_HEIGHT, barColor);
        }

        // CE 수치 텍스트 — 바 오른쪽
        String ceText = (int) ce + " / " + (int) ceMax;
        context.drawTextWithShadow(client.textRenderer,
                Text.literal(ceText),
                barX + BAR_WIDTH + 4, barY - 1,
                0xFFFFFFFF);

        // 등급 아이콘 — 바 왼쪽
        String grade = JjkClientState.getGrade();
        if (grade != null) {
            int gradeIdx = gradeToIndex(grade);
            Identifier iconId = Identifier.of("jjk", "textures/hud/grade_" + gradeIdx + ".png");
            context.drawTexture(iconId, barX - 20, barY - 6, 0, 0, 16, 16, 16, 16);
        }

        // CE = 0 vignette
        if (ce <= 0f) {
            renderVignette(context, screenW, screenH);
        }
    }

    private void renderVignette(DrawContext context, int screenW, int screenH) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        context.fill(0,               0,        VIGNETTE_SIZE, screenH, VIGNETTE_COLOR); // left
        context.fill(screenW - VIGNETTE_SIZE, 0, screenW,      screenH, VIGNETTE_COLOR); // right
        context.fill(0,               0,        screenW,       VIGNETTE_SIZE, VIGNETTE_COLOR); // top
        context.fill(0, screenH - VIGNETTE_SIZE, screenW,      screenH, VIGNETTE_COLOR); // bottom
        RenderSystem.disableBlend();
    }

    private static int gradeToIndex(String grade) {
        return switch (grade) {
            case "grade_3"      -> 1;
            case "grade_2"      -> 2;
            case "grade_1"      -> 3;
            case "semi_grade_1" -> 4;
            case "special_grade"-> 5;
            default             -> 0;
        };
    }
}
