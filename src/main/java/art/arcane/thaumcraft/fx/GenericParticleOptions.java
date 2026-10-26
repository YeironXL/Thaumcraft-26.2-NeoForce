package art.arcane.thaumcraft.fx;

import art.arcane.thaumcraft.registry.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import java.util.List;

/**
 * All settings of TC6 client.fx.particles.FXGeneric on the shared particle sheet (textures/misc/particles.png).
 * layer: 0 additive, 1 translucent, 2 additive without depth, 3 translucent without depth (ParticleEngine layers).
 */
public record GenericParticleOptions(int maxAge, float r, float g, float b, float r2, float g2, float b2, List<Float> alpha, List<Float> scale,
        float gravity, int gridSize, int start, int count, int inc, boolean loop, List<Integer> finalFrames, int layer, float slowDown,
        float rotationStart, float rotationSpeed, float randomX, float randomY, float randomZ, boolean noClip, double windX, double windZ, boolean flipped)
        implements ParticleOptions {
    private static final Codec<List<Float>> FLOATS = Codec.FLOAT.listOf();
    public static final MapCodec<GenericParticleOptions> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.fieldOf("max_age").forGetter(GenericParticleOptions::maxAge),
            Codec.FLOAT.listOf().fieldOf("color").forGetter(o -> List.of(o.r, o.g, o.b, o.r2, o.g2, o.b2)),
            FLOATS.fieldOf("alpha").forGetter(GenericParticleOptions::alpha),
            FLOATS.fieldOf("scale").forGetter(GenericParticleOptions::scale),
            Codec.FLOAT.fieldOf("gravity").forGetter(GenericParticleOptions::gravity),
            Codec.INT.listOf().fieldOf("frames").forGetter(o -> List.of(o.gridSize, o.start, o.count, o.inc, o.loop ? 1 : 0, o.layer, o.noClip ? 1 : 0, o.flipped ? 1 : 0)),
            Codec.INT.listOf().fieldOf("final_frames").forGetter(GenericParticleOptions::finalFrames),
            Codec.FLOAT.listOf().fieldOf("motion").forGetter(o -> List.of(o.slowDown, o.rotationStart, o.rotationSpeed, o.randomX, o.randomY, o.randomZ, (float) o.windX, (float) o.windZ))
    ).apply(i, (age, c, a, s, g, f, ff, m) -> new GenericParticleOptions(age, c.get(0), c.get(1), c.get(2), c.get(3), c.get(4), c.get(5), a, s, g,
            f.get(0), f.get(1), f.get(2), f.get(3), f.get(4) != 0, ff, f.get(5), m.get(0), m.get(1), m.get(2), m.get(3), m.get(4), m.get(5), f.get(6) != 0, m.get(6), m.get(7), f.get(7) != 0)));
    private static final StreamCodec<ByteBuf, List<Float>> FLOAT_LIST = ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list(64));
    private static final StreamCodec<ByteBuf, List<Integer>> INT_LIST = ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(64));
    public static final StreamCodec<RegistryFriendlyByteBuf, GenericParticleOptions> STREAM_CODEC = StreamCodec.of(
            (buf, o) -> {
                buf.writeVarInt(o.maxAge); FLOAT_LIST.encode(buf, List.of(o.r, o.g, o.b, o.r2, o.g2, o.b2)); FLOAT_LIST.encode(buf, o.alpha); FLOAT_LIST.encode(buf, o.scale);
                buf.writeFloat(o.gravity); INT_LIST.encode(buf, List.of(o.gridSize, o.start, o.count, o.inc, o.loop ? 1 : 0, o.layer, o.noClip ? 1 : 0, o.flipped ? 1 : 0));
                INT_LIST.encode(buf, o.finalFrames); FLOAT_LIST.encode(buf, List.of(o.slowDown, o.rotationStart, o.rotationSpeed, o.randomX, o.randomY, o.randomZ));
                buf.writeDouble(o.windX); buf.writeDouble(o.windZ);
            },
            buf -> {
                int age = buf.readVarInt(); var c = FLOAT_LIST.decode(buf); var a = FLOAT_LIST.decode(buf); var s = FLOAT_LIST.decode(buf); float g = buf.readFloat();
                var f = INT_LIST.decode(buf); var ff = INT_LIST.decode(buf); var m = FLOAT_LIST.decode(buf); double wx = buf.readDouble(), wz = buf.readDouble();
                return new GenericParticleOptions(age, c.get(0), c.get(1), c.get(2), c.get(3), c.get(4), c.get(5), a, s, g, f.get(0), f.get(1), f.get(2), f.get(3), f.get(4) != 0, ff, f.get(5),
                        m.get(0), m.get(1), m.get(2), m.get(3), m.get(4), m.get(5), f.get(6) != 0, wx, wz, f.get(7) != 0);
            });
    @Override public ParticleType<GenericParticleOptions> getType() { return ModParticles.GENERIC.get(); }

    /** Mutable builder mirroring the FXGeneric setters. */
    public static final class Builder {
        int maxAge = 20; float r = 1, g = 1, b = 1, r2 = 1, g2 = 1, b2 = 1; List<Float> alpha = List.of(1f), scale = List.of(1f);
        float gravity; int gridSize = 64, start, count = 1, inc = 1; boolean loop; List<Integer> finalFrames = List.of(); int layer;
        float slowDown = 0.98f, rotStart, rotSpeed, rx, ry, rz; boolean noClip; double windX, windZ; boolean flipped;
        public Builder maxAge(int v) { maxAge = v; return this; }
        public Builder color(float r, float g, float b) { this.r = r2 = r; this.g = g2 = g; this.b = b2 = b; return this; }
        public Builder color(float r, float g, float b, float r2, float g2, float b2) { this.r = r; this.g = g; this.b = b; this.r2 = r2; this.g2 = g2; this.b2 = b2; return this; }
        public Builder alpha(float... v) { var l = new java.util.ArrayList<Float>(); for (float x : v) l.add(x); alpha = l; return this; }
        public Builder scale(float... v) { var l = new java.util.ArrayList<Float>(); for (float x : v) l.add(x); scale = l; return this; }
        public Builder gravity(float v) { gravity = v; return this; }
        public Builder grid(int v) { gridSize = v; return this; }
        public Builder particle(int v) { start = v; count = 1; inc = 1; return this; }
        public Builder particles(int start, int count, int inc) { this.start = start; this.count = count; this.inc = inc; return this; }
        public Builder loop(boolean v) { loop = v; return this; }
        public Builder finalFrames(int... v) { var l = new java.util.ArrayList<Integer>(); for (int x : v) l.add(x); finalFrames = l; return this; }
        public Builder layer(int v) { layer = v; return this; }
        public Builder slowDown(double v) { slowDown = (float) v; return this; }
        /** FXGeneric.setRotationSpeed(rot): degrees per tick. */
        public Builder rotation(float speed) { rotSpeed = (float)(speed * 0.017453292519943); return this; }
        /** FXGeneric.setRotationSpeed(start, rot). */
        public Builder rotation(float start, float speed) { rotStart = (float)(start * Math.PI * 2); rotSpeed = (float)(speed * 0.017453292519943); return this; }
        public Builder randomMovement(float x, float y, float z) { rx = x; ry = y; rz = z; return this; }
        /** FXGeneric.setNoClip(clip): the flag is the collision switch itself. */
        public Builder collide(boolean v) { noClip = !v; return this; }
        public Builder wind(double x, double z) { windX = x; windZ = z; return this; }
        public Builder flipped(boolean v) { flipped = v; return this; }
        public GenericParticleOptions build() {
            return new GenericParticleOptions(maxAge, r, g, b, r2, g2, b2, alpha, scale, gravity, gridSize, start, count, inc, loop, finalFrames, layer, slowDown, rotStart, rotSpeed, rx, ry, rz, noClip, windX, windZ, flipped);
        }
    }
    public static Builder builder() { return new Builder(); }
}
