package art.arcane.thaumcraft.crafting.multiblock;

import java.util.function.Predicate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * TC6 api.crafting.Part. source: a Block (any state), a BlockState (exact), or a Predicate&lt;BlockState&gt; (TC6 Material).
 * target: a Block (horizontal FACING applied when present), a BlockState (TC6 ItemStack with metadata), the String "AIR",
 * or null (left untouched).
 */
public final class Part {
    private final Object source;
    private final Object target;
    private final boolean opp;
    private final int priority;
    private boolean applyPlayerFacing;

    public Part(Object source, Object target, boolean opp, int priority) { this.source = source; this.target = target; this.opp = opp; this.priority = priority; }
    public Part(Object source, Object target, boolean opp) { this(source, target, opp, 50); }
    public Part(Object source, Object target) { this(source, target, false, 50); }

    public Object getSource() { return source; }
    public Object getTarget() { return target; }
    public boolean isOpp() { return opp; }
    public int getPriority() { return priority; }
    public boolean getApplyPlayerFacing() { return applyPlayerFacing; }
    public Part setApplyPlayerFacing(boolean b) { applyPlayerFacing = b; return this; }

    @SuppressWarnings("unchecked")
    boolean matches(BlockState bs) {
        if (source instanceof Block b) return bs.getBlock() == b;
        if (source instanceof BlockState s) return bs == s;
        if (source instanceof Predicate<?> p) return ((Predicate<BlockState>) p).test(bs);
        return true;
    }
}
