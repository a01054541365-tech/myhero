package com.jjk.client;

import com.jjk.client.anim.SkillAnimController;
import com.jjk.client.renderer.DomainBoundaryRenderer;
import com.jjk.network.c2s.SkillUseC2SPacket;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import com.jjk.network.s2c.AwakeningS2CPacket;
import com.jjk.network.s2c.SkillResultS2CPacket;
import com.jjk.network.s2c.ZoneEnterS2CPacket;
import com.jjk.network.s2c.ZoneExitS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class JJKModClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-client");

    private static final String KEY_CATEGORY = "JujutsuKaisen";

    private static KeyBinding KEY_SKILL_F;
    private static KeyBinding KEY_SKILL_SF;
    private static KeyBinding KEY_SKILL_R;
    private static KeyBinding KEY_SKILL_SR;
    private static KeyBinding KEY_SKILL_V;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[JJK] Client initialized");
        SkillAnimController.getInstance().registerAll();
        DomainBoundaryRenderer.register();
        registerKeyBindings();
        registerPacketHandlers();
    }

    private void registerKeyBindings() {
        KEY_SKILL_F  = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.jjk.skill_f",  GLFW.GLFW_KEY_F, KEY_CATEGORY));
        KEY_SKILL_SF = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.jjk.skill_sf", GLFW.GLFW_KEY_G, KEY_CATEGORY));
        KEY_SKILL_R  = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.jjk.skill_r",  GLFW.GLFW_KEY_R, KEY_CATEGORY));
        KEY_SKILL_SR = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.jjk.skill_sr", GLFW.GLFW_KEY_H, KEY_CATEGORY));
        KEY_SKILL_V  = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.jjk.skill_v",  GLFW.GLFW_KEY_V, KEY_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (client.world == null) return;
            if (client.currentScreen != null) return;
            checkAndSend(KEY_SKILL_F,  0);
            checkAndSend(KEY_SKILL_SF, 1);
            checkAndSend(KEY_SKILL_R,  2);
            checkAndSend(KEY_SKILL_SR, 3);
            checkAndSend(KEY_SKILL_V,  4);
        });
    }

    private static void checkAndSend(KeyBinding key, int keyId) {
        while (key.wasPressed()) {
            LOGGER.info("[JJK] Key pressed keyId={}", keyId);
            ClientPlayNetworking.send(new SkillUseC2SPacket(keyId));
        }
    }

    private void registerPacketHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(ZoneEnterS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    LOGGER.debug("ZONE_ENTER domain={}", pkt.domainId());
                    MinecraftClient mc = ctx.client();
                    if (mc.player == null) return;
                    String domainName = pkt.domainId();
                    UUID domainUUID = UUID.nameUUIDFromBytes(domainName.getBytes(StandardCharsets.UTF_8));
                    Vec3d center = mc.player.getPos();
                    boolean isOpen = domainName.contains("sukuna");
                    DomainBoundaryRenderer.activateDomain(domainUUID, domainName, center, 15.0f, isOpen);
                }));

        ClientPlayNetworking.registerGlobalReceiver(ZoneExitS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    LOGGER.debug("ZONE_EXIT domain={}", pkt.domainId());
                    UUID domainUUID = UUID.nameUUIDFromBytes(pkt.domainId().getBytes(StandardCharsets.UTF_8));
                    DomainBoundaryRenderer.deactivateDomain(domainUUID);
                }));

        ClientPlayNetworking.registerGlobalReceiver(AwakeningS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() ->
                        LOGGER.debug("AWAKENING active={}", pkt.active())));

        ClientPlayNetworking.registerGlobalReceiver(AnimationTriggerS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    MinecraftClient mc = ctx.client();
                    if (mc.player == null) return;
                    if (pkt.targetUuid().equals(mc.player.getUuid())) {
                        SkillAnimController.getInstance().onAnimTrigger(pkt.animId());
                    } else {
                        ClientWorld world = mc.world;
                        if (world == null) return;
                        AbstractClientPlayerEntity entity = world.getPlayers().stream()
                                .filter(p -> pkt.targetUuid().equals(p.getUuid()))
                                .findFirst().orElse(null);
                        if (entity == null) return;
                        SkillAnimController.getInstance().onAnimTriggerForOther(entity, pkt.animId());
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(SkillResultS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    MinecraftClient mc = ctx.client();
                    if (mc.player == null || mc.world == null) return;
                    Vec3d hitPos = mc.player.getEyePos()
                            .add(mc.player.getRotationVec(1.0f).multiply(3.0));
                    boolean isBlackFlash = pkt.result().contains("BLACK_FLASH");
                    int particleCount = isBlackFlash ? 20 : 8;
                    mc.world.addParticle(
                            isBlackFlash
                                    ? net.minecraft.particle.ParticleTypes.SOUL
                                    : net.minecraft.particle.ParticleTypes.CRIT,
                            hitPos.x, hitPos.y, hitPos.z,
                            0, 0.1, 0
                    );
                    String dmgText = isBlackFlash
                            ? "§4⚡ " + (int) pkt.finalDamage()
                            : "§f" + (int) pkt.finalDamage();
                    mc.player.sendMessage(
                            net.minecraft.text.Text.literal(dmgText), true);
                }));
    }
}
