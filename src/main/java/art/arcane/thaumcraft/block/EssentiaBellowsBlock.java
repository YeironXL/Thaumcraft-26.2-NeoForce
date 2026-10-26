package art.arcane.thaumcraft.block;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.item.context.BlockPlaceContext;
public final class EssentiaBellowsBlock extends Block {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.FACING;
    public EssentiaBellowsBlock(Properties p){super(p.strength(0.5F).noOcclusion());registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH));}
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getClickedFace().getOpposite());}
}
