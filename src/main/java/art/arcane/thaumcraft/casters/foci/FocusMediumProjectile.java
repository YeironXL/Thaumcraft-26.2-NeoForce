package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.entity.FocusProjectileEntity;

public final class FocusMediumProjectile extends FocusMedium {
    @Override public String getResearch() { return "FOCUSPROJECTILE@2"; }
    @Override public String getKey() { return "thaumcraft.PROJECTILE"; }
    @Override public int getComplexity() {
        int c = 4 + (getSettingValue("speed") - 1) / 2;
        switch (getSettingValue("option")) { case 1 -> c += 3; case 2, 3 -> c += 5; default -> {} }
        return c;
    }
    @Override public EnumSupplyType[] willSupply() { return new EnumSupplyType[] {EnumSupplyType.TARGET, EnumSupplyType.TRAJECTORY}; }
    @Override
    public boolean execute(Trajectory trajectory) {
        float speed = getSettingValue("speed") / 3.0f;
        FocusPackage p = getRemainingPackage();
        if (p != null && p.getCaster() != null) return getPackage().getCaster().level().addFreshEntity(new FocusProjectileEntity(p, speed, trajectory, getSettingValue("option")));
        return false;
    }
    @Override public boolean hasIntermediary() { return true; }
    @Override public NodeSetting[] createSettings() {
        return new NodeSetting[] {
                new NodeSetting("option", "focus.common.options", new NodeSetting.NodeSettingIntList(new int[] {0, 1, 2, 3},
                        new String[] {"focus.common.none", "focus.projectile.bouncy", "focus.projectile.seeking.hostile", "focus.projectile.seeking.friendly"}), "FOCUSPROJECTILE"),
                new NodeSetting("speed", "focus.projectile.speed", new NodeSetting.NodeSettingIntRange(1, 5))};
    }
    @Override public String getAspect() { return "motus"; }
}
