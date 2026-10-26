package art.arcane.thaumcraft.blockentity;

import art.arcane.thaumcraft.block.ArcaneEarBlock;
import art.arcane.thaumcraft.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.NoteBlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * TC6 TileArcaneEar: listens for note blocks within 64 blocks playing its own note and instrument (the instrument a
 * note block standing on the ear's mounting block would play) and emits a 10 tick pulse, or toggles for the toggle ear.
 */
public final class ArcaneEarBlockEntity extends BlockEntity {
    public record NoteEvent(BlockPos pos, int instrument, int note) {}
    /** Note block plays heard this tick, per level (cleared at the end of each level tick). */
    public static final Map<Level, List<NoteEvent>> NOTE_EVENTS = new WeakHashMap<>();

    public int note, tone, redstoneSignal;

    public ArcaneEarBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.ARCANE_EAR.get(), pos, state); }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((NoteBlockEvent.Play e) -> {
            if (e.getLevel() instanceof Level l && !l.isClientSide())
                NOTE_EVENTS.computeIfAbsent(l, k -> new ArrayList<>()).add(new NoteEvent(e.getPos().immutable(), e.getInstrument().ordinal(), e.getVanillaNoteId()));
        });
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> { var l = NOTE_EVENTS.get(e.getLevel()); if (l != null) l.clear(); });
    }

    /** The instrument of the block the ear is mounted on. */
    public int instrumentOfMount() {
        var facing = getBlockState().getValue(ArcaneEarBlock.FACING).getOpposite();
        return level.getBlockState(worldPosition.relative(facing)).instrument().ordinal();
    }
    public void updateTone() { tone = instrumentOfMount(); setChanged(); }
    public void changePitch() { note = (note + 1) % 25; setChanged(); }
    public void triggerNote(boolean sound) { level.blockEvent(worldPosition, getBlockState().getBlock(), sound ? instrumentOfMount() : -1, note); }

    public static void tick(Level world, BlockPos pos, BlockState state, ArcaneEarBlockEntity te) {
        if (world.isClientSide()) return;
        if (te.redstoneSignal > 0 && --te.redstoneSignal == 0) ArcaneEarBlock.setEnabled(world, pos, world.getBlockState(pos), false);
        var events = NOTE_EVENTS.get(world);
        if (events == null) return;
        for (NoteEvent e : events) {
            if (e.instrument() == te.tone && e.note() == te.note && pos.distToCenterSqr(e.pos().getX() + 0.5, e.pos().getY() + 0.5, e.pos().getZ() + 0.5) <= 4096.0) {
                te.triggerNote(true);
                BlockState s = world.getBlockState(pos);
                if (((ArcaneEarBlock) s.getBlock()).toggle) ArcaneEarBlock.setEnabled(world, pos, s, !s.getValue(ArcaneEarBlock.ENABLED));
                else { te.redstoneSignal = 10; ArcaneEarBlock.setEnabled(world, pos, s, true); }
                break;
            }
        }
    }

    @Override protected void saveAdditional(ValueOutput o) { super.saveAdditional(o); o.putInt("note", note); o.putInt("tone", tone); }
    @Override protected void loadAdditional(ValueInput i) { super.loadAdditional(i); note = Math.clamp(i.getIntOr("note", 0), 0, 24); tone = i.getIntOr("tone", 0); }
}
