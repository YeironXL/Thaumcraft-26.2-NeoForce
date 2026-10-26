package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.util.EntityUtils;
import java.util.ArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FocusMediumTouch extends FocusMedium {
    @Override public String getResearch() { return "BASEAUROMANCY"; }
    @Override public String getKey() { return "thaumcraft.TOUCH"; }
    @Override public int getComplexity() { return 2; }
    @Override public EnumSupplyType[] willSupply() { return new EnumSupplyType[] {EnumSupplyType.TRAJECTORY, EnumSupplyType.TARGET}; }
    @Override public String getAspect() { return "aversio"; }

    protected double range() {
        return this instanceof FocusMediumBolt ? 16.0 : (getPackage().getCaster() instanceof Player p ? p.blockInteractionRange() : 4.5);
    }

    @Override
    public Trajectory[] supplyTrajectories() {
        if (getParent() == null) return new Trajectory[0];
        ArrayList<Trajectory> out = new ArrayList<>();
        double range = range();
        for (Trajectory sT : getParent().supplyTrajectories()) {
            Vec3 end = sT.direction.normalize();
            HitResult ray = EntityUtils.getPointedEntityRay(getPackage().world, getPackage().getCaster(), sT.source, end, 0.25, range, 0.25f, false);
            if (ray == null) {
                end = end.scale(range).add(sT.source);
                ray = EntityUtils.rayTraceBlocks(getPackage().world, sT.source, end, getPackage().getCaster());
                if (ray != null) end = ray.getLocation();
            } else if (ray instanceof EntityHitResult eh) {
                end = end.scale(sT.source.distanceTo(eh.getEntity().position())).add(sT.source);
            }
            out.add(new Trajectory(end, sT.direction.normalize()));
        }
        return out.toArray(new Trajectory[0]);
    }

    @Override
    public HitResult[] supplyTargets() {
        if (getParent() == null || !(getPackage().getCaster() instanceof Player)) return new HitResult[0];
        ArrayList<HitResult> out = new ArrayList<>();
        double range = range();
        for (Trajectory sT : getParent().supplyTrajectories()) {
            Vec3 end = sT.direction.normalize();
            HitResult ray = EntityUtils.getPointedEntityRay(getPackage().world, getPackage().getCaster(), sT.source, end, 0.25, range, 0.25f, false);
            if (ray == null) ray = EntityUtils.rayTraceBlocks(getPackage().world, sT.source, end.scale(range).add(sT.source), getPackage().getCaster());
            if (ray != null) out.add(ray);
        }
        return out.toArray(new HitResult[0]);
    }

    @Override
    public boolean execute(Trajectory trajectory) {
        FocusEffect[] fe = getPackage().getFocusEffects();
        if (fe != null && fe.length > 0)
            FocusFx.effect(getPackage().world, trajectory.source, new Vec3(trajectory.direction.x / 2.0f, trajectory.direction.y / 2.0f, trajectory.direction.z / 2.0f), FocusFx.keys(fe));
        return true;
    }
}
