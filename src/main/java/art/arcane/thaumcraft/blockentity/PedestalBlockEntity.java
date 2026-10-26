package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** TC6 TilePedestal: a one-item inventory synced to clients (rendered floating above the pedestal). */
public class PedestalBlockEntity extends BlockEntity implements Container {
    protected ItemStack item = ItemStack.EMPTY;

    public PedestalBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.PEDESTAL.get(), pos, state); }
    protected PedestalBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    /** Render scale of the floating item (TC6 1.25 for pedestals, 1.5 for the recharge pedestal). */
    public float renderScale() { return 1.25f; }

    public ItemStack item() { return item; }
    public void setInventorySlotContentsFromInfusion(ItemStack stack) { setItem(0, stack); }

    protected void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return item.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? item : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int count) {
        if (slot != 0 || item.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = item.split(count); sync(); return out;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { ItemStack out = item; item = ItemStack.EMPTY; return out; }
    @Override public void setItem(int slot, ItemStack stack) { if (slot == 0) { item = stack; sync(); } }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.isEmpty() || item.isEmpty(); }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { item = ItemStack.EMPTY; sync(); }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!item.isEmpty()) out.store("item", ItemStack.CODEC, item);
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        item = in.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }

    /** receiveClientEvent: 11 item ejected by instability (pink bamf), 12 infusion finished (bamf), 5 discharge absorbed by a stabilizer (shield runes). */
    @Override public boolean triggerEvent(int id, int param) {
        if (id == 11 || id == 12 || id == 5) {
            if (level != null && level.isClientSide()) art.arcane.thaumcraft.client.fx.InfusionFx.pedestalEvent(worldPosition, id);
            return true;
        }
        return super.triggerEvent(id, param);
    }

    /** TilePedestal.findInstabilityMitigator: follow rising inlay charge to a stabilizer strong enough (5+) to absorb a discharge. */
    public net.minecraft.core.BlockPos findInstabilityMitigator() {
        int charge = getBlockState().hasProperty(art.arcane.thaumcraft.block.InlayBlock.CHARGE) ? getBlockState().getValue(art.arcane.thaumcraft.block.InlayBlock.CHARGE) : 0;
        return charge > 0 ? seekSourceRecursive(worldPosition, charge) : null;
    }
    private net.minecraft.core.BlockPos seekSourceRecursive(net.minecraft.core.BlockPos pos, int lastCharge) {
        for (var face : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            var pp = pos.relative(face);
            if (art.arcane.thaumcraft.block.InlayBlock.getSourceStrengthAt(level, pp) >= 5) return pp;
            var bs = level.getBlockState(pp);
            if (bs.hasProperty(art.arcane.thaumcraft.block.InlayBlock.CHARGE)) {
                int charge = bs.getValue(art.arcane.thaumcraft.block.InlayBlock.CHARGE);
                if (charge > lastCharge) {
                    var ob = seekSourceRecursive(pp, charge);
                    if (ob != null) return ob;
                }
            }
        }
        return null;
    }
}
