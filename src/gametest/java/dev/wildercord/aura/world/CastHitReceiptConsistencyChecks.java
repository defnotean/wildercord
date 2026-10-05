package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
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
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
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
final class CastHitReceiptConsistencyChecks {
	private static final float HEALTH = 200;
	private static final List<EquipmentSlot> EQUIPMENT = List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
	private enum Route { BREAK_CAST, DRIVING_CUT }
	private enum Case { HEALTH, FULL_ABSORPTION, MANA_SKIN, REVERSAL, TOTEM, GUARD, STEP, WARD,
		RESISTANCE, REJECTED, REPLACED, EQUAL_TOKEN, NEW_CHARGE, IDLE, WINDUP_REPLACEMENT }
	private static final class Challenger extends FakePlayer {
		boolean rejectDamage;
		Challenger(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "CastReceipt")); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return rejectDamage; }
	}
	private static boolean registered;
	private static CastHitReceiptConsistencyChecks active;
	private SwordMaster master;
	private Challenger target;
	private LivingEntity attacker;
	private Vec3 origin, positionBefore;
	private Route route;
	private Case probe;
	private WildercordAttachments.Charge beforeCharge, callbackCharge;
	private DamageSource beforeSource, acceptedSource;
	private float beforeHealth, beforeAbsorption, beforeMana;
	private int callbacks;
	private long begun, drivingReady, impactAt;
	private boolean releaseFinished;

	void run(ClientGameTestContext context) {
		registerCallbacks();
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false"))
				world.getServer().runCommand(command);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				origin = new Vec3(player.getBlockX() + .5, 181, player.getBlockZ() + .5);
				for (int x = -15; x <= 15; x++) for (int z = -15; z <= 15; z++)
					player.level().setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.STONE.defaultBlockState());
				prepare(player); place(player, 8, 3, 180);
			});
			for (Route selected : Route.values()) for (Case selectedCase : Case.values()) {
				world.getServer().runOnServer(server -> {
					route = selected; probe = selectedCase;
					target = add(server.getPlayerList().getPlayers().getFirst().level());
					charge(target);
				});
				try {
					release(context, world, true);
					world.getServer().runOnServer(server -> verify());
					// The real Step owns queued movement; finish it before discarding its player body.
					if (selectedCase == Case.STEP) world.getServer().waitFor(server ->
						target.level().getGameTime() >= impactAt + Math.max(AuraRules.STEP_TICKS, AuraRules.STEP_GUARD_TICKS) + 2,
						Math.max(AuraRules.STEP_TICKS, AuraRules.STEP_GUARD_TICKS) + 5);
				} finally { world.getServer().runOnServer(server -> clearTarget()); }
			}
			sharedImmunity(context, world, Route.BREAK_CAST);
			sharedImmunity(context, world, Route.DRIVING_CUT);
			idleSealRecovery(context, world);
			chargedSealRecovery(context, world);
			nonplayer(context, world);
		} finally { active = null; }
	}

	private static void registerCallbacks() {
		if (registered) return;
		registered = true;
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

	private void release(ClientGameTestContext context, TestSingleplayerContext world, boolean configure) {
		if (route == Route.BREAK_CAST) {
			world.getServer().runOnServer(server -> {
				place(server.getPlayerList().getPlayers().getFirst(), 8, 3, 180);
				startMaster(); begun = target.level().getGameTime();
			});
			world.getServer().waitFor(server -> target.level().getGameTime() >= begun + 21, 25);
			world.getServer().runOnServer(server -> {
				master.setTarget(target); master.customServerAiStep(target.level());
				check(master.attackAnimation() == MastersRules.Move.BREAK_CAST.ordinal() + 1 && master.state(AuraFighter.WINDUP),
					"A real held spell inside four blocks selects the original BREAK_CAST tell");
				begun = target.level().getGameTime();
				check(close(target.getHealth(), HEALTH), "The original Master windup is harmless");
			});
			world.getServer().waitFor(server -> {
				if (target.level().getGameTime() < begun + MastersRules.Move.BREAK_CAST.tell) return false;
				check(target.level().getGameTime() == begun + MastersRules.Move.BREAK_CAST.tell, "Observe the original Master exact release frame");
				beforeImpact(configure);
				master.customServerAiStep(target.level());
				check(!master.state(AuraFighter.WINDUP), "The accepted original Master releases its pending hit");
				active = null;
				return true;
			}, MastersRules.Move.BREAK_CAST.tell + 5);
		} else {
			world.getServer().waitFor(server -> server.overworld().getGameTime() >= drivingReady,
				MastersArtRules.DRIVING_CUT.rest() + 5);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				prepareActor(player); attacker = player; releaseFinished = false;
				check(ArtKit.harmable(player, target) && player.hasLineOfSight(target), "The real client may target this player through the native art selector");
				// Same deadline and FIFO as the real release: defence starts before impact, Step motion after it.
				Scheduler.later(MastersArtRules.DRIVING_CUT.windup(), () -> beforeImpact(configure));
				check(MastersArts.activate(player, 2), "The real connected client accepts Driving Cut through its public entrypoint");
				check(close((float) Aura.aura(player), 100 - (float) MastersArtRules.DRIVING_CUT.cost()), "Driving Cut commits its unchanged eighteen-Aura price");
				drivingReady = server.overworld().getGameTime() + MastersArtRules.DRIVING_CUT.rest();
				check(close(target.getHealth(), HEALTH), "The scheduled Driving Cut windup is harmless");
				Scheduler.later(MastersArtRules.DRIVING_CUT.windup(), () -> { active = null; releaseFinished = true; });
			});
			world.getServer().waitFor(server -> releaseFinished, MastersArtRules.DRIVING_CUT.windup() + 5);
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
				target.setAttached(WildercordAttachments.CIRCLES, 3); dress(target); target.doTick();
				check(target.getArmorValue() == 20 && target.getAttributeValue(Attributes.ARMOR_TOUGHNESS) >= 12,
					"Inherited native doTick installs real Protection IV netherite despite FakePlayer.tick being empty");
			}
			case REVERSAL -> {
				target.setHealth(1); CastEngine.cast(target, SpellCompiler.compile(List.of(Runes.SELF, Runes.REVERSAL)).root());
			}
			case TOTEM -> { target.setHealth(1); target.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING)); }
			case GUARD, STEP -> {
				target.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
				target.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("gale", AuraRules.FORM, 0, 80, 0));
				float aura = (float) Aura.aura(target);
				if (probe == Case.GUARD) check(AuraGuard.faces(target, attacker.position()) && AuraGuard.raise(target) && AuraGuard.perfectNow(target),
					"The public Aura Guard entrypoint opens a real frontal perfect window");
				else check(AuraStep.step(target), "The public Aura Step entrypoint admits its genuine swept path");
				check(Aura.aura(target) < aura, "The actual defensive action pays its native Aura cost");
			}
			case WARD -> {
				// Foresight turns at most twelve away; native Warded IV brings the Master's unchanged 28 under that cap.
				target.addEffect(new MobEffectInstance(WildercordEffects.WARDED, 100, 3));
				CastEngine.cast(target, SpellCompiler.compile(List.of(Runes.SELF, Runes.FORESIGHT)).root());
			}
			case RESISTANCE -> check(target.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 4)), "Native Resistance V is installed");
			case REJECTED -> target.rejectDamage = true;
			case NEW_CHARGE, IDLE -> Charging.interrupt(target);
			case WINDUP_REPLACEMENT -> { Charging.interrupt(target); Charging.request(target, 0, true); }
			default -> { }
		}
		beforeCharge = target.getAttached(WildercordAttachments.CHARGE);
		if (probe != Case.NEW_CHARGE && probe != Case.IDLE) check(beforeCharge != null, "A real held charge survives until the pre-impact snapshot");
		beforeHealth = target.getHealth(); beforeAbsorption = target.getAbsorptionAmount(); beforeMana = Spellbooks.mana(target);
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
		else if (probe == Case.MANA_SKIN) check(close(target.getHealth(), beforeHealth) && Spellbooks.mana(target) < beforeMana,
			"Native armour plus Mana Skin fully conceals the real wound in the final health: " + note);
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

	private void sharedImmunity(ClientGameTestContext context, TestSingleplayerContext world, Route first) {
		world.getServer().runOnServer(server -> {
			route = first; probe = Case.HEALTH; target = add(server.getPlayerList().getPlayers().getFirst().level()); charge(target);
		});
		try {
			release(context, world, true);
			long[] interruptedAt = new long[1];
			world.getServer().runOnServer(server -> { verify(); interruptedAt[0] = impactAt; discardMaster(); });
			world.getServer().waitFor(server -> target.level().getGameTime() >= interruptedAt[0] + MastersArtRules.INTERRUPT_TICKS + 1,
				MastersArtRules.INTERRUPT_TICKS + 5);
			world.getServer().runOnServer(server -> {
				check(!CastLock.locked(target), "The original short seal expires without clearing shared Statuses immunity");
				target.setHealth(HEALTH); place(target, 0, 3, 180); charge(target);
				route = first == Route.BREAK_CAST ? Route.DRIVING_CUT : Route.BREAK_CAST;
			});
			release(context, world, false);
			world.getServer().runOnServer(server -> {
				check(target.level().getGameTime() - interruptedAt[0] < Statuses.INTERRUPT_GAP, "Both real releases occur inside the same untouched 160-tick immunity");
				check(target.getHealth() < beforeHealth && target.getAttached(WildercordAttachments.CHARGE) == beforeCharge && !CastLock.locked(target),
					"An accepted " + route + " cannot bypass the shared immunity earned by " + first);
				Effects.withSource(attacker, () -> check(!Statuses.interrupt(target), "Neither cross-route strike resets the existing shared immunity"));
				Wildercord.LOGGER.info("[cast-hit-consistency] shared immunity {} -> {} preserved", first, route);
			});
			world.getServer().waitFor(server -> {
				if (target.level().getGameTime() < interruptedAt[0] + Statuses.INTERRUPT_GAP - 1) return false;
				Effects.withSource(attacker, () -> check(target.level().getGameTime() == interruptedAt[0] + Statuses.INTERRUPT_GAP - 1
					&& !Statuses.interrupt(target) && target.getAttached(WildercordAttachments.CHARGE) == beforeCharge,
					"The exact native held spell remains protected through tick 159"));
				return true;
			}, Statuses.INTERRUPT_GAP + 5);
			world.getServer().waitFor(server -> {
				if (target.level().getGameTime() < interruptedAt[0] + Statuses.INTERRUPT_GAP) return false;
				Effects.withSource(attacker, () -> check(target.level().getGameTime() == interruptedAt[0] + Statuses.INTERRUPT_GAP
					&& Statuses.interrupt(target) && !target.hasAttached(WildercordAttachments.CHARGE),
					"A refused cross-route strike does not extend the original immunity beyond tick 160"));
				return true;
			}, 5);
		} finally { world.getServer().runOnServer(server -> clearTarget()); }
	}

	private void idleSealRecovery(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			route = Route.DRIVING_CUT; probe = Case.IDLE; target = add(server.getPlayerList().getPlayers().getFirst().level()); charge(target);
		});
		try {
			release(context, world, true);
			world.getServer().runOnServer(server -> verify());
			world.getServer().waitFor(server -> {
				if (target.level().getGameTime() < impactAt + MastersArtRules.INTERRUPT_TICKS - 1) return false;
				check(target.level().getGameTime() == impactAt + MastersArtRules.INTERRUPT_TICKS - 1 && CastLock.locked(target),
					"The original idle-player seal still holds on tick nineteen");
				return true;
			}, MastersArtRules.INTERRUPT_TICKS + 5);
			world.getServer().waitFor(server -> {
				if (target.level().getGameTime() < impactAt + MastersArtRules.INTERRUPT_TICKS) return false;
				check(target.level().getGameTime() == impactAt + MastersArtRules.INTERRUPT_TICKS && !CastLock.locked(target),
					"The idle-player Driving Cut seal expires on its unchanged exact twentieth tick");
				Effects.withSource(attacker, () -> CastLock.lock(target, MastersArtRules.INTERRUPT_TICKS));
				check(!CastLock.locked(target), "The unchanged CastLock recovery rejects a second idle seal");
				charge(target);
				Effects.withSource(attacker, () -> check(Statuses.interrupt(target), "An idle-player seal consumes no shared charge-interruption immunity"));
				return true;
			}, 5);
		} finally { world.getServer().runOnServer(server -> clearTarget()); }
	}


	private void chargedSealRecovery(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().waitFor(server -> server.overworld().getGameTime() >= drivingReady, MastersArtRules.DRIVING_CUT.rest() + 5);
		long[] sealedAt = new long[1];
		world.getServer().runOnServer(server -> {
			route = Route.DRIVING_CUT; probe = Case.HEALTH; target = add(server.getPlayerList().getPlayers().getFirst().level());
			Effects.withSource(server.getPlayerList().getPlayers().getFirst(), () -> CastLock.lock(target, MastersArtRules.INTERRUPT_TICKS));
			sealedAt[0] = target.level().getGameTime();
			check(CastLock.locked(target), "A genuine earlier idle seal starts its original native recovery window");
		});
		try {
			world.getServer().waitFor(server -> target.level().getGameTime() >= sealedAt[0] + MastersArtRules.INTERRUPT_TICKS + 1,
				MastersArtRules.INTERRUPT_TICKS + 5);
			world.getServer().runOnServer(server -> {
				check(!CastLock.locked(target), "The old seal is unlocked before a real new spell starts");
				charge(target);
			});
			release(context, world, false);
			world.getServer().runOnServer(server -> {
				check(target.level().getGameTime() < sealedAt[0] + MastersArtRules.INTERRUPT_TICKS + CastLock.PLAYER_RECOVERY,
					"The new actual Driving Cut arrives inside the unchanged forty-tick seal recovery");
				check(target.getHealth() < beforeHealth && target.getAttached(WildercordAttachments.CHARGE) == beforeCharge && !CastLock.locked(target),
					"Recovery refuses both a fresh seal and charge interruption despite accepted Driving Cut damage");
				Effects.withSource(attacker, () -> check(Statuses.interrupt(target), "Refused recovery consumes none of the shared 160-tick interruption immunity"));
				Wildercord.LOGGER.info("[cast-hit-consistency] native cast-lock recovery preserves charge and shared immunity");
			});
		} finally { world.getServer().runOnServer(server -> clearTarget()); }
	}

	private void nonplayer(ClientGameTestContext context, TestSingleplayerContext world) {
		LivingEntity[] mob = new LivingEntity[1];
		world.getServer().waitFor(server -> server.overworld().getGameTime() >= drivingReady, MastersArtRules.DRIVING_CUT.rest() + 5);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst(); prepareActor(player); releaseFinished = false;
			var husk = EntityTypes.HUSK.create(player.level(), EntitySpawnReason.COMMAND);
			check(husk != null, "The ordinary non-player target is constructible");
			husk.setNoAi(true); husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH); husk.setHealth(HEALTH);
			husk.snapTo(origin.x, origin.y, origin.z + 3, 180, 0); player.level().addFreshEntity(husk); mob[0] = husk;
			check(MastersArts.activate(player, 2), "Actual Driving Cut accepts an ordinary non-player target");
			Scheduler.later(MastersArtRules.DRIVING_CUT.windup(), () -> releaseFinished = true);
		});
		world.getServer().waitFor(server -> releaseFinished, MastersArtRules.DRIVING_CUT.windup() + 5);
		world.getServer().runOnServer(server -> {
			check(mob[0].getHealth() < HEALTH && CastLock.locked(mob[0]), "Ordinary non-player Driving Cut retains damage and its original cast lock");
			mob[0].discard();
		});
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
	private Challenger add(ServerLevel level) {
		Challenger player = new Challenger(level); prepare(player);
		player.snapTo(origin.x, origin.y, origin.z + 3, 180, 0); level.addNewPlayer(player); return player;
	}
	private void prepareActor(ServerPlayer player) {
		player.removeAllEffects(); player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("gale", AuraRules.FORM, 1800, 100, 0));
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD)); place(player, 0, 0, 0);
	}
	private static void prepare(ServerPlayer player) {
		player.setGameMode(GameType.SURVIVAL); player.removeAllEffects();
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH); player.setHealth(HEALTH);
		player.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(64); player.setAbsorptionAmount(0);
		for (EquipmentSlot slot : EQUIPMENT) player.setItemSlot(slot, ItemStack.EMPTY);
		player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE); player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
		player.setAttached(WildercordAttachments.CIRCLES, 0); player.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
	}
	private static void charge(ServerPlayer player) {
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
		if (target != null) { Charging.forget(target); target.discard(); target = null; }
		attacker = null;
	}
	private static boolean close(float a, float b) { return Math.abs(a - b) < .001F; }
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
