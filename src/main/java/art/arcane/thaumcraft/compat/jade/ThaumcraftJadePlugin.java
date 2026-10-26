package art.arcane.thaumcraft.compat.jade;

import art.arcane.thaumcraft.Thaumcraft;
import art.arcane.thaumcraft.block.CrucibleBlock;
import art.arcane.thaumcraft.block.EssentiaCentrifugeBlock;
import art.arcane.thaumcraft.block.EssentiaJarBlock;
import art.arcane.thaumcraft.block.EssentiaSmelterBlock;
import art.arcane.thaumcraft.block.EssentiaTubeBlock;
import art.arcane.thaumcraft.block.InfusionMatrixBlock;
import art.arcane.thaumcraft.block.ThaumatoriumBlock;
import art.arcane.thaumcraft.blockentity.CrucibleBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaCentrifugeBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaJarBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaSmelterBlockEntity;
import art.arcane.thaumcraft.blockentity.EssentiaTubeBlockEntity;
import art.arcane.thaumcraft.blockentity.InfusionMatrixBlockEntity;
import art.arcane.thaumcraft.blockentity.ThaumatoriumBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Optional Jade integration (only loaded by Jade itself). Each provider can be switched off in Jade's plugin settings
 * (Thaumcraft section).
 */
@WailaPlugin(Thaumcraft.MOD_ID)
public final class ThaumcraftJadePlugin implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration r) {
        r.registerBlockDataProvider(TcProviders.JAR, EssentiaJarBlockEntity.class);
        r.registerBlockDataProvider(TcProviders.CRUCIBLE, CrucibleBlockEntity.class);
        r.registerBlockDataProvider(TcProviders.INFUSION, InfusionMatrixBlockEntity.class);
        r.registerBlockDataProvider(TcProviders.TUBE, EssentiaTubeBlockEntity.class);
        r.registerBlockDataProvider(TcProviders.CENTRIFUGE, EssentiaCentrifugeBlockEntity.class);
        r.registerBlockDataProvider(TcProviders.SMELTER, EssentiaSmelterBlockEntity.class);
        r.registerBlockDataProvider(TcProviders.THAUMATORIUM, ThaumatoriumBlockEntity.class);
    }

    @Override public void registerClient(IWailaClientRegistration r) {
        r.registerBlockComponent(TcClientProviders.JAR, EssentiaJarBlock.class);
        r.registerBlockComponent(TcClientProviders.CRUCIBLE, CrucibleBlock.class);
        r.registerBlockComponent(TcClientProviders.INFUSION, InfusionMatrixBlock.class);
        r.registerBlockComponent(TcClientProviders.TUBE, EssentiaTubeBlock.class);
        r.registerBlockComponent(TcClientProviders.CENTRIFUGE, EssentiaCentrifugeBlock.class);
        r.registerBlockComponent(TcClientProviders.SMELTER, EssentiaSmelterBlock.class);
        r.registerBlockComponent(TcClientProviders.THAUMATORIUM, ThaumatoriumBlock.class);
        r.registerEntityComponent(GolemProvider.INSTANCE, art.arcane.thaumcraft.golems.GolemEntity.class);
    }
}
