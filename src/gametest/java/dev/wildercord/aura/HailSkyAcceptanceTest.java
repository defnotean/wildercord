package dev.wildercord.aura;

import com.mojang.authlib.GameProfile;
import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.ArtReleaseTargets;
import dev.wildercord.aura.world.HailSkyTrialSupport;
import dev.wildercord.aura.world.SwordMaster;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.duel.DuelRules;
import dev.wildercord.duel.Duels;
import dev.wildercord.party.HailSkyPartySupport;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Bounded native request/target acceptance checks. No direct perform call or fabricated last-hit provenance. */
public final class HailSkyAcceptanceTest implements FabricClientGameTest {
	private static final List<Integer> MARKS = List.of(SwordString.Token.marks(SwordString.Token.LEAP),
		SwordString.Token.marks(SwordString.Token.LOW));
	private static final Vec3 FEET = new Vec3(.5, 100, .5);
	private final Map<String, Integer> completed = new HashMap<>();
	private final List<Entity> fixtures = new ArrayList<>();
	private Probe active;
	private UUID owner;

	private static final class Guest extends FakePlayer {
		Guest(ServerLevel level) { super(level, new GameProfile(UUID.randomUUID(), "HailSkyGuest")); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}


	private record DamageReceipt(long tick, float amount, float lost, Entity owner, Entity applying) {}
	/** Records the real unmodified damage path, then delegates to vanilla Husk damage exactly once. */
	private static final class DamageFoe extends Husk {
		final List<DamageReceipt> artHits = new ArrayList<>();
		DamageFoe(ServerLevel level) { super(EntityTypes.HUSK, level); }
		@Override public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
			float before = getHealth();
			Entity applying = Effects.applying();
			boolean accepted = super.hurtServer(level, source, amount);
			if (source.is(Aura.DAMAGE)) artHits.add(new DamageReceipt(level.getGameTime(), amount,
				Math.max(0, before - getHealth()), source.getEntity(), applying));
			return accepted;
		}
	}

	private static final class Probe {
		final String id;
		final Map<LivingEntity, Float> baseline = new LinkedHashMap<>();
		LivingEntity target, decoy, witness, trap, central;
		Guest guest;
		SwordMaster trial;
		float trialHealth;
		ArtReleaseTargets.Release release;
		long accepted, cooldown, hookTick;
		int before;
		float cutHealth;
		boolean preObserved, noEarly, recovering, boltBeforeObserved, noEarlyBolt;
		long boltObservedAt;
		float witnessAtBolt;
		Probe(String id) { this.id = id; }
		void watch(LivingEntity body) {
			baseline.put(body, body.getHealth());
			if (body instanceof DamageFoe foe) foe.artHits.clear(); // Start receipts after the actual ordinary swing.
		}
	}

