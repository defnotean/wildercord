package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * masters-a pack: the Rime, Thunder, Verdant and Hollow Masters in a real native world. Each Master is spawned, its trial
 * accepted by fake challengers, its ordinary AI observed opening with one of its own named techniques, and its signature
 * resolved twice: once against the correct answer (no damage) and once against the wrong one (damage).
 */
public final class ElementalMastersNativeTest implements FabricClientGameTest {
	private static final float HEALTH = 200;
	private static final class Challenger extends FakePlayer {
		Challenger(ServerLevel level, String name) { super(level, new GameProfile(UUID.randomUUID(), name)); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}
	private ServerLevel level;
	private Vec3 origin;
	private SwordMaster master;
	private Challenger target, ally;
	private final List<Challenger> party = new ArrayList<>();
	private long began;

	@Override public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false"))
				world.getServer().runCommand(command);
			world.getServer().runOnServer(server -> {
				ServerPlayer observer = server.getPlayerList().getPlayers().getFirst();
				level = observer.level(); origin = new Vec3(observer.getBlockX() + .5, 181, observer.getBlockZ() + .5);
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"dev.wildercord.aura.world.ElementalMastersNativeTest\",\"seed\":\"{}\"}", level.getSeed());
				for (int x = -24; x <= 24; x++) for (int z = -24; z <= 24; z++) {
					BlockPos floor = BlockPos.containing(origin).offset(x, -1, z);
					level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
					for (int y = 1; y <= 4; y++) level.setBlockAndUpdate(floor.above(y), Blocks.AIR.defaultBlockState());
				}
				observer.setGameMode(GameType.SPECTATOR); place(observer, 0, 3, -12);
			});
			for (int school : ElementalMasters.SCHOOL_IDS) {
				ordinaryTechniques(world, school);
				for (boolean correct : new boolean[] {true, false}) signature(world, school, correct);
			}
		}
	}

	/** The natural trial: consent, then the Master's own AI opens with one of its school's named techniques. */
	private void ordinaryTechniques(TestSingleplayerContext world, int school) {
		world.getServer().runOnServer(server -> setup(school, 1));
		int[] seen = {0};
		world.getServer().waitFor(server -> {
			place(target, 3, 0, 0);
			if (!master.state(AuraFighter.WINDUP) || master.attackAnimation() != MastersRules.Move.TECHNIQUE.ordinal() + 1) return false;
			var technique = MasterTechniques.byId(master.technique());
			check(technique != null && technique.school() == school,
				"The " + ElementalMasters.name(school) + " Master draws its own named techniques: " + technique);
			int tell = technique.impact(0);
			check(tell >= ElementalMasters.MIN_TELL && tell <= ElementalMasters.MAX_TELL, "The opening strike is warned for 12-14 ticks: " + tell);
			check(technique.recovery() >= ElementalMasters.MIN_RECOVERY && technique.recovery() <= ElementalMasters.MAX_RECOVERY,
				"The chain ends in a 14-16 tick recovery: " + technique.recovery());
			check(master.auraRemaining() < MastersRules.AURA_MAX, "The technique is paid from finite Aura: " + master.auraRemaining());
			seen[0] = technique.id();
			return true;
		}, 600);
		world.getServer().runOnServer(server -> {
			dev.wildercord.Wildercord.LOGGER.info("ELEMENTAL_MASTER_TECHNIQUE school={} technique={}", ElementalMasters.name(school),
				MasterTechniques.byId(seen[0]).key());
			cleanup();
		});
	}

	private void signature(TestSingleplayerContext world, int school, boolean correct) {
		MastersRules.Move move = ElementalMasters.signatureMove(school);
		boolean thunder = school == ElementalMasters.THUNDER;
		world.getServer().runOnServer(server -> setup(school, thunder ? 2 : 1));
		// Admit through every live check once the trial has begun; the seam only stages a quiet second phrase slot.
		world.getServer().waitFor(server -> {
			master.snapTo(origin.x, origin.y, origin.z, 0, 0);
			master.setDeltaMovement(0, master.getDeltaMovement().y, 0);
			place(target, school == ElementalMasters.THUNDER ? 5 : 4.5, 0, 0);
			if (ally != null) place(ally, 6.8, 0, 0);
			party.forEach(player -> player.setHealth(HEALTH));
			if (!master.beginSignatureForTest(level, target, level.getGameTime())) return false;
			began = level.getGameTime();
			check(master.signaturePending() && master.attackAnimation() == move.ordinal() + 1,
				"The " + move + " signature is admitted with its own wire id: " + master.attackAnimation());
			check(master.auraRemaining() <= MastersRules.AURA_MAX - ElementalMasters.cost(move) + .01, "The signature is paid once from finite Aura");
			return true;
		}, 200);
		int[] beats = ElementalMasters.beats(move);
		at(world, 2, () -> check(party.stream().allMatch(player -> player.getHealth() == HEALTH), "The warning itself is harmless"));
		switch (school) {
			case ElementalMasters.RIME -> rime(world, correct);
			case ElementalMasters.THUNDER -> thunderChain(world, correct);
			case ElementalMasters.VERDANT -> verdant(world, correct);
			default -> hollow(world, correct);
		}
		at(world, beats[beats.length - 1] + 1, () -> {
			if (correct) check(party.stream().allMatch(player -> player.getHealth() == HEALTH),
				"The correct answer to " + move + " takes no damage: " + healths());
			else check(party.stream().allMatch(player -> player.getHealth() < HEALTH),
				"The wrong answer to " + move + " is punished: " + healths());
			check(!master.signaturePending() && master.attackAnimation() == move.ordinal() + 1,
				"The signature released and its clip runs through the open recovery");
			dev.wildercord.Wildercord.LOGGER.info("ELEMENTAL_MASTER_SIGNATURE move={} correct={} health={}", move, correct, healths());
		});
		at(world, ElementalMasters.end(move), () -> {
			check(master.attackAnimation() != move.ordinal() + 1, "The signature's clip clears exactly at its end");
			cleanup();
		});
	}

	/** Rime: each plus is laid under the challenger; stepping two cells away before it freezes is the answer. */
	private void rime(TestSingleplayerContext world, boolean correct) {
		double[] z = {3.6, 0, 3.6};
		for (int pulse = 0; pulse < RimeLatticeRules.PULSES; pulse++) {
			int p = pulse;
			at(world, RimeLatticeRules.markAt(pulse) + 2, () -> { if (correct) place(target, 4.5, 0, z[p]); });
			at(world, RimeLatticeRules.freezeAt(pulse), () -> {
				float lost = HEALTH - target.getHealth();
				if (correct) check(lost == 0, "Stepping off the frost cross avoids freeze " + p);
				else check(lost > 0 && lost <= RimeLatticeRules.CAP * 1.5 + .01, "Standing on the cross is frozen, inside the lattice cap: " + lost);
			});
		}
		if (!correct) at(world, RimeLatticeRules.freezeAt(RimeLatticeRules.PULSES - 1) + 1, () ->
			check(HEALTH - target.getHealth() > RimeLatticeRules.DAMAGE, "Repeated freezes stack until the cap: " + target.getHealth()));
	}

	/** Thunder: the bolt jumps between crowded challengers; leaving the rod circle and spreading out is the answer. */
	private void thunderChain(TestSingleplayerContext world, boolean correct) {
		at(world, 10, () -> { if (correct) place(target, 3, 0, 0); });
		at(world, ThunderChainRules.strikeAt(0), () -> {
			if (correct) check(target.getHealth() == HEALTH && ally.getHealth() == HEALTH, "Spread out of the circles, no bolt lands: " + healths());
			else {
				check(target.getHealth() < HEALTH, "The challenger on the rod is struck");
				check(ally.getHealth() < HEALTH, "The bolt chains into the ally crowding them although the ally is outside every circle");
			}
		});
		at(world, ThunderChainRules.strikeAt(ThunderChainRules.RODS - 1), () -> {
			if (!correct) check(target.getHealth() > HEALTH - 2 * ThunderChainRules.DAMAGE * 1.5,
				"A challenger is struck once per Thunder Chain, not once per rod: " + healths());
		});
	}

	/** Verdant: the ring blooms around the challenger's spot; leaving it before it opens is the answer. */
	private void verdant(TestSingleplayerContext world, boolean correct) {
		at(world, 10, () -> { if (correct) place(target, 4.5, 0, 3.5); });
		at(world, VerdantBloomRules.TELL, () -> {
			if (correct) check(target.getHealth() == HEALTH && !target.hasEffect(MobEffects.SLOWNESS), "Outside the ring is untouched");
			else {
				var root = target.getEffect(MobEffects.SLOWNESS);
				check(target.getHealth() < HEALTH && root != null && root.getAmplifier() == VerdantBloomRules.ROOT_AMPLIFIER,
					"Inside the ring the bloom cuts and roots: " + target.getHealth() + " " + root);
			}
		});
	}

	/**
	 * Hollow: the well tugs everyone in reach inward unless they sprint. A fake player has no client to integrate the tug,
	 * so the test observes the server's sent tug and then places the challenger where the tested drift model puts them.
	 */
	private void hollow(TestSingleplayerContext world, boolean correct) {
		double start = 5.5;
		at(world, 3, () -> { place(target, start, 0, 0); target.setSprinting(correct); });
		at(world, HollowPullRules.PULL_FROM + 1, () -> {
			Vec3 tug = target.getDeltaMovement();
			if (correct) check(tug.horizontalDistanceSqr() < 1e-6, "Sprinting breaks the pull: " + tug);
			else check(tug.x < -HollowPullRules.PULL * .9 && Math.abs(tug.z) < 1e-6, "Standing still is tugged toward the well: " + tug);
		});
		at(world, HollowPullRules.TELL - 1, () -> {
			// A sprinter keeps running; stop them just inside the well's reach so they stay in the arena.
			double at = Math.min(HollowPullRules.REACH - .5, HollowPullRules.drift(start, correct ? .28 : 0, correct));
			place(target, at, 0, 0); target.setSprinting(correct);
		});
		at(world, HollowPullRules.TELL, () -> {
			if (correct) check(target.getHealth() == HEALTH, "Sprinting against the pull stays outside the collapsing core");
			else check(target.getHealth() < HEALTH, "Caught by the pull, the core collapses on the challenger");
		});
	}

	private void setup(int school, int count) {
		target = add("ElementalTarget", 4.5, 0); party.add(target);
		ally = null;
		if (count > 1) { ally = add("ElementalAlly", 6.8, 0); party.add(ally); }
		master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
		check(master != null, "The registered Master exists");
		master.snapTo(origin.x, origin.y, origin.z, 0, 0); level.addFreshEntity(master);
		master.setDiscipline(school);
		check(ElementalMasters.schoolOf(master.method()) == school, "The Master carries the " + ElementalMasters.name(school) + " method");
		for (Challenger player : party) { master.mobInteract(player, InteractionHand.MAIN_HAND); master.mobInteract(player, InteractionHand.MAIN_HAND); }
		check(SwordMaster.ready(target) == 1, "Every fake challenger explicitly joins this real trial");
		check(master.challengers().size() == count, "The consenting roster is fixed: " + master.challengers().size());
		master.setTarget(target);
	}
	private Challenger add(String name, double x, double z) {
		Challenger player = new Challenger(level, name); player.setGameMode(GameType.SURVIVAL);
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH); player.setHealth(HEALTH);
		player.snapTo(origin.x + x, origin.y, origin.z + z, 90, 0); level.addNewPlayer(player); return player;
	}
	private void place(ServerPlayer player, double x, double y, double z) {
		player.teleportTo(level, origin.x + x, origin.y + y, origin.z + z, Set.of(), 90, 0, false); player.setDeltaMovement(Vec3.ZERO);
	}
	private String healths() {
		return party.stream().map(player -> player.getGameProfile().name() + "=" + player.getHealth()).toList().toString();
	}
	private void cleanup() {
		if (master != null) master.discard();
		for (Challenger player : party) player.discard();
		party.clear(); ally = null; master = null;
	}
	private void at(TestSingleplayerContext world, int age, Runnable action) {
		long expected = began + age;
		world.getServer().waitFor(server -> {
			long now = level.getGameTime(); if (now < expected) return false;
			check(now == expected, "Observe exact native signature frame " + age + ": expected=" + expected + ", actual=" + now);
			action.run(); return true;
		}, age + 20);
	}
	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
