package art.arcane.thaumcraft.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.RandomSource;

/** Original crucibleBubble color, lifetime, movement and final frames. */
public final class CrucibleAspectBubbleParticle extends SingleQuadParticle {
    public CrucibleAspectBubbleParticle(ClientLevel level,double x,double y,double z,TextureAtlasSprite sprite,ColorParticleOption color){
        super(level,x,y,z,sprite);
        lifetime=15+random.nextInt(10);quadSize=0.1F*(random.nextFloat()*0.3F+0.3F);
        rCol=color.getRed();gCol=color.getGreen();bCol=color.getBlue();gravity=-0.001F;friction=0.98F;hasPhysics=false;
        setParticleSpeed(0,0,0);
    }
    @Override public void tick(){super.tick();if(isAlive()){xd+=random.nextGaussian()*0.002;yd+=random.nextGaussian()*0.002;zd+=random.nextGaussian()*0.002;}}
    private int frame(){return age<=lifetime-3?64:age<lifetime?66:65;}
    @Override protected float getU0(){return sprite.getU0()+(sprite.getU1()-sprite.getU0())*(frame()%64)/64F;}
    @Override protected float getU1(){return getU0()+(sprite.getU1()-sprite.getU0())/64F;}
    @Override protected float getV0(){return sprite.getV0()+(sprite.getV1()-sprite.getV0())/64F;}
    @Override protected float getV1(){return getV0()+(sprite.getV1()-sprite.getV0())/64F;}
    @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
    @Override public int getLightCoords(float tick){return 0xF000F0;}
    public static final class Provider implements ParticleProvider<ColorParticleOption>{
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites){this.sprites=sprites;}
        @Override public Particle createParticle(ColorParticleOption type,ClientLevel level,double x,double y,double z,double dx,double dy,double dz,RandomSource random){
            return new CrucibleAspectBubbleParticle(level,x,y,z,sprites.get(random),type);
        }
    }
}
