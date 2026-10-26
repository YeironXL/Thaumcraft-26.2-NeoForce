package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.aspect.ObjectAspects;
import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * TC6 RenderEventHandler.tooltipEvent + HudHandler.renderAspectsInGui: in a container screen, holding shift (inverted by
 * the showTags option, here -Dthaumcraft.showTags) shows a row of the item's object aspects, sorted by amount, at the
 * bottom of its tooltip.
 */
public final class AspectTooltipClient {
    record Tags(List<Map.Entry<Aspect, Integer>> tags) implements TooltipComponent {}

    record Row(List<Map.Entry<Aspect, Integer>> tags) implements ClientTooltipComponent {
        @Override public int getHeight(Font font) { return 18; }
        @Override public int getWidth(Font font) { return tags.size() * 18; }
        @Override public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor g) {
            for (int i = 0; i < tags.size(); i++) ThaumcraftTheme.drawTag(g, font, tags.get(i).getKey().id(), x + i * 18, y + 1, tags.get(i).getValue());
        }
    }

    public static void register(IEventBus bus) {
        bus.addListener((RegisterClientTooltipComponentFactoriesEvent e) -> e.register(Tags.class, t -> new Row(t.tags())));
        NeoForge.EVENT_BUS.addListener(AspectTooltipClient::gather);
    }

    private static void gather(RenderTooltipEvent.GatherComponents e) {
        var mc = Minecraft.getInstance();
        if (!showsItemTooltips(mc.gui.screen()) || mc.hasShiftDown() == Boolean.getBoolean("thaumcraft.showTags")) return;
        if (e.getItemStack().isEmpty()) return;
        var tags = new ArrayList<>(ObjectAspects.get(e.getItemStack()).entrySet());
        if (tags.isEmpty()) return;
        tags.sort((a, b) -> Integer.compare(b.getValue(), a.getValue())); // AspectList.getAspectsSortedByAmount
        e.getTooltipElements().add(Either.right(new Tags(List.copyOf(tags))));
    }

    /** Container screens, plus recipe viewer screens (JEI's recipe view is not a container screen). */
    private static boolean showsItemTooltips(net.minecraft.client.gui.screens.Screen screen) {
        return screen instanceof AbstractContainerScreen<?> || screen != null && screen.getClass().getName().startsWith("mezz.jei.");
    }

    private AspectTooltipClient() {}
}
