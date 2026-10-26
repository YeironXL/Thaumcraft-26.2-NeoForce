package art.arcane.thaumcraft.aura;

import java.util.LinkedHashMap;
import java.util.Map;
import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.aspect.AspectRegistry;

/** In-memory aura model for a chunk. Persistence/network integration comes next. */
public final class AuraChunk {
    private final Map<Aspect, Integer> vis = new LinkedHashMap<>();
    private int flux;

    public AuraChunk() {
        for (Aspect aspect : AspectRegistry.all().values()) vis.put(aspect, 0);
    }

    public int getVis(Aspect aspect) { return vis.getOrDefault(aspect, 0); }
    public void addVis(Aspect aspect, int amount) {
        vis.put(aspect, Math.max(0, getVis(aspect) + amount));
    }
    public int getFlux() { return flux; }
    public void addFlux(int amount) { flux = Math.max(0, flux + amount); }
}
