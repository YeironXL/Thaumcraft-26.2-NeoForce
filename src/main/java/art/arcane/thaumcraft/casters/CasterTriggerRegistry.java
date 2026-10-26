package art.arcane.thaumcraft.casters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** TC6 CasterTriggerRegistry: caster right-click actions on specific block states (multiblock formation...). */
public final class CasterTriggerRegistry {
    public interface ICasterTriggerManager { boolean performTrigger(Level world, ItemStack casterStack, Player player, BlockPos pos, Direction side, int event); }
    private record Trigger(ICasterTriggerManager manager, int event) {}
    private static final Map<String, LinkedHashMap<BlockState, List<Trigger>>> triggers = new HashMap<>();

    public static void registerWandBlockTrigger(ICasterTriggerManager manager, int event, BlockState state, String modid) {
        triggers.computeIfAbsent(modid, k -> new LinkedHashMap<>()).computeIfAbsent(state, k -> new ArrayList<>()).add(new Trigger(manager, event));
    }
    public static void registerCasterBlockTrigger(ICasterTriggerManager manager, int event, BlockState state) { registerWandBlockTrigger(manager, event, state, "default"); }
    public static boolean hasTrigger(BlockState state) {
        for (var m : triggers.values()) if (m.containsKey(state)) return true;
        return false;
    }
    public static boolean performTrigger(Level world, ItemStack casterStack, Player player, BlockPos pos, Direction side, BlockState state) {
        for (var m : triggers.values()) {
            var l = m.get(state);
            if (l == null) continue;
            for (Trigger t : l) if (t.manager.performTrigger(world, casterStack, player, pos, side, t.event)) return true;
        }
        return false;
    }
    private CasterTriggerRegistry() {}
}
