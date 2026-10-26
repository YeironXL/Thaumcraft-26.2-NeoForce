package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.research.ResearchCatalog;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Last research snapshot received from the server (client-side ThaumcraftCapabilities.getKnowledge). */
public final class ClientResearch {
    private static final Set<String> researched = new HashSet<>();
    private static final Map<String, Integer> stages = new HashMap<>();
    private static final Map<String, Set<String>> flags = new HashMap<>();
    private static String raw = "{}";
    private static art.arcane.thaumcraft.research.PlayerResearch progress = new art.arcane.thaumcraft.research.PlayerResearch();

    /** Raw JSON of the last snapshot (all fields, for screens that need more than keys/stages). */
    public static com.google.gson.JsonObject snapshot() { return JsonParser.parseString(raw).getAsJsonObject(); }
    /** The last snapshot as a PlayerResearch (researched keys and stages), for ResearchRules checks. */
    public static art.arcane.thaumcraft.research.PlayerResearch progress() { return progress; }

    public static void receive(String json) {
        try {
            var o = JsonParser.parseString(json).getAsJsonObject();
            researched.clear(); stages.clear(); flags.clear(); raw = json;
            if (o.has("flags")) o.getAsJsonObject("flags").entrySet().forEach(e -> {
                Set<String> set = new HashSet<>(); e.getValue().getAsJsonArray().forEach(f -> set.add(f.getAsString())); flags.put(e.getKey(), set);
            });
            if (o.has("researched")) o.getAsJsonArray("researched").forEach(e -> researched.add(e.getAsString()));
            if (o.has("stages")) o.getAsJsonObject("stages").entrySet().forEach(e -> stages.put(e.getKey(), e.getValue().getAsInt()));
            var p = new art.arcane.thaumcraft.research.PlayerResearch();
            researched.forEach(p::unlock);
            stages.forEach(p::setStage);
            // PacketSyncWarp: the sanity checker HUD reads the three warp pools from the snapshot.
            if (o.has("permanent_warp")) p.setPermanentWarp(o.get("permanent_warp").getAsInt());
            if (o.has("normal_warp")) p.setNormalWarp(o.get("normal_warp").getAsInt());
            if (o.has("temporary_warp")) p.setTemporaryWarp(o.get("temporary_warp").getAsInt());
            progress = p;
        } catch (RuntimeException ignored) {}
    }

    public static void clear() { researched.clear(); stages.clear(); flags.clear(); }

    /** IPlayerKnowledge.hasResearchFlag (RESEARCH, PAGE, POPUP). */
    public static boolean hasFlag(String key, String flag) { var s = flags.get(key); return s != null && s.contains(flag); }
    /** IPlayerKnowledge.clearResearchFlag on the client copy (the server is told separately). */
    public static void clearFlag(String key, String flag) { var s = flags.get(key); if (s != null) s.remove(flag); }
    /** IPlayerKnowledge.getResearchStage: -1 unknown, 0 known without stage. */
    public static int stage(String key) { return researched.contains(key) ? stages.getOrDefault(key, 0) : -1; }
    /** IPlayerKnowledge.getResearchList. */
    public static Set<String> researchList() { return java.util.Collections.unmodifiableSet(researched); }
    /** ThaumcraftCapabilities.knowsResearch (non-strict: known, or stage reached with @). */
    public static boolean knowsResearch(String expression) {
        if (expression.contains("&&")) { for (String s : expression.split("&&")) if (!knowsResearch(s)) return false; return true; }
        if (expression.contains("||")) { for (String s : expression.split("\\|\\|")) if (knowsResearch(s)) return true; return false; }
        if (expression.isEmpty()) return true;
        return has(expression);
    }
    /** IPlayerKnowledge.isResearchComplete. */
    public static boolean isComplete(String key) { return complete(key); }

    private static boolean has(String key) {
        int sep = key.indexOf('@');
        if (sep < 0) return researched.contains(key);
        try { String k = key.substring(0, sep); return researched.contains(k) && stages.getOrDefault(k, 0) >= Integer.parseInt(key.substring(sep + 1)); }
        catch (NumberFormatException e) { return false; }
    }

    private static boolean complete(String key) {
        var entry = ResearchCatalog.entries().get(key);
        return researched.contains(key) && (entry == null || stages.getOrDefault(key, 0) > entry.stages().size());
    }

    /** ThaumcraftCapabilities.knowsResearchStrict. */
    public static boolean knows(String expression) {
        if (expression == null) return true;
        if (expression.contains("&&")) { for (String s : expression.split("&&")) if (!knows(s)) return false; return true; }
        if (expression.contains("||")) { for (String s : expression.split("\\|\\|")) if (knows(s)) return true; return false; }
        return expression.contains("@") ? has(expression) : complete(expression);
    }

    private ClientResearch() {}
}
