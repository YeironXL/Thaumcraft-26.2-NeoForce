package art.arcane.thaumcraft.client.fx;

import art.arcane.thaumcraft.blockentity.CrucibleBlockEntity;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;

/** Client effects triggered from common code (block events); referenced only when level.isClientSide(). */
public final class ClientHooks {
    public static void bamf(BlockPos pos) { FXDispatcher.drawBamf(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.5f, 0.1f, 0.6f, true, true, Direction.UP); }
    /** TileCrucible block event 99: crafting bamf and spill sound. */
    /** TileVoidSiphon block event 5: FXDispatcher.voidStreak from every feeding rift to the siphon's orb. */
    public static void voidSiphon(art.arcane.thaumcraft.blockentity.VoidSiphonBlockEntity te, int seed) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var pos = te.getBlockPos();
        double tx = pos.getX() + 0.5, ty = pos.getY() + 0.5625, tz = pos.getZ() + 0.5;
        for (var fr : te.validRifts(level)) for (int i = 0; i < 6; i++) {
            float f = i / 6f;
            double x = fr.getX() + (tx - fr.getX()) * f, y = fr.getY() + (ty - fr.getY()) * f, z = fr.getZ() + (tz - fr.getZ()) * f;
            FXDispatcher.drawWispyMotes(x, y, z, (tx - x) / 20.0, (ty - y) / 20.0, (tz - z) / 20.0, 20, 0.35f, 0.1f, 0.45f, 0.0f);
        }
    }
    public static void crucibleCraft(BlockPos pos) {
        FXDispatcher.drawBamf(pos.getX() + 0.5, pos.getY() + 1.25f, pos.getZ() + 0.5, 0.5f, 0.1f, 0.6f, true, true, Direction.UP);
        Minecraft.getInstance().level.playLocalSound(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, art.arcane.thaumcraft.registry.ModSounds.get("spill"), SoundSource.BLOCKS, 0.2f, 1.0f, false);
    }
    /** TileCrucible block event 2: spill sound and ten crucibleBoil bursts. */
    public static void crucibleBoilEvent(CrucibleBlockEntity tile, int j) {
        var pos = tile.getBlockPos(); var level = Minecraft.getInstance().level;
        level.playLocalSound(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f, art.arcane.thaumcraft.registry.ModSounds.get("spill"), SoundSource.BLOCKS, 0.2f, 1.0f, false);
        for (int q = 0; q < 10; ++q) crucibleBoil(tile, j);
    }
    /** FXDispatcher.crucibleBoil. */
    public static void crucibleBoil(CrucibleBlockEntity tile, int j) {
        var pos = tile.getBlockPos(); var rand = Minecraft.getInstance().level.getRandom();
        var tags = java.util.List.copyOf(tile.mixture().aspects().keySet());
        for (int a = 0; a < 2; ++a) {
            float r = 1, g = 1, b = 1;
            if (!tags.isEmpty()) { int c = art.arcane.thaumcraft.aspect.AspectRegistry.color(tags.get(rand.nextInt(tags.size()))); r = (c >> 16 & 255) / 255f; g = (c >> 8 & 255) / 255f; b = (c & 255) / 255f; }
            var o = GenericParticleOptions.builder().maxAge((int)(7.0 + 8.0 / (Math.random() * 0.8 + 0.2))).scale(rand.nextFloat() * 0.3f + 0.2f).color(r, g, b)
                    .randomMovement(0.001f, 0.001f, 0.001f).gravity(-0.025f * j).particle(64).finalFrames(65, 66).build();
            Minecraft.getInstance().level.addParticle(o, true, false, pos.getX() + 0.2f + rand.nextFloat() * 0.6f, pos.getY() + 0.1f + tile.fluidHeight(), pos.getZ() + 0.2f + rand.nextFloat() * 0.6f, 0.0, 0.002, 0.0);
        }
    }
    /** FXDispatcher.arcLightning: an FXArc (h clamped to at least 0.1). */
    public static void arcLightning(double x, double y, double z, double tx, double ty, double tz, float r, float g, float b, float h) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (h <= 0.0f) h = 0.1f;
        mc.particleEngine.add(new ArcParticle(mc.level, x, y, z, tx, ty, tz, r, g, b, h));
    }

    /** FXDispatcher.wispFXEG (FXWispEG x2): dark motes falling off eldritch guardians, faded with camera distance. */
    public static void wispFXEG(double x, double y, double z, net.minecraft.world.entity.Entity target) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var rand = mc.level.getRandom();
        var cam = mc.getCameraEntity();
        double dist = cam == null ? 0 : cam.distanceToSqr(x, y, z);
        float base = (float) (1.0 - Math.min(1024.0, dist) / 1024.0);
        for (int a = 0; a < 2; ++a) {
            float scale = ((rand.nextFloat() * 0.5f + 0.5f) * 2.0f) * 0.4f * 5.0f;
            int age = (int) (40.0 / (Math.random() * 0.3 + 0.7));
            var o = GenericParticleOptions.builder().maxAge(age).color(rand.nextFloat() * 0.05f, rand.nextFloat() * 0.05f, rand.nextFloat() * 0.05f)
                    .alpha(0.2f * base * 0.75f, 0.0f).scale(scale).grid(64).particles(192, 13, 1).loop(true).layer(1).slowDown(0.98).build();
            mc.level.addParticle(o, true, false, x, y, z, rand.nextGaussian() * 0.03, -0.05, rand.nextGaussian() * 0.03);
        }
    }
    /** FXDispatcher.cultistSpawn: white-to-red puffs around a cultist arriving through a portal. */
    public static void cultistSpawn(double x, double y, double z, double a, double b, double c) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var rand = mc.level.getRandom();
        var o = GenericParticleOptions.builder().maxAge(10 + rand.nextInt(10)).color(1.0f, 1.0f, 1.0f, 0.6f, 0.0f, 0.0f).alpha(0.8f)
                .grid(16).particles(160, 6, 1).scale(3.0f + rand.nextFloat() * 2.0f).layer(1).build();
        mc.level.addParticle(o, true, false, x, y, z, a, b, c);
    }
    /** FXDispatcher.crucibleBubble (also the sanity soap lather). */
    public static void crucibleBubble(float x, float y, float z, float cr, float cg, float cb) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var rand = mc.level.getRandom();
        var o = GenericParticleOptions.builder().maxAge(15 + rand.nextInt(10)).scale(rand.nextFloat() * 0.3f + 0.3f).color(cr, cg, cb)
                .randomMovement(0.002f, 0.002f, 0.002f).gravity(-0.001f).particle(64).finalFrames(65, 66, 66).build();
        mc.level.addParticle(o, true, false, x, y, z, 0, 0, 0);
    }
    /** FXDispatcher.drawNitorCore (EntityFollowingItem trail for types other than 10). */
    public static void drawNitorCore(double x, double y, double z, double x2, double y2, double z2) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var rand = mc.level.getRandom();
        var o = GenericParticleOptions.builder().maxAge(10).color(1.0f, 1.0f, 1.0f).alpha(1.0f).particles(457, 1, 1)
                .scale(1.0f, 1.0f + (float) rand.nextGaussian() * 0.1f, 1.0f).layer(1).randomMovement(2.0E-4f, 2.0E-4f, 2.0E-4f).build();
        mc.level.addParticle(o, true, false, x, y, z, x2, y2, z2);
    }
    /** FXDispatcher.burst. */
    public static void burst(double x, double y, double z, float size) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var o = GenericParticleOptions.builder().maxAge(31).grid(16).particles(208, 31, 1).scale(size).build();
        mc.level.addParticle(o, true, false, x, y, z, 0, 0, 0);
    }
    private ClientHooks() {}
    /** EntityTaintSwarm.onUpdate (client): keep 30 FXSwarm insects around the swarm. */
    public static void swarmTick(art.arcane.thaumcraft.entity.monster.TaintSwarmEntity e) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || GenericParticle.SHEET == null) return;
        e.swarm.removeIf(o -> !(o instanceof SwarmParticle p) || !p.alive());
        if (e.swarm.size() < 30) {
            var r = mc.level.getRandom();
            var p = new SwarmParticle(mc.level, e.getX() + (r.nextFloat() - r.nextFloat()) * 2.0f, e.getY() + (r.nextFloat() - r.nextFloat()) * 2.0f, e.getZ() + (r.nextFloat() - r.nextFloat()) * 2.0f,
                    e, 0.8f + r.nextFloat() * 0.2f, r.nextFloat() * 0.4f, 1.0f - r.nextFloat() * 0.2f, 0.22f, 15.0f, 0.08f);
            mc.particleEngine.add(p);
            e.swarm.add(p);
        }
    }
    /** FXDispatcher.tentacleAriseFX: purple slime chips and block dust as a tentacle rises out of the ground. */
    public static void tentacleAriseFX(net.minecraft.world.entity.Entity e) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var r = mc.level.getRandom();
        float h = e.getBbHeight();
        for (int j = 0; j < 2.0f * h; ++j) {
            float f = r.nextFloat() * (float) Math.PI * h, f2 = r.nextFloat() * 0.5f + 0.5f;
            float f3 = net.minecraft.util.Mth.sin(f) * h * 0.25f * f2, f4 = net.minecraft.util.Mth.cos(f) * h * 0.25f * f2;
            mc.particleEngine.add(new BreakingFadeParticle(mc.level, e.getX() + f3, e.getY(), e.getZ() + f4, net.minecraft.world.item.Items.SLIME_BALL)
                    .color(0.4f, 0.0f, 0.4f).alpha(0.5f).maxAge((int) (66.0f / (r.nextFloat() * 0.9f + 0.1f))));
            BlockPos below = e.blockPosition().below();
            if (!mc.level.isEmptyBlock(below)) {
                f = r.nextFloat() * (float) Math.PI * h; f2 = r.nextFloat() * 0.5f + 0.5f;
                f3 = net.minecraft.util.Mth.sin(f) * h * 0.25f * f2; f4 = net.minecraft.util.Mth.cos(f) * h * 0.25f * f2;
                mc.level.addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, mc.level.getBlockState(below)), e.getX() + f3, e.getY(), e.getZ() + f4, 0, 0, 0);
            }
        }
    }
    /** TileMirror.receiveClientEvent(1): purple wispy motes drift out of the mirror's face. */
    public static void mirrorMotes(art.arcane.thaumcraft.blockentity.MirrorBlockEntity te) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        var r = mc.level.getRandom();
        Direction face = te.facing();
        BlockPos pos = te.getBlockPos();
        double xx = pos.getX() + 0.33 + r.nextFloat() * 0.33f - face.getStepX() / 2.0;
        double yy = pos.getY() + 0.33 + r.nextFloat() * 0.33f - face.getStepY() / 2.0;
        double zz = pos.getZ() + 0.33 + r.nextFloat() * 0.33f - face.getStepZ() / 2.0;
        FXDispatcher.drawWispyMotes(xx, yy, zz, face.getStepX() / 50.0 + r.nextGaussian() * 0.01, face.getStepY() / 50.0 + r.nextGaussian() * 0.01, face.getStepZ() / 50.0 + r.nextGaussian() * 0.01,
                net.minecraft.util.Mth.nextInt(r, 10, 30), r.nextFloat() / 3.0f, 0.0f, r.nextFloat() / 2.0f, (float) (r.nextGaussian() * 0.01));
    }
}
