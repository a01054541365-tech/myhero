package com.jjk.network.c2s;

import com.jjk.JJKMod;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public record GuideBookOpenC2SPacket() implements CustomPayload {

    public static final CustomPayload.Id<GuideBookOpenC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "guide_book_open"));

    public static final PacketCodec<RegistryByteBuf, GuideBookOpenC2SPacket> CODEC =
            PacketCodec.of((pkt, buf) -> {}, buf -> new GuideBookOpenC2SPacket());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void handle(GuideBookOpenC2SPacket pkt, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> sendGuideText(ctx.player()));
    }

    private static void sendGuideText(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("--- §e[주술사 가이드]§r ---"), false);
        player.sendMessage(Text.literal("§f캐릭터 선택: 처음 접속 시 자동으로 선택 화면이 열립니다."), false);
        player.sendMessage(Text.literal("§f스킬 키:"), false);
        player.sendMessage(Text.literal("  §7F §f— 기본 술식"), false);
        player.sendMessage(Text.literal("  §7Shift+F §f— 강화 술식"), false);
        player.sendMessage(Text.literal("  §7R §f— 보조 술식"), false);
        player.sendMessage(Text.literal("  §7Shift+R §f— 보조 강화"), false);
        player.sendMessage(Text.literal("  §7V §f— 영역 전개"), false);
        player.sendMessage(Text.literal("  §7C §f— 간이영역"), false);
        player.sendMessage(Text.literal("  §7G §f— 확장술식"), false);
        player.sendMessage(Text.literal("  §7T §f— 속박"), false);
        player.sendMessage(Text.literal("§f주력 주입: §7Shift+우클릭 §f(무기 손에 든 상태)"), false);
        player.sendMessage(Text.literal("§f등급: §74급 → 3급 → 2급 → 1급 → 준특급 → 특급"), false);
        player.sendMessage(Text.literal("§fNPC 위치: §7/jj spawnnpc §f로 소환 가능"), false);
        player.sendMessage(Text.literal("§f명령어 목록: §7/jj help §f(이 가이드북 재지급)"), false);
        player.sendMessage(Text.literal("---"), false);
    }
}
