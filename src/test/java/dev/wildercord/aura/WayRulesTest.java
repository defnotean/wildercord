package dev.wildercord.aura;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ways, the pure part ({@link WayRules}): the four Ways and their twelve nodes, where each node stands for a swordsman, the settling
 * after a change of Way, the crossroads' geometry and its strike, every node's numbers and their PvP caps, the balance between the
 * Ways, and their names, texts, art and voices against the generated assets.
 */
class WayRulesTest {
	private static final List<String> WAYS = WayRules.BUILT_IN;

	// ------------------------------------------------------------------ the Ways and their nodes

	@Test
	void fourWaysOfThreeNodesEach() {
		assertEquals(List.of("blade", "bulwark", "shadowstep", "banner"), WAYS);
		assertArrayEquals(new int[] {AuraRules.EDGE, AuraRules.FORM, AuraRules.SOVEREIGN}, WayRules.NODE_STAGES);
		assertEquals(AuraRules.EDGE, WayRules.FROM, "chosen at the Edge breakthrough");
		Set<String> nodes = new HashSet<>();
		for (String way : WAYS) {
			for (int stage : WayRules.NODE_STAGES) {
				String node = WayRules.node(way, stage);
				assertTrue(node.startsWith(way + "_"), node);
				assertTrue(nodes.add(node), node + " once");
				assertTrue(WayRules.CHANGES.containsKey(node), node + " changes a technique");
				assertTrue(WayRules.WORTH.containsKey(node), node + " is weighed");
			}
		}
		assertEquals(12, nodes.size());
		assertEquals("blade_edge", WayRules.BLADE_EDGE);
		assertEquals("banner_sovereign", WayRules.BANNER_SOVEREIGN);
		Set<Integer> colours = Set.of(WayRules.BLADE_COLOR, WayRules.BULWARK_COLOR, WayRules.SHADOWSTEP_COLOR, WayRules.BANNER_COLOR);
		assertEquals(4, colours.size(), "each Way has its own colour");
	}

	@Test
	void eachNodeChangesATechniqueTheSwordsmanHasByThen() {
		// What a technique opens at: a node never changes one its stage doesn't have yet.
		Map<String, Integer> opens = Map.of("guard", AuraRules.FLOW, "slash", AuraRules.EDGE, "finisher", AuraRules.GLOW, "awaken", AwakeningRules.FROM,
			"intent", AuraRules.FORM, "step", AuraRules.FORM, "dominion", AuraRules.SOVEREIGN);
		for (Map.Entry<String, String> change : WayRules.CHANGES.entrySet()) {
			int stage = change.getKey().endsWith("_edge") ? AuraRules.EDGE : change.getKey().endsWith("_form") ? AuraRules.FORM : AuraRules.SOVEREIGN;
			Integer open = opens.get(change.getValue());
			assertNotNull(open, change.getValue());
			assertTrue(open <= stage, change.getKey() + " changes " + change.getValue() + ", which opens later");
		}
		// The two awakening Ways (offence, movement) and the two Dominion Ways (defence, together) at Sovereign.
		assertEquals("awaken", WayRules.CHANGES.get(WayRules.BLADE_SOVEREIGN));
		assertEquals("awaken", WayRules.CHANGES.get(WayRules.SHADOWSTEP_SOVEREIGN));
		assertEquals("dominion", WayRules.CHANGES.get(WayRules.BULWARK_SOVEREIGN));
		assertEquals("dominion", WayRules.CHANGES.get(WayRules.BANNER_SOVEREIGN));
	}

	// ------------------------------------------------------------------ where a node stands

