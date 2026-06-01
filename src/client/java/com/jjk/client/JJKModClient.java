package com.jjk.client;

import com.jjk.client.JjkClientState;
import com.jjk.client.anim.AnimationCache;
import com.jjk.client.anim.PlayerAnimationDispatcher;
import com.jjk.client.anim.SkillAnimController;
import com.jjk.client.fx.SkillFxDispatcher;
import com.jjk.client.hud.JjkHudRenderer;
import com.jjk.client.renderer.CurtainRenderer;
import com.jjk.client.renderer.DomainBoundaryRenderer;
import com.jjk.client.renderer.NueEntityRenderer;
import com.jjk.client.renderer.RikaEntityRenderer;
import com.jjk.client.renderer.WhiteDogEntityRenderer;
import com.jjk.client.screen.CharacterSelectScreen;
import com.jjk.entity.ShikigamiEntityTypes;
import com.jjk.network.c2s.SkillUseC2SPacket;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import com.jjk.network.s2c.AwakeningS2CPacket;
import com.jjk.network.s2c.CharacterConfirmS2CPacket;
import com.jjk.network.s2c.CharacterInfoS2CPacket;
import com.jjk.network.s2c.CharacterSelectS2CPacket;
import com.jjk.network.s2c.ChantingStateS2CPacket;
import com.jjk.network.s2c.CurtainEnterS2CPacket;
import com.jjk.network.s2c.CurtainExitS2CPacket;
import com.jjk.network.s2c.FingerDropS2CPacket;
import com.jjk.network.s2c.RespawnS2CPacket;
import com.jjk.network.s2c.SkillResultS2CPacket;
import com.jjk.network.s2c.ZoneEnterS2CPacket;
import com.jjk.network.s2c.ZoneExitS2CPacket;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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

        // 렌더러 등록
        DomainBoundaryRenderer.register();
        CurtainRenderer.register();
        EntityRendererRegistry.register(ShikigamiEntityTypes.NUE, NueEntityRenderer::new);
        EntityRendererRegistry.register(ShikigamiEntityTypes.WHITE_DOG, WhiteDogEntityRenderer::new);
        EntityRendererRegistry.register(ShikigamiEntityTypes.RIKA, RikaEntityRenderer::new);
        EntityRendererRegistry.register(ShikigamiEntityTypes.MAHORAGA, NueEntityRenderer::new);

        // HUD 렌더러 등록
        HudRenderCallback.EVENT.register(JjkHudRenderer.INSTANCE::render);

        // PlayerAnimator: 플레이어당 1개 ModifierLayer 등록
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register(
                (player, stack) -> PlayerAnimationDispatcher.registerLayer(player, stack)
        );

        // 애니메이션 파일 로드 (클라이언트 시작 시)
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                AnimationCache.loadAll(client.getResourceManager())
        );

        // 서버 접속 해제 시 레이어 캐시 정리
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.player != null) {
                PlayerAnimationDispatcher.removeLayer(client.player.getUuid());
            }
        });

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
            SkillAnimController.tick(client);
            if (client.player == null || client.world == null) return;
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
            ClientPlayNetworking.send(new SkillUseC2SPacket(
                    MinecraftClient.getInstance().player.getUuid(),
                    (byte) keyId, null));
        }
    }

    private void registerPacketHandlers() {
        // AnimationTriggerS2CPacket — 서버가 보내는 animId로 직접 재생
        ClientPlayNetworking.registerGlobalReceiver(AnimationTriggerS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    ClientWorld world = ctx.client().world;
                    if (world == null) return;
                    AbstractClientPlayerEntity target = world.getPlayers().stream()
                            .filter(p -> pkt.targetUuid().equals(p.getUuid()))
                            .findFirst().orElse(null);
                    if (target == null) return;
                    SkillAnimController.onAnimTrigger(target, pkt.animId());
                }));

        // SkillResultS2CPacket — 애니메이션 + 파티클 이펙트 (TASK-27)
        ClientPlayNetworking.registerGlobalReceiver(SkillResultS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    MinecraftClient mc = ctx.client();
                    if (mc.player == null || mc.world == null) return;
                    SkillAnimController.onSkillResult(pkt, mc.player);
                    SkillFxDispatcher.dispatch(pkt, mc);
                    // 흑섬 발동 시 액션바 알림 유지
                    if (pkt.result().contains("BLACK_FLASH")) {
                        mc.player.sendMessage(
                            Text.literal("§4⚡ 흑섬! " + (int) pkt.finalDamage()), true);
                    }
                }));

        // ZoneEnterS2CPacket — 도메인 경계 렌더 + 입장 애니메이션
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
                    SkillAnimController.onZoneEnter(pkt, mc.player);
                }));

        // ZoneExitS2CPacket — 도메인 경계 제거 + 애니메이션 중지
        ClientPlayNetworking.registerGlobalReceiver(ZoneExitS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    LOGGER.debug("ZONE_EXIT domain={}", pkt.domainId());
                    UUID domainUUID = UUID.nameUUIDFromBytes(pkt.domainId().getBytes(StandardCharsets.UTF_8));
                    DomainBoundaryRenderer.deactivateDomain(domainUUID);
                    MinecraftClient mc = ctx.client();
                    if (mc.player != null) PlayerAnimationDispatcher.stop(mc.player);
                }));

        // AwakeningS2CPacket — 각성 애니메이션
        ClientPlayNetworking.registerGlobalReceiver(AwakeningS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    LOGGER.debug("AWAKENING active={}", pkt.active());
                    MinecraftClient mc = ctx.client();
                    if (mc.player != null) SkillAnimController.onAwakening(pkt, mc.player);
                }));

        // CharacterSelectS2CPacket — 캐릭터 선택 화면 열기 (TASK-31)
        ClientPlayNetworking.registerGlobalReceiver(CharacterSelectS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() ->
                        ctx.client().setScreen(
                                new CharacterSelectScreen(pkt.availableCharacters()))));

        // CharacterInfoS2CPacket — 클라이언트 상태 갱신 (TASK-26)
        ClientPlayNetworking.registerGlobalReceiver(CharacterInfoS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() ->
                        JjkClientState.update(pkt.characterId(), pkt.grade(),
                                pkt.ceMax(), pkt.ceCurrent())));

        // CharacterConfirmS2CPacket — 캐릭터 선택 확정 알림
        ClientPlayNetworking.registerGlobalReceiver(CharacterConfirmS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    JjkClientState.setCharacterId(pkt.characterId());
                    if (ctx.client().player != null) {
                        ctx.client().player.sendMessage(
                            Text.literal("§a[JJK] 캐릭터 선택: " + pkt.characterId()), false);
                    }
                }));

        // CurtainEnterS2CPacket / CurtainExitS2CPacket (TASK-30)
        ClientPlayNetworking.registerGlobalReceiver(CurtainEnterS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() ->
                        CurtainRenderer.onEnter(pkt, ctx.client())));

        ClientPlayNetworking.registerGlobalReceiver(CurtainExitS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() ->
                        CurtainRenderer.onExit(pkt)));

        // ChantingStateS2CPacket — 영창 HUD 갱신 (TASK-28)
        ClientPlayNetworking.registerGlobalReceiver(ChantingStateS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() ->
                        JjkHudRenderer.INSTANCE.updateChanting(pkt.chanting(), pkt.chantTicks())));

        // RespawnS2CPacket
        ClientPlayNetworking.registerGlobalReceiver(RespawnS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    if (ctx.client().player != null) {
                        ctx.client().player.sendMessage(Text.literal("[JJK] 부활"), false);
                    }
                }));

        // FingerDropS2CPacket — 손가락 획득 알림 (TASK-35)
        ClientPlayNetworking.registerGlobalReceiver(FingerDropS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    if (ctx.client().player == null) return;
                    String msg = pkt.isMaxReached()
                        ? "§4§l모든 손가락이 모였다!"
                        : "§4손가락 획득! (" + pkt.newFingerCount() + "/20)";
                    ctx.client().player.sendMessage(Text.literal(msg), true);
                }));
    }
}
