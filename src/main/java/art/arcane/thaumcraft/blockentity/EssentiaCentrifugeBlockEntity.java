package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.essentia.EssentiaPort;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** TC6 centrifugation recovers one randomly selected component, not both components. */
public final class EssentiaCentrifugeBlockEntity extends BlockEntity implements EssentiaPort {
    private String input="",output="";
    private int remaining;
    public EssentiaCentrifugeBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntities.ESSENTIA_CENTRIFUGE.get(),pos,state);}
    public String input(){return input;}
    public int remaining(){return remaining;}
    public boolean paused(){return level!=null && level.hasNeighborSignal(worldPosition);}
    public boolean connects(Direction side){return side==Direction.UP || side==Direction.DOWN;}
    public int suction(){return suction(Direction.DOWN);}
    public int suction(Direction side){return side==Direction.DOWN && !paused()?(input.isEmpty()?128:64):0;}
    public String wanted(){return "";}
    public String aspect(){return output;}
    public String offered(String requested,Direction side){return side==Direction.UP?output:"";}
    public boolean extractOne(String aspect,Direction side){
        if(side!=Direction.UP || output.isEmpty() || !output.equals(aspect))return false;
        output="";syncOut();return true;
    }
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putString("input",input);out.putString("output",output);out.putInt("remaining",remaining);}
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);input=in.getStringOr("input","");output=in.getStringOr("output","");remaining=Math.clamp(in.getIntOr("remaining",39),0,39);
        var aspect=AspectRegistry.get(input);if(aspect==null || aspect.isPrimal()){input="";remaining=0;}
        if(AspectRegistry.get(output)==null)output="";
    }
    /** TC6 world.markAndNotifyBlock on output changes: clients see aspectOut (goggles tags). */
    private void syncOut(){setChanged();if(level!=null && !level.isClientSide())level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider r){return saveWithoutMetadata(r);}
    public static void tick(Level world,BlockPos pos,BlockState state,EssentiaCentrifugeBlockEntity machine){
        if(world.isClientSide() || machine.paused())return;
        if(machine.input.isEmpty() && machine.output.isEmpty() && world.getGameTime()%5==0){
            var source=EssentiaTubeBlockEntity.neighbor(world,pos,Direction.DOWN);
            if(source!=null && source.suction(Direction.UP)<machine.suction(Direction.DOWN) && source.minimumSuction()<=machine.suction(Direction.DOWN)){
                String tag=source.offered("",Direction.UP);var aspect=AspectRegistry.get(tag);
                if(aspect!=null && !aspect.isPrimal() && source.extractOne(tag,Direction.UP)){machine.input=tag;machine.remaining=39;machine.setChanged();}
            }
        }
        if(!machine.input.isEmpty()){
            if(machine.remaining>0){machine.remaining--;machine.setChanged();}
            if(world.getGameTime()%20==0)world.playSound(null,pos,art.arcane.thaumcraft.registry.ModSounds.get("pump"),net.minecraft.sounds.SoundSource.BLOCKS,0.3F,1F);
            if(machine.remaining==0 && machine.output.isEmpty()){
                var aspect=AspectRegistry.get(machine.input);
                machine.output=(world.getRandom().nextBoolean()?aspect.parentA():aspect.parentB()).id();
                machine.input="";machine.syncOut();
            }
        }
    }
}
