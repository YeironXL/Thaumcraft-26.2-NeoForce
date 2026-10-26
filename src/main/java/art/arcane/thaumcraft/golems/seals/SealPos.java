package art.arcane.thaumcraft.golems.seals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** TC6 SealPos: a block and the face the seal is stuck on. */
public final class SealPos {
    public final BlockPos pos;
    public final Direction face;
    public SealPos(BlockPos pos, Direction face) { this.pos = pos.immutable(); this.face = face; }
    @Override public int hashCode() {
        int i = 31 * (face.ordinal() + 1) + pos.getX();
        i = 31 * i + pos.getY();
        return 31 * i + pos.getZ();
    }
    @Override public boolean equals(Object o) { return this == o || o instanceof SealPos sp && pos.equals(sp.pos) && face == sp.face; }
    @Override public String toString() { return pos + "/" + face; }
}
