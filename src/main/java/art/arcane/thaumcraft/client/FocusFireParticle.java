package art.arcane.thaumcraft.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** TC6 FocusEffectFire's ten authored frames, alpha, buoyancy and damping. */
public final class FocusFireParticle extends SingleQuadParticle {
    public FocusFireParticle(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,TextureAtlasSprite sprite){
        super(level,x,y,z,sprite);
        setParticleSpeed(dx,dy,dz);lifetime=10;alpha=0.7F;gravity=-0.2F;friction=0.75F;
        quadSize=0.1F*(float)(1.5+random.nextGaussian()*0.2);hasPhysics=false;
    }
    private int frame(){return 640+Math.min(age,9);}
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
            return new FocusFireParticle(level,x,y,z,dx,dy,dz,sprites.get(random));
        }
    }
}
