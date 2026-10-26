package art.arcane.thaumcraft;

import art.arcane.thaumcraft.alchemy.CrucibleRecipe;
import art.arcane.thaumcraft.api.AspectList;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.research.ResearchGraph;
import java.util.*;

/** Central vanilla-independent content database. It is deliberately data-first so it can later be wired to datapack reloads. */
public final class ThaumcraftContent {
    public static final List<CrucibleRecipe> CRUCIBLE_RECIPES = new ArrayList<>();
    public static final ResearchGraph RESEARCH = new ResearchGraph();
    static {
        RESEARCH.define("BASICS");
        RESEARCH.define("THAUMOMETER", "BASICS");
        RESEARCH.define("ARCANE_WORKBENCH", "THAUMOMETER");
        RESEARCH.define("ALCHEMY", "ARCANE_WORKBENCH");
        RESEARCH.define("INFUSION", "ALCHEMY");
        RESEARCH.define("GOLEMANCY", "INFUSION");
        CRUCIBLE_RECIPES.add(new CrucibleRecipe("thaumcraft:alumentum","minecraft:coal",new AspectList().add(AspectRegistry.POTENTIA,10).add(AspectRegistry.IGNIS,10).add(AspectRegistry.PERDITIO,5),"thaumcraft:alumentum",1));
        CRUCIBLE_RECIPES.add(new CrucibleRecipe("thaumcraft:alumentum_charcoal","minecraft:charcoal",new AspectList().add(AspectRegistry.POTENTIA,10).add(AspectRegistry.IGNIS,10).add(AspectRegistry.PERDITIO,5),"thaumcraft:alumentum",1));
    }
    private ThaumcraftContent(){}
}
