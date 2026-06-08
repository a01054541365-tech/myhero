package com.jjk.quest;

import java.util.List;

public final class QuestRegistry {

    private static final List<QuestDefinition> ALL = List.of(
        // ─── 일일 퀘스트 8종 ──────────────────────────────────────────────────────
        new QuestDefinition("DAILY_01", "첫 전투",
            "전투에서 승리하세요.",
            QuestType.DAILY, new QuestCondition("pvp", 1),
            50, 5),
        new QuestDefinition("DAILY_02", "주령 사냥",
            "주령 3마리를 처치하세요.",
            QuestType.DAILY, new QuestCondition("kill_spirit", 3),
            80, 10),
        new QuestDefinition("DAILY_03", "흑섬 발동",
            "흑섬을 1회 발동하세요.",
            QuestType.DAILY, new QuestCondition("black_flash", 1),
            60, 8),
        new QuestDefinition("DAILY_04", "CE 관리",
            "스킬을 사용하지 않고 20회 이상 공격하세요.",
            QuestType.DAILY, new QuestCondition("skill_use", 20),
            40, 5),
        new QuestDefinition("DAILY_05", "특급 주령 도전",
            "특급 주령에게 피해를 입히세요.",
            QuestType.DAILY, new QuestCondition("damage_special", 1),
            120, 20),
        new QuestDefinition("DAILY_06", "수련",
            "스킬을 20회 사용하세요.",
            QuestType.DAILY, new QuestCondition("skill_use", 20),
            50, 5),
        new QuestDefinition("DAILY_07", "생존",
            "오늘 처치당하지 마세요.",
            QuestType.DAILY, new QuestCondition("survive", 1),
            70, 8),
        new QuestDefinition("DAILY_08", "첫 접속",
            "오늘 처음 접속하세요.",
            QuestType.DAILY, new QuestCondition("login", 1),
            30, 3),

        // ─── 주간 퀘스트 7종 ──────────────────────────────────────────────────────
        new QuestDefinition("WEEKLY_01", "주령 섬멸",
            "주령 30마리를 처치하세요.",
            QuestType.WEEKLY, new QuestCondition("kill_spirit", 30),
            500, 80),
        new QuestDefinition("WEEKLY_02", "흑섬 달인",
            "흑섬 Perfect를 5회 발동하세요.",
            QuestType.WEEKLY, new QuestCondition("black_flash_perfect", 5),
            400, 60),
        new QuestDefinition("WEEKLY_03", "강자와의 전투",
            "더 높은 등급의 플레이어를 처치하세요.",
            QuestType.WEEKLY, new QuestCondition("pvp", 1),
            600, 100),
        new QuestDefinition("WEEKLY_04", "손가락 수집",
            "스쿠나의 손가락을 1개 이상 획득하세요.",
            QuestType.WEEKLY, new QuestCondition("finger_drop", 1),
            800, 120),
        new QuestDefinition("WEEKLY_05", "영역 전개",
            "영역 전개를 1회 성공하세요.",
            QuestType.WEEKLY, new QuestCondition("domain_deploy", 1),
            500, 80),
        new QuestDefinition("WEEKLY_06", "주술사의 길",
            "등급 경험치를 300 이상 획득하세요.",
            QuestType.WEEKLY, new QuestCondition("grade_xp", 300),
            400, 60),
        new QuestDefinition("WEEKLY_07", "속박 달인",
            "속박 선언을 3회 성공하세요.",
            QuestType.WEEKLY, new QuestCondition("binding_vow", 3),
            350, 50)
    );

    public static List<QuestDefinition> getAll() { return ALL; }

    public static List<QuestDefinition> getDaily() {
        return ALL.stream().filter(q -> q.type() == QuestType.DAILY).toList();
    }

    public static List<QuestDefinition> getWeekly() {
        return ALL.stream().filter(q -> q.type() == QuestType.WEEKLY).toList();
    }

    private QuestRegistry() {}
}
