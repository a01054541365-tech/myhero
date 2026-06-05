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

import java.util.List;

@Environment(EnvType.CLIENT)
public class ZeninShopScreen extends Screen {

    private static final Gson GSON = new Gson();

    private record ShopItem(String id, String name, int price, int gradeReq) {}

    private static final List<ShopItem> ITEMS = List.of(
        new ShopItem("cursed_dagger",    "저주 단검",  300, 0),
        new ShopItem("potion_ce",        "CE 회복제",  150, 0),
        new ShopItem("potion_hp",        "HP 치료제",  100, 0),
        new ShopItem("thousand_spear",   "천호창",    1200, 3),
        new ShopItem("playful_cloud",    "유운",      3500, 4),
        new ShopItem("inverted_spear",   "천역모",    8000, 5),
        new ShopItem("split_soul_blade", "석혼도",    6000, 5)
    );

    private final long cursedStones;
    private final int playerGradeRank;

    public ZeninShopScreen(String payload) {
        super(Text.literal("젠인 창고지기"));
        JsonObject json = parsePayload(payload);
        this.cursedStones    = json.has("stones")    ? json.get("stones").getAsLong()    : 0L;
        this.playerGradeRank = json.has("gradeRank") ? json.get("gradeRank").getAsInt()  : 0;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        for (int i = 0; i < ITEMS.size(); i++) {
            final ShopItem item = ITEMS.get(i);
            boolean enabled = playerGradeRank >= item.gradeReq() && cursedStones >= item.price();
            ButtonWidget btn = ButtonWidget.builder(
                Text.literal(item.name() + "  " + item.price() + "석"),
                b -> sendBuy(item.id())
            ).dimensions(cx - 100, cy - 60 + i * 22, 200, 20).build();
            btn.active = enabled;
            addDrawableChild(btn);
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("닫기"), b -> close())
            .dimensions(cx - 40, cy + 100, 80, 20).build());
    }

    private void sendBuy(String itemId) {
        ClientPlayNetworking.send(new NpcServiceC2SPacket("zenin_storage", "buy", itemId));
        close();
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int cx = width / 2, cy = height / 2;
        drawPanel(ctx, cx - 110, cy - 75, 220, 215);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("⚔ 젠인 창고지기"), cx, cy - 70, 0xFFD700);
        ctx.drawText(textRenderer,
            Text.literal("보유 주력석: " + cursedStones + "석"),
            cx - 100, cy - 55, 0xFFFF55, true);
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
