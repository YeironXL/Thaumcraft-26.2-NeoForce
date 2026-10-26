package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.block.LevitatorBlock;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * TC6 TileLevitator: a column of force (4/8/16/32 blocks, stopped by solid blocks) pushing items and pushable creatures
 * along its facing. Runs on both sides like the original; costs 2 x range vis-points per pushed entity per tick, buying
 * 1200 points per vis drained.
 */
public final class LevitatorBlockEntity extends BlockEntity {
    private static final int[] RANGES = {4, 8, 16, 32};
    private int range = 1, rangeActual, counter, vis;

    public LevitatorBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.LEVITATOR.get(), pos, state); }
    public int getCost() { return RANGES[range] * 2; }
    public int range() { return RANGES[range]; }

    public static void tick(Level world, BlockPos pos, BlockState state, LevitatorBlockEntity te) {
        Direction facing = state.getValue(LevitatorBlock.FACING);
        if (te.rangeActual > RANGES[te.range]) te.rangeActual = 0;
        int p = te.counter % RANGES[te.range];
        BlockPos probe = pos.relative(facing, 1 + p);
        if (world.getBlockState(probe).isSolidRender()) {
            if (1 + p < te.rangeActual) te.rangeActual = 1 + p;
            te.counter = -1;
        } else if (1 + p > te.rangeActual) te.rangeActual = 1 + p;
        ++te.counter;
        if (world instanceof ServerLevel sl && te.vis < 10) {
            te.vis += (int) (NativeAura.drainVis(sl, pos, 1.0f, false) * 1200.0f);
            te.sync();
        }
        if (te.rangeActual > 0 && te.vis > 0 && state.getValue(LevitatorBlock.ENABLED)) {
            int r = te.rangeActual;
            var box = new AABB(pos.getX() - (facing.getStepX() < 0 ? r : 0), pos.getY() - (facing.getStepY() < 0 ? r : 0), pos.getZ() - (facing.getStepZ() < 0 ? r : 0),
                    pos.getX() + 1 + (facing.getStepX() > 0 ? r : 0), pos.getY() + 1 + (facing.getStepY() > 0 ? r : 0), pos.getZ() + 1 + (facing.getStepZ() > 0 ? r : 0));
            for (Entity e : world.getEntitiesOfClass(Entity.class, box)) {
                if (!(e instanceof ItemEntity) && !e.isPushable() && !(e instanceof AbstractHorse)) continue;
                te.drawFXAt(e);
                te.drawFX(facing, 0.6);
                var m = e.getDeltaMovement();
                double mx = m.x, my = m.y, mz = m.z;
                if (e.isShiftKeyDown() && facing == Direction.UP) { if (my < 0.0) my *= 0.9; }
                else {
                    mx += 0.1f * facing.getStepX(); my += 0.1f * facing.getStepY(); mz += 0.1f * facing.getStepZ();
                    if (facing.getAxis() != Direction.Axis.Y && !e.onGround()) { if (my < 0.0) my *= 0.9; my += 0.08; }
                    mx = Math.clamp(mx, -0.35, 0.35); my = Math.clamp(my, -0.35, 0.35); mz = Math.clamp(mz, -0.35, 0.35);
                }
                e.setDeltaMovement(mx, my, mz);
                e.fallDistance = 0.0f;
                te.vis -= te.getCost();
                if (te.vis <= 0) break;
            }
            te.drawFX(facing, 0.1);
        }
    }

    private void drawFX(Direction facing, double c) {
        if (level.isClientSide() && level.getRandom().nextFloat() < c) {
            var r = level.getRandom();
            levitatorParticle(worldPosition.getX() + 0.25f + r.nextFloat() * 0.5f, worldPosition.getY() + 0.25f + r.nextFloat() * 0.5f, worldPosition.getZ() + 0.25f + r.nextFloat() * 0.5f,
                    facing.getStepX() / 50.0, facing.getStepY() / 50.0, facing.getStepZ() / 50.0);
        }
    }
    private void drawFXAt(Entity e) {
        if (level.isClientSide() && level.getRandom().nextFloat() < 0.1f) {
            var r = level.getRandom();
            levitatorParticle(e.getX() + (r.nextFloat() - r.nextFloat()) * e.getBbWidth(), e.getY() + r.nextFloat() * e.getBbHeight(), e.getZ() + (r.nextFloat() - r.nextFloat()) * e.getBbWidth(),
                    (r.nextFloat() - r.nextFloat()) * 0.01, (r.nextFloat() - r.nextFloat()) * 0.01, (r.nextFloat() - r.nextFloat()) * 0.01);
        }
    }
    /** FXDispatcher.drawLevitatorParticles. */
    private void levitatorParticle(double x, double y, double z, double vx, double vy, double vz) {
        var r = level.getRandom();
        art.arcane.thaumcraft.client.fx.FXDispatcher.generic(x, y, z, vx, vy, vz, GenericParticleOptions.builder().maxAge(200 + r.nextInt(100)).color(0.5f, 0.5f, 0.2f)
                .alpha(0.3f, 0.0f).grid(16).particles(56, 1, 1).scale(2.0f, 5.0f).layer(0).slowDown(1.0).rotation(r.nextFloat(), r.nextBoolean() ? -1.0f : 1.0f).build(), 0);
    }

    public void increaseRange(Player player) {
        rangeActual = 0;
        if (!level.isClientSide()) {
            if (++range >= RANGES.length) range = 0;
            sync();
            player.sendSystemMessage(Component.translatable("tc.levitator", RANGES[range], getCost()));
        }
    }

    private void sync() { setChanged(); if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    @Override protected void saveAdditional(ValueOutput o) { super.saveAdditional(o); o.putInt("range", range); o.putInt("vis", vis); }
    @Override protected void loadAdditional(ValueInput i) { super.loadAdditional(i); range = Math.clamp(i.getIntOr("range", 1), 0, 3); vis = i.getIntOr("vis", 0); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
