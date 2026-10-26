package art.arcane.thaumcraft.menu;

import art.arcane.thaumcraft.entity.monster.PechEntity;
import art.arcane.thaumcraft.entity.monster.PechTrades;
import art.arcane.thaumcraft.registry.ModMenus;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * TC6 ContainerPech + InventoryPech: one offer slot (36,29) and a 2x2 output grid. Clicking the dice button (menu button 0)
 * rolls the pech's goods for the offered valued item; leftovers are dropped (tagged "PechDrop") when the GUI closes.
 */
public final class PechMenu extends AbstractContainerMenu {
    public static final String PECH_DROP = "PechDrop";
    public final PechEntity pech;
    private final Player player;
    public final SimpleContainer inventory = new SimpleContainer(5) {
        @Override public boolean canPlaceItem(int index, ItemStack stack) { return index == 0; }
        @Override public boolean stillValid(Player p) { return pech != null && pech.isTamed(); }
    };

    public PechMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, inv.player.level().getEntity(buf.readVarInt()) instanceof PechEntity p ? p : null);
    }

    public PechMenu(int id, Inventory inv, PechEntity pech) {
        super(ModMenus.PECH.get(), id);
        this.pech = pech;
        this.player = inv.player;
        if (pech != null) pech.trading = true;
        addSlot(new Slot(inventory, 0, 36, 29));
        for (int i = 0; i < 2; ++i) for (int j = 0; j < 2; ++j)
            addSlot(new Slot(inventory, 1 + j + i * 2, 106 + 18 * j, 20 + 18 * i) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j) addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; ++i) addSlot(new Slot(inv, i, 8 + i * 18, 142));
    }

    /** The dice button is active while a valued item waits in the offer slot and the output grid is empty. */
    public boolean canRoll() {
        if (pech == null || inventory.getItem(0).isEmpty() || !pech.isValued(inventory.getItem(0))) return false;
        for (int a = 1; a < 5; ++a) if (!inventory.getItem(a).isEmpty()) return false;
        return true;
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (id == 0) { generateContents(); return true; }
        return super.clickMenuButton(p, id);
    }

    private boolean hasStuffInPack() {
        for (ItemStack s : pech.loot) if (!s.isEmpty()) return true;
        return false;
    }

    /** ContainerPech.generateContents. */
    public void generateContents() {
        if (player.level().isClientSide() || !canRoll()) return;
        var rand = player.level().getRandom();
        int value = pech.getValue(inventory.getItem(0));
        if (rand.nextInt(100) <= value / 2) {
            pech.setTamed(false);
            pech.playSound(ModSounds.get("pech_trade"), 0.4f, 1.0f);
        }
        if (rand.nextInt(5) == 0) value += rand.nextInt(3);
        else if (rand.nextBoolean()) value -= rand.nextInt(3);
        List<PechTrades.Trade> pos = PechTrades.forType(pech.getPechType());
        while (value > 0) {
            int am = Math.min(5, Math.max((value + 1) / 2, rand.nextInt(value) + 1));
            value -= am;
            if (am == 1 && rand.nextBoolean() && hasStuffInPack()) {
                List<Integer> loot = new ArrayList<>();
                for (int a = 0; a < pech.loot.size(); ++a) if (!pech.loot.get(a).isEmpty()) loot.add(a);
                int r = loot.get(rand.nextInt(loot.size()));
                ItemStack is = pech.loot.get(r).copy();
                is.setCount(1);
                addStack(is);
                pech.loot.get(r).shrink(1);
                if (pech.loot.get(r).isEmpty()) pech.loot.set(r, ItemStack.EMPTY);
            } else {
                if (am >= 4 && rand.nextBoolean()) continue;
                PechTrades.Trade it;
                do { it = pos.get(rand.nextInt(pos.size())); } while (it.value() != am);
                ItemStack is2 = it.stack(player.level().registryAccess());
                is2.onCraftedBy(player, is2.getCount());
                addStack(is2);
            }
        }
        inventory.removeItem(0, 1);
        broadcastChanges();
    }

    private void addStack(ItemStack s) {
        for (int a = 1; a < 5; ++a) {
            ItemStack in = inventory.getItem(a);
            if (in.isEmpty()) { inventory.setItem(a, s); break; }
            if (ItemStack.isSameItemSameComponents(in, s) && in.getCount() + s.getCount() < in.getMaxStackSize()) in.grow(s.getCount());
        }
    }

    @Override public boolean stillValid(Player p) { return pech != null && pech.isAlive() && pech.isTamed(); }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack in = slot.getItem();
            itemstack = in.copy();
            if (index < 5) { if (!moveItemStackTo(in, 5, 41, true)) return ItemStack.EMPTY; }
            else if (!moveItemStackTo(in, 0, 1, true)) return ItemStack.EMPTY;
            if (in.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
            if (in.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(p, in);
        }
        return itemstack;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (pech != null) pech.trading = false;
        if (p.level().isClientSide()) return;
        for (int a = 0; a < 5; ++a) {
            ItemStack s = inventory.removeItemNoUpdate(a);
            if (s.isEmpty()) continue;
            ItemEntity ei = p.drop(s, false);
            if (ei != null) ei.getPersistentData().putString("thaumcraft:thrower", PECH_DROP);
        }
    }
}
