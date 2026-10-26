package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemTrait;
import net.minecraft.resources.Identifier;

public class SealGuardAdvanced extends SealGuard implements ISealConfigToggles {
    @Override public String getKey() { return "thaumcraft:guard_advanced"; }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_guard_advanced"); }
    @Override public SealToggle[] getToggles() { return props; }
    @Override public void setToggle(int i, boolean v) { props[i].setValue(v); }
    @Override public int[] getGuiCategories() { return new int[]{2, 3, 0, 4}; }
    @Override public GolemTrait[] getRequiredTags() { return new GolemTrait[]{GolemTrait.FIGHTER, GolemTrait.SMART}; }
}
