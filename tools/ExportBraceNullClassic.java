import dev.wildercord.aura.MastersArtAnimation;
import dev.wildercord.aura.MastersStyleRules;
import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Source samples for the offline projection preflight. These are never native phase evidence. */
public final class ExportBraceNullClassic {
	public static void main(String[] args) {
		var samples = new ArrayList<Map<String, Object>>();
		var bodies = new ArrayList<Map<String, Object>>();
		for (int move : new int[] {24, 25}) {
			var style = MastersStyleRules.animation(move);
			if (style == null || style.windup() != (move == 24 ? 6 : 4) || style.recovery() != (move == 24 ? 16 : 14))
				throw new AssertionError("Unmoved/Null Parry export requires accepted 6/16 and 4/14 windows");
			int windup = style.windup();
			int recovery = style.recovery();
			String name = style.art();
			for (float age = 0; age <= windup + recovery; age += .25F) {
				var pose = MastersArtAnimation.sample(move, age, windup, recovery);
				var views = new ArrayList<Map<String, Object>>();
				for (boolean left : new boolean[] {false, true}) for (float pitch : new float[] {0, -12})
					views.add(Map.of("left", left, "yawDelta", 0, "pitchDelta", pitch,
						"view", MastersArtAnimation.view(pose, left, 0, 0, pitch)));
				if (move == 24 || move == 25) views.add(Map.of("left", true, "yawDelta", -90, "pitchDelta", -50,
					"view", MastersArtAnimation.view(pose, true, 0, -90, -50)));
				var row = new LinkedHashMap<String, Object>();
				row.put("move", move); row.put("name", name); row.put("windup", windup); row.put("recovery", recovery);
				row.put("age", age); row.put("pose", pose); row.put("views", views); samples.add(row);
			}
			float[] ages = {windup * .65F, windup, windup + Math.min(4, recovery * .25F), windup + recovery - 2};
			String[] phases = {"CHAMBER", "ACTIVE", "FOLLOW", "RECOVERY"};
			for (boolean slim : new boolean[] {false, true}) for (boolean left : new boolean[] {false, true})
				for (int i = 0; i < ages.length; i++) bodies.add(body(move, name, ages[i], windup, recovery, slim, left, phases[i]));
		}
		System.out.println(new Gson().toJson(Map.of("kind", "source_projection_samples_not_native_frames", "samples", samples,
			"bodySamples", bodies, "bodyProvenance", "Original vanilla PlayerModel polygons; documented MastersArtPose.apply sibling-part adaptation from bind baseline. Actual production pivot, boundedHead and bladeTilt; official translateToHand and ItemTransform. Not native frames.")));
	}

