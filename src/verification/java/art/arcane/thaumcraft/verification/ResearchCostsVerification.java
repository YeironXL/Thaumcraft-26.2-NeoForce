package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.item.CrystalEssenceItem;
import art.arcane.thaumcraft.item.EssentiaPhialItem;
import art.arcane.thaumcraft.research.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import java.util.Map;

public final class ResearchCostsVerification {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void test(GameTestHelper h){
        var inventory=new SimpleContainer(20);var research=new PlayerResearch();
        research.unlock("ALUMENTUM");research.setStage("ALUMENTUM",3);
        research.unlock("METALLURGY");research.setStage("METALLURGY",2);
        research.unlock("ESSENTIASMELTER");research.setStage("ESSENTIASMELTER",1);
        for(int i=0;i<6;i++)inventory.setItem(i,CrystalEssenceItem.of("aer",1));
        check(!ResearchProgression.advance(research,"ESSENTIASMELTER",1,cost->ResearchItemCosts.consume(inventory,cost)),"Six air crystals bypassed six different primal requirements");
        check(inventory.countItem(art.arcane.thaumcraft.ModItems.CRYSTAL_ESSENCE.get())==6 && research.stage("ESSENTIASMELTER")==1,"Rejected stage partially consumed costs");
        String[] primals={"aer","terra","ignis","aqua","ordo","perditio"};
        for(int i=0;i<primals.length;i++)inventory.setItem(i,CrystalEssenceItem.of(primals[i],1));
        check(ResearchProgression.advance(research,"ESSENTIASMELTER",1,cost->ResearchItemCosts.consume(inventory,cost)) && inventory.isEmpty(),"Original primal NBT was not migrated to aspect components");
        String[] compounds={"vacuos","lux","motus","gelum","vitreus","metallum","victus","mortuus","potentia","permutatio"};
        for(int i=0;i<compounds.length;i++)inventory.setItem(i,CrystalEssenceItem.of(compounds[i],1));
        check(ResearchProgression.advance(research,"ESSENTIASMELTER",2,cost->ResearchItemCosts.consume(inventory,cost)) && inventory.isEmpty() && research.stage("ESSENTIASMELTER")==3,"Compound crystal research stage stayed blocked");
        String phial=LegacyItemReferences.requirementKey("thaumcraft:phial;1;1;{Aspects:[{amount:10,key:'vitium'}]}");
        inventory.setItem(0,EssentiaPhialItem.filled("aer"));
        check(!ResearchItemCosts.consume(inventory,Map.of(phial,1)) && !inventory.isEmpty(),"Wrong filled phial aspect accepted");
        inventory.setItem(0,EssentiaPhialItem.filled("vitium"));
        check(ResearchItemCosts.consume(inventory,Map.of(phial,1)) && inventory.isEmpty(),"Native filled phial did not satisfy legacy cost");
        inventory.setItem(0,CrystalEssenceItem.of("aer",1));inventory.setItem(1,CrystalEssenceItem.of("ignis",1));
        check(ResearchItemCosts.consume(inventory,Map.of("thaumcraft:crystal_essence",1,"thaumcraft:crystal_essence@aer",1)) && inventory.isEmpty(),"Overlapping generic/specific costs double counted or misallocated items");
        inventory.setItem(0,CrystalEssenceItem.of("aer",1));
        check(!ResearchItemCosts.consume(inventory,Map.of("thaumcraft:crystal_essence@aer",1,"thaumcraft:crystal_essence@ignis",1)) && inventory.getItem(0).getCount()==1,"Missing component cost partially consumed inventory");
        check(LegacyItemReferences.requirementKey("thaumcraft:phial;1;1;{Aspects:[{amount:1,key:'vitium'}]}").isEmpty(),"Wrong phial amount bypassed migration");
        check(LegacyItemReferences.requirementKey("thaumcraft:crystal_essence;1;0;{Aspects:[{amount:1,key:'fake'}]}").isEmpty(),"Unknown aspect accepted");
        System.out.println("Research costs passed: primal/compound smelter stages, correct phials, atomic component costs and overlap allocation.");
        h.succeed();
    }
}
