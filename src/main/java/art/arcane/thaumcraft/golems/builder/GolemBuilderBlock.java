package art.arcane.thaumcraft.golems.builder;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** TC6 BlockGolemBuilder: the golem press core (formed from a piston by salis mundus); breaking any part reverts the multiblock. */
public final class GolemBuilderBlock extends BaseEntityBlock {
    public static final MapCodec<GolemBuilderBlock> CODEC = simpleCodec(GolemBuilderBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static boolean ignore;
    public GolemBuilderBlock(Properties p) { super(p.strength(2.0f, 10.0f).sound(SoundType.STONE).noOcclusion()); registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GolemBuilderBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.GOLEM_BUILDER.get(), GolemBuilderBlockEntity::tick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide() && world.getBlockEntity(pos) instanceof GolemBuilderBlockEntity te) player.openMenu(te, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        if (!level.getBlockState(pos).is(this)) destroy(level, pos, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
    }
    /** TC6 BlockGolemBuilder.destroy: placeholders back to bars, anvil, cauldron and stone table; the core back to an upward piston. */
    public static void destroy(Level world, BlockPos pos, BlockPos startpos) {
        if (ignore || world.isClientSide()) return;
        ignore = true;
        try {
            for (int a = -1; a <= 1; ++a) for (int b = 0; b <= 1; ++b) for (int c = -1; c <= 1; ++c) {
                BlockPos p = pos.offset(a, b, c);
                if (p.equals(startpos)) continue;
                BlockState bs = world.getBlockState(p);
                if (bs.is(ModBlocks.PLACEHOLDER_BARS.get())) world.setBlock(p, Blocks.IRON_BARS.defaultBlockState(), 3);
                if (bs.is(ModBlocks.PLACEHOLDER_ANVIL.get())) world.setBlock(p, Blocks.ANVIL.defaultBlockState(), 3);
                if (bs.is(ModBlocks.PLACEHOLDER_CAULDRON.get())) world.setBlock(p, Blocks.CAULDRON.defaultBlockState(), 3);
                if (bs.is(ModBlocks.PLACEHOLDER_TABLE.get())) world.setBlock(p, ModBlocks.TABLE_STONE.get().defaultBlockState(), 3);
            }
            if (!pos.equals(startpos)) world.setBlock(pos, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.UP), 3);
        } finally { ignore = false; }
    }
}
