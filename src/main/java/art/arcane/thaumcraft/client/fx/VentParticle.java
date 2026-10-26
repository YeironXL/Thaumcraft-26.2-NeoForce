package art.arcane.thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/** TC6 FXVent / FXVent2: a puff that grows (x1.15 / x1.2 per tick) up to its scale, drifting and fading as it grows. */
public final class VentParticle extends SheetParticle {
    private float psm = 1.0f, particleScale;
    private final float grav;
    private final float baseAlpha;

    public VentParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, int color, float scale, boolean v2) {
        super(level, x, y, z);
        layer = 1;
        setSize(0.02f, 0.02f);
        particleScale = random.nextFloat() * 0.1f + 0.05f;
        xd = vx; yd = vy; zd = vz;
        float cr = ((color >> 16) & 255) / 255f, cg = ((color >> 8) & 255) / 255f, cb = (color & 255) / 255f;
        if (v2) {
            cr = (float) Mth.clamp(cr + random.nextGaussian() * 0.05, 0, 1);
            cb = (float) Mth.clamp(cb + random.nextGaussian() * 0.05, 0, 1);
            cg = (float) Mth.clamp(cg + random.nextGaussian() * 0.05, 0, 1);
            grav = (float) (random.nextGaussian() * 0.0075);
        } else {
            grav = 0.0025f;
            heading(0.125f, 5.0f);
        }
        rCol = cr; gCol = cg; bCol = cb;
        this.v2 = v2;
        baseAlpha = 0.4f;
        particleScale *= scale; psm *= scale;
        lifetime = 1000;
        var cam = Minecraft.getInstance().getCameraEntity();
        int visible = 50;
        if (cam != null && cam.distanceToSqr(x, y, z) > visible * visible) lifetime = 0;
        hasPhysics = true;
        index = 1;
    }
    private final boolean v2;

    private void heading(float speed, float spread) {
        double f = Math.sqrt(xd * xd + yd * yd + zd * zd);
        if (f == 0) return;
        double a = xd / f, b = yd / f, c = zd / f;
        a += random.nextGaussian() * (random.nextBoolean() ? -1 : 1) * 0.0075 * spread;
        b += random.nextGaussian() * (random.nextBoolean() ? -1 : 1) * 0.0075 * spread;
        c += random.nextGaussian() * (random.nextBoolean() ? -1 : 1) * 0.0075 * spread;
        xd = a * speed; yd = b * speed; zd = c * speed;
    }

    @Override public void tick() {
        xo = x; yo = y; zo = z;
        ++age;
        if (particleScale >= psm || age > lifetime) { remove(); return; }
        yd += grav;
        move(xd, yd, zd);
        xd *= 0.85; yd *= 0.85; zd *= 0.85;
        if (particleScale < psm) particleScale *= v2 ? 1.2f : 1.15f;
        if (particleScale > psm) particleScale = psm;
        if (onGround) { xd *= 0.7; zd *= 0.7; }
        index = (int) (1.0f + particleScale / psm * 4.0f);
        alpha = baseAlpha * ((psm - particleScale) / psm);
    }
    @Override public float getQuadSize(float partial) { return 0.3f * particleScale; }
}
