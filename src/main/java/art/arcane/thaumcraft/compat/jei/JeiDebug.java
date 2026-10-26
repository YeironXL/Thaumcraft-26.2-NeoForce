package art.arcane.thaumcraft.compat.jei;

import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Development aid (-Dthaumcraft.debugJei=category[@item]): once in game, opens JEI's recipe view on a Thaumcraft
 * category ("arcane", "crucible", "infusion", "centrifuge"), optionally focused on the recipes producing an item.
 */
final class JeiDebug {
    private static IJeiRuntime runtime;
    private static boolean listening;
    private static int ticks;

    static void runtime(IJeiRuntime rt) {
        runtime = rt;
        String spec = System.getProperty("thaumcraft.debugJei");
        if (spec == null || listening) return;
        listening = true;
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            var mc = Minecraft.getInstance();
            if (runtime == null || mc.player == null) return;
            int t = ++ticks - Integer.getInteger("thaumcraft.debugJeiDelay", 120);
            if (spec.startsWith("list")) { list(spec, t); return; }
            String[] specs = spec.split(",");
            if (t < 0 || t % 100 != 0) return;
            show(specs[(t / 100) % specs.length]);
        });
    }

    /** Several specs separated by commas are shown in turn, 100 ticks apart, round and round. */
    private static void show(String spec) {
        {
            String cat = spec.contains("@") ? spec.substring(0, spec.indexOf('@')) : spec;
            String item = spec.contains("@") ? spec.substring(spec.indexOf('@') + 1) : null;
            var type = switch (cat) {
                case "arcane" -> ThaumcraftJeiPlugin.ARCANE;
                case "crucible" -> ThaumcraftJeiPlugin.CRUCIBLE;
                case "centrifuge" -> ThaumcraftJeiPlugin.CENTRIFUGE;
                case "bonus" -> ThaumcraftJeiPlugin.SMELTING_BONUS;
                default -> ThaumcraftJeiPlugin.INFUSION;
            };
            if ("infusion".equals(cat) && "special".equals(item)) { runtime.getRecipesGui().showRecipes(ThaumcraftJeiPlugin.infusionCategory, ThaumcraftJeiPlugin.specialInfusions, List.of()); return; }
            if (item == null) { runtime.getRecipesGui().showTypes(List.of(type)); return; }
            var stack = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(item)));
            var focus = runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack);
            runtime.getRecipesGui().show(focus);
        }
    }

    /** "list@x:y[@filter]": inventory open, JEI's item list filtered, cursor held at window pixel x,y (aspect tooltip check). */
    private static void list(String spec, int t) {
        var mc = Minecraft.getInstance();
        String[] parts = spec.split("@");
        if (t == 0) {
            mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            runtime.getIngredientFilter().setFilterText(parts.length > 2 ? parts[2] : "@thaumcraft");
        }
        if (t < 0 || parts.length < 2) return;
        String[] xy = parts[1].split(":");
        try {
            var fx = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos"); var fy = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos");
            fx.setAccessible(true); fy.setAccessible(true);
            fx.setDouble(mc.mouseHandler, Double.parseDouble(xy[0])); fy.setDouble(mc.mouseHandler, Double.parseDouble(xy[1]));
        } catch (ReflectiveOperationException ignored) {}
    }

    private JeiDebug() {}
}
