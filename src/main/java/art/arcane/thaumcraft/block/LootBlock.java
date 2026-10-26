package art.arcane.thaumcraft.block;

import art.arcane.thaumcraft.item.TcLoot;
import art.arcane.thaumcraft.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.DeferredSoundType;

/**
 * TC6 BlockLoot: the urns (stone) and crates (wood) of the Outer Lands. Breaking one drops 1 + rarity + 0..2 rolls of
 * Utils.generateLoot for its rarity; silk touch keeps the block. Hardness 0.15, no blast resistance.
 */
public class LootBlock extends Block {
    public static final SoundType URN = new DeferredSoundType(0.5f, 1.5f, () -> ModSounds.get("urnbreak"), () -> ModSounds.get("urnbreak"),
            () -> ModSounds.get("urnbreak"), () -> ModSounds.get("urnbreak"), () -> ModSounds.get("urnbreak"));
    private static final VoxelShape URN_SHAPE = Block.box(2, 1, 2, 14, 13, 14);
    private static final VoxelShape CRATE_SHAPE = Block.box(1, 0, 1, 15, 14, 15);
    private final int rarity;
    private final boolean urn;

    public LootBlock(Properties p, int rarity, boolean urn) {
        super(p.strength(0.15F, 0.0F).noOcclusion().sound(urn ? URN : SoundType.WOOD));
        this.rarity = rarity;
        this.urn = urn;
    }

    @Override protected MapCodec<? extends Block> codec() { return simpleCodec(p -> new LootBlock(p, rarity, urn)); }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return urn ? URN_SHAPE : CRATE_SHAPE; }

    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ServerLevel level = params.getLevel();
        net.minecraft.world.item.ItemInstance tool = params.getOptionalParameter(LootContextParams.TOOL);
        if (tool != null && tool.count() > 0) {
            var silk = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH);
            if (EnchantmentHelper.getItemEnchantmentLevel(silk, tool) > 0) return List.of(new ItemStack(this));
        }
        List<ItemStack> ret = new ArrayList<>();
        var rand = level.getRandom();
        for (int q = 1 + rarity + rand.nextInt(3), a = 0; a < q; ++a) {
            ItemStack is = TcLoot.generate(rarity, rand, level.registryAccess());
            if (!is.isEmpty()) ret.add(is);
        }
        return ret;
    }
}
