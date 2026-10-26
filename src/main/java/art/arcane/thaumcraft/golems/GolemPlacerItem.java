package art.arcane.thaumcraft.golems;

import art.arcane.thaumcraft.registry.ModComponents;
import art.arcane.thaumcraft.registry.ModEntities;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** TC6 ItemGolemPlacer: a packed golem; using it on a solid block's face builds the golem there for its user. */
public class GolemPlacerItem extends Item implements ISealDisplayer {
    public GolemPlacerItem(Properties p) { super(p); }

    public static ItemStack of(long props) { ItemStack s = new ItemStack(art.arcane.thaumcraft.ModItems.GOLEM.get()); s.set(ModComponents.GOLEM_PROPS.get(), props); return s; }
    public static GolemProperties props(ItemStack s) { Long l = s.get(ModComponents.GOLEM_PROPS.get()); return l == null ? null : GolemProperties.fromLong(l); }

    /** getSubItems: plain, basic smart, iron fine-armed smart and thaumium clawed smart golems. */
    public static List<ItemStack> creativeVariants() {
        GolemProperties a = new GolemProperties();
        GolemProperties b = new GolemProperties(); b.setHead(GolemProperties.HEADS.get(1)); b.setArms(GolemProperties.ARMS.get(1));
        GolemProperties c = new GolemProperties(); c.setMaterial(GolemProperties.MATERIALS.get(1)); c.setHead(GolemProperties.HEADS.get(1)); c.setArms(GolemProperties.ARMS.get(2));
        GolemProperties d = new GolemProperties(); d.setMaterial(GolemProperties.MATERIALS.get(4)); d.setHead(GolemProperties.HEADS.get(1)); d.setArms(GolemProperties.ARMS.get(3));
        return List.of(of(a.toLong()), of(b.toLong()), of(c.toLong()), of(d.toLong()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay d, Consumer<Component> lines, TooltipFlag flag) {
        GolemProperties props = props(stack);
        if (props == null) return;
        if (props.hasTrait(GolemTrait.SMART)) {
            if (props.getRank() >= 10) lines.accept(Component.translatable("golem.rank").append(" " + props.getRank()).withStyle(ChatFormatting.GOLD));
            else {
                int rx = stack.getOrDefault(ModComponents.GOLEM_XP.get(), 0);
                int xn = (props.getRank() + 1) * (props.getRank() + 1) * GolemEntity.XPM;
                lines.accept(Component.translatable("golem.rank").append(" " + props.getRank()).withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(" (" + rx + "/" + xn + ")").withStyle(ChatFormatting.DARK_GREEN)));
            }
        }
        lines.accept(props.getMaterial().localizedName().copy().withStyle(ChatFormatting.GREEN));
        for (GolemTrait tag : props.getTraits()) lines.accept(Component.literal("-").append(tag.localizedName()).withStyle(ChatFormatting.BLUE));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level world = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();
        if (!world.getBlockState(pos).isSolid()) return InteractionResult.FAIL;
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        pos = pos.relative(ctx.getClickedFace());
        ItemStack stack = ctx.getItemInHand();
        if (player != null && !player.mayUseItemAt(pos, ctx.getClickedFace(), stack)) return InteractionResult.FAIL;
        GolemEntity golem = ModEntities.GOLEM.get().create(world, EntitySpawnReason.SPAWN_ITEM_USE);
        if (golem == null) return InteractionResult.FAIL;
        golem.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0f, 0.0f);
        GolemProperties props = props(stack);
        if (props != null) golem.setProperties(props);
        golem.rankXp = stack.getOrDefault(ModComponents.GOLEM_XP.get(), 0);
        golem.setOwned(true);
        golem.setValidSpawn();
        if (player != null) golem.setOwnerId(player.getUUID());
        golem.initialSpawn();
        if (world.addFreshEntity(golem)) {
            if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
