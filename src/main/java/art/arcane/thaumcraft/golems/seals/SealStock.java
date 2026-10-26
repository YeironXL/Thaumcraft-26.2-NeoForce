package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemHelper;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.InvHelper;
import art.arcane.thaumcraft.golems.tasks.Task;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** TC6 SealStock: keeps the container stocked to the filter amounts by posting provisioning requests. */
public class SealStock extends SealFiltered implements ISealConfigToggles {
    int delay = new Random(System.nanoTime()).nextInt(50);
    protected SealToggle[] props = {new SealToggle(true, "pmeta", "golem.prop.meta"), new SealToggle(true, "pnbt", "golem.prop.nbt"),
            new SealToggle(false, "pore", "golem.prop.ore"), new SealToggle(false, "pmod", "golem.prop.mod")};
    @Override public String getKey() { return "thaumcraft:stock"; }
    @Override public int getFilterSize() { return 9; }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (delay++ % 20 != 0) return;
        var inv = InvHelper.getItemHandlerAt(world, seal.getSealPos().pos, seal.getSealPos().face);
        if (inv == null) return;
        for (int a = 0; a < 9; a++) {
            if (getFilterSlot(a).isEmpty()) continue;
            int amt = InvHelper.countTotalItemsIn(inv, getFilterSlot(a), invFilter(props));
            if (amt < getFilterSlotSize(a)) {
                ItemStack fs = getFilterSlot(a).copy();
                fs.setCount(Math.min(fs.getMaxStackSize(), getFilterSlotSize(a) - amt));
                fs = InvHelper.hasRoomFor(world, seal.getSealPos().pos, seal.getSealPos().face, fs);
                if (!fs.isEmpty()) GolemHelper.requestProvisioning(world, seal.getSealPos().pos, seal.getSealPos().face, fs);
            }
        }
    }
    @Override public void onTaskStarted(Level world, GolemEntity golem, Task task) {}
    @Override public boolean onTaskCompletion(Level world, GolemEntity golem, Task task) { return true; }
    @Override public boolean canGolemPerformTask(GolemEntity golem, Task task) { return false; }
    @Override public boolean canPlaceAt(Level world, BlockPos pos, Direction side) { return InvHelper.getItemHandlerAt(world, pos, side) != null; }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_stock"); }
    @Override public int[] getGuiCategories() { return new int[]{1, 3, 0, 4}; }
    @Override public GolemTrait[] getRequiredTags() { return null; }
    @Override public GolemTrait[] getForbiddenTags() { return new GolemTrait[]{GolemTrait.CLUMSY}; }
    @Override public void onTaskSuspension(Level world, Task task) {}
    @Override public void onRemoval(Level world, BlockPos pos, Direction side) {}
    @Override public boolean hasStacksizeLimiters() { return true; }
    @Override public boolean isBlacklist() { return false; }
    @Override public SealToggle[] getToggles() { return props; }
    @Override public void setToggle(int i, boolean v) { props[i].setValue(v); }
}
