package art.arcane.thaumcraft.client;

/** Screens showing research state refresh themselves when a new snapshot arrives (ClientResearch already updated). */
public interface ResearchSnapshotListener {
    void onSnapshot();
}
