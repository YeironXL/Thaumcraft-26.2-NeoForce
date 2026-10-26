package art.arcane.thaumcraft.entity.construct;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.golems.GolemInteractionHelper;
import art.arcane.thaumcraft.golems.InvHelper;
import art.arcane.thaumcraft.infusion.InfusionEnchantment;
import art.arcane.thaumcraft.registry.ModSounds;
import art.arcane.thaumcraft.util.BlockUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** TC6 EntityArcaneBore: a vis-powered mining turret that bores a spiral tunnel with the pickaxe it holds while powered by redstone. */
public class ArcaneBoreEntity extends OwnedConstruct {
    private static final EntityDataAccessor<Direction> FACING = SynchedEntityData.defineId(ArcaneBoreEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(ArcaneBoreEntity.class, EntityDataSerializers.BOOLEAN);
    BlockPos digTarget, digTargetPrev;
    float digCost = 0.25f;
    long soundDelay;
    int breakCounter, digDelay, digDelayMax;
    float radInc;
    public int spiral;
    public float currentRadius;
    private float charge;
    public boolean clientDigging;

    public ArcaneBoreEntity(EntityType<? extends ArcaneBoreEntity> type, Level level) { super(type, level); setDropChance(EquipmentSlot.MAINHAND, 0.0f); }
    public static AttributeSupplier.Builder createAttributes() { return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.STEP_HEIGHT, 0.0); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(FACING, Direction.DOWN); b.define(ACTIVE, false); }

    public boolean isActive() { return entityData.get(ACTIVE); }
    public void setActive(boolean a) { entityData.set(ACTIVE, a); }
    public Direction getFacing() { return entityData.get(FACING); }
    public void setFacing(Direction f) { entityData.set(FACING, f); }
    public float getCharge() { return charge; }
    public void setCharge(float c) { charge = c; }

