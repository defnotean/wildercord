package dev.wildercord.spell;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The twists a world's resonances are drawn with: what the world does to a spell when its exact runes are cast.
 * Each is a small piece of magic of its own, with its own look and sound (see {@code cast.TwistMagic}): some
 * change the spell they ride (its rain lands as glass, its fire mends friends), some are set pieces that go off
 * beside it (a pale horse, a storm of pages). Every one is a modest extra, priced into the resonance once found.
 *
 * <p>Pure data: what a twist is called in a resonance's name, the omen its riddle opens with, what the Grimoire
 * says once it's found, and which spells it can ride ({@link Fit}). The order is fixed: worlds draw from it.</p>
 */
public final class ResonanceTwists {
	private ResonanceTwists() {}

	/** Shapes that fly and land somewhere: the ones a twist that bursts where a spell lands can ride. */
	public static final Set<String> FLYING = Set.of("bolt", "spark", "comet", "arc", "wisp", "lance", "ricochet", "cluster", "glaive", "crescent",
		"orb");
	/** Shapes a helpful spell reaches friends with. */
	public static final Set<String> FRIENDLY = Set.of("self", "burst", "nova", "touch");
	/** Rain alone. */
	private static final Set<String> RAIN = Set.of("rain");
	private static final Set<String> SELF = Set.of("self");

	/**
	 * Which spells a twist can ride.
	 *
	 * @param shapes   the shapes it may have (by path), or empty for any castable shape but Self
	 * @param elements the element its first effect must have, or empty for any
	 * @param lead     the kind its first effect must be ({@code HARMFUL} or {@code HELPFUL})
	 */
	public record Fit(Set<String> shapes, Set<String> elements, EffectKind lead) {
		static Fit harmful(Set<String> shapes, String... elements) {
			return new Fit(shapes, Set.of(elements), EffectKind.HARMFUL);
		}

		static Fit helpful(Set<String> shapes) {
			return new Fit(shapes, Set.of(), EffectKind.HELPFUL);
		}

		public boolean allowsShape(String path) {
			return shapes.isEmpty() ? !path.equals("self") : shapes.contains(path);
		}

		public boolean allowsLead(RuneDef effect) {
			return effect.kind() == lead && (elements.isEmpty() || elements.contains(effect.element()));
		}
	}

	/**
	 * One twist.
	 *
	 * @param id          a stable id (the resonance records it)
	 * @param title       what the guide and the tests call it
	 * @param stems       words its resonances' names are made from ("Glass" in "the Glasswind Rite")
	 * @param omen        the clause its riddles carry, lower case ("the rain remembers it was glass")
	 * @param description what the Grimoire says it does, once found
	 * @param color       its colour
	 * @param fit         the spells it can ride
	 */
	public record Twist(String id, String title, List<String> stems, String omen, String description, int color, Fit fit) {}

