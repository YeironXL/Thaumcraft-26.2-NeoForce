package art.arcane.thaumcraft.menu;

import art.arcane.thaumcraft.blockentity.PotionSprayerBlockEntity;
import art.arcane.thaumcraft.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** TC6 ContainerPotionSprayer. */
public final class PotionSprayerMenu extends AbstractContainerMenu {
    public final PotionSprayerBlockEntity sprayer;
    private final Container container;
    public PotionSprayerMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof PotionSprayerBlockEntity s ? s : null);
    }
    public PotionSprayerMenu(int id, Inventory inv, PotionSprayerBlockEntity sprayer) {
        super(ModMenus.POTION_SPRAYER.get(), id);
        this.sprayer = sprayer;
        this.container = sprayer != null ? sprayer : new SimpleContainer(1);
        addSlot(new Slot(container, 0, 56, 64) {
            @Override public boolean mayPlace(ItemStack s) { return PotionSprayerBlockEntity.isValidPotion(s); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j) addSlot(new Slot(inv, j + i * 9 + 9, 16 + j * 18, 151 + i * 18));
        for (int i = 0; i < 9; ++i) addSlot(new Slot(inv, i, 16 + i * 18, 209));
    }
    @Override public boolean stillValid(Player p) { return container.stillValid(p); }
    @Override
    public ItemStack quickMoveStack(Player p, int slot) {
        ItemStack stack = ItemStack.EMPTY;
        Slot s = slots.get(slot);
        if (s != null && s.hasItem()) {
            ItemStack in = s.getItem();
            stack = in.copy();
            if (slot == 0) { if (!moveItemStackTo(in, 1, slots.size(), true)) return ItemStack.EMPTY; }
            else if (!PotionSprayerBlockEntity.isValidPotion(in) || !moveItemStackTo(in, 0, 1, false)) return ItemStack.EMPTY;
            if (in.isEmpty()) s.setByPlayer(ItemStack.EMPTY); else s.setChanged();
        }
        return stack;
    }
}
