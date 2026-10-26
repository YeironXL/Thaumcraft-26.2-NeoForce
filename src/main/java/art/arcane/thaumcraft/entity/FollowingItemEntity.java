package art.arcane.thaumcraft.entity;

import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/**
 * TC6 EntityFollowingItem (extends EntitySpecialItem): a dropped item that flies to a target entity (or point), its speed
 * rising as its "age" counts down from 20, trailing nitor sparks (or blue crucible bubbles for type 10, the COLLECTOR
 * colour), and becomes an ordinary item once within half a block.
 */
public final class FollowingItemEntity extends ItemEntity implements IEntityWithComplexSpawn {
    double targetX, targetY, targetZ;
    int type = 3;
    public Entity target;
    int followAge = 20;
    public double gravity = 0.04f;

    public FollowingItemEntity(EntityType<? extends ItemEntity> type, Level level) { super(type, level); }

    public FollowingItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        this(ModEntities.FOLLOWING_ITEM.get(), level);
        setPos(x, y, z);
        setItem(stack);
        setYRot((float) (Math.random() * 360.0));
        lifespan = stack.getEntityLifespan(level);
    }

    public FollowingItemEntity(Level level, double x, double y, double z, ItemStack stack, Entity target, int t) {
        this(level, x, y, z, stack);
        this.target = target;
        targetX = target.getX();
        targetY = target.getBoundingBox().minY + target.getBbHeight() / 2.0f;
        targetZ = target.getZ();
        type = t;
        noPhysics = true;
    }

    public FollowingItemEntity(Level level, double x, double y, double z, ItemStack stack, double tx, double ty, double tz) {
        this(level, x, y, z, stack);
        targetX = tx; targetY = ty; targetZ = tz;
    }

    public boolean isFollowing() { return targetX != 0.0 || targetY != 0.0 || targetZ != 0.0; }

    @Override
    public void tick() {
        if (target != null) {
            if (target.isRemoved()) target = null;
            else {
                targetX = target.getX();
                targetY = target.getBoundingBox().minY + target.getBbHeight() / 2.0f;
                targetZ = target.getZ();
            }
        }
        if (isFollowing()) {
            float xd = (float) (targetX - getX()), yd = (float) (targetY - getY()), zd = (float) (targetZ - getZ());
            if (followAge > 1) --followAge;
            double distance = Mth.sqrt(xd * xd + yd * yd + zd * zd);
            if (distance > 0.5) {
                distance *= followAge;
                setDeltaMovement(xd / distance, yd / distance, zd / distance);
            } else {
                setDeltaMovement(getDeltaMovement().scale(0.1f));
                targetX = targetY = targetZ = 0.0;
                target = null;
                noPhysics = false;
            }
            if (level().isClientSide()) {
                float h = (float) ((getBoundingBox().maxY - getBoundingBox().minY) / 2.0) + Mth.sin(getAge() / 10.0f + bobOffs) * 0.1f + 0.1f;
                var r = random;
                double px = xo + (r.nextFloat() - r.nextFloat()) * 0.125f, py = yo + h + (r.nextFloat() - r.nextFloat()) * 0.125f, pz = zo + (r.nextFloat() - r.nextFloat()) * 0.125f;
                if (type != 10) art.arcane.thaumcraft.client.fx.ClientHooks.drawNitorCore(px, py, pz, r.nextGaussian() * 0.01f, r.nextGaussian() * 0.01f, r.nextGaussian() * 0.01f);
                else art.arcane.thaumcraft.client.fx.ClientHooks.crucibleBubble((float) px, (float) py, (float) pz, 0.33f, 0.33f, 1.0f);
            }
        } else {
            Vec3 m = getDeltaMovement();
            setDeltaMovement(m.x, m.y - gravity, m.z);
        }
        // EntitySpecialItem.onUpdate
        if (tickCount > 1) {
            Vec3 m = getDeltaMovement();
            double y = m.y;
            if (y > 0.0) y *= 0.9f;
            y += 0.04f;
            setDeltaMovement(m.x, y, m.z);
            super.tick();
        }
    }

    @Override public boolean ignoreExplosion(Explosion explosion) { return true; }

    @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putShort("type", (short) type); }
    @Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); type = in.getShortOr("type", (short) 3); }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf data) {
        data.writeInt(target == null ? -1 : target.getId());
        data.writeDouble(targetX);
        data.writeDouble(targetY);
        data.writeDouble(targetZ);
        data.writeByte(type);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf data) {
        int ent = data.readInt();
        if (ent > -1) target = level().getEntity(ent);
        targetX = data.readDouble();
        targetY = data.readDouble();
        targetZ = data.readDouble();
        type = data.readByte();
    }
}
