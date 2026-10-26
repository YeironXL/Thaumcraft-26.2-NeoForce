package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.casters.foci.FocusEffectRift;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** TC6 TileHole (TileMemory): remembers the replaced block, spreads the rift and counts down. */
public final class HoleBlockEntity extends BlockEntity {
    public BlockState oldblock = Blocks.AIR.defaultBlockState();
    public short countdown = 0, countdownmax = 120;
    public byte count = 0;
    public Direction direction = null;

    public HoleBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.HOLE.get(), pos, state); }

    public static void tick(Level world, BlockPos pos, BlockState state, HoleBlockEntity te) {
        if (world.isClientSide()) {
            for (int a = 0; a < 2; ++a) te.surroundWithSparkles();
            return;
        }
        if (te.countdown == 0 && te.count > 1 && te.direction != null) {
            for (int a = 0; a < 9; ++a) {
                if (a / 3 == 1 && a % 3 == 1) continue;
                BlockPos p = switch (te.direction.getAxis()) {
                    case Y -> pos.offset(-1 + a / 3, 0, -1 + a % 3);
                    case Z -> pos.offset(-1 + a / 3, -1 + a % 3, 0);
                    case X -> pos.offset(0, -1 + a / 3, -1 + a % 3);
                };
                FocusEffectRift.createHole(world, p, null, (byte) 1, te.countdownmax);
            }
            if (!FocusEffectRift.createHole(world, pos.relative(te.direction.getOpposite()), te.direction, (byte) (te.count - 1), te.countdownmax)) te.count = 0;
        }
        ++te.countdown;
        if (te.countdown % 20 == 0) te.setChanged();
        if (te.countdown >= te.countdownmax) world.setBlock(pos, te.oldblock, 3);
    }

    private void surroundWithSparkles() {
        var w = level; var pos = worldPosition; var r = w.getRandom();
        for (Direction d1 : Direction.values()) {
            BlockState b1 = w.getBlockState(pos.relative(d1));
            if (b1.is(ModBlocks.HOLE.get()) || b1.isSolidRender()) continue;
            for (Direction d2 : Direction.values()) {
                if (d1.getAxis() == d2.getAxis()) continue;
                if (!(w.getBlockState(pos.relative(d2)).isSolidRender() || w.getBlockState(pos.relative(d1).relative(d2)).isSolidRender())) continue;
                float sx = 0.5f * d1.getStepX(), sy = 0.5f * d1.getStepY(), sz = 0.5f * d1.getStepZ();
                if (sx == 0) sx = 0.5f * d2.getStepX();
                if (sy == 0) sy = 0.5f * d2.getStepY();
                if (sz == 0) sz = 0.5f * d2.getStepZ();
                sx = sx == 0 ? r.nextFloat() : sx + 0.5f;
                sy = sy == 0 ? r.nextFloat() : sy + 0.5f;
                sz = sz == 0 ? r.nextFloat() : sz + 0.5f;
                art.arcane.thaumcraft.client.fx.FXDispatcher.sparkle(pos.getX() + sx, pos.getY() + sy, pos.getZ() + sz, 0.25f, 0.25f, 1.0f);
            }
        }
    }

    @Override protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("oldblock", BlockState.CODEC, oldblock);
        out.putShort("countdown", countdown); out.putShort("countdownmax", countdownmax); out.putByte("count", count);
        out.putByte("direction", direction == null ? -1 : (byte) direction.ordinal());
    }
    @Override protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        oldblock = in.read("oldblock", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
        countdown = (short) in.getShortOr("countdown", (short) 0); countdownmax = (short) in.getShortOr("countdownmax", (short) 120); count = (byte) in.getByteOr("count", (byte) 0);
        byte db = (byte) in.getByteOr("direction", (byte) -1);
        direction = db >= 0 ? Direction.values()[db] : null;
    }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
