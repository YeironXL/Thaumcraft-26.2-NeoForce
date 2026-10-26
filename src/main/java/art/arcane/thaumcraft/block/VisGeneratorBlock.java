package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.VisGeneratorBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

/** TC6 BlockVisGenerator: faces the first neighbour that accepts energy when placed; redstone disables it. */
public final class VisGeneratorBlock extends BaseEntityBlock {
    public static final MapCodec<VisGeneratorBlock> CODEC = simpleCodec(VisGeneratorBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;

    public VisGeneratorBlock(Properties p) {
        super(p.strength(2.0f, 20.0f).sound(SoundType.WOOD).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(ENABLED, true));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, ENABLED); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new VisGeneratorBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.VIS_GENERATOR.get(), VisGeneratorBlockEntity::tick);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level l = ctx.getLevel();
        for (Direction face : Direction.values()) {
            var cap = l.getCapability(Capabilities.Energy.BLOCK, ctx.getClickedPos().relative(face), face.getOpposite());
            if (cap != null) return defaultBlockState().setValue(FACING, face).setValue(ENABLED, true);
        }
        // BlockTCDevice: facing from the placer (opposite when sneaking).
        Direction d = ctx.getNearestLookingDirection().getOpposite();
        return defaultBlockState().setValue(FACING, ctx.isSecondaryUseActive() ? d.getOpposite() : d).setValue(ENABLED, true);
    }

    @Override protected void onPlace(BlockState s, Level l, BlockPos pos, BlockState old, boolean moved) { updateState(l, pos, s); }
    @Override protected void neighborChanged(BlockState s, Level l, BlockPos pos, Block b, Orientation o, boolean moved) { updateState(l, pos, s); }
    private static void updateState(Level l, BlockPos pos, BlockState s) {
        if (l.isClientSide()) return;
        boolean flag = !l.hasNeighborSignal(pos);
        if (flag != s.getValue(ENABLED)) l.setBlock(pos, s.setValue(ENABLED, flag), 3);
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) {
            case UP -> Block.box(4, 0, 4, 12, 14, 12);
            case DOWN -> Block.box(4, 2, 4, 12, 16, 12);
            case NORTH -> Block.box(4, 4, 2, 12, 12, 16);
            case SOUTH -> Block.box(4, 4, 0, 12, 12, 14);
            case WEST -> Block.box(2, 4, 4, 16, 12, 12);
            case EAST -> Block.box(0, 4, 4, 14, 12, 12);
        };
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        if (!(world.getBlockEntity(pos) instanceof VisGeneratorBlockEntity te) || te.energy() <= 0) return;
        Direction face = state.getValue(FACING);
        double x = face.getStepX() == 0 ? rand.nextGaussian() * 0.1 : face.getStepX() * 0.1;
        double y = face.getStepY() == 0 ? rand.nextGaussian() * 0.1 : face.getStepY() * 0.1;
        double z = face.getStepZ() == 0 ? rand.nextGaussian() * 0.1 : face.getStepZ() * 0.1;
        // FXDispatcher.spark (pale blue).
        world.addParticle(new DustParticleOptions(0xA8A8FF, 0.6f), pos.getX() + 0.5 + x, pos.getY() + 0.5 + y, pos.getZ() + 0.5 + z, 0, 0, 0);
    }
}
