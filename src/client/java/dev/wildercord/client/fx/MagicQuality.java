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
	public static void preset(String name) {
		switch(name) {
			case "performance" -> {own=Level.BALANCED;others=Level.MINIMAL;reducedFlash=true;cameraShake=false;
				bladeTrails=Trails.SUBTLE;bodyAura=BodyAura.CALM;impact=Impact.SOFT;banners=Banners.OWN;}
			case "cinematic" -> {own=Level.FULL;others=Level.FULL;reducedFlash=false;cameraShake=true;
				bladeTrails=Trails.FULL;bodyAura=BodyAura.FULL;impact=Impact.FULL;banners=Banners.ALL;}
			default -> {own=Level.BALANCED;others=Level.BALANCED;reducedFlash=false;cameraShake=true;
				bladeTrails=Trails.FULL;bodyAura=BodyAura.FULL;impact=Impact.FULL;banners=Banners.ALL;}
		}save();
	}
	public static void load() {
		try {
			if (!Files.exists(FILE)) { save(); return; }
			var json = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
			if (json.has("own")) own = Level.valueOf(json.get("own").getAsString());
			if (json.has("others")) others = Level.valueOf(json.get("others").getAsString());
			if (json.has("reduced_flash")) reducedFlash = json.get("reduced_flash").getAsBoolean();
			if (json.has("camera_shake")) cameraShake = json.get("camera_shake").getAsBoolean();
			if (json.has("spell_titles")) spellTitles = json.get("spell_titles").getAsBoolean();
			if (json.has("string_indicator")) stringIndicator = StringIndicator.valueOf(upper(json.get("string_indicator").getAsString()));
			if (json.has("blade_trails")) bladeTrails = Trails.valueOf(upper(json.get("blade_trails").getAsString()));
			if (json.has("body_aura")) bodyAura = BodyAura.valueOf(upper(json.get("body_aura").getAsString()));
			if (json.has("impact")) impact = Impact.valueOf(upper(json.get("impact").getAsString()));
			if (json.has("banners")) banners = Banners.valueOf(upper(json.get("banners").getAsString()));
		} catch (Exception e) { dev.wildercord.Wildercord.LOGGER.warn("Invalid local magic preferences: {}", e.toString()); }
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
