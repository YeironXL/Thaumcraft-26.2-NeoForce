package art.arcane.thaumcraft.api;

import art.arcane.thaumcraft.aspect.Aspect;
import java.util.LinkedHashMap;
import java.util.Map;

/** Deterministic aspect storage used by scans and recipes. */
public final class AspectContainer {
    private final Map<Aspect, Integer> values = new LinkedHashMap<>();
    public void add(Aspect aspect, int amount) {
        if (amount <= 0) return;
        values.merge(aspect, amount, Integer::sum);
    }
    public int get(Aspect aspect) { return values.getOrDefault(aspect, 0); }
    public boolean contains(Aspect aspect, int amount) { return amount >= 0 && get(aspect) >= amount; }
    public boolean consume(Aspect aspect, int amount) {
        if (!contains(aspect, amount)) return false;
        int left = get(aspect) - amount;
        if (left == 0) values.remove(aspect); else values.put(aspect, left);
        return true;
    }
    public Map<Aspect, Integer> snapshot() { return Map.copyOf(values); }
    public void clear() { values.clear(); }
}
