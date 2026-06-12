package com.jjk.client.hud;

import com.jjk.client.JjkClientState;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

/** 스킬 쿨타임 HUD — 6슬롯(F/ShiftF/ShiftR/R/V/C) + 원형 오버레이 */
@Environment(EnvType.CLIENT)
public final class SkillCooldownHUD {

    private static final int SLOT_SIZE  = 18;
    private static final int SLOT_GAP   = 4;
    private static final int SLOT_COUNT = 6;
    private static final int TOTAL_W    = SLOT_COUNT * SLOT_SIZE + (SLOT_COUNT - 1) * SLOT_GAP;
    private static final int SLOT_OFFSET_Y = 84; // screenH - 84 (CE바 -62, 체력바 -55 위)

    private static final int COLOR_SLOT_BG      = 0xFF222222;
    private static final int COLOR_SLOT_BORDER   = 0xFF888888;
    private static final int COLOR_SLOT_CE_LACK  = 0xFFFF4444;
    private static final int COLOR_SLOT_READY    = 0xFFFFE066; // 쿨타임 완료 플래시
    private static final int READY_FLASH_TICKS   = 12;

    // 쿨타임 완료 순간 감지용 — keyId별 직전 프레임 쿨타임 여부 + 플래시 만료 틱
    private final boolean[] wasCoolingDown = new boolean[SLOT_COUNT];
    private final long[]    flashUntilTick = new long[SLOT_COUNT];

    public void render(DrawContext context, MinecraftClient client) {
        if (client.player == null) return;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        long worldTick = client.world != null ? client.world.getTime() : 0L;

        int startX = screenW / 2 - TOTAL_W / 2;
        int y = screenH - SLOT_OFFSET_Y;

        float ceCurrent = JjkClientState.getCeCurrent();
        boolean ceLow = ceCurrent < 50f;

        for (int i = 0; i < SLOT_COUNT; i++) {
            int x = startX + i * (SLOT_SIZE + SLOT_GAP);
            drawSlot(context, client, x, y, i, worldTick, ceLow);
        }
    }

    private void drawSlot(DrawContext context, MinecraftClient client,
                           int x, int y, int keyId, long worldTick, boolean ceLow) {
        // 배경
        context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, COLOR_SLOT_BG);

        // 쿨타임 완료 순간 감지 → 짧은 하이라이트 플래시
        boolean coolingNow = JjkClientState.getCooldownRatio(keyId, worldTick) > 0f;
        if (wasCoolingDown[keyId] && !coolingNow) {
            flashUntilTick[keyId] = worldTick + READY_FLASH_TICKS;
        }
        wasCoolingDown[keyId] = coolingNow;
        boolean readyFlash = worldTick < flashUntilTick[keyId] && (worldTick / 2) % 2 == 0;

        // 테두리
        int borderColor = readyFlash ? COLOR_SLOT_READY
                        : ceLow      ? COLOR_SLOT_CE_LACK : COLOR_SLOT_BORDER;
        context.fill(x,                    y,                     x + SLOT_SIZE, y + 1,          borderColor);
        context.fill(x,                    y + SLOT_SIZE - 1,     x + SLOT_SIZE, y + SLOT_SIZE,   borderColor);
        context.fill(x,                    y + 1,                 x + 1,         y + SLOT_SIZE - 1, borderColor);
        context.fill(x + SLOT_SIZE - 1,    y + 1,                 x + SLOT_SIZE, y + SLOT_SIZE - 1, borderColor);

        // 쿨타임 오버레이 (원호)
        float cdRatio = JjkClientState.getCooldownRatio(keyId, worldTick);
        if (cdRatio > 0f) {
            int cx = x + SLOT_SIZE / 2;
            int cy = y + SLOT_SIZE / 2;
            drawCooldownArc(context, cx, cy, SLOT_SIZE / 2 - 1, cdRatio);

            // 남은 시간 숫자
            int remainTicks = JjkClientState.getCooldownRemaining(keyId, worldTick);
            int remainSec   = (remainTicks + 19) / 20;
            if (remainSec > 0) {
                context.drawCenteredTextWithShadow(client.textRenderer,
                        Text.literal(String.valueOf(remainSec)),
                        cx, cy - 4,
                        0xFFFFFFFF);
            }
        }
    }

    private void drawCooldownArc(DrawContext context, int cx, int cy, int radius, float ratio) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        buf.vertex(matrix, cx, cy, 0f).color(0, 0, 0, 153);

        int totalSteps = 40;
        float startAngle = -MathHelper.HALF_PI;
        float sweepAngle = MathHelper.TAU * ratio;
        int steps = Math.max(1, (int)(totalSteps * ratio));

        for (int i = 0; i <= steps; i++) {
            float angle = startAngle + Math.min(i * (MathHelper.TAU / totalSteps), sweepAngle);
            buf.vertex(matrix,
                    cx + MathHelper.cos(angle) * radius,
                    cy + MathHelper.sin(angle) * radius,
                    0f).color(0, 0, 0, 153);
        }

        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.disableBlend();
        matrices.pop();
    }
}
