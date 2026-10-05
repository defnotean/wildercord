import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.ArticulatedCombatPose.Joint;
import dev.wildercord.aura.MastersArtRules;
import dev.wildercord.client.combat.ArticulatedArmorGeometry;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

/**
 * Bounded OFFLINE export of the production armor ModelPart polygons after setupAnim.
 * Uses pristine EntityModelSet.vanilla equipment-slot bakes, never a client or GPU.
 * Run with preview_articulated_shared_armor.py for source/asset/dependency provenance.
 */
public final class ExportArticulatedSharedArmor {
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private ExportArticulatedSharedArmor() {}

	public static void main(String[] args) {
		if (args.length != 0) throw new IllegalArgumentException("This exporter has a fixed, bounded shared-player keyframe domain");
		var roots = EntityModelSet.vanilla();
		StringBuilder json = new StringBuilder(4_000_000);
		json.append("{\"schema\":1,\"label\":\"OFFLINE SOURCE-DRIVEN PREVIEW, NOT GAME FOOTAGE\",\"units\":\"model_pixels\",\"frames\":[");
		boolean comma = false;
		for (boolean slim : new boolean[] {false, true}) {
			var layers = slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR;
			Map<EquipmentSlot, ArticulatedArmorGeometry> models = new EnumMap<>(EquipmentSlot.class);
			for (EquipmentSlot slot : SLOTS) models.put(slot, new ArticulatedArmorGeometry(roots.bakeLayer(layers.get(slot)), slot));
			var viewModel = new ArticulatedArmorGeometry(roots.bakeLayer(layers.chest()), EquipmentSlot.CHEST, true);
			if (viewModel.mesh().controlPoints().stream().anyMatch(point -> !point.region().arm()))
				throw new AssertionError("First-person armor contains a non-arm region");
			for (boolean left : new boolean[] {false, true}) for (int move : new int[] {1, 2}) {
				var rule = MastersArtRules.move(move);
				float[] ages = move == 1 ? new float[] {5.25F, 8, 12, 21} : new float[] {4, 6, 9.5F, 16};
				for (float age : ages) {
					var pose = ArticulatedCombatPose.samplePlayer(move, age, rule.windup(), rule.recovery(), left);
					var view = ArticulatedCombatPose.view(pose, left);
					var palette = ArticulatedArmorGeometry.Palette.from(joint -> new Matrix4f().set(pose.world(joint).values()));
					var viewPalette = ArticulatedArmorGeometry.Palette.from(joint -> new Matrix4f().set(view.world(joint).values()));
					if (comma) json.append(','); comma = true;
					json.append("{\"variant\":\"").append(slim ? "slim" : "wide").append("\",\"left\":").append(left)
						.append(",\"move\":").append(move).append(",\"clip\":\"").append(rule.id()).append("\",\"age\":").append(age)
						.append(",\"weight\":").append(pose.weight()).append(",\"phase\":\"").append(pose.phase()).append("\",\"bodyWorld\":[");
					for (Joint joint : Joint.values()) { if (joint.ordinal() > 0) json.append(','); matrix(json, palette.matrix(joint)); }
					json.append("],\"viewWorld\":[");
					for (Joint joint : Joint.values()) { if (joint.ordinal() > 0) json.append(','); matrix(json, viewPalette.matrix(joint)); }
					json.append("],\"third\":[");
					for (int i = 0; i < SLOTS.length; i++) {
						EquipmentSlot slot = SLOTS[i]; var model = models.get(slot); model.setupAnim(palette);
						if (i > 0) json.append(',');
						json.append("{\"slot\":\"").append(slot.name()).append("\",\"layer\":\"").append(layers.get(slot)).append("\",\"faces\":");
						faces(json, model); json.append('}');
					}
					json.append("],\"first\":{\"slot\":\"CHEST\",\"armsOnly\":true,\"layer\":\"").append(layers.chest()).append("\",\"faces\":");
					viewModel.setupAnim(viewPalette); faces(json, viewModel);
					json.append("},\"viewPlacements\":[");
					// Export the exact production weighted-free-look camera transforms, including clearance.
					for (int i = 0; i < 3; i++) {
						if (i > 0) json.append(',');
						float yaw = (i == 0 ? 0 : i == 1 ? -25 : 25) * pose.weight();
						float pitch = (i == 0 ? 0 : -20) * pose.weight();
						float clearance = ArticulatedCombatPose.viewClearance(yaw, pitch);
						Matrix4f camera = new Matrix4f().translation(view.origin().x(), view.origin().y() - .8F * clearance, view.origin().z() - clearance)
							.rotateY((float) Math.toRadians(-yaw)).rotateX((float) Math.toRadians(-pitch)).scale(-1F / 16, -1F / 16, 1F / 16);
						json.append("{\"yaw\":").append(yaw).append(",\"pitch\":").append(pitch).append(",\"clearance\":").append(clearance).append(",\"camera\":");
						matrix(json, camera); json.append('}');
					}
					json.append("]}");
				}
			}
		}
		System.out.println(json.append("]}"));
	}

	private static void matrix(StringBuilder json, Matrix4f matrix) {
		float[] values = matrix.get(new float[16]); json.append('[');
		for (int i = 0; i < values.length; i++) { if (i > 0) json.append(','); json.append(values[i]); }
		json.append(']');
	}

	private static void faces(StringBuilder json, ArticulatedArmorGeometry model) {
		json.append('['); int[] count = {0};
		model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
			for (ModelPart.Polygon polygon : cube.polygons) {
				if (count[0]++ > 0) json.append(','); json.append('[');
				for (int i = 0; i < polygon.vertices().length; i++) {
					if (i > 0) json.append(','); var vertex = polygon.vertices()[i];
					Vector3f p = pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f()).mul(16);
					if (!p.isFinite() || !Float.isFinite(vertex.u()) || !Float.isFinite(vertex.v())) throw new AssertionError("Non-finite armor polygon");
					json.append('[').append(p.x).append(',').append(p.y).append(',').append(p.z).append(',').append(vertex.u()).append(',').append(vertex.v()).append(']');
				}
				json.append(']');
			}
		});
		if (count[0] != model.mesh().faces().size()) throw new AssertionError("ModelPart polygon count changed");
		json.append(']');
	}
}
