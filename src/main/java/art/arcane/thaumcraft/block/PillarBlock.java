package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** TC6 BlockPillar: the formed infusion-altar pillar. Selection box is half a block, collision two blocks high; breaking returns two stones. */
public final class PillarBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    private static final VoxelShape COLLISION = Block.box(0, 0, 0, 16, 32, 16);

    public PillarBlock(Properties p) {
        super(p.strength(2.5f, 2.0f).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.BLOCK).noLootTable());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }
    @Override protected BlockState rotate(BlockState s, Rotation r) { return s.setValue(FACING, r.rotate(s.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState s, Mirror m) { return s.rotate(m.getRotation(s.getValue(FACING))); }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
    @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return COLLISION; }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        ItemStack drop = ItemStack.EMPTY;
        if (state.is(ModBlocks.PILLAR_ARCANE.get())) drop = new ItemStack(ModItems.BLOCK_STONE_ARCANE.get(), 2);
        else if (state.is(ModBlocks.PILLAR_ANCIENT.get())) drop = new ItemStack(ModBlocks.STONE_ANCIENT.get().asItem(), 2);
        else if (state.is(ModBlocks.PILLAR_ELDRITCH.get())) drop = new ItemStack(ModBlocks.STONE_ELDRITCH_TILE.get().asItem(), 2);
        if (!drop.isEmpty()) Block.popResource(level, pos, drop);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }
}
