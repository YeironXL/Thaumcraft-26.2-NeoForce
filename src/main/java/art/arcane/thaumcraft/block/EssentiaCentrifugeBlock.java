package art.arcane.thaumcraft.block;
import art.arcane.thaumcraft.blockentity.EssentiaCentrifugeBlockEntity;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public final class EssentiaCentrifugeBlock extends BaseEntityBlock {
    private static final MapCodec<EssentiaCentrifugeBlock> CODEC=simpleCodec(EssentiaCentrifugeBlock::new);
    public EssentiaCentrifugeBlock(Properties properties){super(properties.strength(2).noOcclusion());}
    protected MapCodec<EssentiaCentrifugeBlock> codec(){return CODEC;}
    public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new EssentiaCentrifugeBlockEntity(pos,state);}
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide()?null:createTickerHelper(type,ModBlockEntities.ESSENTIA_CENTRIFUGE.get(),EssentiaCentrifugeBlockEntity::tick);}
    protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(3,0,3,13,16,13);}
    protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide() && level.getBlockEntity(pos) instanceof EssentiaCentrifugeBlockEntity machine){
            player.sendSystemMessage(Component.translatable("message.thaumcraft.centrifuge_status",name(machine.input()),name(machine.aspect()),machine.remaining(),Component.translatable(machine.paused()?"message.thaumcraft.paused":"message.thaumcraft.enabled")));
        }return InteractionResult.SUCCESS;
    }
    private static Component name(String aspect){return Component.translatable(aspect.isEmpty()?"message.thaumcraft.empty":aspect);}
}
