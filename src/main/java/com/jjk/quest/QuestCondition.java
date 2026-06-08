package com.jjk.quest;

/** 퀘스트 달성 조건 — type 이벤트가 target 횟수 이상 발생하면 완료. */
public record QuestCondition(String type, int target) {}
