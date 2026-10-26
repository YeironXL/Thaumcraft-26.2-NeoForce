package art.arcane.thaumcraft.block;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
public final class SmelterAuxBlock extends Block {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    private static final MapCodec<SmelterAuxBlock> CODEC=simpleCodec(SmelterAuxBlock::new);
    public SmelterAuxBlock(Properties properties){super(properties.strength(1).sound(SoundType.METAL).noOcclusion());registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
    @Override protected MapCodec<SmelterAuxBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){var face=context.getClickedFace();if(!face.getAxis().isHorizontal())return null;var source=context.getLevel().getBlockState(context.getClickedPos().relative(face.getOpposite()));if(!(source.getBlock() instanceof EssentiaSmelterBlock)||source.getValue(EssentiaSmelterBlock.FACING)==face)return null;return defaultBlockState().setValue(FACING,face.getOpposite());}
}
