package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.PotionSprayerBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.BlockHitResult;

/** TC6 BlockPotionSprayer: faces the placer (opposite when sneaking); redstone turns it "off", which fires one charge. */
public final class PotionSprayerBlock extends BaseEntityBlock {
    public static final MapCodec<PotionSprayerBlock> CODEC = simpleCodec(PotionSprayerBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    public PotionSprayerBlock(Properties p) {
        super(p.strength(2.0f, 20.0f).sound(SoundType.METAL));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(ENABLED, true));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, ENABLED); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PotionSprayerBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.POTION_SPRAYER.get(), PotionSprayerBlockEntity::tick);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) {
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
    @Override protected InteractionResult useWithoutItem(BlockState s, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && world.getBlockEntity(pos) instanceof PotionSprayerBlockEntity te) player.openMenu(te, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }
}
