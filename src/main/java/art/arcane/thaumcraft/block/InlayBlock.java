package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.blockentity.StabilizerBlockEntity;
import art.arcane.thaumcraft.crafting.infusion.IInfusionStabiliserExt;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * TC6 BlockInlay: a redstone-wire-like strip that carries a stabiliser's charge (0-15, minus one per block) to other
 * inlay and to pedestals, and stabilises infusion a little.
 */
public final class InlayBlock extends Block implements IInfusionStabiliserExt {
    public enum Attach implements StringRepresentable {
        NONE("none"), SIDE("side"), EXT("ext");
        private final String n; Attach(String n) { this.n = n; }
        @Override public String getSerializedName() { return n; }
    }
    public static final EnumProperty<Attach> NORTH = EnumProperty.create("north", Attach.class);
    public static final EnumProperty<Attach> EAST = EnumProperty.create("east", Attach.class);
    public static final EnumProperty<Attach> SOUTH = EnumProperty.create("south", Attach.class);
    public static final EnumProperty<Attach> WEST = EnumProperty.create("west", Attach.class);
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 15);
    private static final VoxelShape[] SHAPES = new VoxelShape[16];
    static {
        // BlockRedstoneWire.REDSTONE_WIRE_AABB, indexed by horizontal-index bits (S=0, W=1, N=2, E=3)
        double lo = 0.1875, hi = 0.8125;
        for (int i = 0; i < 16; i++) {
            double x0 = (i & 2) != 0 ? 0 : lo, x1 = (i & 8) != 0 ? 1 : hi, z0 = (i & 4) != 0 ? 0 : lo, z1 = (i & 1) != 0 ? 1 : hi;
            SHAPES[i] = Shapes.box(x0, 0, z0, x1, 0.0625, z1);
        }
    }

    public InlayBlock(Properties p) {
        super(p.strength(0.5f).sound(SoundType.METAL).noCollision().noOcclusion().lightLevel(s -> 1));
        registerDefaultState(stateDefinition.any().setValue(NORTH, Attach.NONE).setValue(EAST, Attach.NONE).setValue(SOUTH, Attach.NONE).setValue(WEST, Attach.NONE).setValue(CHARGE, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(NORTH, EAST, SOUTH, WEST, CHARGE); }
    private static EnumProperty<Attach> prop(Direction d) { return switch (d) { case NORTH -> NORTH; case EAST -> EAST; case SOUTH -> SOUTH; default -> WEST; }; }

    private static int aabbIndex(BlockState s) {
        boolean n = s.getValue(NORTH) != Attach.NONE, e = s.getValue(EAST) != Attach.NONE, so = s.getValue(SOUTH) != Attach.NONE, w = s.getValue(WEST) != Attach.NONE;
        int i = 0;
        if (n || (so && !n && !e && !w)) i |= 1 << Direction.NORTH.get2DDataValue();
        if (e || (w && !n && !e && !so)) i |= 1 << Direction.EAST.get2DDataValue();
        if (so || (n && !e && !so && !w)) i |= 1 << Direction.SOUTH.get2DDataValue();
        if (w || (e && !n && !so && !w)) i |= 1 << Direction.WEST.get2DDataValue();
        return i;
    }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPES[aabbIndex(s)]; }

    /** getActualState: connection to inlay/pedestals (SIDE) or a stabiliser (EXT). */
    private static Attach attach(BlockGetter world, BlockPos pos, Direction d) {
        BlockPos bp = pos.relative(d);
        BlockState bs = world.getBlockState(bp);
        if (canConnectTo(bs)) return Attach.SIDE;
        if (isSourceBlock(bs)) return Attach.EXT;
        return Attach.NONE;
    }
    static boolean canConnectTo(BlockState s) { return s.getBlock() instanceof InlayBlock || s.getBlock() instanceof PedestalBlock; }
    public static boolean isSourceBlock(BlockState s) { return s.is(ModBlocks.STABILIZER.get()); }
    private BlockState connections(BlockGetter world, BlockPos pos, BlockState s) {
        for (Direction d : Direction.Plane.HORIZONTAL) s = s.setValue(prop(d), attach(world, pos, d));
        return s;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return connections(ctx.getLevel(), ctx.getClickedPos(), defaultBlockState()); }
    @Override protected BlockState updateShape(BlockState s, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos np, BlockState ns, RandomSource r) {
        if (dir == Direction.DOWN && !canSurvive(s, level, pos)) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return dir.getAxis().isHorizontal() ? s.setValue(prop(dir), attach(level, pos, dir)) : s;
    }
    @Override protected boolean canSurvive(BlockState s, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
    @Override protected BlockState rotate(BlockState s, Rotation rot) {
        return switch (rot) {
            case CLOCKWISE_180 -> s.setValue(NORTH, s.getValue(SOUTH)).setValue(EAST, s.getValue(WEST)).setValue(SOUTH, s.getValue(NORTH)).setValue(WEST, s.getValue(EAST));
            case COUNTERCLOCKWISE_90 -> s.setValue(NORTH, s.getValue(EAST)).setValue(EAST, s.getValue(SOUTH)).setValue(SOUTH, s.getValue(WEST)).setValue(WEST, s.getValue(NORTH));
            case CLOCKWISE_90 -> s.setValue(NORTH, s.getValue(WEST)).setValue(EAST, s.getValue(NORTH)).setValue(SOUTH, s.getValue(EAST)).setValue(WEST, s.getValue(SOUTH));
            default -> s;
        };
    }
    @Override protected BlockState mirror(BlockState s, Mirror m) {
        return switch (m) {
            case LEFT_RIGHT -> s.setValue(NORTH, s.getValue(SOUTH)).setValue(SOUTH, s.getValue(NORTH));
            case FRONT_BACK -> s.setValue(EAST, s.getValue(WEST)).setValue(WEST, s.getValue(EAST));
            default -> s;
        };
    }

    // ---- charge network (shared with pedestals) ----
    public static void notifyInlayNeighborsOfStateChange(Level world, BlockPos pos) {
        BlockState bs = world.getBlockState(pos);
        if (canConnectTo(bs)) {
            world.updateNeighborsAt(pos, bs.getBlock());
            for (Direction d : Direction.Plane.HORIZONTAL) world.updateNeighborsAt(pos.relative(d), bs.getBlock());
        }
    }
    public static BlockState updateSurroundingInlay(Level world, BlockPos pos, BlockState state) {
        Set<BlockPos> update = Sets.newHashSet();
        state = calculateChanges(world, pos, pos, state, update);
        for (BlockPos bp : new ArrayList<>(update)) world.updateNeighborsAt(bp, world.getBlockState(pos).getBlock());
        return state;
    }
    private static int getMaxStrength(Level world, BlockPos pos, int strength) {
        BlockState bs = world.getBlockState(pos);
        if (!canConnectTo(bs) || !bs.hasProperty(CHARGE)) return strength;
        int i = bs.getValue(CHARGE);
        return i > strength ? i : strength;
    }
    public static int getSourceStrength(BlockGetter world, BlockPos pos) {
        for (Direction d : Direction.Plane.HORIZONTAL) { int e = getSourceStrengthAt(world, pos.relative(d)); if (e > 0) return e; }
        return 0;
    }
    public static int getSourceStrengthAt(BlockGetter world, BlockPos pos) {
        if (isSourceBlock(world.getBlockState(pos)) && world.getBlockEntity(pos) instanceof StabilizerBlockEntity te) return te.getEnergy();
        return 0;
    }
    private static BlockState calculateChanges(Level world, BlockPos pos1, BlockPos pos2, BlockState state, Set<BlockPos> update) {
        BlockState original = state;
        int current = state.getValue(CHARGE);
        int max = getMaxStrength(world, pos2, 0);
        int source = getSourceStrength(world, pos1);
        if (source > 0 && source > max - 1) max = source;
        int neighbour = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos bp = pos1.relative(d);
            if (bp.getX() != pos2.getX() || bp.getZ() != pos2.getZ()) neighbour = getMaxStrength(world, bp, neighbour);
        }
        if (neighbour > max) max = neighbour - 1;
        else if (max > 0) --max;
        else max = 0;
        if (source > max - 1) max = source;
        if (current != max) {
            state = state.setValue(CHARGE, max);
            if (world.getBlockState(pos1) == original) world.setBlock(pos1, state, 2);
            update.add(pos1);
            for (Direction d : Direction.values()) update.add(pos1.relative(d));
        }
        return state;
    }

    @Override protected void onPlace(BlockState s, Level world, BlockPos pos, BlockState old, boolean moved) {
        if (world.isClientSide() || old.is(this)) return;
        BlockState c = connections(world, pos, s);
        if (c != s) { world.setBlock(pos, c, 2); s = c; }
        updateSurroundingInlay(world, pos, s);
        for (Direction d : Direction.Plane.HORIZONTAL) notifyInlayNeighborsOfStateChange(world, pos.relative(d));
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState s, ServerLevel world, BlockPos pos, boolean moved) {
        for (Direction d : Direction.Plane.HORIZONTAL) world.updateNeighborsAt(pos.relative(d), this);
        for (Direction d : Direction.Plane.HORIZONTAL) notifyInlayNeighborsOfStateChange(world, pos.relative(d));
    }
    @Override protected void neighborChanged(BlockState s, Level world, BlockPos pos, Block b, Orientation o, boolean moved) {
        if (world.isClientSide()) return;
        if (canSurvive(s, world, pos)) updateSurroundingInlay(world, pos, s);
        else world.destroyBlock(pos, true);
    }

    /** colorMultiplier: grey 0.3 when empty, otherwise 0.5..1.0 by charge. */
    public static int colorMultiplier(int meta) {
        float f2 = meta == 0 ? 0.3f : (meta / 15.0f) * 0.5f + 0.5f;
        int i = Mth.clamp((int) (f2 * 255.0f), 0, 255);
        return 0xFF000000 | i << 16 | i << 8 | i;
    }

    @Override public void animateTick(BlockState s, Level world, BlockPos pos, RandomSource rand) {
        int charge = s.getValue(CHARGE);
        if (charge > 0 && rand.nextInt(20 - charge) == 0) {
            Direction face = Direction.Plane.HORIZONTAL.getRandomDirection(rand);
            if (s.getValue(prop(face)) != Attach.NONE) {
                double d0 = pos.getX() + 0.5 + rand.nextGaussian() * 0.08, d2 = pos.getY() + 0.025f, d3 = pos.getZ() + 0.5 + rand.nextGaussian() * 0.08;
                double f0 = face.getStepX() / 70.0 * (1.0 - rand.nextFloat() * 0.1), f2 = face.getStepZ() / 70.0 * (1.0 - rand.nextFloat() * 0.1);
                float r = Mth.nextInt(rand, 150, 200) / 255.0f, g = Mth.nextInt(rand, 0, 200) / 255.0f;
                art.arcane.thaumcraft.client.fx.FXDispatcher.drawLineSparkle(rand, d0, d2, d3, f0, 0.0, f2, 0.33f, r, g, g / 2.0f, 0, 1.0f, 0.0f, 16);
            }
        }
    }

    @Override public boolean canStabaliseInfusion(Level world, BlockPos pos) { return true; }
    @Override public float getStabilizationAmount(Level world, BlockPos pos) { return 0.025f; }
}
