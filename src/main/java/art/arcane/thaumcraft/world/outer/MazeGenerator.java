package art.arcane.thaumcraft.world.outer;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.block.eldritch.EldritchLockBlock;
import art.arcane.thaumcraft.block.eldritch.EldritchLockBlockEntity;
import art.arcane.thaumcraft.block.eldritch.EldritchPortalBlockEntity;
import art.arcane.thaumcraft.blockentity.PedestalBlockEntity;
import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * TC4 MazeHandler/GenCommon, rebuilt from the documented layout: corridors 5 wide and 5 high joining 16x16 cells,
 * 7x7 rooms at some intersections and dead ends, a 7x7x7 entry room with the exit portal and void pits in its
 * corners, loot rooms (urns and crates), crab rooms, runed tablet rooms and boss rooms sealed by an Ancient Doorway
 * with an Ancient Locking Mechanism. Ancient stone walls with glyphed/eldritch inlays, bedrock above and below.
 */
public final class MazeGenerator {
    public static final int CELL = 16, SIZE = 9, FLOOR = 64, SPACING_BLOCKS = 1024;
    public static final int ENTRY = SIZE / 2;

    public enum Kind { CORRIDOR, JUNCTION, ENTRY, BOSS, TABLET, LOOT, CRAB, RUNE, SPIDER, TAINT, TAINTACLES }

    /** N, E, S, W bits */
    private static final int[] DX = {0, 1, 0, -1}, DZ = {-1, 0, 1, 0};
    private static final Direction[] DIRS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    /** The planned maze: connection bits and room kind per cell. */
    public record Plan(int[][] conn, Kind[][] kind) {
        public int count(Kind k) { int n = 0; for (Kind[] r : kind) for (Kind c : r) if (c == k) n++; return n; }
    }

    public static BlockPos origin(MazeData.Maze m) { return new BlockPos(m.id() * SPACING_BLOCKS, FLOOR, 0); }
    public static BlockPos cellOrigin(MazeData.Maze m, int cx, int cz) { return origin(m).offset(cx * CELL, 0, cz * CELL); }
    /** Where travellers appear: in the entry room, facing the exit portal. */
    public static BlockPos arrival(MazeData.Maze m) { return cellOrigin(m, ENTRY, ENTRY).offset(8, 1, 11); }
    public static BlockPos exitPortal(MazeData.Maze m) { return cellOrigin(m, ENTRY, ENTRY).offset(8, 1, 8); }

    /** Absolute positions of the Ancient Locking Mechanisms of a maze (one per boss room). */
    public static List<BlockPos> locks(MazeData.Maze m) {
        Plan p = plan(m);
        List<BlockPos> out = new ArrayList<>();
        for (int cx = 0; cx < SIZE; cx++) for (int cz = 0; cz < SIZE; cz++) if (p.kind()[cx][cz] == Kind.BOSS) out.add(lockPos(m, p.conn()[cx][cz], cx, cz));
        return out;
    }
    /** Absolute centres (floor + 1) of every room of the given kind. */
    public static List<BlockPos> rooms(MazeData.Maze m, Kind k) {
        Plan p = plan(m);
        List<BlockPos> out = new ArrayList<>();
        for (int cx = 0; cx < SIZE; cx++) for (int cz = 0; cz < SIZE; cz++) if (p.kind()[cx][cz] == k) out.add(cellOrigin(m, cx, cz).offset(8, 1, 8));
        return out;
    }
    private static BlockPos lockPos(MazeData.Maze m, int conn, int cx, int cz) {
        int d = Integer.numberOfTrailingZeros(conn);
        int bx = 8 + DX[d] * 8 - (DX[d] > 0 ? 1 : 0), bz = 8 + DZ[d] * 8 - (DZ[d] > 0 ? 1 : 0);
        return cellOrigin(m, cx, cz).offset(bx, 3, bz);
    }

