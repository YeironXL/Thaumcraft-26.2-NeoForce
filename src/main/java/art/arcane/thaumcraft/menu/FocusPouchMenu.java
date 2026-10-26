package art.arcane.thaumcraft.menu;

import art.arcane.thaumcraft.item.FocusItem;
import art.arcane.thaumcraft.item.FocusPouchItem;
import art.arcane.thaumcraft.registry.ModMenus;
import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** TC6 ContainerFocusPouch: 18 focus-only slots; the pouch's own hotbar slot is locked while open. */
public final class FocusPouchMenu extends AbstractContainerMenu {
    private final SimpleContainer input = new SimpleContainer(18);
    private final Player player;
    private final int blockSlot;

    public FocusPouchMenu(int id, Inventory inv) {
        super(ModMenus.FOCUS_POUCH.get(), id);
        this.player = inv.player;
        this.blockSlot = inv.getSelectedSlot() + 45;
        for (int a = 0; a < 18; ++a)
            addSlot(new Slot(input, a, 37 + a % 6 * 18, 51 + a / 6 * 18) {
                @Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof FocusItem; }
            });
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j) addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 151 + i * 18));
        for (int i = 0; i < 9; ++i) addSlot(new Slot(inv, i, 8 + i * 18, 209));
        if (!player.level().isClientSide()) {
            ItemStack pouch = player.getMainHandItem();
            if (pouch.getItem() instanceof FocusPouchItem fp) {
                var list = fp.getInventory(pouch);
                for (int a = 0; a < list.size(); ++a) input.setItem(a, list.get(a));
            }
        }
    }

    public int lockedHotbarSlot() { return blockSlot - 45; }

    @Override
    public ItemStack quickMoveStack(Player p, int slot) {
        if (slot == blockSlot) return ItemStack.EMPTY;
        ItemStack stack = ItemStack.EMPTY;
        Slot so = slots.get(slot);
        if (so != null && so.hasItem()) {
            ItemStack in = so.getItem();
            stack = in.copy();
            if (slot < 18) { if (!moveItemStackTo(in, 18, slots.size(), true)) return ItemStack.EMPTY; }
            else if (!(in.getItem() instanceof FocusItem) || !moveItemStackTo(in, 0, 18, false)) return ItemStack.EMPTY;
            if (in.isEmpty()) so.setByPlayer(ItemStack.EMPTY); else so.setChanged();
        }
        return stack;
    }

    @Override public boolean stillValid(Player p) { return p.getMainHandItem().getItem() instanceof FocusPouchItem; }

    @Override
    public void clicked(int slotId, int button, ContainerInput type, Player p) {
        if (slotId == blockSlot) return;
        if (type == ContainerInput.SWAP && button == blockSlot - 45) return;
        super.clicked(slotId, button, type, p);
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (p.level().isClientSide()) return;
        NonNullList<ItemStack> list = NonNullList.withSize(18, ItemStack.EMPTY);
        for (int a = 0; a < 18; ++a) list.set(a, input.getItem(a));
        ItemStack pouch = p.getInventory().getNonEquipmentItems().get(blockSlot - 45);
        if (pouch.getItem() instanceof FocusPouchItem fp) { fp.setInventory(pouch, list); p.getInventory().setChanged(); }
        else for (ItemStack s : list) if (!s.isEmpty()) p.drop(s, false);
    }
}
