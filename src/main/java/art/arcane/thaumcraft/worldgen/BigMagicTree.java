package art.arcane.thaumcraft.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of TC6 WorldGenBigMagicTree (the 1.12 big-oak algorithm with heightLimitLimit 11 and oak log/leaves), the
 * default tree of the magical forest. Like TC6 (one shared instance in BiomeGenMagicalForest) the height limit is
 * chosen on the first tree and then kept, only shortened by obstruction.
 */
public final class BigMagicTree extends TreeGen {
    private static volatile int sharedHeightLimit;
    private Random rand;
    private LevelAccessor world;
    private BlockPos basePos = BlockPos.ZERO;
    int heightLimit, height;
    final double heightAttenuation = 0.6618, branchSlope = 0.381, scaleWidth = 1.25, leafDensity = 0.9;
    final int trunkSize = 1, heightLimitLimit = 11, leafDistanceLimit = 4;
    List<long[]> foliageCoords; // x, y, z, branchBase
    private final List<BlockPos> leaves = new ArrayList<>();

    public BigMagicTree(boolean notify) { super(notify); }

    private static BlockPos add(BlockPos p, double x, double y, double z) { return BlockPos.containing(p.getX() + x, p.getY() + y, p.getZ() + z); }

    void generateLeafNodeList() {
        height = (int) (heightLimit * heightAttenuation);
        if (height >= heightLimit) height = heightLimit - 1;
        int i = (int) (1.382 + Math.pow(leafDensity * heightLimit / 13.0, 2.0));
        if (i < 1) i = 1;
        int j = basePos.getY() + height;
        int k = heightLimit - leafDistanceLimit;
        foliageCoords = new ArrayList<>();
        BlockPos top = basePos.above(k);
        foliageCoords.add(new long[] {top.getX(), top.getY(), top.getZ(), j});
        while (k >= 0) {
            float f = layerSize(k);
            if (f >= 0.0f) {
                for (int l = 0; l < i; ++l) {
                    double d0 = scaleWidth * f * (rand.nextFloat() + 0.328);
                    double d2 = rand.nextFloat() * 2.0f * Math.PI;
                    double d3 = d0 * Math.sin(d2) + 0.5;
                    double d4 = d0 * Math.cos(d2) + 0.5;
                    BlockPos bp = add(basePos, d3, k - 1, d4);
                    BlockPos bp2 = bp.above(leafDistanceLimit);
                    if (checkBlockLine(bp, bp2) == -1) {
                        int i2 = basePos.getX() - bp.getX();
                        int j2 = basePos.getZ() - bp.getZ();
                        double d5 = bp.getY() - Math.sqrt(i2 * i2 + j2 * j2) * branchSlope;
                        int k2 = d5 > j ? j : (int) d5;
                        BlockPos bp3 = new BlockPos(basePos.getX(), k2, basePos.getZ());
                        if (checkBlockLine(bp3, bp) == -1) foliageCoords.add(new long[] {bp.getX(), bp.getY(), bp.getZ(), bp3.getY()});
                    }
                }
            }
            --k;
        }
    }

    void crosSection(BlockPos c, float r, BlockState st) {
        for (int i = (int) (r + 0.618), j = -i; j <= i; ++j)
            for (int k = -i; k <= i; ++k)
                if (Math.pow(Math.abs(j) + 0.5, 2.0) + Math.pow(Math.abs(k) + 0.5, 2.0) <= r * r) {
                    BlockPos bp = c.offset(j, 0, k);
                    BlockState s = world.getBlockState(bp);
                    if (s.isAir() || isLeaves(s)) { set(world, bp, st); leaves.add(bp); }
                }
    }

    float layerSize(int y) {
        if (y < heightLimit * 0.3f) return -1.0f;
        float f = heightLimit / 2.0f;
        float f2 = f - y;
        float f3 = (float) Math.sqrt(f * f - f2 * f2);
        if (f2 == 0.0f) f3 = f;
        else if (Math.abs(f2) >= f) return 0.0f;
        return f3 * 0.5f;
    }

    float leafSize(int y) { return y >= 0 && y < leafDistanceLimit ? (y != 0 && y != leafDistanceLimit - 1 ? 3.0f : 2.0f) : -1.0f; }

    void generateLeafNode(BlockPos pos) {
        BlockState leaf = Blocks.OAK_LEAVES.defaultBlockState();
        for (int i = 0; i < leafDistanceLimit; ++i) crosSection(pos.above(i), leafSize(i), leaf);
    }

