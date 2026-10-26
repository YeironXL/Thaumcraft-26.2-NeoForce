package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * TC6 SealBaseContainer: the seal settings screen. Categories are 0 priority/colour/lock, 1 filter, 2 area, 3 toggles,
 * 4 required/forbidden traits. Filter slots are ghost slots (always present, shown only on the filter page); buttons
 * use the TC6 enchantItem ids.
 */
public class SealMenu extends AbstractContainerMenu {
    public final ISealEntity seal;
    public final int[] categories;
    public int category = -1;
    private final int filterSlots;
    private final Player player;

    public static void open(ServerPlayer sp, ISealEntity se) {
        sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new SealMenu(id, inv, se), Component.translatable("item.thaumcraft.golem_bell")), buf -> {
            buf.writeBlockPos(se.getSealPos().pos);
            buf.writeEnum(se.getSealPos().face);
        });
        // make sure the client copy is current
        se.syncToClient(sp.level());
    }

    public SealMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) { this(id, inv, clientSeal(inv.player, buf.readBlockPos(), buf.readEnum(Direction.class))); }
    private static ISealEntity clientSeal(Player p, BlockPos pos, Direction face) {
        ISealEntity se = SealHandler.getSealEntity(p.level(), new SealPos(pos, face));
        return se != null ? se : new SealEntity(new SealPos(pos, face), new SealPickup());
    }

    public SealMenu(int id, Inventory inv, ISealEntity seal) {
        super(ModMenus.SEAL.get(), id);
        this.seal = seal;
        this.player = inv.player;
        categories = seal.getSeal() instanceof ISealGui g ? g.getGuiCategories() : new int[]{0};
        category = categories[0];
        int s = seal.getSeal() instanceof ISealConfigFilter f ? f.getFilterSize() : 0;
        filterSlots = s;
        if (s > 0) {
            int sx = 16 + (s - 1) % 3 * 12, sy = 16 + (s - 1) / 3 * 12;
            Container c = new FilterContainer(seal);
            for (int a = 0; a < s; a++) {
                int x = a % 3, y = a / 3;
                addSlot(new Slot(c, a, 88 + x * 24 - sx + 8, 72 + y * 24 - sy + 8) {
                    @Override public boolean isActive() { return category == 1; }
                    @Override public boolean mayPickup(Player p) { return false; }
                    @Override public boolean mayPlace(ItemStack st) { return false; }
                });
            }
        }
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j) addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 150 + i * 18));
        for (int i = 0; i < 9; ++i) addSlot(new Slot(inv, i, 8 + i * 18, 208));
        addDataSlot(new DataSlot() { public int get() { return seal.getPriority(); } public void set(int v) { seal.setPriority((byte) v); } });
        addDataSlot(new DataSlot() { public int get() { return seal.getArea().getX(); } public void set(int v) { seal.setArea(new BlockPos(v, seal.getArea().getY(), seal.getArea().getZ())); } });
        addDataSlot(new DataSlot() { public int get() { return seal.getArea().getY(); } public void set(int v) { seal.setArea(new BlockPos(seal.getArea().getX(), v, seal.getArea().getZ())); } });
        addDataSlot(new DataSlot() { public int get() { return seal.getArea().getZ(); } public void set(int v) { seal.setArea(new BlockPos(seal.getArea().getX(), seal.getArea().getY(), v)); } });
        addDataSlot(new DataSlot() { public int get() { return seal.getColor(); } public void set(int v) { seal.setColor((byte) v); } });
    }

    private static final class FilterContainer implements Container {
        private final ISealEntity se;
        FilterContainer(ISealEntity se) { this.se = se; }
        private ISealConfigFilter f() { return (ISealConfigFilter) se.getSeal(); }
        @Override public int getContainerSize() { return f().getFilterSize(); }
        @Override public boolean isEmpty() { return f().getInv().stream().allMatch(ItemStack::isEmpty); }
        @Override public ItemStack getItem(int i) { return f().getFilterSlot(i); }
        @Override public ItemStack removeItem(int i, int n) { return ItemStack.EMPTY; }
        @Override public ItemStack removeItemNoUpdate(int i) { return ItemStack.EMPTY; }
        @Override public void setItem(int i, ItemStack s) { f().setFilterSlot(i, s); }
        @Override public void setChanged() {}
        @Override public boolean stillValid(Player p) { return true; }
        @Override public void clearContent() {}
        @Override public int getMaxStackSize() { return 1; }
    }

    private void changed() { if (seal instanceof SealEntity se && !player.level().isClientSide()) SealHandler.changed(player.level(), se); }

    /** SealBaseContainer.enchantItem; also run client-side by the screen for immediate feedback. */
    public boolean apply(int par2) {
        if (par2 >= 0 && par2 < categories.length) { category = categories[par2]; return true; }
        if (category == 3 && seal.getSeal() instanceof ISealConfigToggles cp && par2 >= 30 && par2 < 30 + cp.getToggles().length) { cp.setToggle(par2 - 30, true); return true; }
        if (category == 3 && seal.getSeal() instanceof ISealConfigToggles cp && par2 >= 60 && par2 < 60 + cp.getToggles().length) { cp.setToggle(par2 - 60, false); return true; }
        if (category == 0 && par2 >= 25 && par2 <= 26) { seal.setLocked(par2 == 25); return true; }
        if (par2 >= 27 && par2 <= 28) { seal.setRedstoneSensitive(par2 == 27); return true; }
        if (category == 1 && seal.getSeal() instanceof ISealConfigFilter cp && par2 >= 20 && par2 <= 21) { cp.setBlacklist(par2 == 20); return true; }
        if (par2 == 80 && seal.getPriority() > -5) { seal.setPriority((byte) (seal.getPriority() - 1)); return true; }
        if (par2 == 81 && seal.getPriority() < 5) { seal.setPriority((byte) (seal.getPriority() + 1)); return true; }
        if (par2 == 82 && seal.getColor() > 0) { seal.setColor((byte) (seal.getColor() - 1)); return true; }
        if (par2 == 83 && seal.getColor() < 16) { seal.setColor((byte) (seal.getColor() + 1)); return true; }
        if (seal.getSeal() instanceof ISealConfigArea) {
            BlockPos a = seal.getArea();
            if (par2 == 90 && a.getY() > 1) { seal.setArea(a.offset(0, -1, 0)); return true; }
            if (par2 == 91 && a.getY() < 8) { seal.setArea(a.offset(0, 1, 0)); return true; }
            if (par2 == 92 && a.getX() > 1) { seal.setArea(a.offset(-1, 0, 0)); return true; }
            if (par2 == 93 && a.getX() < 8) { seal.setArea(a.offset(1, 0, 0)); return true; }
            if (par2 == 94 && a.getZ() > 1) { seal.setArea(a.offset(0, 0, -1)); return true; }
            if (par2 == 95 && a.getZ() < 8) { seal.setArea(a.offset(0, 0, 1)); return true; }
        }
        return false;
    }
    @Override public boolean clickMenuButton(Player p, int id) {
        boolean r = apply(id);
        if (r) changed();
        return r;
    }

    /** SealBaseContainer.slotClick on ghost slots: set the filter item, adjust stack-size limits. */
    @Override
    public void clicked(int slotId, int clickedButton, ContainerInput mode, Player p) {
        if (slotId >= 0 && slotId < filterSlots && seal.getSeal() instanceof ISealConfigFilter f) {
            if (category != 1) return;
            Slot slot = slots.get(slotId);
            ItemStack ic = getCarried().isEmpty() ? ItemStack.EMPTY : getCarried().copy();
            boolean limit = f.hasStacksizeLimiters();
            int n = slot.getContainerSlot();
            if (clickedButton == 1) {
                if (!limit) { f.setFilterSlot(n, ItemStack.EMPTY); f.setFilterSlotSize(n, 0); }
                else if (ic.isEmpty()) {
                    if (slot.hasItem()) {
                        f.setFilterSlotSize(n, f.getFilterSlotSize(n) - (mode == ContainerInput.QUICK_MOVE ? 10 : 1));
                        if (f.getFilterSlotSize(n) < 0) { f.setFilterSlot(n, ItemStack.EMPTY); f.setFilterSlotSize(n, 0); }
                    }
                } else if (slot.hasItem() && ItemStack.isSameItemSameComponents(ic, slot.getItem())) {
                    f.setFilterSlotSize(n, f.getFilterSlotSize(n) - ic.getCount());
                    if (f.getFilterSlotSize(n) < 0) { f.setFilterSlot(n, ItemStack.EMPTY); f.setFilterSlotSize(n, 0); }
                }
            } else if (ic.isEmpty()) {
                if (limit && slot.hasItem()) f.setFilterSlotSize(n, f.getFilterSlotSize(n) + (mode == ContainerInput.QUICK_MOVE ? 10 : 1));
            } else {
                if (!limit) { ic.setCount(1); f.setFilterSlotSize(n, 0); }
                else {
                    int os = ic.getCount();
                    ic.setCount(1);
                    if (slot.hasItem() && ItemStack.isSameItemSameComponents(ic, slot.getItem())) f.setFilterSlotSize(n, f.getFilterSlotSize(n) + os);
                    else f.setFilterSlotSize(n, 0);
                }
                f.setFilterSlot(n, ic);
            }
            changed();
            return;
        }
        super.clicked(slotId, clickedButton, mode, p);
    }
    @Override public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player p) { return SealHandler.getSealEntity(p.level(), seal.getSealPos()) != null || p.level().isClientSide(); }
    public int filterSlots() { return filterSlots; }
}
