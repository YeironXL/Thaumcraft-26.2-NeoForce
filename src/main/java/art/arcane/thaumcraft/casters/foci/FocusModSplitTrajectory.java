package art.arcane.thaumcraft.casters.foci;

import art.arcane.thaumcraft.casters.*;

public final class FocusModSplitTrajectory extends FocusModSplit {
    @Override public String getResearch() { return "FOCUSSPLIT"; }
    @Override public String getKey() { return "thaumcraft.SPLITTRAJECTORY"; }
    @Override public int getComplexity() { return 5; }
    @Override public EnumSupplyType[] mustBeSupplied() { return new EnumSupplyType[] {EnumSupplyType.TRAJECTORY}; }
    @Override public EnumSupplyType[] willSupply() { return new EnumSupplyType[] {EnumSupplyType.TRAJECTORY}; }
    @Override public Trajectory[] supplyTrajectories() { return getParent().supplyTrajectories(); }
    @Override public boolean execute() { return true; }
}
