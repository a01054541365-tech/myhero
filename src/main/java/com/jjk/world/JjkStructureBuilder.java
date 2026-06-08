package com.jjk.world;

import com.jjk.world.structure.NpcSpawnPoint;
import net.minecraft.block.*;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

public final class JjkStructureBuilder {

    private final ServerWorld world;
    private final BlockPos origin;

    public JjkStructureBuilder(ServerWorld world, BlockPos origin) {
        this.world  = world;
        this.origin = origin;
    }

    /** 모든 구조체를 생성하고 NPC 스폰 포인트 목록을 반환. */
    public List<NpcSpawnPoint> buildAll() {
        buildMainBuilding();
        buildAnnex();
        buildOfficeRoom();
        buildUndergroundB1();
        buildUndergroundB2();
        buildTrainingField();
        buildDungeon();
        spawnTrainingDummies();
        spawnGuardStands();
        spawnYagaDolls();
        return collectNpcSpawnPoints();
    }

    private List<NpcSpawnPoint> collectNpcSpawnPoints() {
        return List.of(
            new NpcSpawnPoint("ijichi",        "이치지 키요타카",  -15,  1, -12, 180f),
            new NpcSpawnPoint("kusakabe",      "쿠사카베 아츠야",  -15,  9,   5, 270f),
            new NpcSpawnPoint("nahobino",      "나호비노 아오이",  -10,  9,   5, 270f),
            new NpcSpawnPoint("shoko",         "이에이리 쇼코",    -35,  1,  -8, 270f),
            new NpcSpawnPoint("zenin_storage", "젠인 창고지기",    -35,  1,   8, 270f),
            new NpcSpawnPoint("yaga",          "야가 마사모토",    -10, 19,   0, 180f),
            new NpcSpawnPoint("gojo_shiyu",    "공시우",           -15,-19,   0,   0f)
        );
    }

    // ── 공통 헬퍼 ──────────────────────────────────────────────────────────────

    private void set(int dx, int dy, int dz, Block block) {
        world.setBlockState(origin.add(dx, dy, dz),
            block.getDefaultState(), Block.NOTIFY_LISTENERS);
    }

