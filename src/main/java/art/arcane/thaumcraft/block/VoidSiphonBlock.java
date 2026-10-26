package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.VoidSiphonBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** TC6 BlockVoidSiphon (a BlockTCDevice: disabled by redstone); right-click opens its one-slot output GUI. */
public final class VoidSiphonBlock extends BaseEntityBlock {
    public static final MapCodec<VoidSiphonBlock> CODEC = simpleCodec(VoidSiphonBlock::new);
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 16, 13);
    private static final VoxelShape COLLISION = Shapes.or(Block.box(3, 0, 3, 13, 2, 13), Block.box(4, 2, 4, 12, 11, 12), Block.box(5, 12, 5, 10, 16, 10));

    public VoidSiphonBlock(Properties p) {
        super(p.strength(2.0F, 17.0F).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(ENABLED, true));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(ENABLED); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return COLLISION; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new VoidSiphonBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.VOID_SIPHON.get(), VoidSiphonBlockEntity::tick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && world.getBlockEntity(pos) instanceof VoidSiphonBlockEntity te) player.openMenu(te, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }
    @Override protected void onPlace(BlockState s, Level l, BlockPos pos, BlockState old, boolean moved) { update(l, pos, s); }
    @Override protected void neighborChanged(BlockState s, Level l, BlockPos pos, Block b, Orientation o, boolean moved) { update(l, pos, s); }
    private static void update(Level l, BlockPos pos, BlockState s) {
        if (l.isClientSide()) return;
        boolean on = !l.hasNeighborSignal(pos);
        if (on != s.getValue(ENABLED)) l.setBlock(pos, s.setValue(ENABLED, on), 3);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }
}
