package art.arcane.thaumcraft.golems;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/** TC6 GolemInteractionHelper: a golem left/right clicks a block through a fake player and keeps what it got. */
public final class GolemInteractionHelper {
    public static FakePlayer fakePlayer(ServerLevel level, String name) {
        return FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
    }

    public static void golemClick(Level world, GolemEntity golem, BlockPos pos, Direction face, ItemStack clickStack, boolean sneaking, boolean rightClick) {
        if (!(world instanceof ServerLevel sl)) return;
        FakePlayer fp = fakePlayer(sl, "FakeThaumcraftGolem");
        fp.snapTo(golem.getX(), golem.getY(), golem.getZ(), golem.getYRot(), golem.getXRot());
        fp.getInventory().clearContent();
        fp.setItemInHand(InteractionHand.MAIN_HAND, clickStack);
        fp.setShiftKeyDown(sneaking);
        if (!rightClick) {
            try { fp.gameMode.handleBlockBreakAction(pos, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, face, sl.getMaxY(), 0); } catch (Exception ignored) {}
        } else {
            if (fp.getMainHandItem().getItem() instanceof BlockItem bi && !mayPlace(world, bi, pos)) {
                golem.setPos(golem.getX() + face.getStepX(), golem.getY() + face.getStepY(), golem.getZ() + face.getStepZ());
            }
            try {
                var hit = new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false);
                fp.gameMode.useItemOn(fp, world, fp.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            } catch (Exception ignored) {}
        }
        golem.addRankXp(1);
        if (!fp.getMainHandItem().isEmpty() && fp.getMainHandItem().getCount() <= 0) fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        dropSomeItems(fp, golem);
        fp.setShiftKeyDown(false);
        golem.swingArm();
    }

    private static boolean mayPlace(Level world, BlockItem item, BlockPos pos) {
        var shape = item.getBlock().defaultBlockState().getCollisionShape(world, pos);
        return shape.isEmpty() || world.isUnobstructed(null, shape.move(pos.getX(), pos.getY(), pos.getZ()));
    }

    private static void dropSomeItems(FakePlayer fp, GolemEntity golem) {
        var inv = fp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (golem.canCarry(s, true)) s = golem.holdItem(s);
            if (!s.isEmpty()) InvHelper.dropItemAtEntity(golem.level(), s, golem);
            inv.setItem(i, ItemStack.EMPTY);
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) if (!fp.getItemBySlot(slot).isEmpty()) fp.setItemSlot(slot, ItemStack.EMPTY);
    }
    private GolemInteractionHelper() {}
}
