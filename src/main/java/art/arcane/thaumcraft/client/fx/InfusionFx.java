package art.arcane.thaumcraft.client.fx;

import art.arcane.thaumcraft.blockentity.InfusionMatrixBlockEntity;
import art.arcane.thaumcraft.blockentity.PedestalBlockEntity;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import art.arcane.thaumcraft.network.FxPayload;
import art.arcane.thaumcraft.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;

/** Client side of the infusion altar: TileInfusionMatrix.doEffects helpers and the PacketFX* messages it sends. */
public final class InfusionFx {
    private static int tickCount;

    /** FXDispatcher.blockRunes (FXBlockRunes). */
    public static void blockRunes(Level level, double x, double y, double z, float r, float g, float b, int dur, float grav) {
        level.addParticle(ColorParticleOption.create(ModParticles.SCAN_RUNE.get(), ARGB.colorFromFloat(1, r, g, b)), x + 0.5, y + 0.5, z + 0.5, dur, grav, 0);
    }

    /** FXDispatcher.spark. */
    public static void spark(double x, double y, double z, float size, float r, float g, float b, float a) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var rand = level.getRandom();
        FXDispatcher.generic(x, y, z, 0, 0, 0, GenericParticleOptions.builder().maxAge(5 + rand.nextInt(5)).alpha(a).color(r, g, b).grid(16)
                .particles(8 + rand.nextInt(3) * 16, 8, 1).scale(size).flipped(rand.nextBoolean()).build(), 0);
    }

    /** drawInfusionParticles3 / 4: purple (pedestal) or green (player) motes flying into the matrix. */
    private static void boreSparkle(ClientLevel level, double x, double y, double z, BlockPos target, float r, float g, float b) {
        Minecraft.getInstance().particleEngine.add(new BoreParticles.Sparkle(level, x, y, z, target.getX() + 0.5, target.getY() - 0.5, target.getZ() + 0.5, r, g, b));
    }

    /** TileInfusionMatrix.doEffects source loop. */
    public static void matrixSources(InfusionMatrixBlockEntity te) {
        if (!(te.getLevel() instanceof ClientLevel level)) return;
        var rand = level.getRandom();
        BlockPos pos = te.getBlockPos();
        var it = te.sourceFX.entrySet().iterator();
        while (it.hasNext()) {
            var fx = it.next().getValue();
            if (fx.ticks <= 0) { it.remove(); continue; }
            if (fx.loc.equals(pos)) {
                Entity player = level.getEntity(fx.color);
                if (player != null) for (int a = 0; a < 4; ++a)
                    boreSparkle(level, player.getX() + (rand.nextFloat() - rand.nextFloat()) * player.getBbWidth(), player.getBoundingBox().minY + rand.nextFloat() * player.getBbHeight(),
                            player.getZ() + (rand.nextFloat() - rand.nextFloat()) * player.getBbWidth(), pos, 0.2f, 0.6f + rand.nextFloat() * 0.3f, 0.3f);
            } else if (level.getBlockEntity(fx.loc) instanceof PedestalBlockEntity ped) {
                var is = ped.item();
                if (!is.isEmpty()) {
                    if (rand.nextInt(3) == 0) {
                        boreSparkle(level, fx.loc.getX() + rand.nextFloat(), fx.loc.getY() + rand.nextFloat() + 1.0f, fx.loc.getZ() + rand.nextFloat(), pos,
                                0.4f + rand.nextFloat() * 0.2f, 0.2f, 0.6f + rand.nextFloat() * 0.3f);
                    } else {
                        boolean block = is.getItem() instanceof BlockItem;
                        for (int a = 0; a < 4; ++a) {
                            double px = block ? fx.loc.getX() + rand.nextFloat() : fx.loc.getX() + 0.4f + rand.nextFloat() * 0.2f;
                            double py = block ? fx.loc.getY() + rand.nextFloat() + 1.0f : fx.loc.getY() + 1.23f + rand.nextFloat() * 0.2f;
                            double pz = block ? fx.loc.getZ() + rand.nextFloat() : fx.loc.getZ() + 0.4f + rand.nextFloat() * 0.2f;
                            double s = block ? 0 : 0.03;
                            Minecraft.getInstance().particleEngine.add(new BoreParticles.Chip(level, px, py, pz, pos.getX() + 0.5, pos.getY() - 0.5, pos.getZ() + 0.5,
                                    rand.nextGaussian() * s, rand.nextGaussian() * s, rand.nextGaussian() * s, is));
                        }
                    }
                }
            } else fx.ticks = 0;
            --fx.ticks;
        }
    }

    /** TilePedestal.receiveClientEvent. */
    public static void pedestalEvent(BlockPos pos, int id) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockPos up = pos.above();
        switch (id) {
            case 11 -> FXDispatcher.drawBamf(up.getX() + 0.5, up.getY() + 0.5, up.getZ() + 0.5, 0.75f, 0.0f, 0.5f, true, true, null);
            case 12 -> FXDispatcher.drawBamf(up.getX() + 0.5, up.getY() + 0.5, up.getZ() + 0.5, 0.5f, 0.1f, 0.6f, true, true, null);
            case 5 -> FXDispatcher.drawPedestalShield(pos);
            default -> {}
        }
    }

    /** FXDispatcher handlers for the infusion packets. Returns true when handled. */
    public static boolean handle(FxPayload p) {
        var level = Minecraft.getInstance().level;
        if (level == null) return false;
        var a = p.args();
        switch (p.kind()) {
            case "block_arc" -> { // PacketFXBlockArc: arcLightning(target -> block centre)
                Minecraft.getInstance().particleEngine.add(new ArcParticle(level, a.get(0), a.get(1), a.get(2), p.x() + 0.5, p.y() + 0.5, p.z() + 0.5, a.get(3), a.get(4), a.get(5), 0.5));
                return true;
            }
            case "infusion_source" -> { // PacketFXInfusionSource
                if (level.getBlockEntity(BlockPos.containing(p.x(), p.y(), p.z())) instanceof InfusionMatrixBlockEntity te)
                    te.addSourceFX(BlockPos.containing(a.get(0), a.get(1), a.get(2)), a.get(3).intValue());
                return true;
            }
            case "essentia_source" -> { // PacketFXEssentiaSource, drawn by RenderEventHandler as essentiaTrailFx(source -> device)
                BlockPos start = BlockPos.containing(p.x(), p.y(), p.z());
                BlockPos end = start.offset(-a.get(0).intValue(), -a.get(1).intValue(), -a.get(2).intValue());
                int mod = level.getBlockEntity(start) instanceof InfusionMatrixBlockEntity ? -1 : 0;
                BlockPos to = start.above(mod);
                EssentiaStreamParticle.spawn(level, end.getX() + 0.5, end.getY() + 0.5, end.getZ() + 0.5, to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5,
                        ++tickCount, a.get(3).intValue(), 0.1f, a.get(4).intValue(), 0.0);
                return true;
            }
            case "scan_source" -> { startScan(level, BlockPos.containing(p.x(), p.y(), p.z()), a.get(0).intValue()); return true; }
            default -> { return false; }
        }
    }

    /** PacketFXScanSource.startScan: one glowing marker per connected ore vein, coloured by ore and delayed by distance. */
    private static void startScan(ClientLevel world, BlockPos pos, int r) {
        int range = 4 + r * 4;
        var positions = new java.util.LinkedHashSet<BlockPos>();
        for (int xx = -range; xx <= range; ++xx) for (int yy = -range; yy <= range; ++yy) for (int zz = -range; zz <= range; ++zz) {
            BlockPos q = pos.offset(xx, yy, zz);
            if (world.getBlockState(q).is(net.neoforged.neoforge.common.Tags.Blocks.ORES)) positions.add(q);
        }
        while (!positions.isEmpty()) {
            BlockPos start = positions.iterator().next();
            positions.remove(start);
            var coll = new java.util.ArrayList<BlockPos>();
            coll.add(start);
            calcGroup(world, start, coll, positions);
            int c = oreColor(world, start);
            double x = 0, y = 0, z = 0;
            for (BlockPos p2 : coll) { x += p2.getX() + 0.5; y += p2.getY() + 0.5; z += p2.getZ() + 0.5; }
            x /= coll.size(); y /= coll.size(); z /= coll.size();
            double dis = Math.sqrt(net.minecraft.world.phys.Vec3.atCenterOf(pos).distanceToSqr(x, y, z));
            float cr = ((c >> 16) & 255) / 255f, cg = ((c >> 8) & 255) / 255f, cb = (c & 255) / 255f;
            float q = (cr + cg + cb) / 3.0f;
            FXDispatcher.generic(x, y, z, 0, 0, 0, GenericParticleOptions.builder().maxAge(44).color(cr, cg, cb).alpha(0.0f, 1.0f, 0.8f, 0.0f)
                    .particles(240, 15, 1).grid(16).loop(true).scale(9.0f).layer(q < 0.25f ? 3 : 2).rotation(0.0f).build(), (int) (dis * 3.0));
        }
    }

    private static void calcGroup(ClientLevel world, BlockPos start, java.util.List<BlockPos> coll, java.util.Set<BlockPos> positions) {
        var bs = world.getBlockState(start);
        for (int x = -1; x <= 1; ++x) for (int y = -1; y <= 1; ++y) for (int z = -1; z <= 1; ++z) {
            BlockPos t = start.offset(x, y, z);
            if (world.getBlockState(t).equals(bs) && positions.remove(t)) {
                coll.add(t);
                if (positions.isEmpty()) return;
                calcGroup(world, t, coll, positions);
            }
        }
    }

    /** PacketFXScanSource.getOreColor (ore dictionary names, matched on the block id here). */
    private static int oreColor(ClientLevel world, BlockPos pos) {
        String n = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock()).getPath().toUpperCase(java.util.Locale.ROOT);
        String[][] table = {{"IRON", "14200723"}, {"COAL", "1052688"}, {"REDSTONE", "16711680"}, {"GOLD", "16576075"}, {"LAPIS", "1328572"}, {"DIAMOND", "6155509"},
                {"EMERALD", "1564002"}, {"QUARTZ", "15064789"}, {"SILVER", "14342653"}, {"TIN", "15724539"}, {"COPPER", "16620629"}, {"AMBER", "16626469"}, {"CINNABAR", "10159368"}};
        for (String[] e : table) if (n.contains(e[0])) return Integer.parseInt(e[1]);
        return 12632256;
    }

    private InfusionFx() {}
}
