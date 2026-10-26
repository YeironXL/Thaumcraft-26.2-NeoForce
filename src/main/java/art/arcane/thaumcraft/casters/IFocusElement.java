package art.arcane.thaumcraft.casters;

/** TC6 thaumcraft.api.casters.IFocusElement. */
public interface IFocusElement {
    String getKey();
    String getResearch();
    EnumUnitType getType();
    enum EnumUnitType { EFFECT, MEDIUM, MOD, PACKAGE }
}
