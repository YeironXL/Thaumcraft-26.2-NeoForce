package art.arcane.thaumcraft.block.world.taint;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.registry.ModEffects;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.DeferredSoundType;

/**
 * TC6 BlockTaintFibre: thin taint mats on solid faces with occasional growths (seeded by position), spreading
 * while a taint seed is near and withering away without one. The 1.12 getActualState connections are stored
 * properties here, recomputed whenever a neighbour changes.
 */
public final class TaintFibreBlock extends Block implements ITaintBlock {
    public static final BooleanProperty NORTH = BooleanProperty.create("north"), EAST = BooleanProperty.create("east"),
            SOUTH = BooleanProperty.create("south"), WEST = BooleanProperty.create("west"), UP = BooleanProperty.create("up"),
            DOWN = BooleanProperty.create("down"), GROWTH1 = BooleanProperty.create("growth1"), GROWTH2 = BooleanProperty.create("growth2"),
            GROWTH3 = BooleanProperty.create("growth3"), GROWTH4 = BooleanProperty.create("growth4");
    /** SoundsTC.GORE. */
    public static final SoundType GORE = new DeferredSoundType(0.5f, 1.0f, () -> ModSounds.get("gore"), () -> ModSounds.get("gore"), () -> ModSounds.get("gore"), () -> ModSounds.get("gore"), () -> ModSounds.get("gore"));

    private static final VoxelShape AABB_UP = Block.box(0, 15.2, 0, 16, 16, 16), AABB_DOWN = Block.box(0, 0, 0, 16, 0.8, 16),
            AABB_EAST = Block.box(15.2, 0, 0, 16, 16, 16), AABB_WEST = Block.box(0, 0, 0, 0.8, 16, 16),
            AABB_SOUTH = Block.box(0, 0, 15.2, 16, 16, 16), AABB_NORTH = Block.box(0, 0, 0, 16, 16, 0.8);
    private static final VoxelShape G1 = Block.box(1.6, 0, 1.6, 14.4, 6.4, 14.4), G2 = Block.box(3.2, 0, 3.2, 12.8, 16, 12.8),
            G3 = Block.box(4, 0, 4, 12, 5, 12), G4 = Block.box(1.6, 4.8, 1.6, 14.4, 16, 14.4);

    public TaintFibreBlock(Properties p) {
        super(p.mapColor(MapColor.COLOR_PURPLE).strength(1.0f).sound(GORE).randomTicks().noOcclusion().replaceable().pushReaction(PushReaction.DESTROY)
                .lightLevel(s -> s.getValue(GROWTH3) ? 12 : s.getValue(GROWTH2) || s.getValue(GROWTH4) ? 6 : 0));
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
                .setValue(UP, false).setValue(DOWN, false).setValue(GROWTH1, false).setValue(GROWTH2, false).setValue(GROWTH3, false).setValue(GROWTH4, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, GROWTH1, GROWTH2, GROWTH3, GROWTH4); }

    @Override public void die(Level world, BlockPos pos, BlockState state) { world.removeBlock(pos, false); }

    private static boolean grown(BlockState s) { return s.getValue(GROWTH1) || s.getValue(GROWTH2) || s.getValue(GROWTH3) || s.getValue(GROWTH4); }