    public static Plan plan(MazeData.Maze m) { return plan(m.seed(), m.version()); }
    public static Plan plan(long seed) { return plan(seed, MazeData.VERSION); }

    public static Plan plan(long seed, int version) {
        Random r = new Random(seed);
        int[][] conn = new int[SIZE][SIZE];
        boolean[][] seen = new boolean[SIZE][SIZE];
        int[][] dist = new int[SIZE][SIZE];
        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{ENTRY, ENTRY});
        seen[ENTRY][ENTRY] = true;
        while (!stack.isEmpty()) {
            int[] c = stack.peek();
            List<Integer> opts = new ArrayList<>();
            for (int d = 0; d < 4; d++) {
                int nx = c[0] + DX[d], nz = c[1] + DZ[d];
                if (nx >= 0 && nz >= 0 && nx < SIZE && nz < SIZE && !seen[nx][nz]) opts.add(d);
            }
            if (opts.isEmpty()) { stack.pop(); continue; }
            int d = opts.get(r.nextInt(opts.size()));
            int nx = c[0] + DX[d], nz = c[1] + DZ[d];
            conn[c[0]][c[1]] |= 1 << d;
            conn[nx][nz] |= 1 << ((d + 2) & 3);
            seen[nx][nz] = true;
            dist[nx][nz] = dist[c[0]][c[1]] + 1;
            stack.push(new int[]{nx, nz});
        }
        // a few loops so the maze is not a pure tree (TC4 corridors cross)
        for (int i = 0; i < SIZE; i++) {
            int x = r.nextInt(SIZE - 1), z = r.nextInt(SIZE);
            if (Integer.bitCount(conn[x][z]) > 1 && Integer.bitCount(conn[x + 1][z]) > 1) { conn[x][z] |= 2; conn[x + 1][z] |= 8; }
        }
        Kind[][] kind = new Kind[SIZE][SIZE];
        List<int[]> ends = new ArrayList<>();
        for (int x = 0; x < SIZE; x++) for (int z = 0; z < SIZE; z++) {
            kind[x][z] = Kind.CORRIDOR;
            int deg = Integer.bitCount(conn[x][z]);
            if (deg >= 3 && r.nextInt(2) == 0) kind[x][z] = Kind.JUNCTION;
            if (deg == 1 && !(x == ENTRY && z == ENTRY)) ends.add(new int[]{x, z});
        }
        kind[ENTRY][ENTRY] = Kind.ENTRY;
        ends.sort((a, b) -> dist[b[0]][b[1]] - dist[a[0]][a[1]]);
        int bosses = ends.size() >= 6 ? 2 : 1;
        for (int i = 0; i < ends.size(); i++) {
            int[] e = ends.get(i);
            Kind k;
            if (i < bosses) k = Kind.BOSS;
            else if (i < bosses * 2) k = Kind.TABLET;
            else { int roll = r.nextInt(100); k = roll < 15 ? Kind.CRAB : roll < 70 ? Kind.LOOT : Kind.JUNCTION; }
            kind[e[0]][e[1]] = k;
        }
        if (version >= 2) extraRooms(seed, conn, kind, ends);
        if (version >= 4) taintacleRoom(seed, kind, ends);
        return new Plan(conn, kind);
    }

    /**
     * Version 2 rooms, drawn from a separate random stream so the boss/tablet layout of a seed never changes:
     * at least one rune room, one mind spider room and one tainted room on dead ends that were loot or plain rooms,
     * then each remaining loot/plain dead end becomes rune 6 %, spider 8 %, taint 10 %; 3+ way junction rooms may be
     * tainted (10 %) or web-shrouded (8 %).
     */
    /**
     * Version 4 (invención on frequency): 40 % of mazes turn one loot or plain dead end into a nest of 1-5 Giant
     * Taintacles; drawn from its own stream so the rooms of version 2/3 never move.
     */
    private static void taintacleRoom(long seed, Kind[][] kind, List<int[]> ends) {
        Random r = new Random(seed * 37 + 0x7A1C1EL);
        if (r.nextInt(100) >= 40) return;
        List<int[]> free = new ArrayList<>();
        for (int[] e : ends) if (kind[e[0]][e[1]] == Kind.LOOT || kind[e[0]][e[1]] == Kind.JUNCTION) free.add(e);
        if (free.isEmpty()) return;
        int[] e = free.get(r.nextInt(free.size()));
        kind[e[0]][e[1]] = Kind.TAINTACLES;
    }

    private static void extraRooms(long seed, int[][] conn, Kind[][] kind, List<int[]> ends) {
        Random r = new Random(seed * 31 + 0x7A1D);
        List<int[]> free = new ArrayList<>();
        for (int[] e : ends) if (kind[e[0]][e[1]] == Kind.LOOT || kind[e[0]][e[1]] == Kind.JUNCTION) free.add(e);
        java.util.Collections.shuffle(free, r);
        Kind[] must = {Kind.RUNE, Kind.SPIDER, Kind.TAINT};
        for (int i = 0; i < free.size(); i++) {
            int[] e = free.get(i);
            if (i < must.length) { kind[e[0]][e[1]] = must[i]; continue; }
            int roll = r.nextInt(100);
            if (roll < 6) kind[e[0]][e[1]] = Kind.RUNE; else if (roll < 14) kind[e[0]][e[1]] = Kind.SPIDER; else if (roll < 24) kind[e[0]][e[1]] = Kind.TAINT;
        }
        for (int x = 0; x < SIZE; x++) for (int z = 0; z < SIZE; z++) if (kind[x][z] == Kind.JUNCTION && Integer.bitCount(conn[x][z]) >= 3) {
            int roll = r.nextInt(100);
            if (roll < 10) kind[x][z] = Kind.TAINT; else if (roll < 18) kind[x][z] = Kind.SPIDER;
        }
    }

    public static void generate(ServerLevel w, MazeData.Maze m) {
        Plan p = plan(m);
        Random r = new Random(m.seed() ^ 0x5DEECE66DL);
        for (int cx = 0; cx < SIZE; cx++) for (int cz = 0; cz < SIZE; cz++) buildCell(w, m, p, cx, cz, r);
        if (m.version() >= 2) {
            Random h = new Random(m.seed() ^ 0x0DD5L);
            for (int cx = 0; cx < SIZE; cx++) for (int cz = 0; cz < SIZE; cz++) if (p.kind()[cx][cz] == Kind.CORRIDOR) hazards(w, cellOrigin(m, cx, cz), p.conn()[cx][cz], h, m.version() >= 5 ? new Random(m.seed() ^ 0x9001_5L ^ (cx * 31L + cz) * 0x9E3779B97F4A7C15L) : null);
        }
        if (m.version() >= 3) MazeDecor.apply(w, m, p);
    }

    // ---- position lookup (taint bounds) ----
    private static final java.util.Map<Long, Plan> PLANS = new java.util.concurrent.ConcurrentHashMap<>();
    static Plan cachedPlan(MazeData.Maze m) { return PLANS.computeIfAbsent(m.seed() * 31 + m.version(), k -> plan(m)); }

    /** Where a block sits in a generated maze: the maze, the cell kind and the block's coordinates inside the cell. */
    public record Spot(MazeData.Maze maze, Kind kind, int lx, int ly, int lz) {}

    public static Spot spotAt(net.minecraft.world.level.Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl) || !OuterLands.is(level)) return null;
        int ly = pos.getY() - FLOOR;
        if (ly < -2 || ly > 12 || pos.getZ() < 0 || pos.getZ() >= SIZE * CELL) return null;
        int id = Math.floorDiv(pos.getX(), SPACING_BLOCKS), lxAll = pos.getX() - id * SPACING_BLOCKS;
        if (lxAll >= SIZE * CELL) return null;
        MazeData.Maze m = MazeData.get(sl.getServer()).byId(id);
        if (m == null || !m.generated()) return null;
        Plan p = cachedPlan(m);
        return new Spot(m, p.kind()[lxAll / CELL][pos.getZ() / CELL], lxAll % CELL, ly, pos.getZ() % CELL);
    }

    /** Generated taint inside a maze never withers (TC4 Outer Lands taint had no seeds to keep it alive). */
    public static boolean permanentTaint(net.minecraft.world.level.Level level, BlockPos pos) { Spot s = spotAt(level, pos); return s != null && s.maze().version() >= 2; }

    /**
     * Taint may only creep over the maze inside a taint room: the room's own floor, walls and ceiling face (one block
     * deep) and its air. Bedrock, the rest of the walls and every other cell are immune, so the maze holds together.
     */
    public static boolean taintMayReach(net.minecraft.world.level.Level level, BlockPos pos) {
        Spot s = spotAt(level, pos);
        if (s == null) return true;
        if ((s.kind() != Kind.TAINT && s.kind() != Kind.TAINTACLES) || s.maze().version() < 3) return false;
        int h = half(s.kind()) + 1;
        return Math.abs(s.lx() - 8) <= h && Math.abs(s.lz() - 8) <= h && s.ly() >= 0 && s.ly() <= height(s.kind()) + 1;
    }

    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static BlockState wall(Random r) {
        int n = r.nextInt(100);
        if (n < 80) return ModBlocks.STONE_ANCIENT.get().defaultBlockState();
        if (n < 88) return ModBlocks.ELDRITCH_STONE.get().defaultBlockState();
        if (n < 94) return ModBlocks.ELDRITCH_STONE_CARVED.get().defaultBlockState();
        if (n < 98) return ModBlocks.STONE_ANCIENT_GLYPHED.get().defaultBlockState();
        return ModBlocks.ELDRITCH_STONE_RUNED.get().defaultBlockState();
    }

    static int half(Kind k) { return switch (k) { case CORRIDOR -> 2; case JUNCTION, ENTRY -> 3; case LOOT, CRAB, RUNE, SPIDER, TAINT -> 4; case TABLET, TAINTACLES -> 5; case BOSS -> 6; }; }
    static int height(Kind k) { return switch (k) { case ENTRY, TABLET, TAINTACLES -> 7; case BOSS -> 9; default -> 5; }; }

    private static void buildCell(ServerLevel w, MazeData.Maze m, Plan p, int cx, int cz, Random r) {
        BlockPos o = cellOrigin(m, cx, cz);
        Kind k = p.kind()[cx][cz];
        int conn = p.conn()[cx][cz];
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState(), bedrock = Blocks.BEDROCK.defaultBlockState();
        int h = half(k), hy = height(k);
        for (int x = 0; x < CELL; x++) for (int z = 0; z < CELL; z++) for (int y = -2; y <= 12; y++) {
            mp.set(o.getX() + x, o.getY() + y, o.getZ() + z);
            BlockState s;
            if (y == -2 || y == 12) s = bedrock;
            else if (y >= 1 && carved(k, conn, x, y, z, h, hy)) s = air;
            else s = wall(r);
            w.setBlock(mp, s, FLAGS);
        }
        BlockPos c = o.offset(8, 1, 8);
        switch (k) {
            case ENTRY -> {
                for (int[] q : new int[][]{{5, 5}, {5, 11}, {11, 5}, {11, 11}}) for (int y = -1; y <= 0; y++)
                    w.setBlock(o.offset(q[0], y, q[1]), ModBlocks.ELDRITCH_VOID.get().defaultBlockState(), FLAGS);
                w.setBlock(c.below(), ModBlocks.STONE_ANCIENT_TILE.get().defaultBlockState(), FLAGS);
                for (int y = 0; y < 2; y++) {
                    w.setBlock(c.above(y), ModBlocks.ELDRITCH_PORTAL.get().defaultBlockState(), FLAGS);
                    if (w.getBlockEntity(c.above(y)) instanceof EldritchPortalBlockEntity pe) pe.configure(m.id(), true);
                }
                w.setBlock(c.above(3), ancient(ModBlocks.ELDRITCH_OBELISK_BASE.get()), FLAGS);
                w.setBlock(c.above(4), ancient(ModBlocks.ELDRITCH_OBELISK_BODY.get()), FLAGS);
                w.setBlock(c.above(5), ancient(ModBlocks.ELDRITCH_CAPSTONE.get()), FLAGS);
            }
            case BOSS -> {
                int d = Integer.numberOfTrailingZeros(conn);
                Direction dir = DIRS[d];
                // the door sits on the cell boundary of the only entrance
                BlockPos lock = lockPos(m, conn, cx, cz);
                Direction side = dir.getClockWise();
                for (int a = -2; a <= 2; a++) for (int y = -2; y <= 2; y++) {
                    BlockPos q = lock.relative(side, a).above(y);
                    w.setBlock(q, ModBlocks.ELDRITCH_DOOR.get().defaultBlockState(), FLAGS);
                }
                w.setBlock(lock, ModBlocks.ELDRITCH_LOCK.get().defaultBlockState().setValue(EldritchLockBlock.FACING, dir), FLAGS);
                if (w.getBlockEntity(lock) instanceof EldritchLockBlockEntity le) le.configure(c, r.nextLong());
                for (int[] q : new int[][]{{3, 3}, {3, 13}, {13, 3}, {13, 13}}) w.setBlock(o.offset(q[0], 1, q[1]), lootBlock(r, true), FLAGS);
            }
            case TABLET -> {
                w.setBlock(c, ModBlocks.PEDESTAL_ANCIENT.get().defaultBlockState(), FLAGS);
                if (w.getBlockEntity(c) instanceof PedestalBlockEntity pe) pe.setItem(0, new ItemStack(ModItems.RUNED_TABLET.get()));
                for (int[] q : new int[][]{{4, 4}, {4, 12}, {12, 4}, {12, 12}}) {
                    w.setBlock(o.offset(q[0], 1, q[1]), ModBlocks.STONE_ANCIENT_ROCK.get().defaultBlockState(), FLAGS);
                    w.setBlock(o.offset(q[0], 2, q[1]), ModBlocks.STONE_ANCIENT_ROCK.get().defaultBlockState(), FLAGS);
                }
            }
            case LOOT -> {
                int n = 3 + r.nextInt(4);
                for (int i = 0; i < n; i++) w.setBlock(o.offset(4 + r.nextInt(9), 1, 4 + r.nextInt(9)), lootBlock(r, false), FLAGS);
            }
            case CRAB -> {
                for (int x = 4; x <= 12; x++) for (int z = 4; z <= 12; z++) w.setBlock(o.offset(x, 0, z), ModBlocks.ELDRITCH_STONE_CARVED.get().defaultBlockState(), FLAGS);
                w.setBlock(o.offset(4, 1, 4), lootBlock(r, false), FLAGS);
                w.setBlock(o.offset(12, 1, 12), lootBlock(r, false), FLAGS);
                if (m.version() >= 2) for (int[] q : new int[][]{{3, 6}, {13, 10}, {6, 13}, {10, 3}}) {
                    BlockPos sp = o.offset(q[0], 1, q[1]);
                    if (!w.getBlockState(sp).isAir()) w.setBlock(sp, ModBlocks.ELDRITCH_CRAB_SPAWNER.get().defaultBlockState(), FLAGS);
                }
                int n = 3 + r.nextInt(5);
                for (int i = 0; i < n; i++) {
                    Mob crab = ModEntities.ELDRITCH_CRAB.get().create(w, EntitySpawnReason.STRUCTURE);
                    if (crab == null) continue;
                    crab.snapTo(o.getX() + 6 + r.nextInt(5) + 0.5, o.getY() + 1, o.getZ() + 6 + r.nextInt(5) + 0.5, r.nextFloat() * 360f, 0f);
                    crab.setPersistenceRequired();
                    w.addFreshEntity(crab);
                }
            }
            case RUNE -> {
                // TC4: a lone rune stone in the room; glyphs float and it arcs at whoever lingers
                for (int x = 5; x <= 11; x++) for (int z = 5; z <= 11; z++) if ((x + z) % 2 == 0) w.setBlock(o.offset(x, 0, z), ModBlocks.STONE_ANCIENT_GLYPHED.get().defaultBlockState(), FLAGS);
                w.setBlock(c, ModBlocks.ELDRITCH_RUNE_STONE.get().defaultBlockState(), FLAGS);
                w.setBlock(o.offset(4, 1, 12), lootBlock(r, false), FLAGS);
            }
            case SPIDER -> {
                // TC4: a mind spider spawner surrounded by cobwebs
                w.setBlock(c, Blocks.SPAWNER.defaultBlockState(), FLAGS);
                if (w.getBlockEntity(c) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity sb) sb.setEntityId(ModEntities.MIND_SPIDER.get(), w.getRandom());
                for (int x = 4; x <= 12; x++) for (int z = 4; z <= 12; z++) for (int y = 1; y <= 5; y++) {
                    BlockPos q = o.offset(x, y, z);
                    double d = Math.sqrt((x - 8) * (x - 8) + (z - 8) * (z - 8) + (y - 1) * (y - 1) * 0.5);
                    if (!q.equals(c) && w.getBlockState(q).isAir() && r.nextFloat() < (d < 2.5 ? 0.8f : 0.25f)) w.setBlock(q, Blocks.COBWEB.defaultBlockState(), FLAGS);
                }
                w.setBlock(o.offset(12, 1, 4), lootBlock(r, false), FLAGS);
            }
            case TAINT -> taintZone(w, o, h, hy, r, true);
            case TAINTACLES -> taintZone(w, o, h, hy, r, false);
            default -> {}
        }
    }

    /** Tainted area: crust/soil floor, fibres on floor, flux goo puddles, tainted rock walls, a taintacle for a room. */
    private static void taintZone(ServerLevel w, BlockPos o, int h, int hy, Random r, boolean room) {
        int lo = 8 - h - 1, hi = 8 + h + 1;
        for (int x = lo; x <= hi; x++) for (int z = lo; z <= hi; z++) for (int y = 0; y <= hy + 1; y++) {
            BlockPos q = o.offset(x, y, z);
            BlockState s = w.getBlockState(q);
            if (s.isAir() || s.is(Blocks.BEDROCK) || !s.is(ModBlocks.STONE_ANCIENT.get())) continue;
            boolean exposed = false;
            for (Direction d : Direction.values()) if (w.getBlockState(q.relative(d)).isAir()) { exposed = true; break; }
            if (!exposed) continue;
            if (y == 0) w.setBlock(q, (r.nextInt(3) == 0 ? ModBlocks.TAINT_SOIL : ModBlocks.TAINT_CRUST).get().defaultBlockState(), FLAGS);
            else if (r.nextInt(3) == 0) w.setBlock(q, (r.nextBoolean() ? ModBlocks.TAINT_ROCK : ModBlocks.TAINT_CRUST).get().defaultBlockState(), FLAGS);
        }
        for (int x = lo + 1; x < hi; x++) for (int z = lo + 1; z < hi; z++) {
            BlockPos q = o.offset(x, 1, z);
            if (!w.getBlockState(q).isAir()) continue;
            int n = r.nextInt(100);
            if (n < 35) w.setBlock(q, ModBlocks.TAINT_FIBRE.get().defaultBlockState(), FLAGS);
            else if (n < 45) w.setBlock(q, ModBlocks.FLUX_GOO.get().defaultBlockState(), FLAGS);
        }
        if (room) {
            Mob t = ModEntities.TAINTACLE.get().create(w, EntitySpawnReason.STRUCTURE);
            if (t != null) { t.snapTo(o.getX() + 8.5, o.getY() + 1, o.getZ() + 8.5, 0f, 0f); t.setPersistenceRequired(); w.addFreshEntity(t); }
        }
    }

    /**
     * Corridor hazards of version 2 mazes: taint patches (12 %), pools of lava, water or Liquid Death (8 %),
     * a crusted opening in a side wall (6 %).
     */
    private static void hazards(ServerLevel w, BlockPos o, int conn, Random r, Random pools) {
        int n = r.nextInt(100);
        if (n < 12) taintZone(w, o, 2, 5, r, false);
        else if (n < 20) {
            int k = r.nextInt(10);
            BlockState liquid = (k < 4 ? Blocks.LAVA : k < 7 ? Blocks.WATER : ModBlocks.LIQUID_DEATH.get()).defaultBlockState();
            boolean[] skip = new boolean[9];
            for (int x = 7; x <= 9; x++) for (int z = 7; z <= 9; z++) skip[(x - 7) * 3 + z - 7] = Math.abs(x - 8) == 1 && Math.abs(z - 8) == 1 && r.nextBoolean();
            // version 5: the same draws are consumed (the later corridors keep their hazards), but the pool is reshaped
            if (pools != null) MazePools.pool(w, o, conn, pools);
            else for (int x = 7; x <= 9; x++) for (int z = 7; z <= 9; z++) if (!skip[(x - 7) * 3 + z - 7]) w.setBlock(o.offset(x, 0, z), liquid, Block.UPDATE_CLIENTS);
        } else if (n < 26) {
            // a side wall of the corridor: the first unconnected direction
            for (int d = 0; d < 4; d++) if ((conn & (1 << d)) == 0) {
                BlockPos q = o.offset(8 + DX[d] * 3, 1, 8 + DZ[d] * 3);
                w.setBlock(q, ModBlocks.ELDRITCH_CRAB_SPAWNER.get().defaultBlockState(), FLAGS);
                break;
            }
        }
    }

    private static BlockState ancient(Block b) { return b.defaultBlockState().setValue(art.arcane.thaumcraft.block.eldritch.EldritchObeliskBlock.ANCIENT, true); }

    static BlockState lootBlock(Random r, boolean boss) {
        int n = r.nextInt(100) + (boss ? 30 : 0);
        boolean urn = r.nextBoolean();
        Block b = n < 60 ? (urn ? ModBlocks.LOOT_URN_COMMON : ModBlocks.LOOT_CRATE_COMMON).get()
                : n < 90 ? (urn ? ModBlocks.LOOT_URN_UNCOMMON : ModBlocks.LOOT_CRATE_UNCOMMON).get()
                : (urn ? ModBlocks.LOOT_URN_RARE : ModBlocks.LOOT_CRATE_RARE).get();
        return b.defaultBlockState();
    }

    /** Air inside the room box or along a connected 5x5 arm out to the cell edge. */
    private static boolean carved(Kind k, int conn, int x, int y, int z, int h, int hy) {
        if (Math.abs(x - 8) <= h && Math.abs(z - 8) <= h && y <= hy) return true;
        if (y > 5) return false;
        boolean inX = x >= 6 && x <= 10, inZ = z >= 6 && z <= 10;
        if ((conn & 1) != 0 && inX && z <= 8) return true;
        if ((conn & 4) != 0 && inX && z >= 8) return true;
        if ((conn & 2) != 0 && inZ && x >= 8) return true;
        if ((conn & 8) != 0 && inZ && x <= 8) return true;
        return false;
    }

    private MazeGenerator() {}
}
