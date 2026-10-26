package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemEntity;
import art.arcane.thaumcraft.golems.GolemHelper;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.tasks.Task;
import art.arcane.thaumcraft.golems.tasks.TaskHandler;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * TC4 fishing golem core recreated as a seal: golems go to open water in the seal area, fish for a while and keep the
 * catch (vanilla fishing loot table), to be delivered by other seals (Store/Fill) like any held item.
 */
public class SealFishing implements ISeal, ISealGui, ISealConfigArea {
    int delay = new Random(System.nanoTime()).nextInt(20);
    int count = 0;
    int watchedTask = Integer.MIN_VALUE;
    @Override public String getKey() { return "thaumcraft:fishing"; }

    static boolean isFishingWater(Level world, BlockPos p) {
        return world.getFluidState(p).is(FluidTags.WATER) && world.getFluidState(p).isSource() && world.isEmptyBlock(p.above());
    }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (delay++ % 5 != 0) return;
        Task old = TaskHandler.getTask(world, watchedTask);
        if (old != null && !old.isSuspended() && !old.isCompleted()) return;
        BlockPos p = GolemHelper.getPosInArea(seal, count++);
        BlockPos w = isFishingWater(world, p) ? p : isFishingWater(world, p.below()) ? p.below() : null;
        if (w == null) return;
        Task task = new Task(seal.getSealPos(), w);
        task.setPriority(seal.getPriority());
        TaskHandler.addTask(world, task);
        watchedTask = task.getId();
    }

    @Override
    public boolean onTaskCompletion(Level world, GolemEntity golem, Task task) {
        if (!(world instanceof ServerLevel sl) || !isFishingWater(world, task.getPos())) { task.setSuspended(true); return true; }
        BlockPos w = task.getPos();
        int casts = task.getData();
        task.setData(casts + 1);
        if (casts == 0) {
            golem.swingArm();
            world.playSound(null, w, SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        }
        sl.sendParticles(ParticleTypes.FISHING, w.getX() + 0.5, w.getY() + 1.0, w.getZ() + 0.5, 2, 0.1, 0.0, 0.1, 0.0);
        // TC4/vanilla: a bite takes a while; each attempt (about every 10 ticks) has a chance once the line settled.
        if (casts < 3 || world.getRandom().nextInt(4) != 0) return false;
        ItemStack rod = new ItemStack(Items.FISHING_ROD);
        LootParams params = new LootParams.Builder(sl).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(w)).withParameter(LootContextParams.TOOL, rod)
                .withLuck(golem.getProperties().hasTrait(GolemTrait.SMART) ? 1.0f : 0.0f).create(LootContextParamSets.FISHING);
        for (ItemStack s : sl.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING).getRandomItems(params)) {
            ItemStack rest = golem.holdItem(s);
            if (!rest.isEmpty()) world.addFreshEntity(new ItemEntity(world, golem.getX(), golem.getY() + 0.5, golem.getZ(), rest));
        }
        world.playSound(null, w, SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.NEUTRAL, 0.25f, 1.0f + (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.4f);
        sl.sendParticles(ParticleTypes.SPLASH, w.getX() + 0.5, w.getY() + 1.0, w.getZ() + 0.5, 12, 0.2, 0.0, 0.2, 0.0);
        golem.addRankXp(1);
        golem.swingArm();
        task.setSuspended(true);
        return true;
    }

    @Override
    public boolean canGolemPerformTask(GolemEntity golem, Task task) {
        for (ItemStack s : golem.getCarrying()) if (s.isEmpty()) return true;
        return false;
    }
    @Override public void onTaskStarted(Level world, GolemEntity golem, Task task) { task.setData(0); }
    @Override public void onTaskSuspension(Level world, Task task) {}
    @Override public void readCustomNBT(CompoundTag nbt, HolderLookup.Provider reg) {}
    @Override public void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider reg) {}
    @Override public boolean canPlaceAt(Level world, BlockPos pos, Direction side) { return !world.isEmptyBlock(pos); }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_fishing"); }
    @Override public void onRemoval(Level world, BlockPos pos, Direction side) {}
    @Override public int[] getGuiCategories() { return new int[]{2, 0, 4}; }
    @Override public GolemTrait[] getRequiredTags() { return null; }
    @Override public GolemTrait[] getForbiddenTags() { return new GolemTrait[]{GolemTrait.CLUMSY}; }
}
