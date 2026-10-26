package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.essentia.EssentiaContents;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class EssentiaJarBlockEntity extends BlockEntity implements art.arcane.thaumcraft.essentia.EssentiaPort {
    private EssentiaContents contents = EssentiaContents.EMPTY;
    /** TC6 TileJarFillable.aspectFilter (label), blocked (brass brace) and facing (side the label is on). */
    private String filter = "";
    private boolean blocked;
    private net.minecraft.core.Direction facing = net.minecraft.core.Direction.NORTH;
    public String filter() { return filter; }
    public boolean blocked() { return blocked; }
    public net.minecraft.core.Direction facing() { return facing; }
    public void setFacing(net.minecraft.core.Direction d) { facing = d; sync(); }
    public void setBlocked(boolean b) { blocked = b; sync(); }
    public void setFilter(String f) { filter = f == null ? "" : f; sync(); }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) { super.preRemoveSideEffects(pos, state); if (blocked && level != null) net.minecraft.world.level.block.Block.popResource(level, pos, new net.minecraft.world.item.ItemStack(art.arcane.thaumcraft.ModItems.JAR_BRACE.get())); }
    private void sync() { setChanged(); if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    /** BlockJar.applyLabel: a label marks the jar with its aspect (or the jar's current one); fails if already labelled or nothing to mark. */
    public boolean applyLabel(String labelAspect) {
        if (!filter.isEmpty()) return false;
        if (contents.amount() == 0 && (labelAspect == null || labelAspect.isEmpty())) return false;
        String tag = contents.amount() == 0 ? labelAspect : contents.aspect();
        filter = tag;
        sync();
        return true;
    }
    public EssentiaJarBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.ESSENTIA_JAR.get(),pos,state); }
    public boolean alembic() { return getBlockState().is(art.arcane.thaumcraft.ModBlocks.ALEMBIC.get()); }
    public int capacity() { return alembic()?128:250; }
    public boolean connects(net.minecraft.core.Direction side) { return alembic()?side!=net.minecraft.core.Direction.DOWN:side==net.minecraft.core.Direction.UP; }
    public int suction() { if (alembic()) return 0; if (contents.amount()>=250 && !getBlockState().is(art.arcane.thaumcraft.ModBlocks.JAR_VOID.get())) return 0; return filter.isEmpty()?32:64; }
    public int minimumSuction(){return alembic()?0:filter.isEmpty()?32:64;}
    public String wanted() { return filter.isEmpty() ? contents.aspect() : filter; }
    public String aspect() { return contents.aspect(); }
    public boolean extractOne(String tag,net.minecraft.core.Direction side) { return connects(side) && contents.aspect().equals(tag) && extract(1); }
    @Override public void setChanged() {
        super.setChanged();
        if(level!=null && !level.isClientSide() && getBlockState().hasProperty(art.arcane.thaumcraft.block.EssentiaJarBlock.FILL)) {
            int fill=(int)Math.ceil(contents.amount()*4D/capacity());
            int color=Math.max(0,new java.util.ArrayList<>(art.arcane.thaumcraft.aspect.AspectRegistry.all().keySet()).indexOf(contents.aspect()));
            var state=getBlockState().setValue(art.arcane.thaumcraft.block.EssentiaJarBlock.FILL,fill).setValue(art.arcane.thaumcraft.block.EssentiaJarBlock.COLOR,color);
            if(state!=getBlockState())level.setBlock(worldPosition,state,3);
        }
    }
    @Override public void onLoad(){super.onLoad();setChanged();}
    public static void tick(net.minecraft.world.level.Level world,BlockPos pos,BlockState state,EssentiaJarBlockEntity jar) {
        if(world.isClientSide() || jar.alembic() || world.getGameTime()%5!=0 || jar.suction()==0)return;
        var port=EssentiaTubeBlockEntity.neighbor(world,pos,net.minecraft.core.Direction.UP);
        if(port==null || port.suction(net.minecraft.core.Direction.DOWN)>=jar.suction() || jar.suction()<port.minimumSuction() || port.aspect().isEmpty())return;
        String tag=port.offered(jar.wanted(),net.minecraft.core.Direction.DOWN);
        if(jar.contents.amount()>0 && !jar.contents.aspect().equals(tag))return;
        if(!jar.filter.isEmpty() && !jar.filter.equals(tag))return;
        if(port.extractOne(tag,net.minecraft.core.Direction.DOWN)) {
            if(jar.contents.amount()<250)jar.insert(tag,1);
            else if(world instanceof net.minecraft.server.level.ServerLevel server && world.getRandom().nextInt(250)==0){
                var chunk=server.getChunkAt(pos);art.arcane.thaumcraft.aura.NativeAura.get(server,chunk).addFlux(1);chunk.markUnsaved();
            }
        }
    }
    public EssentiaContents contents() { return contents; }
    public boolean insert(String aspect, int amount) {
        if (amount > capacity()-contents.amount() || !contents.accepts(aspect,amount) || (!filter.isEmpty() && !filter.equals(aspect))) return false;
        contents=contents.insert(aspect,amount); setChanged(); return true;
    }
    public boolean extract(int amount) {
        if (amount <= 0 || contents.amount() < amount) return false;
        contents=contents.extract(amount); setChanged(); return true;
    }
    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.store("essentia",EssentiaContents.CODEC,contents); out.putString("filter",filter); out.putBoolean("blocked",blocked); out.putInt("facing",facing.get3DDataValue()); }
    @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); contents=in.read("essentia",EssentiaContents.CODEC).orElse(EssentiaContents.EMPTY); filter=in.getStringOr("filter",""); blocked=in.getBooleanOr("blocked",false); facing=net.minecraft.core.Direction.from3DDataValue(in.getIntOr("facing",2)); }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider r) { return saveWithoutMetadata(r); }
    @Override protected void collectImplicitComponents(DataComponentMap.Builder components) { super.collectImplicitComponents(components); components.set(ModComponents.ESSENTIA.get(),contents); if(!filter.isEmpty())components.set(ModComponents.JAR_FILTER.get(),filter); }
    @Override protected void applyImplicitComponents(DataComponentGetter components) { super.applyImplicitComponents(components); contents=components.getOrDefault(ModComponents.ESSENTIA.get(),EssentiaContents.EMPTY); filter=components.getOrDefault(ModComponents.JAR_FILTER.get(),""); }
    @Override public void removeComponentsFromTag(ValueOutput out) { super.removeComponentsFromTag(out); out.discard("essentia"); out.discard("filter"); }
}
