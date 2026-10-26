package art.arcane.thaumcraft.worldgen;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aura.node.AuraNodeGen;
import art.arcane.thaumcraft.aura.node.NodeType;
import art.arcane.thaumcraft.registry.ModEntities;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * TC5 world structures rebuilt on the port's TC4-style node system:
 * <ul>
 * <li><b>Wisp Altar</b>: a ring of ancient stone around a glyphed altar holding a wisp spawner, a sinister
 * ({@link NodeType#DARK}) node floating above it and black candles on the ring.</li>
 * <li><b>Sinister Obelisk</b>: an obsidian obelisk on a crying-obsidian plinth with a sinister node hovering at
 * its tip.</li>
 * </ul>
 * Both only generate in magical forest / eerie / spooky (dark forest, swamp) biomes; see FOCOS_ESTRUCTURAS_FUENTES.md.
 */
public final class SinisterShrinesGen {
    public static final int ALTAR_CHANCE = 140, OBELISK_CHANCE = 110;
    private static final ResourceKey<Biome> EERIE = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("thaumcraft", "eerie"));
    private static final ResourceKey<Biome> MAGICAL_FOREST = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("thaumcraft", "magical_forest"));
    private static final net.minecraft.tags.TagKey<Biome> SPOOKY = net.minecraft.tags.TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_spooky"));

    public static boolean altarBiome(Holder<Biome> b) { return b.is(EERIE) || b.is(MAGICAL_FOREST) || b.is(SPOOKY) || b.is(Biomes.DARK_FOREST); }
    public static boolean obeliskBiome(Holder<Biome> b) { return b.is(EERIE) || b.is(SPOOKY) || b.is(Biomes.SWAMP) || b.is(Biomes.DARK_FOREST); }

    /** Per-chunk roll from ThaumcraftWorldFeature. */
    public static void generate(WorldGenLevel world, Random random, int chunkX, int chunkZ) {
        int x = chunkX * 16 + 8, z = chunkZ * 16 + 8;
        BlockPos top = world.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR_WG, new BlockPos(x, 0, z)).below();
        if (!world.getBlockState(top).isSolid() || !world.getFluidState(top.above()).isEmpty() || top.getY() < world.getSeaLevel()) return;
        var biome = world.getBiome(top);
        boolean eerie = biome.is(EERIE);
        if (altarBiome(biome) && random.nextInt(eerie ? ALTAR_CHANCE / 2 : ALTAR_CHANCE) == 0) wispAltar(world, random, top);
        else if (obeliskBiome(biome) && random.nextInt(eerie ? OBELISK_CHANCE / 2 : OBELISK_CHANCE) == 0) obelisk(world, random, top);
    }

    /** Wisp Altar centred on the ground block {@code g}. Returns the spawner position. */
    public static BlockPos wispAltar(LevelAccessor w, Random r, BlockPos g) {
        BlockState ancient = ModBlocks.STONE_ANCIENT.get().defaultBlockState(), glyph = ModBlocks.STONE_ANCIENT_GLYPHED.get().defaultBlockState();
        BlockState mossy = Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 > 10) continue;
            for (int y = -2; y <= 0; y++) w.setBlock(g.offset(dx, y, dz), y == 0 && r.nextInt(4) == 0 ? mossy : ancient, 2);
            for (int y = 1; y <= 6; y++) w.setBlock(g.offset(dx, y, dz), Blocks.AIR.defaultBlockState(), 2);
        }
        // four broken pillars with black candles
        int[][] corners = {{-2, -2}, {2, -2}, {-2, 2}, {2, 2}};
        for (int[] c : corners) {
            int h = 1 + r.nextInt(2);
            for (int y = 1; y <= h; y++) w.setBlock(g.offset(c[0], y, c[1]), y == h ? glyph : ancient, 2);
            w.setBlock(g.offset(c[0], h + 1, c[1]), ModBlocks.CANDLE_BLACK.get().defaultBlockState(), 2);
        }
        BlockPos spawner = g.above();
        w.setBlock(spawner, Blocks.SPAWNER.defaultBlockState(), 2);
        if (w.getBlockEntity(spawner) instanceof SpawnerBlockEntity sp) sp.setEntityId(ModEntities.WISP.get(), RandomSource.create(r.nextLong()));
        w.setBlock(spawner.above(), glyph, 2);
        AuraNodeGen.place(w, spawner.above(3), NodeType.DARK, null, RandomSource.create(r.nextLong()), false);
        return spawner;
    }

    /** Sinister Obelisk on the ground block {@code g}. Returns the node position. */
    public static BlockPos obelisk(LevelAccessor w, Random r, BlockPos g) {
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState(), crying = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue;
            for (int y = -2; y <= 0; y++) w.setBlock(g.offset(dx, y, dz), (Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && y == 0) ? crying : obsidian, 2);
            for (int y = 1; y <= 10; y++) w.setBlock(g.offset(dx, y, dz), Blocks.AIR.defaultBlockState(), 2);
        }
        int height = 5 + r.nextInt(3);
        for (int y = 1; y <= height; y++) w.setBlock(g.offset(0, y, 0), y == 1 || r.nextInt(5) != 0 ? obsidian : crying, 2);
        for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) w.setBlock(g.above().relative(d), obsidian, 2);
        BlockPos node = g.above(height + 2);
        AuraNodeGen.place(w, node, NodeType.DARK, null, RandomSource.create(r.nextLong()), false);
        return node;
    }

    private SinisterShrinesGen() {}
}
