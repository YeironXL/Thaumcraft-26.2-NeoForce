package art.arcane.thaumcraft.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.util.RandomSource;

/** Base for TC6 particles drawing a cell of the shared particle sheet (64x64 grid unless set). */
public abstract class SheetParticle extends SingleQuadParticle {
    protected int index, grid = 64, layer = 0;
    protected SheetParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z, GenericParticle.SHEET.get(RandomSource.create()));
    }
    private float cell() { return (sprite.getU1() - sprite.getU0()) / grid; }
    @Override protected float getU0() { return sprite.getU0() + (Math.max(0, index) % grid) * cell(); }
    @Override protected float getU1() { return sprite.getU0() + (Math.max(0, index) % grid + 1) * cell(); }
    @Override protected float getV0() { return sprite.getV0() + (Math.max(0, index) / grid) * (sprite.getV1() - sprite.getV0()) / grid; }
    @Override protected float getV1() { return sprite.getV0() + (Math.max(0, index) / grid + 1) * (sprite.getV1() - sprite.getV0()) / grid; }
    @Override protected Layer getLayer() { return TcParticleLayers.layer(layer); }
}
