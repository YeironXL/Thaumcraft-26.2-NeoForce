package art.arcane.thaumcraft.aspect;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** TC6 BETA26 aspect identities, compositions and colors, audited against the legacy source. */
public final class AspectRegistry {
    private static final Map<String, Aspect> ASPECTS = new LinkedHashMap<>();
    private static final Map<String, Integer> COLORS = new LinkedHashMap<>();
    public static final Aspect AER = define("aer", null, null, 0xffff7e);
    public static final Aspect TERRA = define("terra", null, null, 0x56c000);
    public static final Aspect IGNIS = define("ignis", null, null, 0xff5a01);
    public static final Aspect AQUA = define("aqua", null, null, 0x3cd4fc);
    public static final Aspect ORDO = define("ordo", null, null, 0xd5d4ec);
    public static final Aspect PERDITIO = define("perditio", null, null, 0x404040);
    public static final Aspect VACUOS = define("vacuos", AER, PERDITIO, 0x888888);
    public static final Aspect LUX = define("lux", AER, IGNIS, 0xffffc0);
    public static final Aspect MOTUS = define("motus", AER, ORDO, 0xcdccf4);
    public static final Aspect GELUM = define("gelum", IGNIS, PERDITIO, 0xe1ffff);
    public static final Aspect VITREUS = define("vitreus", TERRA, AER, 0x80ffff);
    public static final Aspect METALLUM = define("metallum", TERRA, ORDO, 0xb5b5cd);
    public static final Aspect VICTUS = define("victus", TERRA, AQUA, 0xde0005);
    public static final Aspect MORTUUS = define("mortuus", AQUA, PERDITIO, 0x6a0005);
    public static final Aspect POTENTIA = define("potentia", ORDO, IGNIS, 0xc0ffff);
    public static final Aspect PERMUTATIO = define("permutatio", PERDITIO, ORDO, 0x578357);
    public static final Aspect PRAECANTATIO = define("praecantatio", POTENTIA, AER, 0xcf00ff);
    public static final Aspect AURAM = define("auram", PRAECANTATIO, AER, 0xffc0ff);
    public static final Aspect ALKIMIA = define("alkimia", PRAECANTATIO, AQUA, 0x23ac9d);
    public static final Aspect VITIUM = define("vitium", PERDITIO, PRAECANTATIO, 0x800080);
    public static final Aspect TENEBRAE = define("tenebrae", VACUOS, LUX, 0x222222);
    public static final Aspect ALIENIS = define("alienis", VACUOS, TENEBRAE, 0x805080);
    public static final Aspect VOLATUS = define("volatus", AER, MOTUS, 0xe7e7d7);
    public static final Aspect HERBA = define("herba", VICTUS, TERRA, 0x01ac00);
    public static final Aspect INSTRUMENTUM = define("instrumentum", METALLUM, POTENTIA, 0x4040ee);
    public static final Aspect FABRICO = define("fabrico", PERMUTATIO, INSTRUMENTUM, 0x809d80);
    public static final Aspect MACHINA = define("machina", MOTUS, INSTRUMENTUM, 0x8080a0);
    public static final Aspect VINCULUM = define("vinculum", MOTUS, PERDITIO, 0x9a8080);
    public static final Aspect SPIRITUS = define("spiritus", VICTUS, MORTUUS, 0xebebfb);
    public static final Aspect COGNITIO = define("cognitio", IGNIS, SPIRITUS, 0xf9967f);
    public static final Aspect SENSUS = define("sensus", AER, SPIRITUS, 0xc0ffc0);
    public static final Aspect AVERSIO = define("aversio", SPIRITUS, PERDITIO, 0xc05050);
    public static final Aspect PRAEMUNIO = define("praemunio", SPIRITUS, TERRA, 0x00c0c0);
    public static final Aspect DESIDERIUM = define("desiderium", SPIRITUS, VACUOS, 0xe6be44);
    public static final Aspect EXANIMIS = define("exanimis", MOTUS, MORTUUS, 0x3a4000);
    public static final Aspect BESTIA = define("bestia", MOTUS, VICTUS, 0x9f6409);
    public static final Aspect HUMANUS = define("humanus", SPIRITUS, VICTUS, 0xffd7c0);
    private static Aspect define(String id, Aspect a, Aspect b, int color) {
        Aspect value = new Aspect(id, a, b);
        ASPECTS.put(id, value);
        COLORS.put(id, color);
        return value;
    }
    public static Aspect get(String id) { return ASPECTS.get(id); }
    public static int color(String id) { return COLORS.getOrDefault(id, 0xFFFFFF); }
    public static Map<String, Aspect> all() { return Collections.unmodifiableMap(ASPECTS); }
    private AspectRegistry() {}
}
