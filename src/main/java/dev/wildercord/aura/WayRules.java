package dev.wildercord.aura;

import java.util.List;
import java.util.Map;

/**
 * Ways, the pure part. At the Edge breakthrough a swordsman's path forks: they choose a <b>Way</b> at the <b>crossroads</b>, four
 * standards of light that rise round them as the breakthrough settles, by striking the one they mean to walk (once to lean toward it,
 * again to walk it). Each Way has a <b>node</b> at Edge, Form and Sovereign: a passive, and a change to one of the techniques they
 * already know, so the Way changes how they fight rather than how hard.
 * <ul>
 * <li><b>The Blade</b> ({@link #BLADE}), offence: clean hits build momentum faster and the slash pierces (Edge); finishers hit harder
 *     and cascade through a worn crowd (Form); finishers feed an awakening longer and the awakened slash is free and quick (Sovereign).</li>
 * <li><b>The Bulwark</b> ({@link #BULWARK}), defence: a stance hard to break and a guard that covers every side and throws blows back
 *     (Edge); sturdier aura armour and an Intent that draws foes off allies (Form); an awakening nothing breaks and a Dominion shots
 *     can't cross (Sovereign).</li>
 * <li><b>The Shadowstep</b> ({@link #SHADOWSTEP}), movement: blows from behind open foes faster and a perfect guard slips behind its
 *     striker (Edge); every blow after a step counts as from behind and the step leaves an afterimage that strikes (Form); a finisher
 *     from behind readies the step and the awakened step is free and quick, its afterimage striking twice (Sovereign).</li>
 * <li><b>The Banner</b> ({@link #BANNER}), together: momentum shared with allies and a rallying cry on every finisher (Edge); aura
 *     shared and an Intent that steadies everyone near (Form); an awakening that rallies allies' momentum and a Dominion that shelters
 *     the party (Sovereign).</li>
 * </ul>
 *
 * <p>A swordsman already at Edge or above when Ways came in walks none until they choose: the breathing stance held a few seconds
 * calls the crossroads for them, and they get every node they've reached at once (the first choice is free). Changing Way later is
 * costly on purpose ({@link #settles}): it takes a Crossroads Incense burned at a place of power, and the new Way's Form and Sovereign
 * nodes only wake once the swordsman has earned a stretch of experience walking it.</p>
 *
 * <p>Shared by the server ({@code aura.Ways}, {@code aura.Crossroads}, {@code aura.WayBanner}), the Aura page and the unit tests. Every
 * number a server owner may want to change is also in the config's {@code aura} section; the defaults live here.</p>
 */
public final class WayRules {
	private WayRules() {}

	// ------------------------------------------------------------------ the Ways and their nodes

	public static final String BLADE = "blade";
	public static final String BULWARK = "bulwark";
	public static final String SHADOWSTEP = "shadowstep";
	public static final String BANNER = "banner";
	/** The four built-in Ways, in the order the crossroads and the Aura page show them. */
	public static final List<String> BUILT_IN = List.of(BLADE, BULWARK, SHADOWSTEP, BANNER);

	/** Each Way's colour (its standard at the crossroads, its column on the Aura page, its banner). */
	public static final int BLADE_COLOR = 0xFF6A58;
	public static final int BULWARK_COLOR = 0x6FB4FF;
	public static final int SHADOWSTEP_COLOR = 0xB394FF;
	public static final int BANNER_COLOR = 0xFFC94A;

	/** The stage a Way is chosen at, and the three stages that each hold one of its nodes. */
	public static final int FROM = AuraRules.EDGE;
	public static final int[] NODE_STAGES = {AuraRules.EDGE, AuraRules.FORM, AuraRules.SOVEREIGN};

	/** A built-in node's id: {@code <way>_<stage id>} (blade_edge, bulwark_form, banner_sovereign...). */
	public static String node(String way, int stage) {
		return way + "_" + AuraRules.id(stage);
	}

