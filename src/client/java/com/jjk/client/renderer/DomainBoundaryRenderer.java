package com.jjk.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class DomainBoundaryRenderer {

    public record DomainRenderState(UUID domainId, long activatedAtMs) {}

    private static final ConcurrentHashMap<UUID, DomainRenderState> ACTIVE_DOMAINS = new ConcurrentHashMap<>();

    public static void register() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(DomainBoundaryRenderer::render);
    }

    public static void activateDomain(UUID domainId) {
        ACTIVE_DOMAINS.put(domainId, new DomainRenderState(domainId, System.currentTimeMillis()));
    }

    public static void deactivateDomain(UUID domainId) {
        ACTIVE_DOMAINS.remove(domainId);
    }

    private static void render(WorldRenderContext ctx) {
        if (ACTIVE_DOMAINS.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getCurrentFps() < 17) return; // skip render under low frame rate

        for (DomainRenderState state : ACTIVE_DOMAINS.values()) {
            renderDomain(ctx, state);
        }
    }

    private static void renderDomain(WorldRenderContext ctx, DomainRenderState state) {
        // TODO: AzureLib overlay integration
        // Render a translucent boundary sphere at the domain origin.
        // AzureLib 3.0.x overlay hook goes here when the dependency is enabled.
    }
}
