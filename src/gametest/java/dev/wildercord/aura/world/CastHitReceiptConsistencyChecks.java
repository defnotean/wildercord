package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.aura.MastersArtRules;
import dev.wildercord.aura.MastersArts;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.CastReceiptWardProbe;
import dev.wildercord.cast.DeathsDoor;
import dev.wildercord.cast.CastLock;
import dev.wildercord.cast.Charging;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Statuses;
import dev.wildercord.content.WildercordEffects;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.server.MinecraftServer;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Actual close BREAK_CAST releases and scheduled player Driving Cuts, beyond the isolated receipt seam.
 * Targets begin real charges; accepted callbacks, native defences and both interruption cooldowns remain live.
 * The paused Master isolates release admission, not pursuit movement. The Driving Cut owner is the real client.
 */
public final class CastHitReceiptConsistencyChecks {
	private static final float HEALTH = 200;
	private static final List<EquipmentSlot> EQUIPMENT = List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
	private enum Route { BREAK_CAST, DRIVING_CUT }
	private enum Case { HEALTH, FULL_ABSORPTION, MANA_SKIN, REVERSAL, TOTEM, GUARD, STEP, WARD,
		RESISTANCE, REJECTED, REPLACED, EQUAL_TOKEN, NEW_CHARGE, IDLE, WINDUP_REPLACEMENT }
	private static boolean registered;
	private static CastHitReceiptConsistencyChecks active;
	private SwordMaster master;
	private ServerPlayer target;
	private UUID actorId, recipientId;
	private long recipientReadyAt;
	private LivingEntity attacker;
	private Vec3 origin, positionBefore;
	private Route route;
	private Case probe;
	private WildercordAttachments.Charge beforeCharge, callbackCharge;
	private DamageSource beforeSource, acceptedSource;
	private float beforeHealth, beforeAbsorption, beforeMana, nativeHealthAfter;
	private int callbacks;
	private long begun, drivingReady, impactAt;
	private boolean releaseFinished;

	static List<String> expectedCases() {
		var cases = new java.util.ArrayList<String>();
		for (Route route : Route.values()) for (Case sample : Case.values()) cases.add(route.name() + "_" + sample.name());
		cases.addAll(List.of("SHARED_BREAK_CAST_TO_DRIVING_CUT", "SHARED_DRIVING_CUT_TO_BREAK_CAST",
			"IDLE_SEAL_RECOVERY", "CHARGED_SEAL_RECOVERY", "NONPLAYER"));
		return List.copyOf(cases);
	}

