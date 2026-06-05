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
public class GojoShiyuScreen extends Screen {

    private static final Gson GSON = new Gson();

    private final long stones;
    private final long bounty;
    private final boolean isCursedSpirit;

    public GojoShiyuScreen(String payload) {
        super(Text.literal("공시우"));
        JsonObject j = parsePayload(payload);
        this.stones        = j.has("stones")        ? j.get("stones").getAsLong()        : 0L;
        this.bounty        = j.has("bounty")        ? j.get("bounty").getAsLong()        : 0L;
        this.isCursedSpirit= j.has("isCursedSpirit")? j.get("isCursedSpirit").getAsBoolean() : false;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;

        if (!isCursedSpirit) {
            addDrawableChild(ButtonWidget.builder(Text.literal("닫기"), b -> close())
                .dimensions(cx - 40, cy, 80, 20).build());
            return;
        }

        ButtonWidget settleBtn = ButtonWidget.builder(
            Text.literal("현상금 정산  +" + bounty + "석"),
            b -> { ClientPlayNetworking.send(new NpcServiceC2SPacket("gojo_shiyu","settle_bounty",null)); close(); }
        ).dimensions(cx - 100, cy - 20, 200, 20).build();
        settleBtn.active = bounty > 0;
        addDrawableChild(settleBtn);

        ButtonWidget trackerBtn = ButtonWidget.builder(
            Text.literal("손가락 추적기  2000석"),
            b -> { ClientPlayNetworking.send(new NpcServiceC2SPacket("gojo_shiyu","buy_tracker",null)); close(); }
        ).dimensions(cx - 100, cy + 5, 200, 20).build();
        trackerBtn.active = stones >= 2000L;
        addDrawableChild(trackerBtn);

        ButtonWidget buffBtn = ButtonWidget.builder(
            Text.literal("진영 버프  1500석"),
            b -> { ClientPlayNetworking.send(new NpcServiceC2SPacket("gojo_shiyu","buy_buff",null)); close(); }
        ).dimensions(cx - 100, cy + 30, 200, 20).build();
        buffBtn.active = stones >= 1500L;
        addDrawableChild(buffBtn);

        ButtonWidget infoBtn = ButtonWidget.builder(
            Text.literal("정보 입수  500석"),
            b -> { ClientPlayNetworking.send(new NpcServiceC2SPacket("gojo_shiyu","buy_info",null)); close(); }
        ).dimensions(cx - 100, cy + 55, 200, 20).build();
        infoBtn.active = stones >= 500L;
        addDrawableChild(infoBtn);

        addDrawableChild(ButtonWidget.builder(Text.literal("닫기"), b -> close())
            .dimensions(cx - 40, cy + 85, 80, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int cx = width / 2, cy = height / 2;
        drawPanel(ctx, cx - 110, cy - 40, 220, 150);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("공시우"), cx, cy - 35, 0xFFD700);
        if (!isCursedSpirit) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("주령 진영 전용 서비스"), cx, cy - 20, 0xFF5555);
        } else {
            ctx.drawText(textRenderer, Text.literal("보유 주력석: " + stones + "석"),
                cx - 100, cy - 22, 0xFFFF55, true);
            ctx.drawText(textRenderer, Text.literal("현상금: " + bounty + "석"),
                cx - 100, cy - 10, 0xAA00AA, true);
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
