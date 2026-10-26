package art.arcane.thaumcraft.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** The original crucibleFroth sprite and its short popping sequence. */
public final class CrucibleFrothParticle extends SingleQuadParticle {
    public CrucibleFrothParticle(ClientLevel level,double x,double y,double z,TextureAtlasSprite sprite){
        super(level,x,y,z,sprite);
        lifetime=4+random.nextInt(3);quadSize=0.1F*(random.nextFloat()*0.2F+0.2F);
        rCol=0.5F;gCol=0.5F;bCol=0.7F;gravity=0.1F;friction=0.98F;hasPhysics=false;
        setParticleSpeed(random.nextGaussian()*0.001,random.nextGaussian()*0.001,random.nextGaussian()*0.001);
    }
    private int frame(){return age<=lifetime-2?64:age==lifetime-1?66:65;}
    @Override protected float getU0(){return sprite.getU0()+(sprite.getU1()-sprite.getU0())*(frame()%64)/64F;}
    @Override protected float getU1(){return getU0()+(sprite.getU1()-sprite.getU0())/64F;}
    @Override protected float getV0(){return sprite.getV0()+(sprite.getV1()-sprite.getV0())/64F;}
    @Override protected float getV1(){return getV0()+(sprite.getV1()-sprite.getV0())/64F;}
    @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
    @Override public int getLightCoords(float tick){return 0xF000F0;}
    public static final class Provider implements ParticleProvider<SimpleParticleType>{
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites){this.sprites=sprites;}
        @Override public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double dx,double dy,double dz,RandomSource random){
            return new CrucibleFrothParticle(level,x,y,z,sprites.get(random));
        }
    }
}
