package com.jjk.client.screen;

import com.jjk.network.c2s.CharacterSelectC2SPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class CharacterSelectScreen extends Screen {

    public record CharacterCardData(String id, String displayName, String faction) {}

    private static final List<CharacterCardData> CHARACTERS = List.of(
        new CharacterCardData("gojo",     "고죠 사토루",    "JUJUTSU_SORCERER"),
        new CharacterCardData("itadori",  "이타도리 유지",  "JUJUTSU_SORCERER"),
        new CharacterCardData("megumi",   "후시구로 메구미", "JUJUTSU_SORCERER"),
        new CharacterCardData("okkotsu",  "옷코츠 유타",   "JUJUTSU_SORCERER"),
        new CharacterCardData("nanami",   "나나미 켄토",   "JUJUTSU_SORCERER"),
        new CharacterCardData("higuruma", "히구루마 히로미", "JUJUTSU_SORCERER"),
        new CharacterCardData("hakari",   "하카리 킨지",   "JUJUTSU_SORCERER"),
        new CharacterCardData("inumaki",  "이누마키 토게", "JUJUTSU_SORCERER"),
        new CharacterCardData("mahito",   "마히토",         "CURSED_SPIRIT"),
        new CharacterCardData("jogo",     "죠고",           "CURSED_SPIRIT"),
        new CharacterCardData("sukuna",   "료멘 스쿠나",   "CURSED_SPIRIT")
    );

    private static final int CARD_W        = 80;
    private static final int CARD_H        = 120;
    private static final int CARD_GAP      = 10;
    private static final int TEX_SIZE      = 60;
    private static final int BTN_W         = 60;
    private static final int BTN_H         = 20;
    private static final int SCROLL_MARGIN = 20;
    private static final int BADGE_H       = 10;

    private static final int COLOR_OVERLAY  = 0xCC000000;
    private static final int COLOR_SORCERER = 0xFF3366CC;
    private static final int COLOR_CURSED   = 0xFFCC3333;
    private static final int COLOR_CARD_BG  = 0xFF222233;
    private static final int COLOR_CARD_HL  = 0xFF445566;
    private static final int COLOR_CARD_BRD = 0xFFAABBCC;
    private static final int COLOR_BTN_BG   = 0xFF444466;
    private static final int COLOR_BTN_HOV  = 0xFF556688;
    private static final int COLOR_TEX_FALL = 0xFF334455;

    private final List<CharacterCardData> cards;
    private final String currentCharacterId;

    private int scrollOffset = 0;
    private String pendingSelect = null;

    private ButtonWidget confirmBtn;
    private ButtonWidget cancelBtn;

    public CharacterSelectScreen() {
        super(Text.literal("캐릭터 선택"));
        this.currentCharacterId = null;
        this.cards = new ArrayList<>(CHARACTERS);
    }

    public CharacterSelectScreen(List<String> availableCharacters, String currentCharacterId) {
        super(Text.literal("캐릭터 선택"));
        this.currentCharacterId = currentCharacterId;
        this.cards = new ArrayList<>();
        for (CharacterCardData card : CHARACTERS) {
            if (availableCharacters.contains(card.id())) cards.add(card);
        }
        if (cards.isEmpty()) cards.addAll(CHARACTERS);
    }

    public CharacterSelectScreen(List<String> availableCharacters) {
        this(availableCharacters, null);
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;
        confirmBtn = ButtonWidget.builder(Text.literal("확인"), b -> confirmSelect())
            .dimensions(cx - 82, cy + 22, 80, 20).build();
        cancelBtn  = ButtonWidget.builder(Text.literal("취소"), b -> cancelSelect())
            .dimensions(cx + 2,  cy + 22, 80, 20).build();
        confirmBtn.visible = false;
        cancelBtn.visible  = false;
        addDrawableChild(confirmBtn);
        addDrawableChild(cancelBtn);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, COLOR_OVERLAY);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§l캐릭터 선택"), width / 2, 20, 0xFFFFFF);

        int cardsY  = (height - CARD_H) / 2;
        int startX  = cardsStartX();

        for (int i = 0; i < cards.size(); i++) {
            CharacterCardData card = cards.get(i);
            int cardX = startX + i * (CARD_W + CARD_GAP) + scrollOffset;
            if (cardX + CARD_W < 0 || cardX > width) continue;
            drawCard(ctx, card, cardX, cardsY,
                card.id().equals(currentCharacterId), mouseX, mouseY);
        }

        if (pendingSelect != null) {
            renderConfirmPopup(ctx);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawCard(DrawContext ctx, CharacterCardData card,
                          int x, int y, boolean isCurrent, int mx, int my) {
        // 카드 배경 + 테두리
        ctx.fill(x, y, x + CARD_W, y + CARD_H, isCurrent ? COLOR_CARD_HL : COLOR_CARD_BG);
        ctx.fill(x, y,            x + CARD_W, y + 1,            COLOR_CARD_BRD);
        ctx.fill(x, y + CARD_H - 1, x + CARD_W, y + CARD_H,   COLOR_CARD_BRD);
        ctx.fill(x, y,            x + 1,       y + CARD_H,     COLOR_CARD_BRD);
        ctx.fill(x + CARD_W - 1, y, x + CARD_W, y + CARD_H,   COLOR_CARD_BRD);

        // 텍스처 영역 (60×60) — 누락 시 폴백 색상이 먼저 그려짐
        int texX = x + (CARD_W - TEX_SIZE) / 2;
        int texY = y + 4;
        ctx.fill(texX, texY, texX + TEX_SIZE, texY + TEX_SIZE, COLOR_TEX_FALL);
        Identifier tex = Identifier.of("jjk", "textures/character/" + card.id() + ".png");
        ctx.drawTexture(tex, texX, texY, 0, 0, TEX_SIZE, TEX_SIZE, TEX_SIZE, TEX_SIZE);

        // 진영 뱃지
        int badgeY    = texY + TEX_SIZE + 2;
        int badgeColor = "JUJUTSU_SORCERER".equals(card.faction()) ? COLOR_SORCERER : COLOR_CURSED;
        String badgeLabel = "JUJUTSU_SORCERER".equals(card.faction()) ? "주술사" : "주령";
        ctx.fill(x + 2, badgeY, x + CARD_W - 2, badgeY + BADGE_H, badgeColor);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(badgeLabel),
            x + CARD_W / 2, badgeY + 1, 0xFFFFFF);

        // 캐릭터 이름
        String name = isCurrent ? "★ " + card.displayName() : card.displayName();
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(name),
            x + CARD_W / 2, badgeY + BADGE_H + 3, 0xFFFFFF);

        // 선택 버튼 (수동 렌더)
        int btnX = x + (CARD_W - BTN_W) / 2;
        int btnY = y + CARD_H - BTN_H - 4;
        boolean hover = !isCurrent && pendingSelect == null
            && mx >= btnX && mx <= btnX + BTN_W && my >= btnY && my <= btnY + BTN_H;
        ctx.fill(btnX, btnY, btnX + BTN_W, btnY + BTN_H,
            hover ? COLOR_BTN_HOV : COLOR_BTN_BG);
        ctx.fill(btnX, btnY,           btnX + BTN_W, btnY + 1,        COLOR_CARD_BRD);
        ctx.fill(btnX, btnY + BTN_H - 1, btnX + BTN_W, btnY + BTN_H, COLOR_CARD_BRD);
        ctx.fill(btnX, btnY,           btnX + 1,     btnY + BTN_H,   COLOR_CARD_BRD);
        ctx.fill(btnX + BTN_W - 1, btnY, btnX + BTN_W, btnY + BTN_H, COLOR_CARD_BRD);
        String btnLabel = isCurrent ? "§7현재 선택" : "선택";
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(btnLabel),
            btnX + BTN_W / 2, btnY + (BTN_H - 8) / 2, isCurrent ? 0xAAAAAA : 0xFFFFFF);
    }

    private void renderConfirmPopup(DrawContext ctx) {
        int popW = 260;
        int popH = 90;
        int popX = (width - popW) / 2;
        int popY = (height - popH) / 2;
        ctx.fill(popX, popY, popX + popW, popY + popH, 0xEE111122);
        ctx.fill(popX, popY,            popX + popW, popY + 1,       COLOR_CARD_BRD);
        ctx.fill(popX, popY + popH - 1, popX + popW, popY + popH,   COLOR_CARD_BRD);
        ctx.fill(popX, popY,            popX + 1,    popY + popH,    COLOR_CARD_BRD);
        ctx.fill(popX + popW - 1, popY, popX + popW, popY + popH,   COLOR_CARD_BRD);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§l캐릭터 선택 확인"),
            width / 2, popY + 10, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§7이 캐릭터는 특정 NPC 또는 아이템으로만"),
            width / 2, popY + 28, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("§7변경 가능합니다"),
            width / 2, popY + 40, 0xFFFFFF);
    }

    private int cardsStartX() {
        int totalW   = cards.size() * (CARD_W + CARD_GAP) - CARD_GAP;
        int visibleW = width - SCROLL_MARGIN * 2;
        return totalW <= visibleW ? (width - totalW) / 2 : SCROLL_MARGIN;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0 || pendingSelect != null) return false;

        int cardsY = (height - CARD_H) / 2;
        int startX = cardsStartX();

        for (int i = 0; i < cards.size(); i++) {
            CharacterCardData card = cards.get(i);
            int cardX = startX + i * (CARD_W + CARD_GAP) + scrollOffset;
            if (cardX + CARD_W < 0 || cardX > width) continue;

            int btnX = cardX + (CARD_W - BTN_W) / 2;
            int btnY = cardsY + CARD_H - BTN_H - 4;
            if (mx >= btnX && mx <= btnX + BTN_W && my >= btnY && my <= btnY + BTN_H) {
                if (!card.id().equals(currentCharacterId)) {
                    openConfirmation(card.id());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        int totalW   = cards.size() * (CARD_W + CARD_GAP) - CARD_GAP;
        int visibleW = width - SCROLL_MARGIN * 2;
        if (totalW > visibleW) {
            scrollOffset += (int) (vAmount * 20);
            int maxScroll = totalW - visibleW;
            scrollOffset = Math.max(-maxScroll, Math.min(0, scrollOffset));
        }
        return true;
    }

    private void openConfirmation(String charId) {
        pendingSelect = charId;
        confirmBtn.visible = true;
        cancelBtn.visible  = true;
    }

    private void confirmSelect() {
        if (pendingSelect != null) {
            ClientPlayNetworking.send(new CharacterSelectC2SPacket(pendingSelect));
        }
        close();
    }

    private void cancelSelect() {
        pendingSelect = null;
        confirmBtn.visible = false;
        cancelBtn.visible  = false;
    }

    @Override
    public boolean shouldPause() { return false; }
}
