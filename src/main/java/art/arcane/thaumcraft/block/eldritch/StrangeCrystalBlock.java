package art.arcane.thaumcraft.block.eldritch;

import art.arcane.thaumcraft.item.CrystalEssenceItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * TC4 "Strange Crystals": white crystal clusters on the ceilings and walls of the Outer Lands; they drop a Balanced
 * Shard. TC6 has no balanced shard, so they drop one crystal of each primal aspect instead.
 */
public final class StrangeCrystalBlock extends AmethystClusterBlock {
    public static final MapCodec<StrangeCrystalBlock> CODEC = simpleCodec(StrangeCrystalBlock::new);
    public static final String[] PRIMALS = {"aer", "terra", "ignis", "aqua", "ordo", "perditio"};
    public StrangeCrystalBlock(Properties p) { super(12, 1, p.strength(0.25F).sound(SoundType.AMETHYST_CLUSTER).noOcclusion().noCollision().lightLevel(s -> 5)); }
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override public MapCodec<AmethystClusterBlock> codec() { return (MapCodec) CODEC; }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> out = new java.util.ArrayList<>();
        for (String a : PRIMALS) out.add(CrystalEssenceItem.of(a, 1));
        return out;
    }
    @Override public void animateTick(BlockState s, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(6) == 0) level.addParticle(ParticleTypes.END_ROD, pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(), 0, 0, 0);
    }
}
