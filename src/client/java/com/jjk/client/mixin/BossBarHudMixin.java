package com.jjk.client.mixin;

import com.jjk.client.JjkClientState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 바닐라 보스바 렌더 전에 JJK 캐릭터 상태 텍스트를 오버레이.
 * 색 결정은 서버(BossBarUpdateS2CPacket)가 담당 — 여기선 표시만.
 */
@Environment(EnvType.CLIENT)
@Mixin(BossBarHud.class)
public class BossBarHudMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void jjk$renderCharacterStatus(DrawContext context, CallbackInfo ci) {
        String charId = JjkClientState.getCharacterId();
        if (charId == null) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int sw = mc.getWindow().getScaledWidth();
        String statusText = String.format("[%s]  HP: %d / %d  CE: %d / %d",
            charId,
            JjkClientState.getHp(), JjkClientState.getHpMax(),
            (int) JjkClientState.getCeCurrent(), (int) JjkClientState.getCeMax());

        context.drawCenteredTextWithShadow(
            mc.textRenderer,
            Text.literal(statusText),
            sw / 2, 12,
            0xFFFFFFFF
        );
    }
}
