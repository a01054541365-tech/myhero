package com.jjk.test;

import com.jjk.JJKMod;
import com.jjk.JjkConfig;
import com.jjk.trial.TrialStateMachine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TrialStateMachineTest {

    @BeforeAll
    static void setupJJKMod() throws Exception {
        // DELIBERATION→VERDICT transition calls JJKMod.getConfig(); inject a minimal instance
        Field instanceField = JJKMod.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        JJKMod mod = JJKMod.class.getDeclaredConstructor().newInstance();
        Field configField = JJKMod.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(mod, new JjkConfig());
        instanceField.set(null, mod);
    }

    @Test
    void testInitialStateIsIdle() {
        TrialStateMachine tsm = new TrialStateMachine(null);
        assertEquals(TrialStateMachine.State.IDLE, tsm.getState());
    }

    @Test
    void testAccuseTransition() {
        TrialStateMachine tsm = new TrialStateMachine(null);
        tsm.accuse(UUID.randomUUID(), UUID.randomUUID());
        assertEquals(TrialStateMachine.State.ACCUSED, tsm.getState());
    }

    @Test
    void testAccusedToDeliberation() {
        TrialStateMachine tsm = new TrialStateMachine(null);
        tsm.accuse(UUID.randomUUID(), UUID.randomUUID());
        for (int i = 0; i < 20; i++) tsm.tick();
        assertEquals(TrialStateMachine.State.DELIBERATION, tsm.getState());
    }

    @Test
    void testDeliberationToVerdict() {
        TrialStateMachine tsm = new TrialStateMachine(null);
        tsm.accuse(UUID.randomUUID(), UUID.randomUUID());
        // 20 ticks → DELIBERATION, 60 more ticks → VERDICT
        for (int i = 0; i < 80; i++) tsm.tick();
        TrialStateMachine.State state = tsm.getState();
        assertTrue(
            state == TrialStateMachine.State.VERDICT || state == TrialStateMachine.State.END,
            "Expected VERDICT or END but was " + state
        );
    }

    @Test
    void testDuplicateAccuseIgnored() throws Exception {
        TrialStateMachine tsm = new TrialStateMachine(null);
        UUID firstProsecutor = UUID.randomUUID();
        UUID secondProsecutor = UUID.randomUUID();
        tsm.accuse(firstProsecutor, UUID.randomUUID());
        // second accuse is a no-op because state is no longer IDLE
        tsm.accuse(secondProsecutor, UUID.randomUUID());

        Field field = TrialStateMachine.class.getDeclaredField("prosecutorUuid");
        field.setAccessible(true);
        assertEquals(firstProsecutor, field.get(tsm));
    }
}