    private void setState(int dx, int dy, int dz, BlockState state) {
        world.setBlockState(origin.add(dx, dy, dz), state, Block.NOTIFY_LISTENERS);
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        BlockState state = block.getDefaultState();
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++)
                    world.setBlockState(origin.add(x, y, z), state, Block.NOTIFY_LISTENERS);
    }

    private void hollow(int x1, int y1, int z1,
                        int x2, int y2, int z2,
                        Block wall, Block floor, Block ceiling) {
        fill(x1, y1, z1, x2, y1, z2, floor);
        fill(x1, y2, z1, x2, y2, z2, ceiling);
        fill(x1, y1 + 1, z1, x1, y2 - 1, z2, wall);
        fill(x2, y1 + 1, z1, x2, y2 - 1, z2, wall);
        fill(x1 + 1, y1 + 1, z1, x2 - 1, y2 - 1, z1, wall);
        fill(x1 + 1, y1 + 1, z2, x2 - 1, y2 - 1, z2, wall);
    }

    // ── 본관 ──────────────────────────────────────────────────────────────────

    private void buildMainBuilding() {
        build1F();
        build2F();
        build3F();
        buildStaircase();
    }

    private void build1F() {
        hollow(-20, 0, -15, 20, 8, 15,
            Blocks.DEEPSLATE_BRICKS, Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_SLAB);

        for (int[] corner : new int[][]{{-19, -14}, {-19, 14}, {19, -14}, {19, 14}}) {
            fill(corner[0], 0, corner[1], corner[0] + 1, 7, corner[1] + 1,
                Blocks.DEEPSLATE_TILES);
        }

        fill(-1, 0, -15, 1, 3, -15, Blocks.AIR);
        setState(-1, 1, -15, Blocks.IRON_DOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
            .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        setState(0, 1, -15, Blocks.IRON_DOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.NORTH)
            .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
        fill(-2, 4, -15, 2, 4, -15, Blocks.OAK_FENCE);

        fill(-19, 3, -5, -19, 5, 5, Blocks.GLASS_PANE);
        fill(19, 3, -5, 19, 5, 5, Blocks.GLASS_PANE);

        set(0, 0, 0, Blocks.GLOWSTONE);
        set(-15, 1, -12, Blocks.RED_CARPET);

        for (int[] pos : new int[][]{{-10, 7, -7}, {10, 7, -7}, {-10, 7, 7}, {10, 7, 7}}) {
            set(pos[0], pos[1], pos[2], Blocks.SEA_LANTERN);
        }

        for (int i = 0; i < 10; i++) {
            setState(10 + i, -i, 0, Blocks.DEEPSLATE_BRICK_STAIRS.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, Direction.EAST));
        }
    }

    private void build2F() {
        hollow(-20, 8, -15, 20, 18, 15,
            Blocks.DEEPSLATE_BRICKS, Blocks.BIRCH_PLANKS, Blocks.DEEPSLATE_BRICKS);

        fill(0, 8, -15, 0, 18, 15, Blocks.DEEPSLATE_BRICKS);
        fill(0, 9, -3, 0, 12, 3, Blocks.AIR);

        fill(1, 8, -14, 19, 8, 14, Blocks.SMOOTH_STONE);
        fill(1, 8, -14, 1, 8, 14, Blocks.WHITE_CONCRETE);
        fill(19, 8, -14, 19, 8, 14, Blocks.WHITE_CONCRETE);

        fill(-19, 11, -5, -19, 13, 5, Blocks.BLACK_CONCRETE);

        for (int[] pos : new int[][]{{-8, 17, -7}, {-8, 17, 7}, {10, 17, -7}, {10, 17, 7}}) {
            set(pos[0], pos[1], pos[2], Blocks.LANTERN);
        }
    }

    private void build3F() {
        hollow(-20, 18, -15, 20, 27, 15,
            Blocks.DEEPSLATE_BRICKS, Blocks.DARK_OAK_PLANKS, Blocks.DEEPSLATE_BRICKS);

        fill(5, 18, -15, 5, 27, 15, Blocks.DEEPSLATE_BRICKS);
        fill(5, 19, -3, 5, 22, 3, Blocks.AIR);

        fill(6, 18, -14, 19, 18, 14, Blocks.OBSIDIAN);
        fill(6, 19, -14, 6, 27, 14, Blocks.DEEPSLATE_TILES);
        fill(7, 19, -14, 19, 27, -14, Blocks.IRON_BARS);

        setState(5, 19, 0, Blocks.IRON_DOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.EAST)
            .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));

        fill(-18, 19, -10, -15, 23, -5, Blocks.BOOKSHELF);

        for (int z : new int[]{-5, 0, 5}) {
            set(-5, 19, z, Blocks.OAK_FENCE);
            set(-5, 20, z, Blocks.OAK_PRESSURE_PLATE);
        }

        for (int[] pos : new int[][]{{-10, 26, -7}, {-10, 26, 7}, {15, 26, -7}, {15, 26, 7}}) {
            setState(pos[0], pos[1], pos[2], Blocks.SOUL_LANTERN.getDefaultState());
        }
    }

    private void buildStaircase() {
        for (int i = 0; i < 8; i++) {
            setState(-18 + i, 1 + i, -13, Blocks.DEEPSLATE_BRICK_STAIRS.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, Direction.EAST));
        }
        for (int i = 0; i < 8; i++) {
            setState(-18 + i, 9 + i, -13, Blocks.DEEPSLATE_BRICK_STAIRS.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, Direction.EAST));
        }
    }

    // ── 별관 ──────────────────────────────────────────────────────────────────

    private void buildAnnex() {
        hollow(-45, 0, -20, -25, 9, 20,
            Blocks.STONE_BRICKS, Blocks.WHITE_TERRACOTTA, Blocks.DEEPSLATE_BRICKS);

        fill(-44, 0, 0, -26, 9, 0, Blocks.STONE_BRICKS);
        fill(-35, 1, 0, -30, 4, 0, Blocks.AIR);

        fill(-44, 0, -19, -26, 0, -1, Blocks.WHITE_TERRACOTTA);
        fill(-44, 0, 1, -26, 0, 19, Blocks.SPRUCE_PLANKS);

        for (int i = 0; i < 4; i++) {
            setState(-30 - i * 3, 1, -18, Blocks.RED_BED.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, Direction.SOUTH)
                .with(Properties.BED_PART, BedPart.HEAD));
        }

        set(-44, 1, -15, Blocks.WHITE_SHULKER_BOX);
        set(-44, 1, -10, Blocks.WHITE_SHULKER_BOX);

        for (int i = 0; i < 5; i++) {
            set(-30 - i * 2, 1, 15, Blocks.OAK_FENCE);
            set(-30 - i * 2, 2, 15, Blocks.OAK_PRESSURE_PLATE);
        }

        fill(-25, 1, -5, -20, 4, 5, Blocks.AIR);
        fill(-25, 0, -5, -20, 0, 5, Blocks.QUARTZ_BLOCK);
        fill(-25, 5, -5, -20, 5, 5, Blocks.QUARTZ_SLAB);

        for (int z : new int[]{-15, -7, 7, 15}) {
            set(-35, 8, z, Blocks.LANTERN);
        }
    }

    // ── 지하 B1 ───────────────────────────────────────────────────────────────

    private void buildUndergroundB1() {
        hollow(-15, -10, -15, 15, -1, 15,
            Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS);

        for (int[] pos : new int[][]{{-8, -2, -8}, {8, -2, -8}, {-8, -2, 8}, {8, -2, 8}}) {
            setState(pos[0], pos[1], pos[2], Blocks.SOUL_LANTERN.getDefaultState());
        }

        set(-5, -9, 0, Blocks.RED_CARPET);
        set(5, -9, 0, Blocks.RED_CARPET);

        setState(0, -9, 8, Blocks.IRON_DOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.SOUTH)
            .with(Properties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));

        fill(-14, -1, -14, 14, -1, 14, Blocks.POINTED_DRIPSTONE);
    }

    // ── 지하 B2 ───────────────────────────────────────────────────────────────

    private void buildUndergroundB2() {
        hollow(-25, -20, -25, 25, -11, 25,
            Blocks.CRACKED_NETHER_BRICKS, Blocks.GRAVEL, Blocks.CRACKED_DEEPSLATE_BRICKS);

        fill(-24, -19, -24, -1, -19, 24, Blocks.BLACK_CONCRETE);
        for (int z : new int[]{-20, -10, 0, 10, 20}) {
            setState(-20, -12, z, Blocks.SOUL_TORCH.getDefaultState());
        }

        set(-15, -19, 0, Blocks.PURPLE_CARPET);

        for (int i = 0; i < 3; i++) {
            setState(-20 + i * 3, -19, -5, Blocks.NETHER_BRICK_STAIRS.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, Direction.NORTH));
        }

        fill(1, -19, -24, 24, -19, 24, Blocks.MOSSY_COBBLESTONE);
        for (int[] pos : new int[][]{{10, -12, -10}, {10, -12, 10}, {20, -12, -10}, {20, -12, 10}}) {
            set(pos[0], pos[1], pos[2], Blocks.SEA_LANTERN);
        }

        fill(20, -19, -2, 20, -14, 2, Blocks.OBSIDIAN);

        hollow(15, -19, -3, 18, -17, -1,
            Blocks.IRON_BARS, Blocks.IRON_BARS, Blocks.IRON_BARS);
    }

    // ── 훈련 필드 ─────────────────────────────────────────────────────────────

    private void buildTrainingField() {
        fill(-40, 0, 20, 40, 0, 80, Blocks.GRASS_BLOCK);
        fill(-10, 0, 30, 10, 0, 50, Blocks.SMOOTH_STONE);

        fill(-40, 0, 20, 40, 0, 20, Blocks.WHITE_CONCRETE);
        fill(-40, 0, 80, 40, 0, 80, Blocks.WHITE_CONCRETE);
        fill(-40, 0, 20, -40, 0, 80, Blocks.WHITE_CONCRETE);
        fill(40, 0, 20, 40, 0, 80, Blocks.WHITE_CONCRETE);

        for (int i = 0; i < 5; i++) {
            fill(15 + i, 1 + i, 30, 15 + i, 1 + i, 50, Blocks.OAK_STAIRS);
        }
        for (int i = 0; i < 5; i++) {
            fill(-15 - i, 1 + i, 30, -15 - i, 1 + i, 50, Blocks.OAK_STAIRS);
        }

        int[][] markers = {
            {-30, 25}, {0, 25}, {30, 25},
            {-30, 50}, {30, 50},
            {-30, 75}, {0, 75}, {30, 75}
        };
        for (int[] m : markers) {
            set(m[0], 0, m[1], Blocks.OBSIDIAN);
            for (int d : new int[]{-1, 1}) {
                set(m[0] + d, 0, m[1], Blocks.OBSIDIAN);
                set(m[0], 0, m[1] + d, Blocks.OBSIDIAN);
            }
        }

        int[][] torches = {
            {-40, 20}, {0, 20}, {40, 20},
            {-40, 50}, {40, 50},
            {-40, 80}, {0, 80}, {40, 80}
        };
        for (int[] t : torches) {
            setState(t[0], 1, t[1], Blocks.TORCH.getDefaultState());
        }
    }

    // ── 던전 ──────────────────────────────────────────────────────────────────

    private void buildDungeon() {
        // 입구 홀 (Z:100~115, Y:-44~-34)
        hollow(0, -44, 100, 20, -34, 115,
            Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS);

        // 1구역 — 4급 주령 ×4 (Z:116~130)
        hollow(0, -44, 116, 20, -34, 130,
            Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS);
        for (int[] m : new int[][]{{5,-43,120},{15,-43,120},{5,-43,126},{15,-43,126}}) {
            set(m[0], m[1], m[2], Blocks.OBSIDIAN);
        }

        // 2구역 — 3급 주령 ×2 (Z:131~150)
        hollow(0, -44, 131, 20, -34, 150,
            Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS);

        // 3구역 — 2급 주령 ×1 (Z:151~165)
        hollow(0, -44, 151, 20, -34, 165,
            Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICKS);

        // 보스방 — 1급 주령 ×1 (Z:166~180)
        hollow(-5, -44, 166, 25, -30, 180,
            Blocks.DEEPSLATE_TILES, Blocks.OBSIDIAN, Blocks.DEEPSLATE_BRICKS);
        for (int[] p : new int[][]{{0,-31,170},{20,-31,170},{0,-31,178},{20,-31,178}}) {
            set(p[0], p[1], p[2], Blocks.SEA_LANTERN);
        }

        // 보상방 (Z:181~195)
        hollow(0, -44, 181, 20, -34, 195,
            Blocks.GILDED_BLACKSTONE, Blocks.GILDED_BLACKSTONE, Blocks.GILDED_BLACKSTONE);
        set(10, -43, 190, Blocks.CHEST);

        // 방 사이 철문 (잠금 상태)
        for (int z : new int[]{120, 140, 160, 175, 190}) {
            setState(10, -43, z, Blocks.IRON_DOOR.getDefaultState()
                .with(Properties.HORIZONTAL_FACING, net.minecraft.util.math.Direction.NORTH)
                .with(Properties.DOUBLE_BLOCK_HALF,
                    net.minecraft.block.enums.DoubleBlockHalf.LOWER));
        }

        // 조명 (영혼 불꽃 랜턴)
        for (int z = 100; z <= 195; z += 10) {
            setState(10, -35, z, Blocks.SOUL_LANTERN.getDefaultState());
        }
    }

    // ── 사무실 공간 (2층 일부) ────────────────────────────────────────────────

    private void buildOfficeRoom() {
        // 2층 서쪽 코너 (x=-19 ~ -11, z=-14 ~ -6)에 사무실 배치
        fill(-19, 8, -14, -11, 8, -6, Blocks.BIRCH_PLANKS);
        // 사무용 책상 (oak_slab + oak_trapdoor)
        set(-18, 9, -13, Blocks.OAK_SLAB);
        setState(-18, 10, -13, Blocks.OAK_TRAPDOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.EAST)
            .with(Properties.OPEN, true));
        set(-14, 9, -13, Blocks.OAK_SLAB);
        setState(-14, 10, -13, Blocks.OAK_TRAPDOOR.getDefaultState()
            .with(Properties.HORIZONTAL_FACING, Direction.EAST)
            .with(Properties.OPEN, true));
        // 서가
        fill(-19, 9, -7, -19, 12, -7, Blocks.BOOKSHELF);
        // 조명
        set(-15, 17, -10, Blocks.LANTERN);
    }

    // ── 장식 엔티티 ───────────────────────────────────────────────────────────

    private void spawnTrainingDummies() {
        int[][] positions = {
            {3, 9, -10}, {6, 9, -10}, {9, 9, -10},
            {3, 9,  10}, {6, 9,  10}, {9, 9,  10}
        };
        for (int[] p : positions) {
            ArmorStandEntity stand = EntityType.ARMOR_STAND.create(world);
            if (stand == null) continue;
            BlockPos pos = origin.add(p[0], p[1], p[2]);
            stand.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(),
                pos.getZ() + 0.5, 180f, 0f);
            stand.setCustomName(Text.literal("§c훈련 허수아비"));
            stand.setCustomNameVisible(true);
            stand.setInvulnerable(true);
            stand.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
            world.spawnEntity(stand);
        }
    }

    private void spawnGuardStands() {
        for (int x : new int[]{-5, 5}) {
            ArmorStandEntity stand = EntityType.ARMOR_STAND.create(world);
            if (stand == null) continue;
            BlockPos pos = origin.add(x, -9, 0);
            stand.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(),
                pos.getZ() + 0.5, 180f, 0f);
            stand.setCustomName(Text.literal("§7금지 구역 경비"));
            stand.setCustomNameVisible(true);
            stand.setInvulnerable(true);
            stand.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            stand.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            world.spawnEntity(stand);
        }
    }

    private void spawnYagaDolls() {
        for (int i = 0; i < 3; i++) {
            int z = -5 + i * 5;
            ArmorStandEntity stand = EntityType.ARMOR_STAND.create(world);
            if (stand == null) continue;
            BlockPos pos = origin.add(-5, 21, z);
            stand.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(),
                pos.getZ() + 0.5, 180f, 0f);
            stand.setCustomName(Text.literal("§e주술 인형 #" + (i + 1)));
            stand.setCustomNameVisible(true);
            stand.setInvulnerable(true);
            NbtCompound smallNbt = new NbtCompound();
            stand.writeNbt(smallNbt);
            smallNbt.putBoolean("Small", true);
            stand.readNbt(smallNbt);
            world.spawnEntity(stand);
        }
    }
}
