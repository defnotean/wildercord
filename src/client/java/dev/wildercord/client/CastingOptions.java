package dev.wildercord.client;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildercord.spell.TraceGlyph;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * This player's own casting preferences, kept in {@code config/wildercord-casting.json} and set from the
 * Magic settings screen: whether holding sneak while charging traces the spell's glyph, how much help
 * tracing gets, and whose incantations are shown. None of them changes what anyone else sees or what
 * the server allows: turning tracing off only gives up a bonus nobody needs.
 */
public final class CastingOptions {
	private CastingOptions() {}

	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("wildercord-casting.json");

	/** Whose incantations rise from charging casters. */
	public enum Incantations {
		ALL, HIDE_MINE, HIDE_OTHERS, NONE;

		public Incantations next() {
			return values()[(ordinal() + 1) % values().length];
		}

		/** Whether a caster's incantation shows: {@code own} for the player's own. */
		public boolean shows(boolean own) {
			return switch (this) {
				case ALL -> true;
				case HIDE_MINE -> !own;
				case HIDE_OTHERS -> own;
				case NONE -> false;
			};
		}

		public Component label() {
			return Component.translatable("screen.wildercord.casting.incantations." + name().toLowerCase(Locale.ROOT));
		}
	}

	/** Whether holding sneak while charging traces the glyph (and holds the camera still). */
	public static boolean tracing = true;
	/** How much help tracing gets. */
	public static TraceGlyph.Assist assist = TraceGlyph.Assist.LIGHT;
	public static Incantations incantations = Incantations.ALL;
	private static boolean loaded;

	public static void load() {
		loaded = true;
		JsonObject json;
		try {
			if (!Files.exists(FILE)) {
				return;
			}
			json = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
		} catch (Exception e) {
			dev.wildercord.Wildercord.LOGGER.warn("Invalid casting preferences, using the defaults: {}", e.toString());
			return;
		}
		// Each preference is read on its own, so one bad value keeps its default without losing the others.
		try {
			if (json.has("sigil_tracing")) {
				tracing = json.get("sigil_tracing").getAsBoolean();
			}
		} catch (Exception e) {
			dev.wildercord.Wildercord.LOGGER.warn("Ignoring casting preference sigil_tracing: {}", e.toString());
		}
		try {
			if (json.has("trace_assist")) {
				assist = TraceGlyph.Assist.valueOf(json.get("trace_assist").getAsString().toUpperCase(Locale.ROOT));
			}
		} catch (Exception e) {
			dev.wildercord.Wildercord.LOGGER.warn("Ignoring casting preference trace_assist: {}", e.toString());
		}
		try {
			if (json.has("incantations")) {
				incantations = Incantations.valueOf(json.get("incantations").getAsString().toUpperCase(Locale.ROOT));
			}
		} catch (Exception e) {
			dev.wildercord.Wildercord.LOGGER.warn("Ignoring casting preference incantations: {}", e.toString());
		}
	}

	/** Loads the file the first time any preference is read. */
	public static void ensureLoaded() {
		if (!loaded) {
			load();
		}
	}

	public static void save() {
		JsonObject json = new JsonObject();
		json.addProperty("sigil_tracing", tracing);
		json.addProperty("trace_assist", assist.name().toLowerCase(Locale.ROOT));
		json.addProperty("incantations", incantations.name().toLowerCase(Locale.ROOT));
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(json));
		} catch (Exception e) {
			dev.wildercord.Wildercord.LOGGER.warn("Cannot save casting preferences: {}", e.toString());
		}
	}

	public static Component tracingLabel() {
		return Component.translatable("screen.wildercord.casting.tracing",
			Component.translatable(tracing ? "options.on" : "options.off"));
	}

	public static Component assistLabel() {
		return Component.translatable("screen.wildercord.casting.assist",
			Component.translatable("screen.wildercord.casting.assist." + assist.name().toLowerCase(Locale.ROOT)));
	}

	public static Component incantationsLabel() {
		return Component.translatable("screen.wildercord.casting.incantations", incantations.label());
	}
}
