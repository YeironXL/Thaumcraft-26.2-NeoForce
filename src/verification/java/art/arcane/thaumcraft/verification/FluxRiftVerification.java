package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.blockentity.StabilizerBlockEntity;
import art.arcane.thaumcraft.entity.FluxRiftEntity;
import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** TC6 EntityFluxRift: aura trigger spawn, spine/size, growth on flux, stability bands, stabiliser mitigation and collapse drops. */
public final class FluxRiftVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }

    public static void test(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos origin = h.absolutePos(new BlockPos(8, 2, 8));
        var chunk = level.getChunkAt(origin);
        var aura = NativeAura.get(level, chunk);
        BlockPos chunkOrigin = new BlockPos(chunk.getPos().getMinBlockX(), 0, chunk.getPos().getMinBlockZ());
        AABB chunkBox = new AABB(chunkOrigin.getX() - 40, level.getMinY(), chunkOrigin.getZ() - 40, chunkOrigin.getX() + 56, level.getMaxY(), chunkOrigin.getZ() + 56);
        level.getEntitiesOfClass(FluxRiftEntity.class, chunkBox).forEach(FluxRiftEntity::discard);

        // --- aura trigger -> createRift: size sqrt(flux*3), drains that much flux
        aura.setFlux(300.0f);
        NativeAura.RIFT_TRIGGER.put(level, chunkOrigin);
        FluxRiftEntity.createRift(level, NativeAura.RIFT_TRIGGER.remove(level));
        var spawned = level.getEntitiesOfClass(FluxRiftEntity.class, chunkBox);
        check(spawned.size() == 1, "Aura trigger did not create exactly one rift: " + spawned.size());
        check(spawned.get(0).getRiftSize() == 30, "Rift size from 300 flux: " + spawned.get(0).getRiftSize());
        check(Math.abs(aura.flux() - (300.0f - (float) Math.sqrt(900.0))) < 0.01, "Rift did not drain flux: " + aura.flux());
        FluxRiftEntity.createRift(level, chunkOrigin);
        var after = level.getEntitiesOfClass(FluxRiftEntity.class, chunkBox);
        // TC6 only forbids a second rift within 32 blocks of the chosen spot (heights can differ across gametest structures)
        for (var a : after) for (var b : after) check(a == b || a.distanceToSqr(b) > 1024.0, "Second rift spawned within 32 blocks");
        after.stream().filter(r -> r != spawned.get(0)).forEach(FluxRiftEntity::discard);
        spawned.get(0).discard();
        aura.setFlux(0.0f);
        FluxRiftEntity.createRift(level, chunkOrigin);
        check(level.getEntitiesOfClass(FluxRiftEntity.class, chunkBox, e -> e.isAlive()).isEmpty(), "Rift spawned without flux");

        // --- geometry: 2*ceil(size/3)+2 spine points, bounding box around them
        var rift = new FluxRiftEntity(ModEntities.FLUX_RIFT.get(), level);
        Vec3 c = Vec3.atCenterOf(h.absolutePos(new BlockPos(8, 8, 8)));
        rift.snapTo(c.x, c.y, c.z, 0, 0);
        rift.setRiftSeed(12345);
        level.addFreshEntity(rift);
        rift.setRiftSize(20);
        check(rift.points.size() == 2 * 7 + 2 && rift.pointsWidth.size() == rift.points.size(), "Spine points: " + rift.points.size());
        check(rift.getBoundingBox().getXsize() > 0.1 && rift.getBoundingBox().contains(rift.points.get(3).add(c)) , "Bounding box does not wrap the spine");
        check(rift.pointsWidth.get(0) == 0.0f && rift.pointsWidth.get(rift.points.size() - 1) == 0.0f, "Spine ends are not pointed");

        // --- stability bands
        rift.setRiftStability(60); check(rift.getStability() == FluxRiftEntity.EnumStability.VERY_STABLE, "VERY_STABLE band");
        rift.setRiftStability(0); check(rift.getStability() == FluxRiftEntity.EnumStability.STABLE, "STABLE band");
        rift.setRiftStability(-10); check(rift.getStability() == FluxRiftEntity.EnumStability.UNSTABLE, "UNSTABLE band");
        rift.setRiftStability(-30); check(rift.getStability() == FluxRiftEntity.EnumStability.VERY_UNSTABLE, "VERY_UNSTABLE band");
        rift.setRiftStability(-500); check(rift.getRiftStability() == -100, "Stability not clamped");

        // --- growth: every 600 ticks it eats sqrt(size*2) flux and grows by one
        rift.setRiftStability(0);
        NativeAura.get(level, level.getChunkAt(rift.blockPosition())).setFlux(50.0f);
        rift.tickCount = rift.getId() % 600 == 0 ? 600 : rift.getId() % 600;
        rift.tick();
        check(rift.getRiftSize() == 21, "Rift did not grow on flux: " + rift.getRiftSize());
        check(Math.abs(NativeAura.flux(level, rift.blockPosition()) - (50.0f - (float) Math.sqrt(40))) < 0.01, "Growth did not drain flux");

        // --- stabiliser mitigation (TileStabilizer.tryAddStability)
        BlockPos st = new BlockPos(8, 2, 3);
        h.setBlock(st.below(), Blocks.STONE);
        h.setBlock(st, ModBlocks.STABILIZER.get());
        var stab = h.getBlockEntity(st, StabilizerBlockEntity.class);
        rift.setRiftStability(60);
        for (int t = 0; t < 20 * 15; t++) StabilizerBlockEntity.tick(level, h.absolutePos(st), stab.getBlockState(), stab);
        check(stab.getEnergy() == 15 && rift.getRiftStability() == 60, "Stabiliser spent energy on a very stable rift");
        rift.setRiftStability(-10);
        for (int t = 0; t < 5; t++) StabilizerBlockEntity.tick(level, h.absolutePos(st), stab.getBlockState(), stab);
        check(Math.abs(rift.getRiftStability() + 9.875f) < 1e-4 && stab.getEnergy() == 14, "Stabiliser mitigation: stab=" + rift.getRiftStability() + " energy=" + stab.getEnergy());
        for (int t = 0; t < 4; t++) StabilizerBlockEntity.tick(level, h.absolutePos(st), stab.getBlockState(), stab);
        check(Math.abs(rift.getRiftStability() + 9.875f) < 1e-4, "Stabiliser ignored its 5 tick delay");

        // --- collapse: shrinks one per tick, then drops sqrt(maxSize) void seeds
        rift.setRiftStability(60);
        rift.setCollapse(true);
        for (int i = 0; i < 40 && rift.isAlive(); i++) rift.tick();
        check(rift.isRemoved(), "Collapsing rift did not vanish");
        long seeds = level.getEntitiesOfClass(ItemEntity.class, new AABB(c, c).inflate(4), e -> e.getItem().is(ModItems.VOID_SEED.get())).stream().mapToInt(e -> e.getItem().getCount()).sum();
        check(seeds == 4, "Collapse void seeds: " + seeds);
        level.getEntitiesOfClass(ItemEntity.class, new AABB(c, c).inflate(6)).forEach(ItemEntity::discard);
        System.out.println("Flux rift passed: aura spawn, spine, stability, growth, stabiliser and collapse.");
        h.succeed();
    }

    private FluxRiftVerification() {}
}
