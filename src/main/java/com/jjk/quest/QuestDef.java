package com.jjk.quest;

public record QuestDef(
    String questId,
    String description,
    String type,        // "kill_spirit", "black_flash", "pvp", "tool_win"
    int    target,
    long   stoneReward,
    int    xpReward,
    boolean isWeekly
) {}
