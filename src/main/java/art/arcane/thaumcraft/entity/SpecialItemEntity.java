package art.arcane.thaumcraft.entity;

import art.arcane.thaumcraft.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * TC6 EntitySpecialItem: an item that hangs in the air (upward 0.04 per tick cancels gravity, rising motion decays by 0.9)
 * and is not destroyed by explosions. Used for crucible output and the Salis Mundus Thaumonomicon.
 */
public final class SpecialItemEntity extends ItemEntity {
    public SpecialItemEntity(EntityType<? extends ItemEntity> type, Level level) { super(type, level); }
    public SpecialItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        this(ModEntities.SPECIAL_ITEM.get(), level);
        setPos(x, y, z);
        setItem(stack);
        setYRot((float)(Math.random() * 360.0));
        setDeltaMovement((float)(Math.random() * 0.2 - 0.1), 0.2, (float)(Math.random() * 0.2 - 0.1));
        lifespan = stack.getEntityLifespan(level);
    }
    @Override public void tick() {
        if (tickCount > 1) {
            Vec3 m = getDeltaMovement();
            double y = m.y;
            if (y > 0.0) y *= 0.9f;
            y += 0.04f;
            setDeltaMovement(m.x, y, m.z);
            super.tick();
        }
    }
    @Override public boolean ignoreExplosion(Explosion explosion) { return true; }
}
