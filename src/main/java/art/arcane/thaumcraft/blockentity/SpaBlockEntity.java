package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.item.BathSaltsItem;
import art.arcane.thaumcraft.menu.SpaMenu;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * TC6 TileSpa: holds bath salts and a 5000 mB tank. Every 40 ticks (not powered) it pours 1000 mB as a source block on
 * top, then spreads to adjacent replaceable spots within 2 blocks. "Mix" uses one bath salt + water to make purifying
 * fluid; otherwise it places the tank's own fluid.
 */
public final class SpaBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    private ItemStack salts = ItemStack.EMPTY;
    private boolean mix = true;
    private int counter;
    public final FluidStacksResourceHandler tank = new FluidStacksResourceHandler(1, 5000) {
        @Override protected void onContentsChanged(int index, FluidStack previous) { sync(); }
    };

    public SpaBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.SPA.get(), pos, state); }

    public boolean getMix() { return mix; }
    public void toggleMix() { mix = !mix; sync(); }
    public FluidStack fluid() { return tank.copyToList().getFirst(); }

    public static void tick(Level world, BlockPos pos, BlockState state, SpaBlockEntity te) {
        if (world.isClientSide() || te.counter++ % 40 != 0 || world.hasNeighborSignal(pos) || !te.hasIngredients()) return;
        BlockState above = world.getBlockState(pos.above());
        Block tb = te.mix ? ModBlocks.PURIFYING_FLUID.get() : te.fluid().getFluid().defaultFluidState().createLegacyBlock().getBlock();
        if (above.is(tb) && above.getFluidState().isSource()) {
            for (int xx = -2; xx <= 2; ++xx) for (int zz = -2; zz <= 2; ++zz) {
                BlockPos p = pos.offset(xx, 1, zz);
                if (te.isValidLocation(p, true, tb)) {
                    te.consumeIngredients();
                    world.setBlockAndUpdate(p, tb.defaultBlockState());
                    return;
                }
            }
        } else if (te.isValidLocation(pos.above(), false, tb)) {
            te.consumeIngredients();
            world.setBlockAndUpdate(pos.above(), tb.defaultBlockState());
        }
    }

    private boolean hasIngredients() {
        FluidStack fs = fluid();
        if (mix) return fs.getFluid() == Fluids.WATER && fs.getAmount() >= 1000 && salts.getItem() instanceof BathSaltsItem;
        return !fs.isEmpty() && fs.getFluid().defaultFluidState().createLegacyBlock().getBlock() != Blocks.AIR && fs.getAmount() >= 1000;
    }

    private void consumeIngredients() {
        if (mix) { salts.shrink(1); setChanged(); }
        try (var tx = Transaction.openRoot()) {
            tank.extract(0, FluidResource.of(fluid()), 1000, tx);
            tx.commit();
        }
    }

    private boolean isValidLocation(BlockPos p, boolean mustBeAdjacent, Block target) {
        if (target == Blocks.WATER && level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, p)) return false;
        BlockState bs = level.getBlockState(p);
        return level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP) && bs.canBeReplaced()
                && !(bs.is(target) && bs.getFluidState().isSource())
                && (!mustBeAdjacent || touchingSource(p, target));
    }

    /** BlockUtils.isBlockTouching with the target's metadata-0 (source) state. */
    private boolean touchingSource(BlockPos p, Block target) {
        for (Direction d : Direction.values()) { BlockState s = level.getBlockState(p.relative(d)); if (s.is(target) && s.getFluidState().isSource()) return true; }
        return false;
    }

    void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // ---- inventory: slot 0 bath salts, accessible from every side but the top ----
    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return salts.isEmpty(); }
    @Override public ItemStack getItem(int i) { return salts; }
    @Override public ItemStack removeItem(int i, int n) { ItemStack r = salts.split(n); setChanged(); return r; }
    @Override public ItemStack removeItemNoUpdate(int i) { ItemStack r = salts; salts = ItemStack.EMPTY; return r; }
    @Override public void setItem(int i, ItemStack s) { salts = s; setChanged(); }
    @Override public boolean canPlaceItem(int i, ItemStack s) { return s.getItem() instanceof BathSaltsItem; }
    @Override public boolean stillValid(Player p) { return Container.stillValidBlockEntity(this, p); }
    @Override public void clearContent() { salts = ItemStack.EMPTY; }
    @Override public int[] getSlotsForFace(Direction side) { return side != Direction.UP ? new int[] {0} : new int[0]; }
    @Override public boolean canPlaceItemThroughFace(int i, ItemStack s, Direction side) { return side != Direction.UP && canPlaceItem(i, s); }
    @Override public boolean canTakeItemThroughFace(int i, ItemStack s, Direction side) { return side != Direction.UP; }

    @Override public Component getDisplayName() { return Component.translatable("block.thaumcraft.spa"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new SpaMenu(id, inv, this); }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!salts.isEmpty()) out.store("salts", ItemStack.CODEC, salts);
        out.putBoolean("mix", mix);
        tank.serialize(out);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        salts = in.read("salts", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        mix = in.getBooleanOr("mix", true);
        tank.deserialize(in);
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
