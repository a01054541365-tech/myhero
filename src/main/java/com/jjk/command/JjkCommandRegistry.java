package com.jjk.command;

import com.jjk.JJKMod;
import com.jjk.burden.BurdenManager;
import com.jjk.character.CharacterCommandService;
import com.jjk.character.CharacterRegistry;
import com.jjk.data.PlayerData;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Path;

public final class JjkCommandRegistry {

    private JjkCommandRegistry() {}

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

                    // /jj info — 플레이어 권한 (OP 불필요)
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
            )
        );
    }
}
