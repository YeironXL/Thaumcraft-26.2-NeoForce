package art.arcane.thaumcraft.events;

import art.arcane.thaumcraft.aspect.Aspect;
import art.arcane.thaumcraft.block.fluid.LiquidDeathBlock;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.scan.ScanManager;
import java.util.ArrayList;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** TC6 EntityEvents (dissolve drops): creatures killed by liquid death drop crystals of their aspects. */
public final class EntityEvents {
    public static void register() {
        NeoForge.EVENT_BUS.addListener(EntityEvents::drops);
        NeoForge.EVENT_BUS.addListener(EntityEvents::itemExpire);
    }

    /** Bath salts that dissolve (expire) in a still water source turn it into purifying fluid. */
    private static void itemExpire(net.neoforged.neoforge.event.entity.item.ItemExpireEvent event) {
        var item = event.getEntity();
        if (!(item.getItem().getItem() instanceof art.arcane.thaumcraft.item.BathSaltsItem)) return;
        var bp = item.blockPosition();
        var bs = item.level().getBlockState(bp);
        if (bs.is(net.minecraft.world.level.block.Blocks.WATER) && bs.getFluidState().isSource())
            item.level().setBlockAndUpdate(bp, art.arcane.thaumcraft.ModBlocks.PURIFYING_FLUID.get().defaultBlockState());
    }

    private static void drops(LivingDropsEvent event) {
        var e = event.getEntity();
        if (!event.getSource().is(LiquidDeathBlock.DISSOLVE)) return;
        var aspects = ScanManager.entityAspects(e);
        if (aspects == null || aspects.isEmpty()) return;
        var al = new ArrayList<>(aspects.keySet());
        int visSize = aspects.values().stream().mapToInt(Integer::intValue).sum();
        var rand = e.level().getRandom();
        int q = Mth.nextInt(rand, 1, 1 + visSize / 10);
        for (int a = 0; a < q; ++a) {
            Aspect aspect = al.get(rand.nextInt(al.size()));
            event.getDrops().add(new ItemEntity(e.level(), e.getX(), e.getY() + e.getEyeHeight(), e.getZ(), CrystalEssenceItem.of(aspect.id(), 1)));
        }
    }
    private EntityEvents() {}
}
