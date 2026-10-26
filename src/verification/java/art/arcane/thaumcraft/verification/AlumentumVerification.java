package art.arcane.thaumcraft.verification;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.entity.AlumentumProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class AlumentumVerification {
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    public static void test(GameTestHelper h) {
        var world=h.getLevel();
        var center=Vec3.atCenterOf(h.absolutePos(new BlockPos(2,2,2)));
        var player=(ServerPlayer)h.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(center.x,center.y,center.z);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.ALUMENTUM.get(),2));
        ModItems.ALUMENTUM.get().use(world,player,InteractionHand.MAIN_HAND);
        check(player.getMainHandItem().getCount()==1,"Survival throw did not consume exactly one Alumentum");
        var projectiles=world.getEntitiesOfClass(AlumentumProjectile.class,new AABB(center,center).inflate(5));
        check(projectiles.size()==1 && projectiles.getFirst().getOwner()==player,"Throw duplicated projectile or lost owner");
        check(Math.abs(projectiles.getFirst().getDeltaMovement().length()-0.75)<0.06,"Launch speed exceeded original spread");
        projectiles.getFirst().shoot(1,0,0,0.75F,0);
        check(Math.abs(projectiles.getFirst().getDeltaMovement().length()-0.75)<0.0001,"Base launch speed changed");
        projectiles.getFirst().discard();
        var creative=(ServerPlayer)h.makeMockServerPlayer(GameType.CREATIVE);
        creative.setPos(center.x,center.y,center.z);
        creative.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.ALUMENTUM.get(),2));
        ModItems.ALUMENTUM.get().use(world,creative,InteractionHand.MAIN_HAND);
        check(creative.getMainHandItem().getCount()==2,"Creative throw consumed Alumentum");
        world.getEntitiesOfClass(AlumentumProjectile.class,new AABB(center,center).inflate(5)).forEach(AlumentumProjectile::discard);
        h.setBlock(new BlockPos(3,2,2),Blocks.GLASS);
        var shot=new AlumentumProjectile(world,player,new ItemStack(ModItems.ALUMENTUM.get()));
        shot.setPos(center.x,center.y,center.z);shot.setDeltaMovement(0.75,0,0);world.addFreshEntity(shot);
        h.runAfterDelay(10,()->{
            check(shot.isRemoved(),"Block impact did not remove projectile");
            check(world.getBlockState(h.absolutePos(new BlockPos(3,2,2))).isAir(),"Impact did not destroy fragile terrain");
            var pig=h.spawn(EntityTypes.PIG,new BlockPos(3,2,2));pig.setNoAi(true);
            float health=pig.getHealth();
            var entityShot=new AlumentumProjectile(world,player,new ItemStack(ModItems.ALUMENTUM.get()));
            entityShot.setPos(center.x,pig.getY()+0.3,center.z);entityShot.setDeltaMovement(0.75,0,0);world.addFreshEntity(entityShot);
            h.runAfterDelay(10,()->{
                check(entityShot.isRemoved() && pig.getHealth()<health,"Entity impact did not explode and damage target");
                System.out.println("Alumentum projectile passed: survival/creative, owner, speed, block impact and entity damage.");
                h.succeed();
            });
        });
    }
}
