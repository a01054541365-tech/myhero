package com.jjk.client;

import com.jjk.client.JjkClientState;
import com.jjk.client.anim.AnimationCache;
import com.jjk.client.anim.PlayerAnimationDispatcher;
import com.jjk.client.anim.SkillAnimController;
import com.jjk.client.fx.SkillFxDispatcher;
import com.jjk.client.hud.JjkHudRenderer;
import com.jjk.client.renderer.CeProjectileEntityRenderer;
import com.jjk.client.renderer.CursedSpiritEntityRenderer;
import com.jjk.client.renderer.CurtainRenderer;
import com.jjk.client.renderer.DomainBoundaryRenderer;
import com.jjk.client.renderer.NpcEntityRenderer;
import com.jjk.client.renderer.NueEntityRenderer;
import com.jjk.client.renderer.RikaEntityRenderer;
import com.jjk.client.renderer.WhiteDogEntityRenderer;
import com.jjk.client.screen.CharacterSelectScreen;
import com.jjk.client.screen.npc.GojoShiyuScreen;
import com.jjk.client.screen.npc.IjichiScreen;
import com.jjk.client.screen.npc.KusakabeScreen;
import com.jjk.client.screen.npc.NahovinoScreen;
import com.jjk.client.screen.npc.ShokoScreen;
import com.jjk.client.screen.npc.YagaScreen;
import com.jjk.client.screen.npc.ZeninShopScreen;
import com.jjk.client.costume.CostumeClientCache;
import com.jjk.client.costume.CostumeRenderLayer;
import com.jjk.client.fx.ParticleThrottle;
import com.jjk.client.fx.SkillEffectRenderer;
import com.jjk.client.hud.BlackFlashOverlay;
import com.jjk.client.renderer.AwakeningAuraRenderer;
import com.jjk.client.effect.WorldAmbientEffects;
import com.jjk.client.effect.impl.*;
import com.jjk.client.hud.CEAuraRenderer;
import com.jjk.client.hud.EntityHealthBarRenderer;
import com.jjk.network.s2c.BlackFlashTimingS2CPacket;
import com.jjk.network.s2c.CEAuraSyncS2CPacket;
import com.jjk.network.s2c.FingerRadarPulseS2CPacket;
import com.jjk.network.s2c.CostumeSyncS2CPacket;
import com.jjk.network.s2c.EntityHealthSyncS2CPacket;
import com.jjk.network.s2c.HudSyncS2CPacket;
import com.jjk.network.s2c.OpenCharacterSelectS2CPacket;
import com.jjk.network.s2c.SkillEffectS2CPacket;
import com.jjk.network.s2c.NpcOpenGuiS2CPacket;
import com.jjk.network.s2c.SealedSkillSyncS2CPacket;
import com.jjk.network.s2c.VerdictS2CPacket;
import com.jjk.entity.CursedSpiritEntityTypes;
import com.jjk.entity.JJKEntities;
import com.jjk.entity.ShikigamiEntityTypes;
import com.jjk.entity.npc.NpcRegistry;
import com.jjk.entity.npc.SimpleNpcEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import com.jjk.item.GuideBookItem;
import com.jjk.network.c2s.GuideBookOpenC2SPacket;
import com.jjk.network.c2s.SkillUseC2SPacket;
import com.jjk.network.c2s.WeaponInfuseC2SPacket;
import com.jjk.network.s2c.WeaponInfusionS2CPacket;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import com.jjk.network.s2c.AwakeningS2CPacket;
import com.jjk.network.s2c.CharacterConfirmS2CPacket;
import com.jjk.network.s2c.CharacterInfoS2CPacket;
import com.jjk.network.s2c.CharacterSelectFailS2CPacket;
import com.jjk.network.s2c.CharacterSelectS2CPacket;
import com.jjk.network.s2c.ChantingStateS2CPacket;
import com.jjk.network.s2c.CurtainEnterS2CPacket;
import com.jjk.network.s2c.CurtainExitS2CPacket;
import com.jjk.network.s2c.FingerDropS2CPacket;
import com.jjk.network.s2c.DomainDeployFailS2CPacket;
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
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
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
    private static KeyBinding KEY_SKILL_C;
    private static KeyBinding KEY_SKILL_T;
    private static boolean wasUseKeyDown = false;

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

        // ── NPC 렌더러 등록 (8종) ──────────────────────────────────
        EntityRendererRegistry.register(NpcRegistry.ZENIN_STORAGE, NpcEntityRenderer::new);
        EntityRendererRegistry.register(NpcRegistry.KUSAKABE,       NpcEntityRenderer::new);
        EntityRendererRegistry.register(NpcRegistry.SHOKO,          NpcEntityRenderer::new);
        EntityRendererRegistry.register(NpcRegistry.GOJO_SHIYU,     NpcEntityRenderer::new);
        EntityRendererRegistry.register(NpcRegistry.IJICHI,         NpcEntityRenderer::new);
        EntityRendererRegistry.register(NpcRegistry.YAGA,           NpcEntityRenderer::new);
        EntityRendererRegistry.register(NpcRegistry.NAHOBINO,       NpcEntityRenderer::new);
        //noinspection unchecked
        EntityRendererRegistry.register(
            (EntityType<SimpleNpcEntity>)(EntityType<?>) NpcRegistry.TUTORIAL,
            NpcEntityRenderer::new);

        // ── 주령 렌더러 등록 (5종) ──────────────────────────────────
        EntityRendererRegistry.register(CursedSpiritEntityTypes.GRADE_4, CursedSpiritEntityRenderer::new);
        EntityRendererRegistry.register(CursedSpiritEntityTypes.GRADE_3, CursedSpiritEntityRenderer::new);
        EntityRendererRegistry.register(CursedSpiritEntityTypes.GRADE_2, CursedSpiritEntityRenderer::new);
        EntityRendererRegistry.register(CursedSpiritEntityTypes.GRADE_1, CursedSpiritEntityRenderer::new);
        EntityRendererRegistry.register(CursedSpiritEntityTypes.SPECIAL,  CursedSpiritEntityRenderer::new);

        // ── CE 투사체 렌더러 등록 ──────────────────────────────────
        EntityRendererRegistry.register(CursedSpiritEntityTypes.CE_PROJECTILE, CeProjectileEntityRenderer::new);

        // ── [임시] 3D 모델 미완성 엔티티 폴백 렌더러 ──────────────────────────────
        EntityRendererRegistry.register(CursedSpiritEntityTypes.SEMI_SPECIAL,     CursedSpiritEntityRenderer::new);
        EntityRendererRegistry.register(JJKEntities.MUKI,                         bipedFallback());
        EntityRendererRegistry.register(JJKEntities.KOTSIBAKU,                    bipedFallback());
        EntityRendererRegistry.register(JJKEntities.HOMURAKU,                     bipedFallback());
        EntityRendererRegistry.register(JJKEntities.JOGO_NPC,                     bipedFallback());
        EntityRendererRegistry.register(JJKEntities.HANNAMI_NPC,                  bipedFallback());
        EntityRendererRegistry.register(JJKEntities.JUUGO_NPC,                    bipedFallback());
        EntityRendererRegistry.register(JJKEntities.SYOUTO,                       bipedFallback());
        EntityRendererRegistry.register(JJKEntities.MOLE_CURSED_SPIRIT,           bipedFallback());
        EntityRendererRegistry.register(JJKEntities.FINGER_BEARER,                bipedFallback());
        EntityRendererRegistry.register(JJKEntities.PLANT_CURSED_SPIRIT,          bipedFallback());
        EntityRendererRegistry.register(JJKEntities.WATER_CURSED_SPIRIT,          bipedFallback());
        EntityRendererRegistry.register(JJKEntities.SMALLPOX_DEITY,               bipedFallback());
        EntityRendererRegistry.register(JJKEntities.CE_ABSORBER,                  bipedFallback());
        EntityRendererRegistry.register(JJKEntities.SPLITTING_CURSED_SPIRIT,      bipedFallback());
        EntityRendererRegistry.register(JJKEntities.SHADOW_CURSED_SPIRIT,         bipedFallback());
        EntityRendererRegistry.register(JJKEntities.SORCERER_NPC_4,               bipedFallback());
        EntityRendererRegistry.register(JJKEntities.SORCERER_NPC_3,               bipedFallback());
        EntityRendererRegistry.register(JJKEntities.SORCERER_NPC_2,               bipedFallback());
        EntityRendererRegistry.register(JJKEntities.SORCERER_NPC_1,               bipedFallback());
        EntityRendererRegistry.register(JJKEntities.KOTSIBAKU_PROJECTILE,         projectileFallback());
        EntityRendererRegistry.register(JJKEntities.CE_PROJECTILE,                projectileFallback());

        // 캐릭터별 이펙트 핸들러 등록 (SkillEffectRegistry)
        CommonEffects.register();
        GojoEffects.register();
        ItadoriEffects.register();
        NanamiEffects.register();
        JogoEffects.register();
        MahitoEffects.register();
        HakariEffects.register();
        InumakiEffects.register();
        HigurumaEffects.register();
        OkkotsuEffects.register();

        // 스킬 이펙트 렌더러 + 각성 오라 렌더러 + CE 오라 렌더러 + 엔티티 HP바 + 환경 이펙트 등록
        SkillEffectRenderer.register();
        AwakeningAuraRenderer.register();
        CEAuraRenderer.register();
        EntityHealthBarRenderer.register();
        WorldAmbientEffects.register();
        HudRenderCallback.EVENT.register(BlackFlashOverlay.INSTANCE::render);

        // 의상 렌더 레이어 — PlayerEntityRenderer에 FeatureRenderer 추가
        //noinspection unchecked
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
            (entityType, renderer, registrationHelper, context) -> {
                if (renderer instanceof PlayerEntityRenderer per) {
                    registrationHelper.register(
                        new CostumeRenderLayer((net.minecraft.client.render.entity.feature
                            .FeatureRendererContext) per));
                }
            }
        );

        // 서버 접속 해제 시 의상 캐시 초기화
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
            CostumeClientCache.clear());

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
        KEY_SKILL_C  = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.jjk.skill_c",  GLFW.GLFW_KEY_C, KEY_CATEGORY));
        KEY_SKILL_T  = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.jjk.skill_t",  GLFW.GLFW_KEY_T, KEY_CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SkillAnimController.tick(client);
            JjkHudRenderer.INSTANCE.tick(client);
            if (client.player == null || client.world == null) return;
            if (client.currentScreen != null) return;
            checkAndSend(KEY_SKILL_F,  0);
            checkAndSend(KEY_SKILL_SF, 1);
            checkAndSend(KEY_SKILL_R,  2);
            checkAndSend(KEY_SKILL_SR, 3);
            checkAndSend(KEY_SKILL_V,  4);
            checkAndSend(KEY_SKILL_C,  5);
            checkAndSend(KEY_SKILL_T,  6);

            // CE 무기 주입: Shift+우클릭 (검·도끼 장비 시) — rising-edge 감지
            boolean useDown = client.options.useKey.isPressed();
            if (!wasUseKeyDown && useDown
                    && client.player.isSneaking()
                    && hasInfusionWeapon(client.player)) {
                ClientPlayNetworking.send(new WeaponInfuseC2SPacket());
            }
            wasUseKeyDown = useDown;
        });

        // 가이드북 우클릭 감지 → GuideBookOpenC2SPacket 전송
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient()) return TypedActionResult.pass(player.getStackInHand(hand));
            net.minecraft.item.ItemStack stack = player.getStackInHand(hand);
            if (GuideBookItem.isGuideBook(stack)) {
                ClientPlayNetworking.send(new GuideBookOpenC2SPacket());
                return TypedActionResult.success(stack);
            }
            return TypedActionResult.pass(stack);
        });
    }

    private static <T extends MobEntity> EntityRendererFactory<T> bipedFallback() {
        return ctx -> new BipedEntityRenderer<T, BipedEntityModel<T>>(
                ctx, new BipedEntityModel<>(ctx.getPart(EntityModelLayers.ZOMBIE)), 0.5f) {
            @Override
            public Identifier getTexture(T entity) {
                return Identifier.of("minecraft", "textures/entity/zombie/zombie.png");
            }
        };
    }

    private static <T extends Entity> EntityRendererFactory<T> projectileFallback() {
        return ctx -> new EntityRenderer<>(ctx) {
            @Override
            public Identifier getTexture(T entity) {
                return Identifier.of("minecraft", "textures/item/snowball.png");
            }
        };
    }

    private static boolean hasInfusionWeapon(AbstractClientPlayerEntity player) {
        var item = player.getMainHandStack().getItem();
        return item instanceof SwordItem || item instanceof AxeItem;
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
                    long worldTick = mc.world != null ? mc.world.getTime() : 0L;
                    JjkClientState.onZoneEnter(domainName, worldTick);
                    JjkHudRenderer.INSTANCE.getDomainIndicator().onEnter(domainName, worldTick);
                }));

        // ZoneExitS2CPacket — 도메인 경계 제거 + 애니메이션 중지
        ClientPlayNetworking.registerGlobalReceiver(ZoneExitS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    LOGGER.debug("ZONE_EXIT domain={}", pkt.domainId());
                    UUID domainUUID = UUID.nameUUIDFromBytes(pkt.domainId().getBytes(StandardCharsets.UTF_8));
                    DomainBoundaryRenderer.deactivateDomain(domainUUID);
                    MinecraftClient mc = ctx.client();
                    if (mc.player != null) PlayerAnimationDispatcher.stop(mc.player);
                    long worldTick = mc.world != null ? mc.world.getTime() : 0L;
                    JjkClientState.onZoneExit();
                    JjkHudRenderer.INSTANCE.getDomainIndicator().onExit(worldTick);
                }));

        // AwakeningS2CPacket — 각성 애니메이션 + 클라이언트 상태 갱신
        ClientPlayNetworking.registerGlobalReceiver(AwakeningS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    LOGGER.debug("AWAKENING active={}", pkt.active());
                    MinecraftClient mc = ctx.client();
                    JjkClientState.setAwakening(pkt.active());
                    AwakeningAuraRenderer.setLocalPlayerAwakening(pkt.active());
                    if (pkt.active()) BlackFlashOverlay.INSTANCE.triggerActivateFlash();
                    if (mc.player != null) SkillAnimController.onAwakening(pkt, mc.player);
                }));

        // CharacterSelectS2CPacket — 캐릭터 선택 화면 열기 (TASK-31)
        ClientPlayNetworking.registerGlobalReceiver(CharacterSelectS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() ->
                        ctx.client().setScreen(new CharacterSelectScreen(pkt.availableCharacters()))));

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

        // CharacterSelectFailS2CPacket — 캐릭터 선택 실패 알림
        ClientPlayNetworking.registerGlobalReceiver(CharacterSelectFailS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    if (ctx.client().player == null) return;
                    String msg = switch (pkt.reason()) {
                        case "grade_insufficient" -> "§c[JJK] 등급이 부족합니다.";
                        case "duplicate"          -> "§c[JJK] 이미 다른 플레이어가 선택한 캐릭터입니다.";
                        default                   -> "§c[JJK] 캐릭터 선택 실패: " + pkt.reason();
                    };
                    ctx.client().player.sendMessage(Text.literal(msg), false);
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

        // NpcOpenGuiS2CPacket — NPC GUI 화면 열기 (TASK-94)
        ClientPlayNetworking.registerGlobalReceiver(NpcOpenGuiS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    net.minecraft.client.gui.screen.Screen screen = switch (pkt.npcId()) {
                        case "zenin_storage" -> new ZeninShopScreen(pkt.payload());
                        case "kusakabe"      -> new KusakabeScreen(pkt.payload());
                        case "shoko"         -> new ShokoScreen(pkt.payload());
                        case "gojo_shiyu"    -> new GojoShiyuScreen(pkt.payload());
                        case "ijichi"        -> new IjichiScreen(pkt.payload());
                        case "yaga"          -> new YagaScreen(pkt.payload());
                        case "nahobino"      -> new NahovinoScreen(pkt.payload());
                        default -> null;
                    };
                    if (screen != null) ctx.client().setScreen(screen);
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

        // BossBarUpdateS2CPacket — CE/HP 상태 클라이언트 캐시 갱신
        ClientPlayNetworking.registerGlobalReceiver(
                com.jjk.network.s2c.BossBarUpdateS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    JjkClientState.updateHp(pkt.hp(), pkt.hpMax());
                    JjkClientState.updateCe((float) pkt.ce(), (float) pkt.ceMax());
                }));

        // SkillCooldownSyncS2CPacket — 스킬 쿨타임 HUD 갱신
        ClientPlayNetworking.registerGlobalReceiver(
                com.jjk.network.s2c.SkillCooldownSyncS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    MinecraftClient mc = ctx.client();
                    if (mc.world == null) return;
                    JjkClientState.onSkillCooldown(pkt.keyId(), pkt.cooldownTicks(), mc.world.getTime());
                }));

        // CostumeSyncS2CPacket — 의상 캐시 갱신
        ClientPlayNetworking.registerGlobalReceiver(CostumeSyncS2CPacket.ID, (pkt, ctx) ->
            ctx.client().execute(() ->
                CostumeClientCache.update(pkt.targetUuid(), pkt.costumeId())));

        // HudSyncS2CPacket — CE/HP/등급/전투 상태 일괄 갱신
        ClientPlayNetworking.registerGlobalReceiver(HudSyncS2CPacket.ID, (pkt, ctx) ->
            ctx.client().execute(() ->
                JjkClientState.updateHudSync(
                    pkt.cePercent(), pkt.hpPercent(), pkt.grade(),
                    pkt.inCombat(), pkt.skillCooldownsRemaining())));

        // BlackFlashTimingS2CPacket — 흑섬 Just Frame 타이밍 게이지
        ClientPlayNetworking.registerGlobalReceiver(BlackFlashTimingS2CPacket.ID, (pkt, ctx) ->
            ctx.client().execute(() ->
                JjkHudRenderer.INSTANCE.onBlackFlashTiming(
                    pkt.show(), pkt.windowStartTick(), pkt.windowEndTick())));

        // EntityHealthSyncS2CPacket — 주령 체력 바 캐시 갱신
        ClientPlayNetworking.registerGlobalReceiver(EntityHealthSyncS2CPacket.ID, (pkt, ctx) ->
            ctx.client().execute(() ->
                EntityHealthBarRenderer.onPacket(pkt)));

        // OpenCharacterSelectS2CPacket — 현재 캐릭터 하이라이트 포함 선택 화면 열기
        ClientPlayNetworking.registerGlobalReceiver(OpenCharacterSelectS2CPacket.ID, (pkt, ctx) ->
            ctx.client().execute(() ->
                ctx.client().setScreen(new CharacterSelectScreen(
                    pkt.availableCharacterIds(), pkt.currentCharacterId()))));

        // CEAuraSyncS2CPacket — CE 오라 렌더러 캐시 갱신 (G-1)
        ClientPlayNetworking.registerGlobalReceiver(CEAuraSyncS2CPacket.ID, (pkt, ctx) ->
            ctx.client().execute(() -> CEAuraRenderer.onPacket(pkt)));

        // SealedSkillSyncS2CPacket — 단일-스킬 봉인 상태 동기화 (Phase I-2)
        ClientPlayNetworking.registerGlobalReceiver(SealedSkillSyncS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    MinecraftClient mc = ctx.client();
                    if (mc.player == null) return;
                    if (!pkt.targetUuid().equals(mc.player.getUuid())) return;
                    JjkClientState.onSealedSkillSync(pkt.sealedSkillId(), pkt.expireAtTick());
                }));

        // VerdictS2CPacket — 재판 판결 통지 (Phase I-2)
        ClientPlayNetworking.registerGlobalReceiver(VerdictS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() ->
                        JjkHudRenderer.INSTANCE.showVerdict(pkt.guilty(), pkt.sealedSkillId())));

        // FingerRadarPulseS2CPacket — 스쿠나 레이더: 소스 위치에 파티클 + 액션바 알림
        ClientPlayNetworking.registerGlobalReceiver(FingerRadarPulseS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    MinecraftClient mc = ctx.client();
                    if (mc.world == null || mc.player == null) return;
                    Vec3d pos = Vec3d.ofCenter(pkt.sourcePos());
                    for (int i = 0; i < 12; i++) {
                        mc.world.addParticle(ParticleTypes.SOUL,
                                pos.x + (mc.world.random.nextDouble() - 0.5) * 2.0,
                                pos.y + mc.world.random.nextDouble() * 2.0,
                                pos.z + (mc.world.random.nextDouble() - 0.5) * 2.0,
                                0.0, 0.05, 0.0);
                    }
                    mc.player.sendMessage(Text.literal("§4⚡ [저주] 스쿠나의 기운이 느껴진다…"), true);
                }));

        // WeaponInfusionS2CPacket — CE 무기 주입 활성화 알림 + 파티클
        ClientPlayNetworking.registerGlobalReceiver(WeaponInfusionS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    MinecraftClient mc = ctx.client();
                    if (mc.player == null || mc.world == null) return;
                    int seconds = (pkt.durationTicks() + 19) / 20;
                    int pct = Math.round((pkt.multValue() - 1f) * 100f);
                    mc.player.sendMessage(
                        Text.literal("§b⚡ CE 주입! +" + pct + "% (" + seconds + "초)"), true);
                    for (int i = 0; i < 20; i++) {
                        mc.world.addParticle(
                            net.minecraft.particle.ParticleTypes.ENCHANT,
                            mc.player.getX() + (mc.world.random.nextDouble() - 0.5) * 0.8,
                            mc.player.getY() + mc.world.random.nextDouble() * 2.0,
                            mc.player.getZ() + (mc.world.random.nextDouble() - 0.5) * 0.8,
                            0.0, 0.1, 0.0);
                    }
                }));

        // DomainDeployFailS2CPacket — 영역 전개 실패 사유 알림
        ClientPlayNetworking.registerGlobalReceiver(DomainDeployFailS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    MinecraftClient mc = ctx.client();
                    if (mc.player == null) return;
                    String msg = switch (pkt.reason()) {
                        case CE_INSUFFICIENT    -> "§c영역 전개 실패: CE 부족";
                        case ON_COOLDOWN        -> "§c영역 전개 실패: 쿨타임 중";
                        case BANNED_CHUNK       -> "§c영역 전개 실패: 사용 불가 지역";
                        case DOMAIN_ALREADY_ACTIVE -> "§c영역 전개 실패: 이미 활성 중";
                        case CLASH_LOST         -> "§c영역 전개 실패: 충돌 패배";
                    };
                    mc.player.sendMessage(Text.literal(msg), true);
                }));
    }
}
