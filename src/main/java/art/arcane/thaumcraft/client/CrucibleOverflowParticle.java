package art.arcane.thaumcraft.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** Original crucibleFrothDown drip, color, lifetime and terrain collision. */
public final class CrucibleOverflowParticle extends SingleQuadParticle {
    public CrucibleOverflowParticle(ClientLevel level,double x,double y,double z,TextureAtlasSprite sprite){
        super(level,x,y,z,sprite);
        lifetime=12+random.nextInt(12);quadSize=0.1F*(random.nextFloat()*0.2F+0.4F);
        rCol=0.25F;gCol=0F;bCol=0.75F;alpha=.8F;gravity=.05F;friction=.98F;hasPhysics=level!=null;
        setParticleSpeed(0,0,0);
    }
    @Override public void tick(){super.tick();if(isAlive()){xd+=random.nextGaussian()*.001;yd+=random.nextGaussian()*.001;zd+=random.nextGaussian()*.001;}}
    private int frame(){return age<=lifetime-2?73:age==lifetime-1?66:65;}
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
            return new CrucibleOverflowParticle(level,x,y,z,sprites.get(random));
        }
    }
}
