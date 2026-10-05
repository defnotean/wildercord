package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.ArticulatedCombatPose.Joint;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.MastersArts;
import dev.wildercord.client.MastersArtsClient;
import dev.wildercord.client.fx.HitStop;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.joml.Vector3f;

import java.util.Set;

/**
 * Opt-in native renderer checks plus screenshots driven by the real Spellcut input and accepted
 * server timeline. Synthetic bridge/hold checks are explicitly separate from those live captures.
 */
public final class ArticulatedCombatPresentationTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		String previous = System.getProperty(ArticulatedCombat.ENABLE_PROPERTY);
		CameraType camera = context.computeOnClient(mc -> mc.options.getCameraType());
		HumanoidArm hand = context.computeOnClient(mc -> mc.options.mainHand().get());
		System.setProperty(ArticulatedCombat.ENABLE_PROPERTY, "true");
		int[] masterId = {-1};
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(30);
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("fill -10 99 -10 10 99 10 minecraft:stone_bricks");
			world.getServer().runCommand("fill -10 100 -10 10 108 10 minecraft:air");
			world.getServer().runCommand("time set 3000");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setGameMode(GameType.SURVIVAL);
				player.teleportTo(server.overworld(), .5, 100, .5, Set.<Relative>of(), 0, 0, false);
				prepare(player);
				var master = dev.wildercord.aura.world.AuraWorld.SWORD_MASTER.create(player.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
				check(master != null, "Master model fixture entity exists");
				master.setNoAi(true);
				master.setNoGravity(true);
				master.snapTo(3, 100, 3, 0, 0);
				player.level().addFreshEntity(master);
				masterId[0] = master.getId();
			});
			context.waitTicks(8);
			context.waitFor(mc -> mc.level.getEntity(masterId[0]) instanceof dev.wildercord.aura.world.SwordMaster, 30);
			context.runOnClient(mc -> {
				check(ArticulatedCombat.stableCamera(), "The articulated preview starts with stable camera");
				@SuppressWarnings("unchecked") AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer> renderer =
					(AvatarRenderer<net.minecraft.client.player.AbstractClientPlayer>) mc.getEntityRenderDispatcher().getRenderer(mc.player);
				AvatarRenderState state = renderer.createRenderState(mc.player, .5F);
				state.setData(ArticulatedCombat.KNOWN_LAYERS, true);
				state.setData(dev.wildercord.client.render.AuraShellLayer.SHELL_GLOW, null);
				state.walkAnimationSpeed = 0;
				state.showCape = false;
				for (boolean slim : new boolean[] {false, true}) for (boolean left : new boolean[] {false, true}) {
					PlayerModel model = new PlayerModel(mc.getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim);
					check(model instanceof ArticulatedModelAccess, "Player model mixin owns a segmented rig");
					ArticulatedRig rig = ((ArticulatedModelAccess) model).wildercord$rig();
					state.mainArm = left ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
					state.rightHandItemStack = left ? ItemStack.EMPTY : new ItemStack(Items.DIAMOND_SWORD);
					state.leftHandItemStack = left ? new ItemStack(Items.DIAMOND_SWORD) : ItemStack.EMPTY;
					var pose = ArticulatedCombatPose.sampleSpellcut(0, 4, 4, 12, left);
					state.setData(ArticulatedCombat.FRAME, new ArticulatedCombat.Frame(pose, 123, 0, false, left, 0, 0));
					model.setupAnim(state);
					check(rig.root.visible && !model.body.visible && !model.rightArm.visible && !model.leftLeg.visible,
						"Exactly one body backend is visible");
					check(Math.abs(rig.part(left ? Joint.LEFT_FOREARM : Joint.RIGHT_FOREARM).xRot) > .2F, "Elbow visibly bends");
					check(Math.abs(rig.part(Joint.LEFT_SHIN).xRot) > .3F, "Knee visibly bends");
					socket(model, rig, state);
					model.setupAnim(state);
					socket(model, rig, state);
					state.chestEquipment = new ItemStack(Items.NETHERITE_CHESTPLATE);
					model.setupAnim(state);
					check(!rig.root.visible && model.body.visible && model.rightArm.visible, "Armor restores whole rigid backend without hiding gear");
					state.chestEquipment = ItemStack.EMPTY;
					state.setData(ArticulatedCombat.FRAME, null);
					model.setupAnim(state);
					check(!rig.root.visible && model.body.visible, "Cancellation and model reuse restore the complete fallback");
					check(model.root().getChild(ArticulatedRig.CHILD) == rig.root, "Fresh baked models attach exactly one rig");
					// Check the actual vanilla/custom boundary, not merely custom-bind continuity.
					state.xRot = 90;
					model.setupAnim(state);
					float basePitch = model.head.xRot;
					PoseStack baselineGrip = new PoseStack();
					model.translateToHand(state, state.mainArm, baselineGrip);
					baselineGrip.rotateDegrees(Axis.XP, -90);
					baselineGrip.rotateDegrees(Axis.YP, 180);
					baselineGrip.translate((left ? -1 : 1) / 16F, 2F / 16, -10F / 16);
					Vector3f baselinePoint = baselineGrip.last().pose().transformPosition(new Vector3f(0, -1.327F / 16, 1.439F / 16));
					for (float edge : new float[] {.0001F, 15.999F}) {
						state.setData(ArticulatedCombat.FRAME, new ArticulatedCombat.Frame(
							ArticulatedCombatPose.sampleSpellcut(0, edge, 4, 12, left), 321, 0, false, left, 180, 90));
						model.setupAnim(state);
						check(ArticulatedCombat.frame(state) != null && rig.root.visible,
							"Both seam probes must observe the active articulated backend: edge=" + edge);
						check(Math.abs(rig.part(Joint.HEAD).xRot - basePitch) < .001F, "Extreme head look has no backend-edge clamp snap");
						PoseStack edgeGrip = new PoseStack();
						rig.socket(state.mainArm, edgeGrip);
						Vector3f edgePoint = edgeGrip.last().pose().transformPosition(new Vector3f());
						check(edgePoint.distance(baselinePoint) < .001F,
							"Articulated grip converges to the actual vanilla held-sword position: slim=" + slim + ", left=" + left
								+ ", edge=" + edge + ", weight=" + state.getData(ArticulatedCombat.FRAME).pose().weight()
								+ ", actual=" + edgePoint + ", expected=" + baselinePoint + ", distance=" + edgePoint.distance(baselinePoint));
					}
					state.xRot = 0;
					var idle = new ArticulatedCombat.Frame(ArticulatedCombatPose.NONE, Long.MIN_VALUE, -1, false, left, 0, 0);
					state.setData(ArticulatedCombat.FRAME, idle);
					check(ArticulatedCombat.frame(state) == null && ArticulatedCombat.viewFrame(state) == idle,
						"Idle first-person ownership never replaces the world body");
					state.walkAnimationSpeed = 1;
					check(ArticulatedCombat.viewFrame(state) == idle, "Walking retains camera-space idle arms without taking locomotion ownership");
					state.walkAnimationSpeed = 0;
					state.swingAnimation = .5F;
					check(ArticulatedCombat.viewFrame(state) == null, "Ordinary sword swings retain their existing first-person animation");
					state.swingAnimation = 0;
					state.setData(ArticulatedCombat.KNOWN_LAYERS, false);
					check(ArticulatedCombat.viewFrame(state) == null, "Unknown layers fall back in first person too");
					state.setData(ArticulatedCombat.KNOWN_LAYERS, true);
					state.setData(ArticulatedCombat.FRAME, null);
				}
				// The Master fixture samples an original accepted-timeline shape synthetically; it
				// verifies the renderer/clothing bridge and does not claim a live Master attack.
				var master = (dev.wildercord.aura.world.SwordMaster) mc.level.getEntity(masterId[0]);
				var masterRenderer = (dev.wildercord.client.auraworld.MasterRenderer) mc.getEntityRenderDispatcher().getRenderer(master);
				var masterState = masterRenderer.createRenderState(master, .5F);
				masterState.walkAnimationSpeed = 0;
				masterState.setData(ArticulatedCombat.FRAME, new ArticulatedCombat.Frame(
					ArticulatedCombatPose.sampleMaster(1, 18, 18, 1, 19, false), 888, 1, true, false, 0, 0));
				var masterModel = masterRenderer.getModel();
				masterModel.setupAnim(masterState);
				var masterRig = masterModel.articulatedRig();
				check(masterRig.root.visible && !masterModel.body.visible, "Master SWEEP owns the segmented body");
				check(masterRig.part(Joint.HEAD).getChild("hood") == masterModel.hat, "Master retains original hood on head socket");
				check(masterRig.part(Joint.CHEST).getChild("travelling_clothes").getChild("cloak") == masterModel.body.getChild("cloak"),
					"Master retains its original cloak on the articulated chest");
				masterState.setData(ArticulatedCombat.FRAME, null);
				masterModel.setupAnim(masterState);
				check(!masterRig.root.visible && masterModel.body.visible, "Master cancellation restores its rigid travelling model");

				// Synthetic state probes cover cosmetic freeze ownership, not gameplay damage.
				state.mainArm = HumanoidArm.RIGHT;
				var a = new ArticulatedCombat.Frame(ArticulatedCombatPose.sampleSpellcut(0, 2, 4, 12, false), 777, 0, false, false, 0, 0);
				var b = new ArticulatedCombat.Frame(ArticulatedCombatPose.sampleSpellcut(0, 4, 4, 12, false), 777, 0, false, false, 0, 0);
				HitStop.clear();
				HitStop.hold(1000, mc.player.getId());
				state.setData(ArticulatedCombat.FRAME, a);
				HitStop.extracted(mc.player, state);
				state.setData(ArticulatedCombat.FRAME, b);
				HitStop.extracted(mc.player, state);
				check(state.getData(ArticulatedCombat.FRAME) == a, "Hit-stop captures the actual pose palette and first-person source");
				state.setData(ArticulatedCombat.FRAME, null);
				HitStop.extracted(mc.player, state);
				check(state.getData(ArticulatedCombat.FRAME) == null, "Hit-stop cannot revive a cancelled clip");
				HitStop.clear();
			});
			for (HumanoidArm arm : HumanoidArm.values()) for (CameraType view : new CameraType[] {CameraType.THIRD_PERSON_FRONT, CameraType.FIRST_PERSON}) {
				context.waitTicks(105);
				world.getServer().runOnServer(server -> prepare(server.getPlayerList().getPlayers().getFirst()));
				context.runOnClient(mc -> {
					mc.options.mainHand().set(arm);
					mc.options.broadcastOptions();
					mc.options.setCameraType(view);
					mc.gui.setScreen(null);
				});
				context.waitTicks(4);
				context.getInput().pressKey(MastersArtsClient.mapping(0));
				context.waitFor(mc -> MastersArtsClient.timeline(mc.player) != null, 30);
				boolean observed = false;
				for (int i = 0; i < 9; i++) {
					context.waitTicks(1);
					boolean owns = context.computeOnClient(mc -> {
						var renderer = mc.getEntityRenderDispatcher().getRenderer(mc.player);
						var state = (AvatarRenderState) renderer.createRenderState(mc.player, .5F);
						return ArticulatedCombat.frame(state) != null;
					});
					observed |= owns;
					if (owns) context.takeScreenshot(TestScreenshotOptions.of("articulated_live_" + arm.name().toLowerCase(java.util.Locale.ROOT)
						+ "_" + (view == CameraType.FIRST_PERSON ? "first" : "third") + "_frame_" + i).disableCounterPrefix());
				}
				check(observed, "The articulated backend draws a real server-accepted Spellcut");
				context.waitTicks(25);
				check(context.computeOnClient(mc -> MastersArtsClient.timeline(mc.player) == null), "The real clip expires on its unchanged server deadline");
			}
			world.getServer().runOnServer(server -> MastersArts.cancel(server.getPlayerList().getPlayers().getFirst()));
		} finally {
			HitStop.clear();
			if (previous == null) System.clearProperty(ArticulatedCombat.ENABLE_PROPERTY); else System.setProperty(ArticulatedCombat.ENABLE_PROPERTY, previous);
			context.runOnClient(mc -> {
				mc.options.setCameraType(camera);
				mc.options.mainHand().set(hand);
				mc.options.broadcastOptions();
				MastersArtsClient.mapping(0).setDown(false);
			});
		}
	}

	private static void socket(PlayerModel model, ArticulatedRig rig, AvatarRenderState state) {
		PoseStack actual = new PoseStack();
		model.translateToHand(state, state.mainArm, actual);
		actual.rotateDegrees(Axis.XP, -90);
		actual.rotateDegrees(Axis.YP, 180);
		actual.translate((state.mainArm == HumanoidArm.LEFT ? -1 : 1) / 16F, 2F / 16, -10F / 16);
		Vector3f point = actual.last().pose().transformPosition(new Vector3f(0, -1.327F / 16, 1.439F / 16));
		PoseStack expected = new PoseStack();
		rig.socket(state.mainArm, expected);
		Vector3f target = expected.last().pose().transformPosition(new Vector3f());
		check(point.distance(target) < .00001F, "Native item hilt stays at the wrist socket in either hand/skin: " + point + " vs " + target);
	}

	private static void prepare(ServerPlayer player) {
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND})
			player.setItemSlot(slot, ItemStack.EMPTY);
		player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
		player.setAttached(AuraAttachments.AURA, new AuraAttachments.Data("stone", 3, 1800, 100, 0));
		player.inventoryMenu.broadcastChanges();
	}
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
