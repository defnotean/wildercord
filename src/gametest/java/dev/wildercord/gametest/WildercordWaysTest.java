package dev.wildercord.gametest;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraArmour;
import dev.wildercord.aura.AuraBreakthroughs;
import dev.wildercord.aura.AuraDominion;
import dev.wildercord.aura.AuraExperience;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.AuraIntent;
import dev.wildercord.aura.AuraPresence;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.AuraSlash;
import dev.wildercord.aura.AuraStep;
import dev.wildercord.aura.Awakening;
import dev.wildercord.aura.AwakeningRules;
import dev.wildercord.aura.Crescents;
import dev.wildercord.aura.Crossroads;
import dev.wildercord.aura.CrossroadsIncense;
import dev.wildercord.aura.Momentum;
import dev.wildercord.aura.MomentumRules;
import dev.wildercord.aura.Stance;
import dev.wildercord.aura.StanceRules;
import dev.wildercord.aura.StringRules;
import dev.wildercord.aura.SwordStrings;
import dev.wildercord.aura.WayBanner;
import dev.wildercord.aura.WayEffects;
import dev.wildercord.aura.WayRules;
import dev.wildercord.aura.Ways;
import dev.wildercord.world.LeyLines;
import dev.wildercord.cast.LeyWalker;
import dev.wildercord.client.AuraScreen;
import dev.wildercord.client.WayHud;
import dev.wildercord.client.WildercordKeys;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Spellbooks;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Ways, step 7 of the aura overhaul, played with the real keys on a stone platform in the sky:
 * <ul>
 *   <li><b>The crossroads</b>: the Edge breakthrough raises four standards in front of the swordsman (filmed by day and by night,
 *       from behind and through their own eyes, a standard's name under the crosshair); a swing at one leans toward it, a swing at
 *       another moves the lean, a second swing walks it, and the moment plays;</li>
 *   <li><b>Called</b>: a Sovereign who had no Way calls the crossroads by holding the breathing stance, chooses, and has all three
 *       nodes at once (a first choice owes nothing);</li>
 *   <li>each Way's choosing moment, filmed;</li>
 *   <li><b>The Blade</b>, <b>the Bulwark</b>, <b>the Shadowstep</b> and <b>the Banner</b>: every node doing what it says, on husks, a
 *       rival player and an ally (teams kept: never an enemy helped);</li>
 *   <li><b>The incense</b>: refused away from a place of power, unbinding at a ley crossing, the crossroads rising again, the new Way's
 *       later nodes waking as experience is earned;</li>
 *   <li><b>A death</b> keeps the Way and closes a standing crossroads;</li>
 *   <li>the Aura page's Way tab at each state.</li>
 * </ul>
 * Screenshots: {@code ways_*}.
 *
 * <p>{@code WILDERCORD_TOUR_ONLY}, {@code WILDERCORD_CORDS_ONLY} and {@code WILDERCORD_SHOWCASE} skip it; {@code WILDERCORD_WAYS=a,b}
 * plays only the scenes named (crossroads, called, moments, blade, bulwark, shadowstep, banner, incense, death, pages).</p>
 */
public class WildercordWaysTest implements FabricClientGameTest {
	private static final BlockPos STAGE = new BlockPos(0, 190, 0);
	private static final String TAG = "wildercord.ways_test";
	private static final List<String> CHOSEN = Collections.synchronizedList(new ArrayList<>());
	private static final List<String> UNBOUND = Collections.synchronizedList(new ArrayList<>());
	private static final List<Double> EXTRAS = Collections.synchronizedList(new ArrayList<>());
	private static boolean hooked;
	/** A sword's full swing comes back in 11 ticks. */
	private static final int FULL = 13;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (System.getenv("WILDERCORD_TOUR_ONLY") != null || System.getenv("WILDERCORD_CORDS_ONLY") != null || System.getenv("WILDERCORD_SHOWCASE") != null) {
			return;
		}
		if (!hooked) {
			hooked = true;
			AuraApi.onWay(new AuraApi.WayHook() {
				@Override
				public void chosen(ServerPlayer player, AuraApi.Way way, boolean first) {
					CHOSEN.add(way.id() + ":" + first);
				}

				@Override
				public void unbound(ServerPlayer player, AuraApi.Way way) {
					UNBOUND.add(way.id());
				}
			});
			// Registered after the mod's own: it hears what a finisher adds once every Way has had its say.
			AuraApi.onFinisher(new AuraApi.FinisherHook() {
				@Override
				public double extra(ServerPlayer attacker, LivingEntity target, double extra) {
					EXTRAS.add(extra);
					return extra;
				}
			});
		}
		String only = System.getenv("WILDERCORD_WAYS");
		Set<String> scenes = only == null || only.isBlank() ? null : Set.of(only.split(","));
		context.runOnClient(mc -> {
			mc.getWindow().setWindowed(1920, 1080);
			mc.options.guiScale().set(2);
			mc.resizeGui();
			mc.options.toggleCrouch().set(false);
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			MagicQuality.stringIndicator = MagicQuality.StringIndicator.CROSSHAIR;
		});
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
			scene(scenes, "crossroads", failures, "the crossroads at the Edge breakthrough", () -> crossroads(context, world));
			scene(scenes, "called", failures, "the crossroads called by the stance", () -> called(context, world));
			scene(scenes, "moments", failures, "each Way's choosing moment", () -> moments(context, world));
			scene(scenes, "blade", failures, "the Way of the Blade", () -> blade(context, world));
			scene(scenes, "bulwark", failures, "the Way of the Bulwark", () -> bulwark(context, world));
			scene(scenes, "shadowstep", failures, "the Way of the Shadowstep", () -> shadowstep(context, world));
			scene(scenes, "banner", failures, "the Way of the Banner", () -> banner(context, world));
			scene(scenes, "incense", failures, "changing Way with the incense", () -> incense(context, world));
			scene(scenes, "death", failures, "a death", () -> death(context, world));
			scene(scenes, "pages", failures, "the Aura page's Way tab", () -> pages(context, world));
			if (!failures.isEmpty()) {
				throw new AssertionError("Ways went wrong:\n  " + String.join("\n  ", failures));
			}
		} finally {
			context.runOnClient(mc -> {
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				if (mc.gui.hud.isHidden()) {
					mc.gui.hud.toggle();
				}
			});
			context.getInput().releaseKey(o -> o.keyShift);
			context.getInput().releaseKey(o -> o.keyUse);
			context.getInput().releaseKey(WildercordKeys.auraMapping());
			AuraScreen.listArts(false);
		}
	}

	private static void scene(Set<String> scenes, String id, List<String> failures, String what, Runnable test) {
		if (scenes == null || scenes.contains(id)) {
			try {
				test.run();
			} catch (AssertionError e) {
				failures.add(what + ": " + e.getMessage());
			}
		}
	}

	private static void check(boolean ok, String what) {
		if (!ok) {
			throw new AssertionError(what);
		}
	}

	// ------------------------------------------------------------------ the crossroads at the Edge breakthrough

	private static void crossroads(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			// Flow, its breakthrough waiting, no Way.
			player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.FLOW, AuraRules.threshold(AuraRules.EDGE),
				AuraRules.capacity(AuraRules.FLOW), 0));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
			Ways.set(player, "");
			return null;
		});
		context.waitTicks(10);
		check(!on(world, Crossroads::standing), "no crossroads before the breakthrough");
		on(world, player -> {
			AuraBreakthroughs.breakThrough(player);
			return null;
		});
		context.waitTicks(10);
		check(on(world, Aura::stage) == AuraRules.EDGE, "the breakthrough should make Edge");
		check(!on(world, Crossroads::standing), "the crossroads waits for the breakthrough's moment to settle");
		context.waitTicks(WayRules.OPEN_DELAY);
		String rose = on(world, player -> {
			Crossroads.State s = Crossroads.state(player);
			if (s == null) {
				return "the crossroads should rise after the Edge breakthrough";
			}
			if (s.standards().size() != WayRules.BUILT_IN.size()) {
				return "a standard for each Way (" + s.standards().size() + ")";
			}
			for (int i = 0; i < WayRules.BUILT_IN.size(); i++) {
				if (!s.standards().get(i).way().equals(WayRules.BUILT_IN.get(i))) {
					return "the standards stand in the Ways' order, left to right (" + s.standards() + ")";
				}
				double d = s.standards().get(i).foot().distanceTo(player.position());
				if (Math.abs(d - WayRules.RADII[0]) > 0.3) {
					return "a standard stands " + WayRules.RADII[0] + " out on open ground (" + d + ")";
				}
				Vec3 to = s.standards().get(i).foot().subtract(player.position());
				if (to.z < 1.0) {
					return "every standard stands in front (" + to + ")";
				}
			}
			return dev.wildercord.player.Heart.grimoire(player).contains("aura:crossroads") ? null : "the crossroads goes into the Grimoire";
		});
		check(rose == null, rose);
		check(context.computeOnClient(mc -> Crossroads.state(mc.player) != null), "the swordsman's own client should know where the standards stand");
		// Filmed: from behind and above (by day), then through the swordsman's own eyes, then by night.
		thirdPerson(context, world, 0, 24, 6.0, false);
		context.waitTicks(16);
		shot(context, "ways_crossroads_tp");
		firstPerson(context, world);
		context.waitTicks(8);
		shot(context, "ways_crossroads_fp");
		world.getServer().runCommand("time set 18000");
		thirdPerson(context, world, 0, 24, 6.0, false);
		context.waitTicks(10);
		shot(context, "ways_crossroads_night_tp");
		world.getServer().runCommand("time set 3000");
		firstPerson(context, world);
		// Aimed at the Bulwark's standard: its name under the crosshair.
		aim(context, world, WayRules.BULWARK);
		context.waitTicks(4);
		check(context.computeOnClient(mc -> WayHud.lastNamed()).equals(WayRules.BULWARK), "the Bulwark's name should show under the crosshair ("
			+ context.computeOnClient(mc -> WayHud.lastNamed()) + ")");
		shot(context, "ways_crossroads_aim_fp");
		// A swing leans toward it.
		swing(context);
		context.waitTicks(4);
		String leaned = on(world, player -> {
			Crossroads.State s = Crossroads.state(player);
			return s != null && s.leaningAt(player.level().getGameTime()).equals(WayRules.BULWARK) ? null : "a swing at the Bulwark's standard should lean toward it";
		});
		check(leaned == null, leaned);
		check(on(world, p -> Ways.state(p).way().isEmpty()), "a lean walks nothing yet");
		shot(context, "ways_crossroads_lean_fp");
		thirdPerson(context, world, 0, 24, 6.0, false);
		context.waitTicks(6);
		shot(context, "ways_crossroads_lean_tp");
		firstPerson(context, world);
		context.waitTicks(FULL);
		// A swing at another moves the lean.
		aim(context, world, WayRules.BLADE);
		swing(context);
		context.waitTicks(4);
		String moved = on(world, player -> {
			Crossroads.State s = Crossroads.state(player);
			return s != null && s.leaningAt(player.level().getGameTime()).equals(WayRules.BLADE) ? null : "a swing at another standard moves the lean";
		});
		check(moved == null, moved);
		context.waitTicks(FULL);
		// The second swing at it walks it.
		CHOSEN.clear();
		thirdPerson(context, world, 0, 18, 5.0, false);
		aim(context, world, WayRules.BLADE);
		swing(context);
		context.waitTicks(4);
		shot(context, "ways_chosen_tp");
		context.waitTicks(10);
		shot(context, "ways_chosen_after_tp");
		String walked = on(world, player -> {
			Ways.State s = Ways.state(player);
			if (!s.way().equals(WayRules.BLADE)) {
				return "a second swing at the leaned standard should walk its Way (" + s.way() + ")";
			}
			if (s.owed() > 0) {
				return "a first choice owes nothing (" + s.owed() + ")";
			}
			if (Crossroads.standing(player)) {
				return "the crossroads closes once a Way is walked";
			}
			if (!Ways.has(player, WayRules.BLADE_EDGE) || Ways.has(player, WayRules.BLADE_FORM)) {
				return "at Edge the Blade's Edge node is in force, its Form node not yet";
			}
			List<String> grimoire = dev.wildercord.player.Heart.grimoire(player);
			return grimoire.contains("aura:way") && grimoire.contains("aura:way_blade") ? null : "the choice goes into the Grimoire";
		});
		check(walked == null, walked);
		check(CHOSEN.contains("blade:true"), "the Way hooks hear of a first choice (" + CHOSEN + ")");
		check(context.computeOnClient(mc -> Ways.way(mc.player).map(AuraApi.Way::id).orElse("")).equals(WayRules.BLADE),
			"the swordsman's own client should know their Way");
		check(context.computeOnClient(mc -> Ways.has(mc.player, WayRules.BLADE_EDGE)), "and read its node on the client");
		firstPerson(context, world);
	}

	// ------------------------------------------------------------------ called by the stance

	private static void called(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "thunder", AuraRules.SOVEREIGN);
			Ways.set(player, "");
			return null;
		});
		context.waitTicks(10);
		check(on(world, Ways::wayless), "a Sovereign with no Way is past the crossroads with none");
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(AuraRules.SETTLE_TICKS + WayRules.CALL_TICKS / 2);
		check(!on(world, Crossroads::standing), "half the call: not yet");
		context.waitTicks(WayRules.CALL_TICKS / 2 + 10);
		boolean rose = on(world, Crossroads::standing);
		context.getInput().releaseKey(o -> o.keyShift);
		check(rose, "a few seconds of the breathing stance should call the crossroads");
		context.waitTicks(10);
		chooseByStrikes(context, world, WayRules.SHADOWSTEP);
		String all = on(world, player -> {
			if (!Ways.state(player).way().equals(WayRules.SHADOWSTEP)) {
				return "it should walk the Shadowstep (" + Ways.state(player).way() + ")";
			}
			for (int stage : WayRules.NODE_STAGES) {
				if (!Ways.has(player, WayRules.node(WayRules.SHADOWSTEP, stage))) {
					return "a Sovereign's first choice gives every node at once (" + stage + ")";
				}
			}
			return null;
		});
		check(all == null, all);
	}

	/** Strikes {@code way}'s standard twice with the real attack key (lean, then walk). */
	private static void chooseByStrikes(ClientGameTestContext context, TestSingleplayerContext world, String way) {
		aim(context, world, way);
		swing(context);
		context.waitTicks(FULL);
		aim(context, world, way);
		swing(context);
		context.waitTicks(4);
	}

	// ------------------------------------------------------------------ each Way's moment

	private static void moments(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("time set 18000");
		for (String way : WayRules.BUILT_IN) {
			reset(context, world);
			on(world, player -> {
				setAura(player, "starlit", AuraRules.FORM);
				Ways.set(player, "");
				check(AuraApi.openCrossroads(player), "the crossroads should rise for " + way);
				return null;
			});
			context.waitTicks(16);
			thirdPerson(context, world, 0, 16, 5.5, false);
			aim(context, world, way);
			swing(context);
			context.waitTicks(FULL);
			aim(context, world, way);
			// The camera turned square to the swordsman, the standard beside them, as the second swing lands.
			swing(context);
			context.waitTicks(5);
			shot(context, "ways_moment_" + way + "_tp");
			check(on(world, p -> Ways.state(p).way()).equals(way), way + " should be walked");
			context.waitTicks(40);
		}
		world.getServer().runCommand("time set 3000");
		firstPerson(context, world);
	}

	// ------------------------------------------------------------------ the Blade

	private static void blade(ClientGameTestContext context, TestSingleplayerContext world) {
		// Keen Edge: clean hits build momentum a third faster.
		reset(context, world);
		on(world, player -> {
			setAura(player, "stone", AuraRules.EDGE);
			Ways.set(player, "");
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 2.4), 200, 180);
			steady(husk);
			return null;
		});
		context.waitTicks(FULL);
		double plain = hitMomentum(world);
		on(world, player -> {
			Ways.set(player, WayRules.BLADE);
			return null;
		});
		context.waitTicks(FULL);
		double keen = hitMomentum(world);
		check(Math.abs(keen / Math.max(1.0E-3, plain) - WayRules.BLADE_HIT_MOMENTUM) < 0.05, "a clean hit should build a third more momentum ("
			+ plain + " then " + keen + ")");

		// The piercing slash: through a rival's held guard to the husk behind, and through more foes.
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.EDGE);
			Ways.set(player, WayRules.BLADE);
			Rival rival = rival(player);
			Vec3 p = at(0, 3.0);
			rival.snapTo(p.x, p.y, p.z, 180, 0);
			rival.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("rime", AuraRules.FLOW, 0, AuraRules.capacity(AuraRules.FLOW), 0));
			long now = player.level().getGameTime();
			rival.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE.guard(now - 40, now + 400));
			rival.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			steady(spawn(player.level(), EntityTypes.HUSK, at(0, 6.0), 60, 180));
			return null;
		});
		context.waitTicks(10);
		thirdPerson(context, world, -50, 14, 6.5, false);
		float[] behind = on(world, player -> {
			Mob husk = foes(player).getFirst();
			float hp = husk.getHealth();
			check(AuraSlash.loose(player), "the slash should go");
			return new float[] {hp};
		});
		context.waitTicks(3);
		shot(context, "ways_blade_pierce_tp");
		context.waitTicks(8);
		String pierced = on(world, player -> foes(player).getFirst().getHealth() < behind[0] ? null
			: "a Blade's slash should cut through the rival's held guard to the husk behind it");
		check(pierced == null, pierced);
		// Without the Way, the guard stops it there.
		on(world, player -> {
			Ways.set(player, "");
			player.removeAttached(AuraAttachments.STATE);
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(Aura.capacity(player)));
			Mob husk = foes(player).getFirst();
			husk.setHealth(husk.getMaxHealth());
			check(AuraSlash.loose(player), "the slash should go again");
			return null;
		});
		context.waitTicks(11);
		String stopped = on(world, player -> foes(player).getFirst().getHealth() >= foes(player).getFirst().getMaxHealth() ? null
			: "without the Way a held guard stops the slash");
		check(stopped == null, stopped);
		dropRival(world);

		// Through more foes: eight in a line, six without the Way, all eight with it.
		int[] cuts = new int[2];
		for (int k = 0; k < 2; k++) {
			boolean way = k == 1;
			reset(context, world);
			on(world, player -> {
				setAura(player, "ember", AuraRules.EDGE);
				Ways.set(player, way ? WayRules.BLADE : "");
				for (int i = 0; i < 8; i++) {
					steady(spawn(player.level(), EntityTypes.HUSK, at(0, 2.0 + i * 1.3), 200, 180));
				}
				return null;
			});
			context.waitTicks(5);
			on(world, player -> {
				check(AuraSlash.loose(player), "the slash should go");
				return null;
			});
			context.waitTicks(14);
			cuts[k] = on(world, player -> (int) foes(player).stream().filter(m -> m.getHealth() < m.getMaxHealth()).count());
		}
		check(cuts[0] == AuraRules.SLASH_TARGETS, "without the Way the slash cuts six (" + cuts[0] + ")");
		check(cuts[1] == 8, "a Blade's slash cuts all eight (" + cuts[1] + ")");

		// A clash: a piercing crescent cuts the other apart and flies on, weaker.
		reset(context, world);
		String clash = on(world, player -> {
			Rival rival = rival(player);
			Vec3 p = at(0, 12.0);
			rival.snapTo(p.x, p.y, p.z, 180, 0);
			Crescents.Flight mine = Crescents.launch(player, at(0, 0).add(0, 1.2, 0), new Vec3(0, 0, 1), 0xFF6A58, 6.0, 1.0, AuraRules.SLASH_SPEED, 14, 3, 6,
				false, e -> false, (f, t) -> 0).pierce();
			Crescents.Flight theirs = Crescents.launch(rival, at(0, 12).add(0, 1.2, 0), new Vec3(0, 0, -1), 0x8CDCFF, 6.0, 1.0, AuraRules.SLASH_SPEED, 14, 3,
				6, false, e -> false, (f, t) -> 0);
			CLASH.clear();
			CLASH.add(mine);
			CLASH.add(theirs);
			return null;
		});
		check(clash == null, clash);
		context.waitTicks(6);
		String won = on(world, player -> {
			Crescents.Flight mine = CLASH.get(0);
			Crescents.Flight theirs = CLASH.get(1);
			if (!theirs.done()) {
				return "the other crescent should break on the piercing one";
			}
			if (mine.done()) {
				return "the piercing crescent should fly on";
			}
			return Math.abs(mine.damage() - 6.0 * WayRules.BLADE_CLASH_CARRY) < 1.0E-6 ? null : "flying on at half its harm (" + mine.damage() + ")";
		});
		check(won == null, won);
		dropRival(world);

		// Cascade: a finisher opens the next creature worn half through.
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.FORM);
			Ways.set(player, WayRules.BLADE);
			Mob first = spawn(player.level(), EntityTypes.HUSK, at(0, 2.4), 80, 180);
			first.setHealth(60);
			Mob second = spawn(player.level(), EntityTypes.HUSK, at(1.8, 3.6), 80, 180);
			Stance.wear(player, first, StanceRules.pool(StanceRules.Kind.CREATURE, 80, 0), StanceRules.Source.BLOW);
			Stance.wear(player, second, StanceRules.pool(StanceRules.Kind.CREATURE, 80, 0) * 0.6, StanceRules.Source.BLOW);
			return null;
		});
		thirdPerson(context, world, -30, 20, 6.0, false);
		context.waitTicks(FULL);
		int cascades = WayEffects.counted("cascade");
		EXTRAS.clear();
		swing(context);
		context.waitTicks(3);
		shot(context, "ways_blade_cascade_tp");
		String cascaded = on(world, player -> {
			List<Mob> foes = foes(player);
			if (foes.size() < 2) {
				return "both husks should still stand";
			}
			return Stance.opened(foes.get(1)) && WayEffects.counted("cascade") == cascades + 1 ? null
				: "the finisher should cascade, opening the husk worn half through (" + Stance.left(foes.get(1)) + ")";
		});
		check(cascaded == null, cascaded);
		check(!EXTRAS.isEmpty(), "the finisher hook should hear what the finisher added");
		double withWay = EXTRAS.getLast();
		// The same finisher without the Way, for what Cascade adds.
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.FORM);
			Ways.set(player, "");
			Mob first = spawn(player.level(), EntityTypes.HUSK, at(0, 2.4), 80, 180);
			first.setHealth(60);
			Stance.wear(player, first, StanceRules.pool(StanceRules.Kind.CREATURE, 80, 0), StanceRules.Source.BLOW);
			return null;
		});
		firstPerson(context, world);
		context.waitTicks(FULL);
		EXTRAS.clear();
		swing(context);
		context.waitTicks(3);
		check(!EXTRAS.isEmpty(), "a plain finisher too");
		double without = EXTRAS.getLast();
		check(Math.abs(withWay / Math.max(1.0E-3, without) - WayRules.BLADE_FINISHER) < 0.02, "Cascade's finisher adds two fifths more ("
			+ without + " then " + withWay + ")");

		// Storm of Edges: awakened, the slash is free and quick, and a finisher feeds it twice as long.
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.SOVEREIGN);
			Ways.set(player, WayRules.BLADE);
			setMomentumNow(player, 60);
			check(AuraApi.awaken(player), "it should awaken (" + Awakening.refusal(player) + ")");
			return null;
		});
		context.waitTicks(AwakeningRules.RISE);
		String storm = on(world, player -> {
			float before = Aura.aura(player);
			long now = player.level().getGameTime();
			check(AuraSlash.loose(player), "the slash should go");
			if (Math.abs(Aura.aura(player) - before) > 1.0E-3) {
				return "awakened, a Blade's slash costs nothing (" + before + " then " + Aura.aura(player) + ")";
			}
			long rest = Aura.state(player).slashReadyAt() - now;
			return rest == AuraRules.SLASH_COOLDOWN / 2 ? null : "and is ready again in a second (" + rest + ")";
		});
		check(storm == null, storm);
		long[] fedBefore = on(world, player -> {
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 2.4), 80, 180);
			husk.setHealth(60);
			Stance.wear(player, husk, StanceRules.pool(StanceRules.Kind.CREATURE, 80, 0), StanceRules.Source.BLOW);
			return new long[] {Awakening.state(player).extended(), Stance.finishers()};
		});
		context.waitTicks(FULL);
		swing(context);
		context.waitTicks(4);
		String fed = on(world, player -> {
			if (Stance.finishers() != fedBefore[1] + 1) {
				return "the swing should have been a finisher";
			}
			long more = Awakening.state(player).extended() - fedBefore[0];
			return more == WayRules.BLADE_FEED ? null : "a finisher feeds a Blade's awakening two seconds (" + more + ")";
		});
		check(fed == null, fed);
		calm(context, world);
	}

	/** A full swing on the scene's husk from the server, from no momentum: what it built. */
	private static double hitMomentum(TestSingleplayerContext world) {
		return on(world, player -> {
			player.removeAttached(Momentum.MOMENTUM);
			Mob husk = foes(player).getFirst();
			husk.setHealth(husk.getMaxHealth());
			dev.wildercord.cast.Effects.readyToHurt(husk);
			player.attack(husk);
			return Momentum.value(player);
		});
	}

	private static final List<Crescents.Flight> CLASH = Collections.synchronizedList(new ArrayList<>());

	// ------------------------------------------------------------------ the Bulwark

	private static void bulwark(ClientGameTestContext context, TestSingleplayerContext world) {
		// Wide Guard: a held guard covers a blow from behind (and throws a third back), at Edge (no armour to muddy it).
		float[] lost = new float[2];
		for (int k = 0; k < 2; k++) {
			boolean way = k == 1;
			reset(context, world);
			on(world, player -> {
				setAura(player, "stone", AuraRules.EDGE);
				Ways.set(player, way ? WayRules.BULWARK : "");
				// Behind the swordsman (who faces +z), facing them.
				Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, -1.6), 200, 0);
				husk.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
				steady(husk);
				return null;
			});
			context.getInput().holdKey(o -> o.keyShift);
			context.waitTicks(4);
			int k0 = k;
			int reflects = WayEffects.counted("reflect");
			on(world, player -> {
				check(AuraGuard.raise(player), "the guard should go up");
				return null;
			});
			// Past the perfect moment, even the Bulwark's.
			context.waitTicks(WayRules.BULWARK_PERFECT + 3);
			float[] result = on(world, player -> {
				Mob husk = foes(player).getFirst();
				float hp = player.getHealth();
				float huskHp = husk.getHealth();
				dev.wildercord.cast.Effects.readyToHurt(player);
				husk.doHurtTarget(player.level(), player);
				float took = hp - player.getHealth();
				player.setHealth(player.getMaxHealth());
				return new float[] {took, huskHp - husk.getHealth()};
			});
			lost[k0] = result[0];
			if (way) {
				check(result[1] > 0 && WayEffects.counted("reflect") > reflects, "the Bulwark's held guard should throw a share of the blow back ("
					+ result[1] + ")");
				thirdPerson(context, world, 150, 18, 5.0, false);
				context.waitTicks(2);
				shot(context, "ways_bulwark_guard_tp");
				firstPerson(context, world);
			}
			context.getInput().releaseKey(o -> o.keyShift);
			context.waitTicks(4);
		}
		check(lost[1] > 0 && lost[1] < lost[0] * 0.6, "a Bulwark's held guard covers a blow from behind (" + lost[0] + " then " + lost[1] + ")");

		// Its perfect moment lasts half again as long: raised nine ticks before, still perfect.
		reset(context, world);
		on(world, player -> {
			setAura(player, "stone", AuraRules.EDGE);
			Ways.set(player, WayRules.BULWARK);
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 1.8), 200, 180);
			husk.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6);
			steady(husk);
			return null;
		});
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(4);
		on(world, player -> {
			check(AuraGuard.raise(player), "the guard should go up");
			return null;
		});
		context.waitTicks(AuraRules.PERFECT_TICKS + 2);
		String perfect = on(world, player -> {
			float hp = player.getHealth();
			dev.wildercord.cast.Effects.readyToHurt(player);
			foes(player).getFirst().doHurtTarget(player.level(), player);
			return player.getHealth() >= hp ? null : "nine ticks in, a Bulwark's guard is still perfect (took " + (hp - player.getHealth()) + ")";
		});
		context.getInput().releaseKey(o -> o.keyShift);
		check(perfect == null, perfect);

		// Its stance takes less wear; Living Wall's armour takes a third; a creature striking the guard staggers; Intent draws foes off a pet.
		reset(context, world);
		String firm = on(world, player -> {
			setAura(player, "stone", AuraRules.FORM);
			Ways.set(player, WayRules.BULWARK);
			Rival rival = rival(player);
			rival.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.FORM, 0, AuraRules.capacity(AuraRules.FORM), 0));
			rival.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
			player.removeAttached(Stance.STANCE);
			double worn = Stance.wear(rival, player, 5.0, StanceRules.Source.BLOW);
			if (Math.abs(worn - 5.0 * WayRules.BULWARK_STANCE) > 1.0E-6) {
				return "a Bulwark's stance takes two fifths less wear (" + worn + ")";
			}
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 2.0), 200, 180);
			DamageSource blow = player.level().damageSources().mobAttack(husk);
			float share = AuraArmour.share(player, blow, 10);
			if (Math.abs(share - 10 * WayRules.armourShare(AuraRules.ARMOUR_SHARE)) > 1.0E-3) {
				return "Living Wall's armour takes a third (" + share + ")";
			}
			return null;
		});
		check(firm == null, firm);
		dropRival(world);
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(4);
		on(world, player -> {
			check(AuraGuard.raise(player), "the guard should go up");
			return null;
		});
		context.waitTicks(WayRules.BULWARK_PERFECT + 3);
		String staggered = on(world, player -> {
			Mob husk = foes(player).getFirst();
			dev.wildercord.cast.Effects.readyToHurt(player);
			husk.doHurtTarget(player.level(), player);
			player.setHealth(player.getMaxHealth());
			return husk.hasEffect(MobEffects.SLOWNESS) ? null : "a creature striking a Living Wall's raised guard should stagger";
		});
		context.getInput().releaseKey(o -> o.keyShift);
		check(staggered == null, staggered);
		String challenged = on(world, player -> {
			Wolf wolf = EntityTypes.WOLF.create(player.level(), EntitySpawnReason.COMMAND);
			Vec3 p = at(-2.0, 1.0);
			wolf.snapTo(p.x, p.y, p.z, 0, 0);
			wolf.addTag(TAG);
			wolf.tame(player);
			player.level().addFreshEntity(wolf);
			Mob husk = foes(player).getFirst();
			husk.setTarget(wolf);
			int turned = WayEffects.counted("challenge");
			AuraIntent.pulse(player);
			if (husk.getTarget() != player) {
				return "Living Wall's Intent should draw a creature off the swordsman's pet (" + husk.getTarget() + ")";
			}
			return WayEffects.counted("challenge") > turned ? null : "and count it";
		});
		check(challenged == null, challenged);

		// Unbroken: awakened, nothing breaks its stance and harm comes in a fifth weaker; spent, never slowed.
		reset(context, world);
		on(world, player -> {
			setAura(player, "stone", AuraRules.SOVEREIGN);
			Ways.set(player, WayRules.BULWARK);
			setMomentumNow(player, 60);
			check(AuraApi.awaken(player), "it should awaken");
			Rival rival = rival(player);
			rival.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("ember", AuraRules.FORM, 0, AuraRules.capacity(AuraRules.FORM), 0));
			return null;
		});
		context.waitTicks(AwakeningRules.RISE);
		String unbroken = on(world, player -> {
			Rival rival = rival(player);
			player.removeAttached(Stance.STANCE);
			if (Stance.wear(rival, player, 10.0, StanceRules.Source.BLOW) > 0) {
				return "awakened, nothing should wear an unbroken Bulwark's stance";
			}
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 3.0), 200, 180);
			float creature = WayBanner.harm(player, player.level().damageSources().mobAttack(husk), 10);
			float duel = WayBanner.harm(player, player.level().damageSources().playerAttack(rival), 10);
			double pvp = dev.wildercord.config.Config.get().aura().pvpScale();
			if (Math.abs(creature - 10 * (1 - WayRules.BULWARK_AWAKENED_HARM)) > 1.0E-3) {
				return "a creature's blow should reach an unbroken Bulwark a fifth weaker (" + creature + ")";
			}
			return Math.abs(duel - 10 * (1 - WayRules.BULWARK_AWAKENED_HARM * pvp)) < 1.0E-3 ? null : "a player's only by the PvP scale's share (" + duel + ")";
		});
		check(unbroken == null, unbroken);
		dropRival(world);
		on(world, player -> {
			AuraApi.endAwakening(player);
			return null;
		});
		context.waitTicks(25);
		String spent = on(world, player -> !Awakening.spent(player) ? "it should be spent now"
			: player.hasEffect(MobEffects.SLOWNESS) ? "spent, an Unbroken Bulwark isn't slowed" : null);
		check(spent == null, spent);
		calm(context, world);

		// The bastion: a foe's shot crossing into the Dominion is turned back at its edge.
		reset(context, world);
		on(world, player -> {
			setAura(player, "stone", AuraRules.SOVEREIGN);
			Ways.set(player, WayRules.BULWARK);
			check(AuraDominion.raise(player), "a Dominion should rise");
			return null;
		});
		context.waitTicks(4);
		int bastions = WayEffects.counted("bastion");
		double radius = on(world, AuraDominion::radius);
		on(world, player -> {
			Mob archer = spawn(player.level(), EntityTypes.SKELETON, at(radius + 6, 0), 40, 90);
			for (int i = 0; i < 3; i++) {
				Arrow arrow = EntityTypes.ARROW.create(player.level(), EntitySpawnReason.COMMAND);
				Vec3 p = at(radius + 1.6, -1 + i).add(0, 1.3, 0);
				arrow.snapTo(p.x, p.y, p.z, 90, 0);
				arrow.setOwner(archer);
				arrow.setDeltaMovement(-1.2, 0.02, 0);
				arrow.addTag(TAG);
				player.level().addFreshEntity(arrow);
			}
			return null;
		});
		thirdPerson(context, world, -90, 30, 7.0, false);
		context.waitTicks(2);
		shot(context, "ways_bulwark_bastion_tp");
		context.waitTicks(4);
		String turned = on(world, player -> {
			if (WayEffects.counted("bastion") <= bastions) {
				return "the bastion should turn the shots back";
			}
			for (Entity e : player.level().getEntitiesOfClass(Arrow.class, player.getBoundingBox().inflate(16), a -> a.entityTags().contains(TAG))) {
				if (e.getX() - (STAGE.getX() + 0.5) < radius - 0.5) {
					return "no shot should cross into it (" + (e.getX() - STAGE.getX()) + ")";
				}
			}
			return null;
		});
		check(turned == null, turned);
		on(world, player -> {
			AuraDominion.end(player);
			return null;
		});
		world.getServer().runCommand("kill @e[type=arrow]");
		context.waitTicks(30);
		firstPerson(context, world);
	}

	// ------------------------------------------------------------------ the Shadowstep

	private static void shadowstep(ClientGameTestContext context, TestSingleplayerContext world) {
		// Slip: a perfect guard against a blow slips the swordsman behind its striker, facing its back.
		reset(context, world);
		on(world, player -> {
			setAura(player, "hollow", AuraRules.EDGE);
			Ways.set(player, WayRules.SHADOWSTEP);
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 1.8), 200, 180);
			husk.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6);
			steady(husk);
			return null;
		});
		thirdPerson(context, world, -60, 16, 5.0, false);
		context.getInput().holdKey(o -> o.keyShift);
		context.waitTicks(4);
		int slips = WayEffects.counted("slip");
		String slipped = on(world, player -> {
			check(AuraGuard.raise(player), "the guard should go up");
			Mob husk = foes(player).getFirst();
			float hp = player.getHealth();
			husk.doHurtTarget(player.level(), player);
			if (player.getHealth() < hp) {
				return "the perfect guard should turn the blow aside whole";
			}
			if (WayEffects.counted("slip") <= slips) {
				return "a Shadowstep's perfect guard should slip behind the striker";
			}
			return player.getZ() > husk.getZ() + 0.4 ? null : "the swordsman should stand behind the husk (" + player.getZ() + " against " + husk.getZ() + ")";
		});
		context.getInput().releaseKey(o -> o.keyShift);
		context.waitTicks(2);
		shot(context, "ways_shadowstep_slip_tp");
		check(slipped == null, slipped);
		String facing = on(world, player -> {
			Mob husk = foes(player).getFirst();
			Vec3 to = husk.position().subtract(player.position());
			Vec3 look = player.getViewVector(1.0F);
			return look.x * to.x + look.z * to.z > 0 ? null : "the slip leaves the swordsman facing the husk's back";
		});
		check(facing == null, facing);
		String behindNow = on(world, player -> WayEffects.fromBehind(player, foes(player).getFirst()) ? null : "the husk lost the swordsman: blows count as from behind");
		check(behindNow == null, behindNow);

		// Blows from behind wear stance half again as fast.
		reset(context, world);
		String wear = on(world, player -> {
			setAura(player, "hollow", AuraRules.EDGE);
			Ways.set(player, WayRules.SHADOWSTEP);
			Mob front = spawn(player.level(), EntityTypes.HUSK, at(-1.2, 2.2), 200, 180);
			Mob back = spawn(player.level(), EntityTypes.HUSK, at(1.2, 2.2), 200, 0);
			front.setYBodyRot(180);
			back.setYBodyRot(0);
			double faced = Stance.wear(player, front, 5.0, StanceRules.Source.BLOW);
			double behind = Stance.wear(player, back, 5.0, StanceRules.Source.BLOW);
			if (Math.abs(faced - 5.0) > 1.0E-6) {
				return "a blow from in front wears as ever (" + faced + ")";
			}
			return Math.abs(behind - 5.0 * WayRules.BEHIND_WEAR) < 1.0E-6 ? null : "a blow from behind wears half again (" + behind + ")";
		});
		check(wear == null, wear);

		// Afterimage: the step leaves an afterimage where it set off, striking the foes beside it; every blow counts as from behind a moment.
		reset(context, world);
		on(world, player -> {
			setAura(player, "hollow", AuraRules.FORM);
			Ways.set(player, WayRules.SHADOWSTEP);
			steady(spawn(player.level(), EntityTypes.HUSK, at(-1.5, -0.3), 100, 90));
			steady(spawn(player.level(), EntityTypes.HUSK, at(1.5, 0.3), 100, 270));
			return null;
		});
		thirdPerson(context, world, 140, 24, 6.0, false);
		context.waitTicks(4);
		int images = WayEffects.counted("afterimage");
		String stepped = on(world, player -> {
			check(AuraStep.step(player), "the step should go");
			return null;
		});
		check(stepped == null, stepped);
		context.waitTicks(2);
		check(on(world, p -> WayEffects.unseen(p)), "just after a step, a Shadowstep's blows count as from behind");
		context.waitTicks(WayRules.AFTERIMAGE_DELAY - 1);
		shot(context, "ways_shadowstep_afterimage_tp");
		context.waitTicks(3);
		String struck = on(world, player -> {
			if (WayEffects.counted("afterimage") != images + 1) {
				return "the afterimage should strike once";
			}
			for (Mob m : foes(player)) {
				if (m.getHealth() >= m.getMaxHealth()) {
					return "the afterimage should cut every foe beside it";
				}
			}
			return null;
		});
		check(struck == null, struck);
		// Through the swordsman's own eyes: the afterimage behind them, nothing in the middle of the view.
		reset(context, world);
		on(world, player -> {
			setAura(player, "hollow", AuraRules.FORM);
			Ways.set(player, WayRules.SHADOWSTEP);
			steady(spawn(player.level(), EntityTypes.HUSK, at(0, 9.0), 100, 180));
			check(AuraStep.step(player), "the step should go");
			return null;
		});
		context.waitTicks(WayRules.AFTERIMAGE_DELAY);
		shot(context, "ways_shadowstep_afterimage_fp");

		// Thousand Shadows: awakened, the step is free and quick and its afterimage strikes twice; a finisher from behind readies it.
		reset(context, world);
		on(world, player -> {
			setAura(player, "hollow", AuraRules.SOVEREIGN);
			Ways.set(player, WayRules.SHADOWSTEP);
			setMomentumNow(player, 60);
			check(AuraApi.awaken(player), "it should awaken");
			return null;
		});
		context.waitTicks(AwakeningRules.RISE);
		int before = WayEffects.counted("afterimage");
		String free = on(world, player -> {
			float aura = Aura.aura(player);
			long now = player.level().getGameTime();
			check(AuraStep.step(player), "the step should go");
			if (Math.abs(Aura.aura(player) - aura) > 1.0E-3) {
				return "awakened, a Shadowstep's step costs nothing";
			}
			long rest = AuraPresence.timers(player).stepReadyAt() - now;
			return rest == AuraRules.STEP_COOLDOWN / 2 ? null : "and is ready again in a second (" + rest + ")";
		});
		check(free == null, free);
		context.waitTicks(WayRules.AFTERIMAGE_DELAY + WayRules.AFTERIMAGE_AGAIN + 4);
		check(WayEffects.counted("afterimage") == before + 2, "awakened, its afterimage strikes twice (" + (WayEffects.counted("afterimage") - before) + ")");
		calm(context, world);
		reset(context, world);
		on(world, player -> {
			setAura(player, "hollow", AuraRules.SOVEREIGN);
			Ways.set(player, WayRules.SHADOWSTEP);
			// Its back to the swordsman.
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 2.4), 80, 0);
			husk.setYBodyRot(0);
			husk.setHealth(60);
			Stance.wear(player, husk, StanceRules.pool(StanceRules.Kind.CREATURE, 80, 0), StanceRules.Source.BLOW);
			AuraPresence.timers(player);
			player.setAttached(AuraPresence.TIMERS, AuraPresence.Timers.NONE.stepReady(player.level().getGameTime() + 400));
			return null;
		});
		firstPerson(context, world);
		context.waitTicks(FULL);
		long finishers = on(world, p -> (long) Stance.finishers());
		swing(context);
		context.waitTicks(3);
		String ready = on(world, player -> {
			if (Stance.finishers() != finishers + 1) {
				return "the swing should have been a finisher";
			}
			return AuraPresence.timers(player).stepReadyAt() <= player.level().getGameTime() ? null : "a finisher from behind readies the step at once";
		});
		check(ready == null, ready);
	}

	// ------------------------------------------------------------------ the Banner

	private static void banner(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "ember", AuraRules.SOVEREIGN);
			Ways.set(player, WayRules.BANNER);
			// A friend on the swordsman's team, a rival on another: the Banner must help one and never the other.
			Scoreboard board = player.level().getScoreboard();
			PlayerTeam friends = board.getPlayerTeam("ways_friends") != null ? board.getPlayerTeam("ways_friends") : board.addPlayerTeam("ways_friends");
			PlayerTeam others = board.getPlayerTeam("ways_others") != null ? board.getPlayerTeam("ways_others") : board.addPlayerTeam("ways_others");
			board.addPlayerToTeam(player.getScoreboardName(), friends);
			board.addPlayerToTeam("Ally", friends);
			board.addPlayerToTeam("Rival", others);
			Ally ally = ally(player);
			Rival rival = rival(player);
			Vec3 a = at(-2.0, 1.0);
			ally.snapTo(a.x, a.y, a.z, 0, 0);
			Vec3 r = at(2.0, 1.0);
			rival.snapTo(r.x, r.y, r.z, 0, 0);
			for (FakePlayer other : List.of(ally, rival)) {
				other.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("rime", AuraRules.FORM, 0, 0, 0));
				other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
				other.removeAttached(Momentum.MOMENTUM);
				other.setHealth(other.getMaxHealth());
			}
			return null;
		});
		context.waitTicks(5);
		String allies = on(world, player -> {
			if (!WayBanner.ally(player, ally(player))) {
				return "a teammate is an ally";
			}
			return WayBanner.ally(player, rival(player)) ? "a player on another team is never an ally" : null;
		});
		check(allies == null, allies);

		// Battle Cry: a third of the momentum built is shared with the ally; none with the rival.
		String shared = on(world, player -> {
			player.removeAttached(Momentum.MOMENTUM);
			double built = AuraApi.addMomentum(player, 30, "hit");
			double friend = Momentum.value(ally(player));
			double foe = Momentum.value(rival(player));
			if (Math.abs(friend - built * WayRules.BANNER_MOMENTUM) > 0.05) {
				return "the ally should build a third of it (" + built + ", " + friend + ")";
			}
			return foe > 0 ? "the rival should build none (" + foe + ")" : null;
		});
		check(shared == null, shared);

		// Rallying Presence: a quarter of the aura gathered flows to the ally; none to the rival.
		String breath = on(world, player -> {
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(10));
			double got = AuraApi.gain(player, 20, "hit");
			float friend = Aura.aura(ally(player));
			float foe = Aura.aura(rival(player));
			if (Math.abs(friend - got * WayRules.BANNER_AURA) > 0.05) {
				return "the ally should be given a quarter of it (" + got + ", " + friend + ")";
			}
			return foe > 0 ? "the rival should be given none (" + foe + ")" : null;
		});
		check(breath == null, breath);
		// ...and while Intent presses on a foe, the swordsman and the ally near are steadied.
		String presence = on(world, player -> {
			Mob weak = spawn(player.level(), EntityTypes.HUSK, at(0, 4.0), 10, 180);
			steady(weak);
			int pressed = AuraIntent.pulse(player);
			if (pressed <= 0) {
				return "Intent should press on the weak husk";
			}
			if (WayBanner.steadiness(player) < WayRules.PRESENCE_STEADY - 1.0E-6 || WayBanner.steadiness(ally(player)) < WayRules.PRESENCE_STEADY - 1.0E-6) {
				return "the Banner and its ally should be steadied while Intent presses (" + WayBanner.steadiness(player) + ", "
					+ WayBanner.steadiness(ally(player)) + ")";
			}
			return WayBanner.steadiness(rival(player)) > 0 ? "the rival should never be steadied" : null;
		});
		check(presence == null, presence);
		on(world, player -> {
			for (Mob m : foes(player)) {
				m.discard();
			}
			return null;
		});

		// The rallying cry on a finisher: everyone allied near steadied, the ally given momentum and aura; the rival nothing.
		on(world, player -> {
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 2.4), 80, 180);
			husk.setHealth(60);
			Stance.wear(player, husk, StanceRules.pool(StanceRules.Kind.CREATURE, 80, 0), StanceRules.Source.BLOW);
			ally(player).removeAttached(Momentum.MOMENTUM);
			ally(player).setAttached(AuraAttachments.AURA, Aura.data(ally(player)).withAura(0));
			return null;
		});
		thirdPerson(context, world, -20, 26, 7.0, false);
		context.waitTicks(FULL);
		int cries = WayBanner.cries();
		swing(context);
		context.waitTicks(3);
		shot(context, "ways_banner_cry_tp");
		context.waitTicks(2);
		String cry = on(world, player -> {
			if (WayBanner.cries() != cries + 1) {
				return "a Banner's finisher should let out a rallying cry";
			}
			Ally ally = ally(player);
			if (WayBanner.steadiness(ally) < WayRules.CRY_STEADY - 1.0E-6 || WayBanner.steadiness(player) < WayRules.CRY_STEADY - 1.0E-6) {
				return "the cry should steady the Banner and the ally (" + WayBanner.steadiness(ally) + ")";
			}
			if (Momentum.value(ally) < WayRules.CRY_MOMENTUM - 0.05) {
				return "the ally should build momentum from the cry (" + Momentum.value(ally) + ")";
			}
			if (Aura.aura(ally) <= 0) {
				return "the ally should share the finisher's aura";
			}
			return WayBanner.steadiness(rival(player)) > 0 ? "the rival is never steadied" : null;
		});
		check(cry == null, cry);

		// Shelter: hits take less of an ally's momentum near a Banner at Sovereign; an awakening rallies the ally's momentum.
		String near = on(world, player -> {
			Ally ally = ally(player);
			if (!WayBanner.bannerNear(ally)) {
				return "the ally should know a Banner at Sovereign stands near";
			}
			return WayBanner.bannerNear(rival(player)) ? "the rival never counts a Banner near" : null;
		});
		check(near == null, near);
		on(world, player -> {
			ally(player).removeAttached(Momentum.MOMENTUM);
			rival(player).removeAttached(Momentum.MOMENTUM);
			setMomentumNow(player, 60);
			player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(Aura.capacity(player)));
			check(AuraApi.awaken(player), "it should awaken (" + Awakening.refusal(player) + ")");
			return null;
		});
		context.waitTicks(5);
		String rally = on(world, player -> {
			Momentum.State s = Momentum.state(ally(player));
			if (Math.abs(s.floor() - WayRules.RALLY_FLOOR) > 1.0E-3 || s.floorUntil() <= player.level().getGameTime()) {
				return "a Banner's awakening should hold the ally's momentum at the second tier (" + s + ")";
			}
			if (!MomentumRules.peak(Momentum.value(player)) || Momentum.value(ally(player)) < WayRules.RALLY_FLOOR - 1.0E-3) {
				return "the ally's momentum lifted to it (" + Momentum.value(ally(player)) + ")";
			}
			return Momentum.value(rival(player)) > 0 ? "the rival's momentum is never touched" : null;
		});
		check(rally == null, rally);
		calm(context, world);

		// The sheltering Dominion: the ally inside takes a fifth less from foes (a player's harm only by the PvP scale's share); the rival inside, nothing less.
		on(world, player -> {
			player.setAttached(AuraPresence.TIMERS, AuraPresence.Timers.NONE);
			check(AuraDominion.raise(player), "a Dominion should rise");
			return null;
		});
		context.waitTicks(4);
		thirdPerson(context, world, 0, 50, 8.0, false);
		context.waitTicks(6);
		shot(context, "ways_banner_shelter_tp");
		String sheltered = on(world, player -> {
			Ally ally = ally(player);
			Rival rival = rival(player);
			Mob husk = spawn(player.level(), EntityTypes.HUSK, at(0, 6.0), 100, 180);
			DamageSource blow = player.level().damageSources().mobAttack(husk);
			double steady = WayBanner.steadiness(ally);
			double expected = 10 * WayRules.harmLeft(WayRules.steadied(steady, WayRules.SHELTER), false, 0);
			float friend = WayBanner.harm(ally, blow, 10);
			if (Math.abs(friend - expected) > 1.0E-3) {
				return "the ally inside should take a fifth less (" + friend + " against " + expected + ")";
			}
			if (Math.abs(WayBanner.harm(rival, blow, 10) - 10) > 1.0E-3) {
				return "the rival inside takes all of it (" + WayBanner.harm(rival, blow, 10) + ")";
			}
			double pvp = dev.wildercord.config.Config.get().aura().pvpScale();
			float duel = WayBanner.harm(ally, player.level().damageSources().playerAttack(rival), 10);
			double fromPlayer = 10 * WayRules.harmLeft(WayRules.steadied(steady, WayRules.SHELTER), true, pvp);
			return Math.abs(duel - fromPlayer) < 1.0E-3 ? null : "a player's blow on the ally only by the PvP scale's share (" + duel + ")";
		});
		check(sheltered == null, sheltered);
		on(world, player -> {
			AuraDominion.end(player);
			Scoreboard board = player.level().getScoreboard();
			board.removePlayerFromTeam(player.getScoreboardName());
			return null;
		});
		dropRival(world);
		dropAlly(world);
		context.waitTicks(30);
		firstPerson(context, world);
	}

	// ------------------------------------------------------------------ changing Way

	private static void incense(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "verdant", AuraRules.FORM);
			Ways.set(player, WayRules.BLADE);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CrossroadsIncense.INCENSE, 2));
			return null;
		});
		context.waitTicks(5);
		// Away from a place of power, it won't catch.
		check(!on(world, player -> dev.wildercord.cast.PowerPlaces.isPlaceOfPower(player.level(), player.blockPosition())), "the stage isn't a place of power");
		context.getInput().holdKey(o -> o.keyUse);
		context.waitTicks(48);
		context.getInput().releaseKey(o -> o.keyUse);
		context.waitTicks(4);
		String refused = on(world, player -> {
			if (!Ways.state(player).way().equals(WayRules.BLADE)) {
				return "away from a place of power the incense shouldn't unbind the Way";
			}
			return player.getMainHandItem().getCount() == 2 ? null : "a refused incense isn't burned (" + player.getMainHandItem().getCount() + ")";
		});
		check(refused == null, refused);
		// At the heart of a ley crossing it does.
		long seed = on(world, player -> LeyWalker.seed(player.level()));
		double[] heart = strongCrossing(seed);
		check(heart != null, "there should be a ley crossing near spawn");
		int hx = (int) Math.floor(heart[0]);
		int hz = (int) Math.floor(heart[1]);
		int y = 200;
		world.getServer().runCommand("forceload add " + (hx - 6) + " " + (hz - 6) + " " + (hx + 6) + " " + (hz + 6));
		world.getServer().runCommand("fill " + (hx - 6) + " " + (y - 1) + " " + (hz - 6) + " " + (hx + 6) + " " + (y - 1) + " " + (hz + 6) + " minecraft:stone");
		world.getServer().runCommand("fill " + (hx - 6) + " " + y + " " + (hz - 6) + " " + (hx + 6) + " " + (y + 5) + " " + (hz + 6) + " minecraft:air");
		on(world, player -> {
			player.teleportTo(player.level(), heart[0], y, heart[1], Set.<Relative>of(), 0.0F, 10.0F, false);
			player.setDeltaMovement(Vec3.ZERO);
			return null;
		});
		context.waitTicks(20);
		check(on(world, player -> dev.wildercord.cast.PowerPlaces.isPlaceOfPower(player.level(), player.blockPosition())), "the crossing's heart is a place of power");
		UNBOUND.clear();
		thirdPerson(context, world, 30, 14, 5.0, false);
		context.getInput().holdKey(o -> o.keyUse);
		context.waitTicks(38);
		shot(context, "ways_incense_burning_tp");
		context.waitTicks(10);
		context.getInput().releaseKey(o -> o.keyUse);
		context.waitTicks(2);
		shot(context, "ways_incense_unbound_tp");
		String unbound = on(world, player -> {
			Ways.State s = Ways.state(player);
			if (s.walking()) {
				return "the incense should unbind the Way (" + s.way() + ")";
			}
			if (!s.former().equals(WayRules.BLADE) || s.changes() != 1) {
				return "it remembers the Way left (" + s + ")";
			}
			return player.getMainHandItem().getCount() == 1 ? null : "one incense is burned (" + player.getMainHandItem().getCount() + ")";
		});
		check(unbound == null, unbound);
		check(UNBOUND.contains(WayRules.BLADE), "the Way hooks hear of it");
		context.waitTicks(36);
		check(on(world, Crossroads::standing), "the crossroads rises where the incense burned");
		on(world, player -> {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
			return null;
		});
		context.waitTicks(4);
		CHOSEN.clear();
		chooseByStrikes(context, world, WayRules.BULWARK);
		String settling = on(world, player -> {
			Ways.State s = Ways.state(player);
			if (!s.way().equals(WayRules.BULWARK)) {
				return "the new Way should be walked (" + s.way() + ")";
			}
			if (Math.abs(s.owed() - WayRules.SETTLE_XP) > 1.0E-3) {
				return "a change of Way owes the settling (" + s.owed() + ")";
			}
			if (!Ways.has(player, WayRules.BULWARK_EDGE) || Ways.has(player, WayRules.BULWARK_FORM)) {
				return "its Edge node wakes at once, its Form node waits";
			}
			return null;
		});
		check(settling == null, settling);
		check(CHOSEN.contains("bulwark:false"), "the hooks hear it's a change (" + CHOSEN + ")");
		context.runOnClient(mc -> {
			AuraScreen.showWay(true, null);
		});
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		shot(context, "ways_page_waking");
		context.setScreen(() -> null);
		String woke = on(world, player -> {
			AuraExperience.earn(player, WayRules.SETTLE_XP * WayRules.FORM_WAKES + 5, false);
			if (!Ways.has(player, WayRules.BULWARK_FORM)) {
				return "earning half the settling wakes the Form node (" + Ways.state(player).owed() + " owed)";
			}
			return Ways.state(player).owed() > 0 ? null : "the Sovereign node still waits on the rest";
		});
		check(woke == null, woke);
		world.getServer().runCommand("forceload remove all");
		on(world, player -> {
			stand(player);
			return null;
		});
		context.waitTicks(10);
		firstPerson(context, world);
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

	// ------------------------------------------------------------------ a death

	private static void death(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		on(world, player -> {
			setAura(player, "crimson", AuraRules.FORM);
			Ways.set(player, WayRules.BANNER);
			return null;
		});
		context.waitTicks(5);
		respawn(context, world);
		String kept = on(world, player -> Ways.state(player).way().equals(WayRules.BANNER) && Ways.has(player, WayRules.BANNER_FORM) ? null
			: "a death keeps the Way (" + Ways.state(player) + ")");
		check(kept == null, kept);
		check(context.computeOnClient(mc -> Ways.way(mc.player).map(AuraApi.Way::id).orElse("")).equals(WayRules.BANNER), "the new body's client knows it too");
		// A crossroads standing at a death closes.
		on(world, player -> {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
			Ways.set(player, "");
			check(AuraApi.openCrossroads(player), "the crossroads should rise");
			return null;
		});
		context.waitTicks(5);
		respawn(context, world);
		check(!on(world, Crossroads::standing), "a death closes a standing crossroads");
		check(context.computeOnClient(mc -> Crossroads.state(mc.player) == null), "and the new body's client knows");
	}

	private static void respawn(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> player(server).kill(player(server).level()));
		context.waitTicks(5);
		context.runOnClient(mc -> {
			mc.player.respawn();
			mc.gui.setScreen(null);
		});
		context.waitTicks(15);
		on(world, player -> {
			stand(player);
			return null;
		});
		context.waitTicks(5);
	}

	// ------------------------------------------------------------------ the Aura page

	private static void pages(ClientGameTestContext context, TestSingleplayerContext world) {
		reset(context, world);
		// Before Edge: what each Way gives, every node open.
		on(world, player -> {
			setAura(player, "rime", AuraRules.FLOW);
			Ways.set(player, "");
			return null;
		});
		page(context, "ways_page_before_edge", null);
		// Past the crossroads with no Way: the tab breathes gold, the way to call it said.
		on(world, player -> {
			setAura(player, "rime", AuraRules.SOVEREIGN);
			Ways.set(player, "");
			return null;
		});
		page(context, "ways_page_wayless", null);
		// Walking a Way at each stage: chosen, upcoming and locked nodes.
		on(world, player -> {
			setAura(player, "ember", AuraRules.FORM);
			Ways.set(player, WayRules.BLADE);
			return null;
		});
		page(context, "ways_page_blade_form", null);
		on(world, player -> {
			setAura(player, "starlit", AuraRules.SOVEREIGN);
			Ways.set(player, WayRules.BANNER);
			return null;
		});
		page(context, "ways_page_banner_sovereign", WayRules.BANNER_SOVEREIGN);
		page(context, "ways_page_banner_locked", WayRules.SHADOWSTEP_FORM);
		// The tab is clicked like the others.
		context.runOnClient(mc -> AuraScreen.listArts(false));
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		double[] tab = context.computeOnClient(mc -> mc.gui.screen() instanceof AuraScreen s ? s.wayTabPoint() : null);
		check(tab != null, "the Way tab should be drawn");
		double guiScale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(tab[0] * guiScale, tab[1] * guiScale);
		context.waitTicks(1);
		context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(3);
		context.getInput().setCursorPos(4, 4);
		context.waitTicks(2);
		shot(context, "ways_page_clicked");
		check(context.computeOnClient(mc -> mc.gui.screen() instanceof AuraScreen s && s.wayTabPoint() != null && AuraScreen.showingWay()),
			"clicking the tab should open the Way tree");
		context.setScreen(() -> null);
		context.runOnClient(mc -> AuraScreen.listArts(false));
	}

	private static void page(ClientGameTestContext context, String name, String node) {
		context.waitTicks(5);
		context.runOnClient(mc -> AuraScreen.showWay(true, node));
		context.setScreen(() -> new AuraScreen(null));
		context.waitTicks(3);
		shot(context, name);
		context.setScreen(() -> null);
	}

	// ------------------------------------------------------------------ aiming and swinging

	/** Turns the swordsman to look at the middle of {@code way}'s standard. */
	private static void aim(ClientGameTestContext context, TestSingleplayerContext world, String way) {
		on(world, player -> {
			Crossroads.State s = Crossroads.state(player);
			check(s != null, "no crossroads to aim at");
			Crossroads.Standard standard = s.standards().stream().filter(x -> x.way().equals(way)).findFirst().orElseThrow();
			Vec3 eye = player.getEyePosition();
			Vec3 d = standard.foot().add(0, 1.3, 0).subtract(eye);
			float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
			float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), yaw, pitch, false);
			return null;
		});
		context.waitTicks(3);
	}

	private static void swing(ClientGameTestContext context) {
		context.getInput().pressKey(o -> o.keyAttack);
	}

	// ------------------------------------------------------------------ views

	private static void thirdPerson(ClientGameTestContext context, TestSingleplayerContext world, float yaw, float pitch, double distance, boolean hud) {
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(distance);
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), yaw, pitch, false);
			return null;
		});
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			if (mc.gui.hud.isHidden() == hud) {
				mc.gui.hud.toggle();
			}
		});
		context.waitTicks(2);
	}

	private static void firstPerson(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			player.getAttribute(Attributes.CAMERA_DISTANCE).setBaseValue(4.0);
			player.teleportTo(player.level(), player.getX(), player.getY(), player.getZ(), Set.<Relative>of(), 0.0F, 8.0F, false);
			return null;
		});
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
	}

	// ------------------------------------------------------------------ the stage

	private static void stage(TestSingleplayerContext world) {
		int x = STAGE.getX();
		int y = STAGE.getY();
		int z = STAGE.getZ();
		world.getServer().runCommand("fill " + (x - 14) + " " + (y - 1) + " " + (z - 14) + " " + (x + 14) + " " + (y - 1) + " " + (z + 24) + " minecraft:stone");
		world.getServer().runCommand("fill " + (x - 14) + " " + y + " " + (z - 14) + " " + (x + 14) + " " + (y + 10) + " " + (z + 24) + " minecraft:air");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(GameType.SURVIVAL);
			Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
			stand(player);
		});
	}

	private static void stand(ServerPlayer player) {
		player.teleportTo(player.level(), STAGE.getX() + 0.5, STAGE.getY(), STAGE.getZ() + 0.5, Set.<Relative>of(), 0.0F, 8.0F, false);
		player.setDeltaMovement(Vec3.ZERO);
		player.removeAllEffects();
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);
		player.resetFallDistance();
		player.clearFire();
	}

	private static void reset(ClientGameTestContext context, TestSingleplayerContext world) {
		context.getInput().releaseKey(o -> o.keyShift);
		calm(context, world);
		kill(world);
		world.getServer().runCommand("kill @e[type=item]");
		world.getServer().runCommand("kill @e[type=experience_orb]");
		world.getServer().runCommand("kill @e[type=arrow]");
		world.getServer().runCommand("fill " + (STAGE.getX() - 14) + " " + STAGE.getY() + " " + (STAGE.getZ() - 14) + " " + (STAGE.getX() + 14) + " "
			+ (STAGE.getY() + 10) + " " + (STAGE.getZ() + 24) + " minecraft:air");
		context.waitTicks(6);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			stand(player);
			player.removeAttached(AuraAttachments.STATE);
			player.removeAttached(SwordStrings.COOLDOWNS);
			player.removeAttached(Momentum.MOMENTUM);
			player.removeAttached(Stance.STANCE);
			player.removeAttached(Crossroads.CROSSROADS);
			player.setAttached(AuraPresence.TIMERS, AuraPresence.Timers.NONE);
			AuraDominion.end(player);
			dev.wildercord.aura.arts.MethodArts.forget(player.getUUID());
		});
		firstPerson(context, world);
		context.runOnClient(mc -> {
			mc.player.setXRot(8);
			mc.player.xRotO = 8;
		});
		context.waitTicks(StringRules.ENGAGED_TICKS / 2);
	}

	private static void calm(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, player -> {
			AuraApi.endAwakening(player);
			return null;
		});
		context.waitTicks(2);
		on(world, player -> {
			player.removeAttached(Awakening.AWAKENING);
			player.removeAllEffects();
			if (Aura.stage(player) > AuraRules.NONE) {
				player.setAttached(AuraAttachments.AURA, Aura.data(player).withAura(Aura.capacity(player)));
			}
			player.removeAttached(Momentum.MOMENTUM);
			return null;
		});
		context.waitTicks(2);
	}

	private static void kill(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			for (Entity e : player.level().getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(64), e -> e.entityTags().contains(TAG))) {
				e.discard();
			}
		});
	}

	private static void setAura(ServerPlayer player, String method, int stage) {
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(method, stage, AuraRules.threshold(stage), AuraRules.capacity(stage), 0));
		player.removeAttached(AuraAttachments.STATE);
		player.removeAttached(Awakening.AWAKENING);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
	}

	private static void setMomentumNow(ServerPlayer player, double value) {
		player.setAttached(Momentum.MOMENTUM, new Momentum.State((float) value, player.level().getGameTime() + 100000, 0, 0, 0));
	}

	private static Vec3 at(double side, double ahead) {
		return new Vec3(STAGE.getX() + 0.5 + side, STAGE.getY(), STAGE.getZ() + 0.5 + ahead);
	}

	/** A foe that stands its ground (no speed, no sight, its AI left on), facing {@code yaw}. */
	private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, Vec3 at, double health, float yaw) {
		T mob = type.create(level, EntitySpawnReason.COMMAND);
		if (mob == null) {
			throw new AssertionError("couldn't make a " + type);
		}
		mob.snapTo(at.x, at.y, at.z, yaw, 0);
		mob.setYHeadRot(yaw);
		mob.setYBodyRot(yaw);
		mob.addTag(TAG);
		mob.addTag("wildercord.rolled");
		mob.setPersistenceRequired();
		AttributeInstance max = mob.getAttribute(Attributes.MAX_HEALTH);
		if (max != null) {
			max.setBaseValue(health);
		}
		AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.setBaseValue(0.0);
		}
		AttributeInstance sight = mob.getAttribute(Attributes.FOLLOW_RANGE);
		if (sight != null) {
			sight.setBaseValue(0.0);
		}
		mob.setHealth((float) health);
		level.addFreshEntity(mob);
		return mob;
	}

	/** The scene's husks, in the order they were spawned. */
	private static List<Mob> foes(ServerPlayer player) {
		List<Mob> found = new ArrayList<>(player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48),
			m -> m.entityTags().contains(TAG) && m.getType() == EntityTypes.HUSK && m.isAlive()));
		found.sort(java.util.Comparator.comparingInt(Mob::getId));
		if (found.isEmpty()) {
			throw new AssertionError("the foes are gone");
		}
		return found;
	}

	private static void steady(Mob mob) {
		long now = mob.level().getGameTime();
		mob.setAttached(Stance.STANCE, new Stance.State(0, now, (float) StanceRules.pool(Stance.kind(mob), mob.getMaxHealth(), 0), Stance.kind(mob).ordinal(),
			-1, now + 100000, 0));
	}

	// ------------------------------------------------------------------ other players

	/** Another player for a duel (a fake one that can be hurt, on whatever team the scoreboard says). */
	private static final class Rival extends FakePlayer {
		Rival(ServerLevel level) {
			super(level, new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("wildercord-ways-rival".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
				"Rival"));
		}

		@Override
		public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
			return false;
		}

		@Override
		public PlayerTeam getTeam() {
			return level().getScoreboard().getPlayersTeam(getScoreboardName());
		}
	}

	/** A friend fighting beside the swordsman (a fake player on their team). */
	private static final class Ally extends FakePlayer {
		Ally(ServerLevel level) {
			super(level, new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("wildercord-ways-ally".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
				"Ally"));
		}

		@Override
		public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
			return false;
		}

		@Override
		public PlayerTeam getTeam() {
			return level().getScoreboard().getPlayersTeam(getScoreboardName());
		}
	}

	private static Rival rival;
	private static Ally ally;

	private static Rival rival(ServerPlayer player) {
		if (rival == null || rival.isRemoved() || rival.level() != player.level()) {
			rival = new Rival(player.level());
			Vec3 p = at(0, 2.0);
			rival.snapTo(p.x, p.y, p.z, 180, 0);
			rival.setYHeadRot(180);
			player.connection.send(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(rival)));
			player.level().addNewPlayer(rival);
		}
		return rival;
	}

	private static Ally ally(ServerPlayer player) {
		if (ally == null || ally.isRemoved() || ally.level() != player.level()) {
			ally = new Ally(player.level());
			Vec3 p = at(-2.0, 1.0);
			ally.snapTo(p.x, p.y, p.z, 0, 0);
			player.connection.send(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(ally)));
			player.level().addNewPlayer(ally);
		}
		return ally;
	}

	private static void dropRival(TestSingleplayerContext world) {
		on(world, player -> {
			if (rival != null) {
				rival.discard();
				player.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket(List.of(rival.getUUID())));
				rival = null;
			}
			return null;
		});
	}

	private static void dropAlly(TestSingleplayerContext world) {
		on(world, player -> {
			if (ally != null) {
				ally.discard();
				player.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket(List.of(ally.getUUID())));
				ally = null;
			}
			return null;
		});
	}

	private static void shot(ClientGameTestContext context, String name) {
		context.runOnClient(mc -> mc.gui.toastManager().clear());
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	private static <T> T on(TestSingleplayerContext world, Function<ServerPlayer, T> step) {
		return world.getServer().computeOnServer(server -> step.apply(player(server)));
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}
}
