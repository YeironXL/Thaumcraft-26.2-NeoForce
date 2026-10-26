package art.arcane.thaumcraft.menu;

import art.arcane.thaumcraft.blockentity.ArcaneWorkbenchBlockEntity;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ArcaneWorkbenchMenu extends AbstractContainerMenu {
    public static final int[] CRYSTAL_X={64,17,17,112,112,64};
    public static final int[] CRYSTAL_Y={13,93,35,35,93,115};
    private final ArcaneWorkbenchBlockEntity bench;
    private final Player owner;
    private final ContainerData data = new SimpleContainerData(2);
    public ArcaneWorkbenchMenu(int id, Inventory inventory) { this(id,inventory,null); }
    public ArcaneWorkbenchMenu(int id, Inventory inventory, ArcaneWorkbenchBlockEntity bench) {
        super(ModMenus.ARCANE_WORKBENCH.get(),id);
        this.bench = bench; this.owner = inventory.player;
        Container inputs = bench == null ? new SimpleContainer(16) : bench;
        addSlot(new Slot(new SimpleContainer(1),0,160,64) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public ItemStack getItem() { return bench != null && owner instanceof ServerPlayer player ? bench.preview(player) : super.getItem(); }
            @Override public boolean mayPickup(Player player) { return bench == null || (player instanceof ServerPlayer serverPlayer && !bench.preview(serverPlayer).isEmpty() && bench.stillValid(player)); }
            @Override public ItemStack remove(int amount) { return bench != null && owner instanceof ServerPlayer player ? bench.takeOutput(player,amount) : super.remove(amount); }
        });
        for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) addSlot(new Slot(inputs,x+y*3,40+x*24,40+y*24));
        for (int index = 0; index < 6; index++) {
            String aspect = ArcaneWorkbenchBlockEntity.PRIMALS.get(index);
            addSlot(new Slot(inputs,9+index,CRYSTAL_X[index],CRYSTAL_Y[index]) {
                @Override public boolean mayPlace(ItemStack stack) { return CrystalEssenceItem.aspect(stack).equals(aspect); }
            });
        }
        addStandardInventorySlots(inventory,16,151);
        addDataSlots(data);
    }
    public int availableVis() { return data.get(0); }
    public int requiredVis() { return data.get(1); }
    @Override public void broadcastChanges() {
        if (bench != null && owner instanceof ServerPlayer player) { data.set(0,bench.availableVis()); data.set(1,bench.requiredVis(player)); }
        super.broadcastChanges();
    }
    @Override public boolean stillValid(Player player) { return bench == null || bench.stillValid(player); }
    @Override public void clicked(int slot, int button, ContainerInput input, Player player) {
        // Vanilla hotbar swapping extracts via setByPlayer rather than Slot.remove.
        // Commit that path explicitly so it cannot pick up an unpaid preview.
        if (slot == 0 && input == ContainerInput.SWAP) {
            if (bench != null && player instanceof ServerPlayer actor && (button >= 0 && button <= 8 || button == 40)
                    && player.getInventory().getItem(button).isEmpty() && stillValid(player)) {
                var preview = bench.preview(actor);
                if (!preview.isEmpty()) player.getInventory().setItem(button,bench.takeOutput(actor,preview.getCount()));
            }
            return;
        }
        super.clicked(slot,button,input,player);
    }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot != slots.getFirst() && super.canTakeItemForPickAll(stack,slot); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        if (index == 0) {
            var preview = slot.getItem(); int capacity = 0;
            for (int i = 16; i < 52; i++) {
                var existing = slots.get(i).getItem();
                if (existing.isEmpty()) capacity += preview.getMaxStackSize();
                else if (ItemStack.isSameItemSameComponents(existing,preview)) capacity += Math.max(0,existing.getMaxStackSize()-existing.getCount());
            }
            if (capacity < preview.getCount()) return ItemStack.EMPTY;
            var result = slot.remove(preview.getCount()); var copied = result.copy();
            moveItemStackTo(result,16,52,true);
            if (!result.isEmpty()) player.drop(result,false);
            return copied;
        }
        var stack = slot.getItem(); var original = stack.copy();
        if (index >= 16) {
            String aspect = CrystalEssenceItem.aspect(stack);
            int primal = ArcaneWorkbenchBlockEntity.PRIMALS.indexOf(aspect);
            if (primal >= 0) { if (!moveItemStackTo(stack,10+primal,11+primal,false)) return ItemStack.EMPTY; }
            else if (!moveItemStackTo(stack,1,10,false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack,16,52,false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player,stack);
        return original;
    }
}
