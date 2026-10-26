package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.blockentity.EssentiaTubeBlockEntity;

import art.arcane.thaumcraft.registry.ModBlockEntities;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;

import net.minecraft.world.level.*;

import net.minecraft.world.level.block.*;

import net.minecraft.world.level.block.entity.*;

import net.minecraft.world.level.block.state.*;

import net.minecraft.world.level.block.state.properties.BooleanProperty;

import net.minecraft.world.phys.shapes.*;

public final class EssentiaTubeBlock extends BaseEntityBlock {

    private static final MapCodec<EssentiaTubeBlock> CODEC=simpleCodec(EssentiaTubeBlock::new);

    public static final BooleanProperty[] SIDES={BooleanProperty.create("down"),BooleanProperty.create("up"),BooleanProperty.create("north"),BooleanProperty.create("south"),BooleanProperty.create("west"),BooleanProperty.create("east")};

    public static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.core.Direction> FACING=net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

    public static final BooleanProperty ENABLED=BooleanProperty.create("enabled");

    public EssentiaTubeBlock(Properties p){super(p.strength(0.5F).noOcclusion());var s=defaultBlockState().setValue(FACING,net.minecraft.core.Direction.UP).setValue(ENABLED,true);for(var side:SIDES)s=s.setValue(side,false);registerDefaultState(s);}

    protected MapCodec<EssentiaTubeBlock> codec(){return CODEC;}

    protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(SIDES);b.add(FACING,ENABLED);}

    protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){

        var shape=(s.is(art.arcane.thaumcraft.ModBlocks.TUBE_BUFFER.get()) || s.is(art.arcane.thaumcraft.ModBlocks.TUBE_FILTER.get()))?box(4,4,4,12,12,12):box(6,6,6,10,10,10);

        if(s.getValue(SIDES[0]))shape=Shapes.or(shape,box(6,0,6,10,6,10));

        if(s.getValue(SIDES[1]))shape=Shapes.or(shape,box(6,10,6,10,16,10));

        if(s.getValue(SIDES[2]))shape=Shapes.or(shape,box(6,6,0,10,10,6));

        if(s.getValue(SIDES[3]))shape=Shapes.or(shape,box(6,6,10,10,10,16));

        if(s.getValue(SIDES[4]))shape=Shapes.or(shape,box(0,6,6,6,10,10));

        if(s.getValue(SIDES[5]))shape=Shapes.or(shape,box(10,6,6,16,10,10));return shape;

    }

    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getNearestLookingDirection());}

    private net.minecraft.core.Direction selectedSide(net.minecraft.world.phys.BlockHitResult hit){

        var v=hit.getLocation().subtract(net.minecraft.world.phys.Vec3.atCenterOf(hit.getBlockPos()));

        double x=Math.abs(v.x),y=Math.abs(v.y),z=Math.abs(v.z);

        return y>=x&&y>=z?(v.y>0?net.minecraft.core.Direction.UP:net.minecraft.core.Direction.DOWN):x>=z?(v.x>0?net.minecraft.core.Direction.EAST:net.minecraft.core.Direction.WEST):(v.z>0?net.minecraft.core.Direction.SOUTH:net.minecraft.core.Direction.NORTH);

    }

    protected net.minecraft.world.InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState s,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){

        if(!player.mayUseItemAt(pos,hit.getDirection(),stack))return net.minecraft.world.InteractionResult.FAIL;

        if(level.getBlockEntity(pos) instanceof EssentiaTubeBlockEntity tube && tube.kind()==EssentiaTubeBlockEntity.Kind.FILTER){

            String tag=stack.get(art.arcane.thaumcraft.registry.ModComponents.ASPECT.get());

            var jar=stack.get(art.arcane.thaumcraft.registry.ModComponents.ESSENTIA.get());

            if(tag==null && jar!=null)tag=jar.aspect();

            if(tag!=null && !tag.isEmpty()){

                if(!level.isClientSide()){tube.setFilter(tag);player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.thaumcraft.tube_filter",net.minecraft.network.chat.Component.translatable(tag)));}

                return net.minecraft.world.InteractionResult.SUCCESS;

            }

        }return net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND;

    }

    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState s,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit){

        if(!player.getMainHandItem().isEmpty() || !player.mayUseItemAt(pos,hit.getDirection(),net.minecraft.world.item.ItemStack.EMPTY))return net.minecraft.world.InteractionResult.PASS;

        if(!level.isClientSide() && level.getBlockEntity(pos) instanceof EssentiaTubeBlockEntity tube){

            var v=hit.getLocation().subtract(net.minecraft.world.phys.Vec3.atCenterOf(pos));

            boolean arm=Math.max(Math.abs(v.x),Math.max(Math.abs(v.y),Math.abs(v.z)))>0.26;

            var side=selectedSide(hit);

            if(tube.kind()==EssentiaTubeBlockEntity.Kind.BUFFER && player.isShiftKeyDown())tube.cycleChoke(side);

            else if(tube.kind()==EssentiaTubeBlockEntity.Kind.FILTER && player.isShiftKeyDown())tube.setFilter("");

            else if(arm || player.isShiftKeyDown() || tube.kind()==EssentiaTubeBlockEntity.Kind.BUFFER || tube.kind()==EssentiaTubeBlockEntity.Kind.FILTER)tube.toggleSide(side);

            else if(tube.kind()==EssentiaTubeBlockEntity.Kind.VALVE)tube.toggleValve();

            else if(tube.kind()==EssentiaTubeBlockEntity.Kind.ONEWAY)level.setBlock(pos,s.setValue(FACING,net.minecraft.core.Direction.values()[(tube.facing().ordinal()+1)%6]),3);

            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.thaumcraft.tube_status",tube.amount(),tube.suction(side),tube.filter().isEmpty()?"-":tube.filter(),tube.open(),tube.choke(side)));

        }return net.minecraft.world.InteractionResult.SUCCESS;

    }

    public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new EssentiaTubeBlockEntity(p,s);}

    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide()?null:createTickerHelper(t,ModBlockEntities.ESSENTIA_TUBE.get(),EssentiaTubeBlockEntity::tick);}

}

