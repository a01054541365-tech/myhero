package com.jjk.client.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class JjkHudRenderer {

    public static final JjkHudRenderer INSTANCE = new JjkHudRenderer();
    private JjkHudRenderer() {}

    private boolean chanting = false;
    private int chantTicks = 0;
    private float lastDamage = 0f;
    private int damageDisplayTicks = 0;

    private final CEBarRenderer       ceBar        = new CEBarRenderer();
    private final SkillCooldownHUD    cooldownHud  = new SkillCooldownHUD();
    private final DomainIndicator     domainIndicator = new DomainIndicator();

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        long worldTick = client.world.getTime();

        renderHealthBar(context, client);
        ceBar.render(context, client);
        cooldownHud.render(context, client);
        domainIndicator.tick(worldTick);
        domainIndicator.render(context, client);
        renderChantingBar(context, client);
        if (damageDisplayTicks > 0) {
            renderDamageNumber(context, client);
            damageDisplayTicks--;
        }
    }

    /** 매 클라이언트 틱 호출 (ClientTickEvents.END_CLIENT_TICK에서). */
    public void tick(MinecraftClient client) {
        ceBar.tick();
    }

    private void renderHealthBar(DrawContext context, MinecraftClient client) {
        PlayerEntity player = client.player;
        float hp = player.getHealth();
        float maxHp = player.getMaxHealth();
        float hpRatio = maxHp > 0f ? hp / maxHp : 1f;

        int color = getHealthColor(hpRatio);
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        int barX = screenW / 2 - 91;
        int barY = screenH - 39;
        int barW = (int)(182 * hpRatio);

        context.fill(barX, barY, barX + 182, barY + 5, 0xFF333333);
        if (barW > 0) context.fill(barX, barY, barX + barW, barY + 5, color);
    }

    private int getHealthColor(float ratio) {
        if (ratio >= 0.70f) return 0xFF55FF55;
        if (ratio >= 0.40f) return 0xFFFFFF55;
        if (ratio >= 0.20f) return 0xFFFF9900;
        return 0xFFFF2222;
    }

    private void renderChantingBar(DrawContext context, MinecraftClient client) {
        if (!chanting || chantTicks <= 0) return;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        float ratio = Math.min(1.0f, chantTicks / 60f);

        int barW = 100;
        int barX = screenW / 2 - barW / 2;
        int barY = screenH / 2 + 20;

        context.fill(barX - 1, barY - 1, barX + barW + 1, barY + 7, 0xFF000000);
        int chantColor = ratio >= 1.0f ? 0xFFFFD700 : 0xFFFFFFFF;
        int filled = (int)(barW * ratio);
        if (filled > 0) context.fill(barX, barY, barX + filled, barY + 6, chantColor);

        context.drawCenteredTextWithShadow(
            client.textRenderer,
            Text.literal("영창"),
            screenW / 2, barY - 10,
            chantColor
        );
    }

    private void renderDamageNumber(DrawContext context, MinecraftClient client) {
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        context.drawCenteredTextWithShadow(
            client.textRenderer,
            Text.literal("§c" + (int) lastDamage),
            screenW / 2, screenH / 2 - 30,
            0xFFFF4444
        );
    }

    public void updateChanting(boolean chanting, int chantTicks) {
        this.chanting = chanting;
        this.chantTicks = chantTicks;
    }

    public void showDamage(float damage) {
        this.lastDamage = damage;
        this.damageDisplayTicks = 40;
    }

    public DomainIndicator getDomainIndicator() { return domainIndicator; }
}
