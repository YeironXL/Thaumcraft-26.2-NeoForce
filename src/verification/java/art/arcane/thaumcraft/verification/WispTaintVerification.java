package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.aura.NativeAura;
import art.arcane.thaumcraft.block.world.taint.TaintFibreBlock;
import art.arcane.thaumcraft.block.world.taint.TaintHelper;
import art.arcane.thaumcraft.entity.FluxRiftEntity;
import art.arcane.thaumcraft.entity.TaintSeedEntity;
import art.arcane.thaumcraft.entity.TaintSeedPrimeEntity;
import art.arcane.thaumcraft.entity.WispEntity;
import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.registry.ModEffects;
import art.arcane.thaumcraft.registry.ModEntities;
import art.arcane.thaumcraft.scan.ScanManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** TC6 EntityWisp, EntityTaintSeed(Prime), taint spreading and the flux rift events 0 (wisp) and 1 (giant taint seed). */
public final class WispTaintVerification {
    private static void check(boolean c, String m) { if (!c) throw new AssertionError(m); }

    public static void test(GameTestHelper h) {
        var level = h.getLevel();
        // --- wisp: registration, natural nether spawns, attributes, aspect, scan, zap and crystal drop
        var wt = ModEntities.WISP.get();
        check(wt.getCategory() == MobCategory.MONSTER && Math.abs(wt.getWidth() - 0.9f) < 1e-4 && Math.abs(wt.getHeight() - 0.9f) < 1e-4, "Wisp type");
        var nether = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.NETHER_WASTES).value();
        check(nether.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(w -> w.value().type() == wt && w.weight() == 5
                && w.value().minCount() == 1 && w.value().maxCount() == 1), "Nether biomes lack the 5/1/1 wisp spawn");
        WispEntity wisp = h.spawn(wt, new BlockPos(18, 6, 18));
        check(wisp.getAttributeBaseValue(Attributes.MAX_HEALTH) == 22.0 && wisp.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == 3.0, "Wisp attributes");
        wisp.setWispType("ignis");
        var asp = ScanManager.entityAspects(wisp);
        check(asp.getOrDefault(AspectRegistry.IGNIS, 0) == 5 && asp.getOrDefault(AspectRegistry.AURAM, 0) == 5 && asp.getOrDefault(AspectRegistry.VOLATUS, 0) == 5, "Wisp aspects");
        var pig = h.spawn(EntityTypes.PIG, new BlockPos(18, 2, 16));
        pig.setNoAi(true);
        float ph = pig.getHealth();
        for (int i = 0; i < 20 && pig.getHealth() >= ph; i++) { pig.invulnerableTime = 0; wisp.zap(level, pig); }
        check(pig.getHealth() < ph, "Wisp zaps do not hurt");
        pig.discard();
        WispEntity blank = h.spawn(wt, new BlockPos(17, 6, 18));

        // dark stone box around the rift so rift wisps pass isValidLightLevel
        for (int x = 1; x <= 15; x++) for (int y = 1; y <= 15; y++) for (int z = 1; z <= 15; z++)
            if (x == 1 || x == 15 || y == 1 || y == 15 || z == 1 || z == 15) h.setBlock(new BlockPos(x, y, z), Blocks.STONE);

