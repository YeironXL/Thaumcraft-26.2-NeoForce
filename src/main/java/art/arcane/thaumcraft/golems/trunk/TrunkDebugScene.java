package art.arcane.thaumcraft.golems.trunk;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;

/** Development aid (-Dthaumcraft.debugTrunk=follow|gui): travelling trunks walking up to the player, or one trunk's GUI open. */
public final class TrunkDebugScene {
    private static TravellingTrunkEntity guiTrunk;
    public static void register() {
        String mode = System.getProperty("thaumcraft.debugTrunk");
        if (mode == null) return;
        final int[] t = {0};
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> {
            var pl = e.getServer().getPlayerList().getPlayers();
            if (pl.isEmpty()) return;
            t[0]++;
            ServerPlayer p = pl.get(0);
            try {
                if (t[0] == 100) build(p, mode);
                if (t[0] == (mode.equals("gui") ? 180 : 100000) && guiTrunk != null) guiTrunk.openInventory(p);
                if (!mode.equals("gui") && t[0] > 100 && (t[0] - 100) % 120 == 0) place(p); // send them back so a walk is always in progress
            } catch (Throwable ex) { ex.printStackTrace(); }
        });
    }
    private static void build(ServerPlayer p, String mode) {
        ServerLevel level = (ServerLevel) p.level();
        p.getAbilities().flying = false; p.onUpdateAbilities();
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), "time set noon");
        BlockPos base = BlockPos.containing(p.getX() + 40, 230, p.getZ());
        for (int x = -10; x <= 10; x++) for (int y = 1; y <= 6; y++) for (int z = -6; z <= 20; z++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        for (int x = -10; x <= 10; x++) for (int z = -6; z <= 20; z++) level.setBlock(base.offset(x, 0, z), ((x + z) & 1) == 0 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState(), 3);
        p.teleportTo(level, base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5, Set.of(), 0f, mode.equals("gui") ? 30f : 20f, true);
        if (mode.equals("gui")) {
            guiTrunk = TravellingTrunkEntity.spawn(level, base.getX() + 0.5, base.getY() + 1, base.getZ() + 2.5, 180f, p, null);
            guiTrunk.setStaying(true);
            ItemStack[] stuff = {new ItemStack(Items.DIAMOND, 12), new ItemStack(Items.BREAD, 20), new ItemStack(Items.IRON_INGOT, 40), new ItemStack(Items.TORCH, 64), new ItemStack(Items.OAK_LOG, 32), new ItemStack(Items.REDSTONE, 50)};
            for (int i = 0; i < stuff.length; i++) guiTrunk.inventory().setItem(i * 4, stuff[i]);
            return;
        }
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        origin = base;
        double[][] at = {{-2.5, 9.5}, {1.5, 11.5}, {4.5, 8.5}};
        for (int i = 0; i < at.length; i++) {
            var tr = TravellingTrunkEntity.spawn(level, base.getX() + 0.5 + at[i][0], base.getY() + 1, base.getZ() + 0.5 + at[i][1], 180f, p, null);
            if (i == 1) tr.setUpgrade(TravellingTrunkEntity.AIR);
            trunks.add(tr);
        }
    }
    private static BlockPos origin;
    private static final java.util.List<TravellingTrunkEntity> trunks = new java.util.ArrayList<>();
    private static void place(ServerPlayer p) {
        double[][] at = {{-1.8, 8.5}, {0.6, 10.5}, {2.5, 7.5}};
        p.teleportTo((ServerLevel) p.level(), origin.getX() + 0.5, origin.getY() + 1, origin.getZ() + 0.5, Set.of(), 0f, 28f, true);
        for (int i = 0; i < trunks.size(); i++) trunks.get(i).snapTo(origin.getX() + 0.5 + at[i][0], origin.getY() + 1, origin.getZ() + 0.5 + at[i][1], 180f, 0);
    }
    private TrunkDebugScene() {}
}