	public static final String BLADE_EDGE = node(BLADE, AuraRules.EDGE);
	public static final String BLADE_FORM = node(BLADE, AuraRules.FORM);
	public static final String BLADE_SOVEREIGN = node(BLADE, AuraRules.SOVEREIGN);
	public static final String BULWARK_EDGE = node(BULWARK, AuraRules.EDGE);
	public static final String BULWARK_FORM = node(BULWARK, AuraRules.FORM);
	public static final String BULWARK_SOVEREIGN = node(BULWARK, AuraRules.SOVEREIGN);
	public static final String SHADOWSTEP_EDGE = node(SHADOWSTEP, AuraRules.EDGE);
	public static final String SHADOWSTEP_FORM = node(SHADOWSTEP, AuraRules.FORM);
	public static final String SHADOWSTEP_SOVEREIGN = node(SHADOWSTEP, AuraRules.SOVEREIGN);
	public static final String BANNER_EDGE = node(BANNER, AuraRules.EDGE);
	public static final String BANNER_FORM = node(BANNER, AuraRules.FORM);
	public static final String BANNER_SOVEREIGN = node(BANNER, AuraRules.SOVEREIGN);

	/** The technique (or art) each built-in node changes, by the Aura page's row key ({@code aura.wildercord.technique.<key>}). */
	public static final Map<String, String> CHANGES = Map.ofEntries(
		Map.entry(BLADE_EDGE, "slash"), Map.entry(BLADE_FORM, "finisher"), Map.entry(BLADE_SOVEREIGN, "awaken"),
		Map.entry(BULWARK_EDGE, "guard"), Map.entry(BULWARK_FORM, "intent"), Map.entry(BULWARK_SOVEREIGN, "dominion"),
		Map.entry(SHADOWSTEP_EDGE, "guard"), Map.entry(SHADOWSTEP_FORM, "step"), Map.entry(SHADOWSTEP_SOVEREIGN, "awaken"),
		Map.entry(BANNER_EDGE, "finisher"), Map.entry(BANNER_FORM, "intent"), Map.entry(BANNER_SOVEREIGN, "dominion"));

	// ------------------------------------------------------------------ a node's state, as the Aura page shows it

	/** Where one node stands for a swordsman. */
	public enum NodeState {
		/** No Way chosen yet: every Way's nodes are open to choose. */
		OPEN,
		/** Their Way's, reached and awake: in force. */
		CHOSEN,
		/** Their Way's, reached, but still settling after a change of Way (it wakes as they earn experience walking it). */
		WAKING,
		/** Their Way's, at a stage not reached yet. */
		UPCOMING,
		/** Another Way's, while they walk one. */
		LOCKED
	}

	/**
	 * Where a node at {@code nodeStage} of a Way stands for a swordsman at {@code stage} who walks {@code walking} ("" for none), the node
	 * being {@code nodeWay}'s, with {@code owed} experience still owed of {@code settleTotal} since their last change of Way.
	 */
	public static NodeState state(String walking, String nodeWay, int stage, int nodeStage, double owed, double settleTotal) {
		if (walking == null || walking.isEmpty()) {
			return NodeState.OPEN;
		}
		if (!walking.equals(nodeWay)) {
			return NodeState.LOCKED;
		}
		if (stage < nodeStage) {
			return NodeState.UPCOMING;
		}
		return awake(nodeStage, owed, settleTotal) ? NodeState.CHOSEN : NodeState.WAKING;
	}

	// ------------------------------------------------------------------ settling after a change of Way

	/**
	 * Experience a new Way asks before its later nodes wake, after a change of Way (the server's {@code way_settle_xp}): the Form node
	 * wakes at half of it, the Sovereign node at all of it. The Edge node wakes at once. A first choice owes nothing.
	 */
	public static final double SETTLE_XP = 240.0;

	/** The share of the settling the Form node waits on (the Sovereign node waits on all of it). */
	public static final double FORM_WAKES = 0.5;

	/** Whether a node at {@code nodeStage} is awake with {@code owed} of {@code total} still owed. */
	public static boolean awake(int nodeStage, double owed, double total) {
		if (nodeStage <= FROM || owed <= 1.0E-6 || total <= 0) {
			return true;
		}
		double earned = total - owed;
		return nodeStage >= AuraRules.SOVEREIGN ? earned >= total - 1.0E-6 : earned >= total * FORM_WAKES - 1.0E-6;
	}

