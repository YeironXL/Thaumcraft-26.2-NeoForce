package art.arcane.thaumcraft.menu;

import art.arcane.thaumcraft.blockentity.EssentiaSmelterBlockEntity;
import art.arcane.thaumcraft.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public final class EssentiaSmelterMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;
    public EssentiaSmelterMenu(int id,Inventory inventory){this(id,inventory,new SimpleContainer(2),new SimpleContainerData(6));}
    public EssentiaSmelterMenu(int id,Inventory inventory,Container container,ContainerData data){
        super(ModMenus.ESSENTIA_SMELTER.get(),id);
        checkContainerSize(container,2);checkContainerDataCount(data,6);
        this.container=container;this.data=data;
        addSlot(new Slot(container,0,80,8){
            @Override public boolean mayPlace(ItemStack stack){return !stack.isEmpty() && !EssentiaSmelterBlockEntity.tags(stack).isEmpty();}
        });
        addSlot(new Slot(container,1,80,48){
            @Override public boolean mayPlace(ItemStack stack){return !stack.isEmpty() && inventory.player!=null && stack.getBurnTime(RecipeType.SMELTING,inventory.player.level().fuelValues())>0;}
        });
        addStandardInventorySlots(inventory,8,84);addDataSlots(data);
    }
    public int progress(){return data.get(1)>0?Math.min(1000,data.get(0)*1000/data.get(1)):0;}
    public int fuelFraction(){return data.get(2);}
    public int fuelSeconds(){return data.get(3);}
    public int stored(){return data.get(4);}
    public int efficiency(){return data.get(5);}
    @Override public boolean stillValid(Player player){return container.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index<2){if(!moveItemStackTo(stack,2,38,true))return ItemStack.EMPTY;}
        else if(slots.get(1).mayPlace(stack)){
            if(!moveItemStackTo(stack,1,2,false) && !moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        }else if(slots.get(0).mayPlace(stack)){
            if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        }else if(index<29){if(!moveItemStackTo(stack,29,38,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,2,29,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        if(stack.getCount()==original.getCount())return ItemStack.EMPTY;
        slot.onTake(player,stack);return original;
    }
}
