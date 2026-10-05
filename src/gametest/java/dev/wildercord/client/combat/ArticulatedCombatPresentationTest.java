package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.ArticulatedCombatPose.Joint;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.aura.MastersArts;
import dev.wildercord.aura.world.GaleRepriseRules;
import dev.wildercord.aura.world.MasterAnimationRules;
import dev.wildercord.aura.world.MasterSchoolMotionChecks;
import dev.wildercord.aura.world.StoneFractureRules;
import dev.wildercord.aura.world.SwordMaster;
import dev.wildercord.client.MastersArtsClient;
import dev.wildercord.client.auraworld.AuraFighterRenderState;
import dev.wildercord.client.auraworld.MasterModel;
import dev.wildercord.client.auraworld.MasterRenderer;
import dev.wildercord.client.fx.HitStop;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CameraType;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
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

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Opt-in native renderer checks plus real Spellcut input and naturally admitted school-form
 * captures. Synthetic bridge/hold checks are explicitly separate from those live captures.
 */
public final class ArticulatedCombatPresentationTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		ArticulatedSharedPlayerChecks.body(context);
		ArticulatedOpeningStyleChecks.body(context);
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
					check(model instanceof ArticulatedModelAccess, "Player model mixin exposes explicit primary-body ownership");
					((ArticulatedModelAccess) model).wildercord$ownBody(slim);
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
				masterSchools(masterRenderer, master);

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
		// The preceding fixtures inject frames. These captures use fresh, unpaused trials and
		// the registered renderer's real extraction/submission/item calls for both school forms.
		String restored = System.getProperty(ArticulatedCombat.ENABLE_PROPERTY);
		try {
			System.setProperty(ArticulatedCombat.ENABLE_PROPERTY, "true");
			new MasterSchoolMotionChecks().runArticulated(context);
		} finally {
			if (restored == null) System.clearProperty(ArticulatedCombat.ENABLE_PROPERTY);
			else System.setProperty(ArticulatedCombat.ENABLE_PROPERTY, restored);
		}
	}

	/** Synthetic regression matrix only; none of these mutations are reported as live NPC footage. */
	private static void masterSchools(MasterRenderer renderer, SwordMaster master) {
		MasterModel model = renderer.getModel();
		for (boolean left : new boolean[] {false, true}) for (int move : new int[] {7, 8}) {
			int tell = move == 7 ? GaleRepriseRules.TELL : StoneFractureRules.TELL;
			int recovery = (move == 7 ? GaleRepriseRules.RECOVERY : StoneFractureRules.RECOVERY) - 1;
			for (float age : new float[] {4, move == 7 ? 9 : 12, tell - 6, tell, tell + recovery / 2F}) {
				AuraFighterRenderState state = masterState(renderer, master, move, age, tell, recovery, left);
				model.setupAnim(state);
				wholeBackend(model, true, "Synthetic school pose " + move + " at " + age);
				masterSocket(model, state);
				float[] once = segmentedSnapshot(model.articulatedRig());
				model.setupAnim(state);
				equal(once, segmentedSnapshot(model.articulatedRig()), "Repeated school model passes cannot accumulate transforms");
				masterSocket(model, state);
				check(model.articulatedRig().part(Joint.HEAD).getChild("hood") == model.hat
					&& model.articulatedRig().part(Joint.CHEST).getChild("travelling_clothes").getChild("cloak") == model.body.getChild("cloak")
					&& model.articulatedRig().part(Joint.PELVIS).getChild("scabbard_socket").getChild("scabbard") == model.body.getChild("scabbard"),
					"Each school retains the original hood, cloak and scabbard on the segmented body");
				if (move == 7 && age == 9) {
					state.walkAnimationSpeed = 1;
					model.setupAnim(state);
					wholeBackend(model, true, "Gale's accepted lateral step retains segmented ownership while moving");
					masterSocket(model, state);
				}
			}
			List<Consumer<AuraFighterRenderState>> incompatible = List.of(
				state -> state.setData(ArticulatedCombat.KNOWN_LAYERS, false),
				state -> state.chestEquipment = new ItemStack(Items.NETHERITE_CHESTPLATE),
				state -> { if (left) state.rightHandItemStack = new ItemStack(Items.SHIELD); else state.leftHandItemStack = new ItemStack(Items.SHIELD); },
				state -> { if (left) state.leftHandItemStack = new ItemStack(Items.STICK); else state.rightHandItemStack = new ItemStack(Items.STICK); },
				state -> state.walkAnimationSpeed = 1,
				state -> state.isCrouching = true,
				state -> state.isUsingItem = true,
				state -> state.isPassenger = true,
				state -> state.deathTime = 2,
				state -> state.isUpsideDown = true,
				state -> state.sit = 1,
				state -> state.yield = 1,
				state -> state.stagger = 1,
				state -> state.setData(ArticulatedCombat.FRAME, null));
			for (int i = 0; i < incompatible.size(); i++) {
				AuraFighterRenderState state = masterState(renderer, master, move, tell, tell, recovery, left);
				model.setupAnim(state);
				incompatible.get(i).accept(state);
				masterFallback(model, state, "School " + move + " fallback " + i + ", left=" + left);
				model.setupAnim(masterState(renderer, master, move, tell, tell, recovery, left));
				wholeBackend(model, true, "Supported model reuse recovers from every whole-body fallback");
			}
			AuraFighterRenderState state = masterState(renderer, master, move, tell, tell, recovery, left);
			model.setupAnim(state);
			System.setProperty(ArticulatedCombat.ENABLE_PROPERTY, "false");
			try { masterFallback(model, state, "Disabled opt-in restores the complete original school presentation"); }
			finally { System.setProperty(ArticulatedCombat.ENABLE_PROPERTY, "true"); }
			model.setupAnim(masterState(renderer, master, move, tell, tell, recovery, left));
			masterFallback(model, masterState(renderer, master, move, tell + 1 + recovery, tell, recovery, left),
				"Expired accepted school timeline cannot leave the segmented body visible");
			model.setupAnim(masterState(renderer, master, move, tell, tell, recovery, left));
			masterFallback(model, masterState(renderer, master, 9, tell, tell, recovery, left),
				"An unsupported future Master form keeps the complete original renderer");
			if (move == 7) galeLanding(renderer, master, left);
		}
	}

	private static void galeLanding(MasterRenderer renderer, SwordMaster master, boolean left) {
		var model = renderer.getModel();
		for (float age : new float[] {8, 11, 11.999F, 12, 13, 14, 15}) {
			boolean step = age < 12;
			for (double velocity : new double[] {0, 1.0e-8, 1.01e-8, .01, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
				var state = masterState(renderer, master, 7, age, GaleRepriseRules.TELL, GaleRepriseRules.RECOVERY - 1, left);
				var frame = state.getData(ArticulatedCombat.FRAME);
				check(frame.scriptedFootwork() == step, "The synthetic step window ends exactly at age12");
				state.setData(ArticulatedCombat.FRAME, new ArticulatedCombat.Frame(frame.pose(), frame.activation(), frame.move(),
					frame.master(), frame.leftHanded(), frame.yawDelta(), frame.pitchDelta(), frame.scriptedFootwork(), velocity));
				state.walkAnimationSpeed = .8F;
				String reason = "Gale landing at " + age + " with synced horizontal velocity squared=" + velocity + ", left=" + left;
				if (step || Double.isFinite(velocity) && velocity >= 0 && velocity <= 1.0e-8) {
					model.setupAnim(state);
					wholeBackend(model, true, reason + " retains its accepted segmented pose");
					masterSocket(model, state);
				} else {
					// Prime the shared model before each rejection so this also tests whole-body restoration.
					model.setupAnim(masterState(renderer, master, 7, 9, GaleRepriseRules.TELL, GaleRepriseRules.RECOVERY - 1, left));
					masterFallback(model, state, reason + " cannot claim the stationary exception");
				}
			}
		}
		for (int move : new int[] {1, 8}) {
			var state = masterState(renderer, master, move, 12, move == 1 ? 18 : StoneFractureRules.TELL,
				move == 1 ? 19 : StoneFractureRules.RECOVERY - 1, left);
			var frame = state.getData(ArticulatedCombat.FRAME);
			state.setData(ArticulatedCombat.FRAME, new ArticulatedCombat.Frame(frame.pose(), frame.activation(), frame.move(),
				frame.master(), frame.leftHanded(), frame.yawDelta(), frame.pitchDelta(), false, 0));
			state.walkAnimationSpeed = .8F;
			masterFallback(model, state, "Measured stillness cannot broaden ordinary locomotion ownership for Master form " + move);
		}
	}

	private static AuraFighterRenderState masterState(MasterRenderer renderer, SwordMaster master, int move,
		float age, int tell, int recovery, boolean left) {
		var state = renderer.createRenderState(master, .5F);
		state.mainArm = left ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
		state.rightHandItemStack = left ? ItemStack.EMPTY : new ItemStack(Items.DIAMOND_SWORD);
		state.leftHandItemStack = left ? new ItemStack(Items.DIAMOND_SWORD) : ItemStack.EMPTY;
		state.drawn = 1;
		state.walkAnimationSpeed = state.walkAnimationPos = state.xRot = state.yRot = 0;
		state.setData(MasterModel.FRAME, new MasterModel.Frame(MasterAnimationRules.sample(move, age, tell, 1, recovery), left, 999, move));
		state.setData(ArticulatedCombat.FRAME, new ArticulatedCombat.Frame(ArticulatedCombatPose.sampleMaster(move, age, tell, 1, recovery, left),
			999, move, true, left, 0, 0, ArticulatedCombatPose.masterFootwork(move, age, tell)));
		return state;
	}

	private static void wholeBackend(MasterModel model, boolean segmented, String reason) {
		check(model.articulatedRig().root.visible == segmented, reason + ": segmented root visibility");
		for (ModelPart part : new ModelPart[] {model.body, model.head, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg})
			check(part.visible != segmented, reason + ": all six original body parts switch together");
	}

	private static void masterFallback(MasterModel model, AuraFighterRenderState state, String reason) {
		check(ArticulatedCombat.frame(state) == null, reason + ": articulated eligibility rejected");
		model.setupAnim(state);
		wholeBackend(model, false, reason);
		float[] rejected = rigidSnapshot(model);
		PoseStack hand = new PoseStack(); model.translateToHand(state, state.mainArm, hand);
		float[] rejectedHand = hand.last().pose().get(new float[16]);
		state.setData(ArticulatedCombat.FRAME, null);
		model.setupAnim(state);
		equal(rejected, rigidSnapshot(model), reason + ": whole original model and clothes retain their transforms");
		PoseStack originalHand = new PoseStack(); model.translateToHand(state, state.mainArm, originalHand);
		equal(rejectedHand, originalHand.last().pose().get(new float[16]), reason + ": held item retains the original attachment");
	}

	private static void masterSocket(MasterModel model, AuraFighterRenderState state) {
		PoseStack actual = new PoseStack();
		model.translateToHand(state, state.mainArm, actual);
		actual.rotateDegrees(Axis.XP, -90); actual.rotateDegrees(Axis.YP, 180);
		actual.translate((state.mainArm == HumanoidArm.LEFT ? -1 : 1) / 16F, 2F / 16, -10F / 16);
		PoseStack expected = new PoseStack(); model.root().translateAndRotate(expected);
		model.articulatedRig().socket(state.mainArm, expected);
		Vector3f hilt = actual.last().pose().transformPosition(new Vector3f(0, -1.327F / 16, 1.439F / 16));
		Vector3f socket = expected.last().pose().transformPosition(new Vector3f());
		check(hilt.distance(socket) < .00001F, "Synthetic school hilt must stay at its articulated wrist");
		ArticulatedCombat.orientItemAtSocket(expected);
		equal(expected.last().pose().get(new float[16]), actual.last().pose().get(new float[16]),
			"Synthetic school hand attachment must preserve all position and orientation components");
	}

	private static float[] segmentedSnapshot(ArticulatedRig rig) {
		return transforms(java.util.Arrays.stream(Joint.values()).map(rig::part).toList());
	}
	private static float[] rigidSnapshot(MasterModel model) {
		var parts = new java.util.ArrayList<ModelPart>();
		for (ModelPart part : new ModelPart[] {model.body, model.head, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg})
			parts.addAll(part.getAllParts());
		return transforms(parts);
	}
	private static float[] transforms(List<ModelPart> parts) {
		float[] values = new float[parts.size() * 10];
		for (int i = 0; i < parts.size(); i++) {
			ModelPart p = parts.get(i); int at = i * 10;
			values[at] = p.x; values[at + 1] = p.y; values[at + 2] = p.z;
			values[at + 3] = p.xRot; values[at + 4] = p.yRot; values[at + 5] = p.zRot;
			values[at + 6] = p.xScale; values[at + 7] = p.yScale; values[at + 8] = p.zScale; values[at + 9] = p.visible ? 1 : 0;
		}
		return values;
	}
	private static void equal(float[] expected, float[] actual, String message) {
		check(expected.length == actual.length, message + ": transform count");
		for (int i = 0; i < expected.length; i++) check(Float.isFinite(actual[i]) && Math.abs(expected[i] - actual[i]) < .00001F,
			message + ": component " + i + " expected=" + expected[i] + " actual=" + actual[i]);
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
