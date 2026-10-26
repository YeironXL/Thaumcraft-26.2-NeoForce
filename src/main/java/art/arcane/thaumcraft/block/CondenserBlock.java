package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.CondenserBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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

/** TC6 BlockCondenser (BlockTCDevice: disabled by redstone). */
public final class CondenserBlock extends BaseEntityBlock {
    public static final MapCodec<CondenserBlock> CODEC = simpleCodec(CondenserBlock::new);
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public CondenserBlock(Properties p) {
        super(p.strength(2.0F, 17.0F).sound(SoundType.METAL).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(ENABLED, true));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(ENABLED); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CondenserBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CONDENSER.get(), CondenserBlockEntity::tick);
    }
    @Override protected void onPlace(BlockState s, Level l, BlockPos pos, BlockState old, boolean moved) { update(l, pos, s); }
    @Override protected void neighborChanged(BlockState s, Level l, BlockPos pos, Block b, Orientation o, boolean moved) {
        update(l, pos, s);
        if (l.getBlockEntity(pos) instanceof CondenserBlockEntity te) te.latticeCount = -1;
    }
    private static void update(Level l, BlockPos pos, BlockState s) {
        if (l.isClientSide()) return;
        boolean on = !l.hasNeighborSignal(pos);
        if (on != s.getValue(ENABLED)) l.setBlock(pos, s.setValue(ENABLED, on), 3);
    }
}
