package com.jjk.bossbar;

import com.jjk.JJKMod;
import com.jjk.character.CharacterRegistry;
import com.jjk.data.PlayerData;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CeBossBarManager {

    private final Map<UUID, ServerBossBar> bars = new HashMap<>();

    public void onPlayerJoin(ServerPlayerEntity player) {
        ServerBossBar bar = new ServerBossBar(
                buildTitle(player),
                BossBar.Color.BLUE,
                BossBar.Style.PROGRESS);
        bar.addPlayer(player);
        bars.put(player.getUuid(), bar);
    }

    public void onPlayerLeave(UUID uuid) {
        ServerBossBar bar = bars.remove(uuid);
        if (bar != null) bar.clearPlayers();
    }

    public void tick(ServerPlayerEntity player) {
        ServerBossBar bar = bars.get(player.getUuid());
        if (bar == null) return;

        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        if (data.ceMax <= 0f) return;

        float ratio = Math.max(0f, Math.min(1f, data.ceCurrent / data.ceMax));
        bar.setPercent(ratio);
        bar.setColor(colorFor(ratio));
        bar.setName(buildTitle(player));
    }

    private static Text buildTitle(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        String charName = data.characterId != null
                ? CharacterRegistry.get(data.characterId).displayName()
                : "?";
        return Text.literal("주력 [" + charName + "]");
    }

    private static BossBar.Color colorFor(float ratio) {
        if (ratio >= 0.70f) return BossBar.Color.BLUE;
        if (ratio >= 0.40f) return BossBar.Color.GREEN;
        if (ratio >= 0.20f) return BossBar.Color.YELLOW;
        return BossBar.Color.RED;
    }
}
