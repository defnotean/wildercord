package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ReleasedArtOwner;
import dev.wildercord.aura.arts.RimeArts;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Scheduler;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Native first-stone damage mutations, invoked by the released-owner suite on its existing connected server. */
final class HailfallDamageCallbacks {

	private enum Change {
		CONTROL, OWNER_DEATH, OWNER_RESPAWN, OWNER_DIMENSION, OWNER_ROUND_TRIP,
		TARGET_DEATH, TARGET_REMOVAL, TARGET_DIMENSION, LATER_TARGET_REMOVAL, LATER_TARGET_DIMENSION;

		boolean retiresOwner() {
			return this == OWNER_DEATH || this == OWNER_RESPAWN || this == OWNER_DIMENSION || this == OWNER_ROUND_TRIP;
		}
		boolean changesFirst() { return this == TARGET_DEATH || this == TARGET_REMOVAL || this == TARGET_DIMENSION; }
		boolean changesSecond() { return this == LATER_TARGET_REMOVAL || this == LATER_TARGET_DIMENSION; }
	}

	private record Hit(LivingEntity target, DamageSource source) { }
	private static boolean listening;
	private static Consumer<Hit> onDamage;
	private static final Vec3 FEET = new Vec3(.5, 100, .5);
	private static final int FINISH = ArtRules.HAIL_TICKS + 8;
	private Probe current;

	private static final class Probe {
		final Change change;
		final ServerPlayer owner;
		final ServerLevel level;
		final List<LivingEntity> fixtures = new ArrayList<>();
		LivingEntity first, second, destination, moved;
		ReleasedArtOwner binding;
		long accepted, released, mutation;
		int releases, firstHits, secondHits;
		float mutationHealth;
		boolean mutated, finished;
		Throwable failure;
		Probe(Change change, ServerPlayer owner) { this.change = change; this.owner = owner; this.level = owner.level(); }
	}

	static void run(ClientGameTestContext context, TestServerContext server) {
		new HailfallDamageCallbacks().checkCases(context, server);
	}

