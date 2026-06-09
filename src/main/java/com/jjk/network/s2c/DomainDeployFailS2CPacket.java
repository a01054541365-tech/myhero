package com.jjk.network.s2c;

import com.jjk.JJKMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DomainDeployFailS2CPacket(FailReason reason) implements CustomPayload {

    public enum FailReason {
        CE_INSUFFICIENT, ON_COOLDOWN, BANNED_CHUNK, DOMAIN_ALREADY_ACTIVE, CLASH_LOST
    }

    public static final CustomPayload.Id<DomainDeployFailS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(JJKMod.MOD_ID, "domain_deploy_fail"));

    public static final PacketCodec<RegistryByteBuf, DomainDeployFailS2CPacket> CODEC =
            PacketCodec.of(
                (value, buf) -> buf.writeVarInt(value.reason().ordinal()),
                buf -> {
                    int ord = buf.readVarInt();
                    FailReason r = (ord >= 0 && ord < FailReason.values().length)
                            ? FailReason.values()[ord] : FailReason.CE_INSUFFICIENT;
                    return new DomainDeployFailS2CPacket(r);
                }
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() { return ID; }
}
