package art.arcane.thaumcraft.blockentity;
import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.scan.ScanRegistry;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.minecraft.server.level.ServerLevel;
import com.mojang.serialization.Codec;
import java.util.*;

/** Two-slot distillery: one input and one fuel; buffered output is sent to stacked alembics. */
public final class EssentiaSmelterBlockEntity extends BlockEntity implements net.minecraft.world.WorldlyContainer,net.minecraft.world.MenuProvider {
    private ItemStack input=ItemStack.EMPTY,fuel=ItemStack.EMPTY;
    private ItemStack cookingInput=ItemStack.EMPTY;
    private final Map<String,Integer> aspects=new LinkedHashMap<>();
    private int burn,cook,burnDuration;
    private static final Codec<Map<String,Integer>> ASPECTS=Codec.unboundedMap(Codec.STRING,Codec.intRange(1,256));
    public EssentiaSmelterBlockEntity(BlockPos p,BlockState s){super(ModBlockEntities.ESSENTIA_SMELTER.get(),p,s);}
    public int total(){return aspects.values().stream().mapToInt(Integer::intValue).sum();}
    public int burn(){return burn;}
    public int cook(){return cook;}
    public int bellowsCount(){
        if(level==null)return 0;int count=0;var front=getBlockState().getValue(art.arcane.thaumcraft.block.EssentiaSmelterBlock.FACING);
        for(var side:Direction.Plane.HORIZONTAL){if(side==front)continue;var neighbour=worldPosition.relative(side);var state=level.getBlockState(neighbour);
            if(state.is(ModBlocks.BELLOWS.get())&&state.getValue(art.arcane.thaumcraft.block.EssentiaBellowsBlock.FACING)==side.getOpposite()&&!level.hasNeighborSignal(neighbour))count++;}
        return count;
    }
    public int cookDuration(){int total=tags(input).values().stream().mapToInt(Integer::intValue).sum();return total==0?0:Math.max(1,(int)(total*2*(1F-0.125F*bellowsCount())));}
    public int efficiencyPercent(){return getBlockState().is(ModBlocks.SMELTER_THAUMIUM.get())?90:getBlockState().is(ModBlocks.SMELTER_VOID.get())?95:80;}
    @Override public net.minecraft.network.chat.Component getDisplayName(){return getBlockState().getBlock().getName();}
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,net.minecraft.world.entity.player.Inventory inventory,net.minecraft.world.entity.player.Player player){
        var data=new net.minecraft.world.inventory.ContainerData(){
            @Override public int get(int index){return switch(index){
                case 0 -> cook;case 1 -> cookDuration();
                case 2 -> burnDuration>0?(int)Math.min(1000,(long)burn*1000/burnDuration):0;
                case 3 -> (int)Math.min(32767,((long)burn+19)/20);
                case 4 -> total();case 5 -> efficiencyPercent();default -> 0;
            };}
            @Override public void set(int index,int value){}
            @Override public int getCount(){return 6;}
        };
        return new art.arcane.thaumcraft.menu.EssentiaSmelterMenu(id,inventory,this,data);
    }
    public ItemStack input(){return input.copy();}
    public ItemStack fuel(){return fuel.copy();}
    @Override public int getContainerSize(){return 2;}
    @Override public boolean isEmpty(){return input.isEmpty() && fuel.isEmpty();}
    @Override public ItemStack getItem(int slot){return slot==0?input:slot==1?fuel:ItemStack.EMPTY;}
    @Override public void setItem(int slot,ItemStack stack){
        if(slot<0 || slot>1)return;
        stack.limitSize(getMaxStackSize(stack));
        if(slot==0)input=stack;else fuel=stack;
        setChanged();
    }
    @Override public ItemStack removeItem(int slot,int count){
        if(count<=0)return ItemStack.EMPTY;
        var result=getItem(slot).split(count);
        if(!result.isEmpty())setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot){
        var result=getItem(slot);
        if(slot==0)input=ItemStack.EMPTY;else if(slot==1)fuel=ItemStack.EMPTY;
        return result;
    }
    @Override public void clearContent(){input=ItemStack.EMPTY;fuel=ItemStack.EMPTY;setChanged();}
    @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return net.minecraft.world.Container.stillValidBlockEntity(this,player);}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){
        return !stack.isEmpty() && (slot==0?!tags(stack).isEmpty():slot==1 && level!=null && fuelTime(stack)>0);
    }
    // TC6 TileSmelter: top has no item slots; sides expose input, bottom exposes fuel.
    @Override public int[] getSlotsForFace(Direction side){return side==Direction.UP?new int[0]:new int[]{side==Direction.DOWN?1:0};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){
        return side!=null && side!=Direction.UP && slot==(side==Direction.DOWN?1:0) && canPlaceItem(slot,stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){
        return side!=Direction.UP && slot==(side==Direction.DOWN?1:0);
    }
    public static Map<String,Integer> tags(ItemStack stack){
        var result=new LinkedHashMap<String,Integer>();
        var crystal=art.arcane.thaumcraft.item.CrystalEssenceItem.aspect(stack);
        if(!crystal.isEmpty())result.put(crystal,1);
        else ScanRegistry.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).forEach((aspect,count)->result.put(aspect.id(),count));
        return result;
    }
    private int fuelTime(ItemStack stack){return stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING,level.fuelValues());}
    public boolean insert(ItemStack stack,boolean asFuel){
        if(stack.isEmpty() || level==null || (asFuel?fuelTime(stack)<=0:tags(stack).isEmpty()))return false;
        ItemStack current=asFuel?fuel:input;
        if(!current.isEmpty() && (!ItemStack.isSameItemSameComponents(current,stack)||current.getCount()>=current.getMaxStackSize()))return false;
        if(current.isEmpty()){if(asFuel)fuel=stack.copyWithCount(1);else input=stack.copyWithCount(1);}else current.grow(1);
        stack.shrink(1);setChanged();return true;
    }
    public ItemStack retrieve(boolean asFuel){var result=(asFuel?fuel:input).copy();if(asFuel)fuel=ItemStack.EMPTY;else {input=ItemStack.EMPTY;cook=0;}setChanged();return result;}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(!input.isEmpty())out.store("input",ItemStack.CODEC,input);if(!fuel.isEmpty())out.store("fuel",ItemStack.CODEC,fuel);out.store("aspects",ASPECTS,aspects);out.putInt("burn",burn);out.putInt("burn_duration",burnDuration);out.putInt("cook",cook);if(cook>0 && !cookingInput.isEmpty())out.store("cooking_input",ItemStack.CODEC,cookingInput);}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);input=in.read("input",ItemStack.CODEC).orElse(ItemStack.EMPTY);fuel=in.read("fuel",ItemStack.CODEC).orElse(ItemStack.EMPTY);aspects.clear();in.read("aspects",ASPECTS).orElse(Map.of()).forEach((tag,n)->{if(art.arcane.thaumcraft.aspect.AspectRegistry.get(tag)!=null && n<=256-total())aspects.put(tag,n);});burn=Math.max(0,in.getIntOr("burn",0));burnDuration=Math.max(burn,in.getIntOr("burn_duration",burn));cook=Math.max(0,in.getIntOr("cook",0));cookingInput=cook>0?in.read("cooking_input",ItemStack.CODEC).orElseGet(()->input.copyWithCount(1)):ItemStack.EMPTY;}
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){
        if(level instanceof ServerLevel world){net.minecraft.world.Containers.dropItemStack(world,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,input);net.minecraft.world.Containers.dropItemStack(world,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,fuel);pollute(world,total());}
        super.preRemoveSideEffects(pos,state);
    }
    /** TC6 smelter vents: each flux point gets a 1/3 chance per attached vent (S, W, N, E order) to be vented harmlessly. Returns the rest. */
    int vent(ServerLevel world,BlockPos pos,net.minecraft.world.level.block.state.BlockState state,int flux){
        var front=state.getValue(art.arcane.thaumcraft.block.EssentiaSmelterBlock.FACING);int pp=0;
        Direction[] faces={Direction.SOUTH,Direction.WEST,Direction.NORTH,Direction.EAST};
        for(int c=0;c<flux;c++){
            boolean vented=false;
            for(var face:faces){
                if(face==front)continue;
                var vent=world.getBlockState(pos.relative(face));
                if(vent.is(ModBlocks.SMELTER_VENT.get())&&vent.getValue(art.arcane.thaumcraft.block.SmelterVentBlock.FACING)==face.getOpposite()&&world.getRandom().nextFloat()<0.333F){
                    world.blockEvent(pos,state.getBlock(),1,face.getOpposite().get3DDataValue());vented=true;break;
                }
            }
            if(!vented)pp++;
        }
        return pp;
    }
    @Override public boolean triggerEvent(int id,int param){
        if(id==1){
            if(level!=null&&level.isClientSide()){
                var d=Direction.from3DDataValue(param).getOpposite();var r=level.getRandom();
                level.playLocalSound(worldPosition.getX()+0.5+d.getStepX(),worldPosition.getY()+0.5,worldPosition.getZ()+0.5+d.getStepZ(),net.minecraft.sounds.SoundEvents.LAVA_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,0.25F,2.6F+(r.nextFloat()-r.nextFloat())*0.8F,true);
                for(int a=0;a<4;++a){
                    float fx=0.1F-r.nextFloat()*0.2F,fz=0.1F-r.nextFloat()*0.2F,fy=0.1F-r.nextFloat()*0.2F,fx2=0.1F-r.nextFloat()*0.2F,fz2=0.1F-r.nextFloat()*0.2F,fy2=0.1F-r.nextFloat()*0.2F;
                    art.arcane.thaumcraft.client.fx.FXDispatcher.drawVentParticles(worldPosition.getX()+0.5F+fx+d.getStepX(),worldPosition.getY()+0.5F+fy,worldPosition.getZ()+0.5F+fz+d.getStepZ(),d.getStepX()/4.0F+fx2,d.getStepY()/4.0F+fy2,d.getStepZ()/4.0F+fz2,0xAAAAAA);
                }
            }
            return true;
        }
        return super.triggerEvent(id,param);
    }
    private void pollute(ServerLevel world,int amount){if(amount>0){var chunk=world.getChunkAt(worldPosition);art.arcane.thaumcraft.aura.NativeAura.get(world,chunk).addFlux(amount);chunk.markUnsaved();}}
    private boolean output(ServerLevel world,BlockPos origin,String tag){
        for(int pass=0;pass<2;pass++)for(int y=origin.getY()+1;y<world.getMaxY();y++){
            var pos=new BlockPos(origin.getX(),y,origin.getZ());
            if(!(world.getBlockEntity(pos) instanceof EssentiaJarBlockEntity jar)||!jar.alembic())break;
            if(pass==0 && !jar.contents().aspect().equals(tag))continue;
            if(jar.insert(tag,1))return true;
        }return false;
    }
    public static void tick(Level level,BlockPos pos,BlockState state,EssentiaSmelterBlockEntity smelter){
        if(!(level instanceof ServerLevel world))return;
        if(smelter.burn>0){smelter.burn--;smelter.setChanged();}
        int speed=state.is(ModBlocks.SMELTER_THAUMIUM.get())?10:15;
        if(world.getGameTime()%speed==0){
            var outlets=new ArrayList<BlockPos>();outlets.add(pos);var front=state.getValue(art.arcane.thaumcraft.block.EssentiaSmelterBlock.FACING);
            for(var side:Direction.Plane.HORIZONTAL){if(side==front)continue;var outlet=pos.relative(side);var accessory=world.getBlockState(outlet);if(accessory.is(ModBlocks.SMELTER_AUX.get())&&accessory.getValue(art.arcane.thaumcraft.block.SmelterAuxBlock.FACING)==side.getOpposite())outlets.add(outlet);}
            for(var outlet:outlets)for(String tag:List.copyOf(smelter.aspects.keySet()))if(smelter.output(world,outlet,tag)){
                int n=smelter.aspects.get(tag)-1;if(n==0)smelter.aspects.remove(tag);else smelter.aspects.put(tag,n);smelter.setChanged();break;
            }
        }
        var tags=tags(smelter.input);int size=tags.values().stream().mapToInt(Integer::intValue).sum();
        // Reset only during a committed world tick, so transfer rollback cannot erase progress.
        if(smelter.cook>0 && !ItemStack.isSameItemSameComponents(smelter.cookingInput,smelter.input))smelter.cook=0;
        boolean ready=!smelter.input.isEmpty() && size>0 && size<=256-smelter.total();
        if(ready && smelter.burn==0 && smelter.fuelTime(smelter.fuel)>0){
            smelter.burn=smelter.fuelTime(smelter.fuel);smelter.burnDuration=smelter.burn;var remainder=smelter.fuel.getCraftingRemainder();smelter.fuel.shrink(1);
            if(smelter.fuel.isEmpty() && remainder!=null)smelter.fuel=remainder.create();smelter.setChanged();
        }
        if(ready && smelter.burn>0){
            if(smelter.cook==0)smelter.cookingInput=smelter.input.copyWithCount(1);
            if(++smelter.cook>=smelter.cookDuration()){
                float efficiency=state.is(ModBlocks.SMELTER_THAUMIUM.get())?0.9F:state.is(ModBlocks.SMELTER_VOID.get())?0.95F:0.8F;
                int flux=0;
                for(var entry:tags.entrySet())for(int n=0;n<entry.getValue();n++){
                    if(world.getRandom().nextFloat()>(entry.getKey().equals("vitium")?efficiency*0.66F:efficiency))flux++;
                    else smelter.aspects.merge(entry.getKey(),1,Integer::sum);
                }
                smelter.pollute(world,smelter.vent(world,pos,state,flux));smelter.input.shrink(1);smelter.cook=0;
            }smelter.setChanged();
        }else if(smelter.cook!=0){smelter.cook=0;smelter.setChanged();}
        if(state.getValue(art.arcane.thaumcraft.block.EssentiaSmelterBlock.LIT)!=(smelter.burn>0))world.setBlock(pos,state.setValue(art.arcane.thaumcraft.block.EssentiaSmelterBlock.LIT,smelter.burn>0),3);
    }
}
