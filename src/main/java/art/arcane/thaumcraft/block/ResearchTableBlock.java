package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.ResearchTableBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class ResearchTableBlock extends BaseEntityBlock {
    private static final MapCodec<ResearchTableBlock> CODEC=simpleCodec(ResearchTableBlock::new);
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE=Shapes.or(Block.box(0,12,0,16,16,16),Block.box(2,0,2,4,12,4),Block.box(12,0,2,14,12,4),Block.box(2,0,12,4,12,14),Block.box(12,0,12,14,12,14));
    public ResearchTableBlock(Properties properties){super(properties.strength(2.5f).sound(SoundType.WOOD).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<ResearchTableBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ResearchTableBlockEntity(pos,state);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof ResearchTableBlockEntity table)player.openMenu(table);return InteractionResult.SUCCESS;
    }
}
