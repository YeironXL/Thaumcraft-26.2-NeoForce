package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.api.IGogglesDisplayExtended;
import art.arcane.thaumcraft.aspect.ContainerAspects;
import art.arcane.thaumcraft.block.RedstoneRelayBlock;
import art.arcane.thaumcraft.blockentity.ArcaneEarBlockEntity;
import art.arcane.thaumcraft.blockentity.RedstoneRelayBlockEntity;
import art.arcane.thaumcraft.network.NotePayload;
import art.arcane.thaumcraft.util.Baubles;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

/**
 * TC6 RenderEventHandler.blockHighlight: the redstone relay's "Out:"/"In:" for the targeted dial, and with goggles of
 * revealing the IGogglesDisplayExtended lines, an aspect container's tags (drawTagsOnContainer, tagscale up to 0.3)
 * and a note block's / arcane ear's "Note: N" (asking the server with PacketNote every 5 ticks).
 * The blockTags branch is dead code in TC6 (never filled) and is not ported.
 */
public final class GogglesTextClient {
    public static void register() { NeoForge.EVENT_BUS.addListener(GogglesTextClient::render); }

    private static void render(SubmitCustomGeometryEvent e) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        var bp = hit.getBlockPos();
        int x = bp.getX(), y = bp.getY(), z = bp.getZ();
        var te = mc.level.getBlockEntity(bp);
        var state = mc.level.getBlockState(bp);
        if (te instanceof RedstoneRelayBlockEntity relay && state.getBlock() instanceof RedstoneRelayBlock) {
            int sub = RedstoneRelayBlock.dial(state, bp, hit.getLocation());
            if (sub == 0) AspectTagsRender.drawTextInAir(e, x, y + 0.3, z, Component.literal("Out: " + relay.getOut()));
            else if (sub == 1) AspectTagsRender.drawTextInAir(e, x, y + 0.3, z, Component.literal("In: " + relay.getIn()));
        }
        if (!Baubles.hasGoggles(mc.player)) return;
        Object src = te instanceof IGogglesDisplayExtended ? te : state.getBlock();
        if (src instanceof IGogglesDisplayExtended ext) {
            List<Component> lines = ext.getIGogglesText();
            var v = ext.getIGogglesTextOffset();
            // TC6 subtracts the line offset for tile entities and adds it for blocks
            float sign = src == te ? -1 : 1;
            for (int to = 0; to < lines.size(); ++to)
                AspectTagsRender.drawTextInAir(e, x + v.x, y + v.y + sign * (to - lines.size() / 2.0f) / 5.5f, z + v.z, lines.get(to));
        }
        boolean spaceAbove = mc.level.getBlockState(bp.above()).isAir();
        int note = -1;
        if (state.getBlock() instanceof NoteBlock) note = state.getValue(NoteBlock.NOTE);
        else if (te instanceof ArcaneEarBlockEntity ear) note = ear.note;
        else if (te != null) {
            var tags = ContainerAspects.of(te);
            if (tags != null && !tags.isEmpty()) {
                AspectTagsRender.grow(0.3f);
                AspectTagsRender.drawTagsOnContainer(e, x, y + (spaceAbove ? 0.4f : 0.0f), z, tags, spaceAbove ? Direction.UP : hit.getDirection());
            }
        }
        if (note >= 0) {
            if (mc.player.tickCount % 5 == 0 && lastAsk != mc.player.tickCount) {
                lastAsk = mc.player.tickCount;
                ClientPacketDistributor.sendToServer(new NotePayload(bp, (byte) -1));
            }
            AspectTagsRender.drawTextInAir(e, x, y + 1, z, Component.literal("Note: " + note));
        }
    }

    /** TC6 sent once per highlight frame on every 5th tick; once per such tick here. */
    private static int lastAsk = -1;

    private GogglesTextClient() {}
}
