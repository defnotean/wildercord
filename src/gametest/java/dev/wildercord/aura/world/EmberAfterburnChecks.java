package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import dev.wildercord.cast.Effects;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/** Runs inside the registered trial suite: real accepted Cinder Wake plus isolated native lifetime/cover probes. */
final class EmberAfterburnChecks {
	private static final class Challenger extends FakePlayer {
		Challenger(ServerLevel level, String name) { super(level, new GameProfile(UUID.randomUUID(), name)); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}

	private SwordMaster master;
	private Challenger ally, bystander;
	private Vec3 origin;
	private long begun;
	private float health;

	void run(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule natural_health_regeneration false");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				origin = new Vec3(player.getBlockX() + .5, 181, player.getBlockZ() + .5);
				BlockPos floor = BlockPos.containing(origin).below();
				for (int x = -28; x <= 28; x++) for (int z = -28; z <= 28; z++) level.setBlockAndUpdate(floor.offset(x, 0, z), Blocks.STONE.defaultBlockState());
				player.setGameMode(GameType.SURVIVAL);
				player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
				player.setHealth(200);
				place(player, 0, 3);
				ally = add(level, "WakeAlly", 0, 6);
				bystander = add(level, "WakeBystander", 0, 6.8);
				master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
				check(master != null, "Ember fixture is constructible");
				master.setNoAi(true);
				master.setNoGravity(true);
				master.snapTo(origin.x, origin.y, origin.z, 0, 0);
				level.addFreshEntity(master);
				master.mobInteract(player, InteractionHand.MAIN_HAND); master.mobInteract(player, InteractionHand.MAIN_HAND);
				master.mobInteract(ally, InteractionHand.MAIN_HAND); master.mobInteract(ally, InteractionHand.MAIN_HAND);
				check(SwordMaster.ready(player) == 1, "The two challengers consented; the bystander did not");
				master.customServerAiStep(level);
				master.setTarget(player);
			});
			context.waitTicks(21);
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				begin(player);
				check(player.getHealth() == 200 && !master.afterburnPending(), "The first warning causes no immediate damage or hidden wake");
				Effects.withSource(player, () -> check(master.interruptWindup(), "An early spell interrupts Cinder Wake"));
				check(master.attackAnimation() == 0 && !master.afterburnPending(), "Interruption clears both the pose and any delayed harm");
			});
			context.waitTicks(21);
			world.getServer().runOnServer(server -> begin(server.getPlayerList().getPlayers().getFirst()));
			at(world, EmberWakeRules.TELL - 1, player -> {
				place(player, 0, 5); // Backstep leaves the broad first cut, but remains inside the separately warned wake.
				master.customServerAiStep(player.level());
				check(player.getHealth() == 200 && !master.afterburnPending(), "No harm precedes the full first tell");
			});
			at(world, EmberWakeRules.TELL, player -> {
				master.customServerAiStep(player.level());
				check(player.getHealth() == 200 && ally.getHealth() == 200, "The backstep escapes the broad cut");
				check(master.afterburnPending(), "Release starts its own visible, separately delayed afterburn");
				check(master.auraRemaining() == MastersRules.AURA_MAX - 2 * EmberWakeRules.COST, "Cancelled and completed attempts each spend finite Aura");
				health = player.getHealth();
			});
			at(world, EmberWakeRules.TELL + EmberWakeRules.AFTERBURN_TELL - 1, player -> {
				master.customServerAiStep(player.level());
				check(player.getHealth() == health && ally.getHealth() == 200, "The entire second warning is harmless");
				place(ally, 1.2, 5); // A distinct sidestep, after the initial cut, answers the second shape.
			});
			at(world, EmberWakeRules.TELL + EmberWakeRules.AFTERBURN_TELL, player -> {
				master.customServerAiStep(player.level());
				check(player.getHealth() < health, "The real afterburn hurts a challenger who stays in its marked lane");
				check(ally.getHealth() == 200, "A late sideways step independently dodges the second strike");
				check(bystander.getHealth() == 200, "The same lane never enrolls or harms a bystander");
				check(player.getLastDamageSource() != null && player.getLastDamageSource().getEntity() == master, "Delayed damage retains its actual trial owner");
				check(!master.afterburnPending() && !master.guarding(), "One ignition consumes the wake and leaves the master open");
				float after = player.getHealth();
				master.customServerAiStep(player.level());
				check(player.getHealth() == after, "Repeated same-tick AI cannot duplicate the hit");
			});
			at(world, EmberWakeRules.TELL + EmberWakeRules.RECOVERY - 1, player -> {
				master.customServerAiStep(player.level());
				check(!master.state(AuraFighter.WINDUP) && !master.guarding(), "The promised counter window survives after the second beat");
				probeCoverAndAudience(player);
				probeLifecycle(player);
			});
			probeNoAiTransition(context, world);
			world.getServer().runOnServer(server -> { master.discard(); ally.discard(); bystander.discard(); });
		}
	}

	private void probeNoAiTransition(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			place(player, 0, 3);
			master = AuraWorld.SWORD_MASTER.create(player.level(), EntitySpawnReason.COMMAND);
			check(master != null, "The NoAI transition fixture exists");
			master.setNoAi(true);
			master.snapTo(origin.x, origin.y, origin.z, 0, 0);
			player.level().addFreshEntity(master);
			master.mobInteract(player, InteractionHand.MAIN_HAND); master.mobInteract(player, InteractionHand.MAIN_HAND);
			check(SwordMaster.ready(player) == 1, "The resumed solo trial is explicitly accepted");
			master.customServerAiStep(player.level());
			master.setTarget(player);
		});
		context.waitTicks(21);
		world.getServer().runOnServer(server -> begin(server.getPlayerList().getPlayers().getFirst()));
		at(world, EmberWakeRules.TELL, player -> {
			place(player, 0, 5);
			master.customServerAiStep(player.level());
			check(master.afterburnPending(), "The NoAI probe pauses a real accepted pending afterburn");
			health = player.getHealth();
			master.setNoAi(false);
			master.setNoAi(true);
			check(!master.afterburnPending() && master.attackAnimation() == 0, "Entering NoAI cancels both the wake and its body timeline");
		});
		at(world, EmberWakeRules.TELL + EmberWakeRules.AFTERBURN_TELL, player -> {
			master.setNoAi(false);
			master.customServerAiStep(player.level());
			check(player.getHealth() == health && !master.afterburnPending(), "Resuming on the exact ignition tick cannot revive an expired warning");
		});
	}

	private void begin(ServerPlayer player) {
		master.customServerAiStep(player.level());
		begun = player.level().getGameTime();
		check(master.attackAnimation() == MastersRules.Move.CINDER_WAKE.ordinal() + 1 && master.attackElapsed(0) == 0,
			"A grounded Ember opening accepts the new server-synchronized Cinder Wake");
	}

	private void probeCoverAndAudience(ServerPlayer player) {
		ServerLevel level = player.level();
		place(player, 0, 5); place(ally, 0, 6);
		long now = level.getGameTime();
		float before = player.getHealth(), allyBefore = ally.getHealth();
		EmberAfterburn covered = wake(now, 2);
		check(covered.tick(now), "The cover probe begins with a real warning");
		BlockPos wall = BlockPos.containing(origin.add(0, 0, 3));
		for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) level.setBlockAndUpdate(wall.offset(x, y, 0), Blocks.STONE.defaultBlockState());
		check(!covered.tick(now + EmberWakeRules.AFTERBURN_TELL), "The covered wake is still consumed exactly once");
		check(player.getHealth() == before && ally.getHealth() == allyBefore, "Solid cover added after warning blocks delayed damage");
		EmberAfterburn shadowed = wake(now, 2); shadowed.tick(now);
		for (int x = -1; x <= 1; x++) for (int y = 0; y <= 2; y++) level.setBlockAndUpdate(wall.offset(x, y, 0), Blocks.AIR.defaultBlockState());
		check(!shadowed.tick(now + EmberWakeRules.AFTERBURN_TELL) && player.getHealth() == before,
			"Removing original cover cannot extend ignition past the originally warned lane");
		EmberAfterburn unwarned = wake(now, 2);
		check(!unwarned.tick(now + EmberWakeRules.AFTERBURN_TELL) && player.getHealth() == before, "A hazard that never issued its warning cannot ignite");
		EmberAfterburn late = wake(now, 2); late.tick(now);
		check(!late.tick(now + EmberWakeRules.AFTERBURN_TELL + 1) && player.getHealth() == before, "A missed ignition expires without a late invisible hit");
		// The native collision/source path also obeys the maximum party layout; it does not multiply a player's damage.
		EmberAfterburn group = wake(now, 8); group.tick(now);
		master.snapTo(origin.x + 2, origin.y, origin.z, 90, 0);
		group.tick(now + EmberWakeRules.AFTERBURN_TELL);
		check(player.getHealth() < before && ally.getHealth() < allyBefore, "Captured geometry survives owner movement and covers both accepted players");
		check(bystander.getHealth() == 200, "Eight-player coverage still excludes an unaccepted body");
		float once = player.getHealth();
		group.tick(now + EmberWakeRules.AFTERBURN_TELL);
		check(player.getHealth() == once, "Overlapping/re-entered evaluations spend one per-player budget only");
		master.snapTo(origin.x, origin.y, origin.z, 0, 0);
	}

	private void probeLifecycle(ServerPlayer player) {
		ServerLevel level = player.level();
		long now = level.getGameTime();
		place(player, 0, 5); place(ally, 0, 6);
		float before = player.getHealth();
		EmberAfterburn left = wake(now, 2); left.tick(now);
		place(player, 40, 0); place(ally, 40, 1);
		check(!left.tick(now + EmberWakeRules.AFTERBURN_TELL), "The last challengers leaving cancels a Master-owned wake");
		place(player, 0, 5); place(ally, 0, 6);
		check(!left.tick(now + EmberWakeRules.AFTERBURN_TELL) && player.getHealth() == before, "Returning cannot resurrect an already cancelled wake");
		EmberAfterburn outside = wake(now, 2); outside.tick(now);
		master.snapTo(origin.x + 40, origin.y, origin.z, 0, 0);
		check(!outside.tick(now + EmberWakeRules.AFTERBURN_TELL) && player.getHealth() == before, "An owner outside its arena cannot maintain a delayed hazard");
		master.snapTo(origin.x, origin.y, origin.z, 0, 0);
		EmberAfterburn removed = wake(now, 2); removed.tick(now);
		master.setHealth(0);
		master.die(level.damageSources().generic());
		check(!removed.tick(now + EmberWakeRules.AFTERBURN_TELL) && player.getHealth() == before && !master.afterburnPending(),
			"Death cancels delayed harm before the body's removal animation finishes");
		master.discard();
		check(!removed.tick(now + EmberWakeRules.AFTERBURN_TELL) && player.getHealth() == before && !master.afterburnPending(), "Removing the trial prevents every detached probe and clears its owned wake");
	}

	private EmberAfterburn wake(long now, int count) { return new EmberAfterburn(master, origin, new Vec3(0, 0, 1), count, now); }

	private Challenger add(ServerLevel level, String name, double x, double z) {
		Challenger player = new Challenger(level, name);
		player.setGameMode(GameType.SURVIVAL);
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
		player.setHealth(200);
		player.setNoGravity(true);
		player.snapTo(origin.x + x, origin.y, origin.z + z, 180, 0);
		level.addNewPlayer(player);
		return player;
	}

	private void place(ServerPlayer player, double x, double z) {
		player.teleportTo(player.level(), origin.x + x, origin.y, origin.z + z, Set.of(), 180, 0, false);
		player.setDeltaMovement(Vec3.ZERO);
	}

	private void at(TestSingleplayerContext world, int elapsed, Consumer<ServerPlayer> action) {
		long expected = begun + elapsed;
		world.getServer().waitFor(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			long now = player.level().getGameTime();
			if (now < expected) {
				master.customServerAiStep(player.level()); // Maintain every visible warning frame between exact observations.
				return false;
			}
			check(now == expected, "Observe the exact accepted Ember frame: expected=" + expected + ", actual=" + now);
			action.accept(player);
			return true;
		}, elapsed + 20);
	}

	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
