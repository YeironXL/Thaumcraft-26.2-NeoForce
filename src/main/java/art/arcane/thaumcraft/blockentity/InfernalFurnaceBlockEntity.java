package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.block.EssentiaBellowsBlock;
import art.arcane.thaumcraft.block.InfernalFurnaceBlock;
import art.arcane.thaumcraft.crafting.SmeltingBonus;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.util.InventoryUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 TileInfernalFurnace: 32 slot buffer fed through the lava top; smelts one item every 140 ticks (80 while vis-boosted,
 * minus up to 4 unpowered bellows two blocks away), ejecting results plus smelting bonuses out of the front (iron bars side).
 */
public final class InfernalFurnaceBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final int[] SLOTS = java.util.stream.IntStream.range(0, 32).toArray();
    private final NonNullList<ItemStack> items = NonNullList.withSize(32, ItemStack.EMPTY);
    public int furnaceCookTime, furnaceMaxCookTime, speedyTime;

    public InfernalFurnaceBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.INFERNAL_FURNACE.get(), pos, state); }

    private Direction facing() { return getBlockState().getValue(InfernalFurnaceBlock.FACING); }

    public static void tick(Level world, BlockPos pos, BlockState state, InfernalFurnaceBlockEntity te) {
        if (!(world instanceof ServerLevel sl)) return;
        boolean cookedflag = false;
        if (te.furnaceCookTime > 0) { --te.furnaceCookTime; cookedflag = true; }
        if (te.furnaceMaxCookTime <= 0) te.furnaceMaxCookTime = te.calcCookTime();
        if (te.furnaceCookTime > te.furnaceMaxCookTime) te.furnaceCookTime = te.furnaceMaxCookTime;
        if (te.furnaceCookTime <= 0 && cookedflag) {
            for (int a = 0; a < te.items.size(); ++a) {
                ItemStack in = te.items.get(a);
                if (in.isEmpty()) continue;
                var recipe = te.smelting(sl, in);
                if (recipe.isPresent()) {
                    if (te.speedyTime > 0) --te.speedyTime;
                    te.ejectItem(sl, recipe.get().value().assemble(new SingleRecipeInput(in)), in, recipe.get().value().experience());
                    sl.blockEvent(pos, state.getBlock(), 3, 0);
                    if (sl.getRandom().nextInt(20) == 0) NativeAura.addFlux(sl, pos.relative(te.facing().getOpposite()), 1.0f);
                    in.shrink(1);
                    te.setChanged();
                    break;
                }
                te.items.set(a, ItemStack.EMPTY);
            }
        }
        if (te.speedyTime <= 0) te.speedyTime = (int) NativeAura.drainVis(sl, pos, 20.0f, false);
        if (te.furnaceCookTime == 0 && !cookedflag) {
            for (ItemStack s : te.items) {
                if (te.canSmelt(sl, s)) { te.furnaceMaxCookTime = te.calcCookTime(); te.furnaceCookTime = te.furnaceMaxCookTime; break; }
            }
        }
    }

    private Optional<RecipeHolder<SmeltingRecipe>> smelting(ServerLevel sl, ItemStack s) {
        if (s.isEmpty()) return Optional.empty();
        return sl.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(s), sl);
    }
    private boolean canSmelt(ServerLevel sl, ItemStack s) { return smelting(sl, s).isPresent(); }

    private int getBellows() {
        int bellows = 0;
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP) continue;
            BlockPos p2 = worldPosition.relative(dir, 2);
            BlockState bs = level.getBlockState(p2);
            if (bs.is(ModBlocks.BELLOWS.get()) && bs.getValue(EssentiaBellowsBlock.FACING) == dir.getOpposite() && level.getBestNeighborSignal(p2) == 0) ++bellows;
        }
        return Math.min(4, bellows);
    }
    private int calcCookTime() {
        int b = getBellows();
        if (b > 0) b *= 20 - (b - 1);
        return Math.max(10, (speedyTime > 0 ? 80 : 140) - b);
    }

    /** addItemsToInventory: smeltables go into the buffer, anything else is destroyed with a hiss. */
    public ItemStack addItemsToInventory(ServerLevel sl, ItemStack stack) {
        if (canSmelt(sl, stack)) {
            for (int i = 0; i < items.size() && !stack.isEmpty(); ++i) {
                ItemStack cur = items.get(i);
                if (cur.isEmpty()) { items.set(i, stack.copy()); stack = ItemStack.EMPTY; }
                else if (ItemStack.isSameItemSameComponents(cur, stack)) {
                    int move = Math.min(stack.getCount(), cur.getMaxStackSize() - cur.getCount());
                    if (move > 0) { cur.grow(move); stack.shrink(move); }
                }
            }
            setChanged();
            return stack;
        }
        var r = sl.getRandom();
        sl.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 2.6f + (r.nextFloat() - r.nextFloat()) * 0.8f);
        sl.sendParticles(ParticleTypes.LAVA, worldPosition.getX() + r.nextFloat(), worldPosition.getY() + 1, worldPosition.getZ() + r.nextFloat(), 1, 0, 0, 0, 0);
        return ItemStack.EMPTY;
    }

    private void ejectItem(ServerLevel sl, ItemStack result, ItemStack source, float xpf) {
        if (result.isEmpty()) return;
        List<ItemStack> eject = new ArrayList<>();
        eject.add(result.copy());
        int bellows = getBellows() + 1;
        for (int a = 0; a < bellows; ++a) eject.addAll(SmeltingBonus.roll(source, sl.getRandom()));
        Direction out = facing().getOpposite();
        for (ItemStack s : eject) if (!s.isEmpty()) InventoryUtils.ejectStackAt(sl, worldPosition, out, s);
        int cnt = result.getCount();
        if (xpf == 0.0f) cnt = 0;
        else if (xpf < 1.0f) {
            int v = Mth.floor(cnt * xpf);
            if (v < Mth.ceil(cnt * xpf) && (float) Math.random() < cnt * xpf - v) ++v;
            cnt = v;
        }
        if (cnt > 0) {
            int fx = out.getStepX(), fz = out.getStepZ();
            Vec3 at = new Vec3(worldPosition.getX() + 0.5 + fx * 1.2, worldPosition.getY() + 0.4, worldPosition.getZ() + 0.5 + fz * 1.2);
            ExperienceOrb.awardWithDirection(sl, at, new Vec3(fx * 0.13, 0, fz * 0.13), cnt);
        }
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 3) {
            if (level != null && level.isClientSide()) {
                var r = level.getRandom();
                Direction out = facing().getOpposite();
                int fx = out.getStepX(), fz = out.getStepZ();
                for (int a = 0; a < 5; ++a) {
                    level.addParticle(ParticleTypes.LAVA, worldPosition.getX() + 0.5 + (r.nextFloat() - r.nextFloat()) * 0.3 + fx,
                            worldPosition.getY() + 0.3, worldPosition.getZ() + 0.5 + (r.nextFloat() - r.nextFloat()) * 0.3 + fz, 0, 0, 0);
                    level.playLocalSound(worldPosition, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.1f + r.nextFloat() * 0.1f, 0.9f + r.nextFloat() * 0.15f, false);
                }
            }
            return true;
        }
        return super.triggerEvent(id, type);
    }

    // ---- inventory (insertion only from the top, never extractable) ----
    @Override public int getContainerSize() { return 32; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int i) { return items.get(i); }
    @Override public ItemStack removeItem(int i, int n) { ItemStack r = ContainerHelper.removeItem(items, i, n); setChanged(); return r; }
    @Override public ItemStack removeItemNoUpdate(int i) { return ContainerHelper.takeItem(items, i); }
    @Override public void setItem(int i, ItemStack s) { items.set(i, s); setChanged(); }
    @Override public boolean canPlaceItem(int i, ItemStack s) { return level instanceof ServerLevel sl && canSmelt(sl, s); }
    @Override public boolean stillValid(Player p) { return Container.stillValidBlockEntity(this, p); }
    @Override public void clearContent() { items.clear(); }
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.UP ? SLOTS : new int[0]; }
    @Override public boolean canPlaceItemThroughFace(int i, ItemStack s, Direction side) { return side == Direction.UP && canPlaceItem(i, s); }
    @Override public boolean canTakeItemThroughFace(int i, ItemStack s, Direction side) { return false; }

    @Override protected void saveAdditional(ValueOutput o) {
        super.saveAdditional(o);
        ContainerHelper.saveAllItems(o, items);
        o.putShort("CookTime", (short) furnaceCookTime);
        o.putShort("SpeedyTime", (short) speedyTime);
    }
    @Override protected void loadAdditional(ValueInput i) {
        super.loadAdditional(i);
        items.clear();
        ContainerHelper.loadAllItems(i, items);
        furnaceCookTime = i.getShortOr("CookTime", (short) 0);
        speedyTime = i.getShortOr("SpeedyTime", (short) 0);
    }
}
