package art.arcane.thaumcraft.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;

/**
 * TC6 FXSmokeSpiral: a smoke puff (sheet frames 1-5) that winds twice around a sphere of the given radius centred on the
 * origin, from the top down to the bottom over 20-30 ticks, never sinking below miny + 0.1, fading out (alpha 0.66).
 */
public final class SmokeSpiralParticle extends SheetParticle {
    private final double cx, cy, cz;
    private final float radius, particleScale;
    private final int start, miny;

    public SmokeSpiralParticle(ClientLevel level, double x, double y, double z, float radius, int start, int miny, int color) {
        super(level, x, y, z);
        layer = 1;
        cx = x; cy = y; cz = z;
        this.radius = radius; this.start = start; this.miny = miny;
        particleScale = (random.nextFloat() * 0.5f + 0.5f) * 2.0f;
        lifetime = 20 + random.nextInt(10);
        rCol = ((color >> 16) & 255) / 255f; gCol = ((color >> 8) & 255) / 255f; bCol = (color & 255) / 255f;
        hasPhysics = false;
        place(0);
        xo = this.x; yo = this.y; zo = this.z;
        alpha = 0.66f;
    }

    private void place(float t) {
        float r1 = start + 720.0f * (t / lifetime);
        float r2 = 90.0f - 180.0f * (t / lifetime);
        float mX = -Mth.sin(r1 * Mth.DEG_TO_RAD) * Mth.cos(r2 * Mth.DEG_TO_RAD) * radius;
        float mZ = Mth.cos(r1 * Mth.DEG_TO_RAD) * Mth.cos(r2 * Mth.DEG_TO_RAD) * radius;
        float mY = -Mth.sin(r2 * Mth.DEG_TO_RAD) * radius;
        setPos(cx + mX, Math.max(cy + mY, miny + 0.1f), cz + mZ);
    }

    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) { remove(); return; }
        place(age);
        alpha = 0.66f * (lifetime - age) / (float) lifetime;
        index = (int) (1.0f + age / (float) lifetime * 4.0f);
    }
    @Override public float getQuadSize(float partial) { return 0.15f * particleScale; }
}
