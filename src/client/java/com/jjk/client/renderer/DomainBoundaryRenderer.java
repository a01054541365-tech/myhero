package com.jjk.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class DomainBoundaryRenderer {

    public record DomainRenderState(
            UUID domainId,
            String domainName,
            long activatedAtMs,
            Vec3d center,
            float radius,
            boolean isOpen
    ) {}

    private static final ConcurrentHashMap<UUID, DomainRenderState> ACTIVE_DOMAINS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> PARTICLE_TIMESTAMPS = new ConcurrentHashMap<>();

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(DomainBoundaryRenderer::render);
    }

    // Spec-required signature
    public static void activateDomain(UUID domainId, Vec3d center, float radius, boolean isOpen) {
        activateDomain(domainId, "", center, radius, isOpen);
    }

    // Extended with domainName for color lookup — called from JJKModClient
    public static void activateDomain(UUID domainId, String domainName, Vec3d center, float radius, boolean isOpen) {
        ACTIVE_DOMAINS.put(domainId,
                new DomainRenderState(domainId, domainName, System.currentTimeMillis(), center, radius, isOpen));
    }

    public static void deactivateDomain(UUID domainId) {
        ACTIVE_DOMAINS.remove(domainId);
        PARTICLE_TIMESTAMPS.remove(domainId);
    }

    private static void render(WorldRenderContext ctx) {
        if (ACTIVE_DOMAINS.isEmpty()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getCurrentFps() < 17) return;
        for (DomainRenderState state : ACTIVE_DOMAINS.values()) {
            renderDomain(ctx, state);
        }
    }

    private static void renderDomain(WorldRenderContext ctx, DomainRenderState state) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        Vec3d camPos = ctx.camera().getPos();
        MatrixStack matrixStack = ctx.matrixStack();
        if (matrixStack == null) return;

        if (state.isOpen()) {
            // Sukuna open domain: ground particle ring only
            long nowMs = System.currentTimeMillis();
            long lastMs = PARTICLE_TIMESTAMPS.getOrDefault(state.domainId(), 0L);
            if (nowMs - lastMs >= 250L) {
                PARTICLE_TIMESTAMPS.put(state.domainId(), nowMs);
                int count = 24;
                float rad = state.radius();
                for (int i = 0; i < count; i++) {
                    double angle = 2.0 * Math.PI * i / count;
                    double px = state.center().x + rad * Math.cos(angle);
                    double pz = state.center().z + rad * Math.sin(angle);
                    mc.world.addParticle(ParticleTypes.PORTAL, px, state.center().y, pz, 0.0, 0.05, 0.0);
                }
            }
            return;
        }

        // Closed domain: translucent wireframe sphere with 2-second fade-in
        long elapsed = System.currentTimeMillis() - state.activatedAtMs();
        float fadeT = Math.min(1.0f, elapsed / 2000.0f);

        float[] target = getDomainColor(state.domainName());
        int ri = clamp((int)((1.0f + fadeT * (target[0] - 1.0f)) * 255));
        int gi = clamp((int)((1.0f + fadeT * (target[1] - 1.0f)) * 255));
        int bi = clamp((int)((1.0f + fadeT * (target[2] - 1.0f)) * 255));
        int ai = 64; // 0.25 * 255

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        matrixStack.push();
        matrixStack.translate(
                state.center().x - camPos.x,
                state.center().y - camPos.y,
                state.center().z - camPos.z
        );
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();

        float rad = state.radius();
        int divisions = 32;
        Tessellator tessellator = Tessellator.getInstance();

        // Latitude lines (horizontal circles at each latitude step)
        for (int lat = 1; lat < divisions; lat++) {
            float phi = (float)(Math.PI * lat / divisions);
            float y = rad * (float)Math.cos(phi);
            float rXZ = rad * (float)Math.sin(phi);
            BufferBuilder buf = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (int lon = 0; lon < divisions; lon++) {
                float theta1 = (float)(2.0 * Math.PI * lon / divisions);
                float theta2 = (float)(2.0 * Math.PI * (lon + 1) / divisions);
                buf.vertex(matrix, rXZ * (float)Math.cos(theta1), y, rXZ * (float)Math.sin(theta1))
                   .color(ri, gi, bi, ai);
                buf.vertex(matrix, rXZ * (float)Math.cos(theta2), y, rXZ * (float)Math.sin(theta2))
                   .color(ri, gi, bi, ai);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        // Longitude lines (meridians at each longitude step)
        for (int lon = 0; lon < divisions; lon++) {
            float theta = (float)(2.0 * Math.PI * lon / divisions);
            BufferBuilder buf = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (int lat = 0; lat < divisions; lat++) {
                float phi1 = (float)(Math.PI * lat / divisions);
                float phi2 = (float)(Math.PI * (lat + 1) / divisions);
                float sinP1 = (float)Math.sin(phi1), cosP1 = (float)Math.cos(phi1);
                float sinP2 = (float)Math.sin(phi2), cosP2 = (float)Math.cos(phi2);
                float cosT = (float)Math.cos(theta), sinT = (float)Math.sin(theta);
                buf.vertex(matrix, rad * sinP1 * cosT, rad * cosP1, rad * sinP1 * sinT)
                   .color(ri, gi, bi, ai);
                buf.vertex(matrix, rad * sinP2 * cosT, rad * cosP2, rad * sinP2 * sinT)
                   .color(ri, gi, bi, ai);
            }
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        matrixStack.pop();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static float[] getDomainColor(String domainName) {
        String n = domainName.toLowerCase();
        if (n.contains("gojo"))          return new float[]{0.2f, 0.4f,  1.0f};
        if (n.contains("mahito"))        return new float[]{0.5f, 0.0f,  0.8f};
        if (n.contains("itadori"))       return new float[]{1.0f, 0.0f,  0.0f};
        if (n.contains("megumi"))        return new float[]{0.1f, 0.1f,  0.1f};
        if (n.contains("jogo"))          return new float[]{1.0f, 0.5f,  0.0f};
        if (n.contains("hakari_jackpot"))return new float[]{1.0f, 0.85f, 0.0f};
        if (n.contains("hakari"))        return new float[]{1.0f, 1.0f,  0.0f};
        return new float[]{1.0f, 1.0f, 1.0f};
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
