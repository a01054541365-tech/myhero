package com.jjk.client.costume;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class CostumeRenderLayer
        extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    private final FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx;

    public CostumeRenderLayer(
            FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
        this.ctx = context;
    }

    @Override
    public void render(MatrixStack matrices,
                       VertexConsumerProvider vcp,
                       int light,
                       AbstractClientPlayerEntity player,
                       float limbAngle, float limbDistance,
                       float tickDelta, float animProgress,
                       float headYaw, float headPitch) {

        UUID uuid = player.getUuid();
        String costumeId = CostumeClientCache.get(uuid);
        if (costumeId == null || "default".equals(costumeId)) return;

        Identifier texture = Identifier.of("jjk", "textures/costume/" + costumeId + ".png");
        VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
        ctx.getModel().render(matrices, vc, light, OverlayTexture.DEFAULT_UV, 0xFFFFFFFF);
    }
}
