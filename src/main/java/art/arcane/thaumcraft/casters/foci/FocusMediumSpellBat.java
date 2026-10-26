package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.entity.SpellBatEntity;

public final class FocusMediumSpellBat extends FocusMedium {
    @Override public String getResearch() { return "FOCUSSPELLBAT"; }
    @Override public String getKey() { return "thaumcraft.SPELLBAT"; }
    @Override public String getAspect() { return "bestia"; }
    @Override public int getComplexity() { return 8; }
    @Override public EnumSupplyType[] willSupply() { return new EnumSupplyType[] {EnumSupplyType.TARGET}; }
    @Override
    public boolean execute(Trajectory trajectory) {
        FocusPackage p = getRemainingPackage();
        if (p == null) return false;
        SpellBatEntity bat = new SpellBatEntity(p, getSettingValue("target") == 1);
        bat.setPos(trajectory.source.x, trajectory.source.y, trajectory.source.z);
        return getPackage().getCaster().level().addFreshEntity(bat);
    }
    @Override public boolean hasIntermediary() { return true; }
    @Override public float getPowerMultiplier() { return 0.33f; }
    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {new NodeSetting("target", "focus.common.target", new NodeSetting.NodeSettingIntList(new int[] {0, 1}, new String[] {"focus.common.enemy", "focus.common.friend"}))};
    }
}
