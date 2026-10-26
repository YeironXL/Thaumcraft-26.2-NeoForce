package art.arcane.thaumcraft.item;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import art.arcane.thaumcraft.registry.ModComponents;
public final class CelestialNotesItem extends Item {
    public CelestialNotesItem(Properties properties){super(properties);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,net.minecraft.world.item.component.TooltipDisplay display,java.util.function.Consumer<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,context,display,lines,flag);
        int type=stack.getOrDefault(ModComponents.CELESTIAL_NOTE.get(),0);
        String variant=type==0?"sun":type<5?"stars_"+type:"moon_"+(type-4);
        lines.accept(Component.translatable("item.celestial_notes."+variant+".text").withStyle(net.minecraft.ChatFormatting.AQUA));
    }
}
