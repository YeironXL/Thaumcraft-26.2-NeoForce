package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CrystalEssenceItem extends Item {
    public CrystalEssenceItem(Properties properties) { super(properties.component(ModComponents.ASPECT.get(), "aer")); }
    public static String aspect(ItemStack stack) {
        if (!stack.is(ModItems.CRYSTAL_ESSENCE.get())) return "";
        String value = stack.get(ModComponents.ASPECT.get());
        return value != null && AspectRegistry.get(value) != null ? value : "";
    }
    public static ItemStack of(String aspect, int count) {
        if (AspectRegistry.get(aspect) == null) throw new IllegalArgumentException("Unknown aspect " + aspect);
        ItemStack stack = new ItemStack(ModItems.CRYSTAL_ESSENCE.get(), count);
        stack.set(ModComponents.ASPECT.get(), aspect);
        return stack;
    }
    @Override public Component getName(ItemStack stack) {
        return Component.translatable("item.thaumcraft.crystal_essence.aspect", Component.translatable(aspect(stack)));
    }
}
