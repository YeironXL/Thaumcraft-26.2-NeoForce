package art.arcane.thaumcraft.block.eldritch;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.world.outer.MazeData;
import art.arcane.thaumcraft.world.outer.OuterLands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * Eldritch Altar state. Four Eldritch Eyes start the ritual; when it completes an Eldritch Portal opens between the
 * altar and the floating obelisk, bound to this altar's own maze. Each eye raises the Eldritch Guardian spawn rate,
 * also after the portal is open. A naturally generated altar is guarded by Crimson Knights and a Cleric.
 */
public final class EldritchAltarBlockEntity extends BlockEntity {
    public static final int RITUAL_TICKS = 100;
    private int eyes;
    private int ritual = -1;
    private boolean open;
    private boolean natural, guarded;

    public EldritchAltarBlockEntity(BlockPos pos, BlockState s) { super(ModBlockEntities.ELDRITCH_ALTAR.get(), pos, s); }
    public int eyes() { return eyes; }
    public boolean isOpen() { return open; }
    public boolean ritualRunning() { return ritual >= 0; }
    public void markNatural() { natural = true; setChanged(); }

    public boolean insertEye() {
        if (eyes >= 4 || open) return false;
        eyes++;
        if (eyes == 4) ritual = 0;
        setChanged();
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState s, EldritchAltarBlockEntity be) {
        if (!(level instanceof ServerLevel w)) return;
        if (be.natural && !be.guarded) { be.guarded = true; be.setChanged(); be.spawnCultists(w); }
        if (be.ritual >= 0) {
            be.ritual++;
            if (be.ritual % 5 == 0) w.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, 20, 0.5, 1.0, 0.5, 0.5);
            if (be.ritual >= RITUAL_TICKS) be.openPortal(w);
        }
        if (be.eyes > 0 && w.getGameTime() % 200 == 0 && w.getRandom().nextInt(8) < be.eyes) be.spawnGuardian(w);
    }

    public void openPortal(ServerLevel w) {
        ritual = -1;
        open = true;
        MazeData.Maze m = MazeData.get(w.getServer()).forAltar(GlobalPos.of(w.dimension(), worldPosition), w.getRandom().nextLong());
        for (int y = 1; y <= 2; y++) {
            w.setBlock(worldPosition.above(y), ModBlocks.ELDRITCH_PORTAL.get().defaultBlockState(), 3);
            if (w.getBlockEntity(worldPosition.above(y)) instanceof EldritchPortalBlockEntity pe) pe.configure(m.id(), false);
        }
        w.playSound(null, worldPosition, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0f, 0.8f);
        setChanged();
    }

    private void spawnGuardian(ServerLevel w) {
        if (OuterLands.is(w) || w.getNearestPlayer(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 32, false) == null) return;
        if (w.getEntitiesOfClass(art.arcane.thaumcraft.entity.monster.EldritchGuardianEntity.class, new AABB(worldPosition).inflate(32)).size() >= 3) return;
        spawn(w, ModEntities.ELDRITCH_GUARDIAN.get().create(w, EntitySpawnReason.EVENT), 6);
    }

    private void spawnCultists(ServerLevel w) {
        spawn(w, ModEntities.CULTIST_KNIGHT.get().create(w, EntitySpawnReason.STRUCTURE), 3);
        spawn(w, ModEntities.CULTIST_KNIGHT.get().create(w, EntitySpawnReason.STRUCTURE), 3);
        spawn(w, ModEntities.CULTIST_CLERIC.get().create(w, EntitySpawnReason.STRUCTURE), 2);
    }

    private void spawn(ServerLevel w, Mob m, int r) {
        if (m == null) return;
        var rand = w.getRandom();
        BlockPos p = worldPosition.offset(rand.nextInt(r * 2 + 1) - r, 0, rand.nextInt(r * 2 + 1) - r);
        for (int i = 0; i < 4 && !w.getBlockState(p).isAir(); i++) p = p.above();
        m.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, rand.nextFloat() * 360f, 0f);
        m.finalizeSpawn(w, w.getCurrentDifficultyAt(p), EntitySpawnReason.STRUCTURE, null);
        if (m instanceof Enemy) m.setPersistenceRequired();
        w.addFreshEntity(m);
    }

    @Override protected void saveAdditional(ValueOutput o) {
        super.saveAdditional(o);
        o.putInt("eyes", eyes); o.putInt("ritual", ritual); o.putBoolean("open", open); o.putBoolean("natural", natural); o.putBoolean("guarded", guarded);
    }
    @Override protected void loadAdditional(ValueInput i) {
        super.loadAdditional(i);
        eyes = i.getIntOr("eyes", 0); ritual = i.getIntOr("ritual", -1); open = i.getBooleanOr("open", false); natural = i.getBooleanOr("natural", false); guarded = i.getBooleanOr("guarded", false);
    }
}