	@Test
	void aNodeStandsByWayStageAndSettling() {
		assertEquals(WayRules.NodeState.OPEN, WayRules.state("", "blade", AuraRules.SOVEREIGN, AuraRules.EDGE, 0, 0), "no Way yet: every node open");
		assertEquals(WayRules.NodeState.OPEN, WayRules.state(null, "banner", AuraRules.FLOW, AuraRules.FORM, 0, 0));
		assertEquals(WayRules.NodeState.LOCKED, WayRules.state("blade", "bulwark", AuraRules.SOVEREIGN, AuraRules.EDGE, 0, 0));
		assertEquals(WayRules.NodeState.CHOSEN, WayRules.state("blade", "blade", AuraRules.EDGE, AuraRules.EDGE, 0, 0));
		assertEquals(WayRules.NodeState.UPCOMING, WayRules.state("blade", "blade", AuraRules.EDGE, AuraRules.FORM, 0, 0));
		assertEquals(WayRules.NodeState.UPCOMING, WayRules.state("blade", "blade", AuraRules.FORM, AuraRules.SOVEREIGN, 0, 0));
		// After a change, the later nodes wake as experience is earned.
		assertEquals(WayRules.NodeState.CHOSEN, WayRules.state("blade", "blade", AuraRules.SOVEREIGN, AuraRules.EDGE, 240, 240), "Edge at once");
		assertEquals(WayRules.NodeState.WAKING, WayRules.state("blade", "blade", AuraRules.SOVEREIGN, AuraRules.FORM, 240, 240));
		assertEquals(WayRules.NodeState.CHOSEN, WayRules.state("blade", "blade", AuraRules.SOVEREIGN, AuraRules.FORM, 120, 240), "Form at half");
		assertEquals(WayRules.NodeState.WAKING, WayRules.state("blade", "blade", AuraRules.SOVEREIGN, AuraRules.SOVEREIGN, 1, 240));
		assertEquals(WayRules.NodeState.CHOSEN, WayRules.state("blade", "blade", AuraRules.SOVEREIGN, AuraRules.SOVEREIGN, 0, 240), "Sovereign at all of it");
		// A node not reached yet is upcoming, settled or not.
		assertEquals(WayRules.NodeState.UPCOMING, WayRules.state("blade", "blade", AuraRules.EDGE, AuraRules.SOVEREIGN, 240, 240));
	}

	@Test
	void settlingAsksExperienceOnlyAfterAChange() {
		assertFalse(WayRules.settles(false), "a first choice, even made late, owes nothing");
		assertTrue(WayRules.settles(true));
		double total = WayRules.SETTLE_XP;
		assertTrue(total > 0);
		assertTrue(WayRules.awake(AuraRules.EDGE, total, total), "the Edge node wakes at once");
		assertFalse(WayRules.awake(AuraRules.FORM, total, total));
		assertEquals(total * WayRules.FORM_WAKES, WayRules.toWake(AuraRules.FORM, total, total), 1e-9);
		assertEquals(total, WayRules.toWake(AuraRules.SOVEREIGN, total, total), 1e-9);
		double owed = WayRules.settle(total, 100);
		assertEquals(total - 100, owed, 1e-9);
		assertEquals(100 / (total * WayRules.FORM_WAKES), WayRules.wakeProgress(AuraRules.FORM, owed, total), 1e-9);
		assertEquals(100 / total, WayRules.wakeProgress(AuraRules.SOVEREIGN, owed, total), 1e-9);
		assertEquals(0, WayRules.settle(10, 50), "never owes less than nothing");
		assertEquals(10, WayRules.settle(10, -5), "a negative gain settles nothing");
		assertEquals(1, WayRules.wakeProgress(AuraRules.FORM, 0, total));
		assertTrue(WayRules.awake(AuraRules.SOVEREIGN, 50, 0), "nothing to settle against: awake");
	}

	// ------------------------------------------------------------------ the crossroads

	@Test
	void theStandardsStandOnAnArcInFront() {
		assertEquals(0, WayRules.angle(0, 1), 1e-9, "one standard stands straight ahead");
		assertEquals(-WayRules.angle(3, 4), WayRules.angle(0, 4), 1e-9, "four stand evenly either side of ahead");
		assertEquals(WayRules.SPREAD, WayRules.angle(2, 4) - WayRules.angle(1, 4), 1e-9);
		assertTrue(Math.abs(WayRules.angle(0, 4)) < 90, "all four in front, in view");
		// The arc's outer standards stay inside a usual first-person view (about fifty degrees either side at sixteen by nine).
		assertTrue(Math.abs(WayRules.angle(0, 4)) <= 46);
		assertTrue(WayRules.RADII[0] < WayRules.REACH, "a standard at its usual place is in reach");
		for (int i = 1; i < WayRules.RADII.length; i++) {
			assertTrue(WayRules.RADII[i] < WayRules.RADII[i - 1], "nearer where there's no room");
		}
		assertTrue(WayRules.LEAN_TICKS >= 60 && WayRules.LEAN_TICKS < WayRules.CROSSROADS_TICKS, "time to read it and strike again");
		assertTrue(WayRules.CALL_TICKS > AuraRules.BOB_TICKS && WayRules.CALL_TICKS <= 100, "a few seconds of breath, not a moment's");
		assertTrue(WayRules.OPEN_DELAY > 20, "after the breakthrough's title");
	}

