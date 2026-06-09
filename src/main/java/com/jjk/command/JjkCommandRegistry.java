package com.jjk.command;

import com.jjk.JJKMod;
import com.jjk.character.CharacterCommandService;
import com.jjk.item.CursedCrystalItem;
import com.jjk.item.JJKItems;
import com.jjk.character.CharacterRegistry;
import com.jjk.data.Grade;
import com.jjk.data.PlayerData;
import com.jjk.entity.CursedSpiritEntity;
import com.jjk.entity.CursedSpiritEntityTypes;
import com.jjk.entity.JJKEntities;
import com.jjk.entity.cursed.*;
import com.jjk.item.CursedToolItem;
import com.jjk.item.CursedToolRegistry;
import com.jjk.item.GuideBookItem;
import com.jjk.network.s2c.CharacterSelectS2CPacket;
import com.jjk.team.TeamManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

public final class JjkCommandRegistry {

    private JjkCommandRegistry() {}

    private static int giveTool(ServerCommandSource src, ServerPlayerEntity target, String toolId) {
        if (target == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
        CursedToolItem item = switch (toolId) {
            case "cursed_dagger"    -> CursedToolRegistry.CURSED_DAGGER;
            case "thousand_spear"   -> CursedToolRegistry.THOUSAND_SPEAR;
            case "playful_cloud"    -> CursedToolRegistry.PLAYFUL_CLOUD;
            case "inverted_spear"   -> CursedToolRegistry.INVERTED_SPEAR;
            case "split_soul_blade" -> CursedToolRegistry.SPLIT_SOUL_BLADE;
            default -> null;
        };
        if (item == null) {
            src.sendError(Text.literal("[JJK] 알 수 없는 주구: " + toolId));
            return 0;
        }
        ItemStack stack = new ItemStack(item);
        if (!target.getInventory().insertStack(stack)) {
            target.dropItem(stack, false);
        }
        final String name = target.getName().getString();
        src.sendFeedback(() -> Text.literal("[JJK] 주구 지급: " + toolId + " → " + name), true);
        return 1;
    }

    private static int spawnSpirit(ServerCommandSource src, String gradeLabel,
                                    BlockPos pos, ServerWorld world) {
        EntityType<CursedSpiritEntity> type = switch (gradeLabel) {
            case "4급" -> CursedSpiritEntityTypes.GRADE_4;
            case "3급" -> CursedSpiritEntityTypes.GRADE_3;
            case "2급" -> CursedSpiritEntityTypes.GRADE_2;
            case "1급" -> CursedSpiritEntityTypes.GRADE_1;
            case "특급" -> CursedSpiritEntityTypes.SPECIAL;
            default    -> null;
        };
        if (type == null) {
            src.sendError(Text.literal("[JJK] 알 수 없는 등급: " + gradeLabel));
            return 0;
        }
        CursedSpiritEntity spirit = type.create(world);
        if (spirit == null) return 0;
        spirit.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        world.spawnEntity(spirit);
        src.sendFeedback(() -> Text.literal("[JJK] 주령 소환: " + gradeLabel
            + " @ " + pos.toShortString()), true);
        return 1;
    }

    private static final Set<String> VALID_GRADES =
        Set.of("4급", "3급", "2급", "1급", "준특급", "특급");

    private static int spawnCursed(ServerCommandSource src, String entityId, int count) {
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
        if (!(player.getWorld() instanceof ServerWorld world)) return 0;
        BlockPos pos = player.getBlockPos();
        for (int i = 0; i < count; i++) {
            net.minecraft.entity.mob.HostileEntity entity = switch (entityId) {
                case "muki"     -> JJKEntities.MUKI     != null ? JJKEntities.MUKI.create(world)     : null;
                case "kotsibaku"-> JJKEntities.KOTSIBAKU != null ? JJKEntities.KOTSIBAKU.create(world) : null;
                case "homuraku" -> JJKEntities.HOMURAKU  != null ? JJKEntities.HOMURAKU.create(world)  : null;
                case "juugo"    -> JJKEntities.JUUGO_NPC != null ? JJKEntities.JUUGO_NPC.create(world) : null;
                case "jogo"     -> JJKEntities.JOGO_NPC  != null ? JJKEntities.JOGO_NPC.create(world)  : null;
                case "hannami"  -> JJKEntities.HANNAMI_NPC != null ? JJKEntities.HANNAMI_NPC.create(world) : null;
                default -> null;
            };
            if (entity == null) {
                src.sendError(Text.literal("[JJK] 알 수 없는 엔티티ID: " + entityId));
                return 0;
            }
            entity.refreshPositionAndAngles(
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
            world.spawnEntity(entity);
        }
        src.sendFeedback(() -> Text.literal(
            "[JJK] 주령 소환: " + entityId + " ×" + count + " @ " + pos.toShortString()), true);
        return 1;
    }

    private static int showStatus(ServerCommandSource src, PlayerData data, String name) {
        String char_ = data.characterId != null ? data.characterId : "미선택";
        String grade = data.grade != null ? data.grade.display : "-";
        src.sendFeedback(() -> Text.literal("=== [JJK] " + name + " 상태 ==="), false);
        src.sendFeedback(() -> Text.literal("캐릭터: " + char_), false);
        src.sendFeedback(() -> Text.literal("등급: " + grade), false);
        src.sendFeedback(() -> Text.literal(
            "CE: " + (int)data.ceCurrent + " / " + (int)data.ceMax), false);
        src.sendFeedback(() -> Text.literal(
            "HP: " + (int)data.hpCurrent + " / " + (int)data.hpMax), false);
        src.sendFeedback(() -> Text.literal("숙련도: " + data.mastery), false);
        src.sendFeedback(() -> Text.literal("주력 조작: " + String.format("%.2f", data.ceControl)), false);
        src.sendFeedback(() -> Text.literal("손가락: " + data.fingerCount), false);
        src.sendFeedback(() -> Text.literal("==========================="), false);
        return 1;
    }

    private static int printGradeInfo(ServerCommandSource src, PlayerData data, String name) {
        String charId = data.characterId != null ? data.characterId : "미선택";
        String grade  = data.grade != null ? data.grade.display : "-";
        int mastery   = data.mastery;
        src.sendFeedback(() -> Text.literal(
            "[JJK] " + name + " | 캐릭터: " + charId + " | 등급: " + grade
            + " | 숙련도: 평균 Lv." + mastery), false);
        return 1;
    }

    private static int giveSelectionBook(ServerCommandSource src, ServerPlayerEntity target) {
        if (JJKItems.CHARACTER_SELECTION_BOOK == null) {
            src.sendError(Text.literal("[JJK] 선택 책 아이템 미등록"));
            return 0;
        }
        net.minecraft.item.ItemStack book = new net.minecraft.item.ItemStack(JJKItems.CHARACTER_SELECTION_BOOK);
        if (!target.getInventory().insertStack(book)) {
            target.dropItem(book, false);
        }
        final String tName = target.getName().getString();
        src.sendFeedback(() -> Text.literal("[JJK] 캐릭터 선택 책 지급 → " + tName), true);
        return 1;
    }

    // /jj rollback player <player> <backupFile> — 백업 DB에서 PlayerData 추출 복원 (OP 2)
    private static int rollbackPlayer(ServerCommandSource src, ServerPlayerEntity target, String backupFile) {
        net.minecraft.server.MinecraftServer server = src.getServer();
        Path backupPath = com.jjk.data.backup.RotatingBackup.backupDir(server).resolve(backupFile);
        if (!java.nio.file.Files.exists(backupPath)) {
            src.sendError(Text.literal("[JJK] 백업 파일을 찾을 수 없습니다: " + backupFile));
            return 0;
        }

        com.jjk.data.PlayerRepository repo = JJKMod.getPlayerRepository();
        PlayerData restored = repo.loadFromBackup(backupPath, target.getUuid());
        if (restored == null) {
            src.sendError(Text.literal("[JJK] 백업 파일에 해당 플레이어 데이터가 없습니다: " + target.getName().getString()));
            return 0;
        }

        // 복원 전: 현재 player_data.db 전체를 .bak으로 저장 (덮어쓰기 방지)
        try {
            Path dbPath = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve("jjk").resolve("player_data.db");
            Path dir = com.jjk.data.backup.RotatingBackup.backupDir(server);
            java.nio.file.Files.createDirectories(dir);
            String ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            Path preRollback = dir.resolve("pre_rollback_" + target.getUuid() + "_" + ts + ".db.bak");
            java.nio.file.Files.copy(dbPath, preRollback, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (java.io.IOException e) {
            src.sendError(Text.literal("[JJK] 복원 전 백업 실패 — 작업을 중단합니다: " + e.getMessage()));
            return 0;
        }

        long tick = target.getWorld().getTime();
        repo.saveImmediate(restored);

        target.networkHandler.disconnect(Text.literal("[JJK] 관리자에 의해 데이터가 복원되었습니다. 다시 접속해 주세요."));

        if (JJKMod.getAuditLogger() != null) {
            JJKMod.getAuditLogger().logEvent("admin_cmd", target.getUuid(),
                    "{\"event\":\"rollback_player\",\"backupFile\":\"" + backupFile
                            + "\",\"by\":\"" + src.getName() + "\"}", tick);
        }
        com.jjk.discord.DiscordWebhook.sendAsync("[JJK 롤백] " + target.getName().getString()
                + " 의 데이터를 백업 '" + backupFile + "' 로 복원했습니다. (실행자: " + src.getName() + ")");

        final String name = target.getName().getString();
        src.sendFeedback(() -> Text.literal("[JJK] " + name + " 데이터를 '" + backupFile
                + "'에서 복원하고 강제 퇴장시켰습니다."), true);
        return 1;
    }

    // /jj rollback domain <domainId> — 미복구 블록 전체 즉시 복구 (OP 2)
    private static int rollbackDomain(ServerCommandSource src, String domainId) {
        net.minecraft.server.MinecraftServer server = src.getServer();
        com.jjk.domain.DomainBlockQueue queue = JJKMod.getDomainBlockQueue();
        if (queue == null) {
            src.sendError(Text.literal("[JJK] 영역 블록 큐가 초기화되지 않았습니다."));
            return 0;
        }
        int restored = queue.rollbackDomain(server, domainId);

        UUID actor = actorUuid(src);
        if (JJKMod.getAuditLogger() != null) {
            JJKMod.getAuditLogger().logEvent("admin_cmd", actor,
                    "{\"event\":\"rollback_domain\",\"domainId\":\"" + domainId
                            + "\",\"restored\":" + restored + ",\"by\":\"" + src.getName() + "\"}", 0L);
        }
        com.jjk.discord.DiscordWebhook.sendAsync("[JJK 롤백] 영역 '" + domainId + "' 미복구 블록 "
                + restored + "개 복구 완료 (실행자: " + src.getName() + ")");

        src.sendFeedback(() -> Text.literal("[JJK] 영역 '" + domainId + "' 미복구 블록 " + restored + "개를 복구했습니다."), true);
        return 1;
    }

    // /jj rollback chunk <x> <z> — 지정 청크의 미복구 블록 즉시 복구 (OP 2)
    private static int rollbackChunk(ServerCommandSource src, int chunkX, int chunkZ) {
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
        if (!(player.getWorld() instanceof ServerWorld world)) return 0;

        com.jjk.domain.DomainBlockQueue queue = JJKMod.getDomainBlockQueue();
        if (queue == null) {
            src.sendError(Text.literal("[JJK] 영역 블록 큐가 초기화되지 않았습니다."));
            return 0;
        }
        int restored = queue.rollbackChunk(world, chunkX, chunkZ);
        if (restored < 0) {
            src.sendError(Text.literal("청크가 로드되지 않았습니다. 해당 위치로 이동 후 재시도."));
            return 0;
        }

        UUID actor = actorUuid(src);
        if (JJKMod.getAuditLogger() != null) {
            JJKMod.getAuditLogger().logEvent("admin_cmd", actor,
                    "{\"event\":\"rollback_chunk\",\"chunkX\":" + chunkX + ",\"chunkZ\":" + chunkZ
                            + ",\"restored\":" + restored + ",\"by\":\"" + src.getName() + "\"}", world.getTime());
        }
        com.jjk.discord.DiscordWebhook.sendAsync("[JJK 롤백] 청크 (" + chunkX + ", " + chunkZ + ") 미복구 블록 "
                + restored + "개 복구 완료 (실행자: " + src.getName() + ")");

        src.sendFeedback(() -> Text.literal("[JJK] 청크 (" + chunkX + ", " + chunkZ + ") 미복구 블록 "
                + restored + "개를 복구했습니다."), true);
        return 1;
    }

    // audit_log.player_uuid는 NOT NULL — 콘솔 실행 시 시스템 플레이스홀더 UUID 사용
    private static UUID actorUuid(ServerCommandSource src) {
        ServerPlayerEntity p = src.getPlayer();
        return p != null ? p.getUuid() : new UUID(0L, 0L);
    }

    // /jj listbackups — run/backups/jjk/ 백업 파일 목록 (최근 10개, 크기·생성시각) (OP 2)
    private static int listBackups(ServerCommandSource src) {
        net.minecraft.server.MinecraftServer server = src.getServer();
        Path dir = com.jjk.data.backup.RotatingBackup.backupDir(server);
        if (!java.nio.file.Files.isDirectory(dir)) {
            src.sendFeedback(() -> Text.literal("[JJK] 백업 디렉터리가 없습니다: " + dir), false);
            return 1;
        }

        java.time.format.DateTimeFormatter fmt =
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try (java.util.stream.Stream<Path> stream = java.nio.file.Files.list(dir)) {
            java.util.List<Path> files = stream
                    .filter(java.nio.file.Files::isRegularFile)
                    .sorted(java.util.Comparator.comparing((Path p) -> {
                        try { return java.nio.file.Files.getLastModifiedTime(p); }
                        catch (java.io.IOException e) { return java.nio.file.attribute.FileTime.fromMillis(0L); }
                    }).reversed())
                    .limit(10)
                    .toList();

            if (files.isEmpty()) {
                src.sendFeedback(() -> Text.literal("[JJK] 백업 파일이 없습니다."), false);
                return 1;
            }

            src.sendFeedback(() -> Text.literal("=== [JJK] 최근 백업 파일 (최대 10개) ==="), false);
            for (Path p : files) {
                long size = java.nio.file.Files.size(p);
                String created = java.time.LocalDateTime.ofInstant(
                        java.nio.file.Files.getLastModifiedTime(p).toInstant(),
                        java.time.ZoneId.systemDefault()).format(fmt);
                final String line = String.format("  %s — %,d bytes — %s",
                        p.getFileName().toString(), size, created);
                src.sendFeedback(() -> Text.literal(line), false);
            }
            return 1;
        } catch (java.io.IOException e) {
            src.sendError(Text.literal("[JJK] 백업 목록 조회 실패: " + e.getMessage()));
            return 0;
        }
    }

    private static int doResetCooldowns(ServerCommandSource src, PlayerData data) {
        data.cooldowns.clear();
        data.domainCooldownUntil    = 0L;
        data.awakeningCooldownUntil = 0L;
        data.jackpotCooldownUntil   = 0L;
        data.curtainCooldownUntil   = 0L;
        data.bindingVowDeclaredTick = -1L;
        JJKMod.getPlayerRepository().saveImmediate(data);
        src.sendFeedback(() -> Text.literal("[JJK] " + data.uuid + " 쿨타임 전체 초기화 완료"), true);
        return 1;
    }

    private static void resetCharacter(ServerPlayerEntity player) {
        PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
        data.characterId = null;
        JJKMod.getPlayerRepository().saveImmediate(data);
        ServerPlayNetworking.send(player,
                new CharacterSelectS2CPacket(new java.util.ArrayList<>(CharacterRegistry.ids())));
        if (JJKMod.getAuditLogger() != null) {
            JJKMod.getAuditLogger().logEvent("char_reset", player.getUuid(),
                    "{\"by\":\"" + player.getUuid() + "\"}", player.getWorld().getTime());
        }
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                CommandManager.literal("jj")

                    // /jj select <characterId> — 플레이어 권한 (OP 불필요)
                    .then(CommandManager.literal("select")
                        .then(CommandManager.argument("characterId", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                CharacterRegistry.ids().forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                ServerCommandSource src = ctx.getSource();
                                ServerPlayerEntity player = src.getPlayer();
                                if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
                                String charId = StringArgumentType.getString(ctx, "characterId");
                                if (!CharacterRegistry.ids().contains(charId)) {
                                    src.sendError(Text.literal("[JJK] 알 수 없는 캐릭터: " + charId));
                                    return 0;
                                }
                                CharacterCommandService.SelectResult result =
                                        new CharacterCommandService().select(player, charId);
                                return switch (result) {
                                    case OK -> {
                                        src.sendFeedback(() -> Text.literal("[JJK] 캐릭터 [" + charId + "] 선택 완료"), false);
                                        yield 1;
                                    }
                                    case DUPLICATE_BLOCKED -> {
                                        src.sendError(Text.literal("[JJK] 이미 다른 플레이어가 선택한 캐릭터입니다."));
                                        yield 0;
                                    }
                                    case GRADE_INSUFFICIENT -> {
                                        src.sendError(Text.literal("[JJK] 등급이 부족합니다."));
                                        yield 0;
                                    }
                                    case ALREADY_SELECTED -> {
                                        src.sendError(Text.literal("[JJK] 이미 선택된 캐릭터입니다."));
                                        yield 0;
                                    }
                                    case RESELECT_DISABLED -> {
                                        src.sendError(Text.literal("[JJK] 캐릭터 재선택이 비활성화되어 있습니다."));
                                        yield 0;
                                    }
                                };
                            })
                        )
                    )

                    // /jj info [target] — self: 누구나, target: OP 2
                    .then(CommandManager.literal("info")
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            ServerPlayerEntity player = src.getPlayer();
                            if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
                            PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
                            long tick = player.getWorld().getTime();

                            String charDisplay = data.characterId != null ? data.characterId : "미선택";
                            String gradeDisplay = data.grade != null ? data.grade.display : "-";
                            src.sendFeedback(() -> Text.literal(
                                    "[JJK] 캐릭터: " + charDisplay + " | 등급: " + gradeDisplay), false);
                            src.sendFeedback(() -> Text.literal(
                                    "[JJK] CE: " + (int)data.ceCurrent + "/" + (int)data.ceMax
                                    + " | HP: " + (int)data.hpCurrent + "/" + (int)data.hpMax), false);
                            String awakening = data.awakeningActive ? "활성 (잔여: " + (data.awakeningEndTick - tick) + "틱)" : "비활성";
                            String zone = data.zoneActive ? "활성" : "비활성";
                            src.sendFeedback(() -> Text.literal(
                                    "[JJK] 각성: " + awakening + " | Zone: " + zone), false);
                            boolean sealed = data.cooldowns.getOrDefault("skill_seal", 0L) > tick;
                            src.sendFeedback(() -> Text.literal(
                                    "[JJK] 부담: " + data.burden + " | 봉인: " + (sealed ? "활성" : "없음")), false);
                            return 1;
                        })
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                            .requires(src -> src.hasPermissionLevel(2))
                            .executes(ctx -> {
                                ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                PlayerData data = JJKMod.getPlayerRepository().load(target.getUuid());
                                return showStatus(ctx.getSource(), data, target.getName().getString());
                            })
                        )
                    )

                    // /jj reload — OP 2
                    .then(CommandManager.literal("reload")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            try {
                                Path configBase = Path.of("config/jjk");
                                JJKMod.getConfig().reload(configBase.resolve("config.json"));
                                src.sendFeedback(() -> Text.literal("[JJK] config·techniques·domains 리로드 완료"), true);
                                return 1;
                            } catch (Exception e) {
                                src.sendError(Text.literal("[JJK] 리로드 실패: " + e.getMessage()));
                                return 0;
                            }
                        })
                    )

                    // /jj char — 캐릭터 초기화 서브커맨드
                    .then(CommandManager.literal("char")
                        .then(CommandManager.literal("reset")
                            // /jj char reset — 자기 자신 (allowCharacterReselect 무관)
                            .executes(ctx -> {
                                ServerCommandSource src = ctx.getSource();
                                ServerPlayerEntity player = src.getPlayer();
                                if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
                                resetCharacter(player);
                                src.sendFeedback(() -> Text.literal("[JJK] 캐릭터가 초기화됐습니다. /jj select 로 재선택하세요."), false);
                                return 1;
                            })
                            // /jj char reset <player> — OP 2
                            .then(CommandManager.argument("target", EntityArgumentType.player())
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> {
                                    ServerCommandSource src = ctx.getSource();
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    resetCharacter(target);
                                    src.sendFeedback(() -> Text.literal("[JJK] " + target.getName().getString() + " 캐릭터 초기화 완료"), true);
                                    return 1;
                                })
                            )
                        )
                    )

                    // /jj give <toolId> [target] — OP 2
                    .then(CommandManager.literal("give")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("toolId", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                java.util.stream.Stream.of(
                                    "cursed_dagger", "thousand_spear",
                                    "playful_cloud", "inverted_spear",
                                    "split_soul_blade"
                                ).forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> giveTool(ctx.getSource(),
                                ctx.getSource().getPlayer(),
                                StringArgumentType.getString(ctx, "toolId")))
                            .then(CommandManager.argument("target", EntityArgumentType.player())
                                .executes(ctx -> giveTool(ctx.getSource(),
                                    EntityArgumentType.getPlayer(ctx, "target"),
                                    StringArgumentType.getString(ctx, "toolId"))))
                        )
                    )

                    // /jj spawn <grade> [pos] or /jj spawn cursedspirit <grade> — OP 2
                    .then(CommandManager.literal("spawn")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("cursedspirit")
                            .then(CommandManager.argument("grade", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    java.util.stream.Stream.of("4급","3급","2급","1급","특급")
                                        .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    ServerCommandSource src = ctx.getSource();
                                    ServerPlayerEntity player = src.getPlayer();
                                    if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
                                    return spawnSpirit(src,
                                        StringArgumentType.getString(ctx, "grade"),
                                        player.getBlockPos(), (ServerWorld) player.getWorld());
                                })
                            )
                        )
                        .then(CommandManager.argument("grade", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                java.util.stream.Stream.of("4급","3급","2급","1급","특급")
                                    .forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                ServerCommandSource src = ctx.getSource();
                                ServerPlayerEntity player = src.getPlayer();
                                if (player == null) {
                                    src.sendError(Text.literal("플레이어만 사용 가능합니다."));
                                    return 0;
                                }
                                return spawnSpirit(src,
                                    StringArgumentType.getString(ctx, "grade"),
                                    player.getBlockPos(), (ServerWorld) player.getWorld());
                            })
                            .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                .executes(ctx -> {
                                    ServerCommandSource src = ctx.getSource();
                                    BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "pos");
                                    ServerWorld world = src.getWorld();
                                    return spawnSpirit(src,
                                        StringArgumentType.getString(ctx, "grade"), pos, world);
                                })
                            )
                        )
                    )

                    // /jj dungeon — OP 2
                    .then(CommandManager.literal("dungeon")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("enter")
                            .executes(ctx -> {
                                ServerCommandSource src = ctx.getSource();
                                ServerPlayerEntity player = src.getPlayer();
                                if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
                                long tick = player.getWorld().getTime();
                                JJKMod.getDungeonManager().enterDungeon(player, tick);
                                src.sendFeedback(() -> Text.literal("[JJK] 던전 입장"), false);
                                return 1;
                            })
                        )
                        .then(CommandManager.literal("reset")
                            .then(CommandManager.argument("target", EntityArgumentType.player())
                                .executes(ctx -> {
                                    ServerCommandSource src = ctx.getSource();
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    JJKMod.getDungeonManager().exitDungeon(target.getUuid());
                                    src.sendFeedback(() -> Text.literal("[JJK] 던전 진행도 초기화: " + target.getName().getString()), true);
                                    return 1;
                                })
                            )
                        )
                    )

                    // /jj debug ... — OP 2
                    .then(CommandManager.literal("debug")
                        .requires(src -> src.hasPermissionLevel(2))

                        // /jj debug tps
                        .then(CommandManager.literal("tps")
                            .executes(ctx -> {
                                ServerCommandSource src = ctx.getSource();
                                float mspt = src.getServer().getAverageTickTime();
                                float tps = mspt > 0 ? Math.min(20f, 1000f / mspt) : 20f;
                                long usedMib = (Runtime.getRuntime().totalMemory()
                                        - Runtime.getRuntime().freeMemory()) / (1024L * 1024L);
                                String msg = String.format("[JJK] TPS: %.1f | MSPT: %.1fms | 힙: %dMiB", tps, mspt, usedMib);
                                src.sendFeedback(() -> Text.literal(msg), false);
                                return 1;
                            })
                        )

                        // /jj debug domain <domainId>
                        .then(CommandManager.literal("domain")
                            .then(CommandManager.argument("domainId", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerCommandSource src = ctx.getSource();
                                    ServerPlayerEntity player = src.getPlayer();
                                    if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
                                    String domainId = StringArgumentType.getString(ctx, "domainId");
                                    BlockPos pos = player.getBlockPos();
                                    boolean ok = JJKMod.getDomainManager().deployDomain(player, domainId, pos);
                                    if (ok) {
                                        src.sendFeedback(() -> Text.literal("[JJK] 영역 전개: " + domainId), true);
                                        return 1;
                                    } else {
                                        src.sendError(Text.literal("[JJK] 영역 전개 실패 (CE 부족 또는 미등록 domainId)"));
                                        return 0;
                                    }
                                })
                            )
                        )

                        // /jj debug give finger
                        .then(CommandManager.literal("give")
                            .then(CommandManager.literal("finger")
                                .executes(ctx -> {
                                    ServerCommandSource src = ctx.getSource();
                                    ServerPlayerEntity player = src.getPlayer();
                                    if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
                                    PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
                                    int max = JJKMod.getConfig().fingerMaxCount;
                                    data.fingerCount = Math.min(data.fingerCount + 1, max);
                                    JJKMod.getPlayerRepository().saveImmediate(data);
                                    int count = data.fingerCount;
                                    src.sendFeedback(() -> Text.literal("[JJK] 손가락 지급: " + count + "/" + max), true);
                                    return 1;
                                })
                            )
                        )
                    )

                    // /jj guide — 가이드북 재지급 (플레이어 권한)
                    .then(CommandManager.literal("guide")
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            ServerPlayerEntity player = src.getPlayer();
                            if (player == null) {
                                src.sendError(Text.literal("플레이어만 사용 가능합니다."));
                                return 0;
                            }
                            ItemStack guide = GuideBookItem.create();
                            if (!player.getInventory().insertStack(guide)) {
                                player.dropItem(guide, false);
                            }
                            src.sendFeedback(() -> Text.literal("[JJK] 가이드북을 지급했습니다."), false);
                            return 1;
                        })
                    )

                    // /jj status — 자신의 상세 상태 (플레이어 권한)
                    .then(CommandManager.literal("status")
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            ServerPlayerEntity player = src.getPlayer();
                            if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
                            PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
                            return showStatus(src, data, player.getName().getString());
                        })
                    )

                    // /jj economy — CE 결정체 보유량 / give (P3-1)
                    .then(CommandManager.literal("economy")
                        // /jj economy — 자신의 결정체 수량 확인
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            ServerPlayerEntity player = src.getPlayer();
                            if (player == null) {
                                src.sendError(Text.literal("플레이어만 사용 가능합니다."));
                                return 0;
                            }
                            int count = 0;
                            if (CursedCrystalItem.INSTANCE != null) {
                                for (int i = 0; i < player.getInventory().size(); i++) {
                                    net.minecraft.item.ItemStack s = player.getInventory().getStack(i);
                                    if (s.getItem() == CursedCrystalItem.INSTANCE) count += s.getCount();
                                }
                            }
                            final int crystals = count;
                            PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
                            src.sendFeedback(() -> Text.literal(
                                "[JJK] CE 결정체: " + crystals + "개 | 주력석: " + data.cursedStones), false);
                            return 1;
                        })
                        // /jj economy give <player> <amount> — OP 2
                        .then(CommandManager.literal("give")
                            .requires(src -> src.hasPermissionLevel(2))
                            .then(CommandManager.argument("target", EntityArgumentType.player())
                                .then(CommandManager.argument("amount",
                                        com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 9999))
                                    .executes(ctx -> {
                                        ServerCommandSource src = ctx.getSource();
                                        ServerPlayerEntity target =
                                            EntityArgumentType.getPlayer(ctx, "target");
                                        int amount = com.mojang.brigadier.arguments.IntegerArgumentType
                                            .getInteger(ctx, "amount");
                                        if (CursedCrystalItem.INSTANCE == null) {
                                            src.sendError(Text.literal("[JJK] 아이템 미등록"));
                                            return 0;
                                        }
                                        net.minecraft.item.ItemStack crystal =
                                            new net.minecraft.item.ItemStack(
                                                CursedCrystalItem.INSTANCE, amount);
                                        if (!target.getInventory().insertStack(crystal)) {
                                            target.dropItem(crystal, false);
                                        }
                                        final String tName = target.getName().getString();
                                        src.sendFeedback(() -> Text.literal(
                                            "[JJK] CE 결정체 " + amount + "개 → " + tName), true);
                                        return 1;
                                    })
                                )
                            )
                        )
                    )

                    // /jj top — 등급 랭킹 상위 5명 (플레이어 권한)
                    .then(CommandManager.literal("top")
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            java.util.List<String[]> top =
                                JJKMod.getPlayerRepository().findTopPlayers(5);
                            src.sendFeedback(() -> Text.literal("=== [JJK] 등급 랭킹 ==="), false);
                            for (int i = 0; i < top.size(); i++) {
                                String[] entry = top.get(i);
                                int rank = i + 1;
                                src.sendFeedback(() -> Text.literal(rank + "위: " + entry[0] + " — " + entry[1]), false);
                            }
                            if (top.isEmpty()) {
                                src.sendFeedback(() -> Text.literal("데이터 없음"), false);
                            }
                            src.sendFeedback(() -> Text.literal("======================"), false);
                            return 1;
                        })
                    )

                    // /jj tps — 현재 TPS (OP 2)
                    .then(CommandManager.literal("tps")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(ctx -> {
                            float mspt = ctx.getSource().getServer().getAverageTickTime();
                            double tps = mspt > 0 ? Math.min(20.0, 1000.0 / mspt) : 20.0;
                            ctx.getSource().sendFeedback(
                                () -> Text.literal(String.format("[JJK] 현재 TPS: %.1f / 20.0", tps)), false);
                            return 1;
                        })
                    )

                    // /jj resetcooldowns [target] — 쿨타임 전체 초기화 (OP 2)
                    .then(CommandManager.literal("resetcooldowns")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            ServerPlayerEntity player = src.getPlayer();
                            if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
                            PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
                            return doResetCooldowns(src, data);
                        })
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(ctx -> {
                                ServerCommandSource src = ctx.getSource();
                                ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                PlayerData data = JJKMod.getPlayerRepository().load(target.getUuid());
                                return doResetCooldowns(src, data);
                            })
                        )
                    )

                    // /jj grade [player] — 등급·캐릭터·숙련도 조회 (레벨 0)
                    .then(CommandManager.literal("grade")
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            ServerPlayerEntity player = src.getPlayer();
                            if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
                            PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
                            return printGradeInfo(src, data, player.getName().getString());
                        })
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(ctx -> {
                                ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                PlayerData data = JJKMod.getPlayerRepository().load(target.getUuid());
                                return printGradeInfo(ctx.getSource(), data, target.getName().getString());
                            })
                        )
                    )

                    // /jj setgrade <target> <grade> — 등급 강제 설정 (OP 2)
                    .then(CommandManager.literal("setgrade")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                            .then(CommandManager.argument("grade", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    java.util.stream.Stream.of("4급","3급","2급","1급","준특급","특급")
                                        .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    ServerCommandSource src = ctx.getSource();
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                    String grade = StringArgumentType.getString(ctx, "grade");
                                    if (!VALID_GRADES.contains(grade)) {
                                        src.sendError(Text.literal("[JJK] 유효하지 않은 등급: " + grade
                                            + " (4급/3급/2급/1급/준특급/특급)"));
                                        return 0;
                                    }
                                    PlayerData data = JJKMod.getPlayerRepository().load(target.getUuid());
                                    data.grade = Grade.fromKey(grade);
                                    JJKMod.getGradeManager().applyGradeUnlocks(data, grade);
                                    JJKMod.getPlayerRepository().saveImmediate(data);
                                    if (JJKMod.getAuditLogger() != null) {
                                        JJKMod.getAuditLogger().logEvent("admin_setgrade", target.getUuid(),
                                            "{\"grade\":\"" + grade + "\",\"by\":\"" + src.getName() + "\"}",
                                            target.getWorld().getTime());
                                    }
                                    final String tName = target.getName().getString();
                                    src.sendFeedback(() -> Text.literal(
                                        "[JJK] " + tName + " 등급 → " + grade), true);
                                    return 1;
                                })
                            )
                        )
                    )

                    // /jj givebook [player] — 캐릭터 선택 책 지급 (OP 2)
                    .then(CommandManager.literal("givebook")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            ServerPlayerEntity player = src.getPlayer();
                            if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
                            return giveSelectionBook(src, player);
                        })
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(ctx -> {
                                ServerCommandSource src = ctx.getSource();
                                ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                return giveSelectionBook(src, target);
                            })
                        )
                    )

                    // /jj chat faction <message> / /jj chat all <message>
                    .then(CommandManager.literal("chat")
                        .then(CommandManager.literal("faction")
                            .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerCommandSource src = ctx.getSource();
                                    ServerPlayerEntity player = src.getPlayer();
                                    if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
                                    String message = StringArgumentType.getString(ctx, "message");
                                    PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
                                    TeamManager.Team myTeam = TeamManager.getTeam(data.characterId);
                                    String teamLabel = switch (myTeam) {
                                        case JUJUTSU_SORCERER -> "주술사";
                                        case CURSED_SPIRIT    -> "주령";
                                        default               -> "일반";
                                    };
                                    String formatted = "[" + teamLabel + "] " + player.getName().getString() + ": " + message;
                                    player.getServer().getPlayerManager().getPlayerList().stream()
                                        .filter(p -> {
                                            PlayerData pd = JJKMod.getPlayerRepository().load(p.getUuid());
                                            return TeamManager.getTeam(pd.characterId) == myTeam;
                                        })
                                        .forEach(p -> p.sendMessage(Text.literal(formatted), false));
                                    return 1;
                                })
                            )
                        )
                        .then(CommandManager.literal("all")
                            .then(CommandManager.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerCommandSource src = ctx.getSource();
                                    ServerPlayerEntity player = src.getPlayer();
                                    if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
                                    String message = StringArgumentType.getString(ctx, "message");
                                    String formatted = "[전체] " + player.getName().getString() + ": " + message;
                                    player.getServer().getPlayerManager().broadcast(Text.literal(formatted), false);
                                    return 1;
                                })
                            )
                        )
                    )

                    // /jj spawncursed <entityId> [count] — 명칭 주령 소환 (OP 2)
                    .then(CommandManager.literal("spawncursed")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("entityId", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                java.util.stream.Stream.of(
                                    "muki", "kotsibaku", "homuraku",
                                    "juugo", "jogo", "hannami")
                                    .forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> spawnCursed(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "entityId"), 1))
                            .then(CommandManager.argument("count",
                                    IntegerArgumentType.integer(1, 5))
                                .executes(ctx -> spawnCursed(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "entityId"),
                                    IntegerArgumentType.getInteger(ctx, "count"))))
                        )
                    )

                    // /jj db migrate — SQLite 상태 확인 (OP 4)
                    .then(CommandManager.literal("db")
                        .requires(src -> src.hasPermissionLevel(4))
                        .then(CommandManager.literal("migrate")
                            .executes(ctx -> {
                                ServerCommandSource src = ctx.getSource();
                                src.sendFeedback(() -> Text.literal(
                                    "[JJK] 데이터베이스는 이미 SQLite를 사용하고 있습니다. (schemaVersion=" +
                                    com.jjk.data.Migrator.CURRENT_VERSION + ")"), true);
                                return 1;
                            })
                        )
                    )

                    // /jj rollback player|domain|chunk — OP 2: 데이터·블록 롤백
                    .then(CommandManager.literal("rollback")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.literal("player")
                            .then(CommandManager.argument("target", EntityArgumentType.player())
                                .then(CommandManager.argument("backupFile", StringArgumentType.string())
                                    .executes(ctx -> rollbackPlayer(
                                        ctx.getSource(),
                                        EntityArgumentType.getPlayer(ctx, "target"),
                                        StringArgumentType.getString(ctx, "backupFile")))
                                )
                            )
                        )
                        .then(CommandManager.literal("domain")
                            .then(CommandManager.argument("domainId", StringArgumentType.word())
                                .executes(ctx -> rollbackDomain(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "domainId")))
                            )
                        )
                        .then(CommandManager.literal("chunk")
                            .then(CommandManager.argument("x", IntegerArgumentType.integer())
                                .then(CommandManager.argument("z", IntegerArgumentType.integer())
                                    .executes(ctx -> rollbackChunk(
                                        ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "x"),
                                        IntegerArgumentType.getInteger(ctx, "z")))
                                )
                            )
                        )
                    )

                    // /jj listbackups — OP 2: run/backups/jjk/ 최근 백업 목록
                    .then(CommandManager.literal("listbackups")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(ctx -> listBackups(ctx.getSource()))
                    )

                    // /jj audit <player> — OP 2: antiAbuseFlags + 최근 감사로그 10개
                    .then(CommandManager.literal("audit")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(ctx -> {
                                ServerCommandSource src = ctx.getSource();
                                ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                PlayerData data = JJKMod.getPlayerRepository().load(target.getUuid());

                                src.sendFeedback(() -> Text.literal("=== [JJK] 감사 — " + target.getName().getString() + " ==="), false);
                                src.sendFeedback(() -> Text.literal("격리 상태: " + (data.quarantined ? "예" : "아니오")), false);
                                if (data.antiAbuseFlags.isEmpty()) {
                                    src.sendFeedback(() -> Text.literal("이상행동 플래그: 없음"), false);
                                } else {
                                    src.sendFeedback(() -> Text.literal("이상행동 플래그 (" + data.antiAbuseFlags.size() + "):"), false);
                                    for (String flag : data.antiAbuseFlags) {
                                        src.sendFeedback(() -> Text.literal("  - " + flag), false);
                                    }
                                }

                                src.sendFeedback(() -> Text.literal("최근 감사로그:"), false);
                                if (JJKMod.getAuditLogger() != null) {
                                    var events = JJKMod.getAuditLogger().getRecentEvents(target.getUuid(), 10);
                                    if (events.isEmpty()) {
                                        src.sendFeedback(() -> Text.literal("  (기록 없음)"), false);
                                    } else {
                                        for (String event : events) {
                                            src.sendFeedback(() -> Text.literal("  " + event), false);
                                        }
                                    }
                                }
                                src.sendFeedback(() -> Text.literal("==========================="), false);
                                return 1;
                            })
                        )
                    )

                    // /jj quarantine <player> — OP 2: 스킬 발동 전면 차단
                    .then(CommandManager.literal("quarantine")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(ctx -> {
                                ServerCommandSource src = ctx.getSource();
                                ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                PlayerData data = JJKMod.getPlayerRepository().load(target.getUuid());
                                data.quarantined = true;
                                JJKMod.getPlayerRepository().saveImmediate(data);
                                if (JJKMod.getAuditLogger() != null) {
                                    JJKMod.getAuditLogger().logEvent("admin_cmd", target.getUuid(),
                                            "{\"event\":\"quarantine\",\"by\":\"" + src.getName() + "\"}", 0L);
                                }
                                src.sendFeedback(() -> Text.literal("[JJK] " + target.getName().getString() + " 격리 처리 — 스킬 발동이 차단됩니다."), true);
                                return 1;
                            })
                        )
                    )

                    // /jj unquarantine <player> — OP 2: 격리 해제
                    .then(CommandManager.literal("unquarantine")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(ctx -> {
                                ServerCommandSource src = ctx.getSource();
                                ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "target");
                                PlayerData data = JJKMod.getPlayerRepository().load(target.getUuid());
                                data.quarantined = false;
                                JJKMod.getPlayerRepository().saveImmediate(data);
                                if (JJKMod.getAuditLogger() != null) {
                                    JJKMod.getAuditLogger().logEvent("admin_cmd", target.getUuid(),
                                            "{\"event\":\"unquarantine\",\"by\":\"" + src.getName() + "\"}", 0L);
                                }
                                src.sendFeedback(() -> Text.literal("[JJK] " + target.getName().getString() + " 격리 해제 완료."), true);
                                return 1;
                            })
                        )
                    )
            )
        );
    }
}