	/** What a node at {@code nodeStage} still waits on (experience), 0 once awake. */
	public static double toWake(int nodeStage, double owed, double total) {
		if (awake(nodeStage, owed, total)) {
			return 0;
		}
		double earned = total - owed;
		double need = nodeStage >= AuraRules.SOVEREIGN ? total : total * FORM_WAKES;
		return Math.max(0, need - earned);
	}

	/** How far a node at {@code nodeStage} has come toward waking (0 to 1). */
	public static double wakeProgress(int nodeStage, double owed, double total) {
		if (awake(nodeStage, owed, total)) {
			return 1;
		}
		double need = nodeStage >= AuraRules.SOVEREIGN ? total : total * FORM_WAKES;
		return Math.max(0, Math.min(1, (total - owed) / need));
	}

	/** What's still owed after earning {@code xp} more. */
	public static double settle(double owed, double xp) {
		return Math.max(0, owed - Math.max(0, xp));
	}

	/** Whether a choice made now owes anything: only a change of Way does (a first choice, even made late, doesn't). */
	public static boolean settles(boolean changed) {
		return changed;
	}

	// ------------------------------------------------------------------ the crossroads

	/** How long after the Edge breakthrough the crossroads rises (ticks): once the breakthrough's title has been read. */
	public static final int OPEN_DELAY = 50;
	/** How long it stands (ticks) before it fades unchosen. */
	public static final int CROSSROADS_TICKS = 1200;
	/** How long the breathing stance must have held (ticks, after it settles) to call the crossroads for a swordsman without a Way. */
	public static final int CALL_TICKS = 60;
	/** How long after one fades before the stance can call another (ticks). */
	public static final int CALL_REST = 200;
	/** How far from where it rose its swordsman may walk (blocks) before it fades. */
	public static final double LEAVE = 12.0;
	/** A standard leaned toward waits this long (ticks) for the second strike that walks it. */
	public static final int LEAN_TICKS = 160;
	/** How far the standards stand from the swordsman (blocks), trying nearer where there's no room. */
	public static final double[] RADII = {4.2, 3.2, 2.4};
	/** How far apart they stand (degrees round the swordsman), centred on the way they face: four all in view at once. */
	public static final double SPREAD = 24.0;
	/** A standard's height (blocks): its light from its foot to its head. */
	public static final double STANDARD_HEIGHT = 2.6;
	/** A strike reaches a standard this far off (blocks, from the eyes), and counts within this much of its axis. */
	public static final double REACH = 5.5;
	public static final double STRIKE_RADIUS = 0.8;

	/** The angle (degrees, from the way the swordsman faces, clockwise seen from above) of the {@code i}th of {@code count} standards. */
	public static double angle(int i, int count) {
		return (i - (count - 1) / 2.0) * SPREAD;
	}

	/**
	 * How a strike along a look ray meets a standard: the ray from {@code eye} along unit {@code look}, the standard's axis rising
	 * {@link #STANDARD_HEIGHT} from {@code foot} (each as x, y, z). Returns how far along the ray the closest approach is and how far
	 * from the axis it passes (blocks), or null when it passes the standard by: further than {@link #STRIKE_RADIUS} from the axis,
	 * outside the axis' height, behind the eyes or out of {@link #REACH}. Of two standards a strike could meet, the one nearer its line
	 * is struck.
	 */
	public static double[] strike(double[] eye, double[] look, double[] foot) {
		// The closest approach between the ray and the vertical segment, solved in the horizontal plane first (a vertical axis).
		double lx = look[0];
		double lz = look[2];
		double flat = lx * lx + lz * lz;
		double dx = foot[0] - eye[0];
		double dz = foot[2] - eye[2];
		double t;
		if (flat < 1.0E-8) {
			// Looking straight up or down: only a standard right under or over the eyes, which never happens at the crossroads.
			return null;
		}
		t = (dx * lx + dz * lz) / flat;
		if (t <= 0 || t > REACH) {
			return null;
		}
		double px = eye[0] + lx * t;
		double pz = eye[2] + lz * t;
		double miss = Math.hypot(px - foot[0], pz - foot[2]);
		if (miss > STRIKE_RADIUS) {
			return null;
		}
		double y = eye[1] + look[1] * t;
		if (y < foot[1] - 0.2 || y > foot[1] + STANDARD_HEIGHT + 0.3) {
			return null;
		}
		return new double[] {t, miss};
	}

