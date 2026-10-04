package dev.wildercord.client.fx;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;

/** Local visual preferences; gameplay and hostile warnings never depend on these. */
public final class MagicQuality {
	public enum Level { FULL, BALANCED, MINIMAL; public Level next() { return values()[(ordinal() + 1) % values().length]; } }
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("wildercord-visuals.json");
	public static Level own = Level.BALANCED, others = Level.BALANCED;
	public static boolean reducedFlash, cameraShake = true;
	/** Whether the names of mastered spells others cast nearby (and your own) show as a brief title by the caster. */
	public static boolean spellTitles = true;
	/** Where the sword string indicator shows: a little below the crosshair, above the aura bar, or not at all (its ticks go quiet too). */
	public enum StringIndicator { CROSSHAIR, HOTBAR, HIDDEN; public StringIndicator next() { return values()[(ordinal() + 1) % values().length]; } }
	public static StringIndicator stringIndicator = StringIndicator.CROSSHAIR;

	/** Aura's blade trails: whole (with the stage's extras), plain ribbons only (and others' ordinary swings left out), or none. */
	public enum Trails { FULL, SUBTLE, OFF; public Trails next() { return values()[(ordinal() + 1) % values().length]; } }
	/** The body's aura: whole (calm at rest, flaring in a fight, with its wisps and embers), calm always and without them, or none. */
	public enum BodyAura { FULL, CALM, OFF; public BodyAura next() { return values()[(ordinal() + 1) % values().length]; } }
	/** How hard aura's blows land on screen: the full hit-stop and flash, half of each, or no hit-stop and only a small flash. */
	public enum Impact {
		FULL(1.0, 1.0F), SOFT(0.5, 0.75F), OFF(0.0, 0.45F);
		/** How much of the hit-stop is kept, and how big the flash is. */
		public final double hitStop;
		public final float flash;
		Impact(double hitStop, float flash) { this.hitStop = hitStop; this.flash = flash; }
		public Impact next() { return values()[(ordinal() + 1) % values().length]; }
	}
	/** Whose technique banners show: everyone's (yours by the side of the screen, others' over their heads), only yours, or none. */
	public enum Banners { ALL, OWN, OFF; public Banners next() { return values()[(ordinal() + 1) % values().length]; } }
	public static Trails bladeTrails = Trails.FULL;
	public static BodyAura bodyAura = BodyAura.FULL;
	public static Impact impact = Impact.FULL;
	public static Banners banners = Banners.ALL;

	private MagicQuality() {}
	/** The settings a profile sets, together: how much each group's magic draws, flashing, camera motion and aura's feel. */
	private record Preset(Level own, Level others, boolean reducedFlash, boolean cameraShake, Trails bladeTrails, BodyAura bodyAura, Impact impact,
		Banners banners) {
		boolean active() {
			return MagicQuality.own == own && MagicQuality.others == others && MagicQuality.reducedFlash == reducedFlash
				&& MagicQuality.cameraShake == cameraShake && MagicQuality.bladeTrails == bladeTrails && MagicQuality.bodyAura == bodyAura
				&& MagicQuality.impact == impact && MagicQuality.banners == banners;
		}
	}
	/** The three profiles, in the order the settings screen lists them; profiles/&lt;name&gt;/config/wildercord-visuals.json matches each. */
	private static final java.util.Map<String, Preset> PRESETS = java.util.Map.of(
		"performance", new Preset(Level.BALANCED, Level.MINIMAL, true, false, Trails.SUBTLE, BodyAura.CALM, Impact.SOFT, Banners.OWN),
		"balanced", new Preset(Level.BALANCED, Level.BALANCED, false, true, Trails.FULL, BodyAura.FULL, Impact.FULL, Banners.ALL),
		"cinematic", new Preset(Level.FULL, Level.FULL, false, true, Trails.FULL, BodyAura.FULL, Impact.FULL, Banners.ALL));

	/** Applies a profile by name ("performance", "balanced" or "cinematic"; anything else is balanced) and saves it. */
	public static void preset(String name) {
		Preset p = PRESETS.getOrDefault(name, PRESETS.get("balanced"));
		own = p.own; others = p.others; reducedFlash = p.reducedFlash; cameraShake = p.cameraShake;
		bladeTrails = p.bladeTrails; bodyAura = p.bodyAura; impact = p.impact; banners = p.banners;
		save();
	}

	/** Whether the current settings are exactly that profile's, so the screen can show which one is in use. */
	public static boolean isPreset(String name) {
		Preset p = PRESETS.get(name);
		return p != null && p.active();
	}
	public static void load() {
		com.google.gson.JsonObject json;
		try {
			if (!Files.exists(FILE)) { save(); return; }
			json = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
		} catch (Exception e) { dev.wildercord.Wildercord.LOGGER.warn("Invalid local magic preferences: {}", e.toString()); return; }
		// Each setting is read on its own, so one hand-edited or outdated value keeps its default without losing the rest.
		own = read(json, "own", Level.class, own);
		others = read(json, "others", Level.class, others);
		reducedFlash = read(json, "reduced_flash", reducedFlash);
		cameraShake = read(json, "camera_shake", cameraShake);
		spellTitles = read(json, "spell_titles", spellTitles);
		stringIndicator = read(json, "string_indicator", StringIndicator.class, stringIndicator);
		bladeTrails = read(json, "blade_trails", Trails.class, bladeTrails);
		bodyAura = read(json, "body_aura", BodyAura.class, bodyAura);
		impact = read(json, "impact", Impact.class, impact);
		banners = read(json, "banners", Banners.class, banners);
	}
	private static <E extends Enum<E>> E read(com.google.gson.JsonObject json, String key, Class<E> type, E fallback) {
		if (!json.has(key)) return fallback;
		try { return Enum.valueOf(type, upper(json.get(key).getAsString())); }
		catch (Exception e) { dev.wildercord.Wildercord.LOGGER.warn("Ignoring local magic preference {}: {}", key, e.toString()); return fallback; }
	}
	private static boolean read(com.google.gson.JsonObject json, String key, boolean fallback) {
		if (!json.has(key)) return fallback;
		try { return json.get(key).getAsBoolean(); }
		catch (Exception e) { dev.wildercord.Wildercord.LOGGER.warn("Ignoring local magic preference {}: {}", key, e.toString()); return fallback; }
	}
	private static String upper(String s) { return s.toUpperCase(java.util.Locale.ROOT); }
	private static String lower(Enum<?> e) { return e.name().toLowerCase(java.util.Locale.ROOT); }
	public static void save() {
		var json = new com.google.gson.JsonObject();
		json.addProperty("own", own.name()); json.addProperty("others", others.name());
		json.addProperty("reduced_flash", reducedFlash); json.addProperty("camera_shake", cameraShake);
		json.addProperty("spell_titles", spellTitles);
		json.addProperty("string_indicator", lower(stringIndicator));
		json.addProperty("blade_trails", lower(bladeTrails));
		json.addProperty("body_aura", lower(bodyAura));
		json.addProperty("impact", lower(impact));
		json.addProperty("banners", lower(banners));
		try { Files.createDirectories(FILE.getParent()); Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(json)); }
		catch (Exception e) { dev.wildercord.Wildercord.LOGGER.warn("Cannot save local magic preferences: {}", e.toString()); }
	}
}
