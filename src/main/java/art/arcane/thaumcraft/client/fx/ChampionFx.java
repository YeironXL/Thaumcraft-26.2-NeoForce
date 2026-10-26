package art.arcane.thaumcraft.client.fx;

import art.arcane.thaumcraft.entity.champion.ChampionModifier;
import art.arcane.thaumcraft.fx.GenericParticleOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/** IChampionModifierEffect.showFX for the fourteen champion modifiers (RenderEventHandler.livingTick). */
public final class ChampionFx {
    public static void showFX(ChampionModifier mod, LivingEntity boss) {
        var rand = boss.getRandom();
        var bb = boss.getBoundingBox();
        float w = rand.nextFloat() * boss.getBbWidth(), d = rand.nextFloat() * boss.getBbWidth(), h = rand.nextFloat() * boss.getBbHeight();
        double x = bb.minX + w, y = bb.minY + h, z = bb.minZ + d;
        switch (mod.id()) {
            case 0 -> { // bold: orange sparks around the feet
                if (rand.nextBoolean()) return;
                float h3 = rand.nextFloat() * boss.getBbHeight() / 3.0f;
                spark(bb.minX + w, bb.minY + h3, bb.minZ + d, 0.2f, 0.3f - rand.nextFloat() * 0.1f, 0.0f, 0.8f + rand.nextFloat() * 0.2f, 1.0f);
            }
            case 1 -> { // spined
                if (rand.nextBoolean()) return;
                int p = 704 + rand.nextInt(4) * 3;
                FXDispatcher.drawGenericParticles(x, y, z, 0, 0, 0, 0.5f + rand.nextFloat() * 0.2f, 0.1f + rand.nextFloat() * 0.2f, 0.1f + rand.nextFloat() * 0.2f, 0.7f, false, p, 3, 1, 3, 0, 1.2f + rand.nextFloat() * 0.3f, 0.0f, 0);
            }
            case 2 -> { // armored
                if (rand.nextInt(4) != 0) return;
                FXDispatcher.drawGenericParticles(x, y, z, 0, 0, 0, 0.9f, 0.9f, 0.9f + rand.nextFloat() * 0.1f, 0.7f, false, 448, 9, 1, 5 + rand.nextInt(4), 0, 0.6f + rand.nextFloat() * 0.2f, 0.0f, 0);
            }
            case 3 -> { // mighty
                if (rand.nextFloat() > 0.3f) return;
                int p = 704 + rand.nextInt(4) * 3;
                FXDispatcher.drawGenericParticles(x, y, z, 0, 0, 0, 0.8f + rand.nextFloat() * 0.2f, 0.8f + rand.nextFloat() * 0.2f, 0.8f + rand.nextFloat() * 0.2f, 0.7f, false, p, 3, 1, 4 + rand.nextInt(3), 0, 1.0f + rand.nextFloat() * 0.3f, 0.0f, 0);
            }
            case 4 -> { // grim
                if (rand.nextBoolean()) return;
                FXDispatcher.drawGenericParticles(x, y, z, 0, -0.02, 0, rand.nextFloat() * 0.2f, rand.nextFloat() * 0.2f, rand.nextFloat() * 0.2f, 0.8f, false, 640, 10, 1, 8 + rand.nextInt(4), 0, 0.6f + rand.nextFloat() * 0.4f, 0.0f, 0);
            }
            case 5 -> { // warded
                if (rand.nextBoolean()) return;
                FXDispatcher.drawGenericParticles(x, y, z, 0, 0, 0, 0.5f + rand.nextFloat() * 0.1f, 0.5f + rand.nextFloat() * 0.1f, 0.5f + rand.nextFloat() * 0.1f, 0.6f, true, 69, 4, 1, 4 + rand.nextInt(4), 0, 0.8f + rand.nextFloat() * 0.3f, 0.0f, 0);
            }
            case 6 -> { // warp
                if (rand.nextBoolean()) return;
                FXDispatcher.drawGenericParticles(x, y, z, 0, 0, 0, 0.8f + rand.nextFloat() * 0.2f, 0.0f, 0.9f + rand.nextFloat() * 0.1f, 0.7f, true, 264, 8, 1, 10 + rand.nextInt(4), 0, 0.6f + rand.nextFloat() * 0.4f, 0.0f, 0);
            }
            case 7 -> { // undying
                if (rand.nextBoolean()) return;
                FXDispatcher.drawGenericParticles(x, y, z, 0, 0.03, 0, 0.1f + rand.nextFloat() * 0.1f, 0.8f + rand.nextFloat() * 0.2f, 0.1f + rand.nextFloat() * 0.1f, 0.9f, true, 69, 4, 1, 4 + rand.nextInt(4), 0, 0.5f + rand.nextFloat() * 0.2f, 0.0f, 0);
            }
            case 8 -> // fiery
                FXDispatcher.drawGenericParticles(x, y, z, 0, 0.03, 0, 0.9f + rand.nextFloat() * 0.1f, 1.0f, 1.0f, 0.7f, false, 640, 10, 1, 8 + rand.nextInt(4), 0, 0.7f + rand.nextFloat() * 0.2f, 0.0f, 0);
            case 9 -> { // sickly (the 64 grid frames 1..)
                if (rand.nextBoolean()) return;
                FXDispatcher.drawGenericParticles(x, y, z, 0, -0.02, 0, 0.2f, 0.6f + rand.nextFloat() * 0.1f, 0.2f + rand.nextFloat() * 0.1f, 0.5f, false, 1, 4, 2, 5 + rand.nextInt(4), 0, 0.9f + rand.nextFloat() * 0.3f, 0.0f, 0);
            }
            case 10 -> { // venomous
                if (rand.nextBoolean()) return;
                FXDispatcher.drawGenericParticles(x, y, z, 0, 0.02, 0, 0.2f, 0.6f + rand.nextFloat() * 0.1f, 0.2f + rand.nextFloat() * 0.1f, 0.7f, false, 579, 4, 1, 8 + rand.nextInt(4), 0, 0.5f + rand.nextFloat() * 0.2f, 0.5f, 0);
            }
            case 11 -> { // vampiric
                if (rand.nextFloat() > 0.2f) return;
                FXDispatcher.drawGenericParticles(x, y, z, 0, 0, 0, 0.9f + rand.nextFloat() * 0.1f, 0.0f, 0.0f, 0.9f, false, 579, 4, 1, 4 + rand.nextInt(4), 0, 0.5f + rand.nextFloat() * 0.2f, 0.0f, 0);
            }
            case 12 -> { if (rand.nextBoolean()) FXDispatcher.slimeJumpFX(boss, 0); } // infested
            case 13 -> // tainted
                FXDispatcher.drawGenericParticles(x, y, z, 0, -0.01, 0, 0.1f + rand.nextFloat() * 0.2f, 0.0f, 0.1f + rand.nextFloat() * 0.1f, 0.25f, false, 1, 5, 1, 6 + rand.nextInt(6), 0, 2.0f + rand.nextFloat(), 0.5f, 1);
            default -> {}
        }
    }

    /** FXDispatcher.spark: a 16-grid FXGeneric flash. */
    public static void spark(double x, double y, double z, float size, float r, float g, float b, float a) {
        var rand = Minecraft.getInstance().level.getRandom();
        FXDispatcher.generic(x, y, z, 0, 0, 0, GenericParticleOptions.builder().maxAge(5 + rand.nextInt(5)).alpha(a).color(r, g, b).grid(16)
                .particles(8 + rand.nextInt(3) * 16, 8, 1).scale(size).flipped(rand.nextBoolean()).build(), 0);
    }

    /** RenderEventHandler.renderLivingPre: the champion tint (fiery, grim, venomous, vampiric), as an ARGB multiplier. */
    public static int tint(int id) {
        return switch (id) {
            case 8 -> 0xFFFFBFFF; // glColor4f(1, 0.75, 5) clamps to (1, 0.75, 1)
            case 4 -> 0xFF999999;
            case 10 -> 0xFF99FFB2;
            case 11 -> 0xFFFFB2B2;
            default -> 0xFFFFFFFF;
        };
    }

    private ChampionFx() {}
}
