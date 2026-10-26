package art.arcane.thaumcraft.worldgen;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aura.node.AuraNodeGen;
import art.arcane.thaumcraft.aura.node.NodeModifier;
import art.arcane.thaumcraft.aura.node.NodeType;
import art.arcane.thaumcraft.blockentity.AuraNodeBlockEntity;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Barrow Den (Thaumcraft "barrow hill"; FTB wiki "Barrow Hill"): an irregular grassy hillock in flat grassland with a
 * stone entrance on its west side. Inside: an upper ring ledge, a main floor with a central pit, a zombie and a
 * skeleton spawner in the two western corners, three treasure chests at the bottom of the pit, and a big, always
 * present sinister (dark) node that keeps spawning hostile wisps. Dimensions and rarity are invented; see
 * docs/TC45_EXTRA_FUENTES.md.
 */
public final class BarrowDenGen {
    public static final int CHANCE = 260;
    public static final int RADIUS = 9;
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath("thaumcraft", "chests/barrow_den"));

    public static boolean biome(Holder<Biome> b) {
        return b.is(Biomes.PLAINS) || b.is(Biomes.SUNFLOWER_PLAINS) || b.is(Biomes.MEADOW) || b.is(BiomeTags.IS_SAVANNA)
                || b.is(Biomes.SAVANNA_PLATEAU);
    }

    /** Per-chunk roll from ThaumcraftWorldFeature. */
    public static void generate(WorldGenLevel world, Random random, int chunkX, int chunkZ) {
        if (random.nextInt(CHANCE) != 0) return;
        int x = chunkX * 16 + 8, z = chunkZ * 16 + 8;
        BlockPos top = world.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR_WG, new BlockPos(x, 0, z)).below();
        if (!world.getBlockState(top).isSolid() || !world.getFluidState(top.above()).isEmpty() || top.getY() < world.getSeaLevel()) return;
        if (!biome(world.getBiome(top))) return;
        // flat enough: the four mound edges within 3 blocks of the centre height
        for (int[] d : new int[][]{{RADIUS, 0}, {-RADIUS, 0}, {0, RADIUS}, {0, -RADIUS}}) {
            int y = world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x + d[0], z + d[1]) - 1;
            if (Math.abs(y - top.getY()) > 3) return;
        }
        build(world, random, top);
    }

    /** Builds the den around ground block {@code g}. Returns the node position. */
    public static BlockPos build(LevelAccessor w, Random r, BlockPos g) {
        BlockState dirt = Blocks.DIRT.defaultBlockState(), grass = Blocks.GRASS_BLOCK.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        // 1. irregular hillock
        double[] wob = new double[8];
        for (int i = 0; i < 8; i++) wob[i] = r.nextDouble() * 1.6 - 0.8;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) for (int dz = -RADIUS; dz <= RADIUS; dz++) {
            double ang = Math.atan2(dz, dx), d = Math.sqrt(dx * dx + dz * dz);
            double rad = RADIUS - 1 + wob[(int) ((ang + Math.PI) / (Math.PI * 2) * 8) & 7];
            if (d > rad) continue;
            int h = (int) Math.round(8.0 * Math.pow(1.0 - d / (rad + 0.5), 0.45)) + (r.nextInt(5) == 0 ? 1 : 0);
            if (d <= 7.0) h = Math.max(h, 5 + (d < 4 ? 1 : 0)); // earth over the stone roof
            for (int y = -3; y <= h; y++) w.setBlock(g.offset(dx, y, dz), y == h ? grass : dirt, 2);
            for (int y = h + 1; y <= h + 3; y++) if (!w.getBlockState(g.offset(dx, y, dz)).isAir()) w.setBlock(g.offset(dx, y, dz), air, 2);
        }
        // 2. stone shell and chamber: walls radius <= 6, upper ledge floor at g, main floor at g-3, pit to g-6
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > 6.5) continue;
            for (int y = -7; y <= 4; y++) {
                BlockPos q = g.offset(dx, y, dz);
                boolean shell = d > 5.5 || y == 4 || y == -7;
                boolean pit = d <= 2.0;
                boolean ledge = d > 3.6;
                int floor = pit ? -6 : ledge ? 0 : -3;
                if (shell || y <= floor) w.setBlock(q, brick(r), 2);
                else w.setBlock(q, air, 2);
            }
        }
        // pit rim of glyphed ancient stone, ledge edge of ancient stone
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > 2.0 && d <= 3.0) w.setBlock(g.offset(dx, -3, dz), ModBlocks.STONE_ANCIENT_GLYPHED.get().defaultBlockState(), 2);
        }
        // 3. west entrance tunnel (3 wide, 3 tall) through the mound
        for (int dx = -RADIUS - 1; dx <= -5; dx++) for (int dz = -2; dz <= 2; dz++) for (int y = 0; y <= 4; y++) {
            BlockPos q = g.offset(dx, y, dz);
            boolean frame = Math.abs(dz) == 2 || y == 4 || y == 0;
            w.setBlock(q, frame ? (dx == -RADIUS - 1 && y > 0 && y < 4 && Math.abs(dz) == 2 ? ModBlocks.STONE_ANCIENT.get().defaultBlockState() : brick(r)) : air, 2);
        }
        // 4. spawners in the two western corners of the main floor
        spawner(w, g.offset(-3, -2, -1), EntityTypes.ZOMBIE, r);
        spawner(w, g.offset(-3, -2, 1), EntityTypes.SKELETON, r);
        // a ladder up the east face of the ledge, from the main floor
        for (int y = -2; y <= 0; y++) w.setBlock(g.offset(3, y, 0), Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, net.minecraft.core.Direction.WEST), 2);
        // 5. three chests at the bottom of the pit
        int[][] chests = {{-1, 0}, {1, -1}, {1, 1}};
        for (int[] c : chests) {
            BlockPos q = g.offset(c[0], -5, c[1]);
            w.setBlock(q, Blocks.CHEST.defaultBlockState(), 2);
            if (w.getBlockEntity(q) instanceof ChestBlockEntity ch) ch.setLootTable(LOOT, r.nextLong());
        }
        // 6. the dark node, big and bright
        BlockPos node = g.above(2);
        RandomSource rs = RandomSource.create(r.nextLong());
        AuraNodeGen.place(w, node, NodeType.DARK, NodeModifier.BRIGHT, rs, false);
        if (w.getBlockEntity(node) instanceof AuraNodeBlockEntity n) {
            n.base.replaceAll((k, v) -> Math.min(255, v * 2));
            n.aspects.clear(); n.aspects.putAll(n.base);
            n.setChanged();
        }
        return node;
    }

    private static BlockState brick(Random r) {
        int n = r.nextInt(10);
        return (n < 6 ? Blocks.STONE_BRICKS : n < 8 ? Blocks.MOSSY_STONE_BRICKS : n < 9 ? Blocks.CRACKED_STONE_BRICKS : Blocks.COBBLESTONE).defaultBlockState();
    }

    private static void spawner(LevelAccessor w, BlockPos p, net.minecraft.world.entity.EntityType<?> type, Random r) {
        w.setBlock(p, Blocks.SPAWNER.defaultBlockState(), 2);
        if (w.getBlockEntity(p) instanceof SpawnerBlockEntity sp) sp.setEntityId(type, RandomSource.create(r.nextLong()));
    }

    private BarrowDenGen() {}
}
