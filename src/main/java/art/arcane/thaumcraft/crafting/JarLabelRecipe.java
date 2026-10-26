package art.arcane.thaumcraft.crafting;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** TC6 ConfigRecipes "label_<aspect>": blank jar label + phial of that essentia -> label marked with the aspect. */
public final class JarLabelRecipe extends CustomRecipe {
    public static final MapCodec<JarLabelRecipe> CODEC = Codec.STRING.validate(a -> AspectRegistry.get(a) == null ? DataResult.error(() -> "Unknown aspect " + a) : DataResult.success(a))
            .fieldOf("aspect").xmap(JarLabelRecipe::new, JarLabelRecipe::aspect);
    public static final StreamCodec<RegistryFriendlyByteBuf, JarLabelRecipe> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(JarLabelRecipe::new, JarLabelRecipe::aspect).cast();
    private final String aspect;
    public JarLabelRecipe(String aspect) { this.aspect = aspect; }
    public String aspect() { return aspect; }
    @Override public boolean matches(CraftingInput input, Level level) {
        boolean label = false, phial = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(ModItems.LABEL_BLANK.get()) && !label) label = true;
            else if (stack.is(ModItems.PHIAL_FILLED.get()) && !phial && aspect.equals(stack.get(ModComponents.ASPECT.get()))) phial = true;
            else return false;
        }
        return label && phial;
    }
    @Override public ItemStack assemble(CraftingInput input) {
        ItemStack out = new ItemStack(ModItems.LABEL_FILLED.get());
        out.set(ModComponents.ASPECT.get(), aspect);
        return out;
    }
    @Override public RecipeSerializer<JarLabelRecipe> getSerializer() { return ModRecipes.JAR_LABEL.get(); }
}
