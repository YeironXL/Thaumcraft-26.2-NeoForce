package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import java.util.ArrayList;
import net.minecraft.world.phys.Vec3;

public final class FocusModScatter extends FocusMod {
    @Override public String getResearch() { return "FOCUSSCATTER"; }
    @Override public String getKey() { return "thaumcraft.SCATTER"; }
    @Override public int getComplexity() { return (int) Math.max(2.0f, 2.0f * (getSettingValue("forks") - getSettingValue("cone") / 45.0f)); }
    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("forks", "focus.scatter.forks", new NodeSetting.NodeSettingIntRange(2, 10)),
                new NodeSetting("cone", "focus.scatter.cone", new NodeSetting.NodeSettingIntList(new int[] {10, 30, 60, 90, 180, 270, 360}, new String[] {"10", "30", "60", "90", "180", "270", "360"}))};
    }
    @Override public EnumSupplyType[] mustBeSupplied() { return new EnumSupplyType[] {EnumSupplyType.TRAJECTORY}; }
    @Override public EnumSupplyType[] willSupply() { return new EnumSupplyType[] {EnumSupplyType.TRAJECTORY}; }
    @Override
    public Trajectory[] supplyTrajectories() {
        if (getParent() == null) return new Trajectory[0];
        ArrayList<Trajectory> tT = new ArrayList<>();
        int forks = getSettingValue("forks"), angle = getSettingValue("cone");
        var r = getPackage().world.getRandom();
        if (getParent().supplyTrajectories() != null)
            for (Trajectory sT : getParent().supplyTrajectories())
                for (int a = 0; a < forks; ++a) {
                    Vec3 dV = sT.direction.normalize().add(r.nextGaussian() * 0.0075 * angle, r.nextGaussian() * 0.0075 * angle, r.nextGaussian() * 0.0075 * angle);
                    tT.add(new Trajectory(sT.source, dV.normalize()));
                }
        return tT.toArray(new Trajectory[0]);
    }
    @Override public float getPowerMultiplier() { return 1.0f / (getSettingValue("forks") / 2.0f); }
    @Override public boolean execute() { return true; }
    @Override public boolean isExclusive() { return true; }
}
