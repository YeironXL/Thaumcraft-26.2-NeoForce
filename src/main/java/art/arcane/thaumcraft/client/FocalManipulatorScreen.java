package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.blockentity.FocalManipulatorBlockEntity;
import art.arcane.thaumcraft.casters.*;
import art.arcane.thaumcraft.client.gui.TcWidget;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.item.FocusItem;
import art.arcane.thaumcraft.menu.FocalManipulatorMenu;
import art.arcane.thaumcraft.network.FocusNodesPayload;
import art.arcane.thaumcraft.registry.ModSounds;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Port of TC6 GuiFocalManipulator: node-tree focus designer. */
public final class FocalManipulatorScreen extends AbstractContainerScreen<FocalManipulatorMenu> {
    private static final Identifier TEX = Identifier.parse("thaumcraft:textures/gui/gui_wandtable.png");
    private static final Identifier TEX2 = Identifier.parse("thaumcraft:textures/gui/gui_wandtable2.png");
    private static final Identifier TEX3 = Identifier.parse("thaumcraft:textures/gui/gui_wandtable3.png");
    private static final Identifier I_MEDIUM = Identifier.parse("thaumcraft:textures/foci/_medium.png");
    private static final Identifier I_EFFECT = Identifier.parse("thaumcraft:textures/foci/_effect.png");
    private static final DecimalFormat FMT = new DecimalFormat("#######.##");

    private final FocalManipulatorBlockEntity table;
    private final List<TcWidget> buttons = new ArrayList<>();
    private TcWidget.Image buttonConfirm;
    private TcWidget.Slider scrollbarParts, scrollbarMainSide, scrollbarMainBottom;
    private EditBox nameField;
    private int isMouseButtonDown = 0, lastMouseX, lastMouseY;
    private int totalComplexity, maxComplexity, lastNodeHover = -1;
    private final List<String> shownParts = new ArrayList<>();
    private int partsStart = 0;
    private ItemStack[] components;
    private boolean valid;
    private int nodeID = 0, sMinX, sMinY, sMaxX, sMaxY, selectedNode = -1;
    private float costCast;
    private int costXp, costVis, scrollX, scrollY;