	@Test
	void aStrikeMeetsTheStandardItIsAimedAt() {
		double[] eye = {0, 1.62, 0};
		double[] foot = {0, 0, 3};
		// Straight at it (a little down, at its middle).
		double[] look = norm(0, -0.2, 1);
		double[] hit = WayRules.strike(eye, look, foot);
		assertNotNull(hit);
		assertEquals(3.0 / Math.sqrt(1 - 0.2 * 0.2 / 1.04), hit[0], 0.05);
		assertTrue(hit[1] < 0.05);
		// Beside it, past its reach, behind the eyes, over its head.
		assertNull(WayRules.strike(eye, norm(1, 0, 1), foot), "aimed well to the side");
		assertNull(WayRules.strike(eye, look, new double[] {0, 0, 7}), "out of reach");
		assertNull(WayRules.strike(eye, norm(0, 0, -1), foot), "behind");
		assertNull(WayRules.strike(eye, norm(0, 1.5, 1), foot), "over its head");
		assertNull(WayRules.strike(eye, new double[] {0, -1, 0}, foot), "straight down");
		// Two standards side by side at the nearest the crossroads stands them: the one aimed at is the nearer to the line.
		double r = WayRules.RADII[WayRules.RADII.length - 1];
		double a = Math.toRadians(WayRules.SPREAD / 2);
		double[] left = {-Math.sin(a) * r, 0, Math.cos(a) * r};
		double[] right = {Math.sin(a) * r, 0, Math.cos(a) * r};
		double[] atRight = norm(right[0], -0.4, right[2]);
		double[] hl = WayRules.strike(eye, atRight, left);
		double[] hr = WayRules.strike(eye, atRight, right);
		assertNotNull(hr);
		assertTrue(hl == null || hl[1] > hr[1], "the one aimed at wins");
		// At the usual distance, standards side by side are further apart than one swing can reach both.
		double chord = 2 * WayRules.RADII[0] * Math.sin(a);
		assertTrue(chord > 2 * WayRules.STRIKE_RADIUS, "the usual spacing never lets one swing take two (" + chord + ")");
	}

	private static double[] norm(double x, double y, double z) {
		double l = Math.sqrt(x * x + y * y + z * z);
		return new double[] {x / l, y / l, z / l};
	}

	// ------------------------------------------------------------------ the Blade

	@Test
	void theBladesFinisherIsHarderButAPlayersCapHolds() {
		assertEquals(14.0, WayRules.finisher(10, false, 0), 1e-9, "two fifths more on a creature");
		assertEquals(8.0, WayRules.finisher(6, true, 8), 1e-9, "against a player, up to their cap");
		assertEquals(8.0, WayRules.finisher(8, true, 8), 1e-9, "a player at their cap gets no more");
		assertEquals(9.0, WayRules.finisher(9, true, 8), 1e-9, "never less than it was");
		assertEquals(0, WayRules.finisher(-3, false, 0));
		assertEquals(StanceRules.playerFinisherCap(20, 0.6, 1.0), Math.min(StanceRules.PVP_FINISHER * 0.6, 5), 1e-9);
		assertEquals(StanceRules.finisher(StanceRules.Kind.PLAYER, 40, 20, 8, 0.6, 1.0), Math.min(10, StanceRules.playerFinisherCap(20, 0.6, 1.0)), 1e-9,
			"the cap is the one finishers already keep");
		assertTrue(WayRules.cascades(0.5), "half worn opens");
		assertTrue(WayRules.cascades(0.1));
		assertFalse(WayRules.cascades(0.51), "less than half worn doesn't");
		assertTrue(WayRules.BLADE_HIT_MOMENTUM > 1 && WayRules.BLADE_HIT_MOMENTUM < 1.5);
		assertTrue(WayRules.BLADE_SLASH_TARGETS > 0);
		assertTrue(WayRules.BLADE_CLASH_CARRY > 0 && WayRules.BLADE_CLASH_CARRY < 1, "a crescent that wins a clash flies on weaker");
	}

