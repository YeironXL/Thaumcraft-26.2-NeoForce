package art.arcane.thaumcraft.research;

import java.util.Arrays;

/** Legacy keys beginning with ! are literal discovery keys, not negation operators. */
public final class ResearchRules {
    public static boolean complete(PlayerResearch state, String key) {
        var entry = ResearchCatalog.entries().get(key);
        return state.has(key) && (entry == null || state.stage(key) > entry.stages().size());
    }
    public static boolean knows(PlayerResearch state, String expression) {
        if (expression.contains("||")) return Arrays.stream(expression.split("\\|\\|")).anyMatch(key -> knows(state, key));
        if (expression.contains("&&")) return Arrays.stream(expression.split("&&")).allMatch(key -> knows(state, key));
        return expression.contains("@") ? state.has(expression) : complete(state, expression);
    }
    public static boolean available(PlayerResearch state, ResearchCatalog.Entry entry) {
        // ResearchManager.doesPlayerHaveRequisites: parents with the "~" prefix stripped.
        return entry.parents().stream().allMatch(parent -> knows(state, parent.startsWith("~") ? parent.substring(1) : parent));
    }
    public static boolean visible(PlayerResearch state, ResearchCatalog.Entry entry) {
        return visible(state, entry, new java.util.HashSet<>());
    }
    private static boolean visible(PlayerResearch state, ResearchCatalog.Entry entry, java.util.Set<String> visiting) {
        if (state.has(entry.key())) return true;
        if (!visiting.add(entry.key())) return false;
        if (entry.meta().contains("HIDDEN") && (entry.parents().isEmpty() || !available(state, entry))) return false;
        for (String key : entry.parents()) {
            var parent = ResearchCatalog.entries().get(key);
            if (parent != null && !visible(state, parent, visiting)) return false;
        }
        visiting.remove(entry.key());
        return true;
    }
    public static int knowledgeCost(String requirement) {
        String[] parts = requirement.split(";");
        if (parts.length != 3 || !ResearchCatalog.CATEGORIES.contains(parts[1])) return -1;
        int progression = switch (parts[0]) { case "OBSERVATION" -> 16; case "THEORY" -> 32; default -> -1; };
        try { int amount = Integer.parseInt(parts[2]); return amount > 0 && progression > 0 ? Math.multiplyExact(amount, progression) : -1; }
        catch (IllegalArgumentException | ArithmeticException exception) { return -1; }
    }
    private ResearchRules() {}
}
