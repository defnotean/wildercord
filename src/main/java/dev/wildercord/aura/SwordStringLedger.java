package dev.wildercord.aura;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-observed sword-string evidence, independent of Minecraft and the request's claimed marks.
 * An entity attack is sampled before vanilla resets charge or sprinting, then paired once with its
 * following punch. Air punches and spear thrusts carry their own pre-reset observations.
 *
 * <p>Only one stroke is admitted per server tick. The wire protocol has no authenticated action
 * sequence or client clock, so two real strokes arriving in one tick cannot be distinguished from
 * duplicates. This intentionally bounded policy can reject a lag-bunched string; it never invents
 * fully charged strokes from packet timing.</p>
 */
final class SwordStringLedger {

	static final int ATTACK_PUNCH_TICKS = 2;
	private static final long NEVER = Long.MIN_VALUE / 4;
	private static final int BASE_MARKS = SwordString.Token.SWING.bit() | SwordString.Token.FULL.bit()
		| SwordString.Token.LOW.bit() | SwordString.Token.LEAP.bit() | SwordString.Token.RUN.bit();

	/** Identity is deliberate: a new player body, level or held stack must not reuse an old attack snapshot. */
	record Context(Object body, Object level, Object weapon) {
		Context {
			Objects.requireNonNull(body);
			Objects.requireNonNull(level);
			Objects.requireNonNull(weapon);
		}
		boolean sameBodyAndLevel(Context other) { return body == other.body && level == other.level; }
		boolean sameAttack(Context other) { return sameBodyAndLevel(other) && weapon == other.weapon; }
	}

	record Stroke(long serial, long tick, int marks, int recover, Object guardReceipt) {}
	record Proof(Object ledger, List<Stroke> strokes) {
		Proof { strokes = List.copyOf(strokes); }
		long terminalSerial() { return strokes.getLast().serial(); }
		List<Integer> marks() { return strokes.stream().map(Stroke::marks).toList(); }
	}
	private record Snapshot(long tick, int marks, int recover, Context context, long guard, long step, Object guardReceipt) {}

	private final Deque<Stroke> strokes = new ArrayDeque<>();
	private Context context;
	private Snapshot pending;
	private long clock = NEVER;
	private long lastStroke = NEVER;
	private long nextSerial;
	private long consumedSerial;
	private long guardAt = NEVER;
	private long stepAt = NEVER;
	private Object guardReceipt;
	private Context guardOwner;

	void cue(long now, boolean guard, Context owner) { cue(now, guard, owner, null); }

	/** Opaque immutable evidence from the real guard, captured with its owner before any callbacks. */
	void cue(long now, boolean guard, Context owner, Object receipt) {
		observe(now, owner);
		if (guard) { guardAt = now; guardReceipt = receipt; guardOwner = owner; }
		else stepAt = now;
	}

	int cues(long now, Context owner) {
		observe(now, owner);
		return cues(now);
	}

	private int cues(long now) {
		int marks = 0;
		if (fresh(guardAt, now, StringRules.COUNTER_TICKS)) marks |= SwordString.Token.COUNTER.bit();
		if (fresh(stepAt, now, StringRules.STEP_CUT_TICKS)) marks |= SwordString.Token.STEP.bit();
		return marks;
	}

	/** No stroke is counted here: the punch that follows this attack must consume the snapshot. */
	void attack(long now, int observedMarks, int recover, Context owner) {
		observe(now, owner);
		pending = lastStroke == now ? null : snapshot(now, observedMarks, recover, owner);
	}

	/** Called before vanilla's punch reset, with current state as the fallback for an air swing. */
	boolean punch(long now, int observedMarks, int recover, Context owner) {
		observe(now, owner);
		Snapshot before = pending;
		pending = null;
		if (lastStroke == now) return false;
		Snapshot chosen = before != null && before.context().sameAttack(owner)
			&& fresh(before.tick(), now, ATTACK_PUNCH_TICKS)
			? before : snapshot(now, observedMarks, recover, owner);
		return append(now, chosen);
	}

	/** Spears send no punch. Their one component invocation, not each hit entity, supplies this observation. */
	boolean thrust(long now, int observedMarks, int recover, Context owner) {
		observe(now, owner);
		pending = null;
		if (lastStroke == now) return false;
		return append(now, snapshot(now, observedMarks, recover, owner));
	}

