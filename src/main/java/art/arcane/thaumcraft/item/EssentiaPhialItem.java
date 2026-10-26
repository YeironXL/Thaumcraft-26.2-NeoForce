package art.arcane.thaumcraft.item;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.registry.ModComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
public final class EssentiaPhialItem extends Item {
    public EssentiaPhialItem(Properties properties) { super(properties.component(ModComponents.ASPECT.get(),"aer")); }
    public static ItemStack filled(String aspect) {
        if (AspectRegistry.get(aspect)==null) throw new IllegalArgumentException("Unknown aspect");
        var stack=new ItemStack(ModItems.PHIAL_FILLED.get()); stack.set(ModComponents.ASPECT.get(),aspect); return stack;
    }
    @Override public Component getName(ItemStack stack) { return Component.translatable("item.thaumcraft.phial_filled.aspect",Component.translatable(stack.getOrDefault(ModComponents.ASPECT.get(),"aer"))); }
}
