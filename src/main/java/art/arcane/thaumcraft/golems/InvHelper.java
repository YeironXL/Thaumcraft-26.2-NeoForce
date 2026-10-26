package art.arcane.thaumcraft.golems;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/** The TC6 ThaumcraftInvHelper / InventoryUtils pieces used by golems and seals, on the NeoForge transfer API. */
public final class InvHelper {
    /** ThaumcraftInvHelper.InvFilter: ignore damage, ignore NBT (components), match by ore dictionary (shared c: tag), match by mod. */
    public static final class InvFilter {
        public boolean igDmg, igNBT, useOre, useMod, relaxedNBT;
        public InvFilter(boolean ignoreDamage, boolean ignoreNBT, boolean useOre, boolean useMod) { igDmg = ignoreDamage; igNBT = ignoreNBT; this.useOre = useOre; this.useMod = useMod; }
        public InvFilter setRelaxedNBT() { relaxedNBT = true; return this; }
        public static final InvFilter STRICT = new InvFilter(false, false, false, false);
        public static final InvFilter BASEORE = new InvFilter(false, false, true, false);
    }

    public static @Nullable ResourceHandler<ItemResource> getItemHandlerAt(Level world, BlockPos pos, Direction side) {
        if (world == null || !world.hasChunkAt(pos)) return null;
        return world.getCapability(Capabilities.Item.BLOCK, pos, side);
    }

