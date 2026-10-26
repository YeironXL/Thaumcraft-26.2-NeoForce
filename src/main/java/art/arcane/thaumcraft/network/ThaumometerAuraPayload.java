package art.arcane.thaumcraft.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record ThaumometerAuraPayload(int base,float vis,float flux) implements CustomPacketPayload {
 public static final Type<ThaumometerAuraPayload> TYPE=new Type<>(Identifier.parse("thaumcraft:thaumometer_aura"));
 public static final StreamCodec<RegistryFriendlyByteBuf,ThaumometerAuraPayload> CODEC=StreamCodec.composite(ByteBufCodecs.INT,ThaumometerAuraPayload::base,ByteBufCodecs.FLOAT,ThaumometerAuraPayload::vis,ByteBufCodecs.FLOAT,ThaumometerAuraPayload::flux,ThaumometerAuraPayload::new);
 public Type<ThaumometerAuraPayload> type(){return TYPE;}
}
