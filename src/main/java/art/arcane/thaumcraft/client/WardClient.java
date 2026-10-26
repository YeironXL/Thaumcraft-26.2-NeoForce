package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.client.fx.beam.TcBeams;
import art.arcane.thaumcraft.registry.ModAttachments;
import art.arcane.thaumcraft.util.Baubles;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;

/** TC4 warded blocks: a faint shimmering rune overlay on every exposed face, visible only with goggles of revealing. */
public final class WardClient {
    private static final Identifier TEX = Identifier.parse("thaumcraft:textures/misc/ward_overlay.png");
    public static void register() { NeoForge.EVENT_BUS.addListener(WardClient::render); }

    private static void render(SubmitCustomGeometryEvent e) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !Baubles.hasRevealer(mc.player)) return;
        var list = new ArrayList<BlockPos>();
        int cx = mc.player.blockPosition().getX() >> 4, cz = mc.player.blockPosition().getZ() >> 4;
        for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) {
            var c = mc.level.getChunkSource().getChunk(x, z, false);
            if (c == null || !c.hasData(ModAttachments.WARDS)) continue;
            for (long l : c.getData(ModAttachments.WARDS).list()) {
                BlockPos p = BlockPos.of(l);
                if (p.distSqr(mc.player.blockPosition()) < 32 * 32 && !mc.level.getBlockState(p).isAir()) list.add(p);
            }
        }
        if (list.isEmpty()) return;
        Vec3 cam = e.getLevelRenderState().cameraRenderState.pos;
        float pulse = 0.2f + 0.08f * Mth.sin(mc.player.tickCount / 8.0f);
        int argb = ((int) (pulse * 255) << 24) | 0x6FA8FF;
        var set = new java.util.HashSet<>(list);
        e.getSubmitNodeCollector().submitCustomGeometry(e.getPoseStack(), TcBeams.overlayAdditive(TEX), (pose, vc) -> {
            for (BlockPos p : list) for (Direction face : Direction.values())
                if (!set.contains(p.relative(face))) CasterClient.faceQuad(pose, vc, Vec3.atCenterOf(p).subtract(cam).add(new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.002)), face, (p.getX() + p.getY() + p.getZ()) % 4 * 90, argb);
        });
    }
    private WardClient() {}
}
