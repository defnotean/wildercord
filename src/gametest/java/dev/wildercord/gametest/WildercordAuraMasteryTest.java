package dev.wildercord.gametest;

import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraArmour;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraBreakthroughs;
import dev.wildercord.aura.AuraDominion;
import dev.wildercord.aura.AuraIntent;
import dev.wildercord.aura.AuraPresence;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.AuraSlash;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.aura.Spellblade;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.LeyWalker;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.WildercordEntities;
import dev.wildercord.client.AuraClient;
import dev.wildercord.client.AuraScreen;
import dev.wildercord.client.WildercordKeys;
import dev.wildercord.config.Config;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.world.LeyLines;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Aura's top stages and its hybrids, in a real world on a stone platform in the sky:
 * <ul>
 *   <li>Aura Step (Form, the real key, a double tap): about six blocks ahead, its price (and no slash loosed first), held back by
 *       its cooldown, stopped short of a wall, untouchable for its first moments, and never formed at empty (backlash);</li>
 *   <li>aura armour: a husk's blow lands a quarter lighter with aura enough, whole below the floor, the aura paying for it, and
 *       the shell seen on the client;</li>
 *   <li>Intent: a weaker husk slowed, a stronger one not; a simulated rival of a higher stage presses on the player (a slight
 *       slow), not on an equal;</li>
 *   <li>Dominion (Sovereign, the real key held): its price, foes inside slowed and hitting weaker, a blow chaining once to
 *       another foe inside (never to one outside), a second refused while it rests, and its end on time;</li>
 *   <li>the spellblade: a fire spell cast sneaking with a blade rides the next Aura Slash onto every husk the slash cuts (set
 *       alight), both prices paid, no bolt leaving; one cast standing goes out as usual; one left too long slips off and leaves as cast;</li>
 *   <li>aura marks: a Rime blade's strike leaves a husk frozen, and the player's fire spell sets off Shatter on it;</li>
 *   <li>the breakthroughs into Form (the stance held through a thunderstorm at a ley crossing; a stronger foe no longer counts)
 *       and Sovereign (a boss felled by the blade; one a spell touched doesn't count);</li>
 *   <li>against players: a rival inside a Dominion hits weaker only by the PvP scale, a chain onto a player is held to the PvP
 *       cap, and a carried spell meets the spellguard.</li>
 * </ul>
 * Screenshots ({@code aura_mastery_*}): the step's afterimages (Rime), the shell (Verdant, Hollow), Intent, a Dominion (Ember,
 * Starlit), a spell on the blade and a slash carrying it (Rime with fire, Thunder with frost), a mark, the Aura page at Sovereign.
 *
 * <p>Runs in the full suite; {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE} skip it.</p>
 */