	// ------------------------------------------------------------------ the Blade

	/** Clean hits build momentum this many times as fast (Keen Edge). */
	public static final double BLADE_HIT_MOMENTUM = 1.35;
	/** The piercing slash cuts this many more foes, and a crescent of the Blade that wins a clash flies on at this share of its harm. */
	public static final int BLADE_SLASH_TARGETS = 4;
	public static final double BLADE_CLASH_CARRY = 0.5;
	/** Finishers add this many times as much (Cascade), a player's cap still holding. */
	public static final double BLADE_FINISHER = 1.4;
	/** A finisher opens the nearest creature this close (blocks) to the finished foe whose stance is at least this worn. */
	public static final double CASCADE_RADIUS = 5.0;
	public static final double CASCADE_WORN = 0.5;
	/** Each finisher feeds the awakening this long (ticks), all of them together at most this long (Storm of Edges). */
	public static final int BLADE_FEED = 40;
	public static final int BLADE_FEED_MOST = 160;
	/** Awakened, the slash costs this share of its price and rests this share of its rest. */
	public static final double BLADE_AWAKENED_SLASH_PRICE = 0.0;
	public static final double BLADE_AWAKENED_SLASH_REST = 0.5;

	/** What a finisher adds with Cascade: {@code extra} lifted, against a player held to {@code playerCap} (their usual cap). */
	public static double finisher(double extra, boolean againstPlayer, double playerCap) {
		double lifted = Math.max(0, extra) * BLADE_FINISHER;
		return againstPlayer ? Math.min(lifted, Math.max(Math.max(0, extra), playerCap)) : lifted;
	}

	/** Whether a creature with {@code left} of its stance (0 to 1) is worn enough to cascade open. */
	public static boolean cascades(double left) {
		return left <= 1 - CASCADE_WORN + 1.0E-9;
	}

	// ------------------------------------------------------------------ the Bulwark

	/** A Bulwark's own stance takes this share of what would wear it (Wide Guard); awakened at Sovereign, none at all. */
	public static final double BULWARK_STANCE = 0.6;
	/** A hit takes this share of the momentum it otherwise would off a Bulwark. */
	public static final double BULWARK_MOMENTUM_LOSS = 0.67;
	/** The perfect guard's moment (ticks), half again the parry's. */
	public static final int BULWARK_PERFECT = 10;
	/** A held guard throws this share of each blow it catches back at the striker, at most once in this long per striker (ticks). */
	public static final double BULWARK_REFLECT = 1.0 / 3.0;
	public static final int BULWARK_REFLECT_REST = 10;
	/** A held guard turning a shot back costs this much aura. */
	public static final double BULWARK_SHOT_COST = 1.5;
	/** Aura armour takes this much more of each blow (Living Wall), and costs this share of its price a point. */
	public static final double BULWARK_ARMOUR_SHARE = 0.10;
	public static final double BULWARK_ARMOUR_COST = 0.8;
	/** A creature striking a raised guard staggers this long (ticks), at most once in this long per creature. */
	public static final int BULWARK_STAGGER = 20;
	public static final int BULWARK_STAGGER_REST = 40;
	/** Awakened (Unbroken), what reaches a Bulwark is this much weaker (against a player's harm, times the PvP scale). */
	public static final double BULWARK_AWAKENED_HARM = 0.2;
	/** Foes inside a bastion Dominion are slowed this many levels more. */
	public static final int BASTION_SLOW = 1;

	/** What a Bulwark's own stance is worn by, of {@code wear}: less, and nothing while awakened at Sovereign. */
	public static double bulwarkStance(double wear, boolean unbroken) {
		return unbroken ? 0 : Math.max(0, wear) * BULWARK_STANCE;
	}

