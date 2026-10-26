package art.arcane.thaumcraft.golems;

import art.arcane.thaumcraft.golems.seals.ISealEntity;
import art.arcane.thaumcraft.golems.seals.SealHandler;
import art.arcane.thaumcraft.golems.seals.SealMenu;
import art.arcane.thaumcraft.golems.seals.SealPos;
import art.arcane.thaumcraft.registry.ModSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 ItemGolemBell: right-click a seal to configure it, sneak-right-click to pick it up; with Golem Logistics,
 * sneak-use elsewhere opens the logistics request screen. Also toggles follow/stay on golems (GOLEMDIRECT).
 */
public class GolemBellItem extends Item implements ISealDisplayer {
    public GolemBellItem(Properties p) { super(p.stacksTo(1)); }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        player.swing(hand);
        if (!world.isClientSide() && player instanceof ServerPlayer sp) {
            HitResult mop = retrace(player);
            if (mop.getType() == HitResult.Type.BLOCK || mop.getType() == HitResult.Type.ENTITY) {
                ISealEntity se = getSeal(player);
                if (se != null) {
                    if (player.isShiftKeyDown()) {
                        SealHandler.removeSealEntity(world, se.getSealPos(), false);
                        world.playSound(null, se.getSealPos().pos, ModSounds.get("zap"), SoundSource.BLOCKS, 0.5f, 1.0f);
                    } else SealMenu.open(sp, se);
                }
                return InteractionResult.FAIL;
            }
            if (player.isShiftKeyDown() && art.arcane.thaumcraft.progression.Warp.knows(sp, "GOLEMLOGISTICS")) {
                art.arcane.thaumcraft.golems.logistics.LogisticsMenu.open(sp, null, null);
                return InteractionResult.FAIL;
            }
        } else if (world.isClientSide()) player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.0f + world.getRandom().nextFloat() * 0.1f);
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        player.swing(ctx.getHand());
        Level world = ctx.getLevel();
        if (!world.isClientSide() && player instanceof ServerPlayer sp) {
            ISealEntity se = SealHandler.getSealEntity(world, new SealPos(ctx.getClickedPos(), ctx.getClickedFace()));
            if (se != null) {
                if (player.isShiftKeyDown()) {
                    SealHandler.removeSealEntity(world, se.getSealPos(), false);
                    world.playSound(null, ctx.getClickedPos(), ModSounds.get("zap"), SoundSource.BLOCKS, 0.5f, 1.0f);
                } else SealMenu.open(sp, se);
                return InteractionResult.SUCCESS;
            }
            if (player.isShiftKeyDown() && art.arcane.thaumcraft.progression.Warp.knows(sp, "GOLEMLOGISTICS")) {
                art.arcane.thaumcraft.golems.logistics.LogisticsMenu.open(sp, ctx.getClickedPos(), ctx.getClickedFace());
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * RayTracer.retrace: the look ray out to block reach, stopping on liquids, plus the nearest pickable entity (a golem)
     * in front of the first block hit, so pointing the bell at a golem counts as a hit and never opens the logistics screen.
     */
    public static HitResult retrace(Player p) {
        double reach = p.blockInteractionRange();
        Vec3 eye = p.getEyePosition(1.0f), look = p.getViewVector(1.0f), end = eye.add(look.scale(reach));
        HitResult block = p.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, p));
        double max = block.getType() == HitResult.Type.MISS ? reach * reach : block.getLocation().distanceToSqr(eye);
        double er = Math.max(reach, p.entityInteractionRange());
        Vec3 eEnd = eye.add(look.scale(er));
        var eh = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(p, eye, eEnd, p.getBoundingBox().expandTowards(look.scale(er)).inflate(1.0),
                e -> !e.isSpectator() && e.isPickable(), Math.min(max, er * er));
        return eh != null ? eh : block;
    }

    /** getSeal: march the look vector (5 blocks) and return the first seal on a face the ray enters. */
    public static ISealEntity getSeal(Player p) {
        float f = p.getXRot(), f2 = p.getYRot();
        Vec3 vec0 = new Vec3(p.getX(), p.getY() + p.getEyeHeight(), p.getZ());
        float f3 = Mth.cos(-f2 * 0.017453292f - (float) Math.PI), f4 = Mth.sin(-f2 * 0.017453292f - (float) Math.PI);
        float f5 = -Mth.cos(-f * 0.017453292f), f6 = Mth.sin(-f * 0.017453292f);
        float f7 = f4 * f5, f8 = f3 * f5;
        double d4 = 5.0;
        Vec3 vec2 = vec0.add(f7 * d4, f6 * d4, f8 * d4);
        Vec3 vec3 = new Vec3(f7 * d4, f6 * d4, f8 * d4);
        Vec3 vec4 = vec0.add(vec3.x / 10.0, vec3.y / 10.0, vec3.z / 10.0);
        var unit = List.of(new AABB(0, 0, 0, 1, 1, 1));
        for (int a = 0; a < vec3.length() * 10.0; ++a) {
            BlockPos pos = BlockPos.containing(vec4);
            BlockHitResult mop = AABB.clip(unit, vec0, vec2, pos);
            if (mop != null) {
                ISealEntity se = SealHandler.getSealEntity(p.level(), new SealPos(pos, mop.getDirection()));
                if (se != null) return se;
            }
            vec4 = vec4.add(vec3.x / 10.0, vec3.y / 10.0, vec3.z / 10.0);
        }
        return null;
    }
}
