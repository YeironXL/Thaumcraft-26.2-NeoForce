package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.block.EssentiaTransfuserBlock;
import art.arcane.thaumcraft.essentia.EssentiaHandler;
import art.arcane.thaumcraft.essentia.EssentiaPort;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * TC6 TileEssentiaInput / TileEssentiaOutput. The filling transfuser pulls (suction 128) from the tube it is attached
 * to every 5 ticks and drops the point into a nearby jar. The emptying transfuser offers its attached tube whatever
 * aspect that tube asks for, taken from a nearby jar.
 */
public final class EssentiaTransfuserBlockEntity extends BlockEntity implements EssentiaPort {
    private int count;
    public EssentiaTransfuserBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.ESSENTIA_TRANSFUSER.get(), pos, state); }

    private boolean input() { return getBlockState().getBlock() instanceof EssentiaTransfuserBlock b && b.input; }
    private Direction facing() { return getBlockState().getValue(EssentiaTransfuserBlock.FACING); }

    public static void tick(Level level, BlockPos pos, BlockState state, EssentiaTransfuserBlockEntity te) {
        if (!(level instanceof ServerLevel sl) || !te.input() || ++te.count % 5 != 0) return;
        Direction f = te.facing();
        EssentiaPort ic = EssentiaTubeBlockEntity.neighbor(sl, pos, f.getOpposite());
        if (ic == null || ic.aspect().isEmpty() || ic.suction(f) >= 128 || 128 < ic.minimumSuction()) return;
        String ta = ic.offered("", f);
        if (ta == null || ta.isEmpty()) return;
        if (EssentiaHandler.addEssentia(sl, pos, ta, f, 16, 5)) ic.extractOne(ta, f);
    }

    @Override public boolean connects(Direction side) { return side == facing().getOpposite(); }
    @Override public int suction() { return input() ? 128 : 0; }
    @Override public String wanted() { return ""; }
    @Override public String aspect() { return ""; }
    @Override public String offered(String requested, Direction side) {
        if (input() || requested == null || requested.isEmpty() || !(level instanceof ServerLevel sl)) return "";
        return EssentiaHandler.findEssentia(sl, worldPosition, requested, facing(), 16) ? requested : "";
    }
    @Override public boolean extractOne(String aspect, Direction side) {
        if (input() || side != facing().getOpposite() || !(level instanceof ServerLevel sl)) return false;
        BlockPos src = EssentiaHandler.drainSource(sl, worldPosition, aspect, facing(), 16, false);
        if (src == null) return false;
        EssentiaHandler.confirmDrain(sl, worldPosition, src, aspect, 5);
        return true;
    }
}
