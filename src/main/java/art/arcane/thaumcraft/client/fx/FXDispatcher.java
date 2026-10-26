package art.arcane.thaumcraft.client.fx;

import art.arcane.thaumcraft.fx.GenericParticleOptions;
import art.arcane.thaumcraft.network.FxPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Client half of TC6 client.fx.FXDispatcher (routines are added as the systems using them are ported). */
public final class FXDispatcher {
    private static RandomSource rand() { return Minecraft.getInstance().level.getRandom(); }
    private static void add(GenericParticleOptions o, double x, double y, double z, double dx, double dy, double dz) {
        var level = Minecraft.getInstance().level;
        if (level != null) level.addParticle(o, true, false, x, y, z, dx, dy, dz);
    }
    public static void handle(FxPayload p) {
        var a = p.args();
        if (p.kind().startsWith("focus_impact|") || p.kind().startsWith("focus_effect|")) { focusParts(p); return; }
        if (art.arcane.thaumcraft.client.WarpClient.handle(p)) return;
        if (InfusionFx.handle(p)) return;
        switch (p.kind()) {
            case "zap" -> arcBolt(p.x(), p.y(), p.z(), a.get(0), a.get(1), a.get(2), ((a.get(3).intValue() >> 16) & 255) / 255f, ((a.get(3).intValue() >> 8) & 255) / 255f, (a.get(3).intValue() & 255) / 255f, a.get(4));
            case "bamf" -> drawBamf(p.x(), p.y(), p.z(), a.get(0), a.get(1), a.get(2), a.get(3) > 0, a.get(4) > 0, a.get(5) < 0 ? null : Direction.from3DDataValue(a.get(5).intValue()));
            case "slash" -> { // PacketFXSlash
                var level = Minecraft.getInstance().level;
                if (level == null) return;
                var e1 = level.getEntity(a.get(0).intValue());
                var e2 = level.getEntity(a.get(1).intValue());
                if (e1 != null && e2 != null) drawSlash(e1.getX(), e1.getBoundingBox().minY + e1.getBbHeight() / 2.0f, e1.getZ(), e2.getX(), e2.getBoundingBox().minY + e2.getBbHeight() / 2.0f, e2.getZ(), 8);
            }
            case "bore_dig" -> boreDig(net.minecraft.core.BlockPos.containing(p.x(), p.y(), p.z()), a.get(0).intValue(), a.get(1).intValue());
            case "shield" -> shield(a.get(0).intValue(), a.get(1).intValue());
            case "dust" -> {
                var level = Minecraft.getInstance().level;
                if (level != null && level.getEntity(a.get(0).intValue()) instanceof net.minecraft.world.entity.player.Player player) {
                    var positions = new java.util.ArrayList<net.minecraft.core.BlockPos>();
                    for (int i = 5; i + 2 < a.size(); i += 3) positions.add(net.minecraft.core.BlockPos.containing(a.get(i), a.get(i + 1), a.get(i + 2)));
                    dustSparkles(player, a.get(1) > 0 ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND,
                            net.minecraft.core.BlockPos.containing(p.x(), p.y(), p.z()), new net.minecraft.world.phys.Vec3(a.get(2), a.get(3), a.get(4)), positions);
                }
            }
            default -> {}
        }
    }

