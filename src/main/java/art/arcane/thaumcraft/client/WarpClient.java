package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.fx.GenericParticleOptions;
import art.arcane.thaumcraft.item.CasterItem;
import art.arcane.thaumcraft.item.SanityCheckerItem;
import art.arcane.thaumcraft.item.ThaumometerItem;
import art.arcane.thaumcraft.network.FxPayload;
import art.arcane.thaumcraft.registry.ModEffects;
import art.arcane.thaumcraft.registry.ModSounds;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Client half of warp: PacketWarpMessage, PacketMiscEvent (heartbeat, eldritch fog), RenderEventHandler fog and
 * warp vignette, ShaderHandler post effects for the warp potions, HudHandler.renderSanityHud and PacketFXSonic.
 */
public final class WarpClient {
    private WarpClient() {}

    private static final Identifier HUD = Identifier.fromNamespaceAndPath("thaumcraft", "textures/gui/hud.png");
    private static final Identifier VIGNETTE = Identifier.fromNamespaceAndPath("thaumcraft", "textures/misc/vignette.png");
    /** ShaderHandler.shader_resources: desaturate (death gaze), blur, hunger, sun scorned. */
    private static final Identifier[] SHADERS = {
            Identifier.fromNamespaceAndPath("thaumcraft", "desaturatetc"), Identifier.fromNamespaceAndPath("thaumcraft", "blurtc"),
            Identifier.fromNamespaceAndPath("thaumcraft", "hunger"), Identifier.fromNamespaceAndPath("thaumcraft", "sunscorned") };
    private static final boolean[] ACTIVE = new boolean[4];
    private static CrossFrameResourcePool pool;

    public static boolean fogFiddled;
    public static int fogDuration;
    public static float fogTarget;
    public static int warpVignette;
    static float targetBrightness = 1.0f, prevVignetteBrightness;

