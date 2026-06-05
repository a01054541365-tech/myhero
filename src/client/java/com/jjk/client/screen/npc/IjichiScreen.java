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
public class IjichiScreen extends Screen {

    private static final Gson GSON = new Gson();

    private final long stones;
    private final String questDesc;
    private final int questTarget;
    private final int questProg;
    private final boolean questDone;

    public IjichiScreen(String payload) {
        super(Text.literal("이치지 키요타카"));
        JsonObject j = parsePayload(payload);
        this.stones     = j.has("stones")     ? j.get("stones").getAsLong()       : 0L;
        this.questDesc  = j.has("questDesc")  ? j.get("questDesc").getAsString()  : "없음";
        this.questTarget= j.has("questTarget")? j.get("questTarget").getAsInt()   : 1;
        this.questProg  = j.has("questProg")  ? j.get("questProg").getAsInt()     : 0;
        this.questDone  = j.has("questDone")  ? j.get("questDone").getAsBoolean() : false;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;

        ButtonWidget questBtn = ButtonWidget.builder(
            Text.literal(questDone ? "오늘 퀘스트 완료" : "퀘스트 수락"),
            b -> { ClientPlayNetworking.send(new NpcServiceC2SPacket("ijichi","get_quest",null)); close(); }
        ).dimensions(cx - 100, cy + 20, 200, 20).build();
        questBtn.active = !questDone;
        addDrawableChild(questBtn);

        String[] destinations = {"training", "infirmary", "storage", "entrance"};
        String[] destLabels   = {"훈련실", "의무실", "창고", "입구"};
        for (int i = 0; i < destinations.length; i++) {
            final String dest = destinations[i];
            ButtonWidget btn = ButtonWidget.builder(
                Text.literal(destLabels[i] + "  300석"),
                b -> { ClientPlayNetworking.send(new NpcServiceC2SPacket("ijichi","teleport",dest)); close(); }
            ).dimensions(cx - 100 + (i % 2) * 103, cy + 45 + (i / 2) * 24, 100, 20).build();
            btn.active = stones >= 300L;
            addDrawableChild(btn);
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("닫기"), b -> close())
            .dimensions(cx - 40, cy + 100, 80, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int cx = width / 2, cy = height / 2;
        drawPanel(ctx, cx - 110, cy - 50, 220, 165);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("이치지 키요타카"), cx, cy - 45, 0xFFD700);
        ctx.drawText(textRenderer, Text.literal("보유 주력석: " + stones + "석"),
            cx - 100, cy - 32, 0xFFFF55, true);
        ctx.drawText(textRenderer,
            Text.literal("퀘스트: " + questDesc),
            cx - 100, cy - 18, 0xFFFFFF, true);

        // 퀘스트 진행 바
        int barW = 160;
        float ratio = questTarget > 0 ? (float) questProg / questTarget : 0f;
        int filled = (int)(barW * Math.min(ratio, 1f));
        ctx.fill(cx - 80, cy - 5, cx - 80 + barW, cx + 1, 0xFF333333);
        ctx.fill(cx - 80, cy - 5, cx - 80 + filled, cx + 1, questDone ? 0xFF55FF55 : 0xFF5555FF);
        ctx.drawText(textRenderer,
            Text.literal(questProg + " / " + questTarget),
            cx + 85, cy - 4, 0xAAAAAA, false);

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
