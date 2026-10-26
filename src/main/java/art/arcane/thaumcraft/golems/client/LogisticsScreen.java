package art.arcane.thaumcraft.golems.client;

import art.arcane.thaumcraft.golems.logistics.LogisticsMenu;
import art.arcane.thaumcraft.network.LogisticsPayload;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** TC6 GuiLogistics: pick an item the provider seals offer, choose an amount and request it. */
public final class LogisticsScreen extends AbstractContainerScreen<LogisticsMenu> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_logistics.png");
    private static final Identifier BASE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_base.png");
    private int selectedSlot = -1, stackSize = 1;
    private ItemStack selectedStack = ItemStack.EMPTY;
    private long lu;
    private EditBox search;
    public LogisticsScreen(LogisticsMenu m, Inventory inv, Component t) { super(m, inv, t, 215, 215); }
    private void send(int id) { Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, id); }
    @Override protected void init() {
        super.init();
        search = new EditBox(font, leftPos + 143, topPos + 196, 55, font.lineHeight, Component.empty());
        search.setMaxLength(10); search.setBordered(true); search.setTextColor(0xFFFFFFFF);
        search.setResponder(s -> ClientPacketDistributor.sendToServer(new LogisticsPayload(Optional.empty(), Optional.empty(), ItemStack.EMPTY, 0, s)));
        addRenderableWidget(search);
    }
    private void blit(GuiGraphicsExtractor g, Identifier t, int x, int y, int u, int v, int w, int h) { g.blit(RenderPipelines.GUI_TEXTURED, t, x, y, u, v, w, h, 256, 256); }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractBackground(g, mx, my, pt);
        long ct = System.currentTimeMillis();
        if (ct > lu) { lu = ct + 1000L; send(22); }
        blit(g, TEX, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (selectedSlot >= 0 && !selectedStack.isEmpty()) {
            selectedSlot = -1;
            for (int i = 0; i < 81; i++) if (ItemStack.isSameItemSameComponents(selectedStack, menu.slots.get(i).getItem())) { selectedSlot = i; break; }
        }
        blit(g, BASE, leftPos + 195, topPos + 16, 67, 0, 10, 10);
        blit(g, BASE, leftPos + 195, topPos + 180, 67, 10, 10, 10);
        if (selectedSlot >= 0) {
            blit(g, TEX, leftPos + 17 + selectedSlot % 9 * 19, topPos + 17 + selectedSlot / 9 * 19, 222, 46, 20, 20);
            ItemStack st = menu.slots.get(selectedSlot).getItem();
            stackSize = Math.max(1, Math.min(stackSize, st.getCount()));
            blit(g, BASE, leftPos + 13, topPos + 195, 0, 0, 10, 10);
            blit(g, BASE, leftPos + 57, topPos + 195, 10, 0, 10, 10);
            String s = "" + stackSize;
            g.text(font, Component.literal(s), leftPos + 40 - font.width(s) / 2, topPos + 196, 0xFF333333, false);
            blit(g, BASE, leftPos + 116 - 20, topPos + 200 - 6, 37, 82, 40, 13);
            g.pose().pushMatrix(); g.pose().translate(leftPos + 116, topPos + 200); g.pose().scale(0.5f, 0.5f);
            Component r = Component.translatable("tc.logistics.request");
            g.text(font, r, -font.width(r) / 2, -4, 0xFFFFFFFF, true);
            g.pose().popMatrix();
        }
        if (!search.isFocused() && search.getValue().isEmpty()) g.text(font, Component.translatable("tc.logistics.search"), leftPos + 146, topPos + 197, 0xFF222222, false);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {}
    private boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && my >= y && mx < x + w && my < y + h; }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        double mx = e.x(), my = e.y();
        if (in(mx, my, leftPos + 195, topPos + 16, 10, 10)) { send(1); return true; }
        if (in(mx, my, leftPos + 195, topPos + 180, 10, 10)) { send(0); return true; }
        if (selectedSlot >= 0) {
            ItemStack st = menu.slots.get(selectedSlot).getItem();
            if (in(mx, my, leftPos + 13, topPos + 195, 10, 10)) { stackSize = Math.max(1, stackSize - (e.hasShiftDown() ? 10 : 1)); return true; }
            if (in(mx, my, leftPos + 57, topPos + 195, 10, 10)) { stackSize = Math.min(st.getCount(), stackSize + (e.hasShiftDown() ? 10 : 1)); return true; }
            if (in(mx, my, leftPos + 96, topPos + 194, 40, 13) && !st.isEmpty()) {
                ClientPacketDistributor.sendToServer(new LogisticsPayload(Optional.ofNullable(menu.target), Optional.ofNullable(menu.side), st.copyWithCount(1), stackSize, ""));
                Minecraft.getInstance().player.playSound(art.arcane.thaumcraft.registry.ModSounds.get("clack"), 0.66f, 1.0f);
                return true;
            }
        }
        for (int i = 0; i < 81; i++) {
            var s = menu.slots.get(i);
            if (in(mx, my, leftPos + s.x, topPos + s.y, 16, 16) && s.hasItem()) {
                Minecraft.getInstance().player.playSound(art.arcane.thaumcraft.registry.ModSounds.get("clack"), 0.66f, 1.0f);
                selectedSlot = i; selectedStack = s.getItem().copy(); stackSize = 1;
                return true;
            }
        }
        return super.mouseClicked(e, dbl);
    }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) { send(sy < 0 ? 0 : 1); return true; }
}
