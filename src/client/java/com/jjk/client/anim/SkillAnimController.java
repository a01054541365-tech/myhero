package com.jjk.client.anim;

import com.jjk.anim.AnimationRegistry;
import dev.kosmx.playerAnim.api.IPlayable;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayDeque;
import java.util.Deque;

@Environment(EnvType.CLIENT)
public class SkillAnimController {

    private static final SkillAnimController INSTANCE = new SkillAnimController();

    public static SkillAnimController getInstance() { return INSTANCE; }

    public enum AnimState {
        IDLE, SKILL_STARTUP, SKILL_ACTIVE, SKILL_RECOVERY, BLACKFLASH, DOMAIN_CAST, STUNNED
    }

    private static final int PRIO_BLACKFLASH_DEATH = 4;
    private static final int PRIO_HIT_DOMAIN       = 3;
    private static final int PRIO_SKILL            = 2;
    private static final int PRIO_CC               = 1;
    private static final int PRIO_IDLE             = 0;

    // Queue of pending animIds, max 3; excess lowest-priority item is dropped
    private final Deque<Byte> animQueue = new ArrayDeque<>(3);

    private static int priorityOf(byte animId) {
        return switch (animId) {
            case 5, 9, 56 -> PRIO_BLACKFLASH_DEATH;     // death, black_flash, black_flash_great
            case 2, 7, 8, 19, 35 -> PRIO_HIT_DOMAIN;    // hit, domain_start/loop, gojo_domain, mahito_domain
            case 0 -> PRIO_IDLE;
            default -> PRIO_SKILL;
        };
    }

    private synchronized void enqueue(byte animId) {
        if (animQueue.size() >= 3) {
            // drop lowest-priority item
            byte minItem = animId;
            int minPrio = priorityOf(animId);
            for (byte b : animQueue) {
                if (priorityOf(b) < minPrio) {
                    minPrio = priorityOf(b);
                    minItem = b;
                }
            }
            if (minItem == animId) return; // incoming is the lowest, drop it
            animQueue.remove(minItem);
        }
        animQueue.addLast(animId);
    }

    public void onAnimTrigger(byte animId) {
        enqueue(animId);
        // Process immediately for local player — actual PlayerAnimator call happens in render thread
        // The queue is drained by the next registerAll-registered factory tick
    }

    public void onAnimTriggerForOther(AbstractClientPlayerEntity entity, byte animId) {
        playAnim(entity, animId);
    }

    private void playAnim(AbstractClientPlayerEntity player, byte animId) {
        String animName = AnimationRegistry.get(animId);
        Identifier id = Identifier.of("jjk", animName);

        IPlayable playable = PlayerAnimationRegistry.getAnimation(id);
        if (playable == null) return;

        PlayerAnimationAccess.PlayerAssociatedAnimationData data =
                PlayerAnimationAccess.getPlayerAssociatedData(player);
        IAnimation layer = data.get(id);
        if (!(layer instanceof ModifierLayer<?>)) return;

        @SuppressWarnings("unchecked")
        ModifierLayer<IAnimation> modLayer = (ModifierLayer<IAnimation>) layer;

        IAnimation anim = (IAnimation) playable.playAnimation();
        modLayer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(5, Ease.LINEAR),
                anim
        );
    }

    public void registerAll() {
        for (int i = 0; i <= 59; i++) {
            String animName = AnimationRegistry.get(i);
            Identifier id = Identifier.of("jjk", animName);
            final int prio = priorityOf((byte) i);
            PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                    id, prio, player -> new ModifierLayer<>()
            );
        }
    }
}
