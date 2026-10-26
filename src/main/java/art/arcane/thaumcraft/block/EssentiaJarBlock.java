package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity;
import art.arcane.thaumcraft.item.EssentiaPhialItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class EssentiaJarBlock extends BaseEntityBlock {
    private static final MapCodec<EssentiaJarBlock> CODEC=simpleCodec(EssentiaJarBlock::new);
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty FILL=net.minecraft.world.level.block.state.properties.IntegerProperty.create("fill",0,4);
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty COLOR=net.minecraft.world.level.block.state.properties.IntegerProperty.create("color",0,36);
    public EssentiaJarBlock(Properties properties) { super(properties.strength(0.3F).noOcclusion());registerDefaultState(defaultBlockState().setValue(FILL,0).setValue(COLOR,0)); }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> b){b.add(FILL,COLOR);}
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level l,BlockState s,net.minecraft.world.level.block.entity.BlockEntityType<T> t){return l.isClientSide()?null:createTickerHelper(t,art.arcane.thaumcraft.registry.ModBlockEntities.ESSENTIA_JAR.get(),EssentiaJarBlockEntity::tick);}
    @Override protected MapCodec<EssentiaJarBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new EssentiaJarBlockEntity(pos,state); }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) { return box(3,0,3,13,12,13); }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);
        if(placer!=null&&level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity jar)jar.setFacing(placer.getDirection().getOpposite());
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if (stack.is(ModItems.JAR_BRACE.get()) && level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity braced && !braced.alembic() && !braced.blocked()) {
            if (level.isClientSide()) { level.playLocalSound(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,art.arcane.thaumcraft.registry.ModSounds.get("key"),net.minecraft.sounds.SoundSource.BLOCKS,1.0F,1.0F,false); return InteractionResult.SUCCESS; }
            braced.setBlocked(true); stack.shrink(1); return InteractionResult.SUCCESS;
        }
        boolean empty=stack.is(ModItems.PHIAL_EMPTY.get()) || stack.is(ModItems.PHIAL.get());
        boolean filled=stack.is(ModItems.PHIAL_FILLED.get());
        if (!empty && !filled) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!player.mayUseItemAt(pos,hit.getDirection(),stack)) return InteractionResult.FAIL;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity jar)) return InteractionResult.PASS;
        ItemStack result;
        if (empty) {
            String aspect=jar.contents().aspect();
            if (!jar.extract(10)) return InteractionResult.FAIL;
            result=EssentiaPhialItem.filled(aspect);
        } else {
            String aspect=stack.get(art.arcane.thaumcraft.registry.ModComponents.ASPECT.get());
            if (jar.alembic() || aspect == null || !jar.insert(aspect,10)) return InteractionResult.FAIL;
            result=new ItemStack(ModItems.PHIAL_EMPTY.get());
        }
        // Consume containers even in creative, matching the original transfer contract.
        stack.shrink(1);
        if (stack.isEmpty()) player.setItemInHand(hand,result);
        else if (!player.getInventory().add(result)) player.drop(result,false);
        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.BOTTLE_FILL,net.minecraft.sounds.SoundSource.BLOCKS,0.25F,1F);
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity labelled && player.isShiftKeyDown() && !labelled.filter().isEmpty() && hit.getDirection()==labelled.facing()) {
            if (level.isClientSide()) level.playLocalSound(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,art.arcane.thaumcraft.registry.ModSounds.get("page"),net.minecraft.sounds.SoundSource.BLOCKS,1.0F,1.0F,false);
            else {
                labelled.setFilter("");
                var d=hit.getDirection();
                level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+0.5F+d.getStepX()/3.0F,pos.getY()+0.5F,pos.getZ()+0.5F+d.getStepZ()/3.0F,new ItemStack(ModItems.LABEL_BLANK.get())));
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EssentiaJarBlockEntity jar) {
            if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty()
                    && player.mayUseItemAt(pos,hit.getDirection(),player.getMainHandItem())
                    && level instanceof net.minecraft.server.level.ServerLevel world) {
                int amount=jar.contents().amount();
                if (jar.extract(amount)) {
                    var chunk=world.getChunkAt(pos);
                    art.arcane.thaumcraft.aura.NativeAura.get(world,chunk).addFlux(amount);
                    chunk.markUnsaved();
                }
            }
            var contents=jar.contents();
            player.sendSystemMessage(Component.translatable("message.thaumcraft.jar_contents",contents.amount(),jar.capacity(),Component.translatable(contents.amount()==0 ? "message.thaumcraft.empty" : contents.aspect())));
        }
        return InteractionResult.SUCCESS;
    }
}
