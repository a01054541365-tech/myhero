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
public class NahovinoScreen extends Screen {

    private static final Gson GSON = new Gson();

    private final long stones;
    private final int gradeRank;
    private final int atkBonus;
    private final int defBonus;
    private final int spdBonus;
    private final int baseAtk;
    private final int baseDef;

    public NahovinoScreen(String payload) {
        super(Text.literal("나호비노 아오이"));
        JsonObject j = parsePayload(payload);
        this.stones   = j.has("stones")   ? j.get("stones").getAsLong()   : 0L;
        this.gradeRank= j.has("gradeRank")? j.get("gradeRank").getAsInt() : 0;
        this.atkBonus = j.has("atkBonus") ? j.get("atkBonus").getAsInt()  : 0;
        this.defBonus = j.has("defBonus") ? j.get("defBonus").getAsInt()  : 0;
        this.spdBonus = j.has("spdBonus") ? j.get("spdBonus").getAsInt()  : 0;
        this.baseAtk  = j.has("baseAtk")  ? j.get("baseAtk").getAsInt()   : 10;
        this.baseDef  = j.has("baseDef")  ? j.get("baseDef").getAsInt()   : 10;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        int cx = width / 2, cy = height / 2;

        ButtonWidget atkBtn = ButtonWidget.builder(
            Text.literal("공격 특훈  800석  (" + atkBonus + "/5)"),
            b -> { ClientPlayNetworking.send(new NpcServiceC2SPacket("nahobino","train_atk",null)); close(); }
        ).dimensions(cx - 100, cy - 5, 200, 18).build();
        atkBtn.active = gradeRank >= 3 && atkBonus < 5 && stones >= 800L;
        addDrawableChild(atkBtn);

        ButtonWidget defBtn = ButtonWidget.builder(
            Text.literal("방어 특훈  800석  (" + defBonus + "/5)"),
            b -> { ClientPlayNetworking.send(new NpcServiceC2SPacket("nahobino","train_def",null)); close(); }
        ).dimensions(cx - 100, cy + 16, 200, 18).build();
        defBtn.active = gradeRank >= 3 && defBonus < 5 && stones >= 800L;
        addDrawableChild(defBtn);

        ButtonWidget spdBtn = ButtonWidget.builder(
            Text.literal("속도 특훈  600석  (" + spdBonus + "/5)"),
            b -> { ClientPlayNetworking.send(new NpcServiceC2SPacket("nahobino","train_spd",null)); close(); }
        ).dimensions(cx - 100, cy + 37, 200, 18).build();
        spdBtn.active = gradeRank >= 2 && spdBonus < 5 && stones >= 600L;
        addDrawableChild(spdBtn);

        ButtonWidget resetBtn = ButtonWidget.builder(
            Text.literal("스탯 초기화  3000석"),
            b -> { ClientPlayNetworking.send(new NpcServiceC2SPacket("nahobino","reset_stats",null)); close(); }
        ).dimensions(cx - 100, cy + 58, 200, 18).build();
        resetBtn.active = gradeRank >= 5 && stones >= 3000L;
        addDrawableChild(resetBtn);

        addDrawableChild(ButtonWidget.builder(Text.literal("닫기"), b -> close())
            .dimensions(cx - 40, cy + 90, 80, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int cx = width / 2, cy = height / 2;
        drawPanel(ctx, cx - 110, cy - 55, 220, 165);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("나호비노 아오이"), cx, cy - 50, 0xFFD700);
        ctx.drawText(textRenderer, Text.literal("보유 주력석: " + stones + "석"),
            cx - 100, cy - 37, 0xFFFF55, true);
        ctx.drawText(textRenderer,
            Text.literal("공격: " + baseAtk + "(+" + atkBonus * 2 + ")  "
                       + "방어: " + baseDef + "(+" + defBonus * 2 + ")  "
                       + "속도: +" + spdBonus),
            cx - 100, cy - 22, 0xAAAAAA, true);
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
