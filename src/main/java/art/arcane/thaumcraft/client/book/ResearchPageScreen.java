package art.arcane.thaumcraft.client.book;

import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.client.ClientResearch;
import art.arcane.thaumcraft.client.ResearchSnapshotListener;
import art.arcane.thaumcraft.client.ThaumcraftTheme;
import art.arcane.thaumcraft.crafting.multiblock.Part;
import art.arcane.thaumcraft.network.AdvanceResearchPayload;
import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.registry.ModSounds;
import art.arcane.thaumcraft.research.LegacyItemReferences;
import art.arcane.thaumcraft.research.PlayerResearch;
import art.arcane.thaumcraft.research.ResearchCatalog;
import art.arcane.thaumcraft.research.ResearchItemCosts;
import art.arcane.thaumcraft.research.ResearchRules;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Line-by-line port of TC6 GuiResearchPage (6.1.BETA26). */
public final class ResearchPageScreen extends Screen implements ResearchSnapshotListener {
    public static final LinkedList<String> history = new LinkedList<>();
    static String shownRecipe;
    private static int aspectsPage = 0;
    static boolean cycleMultiblockLines = false;

    private static final Identifier TEX1 = Identifier.parse("thaumcraft:textures/gui/gui_researchbook.png");
    private static final Identifier TEX2 = Identifier.parse("thaumcraft:textures/gui/gui_researchbook_overlay.png");
    private static final Identifier TEX3 = Identifier.parse("thaumcraft:textures/aspects/_back.png");
    private static final Identifier TEX4 = Identifier.parse("thaumcraft:textures/gui/paper.png");
    private static final Identifier DUMMY_RESEARCH = Identifier.parse("thaumcraft:textures/aspects/_unknown.png");
    private static final Identifier DUMMY_MAP = Identifier.parse("thaumcraft:textures/research/rd_map.png");
    private static final Identifier DUMMY_FLASK = Identifier.parse("thaumcraft:textures/research/rd_flask.png");
    private static final Identifier DUMMY_CHEST = Identifier.parse("thaumcraft:textures/research/rd_chest.png");
    private static final Identifier[] KNOW_TYPE = {
            Identifier.parse("thaumcraft:textures/research/knowledge_theory.png"),
            Identifier.parse("thaumcraft:textures/research/knowledge_observation.png")};
    private static final String[] KNOW_NAMES = {"THEORY", "OBSERVATION"};
    private static final int[] KNOW_PROGRESSION = {32, 16};

    private static final PageImage PILINE = PageImage.parse("thaumcraft:textures/gui/gui_researchbook.png:24:184:95:6:1");
    private static final PageImage PIDIV = PageImage.parse("thaumcraft:textures/gui/gui_researchbook.png:28:192:140:6:1");

    private final Screen parent;
    private final ResearchCatalog.Entry research;
    private final List<Addendum> addendaAll;
    private final int paneWidth = 256, paneHeight = 181;
    private int currentStage = 0, lastStage = 0;
    private boolean hold = false;
    private int page = 0, maxPages = 0, maxAspectPages = 0;
    private final List<String> knownPlayerAspects = new ArrayList<>();
    private int rhash = 0;
    private final float rotX = 25.0f, rotY = -45.0f;
    private long lastCheck = 0L;
    private int hrx = 0, hry = 0;
    private int recipePage = 0, recipePageMax = 0;
    private long lastCycle = 0L;
    private boolean showingAspects = false, showingKnowledge = false;
    private final LinkedHashMap<String, List<BookRecipes.Recipe>> recipeLists = new LinkedHashMap<>();
    private final LinkedHashMap<String, List<Object>> recipeOutputs = new LinkedHashMap<>();
    private final LinkedHashMap<String, List<BookRecipes.Recipe>> drilldownLists = new LinkedHashMap<>();
    private boolean hasRecipePages;
    private boolean renderingCompound = false;
    private BlueprintAccess blockAccess = null;
    private final HashMap<String, BlueprintAccess> blockAccessIcons = new HashMap<>();
    private final List<Object[]> reference = new ArrayList<>();
    private int cycle = -1;
    private boolean allowWithPagePopup = false;
    private List<Component> tipText = null;
    private Optional<TooltipComponent> tipImage = Optional.empty();
    private final List<List<Object>> pages = new ArrayList<>();
    private boolean isComplete = false, hasAllRequisites = false;
    private boolean[] hasItem, hasCraft, hasResearch, hasKnow;
    private PlayerResearch knowledge = new PlayerResearch();
    private float partialTick;
    private int lastDrawY;
    private int lastDrawX;

    record Addendum(String text, List<String> recipes, List<String> research) {}

    public ResearchPageScreen(String researchKey, Screen parent) { this(researchKey, null, 0, parent); }

    public ResearchPageScreen(String researchKey, String recipe, int recipePage, Screen parent) {
        super(Component.translatable("screen.thaumcraft.thaumonomicon"));
        this.parent = parent;
        this.research = ResearchCatalog.entries().get(researchKey);
        this.addendaAll = loadAddenda(researchKey);
        refreshKnowledge();
        parsePages();
        for (Aspect a : AspectRegistry.all().values())
            if (knowledge.has("!" + a.id().toLowerCase(Locale.ROOT))) knownPlayerAspects.add(a.id());
        maxAspectPages = (int) Math.ceil(knownPlayerAspects.size() / 5.0f);
        page = 0;
        if (recipe != null) {
            shownRecipe = BookRecipes.norm(recipe);
            this.recipePage = recipePage;
            if (!recipeLists.containsKey(shownRecipe) && !drilldownLists.containsKey(shownRecipe))
                addRecipesToList(shownRecipe, drilldownLists, new LinkedHashMap<>(), shownRecipe);
        }
    }

    /**
     * Opens the page that teaches the recipe for an item (GuiResearchPage.getCraftingRecipeKey + clickthrough), or
     * null when the book has no known recipe for it.
     */
    public static ResearchPageScreen forRecipe(ItemStack stack, Screen parent) {
        if (stack == null || stack.isEmpty()) return null;
        String ref = BookRecipes.craftingRecipeKey(stack);
        if (ref == null) return null;
        String[] sr = ref.split(";", 2);
        if (sr.length < 2) return null;
        String rec = BookRecipes.norm(sr[0]);
        for (var entry : ResearchCatalog.entries().values()) {
            if (!ClientResearch.progress().has(entry.key())) continue;
            for (var stage : entry.stages())
                for (String r : stage.recipes())
                    if (BookRecipes.norm(r).equals(rec)) {
                        history.clear();
                        return new ResearchPageScreen(entry.key(), rec, Integer.parseInt(sr[1]), parent);
                    }
        }
        return null;
    }