    private ItemStack tool() { return getMainHandItem(); }
    private static boolean isPick(ItemStack s) { return !s.isEmpty() && (s.is(ItemTags.PICKAXES) || s.isCorrectToolForDrops(Blocks.STONE.defaultBlockState())); }
    public boolean validInventory() {
        ItemStack s = tool();
        return isPick(s) && !(s.isDamageableItem() && s.getDamageValue() + 1 >= s.getMaxDamage());
    }
    private int ench(net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> k) {
        var reg = level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        return EnchantmentHelper.getItemEnchantmentLevel(reg.getOrThrow(k), tool());
    }
    public int getDigRadius() {
        int r = 0;
        if (isPick(tool())) {
            var e = tool().get(net.minecraft.core.component.DataComponents.ENCHANTABLE);
            r = (e == null ? 0 : e.value()) / 3;
            r += InfusionEnchantment.getInfusionEnchantmentLevel(tool(), InfusionEnchantment.DESTRUCTIVE) * 2;
        }
        return r <= 1 ? 2 : r;
    }
    public int getDigDepth() { return getDigRadius() * 8 + InfusionEnchantment.getInfusionEnchantmentLevel(tool(), InfusionEnchantment.BURROWING) * 16; }
    public int getFortune() { return validInventory() ? Math.max(ench(Enchantments.FORTUNE), InfusionEnchantment.getInfusionEnchantmentLevel(tool(), InfusionEnchantment.SOUNDING)) : 0; }
    public int getDigSpeed(BlockState st) { return validInventory() ? (int) (tool().getDestroySpeed(st) / 2.0f) + ench(Enchantments.EFFICIENCY) : 0; }
    public int getRefining() { return tool().isEmpty() ? 0 : InfusionEnchantment.getInfusionEnchantmentLevel(tool(), InfusionEnchantment.REFINING); }
    public boolean hasSilkTouch() { return !tool().isEmpty() && ench(Enchantments.SILK_TOUCH) > 0; }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            setYRot(yHeadRot);
            if (tickCount % 50 == 0) heal(1.0f);
            if (tickCount % 10 == 0 && getCharge() < 10.0f) setCharge(getCharge() + NativeAura.drainVis((ServerLevel) level(), blockPosition(), 10.0f, false));
            BlockPos bp = blockPosition();
            if (BaseRailBlock.isRail(level(), bp.below())) bp = bp.below();
            BlockState st = level().getBlockState(bp);
            if (st.getBlock() instanceof BaseRailBlock) { if (st.is(Blocks.ACTIVATOR_RAIL)) setActive(!st.getValue(PoweredRailBlock.POWERED)); }
            else if (!isPassenger()) setActive(level().hasNeighborSignal(blockPosition().below()));
        }
        if (!isActive()) {
            digTarget = null;
            getLookControl().setLookAt(getX() + getFacing().getStepX(), getY(), getZ() + getFacing().getStepZ(), 10.0f, 33.0f);
        }
        if (digTarget != null && getCharge() >= digCost && !level().isClientSide()) {
            getLookControl().setLookAt(digTarget.getX() + 0.5, digTarget.getY(), digTarget.getZ() + 0.5, 10.0f, 90.0f);
            if (digDelay-- <= 0 && dig()) {
                setCharge(getCharge() - digCost);
                if (soundDelay < System.currentTimeMillis()) {
                    soundDelay = System.currentTimeMillis() + 1200L + level().getRandom().nextInt(100);
                    playSound(ModSounds.get("rumble"), 0.25f, 0.9f + level().getRandom().nextFloat() * 0.2f);
                }
            }
        }
        if (!level().isClientSide() && digTarget == null && isActive() && validInventory()) {
            findNextBlockToDig();
            if (digTarget != null) {
                level().broadcastEntityEvent(this, (byte) 16);
                // PacketFXBoreDig, sent to players within 32 blocks of the target
                if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    var pl = new art.arcane.thaumcraft.network.FxPayload("bore_dig", digTarget.getX(), digTarget.getY(), digTarget.getZ(), java.util.List.of((float) getId(), (float) digDelayMax));
                    net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(sl, null, digTarget.getX(), digTarget.getY(), digTarget.getZ(), 32.0, pl);
                }
                level().destroyBlockProgress(getId(), digTarget, 1);
            } else {
                level().broadcastEntityEvent(this, (byte) 17);
                getLookControl().setLookAt(getX() + getFacing().getStepX() * 2, getY() + getFacing().getStepY() * 2 + getEyeHeight(), getZ() + getFacing().getStepZ() * 2, 10.0f, 33.0f);
            }
        }
    }

    private boolean dig() {
        boolean b = false;
        ServerLevel level = (ServerLevel) level();
        if (digTarget != null && !level.isEmptyBlock(digTarget)) {
            int fortune = getFortune();
            boolean silk = hasSilkTouch();
            if (silk) fortune = 0;
            var fp = GolemInteractionHelper.fakePlayer(level, "FakeThaumcraftBore");
            fp.snapTo(getX(), getY(), getZ(), getYRot(), getXRot());
            fp.setItemInHand(InteractionHand.MAIN_HAND, tool());
            if (BlockUtils.harvestBlock(level, fp, digTarget, false, false, fortune, false)) {
                List<ItemStack> items = new ArrayList<>();
                for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(digTarget).inflate(1.5))) { items.add(e.getItem().copy()); e.discard(); }
                for (ItemStack is : items) {
                    if (is.isEmpty()) continue;
                    boolean e2 = false;
                    for (Direction f : Direction.values()) {
                        BlockPos p = blockPosition().relative(f);
                        if (InvHelper.getItemHandlerAt(level, p, f) != null) { InvHelper.ejectStackAt(level, blockPosition(), f, is); e2 = true; break; }
                    }
                    if (!e2) InvHelper.ejectStackAt(level, blockPosition(), getFacing().getOpposite(), is);
                }
                breakCounter += 1;
            }
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (!tool().isEmpty()) {
                if (breakCounter >= 50) { breakCounter -= 50; tool().hurtAndBreak(1, this, EquipmentSlot.MAINHAND); }
            } else breakCounter = 0;
            level.destroyBlockProgress(getId(), digTarget, -1);
            b = level.removeBlock(digTarget, false) || level.isEmptyBlock(digTarget);
        }
        digTarget = null;
        return b;
    }

    private static Vec3 rotX(Vec3 v, float a) { float c = Mth.cos(a), s = Mth.sin(a); return new Vec3(v.x, v.y * c + v.z * s, v.z * c - v.y * s); }
    private static Vec3 rotY(Vec3 v, float a) { float c = Mth.cos(a), s = Mth.sin(a); return new Vec3(v.x * c + v.z * s, v.y, v.z * c - v.x * s); }
    private static Vec3 rotZ(Vec3 v, float a) { float c = Mth.cos(a), s = Mth.sin(a); return new Vec3(v.x * c + v.y * s, v.y * c - v.x * s, v.z); }

    private void findNextBlockToDig() {
        if (digTargetPrev == null || digTargetPrev.distToCenterSqr(position()) > (getDigRadius() + 1) * (getDigRadius() + 1)) digTargetPrev = blockPosition();
        if (radInc == 0.0f) radInc = 1.0f;
        int digRadius = getDigRadius(), digDepth = getDigDepth();
        int x = digTargetPrev.getX(), z = digTargetPrev.getZ(), y = digTargetPrev.getY();
        Direction f = getFacing();
        BlockPos end = new BlockPos(x + f.getStepX() * digDepth, y + f.getStepY() * digDepth, z + f.getStepZ() * digDepth);
        BlockHitResult mop = level().clip(new ClipContext(Vec3.atCenterOf(digTargetPrev), Vec3.atCenterOf(end), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (mop.getType() == HitResult.Type.BLOCK) {
            Vec3 digger = new Vec3(getX() + f.getStepX(), getY() + getEyeHeight() + f.getStepY(), getZ() + f.getStepZ());
            mop = level().clip(new ClipContext(digger, Vec3.atCenterOf(mop.getBlockPos()), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (mop.getType() == HitResult.Type.BLOCK) {
                BlockState bs = level().getBlockState(mop.getBlockPos());
                float hard = bs.getDestroySpeed(level(), mop.getBlockPos());
                if (hard > -1.0f && !bs.getCollisionShape(level(), mop.getBlockPos()).isEmpty()) {
                    digDelay = Math.max(10 - getDigSpeed(bs), (int) (hard * 2.0f) - getDigSpeed(bs) * 2);
                    if (digDelay < 1) digDelay = 1;
                    digDelayMax = digDelay;
                    if (!mop.getBlockPos().equals(blockPosition()) && !mop.getBlockPos().equals(blockPosition().below())) { digTarget = mop.getBlockPos(); return; }
                }
            }
        }
        while (x == digTargetPrev.getX() && z == digTargetPrev.getZ() && y == digTargetPrev.getY()) {
            if (Math.abs(currentRadius) > digRadius) currentRadius = digRadius;
            spiral += (int) (3.0f + Math.max(0.0f, (10.0f - Math.abs(currentRadius)) * 2.0f));
            if (spiral >= 360) {
                spiral -= 360;
                currentRadius += radInc;
                if (currentRadius > digRadius || currentRadius < -digRadius) currentRadius = 0.0f;
            }
            Vec3 vsource = new Vec3((int) getX() + 0.5 + f.getStepX(), getY() + f.getStepY() + getEyeHeight(), (int) getZ() + 0.5 + f.getStepZ());
            Vec3 vtar = new Vec3(0.0, currentRadius, 0.0);
            vtar = rotZ(vtar, spiral / 180.0f * (float) Math.PI);
            vtar = rotY(vtar, 1.5707964f * f.getStepX());
            vtar = rotX(vtar, 1.5707964f * f.getStepY());
            Vec3 vres = vsource.add(vtar);
            x = Mth.floor(vres.x); y = Mth.floor(vres.y); z = Mth.floor(vres.z);
        }
        digTargetPrev = new BlockPos(x, y, z);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity le && isOwner(le)) {
            Direction f = Direction.getApproximateNearest(le.getX() - getX(), le.getEyeY() - getEyeY(), le.getZ() - getZ());
            if (f != Direction.DOWN) setFacing(f);
            return false;
        }
        setYRot(getYRot() + (float) (getRandom().nextGaussian() * 45.0));
        setXRot(getXRot() + (float) (getRandom().nextGaussian() * 20.0));
        return super.hurtServer(level, source, amount);
    }
    @Override public boolean isPushable() { return true; }
    @Override public void die(DamageSource cause) { super.die(cause); if (!level().isClientSide()) dropStuff(); }
    protected void dropStuff() { if (!tool().isEmpty()) { spawnAtLocation((ServerLevel) level(), tool().copy(), 0.5f); setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); } }
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.getItemInHand(hand).getItem() instanceof NameTagItem) return InteractionResult.PASS;
        if (!level().isClientSide() && isOwner(player) && !isRemoved()) {
            if (player.isShiftKeyDown()) {
                playSound(ModSounds.get("zap"), 1.0f, 1.0f);
                dropStuff();
                spawnAtLocation((ServerLevel) level(), new ItemStack(ModItems.TURRET_BORE.get()), 0.5f);
                discard();
                player.swing(hand);
            } else if (player instanceof ServerPlayer sp) sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new TurretMenu(id, inv, this), getDisplayName()), buf -> buf.writeVarInt(getId()));
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }
    @Override public void knockback(double power, double xd, double zd, DamageSource src, float dmg, boolean fx) {
        super.knockback(power, xd / 10.0, zd / 10.0, src, dmg, fx);
        if (getDeltaMovement().y > 0.1) setDeltaMovement(getDeltaMovement().x, 0.1, getDeltaMovement().z);
    }
    @Override protected void addAdditionalSaveData(ValueOutput out) { super.addAdditionalSaveData(out); out.putFloat("charge", getCharge()); out.putByte("faceing", (byte) getFacing().get3DDataValue()); out.putBoolean("active", isActive()); }
    @Override protected void readAdditionalSaveData(ValueInput in) { super.readAdditionalSaveData(in); setCharge(in.getFloatOr("charge", 0)); setFacing(Direction.from3DDataValue(in.getByteOr("faceing", (byte) 0))); setActive(in.getBooleanOr("active", false)); }
    @Override public void move(MoverType mt, Vec3 d) { super.move(mt, new Vec3(d.x / 5.0, d.y, d.z / 5.0)); }
    @Override public int getMaxHeadXRot() { return 10; }
    @Override public void handleEntityEvent(byte id) { if (id == 16) clientDigging = true; else if (id == 17) clientDigging = false; else super.handleEntityEvent(id); }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        if (random.nextFloat() < 0.2f) spawnAtLocation(level, new ItemStack(ModItems.MIND_CLOCKWORK.get()), 0.5f);
        if (random.nextFloat() < 0.2f) spawnAtLocation(level, new ItemStack(ModItems.MORPHIC_RESONATOR.get()), 0.5f);
        if (random.nextFloat() < 0.2f) spawnAtLocation(level, new ItemStack(art.arcane.thaumcraft.ModBlocks.CRYSTAL_AER.get()), 0.5f);
        if (random.nextFloat() < 0.2f) spawnAtLocation(level, new ItemStack(art.arcane.thaumcraft.ModBlocks.CRYSTAL_TERRA.get()), 0.5f);
        if (random.nextFloat() < 0.5f) spawnAtLocation(level, new ItemStack(ModItems.MECHANISM_SIMPLE.get()), 0.5f);
        if (random.nextFloat() < 0.5f) spawnAtLocation(level, new ItemStack(ModItems.PLATE_BRASS.get()), 0.5f);
        if (random.nextFloat() < 0.5f) spawnAtLocation(level, new ItemStack(art.arcane.thaumcraft.ModBlocks.PLANK_GREATWOOD.get()), 0.5f);
    }
}
