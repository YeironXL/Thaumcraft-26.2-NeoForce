package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.block.CrucibleBlock;
import art.arcane.thaumcraft.crafting.AlchemyMixture;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModRecipes;
import art.arcane.thaumcraft.research.ResearchRules;
import art.arcane.thaumcraft.scan.ScanRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CrucibleBlockEntity extends BlockEntity {
    private AlchemyMixture mixture=new AlchemyMixture();
    private long lastSound=Long.MIN_VALUE;
    public CrucibleBlockEntity(BlockPos pos,BlockState state) { super(ModBlockEntities.CRUCIBLE.get(),pos,state); }
    public AlchemyMixture mixture() { return mixture; }
    /** TC6 TileCrucible IFluidHandler: accepts water only (up to 1000 mB), cannot be drained. */
    public final net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource> waterInput = new WaterInput();
    private final class WaterInput extends net.neoforged.neoforge.transfer.transaction.SnapshotJournal<Integer> implements net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource> {
        @Override public int size() { return 1; }
        @Override public net.neoforged.neoforge.transfer.fluid.FluidResource getResource(int i) { return mixture.water() > 0 ? net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER) : net.neoforged.neoforge.transfer.fluid.FluidResource.EMPTY; }
        @Override public long getAmountAsLong(int i) { return mixture.water(); }
        @Override public long getCapacityAsLong(int i, net.neoforged.neoforge.transfer.fluid.FluidResource r) { return 1000; }
        @Override public boolean isValid(int i, net.neoforged.neoforge.transfer.fluid.FluidResource r) { return r.getFluid() == net.minecraft.world.level.material.Fluids.WATER; }
        @Override public int insert(int i, net.neoforged.neoforge.transfer.fluid.FluidResource r, int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext tx) {
            if (i != 0 || r.isEmpty() || !isValid(i, r) || amount <= 0) return 0;
            int n = Math.min(amount, 1000 - mixture.water());
            if (n <= 0) return 0;
            updateSnapshots(tx); mixture.setWater(mixture.water() + n); return n;
        }
        @Override public int extract(int i, net.neoforged.neoforge.transfer.fluid.FluidResource r, int amount, net.neoforged.neoforge.transfer.transaction.TransactionContext tx) { return 0; }
        @Override protected Integer createSnapshot() { return mixture.water(); }
        @Override protected void revertToSnapshot(Integer w) { mixture.setWater(w); }
        @Override protected void onRootCommit(Integer old) { if (old != mixture.water()) changed(); }
    }
    @Override protected void saveAdditional(ValueOutput output) { super.saveAdditional(output); output.store("mixture",AlchemyMixture.CODEC.codec(),mixture); }
    @Override protected void loadAdditional(ValueInput input) { super.loadAdditional(input); mixture=input.read("mixture",AlchemyMixture.CODEC.codec()).orElseGet(AlchemyMixture::new);
        if(level!=null&&level.isClientSide())level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    public void changed() {
        setChanged();
        if (level instanceof ServerLevel world) {
            int water=(int)Math.ceil(mixture.water()/250D);
            if (getBlockState().getValue(CrucibleBlock.WATER) != water) world.setBlock(worldPosition,getBlockState().setValue(CrucibleBlock.WATER,water),3);
            else world.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
        }
    }
    /** TileCrucible.spillRemnants. */
    public void empty() {
        if(mixture.water()<=0&&mixture.total()<=0)return;
        pollute(mixture.empty()); changed();
        if(level instanceof ServerLevel world)world.blockEvent(worldPosition,getBlockState().getBlock(),2,5);
    }
    private void sound(String id) {
        if (level instanceof ServerLevel world && lastSound != world.getGameTime()) {
            lastSound=world.getGameTime();
            world.playSound(null,worldPosition,art.arcane.thaumcraft.registry.ModSounds.get(id),net.minecraft.sounds.SoundSource.BLOCKS,0.2F,1F);
        }
    }
    private void pollute(float amount) {
        if (amount > 0 && level instanceof ServerLevel world) {
            var chunk=world.getChunkAt(worldPosition); NativeAura.get(world,chunk).addFlux(amount); chunk.markUnsaved();
        }
    }
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state) { pollute(mixture.empty()); super.preRemoveSideEffects(pos,state); }
    public static void tick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,CrucibleBlockEntity crucible) {
        if(level.isClientSide()){
            // TileCrucible.drawEffects (every client tick while there is water).
            if(crucible.mixture.water()<=0)return;
            var visual=level.getRandom();
            if(crucible.mixture.boiling()){
                level.addParticle(art.arcane.thaumcraft.registry.ModParticles.CRUCIBLE_FROTH.get(),pos.getX()+0.2f+visual.nextFloat()*0.6f,pos.getY()+crucible.fluidHeight(),pos.getZ()+0.2f+visual.nextFloat()*0.6f,0,0,0);
                if(crucible.mixture.total()>500)for(int n=0;n<2;n++){
                    var overflow=art.arcane.thaumcraft.registry.ModParticles.CRUCIBLE_OVERFLOW.get();
                    level.addParticle(overflow,pos.getX(),pos.getY()+1,pos.getZ()+visual.nextFloat(),0,0,0);
                    level.addParticle(overflow,pos.getX()+1,pos.getY()+1,pos.getZ()+visual.nextFloat(),0,0,0);
                    level.addParticle(overflow,pos.getX()+visual.nextFloat(),pos.getY()+1,pos.getZ(),0,0,0);
                    level.addParticle(overflow,pos.getX()+visual.nextFloat(),pos.getY()+1,pos.getZ()+1,0,0,0);
                }
            }
            if(visual.nextInt(6)==0&&crucible.mixture.total()>0){
                var tags=java.util.List.copyOf(crucible.mixture.aspects().keySet());
                int color=0xFF000000|art.arcane.thaumcraft.aspect.AspectRegistry.color(tags.get(visual.nextInt(tags.size())));
                level.addParticle(net.minecraft.core.particles.ColorParticleOption.create(art.arcane.thaumcraft.registry.ModParticles.CRUCIBLE_ASPECT_BUBBLE.get(),color),
                    pos.getX()+(5+visual.nextInt(22))/32D+.015625,pos.getY()+0.05f+crucible.fluidHeight(),pos.getZ()+(5+visual.nextInt(22))/32D+.015625,0,0,0);
            }
            return;
        }
        if (!(level instanceof ServerLevel world)) return;
        // TC6 TileCrucible.update: lava, fire, nitor or magma heat a filled crucible; otherwise it cools.
        var below=world.getBlockState(pos.below());
        boolean heated=below.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)||below.getBlock() instanceof net.minecraft.world.level.block.BaseFireBlock
                ||below.getBlock() instanceof art.arcane.thaumcraft.block.NitorBlock||below.is(Blocks.MAGMA_BLOCK);
        int before=crucible.mixture.heat(); boolean wasBoiling=crucible.mixture.boiling(); crucible.mixture.heatTick(heated);
        long tags=crucible.mixture.total(); crucible.pollute(crucible.mixture.tickSpill(world.getRandom().nextInt()));
        if(tags != crucible.mixture.total()||wasBoiling!=crucible.mixture.boiling())crucible.changed();
        else if(before != crucible.mixture.heat())crucible.setChanged();
    }
    /** TileCrucible.getFluidHeight: water level plus the dissolved essentia. */
    public float fluidHeight() {
        float base=0.3f+0.5f*(mixture.water()/1000f);
        float out=base+mixture.total()/500.0f*(1.0f-base);
        if(out>1.0f)out=1.001f;
        if(out==1.0f)out=0.9999f;
        return out;
    }
    /** TileCrucible.attemptSmelt(EntityItem). */
    public void attemptSmelt(ItemEntity entity) {
        var actor=entity.getOwner() instanceof ServerPlayer player ? player : null;
        int left=attemptSmelt(entity.getItem().copy(),actor);
        if(left<=0)entity.discard(); else { var stack=entity.getItem().copy(); stack.setCount(left); entity.setItem(stack); }
    }
    /** TileCrucible.attemptSmelt(ItemStack, player): each item either crafts a known recipe or dissolves. Returns the count left. */
    public int attemptSmelt(ItemStack item,ServerPlayer player) {
        if(!(level instanceof ServerLevel world))return item.getCount();
        boolean bubble=false,craftDone=false;
        int stacksize=item.getCount();
        for(int a=0;a<item.getCount();a++){
            var single=item.copyWithCount(1);
            var recipe=player==null?null:findRecipe(world,player,single);
            if(recipe!=null&&mixture.water()>0){
                var out=recipe.assemble(new SingleRecipeInput(single));
                if(!mixture.pay(recipe.aspects()))break;
                net.neoforged.neoforge.event.EventHooks.firePlayerCraftingEvent(player,out,new net.minecraft.world.SimpleContainer(single));
                PlayerProgressManager.get(player).recordCraft(BuiltInRegistries.ITEM.getKey(out.getItem()).toString());
                ejectItem(out);
                craftDone=true;--stacksize;
            }else{
                Map<String,Integer> tags=new LinkedHashMap<>();
                art.arcane.thaumcraft.aspect.ObjectAspects.get(single).forEach((tag,count)->tags.put(tag.id(),count));
                if(tags.isEmpty()||!mixture.dissolve(tags))continue;
                bubble=true;--stacksize;
            }
        }
        if(bubble){
            world.playSound(null,worldPosition,art.arcane.thaumcraft.registry.ModSounds.get("bubble"),net.minecraft.sounds.SoundSource.BLOCKS,0.2f,1.0f+world.getRandom().nextFloat()*0.4f);
            changed();
            world.blockEvent(worldPosition,getBlockState().getBlock(),2,1);
        }
        if(craftDone){
            changed();
            world.blockEvent(worldPosition,getBlockState().getBlock(),99,0);
        }
        setChanged();
        return stacksize;
    }
    private art.arcane.thaumcraft.crafting.CrucibleRecipe findRecipe(ServerLevel world,ServerPlayer player,ItemStack single){
        var input=new SingleRecipeInput(single);
        return art.arcane.thaumcraft.crafting.CrucibleRecipe.select(
                world.getServer().getRecipeManager().recipeMap().byType(ModRecipes.CRUCIBLE_TYPE.get()).stream().map(holder->holder.value()).toList(),
                candidate->candidate.matches(input,world)&&ResearchRules.knows(PlayerProgressManager.get(player),candidate.research())&&mixture.canPay(candidate.aspects()));
    }
    /** TileCrucible.ejectItem: floating special items that rise out of the pot. */
    public void ejectItem(ItemStack items) {
        boolean first=true;
        do {
            var spitout=items.copy();
            if(spitout.getCount()>spitout.getMaxStackSize())spitout.setCount(spitout.getMaxStackSize());
            items.shrink(spitout.getCount());
            var entity=new art.arcane.thaumcraft.entity.SpecialItemEntity(level,worldPosition.getX()+0.5f,worldPosition.getY()+0.71f,worldPosition.getZ()+0.5f,spitout);
            var r=level.getRandom();
            entity.setDeltaMovement(first?0.0:(r.nextFloat()-r.nextFloat())*0.01f,0.07500000298023224,first?0.0:(r.nextFloat()-r.nextFloat())*0.01f);
            level.addFreshEntity(entity);
            first=false;
        } while(items.getCount()>0);
    }
    @Override public boolean triggerEvent(int id,int type){
        if(id==99){
            if(level!=null&&level.isClientSide()){
                art.arcane.thaumcraft.client.fx.ClientHooks.crucibleCraft(worldPosition);
            }
            return true;
        }
        if(id==1){
            if(level!=null&&level.isClientSide())art.arcane.thaumcraft.client.fx.ClientHooks.bamf(worldPosition.above());
            return true;
        }
        if(id==2){
            if(level!=null&&level.isClientSide())art.arcane.thaumcraft.client.fx.ClientHooks.crucibleBoilEvent(this,type);
            return true;
        }
        return super.triggerEvent(id,type);
    }
    /** Single-item attemptSmelt used by the block and tests; true when the item was consumed. */
    public boolean processOne(ItemStack stack,ServerPlayer player) {
        if (!(level instanceof ServerLevel) || stack.isEmpty() || !mixture.boiling()) return false;
        return attemptSmelt(stack.copyWithCount(1),player)==0;
    }
}
