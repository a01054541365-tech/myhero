package com.jjk.client.hud;

import com.jjk.network.s2c.EntityHealthSyncS2CPacket;
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
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * 반경 32블록 이내 JJK 주령 엔티티 머리 위에 HP바 표시.
 * EntityHealthSyncS2CPacket(period=10)으로 갱신.
 * WorldRenderEvents.AFTER_ENTITIES에 등록 — 빌보딩(카메라 방향 회전) 적용.
 */
@Environment(EnvType.CLIENT)
public final class EntityHealthBarRenderer {

    private static final Map<Integer, EntityHealthData> healthCache = new HashMap<>();

    private record EntityHealthData(float hp, float hpMax, String name, boolean isBoss) {}

    public static void onPacket(EntityHealthSyncS2CPacket pkt) {
        if (pkt.hpCurrent() <= 0f) {
            healthCache.remove(pkt.entityId());
        } else {
            healthCache.put(pkt.entityId(),
                new EntityHealthData(pkt.hpCurrent(), pkt.hpMax(), pkt.displayName(), pkt.isBoss()));
        }
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(EntityHealthBarRenderer::render);
    }

    private static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || healthCache.isEmpty()) return;

        MatrixStack matrices = ctx.matrixStack();
        if (matrices == null) return;
        Camera camera = ctx.camera();
        Vec3d camPos = camera.getPos();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        Tessellator tess = Tessellator.getInstance();
        int rendered = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (rendered >= 20) break;
            EntityHealthData hd = healthCache.get(entity.getId());
            if (hd == null || hd.hpMax() <= 0f) continue;
            if (entity.squaredDistanceTo(mc.player) > 1024.0) continue; // 32블록²

            Vec3d pos = entity.getPos().add(0.0, entity.getHeight() + 0.3, 0.0);
            matrices.push();
            matrices.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
            matrices.multiply(camera.getRotation()); // 빌보딩
            matrices.scale(0.04f, -0.04f, 0.04f);

            Matrix4f mat = matrices.peek().getPositionMatrix();
            float ratio = Math.max(0f, Math.min(1f, hd.hp() / hd.hpMax()));
            float hw = 30f; // half-width
            float bh = 3f;  // half-height

            // 배경 (반투명 검정)
            BufferBuilder bg = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            bg.vertex(mat, -hw, -bh, 0f).color(0, 0, 0, 180);
            bg.vertex(mat, -hw,  bh, 0f).color(0, 0, 0, 180);
            bg.vertex(mat,  hw,  bh, 0f).color(0, 0, 0, 180);
            bg.vertex(mat,  hw, -bh, 0f).color(0, 0, 0, 180);
            BufferRenderer.drawWithGlobalProgram(bg.end());

            // HP 채움 — 보스(特級)는 금색, 일반은 녹색
            float fillRight = -hw + 1f + (hw * 2f - 2f) * ratio;
            int r = hd.isBoss() ? 255 : 0;
            int g = hd.isBoss() ? 215 : 220;
            int b = 0;
            if (ratio > 0f) {
                BufferBuilder fill = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                fill.vertex(mat, -hw + 1f, -bh + 1f, 0f).color(r, g, b, 220);
                fill.vertex(mat, -hw + 1f,  bh - 1f, 0f).color(r, g, b, 220);
                fill.vertex(mat, fillRight,  bh - 1f, 0f).color(r, g, b, 220);
                fill.vertex(mat, fillRight, -bh + 1f, 0f).color(r, g, b, 220);
                BufferRenderer.drawWithGlobalProgram(fill.end());
            }

            matrices.pop();
            rendered++;
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
