package com.jjk.client.effect;

import com.jjk.network.s2c.SkillEffectS2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

import java.util.HashMap;
import java.util.Map;

/**
 * SkillEffectS2CPacket 수신 시 effectType 키로 핸들러를 조회·실행.
 * 캐릭터별 이펙트 구현 클래스는 register(SkillEffectRegistry) 패턴으로 등록.
 */
@Environment(EnvType.CLIENT)
public final class SkillEffectRegistry {
    private SkillEffectRegistry() {}

    @FunctionalInterface
    public interface SkillEffectHandler {
        void handle(SkillEffectS2CPacket pkt, ClientWorld world);
    }

    private static final Map<String, SkillEffectHandler> REGISTRY = new HashMap<>();

    public static void register(String effectId, SkillEffectHandler handler) {
        REGISTRY.put(effectId, handler);
    }

    /**
     * 등록된 핸들러로 이펙트를 처리한다.
     * @return true = 핸들러 있음, false = 미등록 (폴백 필요)
     */
    public static boolean dispatch(SkillEffectS2CPacket pkt, MinecraftClient mc) {
        if (mc.world == null) return false;
        SkillEffectHandler handler = REGISTRY.get(pkt.effectType());
        if (handler == null) return false;
        try {
            handler.handle(pkt, mc.world);
        } catch (Exception ignored) {}
        return true;
    }
}
