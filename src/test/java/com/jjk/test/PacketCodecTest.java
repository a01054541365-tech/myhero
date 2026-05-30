package com.jjk.test;

import com.jjk.network.c2s.SkillUseC2SPacket;
import com.jjk.network.s2c.*;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// 패킷 직렬화 → 역직렬화 왕복 검증 (RegistryByteBuf null-registry 허용 — 레지스트리 미사용 코덱)
class PacketCodecTest {

    private static RegistryByteBuf newBuf() {
        return new RegistryByteBuf(new PacketByteBuf(Unpooled.buffer()), null);
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(PacketCodec<RegistryByteBuf, T> codec, T value) {
        RegistryByteBuf buf = newBuf();
        codec.encode(buf, value);
        buf.readerIndex(0);
        return codec.decode(buf);
    }

    // 1. ZoneEnterS2CPacket (String domainId) 왕복
    @Test
    void testZoneEnterCodec() {
        ZoneEnterS2CPacket original = new ZoneEnterS2CPacket("gojo_unlimited_void");
        ZoneEnterS2CPacket decoded = roundTrip(ZoneEnterS2CPacket.CODEC, original);
        assertEquals("gojo_unlimited_void", decoded.domainId());
    }

    // 2. ZoneExitS2CPacket (String domainId) 왕복
    @Test
    void testZoneExitCodec() {
        ZoneExitS2CPacket original = new ZoneExitS2CPacket("gojo_unlimited_void");
        ZoneExitS2CPacket decoded = roundTrip(ZoneExitS2CPacket.CODEC, original);
        assertEquals("gojo_unlimited_void", decoded.domainId());
    }

    // 3. AwakeningS2CPacket active=true 왕복
    @Test
    void testAwakeningCodec_Active() {
        AwakeningS2CPacket original = new AwakeningS2CPacket(true);
        AwakeningS2CPacket decoded = roundTrip(AwakeningS2CPacket.CODEC, original);
        assertTrue(decoded.active());
    }

    // 4. AwakeningS2CPacket active=false 왕복
    @Test
    void testAwakeningCodec_Inactive() {
        AwakeningS2CPacket original = new AwakeningS2CPacket(false);
        AwakeningS2CPacket decoded = roundTrip(AwakeningS2CPacket.CODEC, original);
        assertFalse(decoded.active());
    }

    // 5. SkillResultS2CPacket (int keyId, String result, float finalDamage) 왕복
    @Test
    void testSkillResultCodec() {
        SkillResultS2CPacket original = new SkillResultS2CPacket(2, "SUCCESS", 42.5f);
        SkillResultS2CPacket decoded = roundTrip(SkillResultS2CPacket.CODEC, original);
        assertEquals(2, decoded.keyId());
        assertEquals("SUCCESS", decoded.result());
        assertEquals(42.5f, decoded.finalDamage(), 0.001f);
    }

    // 6. SkillUseC2SPacket — targetUuid 있는 경우
    @Test
    void testSkillUseC2SCodec_WithTarget() {
        UUID playerUuid = UUID.randomUUID();
        UUID targetUuid = UUID.randomUUID();
        SkillUseC2SPacket original = new SkillUseC2SPacket(playerUuid, (byte) 0, targetUuid);
        SkillUseC2SPacket decoded = roundTrip(SkillUseC2SPacket.CODEC, original);
        assertEquals(playerUuid,  decoded.playerUuid());
        assertEquals((byte) 0,    decoded.keyId());
        assertEquals(targetUuid,  decoded.targetUuid());
    }

    // 7. SkillUseC2SPacket — targetUuid null (단독 사용)
    @Test
    void testSkillUseC2SCodec_NoTarget() {
        UUID playerUuid = UUID.randomUUID();
        SkillUseC2SPacket original = new SkillUseC2SPacket(playerUuid, (byte) 2, null);
        SkillUseC2SPacket decoded = roundTrip(SkillUseC2SPacket.CODEC, original);
        assertEquals(playerUuid, decoded.playerUuid());
        assertEquals((byte) 2,   decoded.keyId());
        assertNull(decoded.targetUuid(), "targetUuid=null 왕복 확인");
    }

    // 8. CharacterSelectFailS2CPacket (String reason) 왕복
    @Test
    void testCharacterSelectFailCodec() {
        CharacterSelectFailS2CPacket original = new CharacterSelectFailS2CPacket("duplicate_character");
        CharacterSelectFailS2CPacket decoded = roundTrip(CharacterSelectFailS2CPacket.CODEC, original);
        assertEquals("duplicate_character", decoded.reason());
    }

    // 9. RespawnS2CPacket (int delayTicks) 왕복
    @Test
    void testRespawnCodec() {
        RespawnS2CPacket original = new RespawnS2CPacket(100);
        RespawnS2CPacket decoded = roundTrip(RespawnS2CPacket.CODEC, original);
        assertEquals(100, decoded.delayTicks());
    }
}
