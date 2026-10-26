package art.arcane.thaumcraft.api;

import art.arcane.thaumcraft.aspect.Aspect;
import java.util.*;

/** Immutable-friendly aspect container matching Thaumcraft's additive aspect semantics. */
public final class AspectList {
    private final LinkedHashMap<Aspect,Integer> values = new LinkedHashMap<>();
    public AspectList add(Aspect aspect, int amount) { if (aspect != null && amount > 0) values.merge(aspect, amount, Integer::sum); return this; }
    public AspectList remove(Aspect aspect, int amount) { if (aspect != null && amount > 0) { int n=values.getOrDefault(aspect,0)-amount; if(n>0) values.put(aspect,n); else values.remove(aspect);} return this; }
    public int get(Aspect aspect) { return values.getOrDefault(aspect,0); }
    public boolean contains(Aspect aspect, int amount) { return get(aspect)>=amount; }
    public boolean containsAll(AspectList other) { for(var e:other.values.entrySet()) if(get(e.getKey())<e.getValue()) return false; return true; }
    public int total() { return values.values().stream().mapToInt(Integer::intValue).sum(); }
    public boolean isEmpty(){return values.isEmpty();}
    public Map<Aspect,Integer> asMap(){return Collections.unmodifiableMap(values);}
    public AspectList copy(){var x=new AspectList(); values.forEach(x::add); return x;}
    public AspectList subtract(AspectList other){var x=copy(); other.values.forEach(x::remove); return x;}
    @Override public String toString(){return values.toString();}
}