	/** The armour's share of a blow for a Bulwark at Form: {@code share} and a tenth more, never past three quarters. */
	public static double armourShare(double share) {
		return Math.min(0.75, Math.max(0, share) + BULWARK_ARMOUR_SHARE);
	}

	// ------------------------------------------------------------------ the Shadowstep

	/**
	 * A blow lands from behind when the striker stands in the rear of the foe's facing: the cosine of the angle between the way it faces
	 * and the way to the striker below this (a third of a turn or more off its front).
	 */
	public static final double BEHIND = -0.5;
	/** Blows and arts from behind wear stance this many times as fast (Slip). */
	public static final double BEHIND_WEAR = 1.5;
	/** A slip lands this far past the striker's middle (blocks), and the striker counts as having lost the swordsman this long (ticks). */
	public static final double SLIP_GAP = 0.9;
	public static final int SLIP_LOST = 30;
	/** After an Aura Step, every blow counts as from behind this long (ticks: Afterimage). */
	public static final int UNSEEN_TICKS = 40;
	/** The step's afterimage strikes this long after the step (ticks), cutting every foe this close to it (blocks) for this share of the weapon. */
	public static final int AFTERIMAGE_DELAY = 8;
	public static final double AFTERIMAGE_RADIUS = 2.2;
	public static final double AFTERIMAGE_FACTOR = 0.6;
	/** Awakened at Sovereign, it strikes again this long after the first (ticks). */
	public static final int AFTERIMAGE_AGAIN = 8;
	/** How long the afterimage left where the step began lingers (ticks), so it's there to strike. */
	public static final int AFTERIMAGE_LINGER = 18;
	/** Awakened (Thousand Shadows), the step costs this share of its price and rests this share of its rest. */
	public static final double SHADOW_AWAKENED_STEP_PRICE = 0.0;
	public static final double SHADOW_AWAKENED_STEP_REST = 0.5;

	/**
	 * Whether a striker at ({@code toX}, {@code toZ}) from a foe (the way from the foe to the striker) is behind a foe facing
	 * ({@code faceX}, {@code faceZ}).
	 */
	public static boolean behind(double faceX, double faceZ, double toX, double toZ) {
		double f = Math.hypot(faceX, faceZ);
		double t = Math.hypot(toX, toZ);
		if (f < 1.0E-6 || t < 1.0E-6) {
			return false;
		}
		return (faceX * toX + faceZ * toZ) / (f * t) < BEHIND;
	}

	/** What a blow from behind wears: more, and on a player never past the most one blow may wear of their stance ({@code playerCap}). */
	public static double fromBehind(double wear, boolean againstPlayer, double playerCap) {
		double more = Math.max(0, wear) * BEHIND_WEAR;
		return againstPlayer ? Math.min(more, Math.max(Math.max(0, wear), playerCap)) : more;
	}

	// ------------------------------------------------------------------ the Banner

	/** How far the Banner reaches (blocks): allies this near share in it (the server's {@code banner_range}). */
	public static final double BANNER_RANGE = 12.0;
	/** The share of the momentum a Banner builds that each allied swordsman near builds too (the server's {@code banner_share}). */
	public static final double BANNER_MOMENTUM = 1.0 / 3.0;
	/** The share of the aura a Banner gathers that each allied swordsman near gathers too (the server's {@code banner_aura_share}). */
	public static final double BANNER_AURA = 0.25;
	/** A finisher's rallying cry (Battle Cry): steadied this much, this long (ticks); allied swordsmen build this much momentum and get this share of its aura. */
	public static final double CRY_STEADY = 1.0 / 6.0;
	public static final int CRY_TICKS = 100;
	public static final double CRY_MOMENTUM = 6.0;
	public static final double CRY_AURA = 0.5;
	/** Intent's steadying (Rallying Presence): while it presses on a foe, the Banner and allies in its reach are steadied this much. */
	public static final double PRESENCE_STEADY = 0.1;
	public static final int PRESENCE_TICKS = 30;
	/** An awakening rallies (Shelter): allied swordsmen's momentum held at this or above for as long as it burns. */
	public static final double RALLY_FLOOR = 50.0;
	/** A sheltering Dominion: allies inside take this much less from foes. */
	public static final double SHELTER = 0.2;
	/** All a body's steadying together (cries, presence, shelter, an unbroken Bulwark's awakening) never takes off more than this. */
	public static final double STEADY_MOST = 0.3;

