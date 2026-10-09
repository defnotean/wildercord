package dev.wildercord.aura.arts;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.AuraFxRules;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Vfx;
import dev.wildercord.cast.feel.Feels;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

import static dev.wildercord.aura.MethodsBArtRules.*;

/**
 * Echo Breath's arts: sound. A blow rings, and its ring comes back as a second blow a moment later on whoever it found; foes caught
 * in the sound reel (a player's view swims, a mob stumbles and loses its aim). Its light is silver and violet, drawn as rings
 * spreading the way sound does.
 * <ul>
 * <li><b>Ringing Cut</b> (I): an arc, and its ring striking the same foes again a moment later.</li>
 * <li><b>Resonant Chord</b> (II): a cone of sound loosed ahead: foes in it struck, reeling and slowed.</li>
 * <li><b>Counterpoint</b> (III): the answer to a press: a ring of sound all round, foes thrown back reeling, then its echo.</li>
 * <li><b>Reverb Step</b> (IV): a dash; the foes in the way cut, then cut again by the dash's echo.</li>
 * <li><b>Grand Resonance</b> (V): a great toll round you, and three echoes after it, each a ring further out.</li>
 * </ul>
 */
public final class EchoArts {
	private EchoArts() {}

	public static final String METHOD = ECHO;
	public static final String RINGING_CUT = "ringing_cut";
	public static final String RESONANT_CHORD = "resonant_chord";
	public static final String COUNTERPOINT = "counterpoint";
	public static final String REVERB_STEP = "reverb_step";
	public static final String GRAND_RESONANCE = "grand_resonance";

	public static List<AuraApi.StringArt> arts() {
		return List.of(
			MethodArts.art(AuraApi.ArtSlot.FIRST, RINGING_CUT, EchoArts::ringingCut),
			MethodArts.art(AuraApi.ArtSlot.SECOND, RESONANT_CHORD, EchoArts::resonantChord),
			MethodArts.art(AuraApi.ArtSlot.THIRD, COUNTERPOINT, EchoArts::counterpoint),
			MethodArts.art(AuraApi.ArtSlot.FOURTH, REVERB_STEP, EchoArts::reverbStep),
			MethodArts.art(AuraApi.ArtSlot.FINAL, GRAND_RESONANCE, EchoArts::grandResonance));
	}

	/** The violet the arts are drawn in: the aura's own colour taken toward violet. */
	static int violet(ServerPlayer player) {
		return ArtKit.mix(ArtKit.color(player), MethodsBKit.VIOLET, 0.55);
	}

	/** Sound, seen: rings spreading from {@code at} facing {@code normal}, violet edged in silver. */
	static void rings(ServerPlayer player, Vec3 at, Vec3 normal, double radius, int count, int color) {
		ArtLight world = ArtLight.world(player);
		for (int i = 0; i < count; i++) {
			double r = radius * (i + 1) / count;
			world.ring(at, normal, i % 2 == 0 ? color : MethodsBKit.SILVER, r * 0.6, r, 0.06, 8 + 2 * i);
		}
		Vfx.emit(player.level(), ParticleTypes.NOTE, at.add(0, 0.4, 0), Math.min(6, 2 + count), radius * 0.3, 0.0);
	}

	/** One foe struck again by the ring of what struck it, {@code delay} ticks on, wherever it stands then (if still near). */
	static void echo(ArtKit.Hits hits, LivingEntity foe, double factor, int delay, int reel) {
		ServerPlayer player = hits.player();
		ServerLevel level = player.level();
		Scheduler.later(delay, () -> {
			if (!player.isAlive() || player.level() != level || !foe.isAlive() || foe.level() != level || foe.distanceTo(player) > 16) return;
			Vec3 c = foe.getBoundingBox().getCenter();
			ArtLight.world(player).ring(c, ArtKit.UP, violet(player), 0.2, 1.4, 0.07, 8);
			Motes.glows(level, c, 5, 0.35, MethodsBKit.SILVER, 0.08, 14, Vec3.ZERO, 0.04);
			Feels.sound(level, c, "aura_echo_impact", 0.7F, 1.35F);
			hits.strike(foe, factor, AuraFxRules.Weight.LIGHT);
			MethodsBKit.reel(player, foe, reel);
		});
	}

