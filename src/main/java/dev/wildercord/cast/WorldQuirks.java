package dev.wildercord.cast;

import dev.wildercord.player.Heart;
import dev.wildercord.spell.Attunements;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneQuirks;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This world's rune quirks at runtime (see {@link RuneQuirks}): {@link Effects} asks for a quirked rune's power and
 * duration as it lands, and calls {@link #after} once it has, for a quirk's faint second strike. A quirk holds where the
 * effect lands (under that sky, at that depth, in that dimension), for everyone's spells: it's the world's nature. The
 * first time one matters to a player's spell, it goes into their Grimoire.
 */
public final class WorldQuirks {
	private WorldQuirks() {}

	/** The quirks in force, by rune id; worked out once per world and forgotten when the draw changes. */
	private static volatile Map<String, RuneQuirks.Quirk> byRune;

	static void forget() {
		byRune = null;
	}

	private static RuneQuirks.Quirk quirkOf(Cast cast, RuneDef rune) {
		Map<String, RuneQuirks.Quirk> quirks = byRune;
		if (quirks == null) {
			MinecraftServer server = cast.level.getServer();
			if (server == null) {
				return null;
			}
			Map<String, RuneQuirks.Quirk> made = new HashMap<>();
			for (RuneQuirks.Quirk quirk : WorldResonances.quirks(server)) {
				made.put(quirk.rune(), quirk);
			}
			quirks = Map.copyOf(made);
			byRune = quirks;
		}
		return quirks.isEmpty() ? null : quirks.get(rune.id());
	}

	/** {@link RuneQuirks#POWER} for a rune whose stronger quirk holds where it lands, otherwise 1. */
	public static double power(Cast cast, RuneDef rune, Cast.Hit hit) {
		RuneQuirks.Quirk quirk = holding(cast, rune, hit, RuneQuirks.Kind.STRONGER);
		return quirk == null ? 1.0 : RuneQuirks.POWER;
	}

	/** {@link RuneQuirks#DURATION} for a rune whose longer quirk holds where it lands, otherwise 1. */
	public static double duration(Cast cast, RuneDef rune, Cast.Hit hit) {
		RuneQuirks.Quirk quirk = holding(cast, rune, hit, RuneQuirks.Kind.LONGER);
		return quirk == null ? 1.0 : RuneQuirks.DURATION;
	}

	/** After an effect has landed: a quirk's faint second strike, once a cast, a moment later. */
	public static void after(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, double groupPower) {
		RuneQuirks.Quirk quirk = holding(cast, node.effect, hit, RuneQuirks.Kind.ECHO);
		if (quirk == null || !cast.once("quirk_echo:" + quirk.id())) {
			return;
		}
		Scheduler.later(RuneQuirks.ECHO_TICKS, () -> {
			if (!cast.alive()) {
				return;
			}
			List<Entity> still = hit.entities().stream().filter(e -> e.isAlive() && e.level() == cast.level).toList();
			TwistVfx.quirk(cast.level, hit.point(), RuneColors.of(node.effect));
			Effects.apply(cast, node, new Cast.Hit(still, hit.point(), hit.dir(), hit.origin(), hit.block(), hit.face(), hit.self(), hit.power()),
				groupPower * RuneQuirks.ECHO_POWER);
		});
	}

	/** {@code rune}'s quirk of {@code kind}, if it has one and it holds where {@code hit} lands; met by the caster if so. */
	private static RuneQuirks.Quirk holding(Cast cast, RuneDef rune, Cast.Hit hit, RuneQuirks.Kind kind) {
		if (cast.passive) {
			// A passive renewing itself is upkeep, not a spell landing anywhere.
			return null;
		}
		RuneQuirks.Quirk quirk = quirkOf(cast, rune);
		if (quirk == null || quirk.kind() != kind || !quirk.holds(placeAt(cast.level, hit.point()))) {
			return null;
		}
		if (cast.caster instanceof ServerPlayer player && cast.once("quirk_met:" + quirk.id())) {
			met(player, quirk, hit.point(), RuneColors.of(rune));
		}
		return quirk;
	}

	/** The first time a quirk matters to a player's spell: it goes into their Grimoire, and they're told. */
	private static void met(ServerPlayer player, RuneQuirks.Quirk quirk, Vec3 at, int color) {
		if (Heart.discovered(player, quirk.key()) || !Grimoire.unlock(player, quirk.key(), false)) {
			return;
		}
		player.sendSystemMessage(Component.translatable("message.wildercord.quirk_met", Component.literal(quirk.text()).withColor(color))
			.withStyle(ChatFormatting.ITALIC).withColor(0xB8C8E8));
		ServerPlayNetworking.send(player, new WorldResonances.Revealed(WorldResonances.Revealed.QUIRK, quirk.text(), color));
		TwistVfx.quirk(player.level(), at, color);
		WorldResonances.refresh(player);
	}

	/** Everything a quirk can ask about where an effect lands (as {@link Attunement#placeOf} does for a player). */
	static Attunements.Place placeAt(ServerLevel level, Vec3 at) {
		BlockPos pos = BlockPos.containing(at);
		String biome = level.getBiome(pos).unwrapKey().map(k -> k.identifier().toString()).orElse("");
		String dimension = level.dimension().identifier().toString();
		long time = Math.floorMod(level.getOverworldClockTime(), 24000L);
		int moon = level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos).index();
		Biome.Precipitation falling = level.precipitationAt(pos.above());
		String precipitation = falling == Biome.Precipitation.RAIN ? "rain" : falling == Biome.Precipitation.SNOW ? "snow" : "none";
		return new Attunements.Place(biome, dimension, pos.getY(), time, moon, precipitation, level.canSeeSky(pos.above()), false);
	}
}
