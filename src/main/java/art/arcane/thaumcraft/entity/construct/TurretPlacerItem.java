package art.arcane.thaumcraft.entity.construct;

import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** TC6 ItemTurretPlacer: 0 automated crossbow, 1 advanced crossbow, 2 arcane bore (facing the player's direction). */
public class TurretPlacerItem extends Item {
    private final int kind;
    public TurretPlacerItem(Properties p, int kind) { super(p); this.kind = kind; }
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (ctx.getClickedFace() == Direction.DOWN) return InteractionResult.PASS;
        BlockPos pos = ctx.getClickedPos();
        BlockPos bp = world.getBlockState(pos).canBeReplaced() ? pos : pos.relative(ctx.getClickedFace());
        if (player != null && !player.mayUseItemAt(bp, ctx.getClickedFace(), stack)) return InteractionResult.PASS;
        BlockPos bp2 = bp.above();
        if ((!world.isEmptyBlock(bp) && !world.getBlockState(bp).canBeReplaced()) || (!world.isEmptyBlock(bp2) && !world.getBlockState(bp2).canBeReplaced())) return InteractionResult.PASS;
        if (!world.getEntities(null, new AABB(bp.getX(), bp.getY(), bp.getZ(), bp.getX() + 1, bp.getY() + 2, bp.getZ() + 1)).isEmpty()) return InteractionResult.PASS;
        if (world.isClientSide()) return InteractionResult.PASS;
        world.removeBlock(bp, false);
        world.removeBlock(bp2, false);
        OwnedConstruct t = switch (kind) {
            case 1 -> ModEntities.TURRET_ADVANCED.get().create(world, EntitySpawnReason.SPAWN_ITEM_USE);
            case 2 -> ModEntities.ARCANE_BORE.get().create(world, EntitySpawnReason.SPAWN_ITEM_USE);
            default -> ModEntities.TURRET_BASIC.get().create(world, EntitySpawnReason.SPAWN_ITEM_USE);
        };
        if (t != null) {
            t.snapTo(bp.getX() + 0.5, bp.getY(), bp.getZ() + 0.5, 0.0f, 0.0f);
            if (t instanceof ArcaneBoreEntity bore) bore.setFacing(player != null ? player.getDirection() : Direction.NORTH);
            t.setOwned(true);
            t.setValidSpawn();
            if (player != null) t.setOwnerId(player.getUUID());
            world.addFreshEntity(t);
            world.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75f, 0.8f);
        }
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