	/** The requested string must match the latest unconsumed observed suffix, with one shared jitter allowance. */
	Optional<Proof> proof(SwordString string, long now, int baseWindow, Context owner) {
		observe(now, owner);
		if (string == null || strokes.size() < string.length()) return Optional.empty();
		List<Stroke> recent = new ArrayList<>(strokes);
		List<Stroke> suffix = recent.subList(recent.size() - string.length(), recent.size());
		Stroke last = suffix.getLast();
		if (!fresh(last.tick(), now, StringRules.LAST_SWING_SLACK)) return Optional.empty();
		long jitter = 0;
		int[] marks = new int[suffix.size()];
		for (int i = 0; i < suffix.size(); i++) {
			Stroke stroke = suffix.get(i);
			if (stroke.serial() <= consumedSerial) return Optional.empty();
			marks[i] = stroke.marks();
			if (i > 0) {
				Stroke previous = suffix.get(i - 1);
				long gap = stroke.tick() - previous.tick();
				if (gap <= 0) return Optional.empty();
				jitter += Math.max(0, gap - StringRules.window(baseWindow, previous.recover()));
				if (jitter > StringRules.SEEN_SLACK) return Optional.empty();
			}
		}
		return string.fits(marks) ? Optional.of(new Proof(this, suffix)) : Optional.empty();
	}

	/** Reserve evidence before performing or entering a clash; the same strokes can never fund another request. */
	boolean consume(Proof proof) {
		if (proof == null || proof.ledger() != this || proof.strokes().isEmpty() || strokes.isEmpty()
			|| proof.terminalSerial() != strokes.getLast().serial() || proof.terminalSerial() <= consumedSerial
			|| proof.strokes().size() > strokes.size()) return false;
		List<Stroke> recent = new ArrayList<>(strokes);
		if (!recent.subList(recent.size() - proof.strokes().size(), recent.size()).equals(proof.strokes())) return false;
		consumedSerial = proof.terminalSerial();
		pending = null;
		return true;
	}

	int size() { return strokes.size(); }
	Stroke last() { return strokes.peekLast(); }

	private Snapshot snapshot(long now, int observedMarks, int recover, Context owner) {
		return new Snapshot(now, (observedMarks & BASE_MARKS) | SwordString.Token.SWING.bit() | cues(now),
			Math.clamp(recover, 0, StringRules.MAX_RECOVER), owner, guardAt, stepAt,
			fresh(guardAt, now, StringRules.COUNTER_TICKS) && guardOwner != null && guardOwner.sameAttack(owner) ? guardReceipt : null);
	}

	private boolean append(long now, Snapshot observation) {
		strokes.addLast(new Stroke(++nextSerial, now, observation.marks(), observation.recover(), observation.guardReceipt()));
		while (strokes.size() > SwordString.MAX_LENGTH + 2) strokes.removeFirst();
		lastStroke = now;
		if (SwordString.Token.COUNTER.fits(observation.marks()) && guardAt == observation.guard()) {
			guardAt = NEVER; guardReceipt = null; guardOwner = null;
		}
		if (SwordString.Token.STEP.fits(observation.marks()) && stepAt == observation.step()) stepAt = NEVER;
		return true;
	}

	private void observe(long now, Context owner) {
		Objects.requireNonNull(owner);
		observeClock(now);
		if (context != null && !context.sameBodyAndLevel(owner)) clear();
		context = owner;
		clock = now;
	}

	private void observeClock(long now) {
		if (clock != NEVER && now < clock) clear();
		clock = now;
	}

	private void clear() {
		strokes.clear();
		context = null;
		pending = null;
		clock = lastStroke = guardAt = stepAt = NEVER;
		guardReceipt = null; guardOwner = null;
		// Serials remain monotonic across body/level/clock changes, so a stale proof cannot
		// accidentally equal a later body that happens to make the same marks at the same tick.
		consumedSerial = nextSerial;
	}

	private static boolean fresh(long tick, long now, int window) {
		return tick != NEVER && now >= tick && now - tick <= window;
	}
}
