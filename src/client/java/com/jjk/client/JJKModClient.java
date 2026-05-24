package com.jjk.client;

import com.jjk.client.anim.SkillAnimController;
import com.jjk.client.renderer.DomainBoundaryRenderer;
import com.jjk.network.s2c.AnimationTriggerS2CPacket;
import com.jjk.network.s2c.AwakeningS2CPacket;
import com.jjk.network.s2c.ZoneEnterS2CPacket;
import com.jjk.network.s2c.ZoneExitS2CPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class JJKModClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-client");

    @Override
    public void onInitializeClient() {
        SkillAnimController.getInstance().registerAll();
        DomainBoundaryRenderer.register();
        registerPacketHandlers();
    }

    private void registerPacketHandlers() {
        ClientPlayNetworking.registerGlobalReceiver(ZoneEnterS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    LOGGER.debug("ZONE_ENTER domain={}", pkt.domainId());
                    // TODO: trigger in-game HUD overlay
                }));

        ClientPlayNetworking.registerGlobalReceiver(ZoneExitS2CPacket.ID, (pkt, ctx) ->
                ctx.client().execute(() -> {
                    LOGGER.debug("ZONE_EXIT domain={}", pkt.domainId());
                    // TODO: remove HUD overlay
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
    }
}
