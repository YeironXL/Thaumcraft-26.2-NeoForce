package art.arcane.thaumcraft.scan;

import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.research.ObservationFormula;
import art.arcane.thaumcraft.research.PlayerResearch;
import java.util.Map;

public final class ScanProgression {
    public static boolean record(PlayerResearch research,String key,Map<Aspect,Integer> aspects) {
        if (aspects.isEmpty()) return false;
        boolean discovered=false;
        if (research.scan(key)) { ObservationFormula.award(research,aspects); discovered=true; }
        for (var aspect : aspects.keySet()) if (research.unlock("!"+aspect.id())) {
            // Original ScanAspect awards one raw observation in each of these categories.
            for (String category : new String[]{"BASICS","ALCHEMY","AUROMANCY"}) research.addKnowledge("OBSERVATION",category,1);
            discovered=true;
        }
        return discovered;
    }
    private ScanProgression() {}
}
