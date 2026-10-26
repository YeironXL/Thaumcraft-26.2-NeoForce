package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.client.render.entity.CultistArmorModel;
import art.arcane.thaumcraft.item.CultistArmor;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** TC6 Item*Armor.getArmorModel: the crimson robe, plate and praetor pieces render with their custom models. */
public final class CultistArmorClient {
    private static final Map<Item, CultistArmorModel> MODELS = new HashMap<>();

    public static void register(IEventBus bus) {
        bus.addListener(CultistArmorModel::register);
        bus.addListener((EntityRenderersEvent.AddLayers e) -> MODELS.clear());
        bus.addListener((RegisterClientExtensionsEvent e) -> {
            IClientItemExtensions ext = new IClientItemExtensions() {
                @Override public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layer, Model original) {
                    if (stack.getItem() instanceof art.arcane.thaumcraft.item.gear.VoidRobeArmorItem v)
                        // ItemVoidRobeArmor.getArmorModel: the same ModelRobe as the crimson robes.
                        return MODELS.computeIfAbsent(v, i -> new CultistArmorModel(Minecraft.getInstance().getEntityModels()
                                .bakeLayer(CultistArmorModel.layerFor(CultistArmor.Kind.ROBE, v.type)), CultistArmor.Kind.ROBE, v.type.getSlot()));
                    if (!(stack.getItem() instanceof CultistArmor a) || a.kind == CultistArmor.Kind.BOOTS) return original;
                    return MODELS.computeIfAbsent(a, i -> new CultistArmorModel(
                            Minecraft.getInstance().getEntityModels().bakeLayer(CultistArmorModel.layerFor(a.kind, a.type)), a.kind, a.type.getSlot()));
                }
            };
            e.registerItem(ext, ModItems.CRIMSON_ROBE_HELM.get(), ModItems.CRIMSON_ROBE_CHEST.get(), ModItems.CRIMSON_ROBE_LEGS.get(),
                    ModItems.CRIMSON_PLATE_HELM.get(), ModItems.CRIMSON_PLATE_CHEST.get(), ModItems.CRIMSON_PLATE_LEGS.get(),
                    ModItems.CRIMSON_PRAETOR_HELM.get(), ModItems.CRIMSON_PRAETOR_CHEST.get(), ModItems.CRIMSON_PRAETOR_LEGS.get(),
                    ModItems.VOID_ROBE_HELM.get(), ModItems.VOID_ROBE_CHEST.get(), ModItems.VOID_ROBE_LEGS.get());
        });
    }
    private CultistArmorClient() {}
}
