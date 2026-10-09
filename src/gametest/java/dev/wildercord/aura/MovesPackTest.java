package dev.wildercord.aura;

import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.MasterVictories;
import dev.wildercord.aura.world.MasterVictoryRules;
import dev.wildercord.aura.world.MastersRules;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.RuneBolt;
import dev.wildercord.client.CombatPresentation;
import dev.wildercord.client.FormDashScreen;
import dev.wildercord.client.MasterFormsClient;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.presentation.CombatPresentationOptions;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellCutRules;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * The moves pack on one Survival save: the three teachers' new lessons, Air Step and Plunging Strike (land and splash), the three
 * parry follow-ups through the real form key, Spell Cut (miss, nothing, and a cut through the real attack key), and a first-person
 * capture that shows the hand moving under Stable camera and Reduced flash while the camera stays put.
 */
public final class MovesPackTest implements FabricClientGameTest {
	private static final String SUITE = "dev.wildercord.aura.MovesPackTest";
	private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
	private static Husk husk;
	private static net.minecraft.world.entity.monster.illager.Pillager caster;

	@Override public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule minecraft:spawn_mobs false");
			world.getServer().runCommand("gamerule minecraft:natural_health_regeneration false");
			world.getServer().runCommand("time set day");
			on(world, p -> {
				dev.wildercord.Wildercord.LOGGER.info("WILDERCORD_NATIVE_WORLD {\"suite\":\"" + SUITE + "\",\"seed\":\"{}\"}", p.level().getServer().overworld().getSeed());
				for (int x = -8; x <= 14; x++) for (int z = -6; z <= 6; z++) {
					p.level().setBlockAndUpdate(new BlockPos(x, 98, z), Blocks.STONE.defaultBlockState());
					p.level().setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
					for (int y = 100; y <= 110; y++) p.level().setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
				}
				for (int x = 10; x <= 12; x++) for (int z = -1; z <= 1; z++) p.level().setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.WATER.defaultBlockState());
				p.setGameMode(GameType.SURVIVAL);
				place(p, .5, 100, .5);
				check(FormDash.data(p).equals(FormDash.Progress.NONE), "An old save knows no field form");
				p.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("gale", AuraRules.SOVEREIGN, 4500, 160, 0));
				p.setAttached(MasterVictories.RECORD, MasterVictoryRules.Progress.NONE.withClear(MastersRules.GALE).withClear(MastersRules.EMBER).withClear(MastersRules.STONE));
				p.setAttached(MasterForms.PROGRESS, new MasterForms.Progress(true, 0, 0, false, false));
				return null;
			});
			lessons(context, world);
			aerial(context, world);
			followUps(context, world);
			spellCut(context, world);
		}
	}

	// ---- Lessons: each school's teacher offers its follow-up first, then its second new form. Every book has its own cover.
	private static void lessons(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, p -> {
			var ember = teacher(p, BreathingMethods.EMBER, 3.5);
			var gale = teacher(p, BreathingMethods.GALE, -3.5);
			var stone = teacher(p, BreathingMethods.STONE, 5.5);
			check(FormDashLessons.next(p, ember) == 0, "Cinder Lunge keeps its place before Ember's new lessons");
			check(FormDash.learn(p, FormDashRules.CINDER_LUNGE), "Cinder Lunge is learned");
			check(FormDashLessons.next(p, ember) == FormDashRules.RIPOSTE, "The Ember teacher then offers Riposte");
			check(FormDashLessons.next(p, gale) == 0, "Reed Slip keeps its place before Gale's new lessons");
			check(FormDash.learn(p, FormDashRules.REED_SLIP), "Reed Slip is learned");
			check(FormDashLessons.next(p, gale) == FormDashRules.SHOVE, "The Gale teacher then offers Disarm Shove");
			check(FormDashLessons.next(p, stone) == FormDashRules.GUARD_BREAK, "The Stone teacher offers Guard Break");
			p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); p.setShiftKeyDown(true);
			check(FormDashLessons.offer(p, stone), "The Stone teacher's offer opens");
			p.setShiftKeyDown(false);
			return null;
		});
		context.waitFor(mc -> mc.gui.screen() instanceof FormDashScreen s && s.form() == FormDashRules.GUARD_BREAK, 40);
		check(context.computeOnClient(mc -> ((FormDashScreen) mc.gui.screen()).layoutFits()), "The Guard Break lesson fits the native font");
		context.takeScreenshot(TestScreenshotOptions.of("moves_pack_stone_break_teacher").disableCounterPrefix());
		context.runOnClient(mc -> {
			for (String name : new String[] {"ember_riposte", "gale_shove", "stone_break", "air_step", "plunge", "spell_cut"}) for (int page = 1; page <= 3; page++)
				check(mc.font.split(net.minecraft.network.chat.Component.translatable("book.wildercord." + name + "." + page), 114).size() <= 14,
					"Every lesson-book page fits the readable area: " + name + " " + page);
			mc.gui.setScreen(null);
		});
		context.waitTicks(3);
		on(world, p -> {
			var teachers = p.level().getEntitiesOfClass(dev.wildercord.aura.world.Duelist.class, p.getBoundingBox().inflate(12));
			for (int form : new int[] {FormDashRules.RIPOSTE, FormDashRules.SHOVE, FormDashRules.GUARD_BREAK}) check(FormDash.learn(p, form), "Learned " + form);
			for (var t : teachers) {
				int next = FormDashLessons.next(p, t);
				int want = t.method().equals(BreathingMethods.EMBER) ? FormDashRules.SPELL_CUT : t.method().equals(BreathingMethods.GALE) ? FormDashRules.AIR_STEP : FormDashRules.PLUNGE;
				check(next == want, "After the follow-up each teacher offers its second form: " + t.method().id() + " -> " + next);
			}
			for (int form : new int[] {FormDashRules.SPELL_CUT, FormDashRules.AIR_STEP, FormDashRules.PLUNGE}) check(FormDash.learn(p, form), "Learned " + form);
			for (var t : teachers) { check(FormDashLessons.next(p, t) == 0, "Nothing left to offer"); t.discard(); }
			for (int form : new int[] {FormDashRules.RIPOSTE, FormDashRules.SHOVE, FormDashRules.GUARD_BREAK, FormDashRules.AIR_STEP, FormDashRules.PLUNGE, FormDashRules.SPELL_CUT})
				check(dev.wildercord.Wildercord.id(FormDash.name(form) + "_lesson").equals(FormDashLessons.book(form).get(DataComponents.ITEM_MODEL)), "Own cover: " + form);
			check(Aura.data(p).xp() == 4500 && Aura.data(p).method().equals("gale"), "Lessons change neither method nor XP");
			p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD)); p.inventoryMenu.broadcastChanges();
			return null;
		});
		context.waitFor(mc -> FormDash.data(mc.player).knows(FormDashRules.PLUNGE) && mc.player.getMainHandItem().is(Items.DIAMOND_SWORD), 40);
	}

	// ---- Air Step lands, Plunging Strike lands on a foe, and the same dive over water splashes.
	private static void aerial(ClientGameTestContext context, TestSingleplayerContext world) {
		ready(world, FormDashRules.AIR_STEP);
		on(world, p -> { place(p, .5, 103.5, .5); return null; });
		context.waitTicks(2);
		double before = aura(world);
		double startY = context.computeOnClient(mc -> mc.player.getY());
		context.getInput().pressKey(MasterFormsClient.mapping());
		double[] top = {startY};
		StringBuilder trace = new StringBuilder();
		List<Integer> phases = ending(context, world, top, trace);
		check(phases.contains(FormDashRules.RISE) && phases.getLast() == FormDashRules.LAND, "Air Step rises and then lands: " + phases + " trace " + trace);
		check(top[0] > startY + .5, "Air Step really rose in the air: " + startY + " -> " + top[0]);
		on(world, p -> {
			check(Math.abs(before - Aura.aura(p) - FormDashRules.AIR_STEP_COST) < .01, "Air Step pays its price once: " + before + " -> " + Aura.aura(p));
			check(FormDash.data(p).drilled(FormDashRules.AIR_STEP) && FormDash.data(p).recoveryUntil() > MasterForms.now(p), "A clean landing drills it and owes recovery");
			check(p.onGround() && Math.abs(p.getY() - 100) < .01, "Landed on the floor");
			return null;
		});
		context.takeScreenshot(TestScreenshotOptions.of("moves_pack_air_step_land").disableCounterPrefix());

		ready(world, FormDashRules.PLUNGE);
		on(world, p -> {
			husk = EntityTypes.HUSK.create(p.level(), EntitySpawnReason.COMMAND);
			husk.setNoAi(true); husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); husk.setHealth(200);
			husk.snapTo(2.0, 100, .5, 90, 0); p.level().addFreshEntity(husk);
			place(p, .5, 104.5, .5);
			return null;
		});
		context.waitTicks(2);
		double plungeBefore = aura(world);
		StringBuilder diveTrace = new StringBuilder(on(world, p -> String.format("{pre %.2f v%d/%d/%d @%d}", p.getY(), FormDash.view(p).phase(), FormDash.view(p).form(), FormDash.view(p).ticks(), p.level().getGameTime())));
		context.getInput().pressKey(MasterFormsClient.mapping());
		List<Integer> dive = ending(context, world, new double[] {0}, diveTrace);
		check(dive.contains(FormDashRules.SET) && dive.contains(FormDashRules.DIVE) && dive.getLast() == FormDashRules.LAND, "Plunge sets, dives and lands: " + dive + " trace " + diveTrace);
		on(world, p -> {
			check(Math.abs(plungeBefore - Aura.aura(p) - FormDashRules.PLUNGE_COST) < .01, "Plunge pays its price once");
			check(Math.abs(p.getY() - 100) < .05, "Plunge stopped on the floor: " + p.position());
			check(husk.getHealth() < 200, "The landing struck the foe below: " + husk.getHealth());
			check(FormDash.data(p).drilled(FormDashRules.PLUNGE), "A clean landing drills Plunge");
			return null;
		});
		context.takeScreenshot(TestScreenshotOptions.of("moves_pack_plunge_land").disableCounterPrefix());

		// The landing plays out first: a press while it still shows is part of its recovery.
		context.waitFor(mc -> FormDash.view(mc.player).phase() == FormDashRules.IDLE, 60);
		ready(world, FormDashRules.PLUNGE);
		on(world, p -> { place(p, 11.5, 104.5, .5); return null; });
		context.waitTicks(2);
		StringBuilder wetTrace = new StringBuilder(on(world, p -> {
			long now = MasterForms.now(p); var d = FormDash.data(p);
			return String.format("{pre %.2f able%s free%s committed%s pay%s air%s mready%d ready%d rec%d now%d}", p.getY(), FormDash.able(p, FormDashRules.PLUNGE), MasterForms.free(p),
				MasterForms.committed(p), FormDashRules.canPay(FormDashRules.PLUNGE, Aura.aura(p), now, Math.max(d.readyAt(), MasterForms.data(p).readyAt()), d.recoveryUntil()),
				MasterForms.data(p).airborneUsed(), MasterForms.data(p).readyAt(), d.readyAt(), d.recoveryUntil(), now);
		}));
		context.getInput().pressKey(MasterFormsClient.mapping());
		List<Integer> wet = ending(context, world, new double[] {0}, wetTrace);
		check(wet.getLast() == FormDashRules.SPLASH, "A dive into water ends in a splash, not a landing: " + wet + " trace " + wetTrace);
		on(world, p -> {
			check(FormDash.data(p).recoveryUntil() > MasterForms.now(p), "A splash still owes recovery");
			place(p, .5, 100, .5);
			return null;
		});
		context.waitTicks(30);
	}

	// ---- Parry follow-ups: a perfect guard opens a short moment; the form key with W, A or nothing answers.
	private static void followUps(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, p -> {
			var f = FormDash.data(p);
			p.setAttached(FormDash.PROGRESS, new FormDash.Progress(f.learned(), 0, 0, 0, f.practiced()));
			return null;
		});
		context.waitFor(mc -> FormDash.data(mc.player).equipped() == 0 && FormDash.view(mc.player).phase() == FormDashRules.IDLE, 40);
		followUp(context, world, FormDashRules.RIPOSTE, o -> o.keyUp, false);
		followUp(context, world, FormDashRules.SHOVE, o -> o.keyLeft, false);
		followUp(context, world, FormDashRules.GUARD_BREAK, null, true);
		on(world, p -> {
			check(!FormDash.followOpen(p), "The moment closes once it is answered");
			FormDash.parried(p, husk);
			check(FormDash.followOpen(p), "A new parry opens a new moment");
			return null;
		});
		context.waitTicks(FormDashRules.FOLLOW_WINDOW + 2);
		on(world, p -> { check(!FormDash.followOpen(p), "An unanswered moment times out"); husk.discard(); return null; });
	}
	private static void followUp(ClientGameTestContext context, TestSingleplayerContext world, int form, Function<net.minecraft.client.Options, net.minecraft.client.KeyMapping> key, boolean capture) {
		on(world, p -> {
			var f = FormDash.data(p);
			p.setAttached(FormDash.PROGRESS, new FormDash.Progress(f.learned(), 0, 0, 0, f.practiced()));
			place(p, .5, 100, .5);
			husk.teleportTo(3.0, 100, .5); husk.setDeltaMovement(Vec3.ZERO); husk.setHealth(200);
			husk.removeAllEffects();
			return null;
		});
		context.waitTicks(4);
		boolean[] reduced = {false};
		Object[] presentation = {null};
		Path[] baseline = new Path[2];
		if (capture) {
			context.runOnClient(mc -> {
				reduced[0] = MagicQuality.reducedFlash; presentation[0] = CombatPresentation.saved();
				MagicQuality.reducedFlash = true;
				CombatPresentation.applied(new CombatPresentationOptions.Saved(CombatPresentationOptions.Animation.CLASSIC, CombatPresentationOptions.Camera.STABLE));
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});
			context.waitTicks(10);
			baseline[0] = context.takeScreenshot(TestScreenshotOptions.of("moves_pack_first_person_idle_a").disableCounterPrefix());
			context.waitTicks(3);
			baseline[1] = context.takeScreenshot(TestScreenshotOptions.of("moves_pack_first_person_idle_b").disableCounterPrefix());
		}
		try {
			double before = aura(world);
			float[] look = context.computeOnClient(mc -> new float[] {mc.player.getYRot(), mc.player.getXRot()});
			on(world, p -> { FormDash.parried(p, husk); check(FormDash.followOpen(p), "A perfect guard against a living foe opens the moment"); return null; });
			context.waitFor(mc -> FormDash.view(mc.player).window() > 0, 10);
			if (key != null) { context.getInput().holdKey(key); context.waitTicks(2); }
			context.getInput().pressKey(MasterFormsClient.mapping());
			started(context, world, form);
			if (key != null) context.getInput().releaseKey(key);
			if (capture) {
				Path posed = context.takeScreenshot(TestScreenshotOptions.of("moves_pack_first_person_stone_break_set").disableCounterPrefix());
				double noise = difference(baseline[0], baseline[1]), moved = difference(baseline[0], posed);
				check(moved > 1.5 && moved > noise * 3, "The first-person hand takes the Guard Break set: noise " + noise + ", pose " + moved);
				float[] after = context.computeOnClient(mc -> new float[] {mc.player.getYRot(), mc.player.getXRot()});
				check(Math.abs(after[0] - look[0]) < .01 && Math.abs(after[1] - look[1]) < .01, "Nothing turns the camera");
				check(context.computeOnClient(mc -> !dev.wildercord.client.fx.ScreenEffects.nudging()), "Stable camera: no camera nudge");
			}
			context.waitFor(mc -> FormDash.view(mc.player).phase() == FormDashRules.endPhase(form, false) || FormDash.view(mc.player).phase() == FormDashRules.STALL, 30);
			context.waitTicks(2);
			on(world, p -> {
				check(Math.abs(before - Aura.aura(p) - FormDashRules.cost(form)) < .01, "The follow-up pays its price: " + form);
				check(FormDash.data(p).recoveryUntil() > MasterForms.now(p), "The follow-up owes recovery: " + form);
				// The still (no-AI) foe keeps the shove's impulse rather than travelling with it.
				if (form == FormDashRules.SHOVE) check(husk.hasEffect(MobEffects.WEAKNESS) && (husk.getX() > 3.2 || husk.getDeltaMovement().x > .3),
					"The shove pushes the foe away and weakens its grip: " + husk.position() + " " + husk.getDeltaMovement() + " weak " + husk.hasEffect(MobEffects.WEAKNESS));
				else check(husk.getHealth() < 200, "The follow-up strikes the caught attacker: " + form + " " + husk.getHealth());
				if (form == FormDashRules.GUARD_BREAK) check(husk.hasEffect(MobEffects.SLOWNESS), "Guard Break staggers the foe");
				check(FormDash.data(p).drilled(form), "A clean follow-up is drilled: " + form);
				return null;
			});
		} finally {
			if (capture) context.runOnClient(mc -> {
				MagicQuality.reducedFlash = reduced[0];
				CombatPresentation.applied((CombatPresentationOptions.Saved) presentation[0]);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
			});
		}
		context.waitFor(mc -> FormDash.view(mc.player).phase() == FormDashRules.IDLE, 40);
	}

	// ---- Spell Cut: an early swing misses and pays the miss, a swing at nothing costs nothing, the real attack key cuts.
	private static void spellCut(ClientGameTestContext context, TestSingleplayerContext world) {
		on(world, p -> {
			caster = EntityTypes.PILLAGER.create(p.level(), EntitySpawnReason.MOB_SUMMONED);
			caster.setNoAi(true); caster.snapTo(-6.5, 100, .5, -90, 0); p.level().addFreshEntity(caster);
			var f = FormDash.data(p);
			p.setAttached(FormDash.PROGRESS, new FormDash.Progress(f.learned(), 0, 0, 0, f.practiced()));
			place(p, .5, 100, .5);
			return null;
		});
		context.waitTicks(3);
		on(world, p -> {
			double before = Aura.aura(p);
			check(!FormDash.counter(p, new FormDash.Counter(FormDash.view(p).epoch())), "No spell near: no counter");
			check(Aura.aura(p) == before && FormDash.data(p).recoveryUntil() <= MasterForms.now(p), "A swing at nothing costs nothing");
			bolt(p, new Vec3(7, 0, 0));
			return null;
		});
		context.waitTicks(1);
		on(world, p -> {
			double before = Aura.aura(p);
			check(!FormDash.counter(p, new FormDash.Counter(FormDash.view(p).epoch())), "Too early is a miss");
			check(Math.abs(before - Aura.aura(p) - SpellCutRules.COUNTER_MISS_COST) < .01, "A miss pays the miss price");
			check(FormDash.data(p).recoveryUntil() >= MasterForms.now(p) + SpellCutRules.COUNTER_MISS_RECOVERY - 1, "A miss owes the long recovery");
			check(!FormDash.counter(p, new FormDash.Counter(FormDash.view(p).epoch())), "One judgement per tick");
			for (var b : p.level().getEntitiesOfClass(RuneBolt.class, p.getBoundingBox().inflate(12))) { check(!b.isRemoved(), "A miss leaves the spell flying"); b.discard(); }
			return null;
		});
		context.waitFor(mc -> FormDash.view(mc.player).phase() == FormDashRules.IDLE, 40);
		on(world, p -> {
			var f = FormDash.data(p);
			p.setAttached(FormDash.PROGRESS, new FormDash.Progress(f.learned(), 0, 0, 0, f.practiced()));
			place(p, .5, 100, .5);
			return null;
		});
		context.waitTicks(25);
		double before = aura(world);
		RuneBolt[] incoming = new RuneBolt[1];
		on(world, p -> { incoming[0] = bolt(p, new Vec3(2.5, 0, .8)); return null; });
		context.waitTicks(1);
		context.getInput().pressKey(o -> o.keyAttack);
		context.waitFor(mc -> FormDash.view(mc.player).form() == FormDashRules.SPELL_CUT
			&& (FormDash.view(mc.player).phase() == FormDashRules.SEVER || FormDash.view(mc.player).phase() == FormDashRules.MISS), 20);
		check(context.computeOnClient(mc -> FormDash.view(mc.player).phase()) == FormDashRules.SEVER, "The real attack key cuts the spell in its window");
		on(world, p -> {
			check(incoming[0].isRemoved(), "The cut spell is gone");
			check(Math.abs(before - Aura.aura(p) - SpellCutRules.COUNTER_COST) < .01, "The cut pays its price: " + before + " -> " + Aura.aura(p));
			check(FormDash.data(p).drilled(FormDashRules.SPELL_CUT), "The cut is drilled");
			caster.discard();
			return null;
		});
		context.takeScreenshot(TestScreenshotOptions.of("moves_pack_spell_cut").disableCounterPrefix());
	}
	/** Follows an aerial form tick by tick until it ends, keeping a client and server trace for the failure message. */
	private static List<Integer> ending(ClientGameTestContext context, TestSingleplayerContext world, double[] top, StringBuilder trace) {
		List<Integer> phases = new ArrayList<>();
		for (int tick = 0; tick < 100; tick++) {
			double[] client = context.computeOnClient(mc -> new double[] {FormDash.view(mc.player).phase(), mc.player.getY(), mc.player.onGround() ? 1 : 0,
				FormDash.view(mc.player).form(), FormDash.view(mc.player).ticks(), mc.level.getGameTime()});
			String server = on(world, p -> String.format("%.2f%s v%d/%d/%d @%d %s", p.getY(), p.onGround() ? "g" : "", FormDash.view(p).phase(), FormDash.view(p).form(),
				FormDash.view(p).ticks(), p.level().getGameTime(), p.level().getBlockState(net.minecraft.core.BlockPos.containing(p.getX(), 99, p.getZ())).getBlock()));
			int phase = (int) client[0];
			boolean end = phase == FormDashRules.LAND || phase == FormDashRules.STALL || phase == FormDashRules.SPLASH || phase == FormDashRules.ABORT;
			// The last form's ending stays on show until this one starts; read from the first live phase on.
			if (phases.isEmpty() && (end || phase == FormDashRules.IDLE)) {
				trace.append(String.format("(%d f%d t%d c%.2f s%s @%d)", phase, (int) client[3], (int) client[4], client[1], server, (long) client[5]));
				context.waitTicks(1);
				continue;
			}
			if (phases.isEmpty() || phases.getLast() != phase) phases.add(phase);
			top[0] = Math.max(top[0], client[1]);
			trace.append(String.format("[%d c%.2f%s s%s]", phase, client[1], client[2] > 0 ? "g" : "", server));
			if (end) return phases;
			context.waitTicks(1);
		}
		throw new AssertionError("The aerial form never ended: " + phases + " trace " + trace);
	}
	/** Waits for the form's set; a refusal names itself from the overlay line and the server's own gates. */
	private static void started(ClientGameTestContext context, TestSingleplayerContext world, int form) {
		StringBuilder trace = new StringBuilder();
		for (int tick = 0; tick < 12; tick++) {
			var view = context.computeOnClient(mc -> FormDash.view(mc.player));
			trace.append('[').append(view.form()).append('/').append(view.phase()).append(" w").append(view.window()).append(']');
			if (view.form() == form && view.phase() == FormDashRules.SET) return;
			context.waitTicks(1);
		}
		String overlay = context.computeOnClient(mc -> {
			StringBuilder text = new StringBuilder();
			for (var field : net.minecraft.client.gui.Gui.class.getDeclaredFields()) {
				if (!net.minecraft.network.chat.Component.class.isAssignableFrom(field.getType())) continue;
				try { field.setAccessible(true); Object value = field.get(mc.gui); if (value != null) text.append(((net.minecraft.network.chat.Component) value).getString()).append(" | "); }
				catch (ReflectiveOperationException | RuntimeException ignored) { }
			}
			return text.toString();
		});
		String server = on(world, p -> String.format("able%s eligible%s aura%.1f rec%d now%d intent%s", FormDash.able(p, form), FormDash.eligible(p, form), Aura.aura(p),
			FormDash.data(p).recoveryUntil(), MasterForms.now(p), p.getLastClientMoveIntent()));
		throw new AssertionError("Follow-up " + form + " never set: " + trace + " overlay " + overlay + " server " + server);
	}
	private static RuneBolt bolt(ServerPlayer p, Vec3 offset) {
		var group = SpellCompiler.compile(List.of(Runes.BOLT, Runes.HARM)).root().groups.getFirst();
		Vec3 from = p.getBoundingBox().getCenter().add(offset);
		Vec3 toward = offset.scale(-1).normalize();
		RuneBolt bolt = RuneBolt.launch(new Cast(caster), group, null, from, toward, false);
		check(bolt != null && bolt.hostileSpellTo(p), "A real hostile bolt flies at the player");
		bolt.setDeltaMovement(toward.scale(.01));
		return bolt;
	}

	private static double difference(Path a, Path b) {
		try {
			BufferedImage x = ImageIO.read(a.toFile()), y = ImageIO.read(b.toFile());
			check(x.getWidth() == y.getWidth() && x.getHeight() == y.getHeight(), "Captures share a size");
			int w = x.getWidth(), h = x.getHeight(); double sum = 0; long n = 0;
			// The hand's corner, clear of the hotbar (hiding the HUD would hide the hand too).
			for (int py = h * 45 / 100; py < h; py += 2) for (int px = w * 74 / 100; px < w; px += 2) {
				int c = x.getRGB(px, py), d = y.getRGB(px, py);
				sum += Math.abs((c >> 16 & 255) - (d >> 16 & 255)) + Math.abs((c >> 8 & 255) - (d >> 8 & 255)) + Math.abs((c & 255) - (d & 255));
				n += 3;
			}
			return sum / Math.max(1, n);
		} catch (java.io.IOException e) { throw new RuntimeException(e); }
	}
	private static dev.wildercord.aura.world.Duelist teacher(ServerPlayer p, dev.wildercord.aura.BreathingMethod method, double z) {
		var teacher = AuraWorld.DUELIST.create(p.level(), EntitySpawnReason.COMMAND);
		check(teacher != null, "The teacher fixture exists");
		teacher.setMethod(method); teacher.setNoAi(true); teacher.snapTo(.5, 100, z, z > 0 ? 180 : 0, 0); p.level().addFreshEntity(teacher);
		return teacher;
	}
	private static void ready(TestSingleplayerContext world, int form) {
		on(world, p -> {
			var f = FormDash.data(p);
			p.setAttached(FormDash.PROGRESS, new FormDash.Progress(f.learned(), form, 0, 0, f.practiced()));
			p.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("gale", AuraRules.SOVEREIGN, 4500, 160, 0));
			return null;
		});
	}
	private static double aura(TestSingleplayerContext world) { return on(world, p -> (double) Aura.aura(p)); }
	private static void place(ServerPlayer p, double x, double y, double z) {
		p.teleportTo(p.level(), x, y, z, Set.of(), -90, 0, false); p.setDeltaMovement(Vec3.ZERO); p.fallDistance = 0;
	}
	private static <T> T on(TestSingleplayerContext world, Function<ServerPlayer, T> action) {
		return world.getServer().computeOnServer(server -> action.apply(server.getPlayerList().getPlayers().getFirst()));
	}
}
