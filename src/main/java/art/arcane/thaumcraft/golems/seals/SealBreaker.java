package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemHelper;
import art.arcane.thaumcraft.golems.GolemInteractionHelper;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.InvHelper;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import art.arcane.thaumcraft.util.BlockUtils;
import java.util.HashMap;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** TC6 SealBreaker: breaker-armed golems dig out (filtered) blocks in the area, chipping away at harder blocks over several visits. */
public class SealBreaker extends SealFiltered implements ISealConfigArea, ISealConfigToggles {
    int delay = new Random(System.nanoTime()).nextInt(42);
    HashMap<Integer, Long> cache = new HashMap<>();
    protected SealToggle[] props = {new SealToggle(true, "pmeta", "golem.prop.meta")};
    @Override public String getKey() { return "thaumcraft:breaker"; }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (delay % 100 == 0) cache.keySet().removeIf(id -> TaskHandler.getTask(world, id) == null);
        ++delay;
        BlockPos p = GolemHelper.getPosInArea(seal, delay);
        if (!cache.containsValue(p.asLong()) && isValidBlock(world, p)) {
            Task task = new Task(seal.getSealPos(), p);
            task.setPriority(seal.getPriority());
            task.setData((int) (world.getBlockState(p).getDestroySpeed(world, p) * 10.0f));
            TaskHandler.addTask(world, task);
            cache.put(task.getId(), p.asLong());
        }
    }
    /** The broken block's item (silk-touch drop when it has one) checked against the filter; "meta" compares exact components. */
    private boolean isValidBlock(Level world, BlockPos p) {
        BlockState bs = world.getBlockState(p);
        if (world.isEmptyBlock(p) || bs.getDestroySpeed(world, p) < 0.0f) return false;
        ItemStack fs = new ItemStack(bs.getBlock().asItem());
        var f = new InvHelper.InvFilter(!getToggles()[0].value, !getToggles()[0].value, false, false);
        for (ItemStack ts : getInv()) {
            if (ts.isEmpty()) continue;
            boolean m = !fs.isEmpty() && InvHelper.areItemStacksEqual(ts, fs, f);
            if (isBlacklist()) { if (m) return false; }
            else if (!m) return false;
        }
        return true;
    }

    @Override
    public boolean onTaskCompletion(Level world, GolemEntity golem, Task task) {
        BlockState bs = world.getBlockState(task.getPos());
        if (cache.containsKey(task.getId()) && isValidBlock(world, task.getPos()) && world instanceof ServerLevel sl) {
            var fp = GolemInteractionHelper.fakePlayer(sl, "FakeThaumcraftGolem");
            fp.setPos(golem.getX(), golem.getY(), golem.getZ());
            golem.swingArm();
            boolean silky = getToggles().length > 1 && getToggles()[1].value;
            int bspd = silky ? 7 : 21;
            if (task.getData() > bspd) {
                float bh = bs.getDestroySpeed(world, task.getPos()) * 10.0f;
                task.setLifespan((short) Math.max(task.getLifespan(), 10L));
                task.setData(task.getData() - bspd);
                int progress = (int) (9.0f * (1.0f - task.getData() / bh));
                var st = bs.getSoundType();
                world.playSound(null, task.getPos(), st.getBreakSound(), SoundSource.BLOCKS, (st.getVolume() + 0.7f) / 8.0f, st.getPitch() * 0.5f);
                world.destroyBlockProgress(golem.getId(), task.getPos(), progress);
                return false;
            }
            world.destroyBlockProgress(golem.getId(), task.getPos(), 10);
            BlockUtils.harvestBlock(world, fp, task.getPos(), true, silky, 0, true);
            golem.addRankXp(1);
            cache.remove(task.getId());
        }
        task.setSuspended(true);
        return true;
    }
    @Override
    public boolean canGolemPerformTask(GolemEntity golem, Task task) {
        if (cache.containsKey(task.getId()) && isValidBlock(golem.level(), task.getPos())) return true;
        task.setSuspended(true);
        return false;
    }
    @Override public void onTaskSuspension(Level world, Task task) { cache.remove(task.getId()); }
    @Override public boolean canPlaceAt(Level world, BlockPos pos, Direction side) { return !world.isEmptyBlock(pos); }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_breaker"); }
    @Override public void onRemoval(Level world, BlockPos pos, Direction side) {}
    @Override public int[] getGuiCategories() { return new int[]{2, 1, 3, 0, 4}; }
    @Override public GolemTrait[] getRequiredTags() { return new GolemTrait[]{GolemTrait.BREAKER}; }
    @Override public GolemTrait[] getForbiddenTags() { return null; }
    @Override public void onTaskStarted(Level world, GolemEntity golem, Task task) {}
    @Override public SealToggle[] getToggles() { return props; }
    @Override public void setToggle(int i, boolean v) { props[i].setValue(v); }
}
