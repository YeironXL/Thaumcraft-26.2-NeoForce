package art.arcane.thaumcraft.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** TC6 manipulator's central crafting sparkle; crystal beams remain separate. */
public final class FocalCraftParticle extends SingleQuadParticle {
    public FocalCraftParticle(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,TextureAtlasSprite sprite){
        super(level,x,y,z,sprite);
        setParticleSpeed(0,0,0);lifetime=6+random.nextInt(5);alpha=0.8F;gravity=0;friction=0.98F;
        setColor(0.5F+random.nextFloat()*0.4F,1-random.nextFloat()*0.4F,1-random.nextFloat()*0.4F);
        quadSize=0.1F*(0.3F+random.nextFloat()*0.3F);hasPhysics=false;
    }
    private int frame(){return 448+Math.min(8,age*9/lifetime);}
    @Override protected float getU0(){return sprite.getU0()+(sprite.getU1()-sprite.getU0())*(frame()%64)/64F;}
    @Override protected float getU1(){return getU0()+(sprite.getU1()-sprite.getU0())/64F;}
    @Override protected float getV0(){return sprite.getV0()+(sprite.getV1()-sprite.getV0())*(frame()/64)/64F;}
    @Override protected float getV1(){return getV0()+(sprite.getV1()-sprite.getV0())/64F;}
    @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
    @Override public int getLightCoords(float tick){return 0xF000F0;}
    public static final class Provider implements ParticleProvider<SimpleParticleType>{
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites){this.sprites=sprites;}
        @Override public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double dx,double dy,double dz,RandomSource random){
            return new FocalCraftParticle(level,x,y,z,dx,dy,dz,sprites.get(random));
        }
    }
}
