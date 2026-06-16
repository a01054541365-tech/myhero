package com.jjk.command;

import com.jjk.JJKMod;
import com.jjk.api.skill.ISkillSet;
import com.jjk.character.SkillRegistry;
import com.jjk.data.PlayerData;
import com.jjk.entity.npc.NpcRegistry;
import com.jjk.entity.npc.SimpleNpcEntity;
import com.jjk.item.GuideBookItem;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public final class JjkCommandRegistry {

    private JjkCommandRegistry() {}

    private static int spawnNpc(ServerCommandSource src, String npcId) {
        ServerPlayerEntity player = src.getPlayer();
        if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
        if (!(player.getWorld() instanceof ServerWorld world)) return 0;
        EntityType<SimpleNpcEntity> type = NpcRegistry.byId(npcId);
        if (type == null) {
            src.sendError(Text.literal("[JJK] 알 수 없는 NPC ID: " + npcId));
            return 0;
        }
        SimpleNpcEntity npc = type.create(world);
        if (npc == null) { src.sendError(Text.literal("[JJK] NPC 생성 실패")); return 0; }
        npc.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0f);
        world.spawnEntity(npc);
        src.sendFeedback(() -> Text.literal("[JJK] NPC 소환: " + npcId), true);
        return 1;
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                CommandManager.literal("jj")

                    // /jj help — 가이드북 지급 (레벨 0)
                    .then(CommandManager.literal("help")
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            ServerPlayerEntity player = src.getPlayer();
                            if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
                            ItemStack guide = GuideBookItem.create();
                            if (!player.getInventory().insertStack(guide)) {
                                player.dropItem(guide, false);
                            }
                            src.sendFeedback(() -> Text.literal("[JJK] 가이드북을 지급했습니다."), false);
                            return 1;
                        })
                    )

                    // /jj stats — 자신의 PlayerData 요약 (레벨 0)
                    .then(CommandManager.literal("stats")
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            ServerPlayerEntity player = src.getPlayer();
                            if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
                            PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
                            String charDisplay = data.characterId != null ? data.characterId : "미선택";
                            String gradeDisplay = data.grade != null ? data.grade.display : "-";
                            src.sendFeedback(() -> Text.literal("[JJK] 캐릭터: " + charDisplay + " | 등급: " + gradeDisplay), false);
                            src.sendFeedback(() -> Text.literal("[JJK] CE: " + (int)data.ceCurrent + " / " + (int)data.ceMax), false);
                            src.sendFeedback(() -> Text.literal("[JJK] 숙련도 평균: " + data.mastery + " | 손가락: " + data.fingerCount), false);
                            return 1;
                        })
                    )

                    // /jj skills — 현재 해금 스킬 목록 (레벨 0)
                    .then(CommandManager.literal("skills")
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            ServerPlayerEntity player = src.getPlayer();
                            if (player == null) { src.sendError(Text.literal("플레이어만 사용 가능합니다.")); return 0; }
                            PlayerData data = JJKMod.getPlayerRepository().load(player.getUuid());
                            if (data.characterId == null) {
                                src.sendFeedback(() -> Text.literal("[JJK] 캐릭터를 먼저 선택하세요."), false);
                                return 0;
                            }
                            ISkillSet skillSet = SkillRegistry.get(data.characterId);
                            if (skillSet == null) {
                                src.sendFeedback(() -> Text.literal("[JJK] 스킬 정보 없음."), false);
                                return 0;
                            }
                            String[] keyLabels = {"F", "Shift+F", "R", "Shift+R", "V", "C"};
                            src.sendFeedback(() -> Text.literal("=== [JJK] " + data.characterId + " 스킬 ==="), false);
                            for (int i = 0; i < keyLabels.length; i++) {
                                String name = skillSet.getSkillName(i);
                                if (name != null && !name.isEmpty() && !name.equalsIgnoreCase("not_implemented")) {
                                    final String line = "  " + keyLabels[i] + " — " + name;
                                    src.sendFeedback(() -> Text.literal(line), false);
                                }
                            }
                            return 1;
                        })
                    )

                    // /jj spawnnpc <npcId> — NPC 소환 (OP 2)
                    .then(CommandManager.literal("spawnnpc")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("npcId", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                java.util.stream.Stream.of(
                                    "zenin_storage", "kusakabe", "shoko",
                                    "gojo_shiyu", "ijichi", "yaga", "nahobino")
                                    .forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> spawnNpc(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "npcId")))
                        )
                    )
            )
        );
    }
}
