package dev.wildercord.party;

import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.MastersArts;
import dev.wildercord.aura.MastersStyleRules;
import dev.wildercord.aura.Momentum;
import dev.wildercord.aura.SwordString;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.aura.arts.ArtFields;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.CrimsonArts;
import dev.wildercord.aura.arts.HollowArts;
import dev.wildercord.aura.arts.MethodArts;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.Scheduler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Actual paid ground releases, with native movement, collision geometry, field ticks and damage/healing receipts.
 * Party/duel changes inside damage callbacks remain covered by ArtFieldMutationSafetyTest; real original-body
 * retirement is covered by GroundFieldReleasedOwnerTest. This fixture never calls a performer or field pulse directly.
 */
public final class MastersGroundFieldTimelineTest implements FabricClientGameTest {

	private static final List<Integer> MARKS = List.of(SwordString.Token.SWING.bit() | SwordString.Token.LEAP.bit(),
		SwordString.Token.SWING.bit() | SwordString.Token.LOW.bit());
	private static final Vec3 FEET = new Vec3(.5, 100, .5);
	private static final float HEALTH = 200;
	private static Probe current;
	private static boolean listening;
	private record Damage(LivingEntity target, long age, float taken) {}
	private enum Healing { SHARE, CAP, SHARED_BUCKET, BURST_KILL, RAIN_KILL }

	private static final class Probe {
		final ServerPlayer owner;
		final ServerLevel level;
		final String id;
		final List<Mob> targets = new ArrayList<>();
		final Map<Mob, Vec3> positions = new IdentityHashMap<>();
		final Map<LivingEntity, Float> health = new IdentityHashMap<>();
		final List<Damage> damage = new ArrayList<>();
		ArtFields.Field field;
		LivingEntity oldStruck;
		Consumer<Probe> afterRelease = p -> {};
		Consumer<Probe> afterTick = p -> {};
		Consumer<Probe> afterDamage = p -> {};
		long accepted, released = -1, cooldown;
		int spends, completions;
		double price, paid;
		boolean resetPositions, started;
		Throwable failure;
		Healing healing;
		double expectedHealing, bucket, totalTaken;
		long bucketAt;
		float initialOwnerHealth;

