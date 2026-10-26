package art.arcane.thaumcraft.client.book;

import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * GUI picture-in-picture state for GuiResearchPage.renderBluePrint: blocks at blueprint coordinates, drawn with the
 * original transform (translate trans, scale (s,-s), rotX about X, rotY about Y, centre on the structure).
 */
public record BlueprintRenderState(List<Block> blocks, int xSize, int ySize, int zSize, float transX, float transY, float blockScale,
                                   float rotX, float rotY, int x0, int y0, int x1, int y1,
                                   @Nullable ScreenRectangle scissorArea, @Nullable ScreenRectangle bounds) implements PictureInPictureRenderState {
    public record Block(BlockPos pos, BlockState state) {}
    public BlueprintRenderState(List<Block> blocks, int xSize, int ySize, int zSize, float transX, float transY, float blockScale, float rotX, float rotY,
                                int x0, int y0, int x1, int y1, @Nullable ScreenRectangle scissorArea) {
        this(blocks, xSize, ySize, zSize, transX, transY, blockScale, rotX, rotY, x0, y0, x1, y1, scissorArea, PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }
    @Override public float scale() { return 1.0f; }
}