	private static Map<String, Object> body(int move, String name, float age, int windup, int recovery, boolean slim, boolean left, String phase) {
		var pose = MastersArtAnimation.sample(move, age, windup, recovery);
		ModelPart root = LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, slim), 64, 64).bakeRoot();
		PlayerModel model = new PlayerModel(root, slim);
		float weight = pose.weight(), side = left ? -1 : 1;
		// Source adapter for MastersArtPose.apply with an explicit fresh bind baseline.
		// No live setupAnim/head look, walk, held-arm, equip/use or breathing baseline is simulated.
		joint(model.body, pose.body(), side, weight);
		model.head.xRot += pose.head().x() * weight;
		model.head.yRot += pose.head().y() * side * weight;
		model.head.zRot += pose.head().z() * side * weight;
		var head = MastersArtAnimation.boundedHead(new MastersArtAnimation.Joint(model.body.xRot, model.body.yRot, model.body.zRot),
			new MastersArtAnimation.Joint(model.head.xRot, model.head.yRot, model.head.zRot), weight);
		model.head.setRotation(head.x(), head.y(), head.z());
		joint(left ? model.leftArm : model.rightArm, pose.sword(), side, weight);
		joint(left ? model.rightArm : model.leftArm, pose.guard(), side, weight);
		joint(left ? model.rightLeg : model.leftLeg, pose.frontLeg(), side, weight);
		joint(left ? model.leftLeg : model.rightLeg, pose.rearLeg(), side, weight);
		var pivots = new ArrayList<Map<String, Object>>();
		String[] upperNames = {"body", "head", "right_arm", "left_arm"};
		ModelPart[] upper = {model.body, model.head, model.rightArm, model.leftArm};
		for (int i = 0; i < upper.length; i++) {
			ModelPart part = upper[i]; var initial = part.getInitialPose();
			var target = MastersArtAnimation.pivot(pose, initial.x(), initial.y(), initial.z(), left);
			part.x = Mth.lerp(weight, part.x, target.x()); part.y = Mth.lerp(weight, part.y, target.y()); part.z = Mth.lerp(weight, part.z, target.z());
			pivots.add(Map.of("part", upperNames[i], "initial", new float[] {initial.x(), initial.y(), initial.z()}, "target", target,
				"applied", new float[] {part.x, part.y, part.z}, "rotation", new float[] {part.xRot, part.yRot, part.zRot}));
		}
		for (ModelPart part : new ModelPart[] {model.rightLeg, model.leftLeg}) {
			part.y = Mth.lerp(weight, part.y, part.getInitialPose().y() + pose.lower());
			part.z = Mth.lerp(weight, part.z, part.getInitialPose().z() + pose.forward());
		}
		var cubes = new ArrayList<Map<String, Object>>();
		root.visit(new PoseStack(), (stack, path, index, cube) -> {
			// Render the six rigid base parts only. Overlay children are explicitly omitted.
			if (path.indexOf('/', 1) >= 0) return;
			var faces = new ArrayList<float[][]>();
			for (var face : cube.polygons) {
				float[][] points = new float[face.vertices().length][3];
				for (int i = 0; i < face.vertices().length; i++) {
					var vertex = face.vertices()[i];
					Vector3f p = new Vector3f(vertex.x() / 16, vertex.y() / 16, vertex.z() / 16).mulPosition(stack.pose()).mul(16);
					points[i] = new float[] {p.x, p.y, p.z};
				}
				faces.add(points);
			}
			cubes.add(Map.of("part", path, "cubeIndex", index, "faces", faces));
		});
		PoseStack sword = new PoseStack();
		// Actual pristine PlayerModel method includes its pre-rotation slim-arm offset.
		model.translateToHand((net.minecraft.client.renderer.entity.state.AvatarRenderState) null, left ? HumanoidArm.LEFT : HumanoidArm.RIGHT, sword);
		sword.rotateDegrees(Axis.XP, -90); sword.rotateDegrees(Axis.YP, 180);
		sword.translate(side / 16, 2F / 16, -10F / 16); // Official adult ItemInHandLayer translation.
		float bladeTilt = MastersArtAnimation.bladeTilt(move, age, windup, weight);
		float y = -1.327F / 16, z = 1.439F / 16;
		sword.translate(0, y, z); sword.rotateDegrees(Axis.XP, bladeTilt); sword.translate(0, -y, -z);
		Vector3f grip = sword.last().pose().transformPosition(new Vector3f(0, y, z)).mul(16);
		new ItemTransform(new Vector3f(0, left ? 90 : -90, left ? -55 : 55),
			new Vector3f(0, 4F / 16, .5F / 16), new Vector3f(.85F)).apply(left, sword.last());
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("move", move); row.put("name", name); row.put("age", age); row.put("phase", phase);
		row.put("variant", slim ? "slim" : "wide"); row.put("left", left); row.put("weight", weight);
		row.put("pivots", pivots); row.put("boundedHead", head); row.put("bladeTilt", bladeTilt); row.put("cubes", cubes);
		row.put("swordMatrixModelPixels", new Matrix4f().scaling(16).mul(sword.last().pose()).get(new float[16]));
		row.put("gripModelPixels", new float[] {grip.x, grip.y, grip.z});
		return row;
	}

	private static void joint(ModelPart part, MastersArtAnimation.Joint joint, float side, float weight) {
		part.xRot = Mth.lerp(weight, part.xRot, joint.x());
		part.yRot = Mth.lerp(weight, part.yRot, joint.y() * side);
		part.zRot = Mth.lerp(weight, part.zRot, joint.z() * side);
	}
}