	// ------------------------------------------------------------------ I. Ringing Cut

	static boolean ringingCut(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = violet(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.CUT, false, 1.35F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_ringing_cut", 1.0F, 1.0F);
		ArtLight.world(player).slash(feet.add(0, 0.9, 0).add(look.scale(1.1)), ArtKit.UP, look, color, RING_REACH * 0.8, 2.2, 0.3, 1, 9);
		for (LivingEntity foe : ArtKit.arc(player, context.struck(), RING_REACH, RING_DEGREES, RING_TARGETS)) {
			hits.strike(foe, RING_FACTOR);
			rings(player, foe.getBoundingBox().getCenter(), look, 1.0, 2, color);
			echo(hits, foe, RING_REPEAT, RING_DELAY, RING_REEL);
		}
		return true;
	}

	// ------------------------------------------------------------------ II. Resonant Chord

	static boolean resonantChord(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = violet(player);
		Vec3 look = ArtKit.flat(player);
		Vec3 eye = player.getEyePosition().subtract(0, 0.4, 0);
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.DRAW, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, eye, "aura_art_resonant_chord", 1.1F, 1.0F);
		// The chord leaves the blade as rings growing on their way out.
		for (int i = 1; i <= 4; i++) {
			int step = i;
			Scheduler.later(i - 1, () -> {
				Vec3 at = eye.add(look.scale(CHORD_REACH * step / 4.0));
				ArtLight.world(player).ring(at, look, step % 2 == 0 ? MethodsBKit.SILVER : color, 0.3 * step, 0.55 * step + 0.4, 0.06, 7);
			});
		}
		for (LivingEntity foe : ArtKit.arcFrom(player, player.position(), look, CHORD_REACH, CHORD_DEGREES, CHORD_TARGETS)) {
			hits.strike(foe, CHORD_FACTOR, AuraFxRules.Weight.FULL);
			MethodsBKit.reel(player, foe, CHORD_REEL);
			ArtKit.slow(player, foe, CHORD_SLOW, 1);
			Vfx.emit(level, ParticleTypes.NOTE, foe.getBoundingBox().getCenter().add(0, 0.6, 0), 3, 0.3, 0.0);
		}
		return true;
	}

	// ------------------------------------------------------------------ III. Counterpoint

