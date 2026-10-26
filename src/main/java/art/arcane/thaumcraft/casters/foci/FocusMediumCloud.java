package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.entity.FocusCloudEntity;

public final class FocusMediumCloud extends FocusMedium {
    @Override public String getResearch() { return "FOCUSCLOUD"; }
    @Override public String getKey() { return "thaumcraft.CLOUD"; }
    @Override public String getAspect() { return "alkimia"; }
    @Override public int getComplexity() { return 4 + getSettingValue("radius") * 2 + getSettingValue("duration") / 5; }
    @Override public EnumSupplyType[] willSupply() { return new EnumSupplyType[] {EnumSupplyType.TARGET}; }
    @Override
    public boolean execute(Trajectory trajectory) {
        FocusPackage p = getRemainingPackage();
        if (p == null) return false;
        return getPackage().getCaster().level().addFreshEntity(new FocusCloudEntity(p, trajectory, getSettingValue("radius"), getSettingValue("duration")));
    }
    @Override public boolean hasIntermediary() { return true; }
    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("radius", "focus.common.radius", new NodeSetting.NodeSettingIntRange(1, 3)),
                new NodeSetting("duration", "focus.common.duration", new NodeSetting.NodeSettingIntRange(5, 30))};
    }
    @Override public float getPowerMultiplier() { return 0.5f; }
}
