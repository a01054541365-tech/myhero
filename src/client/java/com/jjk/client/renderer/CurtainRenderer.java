package com.jjk.client.renderer;

import com.jjk.network.s2c.CurtainEnterS2CPacket;
import com.jjk.network.s2c.CurtainExitS2CPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 장막(Curtain) 경계를 반투명 남색 와이어프레임 구체로 렌더링. */
@Environment(EnvType.CLIENT)
public final class CurtainRenderer {
    private CurtainRenderer() {}

    public record CurtainClientData(Vec3d center, int radius) {}

    private static final ConcurrentHashMap<UUID, CurtainClientData> ACTIVE = new ConcurrentHashMap<>();

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(CurtainRenderer::render);
    }

    public static void onEnter(CurtainEnterS2CPacket packet, MinecraftClient client) {
        if (client.player == null) return;
        Vec3d center = client.player.getPos();
        ACTIVE.put(packet.curtainOwnerId(), new CurtainClientData(center, packet.radius()));
    }

    public static void onExit(CurtainExitS2CPacket packet) {
        ACTIVE.remove(packet.curtainOwnerId());
    }

    private static void render(WorldRenderContext ctx) {
        if (ACTIVE.isEmpty()) return;
        for (CurtainClientData c : ACTIVE.values()) {
            renderDome(ctx, c.center(), c.radius());
        }
    }

    private static void renderDome(WorldRenderContext ctx, Vec3d center, int radius) {
        MatrixStack matrices = ctx.matrixStack();
        if (matrices == null) return;

        Vec3d camPos = ctx.camera().getPos();
        int ri = 0, gi = 0, bi = 170, ai = 50; // 0x330000AA

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        matrices.push();
        matrices.translate(
            center.x - camPos.x,
            center.y - camPos.y,
            center.z - camPos.z
        );
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        float rad = (float) radius;
        int divisions = 16;
        Tessellator tessellator = Tessellator.getInstance();

        // 위도선 (수평 원호) — 상반구만
        for (int lat = 1; lat < divisions / 2; lat++) {
            float phi = (float)(Math.PI * lat / divisions);
            float y = rad * (float)Math.cos(phi);
            float rXZ = rad * (float)Math.sin(phi);
            BufferBuilder buf = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (int lon = 0; lon < divisions; lon++) {
                float t1 = (float)(2.0 * Math.PI * lon / divisions);
                float t2 = (float)(2.0 * Math.PI * (lon + 1) / divisions);
                buf.vertex(matrix, rXZ * (float)Math.cos(t1), y, rXZ * (float)Math.sin(t1)).color(ri, gi, bi, ai);
                buf.vertex(matrix, rXZ * (float)Math.cos(t2), y, rXZ * (float)Math.sin(t2)).color(ri, gi, bi, ai);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        // 경도선 (수직 반원)
        for (int lon = 0; lon < divisions; lon++) {
            float theta = (float)(2.0 * Math.PI * lon / divisions);
            float cosT = (float)Math.cos(theta), sinT = (float)Math.sin(theta);
            BufferBuilder buf = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (int lat = 0; lat < divisions / 2; lat++) {
                float p1 = (float)(Math.PI * lat / divisions);
                float p2 = (float)(Math.PI * (lat + 1) / divisions);
                buf.vertex(matrix, rad * (float)Math.sin(p1) * cosT, rad * (float)Math.cos(p1), rad * (float)Math.sin(p1) * sinT).color(ri, gi, bi, ai);
                buf.vertex(matrix, rad * (float)Math.sin(p2) * cosT, rad * (float)Math.cos(p2), rad * (float)Math.sin(p2) * sinT).color(ri, gi, bi, ai);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        // 바닥 원
        BufferBuilder base = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int lon = 0; lon < divisions; lon++) {
            float t1 = (float)(2.0 * Math.PI * lon / divisions);
            float t2 = (float)(2.0 * Math.PI * (lon + 1) / divisions);
            base.vertex(matrix, rad * (float)Math.cos(t1), 0f, rad * (float)Math.sin(t1)).color(ri, gi, bi, ai);
            base.vertex(matrix, rad * (float)Math.cos(t2), 0f, rad * (float)Math.sin(t2)).color(ri, gi, bi, ai);
        }
        BufferRenderer.drawWithGlobalProgram(base.end());

        matrices.pop();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
