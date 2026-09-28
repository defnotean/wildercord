package dev.wildercord.content;

import dev.wildercord.Wildercord;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wildercord's own sounds. Every one is synthesised by {@code tools/sound_art.py}, which also writes
 * {@code sounds.json} (each event's variants and its subtitle). The tonal ones share one scale, so a
 * cast, its magic circle and its impact always sound well together: play them at pitch 1 (a little
 * random spread is fine) and they stay in key.
 */
public final class WildercordSounds {
	private WildercordSounds() {}

	/** The ten elements, each with a cast (2 variants) and an impact (3 variants). */
	public static final List<String> ELEMENTS = List.of("fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood");

	private static final Map<String, SoundEvent> CASTS = new HashMap<>();
	private static final Map<String, SoundEvent> IMPACTS = new HashMap<>();

	static {
		for (String element : ELEMENTS) {
			CASTS.put(element, register("cast_" + element));
			IMPACTS.put(element, register("impact_" + element));
		}
	}

	// ------------------------------------------------------------------ casting

	/** A warm hum whose overtones slowly shift: a seamless 2-second loop, played rising in pitch while a spell charges. */
	public static final SoundEvent CHARGE_LOOP = register("charge_loop");
	/** A bright rising chime: the charge is full. */
	public static final SoundEvent CHARGE_FULL = register("charge_full");
	/** A rush of air and a soft pop: a charged spell leaves the hand. */
	public static final SoundEvent RELEASE = register("release");
	/** A magic circle unfolding: glass notes climbing the scale over a rising shimmer. */
	public static final SoundEvent CIRCLE_OPEN = register("circle_open");
	/** A beam firing: a punch and a blast of air into a bright, buzzing chord. */
	public static final SoundEvent BEAM_FIRE = register("beam_fire");
	/**
	 * An energy ball's hum. One second, shaped so that playing it again every 10 ticks overlaps into
	 * one steady, gently throbbing hum.
	 */
	public static final SoundEvent ORB_HUM = register("orb_hum");
	/** A rising tone that rings out as a dome of bells: a shield goes up. */
	public static final SoundEvent SHIELD_UP = register("shield_up");
	/** Glass shattering over a collapsing tone: a shield or barrier breaks. */
	public static final SoundEvent SHIELD_BREAK = register("shield_break");
	/** A spell ringing off a Shield: a glassy strike, a bright shimmering ring and a hiss turned aside. */
	public static final SoundEvent SHIELD_BLOCK = register("shield_block");
	/** Magic sinking into an item or a block: a shimmer drawn inward, sealed with a soft chord. */
	public static final SoundEvent IMBUE = register("imbue");
	/** A Domain opening: a swelling drone and wash that crest in a great chord of bells, with a long tail. Heard from 32 blocks. */
	public static final SoundEvent DOMAIN_OPEN = register("domain_open");
	/** A Domain falling in on itself: notes tumbling down, a deep thud and crumbling. Heard from 32 blocks. */
	public static final SoundEvent DOMAIN_CLOSE = register("domain_close");
	/** A blink: a reversed ping rushing in, a pop, and glitter where the caster lands. */
	public static final SoundEvent BLINK = register("blink");
	/** A block broken by magic: a stony crunch with a little shimmer. */
	public static final SoundEvent MAGIC_BREAK = register("magic_break");

	// ------------------------------------------------------------------ the interface

	/** A bead clicking onto the Cord and a small chime: a rune is threaded. */
	public static final SoundEvent RUNE_THREAD = register("rune_thread");
	/** A softer click and two falling notes: a rune comes off the Cord. */
	public static final SoundEvent RUNE_UNTHREAD = register("rune_unthread");
	/** A soft whoosh and bloom: the spell wheel opens. */
	public static final SoundEvent WHEEL_OPEN = register("wheel_open");
	/** A tiny tick as the pointer moves onto another spell in the wheel. */
	public static final SoundEvent WHEEL_HOVER = register("wheel_hover");
	/** A click and a two-note chime: a spell is chosen from the wheel. */
	public static final SoundEvent WHEEL_SELECT = register("wheel_select");
	/** A short fanfare of bells: something new in the Grimoire. */
	public static final SoundEvent DISCOVERY = register("discovery");

	// ------------------------------------------------------------------ the Fusion Altar

	/** A low amethyst hum under a soft chord: the Fusion Altar's screen opens. */
	public static final SoundEvent ALTAR_OPEN = register("altar_open");
	/** Three glass notes drawn together into a swelling chord that blooms into bells: runes fuse. */
	public static final SoundEvent ALTAR_FUSE = register("altar_fuse");
	/** A thread pulled tight, a knot cinching, and a warm two-note bell: a spell is tied into a Knot. */
	public static final SoundEvent ALTAR_KNOT = register("altar_knot");

	// ------------------------------------------------------------------ the heart

	/** A Heart Circle forming: a great bell, a swelling choir and a crown of chimes. */
	public static final SoundEvent CIRCLE_FORMED = register("circle_formed");
	/** Overcasting: a dissonant, straining tone that cracks. */
	public static final SoundEvent OVERCAST = register("overcast");

	/** The sound of casting a spell of {@code element}; Arcane for anything else (shapes, secrets, no element). */
	public static SoundEvent cast(String element) {
		return CASTS.getOrDefault(element, CASTS.get("arcane"));
	}

	/** The sound of a spell of {@code element} landing; Arcane for anything else. */
	public static SoundEvent impact(String element) {
		return IMPACTS.getOrDefault(element, IMPACTS.get("arcane"));
	}

	private static SoundEvent register(String path) {
		Identifier id = Wildercord.id(path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {}
}
