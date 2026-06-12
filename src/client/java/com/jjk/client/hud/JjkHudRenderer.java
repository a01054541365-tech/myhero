package com.jjk.client.hud;

import com.jjk.client.JjkClientState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class JjkHudRenderer {

    public static final JjkHudRenderer INSTANCE = new JjkHudRenderer();
    private JjkHudRenderer() {}

    private boolean chanting = false;
    private int chantTicks = 0;
    private float lastDamage = 0f;
    private int damageDisplayTicks = 0;

    // 흑섬 Just Frame 타이밍 게이지 (BlackFlashTimingS2CPacket 수신 시 갱신)
    private boolean showTimingGauge = false;
    private long timingWindowStart = 0L;
    private long timingWindowEnd = 0L;

    // 히구루마 재판 판결 안내 (VerdictS2CPacket 수신 시 갱신, Phase I-2)
    private Text verdictMessage = null;
    private int  verdictDisplayTicks = 0;

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
        renderTimingGauge(context, client);
        renderSealIndicator(context, client, worldTick);
        if (damageDisplayTicks > 0) {
            renderDamageNumber(context, client);
            damageDisplayTicks--;
        }
        if (verdictDisplayTicks > 0) {
            renderVerdictMessage(context, client);
            verdictDisplayTicks--;
        }
    }

    /** 매 클라이언트 틱 호출 (ClientTickEvents.END_CLIENT_TICK에서). */
    public void tick(MinecraftClient client) {
        ceBar.tick();
    }

    private void renderHealthBar(DrawContext context, MinecraftClient client) {
        int hp    = JjkClientState.getHp();
        int hpMax = JjkClientState.getHpMax();
        float hpPercent = hpMax > 0 ? (float) hp / hpMax : 1f;

        int color = getHealthColor(hpPercent);
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();

        // 바닐라 갑옷/공기 줄(screenH-49)과 겹치지 않도록 그 위에 배치
        int barX = screenW / 2 - 91;
        int barY = screenH - 55;
        int barW = (int)(182 * hpPercent);

        // 전투 중이면 붉은 테두리로 구분
        if (JjkClientState.isInCombat()) {
            context.fill(barX - 1, barY - 1, barX + 183, barY + 6, 0xFFCC3333);
        }
        context.fill(barX, barY, barX + 182, barY + 5, 0xFF333333);
        if (barW > 0) context.fill(barX, barY, barX + barW, barY + 5, color);
    }

    /** HP 비율별 색상 — 단계 경계에서 부드럽게 lerp: 빨강→주황→노랑→초록. */
    private int getHealthColor(float ratio) {
        if (ratio >= 0.75f) return 0xFF55FF55;
        if (ratio >= 0.50f) return lerpColor(0xFFFF55, 0x55FF55, (ratio - 0.50f) / 0.25f);
        if (ratio >= 0.25f) return lerpColor(0xFF9900, 0xFFFF55, (ratio - 0.25f) / 0.25f);
        return lerpColor(0xFF4444, 0xFF9900, Math.max(0f, ratio / 0.25f));
    }

    private static int lerpColor(int from, int to, float t) {
        int r = (int)(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int)(((from >> 8)  & 0xFF) + (((to >> 8)  & 0xFF) - ((from >> 8)  & 0xFF)) * t);
        int b = (int)((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
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

    private void renderTimingGauge(DrawContext context, MinecraftClient client) {
        if (!showTimingGauge || client.world == null) return;
        long currentTick = client.world.getTime();
        long windowLen = timingWindowEnd - timingWindowStart;
        if (windowLen <= 0L || currentTick >= timingWindowEnd) {
            showTimingGauge = false;
            return;
        }
        float progress = (float)(timingWindowEnd - currentTick) / windowLen;
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        int barW = 120;
        int barH = 8;
        int x = (sw - barW) / 2;
        int y = sh / 2 + 30;
        context.fill(x - 1, y - 1, x + barW + 1, y + barH + 1, 0xAA000000);
        int fillW = (int)(barW * progress);
        if (fillW > 0) context.fill(x, y, x + fillW, y + barH, 0xFF00E5FF);
        context.drawCenteredTextWithShadow(client.textRenderer,
            Text.literal("§b흑섬 Just Frame!"), sw / 2, y - 12, 0xFFFFFF);
    }

    /** 단일-스킬 봉인 표시 — 화면 상단 중앙에 봉인된 스킬 ID와 남은 시간(초) 표시 */
    private void renderSealIndicator(DrawContext context, MinecraftClient client, long worldTick) {
        if (!JjkClientState.isSkillSealed(worldTick)) return;
        int remainTicks = (int) Math.max(0, JjkClientState.getSealExpireAtTick() - worldTick);
        int remainSec = (remainTicks + 19) / 20;
        int screenW = client.getWindow().getScaledWidth();

        context.drawCenteredTextWithShadow(
            client.textRenderer,
            Text.literal("§c[봉인] " + JjkClientState.getSealedSkillId() + " (" + remainSec + "s)"),
            screenW / 2, 4,
            0xFFFF5555
        );
    }

    private void renderVerdictMessage(DrawContext context, MinecraftClient client) {
        if (verdictMessage == null) return;
        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        context.drawCenteredTextWithShadow(
            client.textRenderer,
            verdictMessage,
            screenW / 2, screenH / 2 - 50,
            0xFFFFFFFF
        );
    }

    /** 재판 판결 통지 — 2초(40틱) 동안 화면 중앙에 표시. */
    public void showVerdict(boolean guilty, String sealedSkillId) {
        this.verdictMessage = guilty
                ? Text.literal("§c유죄 판결! " + sealedSkillId + " 술식이 봉인됩니다.")
                : Text.literal("§a무죄 판결.");
        this.verdictDisplayTicks = 40;
    }

    public void onBlackFlashTiming(boolean show, int startTick, int endTick) {
        showTimingGauge = show;
        timingWindowStart = startTick;
        timingWindowEnd = endTick;
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
