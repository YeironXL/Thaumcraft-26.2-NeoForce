package art.arcane.thaumcraft.scan;

import art.arcane.thaumcraft.aspect.Aspect;
import java.util.*;

public record ScanResult(String target, Map<Aspect,Integer> aspects) {
    public ScanResult { target=Objects.requireNonNull(target); aspects=Map.copyOf(aspects); }
    public int total(){return aspects.values().stream().mapToInt(Integer::intValue).sum();}
}