    /** PacketFXFocusPartImpact (15 parts spread randomly) and PacketFXFocusEffect (10 parts along the motion). */
    private static void focusParts(FxPayload p) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        boolean impact = p.kind().startsWith("focus_impact|");
        String[] partKeys = p.kind().substring(p.kind().indexOf('|') + 1).split("%");
        int amt = Math.max(1, (impact ? 15 : 10) / partKeys.length);
        var r = level.getRandom();
        for (String k : partKeys) {
            if (art.arcane.thaumcraft.casters.FocusEngine.getElement(k) instanceof art.arcane.thaumcraft.casters.FocusEffect part) {
                for (int i = 0; i < amt; ++i) {
                    if (impact) part.renderParticleFX(level, p.x(), p.y(), p.z(), r.nextGaussian() * 0.15, r.nextGaussian() * 0.15, r.nextGaussian() * 0.15);
                    else part.renderParticleFX(level, p.x(), p.y(), p.z(), p.args().get(0) + r.nextGaussian() / 20.0, p.args().get(1) + r.nextGaussian() / 20.0, p.args().get(2) + r.nextGaussian() / 20.0);
                }
            }
        }
    }

    /** FXDispatcher.drawGenericParticles(..., GenPart) / ParticleEngine.addEffectWithDelay. */
    public static void generic(double x, double y, double z, double mx, double my, double mz, GenericParticleOptions o, int delay) {
        if (delay <= 0) add(o, x, y, z, mx, my, mz);
        else later(delay, () -> add(o, x, y, z, mx, my, mz));
    }

    /** FXDispatcher.smokeSpiral (FXSmokeSpiral). */
    public static void smokeSpiral(double x, double y, double z, float rad, int start, int miny, int color) {
        var level = Minecraft.getInstance().level;
        if (level == null || GenericParticle.SHEET == null) return;
        Minecraft.getInstance().particleEngine.add(new SmokeSpiralParticle(level, x, y, z, rad, start, miny, color));
    }

    /** FXDispatcher.drawAlumentum (FXFireMote with alpha). */
    public static void drawAlumentum(float x, float y, float z, float vx, float vy, float vz, float r, float g, float b, float alpha, float scale) {
        drawFireMote(x, y, z, vx, vy, vz, r, g, b, alpha, scale);
    }

    /** FXDispatcher.drawGenericParticles (64-grid FXGeneric, optional delay). */
    public static void drawGenericParticles(double x, double y, double z, double x2, double y2, double z2, float r, float g, float b, float alpha,
                                            boolean loop, int start, int num, int inc, int age, int delay, float scale, float rot, int layer) {
        generic(x, y, z, x2, y2, z2, GenericParticleOptions.builder().maxAge(age).color(r, g, b).alpha(alpha).loop(loop).particles(start, num, inc)
                .scale(scale).layer(layer).rotation(rot).build(), delay);
    }

    /** FXDispatcher.drawFireMote (FXFireMote: frame 7, max brightness, shrinking and fading over ~16 ticks). */
    public static void drawFireMote(float x, float y, float z, float vx, float vy, float vz, float r, float g, float b, float alpha, float scale) {
        boolean bb = rand().nextBoolean();
        float s = bb ? scale / 3.0f : scale;
        add(GenericParticleOptions.builder().maxAge(14).color(r, g, b).alpha(alpha, 0.0f).particle(7).scale(s, 0.0f).layer(bb ? 1 : 0).slowDown(0.98)
                .rotation(0, 57.29578f).build(), x, y, z, vx, vy, vz);
    }

    /** FXDispatcher.waterTrailFx: an essentia stream from the top of p1 to p2. */
    public static void waterTrailFx(net.minecraft.core.BlockPos p1, net.minecraft.core.BlockPos p2, int count, int color, float scale) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        EssentiaStreamParticle.spawn(level, p1.getX() + 0.5, p1.getY() + 0.66, p1.getZ() + 0.5, p2.getX() + 0.5, p2.getY() + 0.5, p2.getZ() + 0.5, count, color, scale, 0, 0.2);
    }

    /** FXDispatcher.jarSplashFx: water drops jumping out of a full urn. */
    public static void jarSplashFx(double x, double y, double z) {
        var r = rand();
        add(GenericParticleOptions.builder().maxAge(20 + r.nextInt(10)).color(0x28/255f, 0x6F/255f, 0xF6/255f).alpha(0.5f).loop(false).particles(73, 1, 1)
                .scale(0.4f + r.nextFloat() * 0.3f, 0.0f).layer(1).gravity(0.3f).rotation(0.0f).build(),
                x + r.nextGaussian() * 0.075, y, z + r.nextGaussian() * 0.075, r.nextGaussian() * 0.015, 0.075f + r.nextFloat() * 0.05f, r.nextGaussian() * 0.015);
    }

    /**
     * FXDispatcher.drawVentParticles2 (FXVent2): a coloured puff that grows quickly and fades, frames 1-5 of the 64-grid
     * sheet; plus occasional sparks. Size and alpha follow FXVent2 (0.3 x scale, alpha 0.4 fading to 0).
     */
    public static void drawVentParticles2(double x, double y, double z, double x2, double y2, double z2, int color, float scale) {
        var r = rand();
        Minecraft.getInstance().particleEngine.add(new VentParticle(Minecraft.getInstance().level, x, y, z, x2, y2, z2, color, scale, true));
        if (r.nextInt(6) < 2)
            add(GenericParticleOptions.builder().maxAge(10 + r.nextInt(4)).color(1.0f, 0.7f, 0.2f).alpha(0.9f).loop(true).particles(320, 16, 1)
                    .scale(0.25f + r.nextFloat() * 0.1f, 0.0f).layer(1).build(), x, y, z, x2 / 2.0, y2 / 2.0, z2 / 2.0);
    }

    /** FXDispatcher.drawVentParticles (FXVent, alpha 0.4). */
    public static void drawVentParticles(double x, double y, double z, double x2, double y2, double z2, int color) { drawVentParticles(x, y, z, x2, y2, z2, color, 1.0f); }
    public static void drawVentParticles(double x, double y, double z, double x2, double y2, double z2, int color, float scale) {
        Minecraft.getInstance().particleEngine.add(new VentParticle(Minecraft.getInstance().level, x, y, z, x2, y2, z2, color, scale, false));
    }

    /** FXDispatcher.slimeJumpFX: purple translucent slime-ball chips around a thaumic slime. */
    public static void slimeJumpFX(net.minecraft.world.entity.Entity e, int i) {
        var r = rand();
        float f = r.nextFloat() * (float) Math.PI * 2.0f, f2 = r.nextFloat() * 0.5f + 0.5f;
        float f3 = Mth.sin(f) * i * 0.5f * f2, f4 = Mth.cos(f) * i * 0.5f * f2;
        var bb = e.getBoundingBox();
        Minecraft.getInstance().particleEngine.add(new BreakingFadeParticle(Minecraft.getInstance().level, e.getX() + f3, (bb.minY + bb.maxY) / 2.0, e.getZ() + f4,
                net.minecraft.world.item.Items.SLIME_BALL).color(0.7f, 0.0f, 1.0f).alpha(0.4f).maxAge((int) (66.0f / (r.nextFloat() * 0.9f + 0.1f))));
    }

    /** FXDispatcher.visSparkle. */
    public static void visSparkle(int x, int y, int z, int x2, int y2, int z2, int color) {
        var level = Minecraft.getInstance().level;
        if (level == null || GenericParticle.SHEET == null) return;
        var r = rand();
        var p = new VisSparkleParticle(level, x + r.nextFloat(), y + r.nextFloat(), z + r.nextFloat(), x2 + 0.4 + r.nextFloat() * 0.2f, y2 + 0.4 + r.nextFloat() * 0.2f, z2 + 0.4 + r.nextFloat() * 0.2f);
        p.color(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f);
        Minecraft.getInstance().particleEngine.add(p);
    }

    /** FXDispatcher.sparkle. */
    public static void sparkle(float x, float y, float z, float r, float g, float b) {
        var rand = rand();
        if (rand.nextInt(6) < 4) add(GenericParticleOptions.builder().maxAge(6 + rand.nextInt(4)).color(r, g, b).alpha(0.9f).loop(true).particles(320, 16, 1)
                .scale(0.6f + rand.nextFloat() * 0.2f).build(), x, y, z, 0, 0, 0);
    }

    /** FXDispatcher.drawSlash (FXPlane). */
    public static void drawSlash(double x, double y, double z, double x2, double y2, double z2, int dur) {
        var level = Minecraft.getInstance().level;
        if (level != null) Minecraft.getInstance().particleEngine.add(new SlashParticle(level, x, y, z, x2, y2, z2, dur));
    }

    /** FXDispatcher.drawPedestalShield: purple FXShieldRunes dome over a pedestal whose discharge a stabilizer absorbed. */
    public static void drawPedestalShield(net.minecraft.core.BlockPos pos) {
        var level = Minecraft.getInstance().level;
        if (level != null) Minecraft.getInstance().particleEngine.add(new ShieldRunesParticle(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, null, 8, 0.0f, 90.0f));
    }

    /** PacketFXShield.onMessage: runes on the side of the entity facing the attacker, or below/above for falls/falling blocks. */
    public static void shield(int source, int target) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var p = level.getEntity(source);
        if (p == null) return;
        var pe = Minecraft.getInstance().particleEngine;
        if (target >= 0) {
            float pitch, yaw;
            var t = level.getEntity(target);
            if (t != null) {
                double d0 = p.getX() - t.getX();
                double d2 = (p.getBoundingBox().minY + p.getBoundingBox().maxY) / 2.0 - (t.getBoundingBox().minY + t.getBoundingBox().maxY) / 2.0;
                double d3 = p.getZ() - t.getZ();
                double d4 = Math.sqrt(d0 * d0 + d3 * d3);
                yaw = (float) (Math.atan2(d3, d0) * 180.0 / Math.PI) - 90.0f;
                pitch = (float) (-(Math.atan2(d2, d4) * 180.0 / Math.PI));
            } else { pitch = 90.0f; yaw = 0.0f; }
            pe.add(new ShieldRunesParticle(level, p.getX(), p.getY(), p.getZ(), p, 8, yaw, pitch));
        } else if (target == -1) {
            pe.add(new ShieldRunesParticle(level, p.getX(), p.getY(), p.getZ(), p, 8, 0.0f, 90.0f));
            pe.add(new ShieldRunesParticle(level, p.getX(), p.getY(), p.getZ(), p, 8, 0.0f, 270.0f));
        } else if (target == -2) {
            pe.add(new ShieldRunesParticle(level, p.getX(), p.getY(), p.getZ(), p, 8, 0.0f, 270.0f));
        } else if (target == -3) {
            pe.add(new ShieldRunesParticle(level, p.getX(), p.getY(), p.getZ(), p, 8, 0.0f, 90.0f));
        }
    }

    /** FXDispatcher.arcBolt: FXBolt lightning between two points. */
    public static void arcBolt(double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float width) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        Minecraft.getInstance().particleEngine.add(new BoltParticle(level, x, y, z, tx, ty, tz, r, g, b, width));
    }

    public static void drawBamf(double x, double y, double z, float r, float g, float b, boolean sound, boolean flair, Direction side) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var rand = rand();
        if (sound) level.playLocalSound(x, y, z, art.arcane.thaumcraft.registry.ModSounds.get("poof"), SoundSource.BLOCKS, 0.4f, 1.0f + (float) rand.nextGaussian() * 0.05f, false);
        for (int a = 0; a < 6 + rand.nextInt(3) + 2; ++a) {
            double vx = (0.05f + rand.nextFloat() * 0.05f) * (rand.nextBoolean() ? -1 : 1);
            double vy = (0.05f + rand.nextFloat() * 0.05f) * (rand.nextBoolean() ? -1 : 1);
            double vz = (0.05f + rand.nextFloat() * 0.05f) * (rand.nextBoolean() ? -1 : 1);
            if (side != null) { vx += side.getStepX() * 0.1f; vy += side.getStepY() * 0.1f; vz += side.getStepZ() * 0.1f; }
            var o = GenericParticleOptions.builder().maxAge(20 + rand.nextInt(15))
                    .color(Mth.clamp(r * (1f + (float) rand.nextGaussian() * 0.1f), 0, 1), Mth.clamp(g * (1f + (float) rand.nextGaussian() * 0.1f), 0, 1), Mth.clamp(b * (1f + (float) rand.nextGaussian() * 0.1f), 0, 1))
                    .alpha(1.0f, 0.1f).grid(16).particles(123, 5, 1).scale(3.0f, 4.0f + rand.nextFloat() * 3.0f).layer(1).slowDown(0.7)
                    .rotation(rand.nextFloat(), rand.nextBoolean() ? -1.0f : 1.0f).build();
            add(o, x + vx * 2.0, y + vy * 2.0, z + vz * 2.0, vx / 2.0, vy / 2.0, vz / 2.0);
        }
        if (flair) {
            for (int a = 0; a < 2 + rand.nextInt(3); ++a) {
                double vx = (0.025f + rand.nextFloat() * 0.025f) * (rand.nextBoolean() ? -1 : 1);
                double vy = (0.025f + rand.nextFloat() * 0.025f) * (rand.nextBoolean() ? -1 : 1);
                double vz = (0.025f + rand.nextFloat() * 0.025f) * (rand.nextBoolean() ? -1 : 1);
                drawWispyMotes(x + vx * 2.0, y + vy * 2.0, z + vz * 2.0, vx, vy, vz, 15 + rand.nextInt(10), -0.01f);
            }
            add(GenericParticleOptions.builder().maxAge(10 + rand.nextInt(5)).color(1.0f, 0.9f, 1.0f).alpha(1.0f, 0.0f).grid(16).particles(77, 1, 1)
                    .scale(10.0f + rand.nextFloat() * 2.0f, 0.0f).layer(0).rotation(rand.nextFloat(), (float) rand.nextGaussian()).build(), x, y, z, 0, 0, 0);
        }
        for (int a = 0; a < (flair ? 2 : 0) + rand.nextInt(3); ++a)
            drawCurlyWisp(x, y, z, 0, 0, 0, 1.0f, (0.9f + rand.nextFloat() * 0.1f + r) / 2.0f, (0.1f + g) / 2.0f, (0.5f + rand.nextFloat() * 0.1f + b) / 2.0f, 0.75f, side, a, 0, 0);
    }

    public static void drawCurlyWisp(double x, double y, double z, double vx, double vy, double vz, float scale, float r, float g, float b, float a, Direction side, int seed, int layer, int delay) {
        var rand = rand();
        vx += (0.0025f + rand.nextFloat() * 0.005f) * (rand.nextBoolean() ? -1 : 1);
        vy += (0.0025f + rand.nextFloat() * 0.005f) * (rand.nextBoolean() ? -1 : 1);
        vz += (0.0025f + rand.nextFloat() * 0.005f) * (rand.nextBoolean() ? -1 : 1);
        if (side != null) { vx += side.getStepX() * 0.025f; vy += side.getStepY() * 0.025f; vz += side.getStepZ() * 0.025f; }
        var o = GenericParticleOptions.builder().maxAge(25 + rand.nextInt(20 + 20 * seed)).color(r, g, b, 0.1f, 0.0f, 0.1f).alpha(a, 0.0f).grid(16)
                .particles(60 + rand.nextInt(4), 1, 1).scale(5.0f * scale, (10.0f + rand.nextFloat() * 4.0f) * scale).layer(layer)
                .rotation(rand.nextFloat(), rand.nextBoolean() ? (-2.0f - rand.nextFloat() * 2.0f) : (2.0f + rand.nextFloat() * 2.0f)).build();
        double fx = x + vx * 5.0, fy = y + vy * 5.0, fz = z + vz * 5.0, fvx = vx, fvy = vy, fvz = vz;
        later(delay, () -> add(o, fx, fy, fz, fvx, fvy, fvz));
    }

    public static void drawWispyMotes(double x, double y, double z, double vx, double vy, double vz, int age, float grav) {
        var rand = rand();
        drawWispyMotes(x, y, z, vx, vy, vz, age, 0.25f + rand.nextFloat() * 0.75f, 0.25f + rand.nextFloat() * 0.75f, 0.25f + rand.nextFloat() * 0.75f, grav);
    }
    public static void drawWispyMotes(double x, double y, double z, double vx, double vy, double vz, int age, float r, float g, float b, float grav) {
        var rand = rand();
        double[] wind = wind(0.001);
        add(GenericParticleOptions.builder().maxAge((int)(age + age / 2 * rand.nextFloat())).color(r, g, b).alpha(0.0f, 0.6f, 0.6f, 0.0f).grid(64)
                .particles(512, 16, 1).scale(1.0f, 0.5f).loop(true).wind(wind[0], wind[1]).gravity(grav).randomMovement(0.0025f, 0.0f, 0.0025f).build(), x, y, z, vx, vy, vz);
    }

    /** FXDispatcher.burst: the 31-frame puff (16-grid frames 208+) a wisp makes when it appears or dies. */
    public static void burst(double x, double y, double z, float size) {
        add(GenericParticleOptions.builder().maxAge(31).grid(16).particles(208, 31, 1).scale(size).build(), x, y, z, 0, 0, 0);
    }

    /** FXDispatcher.drawWispParticles: looping aspect-coloured motes trailing a wisp. */
    public static void drawWispParticles(double x, double y, double z, double x2, double y2, double z2, int color, int delay) {
        var rand = rand();
        double[] wind = wind(2.5E-4);
        var o = GenericParticleOptions.builder().maxAge(10 + rand.nextInt(5)).color(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f)
                .alpha(0.5f).loop(true).grid(64).particles(264, 8, 1).scale(1.0f + rand.nextFloat() * 0.25f, 0.05f).wind(wind[0], wind[1])
                .randomMovement(0.0025f, 0.0f, 0.0025f).build();
        later(delay, () -> add(o, x, y, z, x2, y2, z2));
    }

    /** FXDispatcher.drawTaintParticles: purple spores rising from taint seeds. */
    public static void drawTaintParticles(float x, float y, float z, float vx, float vy, float vz, float scale) {
        var rand = rand();
        add(GenericParticleOptions.builder().maxAge(80 + rand.nextInt(20)).color(0.4f + rand.nextFloat() * 0.2f, 0.1f + rand.nextFloat() * 0.3f, 0.5f + rand.nextFloat() * 0.2f)
                .alpha(0.75f, 0.0f).grid(16).particles(57 + rand.nextInt(3), 1, 1).scale(scale, scale / 4.0f).layer(1).slowDown(0.9750000238418579).gravity(0.2f)
                .rotation(rand.nextFloat(), rand.nextBoolean() ? -1.0f : 1.0f).build(), x, y, z, vx, vy, vz);
    }

    /** FXDispatcher.drawLightningFlash. */
    public static void drawLightningFlash(double x, double y, double z, float r, float g, float b, float alpha, float scale) {
        var rand = rand();
        add(GenericParticleOptions.builder().maxAge(5 + rand.nextInt(5)).grid(16).color(r, g, b).alpha(alpha, 0.0f).particles(108 + rand.nextInt(4), 1, 1)
                .scale(scale).layer(0).rotation(rand.nextFloat(), 0.0f).build(), x, y, z, 0, 0, 0);
    }

    /** EntityUtils.posToHand. */
    public static net.minecraft.world.phys.Vec3 posToHand(net.minecraft.world.entity.Entity e, net.minecraft.world.InteractionHand hand) {
        double px = e.getX(), py = e.getBoundingBox().minY + e.getBbHeight() / 2.0f + 0.25, pz = e.getZ();
        float m = hand == net.minecraft.world.InteractionHand.MAIN_HAND ? 0.0f : 180.0f;
        px += -Mth.cos((e.getYRot() + m) / 180.0f * 3.141593f) * 0.3f;
        pz += -Mth.sin((e.getYRot() + m) / 180.0f * 3.141593f) * 0.3f;
        var look = e.getViewVector(1.0f);
        return new net.minecraft.world.phys.Vec3(px + look.x * 0.3, py + look.y * 0.3, pz + look.z * 0.3);
    }
    /** ItemMagicDust.doSparkles. */
    public static void dustSparkles(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.core.BlockPos pos, net.minecraft.world.phys.Vec3 hit, java.util.List<net.minecraft.core.BlockPos> sparkles) {
        var level = Minecraft.getInstance().level; var rand = level.getRandom();
        var v1 = posToHand(player, hand);
        var v2 = net.minecraft.world.phys.Vec3.atCenterOf(pos).subtract(v1);
        int cnt = 50;
        for (int a = 0; a < cnt; ++a) {
            boolean floaty = a < cnt / 3;
            float r = Mth.nextInt(rand, 255, 255) / 255.0f, g = Mth.nextInt(rand, 189, 255) / 255.0f, b = Mth.nextInt(rand, 64, 255) / 255.0f;
            drawSimpleSparkle(rand, v1.x, v1.y, v1.z, v2.x / 6.0 + rand.nextGaussian() * 0.05, v2.y / 6.0 + rand.nextGaussian() * 0.05 + (floaty ? 0.05 : 0.15), v2.z / 6.0 + rand.nextGaussian() * 0.05,
                    0.5f, r, g, b, rand.nextInt(5), floaty ? (0.3f + rand.nextFloat() * 0.5f) : 0.85f, floaty ? 0.2f : 0.5f, 16);
        }
        level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), art.arcane.thaumcraft.registry.ModSounds.get("dust"), SoundSource.PLAYERS, 0.33f, 1.0f + (float) rand.nextGaussian() * 0.05f, false);
        for (var p : sparkles) drawBlockSparkles(p, hit);
    }
    /** FXDispatcher.drawLineSparkle (inlay charge sparks). */
    public static void drawLineSparkle(RandomSource rand, double x, double y, double z, double x2, double y2, double z2, float scale, float r, float g, float b, int delay, float decay, float grav, int baseAge) {
        boolean sp = rand.nextFloat() < 0.2;
        int age = baseAge * 4 + rand().nextInt(baseAge);
        var o = GenericParticleOptions.builder().maxAge(age).color(r, g, b).alpha(0.0f, 1.0f, 0.0f).particles(sp ? 320 : 512, 16, 1).loop(true).gravity(grav)
                .scale(scale, scale * 2.0f, scale).layer(0).slowDown(decay).randomMovement(5.0E-5f, 0.0f, 5.0E-5f).build();
        later(delay, () -> add(o, x, y, z, x2, y2, z2));
    }
    public static void drawSimpleSparkle(RandomSource rand, double x, double y, double z, double x2, double y2, double z2, float scale, float r, float g, float b, int delay, float decay, float grav, int baseAge) {
        boolean sp = rand.nextFloat() < 0.2;
        int age = baseAge * 4 + rand.nextInt(baseAge);
        float[] alphas = new float[6 + rand.nextInt(Math.max(1, age / 3))];
        for (int a = 1; a < alphas.length - 1; ++a) alphas[a] = rand.nextFloat();
        double[] wind = wind(5.0E-4);
        var o = GenericParticleOptions.builder().maxAge(age).color(r, g, b).alpha(alphas).particles(sp ? 320 : 512, 16, 1).loop(true).gravity(grav)
                .scale(scale, scale * 2.0f).layer(0).slowDown(decay).randomMovement(5.0E-4f, 0.001f, 5.0E-4f).wind(wind[0], wind[1]).build();
        later(delay, () -> add(o, x, y, z, x2, y2, z2));
    }
    public static void drawBlockSparkles(net.minecraft.core.BlockPos p, net.minecraft.world.phys.Vec3 start) {
        var level = Minecraft.getInstance().level; var rand = level.getRandom();
        var shape = level.getBlockState(p).getShape(level, p);
        var bs = shape.isEmpty() ? new net.minecraft.world.phys.AABB(0, 0, 0, 1, 1, 1) : shape.bounds();
        int num = (int)(bs.getSize() * 20.0);
        for (Direction face : Direction.values()) {
            var state = level.getBlockState(p.relative(face));
            if (state.isSolidRender() || state.isFaceSturdy(level, p.relative(face), face.getOpposite())) continue;
            boolean rx = face.getStepX() == 0, ry = face.getStepY() == 0, rz = face.getStepZ() == 0;
            double mx = 0.5 + face.getStepX() * 0.51, my = 0.5 + face.getStepY() * 0.51, mz = 0.5 + face.getStepZ() * 0.51;
            for (int a = 0; a < num * 2; ++a) {
                double x = mx, y = my, z = mz;
                if (rx) x += rand.nextGaussian() * 0.6; if (ry) y += rand.nextGaussian() * 0.6; if (rz) z += rand.nextGaussian() * 0.6;
                x = Mth.clamp(x, bs.minX, bs.maxX); y = Mth.clamp(y, bs.minY, bs.maxY); z = Mth.clamp(z, bs.minZ, bs.maxZ);
                float r = Mth.nextInt(rand, 255, 255) / 255.0f, g = Mth.nextInt(rand, 189, 255) / 255.0f, b = Mth.nextInt(rand, 64, 255) / 255.0f;
                var v1 = new net.minecraft.world.phys.Vec3(p.getX() + x, p.getY() + y, p.getZ() + z);
                double delay = rand.nextInt(5) + v1.distanceTo(start) * 16.0;
                drawSimpleSparkle(rand, p.getX() + x, p.getY() + y, p.getZ() + z, 0.0, 0.0025, 0.0, 0.4f + (float) rand.nextGaussian() * 0.1f, r, g, b, (int) delay, 1.0f, 0.01f, 16);
            }
        }
    }

    /** FXGeneric.setWind: direction from the moon phase. */
    public static double[] wind(double d) {
        var level = Minecraft.getInstance().level;
        int m = level == null ? 0 : art.arcane.thaumcraft.util.TcWorld.moonPhase(level, Minecraft.getInstance().player == null ? net.minecraft.world.phys.Vec3.ZERO : Minecraft.getInstance().player.position());
        double angle = m * (40 + rand().nextInt(10)) / 180.0f * Math.PI;
        double vx = 0.1 * Math.cos(angle), vz = -0.1 * Math.sin(angle);
        return new double[]{vx * d, vz * d};
    }

    private static final java.util.List<Object[]> DELAYED = new java.util.ArrayList<>();
    /** PacketFXBoreDig: for `delay` ticks run boreDigFx — 50/delay motes per tick, 1 in 4 a green sparkle, the rest block chips homing on the bore. */
    private static void boreDig(net.minecraft.core.BlockPos pos, int boreId, int delay) {
        var level = Minecraft.getInstance().level;
        if (level == null || delay < 1) return;
        var e = level.getEntity(boreId);
        if (e == null) return;
        var ts = level.getBlockState(pos);
        if (ts.isAir()) return;
        for (int t = 0; t < delay; ++t) later(t, () -> {
            var lv = Minecraft.getInstance().level;
            if (lv == null || e.isRemoved()) return;
            var r = lv.getRandom();
            for (int i = 0; i < 50.0f / delay; ++i) {
                double x = pos.getX() + r.nextFloat(), y = pos.getY() + r.nextFloat(), z = pos.getZ() + r.nextFloat();
                if (r.nextInt(4) == 0) {
                    var s = new BoreParticles.Sparkle(lv, x, y, z, e.getX(), e.getY() + e.getEyeHeight(), e.getZ(), 0.2f, 0.6f + r.nextFloat() * 0.3f, 0.2f);
                    s.target = e;
                    Minecraft.getInstance().particleEngine.add(s);
                } else Minecraft.getInstance().particleEngine.add(BoreParticles.Chip.ofBlock(lv, x, y, z, e, ts));
            }
        });
    }

    /** ParticleEngine.addEffectWithDelay. */
    public static void later(int delay, Runnable spawn) {
        if (delay <= 0) { spawn.run(); return; }
        DELAYED.add(new Object[]{delay, spawn});
    }
    public static void tick() {
        if (DELAYED.isEmpty()) return;
        var copy = new java.util.ArrayList<>(DELAYED); DELAYED.clear();
        for (var e : copy) { int d = (Integer) e[0] - 1; if (d <= 0) ((Runnable) e[1]).run(); else DELAYED.add(new Object[]{d, e[1]}); }
    }
    private FXDispatcher() {}
}
