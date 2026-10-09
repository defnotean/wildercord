package dev.wildercord.client.auraworld;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.world.AuraWorld;
import dev.wildercord.aura.world.MasterAnimationRules;
import dev.wildercord.aura.world.MastersRules;
import dev.wildercord.aura.world.SwordMaster;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;

/**
 * Native model bridge contracts on the registered renderer and baked travelling rig. Synthetic
 * frame injection below tests the sampler/model boundary, not server combat or rendered footage.
 * A separate live-AI fixture below captures native NPC motion; these synthetic contracts stay separate.
 */
public final class MasterModelPresentationTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (var world = context.worldBuilder().create()) {
			context.waitTicks(30);
			int[] id = {-1};
			world.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().getFirst();
				SwordMaster master = AuraWorld.SWORD_MASTER.create(player.level(), EntitySpawnReason.COMMAND);
				check(master != null, "Master entity exists");
				master.setNoAi(true);
				master.setNoGravity(true);
				master.snapTo(player.getX() + 2, player.getY(), player.getZ(), 0, 0);
				player.level().addFreshEntity(master);
				id[0] = master.getId();
			});
			context.waitFor(mc -> mc.level.getEntity(id[0]) instanceof SwordMaster, 30);
			context.runOnClient(mc -> {
				SwordMaster master = (SwordMaster) mc.level.getEntity(id[0]);
				check(mc.getEntityRenderDispatcher().getRenderer(master) instanceof MasterRenderer, "Master uses its dedicated renderer");
				MasterRenderer renderer = (MasterRenderer) mc.getEntityRenderDispatcher().getRenderer(master);
				AuraFighterRenderState state = renderer.createRenderState(master, .5F);
				check(state.getData(MasterModel.FRAME) == null, "An idle server master never invents an attack");
				check(state.swingAnimation == 0 && state.currentSwing == null, "Vanilla swings cannot overlap master choreography");
				MasterModel model = renderer.getModel();
				state.ageInTicks = 0;
				state.drawn = 1;
				state.walkAnimationPos = state.walkAnimationSpeed = 0;
				state.xRot = state.yRot = 0;
				for (boolean left : new boolean[] {false, true}) {
					state.mainArm = left ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
					state.setData(MasterModel.FRAME, null);
					model.setupAnim(state);
					float[] neutral = snapshot(model);
					var poses = new java.util.ArrayList<MasterAnimationRules.Pose>();
					for (var move : MastersRules.Move.values()) {
						if (move == MastersRules.Move.TECHNIQUE) continue;
						for (float age : new float[] {0, move.tell * .4F, move.tell * .65F, move.tell, move.tell + 2,
							move.tell + move.recovery - .1F, move.tell + move.recovery})
							poses.add(MasterAnimationRules.sample(move.ordinal() + 1, age, move.tell, 1, move.recovery - 1));
					}
					for (var technique : dev.wildercord.aura.world.MasterTechniques.all()) {
						for (int i = 0; i < technique.strikes().size(); i++)
							poses.add(dev.wildercord.aura.world.MasterTechniques.sample(technique.id(), technique.impact(i)));
						poses.add(dev.wildercord.aura.world.MasterTechniques.sample(technique.id(), technique.impact(0) * .5F));
					}
					{
						for (var pose : poses) {
							state.setData(MasterModel.FRAME, new MasterModel.Frame(pose, left));
							model.setupAnim(state);
							near(pose.rootYaw() * (left ? -1 : 1), model.root().yRot, "Full turn belongs to the root and mirrors with the hand");
							if (pose.weight() == 1) {
								var expected = MasterAnimationRules.mirrored(pose.sword(), left);
								ModelPart sword = left ? model.leftArm : model.rightArm;
								near(expected.x(), sword.xRot, "Native sword pitch follows sampler");
								near(expected.y(), sword.yRot, "Native sword yaw follows handedness");
								near(expected.z(), sword.zRot, "Native sword roll follows handedness");
								for (ModelPart leg : new ModelPart[] {model.rightLeg, model.leftLeg}) {
									near(24, leg.y + 12 * (float) Math.cos(leg.xRot) + 2 * Math.abs((float) Math.sin(leg.xRot)), "Sole edge remains on floor");
								}
								var shoulder = MasterAnimationRules.pivot(new MasterAnimationRules.Joint(model.body.xRot, model.body.yRot, model.body.zRot),
									MasterAnimationRules.lower(pose.stance()), -5, 2, 0);
								near(shoulder.x(), model.rightArm.x, "Right shoulder stays attached to torso");
								near(shoulder.y(), model.rightArm.y, "Right shoulder height follows torso");
								near(shoulder.z(), model.rightArm.z, "Right shoulder depth follows torso");
							}
							float[] once = snapshot(model);
							model.setupAnim(state);
							equal(once, snapshot(model), "Repeated render passes must not accumulate transforms");
							grip(state, left);
							state.setData(MasterModel.FRAME, null);
							model.setupAnim(state);
							equal(neutral, snapshot(model), "Cancellation restores every modified limb and accessory");
						}
					}
				}
				state.drawn = 0;
				model.setupAnim(state);
				check(model.body.getChild("scabbard").getChild("hilt").visible, "Sheathed reuse restores hilt visibility");
				state.drawn = 1;
				model.setupAnim(state);
				check(!model.body.getChild("scabbard").getChild("hilt").visible, "Drawn reuse hides the duplicate hilt");
				state.deathTime = 3;
				state.setData(MasterModel.FRAME, null);
				model.setupAnim(state);
				float[] dead = snapshot(model);
				state.setData(MasterModel.FRAME, new MasterModel.Frame(MasterAnimationRules.sample(2, 22, 22, 1, 23), true));
				model.setupAnim(state);
				equal(dead, snapshot(model), "Death rejects even a stale attack frame");
				PoseStack deadItem = new PoseStack();
				MasterModel.heldSword(state, state.mainArm, new ItemStack(Items.NETHERITE_SWORD), deadItem);
				check(deadItem.last().pose().equals(new org.joml.Matrix4f()), "Death also clears the sword's grip adjustment");
			});
		}
		new dev.wildercord.aura.world.MasterSchoolMotionChecks().run(context);
	}

	private static void grip(AuraFighterRenderState state, boolean left) {
		HumanoidArm hand = left ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
		PoseStack stack = new PoseStack();
		Vector3f grip = new Vector3f(0, -1.327F / 16, 1.439F / 16);
		MasterModel.heldSword(state, hand, new ItemStack(Items.NETHERITE_SWORD), stack);
		Vector3f moved = stack.last().pose().transformPosition(new Vector3f(grip));
		near(grip.x, moved.x, "Held blade rotates about its hilt X");
		near(grip.y, moved.y, "Held blade rotates about its hilt Y");
		near(grip.z, moved.z, "Held blade rotates about its hilt Z");
		PoseStack other = new PoseStack();
		MasterModel.heldSword(state, hand.getOpposite(), new ItemStack(Items.NETHERITE_SWORD), other);
		check(other.last().pose().equals(new org.joml.Matrix4f()), "Offhand items do not inherit the main-hand blade tilt");
		PoseStack nonsword = new PoseStack();
		MasterModel.heldSword(state, hand, new ItemStack(Items.STICK), nonsword);
		check(nonsword.last().pose().equals(new org.joml.Matrix4f()), "Other items do not inherit a sword-specific grip");
	}

	private static float[] snapshot(MasterModel model) {
		var parts = new java.util.ArrayList<ModelPart>();
		parts.add(model.root());
		for (ModelPart part : new ModelPart[] {model.body, model.head, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg}) {
			parts.addAll(part.getAllParts());
		}
		float[] result = new float[parts.size() * 7];
		for (int i = 0; i < parts.size(); i++) {
			ModelPart part = parts.get(i);
			int n = i * 7;
			result[n] = part.x; result[n + 1] = part.y; result[n + 2] = part.z;
			result[n + 3] = part.xRot; result[n + 4] = part.yRot; result[n + 5] = part.zRot; result[n + 6] = part.visible ? 1 : 0;
		}
		return result;
	}

	private static void equal(float[] expected, float[] actual, String why) {
		check(expected.length == actual.length, why);
		for (int i = 0; i < expected.length; i++) near(expected[i], actual[i], why + " channel " + i);
	}
	private static void near(float expected, float actual, String why) { check(Math.abs(expected - actual) < .0001F, why + ": " + actual + " vs " + expected); }
	private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
}