	public static final Twist GLASS_RAIN = new Twist("glass_rain", "Glass Rain", List.of("Glass", "Shard"),
		"the rain remembers it was glass",
		"Every strike of the rain lands as falling glass: it shatters over whatever stands beneath, cutting and slowing it.",
		0xCFF4FF, Fit.harmful(RAIN));
	public static final Twist KINDLY_FLAME = new Twist("kindly_flame", "Kindly Flame", List.of("Hearth", "Warm"),
		"the fire forgets to hate your friends",
		"The flame warms your friends as it burns your foes: every ally near where it lands is mended.",
		0xFFB070, Fit.harmful(Set.of(), "fire"));
	public static final Twist LIGHT_BIRDS = new Twist("light_birds", "Birds of Light", List.of("Lark", "Wing", "Plume"),
		"its arrow breaks into wings",
		"Where it first strikes it bursts into birds of light, which wheel out and peck at the nearest foes.",
		0xFFF4C8, Fit.harmful(FLYING));
	public static final Twist WINTER_BLOSSOM = new Twist("winter_blossom", "Winter Blossom", List.of("Blossom", "Petal"),
		"winter flowers where it walks",
		"Where the frost lands, pale flowers bloom out of the cold, and allies standing among them mend.",
		0xB8E0FF, Fit.harmful(Set.of(), "frost"));
	public static final Twist UPFALL = new Twist("upfall", "Upfall", List.of("Upfall", "Skyward", "Hanging"),
		"the ground lets go",
		"Gravity turns over where it lands: foes nearby fall upward, hang a moment, then come crashing down.",
		0xB49CFF, Fit.harmful(Set.of()));
	public static final Twist PALE_STEED = new Twist("pale_steed", "Pale Steed", List.of("Steed", "Ghost", "Hoof"),
		"a rider is given what no bridle holds",
		"A spectral horse rises under you and carries you for half a minute, then fades.",
		0xD8F0FF, Fit.helpful(SELF));
	public static final Twist SECOND_VOICE = new Twist("second_voice", "Second Voice", List.of("Answer", "Twice", "Reprise"),
		"it speaks again where it fell silent",
		"A moment after it lands, the whole spell sounds again from where it landed, at half its strength.",
		0xF2D98A, Fit.harmful(Set.of()));
	public static final Twist SLOW_HOUR = new Twist("slow_hour", "Slow Hour", List.of("Stillwater", "Amber"),
		"arrows remember how to wait",
		"Time thickens around you: for a few seconds every arrow and thrown thing near you crawls through the air.",
		0xF2E2A0, Fit.helpful(FRIENDLY));
	public static final Twist PAPER_STORM = new Twist("paper_storm", "Paper Storm", List.of("Page", "Quill", "Vellum"),
		"the old words come loose from their book",
		"A storm of torn pages, each written with a rune, whirls around you: they cut the foes they catch and leave them lit.",
		0xF0E4C8, Fit.harmful(Set.of()));
	public static final Twist STAR_WAKE = new Twist("star_wake", "Star Wake", List.of("Star", "Wishing"),
		"small stars follow it down",
		"A few small stars fall around where it lands, each bursting on its own.",
		0xC8D4FF, Fit.harmful(FLYING));
	public static final Twist THORN_CROWN = new Twist("thorn_crown", "Crown of Thorns", List.of("Crown", "Wreath"),
		"every head it touches is crowned",
		"Each foe it strikes is crowned with thorns that bite for a few seconds and slow it.",
		0x9CD860, Fit.harmful(Set.of(), "life", "earth", "blood"));
	public static final Twist MIRROR_SHARDS = new Twist("mirror_shards", "Mirror Shards", List.of("Mirror", "Silver"),
		"three cold eyes keep watch beside you",
		"Three shards of mirror circle you for a while, and each strikes the next foe that comes close.",
		0xE4F0FF, Fit.helpful(FRIENDLY));
	public static final Twist SOUL_LANTERNS = new Twist("soul_lanterns", "Soul Lanterns", List.of("Lantern", "Ghostlight"),
		"what it ends lights a lamp for you",
		"A foe this spell ends gives up a little lantern of soul-light, which drifts to you and mends you.",
		0x8CE0FF, Fit.harmful(Set.of(), "void", "fire", "blood", "arcane"));
	public static final Twist AURORA = new Twist("aurora", "Aurora", List.of("Aurora", "Dawn"),
		"the sky hangs a curtain of colour over friends",
		"A ribbon of aurora unrolls overhead, and you and your allies beneath it are wrapped in a little shimmering protection.",
		0x9CFFD8, Fit.helpful(FRIENDLY));
	public static final Twist TURNING_TIDE = new Twist("turning_tide", "Turning Tide", List.of("Brine", "Surf"),
		"the sea comes looking for it",
		"Where it lands a ring of seawater bursts outward, soaking everything it reaches and shoving foes back.",
		0x70C8F0, Fit.harmful(Set.of(), "frost", "wind"));
	public static final Twist STANDING_STONES = new Twist("standing_stones", "Standing Stones", List.of("Menhir", "Cairn"),
		"old stones stand up to watch",
		"A ring of standing stones heaves out of the ground where it lands, battering the foes inside it.",
		0xC8A878, Fit.harmful(Set.of(), "earth"));
	public static final Twist STORM_CROWN = new Twist("storm_crown", "Storm Crown", List.of("Cloud", "Nimbus"),
		"a little cloud follows the struck",
		"Each foe it strikes is followed by a small thundercloud that zaps it a few times.",
		0xFFF07A, Fit.harmful(Set.of(), "storm"));
	public static final Twist RED_MOON = new Twist("red_moon", "Red Moon", List.of("Moon", "Rust"),
		"a red moon drinks for you",
		"A red moon hangs over the cast: each foe it strikes bleeds a drop of light back to you, mending you a little.",
		0xE04050, Fit.harmful(Set.of()));
	public static final Twist SUN_SEAL = new Twist("sun_seal", "Sun Seal", List.of("Sun", "Noon"),
		"noon is written on the ground",
		"A seal of the sun burns on the ground where it lands, scorching foes who stand in it.",
		0xFFD860, Fit.harmful(Set.of(), "fire", "arcane"));
	public static final Twist FALLING_BLADES = new Twist("falling_blades", "Falling Blades", List.of("Blade", "Edge"),
		"blades are hung over the struck",
		"Spectral blades form in the air above where it lands and fall on the nearest foes.",
		0xE8E8F8, Fit.harmful(Set.of()));
	public static final Twist EDDY = new Twist("eddy", "Whirling Eddy", List.of("Whirl", "Eddy"),
		"the wind turns on its heel",
		"A little whirlwind spins up where it lands, dragging foes round and flinging them out.",
		0xD8F8E8, Fit.harmful(Set.of(), "wind"));
	public static final Twist LANTERN_FLIES = new Twist("lantern_flies", "Lantern Flies", List.of("Firefly", "Glow"),
		"the dark is lit by small lives",
		"A cloud of fireflies spills out around you: your allies see in the dark, and hidden foes are lit.",
		0xE8FF90, Fit.helpful(FRIENDLY));
	public static final Twist TOLLING_HOUR = new Twist("tolling_hour", "Tolling Hour", List.of("Toll", "Knell"),
		"a bell counts three over the struck",
		"A clock face opens over each foe it strikes; three seconds later it tolls, and the foe takes a blow.",
		0xF0D070, Fit.harmful(Set.of()));
	public static final Twist RIME_STEPS = new Twist("rime_steps", "Rime Steps", List.of("Frostfoot", "Footfall"),
		"your footsteps keep the cold",
		"For a few seconds your footsteps leave rime flowers behind you, and foes who tread on them are slowed.",
		0xC8F0FF, Fit.harmful(Set.of(), "frost"));
	public static final Twist CRACKLING_WAKE = new Twist("crackling_wake", "Crackling Wake", List.of("Crackle", "Wake"),
		"it drags a crackling tail behind it",
		"Where it lands, sparks run back along the way it flew, bursting on any foe beside its path.",
		0xFFE0A0, Fit.harmful(FLYING));
	public static final Twist THICKET = new Twist("thicket", "Sudden Thicket", List.of("Thicket", "Seedling"),
		"seeds wake where it falls",
		"It scatters seeds where it lands, and a thicket bursts up to hold the foes nearby fast for a moment.",
		0x70C050, Fit.harmful(Set.of(), "life", "earth"));
	public static final Twist GREAT_BELL = new Twist("great_bell", "Great Bell", List.of("Bell", "Peal"),
		"a bell no one hung rings out",
		"A great bell rings out over you: foes nearby reel, and your allies are quickened.",
		0xE8D8A8, Fit.helpful(FRIENDLY));

	/** Every twist, in the order worlds draw from. Add new ones at the end. */
	public static final List<Twist> ALL = List.of(GLASS_RAIN, KINDLY_FLAME, LIGHT_BIRDS, WINTER_BLOSSOM, UPFALL, PALE_STEED, SECOND_VOICE,
		SLOW_HOUR, PAPER_STORM, STAR_WAKE, THORN_CROWN, MIRROR_SHARDS, SOUL_LANTERNS, AURORA, TURNING_TIDE, STANDING_STONES, STORM_CROWN,
		RED_MOON, SUN_SEAL, FALLING_BLADES, EDDY, LANTERN_FLIES, TOLLING_HOUR, RIME_STEPS, CRACKLING_WAKE, THICKET, GREAT_BELL);

	public static Optional<Twist> byId(String id) {
		for (Twist twist : ALL) {
			if (twist.id().equals(id)) {
				return Optional.of(twist);
			}
		}
		return Optional.empty();
	}
}
