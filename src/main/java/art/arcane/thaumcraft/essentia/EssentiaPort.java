package art.arcane.thaumcraft.essentia;
import net.minecraft.core.Direction;
/** Server-thread transport contract. Receivers pull only after checking capacity. */
public interface EssentiaPort {
    boolean connects(Direction side);
    int suction();
    default int minimumSuction(){return 0;}
    String wanted();
    String aspect();
    default int suction(Direction side){return suction();}
    default String offered(String requested,Direction side){return aspect();}
    boolean extractOne(String aspect, Direction side);
}
