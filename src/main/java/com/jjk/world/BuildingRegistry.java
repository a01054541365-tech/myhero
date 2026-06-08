package com.jjk.world;

import com.google.gson.*;
import com.jjk.world.structure.BuildingInstance;
import com.jjk.world.structure.NpcSpawnPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 건물 생성 결과를 config/jjk/buildings.json 에 영속 저장. */
public final class BuildingRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger("jjk");
    private static final Path REGISTRY_PATH = Path.of("config/jjk/buildings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final List<BuildingInstance> buildings = new ArrayList<>();

    private BuildingRegistry() {}

    public static BuildingRegistry load() {
        BuildingRegistry registry = new BuildingRegistry();
        if (!Files.exists(REGISTRY_PATH)) return registry;
        try (Reader reader = Files.newBufferedReader(REGISTRY_PATH)) {
            JsonArray arr = GSON.fromJson(reader, JsonArray.class);
            if (arr == null) return registry;
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String type = obj.get("type").getAsString();
                int x = obj.get("x").getAsInt();
                int y = obj.get("y").getAsInt();
                int z = obj.get("z").getAsInt();
                List<NpcSpawnPoint> points = new ArrayList<>();
                JsonArray npcArr = obj.getAsJsonArray("npcSpawnPoints");
                if (npcArr != null) {
                    for (JsonElement npcEl : npcArr) {
                        JsonObject npc = npcEl.getAsJsonObject();
                        points.add(new NpcSpawnPoint(
                            npc.get("npcId").getAsString(),
                            npc.get("npcDisplayName").getAsString(),
                            npc.get("dx").getAsInt(),
                            npc.get("dy").getAsInt(),
                            npc.get("dz").getAsInt(),
                            npc.get("yaw").getAsFloat()
                        ));
                    }
                }
                registry.buildings.add(new BuildingInstance(type, x, y, z, points));
            }
        } catch (Exception e) {
            LOGGER.warn("[JJK] BuildingRegistry 로드 실패: {}", e.getMessage());
        }
        return registry;
    }

    public void save() {
        try {
            Files.createDirectories(REGISTRY_PATH.getParent());
            JsonArray arr = new JsonArray();
            for (BuildingInstance b : buildings) {
                JsonObject obj = new JsonObject();
                obj.addProperty("type", b.type);
                obj.addProperty("x", b.x);
                obj.addProperty("y", b.y);
                obj.addProperty("z", b.z);
                JsonArray npcArr = new JsonArray();
                for (NpcSpawnPoint sp : b.npcSpawnPoints) {
                    JsonObject npc = new JsonObject();
                    npc.addProperty("npcId", sp.npcId());
                    npc.addProperty("npcDisplayName", sp.npcDisplayName());
                    npc.addProperty("dx", sp.dx());
                    npc.addProperty("dy", sp.dy());
                    npc.addProperty("dz", sp.dz());
                    npc.addProperty("yaw", sp.yaw());
                    npcArr.add(npc);
                }
                obj.add("npcSpawnPoints", npcArr);
                arr.add(obj);
            }
            Files.writeString(REGISTRY_PATH, GSON.toJson(arr));
        } catch (Exception e) {
            LOGGER.warn("[JJK] BuildingRegistry 저장 실패: {}", e.getMessage());
        }
    }

    public void add(BuildingInstance b) { buildings.add(b); }
    public List<BuildingInstance> getAll() { return Collections.unmodifiableList(buildings); }
    public boolean isEmpty() { return buildings.isEmpty(); }
}
