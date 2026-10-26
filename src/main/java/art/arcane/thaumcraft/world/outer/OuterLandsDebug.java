package art.arcane.thaumcraft.world.outer;

import art.arcane.thaumcraft.block.eldritch.EldritchAltarBlockEntity;
import art.arcane.thaumcraft.block.eldritch.EldritchLockBlockEntity;
import art.arcane.thaumcraft.worldgen.EldritchObeliskGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Set;

/**
 * Development aid (-Dthaumcraft.debugHold=first -Dthaumcraft.debugOuter=altar|altar_open|maze|corridor|lock|boss|loot|tablet|rune|spider|taint|pool|opening|crust|crystal|glow|autocaster|taintacles):
 * builds an Eldritch Obelisk next to the player, or sends the player into the Outer Lands (night vision for the
 * dark maze) and in front of the entry portal, a corridor, a locked boss door or an opened boss room.
 */
public final class OuterLandsDebug {
    public static boolean run(ServerPlayer p, String mode) {
        var server = ((ServerLevel) p.level()).getServer();
        ServerLevel level = server.overworld();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
        if (mode.startsWith("altar")) {
            BlockPos at = (p.level() == level ? p.blockPosition() : level.getRespawnData().pos()).offset(0, 0, -9);
            level.getChunk(at);
            BlockPos g = level.getHeightmapPos(Heightmap.Types.OCEAN_FLOOR, at).below();
            EldritchObeliskGen.generate(level, new java.util.Random(1), g);
            if (mode.equals("altar_open") && level.getBlockEntity(g.above()) instanceof EldritchAltarBlockEntity be) { for (int i = 0; i < 4; i++) be.insertEye(); be.openPortal(level); }
            p.teleportTo(level, g.getX() + 0.5, g.getY() + 2, g.getZ() + 9.5, Set.of(), 180f, 10f, true);
            return true;
        }
        long seed = 4242L;
        if (mode.equals("taintacles")) while (MazeGenerator.plan(seed).count(MazeGenerator.Kind.TAINTACLES) == 0) seed++;
        MazeData.Maze m = MazeData.get(server).forAltar(GlobalPos.of(level.dimension(), new BlockPos(0, -999, MazeData.VERSION * 1000 + (int) (seed - 4242L))), seed);
        ServerLevel outer = OuterLands.level(server);
        if (outer == null) { System.out.println("[DEBUGOUTER] no outer_lands dimension"); return false; }
        OuterLands.ensureGenerated(outer, m.id());
        System.out.println("[DEBUGOUTER] maze " + m.id() + " in " + outer.dimension() + " biome " + outer.getBiome(MazeGenerator.arrival(m)).getRegisteredName());
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 600, 0, false, false));
        BlockPos t; float yaw = 180f;
        switch (mode) {
            case "corridor" -> {
                BlockPos c = MazeGenerator.rooms(m, MazeGenerator.Kind.CORRIDOR).get(0);
                t = c; yaw = 0f;
            }
            case "lock", "boss" -> {
                BlockPos lock = MazeGenerator.locks(m).get(0);
                var facing = outer.getBlockState(lock).getValue(art.arcane.thaumcraft.block.eldritch.EldritchLockBlock.FACING);
                t = lock.relative(facing, 5).below(2);
                yaw = facing.getOpposite().toYRot();
                if (mode.equals("boss") && outer.getBlockEntity(lock) instanceof EldritchLockBlockEntity be) { be.unlock(); }
            }
            case "loot" -> { t = MazeGenerator.rooms(m, MazeGenerator.Kind.LOOT).get(0).offset(0, 0, 3); }
            case "tablet" -> { t = MazeGenerator.rooms(m, MazeGenerator.Kind.TABLET).get(0).offset(0, 0, 4); }
            case "rune" -> { t = MazeGenerator.rooms(m, MazeGenerator.Kind.RUNE).get(0).offset(0, 0, 3); }
            case "spider" -> { t = MazeGenerator.rooms(m, MazeGenerator.Kind.SPIDER).get(0).offset(0, 0, 4); }
            case "taintacles" -> { t = MazeGenerator.rooms(m, MazeGenerator.Kind.TAINTACLES).get(0).offset(0, 0, 4); yaw = 180f; }
            case "taint" -> { t = MazeGenerator.rooms(m, MazeGenerator.Kind.TAINT).get(0).offset(0, 0, 4); }
            case "pool", "opening" -> {
                t = MazeGenerator.arrival(m);
                for (BlockPos c : MazeGenerator.rooms(m, MazeGenerator.Kind.CORRIDOR)) {
                    boolean hit = mode.equals("pool") ? !outer.getBlockState(c.below()).getFluidState().isEmpty()
                            : BlockPos.betweenClosedStream(c.offset(-3, 0, -3), c.offset(3, 0, 3)).anyMatch(q -> outer.getBlockState(q).is(art.arcane.thaumcraft.ModBlocks.ELDRITCH_CRAB_SPAWNER.get()));
                    if (hit) { t = c.offset(0, 0, 0); break; }
                }
                if (mode.equals("opening")) {
                    BlockPos c = t;
                    BlockPos sp = BlockPos.betweenClosedStream(c.offset(-3, 0, -3), c.offset(3, 0, 3)).filter(q -> outer.getBlockState(q).is(art.arcane.thaumcraft.ModBlocks.ELDRITCH_CRAB_SPAWNER.get())).findFirst().map(BlockPos::immutable).orElse(c);
                    yaw = (float) (Math.toDegrees(Math.atan2(-(sp.getX() - c.getX()), sp.getZ() - c.getZ())));
                    t = c.offset(-(sp.getX() - c.getX()) / 2, 0, -(sp.getZ() - c.getZ()) / 2);
                } else t = t.offset(0, 0, 2);
            }
            case "crust" -> { t = MazeGenerator.rooms(m, MazeGenerator.Kind.CRAB).get(0).offset(0, 0, 3); }
            case "crystal", "glow", "autocaster" -> {
                t = MazeGenerator.arrival(m);
                BlockPos o = MazeGenerator.origin(m);
                BlockPos found = null;
                if (mode.equals("autocaster")) {
                    var l = outer.getEntitiesOfClass(art.arcane.thaumcraft.entity.monster.EldritchAutocasterEntity.class, new net.minecraft.world.phys.AABB(o.getX(), o.getY() - 2, o.getZ(), o.getX() + 144, o.getY() + 12, o.getZ() + 144));
                    if (!l.isEmpty()) found = l.get(0).blockPosition();
                } else {
                    var b = mode.equals("crystal") ? art.arcane.thaumcraft.ModBlocks.CRYSTAL_STRANGE.get() : art.arcane.thaumcraft.ModBlocks.CRUSTED_STONE_GLOW.get();
                    for (BlockPos q : BlockPos.betweenClosed(o, o.offset(143, 9, 143))) if (outer.getBlockState(q).is(b)) { found = q.immutable(); break; }
                }
                if (found != null) {
                    // stand 3 blocks away on a free side, looking at it
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        BlockPos s2 = new BlockPos(found.getX(), o.getY() + 1, found.getZ()).relative(d, 3);
                        if (outer.getBlockState(s2).isAir() && outer.getBlockState(s2.above()).isAir()) { t = s2; yaw = d.getOpposite().toYRot(); break; }
                    }
                }
            }
            default -> t = MazeGenerator.arrival(m);
        }
        outer.setChunkForced(t.getX() >> 4, t.getZ() >> 4, true);
        p.teleportTo(outer, t.getX() + 0.5, t.getY(), t.getZ() + 0.5, Set.of(), yaw, 0f, true);
        return true;
    }
    private OuterLandsDebug() {}
}
