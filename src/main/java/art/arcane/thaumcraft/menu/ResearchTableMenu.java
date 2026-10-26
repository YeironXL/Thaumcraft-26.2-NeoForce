package art.arcane.thaumcraft.menu;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.blockentity.ResearchTableBlockEntity;
import art.arcane.thaumcraft.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;

public final class ResearchTableMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    public ResearchTableMenu(int id,Inventory inventory){this(id,inventory,new SimpleContainer(2),new SimpleContainerData(33));}
    public ResearchTableMenu(int id,Inventory inventory,Container container,ContainerData data){
        super(ModMenus.RESEARCH_TABLE.get(),id);checkContainerSize(container,2);checkContainerDataCount(data,33);this.container=container;this.data=data;
        addSlot(new Slot(container,0,16,15){public boolean mayPlace(ItemStack stack){return stack.is(ModItems.SCRIBING_TOOLS.get());}public int getMaxStackSize(){return 1;}});
        addSlot(new Slot(container,1,224,16){public boolean mayPlace(ItemStack stack){return stack.is(Items.PAPER);}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,77+col*18,190+row*18));
        for(int row=0;row<3;row++)for(int col=0;col<3;col++)addSlot(new Slot(inventory,col+row*3,20+col*18,190+row*18));
        addDataSlots(data);
    }
    public int value(int index){return data.get(index);}
    public int button(int action){return data.get(0)*16+action;}
    @Override public boolean clickMenuButton(Player player,int encoded){
        if(!(player instanceof ServerPlayer server)||!stillValid(player)||!(container instanceof ResearchTableBlockEntity table))return false;
        boolean changed=table.act(server,encoded);
        if(changed){broadcastChanges();if(server.connection!=null)net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(server,art.arcane.thaumcraft.network.ResearchSnapshotPayload.of(server,false));}
        if(!changed&&server.connection!=null&&encoded%16>=4&&encoded%16<=6)server.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("screen.thaumcraft.requirements_unmet"));
        return changed;
    }
    @Override public boolean stillValid(Player player){return container.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index<2){if(!moveItemStackTo(stack,2,38,true))return ItemStack.EMPTY;}
        else if(slots.get(0).mayPlace(stack)){if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}
        else if(slots.get(1).mayPlace(stack)){if(!moveItemStackTo(stack,1,2,false))return ItemStack.EMPTY;}
        else if(index<29){if(!moveItemStackTo(stack,29,38,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,2,29,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        if(original.getCount()==stack.getCount())return ItemStack.EMPTY;slot.onTake(player,stack);return original;
    }
}