    void limb(BlockPos a, BlockPos b, Block block) {
        BlockPos d = b.offset(-a.getX(), -a.getY(), -a.getZ());
        int i = getGreatestDistance(d);
        float f = d.getX() / (float) i, f2 = d.getY() / (float) i, f3 = d.getZ() / (float) i;
        for (int j = 0; j <= i; ++j) {
            BlockPos p = add(a, 0.5f + j * f, 0.5f + j * f2, 0.5f + j * f3);
            set(world, p, block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, getLogAxis(a, p)));
        }
    }

    private static int getGreatestDistance(BlockPos p) {
        int i = Math.abs(p.getX()), j = Math.abs(p.getY()), k = Math.abs(p.getZ());
        return k > i && k > j ? k : (j > i ? j : i);
    }

    private static Direction.Axis getLogAxis(BlockPos a, BlockPos b) {
        Direction.Axis axis = Direction.Axis.Y;
        int i = Math.abs(b.getX() - a.getX()), j = Math.abs(b.getZ() - a.getZ()), k = Math.max(i, j);
        if (k > 0) { if (i == k) axis = Direction.Axis.X; else if (j == k) axis = Direction.Axis.Z; }
        return axis;
    }

    void generateLeaves() { for (long[] c : foliageCoords) generateLeafNode(new BlockPos((int) c[0], (int) c[1], (int) c[2])); }

    boolean leafNodeNeedsBase(int y) { return y >= heightLimit * 0.2; }

    void generateTrunk() { limb(basePos, basePos.above(height), Blocks.OAK_LOG); }

    void generateLeafNodeBases() {
        for (long[] c : foliageCoords) {
            int i = (int) c[3];
            BlockPos bp = new BlockPos(basePos.getX(), i, basePos.getZ());
            if (leafNodeNeedsBase(i - basePos.getY())) limb(bp, new BlockPos((int) c[0], (int) c[1], (int) c[2]), Blocks.OAK_LOG);
        }
    }

    int checkBlockLine(BlockPos a, BlockPos b) {
        BlockPos d = b.offset(-a.getX(), -a.getY(), -a.getZ());
        int i = getGreatestDistance(d);
        float f = d.getX() / (float) i, f2 = d.getY() / (float) i, f3 = d.getZ() / (float) i;
        if (i == 0) return -1;
        for (int j = 0; j <= i; ++j) if (!isReplaceable(world, add(a, 0.5f + j * f, 0.5f + j * f2, 0.5f + j * f3))) return j;
        return -1;
    }

    public boolean generate(LevelAccessor w, Random r, BlockPos pos) {
        world = w;
        basePos = pos;
        rand = new Random(r.nextLong());
        heightLimit = sharedHeightLimit;
        if (heightLimit == 0) heightLimit = sharedHeightLimit = 11 + rand.nextInt(heightLimitLimit);
        if (!validTreeLocation()) { world = null; return false; }
        generateLeafNodeList();
        generateLeaves();
        generateTrunk();
        generateLeafNodeBases();
        fixLeafDistances();
        world = null;
        return true;
    }

    /** 1.12 leaves had CHECK_DECAY false; 26.2 leaves need their log distance or they decay. */
    private void fixLeafDistances() {
        for (BlockPos p : leaves) {
            BlockState s = world.getBlockState(p);
            if (!(s.getBlock() instanceof LeavesBlock)) continue;
            int best = 7;
            for (int dx = -6; dx <= 6 && best > 1; ++dx) for (int dy = -6; dy <= 6; ++dy) for (int dz = -6; dz <= 6; ++dz) {
                int m = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
                if (m >= best) continue;
                if (world.getBlockState(p.offset(dx, dy, dz)).is(net.minecraft.tags.BlockTags.LOGS)) best = m;
            }
            set(world, p, s.setValue(LeavesBlock.DISTANCE, Math.max(1, best)).setValue(LeavesBlock.PERSISTENT, best >= 7));
        }
        leaves.clear();
    }

    private boolean validTreeLocation() {
        if (!isSoil(world.getBlockState(basePos.below()))) return false;
        int i = checkBlockLine(basePos, basePos.above(heightLimit - 1));
        if (i == -1) return true;
        if (i < 6) return false;
        heightLimit = i;
        return true;
    }
}
