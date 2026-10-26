package art.arcane.thaumcraft.entity;

import art.arcane.thaumcraft.registry.ModEntities;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/** TC6 EntityGrapple: flies for up to 30 ticks; once it hits something it pulls its thrower in until they sneak. */
public final class GrappleEntity extends ThrowableProjectile implements IEntityWithComplexSpawn {
    /** thrower entity id -> grapple entity id (one live grapple per thrower). */
    /** Thrower id → hook id (TC6). Touched by both logical sides, which share this JVM in singleplayer. */
    public static final Map<Integer, Integer> grapples = new java.util.concurrent.ConcurrentHashMap<>();
    public InteractionHand hand = InteractionHand.MAIN_HAND;
    private LivingEntity cthrower;
    private boolean p, boost, added;
    private int prevDist, count;
    public float ampl;

    public GrappleEntity(EntityType<? extends GrappleEntity> type, Level level) { super(type, level); }

    public GrappleEntity(Level level, LivingEntity thrower, InteractionHand hand) {
        super(ModEntities.GRAPPLE.get(), level);
        setOwner(thrower);
        setPos(thrower.getX(), thrower.getEyeY() - 0.1, thrower.getZ());
        this.hand = hand;
        this.cthrower = thrower;
    }

    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {}
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 4096.0; }
    @Override public void shoot(double x, double y, double z, float velocity, float inaccuracy) { super.shoot(x, y, z, velocity, 0.0f); }

    @Override public void writeSpawnData(RegistryFriendlyByteBuf buf) { buf.writeInt(getThrower() == null ? -1 : getThrower().getId()); buf.writeBoolean(hand == InteractionHand.MAIN_HAND); }
    @Override public void readSpawnData(RegistryFriendlyByteBuf buf) {
        int id = buf.readInt();
        hand = buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (id >= 0 && level().getEntity(id) instanceof LivingEntity l) cthrower = l;
    }

    public LivingEntity getThrower() { return cthrower != null ? cthrower : getOwner() instanceof LivingEntity l ? l : null; }
    @Override protected double getDefaultGravity() { return getPulling() ? 0.0 : 0.03; }
    public void setPulling() { p = true; }
    public boolean getPulling() { return p; }

    @Override
    public void tick() {
        super.tick();
        LivingEntity thrower = getThrower();
        if (!getPulling() && !isRemoved() && (tickCount > 30 || thrower == null)) {
            if (thrower != null) grapples.remove(thrower.getId());
            discard();
        }
        if (thrower == null) return;
        if (!level().isClientSide() && !isRemoved() && !added) {
            Integer ii = grapples.get(thrower.getId());
            if (ii != null && ii != getId()) { Entity e = level().getEntity(ii); if (e != null) e.discard(); }
            grapples.put(thrower.getId(), getId());
            added = true;
        }
        Integer cur = grapples.get(thrower.getId());
        if (cur != null && cur != getId()) discard();
        double dis = thrower.distanceTo(this);
        if (getPulling() && !isRemoved()) {
            if (thrower.isShiftKeyDown()) { grapples.remove(thrower.getId()); discard(); }
            else {
                if (thrower instanceof ServerPlayer sp) sp.connection.resetFlyingTicks();
                thrower.fallDistance = 0.0f;
                double mx = getX() - thrower.getX(), my = getY() - thrower.getY(), mz = getZ() - thrower.getZ();
                double dd = dis;
                if (dis < 8.0) dd = dis * (8.0 - dis);
                dd = Math.max(1.0E-9, dd);
                mx /= dd * 5.0; my /= dd * 5.0; mz /= dd * 5.0;
                Vec3 v2 = new Vec3(mx, my, mz);
                if (v2.length() > 0.25) { v2 = v2.normalize(); mx = v2.x / 4.0; my = v2.y / 4.0; mz = v2.z / 4.0; }
                double by = 0;
                if (!boost) { by = 0.4000000059604645; boost = true; }
                thrower.setDeltaMovement(thrower.getDeltaMovement().add(mx, my + 0.033 + by, mz));
                int d = (int) (dis / 2.0);
                if (d == prevDist) ++count; else count = 0;
                prevDist = d;
            }
        }
        if (level().isClientSide()) { if (!getPulling()) ampl += 0.02f; else ampl *= 0.66f; }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 6) { setPulling(); setDeltaMovement(Vec3.ZERO); }
        else super.handleEntityEvent(id);
    }

    @Override
    protected void onHit(HitResult mop) {
        if (!level().isClientSide()) {
            setPulling();
            setDeltaMovement(Vec3.ZERO);
            setPos(mop.getLocation());
            level().broadcastEntityEvent(this, (byte) 6);
        }
    }
}
