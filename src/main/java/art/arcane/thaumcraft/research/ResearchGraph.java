package art.arcane.thaumcraft.research;

import java.util.*;

/** Dependency-aware research graph matching Thaumcraft's gated progression. */
public final class ResearchGraph {
    private final Map<String,Set<String>> parents=new LinkedHashMap<>();
    public void define(String key,String... prerequisites){parents.put(key,new LinkedHashSet<>(List.of(prerequisites)));}
    public boolean unlocked(String key, Set<String> completed){return parents.containsKey(key) && (completed.contains(key) || completed.containsAll(parents.get(key)));}
    public Set<String> prerequisites(String key){return Collections.unmodifiableSet(parents.getOrDefault(key,Set.of()));}
    public Set<String> keys(){return Collections.unmodifiableSet(parents.keySet());}
}
