package art.arcane.thaumcraft.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BreakingItemParticle;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** TC6 FXBoreSparkle and FXBoreParticles: motes and item chips homing on a point (the infusion matrix). */
public final class BoreParticles {
    private static double maxAgeBase(double dx, double dy, double dz) { return Math.max(1, (int) (Math.sqrt(dx * dx + dy * dy + dz * dz) * 10.0f)); }

    /** FXBoreSparkle. */
    public static final class Sparkle extends SheetParticle {
        private double tx, ty, tz;
        /** FXBoreSparkle(world,x,y,z,Entity): homes on the entity's eye position each tick. */
        public net.minecraft.world.entity.Entity target;
        private float scale;
        public Sparkle(ClientLevel level, double x, double y, double z, double tx, double ty, double tz, float r, float g, float b) {
            super(level, x, y, z);
            this.tx = tx; this.ty = ty; this.tz = tz;
            scale = random.nextFloat() * 0.5f + 0.5f;
            int base = (int) maxAgeBase(tx - x, ty - y, tz - z);
            lifetime = base / 2 + random.nextInt(base);
            float f3 = 0.01f;
            xd = random.nextGaussian() * f3; yd = random.nextGaussian() * f3; zd = random.nextGaussian() * f3;
            rCol = r; gCol = g; bCol = b;
            gravity = 0.2f;
            hasPhysics = false;
            index = 256;
        }
        @Override public void tick() {
            xo = x; yo = y; zo = z;
            if (target != null) { tx = target.getX(); ty = target.getY() + target.getEyeHeight(); tz = target.getZ(); }
            if (age++ >= lifetime || (Mth.floor(x) == Mth.floor(tx) && Mth.floor(y) == Mth.floor(ty) && Mth.floor(z) == Mth.floor(tz))) { remove(); return; }
            move(xd, yd, zd);
            xd *= 0.985; yd *= 0.95; zd *= 0.985;
            double dx = tx - x, dy = ty - y, dz = tz - z;
            double d11 = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double clamp = Math.min(0.25, d11 / 15.0);
            if (d11 < 2.0) scale *= 0.9f;
            dx /= d11; dy /= d11; dz /= d11;
            xd += dx * clamp; yd += dy * clamp; zd += dz * clamp;
            xd = Mth.clamp(xd, -clamp, clamp); yd = Mth.clamp(yd, -clamp, clamp); zd = Mth.clamp(zd, -clamp, clamp);
            xd += random.nextGaussian() * 0.01; yd += random.nextGaussian() * 0.01; zd += random.nextGaussian() * 0.01;
            index = 256 + age % 4;
        }
        @Override public float getQuadSize(float pt) { return 0.1f * scale * (Mth.sin(age / 3.0f) * 0.5f + 1.0f); }
        @Override public int getLightCoords(float pt) { return 0xF000F0; }
    }

    private static final ItemStackRenderState SCRATCH = new ItemStackRenderState();
    private static TextureAtlasSprite sprite(ClientLevel level, ItemStack stack) {
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(SCRATCH, stack, ItemDisplayContext.GROUND, level, null, 0);
        var m = SCRATCH.pickParticleMaterial(level.getRandom());
        return m != null ? m.sprite() : Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.ITEMS).missingSprite();
    }

    /** FXBoreParticles (item form): a chip of the pedestal item drawn into the matrix, alpha 0.3. */
    public static final class Chip extends BreakingItemParticle {
        private double tx, ty, tz;
        public net.minecraft.world.entity.Entity target;
        /** FXBoreParticles(world,x,y,z,tx,ty,tz,IBlockState,md): a chip of the dug block's item icon, opaque. */
        public static Chip ofBlock(ClientLevel level, double x, double y, double z, net.minecraft.world.entity.Entity t, net.minecraft.world.level.block.state.BlockState state) {
            ItemStack st = new ItemStack(state.getBlock().asItem());
            Chip c = new Chip(level, x, y, z, t.getX(), t.getY() + t.getEyeHeight(), t.getZ(), 0, 0, 0, st.isEmpty() ? new ItemStack(net.minecraft.world.item.Items.STONE) : st);
            c.alpha = 1.0f;
            c.target = t;
            return c;
        }
        public Chip(ClientLevel level, double x, double y, double z, double tx, double ty, double tz, double sx, double sy, double sz, ItemStack stack) {
            super(level, x, y, z, sprite(level, stack));
            this.tx = tx; this.ty = ty; this.tz = tz;
            quadSize = 0.1f * (random.nextFloat() * 0.3f + 0.4f);
            int base = (int) maxAgeBase(tx - x, ty - y, tz - z);
            lifetime = base / 2 + random.nextInt(base);
            float f3 = 0.01f;
            xd = sx + random.nextGaussian() * f3; yd = sy + random.nextGaussian() * f3; zd = sz + random.nextGaussian() * f3;
            gravity = 0.01f;
            hasPhysics = false;
            alpha = 0.3f;
        }
        @Override public void tick() {
            xo = x; yo = y; zo = z;
            if (target != null) { tx = target.getX(); ty = target.getY() + target.getEyeHeight(); tz = target.getZ(); }
            if (age++ >= lifetime || (Mth.floor(x) == Mth.floor(tx) && Mth.floor(y) == Mth.floor(ty) && Mth.floor(z) == Mth.floor(tz))) { remove(); return; }
            move(xd, yd, zd);
            xd *= 0.985; yd *= 0.95; zd *= 0.985;
            double dx = tx - x, dy = ty - y, dz = tz - z;
            double d11 = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double clamp = Math.min(0.25, d11 / 15.0);
            if (d11 < 2.0) quadSize *= 0.9f;
            dx /= d11; dy /= d11; dz /= d11;
            xd += dx * clamp; yd += dy * clamp; zd += dz * clamp;
            xd = Mth.clamp(xd, -clamp, clamp); yd = Mth.clamp(yd, -clamp, clamp); zd = Mth.clamp(zd, -clamp, clamp);
            xd += random.nextGaussian() * 0.005; yd += random.nextGaussian() * 0.005; zd += random.nextGaussian() * 0.005;
        }
        @Override public Layer getLayer() { return Layer.TRANSLUCENT_ITEMS; }
    }

    private BoreParticles() {}
}
