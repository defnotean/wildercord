package dev.wildercord.aura;

import dev.wildercord.cast.Cast;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** GameTest-only causal receipts. Ordinary collateral still runs, but cannot stand in for a paid inscription or resonance. */
public final class CounterSpellCapture {
	private CounterSpellCapture() {}
	public static final class Session {
		final ServerPlayer owner;
		final Object action;
		final LivingEntity firstHop;
		final long release;
		final RuneDef rune;
		final IdentityHashMap<ResonantRules.Hit, Cast> spells = new IdentityHashMap<>();
		Cast etching, seed;
		SpellPlan.EffectNode recipe;
		int inscriptions, payments, runeDamage, resonanceDamage;
		boolean closed;
		Session(ServerPlayer owner, Object action, LivingEntity firstHop, long release, RuneDef rune) {
			this.owner = owner; this.action = action; this.firstHop = firstHop; this.release = release; this.rune = rune;
		}
	}
	private record Wake(Session session, ServerPlayer owner, LivingEntity target, float mana, long ready) {}
	private static final class Offer {
		final ServerPlayer incoming;
		final LivingEntity target;
		final boolean blade;
		final Cast cast;
		ResonantRules.Pair pair;
		boolean paid;
		Offer(ServerPlayer incoming, LivingEntity target, boolean blade, Cast cast) {
			this.incoming = incoming; this.target = target; this.blade = blade; this.cast = cast;
		}
	}
	private record Resonance(Cast cast, LivingEntity target, Offer offer) {}
	private record Damage(Cast cast, LivingEntity target, Offer resonance) {}
	private record Effect(Cast cast, SpellPlan.EffectNode node) {}
	private record Reaction(Cast cast, LivingEntity target, String element) {}
	private record NativeDamage(Session session, Cast cast, LivingEntity target, DamageSource source, boolean resonance) {}
	private static Session current;
	private static Wake wake;
	private static Offer offer;
	private static Resonance resonance;
	private static Cast applying;
	private static Effect effect;
	private static Reaction reaction;
	private static Damage damage;
	private static NativeDamage nativeDamage;
	private static Throwable failure;

	public static Session begin(ServerPlayer owner, Object action, LivingEntity firstHop, long release, RuneDef rune) {
		assertIdle();
		if (current != null) throw new AssertionError("Previous spell receipt session was not closed");
		if (action == null) throw new AssertionError("Spell receipt requires the actual native Hits action");
		return current = new Session(owner, action, firstHop, release, rune);
	}
	public static void seed(Session session, Cast cast) {
		if (current != session || session.closed || session.seed != null || cast.caster != session.owner)
			throw new AssertionError("Exactly one original spell seeds this resonance session");
		require(cast.info.spell().equals(List.of(Runes.TOUCH, Runes.HARM)), "The original resonance seed carries its exact Touch/Harm recipe");
		session.seed = cast;
	}
	private static boolean live(Session session) { return session != null && session == current && !session.closed; }
	private static boolean originalHop(Session session) {
		return live(session) && session.owner.level().getGameTime() == session.release + 1
			&& CounterHitCapture.direct(session.owner, "static_riposte", session.firstHop)
			&& CounterHitCapture.action(session.owner, "static_riposte", session.firstHop, session.action) == session.action;
	}
	private static void observe(Runnable observer) { try { observer.run(); } catch (Throwable problem) { if (failure == null) failure = problem; } }
	private static void require(boolean value, String reason) { if (!value) throw new AssertionError(reason); }

