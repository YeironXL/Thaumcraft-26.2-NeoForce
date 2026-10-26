package art.arcane.thaumcraft.aspect;

import art.arcane.thaumcraft.blockentity.CrucibleBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaCentrifugeBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaTubeBlockEntity;
import art.arcane.thaumcraft.blockentity.InfusionMatrixBlockEntity;
import art.arcane.thaumcraft.blockentity.PotionSprayerBlockEntity;
import art.arcane.thaumcraft.blockentity.RechargePedestalBlockEntity;
import art.arcane.thaumcraft.blockentity.ThaumatoriumBlockEntity;
import art.arcane.thaumcraft.blockentity.ThaumatoriumTopBlockEntity;
import art.arcane.thaumcraft.item.IRechargable;
import art.arcane.thaumcraft.item.RechargeHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * TC6 IAspectContainer.getAspects() of the tiles implementing it (TileJarFillable/Void, TileAlembic, TileCrucible,
 * TileCentrifuge, TileTubeBuffer, TileTubeFilter, TileInfusionMatrix, TileThaumatorium/Top, TilePotionSprayer,
 * TileRechargePedestal), mapped onto the port's block entities. Null when the block entity is no aspect container.
 * An amount of -1 (tube filter) draws the icon without a number.
 */
public final class ContainerAspects {
    public static Map<Aspect, Integer> of(BlockEntity be) {
        Map<String, Integer> raw = new LinkedHashMap<>();
        switch (be) {
            case EssentiaJarBlockEntity jar -> { var c = jar.contents(); if (!c.aspect().isEmpty() && c.amount() > 0) raw.put(c.aspect(), c.amount()); }
            case CrucibleBlockEntity c -> raw.putAll(c.mixture().aspects());
            case EssentiaCentrifugeBlockEntity c -> { if (!c.aspect().isEmpty()) raw.put(c.aspect(), 1); }
            case EssentiaTubeBlockEntity t when t.kind() == EssentiaTubeBlockEntity.Kind.BUFFER -> raw.putAll(t.contents());
            case EssentiaTubeBlockEntity t when t.kind() == EssentiaTubeBlockEntity.Kind.FILTER -> { if (t.filter().isEmpty()) return null; raw.put(t.filter(), -1); }
            case InfusionMatrixBlockEntity m -> raw.putAll(m.recipeEssentia());
            case ThaumatoriumBlockEntity t -> raw.putAll(t.essentia);
            case ThaumatoriumTopBlockEntity t -> {
                if (t.getLevel() == null || !(t.getLevel().getBlockEntity(t.getBlockPos().below()) instanceof ThaumatoriumBlockEntity base)) return null;
                raw.putAll(base.essentia);
            }
            case PotionSprayerBlockEntity s -> raw.putAll(s.progress);
            case art.arcane.thaumcraft.blockentity.AuraNodeBlockEntity n -> raw.putAll(n.aspects);
            case RechargePedestalBlockEntity p -> {
                if (!(p.item().getItem() instanceof IRechargable)) return null;
                raw.put(AspectRegistry.POTENTIA.id(), RechargeHelper.getCharge(p.item()));
            }
            default -> { return null; }
        }
        Map<Aspect, Integer> out = new LinkedHashMap<>();
        raw.forEach((id, n) -> { var a = AspectRegistry.get(id); if (a != null) out.put(a, n); });
        return out;
    }

    private ContainerAspects() {}
}
