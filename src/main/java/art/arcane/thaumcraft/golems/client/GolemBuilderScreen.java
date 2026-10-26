package art.arcane.thaumcraft.golems.client;

import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.client.ClientResearch;
import art.arcane.thaumcraft.golems.GolemProperties;
import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.InvHelper;
import art.arcane.thaumcraft.golems.builder.GolemBuilderMenu;
import art.arcane.thaumcraft.golems.parts.GolemMaterial;
import art.arcane.thaumcraft.golems.parts.GolemPart;
import art.arcane.thaumcraft.network.GolemBuilderPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** TC6 GuiGolemBuilder: cycle the researched material, head, arms, legs and addon; shows stats, traits, components and cost. */
public final class GolemBuilderScreen extends AbstractContainerScreen<GolemBuilderMenu> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_golembuilder.png");
    private static final Identifier BASE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_base.png");
    private static final Identifier MAT_ICON = Identifier.fromNamespaceAndPath("thaumcraft", "textures/item/golem.png");
    static int headIndex, matIndex, armIndex, legIndex, addonIndex;
    private static boolean[] hasStuff;
    private final List<GolemPart> valHeads = new ArrayList<>(), valArms = new ArrayList<>(), valLegs = new ArrayList<>(), valAddons = new ArrayList<>();
    private final List<GolemMaterial> valMats = new ArrayList<>();
    private GolemProperties props = GolemProperties.fromLong(0L);
    private float hearts, armor, damage;
    private int cost;
    private boolean allfound, disableAll;
    private ItemStack[] components;
    private boolean[] owns;
    private record Hover(int x, int y, List<Component> tip, Object icon, int color) {}
    private final List<Hover> hovers = new ArrayList<>();

    public GolemBuilderScreen(GolemBuilderMenu menu, Inventory inv, Component title) { super(menu, inv, title, 208, 224); }
    public static void receiveStuff(boolean[] b) { hasStuff = b; GolemBuilderMenu.redo = true; }

    private static boolean knows(String[] r) { for (String s : r) if (!ClientResearch.knows(s)) return false; return true; }

    @Override protected void init() {
        super.init();
        valHeads.clear(); for (var h : GolemProperties.HEADS) if (knows(h.research)) valHeads.add(h);
        valMats.clear(); for (var m : GolemProperties.MATERIALS) if (knows(m.research)) valMats.add(m);
        valArms.clear(); for (var a : GolemProperties.ARMS) if (knows(a.research)) valArms.add(a);
        valLegs.clear(); for (var l : GolemProperties.LEGS) if (knows(l.research)) valLegs.add(l);
        valAddons.clear(); for (var a : GolemProperties.ADDONS) if (knows(a.research)) valAddons.add(a);
        if (headIndex >= valHeads.size()) headIndex = 0;
        if (matIndex >= valMats.size()) matIndex = 0;
        if (armIndex >= valArms.size()) armIndex = 0;
        if (legIndex >= valLegs.size()) legIndex = 0;
        if (addonIndex >= valAddons.size()) addonIndex = 0;
        gatherInfo();
    }
    private boolean ready() { return !valHeads.isEmpty() && !valMats.isEmpty() && !valArms.isEmpty() && !valLegs.isEmpty() && !valAddons.isEmpty(); }

    private void gatherInfo() {
        if (!ready() || menu.builder == null) return;
        props = GolemProperties.fromLong(0L);
        props.setHead(valHeads.get(headIndex)); props.setMaterial(valMats.get(matIndex)); props.setArms(valArms.get(armIndex));
        props.setLegs(valLegs.get(legIndex)); props.setAddon(valAddons.get(addonIndex));
        hasStuff = null;
        ClientPacketDistributor.sendToServer(new GolemBuilderPayload(menu.builder.getBlockPos(), props.toLong(), true));
        redoComps();
        int hh = 10 + props.getMaterial().healthMod;
        if (props.hasTrait(GolemTrait.FRAGILE)) hh = (int) (hh * 0.75);
        hearts = hh / 2.0f;
        int aa = props.getMaterial().armor;
        if (props.hasTrait(GolemTrait.ARMORED)) aa = (int) Math.max(aa * 1.5, aa + 1);
        if (props.hasTrait(GolemTrait.FRAGILE)) aa = (int) (aa * 0.75);
        armor = aa / 2.0f;
        double dd = props.hasTrait(GolemTrait.FIGHTER) ? props.getMaterial().damage : 0.0;
        if (props.hasTrait(GolemTrait.BRUTAL)) dd = Math.max(dd * 1.5, dd + 1.0);
        damage = (float) (dd / 2.0);
    }
    private void redoComps() {
        allfound = true;
        cost = props.getTraits().size() * 2;
        components = props.generateComponents();
        owns = new boolean[components.length];
        for (int a = 0; a < components.length; a++) {
            cost += components[a].getCount();
            owns[a] = hasStuff != null && hasStuff.length > a && hasStuff[a];
            if (!owns[a]) owns[a] = InvHelper.isPlayerCarryingAmount(Minecraft.getInstance().player, components[a], true);
            if (!owns[a]) allfound = false;
        }
    }

    private void blit(GuiGraphicsExtractor g, Identifier t, int x, int y, int u, int v, int w, int h, int col) { g.blit(RenderPipelines.GUI_TEXTURED, t, x, y, u, v, w, h, 256, 256, col); }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float t) {
        super.extractBackground(g, mx, my, t);
        if (GolemBuilderMenu.redo) { GolemBuilderMenu.redo = false; if (ready()) redoComps(); }
        int L = leftPos, T = topPos;
        blit(g, TEX, L, T, 0, 0, imageWidth, imageHeight, -1);
        hovers.clear();
        if (!ready() || menu.builder == null) return;
        boolean busy = menu.builder.cost > 0;
        disableAll = busy;
        if (components != null) {
            int i = 1, q = 0;
            for (int a = 0; a < components.length; a++) {
                if (!owns[a]) blit(g, TEX, L + 144 + q * 16, T + 16 + 16 * i, 240, 0, 16, 16, ARGB.colorFromFloat(0.5f, 1, 1, 1));
                if (++i > 3) { i = 0; ++q; }
            }
        }
        if (busy) blit(g, TEX, L + 145, T + 89, 209, 89, (int) (46.0f * (1.0f - menu.builder.cost / (float) Math.max(1, menu.builder.maxCost))), 6, -1);
        g.centeredText(font, Component.literal("" + hearts), L + 48, T + 108, 0xFFFFFFFF);
        g.centeredText(font, Component.literal("" + armor), L + 72, T + 108, 0xFFFFFFFF);
        g.centeredText(font, Component.literal("" + damage), L + 97, T + 108, 0xFFFFFFFF);
        for (int[] f : new int[][]{{12, 12}, {12, 60}, {108, 12}, {108, 36}, {108, 60}}) blit(g, TEX, L + f[0], T + f[1], 228, 124, 24, 24, -1);
        // scroll arrows
        scroll(g, valHeads.size(), L + 112 - 5 - 6, T - 5 + 16 + 8, L + 112 - 5 + 22);
        scroll(g, valMats.size(), L + 16 - 5 - 6, T - 5 + 16 + 8, L + 16 - 5 + 22);
        scroll(g, valArms.size(), L + 112 - 5 - 6, T - 5 + 40 + 8, L + 112 - 5 + 22);
        scroll(g, valLegs.size(), L + 112 - 5 - 6, T - 5 + 64 + 8, L + 112 - 5 + 22);
        scroll(g, valAddons.size(), L + 16 - 5 - 6, T - 5 + 64 + 8, L + 16 - 5 + 22);
        // craft button
        blit(g, TEX, L + 120, T + 104, 216, 64, 24, 16, -1);
        if (!allfound || disableAll) blit(g, TEX, L + 120, T + 104, 216, 40, 24, 16, -1);
        part(g, L + 120, T + 24, valHeads.get(headIndex));
        hover(g, L + 24, T + 24, List.of(valMats.get(matIndex).localizedName(), desc(valMats.get(matIndex).localizedDescription())), MAT_ICON, 0xFF000000 | valMats.get(matIndex).itemColor);
        part(g, L + 120, T + 48, valArms.get(armIndex));
        part(g, L + 120, T + 72, valLegs.get(legIndex));
        if (!valAddons.get(addonIndex).key.equalsIgnoreCase("none")) part(g, L + 24, T + 72, valAddons.get(addonIndex));
        GolemTrait[] tags = props.getTraits().toArray(new GolemTrait[0]);
        if (tags.length > 0) {
            int yy = tags.length <= 4 ? (tags.length - 1) % 4 * 8 : 24, xx = (tags.length - 1) / 4 % 4 * 8;
            int i = 0, q = 0;
            for (GolemTrait tag : tags) {
                hover(g, L + 72 + q * 16 - xx, T + 48 + 16 * i - yy, List.of(tag.localizedName(), desc(tag.localizedDescription())), tag.icon, -1);
                if (++i > 3) { i = 0; ++q; }
            }
        }
        if (components != null && components.length > 0) {
            var mach = AspectRegistry.get("machina");
            hover(g, L + 152, T + 24, List.of(Component.translatable("aspect.thaumcraft.machina")), Identifier.fromNamespaceAndPath("thaumcraft", "textures/aspects/machina.png"), 0xFF000000 | AspectRegistry.color("machina"));
            int i = 1, q = 0;
            for (ItemStack st : components) {
                hover(g, L + 152 + q * 16, T + 24 + 16 * i, List.of(st.getHoverName()), st, -1);
                if (++i > 3) { i = 0; ++q; }
            }
            g.text(font, Component.literal("" + cost), L + 162 - font.width("" + cost), T + 24, 0xFFFFFFFF, true);
        }
    }
    private static Component desc(Component c) { return c.copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.BLUE); }
    private void scroll(GuiGraphicsExtractor g, int n, int x1, int y, int x2) {
        if (n <= 1) return;
        blit(g, BASE, x1, y, 20, 0, 10, 10, -1);
        blit(g, BASE, x2, y, 30, 0, 10, 10, -1);
    }
    private void part(GuiGraphicsExtractor g, int x, int y, GolemPart p) { hover(g, x, y, List.of(p.localizedName(), desc(p.localizedDescription())), p.icon, -1); }
    private void hover(GuiGraphicsExtractor g, int x, int y, List<Component> tip, Object icon, int color) {
        if (icon instanceof Identifier id) g.blit(RenderPipelines.GUI_TEXTURED, id, x - 8, y - 8, 0, 0, 16, 16, 16, 16, color);
        else if (icon instanceof ItemStack st) g.item(st, x - 8, y - 8);
        hovers.add(new Hover(x, y, tip, icon, color));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float t) {
        super.extractRenderState(g, mx, my, t);
        for (Hover h : hovers) if (mx >= h.x - 8 && my >= h.y - 8 && mx < h.x + 8 && my < h.y + 8) { g.setComponentTooltipForNextFrame(font, h.tip, mx, my); break; }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {}

    private boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && my >= y && mx < x + w && my < y + h; }
    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        if (ready() && !disableAll) {
            double mx = e.x(), my = e.y();
            int L = leftPos, T = topPos;
            int[][] rows = {{112, 24, 0}, {16, 24, 1}, {112, 48, 2}, {112, 72, 3}, {16, 72, 4}};
            int[] sizes = {valHeads.size(), valMats.size(), valArms.size(), valLegs.size(), valAddons.size()};
            for (int r = 0; r < 5; r++) {
                if (sizes[r] <= 1) continue;
                int y = T - 5 + rows[r][1] - 16 + 8 + 16 - 8;
                y = T - 5 + rows[r][1];
                if (in(mx, my, L + rows[r][0] - 5 - 6, y, 10, 10)) { step(r, -1, sizes[r]); return true; }
                if (in(mx, my, L + rows[r][0] - 5 + 22, y, 10, 10)) { step(r, 1, sizes[r]); return true; }
            }
            if (in(mx, my, L + 120, T + 104, 24, 16) && allfound && menu.builder != null) {
                ClientPacketDistributor.sendToServer(new GolemBuilderPayload(menu.builder.getBlockPos(), props.toLong(), false));
                Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, 99);
                disableAll = true;
                return true;
            }
        }
        return super.mouseClicked(e, dbl);
    }
    private void step(int r, int d, int n) {
        switch (r) {
            case 0 -> headIndex = (headIndex + d + n) % n;
            case 1 -> matIndex = (matIndex + d + n) % n;
            case 2 -> armIndex = (armIndex + d + n) % n;
            case 3 -> legIndex = (legIndex + d + n) % n;
            default -> addonIndex = (addonIndex + d + n) % n;
        }
        gatherInfo();
    }
}
