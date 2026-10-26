package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.CropUtils;
import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemHelper;
import art.arcane.thaumcraft.golems.GolemInteractionHelper;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import art.arcane.thaumcraft.util.BlockUtils;
import java.util.HashMap;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** TC6 SealHarvest: harvests grown crops in the area and, optionally, replants them (provisioning seeds if asked to). */
public class SealHarvest implements ISeal, ISealGui, ISealConfigArea, ISealConfigToggles {
    int delay = new Random(System.nanoTime()).nextInt(33);
    int count = 0;
    HashMap<Long, ReplantInfo> replantTasks = new HashMap<>();
    protected SealToggle[] props = {new SealToggle(true, "prep", "golem.prop.replant"), new SealToggle(false, "ppro", "golem.prop.provision")};
    @Override public String getKey() { return "thaumcraft:harvest"; }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (delay % 100 == 0) {
            AABB area = GolemHelper.getBoundsForArea(seal);
            var rt = replantTasks.entrySet().iterator();
            while (rt.hasNext()) {
                var e = rt.next();
                BlockPos pp = BlockPos.of(e.getKey());
                if (!area.contains(new Vec3(pp.getX() + 0.5, pp.getY() + 0.5, pp.getZ() + 0.5))) {
                    Task tt = TaskHandler.getTask(world, e.getValue().taskid);
                    if (tt != null) tt.setSuspended(true);
                    rt.remove();
                }
            }
        }
        if (delay++ % 5 != 0) return;
        BlockPos p = GolemHelper.getPosInArea(seal, count++);
        if (CropUtils.isGrownCrop(world, p)) {
            Task task = new Task(seal.getSealPos(), p);
            task.setPriority(seal.getPriority());
            TaskHandler.addTask(world, task);
        } else if (getToggles()[0].value && replantTasks.containsKey(p.asLong()) && world.isEmptyBlock(p)) {
            Task t = TaskHandler.getTask(world, replantTasks.get(p.asLong()).taskid);
            if (t == null) {
                Task tt2 = new Task(seal.getSealPos(), replantTasks.get(p.asLong()).pos);
                tt2.setPriority(seal.getPriority());
                TaskHandler.addTask(world, tt2);
                replantTasks.get(p.asLong()).taskid = tt2.getId();
            }
        }
    }

    @Override
    public boolean onTaskCompletion(Level world, GolemEntity golem, Task task) {
        if (!(world instanceof ServerLevel sl)) return true;
        if (CropUtils.isGrownCrop(world, task.getPos())) {
            var fp = GolemInteractionHelper.fakePlayer(sl, "FakeThaumcraftGolem");
            fp.setPos(golem.getX(), golem.getY(), golem.getZ());
            BlockState bs = world.getBlockState(task.getPos());
            if (CropUtils.isClickable(bs)) {
                bs.useWithoutItem(world, fp, new BlockHitResult(Vec3.atCenterOf(task.getPos()), Direction.UP, task.getPos(), false));
                golem.addRankXp(1);
                golem.swingArm();
            } else {
                GolemInteractionHelper.golemClick(world, golem, task.getPos(), task.getSealPos().face, ItemStack.EMPTY, false, true);
                if (CropUtils.isGrownCrop(world, task.getPos())) {
                    BlockUtils.harvestBlock(world, fp, task.getPos(), true, false, 0, true);
                    golem.addRankXp(1);
                    golem.swingArm();
                    if (getToggles()[0].value) {
                        ItemStack seed = CropUtils.getSeed(bs.getBlock());
                        if (!seed.isEmpty()) {
                            BlockState bb = world.getBlockState(task.getPos().below());
                            Direction rf = null;
                            if (seed.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock().defaultBlockState().canSurvive(world, task.getPos())) rf = Direction.DOWN;
                            else if (bs.getBlock() instanceof DirectionalBlock) rf = bs.getValue(DirectionalBlock.FACING);
                            if (rf != null) {
                                Task tt = new Task(task.getSealPos(), task.getPos());
                                tt.setPriority(task.getPriority());
                                tt.setLifespan((short) 300);
                                replantTasks.put(tt.getPos().asLong(), new ReplantInfo(tt.getPos(), rf, tt.getId(), seed.copy(), bb.getBlock() instanceof FarmlandBlock));
                                TaskHandler.addTask(world, tt);
                            }
                        }
                    }
                }
            }
        } else if (replantTasks.containsKey(task.getPos().asLong()) && replantTasks.get(task.getPos().asLong()).taskid == task.getId() && world.isEmptyBlock(task.getPos())
                && golem.isCarrying(replantTasks.get(task.getPos().asLong()).stack)) {
            var fp = GolemInteractionHelper.fakePlayer(sl, "FakeThaumcraftGolem");
            fp.setPos(golem.getX(), golem.getY(), golem.getZ());
            BlockState bb2 = world.getBlockState(task.getPos().below());
            ReplantInfo ri = replantTasks.get(task.getPos().asLong());
            if ((bb2.is(net.minecraft.world.level.block.Blocks.DIRT) || bb2.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) && ri.farmland) {
                ItemStack hoe = new ItemStack(Items.DIAMOND_HOE);
                fp.setItemInHand(InteractionHand.MAIN_HAND, hoe);
                hoe.useOn(new UseOnContext(fp, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(task.getPos().below()), Direction.UP, task.getPos().below(), false)));
            }
            ItemStack seed = ri.stack.copy();
            seed.setCount(1);
            fp.setItemInHand(InteractionHand.MAIN_HAND, seed.copy());
            BlockPos on = task.getPos().relative(ri.face);
            var r = seed.copy().useOn(new UseOnContext(fp, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(on), ri.face.getOpposite(), on, false)));
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (r.consumesAction()) {
                world.levelEvent(2001, task.getPos(), Block.getId(world.getBlockState(task.getPos())));
                golem.dropItem(seed);
                golem.addRankXp(1);
                golem.swingArm();
            }
        }
        task.setSuspended(true);
        return true;
    }

    @Override
    public boolean canGolemPerformTask(GolemEntity golem, Task task) {
        ReplantInfo ri = replantTasks.get(task.getPos().asLong());
        if (ri != null && ri.taskid == task.getId()) {
            boolean carry = golem.isCarrying(ri.stack);
            if (!carry && getToggles()[1].value) {
                ISealEntity se = SealHandler.getSealEntity(golem.level(), task.getSealPos());
                if (se != null) GolemHelper.requestProvisioning(golem.level(), se, ri.stack);
            }
            return carry;
        }
        return true;
    }
    @Override public void onTaskSuspension(Level world, Task task) {}

    @Override
    public void readCustomNBT(CompoundTag nbt, HolderLookup.Provider reg) {
        var ops = reg.createSerializationContext(NbtOps.INSTANCE);
        for (Tag t : nbt.getListOrEmpty("replant")) {
            if (!(t instanceof CompoundTag c)) continue;
            long loc = c.getLongOr("taskloc", 0L);
            ItemStack stack = ItemStack.CODEC.parse(ops, c.getCompoundOrEmpty("stack")).result().orElse(ItemStack.EMPTY);
            replantTasks.put(loc, new ReplantInfo(BlockPos.of(loc), Direction.from3DDataValue(c.getByteOr("taskface", (byte) 0)), 0, stack, c.getBooleanOr("farmland", false)));
        }
    }
    @Override
    public void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider reg) {
        if (!getToggles()[0].value) return;
        var ops = reg.createSerializationContext(NbtOps.INSTANCE);
        ListTag list = new ListTag();
        for (ReplantInfo info : replantTasks.values()) {
            CompoundTag c = new CompoundTag();
            c.putLong("taskloc", info.pos.asLong());
            c.putByte("taskface", (byte) info.face.get3DDataValue());
            c.putBoolean("farmland", info.farmland);
            if (!info.stack.isEmpty()) ItemStack.CODEC.encodeStart(ops, info.stack).result().ifPresent(s -> c.put("stack", s));
            list.add(c);
        }
        nbt.put("replant", list);
    }
    @Override public boolean canPlaceAt(Level world, BlockPos pos, Direction side) { return !world.isEmptyBlock(pos); }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_harvest"); }
    @Override public void onRemoval(Level world, BlockPos pos, Direction side) {}
    @Override public int[] getGuiCategories() { return new int[]{2, 3, 0, 4}; }
    @Override public SealToggle[] getToggles() { return props; }
    @Override public void setToggle(int i, boolean v) { props[i].setValue(v); }
    @Override public GolemTrait[] getRequiredTags() { return new GolemTrait[]{GolemTrait.DEFT, GolemTrait.SMART}; }
    @Override public GolemTrait[] getForbiddenTags() { return null; }
    @Override public void onTaskStarted(Level world, GolemEntity golem, Task task) {}

    static final class ReplantInfo {
        Direction face; BlockPos pos; int taskid; ItemStack stack; boolean farmland;
        ReplantInfo(BlockPos pos, Direction face, int taskid, ItemStack stack, boolean farmland) { this.pos = pos; this.face = face; this.taskid = taskid; this.stack = stack; this.farmland = farmland; }
    }
}