	/**
	 * What reaches a body steadied by {@code reduction} (already combined), from a player ({@code fromPlayer}) or anything else, as a share
	 * of the harm: a player's harm is lessened only by the PvP scale's share of it.
	 */
	public static double harmLeft(double reduction, boolean fromPlayer, double pvpScale) {
		double r = Math.max(0, Math.min(STEADY_MOST, reduction));
		if (fromPlayer) {
			r *= Math.max(0, Math.min(1, pvpScale));
		}
		return 1 - r;
	}

	/** Two steadyings together: each takes its share of what the other leaves, never past {@link #STEADY_MOST}. */
	public static double steadied(double a, double b) {
		double together = 1 - (1 - Math.max(0, a)) * (1 - Math.max(0, b));
		return Math.min(STEADY_MOST, together);
	}

	/** A Banner at Sovereign near: allied swordsmen lose this share of the momentum a hit otherwise takes. */
	public static final double BANNER_ALLY_LOSS = 0.67;

	/** What a hit takes off momentum for a swordsman, by its share ({@code bulwark}: theirs; {@code banner}: a Banner at Sovereign near), never under half. */
	public static double lossScale(boolean bulwark, boolean banner) {
		double s = (bulwark ? BULWARK_MOMENTUM_LOSS : 1.0) * (banner ? BANNER_ALLY_LOSS : 1.0);
		return Math.max(0.5, s);
	}

	/** What each ally near builds of {@code built} at {@code share}. */
	public static double share(double built, double share) {
		return Math.max(0, built) * Math.max(0, Math.min(1, share));
	}

	// ------------------------------------------------------------------ the voices

	/** Each built-in Way's own voice as it's chosen (over {@code aura_way_chosen}); an add-on's Way rings plain steel. */
	public static String voice(String wayId) {
		return BUILT_IN.contains(wayId) ? "aura_way_" + wayId : "aura_awaken_steel";
	}

	/** Every kit sound the crossroads and the Ways play (tools/feel/aura_ways.py), for the tests. */
	public static final List<String> SOUNDS = List.of("aura_crossroads", "aura_way_lean", "aura_way_chosen", "aura_way_blade", "aura_way_bulwark",
		"aura_way_shadowstep", "aura_way_banner", "aura_way_unbound", "aura_way_slip", "aura_way_afterimage", "aura_way_cascade", "aura_way_reflect",
		"aura_way_cry", "aura_way_bastion");

	// ------------------------------------------------------------------ the balance model

	/**
	 * What one node is worth, estimated as shares of a swordsman's strength in a typical fight (0.05 is five percent): what it adds to
	 * their own damage ({@code offence}), what it takes off what reaches them ({@code defence}), how much faster their momentum and
	 * openings come ({@code tempo}, weighed at {@link #TEMPO}), what reaching places and getting out of them is worth ({@code mobility}),
	 * and what it gives each ally fighting near them ({@code support}, the same kind of share for each). Solo, support counts for
	 * nothing; in a party it counts once for each ally. {@code WayRulesTest} holds the Ways to each other with it.
	 */
	public record Worth(String node, double offence, double defence, double tempo, double mobility, double support) {
		/** What it's worth to a swordsman with {@code allies} allies fighting near. */
		public double with(int allies) {
			return offence + defence + TEMPO * tempo + mobility + support * Math.max(0, allies);
		}
	}

	/** Tempo (faster momentum and openings) weighed against damage: momentum's tiers are about a sixth of a swordsman's strength. */
	public static final double TEMPO = 0.15;

