package dev.wildercord.cast.feel;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Vfx;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reaction marks made visible: while a creature carries a mark ({@link Reactions.Mark}), a small halo in the mark's own colour
 * shows it, a few particles every half second, and a tiny tick sounds when a mark is first set. So a player can see that the
 * frozen husk will Shatter, the shadowed one will Blight.
 *
 * <p>Nothing to call for a rune that marks through {@link Reactions#mark}: that tells this class. A mark kept elsewhere (a rune's
 * own state) can show the same halo with {@link #show(ServerLevel, Entity, Reactions.Mark)} each time it wants, or its own colour
 * with {@link #halo(ServerLevel, Entity, int, Style)}.</p>
 */
public final class MarkHalos {
	private MarkHalos() {}

	/** How a halo is drawn. */
	public enum Style {
		/** A slow ring over the head (Frozen, Resonant). */
		CROWN,
		/** A small crescent circling the body (Windswept, Pulled). */
		ORBIT,
		/** Drops falling off the creature (Soaked, Wet, Bleeding). */
		DRIP,
		/** A few motes of the colour rising off it (Shadowed, Ionised). */
		MOTES,
		/** A cracked seal at its feet (Cracked). */
		CRACKS
	}

	/** How often halos are redrawn, in ticks. */
	public static final int PERIOD = 10;

	/** Creatures that may carry marks, by id (dropped once they have none left, die or leave). */
	private static final Map<UUID, Entity> TRACKED = new ConcurrentHashMap<>();

	/** The colour of a mark's halo. */
	public static int color(Reactions.Mark mark) {
		return switch (mark) {
			case FROZEN -> ElementFx.FROST.secondary();
			case WINDSWEPT -> ElementFx.WIND.accent();
			case PULLED -> ElementFx.VOID.primary();
			case SOAKED, WET -> 0x4A90E0;
			case RESONANT -> ElementFx.ARCANE.primary();
			case CRACKED -> ElementFx.EARTH.secondary();
			case SHADOWED -> 0x3A1060;
			case BLEEDING -> ElementFx.BLOOD.primary();
			case IONISED -> ElementFx.STORM.primary();
			case AIRBORNE -> 0xBFE3FF;
		};
	}

	/** How a mark's halo is drawn. */
	public static Style style(Reactions.Mark mark) {
		return switch (mark) {
			case FROZEN, RESONANT -> Style.CROWN;
			case WINDSWEPT, PULLED -> Style.ORBIT;
			case SOAKED, WET, BLEEDING -> Style.DRIP;
			case SHADOWED, IONISED, AIRBORNE -> Style.MOTES;
			case CRACKED -> Style.CRACKS;
		};
	}

	/** Called by {@link Reactions#mark}: remembers the creature, and a mark it didn't have yet gets its tick and a first halo. */
	public static void marked(Entity target, Reactions.Mark mark, boolean fresh) {
		if (!(target.level() instanceof ServerLevel level)) {
			return;
		}
		TRACKED.put(target.getUUID(), target);
		if (fresh) {
			show(level, target, mark);
			Feels.sound(level, target.getBoundingBox().getCenter(), "tell_tick", 0.25F, Feels.step(mark.ordinal() % 5 + 3));
		}
	}

	/** One halo of {@code mark} on {@code target}, now. */
	public static void show(ServerLevel level, Entity target, Reactions.Mark mark) {
		halo(level, target, color(mark), style(mark));
	}

	/** One halo in any colour and style, for a rune's own mark. */
	public static void halo(ServerLevel level, Entity target, int color, Style style) {
		Vec3 feet = target.position();
		double h = target.getBbHeight();
		double w = Math.max(0.4, target.getBbWidth() * 0.6);
		switch (style) {
			case CROWN -> Light.ring(level, feet.add(0, h + 0.25, 0), new Vec3(0, 1, 0), color, w * 0.8, w * 0.8, 0.025, PERIOD + 2);
			case ORBIT -> {
				double a = level.getGameTime() * 0.35;
				ElementFx.slash(level, feet.add(0, h * 0.55, 0), new Vec3(0, 1, 0), ElementFx.flatDir(a), color, w + 0.25, 1.2, 0.03, 2, PERIOD);
			}
			case DRIP -> Vfx.emit(level, new DustParticleOptions(color, 0.7F), feet.add(0, h * 0.7, 0), 2, w * 0.5, 0.0);
			case MOTES -> Vfx.emit(level, new DustParticleOptions(color, 0.8F), feet.add(0, h * 0.8, 0), 2, w * 0.6, 0.01);
			case CRACKS -> ElementFx.flatSigil(level, feet, SigilOption.CRACKED, color, w + 0.4, PERIOD + 2, 0.0);
		}
	}

	/** Every {@link #PERIOD} ticks: a halo for each live mark (two at most per creature, the rest take turns). */
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % PERIOD != 0) {
			return;
		}
		TRACKED.values().removeIf(e -> {
			if (e.isRemoved() || !e.isAlive() || !(e.level() instanceof ServerLevel level)) {
				return true;
			}
			Set<Reactions.Mark> marks = Reactions.marks(e);
			if (marks.isEmpty()) {
				return true;
			}
			int shown = 0;
			int turn = (int) (server.getTickCount() / PERIOD);
			Reactions.Mark[] all = marks.toArray(new Reactions.Mark[0]);
			for (int i = 0; i < all.length && shown < 2; i++) {
				show(level, e, all[(i + turn) % all.length]);
				shown++;
			}
			return false;
		});
	}

	/** Forget everything (the server stopped). */
	public static void clear() {
		TRACKED.clear();
	}

	/** How many creatures are being watched, for tests. */
	static int tracked() {
		return TRACKED.size();
	}
}
