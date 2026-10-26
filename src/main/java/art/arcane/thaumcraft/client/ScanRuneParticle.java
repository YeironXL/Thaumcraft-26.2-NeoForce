package art.arcane.thaumcraft.client;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
/** FXBlockRunes (blockRunes2 when the duration argument is negative: scale 0.5, no x offset): sixteen original glyphs on four fixed vertical planes, not camera billboards. */
public final class ScanRuneParticle extends SingleQuadParticle {
 private final int glyph;private final Quaternionf orientation;
 public ScanRuneParticle(ClientLevel level,double x,double y,double z,double duration,double gravity,ColorParticleOption color,TextureAtlasSprite sprite){
  super(level,x,y,z,sprite);boolean v2=duration<0;if(v2)duration=-duration;glyph=random.nextInt(16);float angle=random.nextInt(4)*(float)(Math.PI/2);
  orientation=new Quaternionf().rotateY(angle).rotateZ((float)(Math.PI/2));
  var offset=new org.joml.Vector3f(v2?0F:random.nextFloat()*.2F,-.3F+random.nextFloat()*.6F,-.51F).rotate(orientation);
  this.x+=offset.x;this.y+=offset.y;this.z+=offset.z;xo=this.x;yo=this.y;zo=this.z;
  lifetime=Math.max(1,(int)(3*duration));this.gravity=(float)gravity;quadSize=v2?.15F*(.5F+(float)random.nextGaussian()*.1F):.15F*(1+(float)random.nextGaussian()*.1F);
  setColor(color.getRed(),color.getGreen(),color.getBlue());alpha=0;hasPhysics=false;setParticleSpeed(0,0,0);
 }
 @Override public void tick(){
  xo=x;yo=y;zo=z;float threshold=lifetime/5F;alpha=.5F*(age<=threshold?age/threshold:(lifetime-age)/(float)lifetime);
  if(age++>=lifetime){remove();return;}yd-=.04*gravity;y+=yd;
 }
 @Override public void extract(net.minecraft.client.renderer.state.level.QuadParticleRenderState state,net.minecraft.client.Camera camera,float tick){extractRotatedQuad(state,camera,orientation,tick);}
 @Override protected float getU0(){return sprite.getU0()+(sprite.getU1()-sprite.getU0())*glyph/64F;}
 @Override protected float getU1(){return getU0()+(sprite.getU1()-sprite.getU0())/64F;}
 @Override protected float getV0(){return sprite.getV0()+(sprite.getV1()-sprite.getV0())*6/64F;}
 @Override protected float getV1(){return getV0()+(sprite.getV1()-sprite.getV0())/64F;}
 @Override protected Layer getLayer(){return Layer.TRANSLUCENT;}
 @Override public int getLightCoords(float tick){return 0xF000F0;}
 public static final class Provider implements ParticleProvider<ColorParticleOption>{
  private final SpriteSet sprites;public Provider(SpriteSet sprites){this.sprites=sprites;}
  public Particle createParticle(ColorParticleOption type,ClientLevel level,double x,double y,double z,double dx,double dy,double dz,RandomSource random){return new ScanRuneParticle(level,x,y,z,dx,dy,type,sprites.get(random));}
 }
}
