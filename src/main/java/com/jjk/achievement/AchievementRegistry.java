package com.jjk.achievement;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AchievementRegistry {

    private static final Map<String, JJKAchievement> REGISTRY = new LinkedHashMap<>();

    static {
        reg("first_character",      "주력 각성",                          "첫 캐릭터 선택 완료",                false);
        reg("first_kill",           "저주받은 세계에 오신 것을 환영합니다", "첫 주령 처치",                      false);
        reg("first_grade_up",       "주술사의 길",                         "4급 → 3급 등급 상승",               false);
        reg("first_domain",         "첫 영역",                             "영역 전개 첫 성공",                  false);
        reg("black_flash_perfect",  "흑섬 — 완 (Perfect)",                 "Perfect 흑섬 첫 발동",              false);
        reg("zone_entry",           "무아지경",                            "Zone 진입 첫 성공",                  false);
        reg("triple_black_flash",   "연속 흑섬",                           "10초 내 흑섬 3회 연속 성공",        false);
        reg("domain_collision_win", "영역 충돌 승리자",                    "영역 충돌 우선순위 1위 생존",        false);
        reg("first_finger",         "료멘 스쿠나의 기억",                  "손가락 1개 획득",                   false);
        reg("ten_fingers",          "저주받은 손가락 수집가",              "손가락 10개 획득",                  false);
        reg("all_fingers",          "완전부활",                            "손가락 20개 수집",                   false);
        reg("five_tools",           "주구 사냥꾼",                         "주구 5종 획득",                      false);
        reg("special_tool",         "천역모의 행방",                       "특급 주구 1개 획득",                 false);
        reg("first_special_grade",  "사멸회유의 흔적",                     "야생 특급 주령 첫 조우",             false);
        reg("night_hunter",         "깊은 밤의 사냥꾼",                    "야간 주령 50마리 처치",              false);
        reg("gojo_domain_win",      "모든 걸 아는 자",                     "고죠 영역 충돌 승리",               true);
        reg("seal_then_kill",       "이타도리의 선택",                     "속박 선언 직후 첫 타격 즉사",        true);
        reg("mahito_triple_soul",   "탈출 불가",                           "마히토 영역 3명 동시 영혼 데미지",   true);
        reg("sukuna_full_power",    "저주의 왕",                           "스쿠나 손가락 20개+Zone+영역 동시", true);
        reg("syouto_find",          "승두를 발견했다",                     "승두를 발로 처치",                  true);
        reg("attack_npc",           "NPC를 건드리지 마라",                 "쿠사카베 공격 시도",                true);
    }

    private static void reg(String id, String displayName, String description, boolean isHidden) {
        REGISTRY.put(id, new JJKAchievement(id, displayName, description, isHidden));
    }

    public static JJKAchievement get(String id) {
        return REGISTRY.get(id);
    }

    public static Map<String, JJKAchievement> all() {
        return Collections.unmodifiableMap(REGISTRY);
    }
}
