package com.jjk.world.structure;

import java.util.List;

/** 생성된 건물 한 인스턴스의 월드 위치와 NPC 스폰 포인트 목록. */
public final class BuildingInstance {
    public final String type;
    public final int x, y, z;
    public final List<NpcSpawnPoint> npcSpawnPoints;

    public BuildingInstance(String type, int x, int y, int z, List<NpcSpawnPoint> npcSpawnPoints) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.z = z;
        this.npcSpawnPoints = List.copyOf(npcSpawnPoints);
    }
}