public class WildercordAuraMasteryTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.aura_mastery";

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		context.runOnClient(mc -> {
			mc.getWindow().setWindowed(1920, 1080);
			mc.options.guiScale().set(2);
			mc.resizeGui();
		});
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("gamerule natural_regeneration false");
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("time set 3000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			run(failures, "Aura Step", () -> step(context, world));
			reset(context, world);
			run(failures, "aura armour", () -> armour(context, world));
			reset(context, world);
			run(failures, "Intent", () -> intent(context, world));
			reset(context, world);
			run(failures, "Dominion", () -> dominion(context, world));
			reset(context, world);
			run(failures, "the spellblade", () -> spellblade(context, world));
			reset(context, world);
			run(failures, "aura marks", () -> marks(context, world));
			reset(context, world);
			run(failures, "against players", () -> pvp(context, world));
			reset(context, world);
			run(failures, "the page", () -> page(context, world));
			reset(context, world);
			// Last: the tempest sends the player far off, to a ley crossing.
			run(failures, "breakthroughs", () -> breakthroughs(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("Aura's top stages went wrong:\n  " + String.join("\n  ", failures));
			}
		} finally {
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			context.getInput().releaseKey(o -> o.keyShift);
		}
	}

	private static void run(List<String> failures, String what, Runnable test) {
		try {
			test.run();
		} catch (AssertionError e) {
			failures.add(what + ": " + e.getMessage());
		}
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}

	// ------------------------------------------------------------------ Aura Step

	private static void step(ClientGameTestContext context, TestSingleplayerContext world) {
		double[] before = on(world, player -> {
			setAura(player, "rime", AuraRules.FORM, AuraRules.capacity(AuraRules.FORM), AuraRules.threshold(AuraRules.FORM));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			return new double[] {player.getX(), player.getZ()};
		});
		context.waitTicks(5);
		// Filmed from high behind, so the afterimages read along the way it went.
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(7.0);
			return null;
		});
		world.getServer().runCommand("time set 18000");
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			mc.player.setXRot(30);
			mc.player.xRotO = 30;
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(3);
		doubleTap(context);
		context.waitTicks(3);
		// Turned aside (the way the dash went is the server's already), so the camera looks along the afterimages from the side.
		context.runOnClient(mc -> {
			mc.player.setYRot(-62);
			mc.player.yRotO = -62;
			mc.player.setXRot(22);
			mc.player.xRotO = 22;
		});
		context.waitTicks(1);
		shot(context, "aura_mastery_step_rime");
		int images = context.computeOnClient(mc -> AuraClient.afterimageCount());
		context.waitTicks(4);
		firstPerson(context, world);
		world.getServer().runCommand("time set 3000");
		String moved = on(world, player -> {
			double dz = player.getZ() - before[1];
			double dx = Math.abs(player.getX() - before[0]);
			float aura = Aura.data(player).aura();
			if (dz < AuraRules.STEP_DISTANCE - 0.6 || dz > AuraRules.STEP_DISTANCE + 0.6 || dx > 0.3) {
				return "a double tap should step about " + AuraRules.STEP_DISTANCE + " blocks ahead (moved " + dz + " ahead, " + dx + " aside)";
			}
			if (Math.abs(aura - (AuraRules.capacity(AuraRules.FORM) - AuraRules.STEP_COST)) > 0.05) {
				return "a step should cost " + AuraRules.STEP_COST + " and loose no slash first (aura " + aura + ")";
			}
			return AuraPresence.timers(player).stepReadyAt() > player.level().getGameTime() ? null : "a step should start its cooldown";
		});
		check(moved == null, moved);
		check(images > 0, "everyone who sees a step (the player too) should be told where to draw its afterimages");

		// Straight away again: the cooldown holds it back.
		double z1 = on(world, player -> player.getZ());
		doubleTap(context);
		context.waitTicks(4);
		double held = on(world, player -> player.getZ() - z1);
		check(Math.abs(held) < 0.2, "the step's cooldown should hold the next one back (moved " + held + ")");

		// A wall two blocks ahead: the step stops short of it, never through it.
		on(world, player -> {
			stand(player);
			setAura(player, "rime", AuraRules.FORM, AuraRules.capacity(AuraRules.FORM), AuraRules.threshold(AuraRules.FORM));
			resetTimers(player);
			return null;
		});
		int wallZ = STAGE.getZ() + 3;
		world.getServer().runCommand("fill " + (STAGE.getX() - 3) + " " + STAGE.getY() + " " + wallZ + " " + (STAGE.getX() + 3) + " " + (STAGE.getY() + 3) + " "
			+ wallZ + " minecraft:stone");
		context.waitTicks(5);
		doubleTap(context);
		context.waitTicks(6);
		String wall = on(world, player -> {
			double front = player.getZ() + player.getBbWidth() / 2;
			if (front > wallZ + 1.0E-3) {
				return "a step should never pass into a wall (front at " + front + ", the wall at " + wallZ + ")";
			}
			return player.getZ() - (STAGE.getZ() + 0.5) > 1.5 ? null : "a step toward a wall should still carry as far as there's room (" + player.getZ() + ")";
		});
		world.getServer().runCommand("fill " + (STAGE.getX() - 3) + " " + STAGE.getY() + " " + wallZ + " " + (STAGE.getX() + 3) + " " + (STAGE.getY() + 3) + " "
			+ wallZ + " minecraft:air");
		check(wall == null, wall);

		// Untouchable as it begins: a husk's blow in that moment lands on nothing; once it's over, it lands.
		String guarded = on(world, player -> {
			stand(player);
			setAura(player, "rime", AuraRules.FORM, AuraRules.capacity(AuraRules.FORM), AuraRules.threshold(AuraRules.FORM));
			resetTimers(player);
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, -1.2), 60);
			husk.addTag("wildercord.mastery_attacker");
			if (!AuraStep.step(player)) {
				return "the step should go";
			}
			float health = player.getHealth();
			husk.doHurtTarget(player.level(), player);
			return player.getHealth() < health ? "a blow in a step's first moments should land on nothing (took " + (health - player.getHealth()) + ")" : null;
		});
		check(guarded == null, guarded);
		context.waitTicks(AuraRules.STEP_GUARD_TICKS + 4);
		float later = on(world, player -> {
			Mob husk = tagged(player, "wildercord.mastery_attacker");
			husk.teleportTo(player.getX(), player.getY(), player.getZ() - 1.2);
			dev.wildercord.cast.Effects.readyToHurt(player);
			float health = player.getHealth();
			husk.doHurtTarget(player.level(), player);
			return health - player.getHealth();
		});
		check(later > 0, "after its moment a blow should land again (took " + later + ")");

		// At empty: no step, backlash, no harm.
		String empty = on(world, player -> {
			kill(player, TAG);
			stand(player);
			setAura(player, "rime", AuraRules.FORM, 3, AuraRules.threshold(AuraRules.FORM));
			resetTimers(player);
			double z = player.getZ();
			boolean went = AuraStep.step(player);
			if (went || Math.abs(player.getZ() - z) > 0.01) {
				return "a step short of its price should never form";
			}
			return player.hasEffect(MobEffects.SLOWNESS) && player.hasEffect(MobEffects.WEAKNESS) ? null : "a step at empty should bring backlash";
		});
		check(empty == null, empty);
	}

	/** Two taps of the real Aura key, close together. */
	private static void doubleTap(ClientGameTestContext context) {
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(3);
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(1);
	}

	// ------------------------------------------------------------------ aura armour

	private static void armour(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "verdant", AuraRules.FORM, 10, AuraRules.threshold(AuraRules.FORM));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			spawn(player.level(), EntityTypes.HUSK, at(0, 1.4), 200).addTag("wildercord.mastery_attacker");
			return null;
		});
		context.waitTicks(10);
		float open = on(world, player -> huskStrikes(player));
		on(world, player -> {
			setAura(player, "verdant", AuraRules.FORM, AuraRules.capacity(AuraRules.FORM), AuraRules.threshold(AuraRules.FORM));
			player.setHealth(player.getMaxHealth());
			return null;
		});
		context.waitTicks(15);
		boolean seen = context.computeOnClient(mc -> AuraPresence.look(mc.player).shell());
		check(seen, "the client should see aura armour's shell is up");
		float[] armoured = on(world, player -> {
			float aura = Aura.data(player).aura();
			float took = huskStrikes(player);
			return new float[] {took, aura - Aura.data(player).aura()};
		});
		check(open > 0 && Math.abs(armoured[0] - open * (1 - AuraRules.ARMOUR_SHARE)) < 0.15,
			"aura armour should take a quarter of the husk's blow (" + armoured[0] + " against " + open + ")");
		check(Math.abs(armoured[1] - (open - armoured[0]) * AuraRules.ARMOUR_COST_PER_POINT) < 0.1,
			"aura armour should pay half a point of aura for each point it takes (paid " + armoured[1] + ")");
		check(open > 0, "below the floor (10 aura) the blow lands whole (" + open + ")");

		// The shell, in two colours, at night where it reads.
		world.getServer().runCommand("time set 18000");
		shell(context, world, "aura_mastery_shell_verdant", "verdant");
		shell(context, world, "aura_mastery_shell_hollow", "hollow");
		world.getServer().runCommand("time set 3000");
	}

	private static void shell(ClientGameTestContext context, TestSingleplayerContext world, String name, String method) {
		on(world, player -> {
			kill(player, TAG);
			stand(player);
			setAura(player, method, AuraRules.FORM, AuraRules.capacity(AuraRules.FORM), AuraRules.threshold(AuraRules.FORM));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, -15.0F, false);
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(3.0);
			return null;
		});
		context.waitTicks(10);
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(3);
		context.runOnClient(mc -> {
			mc.player.setYBodyRot(30);
			mc.player.yBodyRotO = 30;
		});
		context.waitTicks(2);
		shot(context, name);
		// A blow on the shell: it flares.
		on(world, player -> {
			Vec3 where = player.position();
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, -1.3), 200);
			dev.wildercord.cast.Effects.readyToHurt(player);
			husk.doHurtTarget(player.level(), player);
			husk.discard();
			player.setHealth(player.getMaxHealth());
			player.teleportTo(player.level(), where.x, where.y, where.z, Set.<Relative>of(), 0.0F, -15.0F, false);
			player.setDeltaMovement(Vec3.ZERO);
			return null;
		});
		context.waitTicks(2);
		shot(context, name + "_struck");
		firstPerson(context, world);
	}

	/** The attacking husk's blow on the player; returns what it took. */
	private static float huskStrikes(ServerPlayer player) {
		Mob husk = tagged(player, "wildercord.mastery_attacker");
		dev.wildercord.cast.Effects.readyToHurt(player);
		float before = player.getHealth();
		husk.doHurtTarget(player.level(), player);
		player.removeEffect(MobEffects.HUNGER);
		return before - player.getHealth();
	}

	// ------------------------------------------------------------------ Intent

	private static void intent(ClientGameTestContext context, TestSingleplayerContext world) {
		int[] ids = on(world, player -> {
			setAura(player, "crimson", AuraRules.FORM, AuraRules.capacity(AuraRules.FORM), AuraRules.threshold(AuraRules.FORM));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			Mob weak = spawn(player.level(), EntityTypes.HUSK, at(-2, 4), 10);
			Mob strong = spawn(player.level(), EntityTypes.HUSK, at(2, 4), 200);
			Mob far = spawn(player.level(), EntityTypes.HUSK, at(0, 14), 10);
			return new int[] {weak.getId(), strong.getId(), far.getId()};
		});
		context.waitTicks(AuraRules.INTENT_PERIOD + 6);
		String pressed = on(world, player -> {
			LivingEntity weak = (LivingEntity) player.level().getEntity(ids[0]);
			LivingEntity strong = (LivingEntity) player.level().getEntity(ids[1]);
			LivingEntity far = (LivingEntity) player.level().getEntity(ids[2]);
			if (!weak.hasEffect(MobEffects.SLOWNESS)) {
				return "a husk with half your health, nearby, should be slowed by Intent";
			}
			if (strong.hasEffect(MobEffects.SLOWNESS)) {
				return "a husk with ten times your health shouldn't feel Intent";
			}
			return far.hasEffect(MobEffects.SLOWNESS) ? "a husk fourteen blocks off is out of Intent's reach" : null;
		});
		check(pressed == null, pressed);
		// Without a blade in hand, nothing is pressed on.
		int bare = on(world, player -> {
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			return AuraIntent.active(player) ? 1 : 0;
		});
		check(bare == 0, "Intent needs a blade in hand");
		on(world, player -> {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			return null;
		});
		context.waitTicks(4);

		// A rival of a higher stage presses on the player (a slight slow and a shadow at the edges); an equal doesn't.
		String rivalry = on(world, player -> {
			setAura(player, "crimson", AuraRules.FLOW, AuraRules.capacity(AuraRules.FLOW), AuraRules.threshold(AuraRules.FLOW));
			FakePlayer rival = rival(player, "thunder", AuraRules.FORM, 1.0);
			double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
			AuraIntent.pulse(rival);
			if (!AuraIntent.slowed(player)) {
				return "a rival at Form should press on a player at Flow";
			}
			double slowed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
			double expected = speed * (1 - Config.get().aura().heights().intentPvpSlow());
			if (Math.abs(slowed - expected) > 1.0E-4) {
				return "Intent should slow a player only slightly (" + slowed + " against " + speed + ")";
			}
			return null;
		});
		check(rivalry == null, rivalry);
		for (int i = 0; i < 2; i++) {
			context.waitTicks(10);
			on(world, player -> AuraIntent.pulse(rival(player, "thunder", AuraRules.FORM, 1.0)));
		}
		context.waitTicks(6);
		shot(context, "aura_mastery_intent_vignette");
		context.waitTicks(AuraRules.INTENT_VIGNETTE_TICKS + 4);
		String equal = on(world, player -> {
			if (AuraIntent.slowed(player)) {
				return "Intent's slow should let go once it stops reaching the player";
			}
			setAura(player, "crimson", AuraRules.FORM, AuraRules.capacity(AuraRules.FORM), AuraRules.threshold(AuraRules.FORM));
			FakePlayer rival = rival(player, "thunder", AuraRules.FORM, 1.0);
			AuraIntent.pulse(rival);
			return AuraIntent.slowed(player) ? "a rival of the same stage shouldn't press on the player" : null;
		});
		check(equal == null, equal);
	}

	// ------------------------------------------------------------------ Dominion

	private static void dominion(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "ember", AuraRules.SOVEREIGN, AuraRules.capacity(AuraRules.SOVEREIGN), AuraRules.threshold(AuraRules.SOVEREIGN));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			spawn(player.level(), EntityTypes.HUSK, at(0, 1.6), 200).addTag("wildercord.mastery_a");
			spawn(player.level(), EntityTypes.HUSK, at(1.2, 2.2), 200).addTag("wildercord.mastery_b");
			spawn(player.level(), EntityTypes.HUSK, at(-1.5, 6.5), 200).addTag("wildercord.mastery_out");
			return null;
		});
		context.waitTicks(20);
		// The baseline: the husk outside strikes (aura armour aside, below its floor, to see the Dominion's own weakening later).
		float open = on(world, player -> {
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(5));
			Mob out = tagged(player, "wildercord.mastery_out");
			dev.wildercord.cast.Effects.readyToHurt(player);
			float before = player.getHealth();
			out.doHurtTarget(player.level(), player);
			float took = before - player.getHealth();
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(AuraRules.capacity(AuraRules.SOVEREIGN)));
			// Back on the spot after the blow's knockback, so the circle rises round both husks.
			stand(player);
			return took;
		});
		world.getServer().runCommand("time set 18000");
		// The real key, held.
		context.getInput().holdKey(WildercordKeys.auraMapping());
		context.waitTicks(8);
		context.getInput().releaseKey(WildercordKeys.auraMapping());
		context.waitTicks(2);
		String raised = on(world, player -> {
			if (!AuraDominion.active(player)) {
				return "holding the Aura key at Sovereign should raise a Dominion";
			}
			// Its price, give or take the trickle it has already begun to give back.
			float aura = Aura.data(player).aura();
			double left = AuraRules.capacity(AuraRules.SOVEREIGN) - AuraRules.DOMINION_COST;
			if (aura < left - 0.5 || aura > left + AuraRules.DOMINION_TRICKLE * AuraRules.DOMINION_FLOW + 0.5) {
				return "a Dominion should cost " + AuraRules.DOMINION_COST + " (aura " + aura + ")";
			}
			return Heart.grimoire(player).contains("aura:dominion") ? null : "the first Dominion goes into the Grimoire";
		});
		check(raised == null, raised);
		// Filmed from above, the circle round the player.
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(7.5);
			return null;
		});
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			mc.player.setXRot(55);
			mc.player.xRotO = 55;
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(12);
		shot(context, "aura_mastery_dominion_ember");
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			mc.player.setXRot(8);
			mc.player.xRotO = 8;
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
			return null;
		});
		context.waitTicks(4);
		shot(context, "aura_mastery_dominion_hud");
		String pressed = on(world, player -> {
			Mob a = tagged(player, "wildercord.mastery_a");
			Mob out = tagged(player, "wildercord.mastery_out");
			MobEffectInstance slow = a.getEffect(MobEffects.SLOWNESS);
			if (slow == null || slow.getAmplifier() < AuraRules.DOMINION_SLOW_CREATURE) {
				return "a husk inside a Dominion should be slowed (Slowness II): " + slow;
			}
			return out.hasEffect(MobEffects.SLOWNESS) ? "a husk outside the circle shouldn't be" : null;
		});
		check(pressed == null, pressed);
		float inside = on(world, player -> {
			// Aura armour aside (its floor), to see the Dominion's own weakening.
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(5));
			Mob a = tagged(player, "wildercord.mastery_a");
			dev.wildercord.cast.Effects.readyToHurt(player);
			float before = player.getHealth();
			a.doHurtTarget(player.level(), player);
			float took = before - player.getHealth();
			player.setHealth(player.getMaxHealth());
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(120));
			return took;
		});
		check(open > 0 && Math.abs(inside - open * (1 - AuraRules.DOMINION_WEAKEN)) < 0.15,
			"a husk inside a Dominion should hit 30% weaker (" + inside + " against " + open + " outside)");
		context.waitTicks(15);
		String[] why = {""};
		double[] chained = on(world, player -> {
			Mob a = tagged(player, "wildercord.mastery_a");
			Mob b = tagged(player, "wildercord.mastery_b");
			Mob out = tagged(player, "wildercord.mastery_out");
			float a0 = a.getHealth();
			float b0 = b.getHealth();
			float o0 = out.getHealth();
			float aura0 = Aura.data(player).aura();
			player.attack(a);
			why[0] = "Dominion up " + AuraDominion.active(player) + ", the struck husk inside " + AuraDominion.inside(player, a) + ", the other inside "
				+ AuraDominion.inside(player, b) + ", aura " + aura0 + " to " + Aura.data(player).aura() + ", the struck husk " + a0 + " to " + a.getHealth()
				+ ", the other " + b0 + " to " + b.getHealth();
			return new double[] {b0 - b.getHealth(), o0 - out.getHealth(), a0 - a.getHealth()};
		});
		check(chained[2] > 0 && chained[0] > 0, "a blow inside the Dominion should chain to the other husk inside (" + chained[0] + "; " + why[0] + ")");
		check(chained[1] <= 1.0E-4, "the chain should never reach a husk outside (" + chained[1] + ")");
		// Again at once: resting.
		float auraBefore = on(world, player -> Aura.data(player).aura());
		context.getInput().holdKey(WildercordKeys.auraMapping());
		context.waitTicks(8);
		context.getInput().releaseKey(WildercordKeys.auraMapping());
		context.waitTicks(2);
		String resting = on(world, player -> {
			AuraPresence.Timers timers = AuraPresence.timers(player);
			long now = player.level().getGameTime();
			if (timers.dominionReadyAt() - now < AuraRules.DOMINION_COOLDOWN - AuraRules.DOMINION_TICKS - 40) {
				return "a Dominion should rest about a minute and a half (ready in " + (timers.dominionReadyAt() - now) + " ticks)";
			}
			// (The Dominion's own trickle still runs: a point or two.)
			float aura = Aura.data(player).aura();
			return aura >= auraBefore - 0.01 && aura - auraBefore < 3 ? null : "a Dominion while resting should cost nothing (aura " + aura + ", was " + auraBefore + ")";
		});
		check(resting == null, resting);
		context.waitTicks(AuraRules.DOMINION_TICKS);
		boolean ended = on(world, player -> !AuraDominion.active(player));
		check(ended, "a Dominion should end after about eight seconds");
		world.getServer().runCommand("time set 3000");

		// Starlit's colour, for the gallery.
		on(world, player -> {
			kill(player, TAG);
			stand(player);
			setAura(player, "starlit", AuraRules.SOVEREIGN, AuraRules.capacity(AuraRules.SOVEREIGN), AuraRules.threshold(AuraRules.SOVEREIGN));
			resetTimers(player);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(7.5);
			return null;
		});
		world.getServer().runCommand("time set 18000");
		context.waitTicks(4);
		on(world, player -> AuraDominion.raise(player));
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			mc.player.setXRot(55);
			mc.player.xRotO = 55;
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(30);
		shot(context, "aura_mastery_dominion_starlit");
		firstPerson(context, world);
		world.getServer().runCommand("time set 3000");
		// Its circle on the ground lasts as long as it does: waited out, so it isn't in the next part's pictures.
		context.waitTicks(AuraRules.DOMINION_TICKS);
	}

	// ------------------------------------------------------------------ the spellblade

	private static void spellblade(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "rime", AuraRules.EDGE, AuraRules.capacity(AuraRules.EDGE), AuraRules.threshold(AuraRules.EDGE));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			spell(player, List.of(Runes.BOLT.id(), Runes.FIRE.id()));
			for (int i = 0; i < 3; i++) {
				spawn(player.level(), EntityTypes.HUSK, at(0, 3 + 2.5 * i), 200).addTag("wildercord.mastery_line");
			}
			return null;
		});
		context.waitTicks(10);
		// Sneaking with the blade: the spell's price is paid as it's cast (read in the same moment, before any comes back).
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(3);
		float[] price = on(world, player -> {
			int cost = Heart.manaCost(player, SpellCompiler.compile(List.of(Runes.BOLT, Runes.FIRE)), 1.0);
			float mana0 = Spellbooks.mana(player);
			SpellCaster.cast(player, 0);
			return new float[] {mana0 - Spellbooks.mana(player), cost, player.isShiftKeyDown() ? 1 : 0};
		});
		check(price[2] == 1, "the server should see the player sneaking");
		check(Math.abs(price[0] - price[1]) < 0.5, "the spell's own price should be paid as it's cast (" + price[0] + " of " + price[1] + ")");
		context.waitTicks(8);
		String held = on(world, player -> {
			if (!Spellblade.holding(player)) {
				return "a spell cast sneaking with a blade at Edge should flow into the blade";
			}
			boolean bolt = !player.level().getEntitiesOfClass(dev.wildercord.cast.RuneBolt.class, player.getBoundingBox().inflate(24)).isEmpty();
			return bolt ? "a spell on the blade shouldn't also fly as its bolt" : null;
		});
		check(held == null, held);
		boolean seen = context.computeOnClient(mc -> AuraPresence.look(mc.player).spellHeld(mc.level.getGameTime()));
		check(seen, "the client should see a spell riding the blade");
		context.getInput().releaseKey(o -> o.keyShift);
		// The blade holding it, close, in third person, and in first person.
		world.getServer().runCommand("time set 18000");
		bladeShot(context, world, "aura_mastery_spellblade_rime_fire");
		// The slash, by the real key, filmed from high behind.
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(6.5);
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, 8.0F, false);
			return null;
		});
		context.waitTicks(3);
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(2);
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			mc.player.setXRot(55);
			mc.player.xRotO = 55;
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(1);
		shot(context, "aura_mastery_spellblade_slash_rime");
		firstPerson(context, world);
		world.getServer().runCommand("time set 3000");
		context.waitTicks(10);
		String carried = on(world, player -> {
			List<Mob> line = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16), m -> m.entityTags().contains("wildercord.mastery_line"));
			if (line.size() != 3) {
				return "three husks should be in the line (" + line.size() + ")";
			}
			for (Mob husk : line) {
				if (!husk.isOnFire()) {
					return "the slash should carry the fire spell onto every husk it cut (one isn't burning, health " + husk.getHealth() + ")";
				}
				if (husk.getHealth() >= 200) {
					return "every husk in the line should be cut";
				}
			}
			float aura = Aura.data(player).aura();
			if (Math.abs(aura - (AuraRules.capacity(AuraRules.EDGE) - AuraRules.SLASH_COST)) > 0.05) {
				return "the slash's own price should be paid too (aura " + aura + ")";
			}
			if (Spellblade.holding(player)) {
				return "the slash should take the spell off the blade";
			}
			return Heart.grimoire(player).contains("aura:spellblade") ? null : "the first spellblade goes into the Grimoire";
		});
		check(carried == null, carried);

		// Cast standing: it goes out as usual, as its bolt.
		String standing = on(world, player -> {
			kill(player, TAG);
			stand(player);
			setAura(player, "rime", AuraRules.EDGE, AuraRules.capacity(AuraRules.EDGE), AuraRules.threshold(AuraRules.EDGE));
			Spellbooks.setReadyAt(player, 0, 0);
			Spellbooks.setMana(player, 400);
			SpellCaster.cast(player, 0);
			return null;
		});
		check(standing == null, standing);
		context.waitTicks(5);
		String flew = on(world, player -> Spellblade.holding(player) ? "a spell cast standing shouldn't ride the blade"
			: player.level().getEntitiesOfClass(dev.wildercord.cast.RuneBolt.class, player.getBoundingBox().inflate(24)).isEmpty()
			? "a spell cast standing should fly as its bolt" : null);
		check(flew == null, flew);
		context.waitTicks(30);

		// Left on the blade too long, it slips off and leaves as cast: the husk ahead is set alight by the bolt.
		on(world, player -> {
			kill(player, TAG);
			player.level().getEntitiesOfClass(dev.wildercord.cast.RuneBolt.class, player.getBoundingBox().inflate(64)).forEach(net.minecraft.world.entity.Entity::discard);
			stand(player);
			Spellbooks.setReadyAt(player, 0, 0);
			Spellbooks.setMana(player, 400);
			spawn(player.level(), EntityTypes.HUSK, at(0, 5), 200).addTag("wildercord.mastery_target");
			return null;
		});
		context.waitTicks(10);
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(3);
		context.getInput().pressKey(WildercordKeys.castMapping());
		context.waitTicks(8);
		context.getInput().releaseKey(o -> o.keyShift);
		boolean waiting = on(world, player -> Spellblade.holding(player));
		check(waiting, "the second spell should ride the blade too");
		context.waitTicks(AuraRules.SPELLBLADE_TICKS + 30);
		String slipped = on(world, player -> {
			if (Spellblade.holding(player)) {
				return "a spell left on the blade past its time should slip off";
			}
			Mob husk = tagged(player, "wildercord.mastery_target");
			return husk.isOnFire() || husk.getHealth() < 200 ? null : "a spell slipping off the blade should leave as cast, its bolt reaching the husk ahead";
		});
		check(slipped == null, slipped);

		// Thunder's blade with a frost spell, for the gallery.
		on(world, player -> {
			kill(player, TAG);
			stand(player);
			setAura(player, "thunder", AuraRules.EDGE, AuraRules.capacity(AuraRules.EDGE), AuraRules.threshold(AuraRules.EDGE));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
			spell(player, List.of(Runes.BOLT.id(), Runes.FROST.id()));
			return null;
		});
		context.waitTicks(5);
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(3);
		context.getInput().pressKey(WildercordKeys.castMapping());
		context.waitTicks(8);
		context.getInput().releaseKey(o -> o.keyShift);
		world.getServer().runCommand("time set 18000");
		bladeShot(context, world, "aura_mastery_spellblade_thunder_frost");
		world.getServer().runCommand("time set 3000");
	}

	/** The blade at chest height, close, in third person from the front, then in first person. */
	private static void bladeShot(ClientGameTestContext context, TestSingleplayerContext world, String name) {
		on(world, player -> {
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, -22.0F, false);
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(2.3);
			return null;
		});
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(3);
		context.runOnClient(mc -> {
			mc.player.setYBodyRot(48);
			mc.player.yBodyRotO = 48;
		});
		context.waitTicks(2);
		shot(context, name + "_tp");
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		on(world, player -> {
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, 25.0F, false);
			return null;
		});
		context.waitTicks(3);
		shot(context, name + "_fp");
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
			return null;
		});
	}

	/** Teaches every rune, puts {@code runes} in the first spell, and fills the mana. */
	private static void spell(ServerPlayer player, List<String> runes) {
		var book = Spellbooks.get(player).withStarterGiven();
		for (String id : runes) {
			book = book.learn(id);
		}
		Spellbooks.set(player, book);
		SpellCaster.edit(player, 0, runes);
		Spellbooks.setReadyAt(player, 0, 0);
		Spellbooks.setMana(player, 400);
	}

	// ------------------------------------------------------------------ aura marks

	private static void marks(ClientGameTestContext context, TestSingleplayerContext world) {
		// Every strike marks, for the test (the chance times three at Sovereign is more than certain).
		Path file = Config.path();
		String original = read(file);
		try {
			write(file, original.replaceAll("\"mark_chance_multiplier\": [0-9.]+", "\"mark_chance_multiplier\": 3.0"));
			world.getServer().runOnServer(Config::reload);
			check(Config.get().aura().heights().markChanceMultiplier() == 3.0, "the test's config should be in force");
			on(world, player -> {
				setAura(player, "rime", AuraRules.SOVEREIGN, AuraRules.capacity(AuraRules.SOVEREIGN), AuraRules.threshold(AuraRules.SOVEREIGN));
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
				spell(player, List.of(Runes.BOLT.id(), Runes.FIRE.id()));
				spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 200).addTag("wildercord.mastery_target");
				return null;
			});
			context.waitTicks(25);
			String marked = on(world, player -> {
				Mob husk = tagged(player, "wildercord.mastery_target");
				player.attack(husk);
				return Reactions.has(husk, Reactions.Mark.FROZEN) ? null : "a Rime blade's strike should leave the husk frozen";
			});
			check(marked == null, marked);
			world.getServer().runCommand("time set 18000");
			on(world, player -> {
				player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
				return null;
			});
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
				mc.player.setXRot(20);
				mc.player.xRotO = 20;
			});
			context.waitTicks(3);
			shot(context, "aura_mastery_mark_frozen");
			firstPerson(context, world);
			world.getServer().runCommand("time set 3000");
			// The player's own fire spell sets it off: Shatter.
			on(world, player -> {
				SpellCaster.cast(player, 0);
				return null;
			});
			context.waitTicks(12);
			String shattered = on(world, player -> {
				Mob husk = tagged(player, "wildercord.mastery_target");
				if (Reactions.has(husk, Reactions.Mark.FROZEN)) {
					return "the fire spell should have used up the frozen mark (husk health " + husk.getHealth() + ")";
				}
				return Heart.grimoire(player).contains(Feats.reactionKey("shatter")) ? null : "a fire spell on the aura's frozen mark should set off Shatter";
			});
			check(shattered == null, shattered);
			// Earth leaves nothing.
			String earth = on(world, player -> {
				kill(player, TAG);
				setAura(player, "stone", AuraRules.SOVEREIGN, AuraRules.capacity(AuraRules.SOVEREIGN), AuraRules.threshold(AuraRules.SOVEREIGN));
				Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 200);
				player.resetAttackStrengthTicker();
				return husk.getId() > 0 ? null : "the husk should be there";
			});
			check(earth == null, earth);
			context.waitTicks(25);
			String none = on(world, player -> {
				Mob husk = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(8), m -> m.entityTags().contains(TAG)).getFirst();
				player.attack(husk);
				return Reactions.marks(husk).isEmpty() && !husk.isOnFire() ? null : "a Stone blade should leave no mark (" + Reactions.marks(husk) + ")";
			});
			check(none == null, none);
		} finally {
			write(file, original);
			world.getServer().runOnServer(Config::reload);
		}
	}

	// ------------------------------------------------------------------ against players

	private static void pvp(ClientGameTestContext context, TestSingleplayerContext world) {
		// A rival inside the player's Dominion strikes the player: weaker, but only by the PvP scale.
		String weakened = on(world, player -> {
			setAura(player, "ember", AuraRules.SOVEREIGN, AuraRules.capacity(AuraRules.SOVEREIGN), AuraRules.threshold(AuraRules.SOVEREIGN));
			resetTimers(player);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			player.removeAttached(WildercordAttachments.SPELLGUARD);
			FakePlayer rival = rival(player, "", 0, 20.0);
			// Outside first (no Dominion yet), then inside one.
			rival.snapTo(player.getX(), player.getY(), player.getZ() + 1.5, 180, 0);
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(5));
			float open = rivalStrikes(rival, player);
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(AuraRules.capacity(AuraRules.SOVEREIGN)));
			if (!AuraDominion.raise(player)) {
				return "the Dominion should rise";
			}
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(5));
			float inside = rivalStrikes(rival, player);
			double expected = open * (1 - AuraRules.DOMINION_WEAKEN * Config.get().aura().pvpScale());
			if (open <= 0 || Math.abs(inside - expected) > 0.15) {
				return "a rival inside a Dominion should hit weaker only by the PvP scale (" + inside + " against " + open + ", expected " + expected
					+ "; Dominion up " + AuraDominion.active(player) + ", 10 would land at " + AuraDominion.weakened(player, player.damageSources().playerAttack(rival), 10F)
					+ ", the rival harmable " + dev.wildercord.cast.Targets.canHarm(player, rival) + " at " + rival.position() + " alive " + rival.isAlive() + ")";
			}
			return null;
		});
		check(weakened == null, weakened);

		// A chain onto a player is aura off the blade: held to the PvP cap. A rival raises a Dominion and strikes a husk beside the
		// player, inside it: the chain leaps to the player.
		String chain = on(world, player -> {
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(0));
			player.setHealth(player.getMaxHealth());
			player.removeAttached(WildercordAttachments.SPELLGUARD);
			dev.wildercord.cast.Effects.readyToHurt(player);
			FakePlayer rival = rival(player, "ember", AuraRules.SOVEREIGN, 8.0);
			rival.snapTo(player.getX(), player.getY(), player.getZ() + 2.0, 180, 0);
			rival.setAttached(AuraPresence.TIMERS, AuraPresence.Timers.NONE);
			Mob husk = spawn(player.level(), EntityTypes.HUSK, new Vec3(player.getX() + 1.0, player.getY(), player.getZ() + 1.0), 200);
			if (!AuraDominion.raise(rival)) {
				return "the rival's Dominion should rise";
			}
			float before = husk.getHealth();
			float health = player.getHealth();
			rival.resetAttackStrengthTicker();
			rival.attack(husk);
			float took = before - husk.getHealth();
			float chained = health - player.getHealth();
			double cap = took * AuraRules.DOMINION_CHAIN_SHARE * Config.get().aura().damageScale() * Config.get().defence().maxBonus()
				* Config.get().aura().pvpScale() + 0.05;
			if (took <= 0 || chained <= 0) {
				return "a blow inside the rival's Dominion should chain to the player inside (husk took " + took + ", player " + chained + ")";
			}
			return chained <= cap ? null : "a chain onto a player should be held to the PvP cap (" + chained + " against at most " + cap + ")";
		});
		check(chain == null, chain);
		// A mark never sets a player alight or poisons them; one that does nothing by itself is left as on a creature.
		String marked = on(world, player -> {
			FakePlayer rival = rival(player, "", 0, 1.0);
			rival.clearFire();
			rival.removeAllEffects();
			dev.wildercord.aura.AuraMarks.leave(player, rival, "burning");
			dev.wildercord.aura.AuraMarks.leave(player, rival, "poisoned");
			if (rival.isOnFire() || rival.hasEffect(MobEffects.POISON)) {
				return "an aura mark should never set a player alight or poison them";
			}
			dev.wildercord.aura.AuraMarks.leave(player, rival, "bleeding");
			return Reactions.has(rival, Reactions.Mark.BLEEDING) ? null : "a bleeding mark should be left on a player as on anything";
		});
		check(marked == null, marked);
		on(world, player -> {
			kill(player, TAG);
			return null;
		});
		context.waitTicks(AuraRules.DOMINION_TICKS + 5);

		// A spell carried on a rival's slash meets the player's spell defences: the spellguard holds a killing one on one heart.
		String guarded = on(world, player -> {
			stand(player);
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(0));
			player.removeAttached(WildercordAttachments.SPELLGUARD);
			dev.wildercord.cast.Effects.readyToHurt(player);
			FakePlayer rival = rival(player, "rime", AuraRules.EDGE, 1.0);
			var root = SpellCompiler.compile(List.of(Runes.BOLT, Runes.HARM)).root();
			Cast cast = new Cast(rival, 1, Heart.Bonuses.NONE.withPower(60), false, null, new Cast.Info(root, 2, ""));
			if (!Spellblade.draw(rival, cast, root, c -> {})) {
				return "the rival's blade should take the spell";
			}
			AuraSlash.loose(rival);
			return null;
		});
		check(guarded == null, guarded);
		context.waitTicks(8);
		String held = on(world, player -> {
			if (!player.isAlive() || Math.abs(player.getHealth() - 2.0F) > 0.01) {
				return "the spellguard should hold a killing carried spell on one heart (health " + player.getHealth() + ", alive " + player.isAlive() + ")";
			}
			return player.getAttached(WildercordAttachments.SPELLGUARD) == null ? "the spellguard should note that it held" : null;
		});
		check(held == null, held);
		on(world, player -> {
			player.setHealth(player.getMaxHealth());
			return null;
		});
	}

	/** A rival's punch (its attack damage {@code attack}) on the player, through vanilla's own attack; returns what it took. */
	private static float rivalStrikes(FakePlayer rival, ServerPlayer player) {
		dev.wildercord.cast.Effects.readyToHurt(player);
		player.setHealth(player.getMaxHealth());
		float before = player.getHealth();
		// Each strike from the same footing (the swing barely begun), so the two compare.
		rival.resetAttackStrengthTicker();
		rival.attack(player);
		return before - player.getHealth();
	}

	/** A rival player (simulated) three blocks in front, facing the player, with the given aura and a sword. */
	private static FakePlayer rival(ServerPlayer player, String method, int stage, double attack) {
		FakePlayer rival = FakePlayer.get(player.level());
		rival.snapTo(player.getX(), player.getY(), player.getZ() + 3, 180, 0);
		rival.setYHeadRot(180);
		if (stage > 0) {
			rival.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(method, stage, AuraRules.threshold(stage), AuraRules.capacity(stage), 0));
		} else {
			rival.removeAttached(AuraAttachments.AURA);
		}
		rival.removeAttached(AuraAttachments.STATE);
		rival.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(stage > 0 ? Items.IRON_SWORD : Items.STICK));
		AttributeInstance damage = rival.getAttribute(Attributes.ATTACK_DAMAGE);
		damage.setBaseValue(attack);
		return rival;
	}

	// ------------------------------------------------------------------ the page

	private static void page(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "starlit", AuraRules.SOVEREIGN, 131, AuraRules.threshold(AuraRules.SOVEREIGN) + 220);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			return null;
		});
		context.waitTicks(3);
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		shot(context, "aura_mastery_page_sovereign");
		context.setScreen(() -> null);
		on(world, player -> {
			setAura(player, "hollow", AuraRules.EDGE, 64, AuraRules.threshold(AuraRules.FORM));
			return null;
		});
		context.waitTicks(3);
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		shot(context, "aura_mastery_page_form_waits");
		context.setScreen(() -> null);
		context.waitTicks(2);
		shot(context, "aura_mastery_hud_edge");
	}

	// ------------------------------------------------------------------ breakthroughs

	private static void breakthroughs(ClientGameTestContext context, TestSingleplayerContext world) {
		// A stronger foe made Edge; it no longer makes Form.
		on(world, player -> {
			kill(player, TAG);
			stand(player);
			setAura(player, "stone", AuraRules.EDGE, AuraRules.capacity(AuraRules.EDGE), AuraRules.threshold(AuraRules.FORM));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 60).addTag("wildercord.mastery_target");
			return null;
		});
		check(on(world, player -> AuraBreakthroughs.ready(player)), "Form's threshold reached, a breakthrough should wait");
		context.waitTicks(30);
		on(world, player -> {
			Mob foe = tagged(player, "wildercord.mastery_target");
			player.attack(foe);
			foe.setHealth(1);
			return null;
		});
		context.waitTicks(30);
		int stronger = on(world, player -> {
			player.attack(tagged(player, "wildercord.mastery_target"));
			return Aura.stage(player);
		});
		check(stronger == AuraRules.EDGE, "a stronger foe shouldn't make the breakthrough into Form (stage " + stronger + ")");

		// A boss for Sovereign: one a spell touched doesn't count, a clean one does.
		on(world, player -> {
			kill(player, TAG);
			stand(player);
			setAura(player, "stone", AuraRules.FORM, AuraRules.capacity(AuraRules.FORM), AuraRules.threshold(AuraRules.SOVEREIGN));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));
			spawn(player.level(), EntityTypes.ELDER_GUARDIAN, at(-2, 3), 80).addTag("wildercord.mastery_tainted");
			spawn(player.level(), EntityTypes.ELDER_GUARDIAN, at(2, 3), 80).addTag("wildercord.mastery_worthy");
			return null;
		});
		context.waitTicks(30);
		String opened = on(world, player -> {
			Mob foe = tagged(player, "wildercord.mastery_tainted");
			player.attack(foe);
			if (Aura.state(player).trialUntil() <= player.level().getGameTime()) {
				return "a blow on a boss should open the guardian's trial";
			}
			if (Aura.state(player).trialUntil() - player.level().getGameTime() < AuraRules.TRIAL_WINDOW + 20) {
				return "the guardian's trial should give a boss fight its longer window";
			}
			dev.wildercord.cast.Effects.readyToHurt(foe);
			foe.hurtServer(player.level(), player.level().damageSources().indirectMagic(player, player), 1);
			foe.setHealth(1);
			return AuraBreakthroughs.spoiled(player, foe) ? null : "a spell of the player's on the boss should spoil the trial";
		});
		check(opened == null, opened);
		context.waitTicks(30);
		int tainted = on(world, player -> {
			player.attack(tagged(player, "wildercord.mastery_tainted"));
			return Aura.stage(player);
		});
		check(tainted == AuraRules.FORM, "a boss a spell touched shouldn't count (stage " + tainted + ")");
		context.waitTicks(30);
		on(world, player -> {
			Mob foe = tagged(player, "wildercord.mastery_worthy");
			player.attack(foe);
			foe.setHealth(1);
			return null;
		});
		context.waitTicks(30);
		String worthy = on(world, player -> {
			player.attack(tagged(player, "wildercord.mastery_worthy"));
			AuraAttachments.Data data = Aura.data(player);
			if (data.stage() != AuraRules.SOVEREIGN) {
				return "felling a boss by the blade alone should break through to Sovereign (" + data + ")";
			}
			if (Math.abs(data.aura() - AuraRules.capacity(AuraRules.SOVEREIGN)) > 0.5) {
				return "the breakthrough should fill Sovereign's aura (" + data.aura() + ")";
			}
			return Heart.grimoire(player).contains("aura:sovereign") ? null : "the breakthrough should go into the Grimoire";
		});
		check(worthy == null, worthy);

		// The tempest for Form: at a ley crossing, first under a clear sky (nothing comes of it), then through a thunderstorm.
		double[] heart = on(world, player -> strongCrossing(LeyWalker.seed(player.level())));
		check(heart != null, "no ley crossing within reach of spawn");
		int hx = (int) Math.floor(heart[0]);
		int hz = (int) Math.floor(heart[1]);
		int y = STAGE.getY();
		world.getServer().runCommand("forceload add " + (hx - 2) + " " + (hz - 2) + " " + (hx + 2) + " " + (hz + 2));
		world.getServer().runCommand("fill " + (hx - 3) + " " + (y - 1) + " " + (hz - 3) + " " + (hx + 3) + " " + (y - 1) + " " + (hz + 3) + " minecraft:stone");
		world.getServer().runCommand("fill " + (hx - 3) + " " + y + " " + (hz - 3) + " " + (hx + 3) + " " + (y + 8) + " " + (hz + 3) + " minecraft:air");
		on(world, player -> {
			kill(player, TAG);
			player.teleportTo(player.level(), heart[0], y, heart[1], Set.<Relative>of(), 0.0F, 15.0F, false);
			player.setDeltaMovement(Vec3.ZERO);
			setAura(player, "ember", AuraRules.EDGE, 8, AuraRules.threshold(AuraRules.FORM));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			return null;
		});
		context.waitTicks(20);
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(AuraRules.SETTLE_TICKS + 40);
		int calm = on(world, player -> Aura.state(player).stillness());
		check(calm == 0, "at Edge, stillness under a clear sky shouldn't count toward Form (held " + calm + ")");
		world.getServer().runCommand("weather thunder");
		context.waitTicks(10);
		context.waitTicks(AuraRules.TEMPEST_FORM_TICKS - 60);
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		world.getServer().runCommand("time set 18000");
		int stage = AuraRules.EDGE;
		for (int i = 0; i < 160 && stage == AuraRules.EDGE; i++) {
			context.waitTicks(1);
			stage = on(world, player -> Aura.stage(player));
		}
		context.waitTicks(4);
		shot(context, "aura_mastery_breakthrough_form");
		world.getServer().runCommand("time set 3000");
		world.getServer().runCommand("weather clear");
		context.getInput().releaseKey(o -> o.keyShift);
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		int heldFor = on(world, player -> Aura.state(player).stillness());
		check(stage == AuraRules.FORM, "the stance held through a thunderstorm at a ley crossing should break through to Form (stage " + stage
			+ ", stillness " + heldFor + ")");
		boolean noted = on(world, player -> Heart.grimoire(player).contains("aura:form"));
		check(noted, "the breakthrough into Form should go into the Grimoire");
		world.getServer().runCommand("forceload remove all");
	}

	/** The nearest ley crossing to spawn whose heart runs strongly, or null. */
	private static double[] strongCrossing(long seed) {
		for (int ring = 0; ring <= 200; ring++) {
			for (int ix = -ring; ix <= ring; ix++) {
				for (int iz = -ring; iz <= ring; iz++) {
					if (Math.max(Math.abs(ix), Math.abs(iz)) != ring) {
						continue;
					}
					double[] heart = LeyLines.crossingIn(seed, ix, iz);
					if (heart != null && heart[2] >= 0.6) {
						return heart;
					}
				}
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ the stage

	/** A stone platform in the sky; the player in survival with an Echo Cord, facing down it (+z). */
	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 12) + " " + (y - 1) + " " + (z - 8) + " " + (x + 12) + " " + (y - 1) + " " + (z + 26) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 12) + " " + y + " " + (z - 8) + " " + (x + 12) + " " + (y + 6) + " " + (z + 26) + " minecraft:air");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			stand(player);
		});
	}

	/** The player back on the spot, facing +z, healthy, nothing lingering. */
	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.removeAllEffects();
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);
	}

	private static void reset(ClientGameTestContext context, TestSingleplayerContext world) {
		context.getInput().releaseKey(o -> o.keyShift);
		world.getServer().runCommand("kill @e[tag=" + TAG + "]");
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runCommand("kill @e[type=wildercord:rune_bolt]");
		context.waitTicks(10);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			player.removeAttached(AuraAttachments.STATE);
			// A Dominion left standing from the last part would feed this one's aura.
			AuraDominion.end(player);
			resetTimers(player);
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
		});
		firstPerson(context, world);
		context.waitTicks(5);
	}

	private static void firstPerson(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			mc.player.setXRot(8);
			mc.player.xRotO = 8;
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
			return null;
		});
	}

	private static void resetTimers(ServerPlayer player) {
		player.setAttached(AuraPresence.TIMERS, AuraPresence.Timers.NONE);
	}

	private static void setAura(ServerPlayer player, String method, int stage, float aura, double xp) {
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(method, stage, xp, aura, 0));
		player.removeAttached(AuraAttachments.STATE);
	}

	private static Vec3 at(double side, double ahead) {
		return new Vec3(STAGE.getX() + 0.5 + side, STAGE.getY(), STAGE.getZ() + 0.5 + ahead);
	}

	private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, Vec3 at, double health) {
		T mob = type.create(level, EntitySpawnReason.COMMAND);
		if (mob == null) {
			throw new AssertionError("couldn't make a " + type);
		}
		mob.snapTo(at.x, at.y, at.z, 180, 0);
		mob.setNoAi(true);
		mob.addTag(TAG);
		mob.addTag("wildercord.rolled");
		AttributeInstance max = mob.getAttribute(Attributes.MAX_HEALTH);
		if (max != null) {
			max.setBaseValue(health);
		}
		mob.setHealth((float) health);
		level.addFreshEntity(mob);
		return mob;
	}

	private static Mob tagged(ServerPlayer player, String tag) {
		List<Mob> found = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48), m -> m.entityTags().contains(tag) && m.isAlive());
		if (found.isEmpty()) {
			throw new AssertionError("the mob tagged " + tag + " is gone");
		}
		return found.getFirst();
	}

	private static void kill(ServerPlayer player, String tag) {
		for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(64), e -> e.entityTags().contains(tag))) {
			e.discard();
		}
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.waitTicks(1);
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static String read(Path file) {
		try {
			return Files.readString(file, StandardCharsets.UTF_8);
		} catch (java.io.IOException e) {
			throw new AssertionError("couldn't read " + file + ": " + e);
		}
	}

	private static void write(Path file, String text) {
		try {
			Files.writeString(file, text, StandardCharsets.UTF_8);
		} catch (java.io.IOException e) {
			throw new AssertionError("couldn't write " + file + ": " + e);
		}
	}

	private static <T> T on(TestSingleplayerContext world, Function<ServerPlayer, T> step) {
		return world.getServer().computeOnServer(server -> step.apply(player(server)));
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}
}
