package art.arcane.thaumcraft.events;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.block.ManaPodBlock;
import art.arcane.thaumcraft.blockentity.DeconstructionTableBlockEntity;
import art.arcane.thaumcraft.blockentity.ManaPodBlockEntity;
import art.arcane.thaumcraft.item.ManaBeanItem;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;

/** Development aid (-Dthaumcraft.debugMana=pods|gui): mana pods under greatwood logs, or the deconstruction GUI open. */
public final class ManaDebugScene {
    public static void register() {
        String mode = System.getProperty("thaumcraft.debugMana");
        if (mode == null) return;
        final int[] t = {0};
        final BlockPos[] table = {null};
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) -> {
            var pl = e.getServer().getPlayerList().getPlayers();
            if (pl.isEmpty()) return;
            ServerPlayer p = pl.get(0);
            try {
                if (++t[0] == 100) table[0] = build(p);
                if (t[0] == 180 && mode.equals("gui") && p.level().getBlockEntity(table[0]) instanceof DeconstructionTableBlockEntity te) {
                    te.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
                    te.setOwner(p.getUUID());
                    p.openMenu(te, buf -> buf.writeBlockPos(table[0]));
                }
            } catch (Throwable ex) { ex.printStackTrace(); }
        });
    }
    private static BlockPos build(ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        p.getAbilities().flying = false; p.onUpdateAbilities();
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(), "time set noon");
        BlockPos base = BlockPos.containing(p.getX() + 40, 230, p.getZ());
        for (int x = -8; x <= 8; x++) for (int y = 1; y <= 7; y++) for (int z = -4; z <= 12; z++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        for (int x = -8; x <= 8; x++) for (int z = -4; z <= 12; z++) level.setBlock(base.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        String[] aspects = {"aer", "ignis", "aqua", "terra", "ordo", "perditio", "herba", "praecantatio"};
        for (int i = 0; i < 8; i++) {
            BlockPos log = base.offset(-4 + i, 4, 5);
            level.setBlock(log, (i % 2 == 0 ? ModBlocks.LOG_GREATWOOD : ModBlocks.LOG_SILVERWOOD).get().defaultBlockState(), 3);
            level.setBlock(log.below(), ModBlocks.MANA_POD.get().defaultBlockState().setValue(ManaPodBlock.AGE, i), 3);
            if (level.getBlockEntity(log.below()) instanceof ManaPodBlockEntity be) be.setAspect(aspects[i]);
        }
        BlockPos table = base.offset(0, 1, 2);
        level.setBlock(table, ModBlocks.DECONSTRUCTION_TABLE.get().defaultBlockState(), 3);
        for (int i = 0; i < 6; i++) p.getInventory().setItem(i, ManaBeanItem.of(aspects[i], 4));
        p.teleportTo(level, base.getX() + 0.5, base.getY() + 1, base.getZ() - 1.5, Set.of(), 0f, -5f, true);
        return table;
    }
    private ManaDebugScene() {}
}
