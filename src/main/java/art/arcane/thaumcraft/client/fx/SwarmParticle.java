package art.arcane.thaumcraft.client.fx;

import art.arcane.thaumcraft.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** TC6 FXSwarm: one insect of a taint swarm, circling and diving at the swarm entity it belongs to (sheet frames 7..14 of row 1). */
public final class SwarmParticle extends SheetParticle {
    private static final List<Long> BUZZCOUNT = new ArrayList<>();
    private final Entity target;
    private float turnSpeed = 10.0f, speed = 0.2f;
    private int deathtimer;
    private float rotationPitch, rotationYaw;
    private final float baseScale;

    public SwarmParticle(ClientLevel level, double x, double y, double z, Entity target, float r, float g, float b, float sp, float ts, float pg) {
        super(level, x, y, z);
        rCol = r; gCol = g; bCol = b; gBase = g; bBase = b;
        baseScale = random.nextFloat() * 0.5f + 1.0f;
        this.target = target;
        float f3 = 0.2f;
        xd = (random.nextFloat() - random.nextFloat()) * f3;
        yd = (random.nextFloat() - random.nextFloat()) * f3;
        zd = (random.nextFloat() - random.nextFloat()) * f3;
        speed = sp; turnSpeed = ts; gravity = pg;
        lifetime = Integer.MAX_VALUE;
        layer = 1;
        hasPhysics = true;
        alpha = 0.75f;
    }

    private boolean targetDead() { return target == null || target.isRemoved() || (target instanceof LivingEntity le && le.deathTime > 0); }

    @Override
    public void tick() {
        xo = x; yo = y; zo = z;
        ++age;
        if (targetDead()) {
            ++deathtimer;
            xd *= 0.9; zd *= 0.9;
            yd -= gravity / 2.0f;
            if (deathtimer > 50) remove();
        } else yd += gravity;
        move(xd, yd, zd);
        xd *= 0.985; yd *= 0.985; zd *= 0.985;
        if (!targetDead()) {
            boolean hurt = target instanceof LivingEntity le && le.hurtTime > 0;
            float t = turnSpeed / 2.0f;
            if (target.distanceToSqr(x, y, z) > target.getBbWidth() && !hurt) faceEntity(t + random.nextInt((int) t), t + random.nextInt((int) t));
            else faceEntity(-(t + random.nextInt((int) t)), -(t + random.nextInt((int) t)));
            double mx = -Mth.sin(rotationYaw / 180.0f * (float) Math.PI) * Mth.cos(rotationPitch / 180.0f * (float) Math.PI);
            double mz = Mth.cos(rotationYaw / 180.0f * (float) Math.PI) * Mth.cos(rotationPitch / 180.0f * (float) Math.PI);
            double my = -Mth.sin(rotationPitch / 180.0f * (float) Math.PI);
            setHeading(mx, my, mz, speed, 15.0f);
        }
        if (BUZZCOUNT.size() < 3 && random.nextInt(50) == 0 && level.getNearestPlayer(x, y, z, 8.0, false) != null) {
            level.playLocalSound(x, y, z, ModSounds.get("fly"), SoundSource.HOSTILE, 0.03f, 0.5f + random.nextFloat() * 0.4f, false);
            BUZZCOUNT.add(System.nanoTime() + 1500000L);
        }
        if (BUZZCOUNT.size() >= 3 && BUZZCOUNT.get(0) < System.nanoTime()) BUZZCOUNT.remove(0);
        index = 4 * 64 + 7 + age % 8; // u = (7 + age % 8) / 64, v = 0.0625
        float dd = target instanceof LivingEntity le && le.hurtTime > 0 ? 2.0f : 1.0f;
        gCol = gBase / dd; bCol = bBase / dd;
        alpha = (50.0f - deathtimer) / 50.0f;
    }

    private final float gBase, bBase;

    private void faceEntity(float yawStep, float pitchStep) {
        double d0 = target.getX() - x, d2 = target.getZ() - z;
        double d3 = (target.getBoundingBox().minY + target.getBoundingBox().maxY) / 2.0 - (getBoundingBox().minY + getBoundingBox().maxY) / 2.0;
        double d4 = Math.sqrt(d0 * d0 + d2 * d2);
        float f2 = (float) (Math.atan2(d2, d0) * 180.0 / Math.PI) - 90.0f;
        float f3 = (float) (-(Math.atan2(d3, d4) * 180.0 / Math.PI));
        rotationPitch = rotationPitch + Mth.clamp(Mth.wrapDegrees(f3 - rotationPitch), -pitchStep, pitchStep);
        rotationYaw = rotationYaw + Mth.clamp(Mth.wrapDegrees(f2 - rotationYaw), -yawStep, yawStep);
    }

    private void setHeading(double a, double b, double c, float sp, float spread) {
        double f2 = Math.sqrt(a * a + b * b + c * c);
        a /= f2; b /= f2; c /= f2;
        a += random.nextGaussian() * (random.nextBoolean() ? -1 : 1) * 0.007499999832361937 * spread;
        b += random.nextGaussian() * (random.nextBoolean() ? -1 : 1) * 0.007499999832361937 * spread;
        c += random.nextGaussian() * (random.nextBoolean() ? -1 : 1) * 0.007499999832361937 * spread;
        xd = a * sp; yd = b * sp; zd = c * sp;
    }

    @Override public float getQuadSize(float pt) { return 0.1f * baseScale * (Mth.sin(age / 3.0f) * 0.25f + 1.0f); }
    @Override public int getLightCoords(float pt) { return 0xF000F0; }
    public boolean alive() { return isAlive(); }
}
