package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.infusion.InfusionEnchantment;
import art.arcane.thaumcraft.infusion.InfusionRecipe;
import art.arcane.thaumcraft.item.gear.ElementalShovelItem;
import art.arcane.thaumcraft.item.gear.FortressArmorItem;
import art.arcane.thaumcraft.util.Baubles;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** TC6 elemental tools and fortress armour: real infusion outputs used the way a player would. */
final class ElementalGearVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }

    private static InfusionRecipe recipe(GameTestHelper h, String id) {
        var holder = h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("thaumcraft", id)));
        check(holder.isPresent(), "Recipe " + id + " missing");
        return (InfusionRecipe) holder.get().value();
    }
    private static ItemStack craft(GameTestHelper h, ServerPlayer p, String id, ItemStack central) {
        return recipe(h, id).output(p, central, List.of()).stack();
    }
    private static BlockHitResult hit(GameTestHelper h, BlockPos rel, Direction face) {
        BlockPos abs = h.absolutePos(rel);
        return new BlockHitResult(Vec3.atCenterOf(abs).relative(face, 0.5), face, abs, false);
    }

    /** Axe of the Stream: stats, and the recipe's BURROWING + COLLECTOR fell a log column from the top down into the player's hands. */
    static void axe(GameTestHelper h) {
        var level = h.getLevel();
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack axe = craft(h, player, "elementalaxe", new ItemStack(ModItems.THAUMIUM_AXE.get()));
        check(axe.is(ModItems.ELEMENTAL_AXE.get()), "Recipe made " + axe);
        check(InfusionEnchantment.getInfusionEnchantmentLevel(axe, InfusionEnchantment.BURROWING) == 1
                && InfusionEnchantment.getInfusionEnchantmentLevel(axe, InfusionEnchantment.COLLECTOR) == 1, "Default infusion enchantments missing");
        check(axe.getMaxDamage() == 1500, "Durability " + axe.getMaxDamage());
        check(axe.isCorrectToolForDrops(Blocks.OAK_LOG.defaultBlockState()) && axe.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) == 9.0f, "Not an elemental-tier axe");
        check(axe.isValidRepairItem(new ItemStack(ModItems.INGOT_THAUMIUM.get())), "Thaumium should repair it");
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        player.setPos(h.absoluteVec(new Vec3(8.5, 1, 8.5)));
        for (int y = 1; y <= 5; y++) h.setBlock(new BlockPos(3, y, 3), Blocks.OAK_LOG);
        player.gameMode.destroyBlock(h.absolutePos(new BlockPos(3, 1, 3)));
        check(level.getBlockState(h.absolutePos(new BlockPos(3, 1, 3))).is(Blocks.OAK_LOG), "Burrowing should leave the struck log");
        check(level.getBlockState(h.absolutePos(new BlockPos(3, 5, 3))).isAir(), "Burrowing should take the top log first");
        for (int i = 0; i < 4; i++) player.gameMode.destroyBlock(h.absolutePos(new BlockPos(3, 1, 3)));
        for (int y = 1; y <= 5; y++) check(level.getBlockState(h.absolutePos(new BlockPos(3, y, 3))).isAir(), "Log left at y=" + y);
        int near = level.getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(new BlockPos(3, 3, 3))).inflate(4), e -> e.getItem().is(Items.OAK_LOG) && e instanceof art.arcane.thaumcraft.entity.FollowingItemEntity)
                .stream().mapToInt(e -> e.getItem().getCount()).sum();
        check(near == 5, "Collector should send all 5 logs flying to the player, got " + near);
        // Holding right click draws loose items in.
        var stray = new ItemEntity(level, player.getX() + 6, player.getY() + 0.5, player.getZ(), new ItemStack(Items.STICK));
        stray.setNoGravity(true);
        level.addFreshEntity(stray);
        art.arcane.thaumcraft.item.gear.ElementalAxeItem.pull(level, player);
        check(stray.getDeltaMovement().x < 0, "Item not pulled towards the user: " + stray.getDeltaMovement());
        System.out.println("Elemental axe passed: stats, recipe enchantments, top-down felling of 5 logs, collector delivery, item pull.");
        h.succeed();
    }

    /** Shovel of the Earthmover: 3x3 placement from inventory in orientations 0 and 1; Hoe of Growth tills 3x3. */
    static void shovel(GameTestHelper h) {
        var level = h.getLevel();
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack shovel = craft(h, player, "elementalshovel", new ItemStack(ModItems.THAUMIUM_SHOVEL.get()));
        check(InfusionEnchantment.getInfusionEnchantmentLevel(shovel, InfusionEnchantment.DESTRUCTIVE) == 1, "Destructive missing");
        player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
        player.setYRot(0);
        player.getInventory().add(new ItemStack(Items.DIRT, 20));
        h.setBlock(new BlockPos(5, 1, 5), Blocks.DIRT);
        var r = shovel.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(h, new BlockPos(5, 1, 5), Direction.UP)));
        check(r.consumesAction(), "Shovel use failed: " + r);
        for (int x = 4; x <= 6; x++) for (int z = 4; z <= 6; z++)
            check(level.getBlockState(h.absolutePos(new BlockPos(x, 2, z))).is(Blocks.DIRT), "No dirt placed at " + x + ",2," + z);
        check(player.getInventory().countItem(Items.DIRT) == 11, "Should use 9 dirt, left " + player.getInventory().countItem(Items.DIRT));
        check(shovel.getDamageValue() == 9, "One durability per block, got " + shovel.getDamageValue());
        // Orientation 1: a vertical panel standing out of the top face.
        ElementalShovelItem.setOrientation(shovel, (byte) 1);
        h.setBlock(new BlockPos(1, 1, 9), Blocks.DIRT);
        shovel.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(h, new BlockPos(1, 1, 9), Direction.UP)));
        // Centred on the block above the face: rows y=1..3, the clicked block itself filling the middle of the bottom row.
        for (int x = 0; x <= 2; x++) for (int y = 1; y <= 3; y++)
            check(level.getBlockState(h.absolutePos(new BlockPos(x, y, 9))).is(Blocks.DIRT), "Vertical panel missing " + x + "," + y);
        check(player.getInventory().countItem(Items.DIRT) == 3, "Second panel should use 8 dirt, left " + player.getInventory().countItem(Items.DIRT));
        // Hoe of Growth.
        ItemStack hoe = new ItemStack(ModItems.ELEMENTAL_HOE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, hoe);
        for (int x = 8; x <= 10; x++) for (int z = 1; z <= 3; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
        hoe.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit(h, new BlockPos(9, 1, 2), Direction.UP)));
        for (int x = 8; x <= 10; x++) for (int z = 1; z <= 3; z++)
            check(level.getBlockState(h.absolutePos(new BlockPos(x, 1, z))).is(Blocks.FARMLAND), "Not tilled " + x + "," + z);
        System.out.println("Elemental shovel passed: 3x3 placement flat and vertical, inventory and durability use; hoe tills 3x3.");
        h.succeed();
    }

    private static double bonus(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a) {
        var m = p.getAttribute(a).getModifier(FortressArmorItem.SET_BONUS);
        return m == null ? 0 : m.amount();
    }
    /** Sum of the worn items' own attribute modifiers (mock players never tick, so equipment attributes are not applied). */
    private static double base(ServerPlayer p, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> a) {
        double[] t = {0};
        for (EquipmentSlot slot : EquipmentSlot.values()) p.getItemBySlot(slot).forEachModifier(slot, (attr, mod) -> { if (attr.equals(a)) t[0] += mod.amount(); });
        return t[0];
    }

    /** Fortress armour: base stats, per-piece set bonus, mask bonus, goggles revealing, magic damage ratio. */
    static void fortress(GameTestHelper h) {
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack helm = craft(h, player, "thaumiumfortresshelm", new ItemStack(ModItems.THAUMIUM_HELM.get()));
        check(helm.is(ModItems.FORTRESS_HELM.get()) && helm.getMaxDamage() == 11 * 40, "Helm " + helm + " durability " + helm.getMaxDamage());
        player.setItemSlot(EquipmentSlot.HEAD, helm);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.FORTRESS_CHEST.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.FORTRESS_LEGS.get()));
        FortressArmorItem.updateSetBonus(player);
        check(bonus(player, Attributes.ARMOR) == 3 && base(player, Attributes.ARMOR) == 16, "Armour with 3 pieces " + base(player, Attributes.ARMOR) + "+" + bonus(player, Attributes.ARMOR));
        check(bonus(player, Attributes.ARMOR_TOUGHNESS) == 3 && base(player, Attributes.ARMOR_TOUGHNESS) == 9, "Toughness " + base(player, Attributes.ARMOR_TOUGHNESS) + "+" + bonus(player, Attributes.ARMOR_TOUGHNESS));
        check(!Baubles.hasRevealer(player), "Plain helm should not reveal");
        // Grinning Devil mask and goggles through their infusion recipes (result_tag on the central helm).
        ItemStack masked = craft(h, player, "helmgoggles", craft(h, player, "maskgrinningdevil", helm));
        check(FortressArmorItem.getMask(masked) == 0 && FortressArmorItem.hasGoggles(masked), "Mask/goggles tags not applied");
        player.setItemSlot(EquipmentSlot.HEAD, masked);
        FortressArmorItem.updateSetBonus(player);
        check(bonus(player, Attributes.ARMOR) == 6, "Armour bonus with masked helm " + bonus(player, Attributes.ARMOR));
        check(Baubles.hasRevealer(player), "Helm of Revealing should reveal");
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        FortressArmorItem.updateSetBonus(player);
        check(bonus(player, Attributes.ARMOR) == 4, "Armour bonus with 2 pieces " + bonus(player, Attributes.ARMOR));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.FORTRESS_CHEST.get()));
        // Magic damage ignores vanilla armour but is cut by 16/35 by the fortress pieces.
        // Real hits through the ISpecialArmor pipeline (equipment attributes applied by hand: mock players never tick).
        player.setItemSlot(EquipmentSlot.HEAD, helm);
        FortressArmorItem.updateSetBonus(player);
        for (EquipmentSlot slot : new EquipmentSlot[] { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS })
            player.getItemBySlot(slot).forEachModifier(slot, (attr, mod) -> player.getAttribute(attr).addOrUpdateTransientModifier(mod));
        var ds = h.getLevel().damageSources();
        // Physical: each piece absorbs def/25 (0.64 in all), then vanilla armour 16 + 3 bonus, toughness 9 + 3.
        float phys = art.arcane.thaumcraft.item.gear.SpecialArmor.applyArmor(player, ds.cactus(), 10.0f);
        float physExp = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(player, 10.0f * (1 - 16 / 25.0f), ds.cactus(), 19, 12);
        check(Math.abs(phys - physExp) < 1e-3, "Physical hit " + phys + " expected " + physExp);
        check(helm.getDamageValue() == 2 && player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == 3 && player.getItemBySlot(EquipmentSlot.LEGS).getDamageValue() == 3,
                "Durability " + helm.getDamageValue() + "," + player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() + "," + player.getItemBySlot(EquipmentSlot.LEGS).getDamageValue());
        // Fire: priority 1 at def/20.
        float fire = art.arcane.thaumcraft.item.gear.SpecialArmor.applyArmor(player, ds.inFire(), 10.0f);
        float fireExp = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(player, 10.0f * (1 - 16 / 20.0f), ds.inFire(), 19, 12);
        check(Math.abs(fire - fireExp) < 1e-3, "Fire hit " + fire + " expected " + fireExp);
        // Magic bypasses vanilla armour: def/35, then only the special pieces' bonus armour/toughness.
        float mag = art.arcane.thaumcraft.item.gear.SpecialArmor.applyArmor(player, ds.magic(), 10.0f);
        float magExp = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(player, 10.0f * (1 - 16 / 35.0f), ds.magic(), 3, 3);
        check(Math.abs(mag - magExp) < 1e-3, "Magic hit " + mag + " expected " + magExp);
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        FortressArmorItem.updateSetBonus(player);
        check(bonus(player, Attributes.ARMOR) == 0 && bonus(player, Attributes.ARMOR_TOUGHNESS) == 0, "Bonus not removed");
        System.out.println("Fortress armour passed: stats, set bonus per piece, mask bonus, goggles revealing, real physical/fire/magic hits and wear, removal.");
        h.succeed();
    }

    /** Other infusion outputs: void robe repair/discount/warp/reveal, traveller's boots vis use, causality collapser vs. a rift, crusher self repair. */
    static void infusionGear(GameTestHelper h) {
        var level = h.getLevel();
        var player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack hood = new ItemStack(ModItems.VOID_ROBE_HELM.get());
        check(hood.getItem() instanceof art.arcane.thaumcraft.item.VisDiscountGear g && g.getVisDiscount(hood, player) == 5, "Void robe discount");
        check(((art.arcane.thaumcraft.item.WarpingGear) hood.getItem()).getWarp(hood, player) == 3, "Void robe warp");
        hood.setDamageValue(10);
        player.setItemSlot(EquipmentSlot.HEAD, hood);
        check(Baubles.hasRevealer(player), "Void robe hood should reveal");
        player.tickCount = 20;
        hood.getItem().inventoryTick(hood, level, player, EquipmentSlot.HEAD);
        check(hood.getDamageValue() == 8, "Worn void robe should repair 2 a second, damage " + hood.getDamageValue());
        ItemStack crusher = recipe(h, "primalcrusher").output(player, new ItemStack(ModItems.PRIMORDIAL_PEARL.get()), List.of()).stack();
        check(crusher.is(ModItems.PRIMAL_CRUSHER.get()) && crusher.getMaxDamage() == 500, "Crusher " + crusher);
        check(crusher.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()) && crusher.getDestroySpeed(Blocks.GRAVEL.defaultBlockState()) == 8.0f, "Crusher mining");
        crusher.setDamageValue(5);
        crusher.getItem().inventoryTick(crusher, level, player, null);
        check(crusher.getDamageValue() == 4, "Crusher self repair");
        // Traveller's boots spend one vis per minute.
        ItemStack boots = new ItemStack(ModItems.TRAVELLER_BOOTS.get());
        check(boots.getMaxDamage() == 350, "Boots durability " + boots.getMaxDamage());
        art.arcane.thaumcraft.item.RechargeHelper.rechargeItemBlindly(boots, player, 240);
        player.setItemSlot(EquipmentSlot.FEET, boots);
        player.tickCount = 40;
        art.arcane.thaumcraft.item.gear.TravellerBootsItem.tick(player);
        check(art.arcane.thaumcraft.item.RechargeHelper.getCharge(boots) == 239, "Boots charge " + art.arcane.thaumcraft.item.RechargeHelper.getCharge(boots));
        player.tickCount = 60;
        art.arcane.thaumcraft.item.gear.TravellerBootsItem.tick(player);
        check(art.arcane.thaumcraft.item.RechargeHelper.getCharge(boots) == 239, "Boots should run 60 s on one vis");
        // Causality collapser: a rift within 3 blocks of the impact collapses.
        var rift = new art.arcane.thaumcraft.entity.FluxRiftEntity(art.arcane.thaumcraft.registry.ModEntities.FLUX_RIFT.get(), level);
        Vec3 c = h.absoluteVec(new Vec3(5.5, 3, 5.5));
        rift.setPos(c);
        level.addFreshEntity(rift);
        art.arcane.thaumcraft.entity.CausalityCollapserProjectile.collapseRifts(level, c.x + 2, c.y, c.z);
        check(rift.getCollapse(), "Rift should collapse");
        rift.discard();
        check(new ItemStack(ModItems.CAUSALITY_COLLAPSER.get()).getMaxStackSize() == 16, "Collapser stack size");
        System.out.println("Infusion gear passed: void robe, primal crusher, traveller's boots, causality collapser.");
        h.succeed();
    }
}
