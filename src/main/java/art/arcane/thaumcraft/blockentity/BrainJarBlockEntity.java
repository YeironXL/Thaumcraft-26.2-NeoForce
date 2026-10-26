package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.registry.ModBlockEntities;
import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 TileJarBrain: pulls experience orbs within 8 blocks towards itself and eats those touching it, up to 2000 xp.
 * Client side the brain turns to face the nearest player (or the orb it is pulling) and sighs now and then.
 */
public final class BrainJarBlockEntity extends BlockEntity {
    public static final int XP_MAX = 2000;
    public int xp, eatDelay;
    public float rota, rotb, target;
    private long lastSigh = System.currentTimeMillis() + 1500L;

    public BrainJarBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.BRAIN_JAR.get(), pos, state); }

    public static void tick(Level world, BlockPos pos, BlockState state, BrainJarBlockEntity te) {
        Entity entity = null;
        if (te.xp > XP_MAX) te.xp = XP_MAX;
        if (te.xp < XP_MAX) {
            entity = te.closestOrb();
            if (entity != null && te.eatDelay == 0) {
                double dx = (pos.getX() + 0.5 - entity.getX()) / 25.0, dy = (pos.getY() + 0.5 - entity.getY()) / 25.0, dz = (pos.getZ() + 0.5 - entity.getZ()) / 25.0;
                double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double f = 1.0 - d;
                if (f > 0.0) {
                    f *= f;
                    entity.setDeltaMovement(entity.getDeltaMovement().add(dx / d * f * 0.3, dy / d * f * 0.5, dz / d * f * 0.3));
                }
            }
        }
        if (world.isClientSide()) {
            te.rotb = te.rota;
            if (entity == null) {
                entity = world.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6.0, false);
                if (entity != null && te.lastSigh < System.currentTimeMillis()) {
                    world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.get("brain"), SoundSource.AMBIENT, 0.15f, 0.8f + world.getRandom().nextFloat() * 0.4f, false);
                    te.lastSigh = System.currentTimeMillis() + 5000L + world.getRandom().nextInt(25000);
                }
            }
            if (entity != null) te.target = (float) Math.atan2(entity.getZ() - (pos.getZ() + 0.5f), entity.getX() - (pos.getX() + 0.5f));
            else te.target += 0.01f;
            te.rota = wrap(te.rota);
            te.target = wrap(te.target);
            te.rota += wrap(te.target - te.rota) * 0.04f;
        }
        if (te.eatDelay > 0) { --te.eatDelay; return; }
        if (world.isClientSide() || te.xp >= XP_MAX) return;
        List<ExperienceOrb> orbs = world.getEntitiesOfClass(ExperienceOrb.class, new AABB(pos.getX() - 0.1, pos.getY() - 0.1, pos.getZ() - 0.1, pos.getX() + 1.1, pos.getY() + 1.1, pos.getZ() + 1.1));
        if (orbs.isEmpty()) return;
        for (ExperienceOrb eo : orbs) {
            te.xp += eo.getValue();
            eo.playSound(SoundEvents.GENERIC_EAT.value(), 0.1f, (world.getRandom().nextFloat() - world.getRandom().nextFloat()) * 0.2f + 1.0f);
            eo.discard();
        }
        te.sync();
    }

    private static float wrap(float a) { while (a >= (float) Math.PI) a -= (float) (2 * Math.PI); while (a < -(float) Math.PI) a += (float) (2 * Math.PI); return a; }

    private Entity closestOrb() {
        Entity orb = null;
        double best = Double.MAX_VALUE;
        Vec3 c = Vec3.atCenterOf(worldPosition);
        for (ExperienceOrb eo : level.getEntitiesOfClass(ExperienceOrb.class, new AABB(worldPosition).inflate(8.0))) {
            double d = eo.distanceToSqr(c);
            if (d < best) { best = d; orb = eo; }
        }
        return orb;
    }

    public void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }

    @Override protected void collectImplicitComponents(DataComponentMap.Builder b) { super.collectImplicitComponents(b); if (xp > 0) b.set(ModComponents.BRAIN_XP.get(), xp); }
    @Override protected void applyImplicitComponents(DataComponentGetter c) { super.applyImplicitComponents(c); xp = c.getOrDefault(ModComponents.BRAIN_XP.get(), 0); }
    @Override public void removeComponentsFromTag(ValueOutput o) {}
    @Override protected void saveAdditional(ValueOutput out) { super.saveAdditional(out); out.putInt("XP", xp); }
    @Override protected void loadAdditional(ValueInput in) { super.loadAdditional(in); xp = in.getIntOr("XP", 0); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
}
