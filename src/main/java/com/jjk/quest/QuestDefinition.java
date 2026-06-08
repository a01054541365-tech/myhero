package com.jjk.quest;

public record QuestDefinition(
    String id,
    String name,
    String description,
    QuestType type,
    QuestCondition condition,
    int rewardXp,
    int rewardCrystals
) {}
