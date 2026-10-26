package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemTrait;
import net.minecraft.resources.Identifier;

public class SealPickupAdvanced extends SealPickup implements ISealConfigToggles {
    @Override public String getKey() { return "thaumcraft:pickup_advanced"; }
    @Override public int getFilterSize() { return 9; }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_pickup_advanced"); }
    @Override public int[] getGuiCategories() { return new int[]{2, 1, 3, 0, 4}; }
    @Override public GolemTrait[] getRequiredTags() { return new GolemTrait[]{GolemTrait.SMART}; }
    @Override public SealToggle[] getToggles() { return props; }
    @Override public void setToggle(int i, boolean v) { props[i].setValue(v); }
}
