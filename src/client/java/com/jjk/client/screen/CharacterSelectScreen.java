package com.jjk.client.screen;

import com.jjk.network.c2s.CharacterSelectC2SPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class CharacterSelectScreen extends Screen {

    private static final Map<String, String> CHARACTER_NAMES;
    static {
        CHARACTER_NAMES = new HashMap<>();
        CHARACTER_NAMES.put("gojo",     "고죠 사토루");
        CHARACTER_NAMES.put("itadori",  "이타도리 유지");
        CHARACTER_NAMES.put("megumi",   "후시구로 메구미");
        CHARACTER_NAMES.put("okkotsu",  "옷코츠 유타");
        CHARACTER_NAMES.put("mahito",   "마히토");
        CHARACTER_NAMES.put("jogo",     "죠고");
        CHARACTER_NAMES.put("hakari",   "하카리 킨지");
        CHARACTER_NAMES.put("inumaki",  "이누마키 토게");
        CHARACTER_NAMES.put("nanami",   "나나미 켄토");
        CHARACTER_NAMES.put("higuruma", "히구루마 히로미");
        CHARACTER_NAMES.put("sukuna",   "료멘 스쿠나");
        CHARACTER_NAMES.put("choso",    "초소");
    }

    private final List<String> availableCharacters;
    private final String currentCharacterId; // null = 미선택

    public CharacterSelectScreen(List<String> availableCharacters, String currentCharacterId) {
        super(Text.literal("캐릭터 선택"));
        this.availableCharacters = availableCharacters;
        this.currentCharacterId = currentCharacterId;
    }

    // CharacterSelectS2CPacket 핸들러 등 기존 호출 지점 호환
    public CharacterSelectScreen(List<String> availableCharacters) {
        this(availableCharacters, null);
    }

    @Override
    protected void init() {
        int cols = 4;
        int btnW = 80, btnH = 40, gap = 10;
        int rows = (int) Math.ceil(availableCharacters.size() / (double) cols);
        int startX = (width - cols * (btnW + gap)) / 2;
        int startY = (height - rows * (btnH + gap)) / 2;

        for (int i = 0; i < availableCharacters.size(); i++) {
            String charId = availableCharacters.get(i);
            int col = i % cols;
            int row = i / cols;
            int x = startX + col * (btnW + gap);
            int y = startY + row * (btnH + gap);
            String label = CHARACTER_NAMES.getOrDefault(charId, charId);
            if (charId.equals(currentCharacterId)) {
                label = "§e★ " + label; // 현재 선택 캐릭터 강조
            }
            addDrawableChild(ButtonWidget.builder(
                Text.literal(label),
                btn -> selectCharacter(charId)
            ).dimensions(x, y, btnW, btnH).build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(
            textRenderer,
            Text.literal("§l캐릭터를 선택하세요"),
            width / 2, height / 2 - 80,
            0xFFFFFF
        );
        super.render(context, mouseX, mouseY, delta);
    }

    private void selectCharacter(String charId) {
        if (charId.equals(currentCharacterId)) {
            // 재선택 경고 — 서버에 전송하지 않음
            if (client != null && client.player != null) {
                client.player.sendMessage(
                    Text.literal("§6[JJK] 이미 선택된 캐릭터입니다. Esc로 닫거나 다른 캐릭터를 선택하세요."), true);
            }
            return;
        }
        ClientPlayNetworking.send(new CharacterSelectC2SPacket(charId));
        close();
    }

    @Override
    public boolean shouldPause() { return false; }
}