	@Test
	void stormOfEdgesKeepsTheFinalArtToOncePerAwakening() {
		int longest = AwakeningRules.ticks(AuraRules.SOVEREIGN, 1) + WayRules.BLADE_FEED_MOST;
		assertTrue(StringRules.FINAL_COOLDOWN > longest, "the Final Art's rest outlasts even a Blade's fully fed awakening (" + longest + ")");
		assertEquals(2 * AwakeningRules.FINISHER_EXTEND, WayRules.BLADE_FEED, "twice a finisher's feed");
		assertEquals(2 * AwakeningRules.MAX_EXTEND, WayRules.BLADE_FEED_MOST);
		assertEquals(0, WayRules.BLADE_AWAKENED_SLASH_PRICE, 1e-9, "free while awakened");
		assertEquals(AuraRules.SLASH_COOLDOWN / 2, (int) Math.round(AuraRules.SLASH_COOLDOWN * WayRules.BLADE_AWAKENED_SLASH_REST), "ready in a second");
	}

	// ------------------------------------------------------------------ the Bulwark

	@Test
	void theBulwarkStandsFirm() {
		assertEquals(6.0, WayRules.bulwarkStance(10, false), 1e-9, "two fifths less wear");
		assertEquals(0, WayRules.bulwarkStance(10, true), "unbreakable while awakened at Sovereign");
		assertEquals(0.35, WayRules.armourShare(AuraRules.ARMOUR_SHARE), 1e-9, "a third, not a quarter");
		assertEquals(0.75, WayRules.armourShare(0.7), 1e-9, "never past three quarters");
		assertTrue(WayRules.BULWARK_PERFECT > AuraRules.PERFECT_TICKS, "a wider perfect moment");
		assertEquals(Math.round(AuraRules.PERFECT_TICKS * 1.5 - 0.5), WayRules.BULWARK_PERFECT, "half again the parry's");
		assertTrue(AuraRules.perfect(100, 110, WayRules.BULWARK_PERFECT));
		assertFalse(AuraRules.perfect(100, 110, AuraRules.PERFECT_TICKS));
		assertFalse(AuraRules.perfect(100, 99, WayRules.BULWARK_PERFECT), "never before it's raised");
		assertTrue(WayRules.BULWARK_REFLECT > 0 && WayRules.BULWARK_REFLECT < 0.5, "throws back a share, never the blow");
		// The cheaper armour holds off more with the same aura above its floor.
		double plain = AuraRules.armourAbsorb(100, 30, 1.0);
		double sturdy = AuraRules.armourAbsorb(100, 30, 1.0, WayRules.BULWARK_ARMOUR_COST);
		assertTrue(sturdy > plain, plain + " then " + sturdy);
		assertEquals(AuraRules.armourAbsorb(10, 60, 0.25), AuraRules.armourAbsorb(10, 60, 0.25, 1.0), 1e-12, "the old call is the plain one");
	}

	@Test
	void momentumLostToHitsNeverFallsUnderHalf() {
		assertEquals(1.0, WayRules.lossScale(false, false), 1e-9);
		assertEquals(WayRules.BULWARK_MOMENTUM_LOSS, WayRules.lossScale(true, false), 1e-9);
		assertEquals(WayRules.BANNER_ALLY_LOSS, WayRules.lossScale(false, true), 1e-9);
		assertEquals(0.5, WayRules.lossScale(true, true), 1e-9, "a Bulwark with a Banner near still loses some");
	}

	// ------------------------------------------------------------------ the Shadowstep

	@Test
	void behindIsTheRearThirdOfATurn() {
		// A foe facing +z; the striker at its back, beside it, in front.
		assertTrue(WayRules.behind(0, 1, 0, -2), "straight behind");
		assertTrue(WayRules.behind(0, 1, 1, -1.5), "behind and a little aside");
		assertFalse(WayRules.behind(0, 1, 2, 0), "square to its side is not behind");
		assertFalse(WayRules.behind(0, 1, 0, 2), "in front");
		assertFalse(WayRules.behind(0, 1, 0, 0), "on top of it: neither");
		double angle = Math.toDegrees(Math.acos(WayRules.BEHIND));
		assertEquals(120, angle, 1e-6, "a third of a turn off its front");
	}

