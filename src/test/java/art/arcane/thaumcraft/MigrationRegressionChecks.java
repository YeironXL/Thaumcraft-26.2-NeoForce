package art.arcane.thaumcraft;

import art.arcane.thaumcraft.aspect.AspectRegistry;
import art.arcane.thaumcraft.research.PlayerResearch;
import art.arcane.thaumcraft.research.ResearchCatalog;
import art.arcane.thaumcraft.research.ResearchGraph;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonObject;
import java.util.Set;

/** Checks for data loss and legacy data drift; requires no running Minecraft client. */
public final class MigrationRegressionChecks {
    public static void main(String[] args) {
        var tank = art.arcane.thaumcraft.essentia.EssentiaContents.EMPTY.insert("aer",240);
        require(!tank.accepts("ignis",10) && !tank.accepts("aer",11),"Jar mixed aspects or exceeded capacity");
        tank=tank.insert("aer",10);
        var encodedTank=art.arcane.thaumcraft.essentia.EssentiaContents.CODEC.encodeStart(JsonOps.INSTANCE,tank).getOrThrow();
        var restoredTank=art.arcane.thaumcraft.essentia.EssentiaContents.CODEC.parse(JsonOps.INSTANCE,encodedTank).getOrThrow();
        require(restoredTank.equals(tank) && restoredTank.extract(250).equals(art.arcane.thaumcraft.essentia.EssentiaContents.EMPTY),"Jar persistence or last extraction failed");
        require(!tank.accepts("aer",Integer.MAX_VALUE) && !tank.accepts("aer",-1),"Jar insertion overflow");
        PlayerResearch original = new PlayerResearch();
        original.unlock("FIRSTSTEPS");
        original.scan("minecraft:stone");
        original.addKnowledge(42);
        original.setPermanentWarp(2);
        original.setNormalWarp(3);
        original.setTemporaryWarp(4);
        var serialized = PlayerResearch.CODEC.codec().encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        PlayerResearch restored = PlayerResearch.CODEC.codec().parse(JsonOps.INSTANCE, serialized).getOrThrow();
        require(restored.has("FIRSTSTEPS") && restored.scanned("minecraft:stone"), "Saved discoveries were lost");
        require(restored.knowledge() == 42 && restored.permanentWarp() == 2 && restored.normalWarp() == 3
                && restored.temporaryWarp() == 4, "Saved knowledge/warp changed");
        require(!restored.scan("minecraft:stone"), "Reload awarded a duplicate scan");
        restored.unlock("ALCHEMY");
        require(!original.has("ALCHEMY"), "Copied player data aliases the original");
        restored.addKnowledge(Integer.MAX_VALUE);
        require(restored.knowledge() == Integer.MAX_VALUE, "Knowledge overflow erased progress");
        PlayerResearch defaults = PlayerResearch.CODEC.codec().parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
        require(defaults.researched().isEmpty() && defaults.knowledge() == 0, "New-player defaults changed");
        var graph = new ResearchGraph();
        graph.define("FIRSTSTEPS");
        graph.define("ALCHEMY", "FIRSTSTEPS");
        require(!graph.unlocked("ALCHEMY", Set.of()) && graph.unlocked("ALCHEMY", Set.of("FIRSTSTEPS")), "Prerequisites bypassed");
        require(!graph.unlocked("unknown", Set.of()), "Unknown research was unlocked");
        require(AspectRegistry.all().size() == 37, "TC6 aspect inventory changed");
        require(AspectRegistry.VITREUS.parentB() == AspectRegistry.AER, "TC5 crystal composition leaked into TC6");
        require(AspectRegistry.MORTUUS.parentA() == AspectRegistry.AQUA, "TC6 death composition changed");
        require(AspectRegistry.color("aer") == 0xFFFF7E, "Original aspect color changed");
        // Object aspects need recipes and tags; they are checked by the object_aspects server test.
        require(ResearchCatalog.entries().size() > 100, "Original research catalog was not loaded");
        var first = ResearchCatalog.entries().get("FIRSTSTEPS");
        require(first != null && first.stages().size() == 3 && first.parents().contains("!gotthaumonomicon"), "Original research stages changed");
        require(ResearchCatalog.entries().values().stream().anyMatch(entry -> entry.key().equals("!Wisp")), "Scan discoveries missing");
        checkStages();
        checkAura();
        checkAlchemy();
        checkScanning();
        System.out.println("Migration checks passed; " + ResearchCatalog.entries().size() + " original research entries, 37 aspects.");
    }
    private static void checkScanning() {
        var research=new PlayerResearch();
        var tags=java.util.Map.of(AspectRegistry.IGNIS,10);
        require(art.arcane.thaumcraft.scan.ScanProgression.record(research,"minecraft:coal",tags) && research.has("!ignis"),"Scanning failed to discover an aspect");
        int knowledge=research.knowledge("OBSERVATION","ALCHEMY");
        require(!art.arcane.thaumcraft.scan.ScanProgression.record(research,"minecraft:coal",tags)
                && research.knowledge("OBSERVATION","ALCHEMY")==knowledge,"Repeated scan granted duplicate aspect knowledge");
        require(art.arcane.thaumcraft.scan.EntityScanRegistry.get("minecraft:zombie").get(AspectRegistry.EXANIMIS)==20,"Original entity aspect amount changed");
        require(art.arcane.thaumcraft.research.LegacyItemReferences.item("thaumcraft:nitor;1;4").equals("thaumcraft:nitor_yellow"),"Nitor metadata conversion changed");
        research.recordCraft("thaumcraft:nitor_yellow");
        require(art.arcane.thaumcraft.research.LegacyItemReferences.crafted(research,"thaumcraft:nitor;1;4"),"Modern Nitor craft failed a legacy requirement");
        require(art.arcane.thaumcraft.research.LegacyItemReferences.item("thaumcraft:nugget;1;10").equals("thaumcraft:nugget_rareearth"),"Rare earth metadata conversion changed");
        require(art.arcane.thaumcraft.research.LegacyItemReferences.item("thaumcraft:nitor;1;16").isEmpty(),"Unknown metadata bypassed migration");
    }
    private static void checkAura() {
        var aura = new art.arcane.thaumcraft.aura.ChunkAura();
        aura.initialize(250);
        require(aura.spend(20) && aura.vis() == 230, "Arcane vis cost changed");
        require(!aura.spend(231) && !aura.spend(-1) && aura.vis() == 230, "Failed spending mutated aura");
        aura.initialize(500);
        require(aura.base() == 250 && aura.vis() == 230, "Reload initialization refilled spent aura");
        aura.addFlux(5);
        var saved = art.arcane.thaumcraft.aura.ChunkAura.CODEC.codec().encodeStart(JsonOps.INSTANCE,aura).getOrThrow();
        var loaded = art.arcane.thaumcraft.aura.ChunkAura.CODEC.codec().parse(JsonOps.INSTANCE,saved).getOrThrow();
        require(loaded.vis() == 230 && loaded.flux() == 5 && loaded.base() == 250, "Aura save lost vis/flux");
    }
    private static void checkAlchemy() {
        var mixture=new art.arcane.thaumcraft.crafting.AlchemyMixture();
        require(!mixture.dissolve(java.util.Map.of("ignis",10)),"Dry crucible consumed ingredients");
        require(mixture.fill() && !mixture.fill(),"Full crucible accepted another bucket");
        for (int i=0;i<150;i++) mixture.heatTick(true);
        require(!mixture.dissolve(java.util.Map.of("ignis",10)),"Cold crucible consumed ingredients");
        mixture.heatTick(true);
        require(mixture.dissolve(java.util.Map.of("ignis",10,"potentia",10)),"Boiling crucible did not dissolve input");
        require(!mixture.pay(java.util.Map.of("ignis",11)) && mixture.water() == 1000 && mixture.total() == 20,"Failed alchemy partially charged contents");
        var saved=art.arcane.thaumcraft.crafting.AlchemyMixture.CODEC.codec().encodeStart(JsonOps.INSTANCE,mixture).getOrThrow();
        var loaded=art.arcane.thaumcraft.crafting.AlchemyMixture.CODEC.codec().parse(JsonOps.INSTANCE,saved).getOrThrow();
        require(loaded.heat()==151 && loaded.water()==1000 && loaded.aspects().equals(mixture.aspects()),"Reload lost crucible heat/water/aspects");
        require(loaded.pay(java.util.Map.of("ignis",10)) && loaded.water()==950 && loaded.total()==10,"Alchemy cost or 50 mB consumption changed");
        loaded.dissolve(java.util.Map.of("vitium",2));
        require(loaded.empty()==4.5F && loaded.total()==0 && loaded.water()==0,"Crucible emptying lost flux or failed to clear contents");
        require(loaded.empty()==0,"Repeated emptying duplicated pollution");
        mixture.dissolve(java.util.Map.of("ignis",501));
        long before=mixture.total();
        require(mixture.tickSpill(0)>0 && mixture.total()==before-1,"Overflow failed to spill one aspect into aura");
    }
    private static void checkStages() {
        var state = new PlayerResearch();
        var noop = (java.util.function.Predicate<java.util.Map<String, Integer>>)(items -> items.isEmpty());
        require(!art.arcane.thaumcraft.research.ResearchProgression.advance(state, "FIRSTSTEPS", -1, noop), "Intro bypassed book discovery");
        state.unlock("!gotthaumonomicon");
        require(art.arcane.thaumcraft.research.ResearchProgression.advance(state, "FIRSTSTEPS", -1, noop) && state.stage("FIRSTSTEPS") == 1, "Research did not start at stage one");
        require(state.has("FIRSTSTEPS@1") && !state.has("FIRSTSTEPS@2"), "Stage-qualified recipe gate is wrong");
        require(art.arcane.thaumcraft.research.ResearchRules.complete(state, "KNOWLEDGETYPES") && state.has("!gotdream"), "Original sibling grants were lost");
        require(!art.arcane.thaumcraft.research.ResearchProgression.advance(state, "FIRSTSTEPS", 1, noop), "Craft requirement bypassed");
        state.recordCraft("thaumcraft:arcane_workbench");
        require(art.arcane.thaumcraft.research.ResearchProgression.advance(state, "FIRSTSTEPS", 1, noop) && state.stage("FIRSTSTEPS") == 2, "Workbench fabrication was not recognized");
        state.recordCraft("thaumcraft:thaumometer");
        state.addKnowledge("OBSERVATION", "BASICS", 15);
        require(!art.arcane.thaumcraft.research.ResearchProgression.advance(state, "FIRSTSTEPS", 2, noop), "Fractional observation paid a whole point");
        require(state.knowledge("OBSERVATION", "BASICS") == 15, "Failed advancement spent knowledge");
        state.addKnowledge("OBSERVATION", "BASICS", 1);
        var saved = PlayerResearch.CODEC.codec().encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        state = PlayerResearch.CODEC.codec().parse(JsonOps.INSTANCE, saved).getOrThrow();
        require(state.stage("FIRSTSTEPS") == 2 && state.crafted("thaumcraft:thaumometer") && state.knowledge("OBSERVATION", "BASICS") == 16, "Reload lost stage/craft/category knowledge");
        require(art.arcane.thaumcraft.research.ResearchProgression.advance(state, "FIRSTSTEPS", 2, noop), "Paid stage did not advance");
        require(art.arcane.thaumcraft.research.ResearchRules.complete(state, "FIRSTSTEPS") && state.knowledge("OBSERVATION", "BASICS") == 0, "Final informational stage or observation cost is wrong");
        state.addKnowledge("OBSERVATION", "BASICS", 16);
        require(!art.arcane.thaumcraft.research.ResearchProgression.advance(state, "FIRSTSTEPS", 2, noop) && state.knowledge("OBSERVATION", "BASICS") == 16, "Repeated request charged twice");
        require(!art.arcane.thaumcraft.research.ResearchProgression.advance(state, "does_not_exist", -1, noop), "Client invented research");
        require(art.arcane.thaumcraft.research.ObservationFormula.calculate("BASICS", java.util.Map.of(AspectRegistry.TERRA,5)) == 2, "Legacy observation formula drift");
        require(art.arcane.thaumcraft.research.ResearchRules.knowledgeCost("THEORY;ARTIFICE;1") == 32, "Theory progression unit changed");
        var furnace = new PlayerResearch();
        furnace.unlock("BASEARTIFICE"); furnace.setStage("BASEARTIFICE", 2);
        require(art.arcane.thaumcraft.research.ResearchProgression.advance(furnace, "INFERNALFURNACE", -1, noop), "Furnace research did not start");
        furnace.addKnowledge("OBSERVATION", "ARTIFICE", 16);
        furnace.unlock("!ignis"); furnace.unlock("m_hellandback");
        require(!art.arcane.thaumcraft.research.ResearchProgression.advance(furnace, "INFERNALFURNACE", 1, items -> false)
                && furnace.knowledge("OBSERVATION", "ARTIFICE") == 16 && furnace.stage("INFERNALFURNACE") == 1, "Inventory failure partially committed costs");
        require(art.arcane.thaumcraft.research.ResearchProgression.advance(furnace, "INFERNALFURNACE", 1,
                items -> items.equals(java.util.Map.of("minecraft:obsidian",1,"minecraft:nether_bricks",1,"minecraft:blaze_rod",1)))
                && furnace.knowledge("OBSERVATION", "ARTIFICE") == 0, "Original inventory demand was not aggregated correctly");
        var visibility = new PlayerResearch();
        var base = ResearchCatalog.entries().get("BASEARTIFICE");
        require(!art.arcane.thaumcraft.research.ResearchRules.visible(visibility, base), "Hidden category article leaked before prerequisites");
        visibility.unlock("UNLOCKARTIFICE"); visibility.setStage("UNLOCKARTIFICE", 3);
        require(art.arcane.thaumcraft.research.ResearchRules.visible(visibility, base), "Hidden base article stayed inaccessible after unlocking its category");
        require(!art.arcane.thaumcraft.research.ResearchRules.visible(visibility, ResearchCatalog.entries().get("WARP")), "Parentless hidden discovery was revealed");
        require(!art.arcane.thaumcraft.research.ResearchRules.knows(visibility, "!gotthaumonomicon"), "Literal discovery marker was treated as negation");
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
