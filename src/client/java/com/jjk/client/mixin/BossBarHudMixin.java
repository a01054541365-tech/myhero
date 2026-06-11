package com.jjk.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
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
        // 보스바 위 별도 텍스트 오버레이 제거 — 보스바 이름(CeBossBarManager)으로 대체됨
    }
}
