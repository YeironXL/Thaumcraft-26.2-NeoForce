package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModBlocks;
import art.arcane.thaumcraft.blockentity.EssentiaSmelterBlockEntity;
import art.arcane.thaumcraft.menu.EssentiaSmelterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

public final class SmelterMenuVerification {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void test(GameTestHelper h){
        int expected=80;
        for(var block:java.util.List.of(ModBlocks.SMELTER_BASIC.get(),ModBlocks.SMELTER_THAUMIUM.get(),ModBlocks.SMELTER_VOID.get())){
            var relative=new BlockPos(2,1,2);h.setBlock(relative,block);
            var pos=h.absolutePos(relative);var machine=h.getBlockEntity(relative,EssentiaSmelterBlockEntity.class);
            var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);player.setPos(pos.getX()+0.5,pos.getY()+1,pos.getZ()+0.5);
            var menu=(EssentiaSmelterMenu)machine.createMenu(12,player.getInventory(),player);
            check(menu.slots.size()==38 && menu.stillValid(player) && menu.efficiency()==expected,"Menu layout/variant/validity failed");
            expected=expected==80?90:95;
            player.getInventory().setItem(9,new ItemStack(Items.IRON_INGOT,2));
            menu.quickMoveStack(player,2);
            check(machine.input().getCount()==2 && player.getInventory().getItem(9).isEmpty(),"Shift-click input lost/duplicated items");
            player.getInventory().setItem(0,new ItemStack(Items.COAL,3));menu.quickMoveStack(player,29);
            check(machine.fuel().getCount()==3 && player.getInventory().getItem(0).isEmpty(),"Shift-click fuel routing failed");
            check(!menu.slots.get(1).mayPlace(new ItemStack(Items.STONE)) && !menu.slots.get(0).mayPlace(new ItemStack(Items.BARRIER)),"Invalid menu item accepted");
            player.getInventory().setItem(0,new ItemStack(Items.STONE));
            menu.clicked(1,0,ContainerInput.SWAP,player);
            check(machine.fuel().getCount()==3 && player.getInventory().getItem(0).is(Items.STONE),"Hotbar swap bypassed fuel validation");
            EssentiaSmelterBlockEntity.tick(h.getLevel(),pos,machine.getBlockState(),machine);
            check(menu.fuelFraction()==1000 && menu.fuelSeconds()==80 && menu.progress()>0,"Server menu did not expose live progress/fuel");
            var saved=machine.saveWithoutMetadata(h.getLevel().registryAccess());
            var restored=new EssentiaSmelterBlockEntity(pos,machine.getBlockState());
            restored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),saved));
            var reopened=(EssentiaSmelterMenu)restored.createMenu(13,player.getInventory(),player);
            check(reopened.fuelFraction()==menu.fuelFraction() && reopened.progress()==menu.progress(),"Reopened menu lost saved progress");
            for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
            check(menu.quickMoveStack(player,0).isEmpty() && machine.input().getCount()==2,"Full player inventory lost furnace input");
            player.getInventory().setItem(0,ItemStack.EMPTY);menu.quickMoveStack(player,0);
            check(machine.input().isEmpty() && player.getInventory().getItem(0).is(Items.IRON_INGOT) && player.getInventory().getItem(0).getCount()==2,"Shift-click removal duplicated/lost material");
            player.setPos(pos.getX()+100,pos.getY(),pos.getZ());check(!menu.stillValid(player),"Remote player retained furnace access");
            h.setBlock(relative,Blocks.AIR);check(!menu.stillValid(player),"Removed furnace retained menu access");
        }
        System.out.println("Smelter menu passed: three variants, shift-click, fuel validation, hotbar swap, full inventory, live data and persistence.");
        h.succeed();
    }
}
