package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ReleasedArtOwner;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Scheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.phys.Vec3;

/** Test-only observations around a connected owner's real guard, ordinary attack and counter release. */
public final class EarnedCounterCaptureFixture implements AutoCloseable {
	private final ServerPlayer owner;
	private final MastersStyleRules.Style style;
	private final Foe attacker;
	private final AuraApi.SpendHook spend;
	private final AuraApi.StringHook complete;
	private AuraGuard.Caught caught;
	private long accepted, ready, released, hitAt;
	private int payments, completions, hits;
	private double paid;
	private boolean exposed, quietWindup, recovery, ended;

	private final class Foe extends Husk {
		Foe(ServerLevel level) { super(EntityTypes.HUSK, level); }
		@Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
			float health = getHealth();
			boolean result = super.hurtServer(level, source, amount);
			var art = SwordStrings.performing();
			if (source.is(Aura.DAMAGE) && source.getEntity() == owner && art != null && art.id().equals(style.art())) {
				check(result && getHealth() < health, "The original attacker actually takes the primary counter hit");
				hits++; hitAt = level.getGameTime();
			}
			return result;
		}
	}

	/** Fixture setup never creates a guard or a cue; both must subsequently arrive through gameplay. */
	public EarnedCounterCaptureFixture(ServerPlayer player, MastersStyleRules.Style style) {
		check(style.art().equals("backdraft") || style.art().equals("rooted_parry"), "Only the two published earned counters use this fixture");
		check(ArtRules.art(style.art()).cost() == 8 && ArtRules.art(style.art()).cooldown() == 80
			&& StringRules.COUNTER_TICKS == 16, "The existing eight-Aura, eighty-tick counter and finite earning window remain unchanged");
		owner = player; this.style = style;
		checkOwner();
		SwordStrings.forget(player.getUUID()); AuraGuard.forget(player.getUUID());
		player.removeAttached(AuraAttachments.STATE);
		player.removeAttached(Momentum.MOMENTUM);
		player.removeAllEffects(); Effects.readyToHurt(player); player.setHealth(player.getMaxHealth());
		attacker = new Foe(player.level());
		attacker.addTag("wildercord.rolled"); attacker.setNoAi(true); attacker.setNoGravity(true);
		attacker.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); attacker.setHealth(200);
		attacker.snapTo(.5, 100, 2.1, 180, 0);
		check(player.level().addFreshEntity(attacker), "The original hostile attacker is actually loaded");
		spend = (who, amount, reason, backlash) -> {
			if (who != owner || !reason.equals("art:" + style.art())) return;
			checkOwner();
			check(caught != null && AuraGuard.caught(owner) == caught && caught.attacker() == attacker,
				"Payment retains the real catch and original attacker");
			accepted = owner.level().getGameTime(); payments++; paid = amount;
			var art = AuraApi.artOf(owner, style.art()).orElseThrow();
			check(!backlash && Math.abs(amount - SwordStrings.price(owner, art)) < .0001,
				"One counter payment uses the ordinary price and its normal modifiers");
			check(AuraGuard.guarding(owner) && owner.isShiftKeyDown(), "The earned guard remains raised until the accepted payment");
			check(accepted >= caught.at() && accepted - caught.at() <= StringRules.COUNTER_TICKS,
				"Ordinary input is accepted inside the original sixteen-tick earning window");
			ready = accepted + SwordStrings.rest(owner, art);
			Scheduler.later(1, () -> exposed = !AuraGuard.guarding(owner) && MastersArts.committed(owner)
				&& SwordStrings.readyAt(owner, style.art()) == ready);
			Scheduler.later(style.windup() - 1, () -> quietWindup = hits == 0 && completions == 0);
			Scheduler.later(style.windup() + style.recovery() - 1,
				() -> recovery = MastersArts.committed(owner) && !AuraGuard.guarding(owner));
			Scheduler.later(style.windup() + style.recovery(), () -> ended = !MastersArts.committed(owner));
		};
		complete = (who, art, input) -> {
			if (who != owner || !art.id().equals(style.art())) return;
			checkOwner();
			var release = MastersArts.earnedCounter(owner);
			check(release != null && release.target() == attacker && release.caughtDamage() == caught.damage()
				&& (release.route() == EarnedCounters.Route.STRUCK || release.route() == EarnedCounters.Route.CAUGHT),
				"The actual release retains its original caught attacker and damage");
			check(input.marks().size() == 1 && SwordString.Token.COUNTER.fits(input.marks().getFirst())
				&& SwordString.Token.LOW.fits(input.marks().getFirst()), "The real one-stroke low counter reaches completion");
			completions++; released = owner.level().getGameTime();
		};
		AuraApi.onSpend(spend); AuraApi.onString(complete);
	}

	/** Actual hostile melee goes through LivingEntity's damage/guard path, never a synthetic catch. */
	public void catchBlow() {
		checkOwner();
		check(AuraGuard.perfectNow(owner) && AuraGuard.caught(owner) == null,
			"The real Aura-key guard is in its perfect window before the hostile blow");
		float health = owner.getHealth();
		attacker.doHurtTarget(owner.level(), owner);
		caught = AuraGuard.caught(owner);
		check(owner.getHealth() == health && caught != null && caught.attacker() == attacker && caught.damage() > 0,
			"Actual hostile melee earns the guard receipt without damaging the owner");
		// The guard's ordinary stagger moved this body; put the same fixture back under the crosshair.
		attacker.teleportTo(.5, 100, 2.1); attacker.setDeltaMovement(Vec3.ZERO);
	}

	public void verify() {
		checkOwner();
		check(payments == 1 && completions == 1 && hits == 1, "Exactly one paid counter and one primary release completed");
		check(released == accepted + style.windup() && hitAt == released, "The primary hit and completion use the fixed server release tick");
		check(exposed && quietWindup && recovery && ended, "The genuine guard lowers for the full exposed windup and recovery, with no early art damage");
		check(SwordStrings.readyAt(owner, style.art()) == ready, "Recovery did not restart or repay the ordinary art rest");
		Wildercord.LOGGER.info("MASTERS_EARNED_COUNTER_CAPTURE art={} owner={} attacker={} caught={} accepted={} released={} paid={} ready={} hits={}",
			style.art(), owner.getUUID(), attacker.getUUID(), caught.at(), accepted, released, paid, ready, hits);
	}

	private void checkOwner() {
		check(owner.connection != null && owner.level().getServer().getPlayerList().getPlayer(owner.getUUID()) == owner
			&& ReleasedArtOwner.capture(owner).valid(), "The fixture requires the actual connected owner, without an owner-identity bypass");
	}

	@Override public void close() {
		AuraApi.spendHooks().remove(spend); AuraApi.stringHooks().remove(complete); attacker.discard();
	}

	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
