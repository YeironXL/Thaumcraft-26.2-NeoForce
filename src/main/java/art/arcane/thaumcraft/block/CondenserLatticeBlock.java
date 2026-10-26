package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.blockentity.CondenserBlockEntity;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import com.mojang.serialization.MapCodec;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * TC6 BlockCondenserLattice (clean, light 5, or dirty). Lattices connect to each other and to a condenser below; one
 * that no longer leads down to a condenser within sqrt(74) blocks drops. A dirty lattice is cleaned with a filter,
 * which has a 50% chance to give a flux crystal.
 */
public final class CondenserLatticeBlock extends Block {
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN, UP = BlockStateProperties.UP, NORTH = BlockStateProperties.NORTH,
            SOUTH = BlockStateProperties.SOUTH, WEST = BlockStateProperties.WEST, EAST = BlockStateProperties.EAST;
    private static final BooleanProperty[] PROPS = { DOWN, UP, NORTH, SOUTH, WEST, EAST };
    private final boolean dirty;

    public CondenserLatticeBlock(Properties p, boolean dirty) {
        super(p.strength(0.5F, 5.0F).sound(SoundType.METAL).noOcclusion().lightLevel(s -> dirty ? 0 : 5));
        this.dirty = dirty;
        BlockState s = stateDefinition.any();
        for (BooleanProperty b : PROPS) s = s.setValue(b, false);
        registerDefaultState(s);
    }
    @Override protected MapCodec<? extends Block> codec() { return simpleCodec(p -> new CondenserLatticeBlock(p, dirty)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(PROPS); }

    private static boolean connects(BlockState n, Direction d) { return n.getBlock() instanceof CondenserLatticeBlock || (d == Direction.DOWN && n.is(ModBlocks.CONDENSER.get())); }

    private BlockState withConnections(BlockState s, BlockGetter level, BlockPos pos) {
        for (Direction d : Direction.values()) s = s.setValue(PROPS[d.ordinal()], connects(level.getBlockState(pos.relative(d)), d));
        return s;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return withConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos()); }
    @Override protected BlockState updateShape(BlockState s, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos np, BlockState ns, RandomSource r) {
        if (!level.isClientSide()) ticks.scheduleTick(pos, this, 1);
        return s.setValue(PROPS[dir.ordinal()], connects(ns, dir));
    }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return box(s.getValue(WEST) ? 0 : 5, s.getValue(DOWN) ? 0 : 5, s.getValue(NORTH) ? 0 : 5, s.getValue(EAST) ? 16 : 11, s.getValue(UP) ? 16 : 11, s.getValue(SOUTH) ? 16 : 11);
    }

    @Override protected void onPlace(BlockState s, Level l, BlockPos pos, BlockState old, boolean moved) { if (!l.isClientSide() && !old.is(s.getBlock())) triggerUpdate(l, pos); }
    @Override protected void tick(BlockState s, ServerLevel l, BlockPos pos, RandomSource r) { triggerUpdate(l, pos); }
    @Override protected void affectNeighborsAfterRemoval(BlockState s, ServerLevel l, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(s, l, pos, moved);
        for (Direction d : Direction.values()) if (l.getBlockState(pos.relative(d)).getBlock() instanceof CondenserLatticeBlock) l.scheduleTick(pos.relative(d), l.getBlockState(pos.relative(d)).getBlock(), 1);
        invalidateCondenser(l, pos, new HashSet<>());
    }

    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!dirty || !stack.is(ModItems.FILTER.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel sl) {
            stack.consume(1, player);
            Direction f = hit.getDirection();
            if (sl.getRandom().nextBoolean())
                sl.addFreshEntity(new ItemEntity(sl, pos.getX() + 0.5f + f.getStepX() / 3.0f, pos.getY() + 0.5f, pos.getZ() + 0.5f + f.getStepZ() / 3.0f, CrystalEssenceItem.of("vitium", 1)));
            sl.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.2f, ((sl.getRandom().nextFloat() - sl.getRandom().nextFloat()) * 0.7f + 1.0f) * 1.6f);
            BlockState clean = ModBlocks.CONDENSER_LATTICE.get().defaultBlockState();
            sl.setBlock(pos, ((CondenserLatticeBlock) clean.getBlock()).withConnections(clean, sl, pos), 3);
            triggerUpdate(sl, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** triggerUpdate: drop unless connected (through lattices) to a condenser below within sqrt(74). */
    public void triggerUpdate(Level world, BlockPos pos) {
        BlockPos p = findCondenser(world, pos, new HashSet<>());
        if (p == null || p.distSqr(pos) > 74.0) world.destroyBlock(pos, true);
    }

    private static BlockPos findCondenser(Level world, BlockPos pos, Set<Long> history) {
        history.add(pos.asLong());
        for (Direction face : Direction.values()) {
            BlockPos p2 = pos.relative(face);
            if (history.contains(p2.asLong())) continue;
            BlockState b = world.getBlockState(p2);
            if (b.getBlock() instanceof CondenserLatticeBlock) {
                BlockPos pp = findCondenser(world, p2, history);
                if (pp != null) return pp;
            }
            if (face == Direction.DOWN && b.is(ModBlocks.CONDENSER.get())) {
                if (world.getBlockEntity(p2) instanceof CondenserBlockEntity te) te.latticeCount = -1;
                return p2;
            }
        }
        return null;
    }

    private static void invalidateCondenser(Level world, BlockPos pos, Set<Long> history) { findCondenser(world, pos, history); }
}
