package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.casters.CasterManager;
import art.arcane.thaumcraft.casters.IArchitect;
import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import art.arcane.thaumcraft.item.CasterItem;
import art.arcane.thaumcraft.item.FocusItem;
import art.arcane.thaumcraft.item.FocusPouchItem;
import art.arcane.thaumcraft.network.CasterKeyPayload;
import art.arcane.thaumcraft.util.Baubles;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/** TC6 KeyHandler (F/G), WandRenderingHandler (focus radial, architect overlay) and HudHandler casting dial. */
public final class CasterClient {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Identifier.fromNamespaceAndPath("thaumcraft", "thaumcraft"));
    public static final KeyMapping KEY_F = new KeyMapping("key.thaumcraft.change_focus", GLFW.GLFW_KEY_F, CATEGORY);
    public static final KeyMapping KEY_G = new KeyMapping("key.thaumcraft.misc_toggle", GLFW.GLFW_KEY_G, CATEGORY);
    private static final Identifier HUD = Identifier.parse("thaumcraft:textures/gui/hud.png");
    private static final Identifier R1 = Identifier.parse("thaumcraft:textures/misc/radial.png");
    private static final Identifier R2 = Identifier.parse("thaumcraft:textures/misc/radial2.png");
    private static final Identifier CFRAME = Identifier.parse("thaumcraft:textures/misc/frame_corner.png");
    private static final Identifier SFRAME = Identifier.parse("thaumcraft:textures/misc/frame_side.png");
    private static final Identifier ARROWS = Identifier.parse("thaumcraft:textures/misc/architect_arrows.png");
    private static final DecimalFormat SECONDS = new DecimalFormat("#######.#");

    static boolean keyPressedF, keyPressedG, radialActive, radialLock, clicked;
    static float radialHudScale;
    private static long lastTime;
    private static boolean lastState;
    private static final TreeMap<String, Integer> foci = new TreeMap<>();
    private static final Map<String, ItemStack> fociItem = new HashMap<>();
    private static final Map<String, Boolean> fociHover = new HashMap<>();
    private static final Map<String, Float> fociScale = new HashMap<>();

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent e) -> { e.registerCategory(CATEGORY); e.register(KEY_F); e.register(KEY_G); });
        modBus.addListener((RegisterGuiLayersEvent e) -> e.registerAboveAll(Identifier.fromNamespaceAndPath("thaumcraft", "focus_radial"), CasterClient::renderRadial));
        NeoForge.EVENT_BUS.addListener(CasterClient::tick);
        NeoForge.EVENT_BUS.addListener(CasterClient::mouse);
        NeoForge.EVENT_BUS.addListener(CasterClient::renderHud);
        NeoForge.EVENT_BUS.addListener(CasterClient::architect);
    }

    private static boolean holdingCaster(Player p) { return p.getMainHandItem().getItem() instanceof CasterItem || p.getOffhandItem().getItem() instanceof CasterItem; }

    /** KeyHandler.playerTick. */
    private static void tick(ClientTickEvent.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        // TC6 and vanilla both default to F (change focus / swap hands). While a caster is held and the keys still clash,
        // F only drives the focus radial instead of also throwing the gauntlet into the other hand.
        if (holdingCaster(player) && KEY_F.same(mc.options.keySwapOffhand)) while (mc.options.keySwapOffhand.consumeClick()) { }
        if (KEY_F.isDown()) {
            if (mc.gui.screen() == null) {
                if (!keyPressedF) radialLock = false;
                if (!radialLock && holdingCaster(player)) {
                    if (player.isShiftKeyDown()) { if (!keyPressedF) ClientPacketDistributor.sendToServer(new CasterKeyPayload("REMOVE", -1, 0)); }
                    else radialActive = true;
                }
                keyPressedF = true;
            }
        } else {
            radialActive = false;
            keyPressedF = false;
        }
        if (KEY_G.isDown()) {
            if (mc.gui.screen() == null && !keyPressedG) {
                long win = mc.getWindow().handle();
                int mod = InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL) ? 1 : InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) ? 2 : 0;
                ClientPacketDistributor.sendToServer(new CasterKeyPayload("", 1, mod));
            }
            keyPressedG = true;
        } else keyPressedG = false;
    }

    private static void mouse(InputEvent.MouseButton.Pre e) {
        if (radialActive && radialHudScale > 0 && e.getButton() == 0 && e.getAction() == GLFW.GLFW_PRESS) { clicked = true; e.setCanceled(true); }
    }

    private static float change(long time, long last, long total) { return (time - last) / (float) total; }

    private static void getFociInfo(Player p) {
        foci.clear(); fociItem.clear(); fociHover.clear(); fociScale.clear();
        int pouchcount = 0;
        for (var w : Baubles.worn(p)) {
            if (!(w.stack().getItem() instanceof FocusPouchItem fp)) continue;
            ++pouchcount;
            var inv = fp.getInventory(w.stack());
            for (int q = 0; q < inv.size(); ++q) add(inv.get(q), q + pouchcount * 1000);
        }
        var main = p.getInventory().getNonEquipmentItems();
        for (int a = 0; a < 36; ++a) {
            ItemStack item = main.get(a);
            if (item.getItem() instanceof FocusItem) add(item, a);
            if (item.getItem() instanceof FocusPouchItem fp) {
                ++pouchcount;
                var inv = fp.getInventory(item);
                for (int q = 0; q < inv.size(); ++q) add(inv.get(q), q + pouchcount * 1000);
            }
        }
    }

    private static void add(ItemStack item, int loc) {
        if (!(item.getItem() instanceof FocusItem f)) return;
        String sh = f.getSortingHelper(item);
        if (sh == null) return;
        foci.put(sh, loc); fociItem.put(sh, item.copy()); fociScale.put(sh, 1.0f); fociHover.put(sh, false);
    }

    /** WandRenderingHandler.handleFociRadial + renderFocusRadialHUD. */
    private static void renderRadial(GuiGraphicsExtractor g, DeltaTracker dt) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        long time = System.currentTimeMillis();
        if (player == null) { lastTime = time; return; }
        if (radialActive || radialHudScale > 0.0f) {
            if (radialActive) {
                if (mc.gui.screen() != null) { radialActive = false; radialLock = true; lastTime = time; return; }
                if (radialHudScale == 0.0f) {
                    getFociInfo(player);
                    if (!foci.isEmpty() && mc.mouseHandler.isMouseGrabbed()) mc.mouseHandler.releaseMouse();
                }
            } else if (mc.gui.screen() == null && lastState) {
                if (!mc.mouseHandler.isMouseGrabbed()) mc.mouseHandler.grabMouse();
                lastState = false;
            }
            drawRadial(g, mc, player, dt.getGameTimeDeltaPartialTick(false));
            if (time > lastTime) {
                for (String key : fociHover.keySet()) {
                    if (fociHover.get(key)) {
                        if (!radialActive && !radialLock) { ClientPacketDistributor.sendToServer(new CasterKeyPayload(key, -1, 0)); radialLock = true; }
                        if (fociScale.get(key) < 1.3f) fociScale.put(key, Math.min(1.3f, fociScale.get(key) + change(time, lastTime, 150L)));
                    } else if (fociScale.get(key) > 1.0f) fociScale.put(key, Math.max(1.0f, fociScale.get(key) - change(time, lastTime, 250L)));
                }
                if (!radialActive) radialHudScale -= change(time, lastTime, 150L);
                else if (radialHudScale < 1.0f) radialHudScale += change(time, lastTime, 150L);
                if (radialHudScale > 1.0f) radialHudScale = 1.0f;
                if (radialHudScale < 0.0f) { radialHudScale = 0.0f; radialLock = false; }
                lastState = radialActive;
            }
        }
        lastTime = time;
        clicked = false;
    }

    private static void drawRadial(GuiGraphicsExtractor g, Minecraft mc, Player player, float pt) {
        ItemStack s = player.getMainHandItem();
        if (!(s.getItem() instanceof CasterItem)) s = player.getOffhandItem();
        if (!(s.getItem() instanceof CasterItem wand) || fociItem.isEmpty()) return;
        double sw = g.guiWidth(), sh = g.guiHeight();
        int i = (int) (mc.mouseHandler.xpos() * sw / mc.getWindow().getScreenWidth());
        int j = (int) (mc.mouseHandler.ypos() * sh / mc.getWindow().getScreenHeight());
        ItemStack tt = null;
        float width = 16.0f + fociItem.size() * 2.5f;
        float rot = (pt + player.tickCount % 720 / 2.0f);
        g.pose().pushMatrix();
        g.pose().translate((int) (sw / 2.0), (int) (sh / 2.0));
        quad(g, R1, rot, width * 2.75f * radialHudScale, 0x80808080);
        quad(g, R2, -rot, width * 2.55f * radialHudScale, 0x80808080);
        if (wand.getFocus(s) != null) {
            ItemStack item = wand.getFocusStack(s).copy();
            g.item(item, -8, -8);
            int mx = (int) (i - sw / 2.0), my = (int) (j - sh / 2.0);
            if (mx >= -10 && mx <= 10 && my >= -10 && my <= 10) tt = item;
        }
        g.pose().scale(radialHudScale, radialHudScale);
        float currentRot = -90.0f * radialHudScale;
        float pieSlice = 360.0f / fociItem.size();
        String key = foci.firstKey();
        for (int a = 0; a < fociItem.size() && key != null; ++a) {
            double xx = Mth.cos(currentRot / 180.0f * (float) Math.PI) * width;
            double yy = Mth.sin(currentRot / 180.0f * (float) Math.PI) * width;
            currentRot += pieSlice;
            g.pose().pushMatrix();
            g.pose().translate((int) xx, (int) yy);
            float sc = fociScale.get(key);
            g.pose().scale(sc, sc);
            g.item(fociItem.get(key), -8, -8);
            g.pose().popMatrix();
            if (!radialLock && radialActive) {
                int mx2 = (int) (i - sw / 2.0 - xx), my2 = (int) (j - sh / 2.0 - yy);
                if (mx2 >= -10 && mx2 <= 10 && my2 >= -10 && my2 <= 10) {
                    fociHover.put(key, true);
                    tt = fociItem.get(key);
                    if (clicked) {
                        radialActive = false; radialLock = true;
                        ClientPacketDistributor.sendToServer(new CasterKeyPayload(key, -1, 0));
                        break;
                    }
                } else fociHover.put(key, false);
            }
            key = foci.higherKey(key);
        }
        g.pose().popMatrix();
        if (tt != null) g.setTooltipForNextFrame(mc.font, tt, (int) (sw / 2.0) - 4, (int) (sh / 2.0) + 20);
    }

    private static void quad(GuiGraphicsExtractor g, Identifier tex, float rotDeg, float size, int color) {
        if (size <= 0) return;
        g.pose().pushMatrix();
        g.pose().rotate((float) Math.toRadians(rotDeg));
        g.pose().scale(size / 256.0f, size / 256.0f);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, -128, -128, 0, 0, 256, 256, 256, 256, color);
        g.pose().popMatrix();
    }

    /** HudHandler.renderCastingWandHud. */
    private static void renderHud(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.gui.hud.isHidden() || mc.gui.screen() != null) return;
        ItemStack wandstack = player.getMainHandItem().getItem() instanceof CasterItem ? player.getMainHandItem() : player.getOffhandItem();
        if (!(wandstack.getItem() instanceof CasterItem wand)) return;
        var aura = ThaumometerHud.current();
        var g = e.getGuiGraphics();
        g.pose().pushMatrix();
        g.pose().pushMatrix(); g.pose().scale(0.5f, 0.5f);
        g.blit(RenderPipelines.GUI_TEXTURED, HUD, 0, 0, 0, 0, 64, 64, 256, 256);
        g.pose().popMatrix();
        g.pose().translate(16, 16);
        int max = aura == null ? 0 : aura.base();
        int amt = aura == null ? 0 : (int) aura.vis();
        FocusItem focus = wand.getFocus(wandstack);
        ItemStack focusStack = wand.getFocusStack(wandstack);
        g.pose().pushMatrix();
        g.pose().translate(16, -10);
        g.pose().scale(0.5f, 0.5f);
        int loc = max > 0 ? (int) (30.0f * amt / max) : 0;
        int ac = AspectRegistry.color("potentia");
        if (loc > 0) g.blit(RenderPipelines.GUI_TEXTURED, HUD, -4, 35 - loc, 104, 0, 8, loc, 256, 256, 0xCC000000 | ac);
        g.blit(RenderPipelines.GUI_TEXTURED, HUD, -8, -3, 72, 0, 16, 42, 256, 256);
        if (player.isShiftKeyDown()) {
            g.pose().pushMatrix(); g.pose().rotate((float) Math.toRadians(-90));
            g.text(mc.font, SECONDS.format(amt), -32, -4, 0xFFFFFFFF, true);
            g.pose().popMatrix();
            if (focus != null && focus.getVisCost(focusStack) > 0.0f) {
                float mod = wand.getConsumptionModifier(wandstack, player, false);
                String msg = SECONDS.format(focus.getVisCost(focusStack) * mod);
                g.text(mc.font, msg, -32 - mc.font.width(msg) / 2, 32, 0xFFFFFFFF, true);
            }
        }
        g.pose().popMatrix();
        if (focus != null) {
            ItemStack picked = wand.getPickedBlock(wandstack);
            if (!picked.isEmpty()) {
                g.item(picked, -8, -8);
                int amount = 0;
                for (ItemStack is : player.getInventory().getNonEquipmentItems()) if (ItemStack.isSameItem(is, picked)) amount += is.getCount();
                String am = "" + amount;
                int swd = mc.font.width(am);
                g.pose().pushMatrix(); g.pose().translate(0, -9); g.pose().scale(0.5f, 0.5f);
                for (int a = -1; a <= 1; ++a) for (int b = -1; b <= 1; ++b) if ((a == 0 || b == 0) && (a != 0 || b != 0)) g.text(mc.font, am, a + 16 - swd, b + 24, 0xFF000000, false);
                g.text(mc.font, am, 16 - swd, 24, 0xFFFFFFFF, false);
                g.pose().popMatrix();
            } else g.item(focusStack, -8, -8);
        }
        g.pose().popMatrix();
    }

    // ---------------- architect overlay ----------------
    private static List<BlockPos> architectBlocks = new ArrayList<>();
    private static int lastArcHash;
    private static final int[][] MOS = {{4, 5, 6, 7}, {0, 1, 2, 3}, {0, 1, 4, 5}, {2, 3, 6, 7}, {0, 2, 4, 6}, {1, 3, 5, 7}};
    private static final int[][] ROTMAT = {{0, 90, 270, 180}, {270, 180, 0, 90}, {180, 90, 270, 0}, {0, 270, 90, 180}, {270, 180, 0, 90}, {180, 270, 90, 0}};

    private static void architect(SubmitCustomGeometryEvent e) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        ItemStack stack;
        IArchitect af;
        if (player.getMainHandItem().getItem() instanceof IArchitect ia) { stack = player.getMainHandItem(); af = ia; }
        else {
            stack = player.getMainHandItem().getItem() instanceof CasterItem ? player.getMainHandItem() : player.getOffhandItem();
            if (!(stack.getItem() instanceof CasterItem caster)) return;
            af = caster.getArchitect(stack);
        }
        if (af == null) return;
        if (!(af.getArchitectMOP(stack, mc.level, player) instanceof BlockHitResult target)) return;
        int hc = (target.getBlockPos().getX() + "" + target.getBlockPos().getY() + "" + target.getBlockPos().getZ() + "" + target.getDirection() + "" + player.tickCount / 5).hashCode();
        if (hc != lastArcHash) { lastArcHash = hc; architectBlocks = af.getArchitectBlocks(stack, mc.level, target.getBlockPos(), target.getDirection(), player); }
        if (architectBlocks == null || architectBlocks.isEmpty()) return;
        Vec3 cam = e.getLevelRenderState().cameraRenderState.pos;
        var blocks = new java.util.HashSet<>(architectBlocks);
        PoseStack ps = e.getPoseStack();
        var collector = e.getSubmitNodeCollector();
        // Axis arrows.
        boolean dx = af.showAxis(stack, mc.level, player, target.getDirection(), IArchitect.EnumAxis.X);
        boolean dy = af.showAxis(stack, mc.level, player, target.getDirection(), IArchitect.EnumAxis.Y);
        boolean dz = af.showAxis(stack, mc.level, player, target.getDirection(), IArchitect.EnumAxis.Z);
        BlockPos tp = target.getBlockPos();
        if (dx || dy || dz) {
            float r = Mth.sin(player.tickCount / 4.0f + tp.getX()) * 0.2f + 0.3f;
            float gg = Mth.sin(player.tickCount / 3.0f + tp.getY()) * 0.2f + 0.3f;
            float b = Mth.sin(player.tickCount / 2.0f + tp.getZ()) * 0.2f + 0.8f;
            int col = net.minecraft.util.ARGB.colorFromFloat(1.0f, r, gg, b);
            Vec3 c = Vec3.atCenterOf(tp).subtract(cam);
            collector.submitCustomGeometry(ps, TcBeams.overlayAdditive(ARROWS), (pose, vc) -> {
                if (dz) { plane(pose, vc, c, Direction.Axis.Y, Direction.Axis.Z, col); plane(pose, vc, c, Direction.Axis.X, Direction.Axis.Z, col); }
                if (dx) { plane(pose, vc, c, Direction.Axis.Y, Direction.Axis.X, col); plane(pose, vc, c, Direction.Axis.Z, Direction.Axis.X, col); }
                if (dy) { plane(pose, vc, c, Direction.Axis.X, Direction.Axis.Y, col); plane(pose, vc, c, Direction.Axis.Z, Direction.Axis.Y, col); }
            });
        }
        collector.submitCustomGeometry(ps, TcBeams.overlay(SFRAME), (pose, vc) -> {
            for (BlockPos p : architectBlocks) for (Direction face : Direction.values())
                if (!blocks.contains(p.relative(face))) faceQuad(pose, vc, Vec3.atCenterOf(p).subtract(cam), face, 0, 0x1AFFFFFF);
        });
        collector.submitCustomGeometry(ps, TcBeams.overlay(CFRAME), (pose, vc) -> {
            for (BlockPos p : architectBlocks) {
                boolean[] bm = sides(blocks, p);
                for (Direction face : Direction.values()) {
                    if (blocks.contains(p.relative(face))) continue;
                    for (int a = 0; a < 4; ++a) if (bm[MOS[face.ordinal()][a]]) faceQuad(pose, vc, Vec3.atCenterOf(p).subtract(cam), face, ROTMAT[face.ordinal()][a], 0xA8FFFFFF);
                }
            }
        });
    }

    private static boolean[] sides(java.util.Set<BlockPos> s, BlockPos p) {
        boolean w = !s.contains(p.offset(-1, 0, 0)), east = !s.contains(p.offset(1, 0, 0)), n = !s.contains(p.offset(0, 0, -1)), so = !s.contains(p.offset(0, 0, 1));
        boolean u = !s.contains(p.offset(0, 1, 0)), d = !s.contains(p.offset(0, -1, 0));
        return new boolean[] {w && n && u, east && n && u, w && so && u, east && so && u, w && n && d, east && n && d, w && so && d, east && so && d};
    }

    /** A unit quad on a block face, the texture rotated by {@code rot} degrees around the face normal. */
    static void faceQuad(PoseStack.Pose pose, VertexConsumer vc, Vec3 c, Direction face, int rot, int argb) {
        Vec3 n = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
        Vec3 u = Math.abs(n.y) > 0.5 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 v = n.cross(u);
        double rr = Math.toRadians(rot);
        Vec3 ur = u.scale(Math.cos(rr)).add(v.scale(Math.sin(rr))), vr = v.scale(Math.cos(rr)).subtract(u.scale(Math.sin(rr)));
        Vec3 o = c.add(n.scale(0.5));
        Vec3 p0 = o.subtract(ur.scale(0.5)).subtract(vr.scale(0.5)), p1 = o.add(ur.scale(0.5)).subtract(vr.scale(0.5));
        Vec3 p2 = o.add(ur.scale(0.5)).add(vr.scale(0.5)), p3 = o.subtract(ur.scale(0.5)).add(vr.scale(0.5));
        TcBeams.v(pose, vc, p0, 0, 1, argb); TcBeams.v(pose, vc, p1, 1, 1, argb); TcBeams.v(pose, vc, p2, 1, 0, argb); TcBeams.v(pose, vc, p3, 0, 0, argb);
    }

    /** Arrow plane through the block centre spanning {@code a} and the arrow axis {@code along}. */
    private static void plane(PoseStack.Pose pose, VertexConsumer vc, Vec3 c, Direction.Axis a, Direction.Axis along, int argb) {
        Vec3 ua = unit(a), ub = unit(along);
        Vec3 p0 = c.subtract(ua.scale(0.5)).subtract(ub.scale(0.5)), p1 = c.add(ua.scale(0.5)).subtract(ub.scale(0.5));
        Vec3 p2 = c.add(ua.scale(0.5)).add(ub.scale(0.5)), p3 = c.subtract(ua.scale(0.5)).add(ub.scale(0.5));
        TcBeams.v(pose, vc, p0, 0, 1, argb); TcBeams.v(pose, vc, p1, 1, 1, argb); TcBeams.v(pose, vc, p2, 1, 0, argb); TcBeams.v(pose, vc, p3, 0, 0, argb);
    }

    private static Vec3 unit(Direction.Axis a) { return a == Direction.Axis.X ? new Vec3(1, 0, 0) : a == Direction.Axis.Y ? new Vec3(0, 1, 0) : new Vec3(0, 0, 1); }

    private CasterClient() {}
}
