package com.jjk.world.structure;

/** 건물 기준점(origin)으로부터의 상대 좌표 + NPC 식별 정보. */
public record NpcSpawnPoint(
    String npcId,
    String npcDisplayName,
    int dx, int dy, int dz,
    float yaw
) {}
