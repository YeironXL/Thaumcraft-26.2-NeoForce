package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity;
import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

/**
 * TC4 alchemy golem core recreated as a seal. Placed on a jar (the target), it sends golems to the other jars around it
 * (the seal area, used as a radius) to draw essentia into a phial-like reserve (10 points, 20 for haulers) and pour it
 * into the target. The aspect moved is the target's label, else its current contents, else whatever is found first.
 */
public class SealEssentia implements ISeal, ISealGui, ISealConfigArea, ISealConfigToggles {
    public static final String CARRIED = "thaumcraft:carried_essentia";
    int delay = new Random(System.nanoTime()).nextInt(20);
    int fetchTask = Integer.MIN_VALUE, deliverTask = Integer.MIN_VALUE;
    protected SealToggle[] props = {new SealToggle(true, "plabel", "golem.prop.essentia.labels"), new SealToggle(false, "pvoid", "golem.prop.essentia.void")};
    @Override public String getKey() { return "thaumcraft:essentia"; }

    // ---- the golem's essentia reserve (a phial carried in its persistent data)
    public static String carriedAspect(GolemEntity g) { return g.getPersistentData().getCompoundOrEmpty(CARRIED).getStringOr("aspect", ""); }
    public static int carriedAmount(GolemEntity g) { return g.getPersistentData().getCompoundOrEmpty(CARRIED).getIntOr("amount", 0); }
    public static void setCarried(GolemEntity g, String aspect, int amount) {
        if (amount <= 0 || aspect.isEmpty()) { g.getPersistentData().remove(CARRIED); return; }
        CompoundTag c = new CompoundTag(); c.putString("aspect", aspect); c.putInt("amount", amount);
        g.getPersistentData().put(CARRIED, c);
    }
    public static int capacity(GolemEntity g) { return g.getProperties().hasTrait(GolemTrait.HAULER) ? 20 : 10; }

    static EssentiaJarBlockEntity jar(Level world, BlockPos p) {
        return world.getBlockEntity(p) instanceof EssentiaJarBlockEntity j && !j.alembic() ? j : null;
    }
    /** Aspect the target wants; "" means anything. */
    static String wanted(EssentiaJarBlockEntity t) { return !t.filter().isEmpty() ? t.filter() : t.contents().amount() > 0 ? t.aspect() : ""; }
    static int room(EssentiaJarBlockEntity t) { return t.capacity() - t.contents().amount(); }

    BlockPos findSource(Level world, ISealEntity seal, EssentiaJarBlockEntity target) {
        BlockPos c = seal.getSealPos().pos, a = seal.getArea();
        String want = wanted(target);
        int rx = a.getX() + 1, ry = a.getY() + 1, rz = a.getZ() + 1;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-rx, -ry, -rz), c.offset(rx, ry, rz))) {
            if (p.equals(c)) continue;
            EssentiaJarBlockEntity j = jar(world, p);
            if (j == null || j.contents().amount() <= 0) continue;
            if (!want.isEmpty() && !want.equals(j.aspect())) continue;
            if (props[0].value && !j.filter().isEmpty()) continue;
            if (!props[1].value && j.getBlockState().is(art.arcane.thaumcraft.ModBlocks.JAR_VOID.get())) continue;
            return p.immutable();
        }
        return null;
    }

    static boolean live(Level world, int id) { Task t = TaskHandler.getTask(world, id); return t != null && !t.isSuspended() && !t.isCompleted(); }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (delay++ % 20 != 0) return;
        EssentiaJarBlockEntity target = jar(world, seal.getSealPos().pos);
        if (target == null || room(target) <= 0) return;
        if (!live(world, deliverTask)) {
            Task t = new Task(seal.getSealPos(), seal.getSealPos().pos);
            t.setPriority(seal.getPriority());
            TaskHandler.addTask(world, t);
            deliverTask = t.getId();
        }
        if (!live(world, fetchTask)) {
            BlockPos src = findSource(world, seal, target);
            if (src != null) {
                Task t = new Task(seal.getSealPos(), src);
                t.setPriority(seal.getPriority());
                TaskHandler.addTask(world, t);
                fetchTask = t.getId();
            }
        }
    }

    boolean isDeliver(Task task) { return task.getPos().equals(task.getSealPos().pos); }

    @Override
    public boolean canGolemPerformTask(GolemEntity golem, Task task) {
        Level world = golem.level();
        EssentiaJarBlockEntity target = jar(world, task.getSealPos().pos);
        if (target == null) return false;
        String want = wanted(target), ca = carriedAspect(golem);
        int amt = carriedAmount(golem);
        if (isDeliver(task)) return amt > 0 && (want.isEmpty() || want.equals(ca)) && room(target) > 0 && target.contents().accepts(ca, 1);
        EssentiaJarBlockEntity src = jar(world, task.getPos());
        if (src == null || src.contents().amount() <= 0) return false;
        if (amt >= capacity(golem)) return false;
        if (amt > 0 && !ca.equals(src.aspect())) return false;
        return want.isEmpty() || want.equals(src.aspect());
    }

    @Override
    public boolean onTaskCompletion(Level world, GolemEntity golem, Task task) {
        String ca = carriedAspect(golem);
        int amt = carriedAmount(golem);
        if (isDeliver(task)) {
            EssentiaJarBlockEntity target = jar(world, task.getPos());
            if (target != null && amt > 0) {
                int moved = 0;
                while (amt > 0 && target.insert(ca, 1)) { amt--; moved++; }
                if (moved > 0) {
                    setCarried(golem, ca, amt);
                    golem.playSound(SoundEvents.BOTTLE_EMPTY, 0.25f, 1.0f);
                    golem.addRankXp(1);
                    golem.swingArm();
                }
            }
        } else {
            EssentiaJarBlockEntity src = jar(world, task.getPos());
            if (src != null && src.contents().amount() > 0 && (amt == 0 || ca.equals(src.aspect()))) {
                String asp = src.aspect();
                int take = Math.min(capacity(golem) - amt, src.contents().amount());
                if (take > 0 && src.extract(take)) {
                    setCarried(golem, asp, amt + take);
                    golem.playSound(SoundEvents.BOTTLE_FILL, 0.25f, 1.0f);
                    golem.addRankXp(1);
                    golem.swingArm();
                }
            }
        }
        task.setSuspended(true);
        return true;
    }
    @Override public void onTaskStarted(Level world, GolemEntity golem, Task task) {}
    @Override public void onTaskSuspension(Level world, Task task) {}
    @Override public void readCustomNBT(CompoundTag nbt, HolderLookup.Provider reg) {}
    @Override public void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider reg) {}
    @Override public boolean canPlaceAt(Level world, BlockPos pos, Direction side) { return jar(world, pos) != null; }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_essentia"); }
    @Override public void onRemoval(Level world, BlockPos pos, Direction side) {}
    @Override public int[] getGuiCategories() { return new int[]{2, 3, 0, 4}; }
    @Override public SealToggle[] getToggles() { return props; }
    @Override public void setToggle(int i, boolean v) { props[i].setValue(v); }
    @Override public GolemTrait[] getRequiredTags() { return new GolemTrait[]{GolemTrait.SMART}; }
    @Override public GolemTrait[] getForbiddenTags() { return new GolemTrait[]{GolemTrait.CLUMSY}; }
}
