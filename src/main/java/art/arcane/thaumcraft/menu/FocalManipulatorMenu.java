package art.arcane.thaumcraft.menu;

import art.arcane.thaumcraft.blockentity.FocalManipulatorBlockEntity;
import art.arcane.thaumcraft.item.FocusItem;
import art.arcane.thaumcraft.registry.ModMenus;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** TC6 ContainerFocalManipulator: focus slot, player inventory in a column on the left; button 0 starts crafting. */
public final class FocalManipulatorMenu extends AbstractContainerMenu {
    private final Container container;
    public final FocalManipulatorBlockEntity table;

    public FocalManipulatorMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof FocalManipulatorBlockEntity t ? t : null);
    }

    public FocalManipulatorMenu(int id, Inventory inv, FocalManipulatorBlockEntity table) {
        super(ModMenus.FOCAL_MANIPULATOR.get(), id);
        this.table = table;
        this.container = table != null ? table : new SimpleContainer(1);
        addSlot(new Slot(container, 0, 31, 191) {
            @Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof FocusItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j) addSlot(new Slot(inv, j + i * 9 + 9, i * 18 - 62, 64 + j * 18));
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 3; ++j) addSlot(new Slot(inv, i + j * 3, i * 18 - 62, j * 18 + 7));
    }

    @Override
    public boolean clickMenuButton(Player p, int button) {
        if (button == 0 && p instanceof ServerPlayer sp && table != null && !table.startCraft(sp))
            table.getLevel().playSound(null, table.getBlockPos(), ModSounds.get("craftfail"), SoundSource.BLOCKS, 0.33f, 1.0f);
        return false;
    }

    @Override public boolean stillValid(Player p) { return container.stillValid(p); }

    @Override
    public ItemStack quickMoveStack(Player p, int idx) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = slots.get(idx);
        if (slot != null && slot.hasItem()) {
            ItemStack s2 = slot.getItem();
            itemstack = s2.copy();
            if (idx != 0) {
                if (s2.getItem() instanceof FocusItem) { if (!moveItemStackTo(s2, 0, 1, false)) return ItemStack.EMPTY; }
                else if (idx < 28) { if (!moveItemStackTo(s2, 28, 37, false)) return ItemStack.EMPTY; }
                else if (idx < 37 && !moveItemStackTo(s2, 1, 28, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(s2, 1, 37, false)) return ItemStack.EMPTY;
            if (s2.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
            if (s2.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(p, s2);
        }
        return itemstack;
    }
}
