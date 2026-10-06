package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.cast.CastEngine;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import dev.wildercord.cast.Effects;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Fresh ordinary AI trials measure the one ring, its real counters, and every retained ownership boundary. */
public final class EmberKilnTest implements FabricClientGameTest {
	private static final float HEALTH = 200;
	private static final class Challenger extends FakePlayer {
		Challenger(ServerLevel level, String name) { super(level, new GameProfile(UUID.randomUUID(), name)); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}
	private enum Answer { HOLD, INNER, OUTER, GUARD, PARRY, FORESIGHT, ABSORPTION,
		INSERT_COVER, REMOVE_COVER, BLOCK_ESCAPE, BODY_BLOCK, GROUND_GAP, INTERRUPT, OUTSIDE_INTERRUPT, NO_AI, DISPLACE,
		LEAVE, DIMENSION, SPECTATOR, TARGET_DEATH, OWNER_DEATH, OWNER_REMOVE, OWNED_INTERRUPT, REENTRANT, CALLBACK_ENTRY, CALLBACK_EXIT, LATE_CALLBACK }
	private static EmberKilnTest active, grounding;
	private record GroundFrame(long age, boolean onGround, boolean pending, Vec3 position, Vec3 velocity) {}
	private final List<GroundFrame> groundFrames = new ArrayList<>();
	private static boolean registered;
	private ServerLevel level;
	private Vec3 origin;
	private SwordMaster master;
	private Challenger target, bystander;
	private final List<Challenger> party = new ArrayList<>();
	private long began;
	private Answer answer;
	private int callbacks;
	private EmberKiln accepted;