	private void checkCases(ClientGameTestContext context, TestServerContext server) {
		AuraApi.StringHook hook = this::released;
		Consumer<Hit> previous = server.computeOnServer(s -> {
			if (!listening) {
				listening = true;
				ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, base, damage, blocked) -> {
					Consumer<Hit> action = onDamage;
					if (action != null && damage > 0) action.accept(new Hit(target, source));
				});
			}
			Consumer<Hit> outer = onDamage;
			AuraApi.onString(hook);
			return outer;
		});
		try {
			for (Change change : Change.values()) {
				server.runOnServer(s -> begin(s, change));
				server.waitFor(s -> current.finished, 300);
				server.runOnServer(s -> {
					Probe p = current;
					if (p.failure != null) throw new AssertionError("Hailfall damage callback " + change, p.failure);
					check(p.releases == 1 && p.mutated && p.mutation == p.released + 4,
						"Exactly one actual first-stone damage callback performs the mutation");
					check(p.binding.valid() != change.retiresOwner(), "Only original-owner retirement ends the released cloud");
					check(Effects.applying() == null && Effects.applyingCast() == null, "Hailfall restores ambient damage provenance");
					Wildercord.LOGGER.info("HAILFALL_DAMAGE_CALLBACK case={} accepted={} release={} mutation={} firstHits={} secondHits={} firstHealth={} secondHealth={} binding={}",
						change, p.accepted, p.released, p.mutation, p.firstHits, p.secondHits,
						p.first.getHealth(), p.second.getHealth(), p.binding.valid());
					onDamage = previous;
					ServerPlayer owner = s.getPlayerList().getPlayer(p.owner.getUUID());
					if (!owner.isAlive()) owner = HailfallReleasedOwnerTest.respawn(s, owner);
					HailfallReleasedOwnerTest.prepare(owner, p.level);
					if (change.retiresOwner()) check(!p.binding.valid(), "Restoration cannot revive the retired cloud");
					p.fixtures.forEach(LivingEntity::discard);
				});
				context.runOnClient(mc -> mc.gui.setScreen(null));
				context.waitTicks(5);
			}
		} finally {
			server.runOnServer(s -> {
				onDamage = previous;
				AuraApi.stringHooks().remove(hook);
				if (current != null) current.fixtures.forEach(LivingEntity::discard);
			});
		}
	}

	private void begin(MinecraftServer server, Change change) {
		ServerPlayer owner = server.getPlayerList().getPlayers().getFirst();
		HailfallReleasedOwnerTest.prepare(owner, server.overworld());
		Probe p = new Probe(change, owner);
		current = p;
		p.first = target(p, p.level, 0);
		// Same horizontal position, slightly higher feet: the first victim is strictly nearer every random first drop.
		p.second = target(p, p.level, .3);
		p.destination = target(p, server.getLevel(Level.NETHER), 0);
		onDamage = hit -> checked(p, () -> observedDamage(p, hit));
		SwordStrings.forget(owner.getUUID());
		owner.jumpFromGround(); owner.setOnGround(false); owner.setDeltaMovement(Vec3.ZERO);
		check(SwordString.Token.LEAP.fits(SwordStrings.observedMarks(owner)), "The native first Punch observes a leap");
		owner.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
		Scheduler.later(2, () -> checked(p, () -> {
			owner.setOnGround(true); owner.setShiftKeyDown(true); owner.setDeltaMovement(Vec3.ZERO);
			check(SwordString.Token.LOW.fits(SwordStrings.observedMarks(owner)), "The native second Punch observes the low stroke");
			owner.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
			AuraApi.StringArt art = AuraApi.artOf(owner, RimeArts.HAILFALL).orElseThrow();
			check(SwordStrings.saw(owner, art), "The real native observations admit Hailfall");
			p.accepted = p.level.getGameTime();
			float aura = Aura.aura(owner);
			SwordStrings.request(owner, new SwordStrings.Perform(RimeArts.HAILFALL,
				List.of(SwordString.Token.LEAP.bit(), SwordString.Token.LOW.bit())));
			check(Aura.aura(owner) < aura && SwordStrings.readyAt(owner, RimeArts.HAILFALL) > p.accepted,
				"The callback probe pays the actual art's Aura and cooldown");
			check(!SwordStrings.saw(owner, art), "The real request consumes its observed strokes");
		}));
	}

	private void released(ServerPlayer owner, AuraApi.StringArt art, AuraApi.StringContext move) {
		Probe p = current;
		if (p == null || owner != p.owner || !RimeArts.HAILFALL.equals(art.id())) return;
		checked(p, () -> {
			p.releases++; p.released = p.level.getGameTime(); p.binding = ReleasedArtOwner.capture(owner);
			check(p.binding.valid() && p.binding.level() == p.level, "The actual release owns its original connected body/world");
			var profile = MastersStyleRules.of(art.id());
			check(p.released - p.accepted == (profile == null ? 0 : profile.windup()), "Hailfall follows its actual windup");
			Scheduler.later(3, () -> checked(p, () -> {
				untouched(p.first, "The first victim is untouched between the actual ray and stone");
				untouched(p.second, "The later snapshot victim is untouched before the stone");
				// The admitted common Rime strike also slows. Switch method after release so zero chill identifies
				// Hailfall's own post-hit continuation without changing the shared admitted-hit contract.
				owner.setAttached(AuraAttachments.AURA, Aura.data(owner).withMethod("stone"));
				check(p.binding.valid(), "An ordinary post-release method change preserves the cloud");
			}));
			Scheduler.later(5, () -> checked(p, () -> firstStone(p)));
			Scheduler.later(FINISH, () -> checked(p, () -> finish(p)));
		});
	}

	private static void observedDamage(Probe p, Hit hit) {
		if (hit.source().getEntity() != p.owner || !hit.source().is(Aura.DAMAGE)
			|| hit.target() != p.first && hit.target() != p.second) return;
		check(Effects.applying() == p.owner && Effects.applyingCast() == null, "The native stone keeps Hailfall's exact damage actor");
		if (hit.target() == p.second) { p.secondHits++; return; }
		p.firstHits++;
		if (p.mutated) return;
		check(p.firstHits == 1 && p.secondHits == 0 && p.level.getGameTime() == p.released + 4,
			"Mutation occurs synchronously inside the first victim's first native stone damage");
		p.mutated = true; p.mutation = p.level.getGameTime();
		MinecraftServer server = p.level.getServer();
		mutationSnapshot(p, "before");
		switch (p.change) {
			case CONTROL -> { }
			case OWNER_DEATH -> { p.owner.kill(p.level); check(!p.owner.isAlive(), "Native damage callback kills the original owner"); }
			case OWNER_RESPAWN -> {
				p.owner.kill(p.level);
				ServerPlayer replacement = HailfallReleasedOwnerTest.respawn(server, p.owner);
				HailfallReleasedOwnerTest.prepare(replacement, p.level);
				check(ReleasedArtOwner.capture(replacement).valid(), "The replacement body can own a fresh release");
			}
			case OWNER_DIMENSION, OWNER_ROUND_TRIP -> {
				check(p.owner.teleportTo(server.getLevel(Level.NETHER), FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false), "The damage callback actually moves the owner to the Nether");
				check(p.owner.level() != p.level, "The original body leaves its released world");
				mutationSnapshot(p, "departed");
				if (p.change == Change.OWNER_ROUND_TRIP) {
					check(p.owner.teleportTo(p.level, FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false), "The same callback returns the original body");
					check(p.owner.level() == p.level && server.getPlayerList().getPlayer(p.owner.getUUID()) == p.owner,
						"The exact connected original body is back before the old stone resumes");
					check(ReleasedArtOwner.capture(p.owner).valid(), "The returned body owns a fresh lifetime before the old stone resumes");
				}
			}
			case TARGET_DEATH -> { p.first.kill(p.level); check(!p.first.isAlive(), "The callback really kills the struck victim"); }
			case TARGET_REMOVAL -> { p.first.discard(); check(p.first.isRemoved(), "The callback really removes the struck victim"); }
			case LATER_TARGET_REMOVAL -> { p.second.discard(); check(p.second.isRemoved(), "The callback removes a later snapshot victim"); }
			case TARGET_DIMENSION, LATER_TARGET_DIMENSION -> {
				LivingEntity victim = p.change == Change.TARGET_DIMENSION ? p.first : p.second;
				ServerLevel destination = server.getLevel(Level.NETHER);
				UUID id = victim.getUUID();
				check(victim.teleportTo(destination, FEET.x + 3, FEET.y, FEET.z + ArtRules.HAIL_AHEAD, Set.of(), 180, 0, false), "The callback actually transfers its chosen victim to another world");
				p.moved = (LivingEntity) destination.getEntity(id);
				check(p.moved != null && p.moved.isAlive() && p.moved.level() != p.level,
					"The target's UUID resolves to its actual destination-world body");
				if (p.moved != victim) p.fixtures.add(p.moved);
			}
		}
		p.mutationHealth = p.first.getHealth();
		mutationSnapshot(p, "after");
		// Do not poll the old binding here: production must observe retirement before returning to chill or later victims.
	}

	private static void mutationSnapshot(Probe p, String phase) {
		ServerPlayer registered = p.level.getServer().getPlayerList().getPlayer(p.owner.getUUID());
		Wildercord.LOGGER.info("HAILFALL_DAMAGE_MUTATION case={} phase={} sourceWorld={} original={} registered={} sameOriginal={} first={} second={} moved={}",
			p.change, phase, p.level.dimension(), body(p.owner), body(registered), registered == p.owner,
			body(p.first), body(p.second), body(p.moved));
	}

	private static String body(LivingEntity entity) {
		return entity == null ? "none" : "id=" + entity.getId() + ",object=" + System.identityHashCode(entity)
			+ ",uuid=" + entity.getUUID() + ",world=" + entity.level().dimension()
			+ ",alive=" + entity.isAlive() + ",removed=" + entity.isRemoved() + ",health=" + entity.getHealth();
	}

	private static void firstStone(Probe p) {
		check(p.level.getGameTime() == p.released + 5 && p.mutated, "The observer runs after the first stone and before the second");
		check(p.firstHits == 1, "The first native victim receives exactly one admitted hit");
		boolean laterAllowed = !p.change.retiresOwner() && !p.change.changesSecond();
		check(p.secondHits == (laterAllowed ? 1 : 0), "The same stone only reaches a still-admissible later snapshot victim");
		if (p.change.retiresOwner() || p.change.changesFirst()) noChill(p.first, "No Hailfall chill resumes on the retired or departed first victim");
		else chilled(p.first);
		if (laterAllowed) chilled(p.second);
		else untouched(p.second, "The retired cloud or departed later victim receives no damage or chill");
		untouched(p.destination, "The original cloud cannot retarget the destination-world victim");
		if (p.moved != null) noChill(p.moved, "The real transferred body receives no foreign-world chill");
	}

	private static void finish(Probe p) {
		check(p.level.getGameTime() >= p.released + FINISH, "The real server outlives every scheduled ray and stone");
		if (p.change.retiresOwner() || p.change.changesFirst()) {
			check(p.firstHits == 1 && p.first.getHealth() == p.mutationHealth, "No later stone revisits the retired first victim");
			noChill(p.first, "No later chill follows first-victim invalidation");
		}
		if (p.change.retiresOwner() || p.change.changesSecond()) {
			check(p.secondHits == 0, "Every subsequent stone leaves the protected snapshot victim alone");
			untouched(p.second, "The protected later victim stays untouched through the full cloud");
		}
		if (p.moved != null) {
			noChill(p.moved, "The destination body never inherits old-world chill");
			check(p.moved.getHealth() == (p.change.changesSecond() ? 200 : p.mutationHealth), "The moved body keeps only damage admitted before its transfer");
		}
		untouched(p.destination, "The unrelated destination victim remains untouched through the full cloud");
		p.finished = true;
	}

	private static LivingEntity target(Probe p, ServerLevel level, double height) {
		LivingEntity target = HailfallReleasedOwnerTest.target(level);
		target.noPhysics = true;
		target.snapTo(FEET.x, FEET.y + height, FEET.z + ArtRules.HAIL_AHEAD, 180, 0);
		target.setDeltaMovement(Vec3.ZERO);
		p.fixtures.add(target);
		return target;
	}

	private static void chilled(LivingEntity target) {
		check(target.getHealth() < 200 && target.hasEffect(MobEffects.SLOWNESS) && target.getTicksFrozen() > 0,
			"A valid hostile victim still receives the actual stone's damage, slow and frost");
	}
	private static void noChill(LivingEntity target, String reason) {
		check(!target.hasEffect(MobEffects.SLOWNESS) && target.getTicksFrozen() == 0,
			reason + ": uuid=" + target.getUUID() + " level=" + target.level().dimension()
				+ " slowness=" + target.hasEffect(MobEffects.SLOWNESS) + " frozen=" + target.getTicksFrozen());
	}
	private static void untouched(LivingEntity target, String reason) {
		check(target.getHealth() == 200, reason + ": health=" + target.getHealth());
		noChill(target, reason);
	}
	private static void checked(Probe p, Runnable action) {
		if (p.failure != null) return;
		try { action.run(); }
		catch (RuntimeException | AssertionError failure) { p.failure = failure; p.finished = true; }
	}
	private static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
}
