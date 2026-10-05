import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.ArticulatedCombatPose.Joint;
import dev.wildercord.client.combat.ArticulatedArmorGeometry;
import dev.wildercord.client.combat.ArticulatedArmorGeometry.Palette;
import dev.wildercord.client.combat.ArticulatedRig;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

/** Bit-level palette/replay gate; the digest can also compare separate baseline and candidate JVMs. */
public final class CheckArticulatedArmorPalette {
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	public static void main(String[] args) throws Exception {
		var roots = EntityModelSet.vanilla();
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		int snapshots = 0;
		for (boolean slim : new boolean[] {false, true}) {
			var layers = slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR;
			List<ArticulatedArmorGeometry> models = new ArrayList<>();
			for (EquipmentSlot slot : SLOTS) models.add(new ArticulatedArmorGeometry(roots.bakeLayer(layers.get(slot)), slot));
			models.add(new ArticulatedArmorGeometry(roots.bakeLayer(layers.chest()), EquipmentSlot.CHEST, true));
			ArticulatedRig rig = new ArticulatedRig(slim, false);
			for (boolean left : new boolean[] {false, true}) for (int tick = 0; tick <= 128; tick++) {
				var pose = ArticulatedCombatPose.sampleSpellcut(0, tick / 8F, 4, 12, left);
				rig.apply(pose::local);
				Palette captured = Palette.capture(rig);
				Matrix4f[] source = Arrays.stream(Joint.values()).map(captured::matrix).toArray(Matrix4f[]::new);
				Palette copied = Palette.from(joint -> source[joint.ordinal()]);
				for (Matrix4f matrix : source) matrix.zero();
				for (Joint joint : Joint.values()) {
					Matrix4f original = captured.matrix(joint);
					check(original.equals(copied.matrix(joint)), "Palette retained caller-owned matrix");
					copied.matrix(joint).zero();
					check(original.equals(copied.matrix(joint)), "Palette exposed mutable matrix");
				}
				check(captured.withHat(true) == captured && captured.hatVisible(), "Default hat flag changed");
				Palette hidden = captured.withHat(false);
				check(hidden != captured && !hidden.hatVisible() && hidden.withHat(false) == hidden
					&& hidden.withHat(true).hatVisible() && captured.hatVisible(), "Hat variants mutated their source");
				rig.apply(ArticulatedCombatPose.sampleSpellcut(0, tick < 64 ? 12 : 4, 4, 12, !left)::local);
				Palette other = Palette.capture(rig);
				for (ArticulatedArmorGeometry model : models) {
					model.setupAnim(captured);
					int[] first = snapshot(model);
					model.setupAnim(captured);
					check(Arrays.equals(first, snapshot(model)), "A/A changed geometry");
					model.setupAnim(other);
					model.setupAnim(captured);
					check(Arrays.equals(first, snapshot(model)), "A/B/A changed positions, UVs or normals");
					model.setupAnim(copied);
					check(Arrays.equals(first, snapshot(model)), "Caller/rig mutation changed captured geometry");
					model.setupAnim(hidden);
					check(!model.root().getChild("hat").visible, "Hidden hat flag was ignored");
					model.setupAnim(captured);
					check(model.root().getChild("hat").visible && Arrays.equals(first, snapshot(model)), "Hat replay changed geometry");
					ByteBuffer bytes = ByteBuffer.allocate(first.length * Integer.BYTES);
					for (int value : first) bytes.putInt(value);
					digest.update(bytes.array()); snapshots++;
				}
			}
		}
		for (float invalid : new float[] {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
			try {
				Palette.from(joint -> new Matrix4f().m00(invalid));
				throw new AssertionError("Accepted nonfinite matrix");
			} catch (IllegalArgumentException expected) { /* Required palette input check. */ }
		}
		try {
			Palette.from(joint -> null);
			throw new AssertionError("Accepted null matrix");
		} catch (NullPointerException expected) { /* Required palette input check. */ }
		System.out.println("{\"kind\":\"offline palette ownership and bit-exact replay\",\"snapshots\":" + snapshots
			+ ",\"geometrySha256\":\"" + HexFormat.of().formatHex(digest.digest()) + "\",\"passes\":true,\"native\":\"not run\"}");
	}
	private static int[] snapshot(ArticulatedArmorGeometry model) {
		List<Integer> bits = new ArrayList<>();
		model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
			for (var polygon : cube.polygons) {
				for (var v : polygon.vertices()) add(bits, v.x(), v.y(), v.z(), v.u(), v.v());
				add(bits, polygon.normal().x(), polygon.normal().y(), polygon.normal().z());
			}
		});
		return bits.stream().mapToInt(Integer::intValue).toArray();
	}
	private static void add(List<Integer> bits, float... values) {
		for (float value : values) {
			check(Float.isFinite(value), "Nonfinite geometry");
			bits.add(Float.floatToIntBits(value));
		}
	}
	private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
}