	/** Single-client aggregate keeps all fifteen NPC releases; the mandatory paired suite owns the PvP roles. */
	void run(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			runMatrix(context, world.getServer(), false, ignored -> {}, ignored -> {});
		}
	}

	/** Both IDs must be distinct genuine connected bodies; no player-registry or charge-state substitution. */
	public void runConnectedPair(ClientGameTestContext context, TestServerContext server, UUID actorId, UUID recipientId, Consumer<String> observed, Consumer<String> completed) {
		check(!actorId.equals(recipientId), "Driving Cut has two distinct connected roles");
		this.actorId = actorId; this.recipientId = recipientId;
		runMatrix(context, server, true, observed, completed);
	}

	private void runMatrix(ClientGameTestContext context, TestServerContext server, boolean paired, Consumer<String> observed, Consumer<String> completed) {
		registerCallbacks();
		try {
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false")) server.runCommand(command);
			server.runOnServer(s -> {
				ServerPlayer player = actor(s);
				origin = new Vec3(player.getBlockX() + .5, 181, player.getBlockZ() + .5);
				for (int x = -15; x <= 15; x++) for (int z = -15; z <= 15; z++)
					player.level().setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.STONE.defaultBlockState());
				prepare(player); place(player, 8, 3, 180);
				check(actor(s).level() == recipient(s).level(), "Both connected roles share the native encounter world");
			});
			for (Route selected : paired ? Route.values() : new Route[] {Route.BREAK_CAST}) for (Case selectedCase : Case.values()) {
				freshRecipient(server, selected, selectedCase, true);
				try {
					release(context, server, true);
					server.runOnServer(s -> verify());
					if (selectedCase == Case.STEP) server.waitFor(s -> target.level().getGameTime() >= impactAt
						+ Math.max(AuraRules.STEP_TICKS, AuraRules.STEP_GUARD_TICKS) + 2, Math.max(AuraRules.STEP_TICKS, AuraRules.STEP_GUARD_TICKS) + 5);
					observed.accept(selected.name() + "_" + selectedCase.name());
				} finally { server.runOnServer(s -> clearTarget()); }
				completed.accept(selected.name() + "_" + selectedCase.name());
			}
			if (paired) {
				sharedImmunity(context, server, Route.BREAK_CAST, () -> observed.accept("SHARED_BREAK_CAST_TO_DRIVING_CUT")); completed.accept("SHARED_BREAK_CAST_TO_DRIVING_CUT");
				sharedImmunity(context, server, Route.DRIVING_CUT, () -> observed.accept("SHARED_DRIVING_CUT_TO_BREAK_CAST")); completed.accept("SHARED_DRIVING_CUT_TO_BREAK_CAST");
				idleSealRecovery(context, server, () -> observed.accept("IDLE_SEAL_RECOVERY")); completed.accept("IDLE_SEAL_RECOVERY");
				chargedSealRecovery(context, server, () -> observed.accept("CHARGED_SEAL_RECOVERY")); completed.accept("CHARGED_SEAL_RECOVERY");
				nonplayer(context, server); observed.accept("NONPLAYER"); completed.accept("NONPLAYER");
			}
		} finally { active = null; server.runOnServer(s -> clearTarget()); }
	}

	private ServerPlayer actor(MinecraftServer server) { return connected(server, actorId); }
	private ServerPlayer recipient(MinecraftServer server) { return connected(server, recipientId == null ? actorId : recipientId); }
	private static ServerPlayer connected(MinecraftServer server, UUID id) {
		ServerPlayer player = id == null ? server.getPlayerList().getPlayers().getFirst() : server.getPlayerList().getPlayer(id);
		check(player != null && server.getPlayerList().getPlayer(player.getUUID()) == player && player.connection != null
			&& player.connection.player == player && player.isAlive(), "The native fixture retains its exact connected role body");
		return player;
	}
	private void freshRecipient(TestServerContext server, Route nextRoute, Case nextCase, boolean chargeNow) {
		server.waitFor(s -> actor(s).connection.hasClientLoaded() && recipient(s).connection.hasClientLoaded()
			&& s.overworld().getGameTime() >= recipientReadyAt && CastReceiptWardProbe.active(recipient(s)).isEmpty()
			&& (nextCase != Case.REVERSAL || DeathsDoor.resting(recipient(s)) == 0), DeathsDoor.REST + 5);
		server.runOnServer(s -> {
			route = nextRoute; probe = nextCase; target = recipient(s); prepare(target); place(target, 0, 3, 180);
			check(CastReceiptWardProbe.active(target).isEmpty(), "No previous UUID-scoped ward can mask this release or defence");
			if (nextCase == Case.MANA_SKIN) { dress(target); reconcileEquipment(target); }
			if (chargeNow) charge(target);
		});
	}

	private static void registerCallbacks() {
		if (registered) return;
		registered = true;
		var nativePhase = Wildercord.id("cast_hit_consistency_native_wound");
		ServerLivingEntityEvents.AFTER_DAMAGE.addPhaseOrdering(nativePhase, net.fabricmc.fabric.api.event.Event.DEFAULT_PHASE);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(nativePhase, (entity, source, base, taken, blocked) -> {
			CastHitReceiptConsistencyChecks fixture = active;
			if (fixture != null && entity == fixture.target && source.getEntity() == fixture.attacker
					&& source.getDirectEntity() == fixture.attacker && source.is(Aura.DAMAGE))
				fixture.nativeHealthAfter = fixture.target.getHealth();
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			CastHitReceiptConsistencyChecks fixture = active;
			if (fixture == null || entity != fixture.target || source.getEntity() != fixture.attacker
				|| source.getDirectEntity() != fixture.attacker || !source.is(Aura.DAMAGE)) return;
			fixture.acceptedSource = source;
			fixture.callbacks++;
			if (fixture.probe == Case.REPLACED || fixture.probe == Case.NEW_CHARGE) {
				// A different real request issued by a damage reaction must not become this earlier hit's victim.
				Charging.interrupt(fixture.target);
				Charging.request(fixture.target, 0, true);
				fixture.callbackCharge = fixture.target.getAttached(WildercordAttachments.CHARGE);
				check(fixture.callbackCharge != null && fixture.callbackCharge != fixture.beforeCharge,
					"Damage callback obtains a genuinely new charge through Charging.request");
			} else if (fixture.probe == Case.EQUAL_TOKEN) {
				// Simulate an add-on replacing an attachment by an equal record, not a new synthetic initial charge.
				var held = fixture.target.getAttached(WildercordAttachments.CHARGE);
				fixture.callbackCharge = held.withStage(held.stage(), held.stageTime());
				check(fixture.callbackCharge != held && fixture.callbackCharge.equals(held),
					"The callback token is structurally equal but a different held-charge identity");
				fixture.target.setAttached(WildercordAttachments.CHARGE, fixture.callbackCharge);
				check(fixture.target.getAttached(WildercordAttachments.CHARGE) == fixture.callbackCharge, "Native attachment replacement retains the distinct callback token");
			}
		});
	}

	private void release(ClientGameTestContext context, TestServerContext server, boolean configure) {
		if (route == Route.BREAK_CAST) {
			server.runOnServer(s -> {
				if (actor(s) != target) place(actor(s), 8, 3, 180);
				startMaster(); begun = target.level().getGameTime();
			});
			server.waitFor(s -> target.level().getGameTime() >= begun + 21, 25);
			server.runOnServer(s -> {
				master.setTarget(target); master.customServerAiStep(target.level());
				check(master.attackAnimation() == MastersRules.Move.BREAK_CAST.ordinal() + 1 && master.state(AuraFighter.WINDUP),
					"A real held spell inside four blocks selects the original BREAK_CAST tell");
				begun = target.level().getGameTime();
				check(close(target.getHealth(), HEALTH), "The original Master windup is harmless");
			});
			server.waitFor(s -> {
				if (target.level().getGameTime() < begun + MastersRules.Move.BREAK_CAST.tell) return false;
				check(target.level().getGameTime() == begun + MastersRules.Move.BREAK_CAST.tell, "Observe the original Master exact release frame");
				beforeImpact(configure);
				master.customServerAiStep(target.level());
				check(!master.state(AuraFighter.WINDUP), "The accepted original Master releases its pending hit");
				active = null;
				return true;
			}, MastersRules.Move.BREAK_CAST.tell + 5);
		} else {
			server.waitFor(s -> s.overworld().getGameTime() >= drivingReady,
				MastersArtRules.DRIVING_CUT.rest() + 5);
			server.runOnServer(s -> {
				ServerPlayer player = actor(s);
				prepareActor(player); attacker = player; releaseFinished = false;
				check(ArtKit.harmable(player, target) && player.hasLineOfSight(target), "The real client may target this player through the native art selector");
				// Same deadline and FIFO as the real release: defence starts before impact, Step motion after it.
				Scheduler.later(MastersArtRules.DRIVING_CUT.windup(), () -> beforeImpact(configure));
				check(MastersArts.activate(player, 2), "The real connected client accepts Driving Cut through its public entrypoint");
				check(close((float) Aura.aura(player), 100 - (float) MastersArtRules.DRIVING_CUT.cost()), "Driving Cut commits its unchanged eighteen-Aura price");
				drivingReady = s.overworld().getGameTime() + MastersArtRules.DRIVING_CUT.rest();
				check(close(target.getHealth(), HEALTH), "The scheduled Driving Cut windup is harmless");
				Scheduler.later(MastersArtRules.DRIVING_CUT.windup(), () -> { active = null; releaseFinished = true; });
			});
			server.waitFor(s -> releaseFinished, MastersArtRules.DRIVING_CUT.windup() + 5);
		}
	}

	private void beforeImpact(boolean configure) {
		impactAt = target.level().getGameTime();
		check(target != null && target.isAlive() && target.level().getBlockState(target.blockPosition().below()).isSolidRender(),
			"A live target stands on the real platform before release");
		if (configure) switch (probe) {
			case FULL_ABSORPTION -> {
				target.setAbsorptionAmount(64);
				check(close(target.getAbsorptionAmount(), 64), "MAX_ABSORPTION permits all fixture hearts");
			}
			case MANA_SKIN -> {
				target.setAttached(WildercordAttachments.CIRCLES, 3);
				check(target.getArmorValue() == 20 && target.getAttributeValue(Attributes.ARMOR_TOUGHNESS) >= 12,
					"Native equipment reconciliation installed real Protection IV netherite before Charging");
			}
			case REVERSAL -> {
				target.setHealth(1); CastEngine.cast(target, SpellCompiler.compile(List.of(Runes.SELF, Runes.REVERSAL)).root());
			}
			case TOTEM -> { target.setHealth(1); target.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING)); }
			case GUARD, STEP -> {
				target.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
				target.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("gale", AuraRules.FORM, 0, 80, 0));
				float aura = (float) Aura.aura(target);
				if (probe == Case.GUARD) {
					String eligibility = route + " now=" + target.level().getGameTime() + ", yaw=" + target.getYRot()
						+ ", head=" + target.getYHeadRot() + ", view=" + target.getViewVector(1) + ", weapon=" + Aura.holdsWeapon(target)
						+ ", aura=" + aura + ", state=" + Aura.state(target) + ", sneak=" + target.isShiftKeyDown()
						+ ", charging=" + target.hasAttached(WildercordAttachments.CHARGE);
					check(AuraGuard.faces(target, attacker.position()), "The native guard view faces the attacker: " + eligibility);
					check(AuraGuard.raise(target), "The public Aura Guard entrypoint accepts the rested, equipped target: " + eligibility);
					check(AuraGuard.perfectNow(target), "The paid native guard opens its real perfect window: " + eligibility + ", after=" + Aura.state(target));
				} else check(AuraStep.step(target), "The public Aura Step entrypoint admits its genuine swept path");
				check(Aura.aura(target) < aura, "The actual defensive action pays its native Aura cost");
			}
			case WARD -> {
				// Foresight turns at most twelve away; native Warded IV brings the Master's unchanged 28 under that cap.
				target.addEffect(new MobEffectInstance(WildercordEffects.WARDED, 100, 3));
				CastEngine.cast(target, SpellCompiler.compile(List.of(Runes.SELF, Runes.FORESIGHT)).root());
			}
			case RESISTANCE -> check(target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 4)), "Native Resistance V is installed");
			case REJECTED -> target.setPermanentlyInvulnerable(true);
			case NEW_CHARGE, IDLE -> Charging.interrupt(target);
			case WINDUP_REPLACEMENT -> { Charging.interrupt(target); Charging.request(target, 0, true); }
			default -> { }
		}
		beforeCharge = target.getAttached(WildercordAttachments.CHARGE);
		if (probe != Case.NEW_CHARGE && probe != Case.IDLE) check(beforeCharge != null, "A real held charge survives until the pre-impact snapshot");
		beforeHealth = target.getHealth(); beforeAbsorption = target.getAbsorptionAmount(); beforeMana = Spellbooks.mana(target);
		nativeHealthAfter = Float.NaN;
		beforeSource = target.getLastDamageSource(); positionBefore = target.position();
		callbackCharge = null; acceptedSource = null; callbacks = 0; active = this;
	}

	private void verify() {
		String note = route + "/" + probe + " health=" + target.getHealth() + ", absorption=" + target.getAbsorptionAmount()
			+ ", mana=" + Spellbooks.mana(target) + ", callbacks=" + callbacks + ", locked=" + CastLock.locked(target);
		Wildercord.LOGGER.info("[cast-hit-consistency] {}", note);
		boolean prevented = switch (probe) { case GUARD, STEP, WARD, RESISTANCE, REJECTED -> true; default -> false; };
		boolean replacement = probe == Case.REPLACED || probe == Case.EQUAL_TOKEN || probe == Case.NEW_CHARGE;
		if (prevented) {
			check(close(target.getHealth(), beforeHealth) && close(target.getAbsorptionAmount(), beforeAbsorption)
				&& close(Spellbooks.mana(target), beforeMana), "A genuine prevention spends no health, absorption or Mana Skin: " + note);
			check(target.getAttached(WildercordAttachments.CHARGE) == beforeCharge && !CastLock.locked(target),
				"A true prevention preserves the precise held spell without a follow-up lock: " + note);
			if (probe == Case.GUARD) check(!AuraGuard.perfectNow(target) && AuraGuard.caught(target) != null,
				"The actual parry consumes its perfect window and records the caught blow: " + note);
			if (probe == Case.WARD) check(target.position().distanceToSqr(positionBefore) > .5, "Native Foresight performs its real sidestep: " + note);
			if (probe != Case.RESISTANCE) check(target.getLastDamageSource() == beforeSource, "A denied blow cannot replace the last accepted source: " + note);
			return;
		}
		check(target.getLastDamageSource() != null && target.getLastDamageSource().is(Aura.DAMAGE)
			&& target.getLastDamageSource().getEntity() == attacker && target.getLastDamageSource().getDirectEntity() == attacker,
			"The accepted native hit retains both owner and direct-source identity: " + note);
		if (probe == Case.FULL_ABSORPTION) check(close(target.getHealth(), beforeHealth) && target.getAbsorptionAmount() < beforeAbsorption,
			"A zero-health-loss release genuinely consumes absorption: " + note);
		else if (probe == Case.MANA_SKIN) {
			float netWound = beforeHealth - target.getHealth(), paidRecovery = (beforeMana - Spellbooks.mana(target)) / 2;
			float nativeWound = beforeHealth - nativeHealthAfter;
			float expectedRecovery = nativeWound * .2F >= .25F ? nativeWound * .2F : 0;
			check(Float.isFinite(nativeHealthAfter) && nativeWound > 0 && netWound > 0
				&& close(paidRecovery, expectedRecovery) && close(netWound, nativeWound - expectedRecovery),
				"Mana Skin repays twenty percent of the native wound only above its existing recovery threshold: " + note
					+ ", nativeWound=" + nativeWound + ", expectedRecovery=" + expectedRecovery);
		}
		else if (probe == Case.REVERSAL) check(target.isAlive() && close(target.getHealth(), HEALTH * .5F), "Real Reversal restores half health after lethal native damage: " + note);
		else if (probe == Case.TOTEM) check(target.isAlive() && close(target.getHealth(), 1) && target.getOffhandItem().isEmpty()
			&& target.hasEffect(MobEffects.ABSORPTION), "An actual consumed totem conceals a lethal native wound: " + note);
		else check(target.getHealth() < beforeHealth, "The ordinary native release damages health: " + note);
		if (replacement) {
			check(callbacks == 1 && acceptedSource == target.getLastDamageSource() && callbackCharge != null
				&& target.getAttached(WildercordAttachments.CHARGE) == callbackCharge && !CastLock.locked(target),
				"This accepted hit cannot interrupt or seal a different post-impact charge token: " + note);
		} else {
			check(!target.hasAttached(WildercordAttachments.CHARGE), "Accepted damage consumes the exact live pre-impact charge: " + note);
			check(CastLock.locked(target) == (route == Route.DRIVING_CUT), "Only Driving Cut preserves its existing twenty-tick cast seal: " + note);
		}
	}

	private void sharedImmunity(ClientGameTestContext context, TestServerContext server, Route first, Runnable observe) {
		freshRecipient(server, first, Case.HEALTH, true);
		try {
			release(context, server, true);
			long[] interruptedAt = new long[1];
			server.runOnServer(s -> { verify(); interruptedAt[0] = impactAt; discardMaster(); });
			server.waitFor(s -> target.level().getGameTime() >= interruptedAt[0] + MastersArtRules.INTERRUPT_TICKS + 1,
				MastersArtRules.INTERRUPT_TICKS + 5);
			server.runOnServer(s -> {
				check(!CastLock.locked(target), "The original short seal expires without clearing shared Statuses immunity");
				target.setHealth(HEALTH); place(target, 0, 3, 180); charge(target);
				route = first == Route.BREAK_CAST ? Route.DRIVING_CUT : Route.BREAK_CAST;
			});
			release(context, server, false);
			server.runOnServer(s -> {
				check(target.level().getGameTime() - interruptedAt[0] < Statuses.INTERRUPT_GAP, "Both real releases occur inside the same untouched 160-tick immunity");
				check(target.getHealth() < beforeHealth && target.getAttached(WildercordAttachments.CHARGE) == beforeCharge && !CastLock.locked(target),
					"An accepted " + route + " cannot bypass the shared immunity earned by " + first);
				Effects.withSource(attacker, () -> check(!Statuses.interrupt(target), "Neither cross-route strike resets the existing shared immunity"));
				Wildercord.LOGGER.info("[cast-hit-consistency] shared immunity {} -> {} preserved", first, route);
			});
			observe.run(); // Witness the protected charge before its deliberate end.
			server.runOnServer(s -> Charging.interrupt(target));
			server.waitFor(s -> target.level().getGameTime() >= interruptedAt[0] + Statuses.INTERRUPT_GAP - 6, Statuses.INTERRUPT_GAP + 5);
			server.runOnServer(s -> {
				charge(target); beforeCharge = target.getAttached(WildercordAttachments.CHARGE);
				check(beforeCharge.start() >= interruptedAt[0] + Statuses.INTERRUPT_GAP - 6,
					"A fresh real edge-probe charge tests retained immunity without bypassing normal overchannel");
			});
			server.waitFor(s -> {
				if (target.level().getGameTime() < interruptedAt[0] + Statuses.INTERRUPT_GAP - 1) return false;
				Effects.withSource(attacker, () -> check(target.level().getGameTime() == interruptedAt[0] + Statuses.INTERRUPT_GAP - 1
					&& !Statuses.interrupt(target) && target.getAttached(WildercordAttachments.CHARGE) == beforeCharge,
					"The exact native held spell remains protected through tick 159"));
				return true;
			}, Statuses.INTERRUPT_GAP + 5);
			server.waitFor(s -> {
				if (target.level().getGameTime() < interruptedAt[0] + Statuses.INTERRUPT_GAP) return false;
				Effects.withSource(attacker, () -> check(target.level().getGameTime() == interruptedAt[0] + Statuses.INTERRUPT_GAP
					&& Statuses.interrupt(target) && !target.hasAttached(WildercordAttachments.CHARGE),
					"A refused cross-route strike does not extend the original immunity beyond tick 160"));
				return true;
			}, 5);
		} finally { server.runOnServer(s -> clearTarget()); }
	}

	private void idleSealRecovery(ClientGameTestContext context, TestServerContext server, Runnable observe) {
		freshRecipient(server, Route.DRIVING_CUT, Case.IDLE, true);
		try {
			release(context, server, true);
			server.runOnServer(s -> verify());
			observe.run(); // Witness the live seal before exact19/20 boundaries and subsequent cleanup.
			server.waitFor(s -> {
				if (target.level().getGameTime() < impactAt + MastersArtRules.INTERRUPT_TICKS - 1) return false;
				check(target.level().getGameTime() == impactAt + MastersArtRules.INTERRUPT_TICKS - 1 && CastLock.locked(target),
					"The original idle-player seal still holds on tick nineteen");
				return true;
			}, MastersArtRules.INTERRUPT_TICKS + 5);
			server.waitFor(s -> {
				if (target.level().getGameTime() < impactAt + MastersArtRules.INTERRUPT_TICKS) return false;
				check(target.level().getGameTime() == impactAt + MastersArtRules.INTERRUPT_TICKS && !CastLock.locked(target),
					"The idle-player Driving Cut seal expires on its unchanged exact twentieth tick");
				Effects.withSource(attacker, () -> CastLock.lock(target, MastersArtRules.INTERRUPT_TICKS));
				check(!CastLock.locked(target), "The unchanged CastLock recovery rejects a second idle seal");
				charge(target);
				Effects.withSource(attacker, () -> check(Statuses.interrupt(target), "An idle-player seal consumes no shared charge-interruption immunity"));
				return true;
			}, 5);
		} finally { server.runOnServer(s -> clearTarget()); }
	}


	private void chargedSealRecovery(ClientGameTestContext context, TestServerContext server, Runnable observe) {
		server.waitFor(s -> s.overworld().getGameTime() >= drivingReady, MastersArtRules.DRIVING_CUT.rest() + 5);
		long[] sealedAt = new long[1];
		freshRecipient(server, Route.DRIVING_CUT, Case.HEALTH, false);
		server.runOnServer(s -> {
			Effects.withSource(actor(s), () -> CastLock.lock(target, MastersArtRules.INTERRUPT_TICKS));
			sealedAt[0] = target.level().getGameTime();
			check(CastLock.locked(target), "A genuine earlier idle seal starts its original native recovery window");
		});
		try {
			server.waitFor(s -> target.level().getGameTime() >= sealedAt[0] + MastersArtRules.INTERRUPT_TICKS + 1,
				MastersArtRules.INTERRUPT_TICKS + 5);
			server.runOnServer(s -> {
				check(!CastLock.locked(target), "The old seal is unlocked before a real new spell starts");
				charge(target);
			});
			release(context, server, false);
			server.runOnServer(s -> {
				check(target.level().getGameTime() < sealedAt[0] + MastersArtRules.INTERRUPT_TICKS + CastLock.PLAYER_RECOVERY,
					"The new actual Driving Cut arrives inside the unchanged forty-tick seal recovery");
				check(target.getHealth() < beforeHealth && target.getAttached(WildercordAttachments.CHARGE) == beforeCharge && !CastLock.locked(target),
					"Recovery refuses both a fresh seal and charge interruption despite accepted Driving Cut damage");
			});
			observe.run(); // Protected charge is still live; the following immunity probe remains mandatory.
			server.runOnServer(s -> {
				Effects.withSource(attacker, () -> check(Statuses.interrupt(target), "Refused recovery consumes none of the shared 160-tick interruption immunity"));
				Wildercord.LOGGER.info("[cast-hit-consistency] native cast-lock recovery preserves charge and shared immunity");
			});
		} finally { server.runOnServer(s -> clearTarget()); }
	}

	private void nonplayer(ClientGameTestContext context, TestServerContext server) {
		LivingEntity[] mob = new LivingEntity[1];
		String[] snapshots = new String[3];
		long[] releasedAt = {-1};
		ItemStack[] blade = new ItemStack[1];
		Vec3[] aim = new Vec3[1];
		server.waitFor(s -> s.overworld().getGameTime() >= drivingReady, MastersArtRules.DRIVING_CUT.rest() + 5);
		server.runOnServer(s -> {
			ServerPlayer player = actor(s); prepareActor(player); releaseFinished = false;
			var husk = EntityTypes.HUSK.create(player.level(), EntitySpawnReason.COMMAND);
			check(husk != null, "The ordinary non-player target is constructible");
			husk.setNoAi(true); husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH); husk.setHealth(HEALTH);
			// An ordinary fixture must not roll Runebound on entity load and refill above the authored health baseline.
			husk.addTag("wildercord.rolled");
			husk.snapTo(origin.x, origin.y, origin.z + 3, 180, 0); player.level().addFreshEntity(husk); mob[0] = husk;
			blade[0] = player.getMainHandItem(); aim[0] = ArtKit.flat(player);
			snapshots[0] = nonplayerSnapshot(player, husk, blade[0], aim[0]);
			Wildercord.LOGGER.info("[cast-hit-consistency] nonplayer admission {}", snapshots[0]);
			check(husk.getHealth() == HEALTH && husk.getMaxHealth() == HEALTH && !husk.hasAttached(WildercordAttachments.RUNEBOUND),
				"Entity admission preserves the exact ordinary target baseline: " + snapshots[0]);
			Scheduler.later(MastersArtRules.DRIVING_CUT.windup(), () -> {
				snapshots[1] = nonplayerSnapshot(player, husk, blade[0], aim[0]);
				Wildercord.LOGGER.info("[cast-hit-consistency] nonplayer before release {}", snapshots[1]);
			});
			check(MastersArts.activate(player, 2), "Actual Driving Cut accepts an ordinary non-player target");
			Scheduler.later(MastersArtRules.DRIVING_CUT.windup(), () -> {
				releasedAt[0] = player.level().getGameTime();
				snapshots[2] = nonplayerSnapshot(player, husk, blade[0], aim[0]);
				Wildercord.LOGGER.info("[cast-hit-consistency] nonplayer after release {}", snapshots[2]);
				releaseFinished = true;
			});
		});
		server.waitFor(s -> releaseFinished, MastersArtRules.DRIVING_CUT.windup() + 5);
		server.runOnServer(s -> {
			ServerPlayer player = actor(s);
			String observed = nonplayerSnapshot(player, mob[0], blade[0], aim[0]);
			String note = "admission={" + snapshots[0] + "}, before={" + snapshots[1] + "}, release={" + snapshots[2]
				+ "}, observed={" + observed + "}, observationGap=" + (player.level().getGameTime() - releasedAt[0]);
			Wildercord.LOGGER.info("[cast-hit-consistency] nonplayer observation {}", note);
			check(mob[0].getHealth() < HEALTH, "Ordinary non-player Driving Cut retains actual damage: " + note);
			check(CastLock.locked(mob[0]), "Ordinary non-player Driving Cut retains its original cast lock: " + note);
			mob[0].discard();
		});
	}

	private static String nonplayerSnapshot(ServerPlayer player, LivingEntity mob, ItemStack blade, Vec3 aim) {
		var move = MastersArtRules.DRIVING_CUT;
		return "tick=" + player.level().getGameTime() + ", actorPos=" + player.position() + ", view=" + player.getViewVector(1)
			+ ", aim=" + aim + ", sameBlade=" + (player.getMainHandItem() == blade) + ", weapon=" + Aura.holdsWeapon(player)
			+ ", stage=" + Aura.stage(player) + ", aura=" + Aura.aura(player) + ", actorAlive=" + player.isAlive()
			+ ", actorGuard=" + AuraGuard.guarding(player) + ", actorLock=" + CastLock.locked(player)
			+ ", harmable=" + ArtKit.harmable(player, mob) + ", los=" + player.hasLineOfSight(mob)
			+ ", selected=" + ArtKit.line(player, player.position(), aim, move.reach(), .8, 2.4, move.targets()).contains(mob)
			+ ", targetPos=" + mob.position() + ", health=" + mob.getHealth() + ", maxHealth=" + mob.getMaxHealth()
			+ ", locked=" + CastLock.locked(mob) + ", alive=" + mob.isAlive() + ", removed=" + mob.isRemoved()
			+ ", source=" + mob.getLastDamageSource() + ", runebound=" + mob.hasAttached(WildercordAttachments.RUNEBOUND)
			+ ", tags=" + mob.entityTags();
	}

	private void startMaster() {
		master = AuraWorld.SWORD_MASTER.create(target.level(), EntitySpawnReason.COMMAND);
		check(master != null, "The original Master is constructible");
		master.setDiscipline(MastersRules.GALE); master.setNoAi(true); master.setNoGravity(true);
		master.snapTo(origin.x, origin.y, origin.z, 0, 0); target.level().addFreshEntity(master);
		master.mobInteract(target, InteractionHand.MAIN_HAND); master.mobInteract(target, InteractionHand.MAIN_HAND);
		check(SwordMaster.ready(target) == 1, "The target explicitly consents to and readies its own isolated trial");
		master.customServerAiStep(target.level()); attacker = master;
		check(master.started() && master.canHarmParticipant(target), "The real original Master's trial is started and legally owns this target");
	}
	private void prepareActor(ServerPlayer player) {
		player.removeAllEffects(); player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("gale", AuraRules.FORM, 1800, 100, 0));
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD)); place(player, 0, 0, 0);
	}
	private static void prepare(ServerPlayer player) {
		player.setGameMode(GameType.SURVIVAL); player.setPermanentlyInvulnerable(false); player.removeAllEffects();
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH); player.setHealth(HEALTH);
		player.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(64); player.setAbsorptionAmount(0);
		for (EquipmentSlot slot : EQUIPMENT) player.setItemSlot(slot, ItemStack.EMPTY);
		reconcileEquipment(player);
		player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE); player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
		player.setAttached(WildercordAttachments.CIRCLES, 0); player.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
	}
	private static void reconcileEquipment(ServerPlayer player) {
		check(!player.hasAttached(WildercordAttachments.CHARGE), "Native armour reconciliation precedes any held action");
		player.doTick();
		check(!player.hasAttached(WildercordAttachments.CHARGE), "Native reconciliation does not create a held action");
	}
	private static void charge(ServerPlayer player) {
		check(player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player && player.connection.player == player,
			"Only the exact connected recipient can request a native held spell");
		Spellbooks.setCord(player, new ItemStack(WildercordItems.TWINE_CORD));
		List<String> runes = List.of(Runes.BOLT.id(), Runes.HARM.id());
		Spellbooks.set(player, new Spellbook(runes, List.of(runes), 0, true));
		Spellbooks.setReadyAt(player, 0, 0); Spellbooks.setMana(player, 100);
		Charging.request(player, 0, true);
		check(player.hasAttached(WildercordAttachments.CHARGE), "Native Charging.request accepts the canonical registered fixture spell");
	}
	private static void dress(ServerPlayer player) {
		var protection = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
		Item[] pieces = {Items.NETHERITE_BOOTS, Items.NETHERITE_LEGGINGS, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_HELMET};
		EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
		for (int i = 0; i < pieces.length; i++) { ItemStack piece = new ItemStack(pieces[i]); piece.enchant(protection, 4); player.setItemSlot(slots[i], piece); }
	}
	private void place(ServerPlayer player, double x, double z, float yaw) {
		player.teleportTo(player.level(), origin.x + x, origin.y, origin.z + z, Set.of(), yaw, 0, false); player.setDeltaMovement(Vec3.ZERO);
	}
	private void discardMaster() { if (master != null) { master.discard(); master = null; } }
	private void clearTarget() {
		active = null; discardMaster();
		if (target != null) {
			recipientReadyAt = target.level().getGameTime() + Statuses.INTERRUPT_GAP;
			Charging.forget(target); target.setPermanentlyInvulnerable(false); target.removeAllEffects(); target.setAbsorptionAmount(0);
			target.setHealth(HEALTH); target.setDeltaMovement(Vec3.ZERO); target = null;
		}
		attacker = null;
	}
	private static boolean close(float a, float b) { return Math.abs(a - b) < .001F; }
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
