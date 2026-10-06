import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.ArticulatedCombatPose.Joint;
import dev.wildercord.aura.ArticulatedCombatPose.Matrix;
import dev.wildercord.aura.ArticulatedCombatPose.Pose;
import dev.wildercord.aura.ArticulatedCombatPose.Transform;
import dev.wildercord.aura.ArticulatedCombatPose.ViewPose;

/**
 * Optional offline inspection data, sampled from the exact production pose code, without Minecraft.
 * javac -sourcepath src/main/java -d /tmp/pose-export src/main/java/dev/wildercord/aura/ArticulatedCombatPose.java tools/ExportArticulatedPose.java
 * java -cp /tmp/pose-export ExportArticulatedPose > /tmp/articulated-poses.json
 *
 * Matrices are column-major and model units are pixels. First-person matrices use the same model
 * convention: apply origin + (-x/16, -y/16, +z/16) to obtain camera-space blocks. This file contains
 * no gameplay geometry or collision data and is not evidence of an in-game rendering test.
 */
public final class ExportArticulatedPose {
	private ExportArticulatedPose() {}
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
		boolean schools = args.length == 1 && args[0].equals("--schools");
		boolean opening = args.length == 1 && args[0].equals("--opening-styles");
		boolean shared = args.length == 1 && args[0].equals("--shared-player");
		boolean groundFields = args.length == 1 && args[0].equals("--ground-fields");
		boolean hailSky = args.length == 1 && args[0].equals("--hail-sky");
		for (int move : groundFields ? new int[] {17, 18} : hailSky ? new int[] {15, 16} : opening ? new int[] {3, 4} : shared ? new int[] {1, 2} : schools ? new int[] {7, 8} : new int[] {0, 1}) for (boolean left : new boolean[] {false, true}) {
			boolean master = !groundFields && !hailSky && !opening && !shared && move != 0;
			if (!first) json.append(','); first = false;
			int tell = opening || hailSky || groundFields ? dev.wildercord.aura.MastersStyleRules.animation(move).windup() : shared ? dev.wildercord.aura.MastersArtRules.move(move).windup() : switch (move) { case 7 -> dev.wildercord.aura.world.GaleRepriseRules.TELL; case 8 -> dev.wildercord.aura.world.StoneFractureRules.TELL; case 1 -> 18; default -> 4; };
			int active = 1, recovery = opening || hailSky || groundFields ? dev.wildercord.aura.MastersStyleRules.animation(move).recovery() : shared ? dev.wildercord.aura.MastersArtRules.move(move).recovery() : switch (move) { case 7 -> dev.wildercord.aura.world.GaleRepriseRules.RECOVERY - 1; case 8 -> dev.wildercord.aura.world.StoneFractureRules.RECOVERY - 1; case 1 -> 19; default -> 12; };
			String id = opening || hailSky || groundFields ? dev.wildercord.aura.MastersStyleRules.animation(move).art() : shared ? dev.wildercord.aura.MastersArtRules.move(move).id() : switch (move) { case 7 -> "gale_crosswind"; case 8 -> "stone_fracture"; case 1 -> "master_sweep"; default -> "spellcut"; };
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
