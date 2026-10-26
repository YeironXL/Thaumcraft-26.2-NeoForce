package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.LevitatorBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** TC6 BlockLevitator: facing column pusher, disabled by redstone, range dial on the face it pushes from. */
public final class LevitatorBlock extends BaseEntityBlock {
    public static final MapCodec<LevitatorBlock> CODEC = simpleCodec(LevitatorBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    private static final Map<Direction, AABB> DIAL = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
    static {
        DIAL.put(Direction.UP, new AABB(0.375, 0.0625, 0.375, 0.625, 0.125, 0.625));
        DIAL.put(Direction.DOWN, new AABB(0.375, 0.875, 0.375, 0.625, 0.9375, 0.625));
        DIAL.put(Direction.EAST, new AABB(0.0625, 0.375, 0.375, 0.125, 0.625, 0.625));
        DIAL.put(Direction.WEST, new AABB(0.875, 0.375, 0.375, 0.9375, 0.625, 0.625));
        DIAL.put(Direction.SOUTH, new AABB(0.375, 0.375, 0.0625, 0.625, 0.625, 0.125));
        DIAL.put(Direction.NORTH, new AABB(0.375, 0.375, 0.875, 0.625, 0.625, 0.9375));
        double f = 0.125;
        for (Direction d : Direction.values()) {
            VoxelShape body = Shapes.box(d.getStepX() > 0 ? f : 0, d.getStepY() > 0 ? f : 0, d.getStepZ() > 0 ? f : 0,
                    1 - (d.getStepX() < 0 ? f : 0), 1 - (d.getStepY() < 0 ? f : 0), 1 - (d.getStepZ() < 0 ? f : 0));
            SHAPES.put(d, Shapes.or(body, Shapes.create(DIAL.get(d))));
        }
    }

    public LevitatorBlock(Properties p) {
        super(p.strength(2.0f, 5.0f).sound(SoundType.WOOD).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(ENABLED, true));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, ENABLED); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPES.get(s.getValue(FACING)); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LevitatorBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.LEVITATOR.get(), LevitatorBlockEntity::tick);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction d = ctx.getNearestLookingDirection().getOpposite();
        if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()) d = d.getOpposite();
        return defaultBlockState().setValue(FACING, d).setValue(ENABLED, !ctx.getLevel().hasNeighborSignal(ctx.getClickedPos()));
    }
    @Override
    protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block b, Orientation o, boolean moved) {
        if (world.isClientSide()) return;
        boolean flag = !world.hasNeighborSignal(pos);
        if (flag != state.getValue(ENABLED)) world.setBlock(pos, state.setValue(ENABLED, flag), 3);
    }
    public static boolean hitsDial(BlockState state, BlockPos pos, Vec3 hit) {
        Vec3 rel = hit.subtract(pos.getX(), pos.getY(), pos.getZ());
        return DIAL.get(state.getValue(FACING)).inflate(0.01).contains(rel);
    }
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!hitsDial(state, pos, hit.getLocation()) || !(world.getBlockEntity(pos) instanceof LevitatorBlockEntity te)) return InteractionResult.PASS;
        te.increaseRange(player);
        if (!world.isClientSide()) world.playSound(null, pos, ModSounds.get("key"), SoundSource.BLOCKS, 0.5f, 1.0f);
        return InteractionResult.SUCCESS;
    }
}
