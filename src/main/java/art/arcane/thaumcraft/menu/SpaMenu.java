package art.arcane.thaumcraft.menu;

import art.arcane.thaumcraft.blockentity.SpaBlockEntity;
import art.arcane.thaumcraft.item.BathSaltsItem;
import art.arcane.thaumcraft.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** TC6 ContainerSpa: one bath-salts slot; button 1 toggles mix mode. */
public final class SpaMenu extends AbstractContainerMenu {
    public final SpaBlockEntity spa;
    private final Container container;

    public SpaMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof SpaBlockEntity s ? s : null);
    }
    public SpaMenu(int id, Inventory inv, SpaBlockEntity spa) {
        super(ModMenus.SPA.get(), id);
        this.spa = spa;
        this.container = spa != null ? spa : new SimpleContainer(1);
        addSlot(new Slot(container, 0, 65, 31) { @Override public boolean mayPlace(ItemStack s) { return s.getItem() instanceof BathSaltsItem; } });
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j) addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; ++i) addSlot(new Slot(inv, i, 8 + i * 18, 142));
    }

    @Override public boolean clickMenuButton(Player p, int button) { if (button == 1 && spa != null) spa.toggleMix(); return false; }
    @Override public boolean stillValid(Player p) { return container.stillValid(p); }

    @Override
    public ItemStack quickMoveStack(Player p, int slot) {
        ItemStack stack = ItemStack.EMPTY;
        Slot s = slots.get(slot);
        if (s != null && s.hasItem()) {
            ItemStack in = s.getItem();
            stack = in.copy();
            if (slot == 0) { if (!moveItemStackTo(in, 1, slots.size(), true)) return ItemStack.EMPTY; }
            else if (!(in.getItem() instanceof BathSaltsItem) || !moveItemStackTo(in, 0, 1, false)) return ItemStack.EMPTY;
            if (in.isEmpty()) s.setByPlayer(ItemStack.EMPTY); else s.setChanged();
        }
        return stack;
    }
}
