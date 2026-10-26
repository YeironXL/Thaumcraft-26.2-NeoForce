package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.block.EssentiaTubeBlock;
import art.arcane.thaumcraft.essentia.EssentiaPort;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import com.mojang.serialization.Codec;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import java.util.*;

public final class EssentiaTubeBlockEntity extends BlockEntity implements EssentiaPort {
    public enum Kind { NORMAL, VALVE, FILTER, RESTRICT, ONEWAY, BUFFER }
    private static final Codec<Map<String,Integer>> CONTENTS=Codec.unboundedMap(Codec.STRING,Codec.intRange(1,10));
    private final Map<String,Integer> contents=new LinkedHashMap<>();
    private String wanted="",filter="";
    private int suction,venting,closedMask,chokes;
    private boolean open=true,wasPowered;
    public EssentiaTubeBlockEntity(BlockPos p,BlockState s){super(ModBlockEntities.ESSENTIA_TUBE.get(),p,s);}
    public Kind kind(){var s=getBlockState();return s.is(ModBlocks.TUBE_VALVE.get())?Kind.VALVE:s.is(ModBlocks.TUBE_FILTER.get())?Kind.FILTER:s.is(ModBlocks.TUBE_RESTRICT.get())?Kind.RESTRICT:s.is(ModBlocks.TUBE_ONEWAY.get())?Kind.ONEWAY:s.is(ModBlocks.TUBE_BUFFER.get())?Kind.BUFFER:Kind.NORMAL;}
    public Direction facing(){return getBlockState().getValue(EssentiaTubeBlock.FACING);}
    public int amount(){return contents.values().stream().mapToInt(Integer::intValue).sum();}
    public Map<String,Integer> contents(){return Map.copyOf(contents);}
    public String filter(){return filter;}
    public boolean open(){return open;}
    public boolean connects(Direction side){return side!=null && (closedMask&(1<<side.ordinal()))==0 && (kind()!=Kind.VALVE || side!=facing());}
    public int choke(Direction side){return (chokes>>(side.ordinal()*2))&3;}
    public void cycleChoke(Direction side){int shift=side.ordinal()*2;chokes=(chokes&~(3<<shift))|(((choke(side)+1)%3)<<shift);setChanged();}
    public void toggleSide(Direction side){
        boolean close=(closedMask&(1<<side.ordinal()))==0;
        setSide(side,close);
        if(level!=null && level.hasChunkAt(worldPosition.relative(side)) && level.getBlockEntity(worldPosition.relative(side)) instanceof EssentiaTubeBlockEntity other)other.setSide(side.getOpposite(),close);
    }
    private void setSide(Direction side,boolean close){if(close)closedMask|=1<<side.ordinal();else closedMask&=~(1<<side.ordinal());suction=0;setChanged();}
    private void sound(String id){if(level instanceof net.minecraft.server.level.ServerLevel world)world.playSound(null,worldPosition,art.arcane.thaumcraft.registry.ModSounds.get(id),net.minecraft.sounds.SoundSource.BLOCKS,0.5F,1F);}
    public void toggleValve(){open=!open;suction=0;setChanged();sound("squeek");}
    public boolean setFilter(String tag){if(kind()!=Kind.FILTER || !tag.isEmpty() && AspectRegistry.get(tag)==null)return false;filter=tag;suction=0;setChanged();if(level!=null && !level.isClientSide())level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);sound("tool");return true;}
    private int bellows(){
        if(level==null)return 0;int count=0;
        for(var side:Direction.values()){var pos=worldPosition.relative(side);if(!level.hasChunkAt(pos))continue;var s=level.getBlockState(pos);
            if(s.is(ModBlocks.BELLOWS.get()) && s.hasProperty(art.arcane.thaumcraft.block.EssentiaBellowsBlock.FACING)
                && s.getValue(art.arcane.thaumcraft.block.EssentiaBellowsBlock.FACING)==side.getOpposite() && !level.hasNeighborSignal(pos))count++;
        }return count;
    }
    /** TileTubeBuffer add/takeFromContainer syncTile: the buffer's aspects reach clients (goggles tags). */
    private void bufferSync(){if(kind()==Kind.BUFFER && level!=null && !level.isClientSide())level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public int suction(){return suction(Direction.UP);}
    public int suction(Direction side){
        if(!connects(side) || venting>0 || kind()==Kind.VALVE && !open)return 0;
        if(kind()==Kind.BUFFER){int choke=choke(side);return choke==2?0:choke==1?1:Math.max(1,bellows()*32);}
        return suction;
    }
    public String wanted(){return kind()==Kind.BUFFER?"":wanted;}
    public String aspect(){if(contents.isEmpty())return "";if(kind()==Kind.BUFFER && level!=null){var tags=new java.util.ArrayList<>(contents.keySet());return tags.get(level.getRandom().nextInt(tags.size()));}return contents.keySet().iterator().next();}
    public String offered(String requested,Direction side){return !requested.isEmpty() && contents.containsKey(requested)?requested:aspect();}
    public boolean extractOne(String tag,Direction side){
        if(tag.isEmpty() || !contents.containsKey(tag) || !connects(side) || venting>0 || kind()==Kind.VALVE && !open || kind()==Kind.ONEWAY && side!=facing())return false;
        if(kind()==Kind.BUFFER && level!=null){
            var receiver=neighbor(level,worldPosition,side);int pull=receiver==null?0:receiver.suction(side.getOpposite());
            for(var other:Direction.values())if(other!=side && connects(other)){
                var port=neighbor(level,worldPosition,other);
                if(port!=null && (port.wanted().isEmpty()||port.wanted().equals(tag)) && port.suction(other.getOpposite())>pull && port.suction(other.getOpposite())>suction(other))return false;
            }
        }
        int left=contents.get(tag)-1;if(left==0)contents.remove(tag);else contents.put(tag,left);setChanged();bufferSync();return true;
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.store("contents",CONTENTS,contents);out.putString("filter",filter);out.putInt("closed",closedMask);out.putInt("chokes",chokes);out.putBoolean("open",open);out.putBoolean("powered",wasPowered);out.putInt("venting",venting);}
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);contents.clear();int capacity=kind()==Kind.BUFFER?10:1;
        in.read("contents",CONTENTS).orElse(Map.of()).forEach((tag,n)->{if(AspectRegistry.get(tag)!=null && amount()+n<=capacity)contents.put(tag,n);});
        String legacy=in.getStringOr("stored","");if(contents.isEmpty() && AspectRegistry.get(legacy)!=null)contents.put(legacy,1);
        filter=in.getStringOr("filter","");if(AspectRegistry.get(filter)==null)filter="";
        closedMask=in.getIntOr("closed",0)&63;chokes=in.getIntOr("chokes",0)&4095;open=in.getBooleanOr("open",true);wasPowered=in.getBooleanOr("powered",false);venting=Math.clamp(in.getIntOr("venting",0),0,40);
    }
    public static EssentiaPort neighbor(Level world,BlockPos pos,Direction side){var next=pos.relative(side);if(!world.hasChunkAt(next))return null;if(world.getBlockEntity(next) instanceof EssentiaPort port)return port.connects(side.getOpposite())?port:null;var cap=world.getCapability(art.arcane.thaumcraft.registry.ModCapabilities.ESSENTIA,next,side.getOpposite());return cap!=null&&cap.connects(side.getOpposite())?cap:null;}
    public static void tick(Level world,BlockPos pos,BlockState state,EssentiaTubeBlockEntity tube){
        if(world.isClientSide())return;
        Kind kind=tube.kind();
        if(kind==Kind.VALVE && world.getGameTime()%5==0){boolean powered=world.hasNeighborSignal(pos);if(powered!=tube.wasPowered){tube.open=!powered;tube.wasPowered=powered;tube.setChanged();tube.sound("squeek");}}
        if(world.getGameTime()%2==0){
            var visual=state.setValue(EssentiaTubeBlock.ENABLED,kind!=Kind.VALVE||tube.open);
            for(var side:Direction.values())visual=visual.setValue(EssentiaTubeBlock.SIDES[side.ordinal()],tube.connects(side)&&neighbor(world,pos,side)!=null);
            if(visual!=state)world.setBlock(pos,visual,3);
        }
        if(tube.venting>0){tube.venting--;tube.setChanged();if(world.getGameTime()%5==0 && world instanceof net.minecraft.server.level.ServerLevel server)server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,2,0.15,0.15,0.15,0.01);return;}
        if(kind==Kind.VALVE && !tube.open){tube.suction=0;return;}
        if(kind!=Kind.BUFFER && world.getGameTime()%2==0){
            int strongest=0;String wanted="";
            for(var side:Direction.values()){
                if(!tube.connects(side) || kind==Kind.ONEWAY && side!=tube.facing())continue;
                var port=neighbor(world,pos,side);if(port==null)continue;
                if(kind==Kind.FILTER && !tube.filter.isEmpty() && !port.wanted().isEmpty() && !tube.filter.equals(port.wanted()))continue;
                if(tube.amount()>0 && !port.wanted().isEmpty() && !tube.aspect().equals(port.wanted()))continue;
                int pull=port.suction(side.getOpposite());int candidate=kind==Kind.RESTRICT?pull/2:pull-1;
                if(candidate>strongest){strongest=candidate;wanted=port.wanted().isEmpty()?tube.filter:port.wanted();}
            }
            tube.suction=strongest;tube.wanted=wanted;
            for(var side:Direction.values()){
                if(!tube.connects(side))continue;var port=neighbor(world,pos,side);
                if(port==null || port instanceof EssentiaTubeBlockEntity other && other.kind()==Kind.FILTER)continue;
                int pull=port.suction(side.getOpposite());
                if(strongest>0 && (pull==strongest||pull==strongest-1) && !wanted.equals(port.wanted())){tube.venting=40;tube.setChanged();return;}
            }
        }
        if(world.getGameTime()%5!=0 || tube.amount()>=(kind==Kind.BUFFER?10:1))return;
        for(var side:Direction.values()){
            if(!tube.connects(side) || kind==Kind.ONEWAY && side==tube.facing())continue;
            var port=neighbor(world,pos,side);int pull=tube.suction(side);
            if(port==null || pull==0 || port.suction(side.getOpposite())>=pull || pull<port.minimumSuction())continue;
            String tag=port.offered(tube.wanted(),side.getOpposite());
            if(tag.isEmpty() || !tube.wanted().isEmpty() && !tag.equals(tube.wanted()) || kind==Kind.FILTER && !tube.filter.isEmpty() && !tag.equals(tube.filter))continue;
            if(port.extractOne(tag,side.getOpposite())){tube.contents.merge(tag,1,Integer::sum);tube.setChanged();tube.bufferSync();return;}
        }
    }
}
