package com.jjk.test;

import com.jjk.JjkConfig;
import com.jjk.data.Grade;
import com.jjk.data.PlayerData;
import com.jjk.domain.DomainDefinition;
import com.jjk.domain.DomainInstance;
import com.jjk.domain.DomainManager;
import com.jjk.domain.DomainPriorityCalculator;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DomainManagerTest {

    private static DomainInstance closedInst(float wallHp, float radius) {
        return new DomainInstance(UUID.randomUUID(), "test", new BlockPos(0, 0, 0),
                wallHp, radius, false, false, true, false);
    }

    private static DomainInstance openInst(float radius) {
        return new DomainInstance(UUID.randomUUID(), "test", new BlockPos(0, 0, 0),
                1500f, radius, true, false, true, false); // wallHp 전달값 무시됨
    }

    private static DomainDefinition makeDef(float ceCost, boolean isOpen, float wallHp,
                                            float radius, int cooldownTicks) {
        DomainDefinition def = new DomainDefinition();
        def.domainId = "test_domain";
        def.ceCost = ceCost;
        def.isOpen = isOpen;
        def.wallHp = wallHp;
        def.radius = radius;
        def.cooldownTicks = cooldownTicks;
        def.sureHitActive = true;
        def.autoTargetAll = false;
        def.isIncomplete = false;
        return def;
    }

    // 1. 결계형 DomainInstance 생성 시 wallHp = domains.json 값(1500) 확인
    @Test
    void testWallHpFromDomainsJson() {
        DomainInstance inst = closedInst(1500f, 20f);
        assertEquals(1500f, inst.wallHp, 0.001f, "결계형 wallHp = 1500 (§8-3)");
    }

    // 2. isOpen=true 이면 wallHp = 0 (생성자에서 강제)
    @Test
    void testOpenDomainWallHpZero() {
        DomainInstance inst = openInst(20f);
        assertEquals(0f, inst.wallHp, 0.001f, "개방형 wallHp = 0");
    }

    // 3. 같은 팀 영역 2개 → applyTeamLimit() → currentRadius = maxRadius/2
    @Test
    void testTeamDomainLimit() {
        DomainInstance inst = closedInst(1500f, 20f);
        inst.applyTeamLimit(2);
        assertEquals(10f, inst.currentRadius, 0.001f, "팀 2개 → currentRadius = maxRadius/2");
    }

    // 4. 팀 영역 1개 종료 시 restoreRadius() → currentRadius = maxRadius
    @Test
    void testRestoreRadiusOnCollapse() {
        DomainInstance inst = closedInst(1500f, 20f);
        inst.applyTeamLimit(2);
        assertEquals(10f, inst.currentRadius, 0.001f);
        inst.restoreRadius();
        assertEquals(20f, inst.currentRadius, 0.001f, "restoreRadius → maxRadius 복원");
    }

    // 5. isOpen=true 영역 CE = ceCost × 2 (decisions §3-4)
    @Test
    void testOpenDomainCeDouble() {
        DomainDefinition def = makeDef(100f, true, 0f, 20f, 200);
        PlayerData attacker = PlayerData.createDefault(UUID.randomUUID());
        attacker.ceCurrent = 300f;
        attacker.ceMax = 300f;

        DomainManager mgr = new DomainManager(new JjkConfig());
        boolean deployed = mgr.deployDomainData(attacker, def, 0L);

        assertTrue(deployed, "전개 성공");
        assertEquals(100f, attacker.ceCurrent, 0.001f, "개방형 CE: 300 - (100×2) = 100");
    }

    // 6. 전개 후 data.domainCooldownUntil > tick
    @Test
    void testDomainCooldownUntilSet() {
        DomainDefinition def = makeDef(10f, false, 1500f, 20f, 300);
        PlayerData attacker = PlayerData.createDefault(UUID.randomUUID());
        attacker.ceCurrent = 200f;
        attacker.ceMax = 200f;
        long tick = 1000L;

        DomainManager mgr = new DomainManager(new JjkConfig());
        mgr.deployDomainData(attacker, def, tick);

        assertTrue(attacker.domainCooldownUntil > tick,
                "전개 후 domainCooldownUntil=" + attacker.domainCooldownUntil + " > tick=" + tick);
    }

    // 7. 동일 등급·숙련도, 결계형 vs 개방형 → 결계형 priority > 개방형 priority
    @Test
    void testPriorityCalculation() {
        PlayerData owner = PlayerData.createDefault(UUID.randomUUID());
        owner.grade = Grade.GRADE_1;
        owner.mastery = 50;
        owner.ceCurrent = 500f;
        owner.ceMax = 500f;

        DomainInstance closed = closedInst(1500f, 20f);
        DomainInstance open   = openInst(20f);

        float closedPriority = DomainPriorityCalculator.calculate(owner, closed);
        float openPriority   = DomainPriorityCalculator.calculate(owner, open);

        assertTrue(closedPriority > openPriority,
                "결계형(" + closedPriority + ") > 개방형(" + openPriority + ")");
    }

    // 8. 특급 vs 4급 충돌 → resolveConflict가 스펙 공식 기반으로 특급 승리
    @Test
    void conflictResolution_higherGradeWins() {
        DomainPriorityCalculator calc = new DomainPriorityCalculator();

        PlayerData special = PlayerData.createDefault(UUID.randomUUID());
        special.grade = Grade.SPECIAL;
        special.mastery = 0;
        special.ceCurrent = 100f;
        special.ceMax = 100f;

        PlayerData grade4 = PlayerData.createDefault(UUID.randomUUID());
        grade4.grade = Grade.GRADE_4;
        grade4.mastery = 0;
        grade4.ceCurrent = 100f;
        grade4.ceMax = 100f;

        DomainInstance domSpecial = closedInst(1500f, 20f);
        DomainInstance domGrade4  = closedInst(1500f, 20f);

        DomainInstance winner = calc.resolveConflict(domSpecial, special, domGrade4, grade4);
        assertSame(domSpecial, winner, "특급 > 4급 → 특급 승리 (§8-2 스펙 공식)");
    }

    // 9. 동점 시 resolveConflict는 동일 입력에 항상 같은 결과 반환 (결정적)
    @Test
    void conflictResolution_tieBreak_deterministic() {
        DomainPriorityCalculator calc = new DomainPriorityCalculator();

        // 동일한 grade/mastery/ce/wallHp → 우선순위 동점
        PlayerData dataA = PlayerData.createDefault(UUID.randomUUID());
        dataA.grade = Grade.GRADE_4; dataA.mastery = 0; dataA.ceCurrent = 0f; dataA.ceMax = 100f;

        PlayerData dataB = PlayerData.createDefault(UUID.randomUUID());
        dataB.grade = Grade.GRADE_4; dataB.mastery = 0; dataB.ceCurrent = 0f; dataB.ceMax = 100f;

        DomainInstance domA = closedInst(0f, 20f);
        DomainInstance domB = closedInst(0f, 20f);

        DomainInstance result1 = calc.resolveConflict(domA, dataA, domB, dataB);
        DomainInstance result2 = calc.resolveConflict(domA, dataA, domB, dataB);
        assertSame(result1, result2, "동점 시 동일 입력 → 항상 같은 결과 (UUID hashCode 기반)");
    }
}
