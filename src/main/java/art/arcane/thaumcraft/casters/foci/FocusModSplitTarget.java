package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;
import net.minecraft.world.phys.HitResult;

public final class FocusModSplitTarget extends FocusModSplit {
    @Override public String getResearch() { return "FOCUSSPLIT"; }
    @Override public String getKey() { return "thaumcraft.SPLITTARGET"; }
    @Override public int getComplexity() { return 4; }
    @Override public EnumSupplyType[] mustBeSupplied() { return new EnumSupplyType[] {EnumSupplyType.TARGET}; }
    @Override public EnumSupplyType[] willSupply() { return new EnumSupplyType[] {EnumSupplyType.TARGET}; }
    @Override public HitResult[] supplyTargets() { return getParent().supplyTargets(); }
    @Override public boolean execute() { return true; }
}
