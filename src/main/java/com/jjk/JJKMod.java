package com.jjk;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.MinecraftServer;
import com.jjk.awakening.AwakeningManager;
import com.jjk.burden.BurdenManager;
import com.jjk.ce.CEManager;
import com.jjk.character.SkillRegistry;
import com.jjk.character.impl.*;
import com.jjk.combat.CombatPipeline;
import com.jjk.data.PlayerRepository;
import com.jjk.domain.DomainManager;
import com.jjk.effect.EffectDeferQueue;
import com.jjk.finger.FingerSystem;
import com.jjk.network.Packets;
import com.jjk.respawn.RespawnManager;
import com.jjk.team.TeamManager;
import com.jjk.tick.TickScheduler;
import com.jjk.trial.TrialManager;
import com.jjk.zone.ZoneStateManager;

import java.nio.file.Path;

public class JJKMod implements ModInitializer {

    public static final String MOD_ID = "jjk";
    private static final Logger LOGGER = LoggerFactory.getLogger("jjk");
    private static JJKMod INSTANCE;

    private JjkConfig config;
    private PlayerRepository playerRepository;
    private CEManager ceManager;
    private AwakeningManager awakeningManager;
    private DomainManager domainManager;
    private TeamManager teamManager;
    private ZoneStateManager zoneStateManager;
    private BurdenManager burdenManager;
    private TrialManager trialManager;
    private FingerSystem fingerSystem;
    private RespawnManager respawnManager;
    private EffectDeferQueue effectDeferQueue;
    private CombatPipeline combatPipeline;
    private TickScheduler tickScheduler;

    @Override
    public void onInitialize() {
        INSTANCE = this;

        config = JjkConfig.load();
        playerRepository = new PlayerRepository();
        teamManager = new TeamManager();
        ceManager = new CEManager(config);
        awakeningManager = new AwakeningManager(config);
        domainManager = new DomainManager(config);
        zoneStateManager = new ZoneStateManager();
        burdenManager = new BurdenManager(config);
        trialManager = new TrialManager(config);
        fingerSystem = new FingerSystem(config);
        respawnManager = new RespawnManager(config);
        effectDeferQueue = new EffectDeferQueue();
        combatPipeline = new CombatPipeline();

        SkillRegistry.register("gojo",    new GojoSkillSet());
        SkillRegistry.register("itadori", new ItadoriSkillSet());
        SkillRegistry.register("megumi",  new MegumiSkillSet());
        SkillRegistry.register("okkotsu", new OkkotsuSkillSet());
        SkillRegistry.register("sukuna",  new SukunaSkillSet());
        SkillRegistry.register("mahito",  new MahitoSkillSet());
        SkillRegistry.register("jogo",    new JogoSkillSet());
        SkillRegistry.register("hakari",  new HakariSkillSet());
        SkillRegistry.register("inumaki", new InumakiSkillSet());
        SkillRegistry.register("nanami",  new NanamiSkillSet());

        tickScheduler = new TickScheduler();
        tickScheduler.register(ceManager::regenTick, 1);
        tickScheduler.register(awakeningManager::tickCheck, 1);
        tickScheduler.register(zoneStateManager::tick, 1);
        tickScheduler.register(burdenManager::tick, 1);
        tickScheduler.register(domainManager::tickDomains, 1);
        tickScheduler.register(effectDeferQueue::tick, 1);
        tickScheduler.register(HakariSkillSet::tickJackpot, 1);
        tickScheduler.register(NanamiSkillSet::tickRCT, 10);

        Packets.register();

        // ???삼쭕?딄텕 筌?쑵????쎄텢 ?귐딅뮞?? fail-open, ???삼쭕?딄텕 筌?Ŧ??怨뺤춸 筌ｌ꼶??        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            try {
                String text = message.getContent().getString();
                com.jjk.data.PlayerData senderData = playerRepository.load(sender.getUuid());
                if (!"inumaki".equals(senderData.characterId)) return true;
                com.jjk.api.skill.ISkillSet skillSet = com.jjk.character.SkillRegistry.get("inumaki");
                if (skillSet instanceof InumakiSkillSet inumaki) {
                    inumaki.handleChat(sender, text);
                }
            } catch (Exception e) {
                LOGGER.warn("Inumaki chat skill error", e);
            }
            return true; // fail-open: ??湲?筌?쑵????됱뒠
        });

        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                playerRepository.evict(handler.player.getUuid()));
    }

    private void onServerStarting(MinecraftServer server) {
        // world/jjk/player_data.db ??spec 吏?3 path
        Path dbPath = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                .resolve("jjk").resolve("player_data.db");
        playerRepository.init(dbPath);
    }

    private void onServerStopping(MinecraftServer server) {
        playerRepository.close();
    }

    public static JJKMod getInstance() { return INSTANCE; }
    public static JjkConfig getConfig() { return INSTANCE.config; }
    public static PlayerRepository getPlayerRepository() { return INSTANCE.playerRepository; }
    public static CEManager getCEManager() { return INSTANCE.ceManager; }
    public static AwakeningManager getAwakeningManager() { return INSTANCE.awakeningManager; }
    public static DomainManager getDomainManager() { return INSTANCE.domainManager; }
    public static TeamManager getTeamManager() { return INSTANCE.teamManager; }
    public static ZoneStateManager getZoneStateManager() { return INSTANCE.zoneStateManager; }
    public static BurdenManager getBurdenManager() { return INSTANCE.burdenManager; }
    public static TrialManager getTrialManager() { return INSTANCE.trialManager; }
    public static FingerSystem getFingerSystem() { return INSTANCE.fingerSystem; }
    public static RespawnManager getRespawnManager() { return INSTANCE.respawnManager; }
    public static EffectDeferQueue getEffectDeferQueue() { return INSTANCE.effectDeferQueue; }
    public static CombatPipeline getCombatPipeline() { return INSTANCE.combatPipeline; }
    public static TickScheduler getTickScheduler() { return INSTANCE.tickScheduler; }
}