	@Test
	void blowsFromBehindOpenFasterButAPlayersCapHolds() {
		double cap = StanceRules.PLAYER_POOL * StanceRules.PVP_BLOW_CAP;
		assertEquals(15.0, WayRules.fromBehind(10, false, cap), 1e-9);
		assertEquals(cap, WayRules.fromBehind(8, true, cap), 1e-9, "a player never loses more than one blow may take");
		assertEquals(12.0, WayRules.fromBehind(12, true, cap), 1e-9, "never less than it was");
		assertTrue(WayRules.UNSEEN_TICKS > 0 && WayRules.UNSEEN_TICKS <= 60, "a moment after a step, not a stance");
		assertTrue(WayRules.AFTERIMAGE_LINGER > WayRules.AFTERIMAGE_DELAY, "the afterimage is still there when it strikes");
		assertTrue(WayRules.AFTERIMAGE_LINGER > WayRules.AFTERIMAGE_DELAY + WayRules.AFTERIMAGE_AGAIN - 4,
			"there (nearly) when it strikes again, awakened");
		assertTrue(WayRules.AFTERIMAGE_FACTOR < 1, "an afterimage's cut is less than a blow");
		assertTrue(WayRules.AFTERIMAGE_DELAY + WayRules.AFTERIMAGE_AGAIN < AuraRules.STEP_COOLDOWN * WayRules.SHADOW_AWAKENED_STEP_REST + 20,
			"its second cut lands before the next step's first, even awakened");
	}

	// ------------------------------------------------------------------ the Banner

	@Test
	void steadyingIsCappedAndScaledAgainstPlayers() {
		assertEquals(0.8, WayRules.harmLeft(0.2, false, 0.6), 1e-9);
		assertEquals(1 - 0.2 * 0.6, WayRules.harmLeft(0.2, true, 0.6), 1e-9, "a player's harm only by the PvP scale's share");
		assertEquals(1 - WayRules.STEADY_MOST, WayRules.harmLeft(0.9, false, 0.6), 1e-9, "never past the most");
		assertEquals(1.0, WayRules.harmLeft(0, false, 0.6), 1e-9);
		assertEquals(1.0, WayRules.harmLeft(0.3, true, 0), 1e-9, "no PvP bonuses: none against a player");
		double both = WayRules.steadied(WayRules.CRY_STEADY, WayRules.SHELTER);
		assertTrue(both > WayRules.SHELTER && both <= WayRules.STEADY_MOST, "two together, capped (" + both + ")");
		assertEquals(WayRules.STEADY_MOST, WayRules.steadied(0.3, 0.3), 1e-9);
		assertEquals(WayRules.SHELTER, WayRules.steadied(WayRules.SHELTER, 0), 1e-9);
		assertTrue(WayRules.STEADY_MOST < 0.5, "never makes anyone hard to kill");
	}

	@Test
	void theBannerSharesLessThanItBuilds() {
		assertEquals(10.0 / 3, WayRules.share(10, WayRules.BANNER_MOMENTUM), 1e-9);
		assertEquals(0, WayRules.share(-5, 0.5));
		assertEquals(10, WayRules.share(10, 3), 1e-9, "never more than all of it");
		assertTrue(WayRules.BANNER_MOMENTUM < 0.5 && WayRules.BANNER_AURA < 0.5, "an ally gets a share, never the whole");
		assertTrue(WayRules.RALLY_FLOOR < MomentumRules.PEAK, "a rally holds the second tier, not the peak (awakening's own)");
		assertEquals(2, MomentumRules.tier(WayRules.RALLY_FLOOR));
		assertTrue(WayRules.BANNER_RANGE >= AuraRules.INTENT_RADIUS && WayRules.BANNER_RANGE <= 24);
		assertTrue(WayRules.CRY_TICKS <= 100 && WayRules.PRESENCE_TICKS > AuraRules.INTENT_PERIOD, "presence holds between Intent's presses");
	}

	// ------------------------------------------------------------------ balance

	private static double sum(String way, ToDoubleFunction<WayRules.Worth> term) {
		double s = 0;
		for (int stage : WayRules.NODE_STAGES) {
			s += term.applyAsDouble(WayRules.WORTH.get(WayRules.node(way, stage)));
		}
		return s;
	}