	@Override public void runTest(ClientGameTestContext context) {
		AuraApi.StringHook hook = (player, art, receipt) -> {
			Probe p = active;
			if (p == null || !player.getUUID().equals(owner) || !art.id().equals(p.id)) return;
			check(Effects.applying() == player && Effects.applyingCast() == null, "Release preserves its original Aura source");
			check(receipt.marks().size() == 2 && SwordString.Token.LEAP.fits(receipt.marks().getFirst())
				&& SwordString.Token.LOW.fits(receipt.marks().getLast()), "Hooks retain the actual observed leap/low proof");
			check(receipt.struck() == p.target, "The hook preserves the real final-hit body, including null for air strings");
			p.release = MastersArts.releaseTargets(player, art.id());
			check(p.release != null, "The actual performer and hook share their validated release receipt");
			p.hookTick = player.level().getGameTime();
			p.cutHealth = p.target == null ? 0 : p.target.getHealth();
			completed.merge(art.id(), 1, Integer::sum);
			if (art.id().equals("skyfall")) {
				Scheduler.later(ArtRules.SKYFALL_DELAY - 1, () -> {
					p.boltBeforeObserved = true;
					p.noEarlyBolt = p.baseline.entrySet().stream().allMatch(e -> e.getKey().getHealth() == e.getValue())
						&& (p.witness == null || p.witness.getHealth() == 100);
				});
				Scheduler.later(ArtRules.SKYFALL_DELAY, () -> {
					p.boltObservedAt = player.level().getGameTime();
					p.witnessAtBolt = p.witness == null ? 100 : p.witness.getHealth();
				});
			}
		};
		AuraApi.onString(hook);
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runCommand("gamerule minecraft:natural_health_regeneration false");
			world.getServer().runCommand("fill -16 99 -16 16 99 16 minecraft:stone");
			world.getServer().runCommand("fill -16 100 -16 16 108 16 minecraft:air");
			world.getServer().runOnServer(server -> {
				owner = server.getPlayerList().getPlayers().getFirst().getUUID();
				server.overworld().getGameRules().set(GameRules.PVP, true, server);
			});
			for (String id : List.of("hailfall", "skyfall")) {
				refuseUnseen(context, world, id);
				for (String cause : List.of("removed", "dead", "world", "replacement", "range", "cover", "friendly")) {
					cancelSelected(context, world, id, cause);
				}
				ground(context, world, id, false);
				ground(context, world, id, true);
			}
			hailPriority(context, world, 4, 4, true);
			hailPriority(context, world, 6, 4, false);
			hailPriority(context, world, 4, 5.5, false);
			skyConeCannotUpgrade(context, world);
			skyTrack(context, world, 3.99, true);
			skyTrack(context, world, 4, false);
			for (String id : List.of("hailfall", "skyfall")) {
				lateAdmission(context, world, id, false);
				lateAdmission(context, world, id, true);
				trialAdmission(context, world, id, false);
				trialAdmission(context, world, id, true);
			}
			context.waitTicks(32);
			world.getServer().runOnServer(server -> {
				prepare(server.getPlayerList().getPlayers().getFirst(), "hailfall");
				geometryBoundaries(server.getPlayerList().getPlayers().getFirst());
				cleanup(server.getPlayerList().getPlayers().getFirst());
			});
		} finally { active = null; AuraApi.stringHooks().remove(hook); }
	}

	private void refuseUnseen(ClientGameTestContext context, TestSingleplayerContext world, String id) {
		context.waitTicks(32);
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			prepare(player, id);
			var art = AuraApi.artOf(player, id).orElseThrow();
			check(art.stage() == AuraRules.FLOW && art.cost() == 8 && art.cooldownTicks() == 80,
				"The registered form retains its original stage, price and rest");
			float before = Aura.aura(player);
			SwordStrings.request(player, new SwordStrings.Perform(id, MARKS));
			check(Aura.aura(player) == before && SwordStrings.readyAt(player, id) == 0 && !MastersArts.committed(player),
				"Client-supplied leap/low marks without server observations cannot pay or start a timeline");
		});
	}

	/** Every timed case crosses the real request/proof-consumption boundary. */
	private Probe request(ClientGameTestContext context, TestSingleplayerContext world, String id, boolean hit,
			BiConsumer<ServerPlayer, Probe> before, BiConsumer<ServerPlayer, Probe> after) {
		context.waitTicks(32);
		Probe p = new Probe(id);
		Wildercord.LOGGER.info("[HailSkyAcceptance] Native request: {} observed victim={}", id, hit);
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			prepare(player, id);
			active = p;
			if (hit) p.target = foe(player.level(), .5, 2.2);
			player.setOnGround(false);
			check(SwordString.Token.LEAP.fits(SwordStrings.observedMarks(player)), "The first native Punch observes an actual server LEAP");
			player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
			Scheduler.later(2, () -> {
				player.setOnGround(true);
				player.setShiftKeyDown(true);
				if (hit) {
					float old = p.target.getHealth();
					player.connection.handleAttack(new ServerboundAttackPacket(p.target.getId()));
					check(p.target.getHealth() < old && player.getLastHurtMob() == p.target,
						"The final native Attack hurts this exact body and establishes observed victim provenance");
					p.target.setDeltaMovement(Vec3.ZERO);
					Effects.readyToHurt(p.target);
				}
				player.connection.handlePunch(ServerboundPunchPacket.INSTANCE);
				var art = AuraApi.artOf(player, id).orElseThrow();
				check(SwordStrings.saw(player, art), "The server saw the native leap then low suffix before requesting");
				before.accept(player, p);
				if (hit) p.watch(p.target);
				p.before = completed.getOrDefault(id, 0);
				p.accepted = player.level().getGameTime();
				p.cooldown = p.accepted + SwordStrings.rest(player, art);
				float aura = Aura.aura(player);
				double price = SwordStrings.price(player, art);
				SwordStrings.request(player, new SwordStrings.Perform(id, MARKS));
				check(MastersArts.committed(player) && Math.abs(Aura.aura(player) - (aura - price)) < .001,
					"The accepted request pays exactly once and starts its fixed windup");
				check(SwordStrings.readyAt(player, id) == p.cooldown, "Individual rest is paid at actual acceptance");
				check(!SwordStrings.saw(player, art), "Acceptance consumes the authoritative suffix");
				float paid = Aura.aura(player);
				SwordStrings.request(player, new SwordStrings.Perform(id, MARKS));
				check(Aura.aura(player) == paid && SwordStrings.readyAt(player, id) == p.cooldown,
					"Replaying the same request cannot pay or schedule twice");
				int windup = MastersStyleRules.of(id).windup();
				Scheduler.later(windup - 1, () -> {
					p.preObserved = true;
					p.noEarly = completed.getOrDefault(id, 0) == p.before && p.baseline.entrySet().stream()
						.allMatch(e -> !e.getKey().isAlive() || e.getKey().isRemoved() || e.getKey().getHealth() == e.getValue());
				});
				Scheduler.later(windup + 1, () -> p.recovering = MastersArts.committed(player));
				after.accept(player, p);
				player.setShiftKeyDown(false);
			});
		});
		context.waitTicks(3);
		return p;
	}

	private void verify(Probe p, ServerPlayer player, boolean released) {
		check(p.preObserved && p.noEarly, "No art damage or completion occurs before the advertised active tick");
		check(p.recovering, "The committed form keeps readable paid recovery after its release or target whiff");
		check(SwordStrings.readyAt(player, p.id) == p.cooldown, "Delayed effects and target invalidation retain the original paid cooldown");
		check(completed.getOrDefault(p.id, 0) == p.before + (released ? 1 : 0), "Only a real release completes, exactly once");
		if (released) {
			check(p.hookTick == p.accepted + MastersStyleRules.of(p.id).windup(), "Release occurs on its exact server tick");
			if (p.id.equals("skyfall")) check(p.boltBeforeObserved && p.noEarlyBolt && p.boltObservedAt == p.hookTick + 6,
				"The released Skyfall bolt waits exactly six server ticks without early damage");
		} else check(p.release == null, "A lost accepted victim never acquires a replacement release");
		check(Effects.applying() == null, "The release and callbacks restore source context");
	}

	private void cancelSelected(ClientGameTestContext context, TestSingleplayerContext world, String id, String cause) {
		Wildercord.LOGGER.info("[HailSkyAcceptance] Selected cancellation: {} cause={}", id, cause);
		Probe p = request(context, world, id, true, (player, probe) -> {
			move(probe.target, .5, -3.5);
			probe.decoy = foe(player.level(), .5, 2.5);
			probe.witness = foe(player.level(), .5, 4.5);
			probe.watch(probe.decoy); probe.watch(probe.witness);
		}, (player, probe) -> {
			switch (cause) {
				case "removed" -> probe.target.discard();
				case "dead" -> probe.target.setHealth(0);
				case "world" -> check(probe.target.teleportTo(player.level().getServer().getLevel(Level.NETHER), .5, 120, -3.5,
					java.util.Set.of(), 0, 0, false), "The accepted victim actually changes world");
				case "replacement" -> {
					UUID identity = probe.target.getUUID();
					probe.target.discard();
					var replacement = newFoe(player.level());
					replacement.setUUID(identity);
					move(replacement, .5, -3.5);
					check(player.level().addFreshEntity(replacement) && player.level().getEntity(identity) == replacement,
						"A distinct same-UUID replacement body is actually registered in the original world");
					probe.trap = replacement;
				}
				case "range" -> move(probe.target, .5, .5 - (id.equals("hailfall") ? 7 : 8.5));
				case "cover" -> wall(player.level(), -2, true);
				case "friendly" -> ally(player, probe.target);
				default -> throw new AssertionError(cause);
			}
		});
		context.waitTicks(MastersStyleRules.of(id).windup() + 22);
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			verify(p, player, false);
			check(untouched(p.decoy) && untouched(p.witness) && (p.trap == null || untouched(p.trap)),
				cause + " cancels the entire unreleased effect, including ordinary cone, nearest, same-UUID and ground substitutes");
			if (p.target.isAlive() && !p.target.isRemoved()) check(p.target.getHealth() == p.baseline.get(p.target),
				"An invalid accepted body gains no art damage");
		});
	}

	private void hailPriority(ClientGameTestContext context, TestSingleplayerContext world, double acceptedRange, double releaseRange, boolean direct) {
		Wildercord.LOGGER.info("[HailSkyAcceptance] Hail priority: accepted={} release={} direct={}", acceptedRange, releaseRange, direct);
		Probe p = request(context, world, "hailfall", true, (player, probe) -> move(probe.target, .5, .5 - acceptedRange), (player, probe) -> {
			move(probe.target, .5, .5 - releaseRange);
			player.setYRot(180); player.setXRot(70);
			Scheduler.later(9, () -> {
				probe.witness = foe(player.level(), .5, .5 - releaseRange);
				move(probe.target, 10.5, .5 - releaseRange);
			});
		});
		context.waitTicks(30);
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			verify(p, player, true);
			check(p.release.route() == ArtReleaseTargets.Route.HAIL_OBSERVED && p.release.point().distanceToSqr(new Vec3(.5, 100, .5 - releaseRange)) < 1e-8,
				"Hailfall samples its original selected body once at release despite free look");
			check((p.release.direct() == p.target) == direct && (p.cutHealth < p.baseline.get(p.target)) == direct,
				"The accepted under-5.5 direct bit cannot be acquired later, and expires at exact 5.5 without cancelling the cloud");
			check(p.witness.getHealth() < 100 && p.witness.hasEffect(MobEffects.SLOWNESS),
				"The first bounded near-center stone hits and chills at the frozen release point after the selected victim moves away");
			DamageReceipt stone = firstArtHit(p.witness, player);
			check(stone.tick() == p.hookTick + 4, "The first actual nested stone retains its release+4 impact tick");
			check(((DamageFoe) p.witness).artHits.size() <= 3, "A single fresh foe receives at most three native stone damage attempts");
			if (direct) {
				DamageReceipt cut = firstArtHit(p.target, player);
				check(cut.tick() == p.hookTick && close(stone.amount() / cut.amount(), .28 / .5),
					"Unmodified native damage receipts retain the .5 immediate cut versus .28 stone factor ratio");
			}
		});
	}

	private void ground(ClientGameTestContext context, TestSingleplayerContext world, String id, boolean covered) {
		Probe p = request(context, world, id, false, (player, probe) -> {}, (player, probe) -> {
			player.setYRot(180); player.setXRot(-75);
			probe.witness = foe(player.level(), .5, .5 + (id.equals("hailfall") ? ArtRules.HAIL_AHEAD : ArtRules.SKYFALL_AHEAD));
			probe.watch(probe.witness);
			if (covered) wall(player.level(), 2, true);
			else Scheduler.later(MastersStyleRules.of(id).windup() + 1, () -> {
				player.setInvulnerableTime(0);
				check(player.hurtServer(player.level(), player.level().damageSources().generic(), 1),
					"The already-released ground effect survives real owner damage");
				player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
				player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.SOVEREIGN, 4500, Aura.aura(player), 0));
			});
		});
		context.waitTicks(MastersStyleRules.of(id).windup() + 22);
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			verify(p, player, !covered);
			if (covered) check(untouched(p.witness), "New cover cancels a saved ground release without refund or replacement");
			else {
				check(p.release.route() == ArtReleaseTargets.Route.GROUND && p.release.tracking() == null,
					"An explicit no-target receipt remains ground despite a fresh target and free look");
				check(p.witness.getHealth() < 100, "The accepted ahead/floor point stays fixed through free look");
			}
		});
	}

	private void skyConeCannotUpgrade(ClientGameTestContext context, TestSingleplayerContext world) {
		Probe p = request(context, world, "skyfall", false, (player, probe) -> {
			probe.decoy = foe(player.level(), .5, 5.5); probe.watch(probe.decoy);
		}, (player, probe) -> {
			move(probe.decoy, .5, 7.5);
			probe.witness = foe(player.level(), .5, 2.5); probe.watch(probe.witness);
		});
		context.waitTicks(28);
		world.getServer().runOnServer(server -> {
			verify(p, server.getPlayerList().getPlayers().getFirst(), false);
			check(untouched(p.decoy) && untouched(p.witness), "A selected cone body beyond 6 plus width never upgrades to observed 8.5 or reselects a closer entrant");
		});
	}

	private void skyTrack(ClientGameTestContext context, TestSingleplayerContext world, double movement, boolean tracks) {
		Wildercord.LOGGER.info("[HailSkyAcceptance] Sky tracking: movement={} tracks={}", movement, tracks);
		Probe p = request(context, world, "skyfall", true, (player, probe) -> {
			move(probe.target, .5, -6.5);
			probe.decoy = foe(player.level(), .5, 2.5); probe.watch(probe.decoy);
		}, (player, probe) -> Scheduler.later(7, () -> {
			move(probe.target, .5 + movement, -6.5);
			probe.witness = foe(player.level(), 1.5 + movement, -6.5);
			probe.central = foe(player.level(), .5, -6.5);
			probe.trap = foe(player.level(), -6, -6.5);
			foe(player.level(), -2.5, -6.5);
			wall(player.level(), -2, true);
			player.setInvulnerableTime(0);
			check(player.hurtServer(player.level(), player.level().damageSources().generic(), 1), "Actual post-release damage interrupts only the physical pose");
			player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.SOVEREIGN, 4500, Aura.aura(player), 0));
		}));
		context.waitTicks(16);
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			verify(p, player, true);
			check(p.release.route() == ArtReleaseTargets.Route.SKY_OBSERVED && p.release.tracking() == p.target,
				"The actual observed last-hit wins over a closer cone body");
			check((p.witnessAtBolt < 100) == tracks && (p.witness.getHealth() < 100) == tracks,
				"Only a strict-under-four move tracks at release+6: its far-side witness is outside every frozen-origin arc");
			DamageReceipt centre = firstArtHit(tracks ? p.witness : p.central, player);
			DamageReceipt arc = firstArtHit(tracks ? p.central : p.target, player);
			check(centre.tick() == p.hookTick + 6 && arc.tick() == centre.tick() && close(centre.amount() / arc.amount(), .95 / .4),
				"The actual bolt and fixed-origin arcs retain their .95/.4 native damage ratio on the exact +6 beat");
			check(untouched(p.trap), "An arc never walks its origin to a previous arc recipient");
			check(untouched(p.decoy), "Released lightning does not replace the primary with a nearer forward body");
		});
	}

	private void lateAdmission(ClientGameTestContext context, TestSingleplayerContext world, String id, boolean duel) {
		Probe p = request(context, world, id, false, (player, probe) -> {}, (player, probe) -> Scheduler.later(MastersStyleRules.of(id).windup() + 1, () -> {
			double z = .5 + (id.equals("hailfall") ? ArtRules.HAIL_AHEAD : ArtRules.SKYFALL_AHEAD);
			probe.guest = new Guest(player.level()); fixtures.add(probe.guest);
			probe.guest.setGameMode(GameType.SURVIVAL); probe.guest.setNoGravity(true); move(probe.guest, .5, z);
			player.level().addNewPlayer(probe.guest);
			check(ArtKit.harmable(player, probe.guest), "The arriving recipient is initially admitted");
			HailSkyPartySupport.join(player, probe.guest);
			check(!ArtKit.harmable(player, probe.guest), "A real late party membership change protects this recipient");
			if (duel) {
				var rules = Duels.startBout(player, probe.guest, new DuelRules.Terms(40, 0, 600, 0, 1, false), FEET, null);
				rules.tick(player.level().getGameTime());
				check(ArtKit.harmable(player, probe.guest), "An explicit fighting duel admits its opponent despite party membership");
			}
			probe.witness = foe(player.level(), 1, z);
			wall(player.level(), 2, true);
		}));
		context.waitTicks(MastersStyleRules.of(id).windup() + (id.equals("hailfall") ? 7 : 8));
		world.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().getFirst();
			verify(p, player, true);
			check(p.witness.getHealth() < 100, "A fresh lawful area entrant is hit through post-release cover");
			if (duel) check(p.guest.getHealth() < 20, "Damage admission is reevaluated after the late party/duel transitions");
			else check(untouchedGuest(p.guest), "The late ally receives no damage, slow, freeze meter, weakness, ionisation or stance wear");
		});
	}


	private void trialAdmission(ClientGameTestContext context, TestSingleplayerContext world, String id, boolean leave) {
		Wildercord.LOGGER.info("[HailSkyAcceptance] Trial admission: {} leave={}", id, leave);
		Probe p = request(context, world, id, false, (player, probe) -> {}, (player, probe) -> Scheduler.later(MastersStyleRules.of(id).windup() + 1, () -> {
			double z = .5 + (id.equals("hailfall") ? ArtRules.HAIL_AHEAD : ArtRules.SKYFALL_AHEAD);
			probe.guest = new Guest(player.level()); fixtures.add(probe.guest);
			probe.guest.setGameMode(GameType.SURVIVAL); probe.guest.setNoGravity(true);
			move(probe.guest, 8.5, .5); player.level().addNewPlayer(probe.guest);
			probe.trial = HailSkyTrialSupport.start(player, probe.guest, .5, z); fixtures.add(probe.trial);
			probe.trialHealth = probe.trial.getHealth();
			check(ArtKit.harmable(player, probe.trial), "A trial begun after release admits its actual enrolled owner at the delayed sink");
			if (leave) {
				HailSkyTrialSupport.removeEnrollment(player, probe.trial);
				check(!ArtKit.harmable(player, probe.trial), "A pruned former participant is now an actual protected bystander to this trial");
			}
			probe.witness = foe(player.level(), 1, z);
		}));
		context.waitTicks(MastersStyleRules.of(id).windup() + 22);
		world.getServer().runOnServer(server -> {
			verify(p, server.getPlayerList().getPlayers().getFirst(), true);
			check(p.witness.getHealth() < 100, "The original released effect still reaches an ordinary lawful area entrant beside the trial");
			if (leave) check(p.trial.getHealth() == p.trialHealth && p.trial.getTicksFrozen() == 0
				&& !p.trial.hasEffect(MobEffects.SLOWNESS) && !p.trial.hasEffect(MobEffects.WEAKNESS)
				&& !Reactions.has(p.trial, Reactions.Mark.IONISED) && Stance.state(p.trial) == null,
				"Lost trial admission vetoes damage, chill/freeze, interrupt/hold, ionisation and stance mutation at the delayed sink");
			else check(p.trial.getHealth() < p.trialHealth, "A newly started trial accepts delayed harm from the still enrolled original owner");
			check(untouchedGuest(p.guest), "The other participant outside the effect receives no stray damage or control");
		});
	}

	/** Supplemental exact-boundary probes exercise receipts directly; these do not substitute for the native requests above. */
	private void geometryBoundaries(ServerPlayer player) {
		for (String id : List.of("hailfall", "skyfall")) {
			prepare(player, id);
			var art = AuraApi.artOf(player, id).orElseThrow();
			var context = new AuraApi.StringContext(art, MARKS, null, player.level().getGameTime());
			var ground = ArtReleaseTargets.accept(player, context, new Vec3(0, 0, 1), new Vec3(0, 0, 1));
			check(ground != null, "Clear accepted ground is available");
			Vec3 point = ground.release(player).point();
			double range = id.equals("hailfall") ? 7 : 6;
			player.teleportTo(point.x, point.y, point.z - range);
			check((ground.release(player) != null) == id.equals("skyfall"), "Ground range is exact: Hailfall strictly under 7, Skyfall inclusive 6");
			player.teleportTo(point.x, point.y, point.z - range + .001);
			check(ground.release(player) != null, "The same fixed ground point is legal just within its 3D range");
			player.teleportTo(point.x, point.y + .1, point.z - range);
			check(ground.release(player) == null, "Ground range uses 3D distance, not just a horizontal radius");
			player.teleportTo(FEET.x, FEET.y, FEET.z);
			wall(player.level(), 2, true);
			check(ArtReleaseTargets.accept(player, context, new Vec3(0, 0, 1), new Vec3(0, 0, 1)) == null,
				"Ground cover is checked at acceptance, before a paid timeline exists");
			wall(player.level(), 2, false);
			var observed = foe(player.level(), .5, .5 - (id.equals("hailfall") ? 7 : 8.5));
			var struck = new AuraApi.StringContext(art, MARKS, observed, player.level().getGameTime());
			var outside = ArtReleaseTargets.accept(player, struck, new Vec3(0, 0, 1), new Vec3(0, 0, 1));
			check(outside != null && outside.release(player).route() == ArtReleaseTargets.Route.GROUND,
				"The exact observed radius boundary grants no selected-body route at acceptance");
			move(observed, .5, .501 - (id.equals("hailfall") ? 7 : 8.5));
			var inside = ArtReleaseTargets.accept(player, struck, new Vec3(0, 0, 1), new Vec3(0, 0, 1));
			check(inside != null && inside.release(player).route() != ArtReleaseTargets.Route.GROUND,
				"A visible observed body strictly inside its original radius retains that route");
			wall(player.level(), -2, true);
			var hidden = ArtReleaseTargets.accept(player, struck, new Vec3(0, 0, 1), new Vec3(0, 0, 1));
			check(hidden != null && hidden.release(player).route() == ArtReleaseTargets.Route.GROUND,
				"Covered observed bodies are not admitted at acceptance even when the ahead ground remains clear");
			wall(player.level(), -2, false);
		}
		prepare(player, "skyfall");
		var art = AuraApi.artOf(player, "skyfall").orElseThrow();
		var body = foe(player.level(), .5, 5.5);
		var context = new AuraApi.StringContext(art, MARKS, null, player.level().getGameTime());
		var cone = ArtReleaseTargets.accept(player, context, new Vec3(0, 0, 1), new Vec3(0, 0, 1));
		check(cone != null && cone.release(player).route() == ArtReleaseTargets.Route.SKY_CONE, "Supplemental body starts on the cone route");
		move(body, .5, .5 + 6 + body.getBbWidth() / 2);
		check(cone.release(player) != null, "Cone reach includes the accepted body's half width at its exact edge");
		move(body, .5, .501 + 6 + body.getBbWidth() / 2);
		check(cone.release(player) == null, "Cone reach cannot upgrade to the selected-victim radius");
		body.snapTo(.5, Math.nextDown(102.2), 5.5, 0, 0);
		check(cone.release(player) != null, "The accepted cone remains valid immediately inside its original vertical bound");
		body.snapTo(.5, 102.201, 5.5, 0, 0);
		check(cone.release(player) == null, "Cone vertical escape suppresses release");
		move(body, 4, 3.99);
		check(cone.release(player) == null, "A body leaving the committed 90-degree cone cannot gain an observed route");
	}

	private void prepare(ServerPlayer player, String id) {
		cleanup(player);
		check(!MastersArts.committed(player), "A preceding isolated probe has completed recovery");
		player.setGameMode(GameType.SURVIVAL);
		player.teleportTo(FEET.x, FEET.y, FEET.z);
		player.setNoGravity(true); player.setDeltaMovement(Vec3.ZERO);
		player.setHealth(player.getMaxHealth()); player.clearFire(); player.removeAllEffects();
		player.setYRot(0); player.setXRot(0); player.setShiftKeyDown(false); player.setSprinting(false); player.setOnGround(true);
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(ArtRules.art(id).method(), AuraRules.SOVEREIGN, 4500, 160, 0));
		player.setAttached(SwordStrings.COOLDOWNS, SwordStrings.Cooldowns.NONE);
		SwordStrings.forget(player.getUUID());
	}

	private void cleanup(ServerPlayer player) {
		Duels.callOff(player);
		HailSkyPartySupport.leave(player);
		var level = player.level();
		level.getScoreboard().removePlayerFromTeam(player.getScoreboardName());
		for (Entity entity : fixtures) {
			if (entity instanceof ServerPlayer guest) HailSkyPartySupport.leave(guest);
			var transferred = level.getServer().getLevel(Level.NETHER).getEntity(entity.getUUID());
			if (transferred != null) transferred.discard();
			level.getScoreboard().removePlayerFromTeam(entity.getScoreboardName());
			entity.discard();
		}
		fixtures.clear();
		wall(level, -2, false); wall(level, 2, false);
	}

	private Mob newFoe(ServerLevel level) {
		Mob body = new DamageFoe(level);
		fixtures.add(body); body.addTag("wildercord.rolled"); body.setNoAi(true); body.setNoGravity(true);
		body.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
		body.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
		var reinforcements = body.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
		if (reinforcements != null) reinforcements.setBaseValue(0);
		body.setHealth(100);
		return body;
	}
	private Mob foe(ServerLevel level, double x, double z) {
		Mob body = newFoe(level); move(body, x, z); level.addFreshEntity(body); return body;
	}
	private static void move(LivingEntity body, double x, double z) { body.snapTo(x, 100, z, 180, 0); body.setDeltaMovement(Vec3.ZERO); }
	private static void wall(ServerLevel level, int z, boolean solid) {
		for (int x = -3; x <= 3; x++) for (int y = 100; y <= 103; y++) level.setBlockAndUpdate(new BlockPos(x, y, z),
			(solid ? Blocks.STONE : Blocks.AIR).defaultBlockState());
	}
	private static void ally(ServerPlayer player, LivingEntity target) {
		var scoreboard = player.level().getScoreboard();
		var team = scoreboard.getPlayerTeam("hail_sky_friends");
		if (team == null) team = scoreboard.addPlayerTeam("hail_sky_friends");
		team.setAllowFriendlyFire(false);
		scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
		scoreboard.addPlayerToTeam(target.getScoreboardName(), team);
		check(!ArtKit.harmable(player, target), "The accepted victim is now actually friendly");
	}
	private static boolean untouched(LivingEntity body) {
		return body.getHealth() == 100 && body.getTicksFrozen() == 0 && !body.hasEffect(MobEffects.SLOWNESS)
			&& !body.hasEffect(MobEffects.WEAKNESS) && !Reactions.has(body, Reactions.Mark.IONISED) && Stance.state(body) == null;
	}
	private static boolean untouchedGuest(Guest body) {
		return body.getHealth() == 20 && body.getTicksFrozen() == 0 && !body.hasEffect(MobEffects.SLOWNESS)
			&& !body.hasEffect(MobEffects.WEAKNESS) && !Reactions.has(body, Reactions.Mark.IONISED) && Stance.state(body) == null;
	}

	private static DamageReceipt firstArtHit(LivingEntity body, ServerPlayer player) {
		check(body instanceof DamageFoe && !((DamageFoe) body).artHits.isEmpty(), "A native art damage receipt must exist");
		DamageReceipt hit = ((DamageFoe) body).artHits.getFirst();
		check(hit.amount() > 0 && hit.lost() > 0 && hit.owner() == player && hit.applying() == player,
			"Native art damage both lands and retains the exact owner/source through the callback");
		return hit;
	}
	private static boolean close(double first, double second) { return Math.abs(first - second) < 1e-4; }

	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