	@Override public void runTest(ClientGameTestContext context) {
		registerCallback();
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false")) world.getServer().runCommand(command);
			world.getServer().runOnServer(server -> {
				ServerPlayer observer = server.getPlayerList().getPlayers().getFirst();
				level = observer.level(); origin = new Vec3(observer.getBlockX() + .5, 181, observer.getBlockZ() + .5);
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.world.EmberKilnTest\",\"seed\":\"{}\"}", level.getSeed());
				for (int x = -28; x <= 48; x++) for (int z = -28; z <= 28; z++)
					level.setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.STONE.defaultBlockState());
				observer.setGameMode(GameType.SPECTATOR); place(observer, 0, 2, 0);
			});
			for (Answer choice : Answer.values()) scenario(world, choice, 0);
			for (int phase = 1; phase <= 2; phase++) scenario(world, Answer.HOLD, phase);
			groundingSurvivesNativeTravel(world);
			lethalParty(world);
			blockedAdmission(world);
		} finally { active = null; grounding = null; }
	}

	private void scenario(TestSingleplayerContext world, Answer choice, int phase) {
		answer = choice; callbacks = 0;
		int count = choice == Answer.HOLD || choice == Answer.REENTRANT ? 8 : choice == Answer.LEAVE || choice == Answer.DIMENSION || choice == Answer.TARGET_DEATH || choice == Answer.CALLBACK_ENTRY || choice == Answer.CALLBACK_EXIT ? 2 : 1;
		beginNaturally(world, count, phase, choice == Answer.REMOVE_COVER);
		at(world, 6, () -> {
			check(target.getHealth() == HEALTH && master.position().distanceToSqr(origin) < .003, "The coil is stationary and harmless");
			check(EmberKiln.prepare(master, target, 0, 1, 100, level.getGameTime(), 0) == null, "A running form cannot admit a second instance");
			switch (choice) {
				case INNER -> place(target, 2, 0, 0);
				case CALLBACK_ENTRY -> place(party.get(1), -2, 0, 0);
				case OUTER -> place(target, 6.1, 0, 0);
				case GUARD -> guard();
				case ABSORPTION -> target.setAbsorptionAmount(40);
				case INSERT_COVER -> cover(2, 0, true);
				case REMOVE_COVER -> { cover(0, 2, false); place(target, 0, 0, 4); }
				case BLOCK_ESCAPE -> cover(5, 0, true);
				case BODY_BLOCK -> place(bystander, 5, 0, 0);
				case GROUND_GAP -> level.setBlockAndUpdate(BlockPos.containing(origin).offset(5, -1, 0), Blocks.AIR.defaultBlockState());
				case INTERRUPT -> Effects.withSource(target, () -> check(master.interruptWindup(), "Enrolled early magic interrupts the ring"));
				case OUTSIDE_INTERRUPT -> {
					Wolf outside = ownedCompanion(bystander);
					Effects.withSource(outside, () -> check(!master.interruptWindup(), "An outsider-owned source cannot cancel the enrolled encounter")); outside.discard();
				}
				case OWNED_INTERRUPT -> {
					Wolf owned = ownedCompanion(target);
					Effects.withSource(owned, () -> check(master.interruptWindup(), "An enrolled companion owner can interrupt through the existing source boundary")); owned.discard();
				}
				case NO_AI -> master.setNoAi(true);
				case DISPLACE -> master.setDeltaMovement(.3, 0, 0);
				case LEAVE -> place(target, 40, 0, 0);
				case DIMENSION -> {
					ServerLevel other = level.getServer().getLevel(Level.NETHER);
					check(other != null, "The native second dimension exists"); target.setNoGravity(true);
					target.teleportTo(other, origin.x, 200, origin.z, Set.of(), 0, 0, false);
				}
				case SPECTATOR -> target.setGameMode(GameType.SPECTATOR);
				case TARGET_DEATH -> { target.setHealth(0); target.die(level.damageSources().generic()); }
				case OWNER_DEATH -> { master.setHealth(0); master.die(level.damageSources().generic()); }
				case OWNER_REMOVE -> master.discard();
				case LATE_CALLBACK -> check(!accepted.tick(began + EmberKilnRules.TELL + 1), "A detached callback cannot schedule a late pulse");
				default -> {}
			}
		});
		at(world, EmberKilnRules.TELL - 5, () -> {
			if (choice == Answer.NO_AI) {
				master.setNoAi(false);
				check(master.attackAnimation() == 0 && !master.kilnPending(), "Resuming AI cannot revive a cancelled warning");
			}
		});
		at(world, EmberKilnRules.TELL - 1, () -> {
			if (choice != Answer.TARGET_DEATH) check(target.getHealth() == HEALTH, "The entire learned tell is harmless: " + choice);
			if (choice == Answer.PARRY) guard();
			if (choice == Answer.FORESIGHT) CastEngine.cast(target, SpellCompiler.compile(List.of(Runes.SELF, Runes.FORESIGHT)).root());
			if (choice == Answer.REENTRANT || choice == Answer.CALLBACK_ENTRY || choice == Answer.CALLBACK_EXIT) active = this;
		});
		at(world, EmberKilnRules.TELL, () -> {
			active = null;
			boolean hits = choice == Answer.HOLD || choice == Answer.OUTSIDE_INTERRUPT;
			if (hits) {
				check(close(HEALTH - target.getHealth(), EmberKilnRules.DAMAGE), "Staying in the ring takes its ordinary 26-damage budget: " + choice);
				check(target.getLastDamageSource() != null && target.getLastDamageSource().getEntity() == master, "Every hit retains its exact trial owner");
				if (choice == Answer.HOLD) for (Challenger player : party) check(close(HEALTH - player.getHealth(), EmberKilnRules.DAMAGE), "All eight independently receive at most one unchanged hit");
			} else if (choice == Answer.GUARD) check(target.getHealth() > HEALTH - EmberKilnRules.DAMAGE, "Facing held guard reduces real projected ring damage");
			else if (choice == Answer.ABSORPTION) check(target.getHealth() == HEALTH && target.getAbsorptionAmount() < 40, "Absorption resolves through the ordinary damage pathway");
			else if (choice == Answer.REENTRANT || choice == Answer.CALLBACK_ENTRY || choice == Answer.CALLBACK_EXIT) {
				check(callbacks == 1 && party.stream().filter(p -> p.getHealth() < HEALTH).count() == 1, "Native callback cancellation, escape or late entry cannot create a later roster hit: " + choice);
			} else if (choice != Answer.TARGET_DEATH) check(target.getHealth() == HEALTH, "This counter leaves the challenger unharmed: " + choice);
			check(bystander.getHealth() == HEALTH, "An uninvited body never becomes an area target");
			check(!master.kilnPending() && !master.guarding(), "The consumed/cancelled ring leaves a real punish window");
			float health = target.getHealth();
			check(!accepted.tick(level.getGameTime()), "A consumed or cancelled instance cannot be called again");
			if (master.isAlive() && !master.isRemoved()) master.customServerAiStep(level);
			check(target.getHealth() == health, "Same-tick AI and detached callback reentry cannot duplicate a hit");
		});
		at(world, EmberKilnRules.TELL + EmberKilnRules.RECOVERY - 1, () -> {
			if (master.isAlive() && !master.isRemoved()) {
				check(!master.state(AuraFighter.WINDUP) && !master.guarding() && !master.kilnPending(), "No cast response, guard or follow-up consumes the full final recovery tick");
				check(close(master.auraRemaining(), 48), "The opening Wake and ring each pay once, including cancelled forms");
				check(!master.canBeginKiln(level.getGameTime()), "The final punish frame cannot be reused as a transition");
			}
			if (choice != Answer.INTERRUPT) cleanup();
		});
		if (choice == Answer.INTERRUPT) at(world, EmberKilnRules.TELL + EmberKilnRules.RECOVERY, () -> {
			check(master.attackAnimation() == 3 && master.attackElapsed(0) == 0 && !master.kilnPending() && close(master.auraRemaining(), 32),
				"After full recovery the still-eligible slot uses ordinary Crescent because the paid 180-tick cooldown survives interruption");
			check(!accepted.tick(level.getGameTime()) && master.attackAnimation() == 3, "The old instance cannot cancel or mutate a later accepted attack");
			cleanup();
		});
	}

	private Wolf ownedCompanion(ServerPlayer owner) {
		Wolf wolf = EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND);
		check(wolf != null, "A native owned companion exists"); wolf.tame(owner); wolf.setNoAi(true);
		wolf.snapTo(origin.x + 12, origin.y, origin.z, 0, 0); level.addFreshEntity(wolf);
		float health = wolf.getHealth();
		check(!master.canHarmParticipant(wolf) && master.projected(wolf, 26) == 0 && wolf.getHealth() == health,
			"A lawful incoming companion source never becomes an outgoing enrolled ring target");
		return wolf;
	}

	private static void registerCallback() {
		if (registered) return;
		registered = true;
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			EmberKilnTest fixture = grounding;
			if (fixture == null || fixture.master == null || server != fixture.level.getServer()) return;
			long age = fixture.level.getGameTime() - fixture.began;
			if (age > 0 && age < EmberKilnRules.TELL && (fixture.groundFrames.isEmpty() || fixture.groundFrames.getLast().age() != age))
				fixture.groundFrames.add(new GroundFrame(age, fixture.master.onGround(), fixture.master.kilnPending(),
					fixture.master.position(), fixture.master.getDeltaMovement()));
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			EmberKilnTest fixture = active;
			if (fixture == null || source.getEntity() != fixture.master || !fixture.party.contains(entity)) return;
			fixture.callbacks++;
			check(!fixture.accepted.tick(fixture.level.getGameTime()), "The native damage callback sees a consumed hit ledger");
			if (fixture.answer == Answer.CALLBACK_ENTRY) fixture.place(fixture.party.get(1), -4, 0, 0);
			else if (fixture.answer == Answer.CALLBACK_EXIT) {
				Challenger other = fixture.party.stream().filter(player -> player != entity).findFirst().orElseThrow();
				fixture.place(other, 0, 1.5, 0);
			} else fixture.master.setNoAi(true);
		});
	}

	private void beginNaturally(TestSingleplayerContext world, int count, int phase, boolean covered) {
		completeOpening(world, count, () -> {
			if (phase > 0) master.setHealth(master.getMaxHealth() * (phase == 1 ? .6F : .3F));
			if (covered) cover(0, 2, true);
		});
		world.getServer().waitFor(server -> {
			if (!master.kilnPending()) return false;
			check(master.attackAnimation() == 9 && master.attackElapsed(0) <= 1 && master.attackTellTicks() == 40, "Ordinary server AI admits the exact appended ring protocol");
			began = level.getGameTime() - (long) master.attackElapsed(0); accepted = owned(master);
			check(close(master.auraRemaining(), 48) && master.challengerCount() == count, "The fixed roster and one finite 28-Aura payment are preserved");
			check(!master.isNoAi() && master.onGround(), "The real native body owns admission");
			return true;
		}, 4);
	}

	private void completeOpening(TestSingleplayerContext world, int count, Runnable beforeReady) {
		world.getServer().runOnServer(server -> setup(count));
		world.getServer().waitFor(server -> {
			if (!master.state(AuraFighter.WINDUP)) return false;
			check(master.attackAnimation() == 5, "The original Ember sequence-zero Wake keeps its priority");
			began = level.getGameTime() - (long) master.attackElapsed(0); return true;
		}, 45);
		at(world, 10, () -> party.forEach(player -> place(player, 8, 0, party.indexOf(player) - 4)));
		at(world, EmberWakeRules.TELL + EmberWakeRules.RECOVERY - 1, () -> {
			for (int i = 0; i < party.size(); i++) {
				double angle = i * Math.PI * 2 / party.size();
				Challenger player = party.get(i); place(player, 4 * Math.cos(angle), 0, 4 * Math.sin(angle));
				player.setHealth(HEALTH);
			}
			master.setTarget(target);
			check(!master.canBeginKiln(level.getGameTime()) && close(master.auraRemaining(), 76), "The original Wake's entire recovery completes before ring admission");
			beforeReady.run();
		});
	}

	/** Observe native post-travel results; never force onGround, disable gravity, or manually advance AI. */
	private void groundingSurvivesNativeTravel(TestSingleplayerContext world) {
		beginNaturally(world, 1, 0, false);
		world.getServer().runOnServer(server -> {
			check(!master.isNoGravity() && !master.isNoAi(), "The grounding regression uses ordinary gravity and server AI");
			groundFrames.clear(); grounding = this;
		});
		at(world, EmberKilnRules.TELL, () -> {
			grounding = null;
			check(groundFrames.size() >= EmberKilnRules.TELL - 2, "Observe the complete natural windup after travel: " + groundFrames);
			for (GroundFrame frame : groundFrames) {
				check(frame.onGround() && frame.pending() && frame.position().distanceToSqr(origin) < .003
					&& frame.velocity().horizontalDistanceSqr() < .0001 && frame.velocity().y < 0 && frame.velocity().y >= -.1,
					"The stationary ring preserves native downward contact between successive AI ticks: " + frame);
			}
			check(!master.kilnPending() && master.onGround() && close(target.getHealth(), HEALTH - EmberKilnRules.DAMAGE),
				"Natural grounding survives all 40 warning ticks and the one paid release");
		});
		at(world, EmberKilnRules.TELL + EmberKilnRules.RECOVERY - 1, () -> {
			check(master.onGround() && !master.state(AuraFighter.WINDUP) && !master.guarding() && close(master.auraRemaining(), 48),
				"Native ground contact and the full exposed recovery remain intact");
			cleanup();
		});
	}

	private void lethalParty(TestSingleplayerContext world) {
		beginNaturally(world, MastersRules.MAX_PARTICIPANTS, 0, false);
		at(world, EmberKilnRules.TELL - 1, () -> {
			party.forEach(player -> player.setHealth(20));
			check(party.stream().allMatch(master::canHarmParticipant), "All eight ordinary-health players are lawful before the pulse");
		});
		at(world, EmberKilnRules.TELL, () -> {
			check(party.stream().noneMatch(Challenger::isAlive), "One lethal hit cannot shield later UUIDs from the same simultaneous ring");
			check(party.stream().allMatch(player -> player.getLastDamageSource() != null && player.getLastDamageSource().getEntity() == master),
				"Every defeated participant retains the pulse's actual owner");
			check(bystander.getHealth() == HEALTH && !master.kilnPending(), "The consumed lethal pulse still excludes bystanders");
			check(!accepted.tick(level.getGameTime()), "Lethal completion cannot repeat");
			cleanup();
		});
	}

	private void blockedAdmission(TestSingleplayerContext world) {
		for (int kind = 0; kind < 4; kind++) {
			int obstruction = kind;
			completeOpening(world, 1, () -> {
				if (obstruction == 0) cover(5, 0, true);
				if (obstruction == 1) place(bystander, 5, 0, 0);
				if (obstruction == 2) level.setBlockAndUpdate(BlockPos.containing(origin).offset(5, -1, 0), Blocks.AIR.defaultBlockState());
				if (obstruction == 3) place(target, 4, 1, 0);
			});
			at(world, EmberWakeRules.TELL + EmberWakeRules.RECOVERY, () -> {
				check(!master.kilnPending() && close(master.auraRemaining(), 60), "Blocked escape, body congestion, broken ground or elevation decline before payment: " + obstruction);
				cleanup();
			});
		}
	}

	private void setup(int count) {
		target = add("KilnTarget", 0, 4.5); party.add(target);
		for (int i = 1; i < count; i++) party.add(add("KilnAlly" + i, i - 4, 6));
		bystander = add("KilnBystander", -8, -8);
		master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
		check(master != null, "The registered Master exists"); master.snapTo(origin.x, origin.y, origin.z, 0, 0); level.addFreshEntity(master);
		for (Challenger player : party) { master.mobInteract(player, InteractionHand.MAIN_HAND); master.mobInteract(player, InteractionHand.MAIN_HAND); }
		check(SwordMaster.ready(target) == 1, "Every fake participant explicitly joins this real trial"); master.setTarget(target);
	}
	private Challenger add(String name, double x, double z) {
		Challenger player = new Challenger(level, name); player.setGameMode(GameType.SURVIVAL);
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH); player.setHealth(HEALTH);
		player.snapTo(origin.x + x, origin.y, origin.z + z, 90, 0); level.addNewPlayer(player); return player;
	}
	private void guard() {
		target.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		target.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.FLOW, 0, 40, 0));
		target.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE); target.setShiftKeyDown(true);
		target.setYRot(90); target.setYHeadRot(90); target.setYBodyRot(90); target.setXRot(0);
		check(AuraGuard.raise(target) && AuraGuard.facing(target, master.position()), "The real guard is paid and faces the fixed origin");
	}
	private void place(ServerPlayer player, double x, double y, double z) {
		player.teleportTo(level, origin.x + x, origin.y + y, origin.z + z, Set.of(), 90, 0, false); player.setDeltaMovement(Vec3.ZERO);
	}
	private void cover(int x, int z, boolean present) {
		for (int y = 0; y <= 2; y++) level.setBlockAndUpdate(BlockPos.containing(origin).offset(x, y, z), (present ? Blocks.STONE : Blocks.AIR).defaultBlockState());
	}
	private void cleanup() {
		active = null; if (grounding == this) grounding = null; if (master != null) master.discard();
		for (Challenger player : party) player.discard(); party.clear(); if (bystander != null) bystander.discard();
		cover(2, 0, false); cover(0, 2, false); cover(5, 0, false);
		level.setBlockAndUpdate(BlockPos.containing(origin).offset(5, -1, 0), Blocks.STONE.defaultBlockState());
	}
	private void at(TestSingleplayerContext world, int age, Runnable action) {
		long expected = began + age;
		world.getServer().waitFor(server -> {
			long now = level.getGameTime(); if (now < expected) return false;
			check(now == expected, "Observe exact native Kiln frame " + age + ": expected=" + expected + ", actual=" + now); action.run(); return true;
		}, age + 20);
	}
	private static EmberKiln owned(SwordMaster master) {
		try { var field = SwordMaster.class.getDeclaredField("kiln"); field.setAccessible(true); return (EmberKiln) field.get(master); }
		catch (ReflectiveOperationException error) { throw new AssertionError(error); }
	}
	private static boolean close(double a, double b) { return Math.abs(a - b) < .01; }
	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
