package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildercord.aura.ArticulatedCombatPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Vector3f;


/**
 * Headless regression using pristine Minecraft ModelPart/PoseStack, not a native client launch.
 * Compile with the verified main/client outputs and dependencies, and run with the original
 * publisher Minecraft jars (the verifier's compile-only jars are not runtime artifacts).
 */
public final class CheckArticulatedGrip {
	private CheckArticulatedGrip() {}

	public static void main(String[] args) {
		float[][] rotations = {{0, 0, 0}, {-.3141593F, .02F, .1F}, {-.3141593F, .02F, -.1F},
			{-.8F, .35F, -.4F}, {.5F, -.6F, .3F}, {-1.2F, 1.1F, -.7F}};
		int checked = 0;
		float worst = 0;
		for (boolean slim : new boolean[] {false, true}) for (boolean left : new boolean[] {false, true}) {
			HumanoidArm hand = left ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
			float side = left ? -1 : 1;
			for (float[] rotation : rotations) {
				PlayerModel vanillaModel = new PlayerModel(LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64).bakeRoot(), slim);
				ModelPart baseline = vanillaModel.getArm(hand);
				baseline.setRotation(rotation[0], rotation[1], rotation[2]);
				for (float offset : new float[] {0, .3F}) {
					baseline.x = baseline.getInitialPose().x() + offset;
					baseline.y = baseline.getInitialPose().y() - offset;
					baseline.z = baseline.getInitialPose().z() + offset;
					PoseStack vanilla = new PoseStack();
					// Execute the pristine official method, including its pre-rotation slim pivot.
					// This 26.3 method does not read its render-state argument.
					vanillaModel.translateToHand((net.minecraft.client.renderer.entity.state.AvatarRenderState) null, hand, vanilla);
					vanilla.rotateDegrees(Axis.XP, -90);
					vanilla.rotateDegrees(Axis.YP, 180);
					vanilla.translate(side / 16, 2F / 16, -10F / 16);
					Vector3f expected = vanilla.last().pose().transformPosition(new Vector3f(0, -1.327F / 16, 1.439F / 16));
					for (float age : new float[] {0, .0001F, .001F, 15.999F, 15.9999F, 16}) {
						var pose = ArticulatedCombatPose.sampleSpellcut(0, age, 4, 12, left);
						ArticulatedRig rig = new ArticulatedRig(slim, false);
						rig.apply(pose::local);
						ArticulatedCombat.baselineArm(rig, baseline, left, pose.weight());
						PoseStack articulated = new PoseStack();
						rig.socket(hand, articulated);
						Vector3f actual = articulated.last().pose().transformPosition(new Vector3f());
						float error = actual.distance(expected);
						if (!(error < .001F)) throw new AssertionError("Grip seam: slim=" + slim + ", left=" + left
							+ ", age=" + age + ", actual=" + actual + ", expected=" + expected + ", distance=" + error);
						worst = Math.max(worst, error);
						checked++;
					}
					// Full-weight authoring must remain untouched by the disappearing baseline.
					var full = ArticulatedCombatPose.sampleSpellcut(0, 4, 4, 12, left);
					ArticulatedRig rig = new ArticulatedRig(slim, false);
					rig.apply(full::local);
					PoseStack before = new PoseStack();
					rig.socket(hand, before);
					ArticulatedCombat.baselineArm(rig, baseline, left, 1);
					PoseStack after = new PoseStack();
					rig.socket(hand, after);
					if (!before.last().pose().equals(after.last().pose())) throw new AssertionError("Full-weight grip changed");
				}
			}
		}
		System.out.println("ARTICULATED_GRIP_CHECK samples=" + checked + " maxErrorBlocks=" + worst
			+ " toleranceBlocks=0.001 widths=wide,slim hands=right,left nativeLaunch=false");
	}
}