	@Test
	void theWaysAreWorthTheSameInTheirOwnWays() {
		double blade = WayRules.worth(WayRules.BLADE, 0);
		double bulwark = WayRules.worth(WayRules.BULWARK, 0);
		double shadow = WayRules.worth(WayRules.SHADOWSTEP, 0);
		double banner = WayRules.worth(WayRules.BANNER, 0);
		double soloMax = Math.max(blade, Math.max(bulwark, shadow));
		double soloMin = Math.min(blade, Math.min(bulwark, shadow));
		assertTrue(soloMax / soloMin <= 1.15, "alone, the Blade, the Bulwark and the Shadowstep within 15% (" + blade + ", " + bulwark + ", " + shadow + ")");
		double mean = (blade + bulwark + shadow) / 3;
		assertTrue(banner >= 0.5 * mean && banner <= 0.8 * mean, "alone, the Banner is the quietest but no trap (" + banner + " of " + mean + ")");
		// With one ally, all four within a fifth of each other.
		double[] pair = {WayRules.worth(WayRules.BLADE, 1), WayRules.worth(WayRules.BULWARK, 1), WayRules.worth(WayRules.SHADOWSTEP, 1),
			WayRules.worth(WayRules.BANNER, 1)};
		double pairMax = 0;
		double pairMin = Double.MAX_VALUE;
		for (double w : pair) {
			pairMax = Math.max(pairMax, w);
			pairMin = Math.min(pairMin, w);
		}
		assertTrue(pairMax / pairMin <= 1.2, "with one ally, all four within 20% (" + java.util.Arrays.toString(pair) + ")");
		// With two, the Banner leads, but not by more than half again.
		double party = WayRules.worth(WayRules.BANNER, 2);
		double best = Math.max(WayRules.worth(WayRules.BLADE, 2), Math.max(WayRules.worth(WayRules.BULWARK, 2), WayRules.worth(WayRules.SHADOWSTEP, 2)));
		assertTrue(party > best && party <= 1.6 * best, "with two allies the Banner leads, within reason (" + party + " against " + best + ")");
	}

	@Test
	void eachWayLeadsInItsOwnThing() {
		String[] terms = {"offence", "defence", "mobility", "support"};
		String[] leaders = {WayRules.BLADE, WayRules.BULWARK, WayRules.SHADOWSTEP, WayRules.BANNER};
		List<ToDoubleFunction<WayRules.Worth>> fns = List.of(WayRules.Worth::offence, WayRules.Worth::defence, WayRules.Worth::mobility,
			WayRules.Worth::support);
		for (int t = 0; t < terms.length; t++) {
			double lead = sum(leaders[t], fns.get(t));
			for (String way : WAYS) {
				if (!way.equals(leaders[t])) {
					assertTrue(lead > sum(way, fns.get(t)), leaders[t] + " leads in " + terms[t] + " over " + way);
				}
			}
		}
		for (WayRules.Worth w : WayRules.WORTH.values()) {
			assertTrue(w.with(0) + w.support() > 0, w.node() + " is worth something");
			assertTrue(w.with(1) <= 0.16, w.node() + " never outweighs the rest of its Way by itself (" + w.with(1) + ")");
		}
	}

	// ------------------------------------------------------------------ named, described, drawn, voiced

