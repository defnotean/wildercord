package dev.wildercord.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.Awakening;
import dev.wildercord.aura.MastersArtAnimation;
import dev.wildercord.aura.MastersStyleRules;
import dev.wildercord.aura.Stance;
import dev.wildercord.aura.StringReader;
import dev.wildercord.aura.Ways;
import dev.wildercord.client.combat.ArticulatedViewModel;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.gametest.perf.HudCapture;
import dev.wildercord.net.NotebookPayload;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Left and right arm, every GUI scale: the Wildercord HUDs (spell panel, aura strip, stance bar, sword strings, the way's
 * standards) stay inside the window and off the vanilla hotbar, its offhand slot and attack indicator; the spell wheel,
 * Cord, aura and rune notebook screens stay inside the window; and a left-handed windup is the right-handed one mirrored.
 * Bounds come from what the GUI actually extracts each frame ({@link HudCapture}), not from re-derived layout maths.
 */
public final class HudHandednessLayoutTest implements FabricClientGameTest {
	private static final int W = 1280, H = 960;
	private static final int[] SCALES = {1, 2, 3, 4, 0};
	private static final Set<String> HUDS = Set.of("SpellHud", "AuraHud", "StanceHud", "StringHud", "WayHud");
	private static final List<String> SCREENS = List.of("SpellWheelScreen", "CordScreen", "AuraScreen", "RuneNotebookScreen");

	private enum Phase { CORD, AURA_ALONE, STANCE }