	/**
	 * The twelve nodes' worth, each with its reasoning. Uptimes: a finisher about every ten seconds in a fight, an awakening a sixth of
	 * a long fight at most (twenty seconds in each two minutes, its rest included), a Dominion a twelfth, Intent pressing most of a
	 * fight against foes weaker than you, the slash about a sixth of a swordsman's output at Edge.
	 */
	public static final Map<String, Worth> WORTH = Map.ofEntries(
		// Momentum a third faster from hits (tempo); the slash through guards and four more foes, and winning clashes (a fifth more
		// out of a sixth of the output).
		Map.entry(BLADE_EDGE, new Worth(BLADE_EDGE, 0.035, 0.0, 0.35, 0.0, 0.0)),
		// Finishers (about a sixth of the damage) two fifths harder; cascades open a worn crowd for more finishers and openings.
		Map.entry(BLADE_FORM, new Worth(BLADE_FORM, 0.09, 0.0, 0.2, 0.0, 0.0)),
		// Up to four seconds more awakening (a fifth more of its sixth); a free slash every second while it burns.
		Map.entry(BLADE_SOVEREIGN, new Worth(BLADE_SOVEREIGN, 0.075, 0.0, 0.1, 0.0, 0.0)),
		// Momentum held through hits and a stance hard to break (tempo kept); a guard round every side, half again as forgiving,
		// throwing blows back and turning shots.
		Map.entry(BULWARK_EDGE, new Worth(BULWARK_EDGE, 0.015, 0.07, 0.2, 0.0, 0.0)),
		// Armour taking a third, not a quarter, for less (a tenth less harm while the pool holds); staggering creatures on the guard;
		// creatures drawn off allies (what they don't take).
		Map.entry(BULWARK_FORM, new Worth(BULWARK_FORM, 0.0, 0.075, 0.0, 0.0, 0.03)),
		// A fifth less harm while awakened (a sixth of the time) and never opened then, never slowed spent; a Dominion shots can't
		// cross, foes slower inside.
		Map.entry(BULWARK_SOVEREIGN, new Worth(BULWARK_SOVEREIGN, 0.0, 0.07, 0.05, 0.0, 0.01)),
		// Stance from behind half again as fast; a perfect guard that lands you behind its striker (a free repositioning, and the
		// counter from behind).
		Map.entry(SHADOWSTEP_EDGE, new Worth(SHADOWSTEP_EDGE, 0.02, 0.01, 0.3, 0.03, 0.0)),
		// Every blow from behind after a step; an afterimage cutting every foe beside where you were (a little over half a blow,
		// every step).
		Map.entry(SHADOWSTEP_FORM, new Worth(SHADOWSTEP_FORM, 0.06, 0.0, 0.2, 0.02, 0.0)),
		// Finishers from behind ready the step (more steps, more afterimages); a free, quick step and a double afterimage while
		// awakened.
		Map.entry(SHADOWSTEP_SOVEREIGN, new Worth(SHADOWSTEP_SOVEREIGN, 0.055, 0.0, 0.1, 0.02, 0.0)),
		// Momentum shared (a third of yours, for each ally); a cry on every finisher steadying you and them (a sixth less harm about
		// half a fight), their momentum and aura besides.
		Map.entry(BANNER_EDGE, new Worth(BANNER_EDGE, 0.0, 0.065, 0.0, 0.0, 0.06)),
		// Aura shared (a quarter of yours); Intent steadying you and allies in its reach (a tenth less harm most of a fight).
		Map.entry(BANNER_FORM, new Worth(BANNER_FORM, 0.0, 0.06, 0.0, 0.0, 0.05)),
		// An awakening holding allies' momentum at the second tier; a Dominion sheltering you and them (a fifth less harm, aura and
		// momentum kept inside).
		Map.entry(BANNER_SOVEREIGN, new Worth(BANNER_SOVEREIGN, 0.0, 0.04, 0.0, 0.0, 0.045)));

	/** What a Way's three nodes are worth together to a swordsman with {@code allies} allies near. */
	public static double worth(String way, int allies) {
		double sum = 0;
		for (int stage : NODE_STAGES) {
			Worth w = WORTH.get(node(way, stage));
			if (w != null) {
				sum += w.with(allies);
			}
		}
		return sum;
	}
}
