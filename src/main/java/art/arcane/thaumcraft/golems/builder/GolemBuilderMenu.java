package art.arcane.thaumcraft.golems.builder;

import art.arcane.thaumcraft.golems.GolemPlacerItem;
import art.arcane.thaumcraft.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** TC6 ContainerGolemBuilder: the finished-golem output slot, cost / max cost progress. */
public final class GolemBuilderMenu extends AbstractContainerMenu {
    public final GolemBuilderBlockEntity builder;
    private final Container container;
    public static boolean redo;

    public GolemBuilderMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof GolemBuilderBlockEntity b ? b : null);
    }
    public GolemBuilderMenu(int id, Inventory inv, GolemBuilderBlockEntity te) {
        super(ModMenus.GOLEM_BUILDER.get(), id);
        builder = te;
        container = te != null ? te : new SimpleContainer(1);
        addSlot(new Slot(container, 0, 160, 104) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j) addSlot(new Slot(inv, j + i * 9 + 9, 24 + j * 18, 142 + i * 18));
        for (int i = 0; i < 9; ++i) addSlot(new Slot(inv, i, 24 + i * 18, 200));
        if (te != null) {
            addDataSlot(new DataSlot() { public int get() { return te.cost; } public void set(int v) { te.cost = v; } });
            addDataSlot(new DataSlot() { public int get() { return te.maxCost; } public void set(int v) { te.maxCost = v; } });
        }
    }
    @Override public boolean clickMenuButton(Player p, int button) { if (button == 99) redo = true; return false; }
    @Override public void setItem(int slot, int state, ItemStack s) { redo = true; super.setItem(slot, state, s); }
    @Override public boolean stillValid(Player p) { return container.stillValid(p); }
    @Override
    public ItemStack quickMoveStack(Player p, int slot) {
        ItemStack stack = ItemStack.EMPTY;
        Slot s = slots.get(slot);
        if (s != null && s.hasItem()) {
            ItemStack in = s.getItem();
            stack = in.copy();
            if (slot == 0) { if (!moveItemStackTo(in, 1, slots.size(), true)) return ItemStack.EMPTY; }
            else if (!(in.getItem() instanceof GolemPlacerItem) || !moveItemStackTo(in, 0, 1, false)) return ItemStack.EMPTY;
            if (in.isEmpty()) s.setByPlayer(ItemStack.EMPTY); else s.setChanged();
        }
        return stack;
    }
}