    public FocalManipulatorScreen(FocalManipulatorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, 231, 231);
        this.table = menu.table;
    }

    private Map<Integer, FocusElementNode> data() { return table.data; }
    private ItemStack focusStack() { return table.getItem(0); }

    /** GuiContainer.isPointInRegion. */
    private boolean inRegion(int rx, int ry, int w, int h, int px, int py) {
        px -= leftPos; py -= topPos;
        return px >= rx - 1 && px < rx + w + 1 && py >= ry - 1 && py < ry + h + 1;
    }

    @Override
    protected void init() {
        super.init();
        nameField = new EditBox(font, leftPos + 30, topPos + 11, 170, 12, Component.empty());
        nameField.setTextColor(-1);
        nameField.setTextColorUneditable(-1);
        nameField.setBordered(false);
        nameField.setMaxLength(50);
        nameField.setResponder(s -> {
            if (!s.equals(table.focusName)) {
                table.focusName = s;
                ClientPacketDistributor.sendToServer(new FocusNodesPayload(table.getBlockPos(), true, List.of(), table.focusName));
            }
        });
        addRenderableWidget(nameField);
        buttonConfirm = new TcWidget.Image(0, leftPos + 232, topPos + 7, 24, 16, null, Component.translatable("wandtable.text3"), TcWidget.BASE, 232, 240, 24, 16);
        gatherInfo(false);
        if (table.focusName.isEmpty() && !focusStack().isEmpty()) table.focusName = focusStack().getHoverName().getString();
        nameField.setValue(table.focusName);
    }

    // ---------------- rendering ----------------
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {
        super.extractBackground(g, mx, my, pt);
        int k = leftPos, l = topPos;
        g.blit(RenderPipelines.GUI_TEXTURED, TEX2, k, l, 0, 0, imageWidth, imageHeight, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, TEX3, k - 71, l - 3, 0, 0, 71, 239, 256, 256);
        if (focusStack().isEmpty() || table.doGuiReset) {
            if (table.doGuiReset) resetNodes();
            else { data().clear(); gatherInfo(false); }
            table.focusName = "";
            if (!focusStack().isEmpty()) { table.focusName = focusStack().getHoverName().getString(); nameField.setValue(table.focusName); }
            table.doGuiReset = false;
        }
        if (table.doGather) { gatherInfo(false); table.doGather = false; }
        drawNodes(g, leftPos + 132 - scrollX, topPos + 48 - scrollY, mx, my);
        g.blit(RenderPipelines.GUI_TEXTURED, TEX, k, l, 0, 0, imageWidth, imageHeight, 256, 256);
        if (maxComplexity > 0) g.text(font, totalComplexity + "/" + maxComplexity, k + 242, l + 36, totalComplexity > maxComplexity ? 0xFFF67178 : 0xFFFFC23F, true);
        g.text(font, "" + costXp, k + 242, l + 50, costXp > minecraft.player.experienceLevel ? 0xFFF67178 : 0xFF9BFF8D, true);
        int v = table.vis > 0.0f ? (int) table.vis : costVis;
        g.text(font, Component.literal("" + v).withStyle(ChatFormatting.AQUA), k + 242, l + 64, 0xFF9BFF8D, true);
        if (costCast > 0.0f)
            g.text(font, Component.translatable("item.Focus.cost1").append(": " + FMT.format(costCast)).withStyle(ChatFormatting.AQUA), k + 230, l + 80, 0xFF9BFF8D, true);
        if (components != null && components.length > 0)
            g.text(font, Component.translatable("wandtable.text4").withStyle(ChatFormatting.GOLD), k + 230, l + 92, 0xFF9BFF8D, true);
        if (!data().isEmpty()) {
            g.blit(RenderPipelines.GUI_TEXTURED, TcWidget.BASE, leftPos + 24, topPos + 8, 192, 224, 8, 14, 256, 256);
            int a;
            for (a = 1; a < 22; ++a) g.blit(RenderPipelines.GUI_TEXTURED, TcWidget.BASE, leftPos + 24 + a * 8, topPos + 8, 200, 224, 8, 14, 256, 256);
            g.blit(RenderPipelines.GUI_TEXTURED, TcWidget.BASE, leftPos + 24 + a * 8, topPos + 8, 208, 224, 8, 14, 256, 256);
        }
        nameField.visible = !data().isEmpty();
        for (TcWidget w : buttons) w.render(g, mx, my);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        syncScrollbars();
        if (isMouseButtonDown != 0 || minecraft.mouseHandler.isLeftPressed()) handleDragScroll(mx, my);
        super.extractRenderState(g, mx, my, pt);
        int gx = leftPos, gy = topPos;
        int count = 0, index = 0;
        for (String sk : shownParts) {
            if (++count - 1 < partsStart) continue;
            FocusNode node = (FocusNode) FocusEngine.getElement(sk);
            drawPart(g, node, gx + 38, 43 + gy + 25 * index, node.getType() == IFocusElement.EnumUnitType.MOD ? 24.0f : 32.0f, inRegion(gx + 38 - 10, 32 + gy + 24 * index, 20, 20, mx + leftPos, my + topPos));
            if (++index > 5) break;
        }
        count = 0; index = 0;
        for (String sk : shownParts) {
            if (++count - 1 < partsStart) continue;
            FocusNode node = (FocusNode) FocusEngine.getElement(sk);
            if (inRegion(gx + 38 - 10, 33 + gy + 25 * index, 20, 20, mx + leftPos, my + topPos))
                g.setComponentTooltipForNextFrame(font, genPartText(node, -1), gx + 44, 46 + gy + 24 * index);
            if (++index > 5) break;
        }
        if (lastNodeHover >= 0) {
            FocusElementNode fn = data().get(lastNodeHover);
            if (fn != null && fn.node != null)
                g.setComponentTooltipForNextFrame(font, genPartText(fn.node, lastNodeHover), leftPos + 140 - scrollX + fn.x * 24, topPos + 50 - scrollY + fn.y * 32);
        }
        buttonConfirm.active = table.vis <= 0.0f && valid;
        for (TcWidget w : buttons) if (w.isMouseOver() && !w.tooltip().isEmpty()) { g.setComponentTooltipForNextFrame(font, w.tooltip(), mx, my); break; }
        drawNodeSettings(g);
        extractTooltip(g, mx, my);
    }

    private void handleDragScroll(int mx, int my) {
        if (minecraft.mouseHandler.isLeftPressed()) {
            if ((isMouseButtonDown == 0 || isMouseButtonDown == 1) && inRegion(leftPos + 63, topPos + 31, 136, 160, mx + leftPos, my + topPos)) {
                if (isMouseButtonDown == 0) isMouseButtonDown = 1;
                else {
                    boolean b = scrollbarParts != null && scrollbarParts.isMouseOver();
                    if (scrollbarMainBottom != null && !scrollbarMainBottom.isMouseOver() && !b) {
                        scrollX -= mx - lastMouseX;
                        scrollX = (int) Math.clamp(scrollX, scrollbarMainBottom.getMin(), scrollbarMainBottom.getMax());
                        scrollbarMainBottom.setSliderValue(scrollX);
                    }
                    if (scrollbarMainSide != null && !scrollbarMainSide.isMouseOver() && !b) {
                        scrollY -= my - lastMouseY;
                        scrollY = (int) Math.clamp(scrollY, scrollbarMainSide.getMin(), scrollbarMainSide.getMax());
                        scrollbarMainSide.setSliderValue(scrollY);
                    }
                }
            }
            lastMouseX = mx; lastMouseY = my;
        } else isMouseButtonDown = 0;
    }

    private void syncScrollbars() {
        if (scrollbarMainSide != null) scrollY = Math.round(scrollbarMainSide.getSliderValue());
        if (scrollbarMainBottom != null) scrollX = Math.round(scrollbarMainBottom.getSliderValue());
        if (scrollbarParts != null) partsStart = Math.round(scrollbarParts.getSliderValue());
    }

    private List<Component> genPartText(FocusNode node, int idx) {
        List<Component> list = new ArrayList<>();
        if (node == null) return list;
        FocusElementNode placed = idx >= 0 ? data().get(idx) : null;
        list.add(Component.translatable(node.getUnlocalizedName()));
        list.add(Component.translatable(node.getUnlocalizedText()).withStyle(ChatFormatting.DARK_PURPLE));
        int c = node.getComplexity();
        if (placed != null) c = (int) (node.getComplexity() * placed.complexityMultiplier);
        list.add(Component.translatable("focuspart.com").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" " + c).withStyle(placed != null && placed.complexityMultiplier > 1.0f ? ChatFormatting.RED : ChatFormatting.GOLD)));
        float p = placed != null ? placed.getPower(data()) : node.getPowerMultiplier();
        if (p != 1.0f) list.add(Component.translatable("focuspart.eff").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" x" + FMT.format(p)).withStyle(p < 1.0f ? ChatFormatting.RED : ChatFormatting.GREEN)));
        if (node instanceof FocusEffect fe) {
            float d = fe.getDamageForDisplay(placed == null ? 1.0f : placed.getPower(data()));
            if (d > 0.0f) list.add(Component.translatable("attribute.modifier.equals.0", FMT.format(d), Component.translatable("attribute.name.attack_damage")).withStyle(ChatFormatting.DARK_RED));
            else if (d < 0.0f) list.add(Component.translatable("attribute.modifier.equals.0", FMT.format(-d), Component.translatable("focus.heal.power")).withStyle(ChatFormatting.DARK_GREEN));
        }
        return list;
    }

    private void clipped(GuiGraphicsExtractor g, int x, int y, int sx, int sy, int w, int h) {
        if (inRegion(48, 26, 166, 174, x + w / 2, y + h / 2)) g.blit(RenderPipelines.GUI_TEXTURED, TEX, x, y, sx, sy, w, h, 256, 256);
    }

    private void drawNodes(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        if (data().isEmpty()) return;
        int hover = -1;
        for (FocusElementNode fn : data().values()) {
            int xx = x + fn.x * 24, yy = y + fn.y * 32;
            boolean mouseover = inRegion(leftPos + 63, topPos + 31, 136, 160, mx + leftPos, my + topPos) && inRegion(xx - 10, yy - 10, 20, 20, mx + leftPos, my + topPos);
            if (mouseover && fn.parent >= 0) hover = fn.id;
            if (fn.node != null) { if (inRegion(48, 16, 154, 192, xx - 8, yy - 8)) drawPart(g, fn.node, xx, yy, 32.0f, mouseover); }
            else clipped(g, xx - 12, yy - 12, 120, 232, 24, 24);
            if (selectedNode == fn.id || (mouseover && fn.parent >= 0)) clipped(g, xx - 12, yy - 12, 96, 232, 24, 24);
            FocusElementNode parent = data().get(fn.parent);
            if (parent == null) continue;
            clipped(g, xx - 6, yy - 22, 54, 232, 12, 12);
            if (parent.node instanceof FocusModSplit) {
                int q = Math.abs(fn.x - parent.x);
                for (int a = 0; a < q; ++a) {
                    if (fn.x < parent.x) { if (a == 0) clipped(g, xx - 4, yy - 36, 8, 240, 16, 16); else clipped(g, xx - 12 + a * 24, yy - 36, 72, 240, 24, 16); }
                    else if (a == 0) clipped(g, xx - 12, yy - 36, 24, 240, 16, 16);
                    else clipped(g, xx - 12 - a * 24, yy - 36, 72, 240, 24, 16);
                }
            }
            if (fn.node != null) continue;
            int s = parent.target && parent.trajectory ? 4 : 0;
            if (!inRegion(48, 16, 168, 192, xx - 4, yy - 4)) continue;
            if (parent.target) {
                g.pose().pushMatrix(); g.pose().translate(xx - s, yy); g.pose().scale(0.5f, 0.5f);
                g.blit(RenderPipelines.GUI_TEXTURED, TEX, -8, -8, 152, 240, 16, 16, 256, 256); g.pose().popMatrix();
            }
            if (!parent.trajectory) continue;
            g.pose().pushMatrix(); g.pose().translate(xx + s, yy); g.pose().scale(0.5f, 0.5f);
            g.blit(RenderPipelines.GUI_TEXTURED, TEX, -8, -8, 168, 240, 16, 16, 256, 256); g.pose().popMatrix();
        }
        if (hover >= 0 && lastNodeHover != hover) { playRollover(); lastNodeHover = hover; }
        if (hover < 0) lastNodeHover = -1;
    }

    private void drawNodeSettings(GuiGraphicsExtractor g) {
        FocusElementNode fn = data().get(selectedNode);
        if (fn == null || fn.node == null || fn.node.getSettingList().isEmpty()) return;
        int a = 0;
        for (String sk : fn.node.getSettingList()) {
            NodeSetting setting = fn.node.getSetting(sk);
            if (setting.getResearch() != null && !ClientResearch.knows(setting.getResearch())) continue;
            int x = leftPos + imageWidth;
            int y = topPos + imageHeight - 10 - fn.node.getSettingList().size() * 26 + a * 26;
            g.text(font, setting.getLocalizedName().copy().withStyle(ChatFormatting.GOLD), x, y, 0xFFFFFFFF, true);
            ++a;
        }
    }

    /** GuiFocalManipulator.drawPart: coloured socket plus the element icon. */
    public static void drawPart(GuiGraphicsExtractor g, FocusNode node, int x, int y, float scale, boolean mouseover) {
        boolean root = node.getType() == IFocusElement.EnumUnitType.MOD || node.getKey().equals("ROOT");
        if (root) scale *= 2.0f;
        int c = FocusEngine.getElementColor(node.getKey());
        int tint = 0xFF000000 | c;
        if (node.getType() == IFocusElement.EnumUnitType.EFFECT) quad(g, I_EFFECT, x, y, (float) (scale * 0.9 + (mouseover ? 2 : 0)), tint);
        else if (node.getType() == IFocusElement.EnumUnitType.MEDIUM && !root) quad(g, I_MEDIUM, x, y, (float) (scale * 0.9 + (mouseover ? 2 : 0)), tint);
        Identifier icon = FocusEngine.getElementIcon(node.getKey());
        if (icon != null) quad(g, icon, x, y, scale / 2.0f + (mouseover ? 2 : 0), 0xFFFFFFFF);
    }

    private static void quad(GuiGraphicsExtractor g, Identifier tex, int x, int y, float size, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(size / 32.0f, size / 32.0f);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, -16, -16, 0, 0, 32, 32, 32, 32, color);
        g.pose().popMatrix();
    }

    // ---------------- input ----------------
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean dbl) {
        int mx = (int) event.x(), my = (int) event.y(), button = event.button();
        for (TcWidget w : new ArrayList<>(buttons)) {
            if (w.press(mx, my)) {
                if (w.id == 0 && buttonConfirm.active) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                else if (w.id >= 10 && w instanceof TcWidget.Spinner) gatherInfo(true);
                return true;
            }
        }
        boolean r = super.mouseClicked(event, dbl);
        int gx = leftPos, gy = topPos;
        if (table.vis <= 0.0f && !data().isEmpty()) {
            if (lastNodeHover >= 0) {
                selectedNode = lastNodeHover;
                if (button == 1 && data().get(selectedNode).node != null) {
                    FocusElementNode fn = data().get(selectedNode);
                    if (data().get(fn.parent).node != null) {
                        String key = data().get(fn.parent).node.getKey();
                        addNodeAt(() -> (FocusNode) FocusEngine.getElement(key), key, fn.parent, true);
                    }
                }
                gatherInfo(false);
                playButtonClick();
            }
            int count = 0, index = 0;
            if (selectedNode >= 0) {
                for (String sk : shownParts) {
                    if (++count - 1 < partsStart) continue;
                    if (inRegion(gx + 28, gy + 32 + 24 * index, 20, 20, mx + leftPos, my + topPos)) {
                        addNodeAt(() -> (FocusNode) FocusEngine.getElement(sk), sk, selectedNode, true);
                        return true;
                    }
                    if (++index > 5) break;
                }
            }
        }
        return r;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (TcWidget w : buttons) w.release(event.x(), event.y());
        isMouseButtonDown = 0;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        for (TcWidget w : buttons) w.drag(event.x(), event.y());
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (scrollbarParts != null && shownParts.size() > 6 && inRegion(24, 24, 32, 157, (int) x, (int) y)) {
            if (sy > 0 && partsStart > 0) { --partsStart; scrollbarParts.setSliderValue(partsStart); }
            else if (sy < 0 && partsStart < shownParts.size() - 6) { ++partsStart; scrollbarParts.setSliderValue(partsStart); }
            return true;
        }
        return super.mouseScrolled(x, y, sx, sy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (nameField.isFocused()) {
            if (event.key() == 256) return super.keyPressed(event);
            return nameField.keyPressed(event) || true;
        }
        return super.keyPressed(event);
    }

    private void playButtonClick() { minecraft.player.playSound(ModSounds.get("clack"), 0.4f, 1.0f); }
    private void playRollover() { minecraft.player.playSound(ModSounds.get("clack"), 0.4f, 2.0f); }

    // ---------------- node tree editing ----------------
    private int getNextId() { while (data().containsKey(nodeID)) ++nodeID; return nodeID; }

    private void cullChildren(int idx) {
        if (!data().containsKey(idx)) return;
        for (int i : data().get(idx).children) { cullChildren(i); data().remove(i); }
    }

    private List<Integer> addNodeAt(Supplier<FocusNode> factory, String key, int idx, boolean gather) {
        List<Integer> ret = new ArrayList<>();
        boolean same = false;
        FocusElementNode previous = null;
        if (data().containsKey(idx)) {
            cullChildren(idx);
            if (data().get(idx).node != null && data().get(idx).node.getKey().equals(key)) same = true;
            else previous = data().remove(idx);
        }
        FocusElementNode fn;
        FocusNode node;
        if (!same) {
            fn = new FocusElementNode();
            node = factory.get();
            fn.node = node;
            if (previous != null) { fn.y = previous.y; fn.x = previous.x; }
            fn.id = getNextId();
            ret.add(fn.id);
            selectedNode = fn.id;
            if (previous != null && data().containsKey(previous.parent)) {
                fn.parent = previous.parent;
                int[] c = data().get(previous.parent).children;
                for (int a = 0; a < c.length; ++a) if (c[a] == previous.id) { c[a] = fn.id; break; }
            }
            fn.target = node.canSupply(FocusNode.EnumSupplyType.TARGET);
            fn.trajectory = node.canSupply(FocusNode.EnumSupplyType.TRAJECTORY);
            data().put(nodeID, fn);
        } else {
            fn = data().get(idx);
            node = fn.node;
        }
        if (fn.target || fn.trajectory) {
            if (node instanceof FocusModSplit) {
                FocusElementNode b1 = new FocusElementNode();
                b1.parent = fn.id; b1.id = getNextId(); ret.add(nodeID); b1.x = fn.x - 1; b1.y = fn.y + 1;
                data().put(nodeID, b1);
                selectedNode = nodeID;
                FocusElementNode b2 = new FocusElementNode();
                b2.parent = fn.id; b2.x = fn.x + 1; b2.y = fn.y + 1; b2.id = getNextId(); ret.add(nodeID);
                data().put(nodeID, b2);
                fn.children = new int[] {b1.id, b2.id};
            } else {
                FocusElementNode b3 = new FocusElementNode();
                b3.parent = fn.id; b3.x = fn.x; b3.y = fn.y + 1; b3.id = getNextId(); ret.add(nodeID);
                data().put(nodeID, b3);
                fn.children = new int[] {b3.id};
                selectedNode = nodeID;
            }
        }
        if (gather) { calcNodeTreeLayout(); gatherInfo(true); }
        return ret;
    }

    private int boundsX;
    private void processLeftNodes(FocusElementNode start) {
        if (start.children.length > 0) processLeftNodes(data().get(start.children[0]));
        int ox = 0;
        if (start.children.length == 1) { ox = boundsX - 1; boundsX = data().get(start.children[0]).x; }
        start.x = boundsX;
        if (start.children.length == 1) boundsX = ox;
        ++boundsX;
        if (start.children.length > 1) processLeftNodes(data().get(start.children[1]));
    }

    private void moveNodes(FocusElementNode start, int amt) {
        for (int ci : start.children) moveNodes(data().get(ci), amt);
        start.x -= amt;
    }

    private void calcNodeTreeLayout() {
        int fsi = -1;
        for (FocusElementNode n : data().values()) if (fsi < 0 && n.node instanceof FocusModSplit) fsi = n.id;
        if (fsi >= 0) { boundsX = 0; processLeftNodes(data().get(fsi)); moveNodes(data().get(fsi), boundsX / 2); }
        for (FocusElementNode n : data().values()) {
            if (!(n.node instanceof FocusModSplit)) continue;
            if (data().containsKey(n.parent) && data().get(n.parent).node != null && !(data().get(n.parent).node instanceof FocusModSplit)) n.x = data().get(n.parent).x;
            else { int xx = 0; for (int a : n.children) xx += data().get(a).x; n.x = xx / n.children.length; }
        }
        if (selectedNode >= 0 && !data().containsKey(selectedNode)) selectedNode = -1;
    }

    private void resetNodes() {
        nodeID = 0;
        data().clear();
        var r1 = addNodeAt(FocusMediumRoot::new, "ROOT", 0, false);
        selectedNode = r1.get(1);
        calcNodeTreeLayout();
        gatherInfo(true);
    }

    private void calcScrollBounds() {
        sMinX = sMinY = sMaxX = sMaxY = 0;
        for (FocusElementNode fn : data().values()) {
            sMinX = Math.min(sMinX, fn.x); sMinY = Math.min(sMinY, fn.y); sMaxX = Math.max(sMaxX, fn.x); sMaxY = Math.max(sMaxY, fn.y);
        }
    }

    private void gatherPartsList() {
        shownParts.clear();
        if (selectedNode < 0 || !data().containsKey(selectedNode)) return;
        partsStart = 0;
        List<String> pMed = new ArrayList<>(), pEff = new ArrayList<>(), pMod = new ArrayList<>(), excluded = new ArrayList<>();
        boolean hasExclusive = false, hasMedium = false;
        for (FocusElementNode fn : data().values()) {
            if (fn.node instanceof FocusMedium) {
                hasMedium = !(fn.node instanceof FocusMediumRoot);
                if (fn.node.isExclusive()) { hasExclusive = true; break; }
            }
            if (fn.node != null && fn.node.isExclusive()) excluded.add(fn.node.getKey());
        }
        FocusElementNode node = data().get(selectedNode);
        FocusElementNode parent = data().get(node.parent);
        if (parent != null && parent.node != null) {
            for (String key : FocusEngine.elements.keySet()) {
                IFocusElement element = FocusEngine.getElement(key);
                if (!ClientResearch.knows(element.getResearch())) continue;
                if (element.getKey().equals("ROOT")) continue;
                if (!(element instanceof FocusNode fn2)) continue;
                if (excluded.contains(fn2.getKey()) || fn2.mustBeSupplied() == null) continue;
                for (FocusNode.EnumSupplyType type : fn2.mustBeSupplied()) {
                    if (parent.node.canSupply(type)) {
                        switch (element.getType()) {
                            case EFFECT -> pEff.add(key);
                            case MEDIUM -> { if (!hasExclusive && (!((FocusMedium) element).isExclusive() || !hasMedium)) pMed.add(key); }
                            case MOD -> pMod.add(key);
                            default -> {}
                        }
                        break;
                    }
                }
            }
        }
        Collections.sort(pMed); Collections.sort(pEff); Collections.sort(pMod);
        shownParts.addAll(pMed); shownParts.addAll(pEff); shownParts.addAll(pMod);
    }

    private static int countCrystals(net.minecraft.world.entity.player.Player p, String aspect) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) if (CrystalEssenceItem.aspect(s).equals(aspect)) n += s.getCount();
        return n;
    }

    private void gatherInfo(boolean sync) {
        buttons.clear();
        buttons.add(buttonConfirm);
        buttonConfirm.x = leftPos + 242;
        buttonConfirm.y = topPos + 18;
        buttons.add(new TcWidget.Hover(1, leftPos + 241, topPos + 39, 32, 16, Component.translatable("wandtable.text5"), null, Identifier.parse("thaumcraft:textures/gui/complex.png")));
        buttons.add(new TcWidget.Hover(2, leftPos + 241, topPos + 53, 32, 16, Component.translatable("wandtable.text2"), null, Identifier.parse("thaumcraft:textures/gui/costxp.png")));
        buttons.add(new TcWidget.Hover(3, leftPos + 241, topPos + 67, 32, 16, Component.translatable("wandtable.text1"), null, Identifier.parse("thaumcraft:textures/gui/costvis.png")));
        FocusElementNode fn = data().get(selectedNode);
        if (fn != null && fn.node != null && !fn.node.getSettingList().isEmpty()) {
            int a = 0;
            for (String sk : fn.node.getSettingList()) {
                NodeSetting setting = fn.node.getSetting(sk);
                if (setting.getResearch() != null && !ClientResearch.knows(setting.getResearch())) continue;
                int x = leftPos + imageWidth;
                int y = topPos + imageHeight + 3 - fn.node.getSettingList().size() * 26 + a * 26;
                int w = setting.getType() instanceof NodeSetting.NodeSettingIntList ? 72 : 32;
                buttons.add(new TcWidget.Spinner(10 + a, x, y, w, setting));
                ++a;
            }
        }
        shownParts.clear();
        components = null;
        if (focusStack().isEmpty()) return;
        Map<String, Integer> compCount = new HashMap<>();
        totalComplexity = 0;
        maxComplexity = focusStack().getItem() instanceof FocusItem f ? f.getMaxComplexity() : 0;
        boolean emptyNodes = false;
        Map<String, Integer> crystals = new LinkedHashMap<>();
        for (FocusElementNode node : data().values()) {
            if (node.node != null) {
                int a2 = compCount.getOrDefault(node.node.getKey(), 0) + 1;
                node.complexityMultiplier = 0.5f * (a2 + 1);
                compCount.put(node.node.getKey(), a2);
                totalComplexity += (int) (node.node.getComplexity() * node.complexityMultiplier);
                if (node.node.getAspect() != null) crystals.merge(node.node.getAspect(), 1, Integer::sum);
            } else emptyNodes = true;
        }
        costCast = totalComplexity / 5.0f;
        costVis = totalComplexity * 10 + maxComplexity / 5;
        costXp = (int) Math.min(1L, Math.round(Math.sqrt(totalComplexity))); // TC6 uses min here (display only)
        boolean validCrystals = false;
        if (!crystals.isEmpty()) {
            validCrystals = true;
            components = new ItemStack[crystals.size()];
            int r = 0;
            for (var e : crystals.entrySet()) {
                components[r++] = CrystalEssenceItem.of(e.getKey(), e.getValue());
                if (countCrystals(minecraft.player, e.getKey()) < e.getValue()) validCrystals = false;
            }
            int i = 0, q = 0, z = 0;
            for (ItemStack stack : components) {
                buttons.add(new TcWidget.Hover(11 + z, leftPos + 234 + i * 16, topPos + 110 + 16 * q, 16, 16, stack.getHoverName(), null, stack));
                if (++i > 4) { i = 0; ++q; }
                ++z;
            }
        }
        gatherPartsList();
        scrollbarParts = null;
        if (shownParts.size() > 6) {
            scrollbarParts = new TcWidget.Slider(4, leftPos + 51, topPos + 30, 8, 149, 0.0f, shownParts.size() - 6, 0.0f, true);
            buttons.add(scrollbarParts);
        }
        valid = totalComplexity <= maxComplexity && !emptyNodes && validCrystals && table.xpCost <= minecraft.player.experienceLevel;
        calcScrollBounds();
        if (scrollY > (sMaxY - 3) * 32) scrollY = (sMaxY - 3) * 32;
        if (scrollX > sMaxX * 24) scrollX = sMaxX * 24;
        if (scrollX < sMinX * 24) scrollX = sMinX * 24;
        scrollbarMainSide = null;
        scrollbarMainBottom = null;
        if (sMaxY * 32 > 130) {
            scrollbarMainSide = new TcWidget.Slider(5, leftPos + 203, topPos + 32, 8, 156, 0.0f, (sMaxY - 3) * 32, scrollY, true);
            buttons.add(scrollbarMainSide);
        } else scrollY = 0;
        if (sMinX * 24 < -70 || sMaxX * 24 > 70) {
            scrollbarMainBottom = new TcWidget.Slider(6, leftPos + 64, topPos + 195, 132, 8, sMinX * 24, sMaxX * 24, scrollX, false);
            buttons.add(scrollbarMainBottom);
        } else scrollX = 0;
        if (table.focusName.isEmpty() && !focusStack().isEmpty()) table.focusName = focusStack().getHoverName().getString();
        if (nameField != null && !nameField.getValue().equals(table.focusName)) nameField.setValue(table.focusName);
        if (sync) {
            List<CompoundTag> nodes = new ArrayList<>();
            for (FocusElementNode n : data().values()) nodes.add(n.serialize());
            ClientPacketDistributor.sendToServer(new FocusNodesPayload(table.getBlockPos(), false, nodes, table.focusName));
        }
    }
}
