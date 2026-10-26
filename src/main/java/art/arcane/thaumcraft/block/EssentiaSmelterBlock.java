package art.arcane.thaumcraft.block;
import art.arcane.thaumcraft.blockentity.EssentiaSmelterBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public final class EssentiaSmelterBlock extends BaseEntityBlock {
    private static final MapCodec<EssentiaSmelterBlock> CODEC=simpleCodec(EssentiaSmelterBlock::new);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT=net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction> FACING=net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
    public EssentiaSmelterBlock(Properties p){super(p.strength(3).lightLevel(s->s.getValue(LIT)?13:0));registerDefaultState(defaultBlockState().setValue(LIT,false).setValue(FACING,Direction.NORTH));}
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){b.add(LIT,FACING);}
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    protected MapCodec<EssentiaSmelterBlock> codec(){return CODEC;}
    public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new EssentiaSmelterBlockEntity(p,s);}
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide()?null:createTickerHelper(t,ModBlockEntities.ESSENTIA_SMELTER.get(),EssentiaSmelterBlockEntity::tick);}
    protected InteractionResult useItemOn(ItemStack stack,BlockState s,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!player.mayUseItemAt(pos,hit.getDirection(),stack))return InteractionResult.FAIL;
        if(level.isClientSide())return InteractionResult.SUCCESS;
        if(!player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof EssentiaSmelterBlockEntity smelter){player.openMenu(smelter);return InteractionResult.SUCCESS;}
        if(level.getBlockEntity(pos) instanceof EssentiaSmelterBlockEntity smelter && smelter.insert(stack,hit.getDirection()!=Direction.UP))return InteractionResult.SUCCESS;
        player.sendSystemMessage(Component.translatable("message.thaumcraft.smelter_help"));return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    protected InteractionResult useWithoutItem(BlockState s,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!player.mayUseItemAt(pos,hit.getDirection(),ItemStack.EMPTY))return InteractionResult.FAIL;
        if(!level.isClientSide() && level.getBlockEntity(pos) instanceof EssentiaSmelterBlockEntity smelter){
            if(player.isShiftKeyDown()){var stack=smelter.retrieve(hit.getDirection()!=Direction.UP);if(!player.getInventory().add(stack))player.drop(stack,false);}
            else player.openMenu(smelter);
        }return InteractionResult.SUCCESS;
    }
}
