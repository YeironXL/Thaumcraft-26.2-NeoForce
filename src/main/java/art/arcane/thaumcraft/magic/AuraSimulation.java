package art.arcane.thaumcraft.magic;

import art.arcane.thaumcraft.aspect.Aspect;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tickable aura simulation. Positive Vis regenerates slowly; Flux is dissipated over time. */
public final class AuraSimulation {
    private final Map<Aspect, Integer> vis = new LinkedHashMap<>();
    private int flux;
    public void addVis(Aspect a, int amount) { if (amount > 0) vis.merge(a, amount, Integer::sum); }
    public int vis(Aspect a) { return vis.getOrDefault(a, 0); }
    public int flux() { return flux; }
    public void addFlux(int amount) { flux = Math.max(0, flux + Math.max(0, amount)); }
    public void tick() { if (flux > 0) flux--; }
}
