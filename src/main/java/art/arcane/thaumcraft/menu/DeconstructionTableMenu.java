package art.arcane.thaumcraft.menu;

import art.arcane.thaumcraft.blockentity.DeconstructionTableBlockEntity;
import art.arcane.thaumcraft.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** One deconstruction slot plus the player inventory; data 0 = progress, 1 = duration. */
public final class DeconstructionTableMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerData data;

    public DeconstructionTableMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof DeconstructionTableBlockEntity t ? t : null, new SimpleContainerData(2));
    }
    public DeconstructionTableMenu(int id, Inventory inv, DeconstructionTableBlockEntity te, ContainerData data) {
        super(ModMenus.DECONSTRUCTION_TABLE.get(), id);
        this.container = te != null ? te : new SimpleContainer(1);
        this.data = data;
        addSlot(new Slot(container, 0, 65, 31) { @Override public boolean mayPlace(ItemStack s) { return DeconstructionTableBlockEntity.breakable(s); } });
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j) addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; ++i) addSlot(new Slot(inv, i, 8 + i * 18, 142));
        addDataSlots(data);
    }
    public float progress() { int max = data.get(1); return max <= 0 ? 0 : data.get(0) / (float) max; }
    @Override public boolean stillValid(Player p) { return container.stillValid(p); }
    @Override
    public ItemStack quickMoveStack(Player p, int slot) {
        ItemStack stack = ItemStack.EMPTY;
        Slot s = slots.get(slot);
        if (s != null && s.hasItem()) {
            ItemStack in = s.getItem();
            stack = in.copy();
            if (slot == 0) { if (!moveItemStackTo(in, 1, slots.size(), true)) return ItemStack.EMPTY; }
            else if (!DeconstructionTableBlockEntity.breakable(in) || !moveItemStackTo(in, 0, 1, false)) return ItemStack.EMPTY;
            if (in.isEmpty()) s.setByPlayer(ItemStack.EMPTY); else s.setChanged();
        }
        return stack;
    }
}
