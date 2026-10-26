package art.arcane.thaumcraft.block.eldritch;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lock state: the boss room centre and a seed. Unlocking picks the boss at random (TC4: the room is randomised when
 * the tablet goes in): Eldritch Warden, Eldritch Construct, Greater Crimson Portal or Giant Taintacle.
 */
public final class EldritchLockBlockEntity extends BlockEntity {
    public static final String[] BOSSES = {"warden", "golem", "crimson_portal", "taintacle"};
    private BlockPos room = BlockPos.ZERO;
    private long seed;
    private int opening = -1;
    private String boss = "";

    public EldritchLockBlockEntity(BlockPos pos, BlockState s) { super(ModBlockEntities.ELDRITCH_LOCK.get(), pos, s); }
    public void configure(BlockPos room, long seed) { this.room = room; this.seed = seed; setChanged(); }
    public boolean opening() { return opening >= 0; }
    public String boss() { return boss; }
    public BlockPos room() { return room; }

    public void unlock() {
        opening = 0;
        boss = BOSSES[new Random(seed).nextInt(BOSSES.length)];
        setChanged();
    }

    /** The 5x5 Ancient Doorway around the lock, outermost blocks first. */
    private List<BlockPos> door() {
        Direction side = getBlockState().getValue(EldritchLockBlock.FACING).getClockWise();
        List<BlockPos> l = new ArrayList<>();
        for (int a = -2; a <= 2; a++) for (int y = -2; y <= 2; y++) if (a != 0 || y != 0) l.add(worldPosition.relative(side, a).above(y));
        l.sort((p, q) -> Integer.compare(q.distManhattan(worldPosition), p.distManhattan(worldPosition)));
        return l;
    }

    public static void tick(Level level, BlockPos pos, BlockState s, EldritchLockBlockEntity be) {
        if (be.opening < 0 || !(level instanceof ServerLevel w)) return;
        be.opening++;
        List<BlockPos> door = be.door();
        int step = be.opening / 3;
        if (be.opening % 3 == 0 && step <= door.size()) {
            BlockPos q = step < door.size() ? door.get(step) : pos;
            if (level.getBlockState(q).is(ModBlocks.ELDRITCH_DOOR.get()) || q.equals(pos)) {
                w.levelEvent(2001, q, net.minecraft.world.level.block.Block.getId(level.getBlockState(q)));
                if (!q.equals(pos)) level.setBlock(q, Blocks.AIR.defaultBlockState(), 3);
            }
            if (q.equals(pos)) {
                be.spawnBoss(w);
                level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6f, 0.7f);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private void spawnBoss(ServerLevel w) {
        EntityType<? extends Mob> type = switch (boss) {
            case "golem" -> ModEntities.ELDRITCH_GOLEM.get();
            case "crimson_portal" -> ModEntities.CULTIST_PORTAL_GREATER.get();
            case "taintacle" -> ModEntities.TAINTACLE_GIANT.get();
            default -> ModEntities.ELDRITCH_WARDEN.get();
        };
        // decorative ancient obelisks (TC4: two in the Warden's room, three in the Construct's)
        int obelisks = boss.equals("warden") ? 2 : boss.equals("golem") ? 3 : 0;
        for (int i = 0; i < obelisks; i++) {
            double a = Math.PI * 2 * i / obelisks + 0.4;
            BlockPos o = room.offset((int) Math.round(Math.cos(a) * 4), 0, (int) Math.round(Math.sin(a) * 4));
            w.setBlock(o, ModBlocks.ELDRITCH_OBELISK_BASE.get().defaultBlockState().setValue(EldritchObeliskBlock.ANCIENT, true), 3);
            w.setBlock(o.above(), ModBlocks.ELDRITCH_OBELISK_BODY.get().defaultBlockState().setValue(EldritchObeliskBlock.ANCIENT, true), 3);
            w.setBlock(o.above(2), ModBlocks.ELDRITCH_CAPSTONE.get().defaultBlockState().setValue(EldritchObeliskBlock.ANCIENT, true), 3);
        }
        Mob m = type.create(w, EntitySpawnReason.STRUCTURE);
        if (m == null) return;
        double y = boss.equals("crimson_portal") ? room.getY() + 1 : room.getY();
        m.snapTo(room.getX() + 0.5, y, room.getZ() + 0.5, 0f, 0f);
        m.finalizeSpawn(w, w.getCurrentDifficultyAt(room), EntitySpawnReason.STRUCTURE, null);
        m.setPersistenceRequired();
        w.addFreshEntity(m);
    }

    @Override protected void saveAdditional(ValueOutput o) {
        super.saveAdditional(o);
        o.store("room", BlockPos.CODEC, room); o.putLong("seed", seed); o.putInt("opening", opening); o.putString("boss", boss);
    }
    @Override protected void loadAdditional(ValueInput i) {
        super.loadAdditional(i);
        room = i.read("room", BlockPos.CODEC).orElse(BlockPos.ZERO); seed = i.getLongOr("seed", 0); opening = i.getIntOr("opening", -1); boss = i.getStringOr("boss", "");
    }
}
