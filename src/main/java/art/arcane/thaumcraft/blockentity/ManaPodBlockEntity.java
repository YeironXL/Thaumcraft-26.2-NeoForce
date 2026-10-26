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

/** TC4 TileManaPod: remembers the aspect the pod will yield. */
public final class ManaPodBlockEntity extends BlockEntity {
    private String aspect = "";
    public ManaPodBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.MANA_POD.get(), pos, state); }
    public String aspect() { return aspect; }
    public void setAspect(String a) {
        aspect = a == null ? "" : a;
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    /** Aspect colour (TC4 BlockManaPod colorMultiplier), white when unseeded. */
    public int tint() { return aspect.isEmpty() ? 0xFFFFFFFF : 0xFF000000 | art.arcane.thaumcraft.aspect.AspectRegistry.color(aspect); }
    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.putString("aspect", aspect); }
    @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); String old = aspect;
        aspect = in.getStringOr("aspect", "");
        if (level != null && level.isClientSide() && !old.equals(aspect)) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 8); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
