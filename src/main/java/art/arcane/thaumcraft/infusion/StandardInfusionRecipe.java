package art.arcane.thaumcraft.infusion;

import art.arcane.thaumcraft.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;

/**
 * TC6 InfusionRecipe as registered by ConfigRecipes.initializeInfusionRecipes. Either produces {@code result}, or (TC6
 * {@code new Object[]{"label", NBTBase}} outputs such as the fortress masks) sets {@code result_tag} on the central item.
 */
public record StandardInfusionRecipe(String research, Ingredient input, List<Ingredient> components, Map<String, Integer> aspectCost,
                                     int instability, Optional<ItemStackTemplate> result, Optional<TagOutput> resultTag) implements InfusionRecipe {
    /** NBT label output: an int (or byte) under {@code key} in the item's custom data. */
    public record TagOutput(String key, int value, boolean asByte) {
        public static final Codec<TagOutput> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("key").forGetter(TagOutput::key),
                Codec.INT.fieldOf("value").forGetter(TagOutput::value),
                Codec.BOOL.optionalFieldOf("byte", false).forGetter(TagOutput::asByte)).apply(i, TagOutput::new));
    }

    /** Decoded into an ImmutableMap, which keeps the JSON order (TC6 AspectList insertion order decides the drain order). */
    static final Codec<Map<String, Integer>> ORDERED_ASPECTS = Codec.unboundedMap(Codec.STRING, Codec.intRange(1, 100000));

    public static final MapCodec<StandardInfusionRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("research").forGetter(StandardInfusionRecipe::research),
            Ingredient.CODEC.fieldOf("input").forGetter(StandardInfusionRecipe::input),
            Ingredient.CODEC.listOf().fieldOf("components").forGetter(StandardInfusionRecipe::components),
            ORDERED_ASPECTS.optionalFieldOf("aspects", Map.of()).forGetter(StandardInfusionRecipe::aspectCost),
            Codec.intRange(0, 100).optionalFieldOf("instability", 0).forGetter(StandardInfusionRecipe::instability),
            ItemStackTemplate.CODEC.optionalFieldOf("result").forGetter(StandardInfusionRecipe::result),
            TagOutput.CODEC.optionalFieldOf("result_tag").forGetter(StandardInfusionRecipe::resultTag)
    ).apply(i, StandardInfusionRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, StandardInfusionRecipe> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    public StandardInfusionRecipe {
        components = List.copyOf(components);
        aspectCost = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(aspectCost));
        if (result.isEmpty() && resultTag.isEmpty()) throw new IllegalArgumentException("Infusion recipe needs a result or result_tag");
    }

    @Override
    public boolean matches(List<ItemStack> comps, ItemStack central, Level level, Player player) {
        if (!InfusionRecipe.isResearchKnown(player, research)) return false;
        return input.test(central) && RecipeMatcher.findMatches(comps, components) != null;
    }

    @Override
    public Output output(Player player, ItemStack central, List<ItemStack> comps) {
        if (resultTag.isPresent()) {
            ItemStack out = central.copy();
            var t = resultTag.get();
            // The verdant charm "type" byte lives in the thaumcraft:variant component in this port (item models select on it).
            if (t.key().equals("thaumcraft:variant")) art.arcane.thaumcraft.item.BaubleItem.withType(out, t.value());
            else CustomData.update(DataComponents.CUSTOM_DATA, out, tag -> { if (t.asByte()) tag.putByte(t.key(), (byte) t.value()); else tag.putInt(t.key(), t.value()); });
            return new Output(out, true);
        }
        return new Output(result.get().create(), false);
    }

    @Override
    public LinkedHashMap<String, Integer> aspects(Player player, ItemStack central, List<ItemStack> comps) { return new LinkedHashMap<>(aspectCost); }
    @Override public int instability(Player player, ItemStack central, List<ItemStack> comps) { return instability; }
    @Override public RecipeSerializer<StandardInfusionRecipe> getSerializer() { return ModRecipes.INFUSION.get(); }
}
