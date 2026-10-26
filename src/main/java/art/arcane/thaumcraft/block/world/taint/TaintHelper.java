package art.arcane.thaumcraft.block.world.taint;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.entity.TaintSeedEntity;
import art.arcane.thaumcraft.registry.ModEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.AABB;

/** TC6 blocks.world.taint.TaintHelper: the taint seed registry and fibre/taint block spreading. */
public final class TaintHelper {
    /** ModConfig.CONFIG_WORLD defaults (taintSpreadRate 100, taintSpreadArea 32) and CONFIG_MISC.wussMode false. */
    public static final float TAINT_SPREAD_RATE = 100.0f;
    public static final int TAINT_SPREAD_AREA = 32;
    public static final boolean WUSS_MODE = false;

    private static final Map<ResourceKey<Level>, List<BlockPos>> SEEDS = new ConcurrentHashMap<>();

    public static void addTaintSeed(Level world, BlockPos pos) {
        SEEDS.computeIfAbsent(world.dimension(), k -> java.util.Collections.synchronizedList(new ArrayList<>())).add(pos.immutable());
    }

    public static void removeTaintSeed(Level world, BlockPos pos) {
        List<BlockPos> locs = SEEDS.get(world.dimension());
        if (locs != null && !locs.isEmpty()) locs.remove(pos);
    }