	static boolean counterpoint(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = violet(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.SWEEP, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_counterpoint", 1.1F, 1.0F);
		rings(player, feet.add(0, 0.1, 0), ArtKit.UP, COUNTER_RADIUS, 3, color);
		AuraFx.burst(level, player, feet.add(0, 1, 0), ArtKit.flat(player), color, 1.3F, AuraFx.Burst.RING | AuraFx.Burst.FLASH);
		for (LivingEntity foe : ArtKit.around(player, feet, COUNTER_RADIUS, 1.0, 2.5, 8)) {
			hits.strike(foe, COUNTER_FACTOR);
			ArtKit.knock(foe, feet, COUNTER_THROW, 0.25);
			MethodsBKit.reel(player, foe, COUNTER_REEL);
			echo(hits, foe, COUNTER_REPEAT, COUNTER_DELAY, 0);
		}
		return true;
	}

	// ------------------------------------------------------------------ IV. Reverb Step

	static boolean reverbStep(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = violet(player);
		Vec3 dir = ArtKit.flat(player);
		List<Vec3> path = ArtKit.path(player, dir, REVERB_DISTANCE);
		if (path.size() < 2 || path.getLast().distanceTo(player.position()) < 1.5) {
			MethodArts.blocked(player, REVERB_STEP);
			return false;
		}
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.THRUST, false, 1.4F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		AuraStep.afterimages(player, path.getFirst(), path.getLast(), dir, color);
		Feels.sound(level, path.getFirst().add(0, 1, 0), "aura_art_reverb_step", 1.1F, 1.0F);
		ArtLight world = ArtLight.world(player);
		List<LivingEntity> cut = new ArrayList<>();
		ArtKit.dash(player, path, REVERB_TICKS, (a, b, step, last) -> {
			world.ray(a.add(0, 1.0, 0), b.add(0, 1.0, 0), color, 0.22, 10);
			world.ring(b.add(0, 1.0, 0), dir, MethodsBKit.SILVER, 0.3, 0.9, 0.04, 6);
			Vec3 seg = b.subtract(a);
			for (LivingEntity foe : ArtKit.line(player, a, seg, Math.max(0.5, seg.horizontalDistance()) + 0.8, REVERB_WIDTH / 2, 2.2, REVERB_TARGETS)) {
				if (hits.hurt(foe) || hits.count() >= REVERB_TARGETS) continue;
				hits.strike(foe, REVERB_FACTOR, AuraFxRules.Weight.FULL);
				cut.add(foe);
			}
			if (last) {
				// The dash's echo comes back down the way it went, striking each it cut again.
				Scheduler.later(REVERB_DELAY, () -> {
					if (!player.isAlive() || player.level() != level) return;
					world.ray(path.getLast().add(0, 1.0, 0), path.getFirst().add(0, 1.0, 0), MethodsBKit.SILVER, 0.12, 10);
					Feels.sound(level, path.getFirst().add(0, 1, 0), "aura_echo_art", 0.7F, 1.4F);
				});
				for (LivingEntity foe : cut) echo(hits, foe, REVERB_ECHO, REVERB_DELAY, 20);
			}
		});
		return true;
	}

	// ------------------------------------------------------------------ V. Grand Resonance

	static boolean grandResonance(ServerPlayer player, AuraApi.StringContext context) {
		ServerLevel level = player.level();
		int color = violet(player);
		Vec3 feet = player.position();
		AuraFx.Art fx = AuraFx.art(player).trail(AuraFxRules.Stroke.FALLING, false, 1.6F);
		ArtKit.Hits hits = ArtKit.hits(player, fx);
		Feels.sound(level, feet.add(0, 1, 0), "aura_art_grand_resonance", 1.4F, 1.0F);
		ArtLight.spectacle(player).ring(feet.add(0, 2.5, 0), ArtKit.UP, MethodsBKit.SILVER, 0.5, 2.0, 0.12, 20);
		rings(player, feet.add(0, 0.1, 0), ArtKit.UP, GRAND_RADIUS, 4, color);
		List<LivingEntity> foes = ArtKit.around(player, feet, GRAND_RADIUS, 1.5, 3.0, GRAND_TARGETS);
		for (LivingEntity foe : foes) {
			hits.strike(foe, GRAND_FACTOR, AuraFxRules.Weight.HEAVY);
			MethodsBKit.reel(player, foe, GRAND_REEL);
		}
		for (int i = 1; i <= GRAND_ECHOES; i++) {
			int n = i;
			Scheduler.later(GRAND_PERIOD * i, () -> {
				if (!player.isAlive() || player.level() != level) return;
				rings(player, feet.add(0, 0.1, 0), ArtKit.UP, GRAND_RADIUS * (0.6 + 0.2 * n), 2, n % 2 == 0 ? color : MethodsBKit.SILVER);
				Feels.sound(level, feet.add(0, 1, 0), "aura_echo_art", 0.9F, 1.0F + 0.15F * n);
			});
			for (LivingEntity foe : foes) echo(hits, foe, GRAND_ECHO, GRAND_PERIOD * i, i == GRAND_ECHOES ? 30 : 0);
		}
		return true;
	}
}
