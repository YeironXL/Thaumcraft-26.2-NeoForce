package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.entity.FocusMineEntity;

public final class FocusMediumMine extends FocusMedium {
    @Override public String getResearch() { return "FOCUSMINE"; }
    @Override public String getKey() { return "thaumcraft.MINE"; }
    @Override public int getComplexity() { return 4; }
    @Override public String getAspect() { return "vinculum"; }
    @Override public EnumSupplyType[] willSupply() { return new EnumSupplyType[] {EnumSupplyType.TARGET, EnumSupplyType.TRAJECTORY}; }
    @Override
    public boolean execute(Trajectory trajectory) {
        FocusPackage p = getRemainingPackage();
        if (p == null) return false;
        return getPackage().getCaster().level().addFreshEntity(new FocusMineEntity(p, trajectory, getSettingValue("target") == 1));
    }
    @Override public boolean hasIntermediary() { return true; }
    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("target", "focus.common.target", new NodeSetting.NodeSettingIntList(new int[] {0, 1}, new String[] {"focus.common.enemy", "focus.common.friend"}))};
    }
}