    public static void register() {
        NeoForge.EVENT_BUS.addListener(WarpClient::tick);
        NeoForge.EVENT_BUS.addListener(WarpClient::fog);
        NeoForge.EVENT_BUS.addListener(WarpClient::afterLevel);
        NeoForge.EVENT_BUS.addListener(WarpClient::gui);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> {
            fogFiddled = false; fogTarget = 0; fogDuration = 0; warpVignette = 0; prevVignetteBrightness = 0; targetBrightness = 1;
            java.util.Arrays.fill(ACTIVE, false);
        });
    }

    /** FxPayload kinds handled here; returns false for anything else. */
    public static boolean handle(FxPayload p) {
        var mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return false;
        var a = p.args();
        switch (p.kind()) {
            case "warp_message" -> { warpMessage(player, a.get(0).intValue(), a.get(1).intValue()); return true; }
            case "misc_event" -> { miscEvent(player, a.get(0).intValue()); return true; }
            case "sonic" -> { if (mc.level != null && mc.level.getEntity(a.get(0).intValue()) instanceof Entity e) sonic(e); return true; }
            default -> { return false; }
        }
    }

    /** PacketWarpMessage.processMessage (type 0 permanent: only the whisper; 1 normal; 2 temporary). */
    static void warpMessage(Player player, int type, int data) {
        if (data == 0) return;
        if (type == 0 && data > 0) {
            player.playSound(ModSounds.get("whispers"), 0.5f, 1.0f);
        } else if (type == 1) {
            String text = data < 0 ? "tc.removewarpsticky" : "tc.addwarpsticky";
            if (data > 0) player.playSound(ModSounds.get("whispers"), 0.5f, 1.0f);
            player.sendOverlayMessage(Component.translatable(text));
        } else if (type == 2 && data > 0) {
            player.sendOverlayMessage(Component.translatable("tc.addwarptemp"));
        }
    }

    /** PacketMiscEvent: 0 heartbeat, 1 eldritch fog for 2400 ticks, 2 keep the fog for at least 200 ticks. */
    static void miscEvent(Player p, int type) {
        switch (type) {
            case 0 -> p.level().playLocalSound(p.getX(), p.getY(), p.getZ(), ModSounds.get("heartbeat"), net.minecraft.sounds.SoundSource.AMBIENT, 1.0f, 1.0f, false);
            case 1 -> { fogFiddled = true; fogDuration = 2400; }
            case 2 -> { fogFiddled = true; if (fogDuration < 200) fogDuration = 200; }
            default -> {}
        }
    }

    /** PacketFXSonic: new FXSonic(world, pos, entity, 10). */
    static void sonic(Entity source) {
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.particleEngine.add(new art.arcane.thaumcraft.client.fx.SonicParticle(mc.level, source, 10));
    }

    private static boolean active(Player p, int i) {
        return switch (i) {
            case 0 -> p.hasEffect(ModEffects.DEATH_GAZE);
            case 1 -> p.hasEffect(ModEffects.BLURRED_VISION);
            case 2 -> p.hasEffect(ModEffects.UNNATURAL_HUNGER);
            default -> p.hasEffect(ModEffects.SUN_SCORNED);
        };
    }

    /** RenderEventHandler.playerTick START: ShaderHandler.checkShaders, warp vignette and the fog fade. */
    private static void tick(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) return;
        for (int i = 0; i < 4; i++) ACTIVE[i] = active(mc.player, i);
        if (ACTIVE[0]) warpVignette = 10;
        if (warpVignette > 0) { --warpVignette; targetBrightness = 0.0f; } else targetBrightness = 1.0f;
        // development aids (-PdebugFog / -PdebugSonic): keep the eldritch fog on / screech from every nearby mob
        if (System.getProperty("thaumcraft.debugFog") != null) { fogFiddled = true; fogDuration = Math.max(fogDuration, 2400); }
        if (System.getProperty("thaumcraft.debugSonic") != null && mc.player.tickCount % 20 == 0 && mc.level != null)
            for (Entity e : mc.level.entitiesForRendering()) if (e instanceof net.minecraft.world.entity.Mob && e.distanceTo(mc.player) < 16) sonic(e);
        if (fogFiddled) {
            if (fogDuration < 100) fogTarget = 0.1f * (fogDuration / 100.0f);
            else if (fogTarget < 0.1f) fogTarget += 0.001f;
            --fogDuration;
            if (fogDuration < 0) fogFiddled = false;
        }
    }

    /**
     * RenderEventHandler.fogDensityEvent: glFogi(GL_FOG_MODE, GL_EXP); glFogf(GL_FOG_DENSITY, fogTarget).
     * 1.12 fixed-function EXP fog blends by f = exp(-density * d) (d = eye distance), replacing the linear start/end
     * entirely, for terrain, entities, sky and clouds alike. 26.2 fog is linear in the shader include fog.glsl, so the
     * mod overrides minecraft:shaders/include/fog.glsl: linear_fog_value(d, start, end) with end &lt; 0 returns
     * 1 - exp(end * d), i.e. density = -end. Every fog end (environmental, render distance, sky, clouds) is set to
     * -density so all of them follow the same exponential curve (for density 0.1: 63% at 10 blocks, 95% at 30).
     * Cylindrical distance never exceeds spherical, so max(env, renderDistance) equals the spherical GL_EXP value.
     */
    private static java.lang.invoke.MethodHandle irisInUse;
    private static Object irisApi;
    private static boolean irisChecked;
    /** Iris is optional: look its public API up reflectively once (IrisApi.getInstance().isShaderPackInUse()). */
    private static boolean irisShadersActive() {
        if (!irisChecked) {
            irisChecked = true;
            try {
                Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = c.getMethod("getInstance").invoke(null);
                irisInUse = java.lang.invoke.MethodHandles.publicLookup().findVirtual(c, "isShaderPackInUse", java.lang.invoke.MethodType.methodType(boolean.class));
            } catch (Throwable ignored) { irisApi = null; }
        }
        if (irisApi == null) return false;
        try { return (boolean) irisInUse.invoke(irisApi); } catch (Throwable t) { return false; }
    }

    private static void fog(ViewportEvent.RenderFog event) {
        if (!fogFiddled || fogTarget <= 0.0f) return;
        var d = event.getFogData();
        if (irisShadersActive()) {
            // Iris shaderpacks ignore our fog.glsl override and read the plain linear fog values: hand them the linear
            // fog that matches the EXP density (exp(-density * d) is 95 % fogged at 3 / density), from the eye.
            float end = Math.max(2.0f, 3.0f / fogTarget);
            d.environmentalStart = 0.0f; d.renderDistanceStart = 0.0f;
            d.environmentalEnd = Math.min(d.environmentalEnd, end); d.renderDistanceEnd = Math.min(d.renderDistanceEnd, end);
            d.skyEnd = Math.min(d.skyEnd, end); d.cloudEnd = Math.min(d.cloudEnd, end);
            return;
        }
        // world fog: flagged in the starts (-10000 - 1000 * density) so the ends, which Sodium uses to cull chunks, stay real
        d.environmentalStart = -10000.0f - 1000.0f * fogTarget;
        d.renderDistanceStart = -10000.0f - 1000.0f * fogTarget;
        d.skyEnd = -fogTarget;
        d.cloudEnd = -fogTarget;
    }

    /** ShaderHandler groups: run each active post chain over the world image. */
    private static void afterLevel(RenderLevelStageEvent.AfterLevel event) {
        boolean any = false;
        for (boolean b : ACTIVE) any |= b;
        if (!any) return;
        var mc = Minecraft.getInstance();
        if (pool == null) pool = new CrossFrameResourcePool(3);
        for (int i = 0; i < 4; i++) {
            if (!ACTIVE[i]) continue;
            var chain = mc.getShaderManager().getPostChain(SHADERS[i], LevelTargetBundle.MAIN_TARGETS);
            if (chain != null) chain.process(mc.gameRenderer.mainRenderTarget(), pool);
        }
        pool.endFrame();
    }

    private static void gui(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.gui.hud.isHidden()) return;
        var g = event.getGuiGraphics();
        // RenderEventHandler.renderVignette (death gaze): the warp vignette eases in and pulses.
        float brightness = 1.0f - targetBrightness;
        prevVignetteBrightness += (brightness - prevVignetteBrightness) * 0.01f;
        if (prevVignetteBrightness > 0.0f) {
            float b = Mth.clamp(prevVignetteBrightness * (1.0f + Mth.sin(player.tickCount / 2.0f) * 0.1f), 0.0f, 1.0f);
            g.blit(RenderPipelines.VIGNETTE, VIGNETTE, 0, 0, 0.0f, 0.0f, g.guiWidth(), g.guiHeight(), g.guiWidth(), g.guiHeight(), ARGB.colorFromFloat(1.0f, b, b, b));
        }
        if (mc.gui.screen() != null) return;
        // HudHandler.renderHuds: caster dial (+33), thaumometer (+80), then the sanity checker.
        int start = 0;
        boolean rC = false, rT = false;
        for (ItemStack hand : new ItemStack[] { player.getMainHandItem(), player.getOffhandItem() }) {
            if (hand.isEmpty()) continue;
            if (!rC && hand.getItem() instanceof CasterItem) { rC = true; start += 33; }
            else if (!rT && hand.getItem() instanceof ThaumometerItem) { rT = true; start += 80; }
            else if (hand.getItem() instanceof SanityCheckerItem) { renderSanityHud(g, player, start); return; }
        }
    }

    /** HudHandler.renderSanityHud: three stacked bars (temporary, normal, permanent) over 48 px, shaking above 100. */
    static void renderSanityHud(net.minecraft.client.gui.GuiGraphicsExtractor g, Player player, int shifty) {
        var research = ClientResearch.progress();
        int p = research.permanentWarp(), s = research.normalWarp(), t = research.temporaryWarp();
        g.pose().pushMatrix();
        g.pose().translate(0, shifty);
        quad(g, 1, 1, 152, 0, 20, 76, -1);
        float tw = p + s + t;
        float mod = 1.0f;
        if (tw > 100.0f) { mod = 100.0f / tw; tw = 100.0f; }
        int gap = (int) ((100.0f - tw) / 100.0f * 48.0f);
        int wt = (int) (t / 100.0f * 48.0f * mod);
        int ws = (int) (s / 100.0f * 48.0f * mod);
        if (t > 0) quad(g, 7, 21 + gap, 200, gap, 8, wt + gap, ARGB.colorFromFloat(1.0f, 1.0f, 0.5f, 1.0f));
        if (s > 0) quad(g, 7, 21 + wt + gap, 200, wt + gap, 8, wt + ws + gap, ARGB.colorFromFloat(1.0f, 0.75f, 0.0f, 0.75f));
        if (p > 0) quad(g, 7, 21 + wt + ws + gap, 200, wt + ws + gap, 8, 48, ARGB.colorFromFloat(1.0f, 0.5f, 0.0f, 0.5f));
        quad(g, 1, 1, 176, 0, 20, 76, -1);
        if (tw >= 100.0f) {
            g.pose().pushMatrix();
            g.pose().scale(0.75f, 0.75f);
            g.pose().translate(player.getRandom().nextInt(2), player.getRandom().nextInt(2));
            quad(g, 3, 3, 216, 0, 20, 16, -1);
            g.pose().popMatrix();
        }
        g.pose().popMatrix();
    }

    /** UtilsFX.drawTexturedQuad(x, y, u, v, w, h) on the 256px HUD sheet. */
    private static void quad(net.minecraft.client.gui.GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h, int color) {
        g.blit(RenderPipelines.GUI_TEXTURED, HUD, x, y, u, v, w, h, 256, 256, color);
    }
}
