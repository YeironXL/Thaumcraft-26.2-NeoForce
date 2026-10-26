package art.arcane.thaumcraft.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the original research definitions without inventing completion rules. */
public final class ResearchCatalog {
    public static final List<String> CATEGORIES = List.of(
            "BASICS", "AUROMANCY", "ALCHEMY", "ARTIFICE", "INFUSION", "GOLEMANCY", "ELDRITCH");
    /** Research keys referenced by each entry's addenda (ResearchAddendum.getResearch). */
    private static final Map<String, java.util.Set<String>> ADDENDA = new LinkedHashMap<>();
    private static final Map<String, Entry> ENTRIES = load();

    public record Stage(String text, Map<String, List<String>> requirements, List<String> recipes, int warp) {}
    public record Entry(String key, String name, String category, List<String> parents, List<String> meta,
            List<Stage> stages, List<String> siblings) {}

    private static Map<String, Entry> load() {
        Map<String, Entry> entries = new LinkedHashMap<>();
        List<String> files = new ArrayList<>(CATEGORIES);
        files.add("SCANS");
        for (String category : files) {
            String path = "/assets/thaumcraft/research/" + category.toLowerCase(java.util.Locale.ROOT) + ".json";
            try (InputStream stream = ResearchCatalog.class.getResourceAsStream(path)) {
                if (stream == null) throw new IllegalStateException("Missing research resource " + path);
                JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                for (JsonElement element : root.getAsJsonArray("entries")) {
                    JsonObject value = element.getAsJsonObject();
                    List<Stage> stages = new ArrayList<>();
                    if (value.has("stages")) for (JsonElement stageElement : value.getAsJsonArray("stages")) {
                        JsonObject stage = stageElement.getAsJsonObject();
                        Map<String, List<String>> requirements = new LinkedHashMap<>();
                        for (var field : stage.entrySet()) {
                            if (field.getKey().startsWith("required_") && field.getValue().isJsonArray()) {
                                requirements.put(field.getKey(), strings(stage, field.getKey()));
                            }
                        }
                        stages.add(new Stage(stage.has("text") ? stage.get("text").getAsString() : "",
                                Collections.unmodifiableMap(requirements), strings(stage, "recipes"), stage.has("warp") ? stage.get("warp").getAsInt() : 0));
                    }
                    Entry entry = new Entry(value.get("key").getAsString(), value.get("name").getAsString(),
                            value.get("category").getAsString(), strings(value, "parents"), strings(value, "meta"), List.copyOf(stages), strings(value, "siblings"));
                    if (value.has("addenda")) for (JsonElement addendum : value.getAsJsonArray("addenda"))
                        ADDENDA.computeIfAbsent(entry.key(), k -> new java.util.LinkedHashSet<>()).addAll(strings(addendum.getAsJsonObject(), "required_research"));
                    if (entries.putIfAbsent(entry.key(), entry) != null) {
                        throw new IllegalStateException("Duplicate research " + entry.key());
                    }
                }
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("Cannot read " + path, exception);
            }
        }
        return Collections.unmodifiableMap(entries);
    }

    private static List<String> strings(JsonObject object, String key) {
        if (!object.has(key)) return List.of();
        JsonArray array = object.getAsJsonArray(key);
        List<String> result = new ArrayList<>();
        for (JsonElement value : array) result.add(value.getAsString());
        return List.copyOf(result);
    }

    public static Map<String, Entry> entries() { return ENTRIES; }
    public static Map<String, java.util.Set<String>> addendaResearch() { return ADDENDA; }
    private ResearchCatalog() {}
}
