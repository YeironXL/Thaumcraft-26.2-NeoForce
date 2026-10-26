package art.arcane.thaumcraft.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Development aid (-Dthaumcraft.debugHold=front|back|first): fixes the camera for visual checks of held items. */
public final class DebugScene {
    private static int ticks, playFrames;
    private static boolean screenshotPage;
    public static void register() {
        String mode = System.getProperty("thaumcraft.debugHold");
        if (mode == null) return;
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            var mc = Minecraft.getInstance();
            // quick-play into the debug save: skip the "experimental settings" backup prompt
            if (mc.gui.screen() instanceof net.minecraft.client.gui.screens.BackupConfirmScreen bs) {
                try { var f = net.minecraft.client.gui.screens.BackupConfirmScreen.class.getDeclaredField("onProceed"); f.setAccessible(true);
                    mc.gui.setScreen(null); ((net.minecraft.client.gui.screens.BackupConfirmScreen.Listener) f.get(bs)).proceed(false, false); } catch (ReflectiveOperationException ignored) {}
            }
            if (mc.player == null) return;
            // -PdebugTooltip=n: open the inventory and show the tooltip of hotbar slots 1..n in turn (aspect tooltip checks)
            if (System.getProperty("thaumcraft.debugTooltip") != null && ticks == 100) mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            if (System.getProperty("thaumcraft.debugTooltip") != null && ticks > 100 && mc.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> cs) {
                // hover hotbar slots 1..n in turn (30 ticks each) by moving the cursor onto them
                int n = Integer.parseInt(System.getProperty("thaumcraft.debugTooltip")), slot = 1 + (ticks / 30) % n;
                for (var sl : cs.getMenu().slots) if (sl.container == mc.player.getInventory() && sl.getContainerSlot() == slot) {
                    double scale = mc.getWindow().getGuiScale();
                    try { var fx = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos"); var fy = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos"); fx.setAccessible(true); fy.setAccessible(true);
                        fx.setDouble(mc.mouseHandler, (cs.getGuiLeft() + sl.x + 8) * scale); fy.setDouble(mc.mouseHandler, (cs.getGuiTop() + sl.y + 8) * scale); } catch (Exception ex) {}
                    break;
                }
            }
            if (++ticks == 60) mc.options.setCameraType(switch (mode) { case "back" -> CameraType.THIRD_PERSON_BACK; case "first" -> CameraType.FIRST_PERSON; default -> CameraType.THIRD_PERSON_FRONT; });
            if (System.getProperty("thaumcraft.debugPlay") != null) {
                // click FIRSTSTEPS once the browser has been open for a while (node position given as window px "x:y")
                if (mc.gui.screen() instanceof art.arcane.thaumcraft.client.book.ResearchBrowserScreen) {
                    playFrames++;
                    String[] xy = System.getProperty("thaumcraft.debugPlay").split(":");
                    if (playFrames >= 20 && playFrames <= 40) { try { var fx = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos"); var fy = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos"); fx.setAccessible(true); fy.setAccessible(true); fx.setDouble(mc.mouseHandler, Double.parseDouble(xy[0])); fy.setDouble(mc.mouseHandler, Double.parseDouble(xy[1])); } catch (Exception ex) {} }
                    if (playFrames == 40) {
                        double gx = mc.mouseHandler.getScaledXPos(mc.getWindow()), gy = mc.mouseHandler.getScaledYPos(mc.getWindow());
                        mc.gui.screen().mouseClicked(new net.minecraft.client.input.MouseButtonEvent(gx, gy, new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
                        System.out.println("[PLAY] client clicked map at " + gx + "," + gy + " -> " + mc.gui.screen().getClass().getSimpleName());
                    }
                } else if (playFrames >= 40 && playFrames < 1000 && mc.gui.screen() instanceof art.arcane.thaumcraft.client.book.ResearchPageScreen) { playFrames = 1000; screenshotPage = true; }
                else if (playFrames == 1000 && screenshotPage) { playFrames++; }
                if (playFrames > 1000 && playFrames < 1060) { playFrames++; if (playFrames == 1059) mc.gui.setScreen(null); }
                if (mc.gui.screen() instanceof art.arcane.thaumcraft.client.book.ResearchPageScreen && playFrames >= 1000 && playFrames < 1059) playFrames++;
            }
            String book = System.getProperty("thaumcraft.debugBook");
            if (book != null && ticks == 100) {
                var browser = new art.arcane.thaumcraft.client.book.ResearchBrowserScreen();
                if (book.equals("map")) mc.gui.setScreen(browser);
                else if (book.contains("#")) mc.gui.setScreen(new art.arcane.thaumcraft.client.book.ResearchPageScreen(book.split("#")[0], art.arcane.thaumcraft.client.book.BookRecipes.norm(book.split("#")[1]), 0, browser));
                else mc.gui.setScreen(new art.arcane.thaumcraft.client.book.ResearchPageScreen(book, browser));
            }
            if (book != null && System.getProperty("thaumcraft.debugClickCenter") != null && mc.gui.screen() != null) {
                long win = mc.getWindow().handle();
                double cx = Double.parseDouble(System.getProperty("thaumcraft.debugClickCenter").split(":")[0]), cy = Double.parseDouble(System.getProperty("thaumcraft.debugClickCenter").split(":")[1]);
                if (ticks >= 140 && ticks <= 160) { try { var fx = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos"); var fy = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos"); fx.setAccessible(true); fy.setAccessible(true); fx.setDouble(mc.mouseHandler, cx); fy.setDouble(mc.mouseHandler, cy); } catch (Exception ex) { System.out.println("[DEBUGCLICK] reflect fail " + ex); } }
                if (ticks == 160) {
                    double gx = mc.mouseHandler.getScaledXPos(mc.getWindow()), gy = mc.mouseHandler.getScaledYPos(mc.getWindow());
                    System.out.println("[DEBUGCLICK] screen=" + mc.gui.screen().getClass().getSimpleName() + " at " + gx + "," + gy);
                    mc.gui.screen().mouseClicked(new net.minecraft.client.input.MouseButtonEvent(gx, gy, new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
                    System.out.println("[DEBUGCLICK] after click screen=" + mc.gui.screen().getClass().getSimpleName());
                }
            }
            if (book != null) return;
            // -PdebugUse=<tick>: hold the use key from that tick on (charged items, throwables).
            if ("xray".equals(System.getProperty("thaumcraft.debugGolems")) && ticks >= 220) mc.options.keyShift.setDown(true);
            if (System.getProperty("thaumcraft.debugUse") != null && ticks >= Integer.parseInt(System.getProperty("thaumcraft.debugUse")) && mc.gui.screen() == null) mc.options.keyUse.setDown(true);
            if (ticks > 60 && System.getProperty("thaumcraft.debugOuter") != null && System.getProperty("thaumcraft.debugYaw") == null) mc.player.setXRot(Float.parseFloat(System.getProperty("thaumcraft.debugPitch", "10")));
            else if (ticks > 60 && System.getProperty("thaumcraft.debugAimDial") != null) aimDial(mc, Integer.parseInt(System.getProperty("thaumcraft.debugAimDial")));
            else if (ticks > 60) { mc.player.setYRot(Float.parseFloat(System.getProperty("thaumcraft.debugYaw", "0"))); mc.player.setXRot(Float.parseFloat(System.getProperty("thaumcraft.debugPitch", "10"))); }
        });
    }
    /** -PdebugAimDial=0|1: look at the out (0) / in (1) dial of the nearest redstone relay (TC6 "Out: n"/"In: n" aim text). */
    private static void aimDial(net.minecraft.client.Minecraft mc, int which) {
        var me = mc.player.blockPosition();
        for (var bp : net.minecraft.core.BlockPos.betweenClosed(me.offset(-6, -3, -6), me.offset(6, 3, 6))) {
            var st = mc.level.getBlockState(bp);
            if (!(st.getBlock() instanceof art.arcane.thaumcraft.block.RedstoneRelayBlock)) continue;
            int q = switch (st.getValue(art.arcane.thaumcraft.block.RedstoneRelayBlock.FACING)) { case WEST -> 1; case NORTH -> 2; case EAST -> 3; default -> 0; };
            double x = which == 0 ? -0.25 : 0.0, z = which == 0 ? -0.25 : 0.25;
            for (int i = 0; i < q; i++) { double t = x; x = -z; z = t; } // inverse of RedstoneRelayBlock.dial's quarter turns
            var eye = mc.player.getEyePosition();
            double dx = bp.getX() + 0.5 + x - eye.x, dy = bp.getY() + 0.25 - eye.y, dz = bp.getZ() + 0.5 + z - eye.z;
            mc.player.setYRot((float) Math.toDegrees(-Math.atan2(dx, dz)));
            mc.player.setXRot((float) Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
            return;
        }
    }
    private DebugScene() {}
}
