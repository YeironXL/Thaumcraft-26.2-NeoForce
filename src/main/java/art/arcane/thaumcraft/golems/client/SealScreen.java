package art.arcane.thaumcraft.golems.client;

import art.arcane.thaumcraft.golems.GolemTrait;
import art.arcane.thaumcraft.golems.seals.ISealConfigFilter;
import art.arcane.thaumcraft.golems.seals.ISealConfigToggles;
import art.arcane.thaumcraft.golems.seals.SealMenu;
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;

/** TC6 SealBaseGUI: category buttons on a ring, then the page of the selected category. */
public final class SealScreen extends AbstractContainerScreen<SealMenu> {
    private static final Identifier TEX = Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/gui_base.png");
    private final int middleX = 88, middleY = (232 - 72) / 2 - 8;
    private record Btn(int id, int x, int y, int w, int h, int kind, Object data) {}
    // kinds: 0 category(centred), 1 redstone, 2 plusminus(data=Boolean minus), 3 lock, 4 bwlist, 5 toggle(data=SealToggle), 6 trait(data=GolemTrait)
    private final List<Btn> buttons = new ArrayList<>();

    public SealScreen(SealMenu menu, Inventory inv, Component title) { super(menu, inv, title, 176, 232); }
    @Override protected void init() { super.init(); setupCategories(); }

    private boolean isOwner() { return menu.seal.getOwner().equals(Minecraft.getInstance().player.getUUID().toString()); }

    private void setupCategories() {
        buttons.clear();
        int[] cats = menu.categories;
        float slice = 60.0f / cats.length;
        float start = -180.0f + (cats.length - 1) * slice / 2.0f;
        if (slice > 24.0f) slice = 24.0f;
        if (slice < 12.0f) slice = 12.0f;
        int c = 0;
        for (int cat : cats) {
            if (cats.length > 1) {
                int xx = (int) (Mth.cos((start - c * slice) / 180.0f * (float) Math.PI) * 86.0f);
                int yy = (int) (Mth.sin((start - c * slice) / 180.0f * (float) Math.PI) * 86.0f);
                buttons.add(new Btn(c, leftPos + middleX + xx, topPos + middleY + yy, 16, 16, 0, cat));
            }
            ++c;
        }
        int xx2 = (int) (Mth.cos((start - c * slice) / 180.0f * (float) Math.PI) * 86.0f);
        int yy2 = (int) (Mth.sin((start - c * slice) / 180.0f * (float) Math.PI) * 86.0f);
        buttons.add(new Btn(27, leftPos + middleX + xx2 - 8, topPos + middleY + yy2 - 8, 16, 16, 1, null));
        int L = leftPos + middleX, T = topPos + middleY;
        switch (menu.category) {
            case 0 -> {
                buttons.add(new Btn(80, L - 5 - 14, T - 17, 10, 10, 2, true));
                buttons.add(new Btn(81, L - 5 + 14, T - 17, 10, 10, 2, false));
                buttons.add(new Btn(82, L + 18 - 12, T + 4, 10, 10, 2, true));
                buttons.add(new Btn(83, L + 18 + 11, T + 4, 10, 10, 2, false));
                buttons.add(new Btn(25, L - 32, T, 16, 16, 3, null));
            }
            case 1 -> {
                if (menu.seal.getSeal() instanceof ISealConfigFilter f) {
                    int s = f.getFilterSize(), sy = 16 + (s - 1) / 3 * 12;
                    buttons.add(new Btn(20, L - 8, T + (s - 1) / 3 * 24 - sy + 27, 16, 16, 4, null));
                }
            }
            case 2 -> {
                buttons.add(new Btn(90, L - 19, T - 25, 10, 10, 2, true)); buttons.add(new Btn(91, L + 9, T - 25, 10, 10, 2, false));
                buttons.add(new Btn(92, L - 19, T, 10, 10, 2, true)); buttons.add(new Btn(93, L + 9, T, 10, 10, 2, false));
                buttons.add(new Btn(94, L - 19, T + 25, 10, 10, 2, true)); buttons.add(new Btn(95, L + 9, T + 25, 10, 10, 2, false));
            }
            case 3 -> {
                if (menu.seal.getSeal() instanceof ISealConfigToggles cp) {
                    int n = cp.getToggles().length;
                    int s2 = n < 4 ? 8 : n < 6 ? 7 : n < 9 ? 6 : 5;
                    int h = (n - 1) * s2, w = 12;
                    for (var prop : cp.getToggles()) w = Math.max(w, (12 + Math.min(100, font.width(Component.translatable(prop.getName())))) / 2);
                    int p = 0;
                    for (var prop : cp.getToggles()) { buttons.add(new Btn(30 + p, L - w, T - 5 - h + p * (s2 * 2), 8, 8, 5, prop)); ++p; }
                }
            }
            case 4 -> {
                GolemTrait[] tags = menu.seal.getSeal().getRequiredTags();
                if (tags != null) for (int p = 0; p < tags.length; p++) buttons.add(new Btn(500 + p, L + p * 18 - (tags.length - 1) * 9, T - 8, 16, 16, 6, tags[p]));
                tags = menu.seal.getSeal().getForbiddenTags();
                if (tags != null) for (int p = 0; p < tags.length; p++) buttons.add(new Btn(600 + p, L + p * 18 - (tags.length - 1) * 9, T + 24, 16, 16, 6, tags[p]));
            }
            default -> {}
        }
    }

