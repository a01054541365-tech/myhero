package com.jjk.anim;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class AnimationRegistry {

    private static final Map<Integer, String> REGISTRY = Collections.unmodifiableMap(register());

    private AnimationRegistry() {}

    private static Map<Integer, String> register() {
        Map<Integer, String> m = new HashMap<>(64);
        m.put(0,  "common_idle");
        m.put(1,  "common_cast");
        m.put(2,  "common_hit");
        m.put(3,  "common_dash");
        m.put(4,  "common_guard");
        m.put(5,  "common_death");
        m.put(6,  "common_respawn");
        m.put(7,  "common_domain_start");
        m.put(8,  "common_domain_loop");
        m.put(9,  "common_black_flash");
        m.put(10, "itadori_f");
        m.put(11, "itadori_shift_f");
        m.put(12, "itadori_r");
        m.put(13, "itadori_shift_r");
        m.put(14, "itadori_v");
        m.put(15, "gojo_f");
        m.put(16, "gojo_shift_f");
        m.put(17, "gojo_r");
        m.put(18, "gojo_shift_r");
        m.put(19, "gojo_domain");
        m.put(20, "gojo_v");
        m.put(21, "sukuna_f");
        m.put(22, "sukuna_shift_f");
        m.put(23, "sukuna_r");
        m.put(24, "sukuna_shift_r");
        m.put(25, "megumi_f");
        m.put(26, "megumi_shift_f");
        m.put(27, "megumi_r");
        m.put(28, "megumi_shift_r");
        m.put(29, "megumi_v");
        m.put(30, "jogo_f");
        m.put(31, "jogo_r");
        m.put(32, "jogo_v");
        m.put(33, "mahito_f");
        m.put(34, "mahito_r");
        m.put(35, "mahito_domain");
        m.put(36, "hakari_f");
        m.put(37, "hakari_v");
        m.put(38, "higuruma_f");
        m.put(39, "higuruma_v");
        m.put(40, "okkotsu_shift_f");
        m.put(41, "okkotsu_shift_r_burst");
        m.put(42, "okkotsu_v");
        m.put(43, "mahito_shift_f");
        m.put(44, "mahito_shift_r");
        m.put(45, "mahito_v");
        m.put(46, "jogo_shift_f");
        m.put(47, "jogo_shift_r");
        m.put(48, "hakari_shift_f");
        m.put(49, "hakari_shift_r");
        m.put(50, "inumaki_f_stop");
        m.put(51, "inumaki_shift_f_burst");
        m.put(52, "inumaki_shift_r_sleep");
        m.put(53, "inumaki_v_run");
        m.put(54, "nanami_shift_r");
        m.put(55, "higuruma_shift_r_jury");
        m.put(56, "black_flash_great");
        m.put(57, "awakening_enter");
        m.put(58, "zone_enter");
        m.put(59, "itadori_domain_startup");
        // 나나미 신규 스킬 — 56·57·58은 기존 공용 이펙트(black_flash_great·awakening_enter·zone_enter)가 사용 중
        m.put(60, "nanami_dismantle");
        m.put(61, "nanami_overtime");
        m.put(62, "nanami_ten_puncture");
        m.put(63, "higuruma_argument");
        m.put(64, "higuruma_evidence");
        return m;
    }

    /** 등록된 animId 이름 반환. 없으면 IllegalArgumentException. */
    public static String get(int id) {
        String name = REGISTRY.get(id);
        if (name == null) throw new IllegalArgumentException("Unknown animId: " + id);
        return name;
    }

    public static boolean has(int id) {
        return REGISTRY.containsKey(id);
    }
}
