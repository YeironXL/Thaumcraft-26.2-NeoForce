package art.arcane.thaumcraft.menu;

import art.arcane.thaumcraft.blockentity.VoidSiphonBlockEntity;
import art.arcane.thaumcraft.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** TC6 ContainerVoidSiphon: the output slot (take only) at 80,32 and the player inventory. */
public final class VoidSiphonMenu extends AbstractContainerMenu {
    private final Container container;

    public VoidSiphonMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof VoidSiphonBlockEntity t ? t : null);
    }
    public VoidSiphonMenu(int id, Inventory inv, VoidSiphonBlockEntity te) {
        super(ModMenus.VOID_SIPHON.get(), id);
        this.container = te != null ? te : new SimpleContainer(1);
        addSlot(new Slot(container, 0, 80, 32) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j) addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; ++i) addSlot(new Slot(inv, i, 8 + i * 18, 142));
    }
    @Override public boolean stillValid(Player p) { return container.stillValid(p); }
    @Override public ItemStack quickMoveStack(Player p, int slot) {
        Slot s = slots.get(slot);
        if (slot != 0 || s == null || !s.hasItem()) return ItemStack.EMPTY;
        ItemStack in = s.getItem(), out = in.copy();
        if (!moveItemStackTo(in, 1, slots.size(), true)) return ItemStack.EMPTY;
        if (in.isEmpty()) s.setByPlayer(ItemStack.EMPTY); else s.setChanged();
        return out;
    }
}