    private boolean over(Btn b, double mx, double my) {
        int x = b.kind == 0 || b.kind == 6 ? b.x - 8 : b.x, y = b.kind == 0 || b.kind == 6 ? b.y - 8 : b.y;
        return mx >= x && my >= y && mx < x + b.w && my < y + b.h;
    }
    private void blit(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h, int col) { g.blit(RenderPipelines.GUI_TEXTURED, TEX, x, y, u, v, w, h, 256, 256, col); }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float t) {
        super.extractBackground(g, mx, my, t);
        int L = leftPos + middleX, T = topPos + middleY;
        blit(g, L - 80, T - 80, 96, 0, 160, 160, -1);
        blit(g, leftPos, topPos + 143, 0, 167, 176, 89, -1);
        g.centeredText(font, Component.translatable("button.category." + menu.category), L, T - 64, 0xFFFFFFFF);
        switch (menu.category) {
            case 0 -> {
                blit(g, L + 17, T + 3, 2, 18, 12, 12, -1);
                if (menu.seal.getColor() >= 1 && menu.seal.getColor() <= 16) blit(g, L + 20, T + 6, 74, 31, 6, 6, 0xFF000000 | DyeColor.byId(menu.seal.getColor() - 1).getTextureDiffuseColor());
                int rx = mx - leftPos, ry = my - topPos;
                if (rx >= middleX + 5 && rx <= middleX + 41 && ry >= middleY + 3 && ry <= middleY + 15) {
                    Component s = menu.seal.getColor() >= 1 && menu.seal.getColor() <= 16
                            ? Component.translatable("golem.prop.color", Component.translatable("color.minecraft." + DyeColor.byId(menu.seal.getColor() - 1).getName()))
                            : Component.translatable("golem.prop.colorall");
                    g.centeredText(font, s, L + 23, T + 17, 0xFFFFFFFF);
                }
                g.centeredText(font, Component.translatable("golem.prop.priority"), L, T - 28, 0xFFBBAAFF);
                g.centeredText(font, Component.literal("" + menu.seal.getPriority()), L, T - 16, 0xFFFFFFFF);
                if (isOwner()) g.centeredText(font, Component.translatable("golem.prop.owner"), L, T + 32, 0xFFBBAAFF);
            }
            case 1 -> {
                if (menu.seal.getSeal() instanceof ISealConfigFilter f) {
                    int s3 = f.getFilterSize(), sx = 16 + (s3 - 1) % 3 * 12, sy = 16 + (s3 - 1) / 3 * 12;
                    for (int a = 0; a < s3; a++) blit(g, L + a % 3 * 24 - sx, T + a / 3 * 24 - sy, 0, 56, 32, 32, -1);
                }
            }
            case 2 -> {
                g.centeredText(font, Component.translatable("button.caption.y"), L, T - 33, 0xFFDDDDDD);
                g.centeredText(font, Component.translatable("button.caption.x"), L, T - 9, 0xFFDDDDDD);
                g.centeredText(font, Component.translatable("button.caption.z"), L, T + 15, 0xFFDDDDDD);
                g.centeredText(font, Component.literal("" + menu.seal.getArea().getY()), L, T - 24, 0xFFFFFFFF);
                g.centeredText(font, Component.literal("" + menu.seal.getArea().getX()), L, T, 0xFFFFFFFF);
                g.centeredText(font, Component.literal("" + menu.seal.getArea().getZ()), L, T + 24, 0xFFFFFFFF);
            }
            case 4 -> {
                g.centeredText(font, Component.translatable("button.caption.required"), L, T - 26, 0xFFDDDDDD);
                g.centeredText(font, Component.translatable("button.caption.forbidden"), L, T + 6, 0xFFDDDDDD);
            }
            default -> {}
        }
        for (Btn b : buttons) drawButton(g, b, mx, my);
    }

    private void drawButton(GuiGraphicsExtractor g, Btn b, int mx, int my) {
        boolean h = over(b, mx, my);
        int dim = ARGB.colorFromFloat(0.9f, 0.9f, 0.9f, 0.9f);
        switch (b.kind) {
            case 0 -> {
                int cat = (Integer) b.data;
                int col = cat == menu.category ? -1 : h ? dim : ARGB.colorFromFloat(0.7f, 0.7f, 0.7f, 0.7f);
                blit(g, b.x - 8, b.y - 8, cat * 16, 120, 16, 16, col);
            }
            case 1 -> blit(g, b.x, b.y, menu.seal.isRedstoneSensitive() ? 64 : 80, 136, 16, 16, h ? -1 : dim);
            case 2 -> blit(g, b.x, b.y, (Boolean) b.data ? 0 : 10, 0, 10, 10, h ? -1 : dim);
            case 3 -> blit(g, b.x, b.y, menu.seal.isLocked() ? 32 : 48, 136, 16, 16, h ? -1 : dim);
            case 4 -> blit(g, b.x, b.y, ((ISealConfigFilter) menu.seal.getSeal()).isBlacklist() ? 0 : 16, 136, 16, 16, h ? -1 : dim);
            case 5 -> {
                var prop = (ISealConfigToggles.SealToggle) b.data;
                blit(g, b.x - 2, b.y - 2, 2, 18, 12, 12, -1);
                if (prop.getValue()) blit(g, b.x - 2, b.y - 2, 18, 18, 12, 12, -1);
                g.text(font, Component.translatable(prop.getName()), b.x + 12, b.y, 0xFFFFFFFF, true);
            }
            case 6 -> g.blit(RenderPipelines.GUI_TEXTURED, ((GolemTrait) b.data).icon, b.x - 8, b.y - 8, 0, 0, 16, 16, 16, 16, h ? -1 : dim);
            default -> {}
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float t) {
        super.extractRenderState(g, mx, my, t);
        if (menu.category == 1 && menu.seal.getSeal() instanceof ISealConfigFilter cp && !cp.isBlacklist()) {
            for (int i = 0; i < cp.getFilterSize(); i++) {
                var slot = menu.slots.get(i);
                if (slot.getItem().isEmpty()) continue;
                int sz = cp.getFilterSlotSize(i);
                Component s = sz == 0 ? Component.literal("*").withStyle(ChatFormatting.GOLD) : Component.literal(String.valueOf(sz));
                g.text(font, s, leftPos + slot.x + 19 - 2 - font.width(s), topPos + slot.y + 9, 0xFFFFFFFF, true);
            }
        }
        for (Btn b : buttons) {
            if (!over(b, mx, my)) continue;
            switch (b.kind) {
                case 0 -> { Component s = Component.translatable("button.category." + b.data); g.text(font, s, b.x - 10 - font.width(s), b.y - 4, 0xFFFFFFFF, true); }
                case 1 -> { Component s = Component.translatable(menu.seal.isRedstoneSensitive() ? "golem.prop.redon" : "golem.prop.redoff"); g.text(font, s, b.x - 2 - font.width(s), b.y + 4, 0xFFFFFFFF, true); }
                case 3 -> g.centeredText(font, Component.translatable(menu.seal.isLocked() ? "golem.prop.lock" : "golem.prop.unlock"), b.x + 8, b.y + 17, 0xFFFFFFFF);
                case 4 -> g.centeredText(font, Component.translatable(((ISealConfigFilter) menu.seal.getSeal()).isBlacklist() ? "button.bl" : "button.wl"), b.x + 8, b.y + 17, 0xFFFFFFFF);
                case 6 -> g.setComponentTooltipForNextFrame(font, List.of(((GolemTrait) b.data).localizedName(), ((GolemTrait) b.data).localizedDescription().copy().withStyle(ChatFormatting.ITALIC, ChatFormatting.BLUE)), mx, my);
                default -> {}
            }
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mx, int my) {}

    private void send(int id) { Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, id); }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        for (Btn b : buttons) {
            if (!over(b, e.x(), e.y()) || b.kind == 6) continue;
            int id = b.id;
            if (b.kind == 0) {
                if (menu.categories[id] != menu.category) { menu.apply(id); send(id); setupCategories(); }
            } else if (b.kind == 3) {
                if (menu.category == 0 && isOwner()) { menu.seal.setLocked(!menu.seal.isLocked()); send(menu.seal.isLocked() ? 25 : 26); }
            } else if (b.kind == 4) {
                var cp = (ISealConfigFilter) menu.seal.getSeal();
                cp.setBlacklist(!cp.isBlacklist());
                send(cp.isBlacklist() ? 20 : 21);
            } else if (b.kind == 5) {
                var cp = (ISealConfigToggles) menu.seal.getSeal();
                cp.setToggle(id - 30, !cp.getToggles()[id - 30].getValue());
                send(cp.getToggles()[id - 30].getValue() ? id : id + 30);
            } else if (b.kind == 1) {
                if (isOwner()) { menu.seal.setRedstoneSensitive(!menu.seal.isRedstoneSensitive()); send(menu.seal.isRedstoneSensitive() ? 27 : 28); }
            } else { menu.apply(id); send(id); }
            Minecraft.getInstance().player.playSound(art.arcane.thaumcraft.registry.ModSounds.get("clack"), 0.4f, 1.0f);
            return true;
        }
        return super.mouseClicked(e, dbl);
    }
}