        h.runAfterDelay(25, () -> {
            check(blank.getAspect() != null, "A wisp without type must pick an aspect");
            BlockPos wp = wisp.blockPosition();
            wisp.kill(level);
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(wp).inflate(3));
            check(drops.stream().anyMatch(e -> "ignis".equals(CrystalEssenceItem.aspect(e.getItem()))), "Ignis wisp did not drop an ignis crystal");
            blank.discard();

            // --- rift event 0: a wisp near the rift, stability +5
            var c = net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(8, 8, 8)));
            var box = new AABB(c, c).inflate(40);
            var rift = new FluxRiftEntity(ModEntities.FLUX_RIFT.get(), level);
            rift.snapTo(c.x, c.y, c.z, 0, 0);
            rift.setRiftSeed(7);
            level.addFreshEntity(rift);
            rift.setRiftSize(40);
            rift.setRiftStability(-10);
            for (int i = 0; i < 400 && level.getEntitiesOfClass(WispEntity.class, box, e -> e.isAlive()).isEmpty(); i++) rift.executeRiftEvent(level, FluxRiftEntity.EVENTS[0]);
            var wisps = level.getEntitiesOfClass(WispEntity.class, box, e -> e.isAlive());
            check(wisps.size() == 1, "Rift event 0 did not release a wisp");
            check(rift.getRiftStability() == -5.0f, "Wisp event cost: " + rift.getRiftStability());
            wisps.forEach(WispEntity::discard);

            // --- rift event 1: giant taint seed boosted by the rift's size; the rift is consumed
            float flux = NativeAura.flux(level, rift.blockPosition());
            rift.executeRiftEvent(level, FluxRiftEntity.EVENTS[1]);
            var primes = level.getEntitiesOfClass(TaintSeedPrimeEntity.class, box);
            check(primes.size() == 1 && primes.get(0).boost == 40, "Rift event 1 did not spawn a boosted giant seed");
            check(rift.isRemoved(), "Rift survived its taint seed event");
            check(NativeAura.flux(level, rift.blockPosition()) >= flux + 19.9f, "Seed event did not pollute");
            NativeAura.drainFlux(level, rift.blockPosition(), 20.0f, false); // leave neighbouring tests' aura as found
            TaintSeedPrimeEntity prime = primes.get(0);
            check(prime.getAttributeBaseValue(Attributes.MAX_HEALTH) == 150.0 && prime.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == 7.0, "Giant seed attributes");
            h.runAfterDelay(2, () -> {
                // a second rift near the seed: event 1 is barred near taint
                var rift2 = new FluxRiftEntity(ModEntities.FLUX_RIFT.get(), level);
                rift2.snapTo(c.x, c.y, c.z, 0, 0);
                level.addFreshEntity(rift2);
                rift2.setRiftSize(20);
                check(TaintHelper.isNearTaintSeed(level, rift2.blockPosition()), "Seed not registered with TaintHelper");
                rift2.executeRiftEvent(level, FluxRiftEntity.EVENTS[1]);
                check(level.getEntitiesOfClass(TaintSeedPrimeEntity.class, box).size() == 1 && !rift2.isRemoved(), "Event 1 ran near a taint seed");
                rift2.discard();
                prime.discard();
                seedPhase(h);
            });
        });
    }

    private static void seedPhase(GameTestHelper h) {
        var level = h.getLevel();
        TaintSeedEntity seed = h.spawn(ModEntities.TAINT_SEED.get(), new BlockPos(8, 2, 8));
        check(seed.getAttributeBaseValue(Attributes.MAX_HEALTH) == 75.0 && seed.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) == 4.0, "Taint seed attributes");
        seed.boost = 400;
        var pig = h.spawn(EntityTypes.PIG, new BlockPos(10, 2, 10));
        pig.setNoAi(true);
        // a fibre on the floor connects downwards
        h.setBlock(new BlockPos(4, 2, 12), ModBlocks.TAINT_FIBRE.get());
        check(h.getBlockState(new BlockPos(4, 2, 12)).getValue(TaintFibreBlock.DOWN), "Fibre on a floor is not attached downwards");
        h.runAfterDelay(30, () -> {
            check(pig.hasEffect(ModEffects.FLUX_TAINT), "Seed did not taint a nearby creature");
            pig.discard();
        });
        h.runAfterDelay(150, () -> {
            BlockPos sp = seed.blockPosition();
            int fibres = 0;
            for (BlockPos p : BlockPos.betweenClosed(sp.offset(-8, -4, -8), sp.offset(8, 4, 8)))
                if (level.getBlockState(p).is(ModBlocks.TAINT_FIBRE.get())) fibres++;
            check(fibres > 3, "Taint seed spread too little: " + fibres);
            check(seed.getY() - h.absolutePos(new BlockPos(8, 2, 8)).getY() < 0.01 && Math.abs(seed.getX() - (h.absolutePos(new BlockPos(8, 2, 8)).getX() + 0.5)) < 0.01, "Taint seed moved");
            seed.kill(level);
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(sp).inflate(3));
            check(drops.stream().anyMatch(e -> "vitium".equals(CrystalEssenceItem.aspect(e.getItem()))), "Taint seed dropped no flux crystal");
            check(!TaintHelper.isNearTaintSeed(level, sp), "Dead seed still keeps taint alive");
            // without a seed, taint withers: fibres vanish, tainted rock turns back to porous stone
            BlockPos fp = h.absolutePos(new BlockPos(4, 2, 12));
            var fs = level.getBlockState(fp);
            fs.randomTick(level, fp, level.getRandom());
            check(level.getBlockState(fp).isAir(), "Fibre without seed did not die");
            BlockPos rp = h.absolutePos(new BlockPos(4, 1, 12));
            level.setBlockAndUpdate(rp, ModBlocks.TAINT_ROCK.get().defaultBlockState());
            ((art.arcane.thaumcraft.block.world.taint.ITaintBlock) ModBlocks.TAINT_ROCK.get()).die(level, rp, level.getBlockState(rp));
            check(level.getBlockState(rp).is(ModBlocks.STONE_POROUS.get()), "Taint rock did not revert to porous stone");
            System.out.println("Wisp/taint passed: wisp spawns/attributes/aspect/zap/drop, rift events 0 and 1, seed spreading " + fibres + " fibres, flux taint, drops and withering.");
            h.succeed();
        });
    }
}
