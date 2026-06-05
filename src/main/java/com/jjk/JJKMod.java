package com.jjk;

import com.jjk.bossbar.CeBossBarManager;
import com.jjk.dungeon.DungeonManager;
import com.jjk.economy.CursedStoneManager;
import com.jjk.entity.npc.NpcRegistry;
import com.jjk.server.AutoAnnouncer;
import com.jjk.server.WelcomeHandler;
import com.jjk.world.BuildingGenerator;
import com.jjk.world.JjkStructureBuilder;
import com.jjk.event.PotionUseHandler;
import com.jjk.item.GuideBookItem;
import com.jjk.chant.ChantingHandler;
import com.jjk.quest.QuestManager;
import com.jjk.curtain.CurtainManager;
import com.jjk.data.PlayerData;
import com.jjk.grade.GradeManager;
import com.jjk.character.CharacterCommandService;
import com.jjk.command.JjkCommandRegistry;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.server.MinecraftServer;
import com.jjk.audit.AuditLogger;
import com.jjk.awakening.AwakeningManager;
import com.jjk.burden.BurdenManager;
import com.jjk.ce.CEManager;
import com.jjk.character.CharacterRegistry;
import com.jjk.character.SkillRegistry;
import com.jjk.character.impl.*;
import com.jjk.network.s2c.CharacterInfoS2CPacket;
import com.jjk.network.s2c.CharacterSelectS2CPacket;
import com.jjk.combat.CombatPipeline;
import com.jjk.data.Migrator;
import com.jjk.data.PlayerRepository;
import com.jjk.domain.DomainManager;
import com.jjk.entity.CursedSpiritEntityTypes;
import com.jjk.entity.CursedSpiritSpawnManager;
import com.jjk.entity.ShikigamiEntityTypes;
import com.jjk.item.CostumeItemRegistry;
import com.jjk.item.CursedToolRegistry;
import com.jjk.effect.EffectDeferQueue;
import com.jjk.effect.FireEffectManager;
import com.jjk.finger.FingerSystem;
import com.jjk.network.Packets;
import com.jjk.respawn.RespawnManager;
import com.jjk.team.TeamManager;
import com.jjk.tick.TickScheduler;
import com.jjk.trial.TrialManager;
import com.jjk.zone.ComboTracker;
import com.jjk.zone.ZoneStateManager;

import java.nio.file.Path;
import java.util.Set;

public class JJKMod implements ModInitializer {

    public static final String MOD_ID = "jjk";
    private static final Logger LOGGER = LoggerFactory.getLogger("jjk");
    private static JJKMod INSTANCE;

    private JjkConfig config;
    private PlayerRepository playerRepository;
    private AuditLogger auditLogger;
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
    private ShadowMarkerRegistry shadowMarkerRegistry;
    private CeBossBarManager ceBossBarManager;
    private GradeManager gradeManager;
    private CurtainManager curtainManager;
    private ChantingHandler chantingHandler;
    private CursedStoneManager cursedStoneManager;
    private QuestManager questManager;
    private DungeonManager dungeonManager;
    private final ComboTracker comboTracker = new ComboTracker();
    private TickScheduler tickScheduler;
    private MinecraftServer server;

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
        shadowMarkerRegistry = new ShadowMarkerRegistry(config);
        ceBossBarManager = new CeBossBarManager();
        gradeManager = new GradeManager();
        curtainManager = new CurtainManager(config, playerRepository);
        chantingHandler = new ChantingHandler();

        cursedStoneManager = new CursedStoneManager(playerRepository);
        questManager = new QuestManager();
        dungeonManager = new DungeonManager();

        ShikigamiEntityTypes.register();
        CursedSpiritEntityTypes.register();
        CursedToolRegistry.registerItems();
        CostumeItemRegistry.register();
        NpcRegistry.register();

        SkillRegistry.register("gojo",    new GojoSkillSet());
        SkillRegistry.register("itadori", new ItadoriSkillSet());
        SkillRegistry.register("megumi",  new MegumiSkillSet());
        SkillRegistry.register("okkotsu", new OkkotsuSkillSet());
        SkillRegistry.register("sukuna",  new SukunaSkillSet());
        SkillRegistry.register("mahito",  new MahitoSkillSet());
        SkillRegistry.register("jogo",    new JogoSkillSet());
        SkillRegistry.register("hakari",    new HakariSkillSet());
        SkillRegistry.register("inumaki",   new InumakiSkillSet());
        SkillRegistry.register("nanami",    new NanamiSkillSet());
        SkillRegistry.register("higuruma",  new HigurumaSkillSet());

