package dev.wildercord.aura.world;

import com.mojang.authlib.GameProfile;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.BreathingManualItem;
import dev.wildercord.aura.SwordString;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.aura.arts.DuneArts;
import dev.wildercord.aura.arts.IronArts;
import dev.wildercord.aura.arts.MethodsAFlavours;
import dev.wildercord.aura.arts.TideArts;
import dev.wildercord.cast.Reactions;
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
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The methods-a pack end to end in a real client: a survival player learns Tide, Iron and Dune from their manuals, performs
 * two arts of each through SwordStrings with their own outcomes, then each Master is challenged with consent, one of its
 * named techniques is read wrong and right, and its signature is held, misread and answered, with the receipt checked.
 */
public final class MethodsAMastersNativeTest implements FabricClientGameTest {
	private static final String SUITE = "dev.wildercord.aura.world.MethodsAMastersNativeTest";
	private static final float HEALTH = 200;
	private static final int[] SCHOOLS = {MethodsAMasters.TIDE, MethodsAMasters.IRON, MethodsAMasters.DUNE};
	private static final class Challenger extends FakePlayer {
		Challenger(ServerLevel level, String name) { super(level, new GameProfile(UUID.randomUUID(), name)); }
		@Override public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return false; }
	}
	private enum Read { HOLD, WRONG, ANSWER }
	private record ArtCase(String method, String art, String outcome, Predicate<LivingEntity> foe, Predicate<ServerPlayer> self) {}

	private ServerLevel level;
	private Vec3 origin;
	private ServerPlayer player;
	private LivingEntity dummy;
	private SwordMaster master;
	private Challenger target;
	private long began;
	private Vec3 anchor;
	private float facing;
	private double radius;

	@Override public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false"))
				world.getServer().runCommand(command);
			world.getServer().runOnServer(server -> {
				player = server.getPlayerList().getPlayers().getFirst();
				level = player.level(); origin = new Vec3(player.getBlockX() + .5, 181, player.getBlockZ() + .5);
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"" + SUITE + "\",\"seed\":\"{}\"}", level.getSeed());
				for (int x = -20; x <= 20; x++) for (int z = -20; z <= 20; z++)
					level.setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.STONE.defaultBlockState());
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(level, origin.x, origin.y, origin.z, Set.of(), 0, 0, false);
			});
			context.runOnClient(mc -> { mc.player.setYRot(0); mc.player.setYHeadRot(0); mc.player.setXRot(0); });
			context.waitTicks(5);
			for (String method : List.of(TideArts.METHOD, IronArts.METHOD, DuneArts.METHOD)) learn(world, method);
			for (ArtCase art : List.of(
				new ArtCase(TideArts.METHOD, TideArts.RIPTIDE_CUT, "hurt and soaked",
					foe -> Reactions.has(foe, Reactions.Mark.WET), self -> true),
				new ArtCase(TideArts.METHOD, TideArts.BREAKER, "hurt and soaked by the breaking line",
					foe -> Reactions.has(foe, Reactions.Mark.WET), self -> true),
				new ArtCase(IronArts.METHOD, IronArts.SUNDER_CUT, "hurt and sundered",
					foe -> MethodsAFlavours.sundered(foe) > 0, self -> true),
				// Bulwark and Sandveil are earned counters: SwordStrings refuses them without a real consumed stroke, so the
				// second art of each is the leaping Anvil Fall and the Quicksand field instead.
				new ArtCase(IronArts.METHOD, IronArts.ANVIL_FALL, "hurt and sundered where the leap lands",
					foe -> MethodsAFlavours.sundered(foe) > 0, self -> true),
				new ArtCase(DuneArts.METHOD, DuneArts.GRIT_FLICK, "hurt and blinded",
					foe -> foe.hasEffect(MobEffects.BLINDNESS), self -> true),
				new ArtCase(DuneArts.METHOD, DuneArts.QUICKSAND, "hurt and sinking in the sand",
					foe -> foe.hasEffect(MobEffects.SLOWNESS), self -> true)))
				art(world, context, art);
			world.getServer().runOnServer(server -> {
				player.removeAllEffects();
				player.setGameMode(GameType.SPECTATOR);
				player.teleportTo(level, origin.x, origin.y + 6, origin.z - 6, Set.of(), 0, 30, false);
			});
			for (int school : SCHOOLS) {
				technique(world, school, false);
				technique(world, school, true);
				for (Read read : Read.values()) signature(world, school, read);
			}
		}
	}

	/** Survival learning: the real manual item, used once for a first method and twice (ask, then confirm) to switch. */
	private void learn(TestSingleplayerContext world, String method) {
		world.getServer().runOnServer(server -> {
			boolean switching = Aura.data(player).learned();
			player.setItemInHand(InteractionHand.MAIN_HAND, BreathingManualItem.of(method));
			player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
			if (switching) {
				check(!Aura.data(player).method().equals(method) && !player.getMainHandItem().isEmpty(),
					"Switching from a known method first asks for confirmation: " + method);
				player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
			}
			check(Aura.data(player).learned() && Aura.data(player).method().equals(method), "The manual teaches " + method + ": " + Aura.data(player).method());
			check(player.getMainHandItem().isEmpty(), "Learning spends the manual");
		});
	}

	private void art(TestSingleplayerContext world, ClientGameTestContext context, ArtCase art) {
		world.getServer().runOnServer(server -> {
			player.removeAllEffects();
			player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(art.method(), AuraRules.SOVEREIGN,
				AuraRules.threshold(AuraRules.SOVEREIGN), AuraRules.capacity(AuraRules.SOVEREIGN), 0));
			player.removeAttached(SwordStrings.COOLDOWNS);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
			player.teleportTo(level, origin.x, origin.y, origin.z, Set.of(), 0, 0, false);
			Mob husk = EntityTypes.HUSK.create(level, EntitySpawnReason.COMMAND);
			check(husk != null, "A husk dummy exists");
			husk.setNoAi(true); husk.setPersistenceRequired();
			husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH); husk.setHealth(HEALTH);
			husk.snapTo(origin.x, origin.y, origin.z + 2.5, 180, 0);
			level.addFreshEntity(husk);
			dummy = husk;
		});
		context.runOnClient(mc -> { mc.player.setYRot(0); mc.player.setYHeadRot(0); mc.player.setXRot(0); });
		context.waitTicks(3);
		world.getServer().runOnServer(server -> {
			player.setYRot(0); player.setYHeadRot(0); player.setXRot(0);
			AuraApi.StringArt string = AuraApi.artOf(player, art.art()).orElseThrow(() -> new AssertionError("Registered art " + art.art()));
			List<Integer> marks = string.string().tokens().stream().map(t -> SwordString.Token.marks(t)).toList();
			float before = Aura.aura(player);
			check(SwordStrings.perform(player, string, marks), "The art is admitted through SwordStrings: " + art.art());
			check(Aura.aura(player) < before, "The art pays its Aura: " + art.art());
		});
		try {
			world.getServer().waitFor(server -> dummy.getHealth() < HEALTH && art.foe().test(dummy) && art.self().test(player), 100);
		} catch (AssertionError timeout) {
			String[] state = new String[1];
			world.getServer().runOnServer(server -> state[0] = "health=" + dummy.getHealth() + " alive=" + dummy.isAlive()
				+ " foe=" + art.foe().test(dummy) + " armour=" + dummy.getArmorValue() + " sundered=" + MethodsAFlavours.sundered(dummy)
				+ " dummyAt=" + dummy.position().subtract(origin) + " playerAt=" + player.position().subtract(origin)
				+ " yaw=" + player.getYRot() + " aura=" + Aura.aura(player));
			throw new AssertionError("The art " + art.art() + " should leave its foe " + art.outcome() + ": " + state[0], timeout);
		}
		world.getServer().runOnServer(server -> {
			dev.wildercord.Wildercord.LOGGER.info("METHODS_A_ART {} {}: {} (health={})", art.method(), art.art(), art.outcome(), dummy.getHealth());
			dummy.discard();
		});
		context.waitTicks(30);
	}

	/** The first natural attack is the school's ordinary opener; then one named combo is read wrong or right. */
	private void technique(TestSingleplayerContext world, int school, boolean answer) {
		world.getServer().runOnServer(server -> setup(school, 2.5));
		world.getServer().waitFor(server -> {
			if (!master.state(AuraFighter.WINDUP) || master.attackAnimation() == 0) return false;
			MastersRules.Move opener = MastersRules.Move.values()[master.attackAnimation() - 1];
			check(opener != MastersRules.Move.TECHNIQUE && !MethodsAMasters.signature(opener), "The natural opener is a plain ordinary attack: " + opener);
			check(master.attackTellTicks() >= 12 && master.attackTellTicks() <= 14, "Ordinary tell is 12 to 14 ticks: " + opener + " " + master.attackTellTicks());
			return true;
		}, 80);
		world.getServer().waitFor(server -> {
			// Dune opens with a crescent: let it land or fade before the combo, so only the combo's own strike is read.
			if (get("attack") != null || dev.wildercord.aura.Crescents.inFlight().stream().anyMatch(f -> f.caster() == master)) return false;
			target.setHealth(HEALTH);
			place(master.position().add(0, 0, 2.5).subtract(origin));
			check(invoke(), "A named technique is admitted in reach");
			began = level.getGameTime();
			return true;
		}, 60);
		MasterTechniques.Technique combo = (MasterTechniques.Technique) get("technique");
		check(combo != null && combo.school() == school && combo.id() > 100, "The combo is one of this school's own: " + combo);
		check(master.technique() == combo.id() && master.attackAnimation() == MastersRules.Move.TECHNIQUE.ordinal() + 1, "The synced combo id is the admitted one");
		int impact = combo.impact(0);
		check(impact >= 12 && impact <= 14, "The first strike tells 12 to 14 ticks: " + combo.key() + " " + impact);
		MasterTechniques.Strike strike = combo.strikes().getFirst();
		at(world, impact - 1, () -> {
			check(target.getHealth() == HEALTH, "The tell is harmless: " + combo.key() + " health=" + target.getHealth()
				+ " source=" + (target.getLastDamageSource() == null ? null : target.getLastDamageSource().typeHolder().getRegisteredName()
				+ "/" + target.getLastDamageSource().getEntity()) + " strikes=" + combo.strikes());
			if (!answer) return;
			Vec3 aim = (Vec3) get("lockedAim");
			check(aim != null, "The aim locks before the strike");
			Vec3 side = new Vec3(-aim.z, 0, aim.x), at = master.position().subtract(origin);
			switch (strike.shape()) {
				case ARC_LOW -> { target.setNoGravity(true); place(at.add(aim.scale(2)).add(0, 1, 0)); }
				case ARC_HIGH -> { target.setShiftKeyDown(true); target.setPose(Pose.CROUCHING); check(target.isCrouching(), "The fixture crouches"); }
				case LANE -> place(at.add(aim.scale(2)).add(side.scale(2.5)));
				case ARC, CIRCLE -> place(at.add(aim.scale(strike.reach() + 2.5)));
			}
		});
		at(world, impact, () -> {
			String receipt = MethodsAMasters.id(school) + " " + combo.key() + " " + strike.shape() + " health=" + target.getHealth();
			if (answer) check(target.getHealth() == HEALTH, "The named answer avoids the strike: " + receipt);
			else check(target.getHealth() < HEALTH && target.getLastDamageSource() != null
				&& target.getLastDamageSource().getEntity() == master, "Standing still is hit by the owned strike: " + receipt);
			check(HEALTH - target.getHealth() <= MethodsAMasters.CHAIN_CAP + .01, "One strike stays within the chain cap");
			dev.wildercord.Wildercord.LOGGER.info("METHODS_A_TECHNIQUE {} {}", answer ? "ANSWER" : "WRONG", receipt);
			target.setNoGravity(false); target.setShiftKeyDown(false); target.setPose(Pose.STANDING);
			cleanup();
		});
	}

	private void signature(TestSingleplayerContext world, int school, Read read) {
		MastersRules.Move move = MethodsAMasters.signature(school);
		double distance = school == MethodsAMasters.TIDE ? 4 : 3;
		world.getServer().runOnServer(server -> {
			setup(school, distance);
			set("sequence", 1);
		});
		world.getServer().waitFor(server -> {
			if (!master.methodsAPending()) return false;
			began = level.getGameTime() - (long) master.attackElapsed(0);
			anchor = target.position(); facing = master.getYRot();
			radius = Math.sqrt(target.position().subtract(master.position()).horizontalDistanceSqr());
			check(master.attackAnimation() == move.ordinal() + 1 && master.attackTellTicks() == move.tell,
				"Ordinary server AI admits the appended signature: " + master.attackAnimation());
			check(master.auraRemaining() <= MastersRules.AURA_MAX - MethodsAMasters.cost(move) + .01, "The signature pays its Aura once");
			return true;
		}, 80);
		Object accepted = get("methodsA");
		int[] expected = switch (school) {
			case MethodsAMasters.TIDE -> tide(world, read);
			case MethodsAMasters.IRON -> iron(world, read);
			default -> dune(world, read);
		};
		at(world, move.tell + 1, () -> {
			check(!master.methodsAPending() && !master.state(AuraFighter.WINDUP), "The form ends in an open recovery: " + move);
			check(get("methodsA") == null && accepted != null, "A finished form is released");
			MethodsASignatureRules.Receipt receipt = master.methodsAReceipt();
			check(receipt.kind() == move && receipt.complete() && receipt.landed() == expected[0] && receipt.evaded() == expected[1],
				"The receipt counts each beat: " + read + " " + receipt);
			dev.wildercord.Wildercord.LOGGER.info("METHODS_A_SIGNATURE {} {} lost={} receipt={}", move, read, lost(), receipt);
			cleanup();
		});
	}

	/** Undertow Ring: step inside the wave's band, then back out past the undertow. Returns the expected {landed, evaded}. */
	private int[] tide(TestSingleplayerContext world, Read read) {
		int wave = MethodsASignatureRules.WAVE, undertow = MethodsASignatureRules.UNDERTOW;
		double damage = MethodsASignatureRules.DAMAGE;
		if (read != Read.HOLD) at(world, wave - 2, () -> ring(1));
		at(world, wave, () -> {
			boolean hit = read == Read.HOLD && MethodsASignatureRules.wave(radius, 0);
			check(read != Read.HOLD || hit, "The challenger started inside the wave's band: r=" + radius);
			check(close(lost(), hit ? damage : 0), "The wave breaks only over its band: " + read + " lost=" + lost());
		});
		if (read == Read.ANSWER) at(world, wave + 1, () -> ring(MethodsASignatureRules.UNDERTOW_RADIUS + 1));
		boolean holdUndertow = MethodsASignatureRules.undertow(radius, 0);
		at(world, undertow, () -> {
			double want = switch (read) {
				case HOLD -> damage + (holdUndertow ? damage : 0);
				case WRONG -> damage;
				case ANSWER -> 0;
			};
			check(close(lost(), want), "The undertow drags only the inner circle: " + read + " lost=" + lost());
		});
		return switch (read) {
			case HOLD -> new int[] {holdUndertow ? 2 : 1, holdUndertow ? 0 : 1};
			case WRONG -> new int[] {1, 1};
			case ANSWER -> new int[] {0, 2};
		};
	}

	/** Anvil Verdict: sidestep the locked hammer, then be off the ground when the anvil rings. */
	private int[] iron(TestSingleplayerContext world, Read read) {
		int hammer = MethodsASignatureRules.HAMMER, shock = MethodsASignatureRules.SHOCK;
		double damage = MethodsASignatureRules.DAMAGE;
		// After the lock: the spot no longer follows.
		if (read != Read.HOLD) at(world, hammer - 2, () -> frame(0, 3, 0));
		at(world, hammer, () -> check(close(lost(), read == Read.HOLD ? damage : 0),
			"The hammer falls on the locked spot: " + read + " lost=" + lost()));
		if (read == Read.ANSWER) at(world, shock - 1, () -> { target.setNoGravity(true); frame(0, 3, 1); });
		at(world, shock, () -> {
			double want = switch (read) {
				case HOLD -> Math.min(MethodsAMasters.CHAIN_CAP, 2 * damage);
				case WRONG -> damage;
				case ANSWER -> 0;
			};
			check(close(lost(), want), "The shock runs through the ground, not the air: " + read + " lost=" + lost());
			target.setNoGravity(false);
		});
		return switch (read) {
			case HOLD -> new int[] {2, 0};
			case WRONG -> new int[] {1, 1};
			case ANSWER -> new int[] {0, 2};
		};
	}

	/** Shifting Sands: sidestep the lane, then step back into the spent lane before the storm. */
	private int[] dune(TestSingleplayerContext world, Read read) {
		int lane = MethodsASignatureRules.LANE, storm = MethodsASignatureRules.STORM;
		double damage = MethodsASignatureRules.DAMAGE;
		if (read != Read.HOLD) at(world, lane - 2, () -> frame(0, 2, 0));
		at(world, lane, () -> {
			check(close(lost(), read == Read.HOLD ? damage : 0), "The sand runs down the lane: " + read + " lost=" + lost());
			if (read == Read.HOLD && MethodsASignatureRules.blind(true, false) > 0)
				check(target.hasEffect(MobEffects.BLINDNESS), "A challenger caught in the lane is blinded");
		});
		if (read == Read.ANSWER) at(world, storm - 2, () -> frame(0, 0, 0));
		at(world, storm, () -> check(close(lost(), read == Read.ANSWER ? 0 : damage),
			"The storm takes everything but the spent lane: " + read + " lost=" + lost()));
		return switch (read) {
			case HOLD, WRONG -> new int[] {1, 1};
			case ANSWER -> new int[] {0, 2};
		};
	}

	private void setup(int school, double distance) {
		target = new Challenger(level, "MethodsATarget"); target.setGameMode(GameType.SURVIVAL);
		target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH); target.setHealth(HEALTH);
		target.snapTo(origin.x, origin.y, origin.z + distance, 180, 0); level.addNewPlayer(target);
		target.setOnGround(true);
		master = AuraWorld.SWORD_MASTER.create(level, EntitySpawnReason.COMMAND);
		check(master != null, "The registered Master exists");
		master.setDiscipline(school);
		check(master.method().equals(MethodsAMasters.method(school)), "The Master teaches its own school");
		master.snapTo(origin.x, origin.y, origin.z, 0, 0); level.addFreshEntity(master);
		master.mobInteract(target, InteractionHand.MAIN_HAND); master.mobInteract(target, InteractionHand.MAIN_HAND);
		check(SwordMaster.ready(target) == 1, "The challenger explicitly accepts this trial");
		master.setTarget(target);
	}

	private double lost() { return HEALTH - target.getHealth(); }
	/** A fixture player has no client to report its footing, so the test reports it: on the floor or not. */
	private void place(Vec3 offset) {
		target.teleportTo(level, origin.x + offset.x, origin.y + offset.y, origin.z + offset.z, Set.of(), 180, 0, false);
		target.setDeltaMovement(Vec3.ZERO);
		target.setOnGround(offset.y < .01);
	}
	/** Moves the challenger relative to where it stood when the form began, along and across the master's held facing. */
	private void frame(double forward, double side, double up) {
		double radians = Math.toRadians(facing);
		Vec3 f = new Vec3(-Math.sin(radians), 0, Math.cos(radians)), right = new Vec3(-f.z, 0, f.x);
		place(anchor.add(f.scale(forward)).add(right.scale(side)).add(0, up, 0).subtract(origin));
	}
	/** Puts the challenger {@code r} from the planted master along its held facing. */
	private void ring(double r) {
		double radians = Math.toRadians(facing);
		Vec3 f = new Vec3(-Math.sin(radians), 0, Math.cos(radians));
		place(master.position().add(f.scale(r)).subtract(origin));
	}
	private void cleanup() {
		if (master != null) master.discard();
		if (target != null) target.discard();
	}
	private void at(TestSingleplayerContext world, int age, Runnable action) {
		long expected = began + age;
		world.getServer().waitFor(server -> {
			long now = level.getGameTime(); if (now < expected) return false;
			check(now == expected, "Observe exact native frame " + age + ": expected=" + expected + ", actual=" + now); action.run(); return true;
		}, age + 20);
	}
	private boolean invoke() {
		try {
			var method = SwordMaster.class.getDeclaredMethod("beginTechnique", ServerLevel.class, LivingEntity.class, long.class);
			method.setAccessible(true);
			return (boolean) method.invoke(master, level, target, level.getGameTime());
		} catch (ReflectiveOperationException error) { throw new AssertionError(error); }
	}
	private Object get(String name) {
		try { var field = SwordMaster.class.getDeclaredField(name); field.setAccessible(true); return field.get(master); }
		catch (ReflectiveOperationException error) { throw new AssertionError(error); }
	}
	private void set(String name, int value) {
		try { var field = SwordMaster.class.getDeclaredField(name); field.setAccessible(true); field.setInt(master, value); }
		catch (ReflectiveOperationException error) { throw new AssertionError(error); }
	}
	private static boolean close(double a, double b) { return Math.abs(a - b) < .01; }
	private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
