package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Visuals for the shapes pack ({@link FieldShapes}): a field lights each block it strikes with a small glow
 * and a mote of the element, so the pattern reads at a glance; a kin shape draws a thread of the element to
 * each creature it chose. Fizzles show a shape that found nothing.
 */
final class FieldShapesVfx {
	private FieldShapesVfx() {}

	private static void glow(ServerLevel level, int color, Vec3 at, double size) {
		Vfx.emit(level, SigilOption.glow(color, (float) size), at, 1, 0.0, 0.0);
	}

	static void field(ServerLevel level, List<BlockPos> cells, Direction face, Vfx.Theme theme, String path) {
		boolean first = true;
		for (BlockPos p : cells) {
			Vec3 at = Vec3.atCenterOf(p).add(face.getStepX() * 0.55, face.getStepY() * 0.55, face.getStepZ() * 0.55);
			glow(level, first ? theme.primary() : theme.secondary(), at, first ? 0.55 : 0.35);
			Vfx.emit(level, theme.mote(), at, 2, 0.2, 0.01);
			first = false;
		}
		Vec3 center = Vec3.atCenterOf(cells.getFirst());
		Vfx.impact(level, center.add(0, 0.6, 0), theme, 0.6);
		Feels.sound(level, center, "field_pulse", 0.45f, 1.0f);
	}

	static void kin(ServerLevel level, Vec3 center, List<Entity> chosen, Vfx.Theme theme) {
		glow(level, theme.primary(), center, 0.6);
		int drawn = 0;
		for (Entity e : chosen) {
			Vec3 to = e.getBoundingBox().getCenter();
			if (drawn++ < 12 && to.distanceToSqr(center) > 0.25) {
				Vfx.beam(level, center, to, theme);
			}
			glow(level, theme.secondary(), to.add(0, e.getBbHeight() * 0.5 + 0.3, 0), 0.35);
		}
		Feels.sound(level, center, "note_link", 0.4f, 1.1f);
	}

	static void fizzle(ServerLevel level, Vec3 at, Vfx.Theme theme) {
		Vfx.emit(level, theme.mote(), at, 4, 0.25, 0.02);
		glow(level, theme.secondary(), at, 0.25);
	}
}
