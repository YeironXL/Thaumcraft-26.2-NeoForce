package art.arcane.thaumcraft.golems;

import art.arcane.thaumcraft.golems.seals.ISealEntity;
import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** TC6 GolemHelper: provisioning requests and seal area geometry. */
public final class GolemHelper {
    public static final HashMap<ResourceKey<Level>, ArrayList<ProvisionRequest>> provisionRequests = new HashMap<>();
    static final int LISTLIMIT = 1000;

    private static void add(Level world, ProvisionRequest pr) {
        var list = provisionRequests.computeIfAbsent(world.dimension(), k -> new ArrayList<>());
        if (!list.contains(pr)) list.add(pr);
        if (list.size() > LISTLIMIT) list.remove(0);
    }
    public static void requestProvisioning(Level world, ISealEntity seal, ItemStack stack) { add(world, new ProvisionRequest(seal, stack.copy())); }
    public static void requestProvisioning(Level world, BlockPos pos, Direction side, ItemStack stack) { add(world, new ProvisionRequest(pos, side, stack.copy())); }
    public static void requestProvisioning(Level world, Entity entity, ItemStack stack) { add(world, new ProvisionRequest(entity, stack.copy())); }
    public static void requestProvisioning(Level world, BlockPos pos, Direction side, ItemStack stack, int ui) { var pr = new ProvisionRequest(pos, side, stack.copy()); pr.setUI(ui); add(world, pr); }
    public static void requestProvisioning(Level world, Entity entity, ItemStack stack, int ui) { var pr = new ProvisionRequest(entity, stack.copy()); pr.setUI(ui); add(world, pr); }

    /** getPosInArea: the count-th block of the seal's area, iterating z, then x, then y. */
    public static BlockPos getPosInArea(ISealEntity seal, int count) {
        Direction f = seal.getSealPos().face;
        BlockPos a = seal.getArea();
        int xx = 1 + (a.getX() - 1) * (f.getStepX() == 0 ? 2 : 1);
        int yy = 1 + (a.getY() - 1) * (f.getStepY() == 0 ? 2 : 1);
        int zz = 1 + (a.getZ() - 1) * (f.getStepZ() == 0 ? 2 : 1);
        int qx = f.getStepX() != 0 ? f.getStepX() : 1;
        int qy = f.getStepY() != 0 ? f.getStepY() : 1;
        int qz = f.getStepZ() != 0 ? f.getStepZ() : 1;
        int y = qy * (count / zz / xx) % yy + f.getStepY();
        int x = qx * (count / zz) % xx + f.getStepX();
        int z = qz * count % zz + f.getStepZ();
        return seal.getSealPos().pos.offset(x - (f.getStepX() == 0 ? xx / 2 : 0), y - (f.getStepY() == 0 ? yy / 2 : 0), z - (f.getStepZ() == 0 ? zz / 2 : 0));
    }

    /** getBoundsForArea: the block in front of the face, extended outwards and grown sideways by the area. */
    public static AABB getBoundsForArea(ISealEntity seal) {
        Direction f = seal.getSealPos().face;
        BlockPos p = seal.getSealPos().pos, a = seal.getArea();
        AABB bb = new AABB(p).move(f.getStepX(), f.getStepY(), f.getStepZ());
        bb = bb.expandTowards(f.getStepX() != 0 ? (a.getX() - 1) * f.getStepX() : 0, f.getStepY() != 0 ? (a.getY() - 1) * f.getStepY() : 0, f.getStepZ() != 0 ? (a.getZ() - 1) * f.getStepZ() : 0);
        return bb.inflate(f.getStepX() == 0 ? a.getX() - 1 : 0, f.getStepY() == 0 ? a.getY() - 1 : 0, f.getStepZ() == 0 ? a.getZ() - 1 : 0);
    }
    private GolemHelper() {}
}
