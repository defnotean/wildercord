package dev.wildercord.client.fx;

import com.google.gson.JsonObject;
import dev.wildercord.client.CombatPresentation;
import dev.wildercord.presentation.CombatPresentationOptions;
import dev.wildercord.presentation.VisualPreferencesFile;
import net.fabricmc.loader.api.FabricLoader;

/** Local visual preferences; gameplay and hostile warnings never depend on these. */
public final class MagicQuality {
	public enum Level { FULL, BALANCED, MINIMAL; public Level next() { return values()[(ordinal() + 1) % values().length]; } }
	private static final VisualPreferencesFile FILE = new VisualPreferencesFile(FabricLoader.getInstance().getConfigDir().resolve("wildercord-visuals.json"));
	public static Level own = Level.BALANCED, others = Level.BALANCED;
	public static boolean reducedFlash, cameraShake = true;
	/** Whether the names of mastered spells others cast nearby (and your own) show as a brief title by the caster. */
	public static boolean spellTitles = true;
	/** Whether magic circles, rings and warning reticles are drawn in colour-blind-safe colours ({@link dev.wildercord.presentation.SafeColourRules}). */
	public static boolean safeTelegraphs;
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
	public static void preset(String name) {
		switch(name) {
			case "performance" -> {own=Level.BALANCED;others=Level.MINIMAL;reducedFlash=true;cameraShake=false;
				bladeTrails=Trails.SUBTLE;bodyAura=BodyAura.CALM;impact=Impact.SOFT;banners=Banners.OWN;}
			case "cinematic" -> {own=Level.FULL;others=Level.FULL;reducedFlash=false;cameraShake=true;
				bladeTrails=Trails.FULL;bodyAura=BodyAura.FULL;impact=Impact.FULL;banners=Banners.ALL;}
			default -> {own=Level.BALANCED;others=Level.BALANCED;reducedFlash=false;cameraShake=true;
				bladeTrails=Trails.FULL;bodyAura=BodyAura.FULL;impact=Impact.FULL;banners=Banners.ALL;}
		}HitStop.clear();ScreenEffects.clearCameraMotion();save();
	}
	public static void load() {
		try {
			var json = FILE.read();
			CombatPresentation.loaded(CombatPresentationOptions.parse(json.get(CombatPresentationOptions.GROUP)));
			// Parse each legacy preference separately: one invalid field cannot hide the new group.
			read(json,"own",()->own=Level.valueOf(upper(json.get("own").getAsString())));
			read(json,"others",()->others=Level.valueOf(upper(json.get("others").getAsString())));
			read(json,"reduced_flash",()->reducedFlash=json.get("reduced_flash").getAsBoolean());
			read(json,"camera_shake",()->cameraShake=json.get("camera_shake").getAsBoolean());
			read(json,"spell_titles",()->spellTitles=json.get("spell_titles").getAsBoolean());
			read(json,"safe_telegraphs",()->safeTelegraphs=json.get("safe_telegraphs").getAsBoolean());
			read(json,"string_indicator",()->stringIndicator=StringIndicator.valueOf(upper(json.get("string_indicator").getAsString())));
			read(json,"blade_trails",()->bladeTrails=Trails.valueOf(upper(json.get("blade_trails").getAsString())));
			read(json,"body_aura",()->bodyAura=BodyAura.valueOf(upper(json.get("body_aura").getAsString())));
			read(json,"impact",()->impact=Impact.valueOf(upper(json.get("impact").getAsString())));
			read(json,"banners",()->banners=Banners.valueOf(upper(json.get("banners").getAsString())));
		} catch (Exception e) {
			CombatPresentation.loaded(new CombatPresentationOptions.Parsed(CombatPresentationOptions.Saved.LEGACY,CombatPresentationOptions.Warning.INVALID_SAVED));
			dev.wildercord.Wildercord.LOGGER.warn("Cannot read local magic preferences; original file preserved");
		}
	}
	private static void read(JsonObject json,String key,Runnable apply) {
		if(!json.has(key))return;
		try{apply.run();}catch(RuntimeException invalid){dev.wildercord.Wildercord.LOGGER.warn("Invalid local magic preference: {}",key);}
	}
	private static String upper(String s) { return s.toUpperCase(java.util.Locale.ROOT); }
	private static String lower(Enum<?> e) { return e.name().toLowerCase(java.util.Locale.ROOT); }
	private static JsonObject legacyValues() {
		var json = new com.google.gson.JsonObject();
		json.addProperty("own", own.name()); json.addProperty("others", others.name());
		json.addProperty("reduced_flash", reducedFlash); json.addProperty("camera_shake", cameraShake);
		json.addProperty("spell_titles", spellTitles); json.addProperty("safe_telegraphs", safeTelegraphs);
		json.addProperty("string_indicator", lower(stringIndicator));
		json.addProperty("blade_trails", lower(bladeTrails));
		json.addProperty("body_aura", lower(bodyAura));
		json.addProperty("impact", lower(impact));
		json.addProperty("banners", lower(banners));
		return json;
	}
	public static void save() {
		try { FILE.write(legacyValues(),null); }
		catch (Exception e) { dev.wildercord.Wildercord.LOGGER.warn("Cannot save local magic preferences; original file preserved"); }
	}
	/** Transactional combat Apply: persist first, then change live choices. No session-only success. */
	public static boolean saveCombat(CombatPresentationOptions.Saved selection) {
		try { FILE.write(new JsonObject(),selection); }
		catch (Exception e) { dev.wildercord.Wildercord.LOGGER.warn("Cannot save combat presentation preferences; original file preserved"); return false; }
		CombatPresentation.applied(selection);
		return true;
	}
}
