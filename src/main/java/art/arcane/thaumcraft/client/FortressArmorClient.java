package art.arcane.thaumcraft.client;

import art.arcane.thaumcraft.ModItems;
import art.arcane.thaumcraft.client.render.entity.FortressArmorModel;
import art.arcane.thaumcraft.item.gear.FortressArmorItem;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** TC6 ItemFortressArmor.getArmorModel: CustomArmorHelper picks ModelFortressArmor(1.0) for the cuirass, (0.5) for helm and legs. */
public final class FortressArmorClient {
    private static final Map<EquipmentSlot, FortressArmorModel> MODELS = new EnumMap<>(EquipmentSlot.class);

    public static void register(IEventBus bus) {
        bus.addListener(FortressArmorModel::register);
        bus.addListener((EntityRenderersEvent.AddLayers e) -> MODELS.clear());
        bus.addListener((RegisterClientExtensionsEvent e) -> e.registerItem(new IClientItemExtensions() {
            @Override public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layer, Model original) {
                if (!(stack.getItem() instanceof FortressArmorItem f)) return original;
                return MODELS.computeIfAbsent(f.type.getSlot(), s -> new FortressArmorModel(Minecraft.getInstance().getEntityModels()
                        .bakeLayer(f.type == ArmorType.CHESTPLATE ? FortressArmorModel.OUTER : FortressArmorModel.INNER), s));
            }
        }, ModItems.FORTRESS_HELM.get(), ModItems.FORTRESS_CHEST.get(), ModItems.FORTRESS_LEGS.get()));
    }
    private FortressArmorClient() {}
}
