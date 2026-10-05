import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.client.combat.ArticulatedArmorGeometry;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Offline source-geometry audit against the official runtime slot bake and texture. No client,
 * GPU, mixin or native test is launched. It measures the optional armor overlay only: the existing
 * skin/sword evidence remains separate and native transparency/material acceptance is still needed.
 */
public final class CheckArticulatedArmorView {
	private static final double NEAR = -.05;
	private static final int WIDTH = 256, HEIGHT = 144;
	private static final double FOCAL = HEIGHT / (2 * Math.tan(Math.toRadians(70 / 2.0)));
	private static final String TEXTURE = "/assets/minecraft/textures/entity/equipment/humanoid/netherite.png";
	private record Face(ModelPart.Vertex[] vertices) {}

	public static void main(String[] args) throws Exception {
		var roots = EntityModelSet.vanilla();
		BufferedImage texture;
		try (var stream = CheckArticulatedArmorView.class.getResourceAsStream(TEXTURE)) {
			if (stream == null) throw new IllegalStateException("Official runtime netherite texture is missing");
			texture = ImageIO.read(stream);
		}
		StringBuilder out = new StringBuilder("{\"kind\":\"offline source geometry, not native acceptance\",\"handFov\":70,\"nearPlane\":0.05,\"texture\":\"")
			.append(TEXTURE).append("\",\"variants\":[");
		boolean comma = false;
		boolean pass = true;
		for (boolean slim : new boolean[] {false, true}) for (boolean left : new boolean[] {false, true}) {
			var model = new ArticulatedArmorGeometry(roots.bakeLayer((slim ? ModelLayers.PLAYER_SLIM_ARMOR : ModelLayers.PLAYER_ARMOR).chest()), EquipmentSlot.CHEST, true);
			double nearest = -Double.MAX_VALUE, worstAge = 0, worstYaw = 0, worstPitch = 0, maxCenterCoverage = 0;
			int placements = 0, crosshairOccluded = 0;
			for (int step = 0; step <= 128; step++) {
				float age = step / 8F;
				var combat = ArticulatedCombatPose.sampleSpellcut(0, age, 4, 12, left);
				var view = ArticulatedCombatPose.view(combat, left);
				model.setupAnim(ArticulatedArmorGeometry.Palette.from(joint -> new Matrix4f().set(view.world(joint).values())));
				List<Face> faces = new ArrayList<>();
				model.root().visit(new PoseStack(), (pose, path, index, cube) -> {
					for (var polygon : cube.polygons) faces.add(new Face(polygon.vertices().clone()));
				});
				for (int yi = -4; yi <= 4; yi++) for (int pi = -4; pi <= 4; pi++) {
					float yaw = yi * 6.25F * combat.weight(), pitch = pi * 5F * combat.weight();
					float clearance = ArticulatedCombatPose.viewClearance(yaw, pitch);
					Matrix4f camera = new Matrix4f().translation(view.origin().x(), view.origin().y() - .8F * clearance, view.origin().z() - clearance)
						.rotateY((float) Math.toRadians(-yaw)).rotateX((float) Math.toRadians(-pitch)).scale(-1F / 16, -1F / 16, 1F / 16);
					placements++;
					for (Face face : faces) for (var vertex : face.vertices()) {
						Vector3f point = camera.transformPosition(vertex.x(), vertex.y(), vertex.z(), new Vector3f());
						if (!point.isFinite()) throw new AssertionError("Non-finite camera-space armor vertex");
						if (point.z > nearest) { nearest = point.z; worstAge = age; worstYaw = yaw; worstPitch = pitch; }
					}
					if (yi == 0 && pi == 0) {
						boolean[][] covered = new boolean[HEIGHT][WIDTH];
						for (Face face : faces) cover(face, camera, texture, covered);
						if (covered[72][128]) crosshairOccluded++;
						int central = 0;
						for (int y = 68; y < 76; y++) for (int x = 121; x < 135; x++) if (covered[y][x]) central++;
						maxCenterCoverage = Math.max(maxCenterCoverage, central / 112.0);
					}
				}
			}
			boolean safe = nearest < NEAR && crosshairOccluded == 0;
			pass &= safe;
			if (comma) out.append(','); comma = true;
			out.append("{\"slim\":").append(slim).append(",\"left\":").append(left).append(",\"placements\":").append(placements)
				.append(",\"nearestZ\":").append(nearest).append(",\"clearanceBlocks\":").append(NEAR - nearest)
				.append(",\"worstAge\":").append(worstAge).append(",\"worstYaw\":").append(worstYaw).append(",\"worstPitch\":").append(worstPitch)
				.append(",\"crosshairOccludedFrames\":").append(crosshairOccluded).append(",\"maxCentralCoverage\":").append(maxCenterCoverage)
				.append(",\"passes\":").append(safe).append('}');
		}
		out.append("],\"passes\":").append(pass).append(",\"limits\":[\"Armor geometry only; separate skin/sword coverage is not combined here.\",\"All vertices including transparent texels bound near-plane clearance.\",\"Alpha coverage samples the original texture with nearest sampling; no native trim, glint, lighting or depth pipeline is simulated.\",\"The camera transform matches production weighted free-look and clearance.\"]}");
		System.out.println(out);
		if (!pass) throw new AssertionError("Optional armor-arms camera gate failed; inspect report");
	}

