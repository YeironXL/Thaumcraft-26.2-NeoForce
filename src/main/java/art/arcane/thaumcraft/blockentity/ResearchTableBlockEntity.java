package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.research.*;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.menu.ResearchTableMenu;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;

public final class ResearchTableBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items=NonNullList.withSize(2,ItemStack.EMPTY);
    private TheorySession session;
    private int revision;
    public ResearchTableBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntities.RESEARCH_TABLE.get(),pos,state);}
    @Override public int getContainerSize(){return 2;}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> value){items=value;}
    @Override protected Component getDefaultName(){return Component.translatable("block.thaumcraft.research_table");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){
        return new ResearchTableMenu(id,inventory,this,new ContainerData(){
            public int get(int index){return display(index,inventory.player instanceof ServerPlayer p?p:null);}
            public void set(int index,int value){}public int getCount(){return 33;}
        });
    }
    public TheorySession session(){return session;}
    public boolean bookshelf(){return (availableAids()&1)!=0;}
    public int availableAids(){
        if(level==null)return 0;int mask=0;
        for(var pos:BlockPos.betweenClosed(worldPosition.offset(-4,-1,-4),worldPosition.offset(4,1,4))){var state=level.getBlockState(pos);
            if(state.is(Blocks.BOOKSHELF))mask|=1;if(state.is(art.arcane.thaumcraft.ModBlocks.CRUCIBLE.get()))mask|=2;
            if(state.is(art.arcane.thaumcraft.ModBlocks.WAND_WORKBENCH.get()))mask|=4;
        }
        return mask;
    }
    public int display(int index,ServerPlayer player){
        if(index==0)return revision;
        if(index==5)return availableAids();
        if(session==null)return (index==2||index==3)&&player!=null?TheorySession.availableInspiration(PlayerProgressManager.get(player)):0;
        if(index>=26&&index<32){int card=(index-26)/2;if(card>=session.cards().size())return -1;
            var choice=session.cards().get(card);if(choice.kind()==21)return index%2==0?choice.amount():choice.second();if((choice.kind()==15||choice.kind()==17))return index%2==0?choice.second():-1;return TheorySession.ASPECTS.indexOf(index%2==0?choice.aspect():choice.aspectSecond());}
        if(index==32)return session.aidMask();
        if(index==1)return player!=null&&!session.ownedBy(player.getUUID().toString())?3:session.complete()?2:1;
        if(index==25)return session.blockedMask();
        if(index==2)return session.inspiration();if(index==3)return session.start();if(index==4)return session.bonus();
        if(index>=6&&index<13)return session.totals().getOrDefault(ResearchCatalog.CATEGORIES.get(index-6),0);
        if(index>=13&&index<25){int card=(index-13)/4,field=(index-13)%4;if(card>=session.cards().size())return field==0?-1:0;
            var c=session.cards().get(card);return switch(field){case 0->c.kind();case 1->c.category();case 2->c.amount();default->c.cost();};}
        return 0;
    }
    private boolean ink(){var tool=getItem(0);return tool.is(ModItems.SCRIBING_TOOLS.get())&&tool.getDamageValue()<tool.getMaxDamage();}
    private boolean consumeScripting(){
        var tool=getItem(0);var paper=getItem(1);
        if(!ink()||tool.getMaxDamage()-tool.getDamageValue()<2||!paper.is(Items.PAPER)||paper.isEmpty())return false;
        tool.setDamageValue(tool.getDamageValue()+1);paper.shrink(1);return true;
    }
    public boolean act(ServerPlayer player,int encoded){
        if(level==null||player.level()!=level||!stillValid(player)||encoded<0||encoded/16!=revision)return false;
        int action=encoded%16;boolean success=false;var state=PlayerProgressManager.get(player);
        if(session==null){
            if(action>7||(action&~availableAids())!=0||!ink()||!getItem(1).is(Items.PAPER))return false;
            session=TheorySession.start(player.getUUID().toString(),state,action);success=true;
        }else{
            if(!session.ownedBy(player.getUUID().toString()))return false;
            if(action==8){session=null;success=true;}
            else if(action==7&&session.complete()){
                session.rewards().forEach((category,raw)->state.addKnowledge("THEORY",category,raw));session=null;success=true;
            }else if((action==2||action==3)&&ink()&&getItem(1).is(Items.PAPER)&&session.draw(state,player.getRandom(),action==3,TheoryResources.player(player))){
                getItem(1).shrink(1);success=true;
            }else if(action>=4&&action<=6&&ink()&&session.choose(action-4,state,TheoryResources.player(player,this::consumeScripting))){
                getItem(0).setDamageValue(getItem(0).getDamageValue()+1);success=true;
            }
        }
        if(success){revision=(revision+1)%32768;setChanged();}return success;
    }
    @Override protected void saveAdditional(ValueOutput output){super.saveAdditional(output);ContainerHelper.saveAllItems(output,items);
        output.putInt("revision",revision);if(session!=null)output.store("theory",TheorySession.CODEC,session);}
    @Override protected void loadAdditional(ValueInput input){super.loadAdditional(input);items=NonNullList.withSize(2,ItemStack.EMPTY);ContainerHelper.loadAllItems(input,items);
        revision=Math.floorMod(input.getIntOr("revision",0),32768);session=input.read("theory",TheorySession.CODEC).orElse(null);}
}