	@Override
	public void runTest(ClientGameTestContext context) {
		CameraType camera = context.computeOnClient(mc -> mc.options.getCameraType());
		HumanoidArm hand = context.computeOnClient(mc -> mc.options.mainHand().get());
		int[] original = context.computeOnClient(mc -> new int[] {mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight(), mc.options.guiScale().get()});
		boolean hidden = context.computeOnClient(mc -> mc.gui.hud.isHidden());
		boolean fullscreen = context.computeOnClient(mc -> mc.options.fullscreen().get());
		AttackIndicatorStatus indicator = context.computeOnClient(mc -> mc.options.attackIndicator().get());
		MagicQuality.StringIndicator strings = MagicQuality.stringIndicator;
		List<String> failures = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(30);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("fill -6 99 -6 6 99 6 minecraft:stone_bricks");
			world.getServer().runCommand("fill -6 100 -6 6 104 6 minecraft:air");
			world.getServer().runCommand("time set 6000");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(server.overworld(), .5, 100, .5, Set.<Relative>of(), 0, 0, false);
			});
			context.runOnClient(HudHandednessLayoutTest::mirror);
			for (int scale : SCALES) for (HumanoidArm arm : HumanoidArm.values()) {
				context.runOnClient(mc -> {
					mc.getWindow().setWindowed(W, H);
					mc.options.guiScale().set(scale);
					mc.resizeGui();
					mc.options.mainHand().set(arm);
					mc.options.attackIndicator().set(AttackIndicatorStatus.HOTBAR);
					mc.options.broadcastOptions();
					mc.options.setCameraType(CameraType.FIRST_PERSON);
					if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
					mc.gui.setScreen(null);
					MagicQuality.stringIndicator = MagicQuality.StringIndicator.CROSSHAIR;
				});
				for (Phase phase : Phase.values()) {
					world.getServer().runOnServer(server -> prepare(server.getPlayerList().getPlayers().getFirst(), phase));
					context.waitTicks(6);
					context.runOnClient(mc -> {
						mc.player.resetAttackStrengthTicker();
						long now = mc.level.getGameTime();
						if (phase == Phase.CORD) StringHud.live(List.of(new StringReader.Stroke(now, 1, 0)), now + 400, 20);
					});
					String label = "gui" + scale + "/" + arm + "/" + phase;
					int[] gui = context.computeOnClient(mc -> new int[] {mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight()});
					Set<String> expect = context.computeOnClient(mc -> {
						Set<String> e = new LinkedHashSet<>();
						if (Spellbooks.tier(mc.player) != null) e.add("SpellHud");
						if (AuraHud.showing(mc.player)) e.add("AuraHud");
						if (phase == Phase.STANCE && !AuraHud.showing(mc.player) && mc.player.getAttached(Stance.STANCE) != null) e.add("StanceHud");
						if (phase == Phase.CORD && StringHud.showing()) e.add("StringHud");
						if (dev.wildercord.aura.Crossroads.state(mc.player) != null) e.add("WayHud");
						return e;
					});
					List<HudCapture.Rect> rects = capture(context);
					verify(label, rects, gui, expect, failures, seen);
					if (phase == Phase.CORD) for (String name : SCREENS) {
						context.runOnClient(mc -> mc.gui.setScreen(screen(name)));
						context.waitTicks(3);
						List<HudCapture.Rect> drawn = capture(context);
						context.runOnClient(mc -> mc.gui.setScreen(null));
						List<HudCapture.Rect> own = drawn.stream().filter(r -> r.owner().equals(name)).toList();
						if (own.isEmpty()) failures.add(label + " " + name + ": drew nothing");
						else seen.add(name);
						own.stream().filter(r -> outside(r, gui)).findFirst()
							.ifPresent(r -> failures.add(label + " " + name + " leaves the " + gui[0] + "x" + gui[1] + " window: " + r));
					}
				}
			}
			context.runOnClient(mc -> StringHud.lapsed());
			System.out.println("WILDERCORD_HUD_LAYOUT seen=" + seen + " failures=" + failures.size());
			for (String f : failures) System.out.println("WILDERCORD_HUD_LAYOUT_FAIL " + f);
			check(seen.containsAll(HUDS) && seen.containsAll(SCREENS), "every HUD and screen was drawn at least once: " + seen);
			check(failures.isEmpty(), failures.size() + " layout failures, first: " + failures.stream().limit(12).toList());
		} finally {
			HudCapture.on = false;
			MagicQuality.stringIndicator = strings;
			context.runOnClient(mc -> {
				mc.gui.setScreen(null);
				mc.options.setCameraType(camera);
				mc.options.mainHand().set(hand);
				mc.options.attackIndicator().set(indicator);
				mc.options.broadcastOptions();
				mc.getWindow().setWindowed(original[0], original[1]);
				mc.getWindow().setFullscreen(fullscreen);
				mc.options.guiScale().set(original[2]); mc.resizeGui();
				if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle();
			});
		}
	}

	private static void prepare(ServerPlayer player, Phase phase) {
		long now = player.level().getGameTime();
		player.setHealth(player.getMaxHealth());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
		player.removeAttached(Stance.STANCE);
		if (phase == Phase.STANCE) {
			player.removeAttached(AuraAttachments.AURA);
			player.removeAttached(AuraAttachments.STATE);
			Spellbooks.setCord(player, ItemStack.EMPTY);
			player.setAttached(Stance.STANCE, new Stance.State(.5F, now, 1, 0, now + 100_000, 0, 0));
			return;
		}
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("starlit", AuraRules.FORM, AuraRules.threshold(AuraRules.FORM), AuraRules.capacity(AuraRules.FORM), 0));
		player.removeAttached(AuraAttachments.STATE);
		player.removeAttached(Awakening.AWAKENING);
		if (phase == Phase.AURA_ALONE) {
			Spellbooks.setCord(player, ItemStack.EMPTY);
			return;
		}
		Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
		var book = Spellbooks.get(player);
		List<String> spell = List.of(rune("bolt"), rune("headlamp"));
		for (String id : spell) book = book.learn(id);
		Spellbooks.set(player, book.withSpell(0, spell));
		Spellbooks.setMana(player, 100);
		if (dev.wildercord.aura.Crossroads.state(player) == null) {
			Ways.set(player, "");
			AuraApi.openCrossroads(player);
		}
	}

	private static String rune(String id) {
		return Runes.get("wildercord:" + id).orElseThrow(() -> new AssertionError("Missing rune " + id)).id();
	}

	private static Screen screen(String name) {
		return switch (name) {
			case "SpellWheelScreen" -> new SpellWheelScreen(WildercordKeys.nextMapping());
			case "CordScreen" -> new CordScreen();
			case "AuraScreen" -> new AuraScreen(null);
			default -> new RuneNotebookScreen(new NotebookPayload(List.of(new NotebookPayload.Entry("x", 3)), 4, 2, true), null);
		};
	}

	/** What two frames' worth of GUI extraction drew, by owner. */
	private static List<HudCapture.Rect> capture(ClientGameTestContext context) {
		context.runOnClient(mc -> { synchronized (HudCapture.RECTS) { HudCapture.RECTS.clear(); } HudCapture.on = true; });
		context.waitTicks(2);
		return context.computeOnClient(mc -> {
			HudCapture.on = false;
			synchronized (HudCapture.RECTS) { List<HudCapture.Rect> copy = List.copyOf(HudCapture.RECTS); HudCapture.RECTS.clear(); return copy; }
		});
	}

	private static boolean outside(HudCapture.Rect r, int[] gui) {
		return r.x0() < 0 || r.y0() < 0 || r.x1() > gui[0] || r.y1() > gui[1];
	}

	private static void verify(String label, List<HudCapture.Rect> rects, int[] gui, Set<String> expect, List<String> failures, Set<String> seen) {
		List<HudCapture.Rect> hotbar = rects.stream().filter(r -> r.owner().equals("hotbar")).toList();
		if (hotbar.isEmpty()) failures.add(label + ": vanilla hotbar not captured");
		Set<String> reported = new LinkedHashSet<>();
		for (HudCapture.Rect r : rects) {
			if (!HUDS.contains(r.owner())) continue;
			seen.add(r.owner());
			if (outside(r, gui) && reported.add(r.owner() + "/out"))
				failures.add(label + " " + r.owner() + " leaves the " + gui[0] + "x" + gui[1] + " window: " + r);
			for (HudCapture.Rect h : hotbar) if (r.overlaps(h) && reported.add(r.owner() + "/hotbar"))
				failures.add(label + " " + r.owner() + " overlaps the hotbar: " + r + " on " + h);
		}
		for (String e : expect) if (rects.stream().noneMatch(r -> r.owner().equals(e))) failures.add(label + ": " + e + " expected but not drawn");
	}

	/** A left-handed windup and recovery is the right-handed one mirrored across the view: grip x flips, y and z hold. */
	private static void mirror(Minecraft mc) {
		ArticulatedViewModel model = new ArticulatedViewModel(false);
		int checked = 0;
		for (MastersStyleRules.Style style : MastersStyleRules.STYLES) {
			int end = style.windup() + style.recovery();
			for (float age = 0; age < end; age += .75F) {
				var right = ArticulatedCombatPose.samplePlayer(style.animation(), age, style.windup(), style.recovery(), false);
				var left = ArticulatedCombatPose.samplePlayer(style.animation(), age, style.windup(), style.recovery(), true);
				check(Math.abs(right.weight() - left.weight()) < 1e-4F, style.art() + " weighs the same in either hand at " + age);
				if (right.weight() == 0) continue;
				Vector3f r = grip(model, ArticulatedCombatPose.view(right, false), HumanoidArm.RIGHT);
				Vector3f l = grip(model, ArticulatedCombatPose.view(left, true), HumanoidArm.LEFT);
				check(Math.abs(l.x + r.x) < 1e-3F && Math.abs(l.y - r.y) < 1e-3F && Math.abs(l.z - r.z) < 1e-3F,
					style.art() + " left-handed grip mirrors the right at age " + age + ": " + l + " vs " + r);
				checked++;
			}
		}
		check(checked > 0, "some articulated styles were sampled");
		int legacy = 0;
		for (int move = 0; move < 128; move++) {
			if (!MastersArtAnimation.supports(move)) continue;
			for (float age = 0; age < 18; age += .75F) {
				MastersArtAnimation.Pose pose = MastersArtAnimation.sample(move, age, 6, 12);
				if (pose.weight() == 0) continue;
				var r = MastersArtAnimation.view(pose, false, .3F, 20, 10);
				var l = MastersArtAnimation.view(pose, true, .3F, -20, 10);
				var a = r.transform(); var b = l.transform();
				check(near(b.x(), -a.x()) && near(b.y(), a.y()) && near(b.z(), a.z()) && near(b.pitch(), a.pitch())
					&& near(b.yaw(), -a.yaw()) && near(b.roll(), -a.roll()) && near(l.grip().x(), -r.grip().x())
					&& near(l.grip().y(), r.grip().y()) && near(l.grip().z(), r.grip().z()),
					"move " + move + " left-handed windup hand mirrors the right at age " + age + ": " + l + " vs " + r);
				legacy++;
			}
		}
		check(legacy > 0, "some first-person art motions were sampled");
		System.out.println("WILDERCORD_HUD_MIRROR articulated=" + checked + " firstPerson=" + legacy);
	}

	private static Vector3f grip(ArticulatedViewModel model, ArticulatedCombatPose.ViewPose view, HumanoidArm arm) {
		model.setupAnim(new ArticulatedViewModel.Frame(view, true, true));
		PoseStack stack = new PoseStack();
		model.rig().socket(arm, stack);
		return stack.last().pose().transformPosition(new Vector3f());
	}

	private static boolean near(float a, float b) {
		return Math.abs(a - b) < 1e-4F;
	}

	private static void check(boolean ok, String message) {
		if (!ok) throw new AssertionError(message);
	}
}
