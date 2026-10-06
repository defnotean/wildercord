import dev.wildercord.aura.MastersArtAnimation;
import dev.wildercord.aura.WallTurnAnimation;
import dev.wildercord.aura.WallTurnRules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Offline projections of the actual pure palette. This is pose evidence, never native gameplay or a screenshot receipt. */
public final class ExportWallTurnPoses {
	public static void main(String[] args) throws Exception {
		StringBuilder svg = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1000\" height=\"420\" viewBox=\"0 0 1000 420\"><rect width=\"1000\" height=\"420\" fill=\"#17262b\"/><style>text{font-family:monospace;fill:#dfede4;font-size:14px} .bone{stroke:#a6cabc;stroke-width:8;stroke-linecap:round}</style><text x=\"24\" y=\"28\">Wall Turn · original body/hand phase palette · OFFLINE projection</text>");
		int[] phases = {WallTurnRules.BRACE, WallTurnRules.KICK, WallTurnRules.LAND, WallTurnRules.ABORT};
		String[] labels = {"BRACE / held wall contact", "KICK / accepted final step", "LAND / actual safe support", "ABORT / no landing claim"};
		for (int i = 0; i < phases.length; i++) {
			var pose = WallTurnAnimation.sample(phases[i], phases[i] == WallTurnRules.KICK ? 1 : 10, 1);
			float cx = 125 + 250 * i;
			svg.append("<text x=\"").append(cx - 112).append("\" y=\"60\">").append(labels[i]).append("</text>");
			segment(svg, cx, upper(pose, 0, 0), pose.body(), 12, pose.weight());
			segment(svg, cx, upper(pose, -5, 2), pose.sword(), 11, pose.weight());
			segment(svg, cx, upper(pose, 5, 2), pose.guard(), 11, pose.weight());
			segment(svg, cx, new Vector3f(-1.9F, 12 + pose.lower() * pose.weight(), pose.forward() * pose.weight()), pose.rearLeg(), 12, pose.weight());
			segment(svg, cx, new Vector3f(1.9F, 12 + pose.lower() * pose.weight(), pose.forward() * pose.weight()), pose.frontLeg(), 12, pose.weight());
			var hand = MastersArtAnimation.view(pose, false, 0, 0, 0).transform();
			svg.append(String.format(Locale.ROOT, "<text x=\"%.1f\" y=\"345\">hand xyz %.2f %.2f %.2f</text><text x=\"%.1f\" y=\"368\">pitch/yaw/roll %.0f %.0f %.0f</text>", cx - 112, hand.x(), hand.y(), hand.z(), cx - 112, hand.pitch(), hand.yaw(), hand.roll()));
		}
		svg.append("<text x=\"24\" y=\"404\">Classic rig, existing skin and grip. Native client / articulated fallback acceptance is still required.</text></svg>");
		Files.writeString(Path.of(args[0]), svg.toString());
	}
	private static Vector3f upper(MastersArtAnimation.Pose pose, float x, float y) {
		var target = MastersArtAnimation.pivot(pose, x, y, 0, false);
		return new Vector3f(x, y, 0).lerp(new Vector3f(target.x(), target.y(), target.z()), pose.weight());
	}
	private static void segment(StringBuilder svg, float cx, Vector3f root, MastersArtAnimation.Joint rotation, float length, float weight) {
		var matrix = new Matrix4f().translate(root).rotateZ(rotation.z() * weight).rotateY(rotation.y() * weight).rotateX(rotation.x() * weight);
		line(svg, cx, matrix.transformPosition(new Vector3f()), matrix.transformPosition(new Vector3f(0, length, 0)));
	}
	private static void line(StringBuilder svg, float cx, Vector3f a, Vector3f b) {
		svg.append(String.format(Locale.ROOT, "<line class=\"bone\" x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\"/>",
			cx + (a.x + .55F * a.z) * 7, 115 + (a.y - .25F * a.z) * 7, cx + (b.x + .55F * b.z) * 7, 115 + (b.y - .25F * b.z) * 7));
	}
}
