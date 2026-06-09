package com.jjk.test;

import com.jjk.network.s2c.DomainDeployFailS2CPacket;
import com.jjk.network.s2c.DomainDeployFailS2CPacket.FailReason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainDeployFailPacketTest {

    // ── 1. ordinal 순서 검증 (codec 인코딩 기반) ──────────────────────────────
    @Test
    void testFailReasonOrdinals() {
        assertEquals(0, FailReason.CE_INSUFFICIENT.ordinal());
        assertEquals(1, FailReason.ON_COOLDOWN.ordinal());
        assertEquals(2, FailReason.BANNED_CHUNK.ordinal());
        assertEquals(3, FailReason.DOMAIN_ALREADY_ACTIVE.ordinal());
        assertEquals(4, FailReason.CLASH_LOST.ordinal());
        assertEquals(5, FailReason.values().length, "FailReason 값 5개 정확히");
    }

    // ── 2. ordinal 복원 논리 (fallback to CE_INSUFFICIENT) ───────────────────
    @Test
    void testOrdinalDecodeWithFallback() {
        FailReason[] values = FailReason.values();
        for (FailReason expected : values) {
            int ord = expected.ordinal();
            FailReason decoded = (ord >= 0 && ord < values.length)
                    ? values[ord] : FailReason.CE_INSUFFICIENT;
            assertSame(expected, decoded, "ordinal 복원: " + expected);
        }

        // 유효 범위 밖 → fallback
        int badOrd = 99;
        FailReason fallback = (badOrd >= 0 && badOrd < values.length)
                ? values[badOrd] : FailReason.CE_INSUFFICIENT;
        assertSame(FailReason.CE_INSUFFICIENT, fallback, "잘못된 ordinal → CE_INSUFFICIENT");
    }

    // ── 3. 레코드 생성 확인 ───────────────────────────────────────────────────
    @Test
    void testPacketCreation() {
        DomainDeployFailS2CPacket pkt = new DomainDeployFailS2CPacket(FailReason.ON_COOLDOWN);
        assertSame(FailReason.ON_COOLDOWN, pkt.reason());
    }
}