    private static List<Addendum> loadAddenda(String key) {
        List<Addendum> out = new ArrayList<>();
        for (String file : List.of("basics", "auromancy", "alchemy", "artifice", "infusion", "golemancy", "eldritch", "scans")) {
            var in = ResearchPageScreen.class.getResourceAsStream("/assets/thaumcraft/research/" + file + ".json");
            if (in == null) continue;
            try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                for (JsonElement e : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("entries")) {
                    JsonObject o = e.getAsJsonObject();
                    if (!o.get("key").getAsString().equals(key) || !o.has("addenda")) continue;
                    for (JsonElement a : o.getAsJsonArray("addenda")) {
                        JsonObject ao = a.getAsJsonObject();
                        out.add(new Addendum(ao.get("text").getAsString(), strings(ao, "recipes"), strings(ao, "required_research")));
                    }
                    return out;
                }
            } catch (Exception ignored) {}
        }
        return out;
    }

    private static List<String> strings(JsonObject o, String k) {
        if (!o.has(k)) return List.of();
        List<String> l = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray(k)) l.add(e.getAsString());
        return l;
    }

    /** ThaumcraftCapabilities.getKnowledge(player) on the client: snapshot plus the "page" block. */
    private void refreshKnowledge() {
        PlayerResearch base = ClientResearch.progress();
        PlayerResearch p = new PlayerResearch();
        for (String k : base.researched()) p.unlock(k);
        base.stages().forEach(p::setStage);
        try {
            JsonObject snap = ClientResearch.snapshot();
            if (snap.has("page")) {
                JsonObject pg = snap.getAsJsonObject("page");
                if (pg.has("crafted")) pg.getAsJsonArray("crafted").forEach(e -> p.recordCraft(e.getAsString()));
                if (pg.has("knowledge")) for (var e : pg.getAsJsonObject("knowledge").entrySet()) {
                    String[] parts = e.getKey().split(";");
                    if (parts.length == 2) p.addKnowledge(parts[0], parts[1], e.getValue().getAsInt());
                }
            }
        } catch (RuntimeException ignored) {}
        knowledge = p;
    }

    @Override public void onSnapshot() { refreshKnowledge(); lastCheck = 0L; }

    private static boolean knowsStrict(String key) { return ClientResearch.knows(key); }
    private static boolean knowsStrict(List<String> keys) { for (String k : keys) if (!ClientResearch.knows(k)) return false; return true; }
    private boolean isResearchComplete(String key) { return ResearchRules.complete(knowledge, key); }
    private int getKnowledge(int type, String category) { return knowledge.knowledge(KNOW_NAMES[type], category) / KNOW_PROGRESSION[type]; }
    private int getKnowledgeRaw(int type, String category) { return knowledge.knowledge(KNOW_NAMES[type], category); }

    private void playSound(String name, float volume, float pitch) {
        if (minecraft != null && minecraft.player != null) minecraft.player.playSound(ModSounds.get(name), volume, pitch);
    }

    // ============================================================== rendering

    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) { extractTransparentBackground(g); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int par1, int par2, float par3) {
        hasRecipePages = false;
        long nano = System.nanoTime();
        if (nano > lastCheck) {
            refreshKnowledge();
            parsePages();
            lastCheck = hold ? nano + 250000000L : nano + 2000000000L;
            if (currentStage > lastStage) hold = false;
        }
        partialTick = par3;
        genResearchBackground(g, par1, par2, par3);
        int sw = (width - paneWidth) / 2;
        int sh = (height - paneHeight) / 2;
        if (!history.isEmpty()) {
            int mx = par1 - (sw + 118);
            int my = par2 - (sh + 190);
            if (mx >= 0 && my >= 0 && mx < 20 && my < 12) {
                g.nextStratum();
                g.text(font, ink(Component.translatable("recipe.return")), par1, par2, 0xFFFFFFFF, true);
            }
        }
        super.extractRenderState(g, par1, par2, par3);
    }

    private void genResearchBackground(GuiGraphicsExtractor g, int par1, int par2, float par3) {
        int sw = (width - paneWidth) / 2;
        int sh = (height - paneHeight) / 2;
        float var10 = (width - paneWidth * 1.3f) / 2.0f;
        float var11 = (height - paneHeight * 1.3f) / 2.0f;
        g.pose().pushMatrix();
        g.pose().translate(var10, var11);
        g.pose().scale(1.3f, 1.3f);
        rect(g, TEX1, 0, 0, 0, 0, paneWidth, paneHeight, -1);
        g.pose().popMatrix();
        reference.clear();
        int current = 0;
        boolean drew = false;
        for (int a = 0; a < pages.size(); ++a) {
            if ((current == page || current == page + 1) && current < maxPages) {
                drawPage(g, pages.get(a), current % 2, sw, sh - 10, par1, par2);
                drew = true;
            }
            if (++current > page + 1) break;
        }
        // The overlays (recipe, inserts, forbidden warning) are drawn once over the finished spread.
        if (drew) drawOverlays(g, lastDrawX, lastDrawY, par1, par2);
        g.nextStratum();
        float bob = (float) Math.sin(minecraft.player.tickCount / 3.0f) * 0.2f + 0.1f;
        if (!history.isEmpty()) rectScaled(g, sw + 118, sh + 190, 38, 202, 20, 12, bob);
        if (page > 0 && shownRecipe == null) rectScaled(g, sw - 16, sh + 190, 0, 184, 12, 8, bob);
        if (page < maxPages - 2 && shownRecipe == null) rectScaled(g, sw + 262, sh + 190, 12, 184, 12, 8, bob);
        if (tipText != null) {
            g.setTooltipForNextFrame(font, tipText, tipImage, par1, par2 + 12);
            tipText = null;
            tipImage = Optional.empty();
        }
    }

    private void drawPage(GuiGraphicsExtractor g, List<Object> pageParm, int side, int x, int y, int mx, int my) {
        if (lastCycle < System.currentTimeMillis()) {
            ++cycle;
            lastCycle = System.currentTimeMillis() + 1000L;
            if (cycleMultiblockLines && blockAccess != null) ++blockAccess.sliceLine;
        }
        if (page == 0 && side == 0) {
            rect(g, TEX1, x + 4, y - 7, 24, 184, 96, 4, -1);
            rect(g, TEX1, x + 4, y + 10, 24, 184, 96, 4, -1);
            Component name = ink(Component.translatable(research.name()));
            int offset = font.width(name);
            if (offset <= 140) {
                g.text(font, name, x - 15 + 140 / 2 - offset / 2, y, 0xFF202020, false);
            } else {
                float vv = 140.0f / offset;
                g.pose().pushMatrix();
                g.pose().translate(x - 15 + 140 / 2 - offset / 2 * vv, y + 1.0f * vv);
                g.pose().scale(vv, vv);
                g.text(font, name, 0, 0, 0xFF202020, false);
                g.pose().popMatrix();
            }
            y += 28;
        }
        for (Object content : pageParm) {
            if (content instanceof Line line) {
                g.text(font, line.draw(), x - 15 + side * 152, y - 6, 0xFF000000, false);
                y += font.lineHeight;
                if (!line.raw().endsWith("~B")) continue;
                y += (int) (font.lineHeight * 0.66);
            } else if (content instanceof PageImage pi) {
                int pad = (140 - pi.aw) / 2;
                g.pose().pushMatrix();
                g.pose().translate(x - 15 + side * 152 + pad, y - 5);
                g.pose().scale(pi.scale, pi.scale);
                rect(g, pi.loc, 0, 0, pi.x, pi.y, pi.w, pi.h, -1);
                g.pose().popMatrix();
                y += pi.ah + 2;
            }
        }
        if (isResearchComplete("FIRSTSTEPS")) {
            y = (height - paneHeight) / 2 + 9;
            int le = mouseInside(x - 48, y, 25, 16, mx, my) ? 0 : 3;
            drawPopupAt(x - 48, y, 25, 16, mx, my, "tc.aspect.name");
            rect(g, TEX1, x - 48 + le, y, 76, 232, 24 - le, 16, -1);
            rect(g, TEX1, x - 28, y, 100, 232, 4, 16, -1);
        }
        if (isResearchComplete("KNOWLEDGETYPES") && !research.key().equals("KNOWLEDGETYPES")) {
            y = (height - paneHeight) / 2 + 32;
            int le = mouseInside(x - 48, y, 25, 16, mx, my) ? 0 : 3;
            drawPopupAt(x - 48, y, 25, 16, mx, my, "tc.knowledge.name");
            rect(g, TEX1, x - 49 + le, y, 44, 232, 24 - le, 16, -1);
            rect(g, TEX1, x - 29, y, 100, 232, 4, 16, -1);
        }
        ResearchCatalog.Stage stage = research.stages().get(currentStage);
        if (!stage.recipes().isEmpty()) drawRecipeBookmarks(g, x, mx, my);
        if (page == 0 && side == 0 && !isComplete) drawRequirements(g, x, mx, my, stage);
        if (isResearchComplete("KNOWLEDGETYPES") && research.key().equals("KNOWLEDGETYPES"))
            drawKnowledges(g, x, (height - paneHeight) / 2 - 16 + 210, mx, my, true);
        lastDrawX = x;
        lastDrawY = y;
    }

    private void drawOverlays(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        ResearchCatalog.Stage stage = research.stages().get(currentStage);
        renderingCompound = false;
        if (showingAspects) {
            g.nextStratum();
            drawAspectsInsert(g, mx, my);
        } else if (showingKnowledge) {
            g.nextStratum();
            drawKnowledgesInsert(g, mx, my);
        } else if (shownRecipe != null) {
            g.nextStratum();
            drawRecipe(g, mx, my);
        } else if (stage.warp() > 0 && !isComplete) {
            int warp = Math.min(stage.warp(), 5);
            drawForbidden(g, x - 57, y - 40);
            Component s = ink(Component.translatable("tc.forbidden.level." + warp));
            g.text(font, s, x - 56 - font.width(s) / 2, y - 43, 0xFFAA99FF, false);
            String text = Component.translatable("tc.warp.warn").getString();
            drawPopupAt(x - 67, y - 50, 20, 20, mx, my, text.replaceAll("%n", s.getString()));
        }
    }

    /** GuiResearchPage calls GuiResearchBrowser.drawForbidden: 90 px node frame, purple, additive (GL_SRC_ALPHA, GL_ONE). */
    private void drawForbidden(GuiGraphicsExtractor g, int x, int y) {
        ResearchBrowserScreen.drawForbidden(g, x, y);
    }

    private void drawKnowledgesInsert(GuiGraphicsExtractor g, int mx, int my) {
        allowWithPagePopup = true;
        int x = (width - 256) / 2;
        int y = (height - 256) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, TEX4, x, y, 0, 0, 255, 255, 256, 256);
        g.nextStratum();
        drawKnowledges(g, x + 60, (height - paneHeight) / 2 + 75, mx, my, false);
        allowWithPagePopup = false;
    }

    private static Identifier categoryIcon(String category) {
        return category.equals("BASICS") ? Identifier.parse("thaumcraft:textures/item/thaumonomicon_cheat.png")
                : Identifier.parse("thaumcraft:textures/research/cat_" + category.toLowerCase(Locale.ROOT) + ".png");
    }

    private void drawKnowledges(GuiGraphicsExtractor g, int x, int y, int mx, int my, boolean inpage) {
        y -= 18;
        boolean drewSomething = false;
        int tc = 0;
        int ka = ResearchCatalog.CATEGORIES.size();
        for (int type = 0; type < 2; type++) {
            int fc = 0;
            int hs = (int) (164.0f / ka);
            boolean b = false;
            for (String category : ResearchCatalog.CATEGORIES) {
                int amt = getKnowledge(type, category);
                int par = getKnowledgeRaw(type, category) % KNOW_PROGRESSION[type];
                if (amt <= 0 && par <= 0) continue;
                drewSomething = true;
                int ix = x - 10 + (inpage ? 18 : hs) * fc, iy = y - tc * (inpage ? 20 : 28);
                full(g, KNOW_TYPE[type], ix, iy, 16, -1);
                // category icon: 0.0625 * 0.66 scale at 66,66 -> 10.5px inset 2.7px, alpha 0.75
                g.pose().pushMatrix();
                g.pose().translate(ix + 66 * 0.66f * 0.0625f, iy + 66 * 0.66f * 0.0625f);
                g.pose().scale(255 * 0.66f * 0.0625f / 16f, 255 * 0.66f * 0.0625f / 16f);
                full(g, categoryIcon(category), 0, 0, 16, argb(1, 1, 1, 0.75f));
                g.pose().popMatrix();
                String s = "" + amt;
                int m = font.width(s);
                g.text(font, ink(s), x - 10 + 16 - m + (inpage ? 18 : hs) * fc, iy + 8, 0xFFFFFFFF, true);
                String tip = Component.translatable("tc.type." + KNOW_NAMES[type].toLowerCase(Locale.ROOT)).getString()
                        + ": " + Component.translatable("tc.research_category." + category).getString();
                drawPopupAt(ix, iy, mx, my, tip);
                if (par > 0) {
                    int l = (int) (par / (float) KNOW_PROGRESSION[type] * 16.0f);
                    rect(g, TEX1, ix, y + 17 - tc * (inpage ? 20 : 28), 0, 232, l, 2, argb(1, 1, 1, 0.75f));
                    rect(g, TEX1, ix + l, y + 17 - tc * (inpage ? 20 : 28), l, 234, 16 - l, 2, argb(1, 1, 1, 0.75f));
                }
                ++fc;
                b = true;
            }
            if (b) ++tc;
        }
        if (inpage && drewSomething) rect(g, TEX1, x + 4, y - tc * 20 + 12, 24, 184, 96, 8, -1);
    }

    private void drawRequirements(GuiGraphicsExtractor g, int x, int mx, int my, ResearchCatalog.Stage stage) {
        int y = (height - paneHeight) / 2 - 16 + 210;
        boolean b = false;
        List<String> research = stage.requirements().get("required_research");
        List<String> obtain = stage.requirements().get("required_item");
        List<String> craft = stage.requirements().get("required_craft");
        List<String> know = stage.requirements().get("required_knowledge");
        if (research != null) {
            y -= 18;
            b = true;
            int shift = 24;
            rect(g, TEX1, x - 12, y - 1, 200, 232, 56, 16, argb(1, 1, 1, 0.25f));
            drawPopupAt(x - 15, y, mx, my, "tc.need.research");
            if (hasResearch != null) {
                if (hasResearch.length != research.size()) hasResearch = new boolean[research.size()];
                int ss = 18;
                if (research.size() > 6) ss = 110 / research.size();
                for (int a = 0; a < research.size(); ++a) {
                    String[] split = research.get(a).split(";");
                    String key = split[0];
                    Object loc = split.length > 1 ? icon(split[1]) : DUMMY_RESEARCH;
                    String text = Component.translatable("research." + key + ".text").getString();
                    int color = -1;
                    if (key.startsWith("!")) {
                        String k = key.replaceAll("!", "");
                        if (AspectRegistry.get(k) != null) { loc = AspectRegistry.get(k); text = aspectName(k); }
                    }
                    ResearchCatalog.Entry re = ResearchCatalog.entries().get(key);
                    List<String> icons = re == null ? List.of() : entryIcons(key);
                    if (re != null && !icons.isEmpty()) {
                        int idx = (int) (System.currentTimeMillis() / 1000L % icons.size());
                        loc = icon(icons.get(idx));
                        text = Component.translatable(re.name()).getString();
                    } else if (key.startsWith("m_")) loc = DUMMY_MAP;
                    else if (key.startsWith("c_")) loc = DUMMY_CHEST;
                    else if (key.startsWith("f_")) loc = DUMMY_FLASK;
                    else color = argb(0.5f, 0.75f, 1.0f, 1.0f);
                    if (loc instanceof Aspect as) full(g, Identifier.parse("thaumcraft:textures/aspects/" + as.id() + ".png"), x - 15 + shift, y, 16, 0xFF000000 | AspectRegistry.color(as.id()));
                    else if (loc instanceof Identifier rl) full(g, rl, x - 15 + shift, y, 16, color);
                    else if (loc instanceof ItemStack is) g.item(is, x - 15 + shift, y);
                    if (hasResearch[a]) rect(g, TEX1, x - 15 + shift + 8, y, 159, 207, 10, 10, -1);
                    drawPopupAt(x - 15 + shift, y, mx, my, text);
                    shift += ss;
                }
            }
        }
        if (obtain != null) {
            y -= 18;
            b = true;
            int shift = 24;
            rect(g, TEX1, x - 12, y - 1, 200, 216, 56, 16, argb(1, 1, 1, 0.25f));
            drawPopupAt(x - 15, y, mx, my, "tc.need.obtain");
            if (hasItem != null) {
                if (hasItem.length != obtain.size()) hasItem = new boolean[obtain.size()];
                int ss2 = 18;
                if (obtain.size() > 6) ss2 = 110 / obtain.size();
                for (int idx2 = 0; idx2 < obtain.size(); ++idx2) {
                    ItemStack stack = BookRecipes.cycle(requirementStacks(obtain.get(idx2), true), idx2);
                    drawStackAt(g, stack, x - 15 + shift, y, mx, my, true);
                    if (hasItem[idx2]) { g.nextStratum(); rect(g, TEX1, x - 15 + shift + 8, y, 159, 207, 10, 10, -1); }
                    shift += ss2;
                }
            }
        }
        if (craft != null) {
            y -= 18;
            b = true;
            int shift = 24;
            rect(g, TEX1, x - 12, y - 1, 200, 200, 56, 16, argb(1, 1, 1, 0.25f));
            drawPopupAt(x - 15, y, mx, my, "tc.need.craft");
            if (hasCraft != null) {
                if (hasCraft.length != craft.size()) hasCraft = new boolean[craft.size()];
                int ss2 = 18;
                if (craft.size() > 6) ss2 = 110 / craft.size();
                for (int idx2 = 0; idx2 < craft.size(); ++idx2) {
                    ItemStack stack = BookRecipes.cycle(requirementStacks(craft.get(idx2), false), idx2);
                    drawStackAt(g, stack, x - 15 + shift, y, mx, my, true);
                    if (hasCraft[idx2]) { g.nextStratum(); rect(g, TEX1, x - 15 + shift + 8, y, 159, 207, 10, 10, -1); }
                    shift += ss2;
                }
            }
        }
        if (know != null) {
            y -= 18;
            b = true;
            int shift = 24;
            rect(g, TEX1, x - 12, y - 1, 200, 184, 56, 16, argb(1, 1, 1, 0.25f));
            drawPopupAt(x - 15, y, mx, my, "tc.need.know");
            if (hasKnow != null) {
                if (hasKnow.length != know.size()) hasKnow = new boolean[know.size()];
                int ss2 = 18;
                if (know.size() > 6) ss2 = 110 / know.size();
                for (int idx2 = 0; idx2 < know.size(); ++idx2) {
                    String[] kn = know.get(idx2).split(";");
                    int type = kn[0].equals("THEORY") ? 0 : 1;
                    String category = kn.length > 1 ? kn[1] : null;
                    int amount = kn.length > 2 ? parseInt(kn[2]) : 0;
                    full(g, KNOW_TYPE[type], x - 15 + shift, y, 16, -1);
                    if (category != null) {
                        // 0.0625 * 0.75 scale, translated 32,32 -> 12px icon inset 2px
                        g.pose().pushMatrix();
                        g.pose().translate(x - 15 + shift + 2, y + 2);
                        g.pose().scale(255 * 0.75f * 0.0625f / 16f, 255 * 0.75f * 0.0625f / 16f);
                        full(g, categoryIcon(category), 0, 0, 16, -1);
                        g.pose().popMatrix();
                    }
                    String am = (hasKnow[idx2] ? "" : "§c") + amount;
                    g.nextStratum();
                    g.pose().pushMatrix();
                    g.pose().translate(x - 15 + shift + 16 - font.width(am) / 2, y + 12);
                    g.pose().scale(0.5f, 0.5f);
                    g.text(font, ink(am), 0, 0, 0xFFFFFFFF, true);
                    g.pose().popMatrix();
                    if (hasKnow[idx2]) rect(g, TEX1, x - 15 + shift + 8, y, 159, 207, 10, 10, -1);
                    String s = Component.translatable("tc.type." + kn[0].toLowerCase(Locale.ROOT)).getString();
                    if (category != null) s = s + ": " + Component.translatable("tc.research_category." + category).getString();
                    drawPopupAt(x - 15 + shift, y, mx, my, s);
                    shift += ss2;
                }
            }
        }
        if (b) {
            y -= 12;
            rect(g, TEX1, x + 4, y - 2, 24, 184, 96, 8, -1);
            if (hasAllRequisites) {
                hrx = x + 20;
                hry = y - 6;
                if (hold) {
                    Component s2 = ink(Component.translatable("tc.stage.hold"));
                    int m = font.width(s2);
                    g.text(font, s2, Math.round(x + 52 - m / 2.0f), y - 4, 0xFFFFFFFF, true);
                } else {
                    int color = mouseInside(hrx, hry, 64, 12, mx, my) ? -1 : argb(0.8f, 0.8f, 0.9f, 1.0f);
                    rect(g, TEX1, hrx, hry, 84, 216, 64, 12, color);
                    Component s2 = ink(Component.translatable("tc.stage.complete"));
                    int m = font.width(s2);
                    g.text(font, s2, Math.round(x + 52 - m / 2.0f), y - 4, 0xFFFFFFFF, true);
                }
            }
        }
    }

    private static int parseInt(String s) { try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; } }

    private static String aspectName(String tag) { return tag.isEmpty() ? tag : Character.toUpperCase(tag.charAt(0)) + tag.substring(1).toLowerCase(Locale.ROOT); }

    private static final Map<String, List<String>> ICONS = new HashMap<>();
    /** ResearchEntry.getIcons from the original research JSON. */
    private static List<String> entryIcons(String key) {
        if (ICONS.isEmpty()) {
            for (String file : List.of("basics", "auromancy", "alchemy", "artifice", "infusion", "golemancy", "eldritch", "scans")) {
                var in = ResearchPageScreen.class.getResourceAsStream("/assets/thaumcraft/research/" + file + ".json");
                if (in == null) continue;
                try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    for (JsonElement e : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("entries")) {
                        JsonObject o = e.getAsJsonObject();
                        ICONS.put(o.get("key").getAsString(), strings(o, "icons"));
                    }
                } catch (Exception ignored) {}
            }
        }
        return ICONS.getOrDefault(key, List.of());
    }

    /** An entry icon: a texture path, or an item reference. */
    private static Object icon(String raw) {
        if (raw.contains("textures/")) {
            Identifier id = Identifier.tryParse(raw.replace("textures/items/", "textures/item/").replace("textures/blocks/", "textures/block/"));
            if (id != null && Minecraft.getInstance().getResourceManager().getResource(id).isPresent()) return id;
            return DUMMY_RESEARCH;
        }
        Identifier id = Identifier.tryParse(LegacyItemReferences.item(raw));
        if (id != null) {
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item != null && item != Items.AIR) return new ItemStack(item);
        }
        return DUMMY_RESEARCH;
    }

    /** ResearchManager.parseJsonOreList entry as display stacks (variants with metadata 32767 cycle). */
    private static List<ItemStack> requirementStacks(String ref, boolean obtain) {
        String[] parts = ref.split(";", -1);
        int amount = parts.length > 1 ? Math.max(1, parseInt(parts[1])) : 1;
        String key = obtain ? LegacyItemReferences.requirementKey(ref) : LegacyItemReferences.item(ref);
        List<ItemStack> out = new ArrayList<>();
        if (key.isEmpty() && parts.length >= 3 && parts[2].equals("32767")) {
            // wildcard over a metadata family
            for (int m = 0; m < 32; m++) {
                String v = LegacyItemReferences.item(parts[0] + ";1;" + m);
                if (v.isEmpty()) break;
                ItemStack s = make(v, amount);
                if (!s.isEmpty()) out.add(s);
            }
            return out;
        }
        ItemStack s = make(key, amount);
        if (!s.isEmpty()) out.add(s);
        return out;
    }

    private static ItemStack make(String key, int amount) {
        if (key.isEmpty()) return ItemStack.EMPTY;
        String[] d = key.split("@", 2);
        Identifier id = Identifier.tryParse(d[0]);
        if (id == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        ItemStack s = new ItemStack(item, amount);
        if (d.length == 2) s.set(ModComponents.ASPECT.get(), d[1]);
        return s;
    }

    private void drawRecipeBookmarks(GuiGraphicsExtractor g, int x, int mx, int my) {
        Random rng = new Random(rhash);
        int y = (height - paneHeight) / 2 - 8;
        allowWithPagePopup = true;
        if (!recipeOutputs.isEmpty()) {
            int space = Math.min(25, 200 / recipeOutputs.size());
            for (var e : recipeOutputs.entrySet()) {
                String rk = e.getKey();
                List<Object> list = e.getValue();
                if (list != null && !list.isEmpty()) {
                    int i = Math.floorMod(cycle, list.size());
                    if (list.get(i) == null) continue;
                    int sh = rng.nextInt(3);
                    int le = rng.nextInt(3) + (mouseInside(x + 280, y - 1, 30, 16, mx, my) ? 0 : 3);
                    int color = rk.equals(shownRecipe) ? argb(1.0f, 0.5f, 0.5f, 1.0f) : -1;
                    rect(g, TEX1, x + 280 + sh, y - 1, 120 + le, 232, 28, 16, color);
                    rect(g, TEX1, x + 280 + sh, y - 1, 116, 232, 4, 16, color);
                    if (list.get(i) instanceof ItemStack is) {
                        drawStackAt(g, is, x + 287 + sh - le, y - 1, mx, my, false, true);
                    } else if (list.get(i) instanceof Part[][][] parts) {
                        BlueprintAccess ba = blockAccessIcons.computeIfAbsent(rk, k -> new BlueprintAccess(parts, true));
                        int h = parts.length;
                        renderBluePrint(g, ba, x + 295 + sh - le, y + 6 + h, 4.0f, parts, -5000, -5000, null);
                    }
                    y += space;
                }
            }
        }
        allowWithPagePopup = false;
    }

    private void generateRecipesLists(ResearchCatalog.Stage stage, List<Addendum> addenda) {
        recipeLists.clear();
        recipeOutputs.clear();
        if (stage == null) return;
        for (String rk : stage.recipes()) addRecipesToList(BookRecipes.norm(rk), recipeLists, recipeOutputs, BookRecipes.norm(rk));
        if (addenda == null) return;
        for (Addendum addendum : addenda)
            if (!addendum.recipes().isEmpty() && knowsStrict(addendum.research()))
                for (String rk2 : addendum.recipes()) addRecipesToList(BookRecipes.norm(rk2), recipeLists, recipeOutputs, BookRecipes.norm(rk2));
    }

    private void addRecipesToList(String rk, LinkedHashMap<String, List<BookRecipes.Recipe>> lists, LinkedHashMap<String, List<Object>> outputs2, String rkey) {
        Object recipe = BookRecipes.lookup(rk);
        if (recipe == null) return;
        if (recipe instanceof List<?> group) {
            for (Object rl : group) addRecipesToList((String) rl, lists, outputs2, rk);
            return;
        }
        BookRecipes.Recipe r = (BookRecipes.Recipe) recipe;
        lists.computeIfAbsent(rkey, k -> new ArrayList<>());
        outputs2.computeIfAbsent(rkey, k -> new ArrayList<>());
        List<BookRecipes.Recipe> list = lists.get(rkey);
        List<Object> outputs = outputs2.get(rkey);
        switch (r.kind()) {
            case BLUEPRINT -> {
                if (knowsStrict(r.research())) {
                    list.add(r);
                    outputs.add(r.blueprint().display() != null ? r.blueprint().display() : r.blueprint().parts());
                }
            }
            case CRUCIBLE -> {
                ItemStack is = BookRecipes.cycle(r.json().get("catalyst"), 0);
                if (!is.isEmpty() && knowsStrict(r.research())) { list.add(r); outputs.add(r.output()); }
            }
            case INFUSION -> {
                ItemStack is = r.output();
                if (is.isEmpty()) is = BookRecipes.cycle(r.json().get("input"), 0).copy();
                if (knowsStrict(r.research())) { list.add(r); outputs.add(is); }
            }
            case ARCANE, ARCANE_SHAPELESS -> {
                ItemStack is = r.output();
                if (!is.isEmpty() && knowsStrict(r.research())) { list.add(r); outputs.add(is); }
            }
            default -> { list.add(r); outputs.add(r.output()); }
        }
    }

    private void drawRecipe(GuiGraphicsExtractor g, int mx, int my) {
        allowWithPagePopup = true;
        int x = (width - 256) / 2;
        int y = (height - 256) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, TEX4, x, y, 0, 0, 255, 255, 256, 256);
        g.nextStratum();
        List<BookRecipes.Recipe> list = recipeLists.get(shownRecipe);
        if (list == null || list.isEmpty()) list = drilldownLists.get(shownRecipe);
        if (list != null && !list.isEmpty()) {
            hasRecipePages = list.size() > 1;
            recipePageMax = list.size() - 1;
            if (recipePage > recipePageMax) recipePage = recipePageMax;
            BookRecipes.Recipe recipe = list.get(recipePage % list.size());
            if (recipe != null) {
                switch (recipe.kind()) {
                    case ARCANE, ARCANE_SHAPELESS -> drawArcaneCraftingPage(g, x + 128, y + 128, mx, my, recipe);
                    case SHAPED, SHAPELESS -> drawCraftingPage(g, x + 128, y + 128, mx, my, recipe);
                    case CRUCIBLE -> drawCruciblePage(g, x + 128, y + 128, mx, my, recipe.json());
                    case INFUSION -> drawInfusionPage(g, x + 128, y + 128, mx, my, recipe.json());
                    case BLUEPRINT -> { drawCompoundCraftingPage(g, x + 128, y + 128, mx, my, recipe.blueprint()); renderingCompound = true; }
                }
            }
            if (hasRecipePages) {
                g.nextStratum();
                float bob = (float) Math.sin(minecraft.player.tickCount / 3.0f) * 0.2f + 0.1f;
                if (recipePage > 0) rectScaled(g, x + 40, y + 232, 0, 184, 12, 8, bob);
                if (recipePage < recipePageMax) rectScaled(g, x + 204, y + 232, 12, 184, 12, 8, bob);
            }
        }
        allowWithPagePopup = false;
    }

    private void drawCompoundCraftingPage(GuiGraphicsExtractor g, int x, int y, int mx, int my, BookRecipes.Blueprint recipe) {
        if (recipe.parts() == null) return;
        if (blockAccess == null) blockAccess = new BlueprintAccess(recipe.parts(), false);
        int ySize = recipe.parts().length, xSize = recipe.parts()[0].length, zSize = recipe.parts()[0][0].length;
        Component text = ink(Component.translatable("recipe.type.construct"));
        int offset = font.width(text);
        g.text(font, text, x - offset / 2, y - 104, 0xFF505050, false);
        int s = Math.max(Math.max(xSize, zSize), ySize) * 2;
        float scale = 38 - s;
        renderBluePrint(g, blockAccess, x, y, scale, recipe.parts(), mx, my, recipe.ingredients());
        rect(g, TEX1, x + 80, y + 100, cycleMultiblockLines ? 168 : 160, 224, 8, 8, argb(1, 1, 1, mouseInside(x + 80, y + 100, 8, 8, mx, my) ? 1.0f : 0.75f));
    }

    /**
     * renderBluePrint: the structure's real block models (fluids, block entities without a model) rendered off-screen
     * with the original transform (rotX 25, rotY -45, scale 38 - 2 * size), then the ingredient row.
     */
    private void renderBluePrint(GuiGraphicsExtractor g, BlueprintAccess ba, int x, int y, float scale, Part[][][] blueprint, int mx, int my, List<ItemStack> ingredients) {
        int ySize = blueprint.length, xSize = blueprint[0].length, zSize = blueprint[0][0].length;
        float transX = x - xSize / 2;
        float transY = y - (float) Math.sqrt(ySize * ySize + xSize * xSize + zSize * zSize) / 2.0f;
        List<BlueprintRenderState.Block> blocks = new ArrayList<>();
        for (int h = 0; h < ySize; ++h) for (int l = 0; l < xSize; ++l) for (int w = 0; w < zSize; ++w) {
            BlockState state = ba.getBlockState(l, h, w);
            if (state == null || state.isAir()) continue;
            blocks.add(new BlueprintRenderState.Block(new net.minecraft.core.BlockPos(l, h, w), state));
        }
        g.submitPictureInPictureRenderState(new BlueprintRenderState(blocks, xSize, ySize, zSize, transX, transY, scale, rotX, rotY,
                x - 128, y - 128, x + 128, y + 128, g.peekScissorStack()));
        if (ingredients != null) {
            g.nextStratum();
            for (int a = 0; a < ingredients.size(); ++a)
                if (ingredients.get(a) != null && !ingredients.get(a).isEmpty())
                    drawStackAt(g, ingredients.get(a), x - 85 + a * 17, y + 90, mx, my, true);
        }
    }

    private static ItemStack blockItem(BlockState state) {
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) {
            if (state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) return new ItemStack(Items.LAVA_BUCKET);
            if (state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) return new ItemStack(Items.WATER_BUCKET);
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    private void drawAspectsInsert(GuiGraphicsExtractor g, int mx, int my) {
        allowWithPagePopup = true;
        int x = (width - 256) / 2;
        int y = (height - 256) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, TEX4, x, y, 0, 0, 255, 255, 256, 256);
        g.nextStratum();
        drawAspectPage(g, x + 60, y + 24, mx, my);
        allowWithPagePopup = false;
    }

    private void drawAspectPage(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        if (knownPlayerAspects.isEmpty()) return;
        int count = -1;
        int start = aspectsPage * 5;
        List<String> sorted = new ArrayList<>(knownPlayerAspects);
        sorted.sort(String::compareTo);
        for (String aspect : sorted) {
            if (++count < start) continue;
            if (count > start + 4) break;
            int tx = x, ty = y + count % 5 * 40;
            if (mx >= tx && my >= ty && mx < tx + 40 && my < ty + 40) {
                g.pose().pushMatrix();
                g.pose().translate(x - 2, y + count % 5 * 40 - 2);
                g.pose().scale(2.0f, 2.0f);
                full(g, TEX3, 0, 0, 16, argb(1, 1, 1, 0.5f));
                g.pose().popMatrix();
            }
            g.pose().pushMatrix();
            g.pose().translate(x + 2, y + 2 + count % 5 * 40);
            g.pose().scale(1.5f, 1.5f);
            ThaumcraftTheme.drawTag(g, font, aspect, 0, 0, 0);
            g.pose().popMatrix();
            halfText(g, aspectName(aspect), x + 16, y + 29 + count % 5 * 40, 0xFF505050);
            Aspect as = AspectRegistry.get(aspect);
            if (as != null && !as.isPrimal()) {
                Aspect[] comps = {as.parentA(), as.parentB()};
                for (int c = 0; c < 2; c++) {
                    g.pose().pushMatrix();
                    g.pose().translate(x + (c == 0 ? 60 : 102), y + 4 + count % 5 * 40);
                    g.pose().scale(1.25f, 1.25f);
                    if (knowledge.has("!" + comps[c].id().toLowerCase(Locale.ROOT))) ThaumcraftTheme.drawTag(g, font, comps[c].id(), 0, 0, 0);
                    else full(g, DUMMY_RESEARCH, 0, 0, 16, argb(0.8f, 0.8f, 0.8f, 1.0f));
                    g.pose().popMatrix();
                }
                for (int c = 0; c < 2; c++)
                    if (knowledge.has("!" + comps[c].id().toLowerCase(Locale.ROOT)))
                        halfText(g, aspectName(comps[c].id()), x + 22 + (c == 0 ? 50 : 92), y + 29 + count % 5 * 40, 0xFF505050);
                g.text(font, ink("="), x + 9 + 32, y + 12 + count % 5 * 40, 0xFF999999, false);
                g.text(font, ink("+"), x + 10 + 79, y + 12 + count % 5 * 40, 0xFF999999, false);
            } else {
                g.text(font, ink(Component.translatable("tc.aspect.primal")), x + 54, y + 12 + count % 5 * 40, 0xFF777777, false);
            }
        }
        g.nextStratum();
        float bob = (float) Math.sin(minecraft.player.tickCount / 3.0f) * 0.2f + 0.1f;
        if (aspectsPage > 0) rectScaled(g, x - 20, y + 208, 0, 184, 12, 8, bob);
        if (aspectsPage < maxAspectPages - 1) rectScaled(g, x + 144, y + 208, 12, 184, 12, 8, bob);
    }

    private void halfText(GuiGraphicsExtractor g, String text, int x, int y, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.5f, 0.5f);
        int offset = font.width(text) / 2;
        g.text(font, ink(text), -offset, 0, color, false);
        g.pose().popMatrix();
    }

    private void drawArcaneCraftingPage(GuiGraphicsExtractor g, int x, int y, int mx, int my, BookRecipes.Recipe recipe) {
        JsonObject json = recipe.json();
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(2.0f, 2.0f);
        rect(g, TEX2, -26, -26, 112, 15, 52, 52, -1);
        rect(g, TEX2, -8, -46, 20, 3, 16, 16, -1);
        rect(g, TEX2, -6, 40, 68, 76, 12, 12, argb(1, 1, 1, 0.4f));
        g.pose().popMatrix();
        String text = "" + (json.has("vis") ? json.get("vis").getAsInt() : 0);
        int offset = font.width(text);
        g.text(font, ink(text), x - offset / 2, y + 90, 0xFF505050, false);
        drawPopupAt(x - offset / 2 - 15, y + 75, 30, 30, mx, my, "wandtable.text1");
        g.nextStratum();
        drawStackAt(g, recipe.output(), x - 8, y - 84, mx, my, false);
        if (json.has("crystals")) {
            var crystals = json.getAsJsonObject("crystals");
            int a = 0, sz = crystals.size();
            for (var e : crystals.entrySet()) {
                ItemStack c = art.arcane.thaumcraft.item.CrystalEssenceItem.of(e.getKey(), e.getValue().getAsInt());
                drawStackAt(g, c, x + 4 - sz * 10 + a * 20, y + 59, mx, my, true);
                ++a;
            }
        }
        if (recipe.kind() == BookRecipes.Kind.ARCANE) {
            Component t = ink(Component.translatable("recipe.type.arcane"));
            g.text(font, t, x - font.width(t) / 2, y - 104, 0xFF505050, false);
            drawShaped(g, json, x, y, mx, my);
        } else {
            Component t = ink(Component.translatable("recipe.type.arcane.shapeless"));
            g.text(font, t, x - font.width(t) / 2, y - 104, 0xFF505050, false);
            drawShapeless(g, json, x, y, mx, my);
        }
    }

    private void drawShaped(GuiGraphicsExtractor g, JsonObject json, int x, int y, int mx, int my) {
        var pattern = json.getAsJsonArray("pattern");
        var key = json.getAsJsonObject("key");
        int rh = pattern.size(), rw = 0;
        for (var row : pattern) rw = Math.max(rw, row.getAsString().length());
        for (int i = 0; i < rw && i < 3; ++i) for (int j = 0; j < rh && j < 3; ++j) {
            String line = pattern.get(j).getAsString();
            if (i >= line.length() || line.charAt(i) == ' ') continue;
            JsonElement ing = key.get(String.valueOf(line.charAt(i)));
            if (ing != null) drawStackAt(g, BookRecipes.cycle(ing, i + j * rw), x - 40 + i * 32, y - 40 + j * 32, mx, my, true);
        }
    }

    private void drawShapeless(GuiGraphicsExtractor g, JsonObject json, int x, int y, int mx, int my) {
        var items = json.getAsJsonArray("ingredients");
        for (int k = 0; k < items.size() && k < 9; ++k)
            drawStackAt(g, BookRecipes.cycle(items.get(k), k), x - 40 + k % 3 * 32, y - 40 + k / 3 * 32, mx, my, true);
    }

    private void drawCraftingPage(GuiGraphicsExtractor g, int x, int y, int mx, int my, BookRecipes.Recipe recipe) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(2.0f, 2.0f);
        rect(g, TEX2, -26, -26, 60, 15, 51, 52, -1);
        rect(g, TEX2, -8, -46, 20, 3, 16, 16, -1);
        g.pose().popMatrix();
        g.nextStratum();
        drawStackAt(g, recipe.output(), x - 8, y - 84, mx, my, false);
        if (recipe.kind() == BookRecipes.Kind.SHAPED) {
            Component t = ink(Component.translatable("recipe.type.workbench"));
            g.text(font, t, x - font.width(t) / 2, y - 104, 0xFF505050, false);
            drawShaped(g, recipe.json(), x, y, mx, my);
        } else {
            Component t = ink(Component.translatable("recipe.type.workbenchshapeless"));
            g.text(font, t, x - font.width(t) / 2, y - 104, 0xFF505050, false);
            drawShapeless(g, recipe.json(), x, y, mx, my);
        }
    }

    /** AspectList.getAspectsSortedByName: sorted by tag. */
    private static List<Map.Entry<String, Integer>> sortedAspects(JsonObject aspects) {
        List<Map.Entry<String, Integer>> l = new ArrayList<>();
        if (aspects != null) for (var e : aspects.entrySet()) l.add(Map.entry(e.getKey(), e.getValue().getAsInt()));
        l.sort(Map.Entry.comparingByKey());
        return l;
    }

    private void aspectTip(String tag) {
        tipText = List.of(Component.literal(aspectName(tag)), Component.translatable("tc.aspect." + tag));
        tipImage = Optional.empty();
    }

    private void drawCruciblePage(GuiGraphicsExtractor g, int x, int y, int mx, int my, JsonObject rc) {
        Component text = ink(Component.translatable("recipe.type.crucible"));
        g.text(font, text, x - font.width(text) / 2, y - 104, 0xFF505050, false);
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(2.0f, 2.0f);
        rect(g, TEX2, -28, -29, 0, 3, 56, 17, -1);
        g.pose().translate(0.0f, 32.0f);
        rect(g, TEX2, -28, -44, 0, 20, 56, 48, -1);
        g.pose().translate(0.0f, -8.0f);
        rect(g, TEX2, -25, -50, 100, 84, 11, 13, -1);
        g.pose().popMatrix();
        g.nextStratum();
        var aspects = sortedAspects(rc.getAsJsonObject("aspects"));
        int total = 0;
        int rows = (aspects.size() - 1) / 3;
        int shift = (3 - aspects.size() % 3) * 10;
        int sx = x - 28, sy = y + 8 - 10 * rows;
        for (var tag : aspects) {
            int m = total / 3 >= rows && (rows > 1 || aspects.size() < 3) ? 1 : 0;
            int vx = sx + total % 3 * 20 + shift * m, vy = sy + total / 3 * 20;
            ThaumcraftTheme.drawTag(g, font, tag.getKey(), vx, vy, tag.getValue());
            ++total;
        }
        ItemStack out = BookRecipes.stack(rc.getAsJsonObject("result"));
        drawStackAt(g, out, x - 8, y - 50, mx, my, false);
        ItemStack catalyst = BookRecipes.cycle(rc.get("catalyst"), 0);
        if (rc.has("catalyst_aspect") && !catalyst.isEmpty()) { catalyst = catalyst.copy(); catalyst.set(ModComponents.ASPECT.get(), rc.get("catalyst_aspect").getAsString()); }
        drawStackAt(g, catalyst, x - 64, y - 56, mx, my, true);
        total = 0;
        for (var tag : aspects) {
            int m = total / 3 >= rows && (rows > 1 || aspects.size() < 3) ? 1 : 0;
            int vx = sx + total % 3 * 20 + shift * m, vy = sy + total / 3 * 20;
            if (mx >= vx && my >= vy && mx < vx + 16 && my < vy + 16) aspectTip(tag.getKey());
            ++total;
        }
    }

    private void drawInfusionPage(GuiGraphicsExtractor g, int x, int y, int mx, int my, JsonObject ri) {
        var aspects = sortedAspects(ri.getAsJsonObject("aspects"));
        Component text = ink(Component.translatable("recipe.type.infusion"));
        g.text(font, text, x - font.width(text) / 2, y - 104, 0xFF505050, false);
        g.pose().pushMatrix();
        g.pose().translate(x, y + 20);
        g.pose().scale(2.0f, 2.0f);
        rect(g, TEX2, -28, -56, 0, 3, 56, 17, -1);
        g.pose().translate(0.0f, 19.0f);
        rect(g, TEX2, -28, -55, 200, 77, 60, 44, -1);
        g.pose().popMatrix();
        g.nextStratum();
        int total = 0;
        int rows = (aspects.size() - 1) / 5;
        int shift = (5 - aspects.size() % 5) * 10;
        int sx = x - 48, sy = y + 50 - 10 * rows;
        for (var tag : aspects) {
            int m = total / 5 >= rows && (rows > 1 || aspects.size() < 5) ? 1 : 0;
            ThaumcraftTheme.drawTag(g, font, tag.getKey(), sx + total % 5 * 20 + shift * m, sy + total / 5 * 20, tag.getValue());
            ++total;
        }
        ItemStack idisp = BookRecipes.stack(ri.getAsJsonObject("result"));
        if (idisp.isEmpty()) idisp = BookRecipes.cycle(ri.get("input"), 0).copy();
        drawStackAt(g, idisp, x - 8, y - 85, mx, my, false);
        ItemStack rinp = BookRecipes.cycle(ri.get("input"), 0).copy();
        drawStackAt(g, rinp, x - 8, y - 16, mx, my, true);
        var components = ri.has("components") ? ri.getAsJsonArray("components") : new com.google.gson.JsonArray();
        int le = components.size();
        List<int[]> coords = new ArrayList<>();
        float pieSlice = le == 0 ? 0 : (float) (360 / le);
        float currentRot = -90.0f;
        for (int a = 0; a < le; ++a) {
            int xx = (int) (Math.cos(currentRot / 180.0f * 3.1415927f) * 40.0f) - 8;
            int yy = (int) (Math.sin(currentRot / 180.0f * 3.1415927f) * 40.0f) - 8;
            currentRot += pieSlice;
            coords.add(new int[]{xx, yy});
        }
        total = 0;
        sx = x;
        sy = y - 8;
        for (JsonElement ingredient : components) {
            ItemStack is = BookRecipes.cycle(ingredient, 0);
            drawStackAt(g, is.copyWithCount(1), sx + coords.get(total)[0], sy + coords.get(total)[1], mx, my, true);
            ++total;
        }
        int inst = Math.min(5, (ri.has("instability") ? ri.get("instability").getAsInt() : 0) / 2);
        String t2 = Component.translatable("tc.inst").getString() + " " + Component.translatable("tc.inst." + inst).getString();
        g.text(font, ink(t2), x - font.width(t2) / 2, y + 94, 0xFF505050, false);
        total = 0;
        for (var tag : aspects) {
            int i = total / 5 >= rows && (rows > 1 || aspects.size() < 5) ? 1 : 0;
            int vx3 = sx - 48 + total % 5 * 20 + shift * i, vy3 = y + 50 - 10 * rows + total / 5 * 20;
            if (mx >= vx3 && my >= vy3 && mx < vx3 + 16 && my < vy3 + 16) aspectTip(tag.getKey());
            ++total;
        }
    }

    // ============================================================== input

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (minecraft.options.keyInventory.matches(event) || key == 256) {
            history.clear();
            if (shownRecipe != null || showingAspects || showingKnowledge) {
                shownRecipe = null;
                blockAccess = null;
                showingAspects = false;
                showingKnowledge = false;
                playSound("page", 0.4f, 1.1f);
            } else {
                minecraft.gui.setScreen(parent);
            }
            return true;
        }
        if (key == 263 || key == 265 || key == 266) { prevPage(); return true; }
        if (key == 262 || key == 264 || key == 267) { nextPage(); return true; }
        if (key == 259) { goBack(); return true; }
        return super.keyPressed(event);
    }

    @Override public void onClose() { minecraft.gui.setScreen(parent); }

    private void nextPage() {
        if (page < maxPages - 2) {
            page += 2;
            lastCycle = 0L;
            cycle = -1;
            playSound("page", 0.66f, 1.0f);
        }
    }

    private void prevPage() {
        if (page >= 2) {
            page -= 2;
            lastCycle = 0L;
            cycle = -1;
            playSound("page", 0.66f, 1.0f);
        }
    }

    private void goBack() {
        if (!history.isEmpty()) {
            playSound("page", 0.66f, 1.0f);
            shownRecipe = history.pop();
            blockAccess = null;
        } else {
            shownRecipe = null;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int par1 = (int) event.x(), par2 = (int) event.y();
        checkRequisites();
        int var4 = (width - paneWidth) / 2;
        int var5 = (height - paneHeight) / 2;
        int mx = par1 - hrx;
        int my = par2 - hry;
        if (shownRecipe == null && !hold && hasAllRequisites && !isComplete && page == 0 && mx >= 0 && my >= 0 && mx < 64 && my < 12) {
            ClientPacketDistributor.sendToServer(new AdvanceResearchPayload(research.key(), knowledge.stage(research.key())));
            playSound("write", 0.66f, 1.0f);
            lastCheck = 0L;
            lastStage = currentStage;
            hold = true;
            BookRecipes.clearKeyCache();
            drilldownLists.clear();
        }
        if (isResearchComplete("FIRSTSTEPS")) {
            mx = par1 - (var4 - 48);
            my = par2 - (var5 + 8);
            if (mx >= 0 && my >= 0 && mx < 25 && my < 16) {
                shownRecipe = null;
                showingKnowledge = false;
                showingAspects = !showingAspects;
                blockAccess = null;
                history.clear();
                if (aspectsPage > maxAspectPages) aspectsPage = 0;
                playSound("page", 0.7f, 0.9f);
            }
        }
        if (isResearchComplete("KNOWLEDGETYPES") && !research.key().equals("KNOWLEDGETYPES")) {
            mx = par1 - (var4 - 48);
            my = par2 - (var5 + 31);
            if (mx >= 0 && my >= 0 && mx < 25 && my < 16) {
                shownRecipe = null;
                showingAspects = false;
                showingKnowledge = !showingKnowledge;
                blockAccess = null;
                history.clear();
                playSound("page", 0.7f, 0.9f);
            }
        }
        mx = par1 - (var4 + 205);
        my = par2 - (var5 + 192);
        if (showingAspects && aspectsPage < maxAspectPages - 1 && mx >= 0 && my >= 0 && mx < 14 && my < 14) {
            ++aspectsPage;
            playSound("page", 0.7f, 0.9f);
        }
        mx = par1 - (var4 + 38);
        my = par2 - (var5 + 192);
        if (showingAspects && aspectsPage > 0 && mx >= 0 && my >= 0 && mx < 14 && my < 14) {
            --aspectsPage;
            playSound("page", 0.7f, 0.9f);
        }
        if (!recipeLists.isEmpty()) {
            int aa = 0;
            int space = Math.min(25, 200 / recipeLists.size());
            for (String rk : recipeLists.keySet()) {
                mx = par1 - (var4 + 280);
                my = par2 - (var5 - 8 + aa * space);
                if (mx >= 0 && my >= 0 && mx < 30 && my < 16) {
                    shownRecipe = rk.equals(shownRecipe) ? null : rk;
                    showingAspects = false;
                    showingKnowledge = false;
                    blockAccess = null;
                    history.clear();
                    playSound("page", 0.7f, 0.9f);
                    break;
                }
                ++aa;
            }
        }
        mx = par1 - (var4 + 205);
        my = par2 - (var5 + 192);
        if (hasRecipePages && recipePage < recipePageMax && mx >= 0 && my >= 0 && mx < 14 && my < 14) {
            ++recipePage;
            playSound("page", 0.7f, 0.9f);
        }
        mx = par1 - (var4 + 38);
        my = par2 - (var5 + 192);
        if (hasRecipePages && recipePage > 0 && mx >= 0 && my >= 0 && mx < 14 && my < 14) {
            --recipePage;
            playSound("page", 0.7f, 0.9f);
        }
        mx = par1 - (var4 + 261);
        my = par2 - (var5 + 189);
        if (shownRecipe == null && mx >= 0 && my >= 0 && mx < 14 && my < 10) nextPage();
        mx = par1 - (var4 - 17);
        my = par2 - (var5 + 189);
        if (shownRecipe == null && mx >= 0 && my >= 0 && mx < 14 && my < 10) prevPage();
        mx = par1 - (var4 + 118);
        my = par2 - (var5 + 190);
        if (mx >= 0 && my >= 0 && mx < 20 && my < 12) goBack();
        mx = par1 - (var4 + 210);
        my = par2 - (var5 + 190);
        if (renderingCompound && mx >= 0 && my >= 0 && mx < 10 && my < 10) {
            playSound("clack", 0.66f, 1.0f);
            cycleMultiblockLines = !cycleMultiblockLines;
        }
        for (Object[] coords : reference) {
            int rx = (int) coords[0], ry = (int) coords[1];
            if (par1 >= rx && par2 >= ry && par1 < rx + 16 && par2 < ry + 16) {
                playSound("page", 0.66f, 1.0f);
                if (shownRecipe != null) history.push(shownRecipe);
                shownRecipe = (String) coords[2];
                recipePage = parseInt((String) coords[3]);
                if (!drilldownLists.containsKey(shownRecipe))
                    addRecipesToList(shownRecipe, drilldownLists, new LinkedHashMap<>(), shownRecipe);
                blockAccess = null;
                break;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override public boolean isPauseScreen() { return false; }

    // ============================================================== helpers

    private void drawPopupAt(int x, int y, int mx, int my, String text) { drawPopupAt(x, y, 16, 16, mx, my, text); }

    private void drawPopupAt(int x, int y, int w, int h, int mx, int my, String text) {
        if ((shownRecipe == null || allowWithPagePopup) && mx >= x && my >= y && mx < x + w && my < y + h) {
            tipText = List.of(Component.translatable(text));
            tipImage = Optional.empty();
        }
    }

    private static boolean mouseInside(int x, int y, int w, int h, int mx, int my) { return mx >= x && my >= y && mx < x + w && my < y + h; }

    private void drawStackAt(GuiGraphicsExtractor g, ItemStack itemstack, int x, int y, int mx, int my, boolean clickthrough) {
        drawStackAt(g, itemstack, x, y, mx, my, clickthrough, false);
    }

    /** UtilsFX.renderItemStack + full vanilla tooltip, with recipe.clickthrough. */
    private void drawStackAt(GuiGraphicsExtractor g, ItemStack itemstack, int x, int y, int mx, int my, boolean clickthrough, boolean hideOverlay) {
        if (itemstack == null || itemstack.isEmpty()) return;
        g.item(itemstack, x, y);
        if (!hideOverlay) g.itemDecorations(font, itemstack, x, y);
        if ((shownRecipe == null || allowWithPagePopup) && mx >= x && my >= y && mx < x + 16 && my < y + 16) {
            List<Component> addtext = new ArrayList<>(Screen.getTooltipFromItem(minecraft, itemstack));
            if (clickthrough) {
                String ref = BookRecipes.craftingRecipeKey(itemstack);
                if (ref != null) {
                    String[] sr = ref.split(";", 2);
                    if (sr.length > 1) {
                        if (sr[0].endsWith(":UNKNOWN")) {
                            addtext.add(Component.translatable("recipe.unknown").withStyle(net.minecraft.ChatFormatting.DARK_RED, net.minecraft.ChatFormatting.ITALIC));
                        } else {
                            addtext.add(Component.translatable("recipe.clickthrough").withStyle(net.minecraft.ChatFormatting.BLUE, net.minecraft.ChatFormatting.ITALIC));
                            reference.add(new Object[]{mx, my, BookRecipes.norm(sr[0]), sr[1]});
                        }
                    }
                }
            }
            tipText = addtext;
            tipImage = itemstack.getTooltipImage();
        }
    }

    /** drawTexturedModalRect: u/v in the original 256x256 texture space (textures may be HD). */
    private static void rect(GuiGraphicsExtractor g, Identifier tex, int x, int y, int u, int v, int w, int h, int color) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, u, v, w, h, 256, 256, color);
    }

    /** UtilsFX.drawTexturedQuadFull. */
    private static void full(GuiGraphicsExtractor g, Identifier tex, int x, int y, int size, int color) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0, 0, size, size, 1, 1, 1, 1, color);
    }

    private static void rectScaled(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h, float scale) {
        g.pose().pushMatrix();
        g.pose().translate(x + w / 2.0f, y + h / 2.0f);
        g.pose().scale(1.0f + scale, 1.0f + scale);
        g.blit(RenderPipelines.GUI_TEXTURED, TEX1, -w / 2, -h / 2, u, v, w, h, 256, 256);
        g.pose().popMatrix();
    }

    private static int argb(float r, float gr, float b, float a) {
        return ((int) (a * 255) & 255) << 24 | ((int) (r * 255) & 255) << 16 | ((int) (gr * 255) & 255) << 8 | ((int) (b * 255) & 255);
    }

    private static Component ink(String s) { return ThaumcraftTheme.ink(s); }
    private static Component ink(Component c) { return ThaumcraftTheme.ink(c); }

    // ============================================================== text layout

    /** One wrapped line: raw text (with legacy codes and the ~B marker) and its starting style. */
    record Line(String raw, Style style) {
        Component draw() { return ThaumcraftTheme.ink(Component.literal(raw.replace("~B", "")).withStyle(style)); }
    }

    private void parsePages() {
        checkRequisites();
        pages.clear();
        if (research == null || research.stages().isEmpty()) return;
        boolean complete = false;
        currentStage = knowledge.stage(research.key()) - 1;
        while (currentStage >= research.stages().size()) { --currentStage; complete = true; }
        if (currentStage < 0) currentStage = 0;
        ResearchCatalog.Stage stage = research.stages().get(currentStage);
        List<Addendum> addenda = null;
        if (!addendaAll.isEmpty() && complete) addenda = addendaAll;
        generateRecipesLists(stage, addenda);
        String rawText = Component.translatable(stage.text()).getString();
        if (addenda != null) {
            int ac = 0;
            for (Addendum addendum : addenda) {
                if (knowsStrict(addendum.research())) {
                    ++ac;
                    String text = Component.translatable("tc.addendumtext", ac).getString();
                    rawText = rawText + "<PAGE>" + text + "<BR>" + Component.translatable(addendum.text()).getString();
                }
            }
        }
        rawText = rawText.replace("<BR>", "~B\n\n").replace("<BR/>", "~B\n\n")
                .replace("<LINE>", "~L").replace("<LINE/>", "~L")
                .replace("<DIV>", "~D").replace("<DIV/>", "~D")
                .replace("<PAGE>", "~P").replace("<PAGE/>", "~P");
        List<PageImage> images = new ArrayList<>();
        for (String s : rawText.split("<IMG>")) {
            int i = s.indexOf("</IMG>");
            if (i >= 0) {
                String clean = s.substring(0, i);
                PageImage pi = PageImage.parse(clean);
                int at = rawText.indexOf(clean);
                if (pi == null) rawText = rawText.substring(0, at) + "\n" + rawText.substring(at + clean.length());
                else { images.add(pi); rawText = rawText.substring(0, at) + "~I" + rawText.substring(at + clean.length()); }
            }
        }
        rawText = rawText.replace("<IMG>", "").replace("</IMG>", "");
        List<String> firstPassText = new ArrayList<>();
        String[] temp = rawText.split("~P");
        for (int a = 0; a < temp.length; ++a) {
            String[] temp2 = temp[a].split("~D");
            for (int x = 0; x < temp2.length; ++x) {
                String[] temp3 = temp2[x].split("~L");
                for (int b = 0; b < temp3.length; ++b) {
                    String[] temp4 = temp3[b].split("~I");
                    for (int c = 0; c < temp4.length; ++c) {
                        firstPassText.add(temp4[c]);
                        if (c != temp4.length - 1) firstPassText.add("~I");
                    }
                    if (b != temp3.length - 1) firstPassText.add("~L");
                }
                if (x != temp2.length - 1) firstPassText.add("~D");
            }
            if (a != temp.length - 1) firstPassText.add("~P");
        }
        List<Line> parsedText = new ArrayList<>();
        for (String s2 : firstPassText) {
            if (s2.isEmpty()) continue;
            for (FormattedText ft : font.getSplitter().splitLines(s2, 140, Style.EMPTY)) {
                StringBuilder sb = new StringBuilder();
                Style[] st = {null};
                ft.visit((style, str) -> { if (st[0] == null) st[0] = style; sb.append(str); return Optional.empty(); }, Style.EMPTY);
                parsedText.add(new Line(sb.toString(), st[0] == null ? Style.EMPTY : st[0]));
            }
        }
        int lineHeight = font.lineHeight;
        int heightRemaining = 182;
        int dividerSpace = 0;
        if (research.key().equals("KNOWLEDGETYPES")) {
            heightRemaining -= 2;
            int tc = 0;
            for (int type = 0; type < 2; type++)
                for (String category : ResearchCatalog.CATEGORIES)
                    if (getKnowledgeRaw(type, category) > 0) { ++tc; break; }
            heightRemaining -= 20 * tc;
            dividerSpace = 12;
        }
        if (!isComplete) {
            for (String t : List.of("required_craft", "required_item", "required_knowledge", "required_research"))
                if (stage.requirements().containsKey(t)) { heightRemaining -= 18; dividerSpace = 15; }
        }
        heightRemaining -= dividerSpace;
        List<Object> page1 = new ArrayList<>();
        List<PageImage> tempImages = new ArrayList<>();
        for (Line l : parsedText) {
            String line = l.raw();
            Line add = l;
            if (line.contains("~I")) { if (!images.isEmpty()) tempImages.add(images.remove(0)); line = ""; }
            if (line.contains("~L")) { tempImages.add(PILINE); line = ""; }
            if (line.contains("~D")) { tempImages.add(PIDIV); line = ""; }
            if (line.contains("~P")) { heightRemaining = 210; pages.add(new ArrayList<>(page1)); page1 = new ArrayList<>(); line = ""; }
            if (!line.isEmpty()) {
                line = line.trim();
                add = new Line(line, l.style());
                page1.add(add);
                heightRemaining -= lineHeight;
                if (line.endsWith("~B")) heightRemaining -= (int) (lineHeight * 0.66);
            }
            while (!tempImages.isEmpty() && heightRemaining >= tempImages.get(0).ah + 2) {
                heightRemaining -= tempImages.get(0).ah + 2;
                page1.add(tempImages.remove(0));
            }
            if (heightRemaining < lineHeight && !page1.isEmpty()) {
                heightRemaining = 210;
                pages.add(new ArrayList<>(page1));
                page1 = new ArrayList<>();
            }
        }
        if (!page1.isEmpty()) pages.add(new ArrayList<>(page1));
        page1 = new ArrayList<>();
        heightRemaining = 210;
        while (!tempImages.isEmpty()) {
            if (heightRemaining < tempImages.get(0).ah + 2) {
                heightRemaining = 210;
                pages.add(new ArrayList<>(page1));
                page1 = new ArrayList<>();
            } else {
                heightRemaining -= tempImages.get(0).ah + 2;
                page1.add(tempImages.remove(0));
            }
        }
        if (!page1.isEmpty()) pages.add(new ArrayList<>(page1));
        rhash = research.key().hashCode() + currentStage * 50;
        maxPages = pages.size();
    }

    private void checkRequisites() {
        if (research == null || research.stages().isEmpty()) return;
        isComplete = isResearchComplete(research.key());
        while (currentStage >= research.stages().size()) --currentStage;
        if (currentStage < 0) return;
        hasAllRequisites = true;
        hasItem = null;
        hasCraft = null;
        hasResearch = null;
        hasKnow = null;
        ResearchCatalog.Stage stage = research.stages().get(currentStage);
        List<String> o = stage.requirements().get("required_item");
        if (o != null) {
            hasItem = new boolean[o.size()];
            for (int a = 0; a < o.size(); ++a) {
                String key = LegacyItemReferences.requirementKey(o.get(a));
                String[] parts = o.get(a).split(";", -1);
                int amount = parts.length > 1 ? Math.max(1, parseInt(parts[1])) : 1;
                var player = Minecraft.getInstance().player;
                boolean has = !key.isEmpty() && player != null && ResearchItemCosts.has(player.getInventory(), Map.of(key, amount));
                if (!(hasItem[a] = has)) hasAllRequisites = false;
            }
        }
        List<String> c = stage.requirements().get("required_craft");
        if (c != null) {
            hasCraft = new boolean[c.size()];
            for (int a2 = 0; a2 < c.size(); ++a2) {
                if (!LegacyItemReferences.crafted(knowledge, c.get(a2))) { hasAllRequisites = false; hasCraft[a2] = false; }
                else hasCraft[a2] = true;
            }
        }
        List<String> r = stage.requirements().get("required_research");
        if (r != null) {
            hasResearch = new boolean[r.size()];
            for (int a3 = 0; a3 < r.size(); ++a3) {
                if (!knowsStrict(r.get(a3).split(";")[0])) { hasAllRequisites = false; hasResearch[a3] = false; }
                else hasResearch[a3] = true;
            }
        }
        List<String> k = stage.requirements().get("required_knowledge");
        if (k != null) {
            hasKnow = new boolean[k.size()];
            for (int a4 = 0; a4 < k.size(); ++a4) {
                String[] kn = k.get(a4).split(";");
                int type = kn[0].equals("THEORY") ? 0 : 1;
                int pk = kn.length > 1 ? getKnowledge(type, kn[1]) : 0;
                if (pk < (kn.length > 2 ? parseInt(kn[2]) : 0)) { hasAllRequisites = false; hasKnow[a4] = false; }
                else hasKnow[a4] = true;
            }
        }
        for (String type : stage.requirements().keySet())
            if (!List.of("required_item", "required_craft", "required_research", "required_knowledge").contains(type)) hasAllRequisites = false;
    }

    // ============================================================== page images / blueprints

    static final class PageImage {
        int x, y, w, h, aw, ah;
        float scale;
        Identifier loc;

        static PageImage parse(String text) {
            String[] s = text.split(":");
            if (s.length != 7) return null;
            try {
                PageImage pi = new PageImage();
                pi.loc = Identifier.fromNamespaceAndPath(s[0], s[1].replace("textures/items/", "textures/item/").replace("textures/blocks/", "textures/block/"));
                pi.x = Integer.parseInt(s[2]);
                pi.y = Integer.parseInt(s[3]);
                pi.w = Integer.parseInt(s[4]);
                pi.h = Integer.parseInt(s[5]);
                pi.scale = Float.parseFloat(s[6]);
                pi.aw = (int) (pi.w * pi.scale);
                pi.ah = (int) (pi.h * pi.scale);
                if (pi.ah > 208 || pi.aw > 140) return null;
                return pi;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /** GuiResearchPage.BlueprintBlockAccess. */
    static final class BlueprintAccess {
        private final BlockState[][][] structure;
        int sliceLine = 0;

        BlueprintAccess(Part[][][] src, boolean target) {
            Part[][][] data = new Part[src.length][][];
            for (int y = 0; y < src.length; ++y) {
                data[y] = new Part[src[y].length][];
                for (int x = 0; x < src[y].length; ++x) data[y][x] = Arrays.copyOf(src[y][x], src[y][x].length);
            }
            if (target) for (int y = 0; y < data.length; ++y) data[y] = rotateRight(data[y], 3);
            structure = new BlockState[data.length][data[0].length][data[0][0].length];
            for (int y = 0; y < data.length; ++y)
                for (int x = 0; x < data[0].length && x < data[y].length; ++x)
                    for (int z = 0; z < data[0][0].length && z < data[y][x].length; ++z)
                        structure[data.length - y - 1][x][z] = target ? convertTarget(data[y][x][z]) : convert(data[y][x][z]);
        }

        private static Part[][] rotateRight(Part[][] m, int times) {
            for (int a = 0; a < times; a++) {
                int rows = m.length, cols = m[0].length;
                Part[][] n = new Part[cols][rows];
                for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) n[j][rows - i - 1] = m[i][j];
                m = n;
            }
            return m;
        }

        private static BlockState state(Object o) {
            if (o instanceof Block b) return b.defaultBlockState();
            if (o instanceof BlockState s) return s;
            if (o instanceof java.util.function.Predicate<?>) return net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState();
            return null;
        }

        private static BlockState convert(Part p) { return p == null || p.getSource() == null ? null : state(p.getSource()); }

        private static BlockState convertTarget(Part p) {
            if (p == null) return null;
            if (p.getTarget() == null) return convert(p);
            return state(p.getTarget());
        }

        BlockState getBlockState(int x, int y, int z) {
            if (sliceLine > structure.length) sliceLine = 0;
            if (y >= 0 && y < structure.length - sliceLine && x >= 0 && x < structure[y].length && z >= 0 && z < structure[y][x].length)
                return structure[y][x][z];
            return null;
        }
    }
}
