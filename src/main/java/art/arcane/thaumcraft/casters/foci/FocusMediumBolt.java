package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.util.EntityUtils;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class FocusMediumBolt extends FocusMediumTouch {
    @Override public String getResearch() { return "FOCUSBOLT"; }
    @Override public String getKey() { return "thaumcraft.BOLT"; }
    @Override public int getComplexity() { return 5; }
    @Override public String getAspect() { return "potentia"; }

    @Override
    public boolean execute(Trajectory trajectory) {
        float range = 16.0f;
        Vec3 end = trajectory.direction.normalize();
        HitResult ray = EntityUtils.getPointedEntityRay(getPackage().world, getPackage().getCaster(), trajectory.source, end, 0.25, range, 0.25f, false);
        if (ray == null) {
            end = end.scale(range).add(trajectory.source);
            ray = EntityUtils.rayTraceBlocks(getPackage().world, trajectory.source, end, getPackage().getCaster());
            if (ray != null) end = ray.getLocation();
        } else if (ray instanceof EntityHitResult eh) {
            end = end.scale(trajectory.source.distanceTo(eh.getEntity().position())).add(trajectory.source);
        }
        int r = 0, g = 0, b = 0;
        FocusEffect[] fe = getPackage().getFocusEffects();
        for (FocusEffect ef : fe) { int c = FocusEngine.getElementColor(ef.getKey()); r += (c >> 16) & 255; g += (c >> 8) & 255; b += c & 255; }
        if (fe.length > 0) { r /= fe.length; g /= fe.length; b /= fe.length; }
        FocusFx.zap(getPackage().world, trajectory.source, end, (r << 16) | (g << 8) | b, getPackage().getPower() * 0.66f);
        return true;
    }
}
