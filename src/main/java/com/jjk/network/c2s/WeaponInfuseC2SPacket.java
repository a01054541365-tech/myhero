package com.jjk.network.c2s;

import com.jjk.JJKMod;
import com.jjk.combat.WeaponInfusionManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public record WeaponInfuseC2SPacket() implements CustomPayload {

    public static final CustomPayload.Id<WeaponInfuseC2SPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "weapon_infuse"));

    public static final PacketCodec<RegistryByteBuf, WeaponInfuseC2SPacket> CODEC =
            PacketCodec.of(
                    (pkt, buf) -> {},
                    buf -> new WeaponInfuseC2SPacket()
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }

    public static void handle(WeaponInfuseC2SPacket packet, ServerPlayNetworking.Context ctx) {
        ctx.server().execute(() -> {
            ServerPlayerEntity player = ctx.player();
            WeaponInfusionManager wim = JJKMod.getWeaponInfusionManager();
            WeaponInfusionManager.InfusionResult result = wim.tryInfuse(player);
            if (result != WeaponInfusionManager.InfusionResult.SUCCESS) {
                String msg = switch (result) {
                    case DISABLED       -> "§c[JJK] CE 주입이 비활성화되어 있습니다.";
                    case NOT_CHARACTER  -> "§c[JJK] 캐릭터를 먼저 선택하세요.";
                    case NOT_SORCERER   -> "§c[JJK] 주술사만 CE를 주입할 수 있습니다.";
                    case INVALID_WEAPON -> "§c[JJK] 검 또는 도끼를 들고 있어야 합니다.";
                    case NO_CE          -> "§c[JJK] CE가 부족합니다.";
                    default             -> "§c[JJK] CE 주입 실패.";
                };
                player.sendMessage(Text.literal(msg), true);
            }
        });
    }
}
