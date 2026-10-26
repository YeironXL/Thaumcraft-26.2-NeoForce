package art.arcane.thaumcraft.golems.seals;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** TC6 SealFiltered: a seal with an item filter (black or white list, optional per-slot amounts). */
public abstract class SealFiltered implements ISeal, ISealGui, ISealConfigFilter {
    NonNullList<ItemStack> filter;
    NonNullList<Integer> filterSize;
    boolean blacklist = true;

    protected SealFiltered() {
        filter = NonNullList.withSize(getFilterSize(), ItemStack.EMPTY);
        filterSize = NonNullList.withSize(getFilterSize(), 0);
    }

    public static ListTag writeItems(NonNullList<ItemStack> items, HolderLookup.Provider reg) {
        ListTag list = new ListTag();
        var ops = reg.createSerializationContext(NbtOps.INSTANCE);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) continue;
            Tag t = ItemStack.CODEC.encodeStart(ops, items.get(i)).result().orElse(null);
            if (t instanceof CompoundTag ct) { ct.putByte("Slot", (byte) i); list.add(ct); }
        }
        return list;
    }
    public static void readItems(ListTag list, NonNullList<ItemStack> items, HolderLookup.Provider reg) {
        var ops = reg.createSerializationContext(NbtOps.INSTANCE);
        for (Tag t : list) {
            if (!(t instanceof CompoundTag ct)) continue;
            int slot = ct.getByteOr("Slot", (byte) 0) & 255;
            if (slot < items.size()) items.set(slot, ItemStack.CODEC.parse(ops, ct).result().orElse(ItemStack.EMPTY));
        }
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, HolderLookup.Provider reg) {
        filter = NonNullList.withSize(getFilterSize(), ItemStack.EMPTY);
        readItems(nbt.getListOrEmpty("Items"), filter, reg);
        for (ItemStack s : filter) if (s.getCount() > 1) s.setCount(1);
        blacklist = nbt.getBooleanOr("bl", true);
        filterSize = NonNullList.withSize(getFilterSize(), 0);
        for (Tag t : nbt.getListOrEmpty("Sizes")) {
            if (!(t instanceof CompoundTag c)) continue;
            int j = c.getByteOr("Slot", (byte) 0) & 255;
            if (j < filterSize.size()) filterSize.set(j, c.getIntOr("Size", 0));
        }
    }
    @Override
    public void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider reg) {
        nbt.put("Items", writeItems(filter, reg));
        nbt.putBoolean("bl", blacklist);
        ListTag sizes = new ListTag();
        for (int i = 0; i < filterSize.size(); i++) {
            int size = filterSize.get(i);
            if (size != 0) { CompoundTag c = new CompoundTag(); c.putByte("Slot", (byte) i); c.putInt("Size", size); sizes.add(c); }
        }
        nbt.put("Sizes", sizes);
    }
    @Override public int[] getGuiCategories() { return new int[]{0}; }
    @Override public int getFilterSize() { return 1; }
    @Override public NonNullList<ItemStack> getInv() { return filter; }
    @Override public NonNullList<Integer> getSizes() { return filterSize; }
    @Override public ItemStack getFilterSlot(int i) { return filter.get(i); }
    @Override public int getFilterSlotSize(int i) { return filterSize.get(i); }
    @Override public void setFilterSlot(int i, ItemStack stack) { filter.set(i, stack.copy()); }
    @Override public void setFilterSlotSize(int i, int size) { filterSize.set(i, size); }
    @Override public boolean isBlacklist() { return blacklist; }
    @Override public void setBlacklist(boolean b) { blacklist = b; }
    @Override public boolean hasStacksizeLimiters() { return false; }
    protected art.arcane.thaumcraft.golems.InvHelper.InvFilter invFilter(ISealConfigToggles.SealToggle[] props) {
        return new art.arcane.thaumcraft.golems.InvHelper.InvFilter(!props[0].value, !props[1].value, props[2].value, props[3].value);
    }
}
