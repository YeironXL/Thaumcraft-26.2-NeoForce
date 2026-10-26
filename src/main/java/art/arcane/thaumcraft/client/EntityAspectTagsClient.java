package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.item.ThaumometerItem;
import art.arcane.thaumcraft.scan.EntityScanRegistry;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * TC6 ItemThaumometer.onUpdate (client, every 5 ticks: thaumTarget = EntityUtils.getPointedEntity(..., 16)) and
 * RenderEventHandler.renderLast thaumTarget branch: the target's AspectHelper.getEntityAspects drawn above its head
 * with drawTagsOnContainer (rows of 5, tagscale growing to 0.5 and decaying 0.005 per frame).
 */
public final class EntityAspectTagsClient {
    private static Entity thaumTarget;

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
        NeoForge.EVENT_BUS.addListener(EntityAspectTagsClient::render);
    }

    public static Entity target() { return thaumTarget; }

    private static boolean holding(Player p) {
        return p.getMainHandItem().getItem() instanceof ThaumometerItem || p.getOffhandItem().getItem() instanceof ThaumometerItem;
    }

    private static void tick() {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { thaumTarget = null; return; }
        if (!holding(mc.player)) { thaumTarget = null; return; }
        if (mc.player.tickCount % 5 != 0) return;
        thaumTarget = pointed(mc.player, 16.0);
    }

    /** EntityUtils.getPointedEntity(world, player, 1.0, range, 5.0f padding... , true): nearest entity along the look ray, blocks stop it. */
    private static Entity pointed(Player player, double range) {
        Vec3 start = player.getEyePosition();
        var block = player.pick(range, 0, false);
        double dist = block.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? range : Math.min(range, start.distanceTo(block.getLocation()));
        Vec3 dir = player.getViewVector(1).scale(dist);
        var hit = ProjectileUtil.getEntityHitResult(player, start, start.add(dir), player.getBoundingBox().expandTowards(dir).inflate(1),
                e -> !e.isSpectator() && e.isPickable(), dist * dist);
        return hit == null ? null : hit.getEntity();
    }

    private static void render(SubmitCustomGeometryEvent e) {
        if (AspectTagsRender.tagscale > 0) AspectTagsRender.tagscale -= 0.005f;
        var mc = Minecraft.getInstance();
        if (mc.player == null || thaumTarget == null || thaumTarget.isRemoved()) return;
        Map<Aspect, Integer> ot = EntityScanRegistry.aspects(thaumTarget);
        if (ot == null || ot.isEmpty()) return;
        AspectTagsRender.grow(0.5f);
        float pt = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 p = thaumTarget.getPosition(pt);
        AspectTagsRender.drawTagsOnContainer(e, p.x, p.y + thaumTarget.getBbHeight(), p.z, ot, null);
    }

    private EntityAspectTagsClient() {}
}
