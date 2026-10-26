package art.arcane.thaumcraft.menu;

import art.arcane.thaumcraft.item.HandMirrorItem;
import art.arcane.thaumcraft.registry.ModMenus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * TC6 ContainerHandMirror + InventoryHandMirror: one slot (80,24); anything put in it is pushed through to the mirror
 * the held hand mirror is bound to. The hand mirror itself cannot be moved while the GUI is open.
 */
public final class HandMirrorMenu extends AbstractContainerMenu {
    private final Player player;
    private final ItemStack mirror;
    public final int currentItem;
    public final SimpleContainer input = new SimpleContainer(1) {
        @Override public void setItem(int index, ItemStack stack) { super.setItem(index, stack); if (!stack.isEmpty()) slotsChanged(this); }
    };

    public HandMirrorMenu(int id, Inventory inv) {
        super(ModMenus.HAND_MIRROR.get(), id);
        player = inv.player;
        mirror = inv.getSelectedItem();
        currentItem = inv.getSelectedSlot();
        addSlot(new Slot(input, 0, 80, 24));
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j) addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; ++i) addSlot(new Slot(inv, i, 8 + i * 18, 142));
    }

    /** onCraftMatrixChanged: send whatever lands in the glass through to the bound mirror. */
    @Override
    public void slotsChanged(net.minecraft.world.Container c) {
        if (player.level() instanceof ServerLevel sl && !input.getItem(0).isEmpty()) {
            ItemStack is = input.getItem(0).copy();
            input.setItem(0, ItemStack.EMPTY);
            if (!HandMirrorItem.transport(mirror, is, player, sl)) input.setItem(0, is);
            broadcastChanges();
        }
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput type, Player p) {
        if (slotId >= 0 && slotId < slots.size() && getSlot(slotId).getItem().getItem() instanceof HandMirrorItem) return;
        if (type == ContainerInput.SWAP && button == currentItem) return;
        super.clicked(slotId, button, type, p);
    }

    @Override
    public ItemStack quickMoveStack(Player p, int slot) {
        ItemStack stack = ItemStack.EMPTY;
        Slot so = slots.get(slot);
        if (so != null && so.hasItem() && !(so.getItem().getItem() instanceof HandMirrorItem)) {
            ItemStack in = so.getItem();
            stack = in.copy();
            if (slot == 0) { if (!moveItemStackTo(in, 1, slots.size(), true)) return ItemStack.EMPTY; }
            else if (!moveItemStackTo(in, 0, 1, false)) return ItemStack.EMPTY;
            if (in.isEmpty()) so.setByPlayer(ItemStack.EMPTY); else so.setChanged();
        }
        return stack;
    }

    @Override public boolean stillValid(Player p) { return true; }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (!p.level().isClientSide()) {
            ItemStack s = input.removeItemNoUpdate(0);
            if (!s.isEmpty()) p.drop(s, false);
        }
    }
}
