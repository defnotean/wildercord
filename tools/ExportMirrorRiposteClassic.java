import dev.wildercord.aura.MastersArtAnimation;
import dev.wildercord.aura.MastersStyleRules;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Source samples for the offline projection preflight. These are never native phase evidence. */
public final class ExportMirrorRiposteClassic {
	public static void main(String[] args) {
		var samples = new ArrayList<Map<String, Object>>();
		for (int move : new int[] {22, 23}) {
			var style = MastersStyleRules.animation(move);
			int windup = style == null ? new int[] {4, 8, 6}[move] : style.windup();
			int recovery = style == null ? new int[] {12, 18, 14}[move] : style.recovery();
			String name = style == null ? new String[] {"spellcut", "rising_break", "driving_cut"}[move] : style.art();
			for (float age = 0; age <= windup + recovery; age += .25F) {
				var pose = MastersArtAnimation.sample(move, age, windup, recovery);
				var views = new ArrayList<Map<String, Object>>();
				for (boolean left : new boolean[] {false, true}) for (float pitch : new float[] {0, -12})
					views.add(Map.of("left", left, "yawDelta", 0, "pitchDelta", pitch,
						"view", MastersArtAnimation.view(pose, left, 0, 0, pitch)));
				if (move == 22 || move == 23) views.add(Map.of("left", true, "yawDelta", -90, "pitchDelta", -50,
					"view", MastersArtAnimation.view(pose, true, 0, -90, -50)));
				var row = new LinkedHashMap<String, Object>();
				row.put("move", move); row.put("name", name); row.put("windup", windup); row.put("recovery", recovery);
				row.put("age", age); row.put("pose", pose); row.put("views", views); samples.add(row);
			}
		}
		System.out.println(new Gson().toJson(Map.of("kind", "source_projection_samples_not_native_frames", "samples", samples)));
	}
}
