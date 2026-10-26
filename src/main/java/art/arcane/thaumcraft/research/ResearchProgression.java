package art.arcane.thaumcraft.research;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Predicate;

/** Validates every cost before committing; clients supply only a key and expected stage. */
public final class ResearchProgression {
    /** TC6 ResearchManager.noFlags: the next completion sets POPUP but not RESEARCH. */
    public static boolean noFlags;
    /** Called with (completed entry key, addendum owner key) when an addendum becomes readable. */
    public static java.util.function.BiConsumer<String, String> addendumListener = (a, b) -> {};
    public static boolean advance(PlayerResearch state, String key, int expectedStage,
            Predicate<Map<String, Integer>> consumeItems) {
        return advanceInner(state, key, expectedStage, consumeItems);
    }
    /** TC6 progressResearch "popups" branch: flags and addenda PAGE markers on completion. */
    private static void completed(PlayerResearch state, String key) {
        state.setFlag(key, PlayerResearch.FLAG_POPUP);
        if (!noFlags) state.setFlag(key, PlayerResearch.FLAG_RESEARCH); else noFlags = false;
        for (var addenda : ResearchCatalog.addendaResearch().entrySet()) {
            if (!ResearchRules.complete(state, addenda.getKey()) || !addenda.getValue().contains(key)) continue;
            state.setFlag(addenda.getKey(), PlayerResearch.FLAG_PAGE);
            addendumListener.accept(key, addenda.getKey());
        }
    }
    private static boolean advanceInner(PlayerResearch state, String key, int expectedStage,
            Predicate<Map<String, Integer>> consumeItems) {
        var entry = ResearchCatalog.entries().get(key);
        if (entry == null || state.stage(key) != expectedStage || ResearchRules.complete(state, key)
                || !ResearchRules.available(state, entry)) return false;
        int stage = state.stage(key);
        if (stage <= 0) {
            state.unlock(key);
            state.setStage(key, entry.stages().size() == 1 && entry.stages().getFirst().requirements().isEmpty() ? 2 : 1);
            if (ResearchRules.complete(state, key) && !entry.stages().isEmpty()) applyWarp(state, entry.stages().getFirst().warp());
            if (ResearchRules.complete(state, key)) completed(state, key);
            grantSiblings(state, entry);
            return true;
        }
        if (stage > entry.stages().size()) return false;
        var current = entry.stages().get(stage - 1);
        Map<String, Integer> items = new LinkedHashMap<>();
        Map<String, Integer> knowledge = new LinkedHashMap<>();
        for (var requirement : current.requirements().entrySet()) {
            for (String value : requirement.getValue()) {
                switch (requirement.getKey()) {
                    case "required_research": if (!ResearchRules.knows(state, value)) return false; break;
                    case "required_craft": if (!LegacyItemReferences.crafted(state,value)) return false; break;
                    case "required_item": {
                        String[] parts = value.split(";", -1);
                        String id=LegacyItemReferences.requirementKey(value);
                        if (id.isEmpty()) return false;
                        try {
                            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                            if (amount < 1) return false;
                            items.merge(id, amount, Math::addExact);
                        } catch (IllegalArgumentException | ArithmeticException exception) { return false; }
                        break;
                    }
                    case "required_knowledge": {
                        int cost = ResearchRules.knowledgeCost(value);
                        if (cost < 0) return false;
                        String[] parts = value.split(";");
                        try { knowledge.merge(parts[0] + ";" + parts[1], cost, Math::addExact); }
                        catch (ArithmeticException exception) { return false; }
                        break;
                    }
                    default: return false; // Never bypass unported requirement types.
                }
            }
        }
        for (var cost : knowledge.entrySet()) {
            String[] parts = cost.getKey().split(";");
            if (state.knowledge(parts[0], parts[1]) < cost.getValue()) return false;
        }
        // Server inventory adapter must first validate the entire aggregated demand.
        if (!consumeItems.test(Map.copyOf(items))) return false;
        knowledge.forEach((id, cost) -> { String[] parts = id.split(";"); state.addKnowledge(parts[0], parts[1], -cost); });
        int next = stage + 1;
        int warp = current.warp();
        if (next == entry.stages().size() && entry.stages().getLast().requirements().isEmpty()) {
            warp += entry.stages().getLast().warp();
            next++;
        }
        applyWarp(state, warp);
        state.setStage(key, next);
        if (ResearchRules.complete(state, key)) completed(state, key);
        grantSiblings(state, entry);
        return true;
    }
    /** TC6 ResearchManager.progressResearch warp: half (rounded up) permanent, the rest normal, through addWarpToPlayer. */
    private static void applyWarp(PlayerResearch state, int warp) {
        if (warp <= 0) return;
        if (warp > 1) {
            int w2 = warp / 2;
            if (warp - w2 > 0) state.addWarp(PlayerResearch.WARP_PERMANENT, warp - w2);
            if (w2 > 0) state.addWarp(PlayerResearch.WARP_NORMAL, w2);
        } else state.addWarp(PlayerResearch.WARP_PERMANENT, warp);
    }
    private static void grantSiblings(PlayerResearch state, ResearchCatalog.Entry entry) {
        // Authored sibling grants are server effects, not client-selected free research.
        for (String key : entry.siblings()) {
            if (ResearchRules.complete(state, key)) continue;
            var sibling = ResearchCatalog.entries().get(key);
            if (sibling == null) { state.unlock(key); continue; }
            if (!ResearchRules.available(state, sibling)) continue;
            state.unlock(key);
            state.setStage(key, sibling.stages().size() + 1);
            applyWarp(state, sibling.stages().stream().mapToInt(ResearchCatalog.Stage::warp).sum());
            completed(state, key);
            grantSiblings(state, sibling);
        }
    }
    private ResearchProgression() {}
}
