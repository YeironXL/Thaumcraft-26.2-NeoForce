package art.arcane.thaumcraft.item;

import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.registry.ModComponents;
import java.text.DecimalFormat;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** TC6 ItemFocus: holds a FocusPackage; vis cost = complexity / 5, cooldown from complexity. */
public final class FocusItem extends Item {
    private final int maxComplexity;
    private static final DecimalFormat FMT = new DecimalFormat("#######.##");

    public FocusItem(int maxComplexity, Properties p) { super(p.stacksTo(1).rarity(Rarity.RARE)); this.maxComplexity = maxComplexity; }

    public int getMaxComplexity() { return maxComplexity; }

    public static void setPackage(ItemStack focusstack, FocusPackage core) { focusstack.set(ModComponents.FOCUS_PACKAGE.get(), core.serialize()); }

    public static FocusPackage getPackage(ItemStack focusstack) {
        if (focusstack == null || focusstack.isEmpty()) return null;
        var tag = focusstack.get(ModComponents.FOCUS_PACKAGE.get());
        if (tag == null) return null;
        FocusPackage p = new FocusPackage();
        p.deserialize(tag);
        return p;
    }

    /** Average colour of the package's effects (TC6 caches it as "color"). */
    public int getFocusColor(ItemStack focusstack) {
        FocusPackage core = getPackage(focusstack);
        if (core == null) return 0xFFFFFF;
        FocusEffect[] fe = core.getFocusEffects();
        int r = 0, g = 0, b = 0;
        for (FocusEffect ef : fe) {
            int c = FocusEngine.getElementColor(ef.getKey());
            r += (c >> 16) & 255; g += (c >> 8) & 255; b += c & 255;
        }
        if (fe.length > 0) { r /= fe.length; g /= fe.length; b /= fe.length; }
        return (r << 16) | (g << 8) | b;
    }

    public String getSortingHelper(ItemStack focusstack) {
        FocusPackage p = getPackage(focusstack);
        if (p == null) return null;
        return focusstack.getHoverName().getString() + p.getSortingHelper();
    }

    public float getVisCost(ItemStack focusstack) {
        FocusPackage p = getPackage(focusstack);
        return p == null ? 0.0f : p.getComplexity() / 5.0f;
    }

    public int getActivationTime(ItemStack focusstack) {
        FocusPackage p = getPackage(focusstack);
        return p == null ? 0 : Math.max(5, p.getComplexity() / 5 * (p.getComplexity() / 4));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        addFocusInformation(stack, lines);
    }

    public void addFocusInformation(ItemStack stack, Consumer<Component> lines) {
        FocusPackage p = getPackage(stack);
        if (p == null) return;
        lines.accept(Component.literal(FMT.format(getVisCost(stack)) + " ").append(Component.translatable("item.Focus.cost1")));
        for (IFocusElement fe : p.nodes) if (fe instanceof FocusNode n && !(fe instanceof FocusMediumRoot)) buildInfo(lines, n, 0);
    }

    private static void buildInfo(Consumer<Component> lines, FocusNode node, int depth) {
        if (node instanceof FocusMediumRoot) return;
        var t = Component.literal("  ".repeat(depth)).append(Component.translatable(node.getUnlocalizedName()).withStyle(ChatFormatting.DARK_PURPLE));
        if (!node.getSettingList().isEmpty()) {
            var s = Component.literal(" [").withStyle(ChatFormatting.DARK_AQUA);
            boolean q = false;
            for (String st : node.getSettingList()) {
                NodeSetting ns = node.getSetting(st);
                if (q) s.append(", ");
                s.append(ns.getLocalizedName()).append(" ").append(ns.getValueText());
                q = true;
            }
            t.append(s.append("]"));
        }
        lines.accept(t);
        if (node instanceof FocusModSplit split)
            for (FocusPackage sp : split.getSplitPackages())
                for (IFocusElement fe : sp.nodes) if (fe instanceof FocusNode n) buildInfo(lines, n, depth + 1);
    }
}
