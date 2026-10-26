package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.item.gear.HoverHarnessItem;
import art.arcane.thaumcraft.network.HarnessTogglePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** Thaumostatic Harness toggle key (H, as TC4) and Potentia gauge, drawn with the caster HUD's gauge sprites. */
public final class HarnessClient {
    public static final KeyMapping KEY_H = new KeyMapping("key.thaumcraft.harness", GLFW.GLFW_KEY_H, CasterClient.CATEGORY);
    private static final Identifier HUD = Identifier.parse("thaumcraft:textures/gui/hud.png");

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(KEY_H));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            var mc = Minecraft.getInstance();
            while (KEY_H.consumeClick())
                if (mc.player != null && !HoverHarnessItem.worn(mc.player).isEmpty()) ClientPacketDistributor.sendToServer(HarnessTogglePayload.INSTANCE);
        });
        NeoForge.EVENT_BUS.addListener(HarnessClient::renderHud);
    }

    private static void renderHud(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden() || mc.gui.screen() != null) return;
        ItemStack s = HoverHarnessItem.worn(mc.player);
        if (s.isEmpty()) return;
        var g = e.getGuiGraphics();
        int fuel = HoverHarnessItem.getFuel(s);
        int x = g.guiWidth() - 14, y = g.guiHeight() - 60;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.5f, 0.5f);
        int loc = (int) (30.0f * fuel / HoverHarnessItem.MAX_FUEL);
        int ac = HoverHarnessItem.isActive(s) ? AspectRegistry.color("potentia") : 0x606060;
        if (loc > 0) g.blit(RenderPipelines.GUI_TEXTURED, HUD, -4, 35 - loc, 104, 0, 8, loc, 256, 256, 0xCC000000 | ac);
        g.blit(RenderPipelines.GUI_TEXTURED, HUD, -8, -3, 72, 0, 16, 42, 256, 256);
        g.pose().popMatrix();
        if (mc.player.isShiftKeyDown()) { String t = String.valueOf(fuel); g.text(mc.font, t, x - 6 - mc.font.width(t), y + 6, 0xFFFFFFFF, true); }
    }
}
