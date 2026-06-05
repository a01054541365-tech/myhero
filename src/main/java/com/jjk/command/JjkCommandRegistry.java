package com.jjk.command;

import com.jjk.JJKMod;
import com.jjk.burden.BurdenManager;
import com.jjk.dungeon.DungeonManager;
import com.jjk.character.CharacterCommandService;
import com.jjk.character.CharacterRegistry;
import com.jjk.data.PlayerData;
import com.jjk.entity.CursedSpiritEntity;
import com.jjk.entity.CursedSpiritEntityTypes;
import com.jjk.entity.CursedSpiritGrade;
import com.jjk.grade.GradeManager;
import com.jjk.item.CursedToolItem;
import com.jjk.item.CursedToolRegistry;
import com.jjk.item.GuideBookItem;
import com.jjk.network.s2c.CharacterSelectS2CPacket;
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

    private static int showStatus(ServerCommandSource src, PlayerData data, String name) {
        String char_ = data.characterId != null ? data.characterId : "미선택";
        String grade = data.grade != null ? data.grade : "-";
        src.sendFeedback(() -> Text.literal("=== [JJK] " + name + " 상태 ==="), false);
        src.sendFeedback(() -> Text.literal("캐릭터: " + char_), false);
        src.sendFeedback(() -> Text.literal("등급: " + grade), false);
        src.sendFeedback(() -> Text.literal(
            "CE: " + (int)data.ceCurrent + " / " + (int)data.ceMax), false);
        src.sendFeedback(() -> Text.literal(
            "HP: " + (int)data.hpCurrent + " / " + (int)data.hpMax), false);
        src.sendFeedback(() -> Text.literal("숙련도: " + data.mastery), false);
        src.sendFeedback(() -> Text.literal("손가락: " + data.fingerCount), false);
        src.sendFeedback(() -> Text.literal("==========================="), false);
        return 1;
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
                            String gradeDisplay = data.grade != null ? data.grade : "-";
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
                                    data.grade = grade;
                                    JJKMod.getPlayerRepository().saveImmediate(data);
                                    final String tName = target.getName().getString();
                                    src.sendFeedback(() -> Text.literal(
                                        "[JJK] " + tName + " 등급 → " + grade), true);
                                    return 1;
                                })
                            )
                        )
                    )
            )
        );
    }
}