    public static boolean isNearTaintSeed(Level world, BlockPos pos) {
        double area = TAINT_SPREAD_AREA * TAINT_SPREAD_AREA;
        List<BlockPos> locs = SEEDS.get(world.dimension());
        if (locs != null && !locs.isEmpty()) {
            for (BlockPos p : List.copyOf(locs)) {
                if (p.distSqr(pos) <= area) {
                    // only a living seed keeps taint alive: a dying seed (still in the level for its death animation)
                    // must not vouch for a stale entry left by another seed at the same spot
                    // (a stale entry is dropped and the next seed in range is still considered)
                    if (inRange(world, p, TaintSeedEntity.class, 1.0, null).stream().noneMatch(net.minecraft.world.entity.Entity::isAlive)) {
                        removeTaintSeed(world, p);
                        continue;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isAtTaintSeedEdge(Level world, BlockPos pos) {
        double area = TAINT_SPREAD_AREA * TAINT_SPREAD_AREA;
        double fringe = TAINT_SPREAD_AREA * 0.8 * (TAINT_SPREAD_AREA * 0.8);
        List<BlockPos> locs = SEEDS.get(world.dimension());
        if (locs != null && !locs.isEmpty()) {
            for (BlockPos p : List.copyOf(locs)) {
                double d = p.distSqr(pos);
                if (d < area && d > fringe) return true;
            }
        }
        return false;
    }

    public static void spreadFibres(ServerLevel world, BlockPos pos) { spreadFibres(world, pos, false); }

    public static void spreadFibres(ServerLevel world, BlockPos pos, boolean ignore) {
        if (!ignore && WUSS_MODE) return;
        var rand = world.getRandom();
        float mod = 0.001f + NativeAura.fluxSaturation(world, pos) * 2.0f;
        if (!ignore && rand.nextFloat() > TAINT_SPREAD_RATE / 100.0f * mod) return;
        if (!isNearTaintSeed(world, pos)) return;
        BlockPos t = pos.offset(rand.nextInt(3) - 1, rand.nextInt(3) - 1, rand.nextInt(3) - 1);
        if (t.equals(pos)) return;
        if (!art.arcane.thaumcraft.world.outer.MazeGenerator.taintMayReach(world, t)) return;
        BlockState bs = world.getBlockState(t);
        float bh = bs.getDestroySpeed(world, t);
        if (bh < 0.0f || bh > 10.0f) return;
        boolean leaves = bs.is(BlockTags.LEAVES);
        Block fibre = ModBlocks.TAINT_FIBRE.get();
        if (!leaves && bs.getFluidState().isEmpty() && (bs.isAir() || bs.canBeReplaced() || bs.getBlock() instanceof FlowerBlock || bs.getBlock() instanceof VegetationBlock)
                && isAdjacentToSolidBlock(world, t) && !TaintFibreBlock.isOnlyAdjacentToTaint(world, t)) {
            place(world, t, fibre.defaultBlockState());
            return;
        }
        if (leaves) {
            Direction face;
            if (rand.nextFloat() < 0.6 && (face = getFaceBlockTouching(world, t, ModBlocks.TAINT_LOG.get())) != null)
                world.setBlockAndUpdate(t, ModBlocks.TAINT_FEATURE.get().defaultBlockState().setValue(BlockStateProperties.FACING, face.getOpposite()));
            else place(world, t, fibre.defaultBlockState());
            return;
        }
        if (TaintFibreBlock.isHemmedByTaint(world, t) && bh < 5.0f) {
            if (bs.is(BlockTags.LOGS) && !ITaintBlock.isTaint(bs)) {
                var axis = bs.hasProperty(RotatedPillarBlock.AXIS) ? bs.getValue(RotatedPillarBlock.AXIS) : Direction.Axis.Y;
                world.setBlockAndUpdate(t, ModBlocks.TAINT_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
                return;
            }
            if (isCrustMaterial(bs)) { place(world, t, ModBlocks.TAINT_CRUST.get().defaultBlockState()); return; }
            if (isSoilMaterial(bs)) { place(world, t, ModBlocks.TAINT_SOIL.get().defaultBlockState()); return; }
            if (isRockMaterial(bs)) { place(world, t, ModBlocks.TAINT_ROCK.get().defaultBlockState()); return; }
        }
        if ((bs.is(ModBlocks.TAINT_SOIL.get()) || bs.is(ModBlocks.TAINT_ROCK.get())) && world.isEmptyBlock(t.above()) && NativeAura.flux(world, t) >= 5.0f
                && rand.nextFloat() < TAINT_SPREAD_RATE / 100.0f * 0.33 && isAtTaintSeedEdge(world, t)) {
            TaintSeedEntity e = ModEntities.TAINT_SEED.get().create(world, EntitySpawnReason.NATURAL);
            if (e == null) return;
            e.snapTo(t.getX() + 0.5f, t.above().getY(), t.getZ() + 0.5f, rand.nextInt(360), 0.0f);
            if (e.getCanSpawnHere()) {
                NativeAura.drainFlux(world, t, 5.0f, false);
                world.addFreshEntity(e);
            }
        }
    }

    /** setBlockState + addBlockEvent(1) (the chorus-death squelch) + drainFlux 0.01. */
    private static void place(ServerLevel world, BlockPos t, BlockState s) {
        world.setBlockAndUpdate(t, s);
        world.blockEvent(t, s.getBlock(), 1, 0);
        NativeAura.drainFlux(world, t, 0.01f, false);
    }

    /** Mushroom blocks, Material.GOURD / CACTUS / CORAL / SPONGE / WOOD. */
    static boolean isCrustMaterial(BlockState bs) {
        return bs.is(Blocks.RED_MUSHROOM_BLOCK) || bs.is(Blocks.BROWN_MUSHROOM_BLOCK) || bs.is(Blocks.MUSHROOM_STEM)
                || bs.is(Blocks.PUMPKIN) || bs.is(Blocks.CARVED_PUMPKIN) || bs.is(Blocks.JACK_O_LANTERN) || bs.is(Blocks.MELON)
                || bs.is(Blocks.CACTUS) || bs.is(BlockTags.CORAL_BLOCKS) || bs.is(Blocks.SPONGE) || bs.is(Blocks.WET_SPONGE)
                || (bs.instrument() == NoteBlockInstrument.BASS && bs.ignitedByLava());
    }

    /** Material.SAND / GROUND / GRASS / CLAY. */
    static boolean isSoilMaterial(BlockState bs) {
        return bs.is(BlockTags.DIRT) || bs.is(BlockTags.SAND) || bs.is(Blocks.GRAVEL) || bs.is(Blocks.CLAY) || bs.is(Blocks.FARMLAND)
                || bs.is(Blocks.DIRT_PATH) || bs.is(Blocks.SOUL_SAND) || bs.is(Blocks.SOUL_SOIL);
    }

    /** Material.ROCK: 1.13+ gave every former rock block the BASEDRUM note block instrument. */
    static boolean isRockMaterial(BlockState bs) {
        return bs.instrument() == NoteBlockInstrument.BASEDRUM && bs.isSolid() && !ITaintBlock.isTaint(bs);
    }

    /** BlockUtils.isAdjacentToSolidBlock. */
    public static boolean isAdjacentToSolidBlock(Level world, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos n = pos.relative(d);
            if (world.getBlockState(n).isFaceSturdy(world, n, d.getOpposite())) return true;
        }
        return false;
    }

    /** BlockUtils.getFaceBlockTouching. */
    public static Direction getFaceBlockTouching(Level world, BlockPos pos, Block block) {
        for (Direction d : Direction.values()) if (world.getBlockState(pos.relative(d)).is(block)) return d;
        return null;
    }

    /** EntityUtils.getEntitiesInRange(world, pos, except, cls, range): boxes touching the block centre grown by range. */
    public static <T extends net.minecraft.world.entity.Entity> List<T> inRange(Level world, BlockPos pos, Class<T> cls, double range, net.minecraft.world.entity.Entity except) {
        var c = net.minecraft.world.phys.Vec3.atCenterOf(pos);
        return world.getEntitiesOfClass(cls, new AABB(c, c).inflate(range), e -> e != except);
    }

    private TaintHelper() {}
}
