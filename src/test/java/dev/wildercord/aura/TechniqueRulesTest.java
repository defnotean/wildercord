package dev.wildercord.aura;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Techniques of one's own, the pure parts: the parts and where each is found, what every combination of them does and is worth against
 * the fifty arts (inside the arts' own band at every rank, temper, edge, method and string), the strings a technique may be given and what
 * they meet, its name made safe, its slots, its ranks and the experience that earns them, an add-on's intent, and a swordsman's book of
 * them kept and sent whole.
 */
class TechniqueRulesTest {
	private static final String[] STRINGS = {"swing swing leap", "run low", "full full low", "full leap low", "step run low leap low"};

	/** Every resolved technique there is: every part together, every method, every rank, temper and edge. */
	private static List<TechniqueRules.Profile> everyProfile(int rank) {
		List<TechniqueRules.Profile> out = new ArrayList<>();
		for (TechniqueRules.Stroke s : TechniqueRules.STROKES) {
			for (TechniqueRules.Release r : TechniqueRules.RELEASES) {
				for (TechniqueRules.Intent in : TechniqueRules.BUILT_IN_INTENTS) {
					for (BreathingMethod.Flavour f : BreathingMethod.Flavour.values()) {
						for (String t : new String[] {"", TechniqueRules.SWIFT, TechniqueRules.HEAVY}) {
							for (String e : new String[] {"", TechniqueRules.LONG, TechniqueRules.BROAD}) {
								out.add(TechniqueRules.profile(s.id(), r.id(), in.id(), f, rank, t, e));
							}
						}
					}
				}
			}
		}
		return out;
	}

	/** The arts' band: their worth for their price and for their rest, least and most, across the First to the Fourth Art. */
	private static double[] band() {
		double minA = Double.MAX_VALUE;
		double maxA = 0;
		double minR = Double.MAX_VALUE;
		double maxR = 0;
		for (ArtRules.Art art : ArtRules.ARTS) {
			if (art.slot() > 3) {
				continue;
			}
			double w = ArtRules.power(art);
			minA = Math.min(minA, w / art.cost());
			maxA = Math.max(maxA, w / art.cost());
			minR = Math.min(minR, w / art.cooldown());
			maxR = Math.max(maxR, w / art.cooldown());
		}
		return new double[] {minA, maxA, minR, maxR};
	}

	// ------------------------------------------------------------------ the parts

	@Test
	void everyPartIsOneOfItsKindWithAWordForIt() throws IOException {
		assertEquals(List.of("thrust", "rising_cut", "falling_cut", "sweep", "spin", "draw"), TechniqueRules.parts(TechniqueRules.Family.STROKE));
		assertEquals(List.of("on_the_blade", "wave", "burst", "afterimage"), TechniqueRules.parts(TechniqueRules.Family.RELEASE));
		assertEquals(List.of("pierce", "sunder", "bind", "echo", "ward", "rally", "infuse"), TechniqueRules.BUILT_IN_INTENTS.stream()
			.map(TechniqueRules.Intent::id).toList());
		Set<String> ids = new HashSet<>();
		JsonObject lang = lang();
		for (String part : TechniqueRules.allParts()) {
			if (part.contains(":")) {
				continue;
			}
			assertTrue(ids.add(part), part + " twice");
			TechniqueRules.Family family = TechniqueRules.family(part).orElseThrow();
			assertTrue(TechniqueRules.is(part, family));
			assertTrue(lang.has(TechniqueRules.nameKey(part)), part + " has a name");
			assertTrue(lang.has(TechniqueRules.nameKey(part) + ".desc"), part + " says what it does");
			assertTrue(Files.exists(Path.of("src/main/resources/assets/wildercord/textures/gui/sprites/technique/" + part + ".png")), part + " has a glyph");
		}
		assertTrue(TechniqueRules.family("no_such_part").isEmpty());
		assertTrue(Files.exists(Path.of("src/main/resources/assets/wildercord/textures/gui/sprites/technique/unknown.png")));
		// The art generator lists them in the same order (tools/technique_art.py's PARTS).
		String art = Files.readString(Path.of("tools/technique_art.py"));
		for (String part : TechniqueRules.allParts()) {
			if (!part.contains(":")) {
				assertTrue(art.contains("\"" + part + "\""), part + " drawn by tools/technique_art.py");
			}
		}
		for (String rank : TechniqueRules.RANKS) {
			assertTrue(lang.has("aura.wildercord.technique_rank." + rank), rank + " has a name");
		}
	}

	private static JsonObject lang() throws IOException {
		try (InputStream in = TechniqueRulesTest.class.getResourceAsStream("/assets/wildercord/lang/en_us.json")) {
			if (in != null) {
				return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
			}
		}
		return JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/wildercord/lang/en_us.json"))).getAsJsonObject();
	}

	@Test
	void eachStrokeIsItsOwnShape() {
		TechniqueRules.Profile thrust = TechniqueRules.profile(TechniqueRules.THRUST, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE,
			BreathingMethod.Flavour.NONE, 3, "", "");
		TechniqueRules.Profile sweep = TechniqueRules.profile(TechniqueRules.SWEEP, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE,
			BreathingMethod.Flavour.NONE, 3, "", "");
		TechniqueRules.Profile spin = TechniqueRules.profile(TechniqueRules.SPIN, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE,
			BreathingMethod.Flavour.NONE, 3, "", "");
		TechniqueRules.Profile falling = TechniqueRules.profile(TechniqueRules.FALLING, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE,
			BreathingMethod.Flavour.NONE, 3, "", "");
		TechniqueRules.Profile rising = TechniqueRules.profile(TechniqueRules.RISING, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE,
			BreathingMethod.Flavour.NONE, 3, "", "");
		TechniqueRules.Profile draw = TechniqueRules.profile(TechniqueRules.DRAW, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE,
			BreathingMethod.Flavour.NONE, 3, "", "");
		assertEquals(TechniqueRules.Shape.LINE, thrust.shape());
		assertEquals(TechniqueRules.Shape.RING, spin.shape());
		assertTrue(thrust.reach() > sweep.reach() && thrust.reach() > spin.reach(), "the thrust reaches furthest");
		assertTrue(sweep.width() > rising.width() && rising.width() > falling.width(), "the sweep widest, the falling cut narrowest");
		assertTrue(spin.targets() > sweep.targets() && sweep.targets() > falling.targets(), "the spin strikes the most foes");
		assertTrue(falling.factor() > thrust.factor() && falling.stance() > 1, "the falling cut heaviest, and hard on a stance");
		assertTrue(rising.lift() > 0 && thrust.lift() == 0, "only the rising cut throws foes up");
		assertTrue(TechniqueRules.worth(draw) < TechniqueRules.worth(thrust) && TechniqueRules.worth(draw) < TechniqueRules.worth(falling),
			"the draw is the lightest stroke");
	}

	@Test
	void eachReleaseCarriesTheStrokeItsOwnWay() {
		BreathingMethod.Flavour f = BreathingMethod.Flavour.NONE;
		TechniqueRules.Profile blade = TechniqueRules.profile(TechniqueRules.SWEEP, TechniqueRules.ON_BLADE, TechniqueRules.SUNDER, f, 3, "", "");
		TechniqueRules.Profile wave = TechniqueRules.profile(TechniqueRules.SWEEP, TechniqueRules.WAVE, TechniqueRules.SUNDER, f, 3, "", "");
		TechniqueRules.Profile burst = TechniqueRules.profile(TechniqueRules.SWEEP, TechniqueRules.BURST, TechniqueRules.SUNDER, f, 3, "", "");
		TechniqueRules.Profile after = TechniqueRules.profile(TechniqueRules.SWEEP, TechniqueRules.AFTERIMAGE, TechniqueRules.SUNDER, f, 3, "", "");
		TechniqueRules.Profile spinWave = TechniqueRules.profile(TechniqueRules.SPIN, TechniqueRules.WAVE, TechniqueRules.SUNDER, f, 3, "", "");
		assertEquals(TechniqueRules.WAVE_LENGTH, wave.flight(), 1e-9, "a wave flies on past its reach");
		assertEquals(TechniqueRules.WAVE_RING, spinWave.flight(), 1e-9, "a spin's wave is a ring racing out");
		assertTrue(wave.factor() < blade.factor() && wave.total() > blade.total() + 6, "a wave is lighter, and reaches far further");
		assertEquals(TechniqueRules.Shape.RING, burst.shape(), "a burst breaks out all round");
		assertTrue(burst.factor() < blade.factor() && burst.fair() > blade.fair(), "lighter, nobody near left out");
		assertTrue(after.later() > 0 && after.laterDelay() > 0, "an afterimage strikes again later");
		assertTrue(after.factor() < blade.factor(), "and the stroke now is lighter for it");
		assertTrue(after.factor() + after.later() > blade.factor(), "both together more than the blade alone (if the foe is still there)");
	}

	@Test
	void eachIntentDoesItsOwnThing() {
		BreathingMethod.Flavour f = BreathingMethod.Flavour.NONE;
		TechniqueRules.Profile infuse = TechniqueRules.profile(TechniqueRules.THRUST, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE, f, 3, "", "");
		TechniqueRules.Profile pierce = TechniqueRules.profile(TechniqueRules.THRUST, TechniqueRules.ON_BLADE, TechniqueRules.PIERCE, f, 3, "", "");
		TechniqueRules.Profile sunder = TechniqueRules.profile(TechniqueRules.THRUST, TechniqueRules.ON_BLADE, TechniqueRules.SUNDER, f, 3, "", "");
		TechniqueRules.Profile bind = TechniqueRules.profile(TechniqueRules.THRUST, TechniqueRules.ON_BLADE, TechniqueRules.BIND, f, 3, "", "");
		TechniqueRules.Profile echo = TechniqueRules.profile(TechniqueRules.THRUST, TechniqueRules.ON_BLADE, TechniqueRules.ECHO, f, 3, "", "");
		TechniqueRules.Profile ward = TechniqueRules.profile(TechniqueRules.THRUST, TechniqueRules.ON_BLADE, TechniqueRules.WARD, f, 3, "", "");
		TechniqueRules.Profile rally = TechniqueRules.profile(TechniqueRules.THRUST, TechniqueRules.ON_BLADE, TechniqueRules.RALLY, f, 3, "", "");
		assertTrue(pierce.targets() > infuse.targets() && pierce.total() > infuse.total(), "pierce: more foes, further");
		assertTrue(sunder.stance() >= 2.5, "sunder: a stance worn two and a half times as hard");
		assertTrue(bind.bind() > 0 && infuse.bind() == 0, "bind roots");
		assertTrue(echo.echo() > 0 && infuse.echo() == 0, "echo strikes again");
		assertTrue(ward.ward() > 0 && ward.wardTicks() > 0, "ward steadies its swordsman");
		assertTrue(rally.rally() > 0 && rally.rallyRadius() > 0 && rally.rallyMomentum() > 0, "rally steadies allies and builds their momentum");
		assertTrue(infuse.flavourScale() >= 2.0 && pierce.flavourScale() == 1.0, "infuse: the element twice over");
		for (TechniqueRules.Intent in : TechniqueRules.BUILT_IN_INTENTS) {
			assertTrue(in.factor() < 1.0, in.id() + " costs a little of the blow");
		}
	}

	@Test
	void everyMethodsElementIsWorthAboutTheSame() {
		for (TechniqueRules.Stroke s : TechniqueRules.STROKES) {
			double[] worths = new double[BreathingMethod.Flavour.values().length];
			int i = 0;
			for (BreathingMethod.Flavour f : BreathingMethod.Flavour.values()) {
				worths[i++] = TechniqueRules.worth(TechniqueRules.profile(s.id(), TechniqueRules.ON_BLADE, TechniqueRules.PIERCE, f, 3, "", ""));
			}
			double lo = java.util.Arrays.stream(worths).min().orElseThrow();
			double hi = java.util.Arrays.stream(worths).max().orElseThrow();
			assertTrue(hi / lo < 1.08, s.id() + ": every method's element within a twelfth of each other (" + lo + " to " + hi + ")");
		}
		// And each one is its own: a different field of the flavour does the work.
		Set<String> kinds = new HashSet<>();
		for (BreathingMethod.Flavour f : BreathingMethod.Flavour.values()) {
			TechniqueRules.Flavour fl = TechniqueRules.flavour(f);
			String kind = fl.ignite() > 0 ? "ignite" : fl.chill() > 0 ? "chill" : fl.spark() > 0 ? "spark" : fl.knock() > 0 ? "knock"
				: fl.stance() > 1 ? "stance" : fl.mend() > 0 ? "mend" : fl.pull() > 0 ? "pull" : fl.aura() > 0 ? "aura" : fl.echo() > 0 ? "echo"
				: fl.bleed() > 0 ? "bleed" : "plain";
			assertTrue(kinds.add(kind), f + " has its own element (" + kind + ")");
		}
	}

	// ------------------------------------------------------------------ worth and price, against the arts

	@Test
	void everyCombinationIsWorthAboutAnArtOfTheFirstFour() {
		double finalArt = ArtRules.SLOT_POWER[4] * (1 - ArtRules.POWER_SPREAD);
		for (int rank = TechniqueRules.RAW; rank <= TechniqueRules.PEERLESS; rank++) {
			for (TechniqueRules.Profile p : everyProfile(rank)) {
				double w = TechniqueRules.worth(p);
				assertTrue(w >= 0.75 && w <= 2.7, p.stroke().id() + "/" + p.release().id() + "/" + p.intent().id() + "/" + p.flavour().of() + " is worth " + w);
				assertTrue(w < finalArt * 0.7, "never near the Final Art (" + w + ")");
			}
		}
		// As written (no temper, no edge), between a First Art and a Fourth.
		for (TechniqueRules.Profile p : everyProfile(TechniqueRules.TEMPERED)) {
			if (p.temper().isEmpty() && p.edge().isEmpty()) {
				double w = TechniqueRules.worth(p);
				assertTrue(w >= ArtRules.SLOT_POWER[0] * 0.65 && w <= ArtRules.SLOT_POWER[3] * 1.05,
					p.stroke().id() + "/" + p.release().id() + "/" + p.intent().id() + " as written is worth " + w);
			}
		}
	}

	@Test
	void everyTechniqueIsPricedInsideTheArtsOwnBand() {
		double[] band = band();
		for (int rank = TechniqueRules.RAW; rank <= TechniqueRules.PEERLESS; rank++) {
			for (TechniqueRules.Profile p : everyProfile(rank)) {
				double priced = TechniqueRules.pricedWorth(p.stroke().id(), p.release().id(), p.intent().id(), p.flavour().of(), p.temper(), p.edge());
				double w = TechniqueRules.worth(p);
				for (String text : STRINGS) {
					SwordString s = SwordString.parse(text);
					double perAura = w / TechniqueRules.cost(priced, s);
					double perTick = w / TechniqueRules.rest(priced, s);
					String id = p.stroke().id() + "/" + p.release().id() + "/" + p.intent().id() + "/" + p.flavour().of() + "/" + p.temper() + "/" + p.edge()
						+ "/" + text + " at " + TechniqueRules.rankId(rank);
					assertTrue(perAura >= band[0] && perAura <= band[1], id + ": " + perAura + " W an aura, outside the arts' " + band[0] + " to " + band[1]);
					assertTrue(perTick >= band[2] && perTick <= band[3], id + ": " + perTick + " W a tick of rest, outside the arts' " + band[2] + " to "
						+ band[3]);
				}
			}
		}
	}

	@Test
	void aTemperedTechniqueIsPricedAtTheArtsOwnRate() {
		double arts = 0;
		int n = 0;
		for (ArtRules.Art art : ArtRules.ARTS) {
			if (art.slot() <= 3) {
				arts += ArtRules.power(art) / art.cost();
				n++;
			}
		}
		arts /= n;
		double techniques = 0;
		int m = 0;
		for (TechniqueRules.Profile p : everyProfile(TechniqueRules.TEMPERED)) {
			techniques += 1 / TechniqueRules.AURA_PER_W;
			m++;
		}
		techniques /= m;
		assertEquals(arts, techniques, arts * 0.04, "a Tempered technique on an even string costs what the arts' average does for its worth");
		assertEquals(TechniqueRules.AURA_PER_W * 10, TechniqueRules.TICKS_PER_W, 1.0, "rested in step with its price, as the arts are (a slot's 6 aura, 60 ticks)");
	}

	@Test
	void ranksSharpenALittleEitherWayOfTempered() {
		for (TechniqueRules.Profile p : everyProfile(TechniqueRules.TEMPERED)) {
			double tempered = TechniqueRules.worth(p);
			double prev = 0;
			for (int rank = TechniqueRules.RAW; rank <= TechniqueRules.PEERLESS; rank++) {
				double w = TechniqueRules.worth(TechniqueRules.profile(p.stroke().id(), p.release().id(), p.intent().id(), p.flavour().of(), rank, p.temper(),
					p.edge()));
				assertTrue(w >= prev - 1e-9, "a rank never makes it weaker");
				prev = w;
			}
			double raw = TechniqueRules.worth(TechniqueRules.profile(p.stroke().id(), p.release().id(), p.intent().id(), p.flavour().of(), TechniqueRules.RAW,
				p.temper(), p.edge()));
			assertTrue(raw / tempered >= 0.92, "a Raw technique is no more than a twelfth weaker than its price (" + raw / tempered + ")");
			assertTrue(prev / tempered <= 1.06, "a Peerless one no more than a sixteenth stronger (" + prev / tempered + ")");
		}
	}

	@Test
	void tempersAndEdgesTradeOneStrengthForAnother() {
		BreathingMethod.Flavour f = BreathingMethod.Flavour.IGNITE;
		for (TechniqueRules.Stroke s : TechniqueRules.STROKES) {
			String stroke = s.id();
			double plain = TechniqueRules.pricedWorth(stroke, TechniqueRules.ON_BLADE, TechniqueRules.BIND, f, "", "");
			double swift = TechniqueRules.pricedWorth(stroke, TechniqueRules.ON_BLADE, TechniqueRules.BIND, f, TechniqueRules.SWIFT, "");
			double heavy = TechniqueRules.pricedWorth(stroke, TechniqueRules.ON_BLADE, TechniqueRules.BIND, f, TechniqueRules.HEAVY, "");
			assertTrue(swift < plain && plain < heavy, stroke + ": a swift temper lighter and cheaper, a heavy one harder and dearer");
			SwordString string = SwordString.parse("full full low");
			assertTrue(TechniqueRules.cost(swift, string) < TechniqueRules.cost(plain, string) && TechniqueRules.rest(heavy, string) > TechniqueRules.rest(plain, string),
				"its price and rest follow");
			TechniqueRules.Profile written = TechniqueRules.profile(stroke, TechniqueRules.WAVE, TechniqueRules.BIND, f, 4, "", "");
			TechniqueRules.Profile longer = TechniqueRules.profile(stroke, TechniqueRules.WAVE, TechniqueRules.BIND, f, 4, "", TechniqueRules.LONG);
			TechniqueRules.Profile broad = TechniqueRules.profile(stroke, TechniqueRules.WAVE, TechniqueRules.BIND, f, 4, "", TechniqueRules.BROAD);
			assertTrue(longer.total() > written.total() && longer.factor() < written.factor(), stroke + ": a long edge reaches further, a little lighter");
			assertTrue(broad.targets() > written.targets() && broad.factor() < written.factor(), stroke + ": a broad edge two foes more, a little lighter");
		}
		assertTrue(TechniqueRules.validChoice(false, TechniqueRules.SWIFT) && TechniqueRules.validChoice(true, TechniqueRules.BROAD));
		assertTrue(TechniqueRules.validChoice(false, "") && TechniqueRules.validChoice(true, ""), "back as written");
		assertFalse(TechniqueRules.validChoice(false, TechniqueRules.LONG), "an edge isn't a temper");
		assertFalse(TechniqueRules.validChoice(true, "sharp"));
		assertEquals(TechniqueRules.HONED, TechniqueRules.TEMPER_RANK);
		assertEquals(TechniqueRules.KEEN, TechniqueRules.EDGE_RANK);
	}

	@Test
	void theStringsEffortMovesThePriceALittle() {
		double easy = TechniqueRules.effort(SwordString.parse("swing swing leap"));
		double hard = TechniqueRules.effort(SwordString.parse("step run low leap low"));
		assertTrue(easy > 1 && hard < 1, "an easy string a little dearer, a demanding one a little cheaper");
		assertTrue(easy <= TechniqueRules.EFFORT_MOST + 1e-9 && hard >= TechniqueRules.EFFORT_LEAST - 1e-9, "never more than a fiftieth either way");
		assertEquals(5.0, TechniqueRules.effortOf(SwordString.parse("full full low")), 1e-9);
		assertEquals(4.5, TechniqueRules.effortOf(SwordString.parse("run low")), 1e-9);
		double priced = 1.6;
		assertTrue(TechniqueRules.cost(priced, SwordString.parse("swing swing leap")) > TechniqueRules.cost(priced, SwordString.parse("step run low leap low")));
	}

	// ------------------------------------------------------------------ strings

	@Test
	void aStringMustAskSomethingOfTheHand() {
		assertTrue(TechniqueRules.problem(SwordString.parse("full low")).isEmpty());
		assertTrue(TechniqueRules.problem(SwordString.parse("full full low")).isEmpty());
		assertTrue(TechniqueRules.problem(SwordString.parse("counter low")).isEmpty());
		assertEquals(TechniqueRules.StringProblem.TOO_SHORT, TechniqueRules.problem(SwordString.parse("low")).orElseThrow());
		assertEquals(TechniqueRules.StringProblem.TOO_LONG, TechniqueRules.problem(SwordString.parse("swing swing swing swing swing low")).orElseThrow());
		assertEquals(TechniqueRules.StringProblem.TOO_PLAIN, TechniqueRules.problem(SwordString.parse("swing low")).orElseThrow());
		assertEquals(TechniqueRules.StringProblem.NO_MARK, TechniqueRules.problem(SwordString.parse("full full full")).orElseThrow(),
			"full swings alone would go off in any fight");
		// None of the five art strings could be a technique's (each is an art's own already, or too plain by itself).
		for (dev.wildercord.api.AuraApi.ArtSlot slot : dev.wildercord.api.AuraApi.ArtSlot.values()) {
			assertTrue(TechniqueRules.clash(slot.string, slot.string));
		}
	}

	@Test
	void stringsThatGetInEachOthersWayAreCaught() {
		SwordString first = SwordString.parse("swing swing low");
		assertTrue(TechniqueRules.clash(first, SwordString.parse("swing swing low")), "the same swings");
		assertTrue(TechniqueRules.clash(SwordString.parse("swing swing low leap"), first), "cut short by the First Art");
		assertTrue(TechniqueRules.clash(SwordString.parse("counter low"), SwordString.parse("counter")), "cut short by the Third");
		assertFalse(TechniqueRules.clash(SwordString.parse("full full low"), first), "full swings and a low one: not the First Art's");
		assertFalse(TechniqueRules.clash(SwordString.parse("full full low"), SwordString.parse("full full full low")), "nor the Final Art's");
		// Overlap: the same swings could finish both; the reader plays the one that asks more.
		assertTrue(TechniqueRules.overlap(SwordString.parse("full full low"), first));
		assertFalse(TechniqueRules.overlap(SwordString.parse("full run"), SwordString.parse("full low")), "a low swing can't be a running one");
		assertTrue(StringReader.compare(spelled("full full low", AuraRules.EDGE), spelled("swing swing low", AuraRules.GLOW)) > 0,
			"the technique asks more than the First Art, so it goes first when its swings fit both");
		assertTrue(StringReader.compare(spelled("full full full low", AuraRules.SOVEREIGN), spelled("full full low", AuraRules.EDGE)) > 0,
			"the Final Art asks more than it");
		// A counter the technique doesn't ask for is never on the way: the page leaves it out of its warning.
		assertTrue(TechniqueRules.overlap(SwordString.parse("full full low"), SwordString.parse("counter")), "a low swing can be a counter");
		assertTrue(TechniqueRules.incidentalCue(SwordString.parse("full full low"), SwordString.parse("counter")));
		assertFalse(TechniqueRules.incidentalCue(SwordString.parse("counter low"), SwordString.parse("swing low")));
		assertFalse(TechniqueRules.incidentalCue(SwordString.parse("full full low"), first));
		assertTrue(TechniqueRules.together(SwordString.Token.LEAP, SwordString.Token.LOW));
		assertFalse(TechniqueRules.together(SwordString.Token.RUN, SwordString.Token.LOW));
	}

	private static StringReader.Spelled spelled(String text, int stage) {
		SwordString s = SwordString.parse(text);
		return new StringReader.Spelled() {
			@Override
			public SwordString string() {
				return s;
			}

			@Override
			public int stage() {
				return stage;
			}
		};
	}

	// ------------------------------------------------------------------ names

	@Test
	void aNameIsMadeSafeToShowAnyone() {
		assertEquals("Ember Fang", TechniqueRules.cleanName("Ember Fang"));
		assertEquals("Red Fang", TechniqueRules.cleanName("§cRed §lFang"), "formatting codes out, letter and all");
		assertEquals("Fang", TechniqueRules.cleanName("‮Fang​"), "nothing that turns text round or hides");
		assertEquals("A B", TechniqueRules.cleanName("A\n\t  B\u0000"), "control characters out, a run of spaces one space");
		assertEquals("Trim", TechniqueRules.cleanName("   Trim   "));
		assertEquals("", TechniqueRules.cleanName(null));
		assertEquals("", TechniqueRules.cleanName("​‎"));
		String longName = "A very long name for a technique of one's own";
		assertTrue(TechniqueRules.cleanName(longName).codePointCount(0, TechniqueRules.cleanName(longName).length()) <= TechniqueRules.MAX_NAME,
			"held to its length");
		String moon = "🌙".repeat(30);
		String kept = TechniqueRules.cleanName(moon);
		assertEquals(TechniqueRules.MAX_NAME, kept.codePointCount(0, kept.length()), "a letter outside the basic plane counts once, never split");
		assertFalse(TechniqueRules.cleanName("Split\uD83C").contains("\uD83C"), "a lone half of a pair dropped");
		assertEquals(TechniqueRules.cleanName("x".repeat(40)).length(), TechniqueRules.MAX_NAME);
	}

	@Test
	void aTechniqueLeftUnnamedIsNamedByItsParts() {
		assertEquals("Sundering Thrust Wave", TechniqueRules.autoName(TechniqueRules.THRUST, TechniqueRules.WAVE, TechniqueRules.SUNDER,
			BreathingMethod.Flavour.IGNITE));
		assertEquals("Blazing Draw", TechniqueRules.autoName(TechniqueRules.DRAW, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE, BreathingMethod.Flavour.IGNITE));
		assertEquals("Frozen Spin Burst", TechniqueRules.autoName(TechniqueRules.SPIN, TechniqueRules.BURST, TechniqueRules.INFUSE,
			BreathingMethod.Flavour.CHILL));
		for (TechniqueRules.Stroke s : TechniqueRules.STROKES) {
			for (TechniqueRules.Release r : TechniqueRules.RELEASES) {
				for (TechniqueRules.Intent in : TechniqueRules.BUILT_IN_INTENTS) {
					for (BreathingMethod.Flavour f : BreathingMethod.Flavour.values()) {
						String name = TechniqueRules.autoName(s.id(), r.id(), in.id(), f);
						assertFalse(name.isBlank());
						assertEquals(name, TechniqueRules.cleanName(name), "a default name is already safe and fits");
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------ slots and ranks

	@Test
	void slotsOpenOneAStageFromEdge() {
		assertEquals(0, TechniqueRules.slots(AuraRules.GLOW));
		assertEquals(0, TechniqueRules.slots(AuraRules.FLOW));
		assertEquals(1, TechniqueRules.slots(AuraRules.EDGE));
		assertEquals(2, TechniqueRules.slots(AuraRules.FORM));
		assertEquals(3, TechniqueRules.slots(AuraRules.SOVEREIGN));
		assertEquals(0, TechniqueRules.slots(AuraRules.NONE));
		for (int i = 0; i < TechniqueRules.MAX_SLOTS; i++) {
			assertEquals(i, TechniqueRules.slotOf(TechniqueRules.artId(i)));
			assertEquals(i + 1, TechniqueRules.slots(TechniqueRules.slotStage(i)), "slot " + i + " opens at its stage");
		}
		assertEquals(-1, TechniqueRules.slotOf("kindling_draw"));
		assertEquals(-1, TechniqueRules.slotOf("technique_9"));
		assertEquals(-1, TechniqueRules.slotOf("technique_x"));
		assertEquals(-1, TechniqueRules.slotOf(null));
		assertEquals("thrust/wave/sunder", TechniqueRules.key(TechniqueRules.THRUST, TechniqueRules.WAVE, TechniqueRules.SUNDER));
	}

	@Test
	void ranksAreEarnedAsSpellsGrow() {
		assertEquals(TechniqueRules.RAW, TechniqueRules.rank(0));
		assertEquals(TechniqueRules.RAW, TechniqueRules.rank(TechniqueRules.threshold(TechniqueRules.HONED) - 0.01));
		for (int rank = TechniqueRules.HONED; rank <= TechniqueRules.PEERLESS; rank++) {
			assertEquals(rank, TechniqueRules.rank(TechniqueRules.threshold(rank)));
			assertTrue(TechniqueRules.threshold(rank) > TechniqueRules.threshold(rank - 1));
		}
		assertEquals(1.0, TechniqueRules.progress(1e6), 1e-9);
		assertEquals(0.5, TechniqueRules.progress((TechniqueRules.threshold(2) + TechniqueRules.threshold(3)) / 2.0), 1e-9);
		// A strike on a monster, harder for a bigger share and a kill; nothing for what isn't a foe.
		assertEquals(TechniqueRules.STRIKE, TechniqueRules.strike(1, 0, false), 1e-9);
		assertTrue(TechniqueRules.strike(1, 0.5, true) > TechniqueRules.strike(1, 0.5, false));
		assertTrue(TechniqueRules.strike(2, 0.2, false) > TechniqueRules.strike(1, 0.2, false), "a boss is worth more");
		assertEquals(0, TechniqueRules.strike(0, 1, true), 1e-9);
		// The same thing in the same place earns less and less, never nothing.
		assertTrue(TechniqueRules.repetition(0) > TechniqueRules.repetition(10));
		assertTrue(TechniqueRules.repetition(1e6) >= dev.wildercord.spell.MasteryRules.REPEAT_FLOOR);
		// Practice: half, to a cap short of Honed.
		assertEquals(TechniqueRules.PRACTICE_RATE * 10, TechniqueRules.practice(0, 10), 1e-9);
		assertEquals(0, TechniqueRules.practice(TechniqueRules.PRACTICE_CAP, 10), 1e-9);
		assertTrue(TechniqueRules.PRACTICE_CAP < TechniqueRules.threshold(TechniqueRules.HONED), "dummies alone never hone a technique");
		// About twenty minutes of a fight's use to Honed, two hours to Keen (about four meaningful strikes a minute, worth about 2.5 each).
		double perMinute = 4 * 2.5;
		assertTrue(TechniqueRules.threshold(TechniqueRules.HONED) / perMinute <= 10, "Honed comes quickly");
		assertTrue(TechniqueRules.threshold(TechniqueRules.KEEN) / perMinute >= 45, "Keen takes a while");
		assertTrue(TechniqueRules.threshold(TechniqueRules.PEERLESS) / perMinute >= 120, "Peerless takes long");
	}

	@Test
	void aTechniquesMomentumCountsAsTheArtItsWorthIsNearest() {
		assertEquals(0, TechniqueRules.momentumSlot(1.2));
		assertEquals(1, TechniqueRules.momentumSlot(1.8));
		assertEquals(2, TechniqueRules.momentumSlot(1.95));
		assertEquals(3, TechniqueRules.momentumSlot(2.6));
		assertEquals(0, TechniqueRules.momentumSlot(0.5));
	}

	// ------------------------------------------------------------------ where parts come from

	@Test
	void everyPartComesFromSomewhere() {
		assertEquals(List.of(TechniqueRules.DRAW, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE), TechniqueRules.INNATE,
			"a stroke, a release and an intent every blade knows, so a technique can be written at the Edge breakthrough");
		assertEquals(Set.copyOf(WayRules.BUILT_IN), TechniqueRules.WAY_PARTS.keySet(), "every built-in Way lends a part");
		for (Map.Entry<String, String> e : TechniqueRules.WAY_PARTS.entrySet()) {
			assertEquals(WayRules.node(e.getKey(), AuraRules.EDGE), TechniqueRules.lendingNode(e.getValue()).orElseThrow(), "lent from its Edge node");
			assertEquals(e.getKey(), TechniqueRules.lendingWay(e.getValue()));
		}
		List<String> scroll = TechniqueRules.scrollParts();
		assertEquals(12, scroll.size(), scroll.toString());
		for (String innate : TechniqueRules.INNATE) {
			assertFalse(scroll.contains(innate), innate + " is never on a scroll");
		}
		for (String own : TechniqueRules.WAY_ONLY) {
			assertFalse(scroll.contains(own), own + " is its Way's alone");
		}
		assertTrue(scroll.contains(TechniqueRules.PIERCE) && scroll.contains(TechniqueRules.AFTERIMAGE), "a Way's part that a scroll can carry too");
		for (String part : TechniqueRules.allParts()) {
			if (part.contains(":")) {
				continue;
			}
			boolean somewhere = TechniqueRules.INNATE.contains(part) || TechniqueRules.WAY_PARTS.containsValue(part) || scroll.contains(part);
			assertTrue(somewhere, part + " comes from somewhere");
		}
		for (BreathingMethod.Flavour f : BreathingMethod.Flavour.values()) {
			Map<String, Integer> weights = TechniqueRules.duelistWeights(f);
			assertEquals(Set.copyOf(scroll), weights.keySet(), f + ": a duelist shows only what a scroll could carry");
			assertTrue(weights.values().stream().allMatch(w -> w == 1 || w == 3));
			if (f != BreathingMethod.Flavour.NONE) {
				assertEquals(3, weights.values().stream().filter(w -> w == 3).count(), f + " favours three parts");
			}
		}
		Map<String, Integer> weights = TechniqueRules.duelistWeights(BreathingMethod.Flavour.SPARK);
		Set<String> known = new HashSet<>(scroll);
		known.remove(TechniqueRules.BIND);
		assertEquals(TechniqueRules.BIND, TechniqueRules.draw(weights, known, 0.5).orElseThrow(), "the one part not known yet");
		known.add(TechniqueRules.BIND);
		assertTrue(TechniqueRules.draw(weights, known, 0.5).isEmpty(), "nothing left to show");
		for (double roll : new double[] {0, 0.25, 0.5, 0.99, 1.0}) {
			assertTrue(TechniqueRules.draw(weights, Set.of(), roll).isPresent());
		}
	}

	@Test
	void scrollsTurnUpInOldPlacesAndASwordTombIsWaiting() {
		boolean tomb = false;
		for (ScrollSources.Source source : ScrollSources.all()) {
			assertTrue(source.chance() >= 0 && source.chance() <= 100, source.id());
			assertFalse(source.weights().isEmpty(), source.id() + " gives parts");
			for (String part : source.weights().keySet()) {
				assertTrue(TechniqueRules.scrollable(part), source.id() + ": " + part + " is a part a scroll can carry");
			}
			if (source.id().equals("sword_tomb")) {
				tomb = true;
				assertTrue(source.lootTable().isEmpty(), "a source in code for the tombs' own tables to draw from");
			}
		}
		assertTrue(tomb, "the sword tombs' source is ready");
		assertTrue(ScrollSources.forLootTable("wildercord:entities/fallen_knight").size() == 1, "a fallen knight carries one now and then");
		assertTrue(ScrollSources.byId("sword_tomb").orElseThrow().draw(new java.util.Random(4)).isPresent());
	}

	// ------------------------------------------------------------------ an add-on's intent

	@Test
	void anAddOnsIntentIsWeighedAndPricedLikeTheRest() {
		TechniqueRules.Intent own = TechniqueRules.Intent.own("testmod:frostbind", 0.85, 1.2, 0.1);
		TechniqueRules.registerIntent(own);
		assertTrue(TechniqueRules.parts(TechniqueRules.Family.INTENT).contains("testmod:frostbind"));
		assertEquals(TechniqueRules.Family.INTENT, TechniqueRules.family("testmod:frostbind").orElseThrow());
		assertFalse(TechniqueRules.scrollable("testmod:frostbind"), "an add-on hands its own parts out");
		TechniqueRules.Profile p = TechniqueRules.profile(TechniqueRules.THRUST, TechniqueRules.ON_BLADE, "testmod:frostbind", BreathingMethod.Flavour.NONE,
			3, "", "");
		TechniqueRules.Profile plain = TechniqueRules.profile(TechniqueRules.THRUST, TechniqueRules.ON_BLADE, TechniqueRules.INFUSE, BreathingMethod.Flavour.NONE,
			3, "", "");
		assertTrue(TechniqueRules.worth(p) > TechniqueRules.worth(plain) - 0.2, "its effect is counted in its worth");
		assertThrows(IllegalArgumentException.class, () -> TechniqueRules.registerIntent(TechniqueRules.Intent.own(TechniqueRules.PIERCE, 1, 0, 0)),
			"a built-in intent can't be replaced");
		assertThrows(IllegalArgumentException.class, () -> TechniqueRules.profile("no_stroke", TechniqueRules.ON_BLADE, TechniqueRules.INFUSE,
			BreathingMethod.Flavour.NONE, 3, "", ""));
	}

	// ------------------------------------------------------------------ the book

	@Test
	void aBookIsKeptAndSentWhole() {
		Techniques.Written w = new Techniques.Written("Ember Fang", TechniqueRules.THRUST, TechniqueRules.WAVE, TechniqueRules.SUNDER, "full full low");
		Techniques.Book book = new Techniques.Book(List.of(TechniqueRules.THRUST, TechniqueRules.WAVE, TechniqueRules.THRUST), List.of(w),
			List.of(new Techniques.Honing(w.key(), 120, 10, TechniqueRules.SWIFT, "", 40)));
		assertEquals(TechniqueRules.MAX_SLOTS, book.slots().size(), "always three slots");
		assertEquals(List.of(TechniqueRules.THRUST, TechniqueRules.WAVE), book.learned(), "a part learned once");
		assertTrue(book.slot(1).empty() && book.slot(9).empty());
		assertEquals(TechniqueRules.HONED, book.honing(w.key()).rank());
		assertEquals(TechniqueRules.RAW, book.honing("nothing/like/this").rank(), "a fresh record for one never used");
		// Saved and read back.
		var saved = Techniques.Book.CODEC.encodeStart(JsonOps.INSTANCE, book).getOrThrow();
		assertEquals(book, Techniques.Book.CODEC.parse(JsonOps.INSTANCE, saved).getOrThrow());
		assertEquals(Techniques.Book.EMPTY, Techniques.Book.CODEC.parse(JsonOps.INSTANCE, new com.google.gson.JsonObject()).getOrThrow(),
			"an empty record reads as an empty book");
		// Sent and read back.
		ByteBuf buf = Unpooled.buffer();
		Techniques.Book.STREAM_CODEC.encode(buf, book);
		assertEquals(book, Techniques.Book.STREAM_CODEC.decode(buf));
		// The oldest records go first past the most kept, never one a slot holds.
		Techniques.Book many = book;
		for (int i = 0; i < TechniqueRules.MAX_RECORDS + 5; i++) {
			many = many.withRecord(new Techniques.Honing("k/" + i + "/x", i, 0, "", "", 100 + i));
		}
		assertEquals(TechniqueRules.MAX_RECORDS, many.records().size());
		assertTrue(many.hasRecord(w.key()), "the written technique's record is kept, however old");
		assertFalse(many.hasRecord("k/0/x"), "the oldest went");
		assertEquals("Ember Fang", w.shownName());
		assertEquals("Fang", new Techniques.Written("§kFang", TechniqueRules.THRUST, TechniqueRules.WAVE, TechniqueRules.SUNDER, "full full low").shownName(),
			"made safe again as it's shown, whatever was stored");
		assertEquals("full full low", w.sword().orElseThrow().text());
		assertTrue(new Techniques.Written("x", "a", "b", "c", "not a string").sword().isEmpty());
	}
}
