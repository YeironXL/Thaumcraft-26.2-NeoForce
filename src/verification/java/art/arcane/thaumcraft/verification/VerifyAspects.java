package art.arcane.thaumcraft.verification;
import java.util.*;
/** Aspect lookups by item id for the verification code. */
final class VerifyAspects {
    static Map<String,Integer> of(String id){var out=new LinkedHashMap<String,Integer>();art.arcane.thaumcraft.scan.ScanRegistry.get(id).forEach((a,n)->out.put(a.id(),n));return out;}
    private VerifyAspects(){}
}
