package art.arcane.thaumcraft.block.eldritch;

import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Which maze this portal leads to; exit portals lead back to the altar. */
public final class EldritchPortalBlockEntity extends BlockEntity {
    private int mazeId = -1;
    private boolean exit;
    public EldritchPortalBlockEntity(BlockPos pos, BlockState s) { super(ModBlockEntities.ELDRITCH_PORTAL.get(), pos, s); }
    public void configure(int id, boolean exit) { this.mazeId = id; this.exit = exit; setChanged(); }
    public int mazeId() { return mazeId; }
    public boolean exit() { return exit; }
    @Override protected void saveAdditional(ValueOutput o) { super.saveAdditional(o); o.putInt("maze", mazeId); o.putBoolean("exit", exit); }
    @Override protected void loadAdditional(ValueInput i) { super.loadAdditional(i); mazeId = i.getIntOr("maze", -1); exit = i.getBooleanOr("exit", false); }
}
