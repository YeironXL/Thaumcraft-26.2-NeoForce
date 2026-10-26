package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.block.DioptraBlock;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** TC6 TileDioptra: every second samples vis (enabled) or flux (disabled) of the 13x13 chunks around it, 0..64 per cell. */
public final class DioptraBlockEntity extends BlockEntity {
    public int[] grid = new int[169];
    public int counter;

    public DioptraBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.DIOPTRA.get(), pos, state); }

    public static void tick(Level world, BlockPos pos, BlockState state, DioptraBlockEntity te) {
        ++te.counter;
        if (!(world instanceof ServerLevel sl)) return;
        if (te.counter % 20 != 0) return;
        boolean vis = state.getValue(DioptraBlock.ENABLED);
        for (int xx = 0; xx < 13; ++xx) for (int zz = 0; zz < 13; ++zz) {
            BlockPos p = pos.offset((xx - 6) * 16, 0, (zz - 6) * 16);
            float v = vis ? NativeAura.vis(sl, p) : NativeAura.flux(sl, p);
            te.grid[xx + zz * 13] = (int) Math.min(64.0f, v / 500.0f * 64.0f);
        }
        te.setChanged();
        world.sendBlockUpdated(pos, state, state, 3);
    }

    public int comparator() {
        float r = grid[84] / 64.0f;
        return (int) Math.floor(r * 14.0f) + (r > 0 ? 1 : 0);
    }

    @Override protected void saveAdditional(ValueOutput o) { super.saveAdditional(o); o.putIntArray("grid_a", grid); }
    @Override protected void loadAdditional(ValueInput i) {
        super.loadAdditional(i);
        i.getIntArray("grid_a").filter(a -> a.length == 169).ifPresent(a -> grid = a);
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
