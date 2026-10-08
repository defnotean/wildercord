package dev.wildercord.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.ArtRules;
import dev.wildercord.aura.AuraRules;
import dev.wildercord.aura.AuraGuard;
import dev.wildercord.aura.EarnedCounterCaptureFixture;
import dev.wildercord.aura.SwordString;
import dev.wildercord.aura.Momentum;
import dev.wildercord.aura.MastersArts;
import dev.wildercord.aura.MastersStyleRules;
import dev.wildercord.client.AuraScreen;
import dev.wildercord.client.MastersArtsClient;
import dev.wildercord.client.MastersArtPose;
import dev.wildercord.client.MastersHandMotionState;
import dev.wildercord.client.SwordStringsClient;
import dev.wildercord.client.WildercordKeys;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Real combat keys, accepted network timelines and the registered player rig, with native first-
 * and third-person screenshots. No direct pose packet or animation-clock injection is used.
 */
public final class WildercordMastersArtsPresentationTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		CameraType camera = context.computeOnClient(mc -> mc.options.getCameraType());
		HumanoidArm mainHand = context.computeOnClient(mc -> mc.options.mainHand().get());
		int scale = context.computeOnClient(mc -> mc.options.guiScale().get());
		int[] size = context.computeOnClient(mc -> new int[] {mc.getWindow().getWidth(), mc.getWindow().getHeight()});
		boolean hidden = context.computeOnClient(mc -> mc.gui.hud.isHidden());
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitTicks(40);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("time set 3000");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("fill -10 99 -10 10 99 40 minecraft:stone_bricks");
			world.getServer().runCommand("fill -10 100 -10 10 108 40 minecraft:air");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(server.overworld(), .5, 100, .5, Set.<Relative>of(), 20, 0, false);
				prepare(player);
			});
			context.runOnClient(mc -> {
				mc.getWindow().setWindowed(1280, 720);
				mc.options.guiScale().set(2);
				mc.resizeGui();
				mc.gui.setScreen(null);
				mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
				if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
			});
			context.waitTicks(10);
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			context.runOnClient(MastersFirstPersonMotionProbe::verify);
			controls(context);
			menusDiscardQueuedInput(context, world);
			holdDoesNotRepeat(context, world);
			for (int move = 0; move < 3; move++) {
				capture(context, world, move, CameraType.THIRD_PERSON_BACK, "third_back");
				capture(context, world, move, CameraType.FIRST_PERSON, "first");
			}
			cancelledWindup(context, world);
			for (float turn : new float[] {-90, 90, 180}) turnDuringWindup(context, world, turn);
			nearVerticalCommit(context, world);
			for (var style : MastersStyleRules.STYLES) {
				captureStyle(context, world, style, CameraType.THIRD_PERSON_BACK, "third_back");
				captureStyle(context, world, style, CameraType.FIRST_PERSON, "first");
				if (ArtRules.art(style.art()).slot() == 1 || ArtRules.art(style.art()).slot() == 3 || ArtRules.art(style.art()).slot() == 4
					|| style.targets() == MastersStyleRules.TargetPolicy.EARNED_COUNTER) {
					captureStyle(context, world, style, CameraType.THIRD_PERSON_BACK, "left_turn_third_back", true, false);
					captureStyle(context, world, style, CameraType.FIRST_PERSON, "left_turn_first", true, false);
					captureStyle(context, world, style, CameraType.FIRST_PERSON, "cancelled", false, true);
				}
			}
			for (int gui = 1; gui <= 4; gui++) {
				final int guiScale = gui;
				context.runOnClient(mc -> {
					mc.options.guiScale().set(guiScale);
					mc.resizeGui();
					mc.gui.setScreen(new AuraScreen(null));
				});
				context.waitTicks(2);
				double[] point = context.computeOnClient(mc -> ((AuraScreen) mc.gui.screen()).mastersHelpPoint());
				int physicalScale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
				context.getInput().setCursorPos(point[0] * physicalScale, point[1] * physicalScale);
				shot(context, "masters_arts_help_scale_" + gui);
			}
		} finally {
			context.runOnClient(mc -> {
				mc.gui.setScreen(null);
				mc.options.setCameraType(camera);
				mc.options.mainHand().set(mainHand);
				mc.options.broadcastOptions();
				mc.options.guiScale().set(scale);
				mc.getWindow().setWindowed(size[0], size[1]);
				mc.resizeGui();
				if (mc.gui.hud.isHidden() != hidden) mc.gui.hud.toggle();
				for (int move = 0; move < 3; move++) MastersArtsClient.mapping(move).setDown(false);
			});
			context.getInput().releaseKey(o -> o.keyShift);
			context.getInput().releaseKey(o -> o.keyJump);
		}
	}

	private static void controls(ClientGameTestContext context) {
		context.runOnClient(mc -> {
			int[] defaults = {InputConstants.KEY_U, InputConstants.KEY_Y, InputConstants.KEY_J};
			for (int move = 0; move < 3; move++) {
				KeyMapping key = MastersArtsClient.mapping(move);
				check(key != null && key.getDefaultKey().getValue() == defaults[move], "The move has its distinct default combat key");
				check(key.getCategory().id().equals(Wildercord.id("masters_arts")), "The dedicated category is registered");
				for (KeyMapping other : mc.options.keyMappings) {
					if (other.getName().startsWith("key.debug.") || other.getName().equals("key.quickActions")) continue;
					check(other == key || !other.getDefaultKey().equals(key.getDefaultKey()), "No default binding conflict with " + other.getName());
				}
				check(!MastersArtsClient.help().get(1 + move * 2).getString().contains("screen.wildercord"), "Translated key help loads");
			}
			KeyMapping key = MastersArtsClient.mapping(0);
			var before = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(key);
			try {
				int probe = before.getValue() == InputConstants.KEY_N ? InputConstants.KEY_M : InputConstants.KEY_N;
				key.setKey(InputConstants.Type.KEYBOARD.getOrCreate(probe));
				check(!net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.getBoundKeyOf(key).equals(before), "The help probe actually changes the combat binding");
				KeyMapping.resetMapping();
				check(MastersArtsClient.help().get(1).getString().contains(key.getTranslatedKeyMessage().getString()), "Help follows an actual rebind");
			} finally {
				key.setKey(before);
				KeyMapping.resetMapping();
			}
		});
	}

	private static void menusDiscardQueuedInput(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> {
			mc.gui.setScreen(new AuraScreen(null));
			KeyMapping.click(MastersArtsClient.mapping(0).getDefaultKey());
		});
		context.waitTicks(3);
		context.runOnClient(mc -> mc.gui.setScreen(null));
		context.waitTicks(3);
		world.getServer().runOnServer(server -> check(!MastersArts.committed(server.getPlayerList().getPlayers().getFirst()),
			"A queued menu press is discarded instead of attacking after the menu closes"));
	}

	private static void capture(ClientGameTestContext context, TestSingleplayerContext world, int move, CameraType camera, String view) {
		context.waitTicks(105);
		world.getServer().runOnServer(server -> prepare(server.getPlayerList().getPlayers().getFirst()));
		context.runOnClient(mc -> {
			mc.options.setCameraType(camera);
			mc.gui.toastManager().clear();
			mc.gui.hud.getChat().clearMessages(false);
		});
		context.waitTicks(3);
		context.getInput().pressKey(MastersArtsClient.mapping(move));
		context.waitFor(mc -> MastersArtsClient.timeline(mc.player) != null, 30);
		check(context.computeOnClient(mc -> MastersArtsClient.timeline(mc.player).move() == move), "The real input reaches its requested server timeline");
		String prefix = "masters_art_" + move + "_" + view;
		captureBeats(context, prefix);
		context.waitTicks(30);
		check(context.computeOnClient(mc -> MastersArtsClient.timeline(mc.player) == null && MastersArtsClient.pose(mc.player, .5F).weight() == 0),
			"The native animation returns to vanilla after recovery");
		shot(context, prefix + "_settled");
	}

	private static void holdDoesNotRepeat(ClientGameTestContext context, TestSingleplayerContext world) {
		context.getInput().holdKey(MastersArtsClient.mapping(0));
		try {
			context.waitFor(mc -> MastersArtsClient.timeline(mc.player) != null, 30);
			context.waitTicks(70);
			world.getServer().runOnServer(server -> check(!MastersArts.committed(server.getPlayerList().getPlayers().getFirst()),
				"Holding the combat key does not auto-spend aura again when its 60-tick cooldown expires"));
		} finally {
			context.getInput().releaseKey(MastersArtsClient.mapping(0));
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static boolean inspect(ClientGameTestContext context) {
		return inspect(context, false);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static boolean inspect(ClientGameTestContext context, boolean lateMoonRecovery) {
		return context.computeOnClient(mc -> {
			var timeline = MastersArtsClient.timeline(mc.player);
			var expected = MastersArtsClient.pose(mc.player, .5F);
			if (timeline == null) return false;
			float age = mc.level.getGameTime() - timeline.startTick() + .5F;
			// Moon deliberately captures its late settle at 27.5/30. The unchanged
			// cubic fade is 0.06561279 there, below the other phases' admission floor.
			boolean moonSettle = lateMoonRecovery && timeline.move() == 19
				&& timeline.windup() == 10 && timeline.recovery() == 20 && age == 27.5F;
			if (moonSettle) check(Float.isFinite(expected.weight()) && expected.weight() > 0
				&& Math.abs(expected.weight() - .06561279F) < .000001F,
				"Moon's exact late-recovery sample retains its positive authored fade");
			else if (expected.weight() < .15F) return false;
			AvatarRenderer renderer = (AvatarRenderer) mc.getEntityRenderDispatcher().getRenderer(mc.player);
			AvatarRenderState state = (AvatarRenderState) renderer.createRenderState(mc.player, .5F);
			PlayerModel model = (PlayerModel) renderer.getModel();
			model.setupAnim(state);
			boolean left = mc.player.getMainArm() == HumanoidArm.LEFT;
			var sword = left ? model.leftArm : model.rightArm;
			check(Math.abs(sword.xRot) + Math.abs(sword.yRot) + Math.abs(sword.zRot) > .03F, "The real player sword arm moves");
			check(Math.abs(model.body.yRot) > .005F || Math.abs(model.body.xRot) > .005F, "The real torso participates");
			check(Math.abs(model.leftLeg.xRot) > .005F || Math.abs(model.rightLeg.xRot) > .005F, "The real footwork participates");
			float yaw = mc.player.getYRot(), pitch = mc.player.getXRot();
			PoseStack main = new PoseStack(), off = new PoseStack();
			MastersArtPose.firstPerson(main, InteractionHand.MAIN_HAND, state, 0);
			MastersArtPose.firstPerson(off, InteractionHand.OFF_HAND, state, 0);
			check(main.last().pose().isFinite() && !main.last().pose().equals(off.last().pose()), "The real first-person main hand receives a finite authored transform");
			check(off.last().pose().equals(new org.joml.Matrix4f()), "The offhand stays independent of the main-hand art");
			check(mc.player.getYRot() == yaw && mc.player.getXRot() == pitch, "Rendering the body and hand never steers the camera");
			if (timeline.move() == 14) {
				PoseStack held = new PoseStack();
				var grip = new org.joml.Vector3f(0, -1.327F / 16, 1.439F / 16);
				MastersArtPose.heldSword(state, state.mainArm, new ItemStack(Items.DIAMOND_SWORD), held);
				var moved = held.last().pose().transformPosition(new org.joml.Vector3f(grip));
				check(moved.distance(grip) < .0001F, "Blossom's ground-facing finish rotates about its actual held-sword hilt");
			}
			check(model.body.getChild("jacket") == model.jacket && Math.abs(model.jacket.yRot - model.jacket.getInitialPose().yRot()) < .0001F,
				"The outer skin inherits its parent torso without double rotation");
			check(model.rightArm.getChild("right_sleeve") == model.rightSleeve && Math.abs(model.rightSleeve.xRot - model.rightSleeve.getInitialPose().xRot()) < .0001F,
				"The outer sleeve inherits its parent arm without double rotation");

			if (timeline.move() == 13 || timeline.move() == 14) {
				for (boolean slim : new boolean[] {false, true}) {
					PlayerModel variant = new PlayerModel(mc.getEntityModels().bakeLayer(slim
						? net.minecraft.client.model.geom.ModelLayers.PLAYER_SLIM : net.minecraft.client.model.geom.ModelLayers.PLAYER), slim);
					variant.setupAnim(state);
					var hand = left ? variant.leftArm : variant.rightArm;
					float x = hand.x, y = hand.y, z = hand.z, rx = hand.xRot, ry = hand.yRot, rz = hand.zRot;
					variant.setupAnim(state);
					check(Math.abs(hand.x - x) + Math.abs(hand.y - y) + Math.abs(hand.z - z)
						+ Math.abs(hand.xRot - rx) + Math.abs(hand.yRot - ry) + Math.abs(hand.zRot - rz) < .0001F,
						"Repeated second-form render passes never accumulate transforms on either skin rig");
					check(variant.leftArm.getChild("left_sleeve") == variant.leftSleeve
						&& Math.abs(variant.leftSleeve.xRot - variant.leftSleeve.getInitialPose().xRot()) < .0001F,
						"Both skin rigs inherit the mirrored limb's outer layer without double application");
				}
			}

			Wildercord.LOGGER.info("MASTERS_ART_FRAME move={} age={} weight={} body=({},{},{}) sword=({},{},{}) legs=({},{}) camera={}",
				timeline.move(), mc.level.getGameTime() - timeline.startTick(), expected.weight(), model.body.xRot, model.body.yRot, model.body.zRot,
				sword.xRot, sword.yRot, sword.zRot, model.leftLeg.xRot, model.rightLeg.xRot, mc.options.getCameraType());
			return true;
		});
	}

	private static void cancelledWindup(ClientGameTestContext context, TestSingleplayerContext world) {
		context.waitTicks(105);
		world.getServer().runOnServer(server -> prepare(server.getPlayerList().getPlayers().getFirst()));
		context.getInput().pressKey(MastersArtsClient.mapping(1));
		context.waitFor(mc -> MastersArtsClient.timeline(mc.player) != null, 30);
		world.getServer().runOnServer(server -> MastersArts.cancel(server.getPlayerList().getPlayers().getFirst()));
		context.waitFor(mc -> MastersArtsClient.timeline(mc.player) == null, 20);
		check(context.computeOnClient(mc -> MastersArtsClient.pose(mc.player, .5F).weight() == 0), "Authoritative cancellation removes the body and hand pose");
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void turnDuringWindup(ClientGameTestContext context, TestSingleplayerContext world, float turn) {
		context.waitTicks(105);
		world.getServer().runCommand("fill 0 99 1 0 99 6 minecraft:gold_block");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			prepare(player);
			player.teleportTo(player.level(), .5, 100, .5, Set.<Relative>of(), 0, 0, false);
		});
		context.waitTicks(4);
		context.getInput().pressKey(MastersArtsClient.mapping(1));
		context.waitFor(mc -> MastersArtsClient.timeline(mc.player) != null, 30);
		context.runOnClient(mc -> {
			mc.player.setYRot(turn);
			mc.player.setYHeadRot(turn);
			mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
		});
		context.waitFor(mc -> MastersArtsClient.pose(mc.player, .5F).weight() > .999F, 10);
		context.runOnClient(mc -> {
			var timeline = MastersArtsClient.timeline(mc.player);
			check(timeline != null, "Turn regression observes a live accepted move");
			AvatarRenderer renderer = (AvatarRenderer) mc.getEntityRenderDispatcher().getRenderer(mc.player);
			AvatarRenderState state = (AvatarRenderState) renderer.createRenderState(mc.player, .5F);
			check(Math.abs(net.minecraft.util.Mth.wrapDegrees(state.bodyRot - timeline.yaw())) < 1,
				"Body remains aligned to the accepted strike after the free-look turn");
			check(Math.abs(net.minecraft.util.Mth.wrapDegrees(mc.player.getYRot() - turn)) < 1, "The art never steers the camera back");
			check(Math.abs(state.yRot) > 45 && Math.abs(state.yRot) <= 75, "The head turns naturally without following the camera through an impossible twist");
			PlayerModel model = (PlayerModel) renderer.getModel();
			model.setupAnim(state);
			check(Math.abs(model.head.yRot - model.body.yRot) <= Math.toRadians(75) + .001,
				"The final neck angle remains bounded relative to the authored torso");
		});
		shot(context, "masters_committed_turn_" + (int) turn + "_third_back_body_and_trail");
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			if (mc.gui.hud.isHidden()) mc.gui.hud.toggle();
		});
		shot(context, "masters_committed_turn_" + (int) turn + "_first_weapon_and_hint");
		context.waitTicks(30);
	}

	private static void nearVerticalCommit(ClientGameTestContext context, TestSingleplayerContext world) {
		context.waitTicks(105);
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			prepare(player);
			player.teleportTo(player.level(), .5, 100, .5, Set.<Relative>of(), 90, 90, false);
		});
		context.waitTicks(4);
		context.getInput().pressKey(MastersArtsClient.mapping(0));
		context.waitFor(mc -> MastersArtsClient.timeline(mc.player) != null, 30);
		context.runOnClient(mc -> {
			var timeline = MastersArtsClient.timeline(mc.player);
			check(Math.abs(net.minecraft.util.Mth.wrapDegrees(timeline.yaw() - 90)) < 1,
				"A vertical look retains the accepted yaw rather than a fixed world-axis fallback");
			check(timeline.pitch() == 0, "A shared melee art advertises its actual level hit plane");
		});
		shot(context, "masters_vertical_look_level_commit");
		context.waitTicks(30);
	}

	/** Uses each registered family's real ordinary input, including a genuinely earned low counter. */
	private static void captureStyle(ClientGameTestContext context, TestSingleplayerContext world, MastersStyleRules.Style style,
			CameraType camera, String view) {
		captureStyle(context, world, style, camera, view, false, false);
	}

	private static void captureStyle(ClientGameTestContext context, TestSingleplayerContext world, MastersStyleRules.Style style,
			CameraType camera, String view, boolean leftHanded, boolean cancel) {
		int slot = ArtRules.art(style.art()).slot();
		boolean second = slot == 1, fourth = slot == 3, finalArt = slot == 4;
		boolean counter = slot == 2 && style.targets() == MastersStyleRules.TargetPolicy.EARNED_COUNTER;
		check(slot == 0 || second || fourth || finalArt || counter, "The capture declares its supported input family");
		if (counter) {
			captureEarnedCounterStyle(context, world, style, camera, view, leftHanded, cancel);
			return;
		}
		context.getInput().releaseKey(o -> o.keyShift);
		context.waitTicks(finalArt ? ArtRules.art(style.art()).cooldown() + 5 : 105);
		Mob[] target = new Mob[1];
		world.getServer().runOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			prepare(player);
			player.teleportTo(server.overworld(), .5, 100, .5, Set.<Relative>of(), 0, 3, false);
			player.setDeltaMovement(Vec3.ZERO);
			player.setAttached(AuraAttachments.AURA, finalArt
				? new AuraAttachments.Data(ArtRules.art(style.art()).method(), AuraRules.SOVEREIGN,
					AuraRules.threshold(AuraRules.SOVEREIGN), AuraRules.capacity(AuraRules.SOVEREIGN), 0)
				: new AuraAttachments.Data(ArtRules.art(style.art()).method(), 4, 1800, 100, 0));
			if (finalArt) {
				player.setHealth(player.getMaxHealth());
				player.setAttached(Momentum.MOMENTUM, new Momentum.State(100, player.level().getGameTime() + 100000, 0, 0, 0));
			}
			Mob foe = EntityTypes.HUSK.create(player.level(), EntitySpawnReason.COMMAND);
			check(foe != null, "Actual style target exists");
			foe.addTag("wildercord.rolled");
			foe.setNoAi(true);
			foe.setNoGravity(true);
			foe.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
			foe.setHealth(200);
			foe.snapTo(fourth ? 2.5 : .5, 100, fourth ? 28.0 : 3.1, 180, 0);
			player.level().addFreshEntity(foe);
			target[0] = foe;
		});
		context.runOnClient(mc -> {
			mc.options.setCameraType(CameraType.FIRST_PERSON);
			mc.options.mainHand().set(leftHanded ? HumanoidArm.LEFT : HumanoidArm.RIGHT);
			mc.options.broadcastOptions();
		});
		context.waitTicks(15);
		if (fourth) {
			context.getInput().pressKey(WildercordKeys.auraMapping());
			context.waitTicks(2);
			context.getInput().pressKey(WildercordKeys.auraMapping());
			context.waitTicks(5);
			context.getInput().pressKey(o -> o.keyAttack);
		} else {
			if (second) {
				context.runOnClient(mc -> mc.player.setXRot(25));
				String beforeJump = jumpState(context);
				// Fabric pressKey releases before its waitTick; jump needs a held key during input polling.
				context.getInput().holdKey(o -> o.keyJump);
				try {
					context.waitTicks(1); // Preserve the tick previously spent inside pressKey.
					context.waitFor(mc -> !mc.player.onGround(), 20);
				} catch (AssertionError failure) {
					throw new AssertionError("Actual " + style.art() + " leap did not leave the stage (view=" + view
						+ ", leftHanded=" + leftHanded + "): before={" + beforeJump + "}, after={" + jumpState(context) + "}", failure);
				} finally {
					context.getInput().releaseKey(o -> o.keyJump);
				}
				context.waitTicks(1);
			}
			for (int swing = 0; swing < (second ? 1 : finalArt ? 3 : 2); swing++) {
				if (finalArt) context.waitFor(mc -> mc.player.getAttackStrengthScale(0) >= .999F, 40);
				context.getInput().pressKey(o -> o.keyAttack);
				context.waitTicks(2);
				world.getServer().runOnServer(server -> {
					target[0].snapTo(.5, 100, 3.1, 180, 0);
					target[0].setDeltaMovement(Vec3.ZERO);
				});
				context.waitTicks(12);
			}
			context.getInput().holdKey(o -> o.keyShift);
			context.waitTicks(2);
			context.getInput().pressKey(o -> o.keyAttack);
			context.getInput().releaseKey(o -> o.keyShift);
		}
		context.runOnClient(mc -> mc.options.setCameraType(camera));
		context.waitFor(mc -> MastersArtsClient.timeline(mc.player) != null
			&& MastersArtsClient.timeline(mc.player).move() == style.animation(), 30);
		String prefix = "masters_style_" + style.art() + "_" + view;
		if (cancel) {
			world.getServer().runOnServer(server -> MastersArts.cancel(server.getPlayerList().getPlayers().getFirst()));
			context.waitFor(mc -> MastersArtsClient.timeline(mc.player) == null, 20);
			check(context.computeOnClient(mc -> MastersArtsClient.pose(mc.player, .5F).weight() == 0), "Cancelled authored body and hand poses clear together");
			waitForCancelledNeutral(context, prefix);
			shot(context, prefix + "_neutral");
			world.getServer().runOnServer(server -> target[0].discard());
			return;
		}
		context.runOnClient(mc -> {
			check(mc.player.getMainArm() == (leftHanded ? HumanoidArm.LEFT : HumanoidArm.RIGHT), "The real player's selected hand is in effect");
			if (leftHanded) {
				mc.player.setYRot(90); mc.player.setYHeadRot(90);
				mc.player.setXRot(camera.isFirstPerson() ? 75 : 12);
			}
		});
		captureBeats(context, prefix);
		context.waitTicks(30);
		check(context.computeOnClient(mc -> MastersArtsClient.timeline(mc.player) == null), "Style returns to vanilla after its real recovery");
		shot(context, prefix + "_settled");
		world.getServer().runOnServer(server -> target[0].discard());
	}

	private static void captureEarnedCounterStyle(ClientGameTestContext context, TestSingleplayerContext world,
			MastersStyleRules.Style style, CameraType camera, String view, boolean leftHanded, boolean cancel) {
		context.getInput().releaseKey(o -> o.keyShift);
		context.waitTicks(105);
		boolean toggleCrouch = context.computeOnClient(mc -> mc.options.toggleCrouch().get());
		EarnedCounterCaptureFixture fixture = world.getServer().computeOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			prepare(player);
			player.teleportTo(server.overworld(), .5, 100, .5, Set.<Relative>of(), 0, 8, false);
			player.setDeltaMovement(Vec3.ZERO);
			player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data(ArtRules.art(style.art()).method(), 4, 1800, 100, 0));
			return new EarnedCounterCaptureFixture(player, style);
		});
		try {
			context.runOnClient(mc -> {
				mc.options.toggleCrouch().set(false);
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				mc.options.mainHand().set(leftHanded ? HumanoidArm.LEFT : HumanoidArm.RIGHT); mc.options.broadcastOptions();
				mc.gui.toastManager().clear(); mc.gui.hud.getChat().clearMessages(false);
			});
			context.waitTicks(15);
			context.getInput().holdKey(o -> o.keyShift);
			context.waitTicks(2);
			context.getInput().pressKey(WildercordKeys.auraMapping());
			for (int t = 0; t < 4 && !world.getServer().computeOnServer(server -> AuraGuard.perfectNow(server.getPlayerList().getPlayers().getFirst())); t++) {
				context.waitTicks(1);
			}
			world.getServer().runOnServer(server -> fixture.catchBlow());
			context.waitFor(mc -> SwordString.Token.COUNTER.fits(SwordStringsClient.cueMarks(mc.level.getGameTime())), 8);
			int asked = context.computeOnClient(mc -> SwordStringsClient.counts()[0]);
			context.getInput().pressKey(o -> o.keyAttack);
			context.runOnClient(mc -> {
				check(SwordStringsClient.counts()[0] == asked + 1 && style.art().equals(SwordStringsClient.lastAsked()),
					"Actual ordinary attack input asks for the earned counter exactly once");
				mc.options.setCameraType(camera);
			});
			context.waitFor(mc -> MastersArtsClient.timeline(mc.player) != null
				&& MastersArtsClient.timeline(mc.player).move() == style.animation(), 30);
			String prefix = "masters_style_" + style.art() + "_" + view;
			context.runOnClient(mc -> {
				check(mc.player.getMainArm() == (leftHanded ? HumanoidArm.LEFT : HumanoidArm.RIGHT), "The earned counter uses the actual selected main hand");
				if (leftHanded) { mc.player.setYRot(90); mc.player.setYHeadRot(90); mc.player.setXRot(camera.isFirstPerson() ? 75 : 12); }
			});
			if (cancel) {
				world.getServer().runOnServer(server -> MastersArts.cancel(server.getPlayerList().getPlayers().getFirst()));
				context.waitFor(mc -> MastersArtsClient.timeline(mc.player) == null, 20);
				waitForCancelledNeutral(context, prefix); shot(context, prefix + "_neutral"); context.waitTicks(30);
				world.getServer().runOnServer(server -> fixture.verifyCancelled()); return;
			}
			captureBeats(context, prefix);
			context.waitTicks(30);
			world.getServer().runOnServer(server -> fixture.verify());
			check(context.computeOnClient(mc -> MastersArtsClient.timeline(mc.player) == null), "Earned counter returns to vanilla after its real recovery");
			context.getInput().releaseKey(o -> o.keyShift);
			shot(context, prefix + "_settled");
		} finally {
			context.getInput().releaseKey(o -> o.keyShift);
			context.runOnClient(mc -> mc.options.toggleCrouch().set(toggleCrouch));
			world.getServer().runOnServer(server -> fixture.close());
		}
	}

	/** Cancellation clears only the art; let the triggering vanilla swing and item dip finish naturally. */
	private static void waitForCancelledNeutral(ClientGameTestContext context, String prefix) {
		var hands = new FirstPersonHandsAndItemsRenderState();
		String[] observed = {"not sampled"};
		try {
			context.waitFor(mc -> {
				check(MastersArtsClient.timeline(mc.player) == null && MastersArtsClient.pose(mc.player, MastersCaptureProbe.PARTIAL).weight() == 0,
					"Cancelled second-form timeline and pose remain clear while ordinary hand motion settles");
				mc.player.firstPersonHandsAndItems().extractRenderState(mc.player, MastersCaptureProbe.PARTIAL, hands);
				boolean swinging = mc.player.isSwinging();
				float attack = mc.player.getSwingAnimation(MastersCaptureProbe.PARTIAL);
				boolean equipping = ((MastersHandMotionState) hands).wildercord$mainHandEquipping();
				observed[0] = "swinging=" + swinging + ", attack=" + attack + ", previousHeight=" + hands.oldMainHandHeight
					+ ", currentHeight=" + hands.mainHandHeight + ", swapScale=" + hands.mainHandSwapScale + ", genuineEquip=" + equipping;
				// Both native height samples must be full: the screenshot interpolates between them.
				return !swinging && attack == 0 && hands.oldMainHandHeight == 1 && hands.mainHandHeight == 1
					&& hands.mainHandSwapScale == 1 && !equipping;
			}, 30);
		} catch (AssertionError failure) {
			throw new AssertionError("Cancelled art did not reach native neutral hand state: " + prefix + " {" + observed[0] + "}", failure);
		}
		Wildercord.LOGGER.info("MASTERS_CANCELLED_NEUTRAL_READY name={} state={}", prefix + "_neutral", observed[0]);
	}

	private static String jumpState(ClientGameTestContext context) {
		return context.computeOnClient(mc -> "onGround=" + mc.player.onGround() + ", position=" + mc.player.position()
			+ ", velocity=" + mc.player.getDeltaMovement() + ", jumpDown=" + mc.options.keyJump.isDown()
			+ ", jumpInput=" + mc.player.input.keyPresses.jump() + ", flying=" + mc.player.getAbilities().flying
			+ ", screen=" + mc.gui.screen());
	}

	private static void prepare(ServerPlayer player) {
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("stone", 4, 1800, 100, 0));
		player.inventoryMenu.broadcastChanges();
	}

	/** Samples real accepted time, never a loop index presented as an active/recovery beat. */
	private static void captureBeats(ClientGameTestContext context, String prefix) {
		if (context.computeOnClient(mc -> MastersArtsClient.timeline(mc.player).move() == 19)) {
			captureMoonBeats(context, prefix); return;
		}
		context.runOnClient(mc -> {
			if (!mc.options.getCameraType().isFirstPerson()) mc.player.setXRot(12);
		});
		context.waitFor(mc -> {
			var move = MastersArtsClient.timeline(mc.player);
			return move != null && mc.level.getGameTime() - move.startTick() >= Math.max(1, move.windup() / 2);
		}, 30);
		var windup = phaseShot(context, prefix, "windup");
		context.waitFor(mc -> {
			var move = MastersArtsClient.timeline(mc.player);
			return move != null && mc.level.getGameTime() - move.startTick() >= move.windup();
		}, 30);
		var active = phaseShot(context, prefix, "active");
		context.waitFor(mc -> {
			var move = MastersArtsClient.timeline(mc.player);
			return move != null && mc.level.getGameTime() - move.startTick() >= move.windup() + move.recovery() / 2;
		}, 30);
		var recovery = phaseShot(context, prefix, "recovery");
		// GPU readback may advance ticks; finish all phase renders before waiting for file writes.
		awaitShots(context, CompletableFuture.allOf(windup, active, recovery));
	}

	/** Label the physical release separately from the distance-delayed damage. */
	private static void captureMoonBeats(ClientGameTestContext context, String prefix) {
		java.util.List<CompletableFuture<Void>> frames = new java.util.ArrayList<>();
		int[] ticks = {6, 10, 14, 27};
		String[] names = {"windup", "release", "delayed_damage", "recovery"};
		for (int i = 0; i < ticks.length; i++) {
			int wanted = ticks[i];
			context.waitFor(mc -> {
				var move = MastersArtsClient.timeline(mc.player);
				return move != null && mc.level.getGameTime() - move.startTick() >= wanted;
			}, 35);
			frames.add(phaseShot(context, prefix, names[i]));
		}
		awaitShots(context, CompletableFuture.allOf(frames.toArray(CompletableFuture[]::new)));
	}

	private static CompletableFuture<Void> phaseShot(ClientGameTestContext context, String prefix, String phase) {
		boolean pair = prefix.startsWith("masters_style_unmoved_") || prefix.startsWith("masters_style_null_parry_");
		if (!pair) return phaseShotReady(context, prefix, phase);
		var result = new java.util.concurrent.atomic.AtomicReference<CompletableFuture<Void>>();
		OpeningCaptureWait.withCleanup(() -> {
			var snapshot = context.computeOnClient(mc -> {
				boolean held = dev.wildercord.client.fx.HitStop.holding();
				long bound = java.util.Arrays.stream(dev.wildercord.aura.AuraFxRules.Weight.values())
					.mapToInt(weight -> dev.wildercord.aura.AuraFxRules.hitStop(weight, 1)).max().orElseThrow() * 1_000_000L;
				return new OpeningCaptureWait.Snapshot(pairCaptureIdentity(mc), held, System.nanoTime() + (held ? bound : 0));
			});
			// Let the real wall-clock hold expire without advancing game time or replacing a pose.
			OpeningCaptureWait.await(snapshot);
			context.runOnClient(mc -> OpeningCaptureWait.requireReady(snapshot, pairCaptureIdentity(mc), dev.wildercord.client.fx.HitStop.holding()));
			result.set(phaseShotReady(context, prefix, phase));
		});
		return result.get();
	}

	private static OpeningCaptureWait.Identity pairCaptureIdentity(net.minecraft.client.Minecraft mc) {
		var accepted = MastersArtsClient.timeline(mc.player);
		check(accepted != null, "Pair phase requires the same authentic accepted timeline");
		return new OpeningCaptureWait.Identity(mc.player.getUUID(), mc.player.getId(), accepted.entity(), accepted.move(), accepted.startTick(), mc.level.getGameTime());
	}

	private static CompletableFuture<Void> phaseShotReady(ClientGameTestContext context, String prefix, String phase) {
		context.runOnClient(mc -> {
			var move = MastersArtsClient.timeline(mc.player);
			check(move != null, "A live accepted timeline owns " + prefix + "_" + phase);
			float age = mc.level.getGameTime() - move.startTick() + MastersCaptureProbe.PARTIAL;
			boolean correct = switch (phase) {
				case "windup" -> age >= 0 && age < move.windup();
				case "active" -> age >= move.windup() && age < move.windup() + 2;
				case "release" -> move.move() == 19 && age >= 10 && age < 11;
				case "delayed_damage" -> move.move() == 19 && age >= 11 && age < 16;
				case "recovery" -> age >= move.windup() + move.recovery() / 2 && age < move.windup() + move.recovery();
				default -> false;
			};
			check(correct, "The filename matches accepted combat time: " + prefix + "_" + phase + " age=" + age);
			Wildercord.LOGGER.info("MASTERS_CAPTURE_PHASE name={} phase={} age={} windup={} recovery={}",
				prefix, phase, age, move.windup(), move.recovery());
		});
		check(inspect(context, phase.equals("recovery")), "The registered live rig participates during " + phase);
		return context.computeOnClient(mc -> MastersCaptureProbe.capture(mc, prefix + "_" + phase));
	}

	private static void shot(ClientGameTestContext context, String name) {
		if (context.computeOnClient(mc -> mc.gui.screen() != null)) {
			context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
		} else {
			awaitShots(context, context.computeOnClient(mc -> MastersCaptureProbe.capture(mc, name)));
		}
	}

	private static void awaitShots(ClientGameTestContext context, CompletableFuture<Void> captured) {
		context.waitFor(mc -> captured.isDone());
		captured.join();
	}

	private static void check(boolean ok, String message) {
		if (!ok) throw new AssertionError(message);
	}
}
