package art.arcane.thaumcraft.research;

import java.util.List;

/** Original ItemCurio variants, represented by separate modern item registrations. */
public final class CurioTheory {
    public static final List<String> IDS=List.of("thaumcraft:curio","thaumcraft:curio_arcane","thaumcraft:curio_preserved","thaumcraft:curio_ancient","thaumcraft:curio_eldritch","thaumcraft:curio_knowledge","thaumcraft:curio_twisted","thaumcraft:curio_rites");
    public static final List<String> CATEGORIES=List.of("BASICS","AUROMANCY","ALCHEMY","GOLEMANCY","ELDRITCH","INFUSION","ARTIFICE","ELDRITCH");
    public static List<Integer> available(TheoryResources resources){return resources.curioCandidates();}
    private CurioTheory(){}
}
