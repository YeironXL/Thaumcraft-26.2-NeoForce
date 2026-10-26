package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.aspect.ContainerAspects;
import art.arcane.thaumcraft.blockentity.ArcaneEarBlockEntity;
import art.arcane.thaumcraft.blockentity.CrucibleBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaTubeBlockEntity;
import art.arcane.thaumcraft.network.NotePayload;
import io.netty.buffer.Unpooled;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;

/** Goggles of revealing block overlay data: IAspectContainer mapping (drawTagsOnContainer) and PacketNote. */
public final class GogglesVerification {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    public static void test(GameTestHelper h) {
        var world = h.getLevel();
        // PacketNote: codec round trip, server note lookup for note blocks and arcane ears
        var buf = Unpooled.buffer();
        NotePayload.CODEC.encode(buf, new NotePayload(new BlockPos(1, -2, 3), (byte) -1));
        var back = NotePayload.CODEC.decode(buf);
        check(back.pos().equals(new BlockPos(1, -2, 3)) && back.note() == -1, "NotePayload codec round trip failed");
        BlockPos noteRel = new BlockPos(1, 2, 1);
        h.setBlock(noteRel, Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.NOTE, 13));
        check(NotePayload.noteAt(world, h.absolutePos(noteRel)) == 13, "Note block note not read");
        BlockPos earRel = new BlockPos(3, 2, 1);
        h.setBlock(earRel, ModBlocks.ARCANE_EAR.get());
        var ear = (ArcaneEarBlockEntity) world.getBlockEntity(h.absolutePos(earRel));
        ear.note = 7;
        check(NotePayload.noteAt(world, h.absolutePos(earRel)) == 7, "Arcane ear note not read");
        check(NotePayload.noteAt(world, h.absolutePos(new BlockPos(5, 2, 1))) == -1, "Air should have no note");
        // aspect containers
        BlockPos jarRel = new BlockPos(1, 2, 3);
        h.setBlock(jarRel, ModBlocks.JAR_NORMAL.get());
        var jar = (EssentiaJarBlockEntity) world.getBlockEntity(h.absolutePos(jarRel));
        check(ContainerAspects.of(jar).isEmpty(), "Empty jar must have no aspects");
        jar.insert("ignis", 32);
        check(ContainerAspects.of(jar).equals(Map.of(AspectRegistry.IGNIS, 32)), "Jar aspects wrong: " + ContainerAspects.of(jar));
        var tag = jar.getUpdateTag(world.registryAccess());
        check(tag.toString().contains("ignis"), "Jar update tag lacks contents");
        BlockPos cruRel = new BlockPos(3, 2, 3);
        h.setBlock(cruRel, ModBlocks.CRUCIBLE.get());
        var cru = (CrucibleBlockEntity) world.getBlockEntity(h.absolutePos(cruRel));
        cru.mixture().fill();
        for (int i = 0; i < 160; i++) cru.mixture().heatTick(true);
        var first = new java.util.LinkedHashMap<String, Integer>(); first.put("aqua", 5); first.put("terra", 2); cru.mixture().dissolve(first);
        var ca = ContainerAspects.of(cru);
        check(ca.get(AspectRegistry.AQUA) == 5 && ca.get(AspectRegistry.TERRA) == 2, "Crucible aspects wrong: " + ca);
        // TC6 AspectList insertion order survives save/sync
        var order = new java.util.LinkedHashMap<String, Integer>();
        for (String id : new String[] {"victus", "aqua", "metallum", "terra", "herba", "ordo", "ignis"}) order.put(id, 2);
        cru.mixture().dissolve(order);
        var reloaded = art.arcane.thaumcraft.crafting.AlchemyMixture.CODEC.codec().parse(net.minecraft.nbt.NbtOps.INSTANCE,
                art.arcane.thaumcraft.crafting.AlchemyMixture.CODEC.codec().encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, cru.mixture()).getOrThrow()).getOrThrow();
        check(java.util.List.copyOf(reloaded.aspects().keySet()).equals(java.util.List.of("aqua", "terra", "victus", "metallum", "herba", "ordo", "ignis")), "Crucible aspect order lost: " + reloaded.aspects().keySet());
        BlockPos filRel = new BlockPos(5, 2, 3);
        h.setBlock(filRel, ModBlocks.TUBE_FILTER.get());
        var fil = (EssentiaTubeBlockEntity) world.getBlockEntity(h.absolutePos(filRel));
        check(ContainerAspects.of(fil) == null, "Unset filter tube is no container");
        fil.setFilter("aer");
        check(ContainerAspects.of(fil).equals(Map.of(AspectRegistry.AER, -1)), "Filter tube must show its aspect without amount");
        BlockPos tubeRel = new BlockPos(5, 2, 5);
        h.setBlock(tubeRel, ModBlocks.TUBE.get());
        check(ContainerAspects.of(world.getBlockEntity(h.absolutePos(tubeRel))) == null, "Plain tubes are no aspect container in TC6");
        h.succeed();
    }

    private GogglesVerification() {}
}
