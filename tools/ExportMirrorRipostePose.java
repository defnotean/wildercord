import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.ArticulatedCombatPose.Joint;
import dev.wildercord.aura.ArticulatedCombatPose.Matrix;
import dev.wildercord.aura.ArticulatedCombatPose.Pose;
import dev.wildercord.aura.ArticulatedCombatPose.Transform;
import dev.wildercord.aura.ArticulatedCombatPose.ViewPose;

/**
 * Optional offline inspection data, sampled from the exact production pose code, without Minecraft.
 * javac -sourcepath src/main/java -d /tmp/pose-export src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java tools/ExportMirrorRipostePose.java
 * java -cp /tmp/pose-export ExportMirrorRipostePose > /tmp/articulated-poses.json
 *
 * Matrices are column-major and model units are pixels. First-person matrices use the same model
 * convention: apply origin + (-x/16, -y/16, +z/16) to obtain camera-space blocks. This file contains
 * no gameplay geometry or collision data and is not evidence of an in-game rendering test.
 */
public final class ExportMirrorRipostePose {
	private ExportMirrorRipostePose() {}
	public static void main(String[] args) {
		StringBuilder json = new StringBuilder(1_000_000);
		json.append("{\"schema\":1,\"units\":\"model_pixels\",\"matrixOrder\":\"column_major\",\"joints\":[");
		for (Joint joint : Joint.values()) {
			if (joint.ordinal() > 0) json.append(',');
			json.append("{\"name\":\"").append(joint.name()).append("\",\"parent\":")
				.append(joint.parent() == null ? -1 : joint.parent().ordinal()).append(",\"bind\":");
			transform(json, joint.bind()); json.append('}');
		}
		json.append("],\"clips\":[");
		boolean first = true;
		for (int move : new int[] {22, 23}) for (boolean left : new boolean[] {false, true}) {
			boolean master = false;
			if (!first) json.append(','); first = false;
			var style = dev.wildercord.aura.MastersStyleRules.animation(move);
			int tell = style.windup(), active = 1, recovery = style.recovery();
			String id = style.art();
			int end = tell + recovery + (master ? active : 0);
			json.append("{\"id\":\"").append(id).append("\",\"leftHanded\":").append(left)
				.append(",\"tell\":").append(tell).append(",\"active\":").append(active).append(",\"recovery\":").append(recovery).append(",\"frames\":[");
			for (int sample = 0; sample <= end * 8; sample++) {
				if (sample > 0) json.append(',');
				float age = sample / 8F;
				Pose pose = master ? ArticulatedCombatPose.sampleMaster(move, age, tell, active, recovery, left)
					: ArticulatedCombatPose.samplePlayer(move, age, tell, recovery, left);
				ViewPose view = ArticulatedCombatPose.view(pose, left);
				json.append("{\"age\":").append(age).append(",\"weight\":").append(pose.weight()).append(",\"phase\":\"")
					.append(pose.phase()).append("\",\"local\":[");
				for (Joint joint : Joint.values()) { if (joint.ordinal() > 0) json.append(','); transform(json, pose.local(joint)); }
				json.append("],\"world\":[");
				for (Joint joint : Joint.values()) { if (joint.ordinal() > 0) json.append(','); matrix(json, pose.world(joint)); }
				json.append("],\"firstPerson\":{\"origin\":[").append(view.origin().x()).append(',').append(view.origin().y()).append(',').append(view.origin().z()).append("],\"local\":[");
				for (Joint joint : Joint.values()) { if (joint.ordinal() > 0) json.append(','); transform(json, view.local(joint)); }
				json.append("],\"world\":[");
				for (Joint joint : Joint.values()) { if (joint.ordinal() > 0) json.append(','); matrix(json, view.world(joint)); }
				json.append("]}}");
			}
			json.append("]}");
		}
		System.out.println(json.append("]}"));
	}
	private static void transform(StringBuilder json, Transform t) {
		json.append('[').append(t.x()).append(',').append(t.y()).append(',').append(t.z()).append(',')
			.append(t.rotation().x()).append(',').append(t.rotation().y()).append(',').append(t.rotation().z()).append(']');
	}
	private static void matrix(StringBuilder json, Matrix matrix) {
		json.append('['); float[] values = matrix.values();
		for (int i = 0; i < values.length; i++) { if (i > 0) json.append(','); json.append(values[i]); }
		json.append(']');
	}
}