	private static void cover(Face face, Matrix4f camera, BufferedImage texture, boolean[][] covered) {
		Vector3f[] p = new Vector3f[4];
		for (int i = 0; i < 4; i++) {
			var vertex = face.vertices()[i];
			p[i] = camera.transformPosition(vertex.x(), vertex.y(), vertex.z(), new Vector3f());
			if (p[i].z >= NEAR) return;
		}
		for (int[] tri : new int[][] {{0, 1, 2}, {0, 2, 3}}) {
			double[] x = new double[3], y = new double[3], depth = new double[3];
			for (int i = 0; i < 3; i++) {
				var point = p[tri[i]]; depth[i] = -point.z;
				x[i] = WIDTH / 2.0 + FOCAL * point.x / depth[i]; y[i] = HEIGHT / 2.0 - FOCAL * point.y / depth[i];
			}
			double area = edge(x[0], y[0], x[1], y[1], x[2], y[2]);
			if (Math.abs(area) < 1e-8) continue;
			int minX = Math.max(121, (int) Math.floor(Math.min(x[0], Math.min(x[1], x[2]))));
			int maxX = Math.min(134, (int) Math.ceil(Math.max(x[0], Math.max(x[1], x[2]))));
			int minY = Math.max(68, (int) Math.floor(Math.min(y[0], Math.min(y[1], y[2]))));
			int maxY = Math.min(75, (int) Math.ceil(Math.max(y[0], Math.max(y[1], y[2]))));
			for (int py = minY; py <= maxY; py++) for (int px = minX; px <= maxX; px++) {
				double a = edge(x[1], y[1], x[2], y[2], px + .5, py + .5) / area;
				double b = edge(x[2], y[2], x[0], y[0], px + .5, py + .5) / area, c = 1 - a - b;
				if (a < -1e-7 || b < -1e-7 || c < -1e-7) continue;
				double denominator = a / depth[0] + b / depth[1] + c / depth[2];
				double u = (a * face.vertices()[tri[0]].u() / depth[0] + b * face.vertices()[tri[1]].u() / depth[1] + c * face.vertices()[tri[2]].u() / depth[2]) / denominator;
				double v = (a * face.vertices()[tri[0]].v() / depth[0] + b * face.vertices()[tri[1]].v() / depth[1] + c * face.vertices()[tri[2]].v() / depth[2]) / denominator;
				int tx = Math.max(0, Math.min(texture.getWidth() - 1, (int) (u * texture.getWidth())));
				int ty = Math.max(0, Math.min(texture.getHeight() - 1, (int) (v * texture.getHeight())));
				if ((texture.getRGB(tx, ty) >>> 24) > 127) covered[py][px] = true;
			}
		}
	}
	private static double edge(double ax, double ay, double bx, double by, double px, double py) {
		return (px - ax) * (by - ay) - (py - ay) * (bx - ax);
	}
}
