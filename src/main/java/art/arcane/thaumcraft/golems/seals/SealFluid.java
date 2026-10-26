package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * TC4 decanting (liquid) golem core recreated as a seal. Placed on a fluid container (tank, cauldron... anything with a
 * fluid handler), it sends golems to the fluid sources around it (the seal area, used as a radius): other fluid
 * containers and/or source blocks in the world, which they pick up like a bucket. Golems carry one bucket (two for
 * haulers) and pour it into the target. If the target already holds a fluid only that fluid is fetched.
 */
public class SealFluid implements ISeal, ISealGui, ISealConfigArea, ISealConfigToggles {
    public static final String CARRIED = "thaumcraft:carried_fluid";
    int delay = new Random(System.nanoTime()).nextInt(20);
    int fetchTask = Integer.MIN_VALUE, deliverTask = Integer.MIN_VALUE;
    protected SealToggle[] props = {new SealToggle(true, "ptank", "golem.prop.fluid.tanks"), new SealToggle(true, "pworld", "golem.prop.fluid.world")};
    @Override public String getKey() { return "thaumcraft:fluid"; }

    // ---- the golem's fluid reserve
    public static Fluid carriedFluid(GolemEntity g) {
        String id = g.getPersistentData().getCompoundOrEmpty(CARRIED).getStringOr("fluid", "");
        Identifier rl = id.isEmpty() ? null : Identifier.tryParse(id);
        return rl == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.getValue(rl);
    }
    public static int carriedAmount(GolemEntity g) { return g.getPersistentData().getCompoundOrEmpty(CARRIED).getIntOr("amount", 0); }
    public static void setCarried(GolemEntity g, Fluid f, int amount) {
        if (amount <= 0 || f == Fluids.EMPTY) { g.getPersistentData().remove(CARRIED); return; }
        CompoundTag c = new CompoundTag(); c.putString("fluid", BuiltInRegistries.FLUID.getKey(f).toString()); c.putInt("amount", amount);
        g.getPersistentData().put(CARRIED, c);
    }
    public static int capacity(GolemEntity g) { return g.getProperties().hasTrait(GolemTrait.HAULER) ? 2000 : 1000; }

    static ResourceHandler<FluidResource> handler(Level world, BlockPos p, Direction side) {
        return world instanceof ServerLevel sl ? sl.getCapability(Capabilities.Fluid.BLOCK, p, side) : null;
    }
    /** The fluid the target already holds (or EMPTY = anything). */
    static Fluid targetFluid(ResourceHandler<FluidResource> h) {
        for (int i = 0; i < h.size(); i++) if (!h.getResource(i).isEmpty() && h.getAmountAsLong(i) > 0) return h.getResource(i).getFluid();
        return Fluids.EMPTY;
    }
    static int simulateInsert(ResourceHandler<FluidResource> h, Fluid f, int amount) {
        try (var tx = Transaction.openRoot()) { return h.insert(FluidResource.of(f), amount, tx); }
    }
    static boolean sameFluid(Fluid a, Fluid b) { return a == Fluids.EMPTY || b == Fluids.EMPTY || a.isSame(b); }

    /** Fluid a source position offers (EMPTY if none). */
    Fluid offered(Level world, BlockPos p, Fluid want) {
        if (props[1].value) {
            BlockState bs = world.getBlockState(p);
            if (bs.getBlock() instanceof LiquidBlock && bs.getBlock() instanceof BucketPickup && world.getFluidState(p).isSource()) {
                Fluid f = world.getFluidState(p).getType();
                if (sameFluid(want, f)) return f;
            }
        }
        if (props[0].value) {
            var h = handler(world, p, Direction.UP);
            if (h != null) for (int i = 0; i < h.size(); i++) {
                FluidResource r = h.getResource(i);
                if (r.isEmpty() || h.getAmountAsLong(i) <= 0 || !r.isComponentsPatchEmpty() || !sameFluid(want, r.getFluid())) continue;
                try (var tx = Transaction.openRoot()) { if (h.extract(r, 1, tx) > 0) return r.getFluid(); }
            }
        }
        return Fluids.EMPTY;
    }

