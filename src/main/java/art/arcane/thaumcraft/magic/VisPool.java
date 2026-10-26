package art.arcane.thaumcraft.magic;

/** Simple bounded Vis/Flux storage used by machines and future player attachments. */
public final class VisPool {
    private final int capacity;
    private int vis, flux;
    public VisPool(int capacity){if(capacity<1)throw new IllegalArgumentException();this.capacity=capacity;}
    public int capacity(){return capacity;} public int vis(){return vis;} public int flux(){return flux;}
    public int addVis(int n){int a=Math.max(0,Math.min(n,capacity-vis));vis+=a;return a;}
    public int drainVis(int n){int a=Math.max(0,Math.min(n,vis));vis-=a;return a;}
    public int addFlux(int n){int a=Math.max(0,Math.min(n,capacity-flux));flux+=a;return a;}
    public int drainFlux(int n){int a=Math.max(0,Math.min(n,flux));flux-=a;return a;}
}
