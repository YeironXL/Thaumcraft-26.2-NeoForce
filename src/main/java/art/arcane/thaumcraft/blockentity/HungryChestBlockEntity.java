package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class HungryChestBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    public HungryChestBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.HUNGRY_CHEST.get(), pos, state); }
    @Override public int getContainerSize() { return 27; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> values) { items = values; }
    @Override protected Component getDefaultName() { return Component.translatable("block.thaumcraft.hungry_chest"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return ChestMenu.threeRows(id, inventory, this); }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!trySaveLootTable(output)) ContainerHelper.saveAllItems(output, items);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(input)) ContainerHelper.loadAllItems(input, items);
    }

    /** Returns an independent remainder; never consumes items that could not fit. */
    public ItemStack insert(ItemStack incoming) {
        ItemStack remainder = incoming.copy();
        for (int pass = 0; pass < 2 && !remainder.isEmpty(); pass++) {
            for (int slot = 0; slot < getContainerSize() && !remainder.isEmpty(); slot++) {
                ItemStack existing = getItem(slot);
                if (existing.isEmpty()) {
                    if (pass == 0) continue;
                    int moved = Math.min(remainder.getCount(), getMaxStackSize(remainder));
                    setItem(slot, remainder.copyWithCount(moved));
                    remainder.shrink(moved);
                } else if (pass == 0 && ItemStack.isSameItemSameComponents(existing, remainder)) {
                    int moved = Math.min(remainder.getCount(), Math.max(0, getMaxStackSize(existing) - existing.getCount()));
                    if (moved > 0) { existing.grow(moved); remainder.shrink(moved); setChanged(); }
                }
            }
        }
        return remainder;
    }
}
