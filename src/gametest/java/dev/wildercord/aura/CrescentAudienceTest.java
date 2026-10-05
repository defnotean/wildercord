package dev.wildercord.aura;

import com.mojang.authlib.GameProfile;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.MastersRules;
import dev.wildercord.aura.world.SwordMaster;
import dev.wildercord.cast.Targets;
import dev.wildercord.duel.DuelRules;
import dev.wildercord.duel.Duels;
import dev.wildercord.party.Parties;
import dev.wildercord.party.PartyRules;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Native parries, enrollment, damage, locks and burst physics retain the same audience after ownership changes. */
public final class CrescentAudienceTest implements FabricClientGameTest {
	private static final int COLOR = 0xF26E42;
	private static final double Y = 180;
	private static final Vec3 FORWARD = new Vec3(0, 0, 1);
	private static final Vec3 UNCHANGED_MOTION = new Vec3(.125, .0625, -.125);
	private static final AuraApi.StringArt ART = AuraApi.StringArt.of("test:crescent_audience", "swing swing low",
		AuraRules.GLOW, 1, 0, (player, marks) -> {
			throw new AssertionError("The test's evenly resolved held art must never perform");
		});

	private static final class Challenger extends FakePlayer {
		Challenger(ServerLevel level, String name) {
			super(level, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name));
		}

		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}

	private SwordMaster master;
	private Challenger enrolled, outsider;
	private LivingEntity creature;

	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			for (String command : List.of("difficulty normal", "gamerule minecraft:spawn_mobs false",
				"gamerule minecraft:natural_health_regeneration false", "fill -16 179 -16 16 179 20 minecraft:stone",
				"fill -16 180 -16 16 189 20 minecraft:air")) {
				world.getServer().runCommand(command);
			}
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				try {
					teach(player);
					place(player, .5, .5, 0);
					enrolled = add(level, "CrescentMember", 7.5, .5);
					outsider = add(level, "CrescentVisitor", .5, 4.5);
					creature = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
					check(creature != null, "A real non-player bystander exists");
					((net.minecraft.world.entity.Mob) creature).setNoAi(true);
					place(creature, .5, 2.5, 0);
					level.addFreshEntity(creature);
					master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
					check(master != null, "The registered Sword Master exists");
					master.setNoAi(true);
					place(master, .5, 8.5, 180);
					level.addFreshEntity(master);
					enroll(player);
					enroll(enrolled);
					check(SwordMaster.ready(player) == 1, "The original challenger starts the accepted roster");
					invoke(SwordMaster.class, master, "customServerAiStep", new Class<?>[] {ServerLevel.class}, level);
					check(master.started() && master.challengerCount() == 2, "Both separately consenting players are enrolled");
					check(!master.canHarmParticipant(outsider), "Proximity did not enroll the visitor");
					check(Clashes.on(), "This native fixture requires the default enabled clash configuration");

					returnedCrescents(player);
					trialMeetingsAndBursts(player);
					trialRetirement(player);
					partyMeetingsAndBursts(player);
				} finally {
					stopFlights();
					resolveAllEven();
					Duels.callOff(player);
					if (master != null) master.discard();
					if (enrolled != null) enrolled.discard();
					if (outsider != null) outsider.discard();
					if (creature != null) creature.discard();
					Clashes.forgetRest();
				}
			});
		}
	}

	private void returnedCrescents(ServerPlayer player) {
		armGuard(player);
		armGuard(outsider);
		float visitorHealth = outsider.getHealth(), creatureHealth = creature.getHealth(), playerHealth = player.getHealth();
		double visitorAura = Aura.aura(outsider);
		Crescents.Flight incoming = masterSlash(new Vec3(.5, Y + 1.2, 7.5), FORWARD.scale(-1));
		check(incoming.trialMaster == master && incoming.trialActive(), "A launched Master crescent captures its live trial");
		advance(incoming);
		check(incoming.done() && player.getHealth() == playerHealth && !AuraGuard.perfectNow(player),
			"The actual participant Aura Guard parries the incoming crescent without damage");
		check(incoming.hit.equals(Set.of(player.getUUID())), "Neither a non-enrolled player nor a creature can absorb the one-target budget");
		Crescents.Flight returned = lastReturn(player);
		check(returned.trialMaster == master && returned.trialTarget(master) && !returned.trialTarget(outsider)
			&& !returned.trialTarget(creature), "The participant-owned return keeps the original trial and admits its originating Master");
		Crescents.Flight contest = masterSlash(returned.front().add(0, 0, .8), FORWARD.scale(-1));
		Clashes.forgetRest();
		check(Clashes.meet(returned, contest) == Clashes.Meeting.LOCKED, "The reflected crescent enters a real continuation clash");
		double beforeClash = returned.damage();
		resolveOutcome(player, ClashRules.Outcome.A);
		check(!returned.done() && !returned.held() && contest.done() && returned.damage() < beforeClash
			&& returned.trialMaster == master && returned.trialTarget(master) && !returned.trialTarget(outsider),
			"The actual winning return carries reduced damage onward without losing its original trial audience");
		float masterHealth = master.getHealth();
		advance(returned);
		check(returned.hit.equals(Set.of(master.getUUID())) && master.getHealth() < masterHealth,
			"A visitor standing in the return path cannot consume its budget before the eligible Master is actually hurt");
		check(outsider.getHealth() == visitorHealth && Aura.aura(outsider) == visitorAura && AuraGuard.perfectNow(outsider)
			&& creature.getHealth() == creatureHealth, "Both outbound and returned crescents spare visitor health, guard, aura and creatures");
		stopFlights();

		// A second native perfect guard, this time the Master's, must not erase the initial trial owner.
		armGuard(player);
		check(master.raiseGuard(), "The Master can raise a fresh perfect guard for the second return");
		incoming = masterSlash(new Vec3(.5, Y + 1.2, 7.5), FORWARD.scale(-1));
		advance(incoming);
		returned = lastReturn(player);
		masterHealth = master.getHealth();
		advance(returned);
		Crescents.Flight twice = lastReturn(master);
		check(returned.done() && master.getHealth() == masterHealth && !master.perfectNow(),
			"The Master's actual perfect guard sends the participant return back again");
		check(twice.trialMaster == master && !twice.trialTarget(outsider) && twice.trialTarget(player),
			"A subsequent reflection remains confined to the same enrolled audience");
		armGuard(player);
		advance(twice);
		check(lastReturn(player).trialMaster == master && outsider.getHealth() == visitorHealth && AuraGuard.perfectNow(outsider),
			"A third native reflection still passes the uninvolved visitor without spending their guard");
		master.dropGuard();
		stopFlights();
	}

	private void trialMeetingsAndBursts(ServerPlayer player) {
		place(player, .5, .5, 0);
		place(outsider, .5, 2.5, 0);
		place(enrolled, 2.0, 2.5, 180);
		place(creature, -.5, 3.5, 0);
		Crescents.Flight trial = masterSlash(new Vec3(.5, Y + 1.2, 4.8), FORWARD.scale(-1));
		Crescents.Flight outside = playerSlash(outsider, new Vec3(.5, Y + 1.2, 3), FORWARD);
		check(Clashes.meet(trial, outside) == Clashes.Meeting.PASS && !trial.done() && !trial.held() && !outside.done(),
			"An outsider crescent cannot lock, consume or delete a trial projectile");
		check(!Clashes.meets(outsider, ART, List.of(1, 1, 0)) && !trial.done() && !trial.held() && !Clashes.holding(outsider),
			"An outsider art cannot hold or remove an oncoming trial projectile");
		outside.stop();
		place(outsider, 1.5, 3.5, 180);
		place(player, .5, 2.5, 0);
		Clashes.forgetRest();
		check(Clashes.meets(player, ART, List.of(1, 1, 0)) && trial.held() && Clashes.holding(player),
			"An enrolled participant's actual art admission can hold that same trial projectile");
		setBurstMotion(player);
		float visitorHealth = outsider.getHealth();
		resolveEven(player);
		assertTrialBurst(player, visitorHealth, "An even art/crescent lock");
		check(trial.done() && !trial.held() && !Clashes.holding(player), "An even lock releases the art and ends its held crescent");

		Clashes.forgetRest();
		trial = masterSlash(new Vec3(.5, Y + 1.2, 4.8), FORWARD.scale(-1));
		Crescents.Flight participant = playerSlash(player, new Vec3(.5, Y + 1.2, 3.2), FORWARD);
		check(Clashes.meet(trial, participant) == Clashes.Meeting.LOCKED && trial.held() && participant.held(),
			"An enrolled participant's crescent can lock against the Master");
		setBurstMotion(player);
		resolveEven(player);
		assertTrialBurst(player, visitorHealth, "An even crescent/crescent lock");

		trial = masterSlash(new Vec3(.5, Y + 1.2, 4.8), FORWARD.scale(-1));
		participant = playerSlash(player, new Vec3(.5, Y + 1.2, 3.2), FORWARD);
		check(Clashes.meet(trial, participant) == Clashes.Meeting.BREAK, "A just-resolved pair takes the real fallback-burst path");
		setBurstMotion(player);
		Crescents.clash(trial, participant);
		assertTrialBurst(player, visitorHealth, "The fallback crescent burst");
		check(trial.done() && participant.done(), "The fallback actually consumes both admitted crescents");
		stopFlights();
	}

	private void trialRetirement(ServerPlayer player) {
		place(player, .5, .5, 0);
		place(enrolled, 7.5, .5, 0);
		place(outsider, .5, 4.5, 180);
		place(creature, .5, 2.5, 0);
		armGuard(player);
		Crescents.Flight original = masterSlash(new Vec3(.5, Y + 1.2, 7.5), FORWARD.scale(-1));
		advance(original);
		Crescents.Flight returned = lastReturn(player);
		place(player, MastersRules.ARENA_RADIUS + 20, .5, 0);
		check(master.canHarmParticipant(enrolled) && !returned.trialActive() && !returned.trialTarget(master),
			"An ineligible reflector cannot keep a return active just because another participant remains");
		invoke(Crescents.class, null, "tick", new Class<?>[] {MinecraftServer.class}, player.level().getServer());
		check(returned.done(), "The actual flight tick retires a return immediately when its caster leaves the trial");
		place(player, .5, .5, 0);
		Crescents.Flight pending = masterSlash(new Vec3(.5, Y + 1.2, 7.5), FORWARD.scale(-1));
		place(player, MastersRules.ARENA_RADIUS + 20, .5, 0);
		place(enrolled, MastersRules.ARENA_RADIUS + 22, .5, 0);
		check(!pending.trialActive(), "An original crescent also retires when the last eligible challenger leaves");
		invoke(Crescents.class, null, "tick", new Class<?>[] {MinecraftServer.class}, player.level().getServer());
		check(pending.done(), "An abandoned trial projectile ends before another movement or collision");
		place(player, .5, .5, 0);
		place(enrolled, 7.5, .5, 0);
		armGuard(player);
		original = masterSlash(new Vec3(.5, Y + 1.2, 7.5), FORWARD.scale(-1));
		advance(original);
		returned = lastReturn(player);
		Clashes.forgetRest();
		place(enrolled, 1.5, 1.5, 180);
		Crescents.Flight answer = playerSlash(enrolled, returned.front(), returned.aim().scale(-1));
		check(Clashes.meet(returned, answer) == Clashes.Meeting.LOCKED,
			"Another enrolled participant can intercept the participant-owned return");
		player.setDeltaMovement(UNCHANGED_MOTION);
		enrolled.setDeltaMovement(UNCHANGED_MOTION);
		outsider.setDeltaMovement(UNCHANGED_MOTION);
		master.discard();
		check(!returned.trialActive() && !returned.trialTarget(outsider), "Removing the originating Master invalidates the inherited return");
		float visitorHealth = outsider.getHealth();
		invoke(Clashes.class, null, "tick", new Class<?>[] {MinecraftServer.class}, player.level().getServer());
		check(returned.done() && answer.done() && !returned.held() && !answer.held() && !Clashes.clashing(player)
			&& player.getDeltaMovement().equals(UNCHANGED_MOTION) && enrolled.getDeltaMovement().equals(UNCHANGED_MOTION)
			&& outsider.getDeltaMovement().equals(UNCHANGED_MOTION),
			"An ended trial cancels an already-held return without a winner, carry or burst push");
		invoke(Crescents.class, null, "tick", new Class<?>[] {MinecraftServer.class}, player.level().getServer());
		check(returned.done() && outsider.getHealth() == visitorHealth, "An ended-trial return is retired without hitting nearby outsiders");
		stopFlights();
	}

	private void partyMeetingsAndBursts(ServerPlayer player) {
		place(player, .5, 2.5, 0);
		place(enrolled, 1.5, 3.5, 180);
		place(outsider, .5, 5.5, 180);
		place(creature, 8.5, 8.5, 0);
		PartyRules rules = partyRules(player.level().getServer());
		long now = player.level().getGameTime();
		check(rules.invite(player.getUUID(), enrolled.getUUID(), now) == PartyRules.Result.OK
			&& rules.accept(enrolled.getUUID(), player.getUUID(), now) == PartyRules.Result.OK,
			"The second player explicitly accepts a real party invitation");
		check(Parties.sameParty(player, enrolled) && Parties.blocksHarm(player, enrolled), "The native party boundary is in force");
		Crescents.Flight own = playerSlash(player, new Vec3(.5, Y + 1.2, 3.2), FORWARD);
		Crescents.Flight friend = playerSlash(enrolled, new Vec3(.5, Y + 1.2, 4.8), FORWARD.scale(-1));
		check(Clashes.meet(own, friend) == Clashes.Meeting.PASS && Clashes.meet(friend, own) == Clashes.Meeting.PASS
			&& !own.done() && !friend.done() && !own.held() && !friend.held(), "Party crescents pass through each other in both owner orders");
		own.stop();
		check(!Clashes.meets(player, ART, List.of(1, 1, 0)) && !friend.held(), "An allied oncoming crescent cannot consume the party member's art");
		stopFlights();
		Clashes.forgetRest();
		own = playerSlash(player, new Vec3(.5, Y + 1.2, 3.2), FORWARD);
		Crescents.Flight hostile = playerSlash(outsider, new Vec3(.5, Y + 1.2, 4.8), FORWARD.scale(-1));
		check(Clashes.meet(own, hostile) == Clashes.Meeting.LOCKED, "Unrelated player crescents still form a real lock");
		enrolled.setDeltaMovement(UNCHANGED_MOTION);
		player.setDeltaMovement(Vec3.ZERO);
		float friendHealth = enrolled.getHealth();
		resolveEven(player);
		check(enrolled.getDeltaMovement().equals(UNCHANGED_MOTION) && enrolled.getHealth() == friendHealth
			&& player.getDeltaMovement().lengthSqr() > 0, "The even burst pushes a combatant but leaves their nearby party ally untouched");
		own = playerSlash(player, new Vec3(.5, Y + 1.2, 3.2), FORWARD);
		hostile = playerSlash(outsider, new Vec3(.5, Y + 1.2, 4.8), FORWARD.scale(-1));
		check(Clashes.meet(own, hostile) == Clashes.Meeting.BREAK, "A recent pair uses fallback while party membership stays live");
		player.setDeltaMovement(Vec3.ZERO);
		Crescents.clash(own, hostile);
		check(enrolled.getDeltaMovement().equals(UNCHANGED_MOTION) && enrolled.getHealth() == friendHealth
			&& player.getDeltaMovement().lengthSqr() > 0, "The fallback burst also spares nearby party allies");

		stopFlights();
		Clashes.forgetRest();
		Duels.rested(player);
		Duels.rested(enrolled);
		var duel = Duels.startBout(player, enrolled, new DuelRules.Terms(20, 0, 200, 0, 1, false), player.position(), null);
		duel.tick(now);
		check(Boolean.TRUE.equals(Duels.canHarm(player, enrolled)) && Duels.opponents(player.getUUID(), enrolled.getUUID()),
			"The native agreed duel is fighting, not merely counting down");
		own = playerSlash(player, new Vec3(.5, Y + 1.2, 3.2), FORWARD);
		friend = playerSlash(enrolled, new Vec3(.5, Y + 1.2, 4.8), FORWARD.scale(-1));
		check(Clashes.meet(friend, own) == Clashes.Meeting.LOCKED && own.held() && friend.held(),
			"An explicit duel overrides party protection even when the other participant is the first owner");
		player.setDeltaMovement(Vec3.ZERO);
		enrolled.setDeltaMovement(Vec3.ZERO);
		resolveEven(player);
		check(player.getDeltaMovement().lengthSqr() > 0 && enrolled.getDeltaMovement().lengthSqr() > 0,
			"The duel's actual even burst may push both consenting party opponents");
		Duels.callOff(player);
	}

	private void setBurstMotion(ServerPlayer player) {
		player.setDeltaMovement(Vec3.ZERO);
		outsider.setDeltaMovement(UNCHANGED_MOTION);
		creature.setDeltaMovement(UNCHANGED_MOTION);
	}

	private void assertTrialBurst(ServerPlayer player, float visitorHealth, String scene) {
		check(outsider.getDeltaMovement().equals(UNCHANGED_MOTION) && creature.getDeltaMovement().equals(UNCHANGED_MOTION)
			&& outsider.getHealth() == visitorHealth, scene + " cannot push or hurt trial bystanders");
		check(player.getDeltaMovement().lengthSqr() > 0, scene + " still pushes its eligible enrolled combatant");
	}

	private Crescents.Flight masterSlash(Vec3 origin, Vec3 aim) {
		return Crescents.launch(master, origin, aim, COLOR, 6, 1, .7, 20, .8, 1, false,
			master::canHarmParticipant, (flight, target) -> master.projected(target, flight.damage()));
	}

	private static Crescents.Flight playerSlash(ServerPlayer player, Vec3 origin, Vec3 aim) {
		return Crescents.launch(player, origin, aim, COLOR, 6, 1, .7, 20, .8, 1, false,
			entity -> Targets.canHarm(player, entity), AuraSlash.cutter(player));
	}

	/** Step the native path without advancing the world clock, so perfect windows and scene positions are deterministic. */
	private static void advance(Crescents.Flight flight) {
		while (!flight.done() && flight.step < flight.steps && flight.hit.size() < flight.targets) {
			Crescents.move(flight);
			if (!flight.done()) Crescents.cutFrom(flight);
		}
	}

	private Crescents.Flight lastReturn(LivingEntity owner) {
		Crescents.Flight result = Crescents.inFlight().getLast();
		check(result.caster() == owner && !result.done() && result.trialMaster == master, "A real parry launched the expected new return");
		return result;
	}

	private void enroll(ServerPlayer player) {
		for (int i = 0; i < 2; i++) {
			invoke(SwordMaster.class, master, "mobInteract", new Class<?>[] {net.minecraft.world.entity.player.Player.class, InteractionHand.class},
				player, InteractionHand.MAIN_HAND);
		}
	}

	private static Challenger add(ServerLevel level, String name, double x, double z) {
		Challenger player = new Challenger(level, name);
		teach(player);
		place(player, x, z, 0);
		level.addNewPlayer(player);
		return player;
	}

	private static void teach(ServerPlayer player) {
		player.setGameMode(GameType.SURVIVAL);
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
		player.clearFire();
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.FLOW, 0, AuraRules.capacity(AuraRules.FLOW), 0));
		player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
	}

	private static void armGuard(ServerPlayer player) {
		long now = player.level().getGameTime();
		player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE.guard(now, now + 40));
	}

	private static void place(LivingEntity entity, double x, double z, float yaw) {
		if (entity instanceof ServerPlayer player) player.teleportTo(player.level(), x, Y, z, Set.of(), yaw, 0, false);
		else entity.snapTo(x, Y, z, yaw, 0);
		entity.setYRot(yaw);
		entity.setYHeadRot(yaw);
		entity.setXRot(0);
		entity.setDeltaMovement(Vec3.ZERO);
	}

	private static void stopFlights() {
		for (Crescents.Flight flight : Crescents.inFlight()) flight.stop();
	}

	private static PartyRules partyRules(MinecraftServer server) {
		Object session = invoke(Parties.class, null, "session", new Class<?>[] {MinecraftServer.class}, server);
		return (PartyRules) field(session.getClass(), session, "rules");
	}

	/** Resolve the real locked object through its native even branch; no synthetic burst or fake combat result is substituted. */
	private static void resolveEven(LivingEntity participant) {
		resolveOutcome(participant, ClashRules.Outcome.EVEN);
	}

	private static void resolveOutcome(LivingEntity participant, ClashRules.Outcome outcome) {
		Map<?, ?> active = (Map<?, ?>) field(Clashes.class, null, "ACTIVE");
		Object clash = active.get(Clashes.idOf(participant));
		check(clash != null, "An actual native clash exists to resolve");
		invoke(Clashes.class, null, "resolve", new Class<?>[] {clash.getClass(), ClashRules.Outcome.class}, clash, outcome);
	}

	private static void resolveAllEven() {
		Map<?, ?> active = (Map<?, ?>) field(Clashes.class, null, "ACTIVE");
		for (Object clash : List.copyOf(active.values())) {
			invoke(Clashes.class, null, "resolve", new Class<?>[] {clash.getClass(), ClashRules.Outcome.class}, clash, ClashRules.Outcome.EVEN);
		}
	}

	private static Object field(Class<?> owner, Object target, String name) {
		try {
			Field field = owner.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(target);
		} catch (ReflectiveOperationException failure) {
			throw new AssertionError("Cannot inspect native fixture state " + owner.getSimpleName() + "." + name, failure);
		}
	}

	private static Object invoke(Class<?> owner, Object target, String name, Class<?>[] types, Object... args) {
		try {
			Method method = owner.getDeclaredMethod(name, types);
			method.setAccessible(true);
			return method.invoke(target, args);
		} catch (InvocationTargetException failure) {
			if (failure.getCause() instanceof Error error) throw error;
			if (failure.getCause() instanceof RuntimeException exception) throw exception;
			throw new AssertionError("Native fixture invocation failed: " + name, failure.getCause());
		} catch (ReflectiveOperationException failure) {
			throw new AssertionError("Cannot invoke native fixture path " + owner.getSimpleName() + "." + name, failure);
		}
	}

	private static void check(boolean value, String message) {
		if (!value) throw new AssertionError(message);
	}
}
