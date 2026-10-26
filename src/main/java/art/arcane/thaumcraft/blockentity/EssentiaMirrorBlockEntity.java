package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.block.MirrorBlock;
import art.arcane.thaumcraft.essentia.EssentiaHandler;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * TC6 TileMirrorEssentia: an IAspectSource that relays essentia draws (one point at a time) to the essentia sources
 * within 8 blocks in front of its linked partner. Devices pulling through EssentiaHandler (infusion matrix, etc.) can use it
 * as if it were a warded jar. Every 64 instability pollutes one flux.
 */
public class EssentiaMirrorBlockEntity extends MirrorBlockEntity {
    public Direction linkedFacing = Direction.DOWN;

    public EssentiaMirrorBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.MIRROR_ESSENTIA.get(), pos, state); }

    @Override protected int instabilityThreshold() { return 64; }

    @Override
    protected void onLinked(MirrorBlockEntity other) {
        linkedFacing = other.getBlockState().getValue(MirrorBlock.FACING);
    }
    @Override protected void onUnlinked() { linkedFacing = Direction.DOWN; }

    /** TileMirrorEssentia has no item queue (update(): only instability and links). */
    @Override
    protected void serverTick() {
        checkInstability();
        if (count++ % inc == 0) {
            if (!isLinkValidSimple()) {
                if (inc < 600) inc += 20;
                restoreLink();
            } else inc = 40;
        }
    }

    private Direction partnerFacing(ServerLevel w) {
        if (linkedFacing == Direction.DOWN && w != null) {
            BlockState bs = w.getBlockState(linkPos());
            if (bs.getBlock() instanceof MirrorBlock) linkedFacing = bs.getValue(MirrorBlock.FACING);
        }
        return linkedFacing;
    }

    /** takeFromContainer(aspect, 1): drain one point from a source in front of the partner (sources there that are mirrors are ignored). */
    public boolean takeFromContainer(String aspect, int amount) {
        if (!isLinkValid() || amount > 1) return false;
        ServerLevel w = targetWorld();
        Direction f = partnerFacing(w);
        if (partner() == null) return false;
        boolean b = EssentiaHandler.drainEssentia(w, linkPos(), aspect, f, 8, true, 5);
        if (b) addInstability(null, amount);
        return b;
    }

    /** doesContainerContainAmount(aspect, 1). */
    public boolean doesContainerContainAmount(String aspect, int amount) {
        if (!isLinkValid() || amount > 1) return false;
        ServerLevel w = targetWorld();
        Direction f = partnerFacing(w);
        return partner() != null && EssentiaHandler.findEssentia(w, linkPos(), aspect, f, 8, true);
    }
}
