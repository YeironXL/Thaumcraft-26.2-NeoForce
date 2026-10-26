package art.arcane.thaumcraft.worldgen;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.block.CrystalClusterBlock;
import art.arcane.thaumcraft.world.BiomeHandler;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * TC6 ThaumcraftWorldGenerator.generateAll, run once per chunk: vegetation (silverwood 1/80, greatwood 1/25,
 * cinderpearl in hot sand) and ores (cinnabar, quartz, amber) plus vis crystal clusters.
 * Structures (mounds, cultist portals) belong to later content.
 */
public final class ThaumcraftWorldFeature extends Feature<NoneFeatureConfiguration> {
    public ThaumcraftWorldFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel world = ctx.level();
        Random random = new Random(ctx.random().nextLong());
        int chunkX = ctx.origin().getX() >> 4, chunkZ = ctx.origin().getZ() >> 4;
        boolean flat = ctx.chunkGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource;
        boolean nether = world.getLevel().dimension() == net.minecraft.world.level.Level.NETHER;
        if (!flat) generateVegetation(world, random, chunkX, chunkZ);
        if (!nether) generateOres(world, random, chunkX, chunkZ);
        if (!flat && !nether) art.arcane.thaumcraft.aura.node.AuraNodeGen.generate(world, new Random(ctx.random().nextLong() ^ 0x40DE5L), chunkX, chunkZ);
        if (!flat && !nether) generateObelisk(world, new Random(ctx.random().nextLong() ^ 0x0B311C5L), chunkX, chunkZ);
        if (!flat && !nether) SinisterShrinesGen.generate(world, new Random(ctx.random().nextLong() ^ 0x515157L), chunkX, chunkZ);
        if (!flat && !nether) BarrowDenGen.generate(world, new Random(ctx.random().nextLong() ^ 0xBA770L), chunkX, chunkZ);
        return true;
    }

    /** TC4 Eldritch Obelisk on dry, solid ground. */
    private static void generateObelisk(WorldGenLevel world, Random random, int chunkX, int chunkZ) {
        if (random.nextInt(EldritchObeliskGen.CHANCE) != 0) return;
        BlockPos top = world.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR_WG, new BlockPos(chunkX * 16 + 8, 0, chunkZ * 16 + 8)).below();
        if (!world.getBlockState(top).isSolid() || !world.getFluidState(top.above()).isEmpty() || top.getY() < world.getSeaLevel()) return;
        EldritchObeliskGen.generate(world, random, top);
    }

    private static BlockPos precipitationHeight(WorldGenLevel w, int x, int z) {
        return w.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
    }

    private void generateVegetation(WorldGenLevel world, Random random, int chunkX, int chunkZ) {
        if (random.nextInt(80) == 3) generateSilverwood(world, random, chunkX, chunkZ);
        if (random.nextInt(25) == 7) generateGreatwood(world, random, chunkX, chunkZ);
        int x = chunkX * 16 + 8, z = chunkZ * 16 + 8;
        BlockPos bp = world.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, new BlockPos(x, 0, z));
        var biome = world.getBiome(bp);
        if (world.getBlockState(bp.below()).is(BlockTags.SAND) && biome.value().getBaseTemperature() > 1.0f && random.nextInt(30) == 0)
            CustomFlowers.generate(world, random, bp, ModBlocks.CINDERPEARL.get());
    }

    private static boolean generateGreatwood(WorldGenLevel world, Random random, int chunkX, int chunkZ) {
        int x = chunkX * 16 + 8 + random.nextInt(9) - 4;
        int z = chunkZ * 16 + 8 + random.nextInt(9) - 4;
        BlockPos bp = precipitationHeight(world, x, z);
        if (BiomeHandler.greatwoodChance(world.getBiome(bp)) > random.nextFloat())
            return new GreatwoodTree(false, random.nextInt(8) == 0).generate(world, random, bp);
        return false;
    }

    private static boolean generateSilverwood(WorldGenLevel world, Random random, int chunkX, int chunkZ) {
        int x = chunkX * 16 + 8 + random.nextInt(9) - 4;
        int z = chunkZ * 16 + 8 + random.nextInt(9) - 4;
        BlockPos bp = precipitationHeight(world, x, z);
        var bio = world.getBiome(bp);
        if (BiomeHandler.greatwoodChance(bio) / 2.0f > random.nextFloat() || bio.is(BiomeHandler.MAGICAL))
            return new SilverwoodTree(false, 7, 4).generate(world, random, bp);
        return false;
    }

    private static boolean replaceableOre(WorldGenLevel w, BlockPos p) { return w.getBlockState(p).is(Blocks.STONE); }

    private void generateOres(WorldGenLevel world, Random random, int chunkX, int chunkZ) {
        float density = 1.0f; // ModConfig oreDensity 100
        for (int i = 0; i < Math.round(18.0f * density); ++i) {
            BlockPos pos = new BlockPos(chunkX * 16 + 8 + random.nextInt(13) - 6, random.nextInt(256 / 5), chunkZ * 16 + 8 + random.nextInt(13) - 6);
            if (replaceableOre(world, pos)) world.setBlock(pos, ModBlocks.ORE_CINNABAR.get().defaultBlockState(), 2);
        }
        for (int i = 0; i < Math.round(18.0f * density); ++i) {
            BlockPos pos = new BlockPos(chunkX * 16 + 8 + random.nextInt(13) - 6, random.nextInt(256 / 4), chunkZ * 16 + 8 + random.nextInt(13) - 6);
            if (replaceableOre(world, pos)) world.setBlock(pos, ModBlocks.ORE_QUARTZ.get().defaultBlockState(), 2);
        }
        for (int i = 0; i < Math.round(20.0f * density); ++i) {
            int x = chunkX * 16 + 8 + random.nextInt(13) - 6, z = chunkZ * 16 + 8 + random.nextInt(13) - 6;
            int y = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - random.nextInt(25);
            BlockPos pos = new BlockPos(x, y, z);
            if (replaceableOre(world, pos)) world.setBlock(pos, ModBlocks.ORE_AMBER.get().defaultBlockState(), 2);
        }
        int t = 8, maxCrystals = Math.round(64.0f * density), cc = 0;
        Block[] ores = {ModBlocks.CRYSTAL_AER.get(), ModBlocks.CRYSTAL_IGNIS.get(), ModBlocks.CRYSTAL_AQUA.get(),
                ModBlocks.CRYSTAL_TERRA.get(), ModBlocks.CRYSTAL_ORDO.get(), ModBlocks.CRYSTAL_PERDITIO.get()}; // ShardType order
        String[] order = {"aer", "ignis", "aqua", "terra", "ordo", "perditio"};
        for (int j = 0; j < Math.round(t * density); ++j) {
            int x = chunkX * 16 + 8 + random.nextInt(13) - 6, z = chunkZ * 16 + 8 + random.nextInt(13) - 6;
            int y = random.nextInt(Math.max(5, world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 5));
            BlockPos bp = new BlockPos(x, y, z);
            int md = random.nextInt(6);
            if (random.nextInt(3) == 0) {
                String tag = BiomeHandler.randomTag(world.getBiome(bp), net.minecraft.util.RandomSource.create(random.nextLong()));
                md = tag == null ? random.nextInt(6) : java.util.Arrays.asList(order).indexOf(tag);
                if (md < 0) md = random.nextInt(6);
            }
            Block ore = ores[md];
            for (int xx = -1; xx <= 1; ++xx) for (int yy = -1; yy <= 1; ++yy) for (int zz = -1; zz <= 1; ++zz) {
                if (random.nextInt(3) == 0) continue;
                BlockPos p = bp.offset(xx, yy, zz);
                var bs = world.getBlockState(p);
                if (bs.getFluidState().isEmpty() && (bs.isAir() || bs.canBeReplaced()) && CrystalClusterBlock.touchingRock(world, p)) {
                    int amt = 1 + random.nextInt(3);
                    var st = ((CrystalClusterBlock) ore).attached(world, p, ore.defaultBlockState().setValue(CrystalClusterBlock.SIZE, amt & 3));
                    world.setBlock(p, st, 2);
                    cc += amt;
                }
            }
            if (cc > maxCrystals) break;
        }
    }
}
