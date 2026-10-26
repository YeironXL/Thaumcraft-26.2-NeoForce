package art.arcane.thaumcraft.client.book;

import art.arcane.thaumcraft.client.ClientResearch;
import art.arcane.thaumcraft.client.ResearchBackgroundTextures;
import art.arcane.thaumcraft.client.ResearchIcons;
import art.arcane.thaumcraft.client.ResearchMapData;
import art.arcane.thaumcraft.client.ResearchSnapshotListener;
import art.arcane.thaumcraft.network.ResearchFlagsPayload;
import art.arcane.thaumcraft.research.PlayerResearch;
import art.arcane.thaumcraft.research.ResearchCatalog;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Line-by-line port of TC6 GuiResearchBrowser (the Thaumonomicon research map). */
public class ResearchBrowserScreen extends Screen implements ResearchSnapshotListener {
    /** UtilsFX.renderQuadCentered with GL_SRC_ALPHA/GL_ONE blending (drawForbidden). */
    public static final RenderPipeline GUI_ADDITIVE = RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("thaumcraft", "pipeline/gui_textured_additive"))
            .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING)).build();
    /** ConfigResearch.TCCategories. */
    private static final String[] TC_CATEGORIES = {"BASICS", "ALCHEMY", "AUROMANCY", "ARTIFICE", "INFUSION", "GOLEMANCY", "ELDRITCH"};
    private static final Identifier BACK_OVER = Identifier.parse("thaumcraft:textures/gui/gui_research_back_over.png");
    private static final Identifier NODE_TEXTURE = Identifier.parse("thaumcraft:textures/misc/auranodes.png");

    private static int guiBoundsLeft, guiBoundsTop, guiBoundsRight, guiBoundsBottom;
    public static double lastX = -9999.0, lastY = -9999.0;
    static String selectedCategory = null;
    private static boolean searching = false;
    static int catScrollPos = 0, catScrollMax = 0;

    protected int mouseX, mouseY;
    protected float screenZoom = 1.0f;
    protected double curMouseX, curMouseY, guiMapX, guiMapY, tempMapX, tempMapY;
    private int isMouseButtonDown = 0;
    private int screenX, screenY;
    private final int startX = 16, startY = 16;
    private final LinkedList<ResearchCatalog.Entry> research = new LinkedList<>();
    private ResearchCatalog.Entry currentHighlight;
    long popuptime = 0L;
    String popupmessage = "";
    private EditBox searchField;
    private final ArrayList<String> categoriesTC = new ArrayList<>();
    private final ArrayList<String> categoriesOther = new ArrayList<>();
    public int addonShift = 0;
    private final ArrayList<String> invisible = new ArrayList<>();
    private final ArrayList<SearchEntry> searchResults = new ArrayList<>();
    private final Identifier tx1 = Identifier.parse("thaumcraft:textures/gui/gui_research_browser.png");
    private final ResearchBackgroundTextures backgrounds = new ResearchBackgroundTextures();
    /** A research page was opened from here; TC6 came back with a fresh GuiResearchBrowser(x, y). */
    private boolean reopened;

    public ResearchBrowserScreen() {
        super(Component.translatable("screen.thaumcraft.thaumonomicon"));
        tempMapX = guiMapX = curMouseX = lastX;
        tempMapY = guiMapY = curMouseY = lastY;
    }

    // ---------------------------------------------------------------- setup

    /** setWorldAndResolution: updateResearch plus re-centering when out of bounds. */
    @Override protected void init() {
        if (reopened) {
            // new GuiResearchBrowser(guiMapX, guiMapY): zoom and popup belong to the old instance.
            reopened = false; screenZoom = 1.0f; popuptime = 0L; isMouseButtonDown = 0;
            tempMapX = curMouseX = guiMapX; tempMapY = curMouseY = guiMapY;
        }
        updateResearch();
        if (lastX == -9999.0 || guiMapX > guiBoundsRight || guiMapX < guiBoundsLeft) {
            double n = (guiBoundsLeft + guiBoundsRight) / 2;
            tempMapX = n; guiMapX = n;
        }
        if (lastY == -9999.0 || guiMapY > guiBoundsBottom || guiMapY < guiBoundsTop) {
            double n2 = (guiBoundsBottom + guiBoundsTop) / 2;
            tempMapY = n2; guiMapY = n2;
        }
    }

    @Override public void onSnapshot() {
        String query = searchField != null ? searchField.getValue() : "";
        updateResearch();
        if (searching && searchField != null && !query.isEmpty()) { searchField.setValue(query); updateSearch(); }
    }

    public void updateResearch() {
        clearWidgets();
        addRenderableWidget(new SearchButton(1, height - 17, 16, 16, Component.translatable("tc.search")));
        searchField = new EditBox(font, 20, 20, 89, font.lineHeight, Component.translatable("tc.search"));
        searchField.setMaxLength(15);
        searchField.setBordered(true);
        searchField.visible = false;
        searchField.setTextColor(0xFFFFFFFF);
        addRenderableWidget(searchField);
        if (searching) {
            searchField.visible = true;
            searchField.setCanLoseFocus(false);
            setFocused(searchField);
            searchField.setFocused(true);
            searchField.setValue("");
            updateSearch();
        }
        searchField.setResponder(value -> { if (searching) updateSearch(); });
        screenX = width - 32;
        screenY = height - 32;
        research.clear();
        if (selectedCategory == null) selectedCategory = ResearchCatalog.CATEGORIES.getFirst();
        int limit = (int) Math.floor((screenY - 28) / 24.0f);
        addonShift = 0;
        int count = 0;
        categoriesTC.clear();
        categoriesOther.clear();
        outer:
        for (String rcl : ResearchCatalog.CATEGORIES) {
            int rt = 0, rco = 0;
            for (var res : categoryResearch(rcl)) {
                if (res.meta().contains("AUTOUNLOCK")) continue;
                ++rt;
                if (!ClientResearch.knowsResearch(res.key())) continue;
                ++rco;
            }
            int v = (int) (rco / (float) rt * 100.0f);
            String researchKey = categoryResearchKey(rcl);
            if (researchKey != null && !ClientResearch.knows(researchKey)) continue;
            for (String tcc : TC_CATEGORIES) {
                if (tcc.equals(rcl)) {
                    categoriesTC.add(rcl);
                    addRenderableWidget(new CategoryButton(rcl, false, 1, 10 + categoriesTC.size() * 24, 16, 16, Component.translatable("tc.research_category." + rcl), v));
                    continue outer;
                }
            }
            if (++count > limit + catScrollPos) continue;
            if (count - 1 < catScrollPos) continue;
            categoriesOther.add(rcl);
            addRenderableWidget(new CategoryButton(rcl, true, width - 17, 10 + categoriesOther.size() * 24, 16, 16, Component.translatable("tc.research_category." + rcl), v));
        }
        if (count > limit || count < catScrollPos) {
            addonShift = (screenY - 28) % 24 / 2;
            addRenderableWidget(new ScrollButton(false, width - 14, 20, 10, 11, () -> { if (catScrollPos > 0) { --catScrollPos; updateResearch(); } }));
            addRenderableWidget(new ScrollButton(true, width - 14, screenY + 1, 10, 11, () -> { if (catScrollPos < catScrollMax) { ++catScrollPos; updateResearch(); } }));
        }
        catScrollMax = count - limit;
        if (selectedCategory == null || selectedCategory.isEmpty()) return;
        research.addAll(categoryResearch(selectedCategory));
        guiBoundsLeft = 99999; guiBoundsTop = 99999; guiBoundsRight = -99999; guiBoundsBottom = -99999;
        for (var res3 : research) {
            if (res3 != null && isVisible(res3)) {
                int col = column(res3), row = row(res3);
                if (col * 24 - screenX + 48 < guiBoundsLeft) guiBoundsLeft = col * 24 - screenX + 48;
                if (col * 24 - 24 > guiBoundsRight) guiBoundsRight = col * 24 - 24;
                if (row * 24 - screenY + 48 < guiBoundsTop) guiBoundsTop = row * 24 - screenY + 48;
                if (row * 24 - 24 <= guiBoundsBottom) continue;
                guiBoundsBottom = row * 24 - 24;
            }
        }
    }

    /** ResearchCategory.research: the entries registered in a category (with a map location). */
    private static List<ResearchCatalog.Entry> categoryResearch(String category) {
        List<ResearchCatalog.Entry> out = new ArrayList<>();
        for (var e : ResearchCatalog.entries().values())
            if (e.category().equals(category) && ResearchMapData.node(e.key()) != null) out.add(e);
        return out;
    }
    /** ResearchCategory.researchKey (ConfigResearch.initCategories). */
    private static String categoryResearchKey(String category) { return category.equals("BASICS") ? null : "UNLOCK" + category; }
    private static int column(ResearchCatalog.Entry e) { return ResearchMapData.node(e.key()).column(); }
    private static int row(ResearchCatalog.Entry e) { return ResearchMapData.node(e.key()).row(); }
    private static String strip(String parent) { return parent.startsWith("~") ? parent.substring(1) : parent; }
    private static String clean(String parent) { String s = strip(parent); int at = s.indexOf('@'); return at < 0 ? s : s.substring(0, at); }
    private static String name(ResearchCatalog.Entry e) { return Component.translatable(e.name()).getString(); }
    private static boolean hasWarp(ResearchCatalog.Entry e) { for (var s : e.stages()) if (s.warp() > 0) return true; return false; }

    private boolean isVisible(ResearchCatalog.Entry res) {
        if (ClientResearch.knowsResearch(res.key())) return true;
        if (invisible.contains(res.key()) || (res.meta().contains("HIDDEN") && !canUnlockResearch(res))) return false;
        if (res.parents().isEmpty() && res.meta().contains("HIDDEN")) return false;
        for (String r : res.parents()) {
            var ri = ResearchCatalog.entries().get(r);
            if (ri != null && !isVisible(ri)) { invisible.add(r); return false; }
        }
        return true;
    }

    /** ResearchManager.doesPlayerHaveRequisites. */
    private static boolean canUnlockResearch(ResearchCatalog.Entry res) {
        for (String p : res.parents()) if (!ClientResearch.knows(strip(p))) return false;
        return true;
    }

    @Override public void removed() {
        lastX = guiMapX; lastY = guiMapY;
        backgrounds.close();
        super.removed();
    }

    @Override public boolean isPauseScreen() { return false; }

    // ---------------------------------------------------------------- search

    private void updateSearch() {
        searchResults.clear();
        invisible.clear();
        String s1 = searchField.getValue().toLowerCase(Locale.ROOT);
        for (String cat : categoriesTC)
            if (cat.toLowerCase(Locale.ROOT).contains(s1)) searchResults.add(new SearchEntry(Component.translatable("tc.research_category." + cat).getString(), cat, null, true));
        for (String cat : categoriesOther)
            if (cat.toLowerCase(Locale.ROOT).contains(s1)) searchResults.add(new SearchEntry(Component.translatable("tc.research_category." + cat).getString(), cat, null, true));
        List<String> dupCheck = new ArrayList<>();
        for (String pre : ClientResearch.researchList()) {
            var ri = ResearchCatalog.entries().get(pre);
            if (ri == null) continue;
            String localized = name(ri);
            if (localized.toLowerCase(Locale.ROOT).contains(s1)) searchResults.add(new SearchEntry(localized, pre, null, false));
            int stage = ClientResearch.stage(pre);
            if (ri.stages().isEmpty()) continue;
            int s2 = (ri.stages().size() - 1 < stage + 1) ? (ri.stages().size() - 1) : (stage + 1);
            if (s2 < 0) continue;
            var page = ri.stages().get(s2);
            for (String rec : page.recipes()) {
                if (dupCheck.contains(rec)) continue;
                dupCheck.add(rec);
                ItemStack ro = recipeOutput(rec);
                if (ro != null && !ro.isEmpty() && ro.getHoverName().getString().toLowerCase(Locale.ROOT).contains(s1))
                    searchResults.add(new SearchEntry(ro.getHoverName().getString(), pre, rec, false));
            }
        }
        searchResults.sort(null);
    }

    /** CommonInternals.getCatalogRecipe(...).getRecipeOutput() for the shipped book recipe of an original id. */
    private static ItemStack recipeOutput(String raw) {
        int colon = raw.indexOf(':');
        var id = Identifier.tryParse(colon < 0 ? raw : raw.substring(0, colon + 1) + raw.substring(colon + 1).toLowerCase(Locale.ROOT));
        if (id == null) return null;
        var resource = Minecraft.getInstance().getResourceManager().getResource(Identifier.fromNamespaceAndPath(id.getNamespace(), "book_recipes/" + id.getPath() + ".json"));
        if (resource.isEmpty()) return null;
        try (var reader = resource.get().openAsReader()) {
            var json = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            if (!json.has("result") || !json.get("result").isJsonObject()) return null;
            var result = json.getAsJsonObject("result");
            if (!result.has("id")) return null;
            var itemId = Identifier.tryParse(result.get("id").getAsString());
            if (itemId == null) return null;
            var item = BuiltInRegistries.ITEM.getValue(itemId);
            return item == null || item == Items.AIR ? null : new ItemStack(item);
        } catch (java.io.IOException | RuntimeException e) { return null; }
    }

    // ---------------------------------------------------------------- rendering

    /** drawDefaultBackground. */
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float a) { extractTransparentBackground(g); }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float par3) {
        if (!searching) {
            if (minecraft.mouseHandler.isLeftPressed()) {
                if ((isMouseButtonDown == 0 || isMouseButtonDown == 1) && mx >= startX && mx < startX + screenX && my >= startY && my < startY + screenY) {
                    if (isMouseButtonDown == 0) isMouseButtonDown = 1;
                    else {
                        guiMapX -= (mx - mouseX) * (double) screenZoom;
                        guiMapY -= (my - mouseY) * (double) screenZoom;
                        curMouseX = tempMapX = guiMapX;
                        curMouseY = tempMapY = guiMapY;
                    }
                    mouseX = mx; mouseY = my;
                }
                if (tempMapX < guiBoundsLeft * (double) screenZoom) tempMapX = guiBoundsLeft * (double) screenZoom;
                if (tempMapY < guiBoundsTop * (double) screenZoom) tempMapY = guiBoundsTop * (double) screenZoom;
                if (tempMapX >= guiBoundsRight * (double) screenZoom) tempMapX = guiBoundsRight * screenZoom - 1.0f;
                if (tempMapY >= guiBoundsBottom * (double) screenZoom) tempMapY = guiBoundsBottom * screenZoom - 1.0f;
            } else isMouseButtonDown = 0;
        }
        int locX = Mth.floor(curMouseX + (guiMapX - curMouseX) * par3);
        int locY = Mth.floor(curMouseY + (guiMapY - curMouseY) * par3);
        if (locX < guiBoundsLeft * screenZoom) locX = (int) (guiBoundsLeft * screenZoom);
        if (locY < guiBoundsTop * screenZoom) locY = (int) (guiBoundsTop * screenZoom);
        if (locX >= guiBoundsRight * screenZoom) locX = (int) (guiBoundsRight * screenZoom - 1.0f);
        if (locY >= guiBoundsBottom * screenZoom) locY = (int) (guiBoundsBottom * screenZoom - 1.0f);
        if (!searching) {
            g.pose().pushMatrix();
            g.pose().scale(1.0f / screenZoom, 1.0f / screenZoom);
            genResearchBackgroundZoomable(g, mx, my, locX, locY);
            g.pose().popMatrix();
        } else {
            int q = 0;
            for (var sr : searchResults) {
                int color = sr.cat ? 14527146 : (sr.recipe == null ? 14540253 : 11184861);
                if (sr.recipe != null) g.blit(RenderPipelines.GUI_TEXTURED, tx1, 22, 32 + q * 10, 224, 48, 8, 8, 16, 16, 256, 256);
                if (mx > 22 && mx < 18 + screenX && my >= 32 + q * 10 && my < 40 + q * 10)
                    color = sr.recipe == null ? 16777215 : (sr.cat ? 16764108 : 13421823);
                g.text(font, sr.display, 32, 32 + q * 10, 0xFF000000 | color, false);
                ++q;
                if (32 + (q + 1) * 10 > screenY) {
                    g.text(font, Component.translatable("tc.search.more"), 22, 34 + q * 10, 0xFFAAAAAA, false);
                    break;
                }
            }
        }
        genResearchBackgroundFixedPost(g, mx, my, par3);
        if (popuptime > System.currentTimeMillis()) {
            g.nextStratum();
            drawCustomTooltip(g, List.of(new Line(Component.literal(popupmessage), false)), 10, 34, mx);
        }
    }

    /** UtilsFX/GuiScreen.updateScreen: 0.85 easing towards the drag target. */
    @Override public void tick() {
        curMouseX = guiMapX;
        curMouseY = guiMapY;
        double var1 = tempMapX - guiMapX;
        double var2 = tempMapY - guiMapY;
        if (var1 * var1 + var2 * var2 < 4.0) { guiMapX += var1; guiMapY += var2; }
        else { guiMapX += var1 * 0.85; guiMapY += var2 * 0.85; }
    }

    /** drawTexturedModalRectWithDoubles: UVs over 256 with GL_REPEAT, drawn as wrapped tiles. */
    private static void tiled(GuiGraphicsExtractor g, Identifier texture, float x, float y, double u0, double v0, float w, float h) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        int width = Mth.ceil(w), height = Mth.ceil(h);
        for (int yy = 0; yy < height; ) {
            double v = Math.floorMod((long) Math.floor(v0 + yy), 256L) + ((v0 + yy) - Math.floor(v0 + yy));
            int sh = Math.max(1, Math.min(height - yy, (int) Math.ceil(256.0 - v)));
            for (int xx = 0; xx < width; ) {
                double u = Math.floorMod((long) Math.floor(u0 + xx), 256L) + ((u0 + xx) - Math.floor(u0 + xx));
                int sw = Math.max(1, Math.min(width - xx, (int) Math.ceil(256.0 - u)));
                g.blit(texture, xx, yy, xx + sw, yy + sh, (float) (u / 256.0), (float) Math.min(1.0, (u + sw) / 256.0),
                        (float) (v / 256.0), (float) Math.min(1.0, (v + sh) / 256.0));
                xx += sw;
            }
            yy += sh;
        }
        g.pose().popMatrix();
    }

    private static int background(String category) { return ResearchMapData.background(category); }

    protected void genResearchBackgroundZoomable(GuiGraphicsExtractor g, int mx, int my, int locX, int locY) {
        tiled(g, backgrounds.get(background(selectedCategory)), (startX - 2) * screenZoom, (startY - 2) * screenZoom, locX / 2.0, locY / 2.0, (screenX + 4) * screenZoom, (screenY + 4) * screenZoom);
        tiled(g, BACK_OVER, (startX - 2) * screenZoom, (startY - 2) * screenZoom, locX / 1.5, locY / 1.5, (screenX + 4) * screenZoom, (screenY + 4) * screenZoom);
        for (var source : research) {
            List<String> parents = source.parents();
            for (String rawParent : parents) {
                var parent = ResearchCatalog.entries().get(clean(rawParent));
                if (parent == null || !parent.category().equals(selectedCategory) || ResearchMapData.node(parent.key()) == null) continue;
                if (parent.siblings().contains(source.key())) continue;
                boolean knowsParent = ClientResearch.knows(rawParent);
                boolean b = isVisible(source) && !rawParent.startsWith("~");
                if (!b) continue;
                if (knowsParent) drawLine(g, column(source), row(source), column(parent), row(parent), 0.6f, 0.6f, 0.6f, locX, locY, true, source.meta().contains("REVERSE"));
                else if (isVisible(parent)) drawLine(g, column(source), row(source), column(parent), row(parent), 0.2f, 0.2f, 0.2f, locX, locY, true, source.meta().contains("REVERSE"));
            }
            for (String rawSibling : source.siblings()) {
                var sibling = ResearchCatalog.entries().get(rawSibling);
                if (sibling == null || !sibling.category().equals(selectedCategory) || ResearchMapData.node(sibling.key()) == null) continue;
                boolean knowsSibling = ClientResearch.knows(sibling.key());
                if (!isVisible(source) || rawSibling.startsWith("~")) continue;
                if (knowsSibling) drawLine(g, column(sibling), row(sibling), column(source), row(source), 0.3f, 0.3f, 0.4f, locX, locY, false, source.meta().contains("REVERSE"));
                else if (isVisible(sibling)) drawLine(g, column(sibling), row(sibling), column(source), row(source), 0.1875f, 0.1875f, 0.25f, locX, locY, false, source.meta().contains("REVERSE"));
            }
        }
        currentHighlight = null;
        for (var iconResearch : research) {
            int var25 = column(iconResearch) * 24 - locX;
            int var26 = row(iconResearch) * 24 - locY;
            if (var25 < -24 || var26 < -24 || var25 > screenX * screenZoom || var26 > screenY * screenZoom) continue;
            int iconX = startX + var25, iconY = startY + var26;
            if (!isVisible(iconResearch)) continue;
            if (hasWarp(iconResearch)) drawForbidden(g, iconX + 8, iconY + 8);
            float var27;
            boolean canUnlock = canUnlockResearch(iconResearch);
            if (ClientResearch.isComplete(iconResearch.key())) var27 = 1.0f;
            else if (canUnlock) var27 = (float) Math.sin(System.currentTimeMillis() % 600L / 600.0 * Math.PI * 2.0) * 0.25f + 0.75f;
            else var27 = 0.3f;
            int tint = gray(var27);
            boolean hidden = iconResearch.meta().contains("HIDDEN");
            if (iconResearch.meta().contains("ROUND")) g.blit(RenderPipelines.GUI_TEXTURED, tx1, iconX - 8, iconY - 8, 144, 48 + (hidden ? 32 : 0), 32, 32, 256, 256, tint);
            else {
                int ix = 80, iy = 48;
                if (hidden) iy += 32;
                if (iconResearch.meta().contains("HEX")) ix += 32;
                g.blit(RenderPipelines.GUI_TEXTURED, tx1, iconX - 8, iconY - 8, ix, iy, 32, 32, 256, 256, tint);
            }
            if (iconResearch.meta().contains("SPIKY")) g.blit(RenderPipelines.GUI_TEXTURED, tx1, iconX - 8, iconY - 8, 176, 48 + (hidden ? 32 : 0), 32, 32, 256, 256, tint);
            boolean bw = !canUnlock;
            if (ClientResearch.hasFlag(iconResearch.key(), PlayerResearch.FLAG_RESEARCH))
                g.blit(RenderPipelines.GUI_TEXTURED, tx1, iconX - 9, iconY - 9, 176, 16, 16, 16, 32, 32, 256, 256);
            if (ClientResearch.hasFlag(iconResearch.key(), PlayerResearch.FLAG_PAGE))
                g.blit(RenderPipelines.GUI_TEXTURED, tx1, iconX - 9, iconY + 9, 208, 16, 16, 16, 32, 32, 256, 256);
            ResearchIcons.draw(g, iconResearch.key(), iconX, iconY, bw);
            if (mx >= startX && my >= startY && mx < startX + screenX && my < startY + screenY
                    && mx >= (iconX - 2) / screenZoom && mx <= (iconX + 18) / screenZoom && my >= (iconY - 2) / screenZoom && my <= (iconY + 18) / screenZoom)
                currentHighlight = iconResearch;
        }
    }

    private static int gray(float f) { int c = Mth.clamp((int) (f * 255.0f), 0, 255); return 0xFF000000 | c << 16 | c << 8 | c; }
    private static int rgb(float r, float gg, float b) { return 0xFF000000 | (int) (r * 255) << 16 | (int) (gg * 255) << 8 | (int) (b * 255); }

    private void genResearchBackgroundFixedPost(GuiGraphicsExtractor g, int mx, int my, float par3) {
        g.nextStratum();
        for (int c = 16; c < width - 16; c += 64) {
            int p = 64;
            if (c + p > width - 16) p = width - 16 - c;
            if (p > 0) {
                g.blit(RenderPipelines.GUI_TEXTURED, tx1, c, -2, 48, 13, p, 22, 256, 256);
                g.blit(RenderPipelines.GUI_TEXTURED, tx1, c, height - 20, 48, 13, p, 22, 256, 256);
            }
        }
        for (int c = 16; c < height - 16; c += 64) {
            int p = 64;
            if (c + p > height - 16) p = height - 16 - c;
            if (p > 0) {
                g.blit(RenderPipelines.GUI_TEXTURED, tx1, -2, c, 13, 48, 22, p, 256, 256);
                g.blit(RenderPipelines.GUI_TEXTURED, tx1, width - 20, c, 13, 48, 22, p, 256, 256);
            }
        }
        g.blit(RenderPipelines.GUI_TEXTURED, tx1, -2, -2, 13, 13, 22, 22, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, tx1, -2, height - 20, 13, 13, 22, 22, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, tx1, width - 20, -2, 13, 13, 22, 22, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, tx1, width - 20, height - 20, 13, 13, 22, 22, 256, 256);
        super.extractRenderState(g, mx, my, par3);
        if (currentHighlight != null) {
            List<Line> text = new ArrayList<>();
            text.add(new Line(Component.literal(name(currentHighlight)).withStyle(ChatFormatting.GOLD), false));
            if (canUnlockResearch(currentHighlight)) {
                if (!ClientResearch.isComplete(currentHighlight.key()) && !currentHighlight.stages().isEmpty()) {
                    int stage = ClientResearch.stage(currentHighlight.key());
                    if (stage > 0) text.add(new Line(Component.translatable("tc.research.stage").append(" " + stage + "/" + currentHighlight.stages().size()).withStyle(ChatFormatting.AQUA), true));
                    else text.add(new Line(Component.translatable("tc.research.begin").withStyle(ChatFormatting.GREEN), true));
                }
            } else {
                text.add(new Line(Component.translatable("tc.researchmissing").withStyle(ChatFormatting.RED), true));
                for (String p2 : currentHighlight.parents()) {
                    if (!ClientResearch.knows(p2)) {
                        var parent = ResearchCatalog.entries().get(clean(p2));
                        String s = parent == null ? "?" : name(parent);
                        text.add(new Line(Component.literal(" - " + s).withStyle(ChatFormatting.YELLOW), true));
                    }
                }
            }
            if (ClientResearch.hasFlag(currentHighlight.key(), PlayerResearch.FLAG_RESEARCH)) text.add(new Line(Component.translatable("tc.research.newresearch"), true));
            if (ClientResearch.hasFlag(currentHighlight.key(), PlayerResearch.FLAG_PAGE)) text.add(new Line(Component.translatable("tc.research.newpage"), true));
            g.nextStratum();
            drawCustomTooltip(g, text, mx + 3, my - 3, mx);
        }
    }

    /** GuiResearchBrowser.drawLine with the authored atlas segments. */
    private void drawLine(GuiGraphicsExtractor g, int x, int y, int x2, int y2, float r, float gg, float b, int locX, int locY, boolean arrow, boolean flipped) {
        boolean bigCorner = false;
        int xd, yd, xm, ym, xx, yy;
        if (flipped) {
            xd = Math.abs(x2 - x); yd = Math.abs(y2 - y);
            xm = xd == 0 ? 0 : (x2 - x > 0 ? -1 : 1);
            ym = yd == 0 ? 0 : (y2 - y > 0 ? -1 : 1);
            if (xd > 1 && yd > 1) bigCorner = true;
            xx = x2 * 24 - 4 - locX + startX;
            yy = y2 * 24 - 4 - locY + startY;
        } else {
            xd = Math.abs(x - x2); yd = Math.abs(y - y2);
            xm = xd == 0 ? 0 : (x - x2 > 0 ? -1 : 1);
            ym = yd == 0 ? 0 : (y - y2 > 0 ? -1 : 1);
            if (xd > 1 && yd > 1) bigCorner = true;
            xx = x * 24 - 4 - locX + startX;
            yy = y * 24 - 4 - locY + startY;
        }
        int color = rgb(r, gg, b);
        if (arrow) {
            if (flipped) {
                int xx2 = x * 24 - 8 - locX + startX, yy2 = y * 24 - 8 - locY + startY;
                if (xm < 0) rect(g, xx2, yy2, 160, 112, 32, 32, color);
                else if (xm > 0) rect(g, xx2, yy2, 128, 112, 32, 32, color);
                else if (ym > 0) rect(g, xx2, yy2, 64, 112, 32, 32, color);
                else if (ym < 0) rect(g, xx2, yy2, 96, 112, 32, 32, color);
            } else if (ym < 0) rect(g, xx - 4, yy - 4, 64, 112, 32, 32, color);
            else if (ym > 0) rect(g, xx - 4, yy - 4, 96, 112, 32, 32, color);
            else if (xm > 0) rect(g, xx - 4, yy - 4, 160, 112, 32, 32, color);
            else if (xm < 0) rect(g, xx - 4, yy - 4, 128, 112, 32, 32, color);
        }
        int v = 1, h = 0;
        while (v < yd - (bigCorner ? 1 : 0)) { rect(g, xx + xm * 24 * h, yy + ym * 24 * v, 0, 228, 24, 24, color); ++v; }
        if (bigCorner) {
            if (xm < 0 && ym > 0) rect(g, xx + xm * 24 * h - 24, yy + ym * 24 * v, 0, 180, 48, 48, color);
            if (xm > 0 && ym > 0) rect(g, xx + xm * 24 * h, yy + ym * 24 * v, 48, 180, 48, 48, color);
            if (xm < 0 && ym < 0) rect(g, xx + xm * 24 * h - 24, yy + ym * 24 * v - 24, 96, 180, 48, 48, color);
            if (xm > 0 && ym < 0) rect(g, xx + xm * 24 * h, yy + ym * 24 * v - 24, 144, 180, 48, 48, color);
        } else {
            if (xm < 0 && ym > 0) rect(g, xx + xm * 24 * h, yy + ym * 24 * v, 48, 228, 24, 24, color);
            if (xm > 0 && ym > 0) rect(g, xx + xm * 24 * h, yy + ym * 24 * v, 72, 228, 24, 24, color);
            if (xm < 0 && ym < 0) rect(g, xx + xm * 24 * h, yy + ym * 24 * v, 96, 228, 24, 24, color);
            if (xm > 0 && ym < 0) rect(g, xx + xm * 24 * h, yy + ym * 24 * v, 120, 228, 24, 24, color);
        }
        v += bigCorner ? 1 : 0;
        for (h += bigCorner ? 2 : 1; h < xd; ++h) rect(g, xx + xm * 24 * h, yy + ym * 24 * v, 24, 228, 24, 24, color);
    }
    private void rect(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h, int color) {
        g.blit(RenderPipelines.GUI_TEXTURED, tx1, x, y, u, v, w, h, 256, 256, color);
    }

    /** drawForbidden: animated aura-node frame (row 5 of the 32x32 grid), 90 px, purple, additive. */
    public static void drawForbidden(GuiGraphicsExtractor g, double x, double y) {
        var player = Minecraft.getInstance().player;
        int count = player == null ? 0 : player.tickCount;
        int frame = 160 + count % 32;
        int xm = frame % 32, ym = frame / 32;
        g.pose().pushMatrix();
        g.pose().translate((float) x, (float) y);
        // TexturedQuadTC's vertex/UV order turns the cell a quarter turn.
        g.pose().rotate((float) (Math.PI / 2));
        int color = ((int) (0.9f * 255.0f)) << 24 | (int) (0.33f * 255) << 16 | 0 << 8 | (int) (0.44f * 255);
        g.blit(GUI_ADDITIVE, NODE_TEXTURE, -45, -45, xm * 64, ym * 64, 90, 90, 64, 64, 2048, 2048, color);
        g.pose().popMatrix();
    }

    // ---------------------------------------------------------------- tooltip

    private record Line(Component text, boolean small) {}

    /** UtilsFX.drawCustomTooltip ("@@" lines are half size). */
    private void drawCustomTooltip(GuiGraphicsExtractor g, List<Line> textList, int x, int y, int mouseXNow) {
        if (textList.isEmpty()) return;
        int max = 240;
        boolean flip = false;
        if (max + 24 + mouseXNow > width) {
            max = width - mouseXNow - 24;
            if (max < 120) { max = 240; flip = true; }
        }
        boolean b = false;
        for (var l : textList) if (font.width(l.text) > max) { b = true; break; }
        List<FormattedCharSequence> seqs = new ArrayList<>();
        List<Boolean> small = new ArrayList<>();
        for (var l : textList) {
            if (b) for (var part : font.split(l.text, l.small ? max * 2 : max)) { seqs.add(part); small.add(l.small); }
            else { seqs.add(l.text.getVisualOrderText()); small.add(l.small); }
        }
        int widestLineWidth = 0, totalHeight = -2;
        for (int i = 0; i < seqs.size(); i++) {
            int lineWidth = font.width(seqs.get(i));
            if (small.get(i)) lineWidth /= 2;
            if (lineWidth > widestLineWidth) widestLineWidth = lineWidth;
            totalHeight += small.get(i) ? 7 : 10;
        }
        int sX = x + 12, sY = y - 12;
        if (seqs.size() > 1) totalHeight += 2;
        if (sY + totalHeight > height) sY = height - totalHeight - 5;
        if (flip) sX -= widestLineWidth + 24;
        int var10 = -267386864;
        g.fillGradient(sX - 3, sY - 4, sX + widestLineWidth + 3, sY - 3, var10, var10);
        g.fillGradient(sX - 3, sY + totalHeight + 3, sX + widestLineWidth + 3, sY + totalHeight + 4, var10, var10);
        g.fillGradient(sX - 3, sY - 3, sX + widestLineWidth + 3, sY + totalHeight + 3, var10, var10);
        g.fillGradient(sX - 4, sY - 3, sX - 3, sY + totalHeight + 3, var10, var10);
        g.fillGradient(sX + widestLineWidth + 3, sY - 3, sX + widestLineWidth + 4, sY + totalHeight + 3, var10, var10);
        int var11 = 1347420415;
        int var12 = (var11 & 0xFEFEFE) >> 1 | (var11 & 0xFF000000);
        g.fillGradient(sX - 3, sY - 3 + 1, sX - 3 + 1, sY + totalHeight + 3 - 1, var11, var12);
        g.fillGradient(sX + widestLineWidth + 2, sY - 3 + 1, sX + widestLineWidth + 3, sY + totalHeight + 3 - 1, var11, var12);
        g.fillGradient(sX - 3, sY - 3, sX + widestLineWidth + 3, sY - 3 + 1, var11, var11);
        g.fillGradient(sX - 3, sY + totalHeight + 2, sX + widestLineWidth + 3, sY + totalHeight + 3, var12, var12);
        for (int i = 0; i < seqs.size(); ++i) {
            g.pose().pushMatrix();
            g.pose().translate(sX, sY);
            boolean shift = small.get(i);
            if (shift) { sY += 7; g.pose().scale(0.5f, 0.5f); }
            else sY += 10;
            g.text(font, seqs.get(i), 0, shift ? 3 : 0, -1, true);
            if (i == 0) sY += 2;
            g.pose().popMatrix();
        }
    }

    // ---------------------------------------------------------------- input

    @Override public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (!searching) {
            if (scrollY < 0) screenZoom += 0.25f;
            else if (scrollY > 0) screenZoom -= 0.25f;
            screenZoom = Mth.clamp(screenZoom, 1.0f, 2.0f);
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (!searching && minecraft.options.keyInventory.matches(event)) { onClose(); return true; }
        return super.keyPressed(event);
    }

    private void openPage(String key) {
        reopened = true;
        Minecraft.getInstance().gui.setScreen(new ResearchPageScreen(key, this));
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        popuptime = System.currentTimeMillis() - 1L;
        if (!searching && currentHighlight != null && !ClientResearch.knowsResearch(currentHighlight.key()) && canUnlockResearch(currentHighlight)) {
            var highlight = currentHighlight;
            updateResearch();
            ClientPacketDistributor.sendToServer(new ResearchFlagsPayload(highlight.key(), ResearchFlagsPayload.START));
            openPage(highlight.key());
            popuptime = System.currentTimeMillis() + 3000L;
            popupmessage = Component.translatable(Component.translatable("tc.research.popup").getString(), name(highlight)).getString();
            return true;
        } else if (currentHighlight != null && ClientResearch.knowsResearch(currentHighlight.key())) {
            String key = currentHighlight.key();
            ClientResearch.clearFlag(key, PlayerResearch.FLAG_RESEARCH);
            ClientResearch.clearFlag(key, PlayerResearch.FLAG_PAGE);
            ClientPacketDistributor.sendToServer(new ResearchFlagsPayload(key, ResearchFlagsPayload.CLEAR));
            int stage = ClientResearch.stage(key);
            if (stage > 1 && stage >= currentHighlight.stages().size())
                ClientPacketDistributor.sendToServer(new ResearchFlagsPayload(key, ResearchFlagsPayload.FINISH));
            openPage(key);
            return true;
        } else if (searching) {
            int q = 0;
            for (var sr : searchResults) {
                if (mx > 22 && mx < 18 + screenX && my >= 32 + q * 10 && my < 40 + q * 10) {
                    if (ClientResearch.knowsResearch(sr.key) && !sr.cat) {
                        if (sr.recipe != null) { reopened = true; Minecraft.getInstance().gui.setScreen(new ResearchPageScreen(sr.key, BookRecipes.norm(sr.recipe), 0, this)); }
                        else openPage(sr.key);
                        return true;
                    }
                    if (categoriesTC.contains(sr.key) || categoriesOther.contains(sr.key)) {
                        selectCategoryAndCenter(sr.key);
                        break;
                    }
                }
                ++q;
                if (32 + (q + 1) * 10 > screenY) break;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void selectCategoryAndCenter(String key) {
        searching = false;
        searchField.visible = false;
        searchField.setCanLoseFocus(true);
        searchField.setFocused(false);
        selectedCategory = key;
        updateResearch();
        double n = (guiBoundsLeft + guiBoundsRight) / 2;
        tempMapX = n; guiMapX = n;
        double n2 = (guiBoundsBottom + guiBoundsTop) / 2;
        tempMapY = n2; guiMapY = n2;
    }

    private void startSearch() {
        selectedCategory = "";
        searching = true;
        searchField.visible = true;
        searchField.setCanLoseFocus(false);
        setFocused(searchField);
        searchField.setFocused(true);
        searchField.setValue("");
        updateSearch();
    }

    /** Category tab press (actionPerformed id >= 20); false when the category is not listed. */
    public boolean selectCategory(String key) {
        if (!categoriesTC.contains(key) && !categoriesOther.contains(key)) return false;
        if (!key.equals(selectedCategory)) selectCategoryAndCenter(key);
        return true;
    }

    /** Same as clicking a visible node of the current category. */
    public boolean openResearch(String key) {
        for (var e : research) if (e.key().equals(key) && isVisible(e)) {
            currentHighlight = e;
            var event = new MouseButtonEvent(-1, -1, new net.minecraft.client.input.MouseButtonInfo(0, 0));
            return mouseClicked(event, false);
        }
        return false;
    }

    // ---------------------------------------------------------------- widgets

    private static final class SearchEntry implements Comparable<SearchEntry> {
        final String display, key, recipe;
        final boolean cat;
        SearchEntry(String display, String key, String recipe, boolean cat) { this.display = display; this.key = key; this.recipe = recipe; this.cat = cat; }
        @Override public int compareTo(SearchEntry o) {
            int d = display.compareTo(o.display);
            if (d != 0) return d;
            int k = key.compareTo(o.key);
            return (k == 0 && recipe != null && o.recipe != null) ? recipe.compareTo(o.recipe) : k;
        }
    }

    private final class CategoryButton extends Button {
        final String key;
        final boolean flip;
        final int completion;
        CategoryButton(String key, boolean flip, int x, int y, int w, int h, Component label, int completion) {
            super(x, y + addonShift, w, h, label, b -> {}, DEFAULT_NARRATION);
            this.key = key; this.flip = flip; this.completion = completion;
        }
        @Override public void onPress(net.minecraft.client.input.InputWithModifiers input) {
            if (!key.equals(selectedCategory)) selectCategoryAndCenter(key);
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float tick) {
            int x = getX(), y = getY();
            boolean hovered = isHovered();
            g.blit(RenderPipelines.GUI_TEXTURED, tx1, x - 3, y - 3, 13, 13, 22, 22, 256, 256, selectedCategory.equals(key) ? 0xFF99FFFF : 0xFFFFFFFF);
            int iconColor = selectedCategory.equals(key) || hovered ? 0xFFFFFFFF : 0xCCA8A8A8;
            g.blit(RenderPipelines.GUI_TEXTURED, categoryIcon(key), x, y, 0, 0, 16, 16, 16, 16, iconColor);
            boolean nr = false, np = false;
            for (var e : ResearchCatalog.entries().values()) {
                if (!e.category().equals(key) || !ClientResearch.knowsResearch(e.key())) continue;
                if (!nr && ClientResearch.hasFlag(e.key(), PlayerResearch.FLAG_RESEARCH)) nr = true;
                if (!np && ClientResearch.hasFlag(e.key(), PlayerResearch.FLAG_PAGE)) np = true;
                if (nr && np) break;
            }
            if (nr) g.blit(RenderPipelines.GUI_TEXTURED, tx1, x - 2, y - 2, 176, 16, 8, 8, 32, 32, 256, 256, 0xB3FFFFFF);
            if (np) g.blit(RenderPipelines.GUI_TEXTURED, tx1, x - 2, y + 9, 208, 16, 8, 8, 32, 32, 256, 256, 0xB3FFFFFF);
            if (hovered) {
                String dp = getMessage().getString() + " (" + completion + "%)";
                g.text(font, dp, flip ? (screenX + 9 - font.width(dp)) : (x + 22), y + 4, 0xFFFFFFFF, true);
                int t = 9;
                if (nr) {
                    var s = Component.translatable("tc.research.newresearch");
                    g.text(font, s, flip ? (screenX + 9 - font.width(s)) : (x + 22), y + 4 + t, 0xFFFFFFFF, true);
                    t += 9;
                }
                if (np) {
                    var s = Component.translatable("tc.research.newpage");
                    g.text(font, s, flip ? (screenX + 9 - font.width(s)) : (x + 22), y + 4 + t, 0xFFFFFFFF, true);
                }
            }
        }
    }

    /** ResearchCategory.icon. */
    private static Identifier categoryIcon(String key) {
        if (key.equals("BASICS")) return Identifier.parse("thaumcraft:textures/item/thaumonomicon_cheat.png");
        return Identifier.parse("thaumcraft:textures/research/cat_" + key.toLowerCase(Locale.ROOT) + ".png");
    }

    private final class ScrollButton extends Button {
        final boolean flip;
        final Runnable action;
        ScrollButton(boolean flip, int x, int y, int w, int h, Runnable action) {
            super(x, y, w, h, Component.empty(), b -> {}, DEFAULT_NARRATION);
            this.flip = flip; this.action = action;
        }
        @Override public void onPress(net.minecraft.client.input.InputWithModifiers input) { action.run(); }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float tick) {
            g.blit(RenderPipelines.GUI_TEXTURED, tx1, getX(), getY(), 51, flip ? 71 : 55, 10, 11, 256, 256, isHovered() ? 0xFFFFFFFF : 0xFFB3B3B3);
        }
    }

    private final class SearchButton extends Button {
        SearchButton(int x, int y, int w, int h, Component label) { super(x, y, w, h, label, b -> {}, DEFAULT_NARRATION); }
        @Override public void onPress(net.minecraft.client.input.InputWithModifiers input) { startSearch(); }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float tick) {
            g.blit(RenderPipelines.GUI_TEXTURED, tx1, getX(), getY(), 160, 16, 16, 16, 256, 256, isHovered() ? 0xFFFFFFFF : 0xFFCCCCCC);
            if (isHovered()) g.text(font, getMessage(), getX() + 19, getY() + 4, 0xFFFFFFFF, true);
        }
    }
}