	public static boolean wake(ServerPlayer owner, LivingEntity target, Supplier<Boolean> original) {
		Wake previous = wake;
		observe(() -> wake = live(current) ? new Wake(current, owner, target, Spellbooks.mana(owner), owner.getAttachedOrElse(RuneEtchings.READY, 0L)) : null);
		try { return original.get(); } finally { wake = previous; }
	}
	/** This call site exists only in RuneEtchings.wake, after the original mana/cooldown payment. */
	public static void etching(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, Runnable original) {
		Cast previous = applying;
		observe(() -> {
			applying = cast;
			Session s = current;
			if (!live(s) || wake == null || wake.session() != s || wake.owner() != s.owner || wake.target() != s.firstHop) return;
			require(originalHop(s), "Inscription must belong to the original one-tick native delayed hit");
			require(s.rune != null && s.etching == null && cast.caster == s.owner && cast.level == s.owner.level(), "Unique native inscription Cast");
			require(cast.info.runes() == 2 && cast.info.leaning().equals(s.rune.element())
				&& cast.info.spell().equals(List.of(Runes.TOUCH, s.rune)) && node.effect == s.rune
				&& cast.info.root() != null && cast.info.root().link == null && cast.info.root().groups.size() == 1
				&& cast.info.root().groups.getFirst().shape == Runes.TOUCH
				&& !cast.info.root().groups.getFirst().implicit && cast.info.root().groups.getFirst().shapeMods.isEmpty()
				&& cast.info.root().groups.getFirst().factor == 1 && node.factor == 1 && node.rank == 1 && node.mods.isEmpty()
				&& cast.info.root().groups.getFirst().effects.size() == 1
				&& cast.info.root().groups.getFirst().effects.getFirst() == node
				&& hit.entities().equals(List.of(s.firstHop)), "Exact native Touch/rune recipe and recipient");
			int price = RuneEtchingRules.price(s.rune);
			require(price > 0 && wake.mana() - Spellbooks.mana(s.owner) == price
				&& wake.ready() <= s.release + 1 && s.owner.getAttachedOrElse(RuneEtchings.READY, 0L) > s.release + 1,
				"Original inscription actually paid mana and cooldown before applying its recipe");
			s.etching = cast; s.recipe = node; s.inscriptions++;
		});
		try { original.run(); } finally { applying = previous; }
	}
	public static void effect(Cast cast, SpellPlan.EffectNode node, Runnable original) {
		Effect previous = effect;
		observe(() -> effect = new Effect(cast, node));
		try { original.run(); } finally { effect = previous; }
	}
	public static double reaction(Cast cast, LivingEntity target, String element, java.util.function.DoubleSupplier original) {
		Reaction previous = reaction;
		observe(() -> reaction = new Reaction(cast, target, element));
		try { return original.getAsDouble(); } finally { reaction = previous; }
	}
	private static boolean exactReaction(Session session, LivingEntity target) {
		return live(session) && reaction != null && reaction.cast() == session.etching
			&& reaction.target() == target && reaction.element().equals(session.rune.element());
	}
	public static boolean offer(ServerPlayer incoming, LivingEntity target, boolean blade, Cast cast, Supplier<Boolean> original) {
		Offer previous = offer;
		observe(() -> offer = live(current) ? new Offer(incoming, target, blade, cast) : null);
		try { return original.get(); } finally { offer = previous; }
	}
	/** The ledger supplies the exact Hit objects and Pair used by native offer, without synthesising either. */
	public static ResonantRules.Pair ledger(UUID target, ResonantRules.Hit hit, Supplier<ResonantRules.Pair> original) {
		Offer frame = offer;
		observe(() -> {
			Session s = current;
			if (live(s) && frame != null && !frame.blade && frame.incoming == s.owner && target.equals(frame.target.getUUID())
				&& (frame.cast == s.seed || frame.cast == s.etching) && frame.cast != null) s.spells.put(hit, frame.cast);
		});
		ResonantRules.Pair result = original.get();
		observe(() -> { if (frame != null) frame.pair = result; });
		return result;
	}
	private static boolean payment(double before, double after, double cost, String reason, AuraRules.Spend result) {
		return cost == ResonantRules.COST && "resonant_strike".equals(reason) && !result.backlash()
			&& result.paid() == cost && before - after == cost && result.left() == after;
	}
	private static boolean intendedPair(Session s, Offer frame) {
		if (!originalHop(s) || frame == null || frame.pair == null) return false;
		Cast spell = s.spells.get(frame.pair.spell());
		return spell != null && (spell == s.seed && frame.target == s.firstHop || spell == s.etching)
			&& frame.pair.spell().player().equals(s.owner.getUUID())
			&& frame.pair.spell().element().equals(spell == s.seed ? "arcane" : s.rune.element())
			&& frame.pair.blade().tick() == s.release + 1;
	}
	public static AuraRules.Spend spend(ServerPlayer player, double cost, String reason, Supplier<AuraRules.Spend> original) {
		Offer frame = offer; float[] before = {Float.NaN};
		observe(() -> before[0] = Aura.aura(player));
		AuraRules.Spend result = original.get();
		observe(() -> {
			Session s = current;
			if (!intendedPair(s, frame)) return;
			require(frame.pair.blade().player().equals(player.getUUID()) && payment(before[0], Aura.aura(player), cost, reason, result), "Native exact pair paid the real resonance charge");
			require(!frame.paid, "One native payment per resonance offer");
			frame.paid = true; s.payments++;
		});
		return result;
	}
	public static void resonant(Cast cast, LivingEntity target, Runnable original) {
		Resonance previous = resonance;
		observe(() -> resonance = intendedPair(current, offer) && offer.paid && offer.target == target ? new Resonance(cast, target, offer) : null);
		try { original.run(); } finally { resonance = previous; }
	}
	public static boolean damage(Cast cast, LivingEntity target, Supplier<Boolean> original) {
		Damage previous = damage;
		observe(() -> damage = new Damage(cast, target, resonance != null && resonance.cast() == cast && resonance.target() == target ? resonance.offer() : null));
		try { return original.get(); } finally { damage = previous; }
	}
	/** Untyped native damage cannot borrow a typed caller's Cast while callbacks are nested. */
	public static boolean identity(Object castIdentity, Supplier<Boolean> original) {
		Damage previous = damage;
		observe(() -> { if (damage == null || damage.cast().identity() != castIdentity) damage = null; });
		try { return original.get(); } finally { damage = previous; }
	}
	/** Wraps the final unchanged non-player hurtServer invocation and retains its exact native DamageSource. */
	public static boolean nativeDamage(LivingEntity target, ServerLevel level, DamageSource source, Supplier<Boolean> original) {
		NativeDamage previous = nativeDamage;
		observe(() -> {
			nativeDamage = null;
			Session s = current;
			if (!live(s) || damage == null || damage.target() != target || level != s.owner.level() || source.getEntity() != s.owner) return;
			boolean resonant = damage.resonance() != null && damage.resonance().paid && intendedPair(s, damage.resonance());
			if (damage.cast() == s.etching || resonant) nativeDamage = new NativeDamage(s, damage.cast(), target, source, resonant);
		});
		try {
			boolean result = original.get();
			observe(() -> { if (result && nativeDamage != null) { if (nativeDamage.resonance()) nativeDamage.session().resonanceDamage++; else nativeDamage.session().runeDamage++; } });
			return result;
		} finally { nativeDamage = previous; }
	}
	public static boolean matches(Session session, LivingEntity target, DamageSource source) {
		return live(session) && nativeDamage != null && nativeDamage.session() == session
			&& nativeDamage.target() == target && nativeDamage.source() == source;
	}
	public static boolean inEtching(Session session, LivingEntity target) {
		return live(session) && applying != null && applying == session.etching && effect != null
			&& effect.cast() == session.etching && effect.node() == session.recipe
			&& exactReaction(session, target) && dev.wildercord.cast.Effects.applyingCast() == session.etching
			&& target == session.firstHop && originalHop(session);
	}
	public static boolean paidPair(Session session, ServerPlayer striker, LivingEntity target) {
		return intendedPair(session, offer) && offer.paid && offer.target == target && offer.pair.blade().player().equals(striker.getUUID());
	}
	public static void verify(Session session, boolean resonanceExpected, boolean damageExpected) {
		assertIdle(); require(live(session), "Current causal receipt session");
		if (session.rune != null) require(session.inscriptions == 1 && session.etching != null, "Exactly one actual paid native inscription");
		if (resonanceExpected) require(session.payments == 1 && session.resonanceDamage == 1, "Exact native pair, real payment and one resonant damage callback");
		if (damageExpected) require(session.runeDamage > 0, "Only actual damage from the captured rune Cast satisfies the fixture");
	}
	public static void close(Session session) {
		if (session == null) return;
		session.closed = true; session.spells.clear(); session.etching = null; session.seed = null; session.recipe = null;
		if (current == session) current = null;
	}
	/** Adversarial tests call the real receipt predicates and wrappers, never the production damage method. */
	static void negativeControls(ServerPlayer owner, LivingEntity target, Object action) {
		assertIdle(); require(current == null, "Negative controls run outside native sessions");
		Session s = new Session(owner, action, target, owner.level().getGameTime(), Runes.HARM);
		Cast expected = new Cast(owner), foreign = new Cast(owner);
		DamageSource source = owner.level().damageSources().source(net.minecraft.world.damagesource.DamageTypes.MAGIC, owner, owner);
		DamageSource otherSource = owner.level().damageSources().source(net.minecraft.world.damagesource.DamageTypes.MAGIC, owner, owner);
		Session stale = new Session(owner, action, target, s.release, Runes.HARM);
		int[] calls = {0};
		try {
			current = s; s.etching = expected;
			boolean result = damage(expected, target, () -> nativeDamage(target, owner.level(), source, () -> {
				calls[0]++; require(matches(s, target, source), "Control matches the exact Cast/body/source/session");
				require(!matches(stale, target, source) && !matches(s, owner, source) && !matches(s, target, otherSource),
					"Stale session, foreign recipient and equivalent-but-distinct damage sources do not qualify");
				return true;
			}));
			require(result && calls[0] == 1 && s.runeDamage == 1, "Original result and exactly one original call retained");
			damage(foreign, target, () -> nativeDamage(target, owner.level(), source, () -> {
				calls[0]++; require(!matches(s, target, source), "An unrelated Cast by the same owner cannot satisfy the rune receipt"); return false;
			}));
			require(calls[0] == 2 && s.runeDamage == 1, "Incidental damage is neither suppressed nor counted");
			damage(expected, target, () -> nativeDamage(target, owner.level(), source, () -> {
				calls[0]++;
				damage(foreign, target, () -> nativeDamage(target, owner.level(), source, () -> {
					require(!matches(s, target, source), "Nested unrelated damage cannot borrow an outer rune receipt"); return true;
				}));
				require(matches(s, target, source), "Nested invocation restores its exact caller"); return false;
			}));
			damage(expected, target, () -> identity(new Object(), () -> nativeDamage(target, owner.level(), source, () -> {
				require(!matches(s, target, source), "Untyped nested damage cannot borrow a typed caller's Cast"); return true;
			})));
			require(s.runeDamage == 1, "Untyped ordinary damage remains uncredited");
			damage(expected, target, () -> identity(expected.identity(), () -> nativeDamage(target, owner.level(), source, () -> {
				require(matches(s, target, source), "Exact original native Cast identity retains its typed receipt"); return false;
			})));
			double reacted = reaction(expected, target, "arcane", () -> {
				require(exactReaction(s, target), "Original native reaction Cast/target/element qualifies");
				reaction(foreign, target, "arcane", () -> { require(!exactReaction(s, target), "Nested foreign Cast reaction cannot borrow its outer inscription"); return 2; });
				reaction(expected, owner, "arcane", () -> { require(!exactReaction(s, target), "Foreign reaction recipient rejected"); return 3; });
				reaction(expected, target, "storm", () -> { require(!exactReaction(s, target), "Foreign reaction element rejected"); return 4; });
				require(exactReaction(s, target), "Nested reaction restores its exact outer frame"); return 1.25;
			});
			require(reacted == 1.25 && !exactReaction(s, target), "Native reaction return value preserved and frame cleared");
			RuntimeException originalFailure = new RuntimeException("native sentinel");
			try {
				damage(expected, target, () -> nativeDamage(target, owner.level(), source, () -> { calls[0]++; throw originalFailure; }));
				throw new AssertionError("Native exception must propagate");
			} catch (RuntimeException actual) { require(actual == originalFailure, "Original throwable identity retained"); }
			require(calls[0] == 4 && s.runeDamage == 1, "Exceptions and false returns cannot create damage receipts");
			require(!matches(s, target, source), "Receipt cannot escape the final native damage invocation");
			require(payment(20, 16, 4, "resonant_strike", new AuraRules.Spend(16, 4, false)), "Paid control");
			require(!payment(20, 20, 4, "resonant_strike", new AuraRules.Spend(20, 4, false))
				&& !payment(20, 16, 4, "other", new AuraRules.Spend(16, 4, false))
				&& !payment(20, 16, 4, "resonant_strike", new AuraRules.Spend(16, 3, false))
				&& !payment(20, 16, 4, "resonant_strike", new AuraRules.Spend(16, 4, true))
				&& !payment(20, 15, 4, "resonant_strike", new AuraRules.Spend(15, 4, false)),
				"No unpaid, foreign-reason, partial, backlash or wrong-delta resonance receipt");
			ResonantRules.Hit genuine = new ResonantRules.Hit(owner.getUUID(), "arcane", 1, s.release, false);
			ResonantRules.Hit copy = new ResonantRules.Hit(owner.getUUID(), "arcane", 1, s.release, false);
			s.spells.put(genuine, expected);
			require(s.spells.get(copy) == null && s.spells.get(genuine) == expected, "Equivalent ledger values cannot impersonate the original native Hit object");
			close(s); require(!matches(s, target, source), "Closed session cannot be reused");
			assertIdle();
		} finally { close(s); }
	}

	public static void assertIdle() {
		if (failure != null) throw new AssertionError("Spell observer failed without replacing native behavior", failure);
		if (wake != null || offer != null || resonance != null || applying != null || effect != null || reaction != null || damage != null || nativeDamage != null)
			throw new AssertionError("Native spell observer leaked an invocation frame");
	}
}