        CharacterRegistry.register("gojo",    new CharacterRegistry.CharacterMeta("gojo",    "Satoru Gojo",       "special_grade", 5000f));
        CharacterRegistry.register("itadori", new CharacterRegistry.CharacterMeta("itadori", "Yuji Itadori",      "grade_1",       4000f));
        CharacterRegistry.register("megumi",  new CharacterRegistry.CharacterMeta("megumi",  "Megumi Fushiguro",  "semi_grade_1",  3500f));
        CharacterRegistry.register("okkotsu", new CharacterRegistry.CharacterMeta("okkotsu", "Yuta Okkotsu",      "special_grade", 4500f));
        CharacterRegistry.register("sukuna",  new CharacterRegistry.CharacterMeta("sukuna",  "Ryomen Sukuna",     "special_grade", 6000f));
        CharacterRegistry.register("mahito",  new CharacterRegistry.CharacterMeta("mahito",  "Mahito",            "special_grade", 4500f));
        CharacterRegistry.register("jogo",    new CharacterRegistry.CharacterMeta("jogo",    "Jogo",              "semi_grade_1",  4000f));
        CharacterRegistry.register("hakari",  new CharacterRegistry.CharacterMeta("hakari",  "Kinji Hakari",      "grade_1",       4000f));
        CharacterRegistry.register("inumaki", new CharacterRegistry.CharacterMeta("inumaki", "Toge Inumaki",      "semi_grade_1",  3000f));
        CharacterRegistry.register("nanami",    new CharacterRegistry.CharacterMeta("nanami",    "Kento Nanami",      "grade_1",       3500f));
        CharacterRegistry.register("higuruma",  new CharacterRegistry.CharacterMeta("higuruma",  "Hiromi Higuruma",   "grade_1",       3500f));

        tickScheduler = new TickScheduler();
        tickScheduler.register(ceManager::regenTick, 1);
        tickScheduler.register(ceBossBarManager::tick, 2);
        tickScheduler.register(awakeningManager::tickCheck, 1);
        tickScheduler.register(zoneStateManager::tick, 1);
        tickScheduler.register(burdenManager::tick, 1);
        tickScheduler.register(HakariSkillSet::tickJackpot, 1);
        tickScheduler.register(NanamiSkillSet::tickRCT, 10);
        tickScheduler.register(FireEffectManager::tickFirePlayer, 1);
        tickScheduler.register(MegumiSkillSet::tickMaharagaFailCheck, 20);
        AutoAnnouncer autoAnnouncer = new AutoAnnouncer();
        tickScheduler.register(autoAnnouncer::tick, 6000);
        tickScheduler.register(CursedSpiritSpawnManager::tick, 200);

        Packets.register();
        PotionUseHandler.register();
        WelcomeHandler.register();
        LOGGER.info("[JJK] 초기화 완료. schemaVersion={}", Migrator.CURRENT_VERSION);

        // /jj — §10-1 (player commands via JjkCommandRegistry registered first, then OP subcommands)
        JjkCommandRegistry.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                CommandManager.literal("jj")

                    // /jj reload (OP 2)
                    .then(CommandManager.literal("reload")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(ctx -> {
                            JJKMod.reloadConfig();
                            ctx.getSource().sendFeedback(() -> Text.literal("[JJK] Config reloaded."), true);
                            return 1;
                        })
                    )

