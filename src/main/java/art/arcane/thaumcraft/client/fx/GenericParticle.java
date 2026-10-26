package art.arcane.thaumcraft.client.fx;

import art.arcane.thaumcraft.fx.GenericParticleOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Port of TC6 FXGeneric: frame index on the 1024px sheet, keyed alpha/scale, colour fade, spin, slow-down and wind. */
public final class GenericParticle extends SingleQuadParticle {
    private final GenericParticleOptions o;
    private float[] alphaFrames, scaleFrames;
    private int index;
    private float particleScale;

    public GenericParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, TextureAtlasSprite sprite, GenericParticleOptions options) {
        super(level, x, y, z, sprite);
        this.o = options;
        setSize(0.1f, 0.1f);
        xd = dx; yd = dy; zd = dz;
        lifetime = options.maxAge();
        gravity = options.gravity();
        rCol = options.r(); gCol = options.g(); bCol = options.b();
        hasPhysics = !options.noClip();
        roll = oRoll = options.rotationStart();
        particleScale = options.scale().isEmpty() ? 1f : options.scale().get(0);
        alpha = options.alpha().isEmpty() ? 1f : options.alpha().get(0);
        index = options.start();
        frames();
    }
    private static float[] keyed(java.util.List<Float> keys, int maxAge) {
        var frames = new float[maxAge + 1];
        if (keys.isEmpty()) return frames;
        float inc = (keys.size() - 1) / (float) Math.max(1, maxAge), is = 0;
        for (int a = 0; a <= maxAge; a++) {
            int f = Mth.floor(is);
            float diff = f < keys.size() - 1 ? keys.get(f + 1) - keys.get(f) : 0;
            frames[a] = keys.get(Math.min(f, keys.size() - 1)) + diff * (is - f);
            is += inc;
        }
        return frames;
    }
    private void frames() {
        alphaFrames = keyed(o.alpha(), lifetime);
        scaleFrames = keyed(o.scale(), lifetime);
    }
    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) remove();
        oRoll = roll;
        roll += (float)(Math.PI * o.rotationSpeed() * 2.0);
        yd -= 0.04 * gravity;
        move(xd, yd, zd);
        xd *= o.slowDown(); yd *= o.slowDown(); zd *= o.slowDown();
        xd += random.nextGaussian() * o.randomX(); yd += random.nextGaussian() * o.randomY(); zd += random.nextGaussian() * o.randomZ();
        xd += o.windX(); zd += o.windZ();
        if (onGround && o.slowDown() != 1.0f) { xd *= 0.7; zd *= 0.7; }
        if (o.loop()) index = o.start() + age / Math.max(1, o.inc()) % Math.max(1, o.count());
        else index = (int)(o.start() + Math.min(o.count() * (age / (float) lifetime), o.count() - 1));
        var ff = o.finalFrames();
        if (!ff.isEmpty() && age > lifetime - ff.size()) index = ff.get(Math.max(0, Math.min(ff.size() - 1, lifetime - age)));
        alpha = alphaFrames.length == 0 ? 0 : alphaFrames[Math.min(age, alphaFrames.length - 1)];
        particleScale = scaleFrames.length == 0 ? 0 : scaleFrames[Math.min(age, scaleFrames.length - 1)];
    }
    @Override public float getQuadSize(float partial) { return 0.1f * particleScale; }
    @Override public void extract(net.minecraft.client.renderer.state.level.QuadParticleRenderState state, net.minecraft.client.Camera camera, float partial) {
        float fs = Mth.clamp((age + partial) / lifetime, 0, 1);
        float pr = rCol, pg = gCol, pb = bCol;
        rCol = pr + (o.r2() - pr) * fs; gCol = pg + (o.g2() - pg) * fs; bCol = pb + (o.b2() - pb) * fs;
        super.extract(state, camera, partial);
        rCol = pr; gCol = pg; bCol = pb;
    }
    private float cell() { return (sprite.getU1() - sprite.getU0()) / o.gridSize(); }
    private int col() { return Math.max(0, index) % o.gridSize(); }
    private int row() { return Math.max(0, index) / o.gridSize(); }
    @Override protected float getU0() { float u = sprite.getU0() + col() * cell(); return o.flipped() ? u + cell() : u; }
    @Override protected float getU1() { float u = sprite.getU0() + (col() + 1) * cell(); return o.flipped() ? u - cell() : u; }
    @Override protected float getV0() { return sprite.getV0() + row() * (sprite.getV1() - sprite.getV0()) / o.gridSize(); }
    @Override protected float getV1() { return sprite.getV0() + (row() + 1) * (sprite.getV1() - sprite.getV0()) / o.gridSize(); }
    @Override protected Layer getLayer() { return TcParticleLayers.layer(o.layer()); }

    /** The 1024px TC6 particle sheet, for hand-built particles. */
    public static SpriteSet SHEET;

    public static final class Provider implements ParticleProvider<GenericParticleOptions> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; SHEET = sprites; }
        @Override public Particle createParticle(GenericParticleOptions options, ClientLevel level, double x, double y, double z, double dx, double dy, double dz, RandomSource random) {
            return new GenericParticle(level, x, y, z, dx, dy, dz, sprites.get(random), options);
        }
    }
}
