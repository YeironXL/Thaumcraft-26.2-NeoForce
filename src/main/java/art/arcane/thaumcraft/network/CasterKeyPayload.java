package art.arcane.thaumcraft.network;

import art.arcane.thaumcraft.casters.CasterManager;
import art.arcane.thaumcraft.item.CasterItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** TC6 PacketFocusChangeToServer (key != null) and PacketItemKeyToServer (G: misc toggle). */
public record CasterKeyPayload(String focus, int key, int mod) implements CustomPacketPayload {
    public static final Type<CasterKeyPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("thaumcraft", "caster_key"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CasterKeyPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(512), CasterKeyPayload::focus, ByteBufCodecs.VAR_INT, CasterKeyPayload::key, ByteBufCodecs.VAR_INT, CasterKeyPayload::mod, CasterKeyPayload::new);
    @Override public Type<CasterKeyPayload> type() { return TYPE; }

    public static void handle(CasterKeyPayload msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer p)) return;
            ItemStack is = p.getMainHandItem().getItem() instanceof CasterItem ? p.getMainHandItem() : p.getOffhandItem();
            if (!(is.getItem() instanceof CasterItem)) {
                // PacketItemKeyToServer: G with the Shovel of the Earthmover in hand cycles its placement orientation.
                ItemStack main = p.getMainHandItem();
                if (msg.key == 1 && main.getItem() instanceof art.arcane.thaumcraft.item.gear.ElementalShovelItem)
                    art.arcane.thaumcraft.item.gear.ElementalShovelItem.setOrientation(main, (byte) (art.arcane.thaumcraft.item.gear.ElementalShovelItem.getOrientation(main) + 1));
                return;
            }
            if (msg.key < 0) CasterManager.changeFocus(is, p, msg.focus);
            else if (msg.key == 1) CasterManager.toggleMisc(is, p, msg.mod);
        });
    }
}
