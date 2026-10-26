package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.PatternCrafterBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/** TC6 BlockPatternCrafter: horizontal facing, disabled by redstone, mode dial on the side that cycles the 10 patterns. */
public final class PatternCrafterBlock extends BaseEntityBlock {
    public static final MapCodec<PatternCrafterBlock> CODEC = simpleCodec(PatternCrafterBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    public PatternCrafterBlock(Properties p) {
        super(p.strength(2.0f, 10.0f).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ENABLED, true));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, ENABLED); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PatternCrafterBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PATTERN_CRAFTER.get(), PatternCrafterBlockEntity::tick);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection()).setValue(ENABLED, !ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
    }
    @Override protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block b, Orientation o, boolean moved) {
        if (world.isClientSide()) return;
        boolean flag = !world.hasNeighborSignal(pos);
        if (flag != state.getValue(ENABLED)) world.setBlock(pos, state.setValue(ENABLED, flag), 3);
    }
    /** TilePatternCrafter.getCuboidByFacing (block-relative). */
    public static AABB dial(Direction facing) {
        return switch (facing) {
            case EAST -> new AABB(0.125, 0.125, 0.375, 0.25, 0.375, 0.625);
            case NORTH -> new AABB(0.375, 0.125, 0.75, 0.625, 0.375, 0.875);
            case SOUTH -> new AABB(0.375, 0.125, 0.125, 0.625, 0.375, 0.25);
            default -> new AABB(0.75, 0.125, 0.375, 0.875, 0.375, 0.625);
        };
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        var rel = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        // the dial sits inside the full-block outline, so accept clicks on that face within the dial's footprint
        AABB d = dial(state.getValue(FACING)).inflate(0.13);
        if (!d.contains(rel) || !(world.getBlockEntity(pos) instanceof PatternCrafterBlockEntity te)) return InteractionResult.PASS;
        if (!world.isClientSide()) {
            te.cycle();
            world.playSound(null, pos, ModSounds.get("key"), SoundSource.BLOCKS, 0.5f, 1.0f);
        }
        return InteractionResult.SUCCESS;
    }
}
