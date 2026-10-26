package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** TC6 TileRedstoneRelay: input threshold (1-15, default 1) and output strength (1-15, default 15). */
public final class RedstoneRelayBlockEntity extends BlockEntity {
    private int in = 1, out = 15;
    public RedstoneRelayBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.REDSTONE_RELAY.get(), pos, state); }
    public int getIn() { return in; }
    public int getOut() { return out; }
    public void increaseIn() { if (++in > 15) in = 1; sync(); }
    public void increaseOut() { if (++out > 15) out = 1; sync(); }
    private void sync() { setChanged(); if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    @Override protected void saveAdditional(ValueOutput o) { super.saveAdditional(o); o.putInt("in", in); o.putInt("out", out); }
    @Override protected void loadAdditional(ValueInput i) { super.loadAdditional(i); in = i.getIntOr("in", 1); out = i.getIntOr("out", 15); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
