package com.jjk.client.anim;

import com.jjk.anim.AnimationRegistry;
import com.jjk.network.s2c.AwakeningS2CPacket;
import com.jjk.network.s2c.SkillResultS2CPacket;
import com.jjk.network.s2c.ZoneEnterS2CPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public final class SkillAnimController {
    private SkillAnimController() {}

    private static final List<PendingPlay> PENDING = new ArrayList<>();

    private static class PendingPlay {
        final UUID playerUuid;
        final String animName;
        final boolean loop;
        int ticksLeft;

        PendingPlay(UUID uuid, String name, boolean loop, int ticks) {
            playerUuid = uuid;
            animName = name;
            this.loop = loop;
            ticksLeft = ticks;
        }
    }

    /** AnimationTriggerS2CPacket 수신 시 호출 */
    public static void onAnimTrigger(AbstractClientPlayerEntity player, byte animId) {
        if (!AnimationRegistry.has(animId)) return;
        String animName = AnimationRegistry.get(animId);
        if (animId == 7) {
            // domain_start → 3틱 후 domain_loop LOOP 재생
            PlayerAnimationDispatcher.play(player, animName, false);
            PENDING.add(new PendingPlay(player.getUuid(), AnimationRegistry.get(8), true, 3));
        } else {
            boolean loop = (animId == 8 || animId == 0);
            PlayerAnimationDispatcher.play(player, animName, loop);
        }
    }

    /** SkillResultS2CPacket 수신 시 호출 (로컬 플레이어 전용)
     *  packet에 characterId/playerUuid 없으므로 result 필드로만 분기 */
    public static void onSkillResult(SkillResultS2CPacket pkt, AbstractClientPlayerEntity player) {
        if (pkt.result().contains("BLACK_FLASH")) {
            PlayerAnimationDispatcher.play(player, AnimationRegistry.get(9), false);   // common_black_flash
        } else if (pkt.result().equals("SUCCESS")) {
            PlayerAnimationDispatcher.play(player, AnimationRegistry.get(1), false);   // common_cast
        }
    }

    /** ZoneEnterS2CPacket 수신 시 호출 */
    public static void onZoneEnter(ZoneEnterS2CPacket pkt, AbstractClientPlayerEntity player) {
        PlayerAnimationDispatcher.play(player, AnimationRegistry.get(58), false);  // zone_enter
    }

    /** AwakeningS2CPacket 수신 시 호출 */
    public static void onAwakening(AwakeningS2CPacket pkt, AbstractClientPlayerEntity player) {
        if (pkt.active()) {
            PlayerAnimationDispatcher.play(player, AnimationRegistry.get(57), false);  // awakening_enter
        } else {
            PlayerAnimationDispatcher.stop(player);
        }
    }

    /** JJKModClient.ClientTickEvents.END_CLIENT_TICK 에서 호출 */
    public static void tick(MinecraftClient mc) {
        if (mc.world == null || PENDING.isEmpty()) return;
        Iterator<PendingPlay> it = PENDING.iterator();
        while (it.hasNext()) {
            PendingPlay pp = it.next();
            if (--pp.ticksLeft <= 0) {
                mc.world.getPlayers().stream()
                        .filter(p -> p.getUuid().equals(pp.playerUuid))
                        .findFirst()
                        .ifPresent(p -> PlayerAnimationDispatcher.play(p, pp.animName, pp.loop));
                it.remove();
            }
        }
    }
}
