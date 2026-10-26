package art.arcane.thaumcraft.research;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Server-side-independent research progression model used by Thaumonomicon integrations. */
public final class ResearchState {
    private final Set<String> completed = new LinkedHashSet<>();
    private final Set<String> scanned = new LinkedHashSet<>();

    public boolean complete(String key) { return completed.add(key); }
    public boolean hasResearch(String key) { return completed.contains(key); }
    public boolean scan(String key) { return scanned.add(key); }
    public boolean hasScan(String key) { return scanned.contains(key); }
    public Set<String> completedResearch() { return Collections.unmodifiableSet(completed); }
    public Set<String> scannedObjects() { return Collections.unmodifiableSet(scanned); }
}
