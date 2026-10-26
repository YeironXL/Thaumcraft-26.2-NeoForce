package art.arcane.thaumcraft.worldgen;

import art.arcane.thaumcraft.ModBlocks;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * TC6 BiomeGenMagicalForest.decorate plus its tree decorator (treesPerChunk 2, getRandomTreeFeature: silverwood 1/18,
 * greatwood 1/12 (1/8 with spiders), otherwise the big magic oak): ambient grass, mossy cobblestone blobs, huge
 * mushrooms, trees, then vishrooms beside logs. 1.12 decorated at chunk+8; 26.2 features are already centred on their
 * own chunk, so offsets are taken inside it.
 */
public final class MagicalForestFeature extends Feature<NoneFeatureConfiguration> {
    public MagicalForestFeature() { super(NoneFeatureConfiguration.CODEC); }

    private static BlockPos top(WorldGenLevel w, BlockPos p) { return w.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, p); }

    private static ConfiguredFeature<?, ?> cf(WorldGenLevel w, String id) {
        return w.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getValue(ResourceKey.create(Registries.CONFIGURED_FEATURE, Identifier.withDefaultNamespace(id)));
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel world = ctx.level();
        Random random = new Random(ctx.random().nextLong());
        BlockPos pos = new BlockPos(ctx.origin().getX() & ~15, 0, ctx.origin().getZ() & ~15);
        for (int a = 0; a < 3; ++a) {
            BlockPos pp = top(world, pos.offset(4 + random.nextInt(8), 0, 4 + random.nextInt(8)));
            while (pp.getY() > 30 && !world.getBlockState(pp).is(Blocks.GRASS_BLOCK)) pp = pp.below();
            if (world.getBlockState(pp).is(Blocks.GRASS_BLOCK)) { world.setBlock(pp, ModBlocks.GRASS_AMBIENT.get().defaultBlockState(), 2); break; }
        }
        ConfiguredFeature<?, ?> rock = cf(world, "forest_rock");
        for (int k = random.nextInt(3), i = 0; i < k; ++i) {
            BlockPos p2 = top(world, pos.offset(random.nextInt(16), 0, random.nextInt(16)));
            if (rock != null) rock.place(world, ctx.chunkGenerator(), ctx.random(), p2);
        }
        for (int k = 0; k < 4; ++k) for (int i = 0; i < 4; ++i) if (random.nextInt(40) == 0) {
            BlockPos p2 = top(world, pos.offset(k * 4 + 1 + random.nextInt(3), 0, i * 4 + 1 + random.nextInt(3)));
            ConfiguredFeature<?, ?> m = cf(world, random.nextBoolean() ? "huge_brown_mushroom" : "huge_red_mushroom");
            if (m != null) m.place(world, ctx.chunkGenerator(), ctx.random(), p2);
        }
        // BiomeDecorator.genDecorations: treesPerChunk (+1 with extraTreeChance 0.1)
        int trees = 2 + (random.nextFloat() < 0.1f ? 1 : 0);
        for (int t = 0; t < trees; ++t) {
            BlockPos tp = top(world, pos.offset(random.nextInt(16), 0, random.nextInt(16)));
            if (random.nextInt(18) == 0) new SilverwoodTree(false, 8, 5).generate(world, random, tp);
            else if (random.nextInt(12) == 0) new GreatwoodTree(false, random.nextInt(8) == 0).generate(world, random, tp);
            else new BigMagicTree(false).generate(world, random, tp); // one per tree: worldgen runs on several threads
        }
        // TC4 recreation: mana pods hanging under the logs of the forest's trees.
        for (int a = 0; a < 24; ++a) {
            BlockPos col = top(world, pos.offset(random.nextInt(16), 0, random.nextInt(16)));
            for (int y = col.getY(); y > col.getY() - 24; --y) {
                BlockPos pp = new BlockPos(col.getX(), y, col.getZ());
                if (world.getBlockState(pp).isAir() && world.getBlockState(pp.above()).is(BlockTags.LOGS)) {
                    var pod = ModBlocks.MANA_POD.get().defaultBlockState().setValue(art.arcane.thaumcraft.block.ManaPodBlock.AGE, random.nextInt(6));
                    if (pod.canSurvive(world, pp)) world.setBlock(pp, pod, 2);
                    break;
                }
            }
        }
        for (int a = 0; a < 8; ++a) {
            BlockPos p3 = top(world, pos.offset(random.nextInt(16), 0, random.nextInt(16)));
            while (p3.getY() > 50 && !world.getBlockState(p3).is(Blocks.GRASS_BLOCK)) p3 = p3.below();
            if (world.getBlockState(p3).is(Blocks.GRASS_BLOCK) && world.getBlockState(p3.above()).canBeReplaced() && adjacentToWood(world, p3.above()))
                world.setBlock(p3.above(), ModBlocks.VISHROOM.get().defaultBlockState(), 2);
        }
        return true;
    }

    private static boolean adjacentToWood(WorldGenLevel w, BlockPos p) {
        for (int x = -1; x <= 1; ++x) for (int y = -1; y <= 1; ++y) for (int z = -1; z <= 1; ++z)
            if ((x != 0 || y != 0 || z != 0) && w.getBlockState(p.offset(x, y, z)).is(BlockTags.LOGS)) return true;
        return false;
    }
}
