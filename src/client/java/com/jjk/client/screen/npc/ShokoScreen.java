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
public class ShokoScreen extends Screen {

    private static final Gson GSON = new Gson();

    private record ServiceDef(String action, String label, long cost) {}

    private static final ServiceDef[] SERVICES = {
        new ServiceDef("heal_half",    "HP 절반 회복  200석",  200L),
        new ServiceDef("heal_full",    "HP 완전 회복  600석",  600L),
        new ServiceDef("ce_fill",      "CE 완전 충전  300석",  300L),
        new ServiceDef("full_package", "풀 패키지     800석",  800L),
        new ServiceDef("status_clear", "상태이상 해제 400석",  400L)
    };

    private final long stones;

    public ShokoScreen(String payload) {
        super(Text.literal("이에이리 쇼코"));
        JsonObject j = parsePayload(payload);
        this.stones = j.has("stones") ? j.get("stones").getAsLong() : 0L;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;

        for (int i = 0; i < SERVICES.length; i++) {
            final ServiceDef svc = SERVICES[i];
            int col = i % 2, row = i / 2;
            ButtonWidget btn = ButtonWidget.builder(
                Text.literal(svc.label()),
                b -> { ClientPlayNetworking.send(new NpcServiceC2SPacket("shoko", svc.action(), null)); close(); }
            ).dimensions(cx - 105 + col * 108, cy - 20 + row * 24, 105, 20).build();
            btn.active = stones >= svc.cost();
            addDrawableChild(btn);
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("닫기"), b -> close())
            .dimensions(cx - 40, cy + 70, 80, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int cx = width / 2, cy = height / 2;
        drawPanel(ctx, cx - 115, cy - 40, 230, 125);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("이에이리 쇼코"), cx, cy - 35, 0xFFD700);
        ctx.drawText(textRenderer, Text.literal("보유 주력석: " + stones + "석"),
            cx - 105, cy - 22, 0xFFFF55, true);
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
