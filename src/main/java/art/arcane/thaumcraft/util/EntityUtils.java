package art.arcane.thaumcraft.util;

import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** TC6 common.lib.utils.EntityUtils (subset). */
public final class EntityUtils {
    /** Looting level of the killer behind a damage source (0 when it is not a living entity). */
    public static int lootingLevel(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        if (!(source.getEntity() instanceof net.minecraft.world.entity.LivingEntity le)) return 0;
        var ench = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(net.minecraft.world.item.enchantment.Enchantments.LOOTING);
        return ench.map(h -> net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(h, le)).orElse(0);
    }
    /** Block ray trace ignoring non-colliding blocks (rayTraceBlocks(start,end,false,true,false)); null when nothing is hit. */
    public static HitResult rayTraceBlocks(Level world, Vec3 start, Vec3 end, Entity source) {
        var r = world.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source == null ? net.minecraft.world.phys.shapes.CollisionContext.empty() : net.minecraft.world.phys.shapes.CollisionContext.of(source)));
        return r.getType() == HitResult.Type.MISS ? null : r;
    }

    public static EntityHitResult getPointedEntityRay(Level world, Entity ignoreEntity, Vec3 startVec, Vec3 lookVec, double minrange, double range, float padding, boolean nonCollide) {
        EntityHitResult pointed = null;
        Vec3 end = startVec.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);
        AABB bb = ignoreEntity != null ? ignoreEntity.getBoundingBox() : new AABB(startVec, startVec).inflate(0.5);
        List<Entity> list = world.getEntities(ignoreEntity, bb.expandTowards(lookVec.x * range, lookVec.y * range, lookVec.z * range).inflate(padding));
        double d2 = 0.0;
        for (Entity entity : list) {
            if (startVec.distanceTo(entity.position()) < minrange) continue;
            if (!(entity.isPickable() || nonCollide)) continue;
            if (rayTraceBlocks(world, startVec, new Vec3(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ()), ignoreEntity) != null) continue;
            float f2 = Math.max(0.8f, entity.getPickRadius());
            AABB box = entity.getBoundingBox().inflate(f2);
            var hit = box.clip(startVec, end);
            if (box.contains(startVec)) {
                if (0.0 < d2 || d2 == 0.0) { pointed = new EntityHitResult(entity, hit.orElse(startVec)); d2 = 0.0; }
            } else if (hit.isPresent()) {
                double d3 = startVec.distanceTo(hit.get());
                if (d3 < d2 || d2 == 0.0) { pointed = new EntityHitResult(entity, hit.get()); d2 = d3; }
            }
        }
        return pointed;
    }

    public static boolean isFriendly(Entity source, Entity target) {
        if (source == null || target == null) return false;
        if (source.getId() == target.getId()) return true;
        if (source.isPassengerOfSameVehicle(target) || source.hasPassenger(target) || target.hasPassenger(source)) return true;
        if (source.isAlliedTo(target)) return true;
        if (target instanceof OwnableEntity o && o.getOwner() != null && o.getOwner().equals(source)) return true;
        if (target.level() instanceof net.minecraft.server.level.ServerLevel sl && target instanceof Player && !sl.isPvpAllowed()) return true;
        return false;
    }

    public static <T extends Entity> List<T> getEntitiesInRangeSorted(Level world, Entity entity, Class<T> cls, double range) {
        AABB box = new AABB(entity.position(), entity.position()).inflate(range);
        return world.getEntitiesOfClass(cls, box, e -> e.getId() != entity.getId()).stream()
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(entity))).toList();
    }

    public static boolean isVisibleTo(float fov, Entity ent, Entity ent2, float range) {
        double[] x = {ent2.getX(), ent2.getBoundingBox().minY + ent2.getBbHeight() / 2.0f, ent2.getZ()};
        double[] t = {ent.getX(), ent.getBoundingBox().minY + ent.getEyeHeight(), ent.getZ()};
        Vec3 q = ent.getLookAngle().scale(range);
        double[] b = {q.x + ent.getX(), q.y + ent.getBoundingBox().minY + ent.getEyeHeight(), q.z + ent.getZ()};
        return isLyingInCone(x, t, b, fov);
    }

    public static boolean canEntityBeSeen(Entity looking, Entity target) {
        return rayTraceBlocks(looking.level(), new Vec3(looking.getX(), looking.getY() + looking.getBbHeight() / 2.0f, looking.getZ()),
                new Vec3(target.getX(), target.getY() + target.getBbHeight() / 2.0f, target.getZ()), looking) == null;
    }

    public static boolean isLyingInCone(double[] x, double[] t, double[] b, float aperture) {
        double half = aperture / 2.0f;
        double[] ax = {t[0] - x[0], t[1] - x[1], t[2] - x[2]};
        double[] axis = {t[0] - b[0], t[1] - b[1], t[2] - b[2]};
        double dot = ax[0] * axis[0] + ax[1] * axis[1] + ax[2] * axis[2];
        double ma = Math.sqrt(ax[0] * ax[0] + ax[1] * ax[1] + ax[2] * ax[2]), mb = Math.sqrt(axis[0] * axis[0] + axis[1] * axis[1] + axis[2] * axis[2]);
        if (!(dot / ma / mb > Math.cos(half))) return false;
        return dot / mb < mb;
    }

    private EntityUtils() {}
}