    /** InventoryUtils.areItemStacksEqual. */
    public static boolean areItemStacksEqual(ItemStack a, ItemStack b, InvFilter filter) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        if (filter.useMod) {
            return BuiltInRegistries.ITEM.getKey(a.getItem()).getNamespace().equals(BuiltInRegistries.ITEM.getKey(b.getItem()).getNamespace());
        }
        if (filter.useOre) {
            var tags = a.typeHolder().tags().filter(t -> t.location().getNamespace().equals("c")).toList();
            for (var t : tags) if (b.is(t)) return true;
        }
        if (a.getItem() != b.getItem()) return false;
        if (!filter.igDmg && a.getDamageValue() != b.getDamageValue()) return false;
        if (!filter.igNBT) {
            var pa = a.getComponentsPatch().forget(c -> c == DataComponents.DAMAGE);
            var pb = b.getComponentsPatch().forget(c -> c == DataComponents.DAMAGE);
            if (filter.relaxedNBT) {
                for (var e : pa.entrySet()) if (!java.util.Objects.equals(pb.getPatch(e.getKey()), e.getValue())) return false;
                return true;
            }
            return pa.equals(pb);
        }
        return true;
    }

    public static int countTotalItemsIn(@Nullable ResourceHandler<ItemResource> inv, ItemStack stack, InvFilter filter) {
        int count = 0;
        if (inv != null) for (int a = 0; a < inv.size(); a++) {
            ItemStack in = inv.getResource(a).toStack(inv.getAmountAsInt(a));
            if (areItemStacksEqual(stack, in, filter)) count += in.getCount();
        }
        return count;
    }
    public static int countTotalItemsIn(Level world, BlockPos pos, Direction side, ItemStack stack, InvFilter filter) {
        return countTotalItemsIn(getItemHandlerAt(world, pos, side), stack, filter);
    }

    /** ItemHandlerHelper.insertItemStacked: returns what did not fit. */
    public static ItemStack insertStacked(@Nullable ResourceHandler<ItemResource> inv, ItemStack stack, boolean simulate) {
        if (inv == null || stack.isEmpty()) return stack;
        try (var tx = Transaction.openRoot()) {
            int moved = ResourceHandlerUtil.insertStacking(inv, ItemResource.of(stack), stack.getCount(), tx);
            if (!simulate) tx.commit();
            return stack.copyWithCount(stack.getCount() - moved);
        }
    }
    public static ItemStack insertStackAt(Level world, BlockPos pos, Direction side, ItemStack stack, boolean simulate) {
        var inv = getItemHandlerAt(world, pos, side);
        return inv != null ? insertStacked(inv, stack, simulate) : stack;
    }
    /** ThaumcraftInvHelper.hasRoomFor: how much of the stack fits. */
    public static ItemStack hasRoomFor(Level world, BlockPos pos, Direction side, ItemStack stack) {
        ItemStack test = insertStackAt(world, pos, side, stack.copy(), true);
        if (test.isEmpty()) return stack.copy();
        return stack.copyWithCount(stack.getCount() - test.getCount());
    }
    public static boolean hasRoomForSome(Level world, BlockPos pos, Direction side, ItemStack stack) {
        ItemStack test = insertStackAt(world, pos, side, stack.copy(), true);
        return stack.getCount() == 0 || test.getCount() != stack.getCount();
    }

    public static ItemStack copyLimitedStack(ItemStack stack, int limit) {
        if (stack == null) return ItemStack.EMPTY;
        ItemStack s = stack.copy();
        if (s.getCount() > limit) s.setCount(limit);
        return s;
    }

    /** InventoryUtils.removeStackFrom: takes up to stack.count matching items, returns what was taken. */
    public static ItemStack removeStackFrom(@Nullable ResourceHandler<ItemResource> inv, ItemStack stack, InvFilter filter, boolean simulate) {
        int amount = stack.getCount(), removed = 0;
        ItemStack sample = ItemStack.EMPTY;
        if (inv != null) {
            try (var tx = Transaction.openRoot()) {
                for (int a = 0; a < inv.size() && removed < amount; a++) {
                    ItemResource res = inv.getResource(a);
                    ItemStack in = res.toStack(inv.getAmountAsInt(a));
                    if (!in.isEmpty() && areItemStacksEqual(stack, in, filter)) {
                        int got = inv.extract(a, res, Math.min(amount - removed, in.getCount()), tx);
                        if (got > 0 && sample.isEmpty()) sample = in.copyWithCount(1);
                        removed += got;
                    }
                }
                if (!simulate) tx.commit();
            }
        }
        if (removed == 0) return ItemStack.EMPTY;
        ItemStack out = (sample.isEmpty() ? stack : sample).copy();
        out.setCount(removed);
        return out;
    }
    public static ItemStack removeStackFrom(Level world, BlockPos pos, Direction side, ItemStack stack, InvFilter filter, boolean simulate) {
        return removeStackFrom(getItemHandlerAt(world, pos, side), stack, filter, simulate);
    }

    /** InventoryUtils.ejectStackAt(world, pos, side, out, smart): insert into the inventory beyond the face, otherwise throw it out. */
    public static ItemStack ejectStackAt(Level world, BlockPos pos, Direction side, ItemStack out, boolean smart) {
        out = insertStackAt(world, pos.relative(side), side.getOpposite(), out, false);
        if (smart && getItemHandlerAt(world, pos.relative(side), side.getOpposite()) != null) return out;
        if (!out.isEmpty()) {
            BlockPos p = pos;
            if (world.getBlockState(pos.relative(side)).isCollisionShapeFullBlock(world, pos.relative(side))) p = pos.relative(side.getOpposite());
            var e = new ItemEntity(world, p.getX() + 0.5 + side.getStepX(), p.getY() + side.getStepY(), p.getZ() + 0.5 + side.getStepZ(), out);
            e.setDeltaMovement(0.3 * side.getStepX(), 0.3 * side.getStepY(), 0.3 * side.getStepZ());
            world.addFreshEntity(e);
        }
        return ItemStack.EMPTY;
    }
    public static void ejectStackAt(Level world, BlockPos pos, Direction side, ItemStack out) { ejectStackAt(world, pos, side, out, false); }

    public static void dropItemAtEntity(Level world, ItemStack item, Entity entity) {
        if (!world.isClientSide() && item != null && !item.isEmpty()) world.addFreshEntity(new ItemEntity(world, entity.getX(), entity.getY() + entity.getEyeHeight() / 2.0f, entity.getZ(), item.copy()));
    }

    /** InventoryUtils.countStackInWorld (TC6 counts only empty item entities, so this is always 0; kept as is). */
    public static int countStackInWorld(Level world, BlockPos pos, ItemStack stack, double range, InvFilter filter) {
        int count = 0;
        List<ItemEntity> l = world.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(range));
        for (ItemEntity ei : l) if (ei.getItem().isEmpty() && areItemStacksEqual(stack, ei.getItem(), filter)) count += ei.getItem().getCount();
        return count;
    }

    /** findFirstMatchFromFilter over an inventory (SealEmpty), optionally leaving one of each. */
    public static ItemStack findFirstMatchFromFilter(NonNullList<ItemStack> filterStacks, boolean blacklist, @Nullable ResourceHandler<ItemResource> inv, Direction face, InvFilter filter, boolean leaveOne) {
        if (inv == null) return ItemStack.EMPTY;
        outer:
        for (int a = 0; a < inv.size(); a++) {
            ItemStack is = inv.getResource(a).toStack(inv.getAmountAsInt(a));
            if (is.isEmpty()) continue;
            if (leaveOne && countTotalItemsIn(inv, is, filter) < 2) continue;
            boolean allow = false, allEmpty = true;
            for (ItemStack fs : filterStacks) {
                if (fs.isEmpty()) continue;
                allEmpty = false;
                boolean r = areItemStacksEqual(fs, is, filter);
                if (blacklist) { if (r) continue outer; allow = true; }
                else if (r) return is;
            }
            if (blacklist && (allow || allEmpty)) return is;
        }
        return ItemStack.EMPTY;
    }

    public static boolean matchesFilters(NonNullList<ItemStack> list, boolean blacklist, ItemStack is, InvFilter filter) {
        if (is == null || is.isEmpty()) return false;
        boolean allow = false, allEmpty = true;
        for (ItemStack fs : list) {
            if (fs.isEmpty()) continue;
            allEmpty = false;
            boolean r = areItemStacksEqual(fs, is, filter);
            if (blacklist) { if (r) return false; allow = true; }
            else if (r) return true;
        }
        return blacklist && (allow || allEmpty);
    }

    public record Match(ItemStack stack, int size) {}
    /** findFirstMatchFromFilterTuple over a list of stacks (the golem's carried items). */
    public static Match findFirstMatchFromFilterTuple(NonNullList<ItemStack> filterStacks, NonNullList<Integer> sizes, boolean blacklist, List<ItemStack> stacks, InvFilter filter) {
        outer:
        for (ItemStack is : stacks) {
            if (is == null || is.isEmpty()) continue;
            boolean allow = false, allEmpty = true;
            for (int idx = 0; idx < filterStacks.size(); idx++) {
                ItemStack fs = filterStacks.get(idx);
                if (fs.isEmpty()) continue;
                allEmpty = false;
                boolean r = areItemStacksEqual(fs, is, filter);
                if (blacklist) { if (r) continue outer; allow = true; }
                else if (r) return new Match(is, sizes.get(idx));
            }
            if (blacklist && (allow || allEmpty)) return new Match(is, 0);
        }
        return new Match(ItemStack.EMPTY, 0);
    }
    public static ItemStack findFirstMatchFromFilter(NonNullList<ItemStack> filterStacks, NonNullList<Integer> sizes, boolean blacklist, List<ItemStack> stacks, InvFilter filter) {
        return findFirstMatchFromFilterTuple(filterStacks, sizes, blacklist, stacks, filter).stack();
    }

    // ---- golem builder: InventoryUtils.consumeItemsFromAdjacentInventoryOrPlayer
    public static boolean checkAdjacentChests(Level world, BlockPos pos, ItemStack stack) {
        int c = stack.getCount();
        for (Direction face : Direction.values()) {
            if (face == Direction.UP) continue;
            c -= countTotalItemsIn(world, pos.relative(face), face.getOpposite(), stack.copy(), InvFilter.BASEORE);
            if (c <= 0) return true;
        }
        return false;
    }
    public static boolean consumeFromAdjacentChests(Level world, BlockPos pos, ItemStack stack) {
        for (Direction face : Direction.values()) {
            if (face == Direction.UP || stack.isEmpty()) continue;
            ItemStack os = removeStackFrom(world, pos.relative(face), face.getOpposite(), stack, InvFilter.BASEORE, false);
            stack.setCount(stack.getCount() - os.getCount());
            if (stack.isEmpty()) break;
        }
        return stack.isEmpty();
    }
    public static boolean isPlayerCarryingAmount(Player player, ItemStack stack, boolean ore) {
        if (stack == null || stack.isEmpty()) return false;
        int count = stack.getCount();
        var f = new InvFilter(false, stack.getComponentsPatch().isEmpty(), ore, false).setRelaxedNBT();
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (areItemStacksEqual(s, stack, f)) { count -= s.getCount(); if (count <= 0) return true; }
        }
        return false;
    }
    public static boolean consumePlayerItem(Player player, ItemStack item, boolean nocheck, boolean ore) {
        if (!nocheck && !isPlayerCarryingAmount(player, item, ore)) return false;
        int count = item.getCount();
        var inv = player.getInventory().getNonEquipmentItems();
        var f = new InvFilter(false, item.getComponentsPatch().isEmpty(), ore, false).setRelaxedNBT();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.get(i);
            if (areItemStacksEqual(s, item, f)) {
                if (s.getCount() > count) { s.shrink(count); count = 0; }
                else { count -= s.getCount(); inv.set(i, ItemStack.EMPTY); }
                if (count <= 0) return true;
            }
        }
        return false;
    }
    public static boolean consumeItemsFromAdjacentInventoryOrPlayer(Level world, BlockPos pos, @Nullable Player player, boolean sim, ItemStack... items) {
        for (ItemStack stack : items) {
            boolean b = checkAdjacentChests(world, pos, stack);
            if (!b && player != null) b = isPlayerCarryingAmount(player, stack, true);
            if (!b) return false;
        }
        if (!sim) for (ItemStack stack : items) {
            if (!consumeFromAdjacentChests(world, pos, stack.copy()) && player != null) consumePlayerItem(player, stack, true, true);
        }
        return true;
    }
    private InvHelper() {}
}
