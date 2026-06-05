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
public class KusakabeScreen extends Screen {

    private static final Gson GSON = new Gson();

    private final long stones;
    private final int mastery;
    private final int resetCount;

    public KusakabeScreen(String payload) {
        super(Text.literal("쿠사카베 아츠야"));
        JsonObject j = parsePayload(payload);
        this.stones     = j.has("stones")     ? j.get("stones").getAsLong()     : 0L;
        this.mastery    = j.has("mastery")    ? j.get("mastery").getAsInt()     : 0;
        this.resetCount = j.has("resetCount") ? j.get("resetCount").getAsInt()  : 0;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;

        long trainCost = mastery <= 3 ? 500L : mastery <= 6 ? 1200L : 2500L;
        long resetCost = resetCount == 0 ? 0L : resetCount == 1 ? 1500L : 5000L;

        ButtonWidget trainBtn = ButtonWidget.builder(
            Text.literal("숙련도 전수  " + trainCost + "석"),
            b -> { ClientPlayNetworking.send(new NpcServiceC2SPacket("kusakabe","train",null)); close(); }
        ).dimensions(cx - 100, cy, 200, 20).build();
        trainBtn.active = mastery < 10 && stones >= trainCost;
        addDrawableChild(trainBtn);

        String resetLabel = resetCost == 0 ? "숙련도 초기화  무료" : "숙련도 초기화  " + resetCost + "석";
        ButtonWidget resetBtn = ButtonWidget.builder(
            Text.literal(resetLabel),
            b -> { ClientPlayNetworking.send(new NpcServiceC2SPacket("kusakabe","reset",null)); close(); }
        ).dimensions(cx - 100, cy + 25, 200, 20).build();
        resetBtn.active = stones >= resetCost;
        addDrawableChild(resetBtn);

        addDrawableChild(ButtonWidget.builder(Text.literal("닫기"), b -> close())
            .dimensions(cx - 40, cy + 60, 80, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int cx = width / 2, cy = height / 2;
        drawPanel(ctx, cx - 110, cy - 50, 220, 130);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("쿠사카베 아츠야"), cx, cy - 45, 0xFFD700);
        ctx.drawText(textRenderer, Text.literal("보유 주력석: " + stones + "석"),
            cx - 100, cy - 32, 0xFFFF55, true);
        ctx.drawText(textRenderer,
            Text.literal("숙련도: " + mastery + " / 10   초기화 " + resetCount + "회"),
            cx - 100, cy - 20, 0xAAAAAA, true);

        // 숙련도 바
        int barW = 160;
        int filled = (int)(barW * mastery / 10.0);
        ctx.fill(cx - 80, cy - 8, cx - 80 + barW, cy - 2, 0xFF333333);
        ctx.fill(cx - 80, cy - 8, cx - 80 + filled, cy - 2, 0xFF55FFFF);

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
