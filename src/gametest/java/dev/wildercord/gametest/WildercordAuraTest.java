package dev.wildercord.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraBreakthroughs;
import dev.wildercord.aura.AuraCombat;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.AuraSlash;
import dev.wildercord.aura.BreathingManualItem;
import dev.wildercord.aura.BreathingMethods;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.LeyWalker;
import dev.wildercord.cast.RuneBolt;
import dev.wildercord.cast.TrainingDummy;
import dev.wildercord.cast.WildercordEntities;
import dev.wildercord.client.AuraClient;
import dev.wildercord.client.AuraScreen;
import dev.wildercord.client.CordScreen;
import dev.wildercord.client.WildercordKeys;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.world.LeyLines;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Aura, the swordsman's path, in a real world on a stone platform in the sky:
 * <ul>
 *   <li>the Aura key: Z by default, under Wildercord, sharing its key with nothing;</li>
 *   <li>a Breathing Manual teaches its method (Glow, its colour and element) and is used up; another asks twice and then
 *       switches, back to the start of the stage, the aura emptied;</li>
 *   <li>a full swing on a husk fills aura and earns experience; a half swing earns nothing; a training dummy gives a quarter
 *       and its practice stops at the cap; the breathing stance fills aura, a breath on the beat more, and senses the husks
 *       within 16 blocks (outlined on the client) but not one further off;</li>
 *   <li>Glow: a coated blow lands 10% harder, and carries its element (Verdant on the undead, half again);</li>
 *   <li>Flow: an axe sweeps, wider than a sword ever did; the guard halves a husk's blow; a perfect guard turns it whole and
 *       staggers the husk, sends an arrow back at its shooter, and parries a spell;</li>
 *   <li>Edge: a block more reach, a bite through diamond armour, and the slash (the real key) cutting a line of husks for the
 *       weapon's damage in its element, at its price, held back by its cooldown;</li>
 *   <li>backlash at empty: slowed and weakened, never hurt;</li>
 *   <li>breakthroughs by both trials: half a minute of stillness at a ley crossing, and a stronger foe felled by the blade
 *       (one a spell touched first doesn't count);</li>
 *   <li>against a player (a simulated one): the cap and the PvP scale on aura's bonuses, and the spellguard holding a slash.</li>
 * </ul>
 * Screenshots: the blade at each stage in a few methods' colours, first and third person ({@code aura_blade_*}), the aura bar
 * on the spell panel and alone ({@code aura_hud_*}), the Aura page ({@code aura_page*}), aura sense ({@code aura_sense}), a
 * slash in flight ({@code aura_slash}) and a breakthrough ({@code aura_breakthrough}).
 *
 * <p>Runs in the full suite; {@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE} skip it.</p>
 */
public class WildercordAuraTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 180, 0);
	private static final String TAG = "wildercord.aura_test";
	/** The last creature Fabric's after-damage event told of, and by what (to check nothing aura wraps keeps it from firing). */
	private static volatile String lastDamaged = "";
	private static boolean listening;

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
		if (!listening) {
			listening = true;
			net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) ->
				lastDamaged = entity.getUUID() + " " + source.getMsgId());
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule natural_regeneration false");
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("time set 3000");
			world.getServer().runCommand("weather clear");
			stage(world);
			context.waitTicks(10);
			List<String> failures = new ArrayList<>();
			run(failures, "the Aura key", () -> controls(context));
			run(failures, "learning a method", () -> learning(context, world));
			reset(context, world);
			run(failures, "gaining aura", () -> gains(context, world));
			reset(context, world);
			run(failures, "Glow", () -> glow(context, world));
			reset(context, world);
			run(failures, "Flow", () -> flow(context, world));
			reset(context, world);
			run(failures, "Edge", () -> edge(context, world));
			reset(context, world);
			run(failures, "backlash", () -> backlash(context, world));
			reset(context, world);
			run(failures, "the blade's look, the bar and the page", () -> visuals(context, world));
			reset(context, world);
			run(failures, "against a player", () -> pvp(context, world));
			reset(context, world);
			// Last: the stillness trial sends the player far off, to a ley crossing.
			run(failures, "breakthroughs", () -> breakthroughs(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("Aura went wrong:\n  " + String.join("\n  ", failures));
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

	// ------------------------------------------------------------------ the key

	private static void controls(ClientGameTestContext context) {
		String problem = context.computeOnClient(mc -> {
			KeyMapping aura = WildercordKeys.auraMapping();
			if (aura == null) {
				return "there's no Aura key";
			}
			if (aura.getDefaultKey().getValue() != InputConstants.KEY_Z) {
				return "the Aura key should default to Z (it's " + aura.getDefaultKey().getName() + ")";
			}
			if (!aura.getCategory().id().equals(dev.wildercord.Wildercord.id("wildercord"))) {
				return "the Aura key should be listed under Wildercord (it's under " + aura.getCategory().id() + ")";
			}
			for (KeyMapping other : mc.options.keyMappings) {
				if (other != aura && other.getDefaultKey().equals(aura.getDefaultKey())) {
					return "the Aura key's default is also " + other.getName() + "'s";
				}
			}
			return null;
		});
		check(problem == null, problem);
	}

	// ------------------------------------------------------------------ learning

	private static void learning(ClientGameTestContext context, TestSingleplayerContext world) {
		String first = on(world, player -> {
			player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE);
			ItemStack manual = BreathingManualItem.of("ember");
			player.setItemInHand(InteractionHand.MAIN_HAND, manual);
			manual.use(player.level(), player, InteractionHand.MAIN_HAND);
			AuraAttachments.Data data = Aura.data(player);
			if (!data.method().equals("ember") || data.stage() != AuraRules.GLOW) {
				return "reading Ember Breath's manual should teach it at Glow (" + data + ")";
			}
			if (!player.getMainHandItem().isEmpty()) {
				return "the manual should be used up";
			}
			if (!Heart.grimoire(player).contains("aura:method_ember")) {
				return "the Grimoire should note the method";
			}
			return Aura.element(player).equals("fire") ? null : "Ember Breath's aura should carry fire (" + Aura.element(player) + ")";
		});
		check(first == null, first);
		context.waitTicks(3);
		String seen = context.computeOnClient(mc -> Aura.data(mc.player).method().equals("ember") && Aura.color(mc.player) == BreathingMethods.EMBER.color()
			? null : "the client should know its own method and colour (" + Aura.data(mc.player) + ", " + Integer.toHexString(Aura.color(mc.player)) + ")");
		check(seen == null, seen);
		String switching = on(world, player -> {
			player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.FLOW, 400, 30, 0));
			ItemStack rime = BreathingManualItem.of("rime");
			player.setItemInHand(InteractionHand.MAIN_HAND, rime);
			rime.use(player.level(), player, InteractionHand.MAIN_HAND);
			if (!Aura.data(player).method().equals("ember") || rime.isEmpty()) {
				return "a second method should ask to be read again before switching (" + Aura.data(player) + ")";
			}
			rime.use(player.level(), player, InteractionHand.MAIN_HAND);
			AuraAttachments.Data data = Aura.data(player);
			if (!data.method().equals("rime") || data.stage() != AuraRules.FLOW) {
				return "reading it again should switch to Rime Breath and keep Flow (" + data + ")";
			}
			if (Math.abs(data.xp() - AuraRules.threshold(AuraRules.FLOW)) > 1e-6 || data.aura() != 0) {
				return "switching should go back to the start of the stage and empty the aura (" + data + ")";
			}
			return Aura.color(player) == BreathingMethods.RIME.color(AuraRules.FLOW) ? null : "the aura should take Rime's colour";
		});
		check(switching == null, switching);
	}

	// ------------------------------------------------------------------ gaining aura

	private static void gains(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "ember", AuraRules.GLOW, 0, 0);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 200).addTag("wildercord.aura_a");
			spawn(player.level(), EntityTypes.HUSK, at(1.5, 2.2), 200).addTag("wildercord.aura_b");
			return null;
		});
		context.waitTicks(30);
		double[] hit = on(world, player -> {
			Mob a = tagged(player, "wildercord.aura_a");
			Mob b = tagged(player, "wildercord.aura_b");
			double aura0 = Aura.data(player).aura();
			double xp0 = Aura.data(player).xp();
			player.attack(a);
			double aura1 = Aura.data(player).aura();
			double xp1 = Aura.data(player).xp();
			// Straight after: a swing barely begun.
			player.attack(b);
			double aura2 = Aura.data(player).aura();
			double xp2 = Aura.data(player).xp();
			return new double[] {aura1 - aura0, xp1 - xp0, aura2 - aura1, xp2 - xp1, 200 - b.getHealth()};
		});
		check(hit[0] > 1.5, "a full swing on a husk should fill aura (" + hit[0] + ")");
		check(hit[1] > 0, "a full swing on a husk should earn experience (" + hit[1] + ")");
		check(hit[4] > 0, "the half swing should still have landed (" + hit[4] + ")");
		check(hit[2] <= 1e-6 && hit[3] <= 1e-6, "a swing barely begun should earn nothing (" + hit[2] + " aura, " + hit[3] + " experience)");

		// A training dummy: a quarter of the aura, and its practice stops at the cap.
		context.waitTicks(30);
		double[] dummy = on(world, player -> {
			TrainingDummy target = WildercordEntities.TRAINING_DUMMY.create(player.level(), EntitySpawnReason.COMMAND);
			target.snapTo(STAGE.getX() + 0.5 - 2.5, STAGE.getY(), STAGE.getZ() + 0.5 + 2.2, 180, 0);
			target.addTag(TAG);
			player.level().addFreshEntity(target);
			setAura(player, "ember", AuraRules.GLOW, 0, 0);
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withXp(0, AuraRules.PRACTICE_CAP - 0.1));
			return new double[] {target.getId()};
		});
		context.waitTicks(30);
		double[] practised = on(world, player -> {
			Entity target = player.level().getEntity((int) dummy[0]);
			player.attack(target);
			AuraAttachments.Data data = Aura.data(player);
			return new double[] {data.aura(), data.practice(), data.xp()};
		});
		check(practised[0] > 0 && practised[0] < 0.4 * hit[0], "a dummy should give about a quarter of a real blow's aura (" + practised[0] + " against " + hit[0] + ")");
		check(practised[1] <= AuraRules.PRACTICE_CAP + 1e-6 && practised[2] <= 0.1 + 1e-6,
			"a dummy's practice should stop at the cap (" + practised[1] + " practice, " + practised[2] + " experience)");

		// The breathing stance: aura flows in, a breath on the beat draws in more, and the husks nearby are sensed.
		int[] husks = on(world, player -> {
			setAura(player, "ember", AuraRules.GLOW, 0, 0);
			kill(player, TAG);
			Mob near = spawn(player.level(), EntityTypes.HUSK, at(-6, 6), 200);
			Mob far = spawn(player.level(), EntityTypes.HUSK, at(2, 22), 200);
			stand(player);
			return new int[] {near.getId(), far.getId()};
		});
		context.waitTicks(5);
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(AuraRules.SETTLE_TICKS + 30);
		boolean breathing = context.computeOnClient(mc -> Aura.state(mc.player).breathing());
		check(breathing, "sneaking still with a blade in hand should settle into the breathing stance (seen on the client)");
		float early = on(world, player -> Aura.data(player).aura());
		check(early > 0.5, "the stance should draw aura in (" + early + ")");
		// The beat: let sneak up just before it and press it again on it.
		long[] beat = context.computeOnClient(mc -> new long[] {AuraRules.nextBeat(Aura.state(mc.player).settledAt(), mc.level.getGameTime()), mc.level.getGameTime()});
		long until = beat[0] - beat[1];
		if (until < 4) {
			until += AuraRules.BREATH_PERIOD;
		}
		context.waitTicks((int) (until - 3));
		float before = on(world, player -> Aura.data(player).aura());
		context.getInput().releaseKey(o -> o.keyShift);
		context.waitTicks(2);
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(3);
		float after = on(world, player -> Aura.data(player).aura());
		check(after - before >= AuraRules.BEAT_GAIN - 0.05, "a breath on the beat should draw in " + AuraRules.BEAT_GAIN + " more (" + (after - before) + ")");
		context.waitTicks(AuraRules.BREATH_PERIOD + 4);
		String sensed = context.computeOnClient(mc -> {
			Entity near = mc.level.getEntity(husks[0]);
			Entity far = mc.level.getEntity(husks[1]);
			if (near == null) {
				return "the near husk isn't on the client";
			}
			if (!AuraClient.sensed(near)) {
				return "a husk six blocks off should be sensed by the breath (" + AuraClient.sensedCount() + " sensed)";
			}
			return far != null && AuraClient.sensed(far) ? "a husk 22 blocks off shouldn't be sensed" : null;
		});
		// Toward the near husk (six blocks off to the left and ahead).
		on(world, player -> {
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 45.0F, 12.0F, false);
			return null;
		});
		context.waitTicks(4);
		shot(context, "aura_sense");
		context.getInput().releaseKey(o -> o.keyShift);
		check(sensed == null, sensed);
	}

	// ------------------------------------------------------------------ Glow

	private static void glow(ClientGameTestContext context, TestSingleplayerContext world) {
		// The climate where the platform stands (a hot land favours fire, the sun life) counts for aura as for a spell.
		double fire = on(world, player -> dev.wildercord.cast.PowerPlaces.of(player).factor("fire"));
		double life = on(world, player -> dev.wildercord.cast.PowerPlaces.of(player).factor("life"));
		double plain = blowOnFreshHusk(context, world, "ember", AuraRules.GLOW, 0, Items.IRON_SWORD, false);
		double coated = blowOnFreshHusk(context, world, "ember", AuraRules.GLOW, 20, Items.IRON_SWORD, false);
		check(plain > 0 && Math.abs(coated / plain - (1 + AuraRules.COAT_BONUS) * fire) < 0.02,
			"a coated blow should land 10% harder (" + coated + " against " + plain + ", fire's climate " + fire + ")");
		double verdant = blowOnFreshHusk(context, world, "verdant", AuraRules.GLOW, 20, Items.IRON_SWORD, false);
		check(Math.abs(verdant / plain - (1 + AuraRules.COAT_BONUS) * 1.5 * life) < 0.05,
			"a Verdant blade's blow should carry life, half again on the undead (" + verdant + " against " + plain + ", life's climate " + life + ")");
	}

	/** A full swing on a fresh 200-health husk (in diamond armour if asked) with the given aura; returns what it took. */
	private static double blowOnFreshHusk(ClientGameTestContext context, TestSingleplayerContext world, String method, int stage, float aura, Item weapon,
			boolean armoured) {
		on(world, player -> {
			kill(player, TAG);
			stand(player);
			setAura(player, method, stage, aura, AuraRules.threshold(stage));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(weapon));
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 200);
			husk.addTag("wildercord.aura_target");
			if (armoured) {
				husk.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
				husk.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
				husk.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
				husk.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
			}
			return null;
		});
		context.waitTicks(30);
		return on(world, player -> {
			Mob husk = tagged(player, "wildercord.aura_target");
			float before = husk.getHealth();
			player.attack(husk);
			return (double) (before - husk.getHealth());
		});
	}

	// ------------------------------------------------------------------ Flow

	private static void flow(ClientGameTestContext context, TestSingleplayerContext world) {
		// The sweep: an axe never sweeps, but a Flowing blade's every swing does, and wider than a sword's.
		double[] uncoated = sweep(context, world, 0);
		double[] coated = sweep(context, world, AuraRules.capacity(AuraRules.FLOW));
		check(uncoated[0] > 0 && uncoated[1] <= 1e-6, "without aura an axe shouldn't sweep (" + uncoated[1] + " on the husk beside)");
		check(coated[1] > 0, "a Flowing axe should sweep the husk beside, further than a sword's sweep reaches (" + coated[1] + ")");

		// The guard: a husk's blow, then the same through a held guard.
		on(world, player -> {
			kill(player, TAG);
			stand(player);
			setAura(player, "stone", AuraRules.FLOW, AuraRules.capacity(AuraRules.FLOW), AuraRules.threshold(AuraRules.FLOW));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			spawn(player.level(), EntityTypes.HUSK, at(0, 1.4), 200).addTag("wildercord.aura_attacker");
			return null;
		});
		context.waitTicks(25);
		float open = on(world, player -> huskStrikes(player));
		context.waitTicks(25);
		raiseGuard(context);
		context.waitTicks(AuraRules.PERFECT_TICKS + 4);
		boolean guarding = on(world, player -> Aura.state(player).guarding(player.level().getGameTime()));
		check(guarding, "sneak and the Aura key should raise the guard");
		float guarded = on(world, player -> huskStrikes(player));
		check(open > 0 && Math.abs(guarded - open * AuraRules.GUARD_SHARE) < 0.15,
			"a held guard should halve the husk's blow (" + guarded + " against " + open + ")");
		context.getInput().releaseKey(o -> o.keyShift);
		context.waitTicks(AuraRules.GUARD_REST + 5);

		// A perfect guard: the blow turned aside whole, the husk staggered.
		raiseGuard(context);
		context.waitTicks(2);
		String perfect = on(world, player -> {
			float taken = huskStrikes(player);
			Mob husk = tagged(player, "wildercord.aura_attacker");
			if (taken > 0) {
				return "a perfect guard should turn the blow aside whole (took " + taken + ")";
			}
			MobEffectInstance slow = husk.getEffect(MobEffects.SLOWNESS);
			if (slow == null || slow.getAmplifier() < 2 || !husk.hasEffect(MobEffects.WEAKNESS)) {
				return "a perfect guard should stagger the husk (slowness " + slow + ")";
			}
			return Heart.grimoire(player).contains("aura:perfect_guard") ? null : "the first perfect guard should go into the Grimoire";
		});
		check(perfect == null, perfect);
		context.getInput().releaseKey(o -> o.keyShift);
		context.waitTicks(AuraRules.GUARD_REST + 5);

		// An arrow loosed at a perfect guard flies back at its shooter. It's loosed once the server has the guard in its perfect
		// moment, so the moment can't run out while the arrow is still on its way.
		raiseGuard(context);
		for (int t = 0; t < 4 && !on(world, player -> AuraGuard.perfectNow(player)); t++) {
			context.waitTicks(1);
		}
		String raised = on(world, player -> AuraGuard.perfectNow(player) ? null
			: "the guard should be in its perfect moment (guarding " + AuraGuard.guarding(player) + ")");
		check(raised == null, raised);
		int arrowId = on(world, player -> {
			player.setHealth(player.getMaxHealth());
			// Off to one side, so the husk standing in front of the player isn't in the arrow's way.
			Mob shooter = spawn(player.level(), EntityTypes.SKELETON, at(5, 5), 200);
			shooter.addTag("wildercord.aura_shooter");
			Arrow arrow = new Arrow(player.level(), shooter, new ItemStack(Items.ARROW), null);
			Vec3 eye = shooter.getEyePosition();
			Vec3 from = eye.add(player.getBoundingBox().getCenter().subtract(eye).normalize().scale(0.8));
			arrow.setPos(from);
			Vec3 to = player.getBoundingBox().getCenter().subtract(from);
			arrow.shoot(to.x, to.y, to.z, 2.6F, 0);
			player.level().addFreshEntity(arrow);
			return arrow.getId();
		});
		// Followed a tick at a time: where it is, who owns it, and whether the shooter has been hit yet.
		StringBuilder flight = new StringBuilder();
		boolean struckBack = false;
		boolean hurt = false;
		for (int t = 0; t < 16 && !struckBack; t++) {
			context.waitTicks(1);
			String step = on(world, player -> {
				Entity shot = player.level().getEntity(arrowId);
				Mob shooter = tagged(player, "wildercord.aura_shooter");
				String where = shot == null ? "gone" : String.format(java.util.Locale.ROOT, "%.1f,%.1f,%.1f v%.2f,%.2f,%.2f owner %s",
					shot.getX(), shot.getY(), shot.getZ(), shot.getDeltaMovement().x, shot.getDeltaMovement().y, shot.getDeltaMovement().z,
					shot instanceof Arrow a && a.getOwner() != null ? a.getOwner().getType().toShortString() : "none");
				return where + (player.getHealth() < player.getMaxHealth() ? " (player hurt)" : "")
					+ (shooter.getHealth() < shooter.getMaxHealth() ? " HIT" : "");
			});
			flight.append("\n      ").append(step);
			struckBack = step.endsWith("HIT");
			hurt |= step.contains("(player hurt)");
		}
		String arrow = hurt ? "a perfect guard should turn the arrow aside; its flight:" + flight
			: struckBack ? null : "the arrow should fly back into its shooter; its flight:" + flight;
		check(arrow == null, arrow);
		context.getInput().releaseKey(o -> o.keyShift);
		context.waitTicks(AuraRules.GUARD_REST + 5);

		// A spell at a perfect guard is parried: the husk's Harm bolt negated, and answered.
		raiseGuard(context);
		on(world, player -> {
			player.setHealth(player.getMaxHealth());
			Mob caster = tagged(player, "wildercord.aura_attacker");
			caster.setHealth(caster.getMaxHealth());
			SpellPlan.Group group = SpellCompiler.compile(List.of(Runes.BOLT, Runes.HARM)).root().groups.getFirst();
			Vec3 chest = player.getBoundingBox().getCenter();
			Vec3 from = caster.getEyePosition().add(chest.subtract(caster.getEyePosition()).normalize().scale(0.5));
			RuneBolt.launch(new Cast(caster), group, null, from, chest.subtract(from), false);
			return null;
		});
		context.waitTicks(10);
		String parried = on(world, player -> {
			Mob caster = tagged(player, "wildercord.aura_attacker");
			if (player.getHealth() < player.getMaxHealth()) {
				return "a perfect guard should parry the spell (health " + player.getHealth() + ")";
			}
			if (caster.getHealth() >= caster.getMaxHealth()) {
				return "the parried spell should be answered at its caster";
			}
			return Heart.grimoire(player).contains("feat:" + dev.wildercord.spell.Feats.PARRY) ? null : "a parried spell is a Parry, for the Grimoire";
		});
		check(parried == null, parried);
		context.getInput().releaseKey(o -> o.keyShift);
	}

	/** Sneak held and the Aura key pressed (the real key, as a player would). */
	private static void raiseGuard(ClientGameTestContext context) {
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(2);
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(1);
	}

	/** The attacking husk's blow on the player; returns what it took. */
	private static float huskStrikes(ServerPlayer player) {
		Mob husk = tagged(player, "wildercord.aura_attacker");
		float before = player.getHealth();
		husk.doHurtTarget(player.level(), player);
		player.removeEffect(MobEffects.HUNGER);
		return before - player.getHealth();
	}

	/** A full axe swing at a husk, with another 1.8 blocks beside it: what each took. */
	private static double[] sweep(ClientGameTestContext context, TestSingleplayerContext world, float aura) {
		on(world, player -> {
			kill(player, TAG);
			stand(player);
			setAura(player, "stone", AuraRules.FLOW, aura, AuraRules.threshold(AuraRules.FLOW));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
			spawn(player.level(), EntityTypes.HUSK, at(0, 2.2), 200).addTag("wildercord.aura_target");
			spawn(player.level(), EntityTypes.HUSK, at(1.8, 2.4), 200).addTag("wildercord.aura_side");
			return null;
		});
		context.waitTicks(35);
		return on(world, player -> {
			Mob target = tagged(player, "wildercord.aura_target");
			Mob side = tagged(player, "wildercord.aura_side");
			player.attack(target);
			return new double[] {200 - target.getHealth(), 200 - side.getHealth()};
		});
	}

	// ------------------------------------------------------------------ Edge

	private static void edge(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			setAura(player, "starlit", AuraRules.EDGE, AuraRules.capacity(AuraRules.EDGE), AuraRules.threshold(AuraRules.EDGE));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			return null;
		});
		context.waitTicks(8);
		double reach = on(world, player -> player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE));
		on(world, player -> {
			setAura(player, "starlit", AuraRules.EDGE, 0, AuraRules.threshold(AuraRules.EDGE));
			return null;
		});
		context.waitTicks(8);
		double bare = on(world, player -> player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE));
		check(Math.abs(reach - bare - AuraRules.EDGE_REACH) < 1e-6, "the Edge should add a block of reach while the blade is lit (" + reach + " against " + bare + ")");

		double glowArmoured = blowOnFreshHusk(context, world, "ember", AuraRules.GLOW, 20, Items.IRON_SWORD, true);
		double edgeArmoured = blowOnFreshHusk(context, world, "ember", AuraRules.EDGE, 70, Items.IRON_SWORD, true);
		check(glowArmoured > 0 && edgeArmoured > 1.4 * glowArmoured,
			"the Edge should bite through diamond armour (" + edgeArmoured + " against " + glowArmoured + " at Glow)");

		// The slash, by the real key: a line of husks cut for the weapon's damage in its element (Verdant on the undead).
		on(world, player -> {
			kill(player, TAG);
			stand(player);
			setAura(player, "verdant", AuraRules.EDGE, AuraRules.capacity(AuraRules.EDGE), AuraRules.threshold(AuraRules.EDGE));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			for (int i = 0; i < 3; i++) {
				spawn(player.level(), EntityTypes.HUSK, at(0, 3 + 2.5 * i), 200).addTag("wildercord.aura_line");
			}
			return null;
		});
		// Filmed at night, where its light reads against the sky.
		world.getServer().runCommand("time set 18000");
		context.waitTicks(10);
		double weapon = on(world, player -> player.getAttributeValue(Attributes.ATTACK_DAMAGE));
		double life = on(world, player -> dev.wildercord.cast.PowerPlaces.of(player).factor("life"));
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(6.5);
			return null;
		});
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(2);
		// Filmed from high behind, looking down on the crescent's face as it flies into the line.
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			mc.player.setXRot(58);
			mc.player.xRotO = 58;
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(1);
		shot(context, "aura_slash");
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
			return null;
		});
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			mc.player.setXRot(8);
			mc.player.xRotO = 8;
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		world.getServer().runCommand("time set 3000");
		context.waitTicks(10);
		String slash = on(world, player -> {
			float aura = Aura.data(player).aura();
			if (Math.abs(aura - (AuraRules.capacity(AuraRules.EDGE) - AuraRules.SLASH_COST)) > 0.05) {
				return "the slash should cost " + AuraRules.SLASH_COST + " (aura " + aura + ")";
			}
			List<Mob> line = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16), m -> m.entityTags().contains("wildercord.aura_line"));
			double expected = weapon * AuraRules.SLASH_FACTOR * 1.5 * life;
			for (Mob husk : line) {
				double taken = 200 - husk.getHealth();
				if (taken < expected * 0.85 || taken > expected * 1.05) {
					return "each husk in the line should take the weapon's damage times " + AuraRules.SLASH_FACTOR + ", half again for life on the undead ("
						+ taken + ", expected about " + expected + ")";
				}
			}
			return line.size() == 3 ? null : "three husks should be in the line (" + line.size() + ")";
		});
		check(slash == null, slash);
		// Straight away again: the cooldown holds it back.
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(4);
		float held = on(world, player -> Aura.data(player).aura());
		check(Math.abs(held - (AuraRules.capacity(AuraRules.EDGE) - AuraRules.SLASH_COST)) < 0.05, "the slash's cooldown should hold the next one back (" + held + ")");
	}

	// ------------------------------------------------------------------ backlash

	private static void backlash(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			stand(player);
			setAura(player, "ember", AuraRules.EDGE, 3, AuraRules.threshold(AuraRules.EDGE));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			player.removeAllEffects();
			return null;
		});
		context.waitTicks(5);
		float health = on(world, player -> player.getHealth());
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(5);
		String spent = on(world, player -> {
			if (Aura.data(player).aura() > 1e-4) {
				return "a slash short of its price should spend everything there (" + Aura.data(player).aura() + ")";
			}
			if (!player.hasEffect(MobEffects.SLOWNESS) || !player.hasEffect(MobEffects.WEAKNESS)) {
				return "spending past empty should bring backlash: slowed and weakened";
			}
			return player.getHealth() < health ? "backlash should never hurt (" + player.getHealth() + " of " + health + ")" : null;
		});
		check(spent == null, spent);
		boolean seen = context.computeOnClient(mc -> Aura.state(mc.player).backlashUntil() > mc.level.getGameTime());
		check(seen, "the HUD should know backlash is on");
		on(world, player -> {
			player.removeAllEffects();
			AuraAttachments.State state = Aura.state(player);
			player.setAttached(AuraAttachments.STATE, state.slashReady(0).backlash(0));
			return null;
		});
		context.waitTicks(2);
		context.getInput().pressKey(WildercordKeys.auraMapping());
		context.waitTicks(5);
		String empty = on(world, player -> player.hasEffect(MobEffects.SLOWNESS) && player.getHealth() >= health ? null
			: "a technique failing at nothing should bring backlash and no harm");
		check(empty == null, empty);
		on(world, player -> {
			player.removeAllEffects();
			return null;
		});
	}

	// ------------------------------------------------------------------ against a player

	private static void pvp(ClientGameTestContext context, TestSingleplayerContext world) {
		String capped = on(world, player -> {
			double held = AuraCombat.againstPlayer(player, 10.0);
			double expected = 1 + (dev.wildercord.config.Config.get().defence().maxBonus() - 1) * dev.wildercord.config.Config.get().aura().pvpScale();
			return Math.abs(held - expected) < 1e-6 ? null : "aura's bonuses against a player should be held to the cap, then the PvP scale (" + held + ")";
		});
		check(capped == null, capped);
		double[] small = on(world, player -> {
			stand(player);
			player.setHealth(player.getMaxHealth());
			player.removeAttached(WildercordAttachments.SPELLGUARD);
			FakePlayer rival = rival(player, 1.0);
			double weapon = rival.getAttributeValue(Attributes.ATTACK_DAMAGE);
			float before = player.getHealth();
			AuraSlash.loose(rival);
			return new double[] {weapon, before};
		});
		context.waitTicks(6);
		double tookSmall = on(world, player -> small[1] - player.getHealth());
		double expected = small[0] * AuraRules.SLASH_FACTOR * dev.wildercord.config.Config.get().aura().pvpScale();
		check(Math.abs(tookSmall - expected) < 0.15, "a player's slash should land at the PvP scale (" + tookSmall + ", expected " + expected + ")");
		on(world, player -> {
			player.setHealth(player.getMaxHealth());
			dev.wildercord.cast.Effects.readyToHurt(player);
			FakePlayer rival = rival(player, 500.0);
			AuraSlash.loose(rival);
			return null;
		});
		context.waitTicks(6);
		String guard = on(world, player -> {
			if (!player.isAlive() || Math.abs(player.getHealth() - 2.0F) > 0.01) {
				return "the spellguard should hold a killing slash on one heart (health " + player.getHealth() + ", alive " + player.isAlive() + ")";
			}
			return player.getAttached(WildercordAttachments.SPELLGUARD) == null ? "the spellguard should note that it held" : null;
		});
		check(guard == null, guard);
		on(world, player -> {
			player.setHealth(player.getMaxHealth());
			return null;
		});
	}

	/** A rival player (simulated) three blocks in front, facing the player, at Edge with a lit Verdant blade. */
	private static FakePlayer rival(ServerPlayer player, double attack) {
		FakePlayer rival = FakePlayer.get(player.level());
		rival.snapTo(player.getX(), player.getY(), player.getZ() + 3, 180, 0);
		rival.setYHeadRot(180);
		rival.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("verdant", AuraRules.EDGE, AuraRules.threshold(AuraRules.EDGE),
			AuraRules.capacity(AuraRules.EDGE), 0));
		rival.removeAttached(AuraAttachments.STATE);
		rival.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
		AttributeInstance damage = rival.getAttribute(Attributes.ATTACK_DAMAGE);
		damage.setBaseValue(attack);
		return rival;
	}

	// ------------------------------------------------------------------ breakthroughs

	private static void breakthroughs(ClientGameTestContext context, TestSingleplayerContext world) {
		// A stronger foe, felled by the blade: first one a spell touched (it doesn't count), then a clean one.
		on(world, player -> {
			kill(player, TAG);
			stand(player);
			// A Stone blade: it sets nothing alight (an Ember blade's fire, or its burning mark, would finish a foe left on one heart
			// before the killing blow).
			setAura(player, "stone", AuraRules.FLOW, AuraRules.capacity(AuraRules.FLOW), AuraRules.threshold(AuraRules.EDGE));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD));
			spawn(player.level(), EntityTypes.HUSK, at(-1, 2.2), 60).addTag("wildercord.aura_tainted");
			spawn(player.level(), EntityTypes.HUSK, at(1, 2.2), 60).addTag("wildercord.aura_worthy");
			return null;
		});
		check(on(world, player -> AuraBreakthroughs.ready(player)), "the threshold reached, a breakthrough should wait");
		context.waitTicks(30);
		String opened = on(world, player -> {
			Mob foe = tagged(player, "wildercord.aura_tainted");
			player.attack(foe);
			if (Aura.state(player).trialUntil() <= player.level().getGameTime()) {
				return "a blow on a foe of three times your health should open the trial (until " + Aura.state(player).trialUntil() + ")";
			}
			// A spell of the player's on it (spells land through a foe's moment of invulnerability, as Effects.hurt makes them).
			dev.wildercord.cast.Effects.readyToHurt(foe);
			boolean hurt = foe.hurtServer(player.level(), player.level().damageSources().indirectMagic(player, player), 1);
			foe.setHealth(1);
			return AuraBreakthroughs.spoiled(player, foe) ? null : "a spell of the player's on the trial's foe should spoil it (the spell hurt: " + hurt
				+ "; the after-damage event last told of " + lastDamaged + ", the foe is " + foe.getUUID() + ")";
		});
		check(opened == null, opened);
		context.waitTicks(30);
		int tainted = on(world, player -> {
			player.attack(tagged(player, "wildercord.aura_tainted"));
			return Aura.stage(player);
		});
		check(tainted == AuraRules.FLOW, "a foe a spell touched shouldn't count for the trial (stage " + tainted + ")");
		context.waitTicks(30);
		on(world, player -> {
			Mob foe = tagged(player, "wildercord.aura_worthy");
			player.attack(foe);
			foe.setHealth(1);
			return null;
		});
		context.waitTicks(30);
		String worthy = on(world, player -> {
			player.attack(tagged(player, "wildercord.aura_worthy"));
			AuraAttachments.Data data = Aura.data(player);
			if (data.stage() != AuraRules.EDGE) {
				return "felling a foe of three times your health by the blade alone should break through to Edge (" + data + ")";
			}
			if (Math.abs(data.aura() - AuraRules.capacity(AuraRules.EDGE)) > 0.5) {
				return "a breakthrough should fill the new stage's aura (" + data.aura() + ")";
			}
			return Heart.grimoire(player).contains("aura:edge") ? null : "the breakthrough should go into the Grimoire";
		});
		check(worthy == null, worthy);

		// Stillness at a ley crossing.
		double[] heart = on(world, player -> strongCrossing(LeyWalker.seed(player.level())));
		check(heart != null, "no ley crossing within reach of spawn");
		int hx = (int) Math.floor(heart[0]);
		int hz = (int) Math.floor(heart[1]);
		int y = STAGE.getY();
		world.getServer().runCommand("forceload add " + (hx - 2) + " " + (hz - 2) + " " + (hx + 2) + " " + (hz + 2));
		world.getServer().runCommand("fill " + (hx - 3) + " " + (y - 1) + " " + (hz - 3) + " " + (hx + 3) + " " + (y - 1) + " " + (hz + 3) + " minecraft:stone");
		world.getServer().runCommand("fill " + (hx - 3) + " " + y + " " + (hz - 3) + " " + (hx + 3) + " " + (y + 4) + " " + (hz + 3) + " minecraft:air");
		on(world, player -> {
			player.teleportTo(player.level(), heart[0], y, heart[1], Set.<Relative>of(), 0.0F, 15.0F, false);
			player.setDeltaMovement(Vec3.ZERO);
			setAura(player, "ember", AuraRules.GLOW, 4, AuraRules.threshold(AuraRules.FLOW));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			return null;
		});
		context.waitTicks(20);
		boolean place = on(world, player -> dev.wildercord.cast.PowerPlaces.isPlaceOfPower(player.level(), player.blockPosition()));
		check(place, "the crossing's heart should be a place of power");
		shot(context, "aura_hud_ready");
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(AuraRules.SETTLE_TICKS + AuraRules.STILLNESS_TICKS - 40);
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
		// The moment itself is filmed at night, where its colour reads.
		world.getServer().runCommand("time set 18000");
		int stage = AuraRules.GLOW;
		for (int i = 0; i < 120 && stage == AuraRules.GLOW; i++) {
			context.waitTicks(1);
			stage = on(world, player -> Aura.stage(player));
		}
		context.waitTicks(4);
		shot(context, "aura_breakthrough");
		world.getServer().runCommand("time set 3000");
		context.getInput().releaseKey(o -> o.keyShift);
		context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
		check(stage == AuraRules.FLOW, "half a minute of stillness at a ley crossing should break through to Flow (stage " + stage + ")");
		boolean noted = on(world, player -> Heart.grimoire(player).contains("aura:flow"));
		check(noted, "the breakthrough should go into the Grimoire");
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

	// ------------------------------------------------------------------ the blade's look, the bar and the page

	private static void visuals(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			kill(player, TAG);
			stand(player);
			return null;
		});
		blade(context, world, "aura_blade_glow_ember", "ember", AuraRules.GLOW, Items.IRON_SWORD, true);
		blade(context, world, "aura_blade_flow_rime", "rime", AuraRules.FLOW, Items.DIAMOND_SWORD, true);
		blade(context, world, "aura_blade_edge_hollow", "hollow", AuraRules.EDGE, Items.NETHERITE_SWORD, true);
		blade(context, world, "aura_blade_edge_starlit_axe", "starlit", AuraRules.EDGE, Items.IRON_AXE, false);
		blade(context, world, "aura_blade_edge_thunder_spear", "thunder", AuraRules.EDGE, Items.IRON_SPEAR, false);
		blade(context, world, "aura_blade_edge_crimson_trident", "crimson", AuraRules.EDGE, Items.TRIDENT, false);
		blade(context, world, "aura_blade_edge_verdant_mace", "verdant", AuraRules.EDGE, Items.MACE, false);
		world.getServer().runCommand("time set 18000");
		blade(context, world, "aura_blade_night_ember", "ember", AuraRules.EDGE, Items.DIAMOND_SWORD, false);
		world.getServer().runCommand("time set 3000");

		// The bar: on top of the spell panel with a Cord, alone without.
		on(world, player -> {
			setAura(player, "ember", AuraRules.EDGE, 47, 900);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			return null;
		});
		context.waitTicks(5);
		shot(context, "aura_hud_cord");
		ItemStack cord = on(world, player -> {
			ItemStack worn = Spellbooks.cord(player).copy();
			Spellbooks.setCord(player, ItemStack.EMPTY);
			return worn;
		});
		context.waitTicks(5);
		shot(context, "aura_hud_alone");

		// The page: from the Cord screen's badge (here, without a Cord: aura needs none), then with a breakthrough waiting.
		context.setScreen(CordScreen::new);
		context.waitTicks(3);
		double[] badge = context.computeOnClient(mc -> ((CordScreen) mc.gui.screen()).auraBadgePoint());
		double guiScale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(badge[0] * guiScale, badge[1] * guiScale);
		context.waitTicks(2);
		shot(context, "aura_cord_badge");
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(3);
		String page = context.computeOnClient(mc -> mc.gui.screen() instanceof AuraScreen ? null
			: "the Cord screen's Aura badge should open the Aura page (clicked " + badge[0] + ", " + badge[1] + "; the screen is " + mc.gui.screen() + ")");
		check(page == null, page);
		shot(context, "aura_page");
		context.setScreen(() -> null);
		on(world, player -> {
			Spellbooks.setCord(player, cord);
			setAura(player, "rime", AuraRules.FLOW, 31, AuraRules.threshold(AuraRules.EDGE));
			return null;
		});
		context.waitTicks(3);
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		shot(context, "aura_page_ready");
		context.setScreen(() -> null);
		on(world, player -> {
			player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE);
			return null;
		});
		context.waitTicks(3);
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		shot(context, "aura_page_none");
		context.setScreen(() -> null);
	}

	/** The blade at a stage in a method's colour: third person from the front, and (when asked) first person. */
	private static void blade(ClientGameTestContext context, TestSingleplayerContext world, String name, String method, int stage, Item weapon, boolean firstPerson) {
		on(world, player -> {
			stand(player);
			setAura(player, method, stage, AuraRules.capacity(stage), AuraRules.threshold(stage));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(weapon));
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, 25.0F, false);
			// Close in, so the blade fills the shot.
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(2.3);
			return null;
		});
		context.waitTicks(8);
		if (firstPerson) {
			// The HUD stays up: hiding it hides the hand too (and the aura bar belongs in the shot).
			context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
			context.waitTicks(3);
			shot(context, name + "_fp");
		}
		on(world, player -> {
			// Looking up a little: the camera in front looks down on the blade held out before the body.
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, -22.0F, false);
			return null;
		});
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(3);
		// The body turned aside under the head (as it stays when standing still), so the blade shows its face to the camera.
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
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
			return null;
		});
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
		world.getServer().runCommand("kill @e[type=arrow]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runCommand("kill @e[type=wildercord:rune_bolt]");
		context.waitTicks(10);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			player.removeAttached(AuraAttachments.STATE);
		});
		context.waitTicks(5);
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

	/** Clicks a point given in GUI coordinates. */
	private static void click(ClientGameTestContext context, double[] gui) {
		if (gui == null) {
			throw new AssertionError("nothing to click there");
		}
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(gui[0] * scale, gui[1] * scale);
		context.waitTicks(1);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(3);
	}

	private static <T> T on(TestSingleplayerContext world, Function<ServerPlayer, T> step) {
		return world.getServer().computeOnServer(server -> step.apply(player(server)));
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}
}
