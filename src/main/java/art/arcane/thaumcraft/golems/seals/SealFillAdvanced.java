package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemTrait;
import net.minecraft.resources.Identifier;

public class SealFillAdvanced extends SealFill implements ISealConfigToggles {
    @Override public String getKey() { return "thaumcraft:fill_advanced"; }
    @Override public int getFilterSize() { return 9; }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_fill_advanced"); }
    @Override public int[] getGuiCategories() { return new int[]{1, 3, 0, 4}; }
    @Override public SealToggle[] getToggles() { return props; }
    @Override public void setToggle(int i, boolean v) { props[i].setValue(v); }
    @Override public GolemTrait[] getRequiredTags() { return new GolemTrait[]{GolemTrait.SMART}; }
}
