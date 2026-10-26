package art.arcane.thaumcraft.casters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** TC6 FocusEngine: element registry and package execution. */
public final class FocusEngine {
    public static final Map<String, Supplier<IFocusElement>> elements = new HashMap<>();
    private static final Map<String, Identifier> elementIcons = new HashMap<>();
    private static final Map<String, Integer> elementColor = new HashMap<>();

    public static void registerElement(Supplier<IFocusElement> element, Identifier icon, int color) {
        IFocusElement fe = element.get();
        elements.put(fe.getKey(), element);
        elementIcons.put(fe.getKey(), icon);
        elementColor.put(fe.getKey(), color);
    }

    public static IFocusElement getElement(String key) { var s = elements.get(key); return s == null ? null : s.get(); }
    public static Identifier getElementIcon(String key) { return elementIcons.get(key); }
    public static int getElementColor(String key) { return elementColor.getOrDefault(key, 0xFFFFFF); }

    public static boolean doesPackageContainElement(FocusPackage p, String key) {
        for (IFocusElement node : p.nodes) if (node.getKey().equals(key)) return true;
        return false;
    }

    public static void castFocusPackage(LivingEntity caster, FocusPackage focusPackage, boolean nocopy) {
        FocusPackage copy = nocopy ? focusPackage : focusPackage.copy(caster);
        copy.initialize(caster);
        copy.setUniqueID(UUID.randomUUID());
        for (FocusEffect effect : copy.getFocusEffects()) effect.onCast(caster);
        runFocusPackage(copy, null, null);
    }

    public static void castFocusPackage(LivingEntity caster, FocusPackage focusPackage) { castFocusPackage(caster, focusPackage, false); }

    public static void runFocusPackage(FocusPackage focusPackage, Trajectory[] trajectories, HitResult[] targets) {
        Trajectory[] prevTrajectories = trajectories;
        HitResult[] prevTargets = targets;
        synchronized (focusPackage.nodes) {
            if (!(focusPackage.nodes.get(0) instanceof FocusMediumRoot)) focusPackage.nodes.add(0, new FocusMediumRoot(trajectories, targets));
            for (int idx = 0; idx < focusPackage.nodes.size(); idx++) {
                focusPackage.setExecutionIndex(idx);
                IFocusElement node = focusPackage.nodes.get(idx);
                if (idx > 0 && node instanceof FocusNode fn && fn.getParent() == null && focusPackage.nodes.get(idx - 1) instanceof FocusNode prev) fn.setParent(prev);
                if (node instanceof FocusNode fn && fn.getPackage() == null) fn.setPackage(focusPackage);
                if (node instanceof FocusNode fn) focusPackage.multiplyPower(fn.getPowerMultiplier());
                if (node instanceof FocusPackage sub) {
                    runFocusPackage(sub, prevTrajectories, prevTargets);
                    break;
                } else if (node instanceof FocusMedium medium) {
                    if (prevTrajectories != null) for (Trajectory t : prevTrajectories) medium.execute(t);
                    if (medium.hasIntermediary()) break;
                } else if (node instanceof FocusMod mod) {
                    if (mod instanceof FocusModSplit split) {
                        for (FocusPackage sp : split.getSplitPackages()) {
                            split.setPackage(sp);
                            sp.multiplyPower(focusPackage.getPower());
                            split.execute();
                            runFocusPackage(sp, split.supplyTrajectories(), split.supplyTargets());
                        }
                        break;
                    } else mod.execute();
                } else if (node instanceof FocusEffect effect) {
                    if (prevTargets != null) {
                        int num = 0;
                        for (HitResult target : prevTargets) {
                            if (target instanceof EntityHitResult eh) {
                                String k = eh.getEntity().getId() + focusPackage.getUniqueID().toString();
                                if (damageResistList.contains(k) && eh.getEntity().invulnerableTime > 0) eh.getEntity().invulnerableTime = 0;
                                else { if (damageResistList.size() > 10) damageResistList.remove(0); damageResistList.add(k); }
                            }
                            Trajectory tra = prevTrajectories != null ? (prevTrajectories.length == prevTargets.length ? prevTrajectories[num] : prevTrajectories[0]) : null;
                            effect.execute(target, tra, focusPackage.getPower(), num);
                            num++;
                        }
                    }
                }
                if (node instanceof FocusNode fn) {
                    prevTrajectories = fn.supplyTrajectories();
                    prevTargets = fn.supplyTargets();
                }
            }
        }
    }

    private static final ArrayList<String> damageResistList = new ArrayList<>();
    private FocusEngine() {}
}