	@Test
	void everythingIsNamedDescribedDrawnAndVoiced() throws java.io.IOException {
		com.google.gson.JsonObject lang = read("/assets/wildercord/lang/en_us.json");
		Set<String> names = new HashSet<>();
		for (String way : WAYS) {
			String key = "aura.wildercord.way." + way;
			for (String k : List.of(key, key + ".creed", key + ".short", "toast.wildercord.aura.way_" + way)) {
				assertTrue(lang.has(k), k);
			}
			assertTrue(names.add(lang.get(key).getAsString()), way + " has a name of its own");
			assertNotNull(WayRulesTest.class.getResourceAsStream("/assets/wildercord/textures/gui/sprites/aura/way_" + way + ".png"), way + "'s emblem");
			for (int stage : WayRules.NODE_STAGES) {
				String node = "aura.wildercord.way_node." + WayRules.node(way, stage);
				for (String k : List.of(node, node + ".passive", node + ".change")) {
					assertTrue(lang.has(k), k);
					assertFalse(lang.get(k).getAsString().isBlank(), k);
				}
				assertTrue(names.add(lang.get(node).getAsString()), node + " has a name of its own");
			}
		}
		assertNotNull(WayRulesTest.class.getResourceAsStream("/assets/wildercord/textures/gui/sprites/aura/way_unknown.png"), "an add-on's emblem");
		for (String change : new HashSet<>(WayRules.CHANGES.values())) {
			assertTrue(lang.has("aura.wildercord.technique." + change), change + " is a technique with a name");
		}
		for (String key : List.of("aura.wildercord.way.kicker", "aura.wildercord.crossroads", "aura.wildercord.crossroads.kicker",
				"item.wildercord.crossroads_incense", "item.wildercord.crossroads_incense.lore", "item.wildercord.crossroads_incense.use",
				"message.wildercord.aura.way.crossroads", "message.wildercord.aura.way.crossroads_how", "message.wildercord.aura.way.lean",
				"message.wildercord.aura.way.chosen", "message.wildercord.aura.way.node_now", "message.wildercord.aura.way.node_later",
				"message.wildercord.aura.way.node_waking", "message.wildercord.aura.way.faded", "message.wildercord.aura.way.waiting",
				"message.wildercord.aura.way.waiting_how", "message.wildercord.aura.way.no_room", "message.wildercord.aura.way.with_blade",
				"message.wildercord.aura.way.woke", "message.wildercord.aura.way.woke_later", "message.wildercord.aura.way.unbound",
				"message.wildercord.aura.way.incense_off", "message.wildercord.aura.way.incense_stage", "message.wildercord.aura.way.incense_none",
				"message.wildercord.aura.way.incense_place", "hud.wildercord.crossroads.hint", "hud.wildercord.crossroads.lean",
				"hud.wildercord.crossroads.walk", "screen.wildercord.aura.way", "screen.wildercord.aura.way.walking", "screen.wildercord.aura.way.none_yet",
				"screen.wildercord.aura.way.before_edge", "screen.wildercord.aura.way.node_title", "screen.wildercord.aura.way.passive",
				"screen.wildercord.aura.way.changes", "screen.wildercord.aura.way.change_line", "screen.wildercord.aura.way.change",
				"screen.wildercord.aura.way.how", "screen.wildercord.aura.way.detail.waking", "screen.wildercord.aura.way.detail.upcoming",
				"toast.wildercord.aura.crossroads", "toast.wildercord.aura.way", "toast.wildercord.aura.way_cascade", "toast.wildercord.aura.way_slip",
				"toast.wildercord.aura.way_afterimage", "toast.wildercord.aura.way_cry", "command.wildercord.aura.way.no_crossroads",
				"command.wildercord.aura.way.unknown", "command.wildercord.aura.way.state")) {
			assertTrue(lang.has(key), key);
		}
		for (WayRules.NodeState s : WayRules.NodeState.values()) {
			assertTrue(lang.has("screen.wildercord.aura.way.state." + s.name().toLowerCase(java.util.Locale.ROOT)), s.name());
			if (s != WayRules.NodeState.WAKING && s != WayRules.NodeState.UPCOMING) {
				assertTrue(lang.has("screen.wildercord.aura.way.detail." + s.name().toLowerCase(java.util.Locale.ROOT)), s.name());
			}
		}
		com.google.gson.JsonObject kit = read("/assets/wildercord/kit_sounds.json").getAsJsonObject("events");
		for (String sound : WayRules.SOUNDS) {
			assertTrue(kit.has(sound), sound + " isn't in the feel kit: run python tools/feel/build.py --only aura");
		}
		for (String way : WAYS) {
			assertTrue(WayRules.SOUNDS.contains(WayRules.voice(way)), way + " has its own voice");
		}
		assertEquals("aura_awaken_steel", WayRules.voice("someone:else"));
		assertNotNull(WayRulesTest.class.getResourceAsStream("/data/wildercord/recipe/crossroads_incense.json"), "the incense is made");
		assertNotNull(WayRulesTest.class.getResourceAsStream("/assets/wildercord/items/crossroads_incense.json"), "the incense is drawn");
	}

	private static com.google.gson.JsonObject read(String path) throws java.io.IOException {
		try (java.io.InputStream in = WayRulesTest.class.getResourceAsStream(path)) {
			assertNotNull(in, path);
			return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}
}