		Probe(ServerPlayer owner, String id) { this.owner = owner; this.level = owner.level(); this.id = id; }
		String kind() { return id.equals(HollowArts.COLLAPSE) ? HollowArts.WELL : CrimsonArts.RAIN; }
		int lifetime() { return id.equals(HollowArts.COLLAPSE) ? ArtRules.COLLAPSE_TICKS : ArtRules.RAIN_TICKS; }
		double ahead() { return id.equals(HollowArts.COLLAPSE) ? ArtRules.COLLAPSE_AHEAD : ArtRules.RAIN_AHEAD; }
		long age() { return level.getGameTime() - accepted - 8; }
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		installObservers();
		AuraApi.StringHook release = MastersGroundFieldTimelineTest::released;
		AuraApi.SpendHook spend = (owner, paid, reason, backlash) -> {
			Probe p = current;
			if (p != null && owner == p.owner && reason.equals("art:" + p.id)) checked(p, () -> {
				check(!backlash, "The admitted ground form is fully funded");
				p.spends++; p.paid += paid;
			});
		};
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runCommand("gamerule minecraft:natural_health_regeneration false");
			world.getServer().runOnServer(server -> {
				arena(server.overworld());
				arena(server.getLevel(Level.NETHER));
				AuraApi.onString(release);
				AuraApi.onSpend(spend);
			});
			try {
				for (String id : List.of(HollowArts.COLLAPSE, CrimsonArts.RED_RAIN)) {
					refusals(context, world, id);
					for (String cause : List.of("damage", "weapon round trip", "method", "spectator", "world", "death")) {
						cancelled(context, world, id, cause);
					}
					anchor(context, world, id, true);
					anchor(context, world, id, false);
					cover(context, world, id);
				}
				burstVisibilityCap(context, world);
				collapsePulseTimes(context, world);
				for (Healing healing : Healing.values()) rainHealing(context, world, healing);
				burstOwnerRoundTrip(context, world);
			} finally {
				world.getServer().runOnServer(server -> {
					cleanup();
					AuraApi.stringHooks().remove(release);
					AuraApi.spendHooks().remove(spend);
				});
			}
		}
	}

	private static void installObservers() {
		if (listening) return;
		listening = true;
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			Probe p = current;
			if (p == null || p.level.getServer() != server || !p.resetPositions) return;
			checked(p, () -> p.positions.forEach((target, at) -> {
				if (target.isAlive()) move(target, at);
			}));
		});
		// Registered after production listeners: observe the real field's result once per native server tick.
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Probe p = current;
			if (p == null || p.level.getServer() != server || !p.started) return;
			checked(p, () -> {
				p.afterTick.accept(p);
				if (p.healing != null && p.age() >= 0) near(p.owner.getHealth(), p.initialOwnerHealth + p.expectedHealing,
					"Every completed tick heals only the admitted actual health loss, art cap and shared bucket allowance");
			});
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, base, damage, blocked) -> recordDamage(target, source));
		// Fabric skips AFTER_DAMAGE for a lethal hit; AFTER_DEATH runs inside die(), before the art's drink resumes.
		ServerLivingEntityEvents.AFTER_DEATH.register(MastersGroundFieldTimelineTest::recordDamage);
	}

	private static void recordDamage(LivingEntity target, DamageSource source) {
		Probe p = current;
		if (p == null || source.getEntity() != p.owner || !p.health.containsKey(target)) return;
		checked(p, () -> {
			float before = p.health.put(target, target.getHealth());
			float taken = Math.max(0, before - Math.max(0, target.getHealth()));
			// A duplicate notification cannot manufacture another hit or another share of the same health loss.
			if (taken <= 0) return;
			check(Effects.applying() == p.owner && Effects.applyingCast() == null, "Damage retains this art's owner and source scope");
			p.damage.add(new Damage(target, p.age(), taken));
			if (p.healing != null) {
				long now = p.level.getGameTime();
				p.bucket = Math.max(0, p.bucket - (now - p.bucketAt) * ArtRules.MEND_CAP / ArtRules.MEND_WINDOW);
				p.bucketAt = now;
				double heal = Math.min(taken * .30, Math.min(4 - p.expectedHealing, ArtRules.MEND_CAP - p.bucket));
				p.expectedHealing += Math.max(0, heal);
				p.bucket += Math.max(0, heal);
				p.totalTaken += taken;
			}
			p.afterDamage.accept(p);
		});
	}

	private static void released(ServerPlayer owner, AuraApi.StringArt art, AuraApi.StringContext receipt) {
		Probe p = current;
		if (p == null || owner != p.owner || !art.id().equals(p.id)) return;
		checked(p, () -> {
			p.completions++;
			p.released = p.level.getGameTime();
			check(p.released == p.accepted + 8, "The actual performer completes at precisely acceptance +8");
			check(receipt.at() == p.accepted && receipt.marks().equals(MARKS), "Completion preserves the original acceptance time and authoritative marks");
			check(receipt.struck() == p.oldStruck, "GROUND_AHEAD leaves the observed swing receipt intact for hooks");
			p.field = field(p.kind());
			p.afterRelease.accept(p);
		});
	}

	private static void refusals(ClientGameTestContext context, TestSingleplayerContext world, String id) {
		context.waitTicks(30);
		world.getServer().runOnServer(server -> {
			Probe p = prepare(server, id);
			var art = AuraApi.artOf(p.owner, id).orElseThrow();
			var style = MastersStyleRules.of(id);
			check(style != null && style.animation() == (id.equals(HollowArts.COLLAPSE) ? 17 : 18)
				&& style.windup() == 8 && style.recovery() == (id.equals(HollowArts.COLLAPSE) ? 18 : 16)
				&& style.targets() == MastersStyleRules.TargetPolicy.GROUND_AHEAD, "Each form has its explicit untargeted ground profile");
			check(art.stage() == AuraRules.FLOW && art.cost() == 8 && art.cooldownTicks() == 80
				&& art.string().fits(MARKS.stream().mapToInt(Integer::intValue).toArray()), "The original base Flow leap/low economy is unchanged");
			p.owner.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
			check(!SwordStrings.perform(p.owner, art, MARKS), "No blade refuses before payment");
			p.owner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
			p.owner.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("rime", AuraRules.SOVEREIGN, 4500, 160, 0));
			check(!SwordStrings.perform(p.owner, art, MARKS), "The wrong method refuses before payment");
			check(p.spends == 0 && p.completions == 0 && !MastersArts.committed(p.owner) && Aura.aura(p.owner) == 160,
				"Rejected ground forms have no spend, completion or physical commitment");
		});
	}

	private static void cancelled(ClientGameTestContext context, TestSingleplayerContext world, String id, String cause) {
		context.waitTicks(30);
		world.getServer().runOnServer(server -> {
			Probe p = prepare(server, id);
			Mob target = target(p, FEET.add(0, 0, p.ahead()), HEALTH);
			accept(p);
			Scheduler.later(4, () -> checked(p, () -> {
				switch (cause) {
					case "damage" -> {
						p.owner.setInvulnerableTime(0);
						check(p.owner.hurtServer(p.level, p.level.damageSources().generic(), 1), "Native accepted damage interrupts the windup");
					}
					case "weapon round trip" -> {
						int slot = p.owner.getInventory().getSelectedSlot();
						p.owner.getInventory().setSelectedSlot((slot + 1) % 9);
						p.owner.getInventory().setSelectedSlot(slot);
					}
					case "method" -> p.owner.setAttached(AuraAttachments.AURA,
						new AuraAttachments.Data("rime", AuraRules.SOVEREIGN, 4500, Aura.aura(p.owner), 0));
					case "spectator" -> p.owner.setGameMode(GameType.SPECTATOR);
					case "world" -> check(p.owner.teleportTo(server.getLevel(Level.NETHER), .5, 100, .5, Set.of(), 0, 0, false), "Native world departure interrupts the windup");
					case "death" -> { p.owner.kill(p.level); check(!p.owner.isAlive(), "Native death occurred before release"); }
					default -> throw new AssertionError(cause);
				}
				check(MastersArts.committed(p.owner), "The cancelled windup retains its paid recovery");
			}));
			p.afterTick = q -> {
				check(target.getHealth() == HEALTH && target.getDeltaMovement().lengthSqr() == 0,
					"A cancelled windup never bursts, pulls, crushes or bleeds");
				check(ArtFields.count(q.owner, q.kind()) == 0, "A cancelled windup never opens a field");
			};
		});
		context.waitTicks(12);
		world.getServer().runOnServer(server -> {
			Probe p = current;
			verify(p, false);
			ServerPlayer player = server.getPlayerList().getPlayer(p.owner.getUUID());
			if (!player.isAlive()) {
				player.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
				player = server.getPlayerList().getPlayer(p.owner.getUUID());
				check(player != p.owner && player.isAlive(), "A real respawn restores a different connected body after the cancellation test");
			}
			player.setGameMode(GameType.SURVIVAL);
			player.teleportTo(p.level, FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false);
			p.afterTick = q -> {};
		});
		context.runOnClient(mc -> mc.gui.setScreen(null));
	}

	private static void anchor(ClientGameTestContext context, TestSingleplayerContext world, String id, boolean floor) {
		context.waitTicks(30);
		world.getServer().runOnServer(server -> {
			Probe p = prepare(server, id);
			// A real displacement changes the anchor by eight blocks; camera reversal cannot change accepted facing.
			Vec3 acceptedFeet = FEET.add(0, floor ? 2 : 12, 0);
			p.owner.teleportTo(acceptedFeet.x, acceptedFeet.y, acceptedFeet.z);
			Vec3 releaseFeet = acceptedFeet.add(8, 0, 0);
			Vec3 expected = releaseFeet.add(0, 0, p.ahead());
			if (floor) expected = new Vec3(expected.x, 100, expected.z);
			Vec3 centre = expected;
			p.oldStruck = target(p, acceptedFeet.add(0, 0, -10), HEALTH);
			p.owner.setLastHurtMob(p.oldStruck);
			Mob oldAnchor = target(p, new Vec3(acceptedFeet.x, expected.y, expected.z), HEALTH);
			Mob actualAnchor = target(p, expected, HEALTH);
			p.resetPositions = true;
			p.afterRelease = q -> {
				check(q.field != null && q.field.shape().centre().distanceTo(centre) < 1.0E-5,
					"The field uses release-time owner feet, accepted level facing and the original floor/fallback search");
				check(q.owner.position().distanceTo(releaseFeet) < 1.0E-5, "The owner really moved during the native windup");
				check(q.owner.getYRot() == 180, "The camera really reversed before release");
				q.owner.teleportTo(q.level, 18.5, acceptedFeet.y, -8.5, Set.of(), 90, 0, false);
			};
			p.afterTick = q -> {
				if (q.age() < 0) check(q.damage.isEmpty() && ArtFields.count(q.owner, q.kind()) == 0,
					"The fixed windup creates no ground field or early damage");
				if (q.field != null) check(q.field.shape().centre().distanceTo(centre) < 1.0E-5,
					"A released ground centre stays fixed after owner movement and free look");
				check(oldAnchor.getHealth() == HEALTH && p.oldStruck.getHealth() == HEALTH,
					"The accepted origin and stale swing victim never anchor or receive this release");
			};
			accept(p);
			Scheduler.later(6, () -> checked(p, () -> {
				Vec3 before = p.owner.position();
				p.owner.move(MoverType.SELF, new Vec3(8, 0, 0));
				check(p.owner.getX() - before.x > 7.9, "Native collision-aware movement advances through the clear arena");
				// Synchronize the moved native position to the real client, so an old client packet cannot undo it.
				p.owner.teleportTo(p.level, p.owner.getX(), p.owner.getY(), p.owner.getZ(), Set.of(), 180, 0, false);
			}));
		});
		context.waitTicks(8 + (id.equals(HollowArts.COLLAPSE) ? ArtRules.COLLAPSE_TICKS : ArtRules.RAIN_TICKS) + 5);
		world.getServer().runOnServer(server -> {
			Probe p = current;
			verify(p, true);
			check(p.targets.getLast().getHealth() < HEALTH, "The actual release-location victim receives the native field's damage");
			check(ArtFields.count(p.owner, p.kind()) == 0, "The original field expires on its own lifetime");
			Wildercord.LOGGER.info("GROUND_FIELD_ANCHOR art={} floor={} accepted={} release={} centre={}", id, floor, p.accepted, p.released, p.field.shape().centre());
		});
	}

	private static void cover(ClientGameTestContext context, TestSingleplayerContext world, String id) {
		context.waitTicks(30);
		world.getServer().runOnServer(server -> {
			Probe p = prepare(server, id);
			for (int y = 100; y <= 104; y++) p.level.setBlock(new BlockPos(0, y, 1), Blocks.STONE.defaultBlockState(), 2);
			Mob hidden = target(p, FEET.add(0, 0, p.ahead()), HEALTH);
			Mob visible = target(p, FEET.add(3, 0, p.ahead()), HEALTH);
			check(!p.owner.hasLineOfSight(hidden) && p.owner.hasLineOfSight(visible), "Native block collision hides only the centre target");
			p.resetPositions = true;
			p.afterRelease = q -> {
				check(hidden.getHealth() == HEALTH, "The opening ground release does not hit through native cover");
				if (id.equals(CrimsonArts.RED_RAIN)) check(visible.getHealth() < HEALTH, "Red Rain's visible opening burst still lands");
			};
			accept(p);
		});
		context.waitTicks(8 + (id.equals(HollowArts.COLLAPSE) ? ArtRules.COLLAPSE_TICKS : ArtRules.BLEED_PERIOD) + 5);
		world.getServer().runOnServer(server -> {
			Probe p = current;
			verify(p, true);
			Mob hidden = p.targets.getFirst();
			check(!p.owner.hasLineOfSight(hidden) && hidden.getHealth() < HEALTH,
				"The independent rain/crush keeps its original ground-area cover behavior");
		});
	}

	private static void collapsePulseTimes(ClientGameTestContext context, TestSingleplayerContext world) {
		context.waitTicks(30);
		List<Long> pulls = new ArrayList<>();
		world.getServer().runOnServer(server -> {
			Probe p = prepare(server, HollowArts.COLLAPSE);
			Vec3 centre = FEET.add(0, 0, p.ahead());
			Mob pulled = target(p, centre.add(3, 0, 0), HEALTH);
			Mob crushed = target(p, centre, HEALTH);
			p.resetPositions = true;
			p.afterTick = q -> {
				long age = q.age();
				if (age < 0 || age > 18) return;
				boolean moving = pulled.getDeltaMovement().horizontalDistanceSqr() > 1.0E-8;
				if (moving) pulls.add(age);
				check(moving == (age >= 2 && age <= 16 && age % 2 == 0), "Collapse pulls on exactly +2,+4,...,+16");
				if (moving) near(pulled.getDeltaMovement().x, -ArtRules.COLLAPSE_DRAG, "The native pull keeps its original .3 speed");
				if (age < 16) check(crushed.getHealth() == HEALTH, "Collapse has no early crush damage");
				if (age == 16) check(crushed.getHealth() < HEALTH && Reactions.has(crushed, Reactions.Mark.SHADOWED),
					"The native +16 final crush damages and shadows its central target");
			};
			accept(p);
		});
		context.waitTicks(29);
		world.getServer().runOnServer(server -> {
			Probe p = current;
			verify(p, true);
			check(pulls.equals(List.of(2L, 4L, 6L, 8L, 10L, 12L, 14L, 16L)), "Exactly eight native pulls occur");
			check(p.damage.size() == 1 && p.damage.getFirst().age == 16, "The actual crush runs once, at +16");
		});
	}

	private static void burstVisibilityCap(ClientGameTestContext context, TestSingleplayerContext world) {
		context.waitTicks(30);
		world.getServer().runOnServer(server -> {
			Probe p = prepare(server, CrimsonArts.RED_RAIN);
			for (int y = 100; y <= 104; y++) p.level.setBlock(new BlockPos(0, y, 1), Blocks.STONE.defaultBlockState(), 2);
			Vec3 centre = FEET.add(0, 0, p.ahead());
			List<Mob> hidden = new ArrayList<>();
			List<Mob> visible = new ArrayList<>();
			for (int i = 0; i < 6; i++) hidden.add(target(p, centre.add((i % 3 - 1) * .1, 0, (i / 3) * .1), HEALTH));
			for (int i = 0; i < 7; i++) visible.add(target(p, centre.add(2.7 + i * .1, 0, 0), HEALTH));
			check(hidden.stream().noneMatch(p.owner::hasLineOfSight) && visible.stream().allMatch(p.owner::hasLineOfSight),
				"Six nearer candidates are hidden by real blocks and seven farther candidates are in sight");
			p.resetPositions = true;
			p.afterRelease = q -> {
				check(hidden.stream().allMatch(target -> target.getHealth() == HEALTH), "No hidden candidate receives the opening burst");
				check(visible.stream().filter(target -> target.getHealth() < HEALTH).count() == ArtRules.RAIN_TARGETS,
					"LOS filtering precedes the unchanged six-victim burst cap");
				check(q.damage.size() == 6 && q.damage.stream().allMatch(hit -> hit.age == 0),
					"Exactly six real visible opening hits land at the release frame");
			};
			accept(p);
		});
		context.waitTicks(14);
		world.getServer().runOnServer(server -> verify(current, true));
	}

	private static void rainHealing(ClientGameTestContext context, TestSingleplayerContext world, Healing healing) {
		context.waitTicks(30);
		world.getServer().runOnServer(server -> {
			Probe p = prepare(server, CrimsonArts.RED_RAIN);
			p.healing = healing;
			p.owner.setHealth(1);
			if (healing == Healing.SHARED_BUCKET) {
				near(ArtKit.mend(p.owner, p.owner, 10), 10, "An actual prior art mend fills the shared healing bucket");
				p.bucket = 10;
			}
			p.owner.setHealth(5);
			p.initialOwnerHealth = 5;
			p.bucketAt = p.level.getGameTime();
			Vec3 centre = FEET.add(0, 0, p.ahead());
			int count = healing == Healing.CAP || healing == Healing.SHARED_BUCKET ? 6 : 1;
			if (healing != Healing.RAIN_KILL) {
				for (int i = 0; i < count; i++) target(p, centre.add((i % 3 - 1) * .5, 0, (i / 3) * .5),
					healing == Healing.BURST_KILL ? .1F : HEALTH);
			}
			p.resetPositions = true;
			p.afterRelease = q -> {
				if (healing == Healing.RAIN_KILL) target(q, centre, .1F);
			};
			accept(p);
		});
		context.waitTicks(8 + ArtRules.RAIN_TICKS + 5);
		world.getServer().runOnServer(server -> {
			Probe p = current;
			verify(p, true);
			for (Mob target : p.targets) {
				List<Long> ages = p.damage.stream().filter(hit -> hit.target == target).map(Damage::age).toList();
				List<Long> expected = switch (healing) {
					case BURST_KILL -> List.of(0L);
					case RAIN_KILL -> List.of(10L);
					default -> List.of(0L, 10L, 20L, 30L, 40L);
				};
				check(ages.equals(expected), "Red Rain has exactly the opening strike and four native bleed pulses: " + ages);
			}
			if (healing == Healing.CAP) near(p.expectedHealing, 4, "One shared burst/rain drink reaches exactly the four-health cap");
			if (healing == Healing.SHARE) near(p.expectedHealing, Math.min(4, p.totalTaken * .30), "Red Rain returns thirty percent of actual health taken");
			if (healing == Healing.SHARED_BUCKET) check(p.expectedHealing > 0 && p.expectedHealing < 3,
				"The real prefilled shared mend bucket limits drinking below the per-art cap");
			if (healing == Healing.BURST_KILL || healing == Healing.RAIN_KILL) {
				check(!p.targets.getFirst().isAlive(), "The native admitted strike kills its low-health target");
				near(p.totalTaken, .1, "A lethal overkill receipt counts only actual remaining health");
				near(p.expectedHealing, .03, "A valid owner can still drink from admitted kill damage");
			}
			check(ArtFields.count(p.owner, CrimsonArts.RAIN) == 0, "No fifth bleed pulse or renewed field follows +40");
			Wildercord.LOGGER.info("GROUND_FIELD_RAIN case={} actualDamage={} expectedHealing={} ownerHealth={} hits={}",
				healing, p.totalTaken, p.expectedHealing, p.owner.getHealth(), p.damage.size());
		});
	}

	private static void burstOwnerRoundTrip(ClientGameTestContext context, TestSingleplayerContext world) {
		context.waitTicks(30);
		world.getServer().runOnServer(server -> {
			Probe p = prepare(server, CrimsonArts.RED_RAIN);
			p.owner.setHealth(5);
			Vec3 centre = FEET.add(0, 0, p.ahead());
			Mob first = target(p, centre, HEALTH);
			Mob next = target(p, centre.add(1, 0, 0), HEALTH);
			p.afterDamage = q -> {
				check(q.damage.size() == 1 && q.damage.getFirst().target == first, "The nearest burst target is the first admitted damage callback");
				check(q.owner.teleportTo(server.getLevel(Level.NETHER), FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false), "Burst callback really leaves its source world");
				check(q.owner.teleportTo(q.level, FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false), "Burst callback returns the same owner body immediately");
			};
			p.afterRelease = q -> {
				check(q.field == null && ArtFields.count(q.owner, q.kind()) == 0, "A burst retired in its first hit never opens the pending rain field");
				check(next.getHealth() == HEALTH && !Reactions.has(next, Reactions.Mark.BLEEDING), "The next already-selected burst target receives no mutation");
				near(q.owner.getHealth(), 5, "Retirement inside burst damage suppresses its post-callback drink");
			};
			accept(p);
		});
		context.waitTicks(8 + ArtRules.RAIN_TICKS + 5);
		world.getServer().runOnServer(server -> {
			Probe p = current;
			verify(p, true);
			check(p.damage.size() == 1, "A same-callback world round trip never revives the retired burst or its rain");
		});
	}

	private static Probe prepare(MinecraftServer server, String id) {
		cleanup();
		ServerPlayer owner = server.getPlayerList().getPlayers().getFirst();
		check(owner.isAlive(), "Each scenario begins with a live connected original body");
		owner.setGameMode(GameType.SURVIVAL);
		owner.teleportTo(server.overworld(), FEET.x, FEET.y, FEET.z, Set.of(), 0, 0, false);
		owner.setNoGravity(true);
		owner.setDeltaMovement(Vec3.ZERO);
		owner.setShiftKeyDown(false);
		owner.setSprinting(false);
		owner.stopUsingItem();
		owner.removeAllEffects();
		owner.clearFire();
		owner.setHealth(owner.getMaxHealth());
		owner.setLastHurtMob(null);
		owner.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(ArtRules.art(id).method(), AuraRules.SOVEREIGN, 4500, 160, 0));
		owner.setAttached(SwordStrings.COOLDOWNS, SwordStrings.Cooldowns.NONE);
		owner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		owner.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		owner.setAttached(Momentum.MOMENTUM, Momentum.State.NONE);
		for (int y = 100; y <= 104; y++) owner.level().setBlock(new BlockPos(0, y, 1), Blocks.AIR.defaultBlockState(), 2);
		Probe p = new Probe(owner, id);
		current = p;
		return p;
	}

	private static void accept(Probe p) {
		// Share the real scheduler clock with the release, independent of runOnServer's position within a tick.
		Scheduler.later(2, () -> checked(p, () -> acceptOnTick(p)));
	}

	private static void acceptOnTick(Probe p) {
		var art = AuraApi.artOf(p.owner, p.id).orElseThrow();
		p.owner.setLastHurtMob(p.oldStruck);
		p.accepted = p.level.getGameTime();
		p.started = true;
		p.price = SwordStrings.price(p.owner, art);
		p.cooldown = p.accepted + SwordStrings.rest(p.owner, art);
		float before = Aura.aura(p.owner);
		check(SwordStrings.perform(p.owner, art, MARKS), "The registered ground form enters its real paid windup");
		near(Aura.aura(p.owner), before - p.price, "Acceptance deducts the effective price once");
		check(p.spends == 1 && p.completions == 0 && ArtFields.count(p.owner, p.kind()) == 0, "Acceptance pays but creates neither completion nor field");
		check(SwordStrings.readyAt(p.owner, p.id) == p.cooldown, "The original rest starts at acceptance");
		float paid = Aura.aura(p.owner);
		check(!SwordStrings.perform(p.owner, art, MARKS) && Aura.aura(p.owner) == paid, "A duplicate request cannot pay or schedule twice");
	}

	private static void verify(Probe p, boolean completed) {
		if (p.failure != null) throw new AssertionError(p.id + " native ground-field case failed", p.failure);
		check(p.completions == (completed ? 1 : 0) && p.spends == 1, "Every accepted form spends once and completes only when actually released");
		near(p.paid, p.price, "The entire original form pays its original effective price once");
		check(SwordStrings.readyAt(p.owner, p.id) == p.cooldown, "Neither cancellation nor delayed field pulses rest a second time");
		check(Effects.applying() == null && Effects.applyingCast() == null, "The native field leaves no source or cast scope behind");
	}

	/** Neutral cows avoid Crimson's separate on-hit leech, isolating the actual art's drink and shared mend cap. */
	private static Mob target(Probe p, Vec3 at, float health) {
		Mob target = EntityTypes.COW.create(p.level, EntitySpawnReason.COMMAND);
		check(target != null, "The neutral native damage target exists");
		target.setNoAi(true);
		target.setNoGravity(true);
		target.setPersistenceRequired();
		target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
		target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
		target.setHealth(health);
		move(target, at);
		p.level.addFreshEntity(target);
		p.targets.add(target); p.positions.put(target, at); p.health.put(target, health);
		return target;
	}

	private static void move(LivingEntity target, Vec3 at) {
		target.snapTo(at.x, at.y, at.z, 0, 0);
		target.setDeltaMovement(Vec3.ZERO);
	}

	/** Read-only observation of the actual field, without replacing its shape, callback or owner. */
	private static ArtFields.Field field(String kind) {
		try {
			var fields = ArtFields.class.getDeclaredField("FIELDS");
			fields.setAccessible(true);
			for (Object value : List.copyOf((List<?>) fields.get(null))) {
				if (value instanceof ArtFields.Field field && field.kind().equals(kind)) return field;
			}
			return null;
		} catch (ReflectiveOperationException failure) {
			throw new AssertionError("Pinned field observation is unavailable", failure);
		}
	}

	private static void cleanup() {
		Probe p = current;
		current = null;
		if (p == null) return;
		MethodArts.forget(p.owner.getUUID());
		p.targets.forEach(LivingEntity::discard);
	}

	private static void arena(ServerLevel level) {
		check(level != null, "The native test dimension exists");
		for (int x = -1; x <= 1; x++) for (int z = -1; z <= 0; z++) level.setChunkForced(x, z, true);
		for (int x = -8; x <= 24; x++) for (int z = -12; z <= 12; z++) {
			level.setBlock(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState(), 2);
			for (int y = 100; y <= 115; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
		}
	}

	private static void checked(Probe p, Runnable action) {
		if (p.failure != null) return;
		try { action.run(); } catch (RuntimeException | AssertionError failure) { p.failure = failure; }
	}

	private static void near(double actual, double expected, String reason) {
		check(Math.abs(actual - expected) < .002, reason + ": expected " + expected + ", got " + actual);
	}

	private static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); }
}
