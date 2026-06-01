package com.jjk.client.anim;

import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.util.Ease;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public final class PlayerAnimationDispatcher {
    private PlayerAnimationDispatcher() {}

    // 플레이어별 ModifierLayer 캐시 (REGISTER_ANIMATION_EVENT에서 등록됨)
    private static final Map<UUID, ModifierLayer<KeyframeAnimationPlayer>> LAYERS = new ConcurrentHashMap<>();

    /** REGISTER_ANIMATION_EVENT 콜백에서 호출. 플레이어당 1회. */
    public static void registerLayer(AbstractClientPlayerEntity player, AnimationStack stack) {
        ModifierLayer<KeyframeAnimationPlayer> layer = new ModifierLayer<>();
        stack.addAnimLayer(10, layer);
        LAYERS.put(player.getUuid(), layer);
    }

    /** 1회 재생 편의 메서드. */
    public static void playOnce(AbstractClientPlayerEntity player, String animName) {
        play(player, animName, false);
    }

    /** 루프 재생 편의 메서드. */
    public static void loop(AbstractClientPlayerEntity player, String animName) {
        play(player, animName, true);
    }

    /** 스킬 발동 시 애니메이션 재생. */
    public static void play(AbstractClientPlayerEntity player, String animName, boolean loop) {
        ModifierLayer<KeyframeAnimationPlayer> layer = LAYERS.get(player.getUuid());
        if (layer == null) return;

        KeyframeAnimation animData = AnimationCache.get(animName);
        if (animData == null) return;

        KeyframeAnimationPlayer anim = loop
                ? new KeyframeAnimationPlayer(animData, 0, true)
                : new KeyframeAnimationPlayer(animData);
        layer.replaceAnimationWithFade(AbstractFadeModifier.standardFadeIn(5, Ease.LINEAR), anim);
    }

    /** 현재 재생 중인 애니메이션 중지 */
    public static void stop(AbstractClientPlayerEntity player) {
        ModifierLayer<KeyframeAnimationPlayer> layer = LAYERS.get(player.getUuid());
        if (layer == null || layer.getAnimation() == null) return;
        layer.getAnimation().stop();
    }

    /** 플레이어 퇴장 시 레이어 제거 */
    public static void removeLayer(UUID playerUuid) {
        LAYERS.remove(playerUuid);
    }
}
