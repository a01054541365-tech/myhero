package com.jjk.client.hud;

import com.jjk.network.s2c.CEAuraSyncS2CPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * 다른 플레이어 머리 위에 CE 비율 기반 오라 링 렌더링.
 * CEAuraSyncS2CPacket(period=10)으로 수신된 데이터 사용.
 * WorldRenderEvents.AFTER_ENTITIES — HP바보다 위에 표시.
 */
@Environment(EnvType.CLIENT)
public final class CEAuraRenderer {

    private static final Map<Integer, Float> ceCache = new HashMap<>();
    private static final float RING_INNER_RADIUS = 0.6f;
    private static final float RING_OUTER_RADIUS = 0.8f;
    private static final int RING_SEGMENTS = 20;
    private static final float MAX_DIST_SQ = 30f * 30f;

    private CEAuraRenderer() {}

    public static void onPacket(CEAuraSyncS2CPacket pkt) {
        ceCache.clear();
        ceCache.putAll(pkt.entityCePercent());
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(CEAuraRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || ceCache.isEmpty()) return;

        MatrixStack matrices = ctx.matrixStack();
        if (matrices == null) return;
        Camera camera = ctx.camera();
        Vec3d camPos = camera.getPos();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        Tessellator tess = Tessellator.getInstance();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            Float cePercent = ceCache.get(player.getId());
            if (cePercent == null || cePercent <= 0f) continue;
            if (player.squaredDistanceTo(mc.player) > MAX_DIST_SQ) continue;

            float ratio = Math.min(1f, Math.max(0f, cePercent));

            // CE > 30%: 청색, <= 30%: 붉은색
            int r, g, b;
            if (ratio > 0.30f) {
                r = 30; g = 100; b = 255;
            } else {
                r = 255; g = 50; b = 50;
            }
            // 두께: CE 비율에 비례 (inner 고정, outer 비례)
            float outerR = RING_INNER_RADIUS + (RING_OUTER_RADIUS - RING_INNER_RADIUS) * ratio;
            float innerR = RING_INNER_RADIUS;
            int alpha = (int)(160 * ratio + 60 * (1f - ratio));

            // 엔티티 머리 위 (HP바보다 위: height + 0.7)
            Vec3d pos = player.getPos().add(0.0, player.getHeight() + 0.7, 0.0);
            double dx = pos.x - camPos.x;
            double dy = pos.y - camPos.y;
            double dz = pos.z - camPos.z;

            matrices.push();
            matrices.translate(dx, dy, dz);
            Matrix4f mat = matrices.peek().getPositionMatrix();

            // 수평 링: 사다리꼴 QUADS
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            for (int i = 0; i < RING_SEGMENTS; i++) {
                double a1 = 2.0 * Math.PI * i       / RING_SEGMENTS;
                double a2 = 2.0 * Math.PI * (i + 1) / RING_SEGMENTS;
                float x1i = (float)(innerR * Math.cos(a1));
                float z1i = (float)(innerR * Math.sin(a1));
                float x1o = (float)(outerR * Math.cos(a1));
                float z1o = (float)(outerR * Math.sin(a1));
                float x2o = (float)(outerR * Math.cos(a2));
                float z2o = (float)(outerR * Math.sin(a2));
                float x2i = (float)(innerR * Math.cos(a2));
                float z2i = (float)(innerR * Math.sin(a2));

                buf.vertex(mat, x1i, 0f, z1i).color(r, g, b, alpha);
                buf.vertex(mat, x1o, 0f, z1o).color(r, g, b, alpha);
                buf.vertex(mat, x2o, 0f, z2o).color(r, g, b, alpha);
                buf.vertex(mat, x2i, 0f, z2i).color(r, g, b, alpha);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());

            matrices.pop();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
