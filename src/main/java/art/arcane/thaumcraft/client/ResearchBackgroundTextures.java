package art.arcane.thaumcraft.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/** Decodes the original JPEG artwork without changing the shipped source assets. */
public final class ResearchBackgroundTextures implements AutoCloseable {
    private final Map<Integer, Identifier> textures = new HashMap<>();

    public Identifier get(int number) {
        return textures.computeIfAbsent(number, value -> {
            var client = Minecraft.getInstance();
            var source = Identifier.parse("thaumcraft:textures/gui/gui_research_back_" + value + ".jpg");
            try (var stream = client.getResourceManager().open(source)) {
                var decoded = ImageIO.read(stream);
                if (decoded == null) throw new IOException("Cannot decode " + source);
                var pixels = new NativeImage(decoded.getWidth(), decoded.getHeight(), false);
                for (int y = 0; y < decoded.getHeight(); y++)
                    for (int x = 0; x < decoded.getWidth(); x++) pixels.setPixel(x, y, decoded.getRGB(x, y));
                var target = Identifier.parse("thaumcraft:dynamic/research_background_" + value);
                client.getTextureManager().register(target, new DynamicTexture(() -> source.toString(), pixels));
                return target;
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot load research background " + source, exception);
            }
        });
    }

    @Override public void close() {
        textures.values().forEach(Minecraft.getInstance().getTextureManager()::release);
        textures.clear();
    }
}
