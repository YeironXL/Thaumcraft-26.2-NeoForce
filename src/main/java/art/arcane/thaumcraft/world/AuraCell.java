package art.arcane.thaumcraft.world;

/** Per-chunk aura state: Vis is useful energy; Flux is pollution. */
public final class AuraCell {
    private float vis, flux;
    public AuraCell(float vis,float flux){this.vis=Math.max(0,vis);this.flux=Math.max(0,flux);}
    public float vis(){return vis;} public float flux(){return flux;}
    public void addVis(float v){vis=Math.max(0,vis+v);} public void addFlux(float f){flux=Math.max(0,flux+f);}
    public void tick(float naturalRecharge){vis=Math.max(0,vis+naturalRecharge); flux*=0.9995f;}
}
