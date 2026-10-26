package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.aura.NativeAura;
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

/**
 * TC6 TileStabilizer: charges one energy per second up to 15 (polluting 0.25 flux each time) and feeds it to the inlay
 * network, and every 5 ticks spends it stabilising nearby flux rifts (tryAddStability).
 */
public final class StabilizerBlockEntity extends BlockEntity {
    private int ticks, delay, lastEnergy;
    protected int energy;

    public StabilizerBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.STABILIZER.get(), pos, state); }
    public int getEnergy() { return energy; }

    public static void tick(Level world, BlockPos pos, BlockState state, StabilizerBlockEntity te) {
        if (world instanceof ServerLevel sl) {
            ++te.ticks;
            if (te.energy < 15 && te.ticks % 20 == 0) {
                ++te.energy;
                NativeAura.addFlux(sl, pos, 0.25f);
                te.sync();
                world.updateNeighborsAt(pos, state.getBlock());
            }
            if (te.energy > 0 && te.delay <= 0 && te.ticks % 5 == 0) te.tryAddStability();
            if (te.delay > 0) --te.delay;
        } else if (te.energy != te.lastEnergy) {
            world.sendBlockUpdated(pos, state, state, 8);
            te.lastEnergy = te.energy;
        }
    }

    private void tryAddStability() {
        var targets = level.getEntitiesOfClass(art.arcane.thaumcraft.entity.FluxRiftEntity.class, new net.minecraft.world.phys.AABB(worldPosition).inflate(8.0));
        for (var e : targets) {
            if (!e.isAlive()) continue;
            if (e.getStability() == art.arcane.thaumcraft.entity.FluxRiftEntity.EnumStability.VERY_STABLE || !mitigate(1)) continue;
            e.addStability();
            delay += 5;
            if (energy <= 0) return;
        }
    }

    public boolean mitigate(int e) {
        if (energy >= e) {
            energy -= e;
            sync();
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            return true;
        }
        return false;
    }

    private void sync() { setChanged(); if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    @Override protected void saveAdditional(ValueOutput o) { super.saveAdditional(o); o.putInt("energy", energy); }
    @Override protected void loadAdditional(ValueInput i) { super.loadAdditional(i); energy = Math.min(i.getIntOr("energy", 0), 15); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