    @Override
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!grown(state) && isOnlyAdjacentToTaint(world, pos)) die(world, pos, state);
        else if (!(TaintHelper.isNearTaintSeed(world, pos) || art.arcane.thaumcraft.world.outer.MazeGenerator.permanentTaint(world, pos))) die(world, pos, state);
        else TaintHelper.spreadFibres(world, pos);
    }

    /** getActualState: faces touched + growth chosen from new Random(pos). */
    public static BlockState actualState(BlockState state, BlockGetter world, BlockPos pos) {
        boolean d = drawAt(world, pos.below(), Direction.DOWN), u = drawAt(world, pos.above(), Direction.UP);
        int growth = 0;
        int q = new Random(pos.asLong()).nextInt(50);
        if (d) { if (q < 4) growth = 1; else if (q == 4 || q == 5) growth = 2; else if (q == 6) growth = 3; }
        if (u && q > 47) growth = 4;
        return state.setValue(DOWN, d).setValue(UP, u).setValue(NORTH, drawAt(world, pos.north(), Direction.NORTH)).setValue(SOUTH, drawAt(world, pos.south(), Direction.SOUTH))
                .setValue(WEST, drawAt(world, pos.west(), Direction.WEST)).setValue(EAST, drawAt(world, pos.east(), Direction.EAST))
                .setValue(GROWTH1, growth == 1).setValue(GROWTH2, growth == 2).setValue(GROWTH3, growth == 3).setValue(GROWTH4, growth == 4);
    }

    private static boolean drawAt(BlockGetter world, BlockPos pos, Direction side) {
        BlockState b = world.getBlockState(pos);
        return !b.is(ModBlocks.TAINT_FIBRE.get()) && !b.is(ModBlocks.TAINT_FEATURE.get()) && b.isFaceSturdy(world, pos, side.getOpposite());
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return actualState(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos()); }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        BlockState actual = actualState(state, level, pos);
        if (actual != state) level.setBlock(pos, actual, 2);
    }

    /** getActualState refresh plus neighborChanged: a bare fibre touching only taint drops away. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos npos, BlockState nstate, RandomSource random) {
        BlockState actual = actualState(state, level, pos);
        if (!grown(actual) && isOnlyAdjacentToTaint(level, pos)) return Blocks.AIR.defaultBlockState();
        return actual;
    }

    public static boolean isOnlyAdjacentToTaint(BlockGetter world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos n = pos.relative(dir);
            BlockState s = world.getBlockState(n);
            if (!s.isAir() && !ITaintBlock.isTaint(s) && s.isFaceSturdy(world, n, dir.getOpposite())) return false;
        }
        return true;
    }

    public static boolean isHemmedByTaint(BlockGetter world, BlockPos pos) {
        int c = 0;
        for (Direction dir : Direction.values()) {
            BlockPos n = pos.relative(dir);
            BlockState block = world.getBlockState(n);
            if (ITaintBlock.isTaint(block)) ++c;
            else if (block.isAir()) --c;
            else if (block.getFluidState().isEmpty() && !block.isFaceSturdy(world, n, dir.getOpposite())) --c;
        }
        return c > 0;
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (!world.isClientSide() && entity instanceof LivingEntity le && !le.isInvertedHealAndHarm() && world.getRandom().nextInt(750) == 0)
            le.addEffect(new MobEffectInstance(ModEffects.FLUX_TAINT, 200, 0, false, true));
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        if (id == 1) {
            if (level.isClientSide()) level.playLocalSound(pos, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 0.1f, 0.9f + level.getRandom().nextFloat() * 0.2f, false);
            return true;
        }
        return super.triggerEvent(state, level, pos, id, param);
    }

    /** dropBlockAsItemWithChance: a GROWTH3 bulb may drop a flux crystal and always pollutes the aura. */
    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean xp) {
        super.spawnAfterBreak(state, level, pos, tool, xp);
        if (state.getValue(GROWTH3)) {
            if (level.getRandom().nextInt(5) <= 0) Block.popResource(level, pos, CrystalEssenceItem.of("vitium", 1));
            NativeAura.addFlux(level, pos, 1.0f);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        if (s.getValue(GROWTH1)) return G1;
        if (s.getValue(GROWTH2)) return G2;
        if (s.getValue(GROWTH3)) return G3;
        if (s.getValue(GROWTH4)) return G4;
        VoxelShape v = getCollisionShape(s, l, p, c);
        return v.isEmpty() ? AABB_DOWN : v;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        VoxelShape v = Shapes.empty();
        if (s.getValue(UP)) v = Shapes.or(v, AABB_UP);
        if (s.getValue(DOWN)) v = Shapes.or(v, AABB_DOWN);
        if (s.getValue(EAST)) v = Shapes.or(v, AABB_EAST);
        if (s.getValue(WEST)) v = Shapes.or(v, AABB_WEST);
        if (s.getValue(SOUTH)) v = Shapes.or(v, AABB_SOUTH);
        if (s.getValue(NORTH)) v = Shapes.or(v, AABB_NORTH);
        return v;
    }

    @Override protected boolean canBeReplaced(BlockState s, BlockPlaceContext ctx) { return true; }
    @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) { return 3; }
    @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) { return 3; }
}
