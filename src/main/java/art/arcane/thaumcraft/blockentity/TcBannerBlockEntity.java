package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** TC6 TileBanner: the banner's aspect (rotation and wall are block state here); kept on the item as thaumcraft:aspect. */
public final class TcBannerBlockEntity extends BlockEntity {
    private String aspect = "";
    public TcBannerBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.BANNER.get(), pos, state); }

    public String aspect() { return aspect; }
    public void setAspect(String a) {
        aspect = a == null ? "" : a;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void collectImplicitComponents(DataComponentMap.Builder b) { super.collectImplicitComponents(b); if (!aspect.isEmpty()) b.set(ModComponents.ASPECT.get(), aspect); }
    @Override protected void applyImplicitComponents(DataComponentGetter c) { super.applyImplicitComponents(c); aspect = c.getOrDefault(ModComponents.ASPECT.get(), ""); }
    @Override public void removeComponentsFromTag(ValueOutput o) {}
    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.putString("aspect", aspect); }
    @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); aspect = in.getStringOr("aspect", ""); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
