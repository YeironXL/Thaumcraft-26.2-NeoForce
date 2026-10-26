package art.arcane.thaumcraft.entity;

import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** TC6 EntityThaumicSlime: purple flux slime that spits smaller slimes at distant players and bursts into size-1 slimes. */
public class ThaumicSlimeEntity extends Slime {
    public int launched = 10;
    int spitCounter = 100;
    private boolean groundBefore;

    public ThaumicSlimeEntity(EntityType<? extends Slime> type, Level level) { super(type, level); }

    /** Spit constructor: a size 1 slime launched from {@code from} at {@code target}. */
    public static ThaumicSlimeEntity spit(ServerLevel level, LivingEntity from, LivingEntity target) {
        var s = new ThaumicSlimeEntity(ModEntities.THAUMIC_SLIME.get(), level);
        s.setSize(1, true);
        double y = (from.getBoundingBox().minY + from.getBoundingBox().maxY) / 2.0;
        double dx = target.getX() - from.getX(), dy = target.getBoundingBox().minY + target.getBbHeight() / 3.0f - y, dz = target.getZ() - from.getZ();
        double h = Math.sqrt(dx * dx + dz * dz);
        if (h >= 1.0e-7) {
            float yaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0f, pitch = (float) (-(Math.atan2(dy, h) * 180.0 / Math.PI));
            s.snapTo(from.getX() + dx / h, y, from.getZ() + dz / h, yaw, pitch);
            s.shoot(dx, dy + h * 0.2f, dz, 1.5f, 1.0f);
        } else s.snapTo(from.getX(), y, from.getZ(), 0, 0);
        return s;
    }
    private void shoot(double x, double y, double z, float speed, float spread) {
        double f = Math.sqrt(x * x + y * y + z * z);
        x /= f; y /= f; z /= f;
        x += random.nextGaussian() * 0.0075 * spread; y += random.nextGaussian() * 0.0075 * spread; z += random.nextGaussian() * 0.0075 * spread;
        x *= speed; y *= speed; z *= speed;
        setDeltaMovement(x, y, z);
        float yaw = (float) (Math.atan2(x, z) * 180.0 / Math.PI), pitch = (float) (Math.atan2(y, Math.sqrt(x * x + z * z)) * 180.0 / Math.PI);
        setYRot(yaw); yRotO = yaw; setXRot(pitch); xRotO = pitch;
    }

    @Override public void setSize(int size, boolean updateHealth) {
        super.setSize(size, updateHealth);
        xpReward = getSize() + 2;
    }
    @Override protected boolean spawnCustomParticles() { return true; }

    @Override public void tick() {
        boolean landing;
        super.tick();
        landing = onGround() && !groundBefore;
        groundBefore = onGround();
        int i = getSize();
        if (level().isClientSide()) {
            if (landing) for (int j = 0; j < i * 2; ++j) art.arcane.thaumcraft.client.fx.FXDispatcher.slimeJumpFX(this, i);
            if (launched > 0) {
                --launched;
                for (int k = 0; k < i * (launched + 1); ++k) art.arcane.thaumcraft.client.fx.FXDispatcher.slimeJumpFX(this, i);
            }
        } else if (isAlive() && level() instanceof ServerLevel sl) {
            if (launched > 0) --launched;
            Player p = sl.getNearestPlayer(this, 16.0);
            if (p != null) {
                if (spitCounter > 0) --spitCounter;
                lookAt(p, 10.0f, 20.0f);
                if (distanceTo(p) > 4.0f && spitCounter <= 0 && getSize() > 2) {
                    spitCounter = 101;
                    sl.addFreshEntity(spit(sl, this, p));
                    playSound(ModSounds.get("gore"), 1.0f, ((random.nextFloat() - random.nextFloat()) * 0.2f + 1.0f) * 0.8f);
                    setSize(getSize() - 1, true);
                }
            }
        }
    }

    /** setDead: a slime bigger than 1 bursts into {@code size} slimes of size 1. */
    @Override protected int getSplitCount() { return getSize(); }
    @Override protected void setUpSplitCube(AbstractCubeMob cube, int half, float xd, float zd) { super.setUpSplitCube(cube, 1, xd, zd); }

    @Override protected float getAttackDamage() { return getSize() + 1; }
    @Override protected boolean isDealsDamage() { return true; }
    @Override public boolean isWithinMeleeAttackRange(LivingEntity target) {
        double r = 0.6 * (getSize() + (launched > 0 ? 2 : 0));
        return distanceToSqr(target) < r * r;
    }

    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (getSize() > 1) spawnAtLocation(level, CrystalEssenceItem.of("vitium", 1), getBbHeight() / 2.0f);
    }
}
