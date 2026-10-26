package art.arcane.thaumcraft.world.outer;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.entity.monster.EldritchAutocasterEntity;
import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Maze version 3 dressing, from its own random stream so older layouts never shift: crusted stone crab rooms, glowing
 * crusted stone and strange crystals in the halls, Ancient Autocasters on Ancient Doorway supports, and living taint
 * rooms (a taint seed that keeps the taint growing, and the Tainted Lands biome).
 */
public final class MazeDecor {
    public static final ResourceKey<Biome> TAINTED_LANDS = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("thaumcraft", "tainted_lands"));
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    static void apply(ServerLevel w, MazeData.Maze m, MazeGenerator.Plan p) {
        Random r = new Random(m.seed() ^ 0xC3C3_0003L);
        for (int cx = 0; cx < MazeGenerator.SIZE; cx++) for (int cz = 0; cz < MazeGenerator.SIZE; cz++) {
            BlockPos o = MazeGenerator.cellOrigin(m, cx, cz);
            MazeGenerator.Kind k = p.kind()[cx][cz];
            switch (k) {
                case TAINT -> livingTaint(w, o);
                case TAINTACLES -> { livingTaint(w, o); giantTaintacles(w, o, r); }
                case CRAB -> crustRoom(w, o, r);
                case CORRIDOR, JUNCTION -> {
                    if (r.nextInt(100) < 15) glow(w, o, r);
                    if (r.nextInt(100) < 12) crystals(w, o, r, k == MazeGenerator.Kind.JUNCTION ? 3 : 2);
                    if (r.nextInt(100) < (k == MazeGenerator.Kind.JUNCTION ? 20 : 7)) autocaster(w, o, r, MazeGenerator.half(k));
                }
                case LOOT -> { if (r.nextInt(4) == 0) autocaster(w, o, r, 4); if (r.nextInt(3) == 0) crystals(w, o, r, 4); }
                default -> {}
            }
        }
    }

    /** TC4: crab rooms are made of crusted stone; the room's inner faces become crusted stone, with one glowing lump. */
    private static void crustRoom(ServerLevel w, BlockPos o, Random r) {
        int h = MazeGenerator.half(MazeGenerator.Kind.CRAB) + 1;
        for (int x = 8 - h; x <= 8 + h; x++) for (int z = 8 - h; z <= 8 + h; z++) for (int y = 0; y <= 6; y++) {
            BlockPos q = o.offset(x, y, z);
            BlockState s = w.getBlockState(q);
            if (!(s.is(ModBlocks.STONE_ANCIENT.get()) || s.is(ModBlocks.ELDRITCH_STONE_CARVED.get()) || s.is(ModBlocks.ELDRITCH_STONE.get()))) continue;
            for (Direction d : Direction.values()) if (w.getBlockState(q.relative(d)).isAir()) { w.setBlock(q, ModBlocks.CRUSTED_STONE.get().defaultBlockState(), FLAGS); break; }
        }
        glow(w, o, r);
    }

    private static BlockPos floorSpot(ServerLevel w, BlockPos o, Random r, int half) {
        for (int i = 0; i < 12; i++) {
            BlockPos q = o.offset(8 - half + r.nextInt(half * 2 + 1), 1, 8 - half + r.nextInt(half * 2 + 1));
            if (w.getBlockState(q).isAir() && w.getBlockState(q.above()).isAir() && w.getBlockState(q.below()).isSolidRender()) return q;
        }
        return null;
    }

    /** TC4 Glowing Crusted Stone: a lump of crusted stone with a yellow light, lighting the otherwise dark halls. */
    private static void glow(ServerLevel w, BlockPos o, Random r) {
        BlockPos q = floorSpot(w, o, r, 2);
        if (q != null) w.setBlock(q, ModBlocks.CRUSTED_STONE_GLOW.get().defaultBlockState(), FLAGS);
    }

    /** TC4 Strange Crystals: white clusters on the ceilings and walls. */
    private static void crystals(ServerLevel w, BlockPos o, Random r, int n) {
        for (int i = 0; i < n; i++) {
            for (int t = 0; t < 10; t++) {
                BlockPos q = o.offset(5 + r.nextInt(7), 1 + r.nextInt(8), 5 + r.nextInt(7));
                if (!w.getBlockState(q).isAir()) continue;
                Direction face = null;
                if (w.getBlockState(q.above()).isSolidRender()) face = Direction.DOWN;
                else for (Direction d : Direction.Plane.HORIZONTAL) if (w.getBlockState(q.relative(d)).isSolidRender()) { face = d.getOpposite(); break; }
                if (face == null) continue;
                w.setBlock(q, ModBlocks.CRYSTAL_STRANGE.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, face), FLAGS);
                break;
            }
        }
    }

    /** TC5: Ancient Autocasters scattered through the halls, each on an Ancient Doorway block. */
    static EldritchAutocasterEntity autocaster(ServerLevel w, BlockPos o, Random r, int half) {
        BlockPos q = floorSpot(w, o, r, Math.max(1, half - 1));
        if (q == null || !w.getBlockState(q.above(2)).isAir()) return null;
        w.setBlock(q, ModBlocks.ELDRITCH_DOOR.get().defaultBlockState(), FLAGS);
        EldritchAutocasterEntity a = ModEntities.ELDRITCH_AUTOCASTER.get().create(w, EntitySpawnReason.STRUCTURE);
        if (a == null) return null;
        a.snapTo(q.getX() + 0.5, q.getY() + 1, q.getZ() + 0.5, r.nextFloat() * 360f, 0f);
        a.setFocus(r.nextInt(2));
        w.addFreshEntity(a);
        return a;
    }

    /** TC4 taintacle room: it keeps slowly tainting itself (a taint seed) and its area becomes Tainted Lands. */
    private static void livingTaint(ServerLevel w, BlockPos o) {
        Mob seed = ModEntities.TAINT_SEED.get().create(w, EntitySpawnReason.STRUCTURE);
        if (seed != null) {
            seed.snapTo(o.getX() + 8.5, o.getY() + 1, o.getZ() + 6.5, 0f, 0f);
            seed.setPersistenceRequired();
            w.addFreshEntity(seed);
        }
        w.registryAccess().lookupOrThrow(Registries.BIOME).get(TAINTED_LANDS).ifPresent(b -> FillBiomeCommand.fill(w, o.offset(1, -2, 1), o.offset(14, 12, 14), b));
    }

    /** Spots of the Giant Taintacles in their 11x11 room, kept away from the walls and the seed (lz 6.5). */
    private static final int[][] GIANT_SPOTS = {{8, 10}, {4, 4}, {12, 12}, {12, 4}, {4, 12}};

    /** TC4 tainted room of the Outer Lands: 1-5 Giant Taintacles (bosses, persistent). */
    private static void giantTaintacles(ServerLevel w, BlockPos o, Random r) {
        int n = 1 + r.nextInt(5);
        for (int i = 0; i < n; i++) {
            Mob g = ModEntities.TAINTACLE_GIANT.get().create(w, EntitySpawnReason.STRUCTURE);
            if (g == null) continue;
            g.snapTo(o.getX() + GIANT_SPOTS[i][0] + 0.5, o.getY() + 1, o.getZ() + GIANT_SPOTS[i][1] + 0.5, r.nextFloat() * 360f, 0f);
            g.setPersistenceRequired();
            w.addFreshEntity(g);
        }
    }

    private MazeDecor() {}
}