                    // /jj selectchar <player> <characterId> (OP 2)
                    .then(CommandManager.literal("selectchar")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                            .then(CommandManager.argument("characterId", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    CharacterRegistry.ids().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    var source = ctx.getSource();
                                    var target = EntityArgumentType.getPlayer(ctx, "player");
                                    String charId = StringArgumentType.getString(ctx, "characterId");
                                    if (!CharacterRegistry.ids().contains(charId)) {
                                        source.sendError(Text.literal("Unknown character: " + charId));
                                        return 0;
                                    }
                                    CharacterCommandService.SelectResult result =
                                            new CharacterCommandService().select(target, charId);
                                    switch (result) {
                                        case OK ->
                                            source.sendFeedback(() -> Text.literal(
                                                    target.getName().getString() + " -> " + charId), true);
                                        case DUPLICATE_BLOCKED ->
                                            source.sendError(Text.literal("Character already taken."));
                                        case GRADE_INSUFFICIENT ->
                                            source.sendError(Text.literal("Insufficient grade."));
                                        case ALREADY_SELECTED ->
                                            source.sendError(Text.literal("Already selected."));
                                        case RESELECT_DISABLED ->
                                            source.sendError(Text.literal("Reselection disabled."));
                                    }
                                    return result == CharacterCommandService.SelectResult.OK ? 1 : 0;
                                })
                            )
                        )
                    )

                    // /jj data save <player> (OP 2)
                    .then(CommandManager.literal("data")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("save")
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(ctx -> {
                                    var source = ctx.getSource();
                                    var target = EntityArgumentType.getPlayer(ctx, "player");
                                    com.jjk.data.PlayerData data =
                                            JJKMod.getPlayerRepository().load(target.getUuid());
                                    JJKMod.getPlayerRepository().saveImmediate(data);
                                    source.sendFeedback(() -> Text.literal(
                                            "[JJK] Saved: " + target.getName().getString()), true);
                                    return 1;
                                })
                            )
                        )
                    )

                    // /jj domain clear (OP 2)
                    .then(CommandManager.literal("domain")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("clear")
                            .executes(ctx -> {
                                JJKMod.getDomainManager().clearAll();
                                ctx.getSource().sendFeedback(
                                        () -> Text.literal("[JJK] All domains cleared."), true);
                                return 1;
                            })
                        )
                    )
            )
        );

        // 이누마키 채팅 스킬 인터셉트 — spec §6-9
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) ->
                handleInumakiChat(sender, message.getContent().getString()));
        ServerMessageEvents.ALLOW_COMMAND_MESSAGE.register((message, sender, params) -> {
            net.minecraft.server.network.ServerPlayerEntity p = sender.getPlayer();
            if (p == null) return true;
            return handleInumakiChat(p, message.getContent().getString());
        });

        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (!config.jjtBuildingEnabled()) return;
            if (config.jjtBuildingGenerated()) return;
            net.minecraft.server.world.ServerWorld overworld =
                server.getWorld(net.minecraft.world.World.OVERWORLD);
            if (overworld == null) return;
            int cx = config.jjtBuildingCenterX();
            int cz = config.jjtBuildingCenterZ();
            int surfaceY = overworld.getTopY(
                net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, cx, cz);
            net.minecraft.util.math.BlockPos center = new net.minecraft.util.math.BlockPos(
                cx, surfaceY, cz);
            new JjkStructureBuilder(overworld, center).buildAll();
            config.setJjtBuildingGenerated(true);
            config.save();
            LOGGER.info("[JJK] 주술고전 건축물 생성 완료");
        });
        // 원작 건축물 4개 (시부야역·죠고화산·훈련도장·암시장) — jjtBuilding_generated 와 별도 플래그
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (!config.buildingsGenerated) {
                BuildingGenerator.generateAll(server);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
                ceBossBarManager.onPlayerJoin(handler.player);
                // 일일 접속 XP
                PlayerData loginData = playerRepository.load(handler.player.getUuid());
                int todayEpochDay = (int) java.time.LocalDate.now().toEpochDay();
                boolean isNewDay = (loginData.lastLoginDay != todayEpochDay);
                gradeManager.onDailyLogin(loginData, handler.player);
                if (isNewDay) {
                    cursedStoneManager.give(loginData, 15L, "daily_login", handler.player);
                }

                // 가이드북 지급 — 1틱 딜레이로 인벤토리 로드 완료 보장
                final net.minecraft.server.network.ServerPlayerEntity joinedPlayer = handler.player;
                server.execute(() -> {
                    PlayerData data = playerRepository.load(joinedPlayer.getUuid());
                    if (!data.receivedGuideBook) {
                        net.minecraft.item.ItemStack guide = GuideBookItem.create();
                        if (!joinedPlayer.getInventory().insertStack(guide)) {
                            joinedPlayer.dropItem(guide, false);
                        }
                        data.receivedGuideBook = true;
                        playerRepository.saveImmediate(data);
                    }
                    // B-2: 캐릭터 선택 화면(미선택) 또는 캐릭터 정보(선택됨) 전송
                    if (data.characterId == null) {
                        ServerPlayNetworking.send(joinedPlayer,
                            new CharacterSelectS2CPacket(new java.util.ArrayList<>(CharacterRegistry.ids())));
                    } else {
                        ServerPlayNetworking.send(joinedPlayer,
                            new CharacterInfoS2CPacket(data.characterId, data.grade,
                                data.ceMax, data.ceCurrent));
                    }
                });
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
                playerRepository.evict(handler.player.getUuid());
                ceBossBarManager.onPlayerLeave(handler.player.getUuid());
        });
    }

    private void onServerStarting(MinecraftServer server) {
        this.server = server;
        // world/jjk/player_data.db ??spec 吏?3 path
        Path dbPath = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                .resolve("jjk").resolve("player_data.db");
        playerRepository.init(dbPath);
        Path auditDbPath = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                .resolve("jjk").resolve("audit_log.db");
        auditLogger = AuditLogger.open(auditDbPath);
    }

    private void onServerStopping(MinecraftServer server) {
        playerRepository.close();
    }

    private static boolean handleInumakiChat(net.minecraft.server.network.ServerPlayerEntity sender, String content) {
        com.jjk.data.PlayerData data = INSTANCE.playerRepository.load(sender.getUuid());
        if (!"inumaki".equals(data.characterId)) return true;

        long tick = sender.getServerWorld().getTime();
        InumakiSkillSet skill = (InumakiSkillSet) SkillRegistry.get("inumaki");
        if (skill == null) return true;

        if (content.startsWith("!멈춰"))   { skill.onF(data, sender, tick);      return false; }
        if (content.startsWith("!터져"))   { skill.onShiftF(data, sender, tick);  return false; }
        if (content.startsWith("!잠들어")) { skill.onShiftR(data, sender, tick);  return false; }
        if (content.startsWith("!달려"))   { skill.onV(data, sender, tick);       return false; }
        return true;
    }

    public static void reloadConfig() {
        INSTANCE.config = JjkConfig.load();
    }

    public static void handlePlayerDeath(net.minecraft.server.network.ServerPlayerEntity player) {
        INSTANCE.respawnManager.scheduleRespawn(player);
        INSTANCE.playerRepository.evict(player.getUuid());
    }

    public static JJKMod getInstance() { return INSTANCE; }
    public static JjkConfig getConfig() { return INSTANCE.config; }
    public static PlayerRepository getPlayerRepository() { return INSTANCE.playerRepository; }
    public static AuditLogger getAuditLogger() { return INSTANCE.auditLogger; }
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
    public static ComboTracker getComboTracker() { return INSTANCE.comboTracker; }
    public static TickScheduler getTickScheduler() { return INSTANCE.tickScheduler; }
    public static MinecraftServer getServer() { return INSTANCE.server; }
    public static ShadowMarkerRegistry getShadowMarkerRegistry() { return INSTANCE.shadowMarkerRegistry; }
    public static CeBossBarManager getCeBossBarManager() { return INSTANCE.ceBossBarManager; }
    public static GradeManager getGradeManager() { return INSTANCE.gradeManager; }
    public static CurtainManager getCurtainManager() { return INSTANCE.curtainManager; }
    public static ChantingHandler getChantingHandler() { return INSTANCE.chantingHandler; }
    public static CursedStoneManager getCursedStoneManager() { return INSTANCE.cursedStoneManager; }
    public static QuestManager getQuestManager() { return INSTANCE.questManager; }
    public static DungeonManager getDungeonManager() { return INSTANCE.dungeonManager; }

    /** 테스트용: 최소 JJKMod 상태 초기화. 프로덕션 코드에서 호출 금지. */
    public static void initForTest(CursedStoneManager csm, PlayerRepository repo) {
        if (INSTANCE == null) INSTANCE = new JJKMod();
        INSTANCE.cursedStoneManager = csm;
        INSTANCE.playerRepository   = repo;
        if (INSTANCE.config == null)         INSTANCE.config = new JjkConfig();
        if (INSTANCE.gradeManager == null)   INSTANCE.gradeManager = new GradeManager();
        if (INSTANCE.questManager == null)   INSTANCE.questManager = new QuestManager();
        if (INSTANCE.teamManager == null)    INSTANCE.teamManager = new TeamManager();
        if (INSTANCE.dungeonManager == null) INSTANCE.dungeonManager = new DungeonManager();
        if (INSTANCE.ceManager == null)      INSTANCE.ceManager = new CEManager(INSTANCE.config);
        if (INSTANCE.awakeningManager == null) INSTANCE.awakeningManager = new AwakeningManager(INSTANCE.config);
    }
}
