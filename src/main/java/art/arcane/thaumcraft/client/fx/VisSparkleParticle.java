package art.arcane.thaumcraft.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/** Port of TC6 FXVisSparkle: a sparkle homing on a target point, shrinking as it arrives. */
public final class VisSparkleParticle extends SheetParticle {
    private final double tx, ty, tz;
    private final float sizeMod;
    private float particleScale = 0;

    public VisSparkleParticle(ClientLevel level, double x, double y, double z, double tx, double ty, double tz) {
        super(level, x, y, z);
        this.tx = tx; this.ty = ty; this.tz = tz;
        lifetime = 1000;
        float f3 = 0.01f;
        xd = (float) random.nextGaussian() * f3; yd = (float) random.nextGaussian() * f3; zd = (float) random.nextGaussian() * f3;
        sizeMod = 45 + random.nextInt(15);
        rCol = 0.2f; gCol = 0.6f + random.nextFloat() * 0.3f; bCol = 0.2f;
        gravity = 0.2f;
        alpha = 0.5f;
        hasPhysics = true;
    }

    public void color(float r, float g, float b) { rCol = r; gCol = g; bCol = b; }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;
        move(xd, yd, zd);
        xd *= 0.985; yd *= 0.985; zd *= 0.985;
        double dx = tx - x, dy = ty - y, dz = tz - z;
        double d14 = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d14 < 2.0) particleScale *= 0.95f;
        if (d14 < 0.2) lifetime = age;
        if (age < 10) particleScale = age / sizeMod;
        dx /= d14; dy /= d14; dz /= d14;
        xd += dx * 0.1; yd += dy * 0.1; zd += dz * 0.1;
        xd = Mth.clamp(xd, -0.1, 0.1); yd = Mth.clamp(yd, -0.1, 0.1); zd = Mth.clamp(zd, -0.1, 0.1);
        index = 512 + age % 16;
        if (age++ >= lifetime) remove();
    }

    @Override public float getQuadSize(float pt) { return 0.1f * particleScale * (Mth.sin(age / 3.0f) * 0.3f + 6.0f); }
}
