package art.arcane.thaumcraft.research;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Serializable-independent research state for one player; integration layer can persist it. */
public final class PlayerResearch {
    public static final MapCodec<PlayerResearch> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("researched", List.of()).forGetter(value -> List.copyOf(value.researched)),
            Codec.STRING.listOf().optionalFieldOf("scanned", List.of()).forGetter(value -> List.copyOf(value.scanned)),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("knowledge", 0).forGetter(PlayerResearch::knowledge),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("permanent_warp", 0).forGetter(PlayerResearch::permanentWarp),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("normal_warp", 0).forGetter(PlayerResearch::normalWarp),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("temporary_warp", 0).forGetter(PlayerResearch::temporaryWarp),
            Codec.unboundedMap(Codec.STRING, Codec.intRange(0, Integer.MAX_VALUE)).optionalFieldOf("stages", Map.of()).forGetter(PlayerResearch::stages),
            Codec.STRING.listOf().optionalFieldOf("crafted", List.of()).forGetter(value -> List.copyOf(value.crafted)),
            Codec.unboundedMap(Codec.STRING, Codec.intRange(0, Integer.MAX_VALUE)).optionalFieldOf("category_knowledge", Map.of()).forGetter(value -> Map.copyOf(value.categoryKnowledge)),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).optionalFieldOf("flags", Map.of()).forGetter(PlayerResearch::flags),
            Codec.INT.optionalFieldOf("warp_counter", 0).forGetter(PlayerResearch::warpCounter)
    ).apply(instance, PlayerResearch::restore));
    private final Set<String> researched = new LinkedHashSet<>();
    private final Set<String> scanned = new LinkedHashSet<>();
    private int knowledge;
    private int permanentWarp;
    private int normalWarp;
    private int temporaryWarp;
    private final Map<String, Integer> stages = new LinkedHashMap<>();
    private final Set<String> crafted = new LinkedHashSet<>();
    private final Map<String, Integer> categoryKnowledge = new LinkedHashMap<>();
    /** TC6 IPlayerKnowledge.EnumResearchFlag per research key. */
    private final Map<String, Set<String>> flags = new LinkedHashMap<>();
    public static final String FLAG_PAGE = "PAGE", FLAG_RESEARCH = "RESEARCH", FLAG_POPUP = "POPUP";
    private static PlayerResearch restore(List<String> researched, List<String> scanned, int knowledge,
            int permanentWarp, int normalWarp, int temporaryWarp, Map<String, Integer> stages,
            List<String> crafted, Map<String, Integer> categoryKnowledge, Map<String, List<String>> flags, int warpCounter) {
        PlayerResearch value = new PlayerResearch();
        value.researched.addAll(researched);
        value.scanned.addAll(scanned);
        value.knowledge = knowledge;
        value.permanentWarp = Math.min(MAX_WARP, permanentWarp);
        value.normalWarp = Math.min(MAX_WARP, normalWarp);
        value.temporaryWarp = Math.min(MAX_WARP, temporaryWarp);
        value.warpCounter = warpCounter;
        stages.forEach((key, stage) -> { if (value.researched.contains(key)) value.stages.put(key, stage); });
        value.crafted.addAll(crafted);
        value.categoryKnowledge.putAll(categoryKnowledge);
        flags.forEach((key, list) -> list.forEach(flag -> value.setFlag(key, flag)));
        return value;
    }
    public boolean unlock(String key) { return researched.add(key); }
    public boolean has(String key) {
        int separator = key.indexOf('@');
        if (separator < 0) return researched.contains(key);
        try { return researched.contains(key.substring(0, separator)) && stage(key.substring(0, separator)) >= Integer.parseInt(key.substring(separator + 1)); }
        catch (NumberFormatException exception) { return false; }
    }
    public int stage(String key) { return researched.contains(key) ? stages.getOrDefault(key, 0) : -1; }
    public void setStage(String key, int value) { if (researched.contains(key) && value > 0) stages.put(key, value); }
    public Map<String, Integer> stages() { return Map.copyOf(stages); }
    public boolean recordCraft(String item) { return crafted.add(item); }
    public boolean crafted(String item) { return crafted.contains(item); }
    public Set<String> craftedItems() { return Set.copyOf(crafted); }
    public Map<String, Integer> categoryKnowledge() { return Map.copyOf(categoryKnowledge); }
    public int knowledge(String type, String category) { return categoryKnowledge.getOrDefault(type + ";" + category, 0); }
    public void addKnowledge(String type, String category, int rawAmount) {
        String key = type + ";" + category;
        categoryKnowledge.put(key, (int)Math.clamp((long)categoryKnowledge.getOrDefault(key, 0) + rawAmount, 0L, Integer.MAX_VALUE));
    }
    public boolean scan(String key) { return scanned.add(key); }
    public boolean scanned(String key) { return scanned.contains(key); }
    public int knowledge() { return knowledge; }
    public void addKnowledge(int amount) { knowledge = (int)Math.clamp((long)knowledge + amount, 0L, Integer.MAX_VALUE); }
    public Set<String> researched() { return Set.copyOf(researched); }
    public void clearOldCelestial(String prefix) { researched.removeIf(key -> key.startsWith("CEL_") && !key.startsWith(prefix)); }
    public Set<String> scannedObjects() { return Set.copyOf(scanned); }
    public int permanentWarp() { return permanentWarp; }
    public int normalWarp() { return normalWarp; }
    public int temporaryWarp() { return temporaryWarp; }
    public void setPermanentWarp(int value) { permanentWarp = Math.clamp(value, 0, MAX_WARP); }
    public void setNormalWarp(int value) { normalWarp = Math.clamp(value, 0, MAX_WARP); }
    public void setTemporaryWarp(int value) { temporaryWarp = Math.clamp(value, 0, MAX_WARP); }
    /** TC6 PlayerWarp: each warp pool is clamped to 0..500. */
    public static final int MAX_WARP = 500;
    /** IPlayerWarp.EnumWarpType ordinals (also the PacketWarpMessage type byte). */
    public static final int WARP_PERMANENT = 0, WARP_NORMAL = 1, WARP_TEMPORARY = 2;
    /** TC6 IPlayerWarp counter: reset to the total warp whenever warp is gained, drained by warp events. */
    private int warpCounter;
    public int warpCounter() { return warpCounter; }
    public void setWarpCounter(int value) { warpCounter = value; }
    public int warp(int type) { return switch (type) { case WARP_PERMANENT -> permanentWarp; case WARP_NORMAL -> normalWarp; default -> temporaryWarp; }; }
    /** Server side effects of a warp change (messages, WARP research, client sync); installed per player. */
    public interface WarpListener { void changed(int type, int amount); }
    private transient WarpListener warpListener;
    public WarpListener warpListener() { return warpListener; }
    public void setWarpListener(WarpListener listener) { warpListener = listener; }
    /** TC6 InternalMethodHandler.addWarpToPlayer state part: clamped to 0..500, a gain resets the counter to the total. */
    public int addWarp(int type, int amount) {
        if (amount == 0) return 0;
        int cur = warp(type);
        // TC6 1:1 quirk: an over-large reduction becomes "+cur" (InternalMethodHandler: amount = cur), doubling that pool.
        if (amount < 0 && cur + amount < 0) amount = cur;
        int next = (int) Math.clamp((long) cur + amount, 0L, MAX_WARP);
        switch (type) { case WARP_PERMANENT -> permanentWarp = next; case WARP_NORMAL -> normalWarp = next; default -> temporaryWarp = next; }
        if (amount > 0) warpCounter = temporaryWarp + permanentWarp + normalWarp;
        if (warpListener != null) warpListener.changed(type, amount);
        return amount;
    }
    public boolean setFlag(String key, String flag) { return flags.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(flag); }
    public boolean clearFlag(String key, String flag) {
        var set = flags.get(key); if (set == null) return false;
        boolean removed = set.remove(flag); if (set.isEmpty()) flags.remove(key); return removed;
    }
    public boolean hasFlag(String key, String flag) { var set = flags.get(key); return set != null && set.contains(flag); }
    public Map<String, List<String>> flags() {
        Map<String, List<String>> out = new LinkedHashMap<>(); flags.forEach((k, v) -> out.put(k, List.copyOf(v))); return out;
    }
}
