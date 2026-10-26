package art.arcane.thaumcraft.block.eldritch;

import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Crusted Opening spawner: while a player is within 16 blocks, every 15-30 s a crab crawls out of an open face,
 * up to {@link #MAX_NEARBY} crabs around the opening.
 */
public final class EldritchCrabSpawnerBlockEntity extends BlockEntity {
    public static final int RANGE = 16, MAX_NEARBY = 4;
    private int delay = 100;

    public EldritchCrabSpawnerBlockEntity(BlockPos pos, BlockState s) { super(ModBlockEntities.ELDRITCH_CRAB_SPAWNER.get(), pos, s); }

    public static void tick(Level level, BlockPos pos, BlockState s, EldritchCrabSpawnerBlockEntity be) {
        if (!(level instanceof ServerLevel w) || --be.delay > 0) return;
        be.delay = 300 + w.getRandom().nextInt(300);
        if (w.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, new AABB(pos).inflate(RANGE), p -> !p.isSpectator() && !p.isCreative()).isEmpty()) return;
        be.spawnCrab(w);
    }

    /** Pushes one crab out of the first free face; false if crowded or walled in. */
    public boolean spawnCrab(ServerLevel w) {
        BlockPos pos = worldPosition;
        if (w.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(RANGE), e -> e.getType() == ModEntities.ELDRITCH_CRAB.get()).size() >= MAX_NEARBY) return false;
        for (Direction d : Direction.Plane.HORIZONTAL.shuffledCopy(w.getRandom())) {
            BlockPos q = pos.relative(d);
            if (!w.getBlockState(q).getCollisionShape(w, q).isEmpty() || !w.getBlockState(q.above()).getCollisionShape(w, q.above()).isEmpty()) continue;
            Mob crab = ModEntities.ELDRITCH_CRAB.get().create(w, EntitySpawnReason.SPAWNER);
            if (crab == null) return false;
            crab.snapTo(q.getX() + 0.5, q.getY(), q.getZ() + 0.5, d.toYRot(), 0f);
            w.addFreshEntity(crab);
            w.sendParticles(ParticleTypes.LARGE_SMOKE, q.getX() + 0.5, q.getY() + 0.5, q.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
            w.playSound(null, pos, SoundEvents.SILVERFISH_STEP, SoundSource.HOSTILE, 1.0f, 0.5f);
            return true;
        }
        return false;
    }
}