    BlockPos findSource(Level world, ISealEntity seal, Fluid want) {
        BlockPos c = seal.getSealPos().pos, a = seal.getArea();
        int rx = a.getX() + 1, ry = a.getY() + 1, rz = a.getZ() + 1;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-rx, -ry, -rz), c.offset(rx, ry, rz))) {
            if (p.equals(c)) continue;
            if (offered(world, p, want) != Fluids.EMPTY) return p.immutable();
        }
        return null;
    }
    static boolean live(Level world, int id) { Task t = TaskHandler.getTask(world, id); return t != null && !t.isSuspended() && !t.isCompleted(); }
    boolean isDeliver(Task task) { return task.getPos().equals(task.getSealPos().pos); }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (delay++ % 20 != 0) return;
        var target = handler(world, seal.getSealPos().pos, seal.getSealPos().face);
        if (target == null) return;
        Fluid want = targetFluid(target);
        if (want != Fluids.EMPTY && simulateInsert(target, want, 1) <= 0) return;
        if (!live(world, deliverTask)) {
            Task t = new Task(seal.getSealPos(), seal.getSealPos().pos);
            t.setPriority(seal.getPriority());
            TaskHandler.addTask(world, t);
            deliverTask = t.getId();
        }
        if (!live(world, fetchTask)) {
            BlockPos src = findSource(world, seal, want);
            if (src != null) {
                Task t = new Task(seal.getSealPos(), src);
                t.setPriority(seal.getPriority());
                TaskHandler.addTask(world, t);
                fetchTask = t.getId();
            }
        }
    }

    @Override
    public boolean canGolemPerformTask(GolemEntity golem, Task task) {
        Level world = golem.level();
        var target = handler(world, task.getSealPos().pos, task.getSealPos().face);
        if (target == null) return false;
        Fluid want = targetFluid(target), cf = carriedFluid(golem);
        int amt = carriedAmount(golem);
        if (isDeliver(task)) return amt > 0 && simulateInsert(target, cf, amt) > 0;
        if (capacity(golem) - amt < 1000 && !hasTankSource(world, task.getPos())) return false;
        if (amt >= capacity(golem)) return false;
        Fluid f = offered(world, task.getPos(), amt > 0 ? cf : want);
        return f != Fluids.EMPTY;
    }
    boolean hasTankSource(Level world, BlockPos p) { return props[0].value && handler(world, p, Direction.UP) != null; }

    @Override
    public boolean onTaskCompletion(Level world, GolemEntity golem, Task task) {
        Fluid cf = carriedFluid(golem);
        int amt = carriedAmount(golem);
        if (isDeliver(task)) {
            var target = handler(world, task.getPos(), task.getSealPos().face);
            if (target != null && amt > 0) {
                int moved;
                try (var tx = Transaction.openRoot()) { moved = target.insert(FluidResource.of(cf), amt, tx); tx.commit(); }
                if (moved > 0) {
                    setCarried(golem, cf, amt - moved);
                    golem.playSound(cf.isSame(Fluids.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY, 0.5f, 1.0f);
                    golem.addRankXp(1);
                    golem.swingArm();
                }
            }
        } else {
            BlockPos p = task.getPos();
            int room = capacity(golem) - amt;
            Fluid want = amt > 0 ? cf : Fluids.EMPTY;
            var dest = handler(world, task.getSealPos().pos, task.getSealPos().face);
            if (amt == 0 && dest != null) want = targetFluid(dest);
            Fluid got = Fluids.EMPTY;
            int gained = 0;
            BlockState bs = world.getBlockState(p);
            if (props[1].value && room >= 1000 && bs.getBlock() instanceof LiquidBlock && bs.getBlock() instanceof BucketPickup bp && world.getFluidState(p).isSource()
                    && sameFluid(want, world.getFluidState(p).getType())) {
                Fluid f = world.getFluidState(p).getType();
                if (!bp.pickupBlock(golem, world, p, bs).isEmpty()) { got = f; gained = 1000; }
            } else if (props[0].value) {
                var h = handler(world, p, Direction.UP);
                if (h != null) for (int i = 0; i < h.size() && gained == 0; i++) {
                    FluidResource r = h.getResource(i);
                    if (r.isEmpty() || !r.isComponentsPatchEmpty() || !sameFluid(want, r.getFluid())) continue;
                    try (var tx = Transaction.openRoot()) { gained = h.extract(r, room, tx); tx.commit(); }
                    if (gained > 0) got = r.getFluid();
                }
            }
            if (gained > 0) {
                setCarried(golem, got, amt + gained);
                golem.playSound(got.isSame(Fluids.LAVA) ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL, 0.5f, 1.0f);
                golem.addRankXp(1);
                golem.swingArm();
            }
        }
        task.setSuspended(true);
        return true;
    }
    @Override public void onTaskStarted(Level world, GolemEntity golem, Task task) {}
    @Override public void onTaskSuspension(Level world, Task task) {}
    @Override public void readCustomNBT(CompoundTag nbt, HolderLookup.Provider reg) {}
    @Override public void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider reg) {}
    @Override public boolean canPlaceAt(Level world, BlockPos pos, Direction side) { return world.isClientSide() ? !world.isEmptyBlock(pos) : handler(world, pos, side) != null; }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_fluid"); }
    @Override public void onRemoval(Level world, BlockPos pos, Direction side) {}
    @Override public int[] getGuiCategories() { return new int[]{2, 3, 0, 4}; }
    @Override public SealToggle[] getToggles() { return props; }
    @Override public void setToggle(int i, boolean v) { props[i].setValue(v); }
    @Override public GolemTrait[] getRequiredTags() { return null; }
    @Override public GolemTrait[] getForbiddenTags() { return new GolemTrait[]{GolemTrait.CLUMSY}; }
}
