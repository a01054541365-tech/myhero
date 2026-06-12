package com.jjk;

import com.jjk.bossbar.CeBossBarManager;
import com.jjk.dungeon.DungeonManager;
import com.jjk.economy.CursedStoneManager;
import com.jjk.entity.npc.NpcRegistry;
import com.jjk.discord.DiscordReporter;
import com.jjk.server.AutoAnnouncer;
import com.jjk.server.BackupScheduler;
import com.jjk.server.SeasonManager;
import com.jjk.server.WelcomeHandler;
import com.jjk.world.WorldGenerationManager;
import com.jjk.event.PotionUseHandler;
import com.jjk.event.RaidEventManager;
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
    private com.jjk.domain.DomainBlockHistoryDao domainBlockHistoryDao;
    private com.jjk.domain.DomainBlockQueue domainBlockQueue;
    private TeamManager teamManager;
    private ZoneStateManager zoneStateManager;
    private BurdenManager burdenManager;
    private TrialManager trialManager;
    private FingerSystem fingerSystem;
    private RespawnManager respawnManager;
    private EffectDeferQueue effectDeferQueue;
    private com.jjk.performance.TPSGuard tpsGuard;
    private CombatPipeline combatPipeline;
    private com.jjk.combat.AntiAbuseManager antiAbuseManager;
    private com.jjk.security.MovementValidator movementValidator;
    private com.jjk.combat.BlackFlashHandler blackFlashHandler;
    private com.jjk.data.backup.RotatingBackup rotatingBackup;
    private com.jjk.combat.BindingVowSystem bindingVowSystem;
    private ShadowMarkerRegistry shadowMarkerRegistry;
    private CeBossBarManager ceBossBarManager;
    private GradeManager gradeManager;
    private CurtainManager curtainManager;
    private ChantingHandler chantingHandler;
    private CursedStoneManager cursedStoneManager;
    private QuestManager questManager;
    private DungeonManager dungeonManager;
    private RaidEventManager raidEventManager;
    private final ComboTracker comboTracker = new ComboTracker();
    private TickScheduler tickScheduler;
    private MinecraftServer server;

    @Override
    public void onInitialize() {
        INSTANCE = this;

        config = JjkConfig.load();
        // techniques.json 로드 — 스킬 CE/데미지/쿨다운 + characterStats(CE풀·재생·HP) 적용.
        // (미로드 시 TechniqueLoader 게터가 기본값을 반환 → 데이터 주도 스킬셋 무력화)
        com.jjk.combat.TechniqueLoader.load(java.nio.file.Path.of("config/jjk/techniques.json"));
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
        antiAbuseManager = new com.jjk.combat.AntiAbuseManager();
        movementValidator = new com.jjk.security.MovementValidator();
        blackFlashHandler = new com.jjk.combat.BlackFlashHandler();
        rotatingBackup = new com.jjk.data.backup.RotatingBackup();
        bindingVowSystem = new com.jjk.combat.BindingVowSystem();
        shadowMarkerRegistry = new ShadowMarkerRegistry(config);
        ceBossBarManager = new CeBossBarManager();
        gradeManager = new GradeManager();
        curtainManager = new CurtainManager(config, playerRepository);
        chantingHandler = new ChantingHandler();

        cursedStoneManager = new CursedStoneManager(playerRepository);
        questManager = new QuestManager();
        dungeonManager = new DungeonManager();
        raidEventManager = new RaidEventManager();

        ShikigamiEntityTypes.register();
        CursedSpiritEntityTypes.register();
        com.jjk.entity.JJKEntities.register();
        CursedToolRegistry.registerItems();
        com.jjk.item.CursedCrystalItem.register();
        CostumeItemRegistry.register();
        com.jjk.item.JJKItems.register();
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
        SkillRegistry.register("choso",     new ChosoSkillSet());
        SkillRegistry.register("todo",      new com.jjk.character.impl.NonSorcererSkillSet());

        CharacterRegistry.register("gojo",   new CharacterRegistry.CharacterMeta("gojo",    "Satoru Gojo",       "special_grade", 5000f));
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
        CharacterRegistry.register("choso",     new CharacterRegistry.CharacterMeta("choso",     "Choso",             "grade_1",       2200f));
        CharacterRegistry.register("todo",      new CharacterRegistry.CharacterMeta("todo",      "Aoi Todo",          "grade_3",       0f));

        tickScheduler = new TickScheduler();
        tickScheduler.register(bindingVowSystem::tickPlayer, 1);
        tickScheduler.register(ceManager::regenTick, 1);
        tickScheduler.register(ceBossBarManager::tick, 2);
        tickScheduler.register(awakeningManager::tickCheck, 1);
        tickScheduler.register(zoneStateManager::tick, 1);
        tickScheduler.register(burdenManager::tick, 1);
        tickScheduler.register(com.jjk.trial.TrialManager::tickSealExpiry, 1);
        tickScheduler.register(HakariSkillSet::tickJackpot, 1);
        tickScheduler.register(NanamiSkillSet::tickRCT, 10);
        tickScheduler.register(FireEffectManager::tickFirePlayer, 1);
        tickScheduler.register(MegumiSkillSet::tickMaharagaFailCheck, 20);
        AutoAnnouncer autoAnnouncer = new AutoAnnouncer();
        tickScheduler.register(autoAnnouncer::tick, 6000);
        tickScheduler.register(CursedSpiritSpawnManager::tick, 200);
        tickScheduler.register(com.jjk.entity.spawn.CursedEntitySpawnManager::tick, 200);
        tickScheduler.register(com.jjk.network.HudPacketSender::sendHudSync, 5);
        tickScheduler.register(com.jjk.network.HudPacketSender::sendEntityHealthSync, 10);
        tickScheduler.register(com.jjk.network.HudPacketSender::sendCEAuraSync, 10);
        tickScheduler.registerServerTask(raidEventManager::tick, 1);
        tickScheduler.registerServerTask(sv -> {
            if (domainBlockQueue != null) domainBlockQueue.tick(sv);
        }, 1);
        tickScheduler.registerServerTask(sv -> {
            if (tpsGuard != null) tpsGuard.tick(sv);
        }, 20);
        tickScheduler.registerServerTask(sv -> {
            if (movementValidator != null) movementValidator.validateAll(sv);
        }, 1);
        // H-1-5: 매 24시간(1,728,000틱)마다 7일 이상 지난 복구 완료 항목 정리
        tickScheduler.registerServerTask(sv -> {
            if (domainBlockHistoryDao != null) {
                domainBlockHistoryDao.purgeOldRecovered(
                    System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000L);
            }
        }, 20 * 60 * 60 * 24);
        tickScheduler.registerServerTask(BackupScheduler::tick, BackupScheduler.INTERVAL_TICKS);
        tickScheduler.registerServerTask(DiscordReporter::sendPeriodicReport, DiscordReporter.INTERVAL_TICKS);

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
        ServerLifecycleEvents.SERVER_STARTED.register(SeasonManager::checkAndReset);
        ServerLifecycleEvents.SERVER_STARTED.register(WorldGenerationManager::onServerStarted);
        ServerLifecycleEvents.SERVER_STARTED.register(sv -> rotatingBackup.register(sv));
        // H-1-4: 서버 완전 시작 후 미복구 블록 복구 (청크 조회가 가능한 시점)
        ServerLifecycleEvents.SERVER_STARTED.register(sv -> {
            if (domainBlockQueue != null) domainBlockQueue.recoverFromDB(sv);
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
                    // G-5-2: 튜토리얼 NPC 스폰 (첫 접속 + 튜토리얼 미완료)
                    if (!data.hasCompletedTutorial && !data.hasReceivedSelectionBook) {
                        spawnTutorialNpc(joinedPlayer, server);
                    }

                    // B-2: 선택 책 첫 지급 → OpenCharacterSelectS2CPacket. 기존 플레이어 → CharacterInfo.
                    boolean selectionSent = new CharacterCommandService().handleJoin(joinedPlayer);
                    if (!selectionSent) {
                        if (data.characterId == null) {
                            ServerPlayNetworking.send(joinedPlayer,
                                new CharacterSelectS2CPacket(new java.util.ArrayList<>(CharacterRegistry.ids())));
                        } else {
                            ServerPlayNetworking.send(joinedPlayer,
                                new CharacterInfoS2CPacket(data.characterId,
                                    data.grade != null ? data.grade.display : "4급",
                                    data.ceMax, data.ceCurrent));
                        }
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
        // 1. SQLite 연결 초기화 + domain_block_history 테이블 생성 (H-1-1 포함)
        playerRepository.init(dbPath);
        // 2. DomainBlockHistoryDao + DomainBlockQueue 초기화 (H-1-2, H-1-3)
        domainBlockHistoryDao = new com.jjk.domain.DomainBlockHistoryDao(
                playerRepository.getConnection());
        domainBlockQueue = new com.jjk.domain.DomainBlockQueue(domainBlockHistoryDao);
        // 3. 청크 로드 리스너 등록 — 서버 시작 전에 등록해야 모든 청크 이벤트를 수신
        domainBlockQueue.registerChunkLoadListener(server);
        // 4. TPSGuard — EffectDeferQueue·DomainBlockQueue 부하 제어 연동
        tpsGuard = new com.jjk.performance.TPSGuard(effectDeferQueue, domainBlockQueue);
        Path auditDbPath = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                .resolve("jjk").resolve("audit_log.db");
        auditLogger = AuditLogger.open(auditDbPath);
    }

    private void onServerStopping(MinecraftServer server) {
        LOGGER.info("[JJK] 서버 종료 감지 — 전체 플레이어 데이터 강제 저장 시작");
        long startMs = System.currentTimeMillis();
        playerRepository.flushAll();
        long elapsed = System.currentTimeMillis() - startMs;
        LOGGER.info("[JJK] 강제 저장 완료. 소요시간={}ms", elapsed);
        playerRepository.close();
    }

    private static boolean handleInumakiChat(net.minecraft.server.network.ServerPlayerEntity sender, String content) {
        com.jjk.data.PlayerData data = INSTANCE.playerRepository.load(sender.getUuid());
        if (!"inumaki".equals(data.characterId)) return true;

        // ! 없는 일반 채팅은 완전 허용
        if (!content.startsWith("!")) return true;

        long tick = sender.getServerWorld().getTime();
        InumakiSkillSet skill = (InumakiSkillSet) SkillRegistry.get("inumaki");
        if (skill == null) return true;

        if (content.startsWith("!멈춰"))   { skill.onF(data, sender, tick);      return false; }
        if (content.startsWith("!터져"))   { skill.onShiftF(data, sender, tick);  return false; }
        if (content.startsWith("!잠들어")) { skill.onShiftR(data, sender, tick);  return false; }
        if (content.startsWith("!달려"))   { skill.onV(data, sender, tick);       return false; }

        // 유효하지 않은 ! 메시지: 안내 후 차단
        sender.sendMessage(Text.literal("§7[이누마키] 유효하지 않은 주언입니다. (!멈춰 !터져 !잠들어 !달려)"), false);
        return false;
    }

    private static void spawnTutorialNpc(net.minecraft.server.network.ServerPlayerEntity player,
                                           net.minecraft.server.MinecraftServer server) {
        if (com.jjk.entity.npc.NpcRegistry.TUTORIAL == null) return;
        if (!(player.getWorld() instanceof net.minecraft.server.world.ServerWorld sw)) return;

        // 이미 플레이어 주변 20블록 이내에 튜토리얼 NPC가 있으면 재스폰 생략
        boolean exists = !sw.getEntitiesByClass(
            com.jjk.entity.npc.TutorialNpcEntity.class,
            player.getBoundingBox().expand(20.0), e -> true).isEmpty();
        if (exists) return;

        com.jjk.entity.npc.TutorialNpcEntity npc =
            com.jjk.entity.npc.NpcRegistry.TUTORIAL.create(sw);
        if (npc == null) return;
        double x = player.getX() + 2.0;
        double y = player.getY();
        double z = player.getZ() + 3.0;
        npc.refreshPositionAndAngles(x, y, z, 0f, 0f);
        sw.spawnEntity(npc);
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
    public static com.jjk.domain.DomainBlockQueue getDomainBlockQueue() { return INSTANCE.domainBlockQueue; }
    public static com.jjk.performance.TPSGuard getTpsGuard() { return INSTANCE.tpsGuard; }
    public static TeamManager getTeamManager() { return INSTANCE.teamManager; }
    public static ZoneStateManager getZoneStateManager() { return INSTANCE.zoneStateManager; }
    public static BurdenManager getBurdenManager() { return INSTANCE.burdenManager; }
    public static TrialManager getTrialManager() { return INSTANCE.trialManager; }
    public static FingerSystem getFingerSystem() { return INSTANCE.fingerSystem; }
    public static RespawnManager getRespawnManager() { return INSTANCE.respawnManager; }
    public static EffectDeferQueue getEffectDeferQueue() { return INSTANCE.effectDeferQueue; }
    public static CombatPipeline getCombatPipeline() { return INSTANCE.combatPipeline; }
    public static com.jjk.combat.AntiAbuseManager getAntiAbuseManager() { return INSTANCE.antiAbuseManager; }
    public static com.jjk.security.MovementValidator getMovementValidator() { return INSTANCE.movementValidator; }
    public static com.jjk.combat.BlackFlashHandler getBlackFlashHandler() { return INSTANCE.blackFlashHandler; }
    public static com.jjk.combat.BindingVowSystem getBindingVowSystem() { return INSTANCE.bindingVowSystem; }
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
        if (INSTANCE.antiAbuseManager == null) INSTANCE.antiAbuseManager = new com.jjk.combat.AntiAbuseManager();
    }
}
