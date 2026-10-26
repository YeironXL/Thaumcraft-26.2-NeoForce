package art.arcane.thaumcraft.research;

import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import java.util.Map;

/** TC6 category weights: ceil(sqrt(sum(aspect quantity * weight / 10))). */
public final class ObservationFormula {
    private static final Map<String, Map<String, Integer>> WEIGHTS = Map.of(
        "BASICS", Map.of("herba",5,"ordo",5,"perditio",5,"aer",5,"ignis",5,"terra",3,"aqua",5),
        "AUROMANCY", Map.of("auram",20,"praecantatio",20,"vitium",15,"vitreus",5,"gelum",5,"aer",5),
        "ALCHEMY", Map.of("alkimia",30,"vitium",10,"praecantatio",10,"victus",5,"aversio",5,"desiderium",5,"aqua",5),
        "ARTIFICE", Map.of("machina",10,"fabrico",10,"metallum",10,"instrumentum",10,"potentia",10,"lux",5,"volatus",5,"vinculum",5,"ignis",5),
        "INFUSION", Map.of("praecantatio",30,"praemunio",10,"instrumentum",10,"vitium",5,"fabrico",5,"spiritus",5,"terra",3),
        "GOLEMANCY", Map.of("humanus",20,"motus",10,"cognitio",10,"machina",10,"permutatio",5,"sensus",5,"bestia",5,"ordo",5),
        "ELDRITCH", Map.of("alienis",20,"tenebrae",10,"praecantatio",5,"cognitio",5,"vacuos",5,"mortuus",5,"exanimis",5,"perditio",5));
    public static int calculate(String category, Map<Aspect, Integer> aspects) {
        double sum = 0;
        for (var weight : WEIGHTS.getOrDefault(category, Map.of()).entrySet())
            sum += Math.max(0, aspects.getOrDefault(AspectRegistry.get(weight.getKey()), 0)) * (weight.getValue() / 10.0);
        return (int)Math.ceil(Math.sqrt(sum));
    }
    public static void award(PlayerResearch research, Map<Aspect, Integer> aspects) {
        for (String category : ResearchCatalog.CATEGORIES) research.addKnowledge("OBSERVATION", category, calculate(category, aspects));
    }
    private ObservationFormula() {}
}
