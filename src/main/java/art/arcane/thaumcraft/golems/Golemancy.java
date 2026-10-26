package art.arcane.thaumcraft.golems;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.golems.seals.*;

/** ConfigItems seal registration (TC6 order defines the seal ids) and golemancy event wiring. */
public final class Golemancy {
    private static boolean done;
    public static void init() {
        if (done) return;
        done = true;
        GolemProperties.init();
        SealHandler.registerSeal(SealPickup::new, ModItems.SEAL_PICKUP);
        SealHandler.registerSeal(SealPickupAdvanced::new, ModItems.SEAL_PICKUP_ADVANCED);
        SealHandler.registerSeal(SealFill::new, ModItems.SEAL_FILL);
        SealHandler.registerSeal(SealFillAdvanced::new, ModItems.SEAL_FILL_ADVANCED);
        SealHandler.registerSeal(SealEmpty::new, ModItems.SEAL_EMPTY);
        SealHandler.registerSeal(SealEmptyAdvanced::new, ModItems.SEAL_EMPTY_ADVANCED);
        SealHandler.registerSeal(SealHarvest::new, ModItems.SEAL_HARVEST);
        SealHandler.registerSeal(SealButcher::new, ModItems.SEAL_BUTCHER);
        SealHandler.registerSeal(SealGuard::new, ModItems.SEAL_GUARD);
        SealHandler.registerSeal(SealGuardAdvanced::new, ModItems.SEAL_GUARD_ADVANCED);
        SealHandler.registerSeal(SealLumber::new, ModItems.SEAL_LUMBER);
        SealHandler.registerSeal(SealBreaker::new, ModItems.SEAL_BREAKER);
        SealHandler.registerSeal(SealUse::new, ModItems.SEAL_USE);
        SealHandler.registerSeal(SealProvide::new, ModItems.SEAL_PROVIDER);
        SealHandler.registerSeal(SealStock::new, ModItems.SEAL_STOCK);
        SealHandler.registerSeal(SealBreakerAdvanced::new, ModItems.SEAL_BREAKER_ADVANCED);
        // TC4 golem cores recreated as seals (added after the TC6 seals so existing ids keep their order)
        SealHandler.registerSeal(SealFishing::new, ModItems.SEAL_FISHING);
        SealHandler.registerSeal(SealFluid::new, ModItems.SEAL_FLUID);
        SealHandler.registerSeal(SealEssentia::new, ModItems.SEAL_ESSENTIA);
        GolemEvents.register();
    }
    private Golemancy() {}
}
