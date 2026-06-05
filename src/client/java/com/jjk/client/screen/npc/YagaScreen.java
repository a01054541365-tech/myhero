package com.jjk.client.screen.npc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jjk.network.c2s.NpcServiceC2SPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class YagaScreen extends Screen {

    private static final Gson GSON = new Gson();
    private static final long[] ENHANCE_COSTS = {0, 1000L, 2500L, 5000L};

    private final long stones;
    private final String toolId;
    private final int enhLevel;

    public YagaScreen(String payload) {
        super(Text.literal("야가 마사모토"));
        JsonObject j = parsePayload(payload);
        this.stones   = j.has("stones")   ? j.get("stones").getAsLong()    : 0L;
        this.toolId   = j.has("toolId")   ? j.get("toolId").getAsString()  : "";
        this.enhLevel = j.has("enhLevel") ? j.get("enhLevel").getAsInt()   : 0;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;

        long nextCost = enhLevel < 3 ? ENHANCE_COSTS[enhLevel + 1] : 0L;
        String enhLabel = enhLevel >= 3
            ? "최대 강화 완료"
            : "주구 강화  " + nextCost + "석  (" + enhLevel + " → " + (enhLevel + 1) + ")";
        ButtonWidget enhBtn = ButtonWidget.builder(
            Text.literal(enhLabel),
            b -> { ClientPlayNetworking.send(new NpcServiceC2SPacket("yaga","enhance", toolId)); close(); }
        ).dimensions(cx - 100, cy, 200, 20).build();
        enhBtn.active = enhLevel < 3 && !toolId.isEmpty() && stones >= nextCost;
        addDrawableChild(enhBtn);

        ButtonWidget craftBtn = ButtonWidget.builder(
            Text.literal("CE 포션 제작  500석"),
            b -> { ClientPlayNetworking.send(new NpcServiceC2SPacket("yaga","craft_ce_potion",null)); close(); }
        ).dimensions(cx - 100, cy + 25, 200, 20).build();
        craftBtn.active = stones >= 500L;
        addDrawableChild(craftBtn);

        addDrawableChild(ButtonWidget.builder(Text.literal("닫기"), b -> close())
            .dimensions(cx - 40, cy + 60, 80, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int cx = width / 2, cy = height / 2;
        drawPanel(ctx, cx - 110, cy - 40, 220, 120);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("야가 마사모토"), cx, cy - 35, 0xFFD700);
        ctx.drawText(textRenderer, Text.literal("보유 주력석: " + stones + "석"),
            cx - 100, cy - 22, 0xFFFF55, true);

        String toolDisplay = toolId.isEmpty() ? "(주구 없음)" : toolId + "  강화 " + enhLevel + "단계";
        ctx.drawText(textRenderer, Text.literal("장착 주구: " + toolDisplay),
            cx - 100, cy - 10, 0xAAAAAA, true);

        // 강화 단계 표시 (별 3개)
        for (int i = 0; i < 3; i++) {
            int color = i < enhLevel ? 0xFFFFD700 : 0xFF444444;
            ctx.fill(cx - 20 + i * 15, cy - 7, cx - 7 + i * 15, cy - 0, color);
        }

        super.render(ctx, mx, my, delta);
    }

    private static void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, 0xCC1A1A1A);
        ctx.fill(x, y, x + w, y + 1, 0xFFFFD700);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFFFFD700);
        ctx.fill(x, y, x + 1, y + h, 0xFFFFD700);
        ctx.fill(x + w - 1, y, x + w, y + h, 0xFFFFD700);
    }

    private static JsonObject parsePayload(String payload) {
        try { return GSON.fromJson(payload, JsonObject.class); }
        catch (Exception e) { return new JsonObject(); }
    }
}
