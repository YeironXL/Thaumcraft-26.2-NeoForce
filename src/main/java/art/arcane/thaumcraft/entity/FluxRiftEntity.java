package art.arcane.thaumcraft.entity;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.casters.FocusEngine;
import art.arcane.thaumcraft.casters.FocusMediumRoot;
import art.arcane.thaumcraft.casters.FocusPackage;
import art.arcane.thaumcraft.casters.foci.FocusEffectFlux;
import art.arcane.thaumcraft.casters.foci.FocusMediumCloud;
import art.arcane.thaumcraft.fx.Fx;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModEffects;
import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.registry.ModSounds;
import art.arcane.thaumcraft.research.ResearchEvents;
import art.arcane.thaumcraft.research.ResearchRules;
import art.arcane.thaumcraft.util.EntityUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 EntityFluxRift: a jagged tear in reality fed by local flux. It grows by draining flux, slowly destabilises,
 * erodes blocks and hurts entities along its spine, triggers flux events while unstable and collapses into void seeds
 * (and occasionally a primordial pearl fragment) when it shrinks below three points.
 */
public final class FluxRiftEntity extends Entity {
    private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(FluxRiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(FluxRiftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> STABILITY = SynchedEntityData.defineId(FluxRiftEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> COLLAPSE = SynchedEntityData.defineId(FluxRiftEntity.class, EntityDataSerializers.BOOLEAN);
    /** event, weight, stability cost, allowed near a taint seed. */
    public static final int[][] EVENTS = {{0, 50, 5, 1}, {1, 10, 0, 0}, {2, 20, 10, 1}, {3, 20, 10, 1}, {4, 1, 0, 1}};

    public enum EnumStability { VERY_STABLE, STABLE, UNSTABLE, VERY_UNSTABLE }

    int maxSize = 0;
    int lastSize = -1;
    public final ArrayList<Vec3> points = new ArrayList<>();
    public final ArrayList<Float> pointsWidth = new ArrayList<>();
    private double minX, minY, minZ, maxX, maxY, maxZ;
    private boolean hasExtent;

    public FluxRiftEntity(EntityType<? extends FluxRiftEntity> type, Level level) { super(type, level); noPhysics = true; }

    @Override protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(SEED, 0); b.define(SIZE, 5); b.define(STABILITY, 0.0f); b.define(COLLAPSE, false);
    }

    public boolean getCollapse() { return entityData.get(COLLAPSE); }
    public void setCollapse(boolean b) { if (b) maxSize = getRiftSize(); entityData.set(COLLAPSE, b); }
    public float getRiftStability() { return entityData.get(STABILITY); }
    public void setRiftStability(float s) { entityData.set(STABILITY, Mth.clamp(s, -100.0f, 100.0f)); }
    public int getRiftSize() { return entityData.get(SIZE); }
    public void setRiftSize(int s) { entityData.set(SIZE, s); setSize(); }
    public int getRiftSeed() { return entityData.get(SEED); }
    public void setRiftSeed(int s) { entityData.set(SEED, s); }

    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (level().isClientSide() && (SIZE.equals(key) || SEED.equals(key))) setSize();
    }

    /** TC6 setSize(): rebuild the spine from the seed and fit the bounding box around it. */
    protected void setSize() {
        calcSteps(points, pointsWidth, new Random(getRiftSeed()));
        lastSize = getRiftSize();
        double x0 = Double.MAX_VALUE, y0 = Double.MAX_VALUE, z0 = Double.MAX_VALUE;
        double x2 = Double.MIN_VALUE, y2 = Double.MIN_VALUE, z2 = Double.MIN_VALUE;
        for (Vec3 v : points) {
            x0 = Math.min(x0, v.x); x2 = Math.max(x2, v.x);
            y0 = Math.min(y0, v.y); y2 = Math.max(y2, v.y);
            z0 = Math.min(z0, v.z); z2 = Math.max(z2, v.z);
        }
        hasExtent = !points.isEmpty();
        minX = x0; minY = y0; minZ = z0; maxX = x2; maxY = y2; maxZ = z2;
        setBoundingBox(makeBoundingBox(position()));
    }

    @Override protected AABB makeBoundingBox(Vec3 p) {
        if (!hasExtent) return super.makeBoundingBox(p);
        return new AABB(p.x + minX, p.y + minY, p.z + minZ, p.x + maxX, p.y + maxY, p.z + maxZ);
    }

    @Override protected void addAdditionalSaveData(ValueOutput o) {
        o.putInt("MaxSize", maxSize); o.putInt("RiftSize", getRiftSize()); o.putInt("RiftSeed", getRiftSeed());
        o.putFloat("Stability", getRiftStability()); o.putBoolean("collapse", getCollapse());
    }
    @Override protected void readAdditionalSaveData(ValueInput i) {
        maxSize = i.getIntOr("MaxSize", 0);
        setRiftSeed(i.getIntOr("RiftSeed", 0));
        setRiftSize(i.getIntOr("RiftSize", 5));
        // TC6 reads the float back with getInteger, truncating it.
        setRiftStability(Mth.floor(i.getFloatOr("Stability", 0.0f)));
        setCollapse(i.getBooleanOr("collapse", false));
    }

    @Override public void move(MoverType type, Vec3 delta) {}
    @Override public boolean hurtServer(ServerLevel l, DamageSource s, float a) { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean displayFireAnimation() { return false; }
    @Override public boolean isOnFire() { return false; }
    @Override public boolean fireImmune() { return true; }

    @Override
    public void tick() {
        super.tick();
        if (lastSize != getRiftSize()) setSize();
        if (level() instanceof ServerLevel sl) {
            if (getRiftSeed() == 0) setRiftSeed(random.nextInt());
            if (!points.isEmpty()) {
                int pi = random.nextInt(points.size() - 1);
                Vec3 v1 = points.get(pi).add(getX(), getY(), getZ());
                Vec3 v2 = points.get(pi + 1).add(getX(), getY(), getZ());
                HitResult rt = EntityUtils.rayTraceBlocks(sl, v1, v2, this);
                if (rt instanceof BlockHitResult bh && rt.getType() == HitResult.Type.BLOCK) {
                    BlockPos p = bh.getBlockPos();
                    BlockState bs = sl.getBlockState(p);
                    if (!bs.isAir() && bs.getDestroySpeed(sl, p) >= 0.0f && bs.getFluidState().isEmpty()) {
                        sl.levelEvent(2001, p, Block.getId(bs));
                        sl.removeBlock(p, false);
                    }
                }
                for (Entity e : sl.getEntities(this, new AABB(v1, v1).inflate(0.5), e -> e.distanceToSqr(v1) <= 0.25)) {
                    if (!e.isAlive()) continue;
                    if (e instanceof Player pl && pl.isCreative()) continue;
                    e.hurtServer(sl, sl.damageSources().fellOutOfWorld(), 2.0f);
                    if (e instanceof ItemEntity) e.discard();
                }
            }
            if (points.size() < 3 && !getCollapse()) setCollapse(true);
            if (getCollapse()) {
                setRiftSize(getRiftSize() - 1);
                if (random.nextBoolean()) NativeAura.addVis(sl, blockPosition(), 1.0f);
                else NativeAura.addFlux(sl, blockPosition(), 1.0f);
                if (random.nextInt(10) == 0)
                    sl.explode(this, getX() + random.nextGaussian() * 2.0, getY() + random.nextGaussian() * 2.0, getZ() + random.nextGaussian() * 2.0,
                            random.nextFloat() / 2.0f, Level.ExplosionInteraction.NONE);
                if (getRiftSize() <= 1) { completeCollapse(sl); return; }
            }
            if (tickCount % 120 == 0) setRiftStability(getRiftStability() - 0.2f);
            if (tickCount % 600 == getId() % 600) {
                float taint = NativeAura.flux(sl, blockPosition());
                double size = Math.sqrt(getRiftSize() * 2);
                if (taint >= size && getRiftSize() < 100 && getStability() != EnumStability.VERY_STABLE) {
                    NativeAura.drainFlux(sl, blockPosition(), (float) size, false);
                    setRiftSize(getRiftSize() + 1);
                }
                if (getRiftStability() < 0.0f && random.nextInt(1000) < Math.abs(getRiftStability()) + getRiftSize()) executeRiftEvent(sl);
            }
            if (isAlive() && tickCount % 300 == 0)
                playSound(ModSounds.get("evilportal"), (float) (0.15 + random.nextGaussian() * 0.066), (float) (0.75 + random.nextGaussian() * 0.1));
        } else {
            if (points.size() > 2 && !getCollapse() && getRiftStability() < 0.0f && random.nextInt(150) < Math.abs(getRiftStability())) {
                int pi = 1 + random.nextInt(points.size() - 2);
                Vec3 v1 = points.get(pi).add(getX(), getY(), getZ());
                art.arcane.thaumcraft.client.fx.FXDispatcher.drawCurlyWisp(v1.x, v1.y, v1.z, 0, 0, 0, 0.1f + pointsWidth.get(pi) * 3.0f, 1.0f, 1.0f, 1.0f, 0.25f, null, 1, 0, 0);
            }
            if (points.size() > 2 && getCollapse()) {
                int pi = 1 + random.nextInt(points.size() - 2);
                Vec3 v1 = points.get(pi).add(getX(), getY(), getZ());
                art.arcane.thaumcraft.client.fx.FXDispatcher.drawCurlyWisp(v1.x, v1.y, v1.z, 0, 0, 0, 0.1f + pointsWidth.get(pi) * 3.0f, 1.0f,
                        0.3f + random.nextFloat() * 0.1f, 0.3f + random.nextFloat() * 0.1f, 0.4f, null, 1, 0, 0);
            }
        }
    }

    /** TC6 createRift: called from the aura tick when a chunk's flux crosses 75% of its base. */
    public static void createRift(ServerLevel world, BlockPos pos) {
        var rand = world.getRandom();
        pos = pos.offset(rand.nextInt(16), 0, rand.nextInt(16));
        if (!world.hasChunkAt(pos)) return;
        BlockPos p2 = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos);
        int actualHeight = world.getMinY() + world.dimensionType().logicalHeight();
        if (!world.dimensionType().hasSkyLight()) {
            for (p2 = new BlockPos(p2.getX(), world.getMinY() + 10, p2.getZ()); !world.isEmptyBlock(p2); p2 = p2.above(rand.nextInt(5) + 1))
                if (p2.getY() > actualHeight - 5) return;
        }
        if (p2.getY() >= actualHeight - 4) return;
        Vec3 c = Vec3.atCenterOf(p2);
        if (!world.getEntitiesOfClass(FluxRiftEntity.class, new AABB(c, c).inflate(32.0), e -> e.distanceToSqr(c) <= 1024.0).isEmpty()) return;
        FluxRiftEntity rift = new FluxRiftEntity(ModEntities.FLUX_RIFT.get(), world);
        rift.setRiftSeed(rand.nextInt());
        rift.snapTo(p2.getX() + 0.5, p2.getY() + 0.5, p2.getZ() + 0.5, rand.nextInt(360), 0.0f);
        float taint = NativeAura.flux(world, p2);
        double size = Math.sqrt(taint * 3.0f);
        if (size > 5.0 && world.addFreshEntity(rift)) {
            rift.setRiftSize((int) size);
            NativeAura.drainFlux(world, p2, (float) size, false);
            for (ServerPlayer target : world.getEntitiesOfClass(ServerPlayer.class, new AABB(p2).inflate(32.0))) {
                if (!ResearchRules.knows(PlayerProgressManager.get(target), "f_toomuchflux")) {
                    target.sendSystemMessage(Component.literal("§5§o").append(Component.translatable("tc.fluxevent.3")), true);
                    ResearchEvents.progress(target, "f_toomuchflux");
                }
            }
        }
    }

    private void executeRiftEvent(ServerLevel sl) {
        int total = 0;
        for (int[] e : EVENTS) total += e[1];
        int roll = random.nextInt(total);
        int[] ei = null;
        for (int[] e : EVENTS) { roll -= e[1]; if (roll < 0) { ei = e; break; } }
        if (ei == null) return;
        executeRiftEvent(sl, ei);
    }

    /** One rift event row {event, weight, stability cost, nearTaintAllowed}; public for the verification suite. */
    public void executeRiftEvent(ServerLevel sl, int[] ei) {
        if (ei[3] == 0 && art.arcane.thaumcraft.block.world.taint.TaintHelper.isNearTaintSeed(sl, blockPosition())) return;
        boolean didit = false;
        switch (ei[0]) {
            case 0 -> {
                WispEntity wisp = new WispEntity(ModEntities.WISP.get(), sl);
                wisp.snapTo(getX() + random.nextGaussian() * 5.0, getY() + random.nextGaussian() * 5.0, getZ() + random.nextGaussian() * 5.0, 0.0f, 0.0f);
                if (sl.getRandom().nextInt(5) == 0) wisp.setWispType("vitium");
                if (wisp.getCanSpawnHere() && sl.addFreshEntity(wisp)) didit = true;
            }
            case 1 -> {
                TaintSeedPrimeEntity seed = new TaintSeedPrimeEntity(ModEntities.TAINT_SEED_PRIME.get(), sl);
                seed.snapTo((int) (getX() + random.nextGaussian() * 5.0) + 0.5, (int) (getY() + random.nextGaussian() * 5.0), (int) (getZ() + random.nextGaussian() * 5.0) + 0.5,
                        sl.getRandom().nextInt(360), 0.0f);
                if (seed.getCanSpawnHere() && sl.addFreshEntity(seed)) {
                    didit = true;
                    seed.boost = getRiftSize();
                    NativeAura.addFlux(sl, blockPosition(), getRiftSize() / 2);
                    discard();
                }
            }
            case 2 -> {
                for (LivingEntity target : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16.0))) {
                    didit = true;
                    if (target instanceof ServerPlayer p)
                        p.sendSystemMessage(Component.literal("§5§o").append(Component.translatable("tc.fluxevent.2")), true);
                    target.addEffect(new MobEffectInstance(ModEffects.INFECTIOUS_VIS_EXHAUST, 3000, 2));
                }
            }
            case 3 -> {
                Player target2 = sl.getNearestPlayer(this, 16.0);
                if (target2 != null) {
                    FocusPackage p = new FocusPackage(target2);
                    FocusMediumRoot root = new FocusMediumRoot();
                    root.setupFromCasterToTarget(target2, target2, 0.5);
                    p.addNode(root);
                    FocusMediumCloud fp = new FocusMediumCloud();
                    fp.initialize();
                    fp.getSetting("radius").setValue(Mth.randomBetweenInclusive(random, 1, 3));
                    fp.getSetting("duration").setValue(Mth.randomBetweenInclusive(random, Math.min(getRiftSize() / 2, 30), Math.min(getRiftSize(), 120)));
                    p.addNode(fp);
                    p.addNode(new FocusEffectFlux());
                    FocusEngine.castFocusPackage(target2, p, true);
                }
            }
            case 4 -> setCollapse(true);
            default -> {}
        }
        if (didit) setRiftStability(getRiftStability() + ei[2]);
    }

    private void calcSteps(ArrayList<Vec3> pp, ArrayList<Float> ww, Random rr) {
        pp.clear(); ww.clear();
        Vec3 right = new Vec3(rr.nextGaussian(), rr.nextGaussian(), rr.nextGaussian()).normalize();
        Vec3 left = right.scale(-1.0);
        Vec3 lr = Vec3.ZERO, ll = Vec3.ZERO;
        int steps = Mth.ceil(getRiftSize() / 3.0f);
        float girth = getRiftSize() / 300.0f;
        double angle = 0.33;
        float dec = girth / steps;
        for (int a = 0; a < steps; ++a) {
            girth -= dec;
            right = right.xRot((float) (rr.nextGaussian() * angle));
            right = right.yRot((float) (rr.nextGaussian() * angle));
            lr = lr.add(right.scale(0.2));
            pp.add(lr); ww.add(girth);
            left = left.xRot((float) (rr.nextGaussian() * angle));
            left = left.yRot((float) (rr.nextGaussian() * angle));
            ll = ll.add(left.scale(0.2));
            pp.add(0, ll); ww.add(0, girth);
        }
        lr = lr.add(right.scale(0.1));
        pp.add(lr); ww.add(0.0f);
        ll = ll.add(left.scale(0.1));
        pp.add(0, ll); ww.add(0, 0.0f);
    }

    public void addStability() { setRiftStability(getRiftStability() + 0.125f); }

    public EnumStability getStability() {
        float s = getRiftStability();
        return s > 50.0f ? EnumStability.VERY_STABLE : s >= 0.0f ? EnumStability.STABLE : s > -25.0f ? EnumStability.UNSTABLE : EnumStability.VERY_UNSTABLE;
    }

    private void completeCollapse(ServerLevel sl) {
        int qq = (int) Math.sqrt(maxSize);
        if (random.nextInt(100) < qq) {
            ItemStack pearl = new ItemStack(ModItems.PRIMORDIAL_PEARL.get());
            pearl.set(DataComponents.DAMAGE, 4 + random.nextInt(4));
            spawnAtLocation(sl, pearl, 0.0f);
        }
        for (int a = 0; a < qq; ++a) spawnAtLocation(sl, new ItemStack(ModItems.VOID_SEED.get()), 0.0f);
        Fx.bamf(sl, getX(), getY(), getZ(), true, true, null);
        List<LivingEntity> list = sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(32.0), e -> e.distanceToSqr(this) <= 1024.0);
        EnumStability st = getStability();
        if (st == EnumStability.VERY_UNSTABLE) for (LivingEntity p : list) {
            int w = (int) ((1.0 - p.distanceToSqr(this) / 32.0) * 120.0);
            if (w > 0) p.addEffect(new MobEffectInstance(ModEffects.FLUX_TAINT, w * 20, 0));
        }
        if (st == EnumStability.VERY_UNSTABLE || st == EnumStability.UNSTABLE) for (LivingEntity p : list) {
            int w = (int) ((1.0 - p.distanceToSqr(this) / 32.0) * 300.0);
            if (w > 0) p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, w * 20, 0));
        }
        if (st != EnumStability.VERY_STABLE) for (LivingEntity p : list) {
            if (!(p instanceof ServerPlayer sp)) continue;
            int w = (int) ((1.0 - p.distanceToSqr(this) / 32.0) * 25.0);
            if (w <= 0) continue;
            PlayerProgressManager.addWarp(sp, PlayerProgressManager.WarpType.NORMAL, w);
            PlayerProgressManager.addWarp(sp, PlayerProgressManager.WarpType.TEMPORARY, w);
        }
        discard();
    }
}
