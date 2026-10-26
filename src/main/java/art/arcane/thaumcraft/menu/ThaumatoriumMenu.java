package art.arcane.thaumcraft.menu;

import art.arcane.thaumcraft.blockentity.ThaumatoriumBlockEntity;
import art.arcane.thaumcraft.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** TC6 ContainerThaumatorium: catalyst slot; refreshes the recipe choices for its player every sync. */
public final class ThaumatoriumMenu extends AbstractContainerMenu {
    public final ThaumatoriumBlockEntity te;
    private final Container container;
    private final Player player;
    public ThaumatoriumMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof ThaumatoriumBlockEntity t ? t : null);
    }
    public ThaumatoriumMenu(int id, Inventory inv, ThaumatoriumBlockEntity te) {
        super(ModMenus.THAUMATORIUM.get(), id);
        this.te = te; this.player = inv.player;
        this.container = te != null ? te : new SimpleContainer(1);
        addSlot(new Slot(container, 0, 55, 24));
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j) addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 135 + i * 18));
        for (int i = 0; i < 9; ++i) addSlot(new Slot(inv, i, 8 + i * 18, 193));
        if (te != null && player instanceof ServerPlayer sp) te.updateRecipes(sp);
    }
    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (te != null && player instanceof ServerPlayer sp) te.updateRecipes(sp);
    }
    @Override public boolean stillValid(Player p) { return container.stillValid(p); }
    @Override
    public ItemStack quickMoveStack(Player p, int idx) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = slots.get(idx);
        if (slot != null && slot.hasItem()) {
            ItemStack s2 = slot.getItem();
            itemstack = s2.copy();
            if (idx != 0) { if (!moveItemStackTo(s2, 0, 1, false)) return ItemStack.EMPTY; }
            else if (!moveItemStackTo(s2, 1, 37, false)) return ItemStack.EMPTY;
            if (s2.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
            if (s2.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(p, s2);
        }
        return itemstack;
    }
}
