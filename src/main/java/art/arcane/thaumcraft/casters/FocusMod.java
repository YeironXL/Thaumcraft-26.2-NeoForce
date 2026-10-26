package art.arcane.thaumcraft.casters;

/** TC6 FocusMod. */
public abstract class FocusMod extends FocusNode {
    @Override public EnumUnitType getType() { return EnumUnitType.MOD; }
    public abstract boolean execute();
    @Override public String getAspect() { return null; }
}
