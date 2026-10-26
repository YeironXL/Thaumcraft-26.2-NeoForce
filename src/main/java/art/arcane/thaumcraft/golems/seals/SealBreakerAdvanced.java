package art.arcane.thaumcraft.golems.seals;

import art.arcane.thaumcraft.golems.GolemTrait;
import net.minecraft.resources.Identifier;

public class SealBreakerAdvanced extends SealBreaker {
    public SealBreakerAdvanced() { props = new SealToggle[]{new SealToggle(true, "pmeta", "golem.prop.meta"), new SealToggle(false, "psilk", "golem.prop.silk")}; }
    @Override public String getKey() { return "thaumcraft:breaker_advanced"; }
    @Override public int getFilterSize() { return 9; }
    @Override public Identifier getSealIcon() { return Identifier.fromNamespaceAndPath("thaumcraft", "item/seals/seal_breaker_advanced"); }
    @Override public GolemTrait[] getRequiredTags() { return new GolemTrait[]{GolemTrait.BREAKER, GolemTrait.SMART}; }
}
