package com.jjk.client.anim;

import com.jjk.anim.AnimationRegistry;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationJson;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public final class AnimationCache {
    private AnimationCache() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk-anim");
    private static final Map<String, KeyframeAnimation> CACHE = new HashMap<>();

    /** ClientLifecycleEvents.CLIENT_STARTED 에서 호출 */
    public static void loadAll(ResourceManager manager) {
        CACHE.clear();
        int attempted = 0;
        for (int id = 0; id <= 60; id++) {
            if (!AnimationRegistry.has(id)) continue;
            attempted++;
            String animName = AnimationRegistry.get(id);
            Identifier resId = Identifier.of("jjk", "animations/" + animName + ".animation.json");
            Optional<Resource> resource = manager.getResource(resId);
            if (resource.isEmpty()) continue;
            try (InputStreamReader reader = new InputStreamReader(resource.get().getInputStream())) {
                List<KeyframeAnimation> list = AnimationJson.GSON.fromJson(reader, AnimationJson.getListedTypeToken());
                if (list != null && !list.isEmpty()) {
                    CACHE.put(animName, list.get(0));
                }
            } catch (Exception e) {
                LOGGER.warn("[JJK] Failed to load animation: {}", animName, e);
            }
        }
        LOGGER.info("[JJK] AnimationCache loaded {}/{} animations", CACHE.size(), attempted);
    }

    /** 없으면 null 반환 (예외 throw 금지) */
    public static KeyframeAnimation get(String animName) {
        return CACHE.get(animName);
    }
}
