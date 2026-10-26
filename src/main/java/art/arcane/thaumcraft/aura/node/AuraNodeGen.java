package art.arcane.thaumcraft.aura.node;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.blockentity.AuraNodeBlockEntity;
import art.arcane.thaumcraft.world.BiomeHandler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * TC4 ThaumcraftWorldGenerator node generation (createRandomNodeAt) as far as it is documented: wild nodes on roughly
 * one chunk in {@link #NODE_RARITY}, a special type on about one in {@link #SPECIAL_RARITY}, a modifier on about one in
 * half that; sinister nodes in Eerie biomes, tainted nodes in the Tainted Lands, more pure nodes in magical forests and
 * a pure node inside every naturally generated silverwood tree. Aspects: two or three primals biased by the biome tag,
 * sometimes one compound, 15..39 each (silverwood nodes are smaller).
 */
public final class AuraNodeGen {
    /** TC4 Config.nodeRarity / specialNodeRarity defaults. */
    public static final int NODE_RARITY = 36, SPECIAL_RARITY = 18;
    private static final List<Aspect> PRIMALS = List.of(AspectRegistry.AER, AspectRegistry.TERRA, AspectRegistry.IGNIS, AspectRegistry.AQUA, AspectRegistry.ORDO, AspectRegistry.PERDITIO);
    private static final ResourceKey<Biome> EERIE = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("thaumcraft", "eerie"));
    private static final ResourceKey<Biome> TAINTED = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("thaumcraft", "tainted_lands"));

    /** One attempt per generated chunk (called from ThaumcraftWorldFeature). */
    public static boolean generate(WorldGenLevel world, java.util.Random random, int chunkX, int chunkZ) {
        if (random.nextInt(NODE_RARITY) != 0) return false;
        int x = chunkX * 16 + 8 + random.nextInt(8) - 4, z = chunkZ * 16 + 8 + random.nextInt(8) - 4;
        int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 1 + random.nextInt(4);
        BlockPos p = new BlockPos(x, y, z);
        if (!world.getBlockState(p).isAir() || y >= world.getMaxY()) return false;
        return place(world, p, null, null, RandomSource.create(random.nextLong()), false);
    }

    /** Place a node; null type/modifier = rolled from the biome. */
    public static boolean place(LevelAccessor world, BlockPos p, NodeType type, NodeModifier mod, RandomSource random, boolean small) {
        world.setBlock(p, ModBlocks.AURA_NODE.get().defaultBlockState(), 2);
        if (!(world.getBlockEntity(p) instanceof AuraNodeBlockEntity node)) return false;
        roll(node, world.getBiome(p), random, type, mod, small);
        return true;
    }

    public static void randomize(AuraNodeBlockEntity node, LevelAccessor world, RandomSource random, NodeType type) {
        roll(node, world.getBiome(node.getBlockPos()), random, type, null, false);
    }

    public static void roll(AuraNodeBlockEntity node, Holder<Biome> biome, RandomSource r, NodeType type, NodeModifier mod, boolean small) {
        if (type == null) {
            type = NodeType.NORMAL;
            if (biome.is(EERIE) && r.nextInt(3) == 0) type = NodeType.DARK;
            else if (biome.is(TAINTED) && r.nextInt(3) == 0) type = NodeType.TAINTED;
            else if (biome.is(BiomeHandler.MAGICAL) && r.nextInt(SPECIAL_RARITY / 3) == 0) type = NodeType.PURE;
            else if (r.nextInt(SPECIAL_RARITY) == 0) type = switch (r.nextInt(10)) {
                case 0, 1, 2 -> NodeType.UNSTABLE;
                case 3, 4 -> NodeType.HUNGRY;
                case 5, 6 -> NodeType.DARK;
                case 7, 8 -> NodeType.TAINTED;
                default -> NodeType.PURE;
            };
        }
        if (mod == null) {
            mod = NodeModifier.NONE;
            if (r.nextInt(SPECIAL_RARITY / 2) == 0) mod = switch (r.nextInt(3)) { case 0 -> NodeModifier.BRIGHT; case 1 -> NodeModifier.PALE; default -> NodeModifier.FADING; };
        }
        node.type = type; node.modifier = mod;
        node.aspects.clear(); node.base.clear();
        int count = small ? 1 + r.nextInt(2) : 2 + r.nextInt(2);
        String tag = BiomeHandler.randomTag(biome, r);
        Aspect biased = tag == null ? null : AspectRegistry.get(tag);
        for (int i = 0; i < count; i++) {
            Aspect a = i == 0 && biased != null ? biased : PRIMALS.get(r.nextInt(PRIMALS.size()));
            int v = small ? 5 + r.nextInt(10) : 15 + r.nextInt(25);
            node.base.merge(a.id(), v, Integer::sum);
        }
        if (!small && r.nextInt(4) == 0) {
            var all = AspectRegistry.all().values().stream().filter(a -> !a.isPrimal()).toList();
            node.base.merge(all.get(r.nextInt(all.size())).id(), 5 + r.nextInt(10), Integer::sum);
        }
        switch (type) {
            case DARK -> node.base.merge(AspectRegistry.TENEBRAE.id(), 5 + r.nextInt(10), Integer::sum);
            case TAINTED -> node.base.merge(AspectRegistry.VITIUM.id(), 5 + r.nextInt(10), Integer::sum);
            case PURE -> node.base.merge(AspectRegistry.AURAM.id(), 5 + r.nextInt(5), Integer::sum);
            case HUNGRY -> node.base.merge(AspectRegistry.VACUOS.id(), 5 + r.nextInt(10), Integer::sum);
            default -> {}
        }
        node.base.replaceAll((k, v) -> Math.min(255, v));
        node.aspects.putAll(node.base);
        node.initialized = true;
        node.setChanged();
    }

    private AuraNodeGen() {}
}
