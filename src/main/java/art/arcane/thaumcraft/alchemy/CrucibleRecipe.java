package art.arcane.thaumcraft.alchemy;

import art.arcane.thaumcraft.api.AspectList;
import java.util.*;

/** Data model for a Thaumcraft-style crucible recipe. */
public record CrucibleRecipe(String id, String catalystItem, AspectList required, String resultItem, int resultCount) {
    public CrucibleRecipe { Objects.requireNonNull(id); Objects.requireNonNull(catalystItem); Objects.requireNonNull(required); Objects.requireNonNull(resultItem); if(resultCount<1) throw new IllegalArgumentException("resultCount"); }
    public boolean matches(String catalyst, AspectList aura) { return catalystItem.equals(catalyst) && aura.containsAll(required); }
}
