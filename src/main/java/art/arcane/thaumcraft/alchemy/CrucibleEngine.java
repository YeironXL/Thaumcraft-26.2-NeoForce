package art.arcane.thaumcraft.alchemy;

import art.arcane.thaumcraft.api.AspectContainer;
import art.arcane.thaumcraft.aspect.Aspect;
import java.util.ArrayList;
import java.util.List;

/** Server-safe recipe engine. A recipe only fires when all required aspects exist. */
public final class CrucibleEngine {
    public record Requirement(Aspect aspect, int amount) {}
    public record Result(String itemId, int count) {}
    public record Recipe(String id, List<Requirement> requirements, Result result) {}

    private final List<Recipe> recipes = new ArrayList<>();
    public void add(Recipe recipe) { recipes.add(recipe); }
    public List<Recipe> recipes() { return List.copyOf(recipes); }

    public Result craft(AspectContainer contents) {
        for (Recipe recipe : recipes) {
            boolean ok = true;
            for (Requirement req : recipe.requirements()) {
                if (!contents.contains(req.aspect(), req.amount())) { ok = false; break; }
            }
            if (!ok) continue;
            for (Requirement req : recipe.requirements()) contents.consume(req.aspect(), req.amount());
            return recipe.result();
        }
        return null;
    }
}
