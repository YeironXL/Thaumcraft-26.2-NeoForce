package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemTrait;
import java.util.ArrayList;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public class SealEmptyAdvanced extends SealEmpty implements ISealConfigToggles {
    @Override public String getKey() { return "thaumcraft:empty_advanced"; }
    @Override public int getFilterSize() { return 9; }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_empty_advanced"); }
    /** Cycle mode on a whitelist: one filter entry at a time, advancing after each job. */
    @Override public NonNullList<ItemStack> getInv(int c) {
        if (getToggles()[4].value && !isBlacklist()) {
            ArrayList<ItemStack> w = new ArrayList<>();
            for (ItemStack s : super.getInv()) if (!s.isEmpty()) w.add(s);
            if (!w.isEmpty()) return NonNullList.withSize(1, w.get(Math.abs(c % w.size())));
        }
        return super.getInv();
    }
    @Override public SealToggle[] getToggles() { return props; }
    @Override public int[] getGuiCategories() { return new int[]{1, 3, 0, 4}; }
    @Override public void setToggle(int i, boolean v) { props[i].setValue(v); }
    @Override public GolemTrait[] getRequiredTags() { return new GolemTrait[]{GolemTrait.SMART}; }
}
