package art.arcane.thaumcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import art.arcane.thaumcraft.blockentity.ThaumcraftMachineBlockEntity;

/** Base machine block used while the original 1.12.2 tile entities are migrated. */
public class ThaumcraftMachineBlock extends Block implements EntityBlock {
    private final MachineKind kind;

    public ThaumcraftMachineBlock(Properties properties, MachineKind kind) {
        super(properties.noOcclusion());
        this.kind = kind;
    }

    public MachineKind machineKind() {
        return kind;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThaumcraftMachineBlockEntity(pos, state);
    }

    public enum MachineKind {
        CRUCIBLE,
        ARCANE_WORKBENCH,
        RESEARCH_TABLE,
        INFUSION_MATRIX,
        ALEMBIC,
        CENTRIFUGE,
        CONDENSER,
        THAUMATORIUM
    }
}
