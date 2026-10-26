package art.arcane.thaumcraft.block.eldritch;

import art.arcane.thaumcraft.registry.ModSounds;
import art.arcane.thaumcraft.fx.Fx;
import art.arcane.thaumcraft.progression.PlayerProgressManager;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * TC4 rune stone: after a player has stood near it for 5 seconds it lashes out with wisp-like arcs that hurt and
 * give temporary warp, then keeps zapping every 2 seconds while someone stays in range.
 */
public final class EldritchRuneStoneBlockEntity extends BlockEntity {
    public static final int RANGE = 6, CHARGE = 100, REPEAT = 40;
    private int charge;

    public EldritchRuneStoneBlockEntity(BlockPos pos, BlockState s) { super(ModBlockEntities.ELDRITCH_RUNE_STONE.get(), pos, s); }
    public int charge() { return charge; }

    public static void tick(Level level, BlockPos pos, BlockState s, EldritchRuneStoneBlockEntity be) {
        if (!(level instanceof ServerLevel w)) return;
        var near = w.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(RANGE), p -> !p.isSpectator() && !p.isCreative() && p.isAlive());
        if (near.isEmpty()) { be.charge = 0; return; }
        if (++be.charge < CHARGE || (be.charge - CHARGE) % REPEAT != 0) return;
        for (ServerPlayer p : near) {
            Fx.send(w, "zap", pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, (float) p.getX(), (float) (p.getY() + p.getBbHeight() / 2), (float) p.getZ(), 0xAA44FF, 1.0f);
            w.playSound(null, pos, ModSounds.get("zap"), SoundSource.BLOCKS, 1.0f, 0.8f);
            p.hurtServer(w, w.damageSources().magic(), 3.0f);
            PlayerProgressManager.addWarp(p, PlayerProgressManager.WarpType.TEMPORARY, 1);
        }
    }
}
