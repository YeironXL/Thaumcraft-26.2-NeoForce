package art.arcane.thaumcraft.scan;

import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EntityScanRegistry {
    private static final Map<String,Map<Aspect,Integer>> ENTRIES=new LinkedHashMap<>();
    static {
        try (var reader=new InputStreamReader(EntityScanRegistry.class.getResourceAsStream("/assets/thaumcraft/migration/entity_aspects.json"),StandardCharsets.UTF_8)) {
            JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entity -> {
                var aspects=new LinkedHashMap<Aspect,Integer>();
                entity.getValue().getAsJsonObject().entrySet().forEach(tag -> aspects.put(java.util.Objects.requireNonNull(AspectRegistry.get(tag.getKey())),tag.getValue().getAsInt()));
                ENTRIES.put(entity.getKey(),java.util.Collections.unmodifiableMap(aspects));
            });
        } catch (java.io.IOException exception) { throw new ExceptionInInitializerError(exception); }
    }
    public static Map<Aspect,Integer> get(String id) { return ENTRIES.getOrDefault(id,Map.of()); }
    /**
     * AspectHelper.getEntityAspects: players get humanus 4 plus three name-seeded aspects at 15; every other entity
     * uses the ConfigAspects table (entity_aspects.json), with the EntityTagsNBT conditions mapped to variant keys
     * (creeper "powered" -> /charged, pech "PechType" -> /1 /2, wisp "Type" -> its aspect). No fallback for unlisted entities.
     */
    public static Map<Aspect,Integer> aspects(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof net.minecraft.world.entity.player.Player player) {
            var tags = new LinkedHashMap<Aspect, Integer>();
            tags.merge(AspectRegistry.HUMANUS, 4, Integer::sum);
            var rand = new java.util.Random(player.getName().getString().hashCode());
            var all = AspectRegistry.all().values().toArray(new Aspect[0]);
            for (int i = 0; i < 3; i++) tags.merge(all[rand.nextInt(all.length)], 15, Integer::sum);
            return tags;
        }
        if (entity instanceof art.arcane.thaumcraft.entity.WispEntity wisp) {
            if (wisp.getAspect() == null) return Map.of();
            var tags = new LinkedHashMap<Aspect, Integer>();
            tags.merge(wisp.getAspect(), 5, Integer::sum);
            tags.merge(AspectRegistry.AURAM, 5, Integer::sum);
            tags.merge(AspectRegistry.VOLATUS, 5, Integer::sum);
            return tags;
        }
        String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        String variant = entity instanceof net.minecraft.world.entity.monster.Creeper creeper && creeper.isPowered() ? "/charged"
                : entity instanceof art.arcane.thaumcraft.entity.monster.PechEntity pech && pech.getPechType() > 0 ? "/" + pech.getPechType() : "";
        var tags = get(id + variant);
        return tags.isEmpty() ? get(id) : tags;
    }
    private EntityScanRegistry() {}
}
