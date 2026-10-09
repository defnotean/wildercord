package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.ArticulatedArmorMesh;
import dev.wildercord.aura.ArticulatedArmorMesh.Region;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.ArticulatedCombatPose.Vec3;
import dev.wildercord.client.render.AuraShellLayer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The authored aura shell's original exterior quads, subdivided at the existing body joints.
 * Runtime shell bakes supply the 64x64 UVs and .55px stand-off. No caps, duplicate rigid shell,
 * skin overlays, or copied texture assets are added. Every deferred pass uses its own palette.
 */
public final class ArticulatedAuraShellGeometry extends Model<ArticulatedArmorGeometry.Palette> {
	private record Source(Matrix4f pose, ModelPart.Cube cube) {}
	private record Baked(ModelPart root, ArticulatedArmorMesh.Mesh mesh, List<ModelPart.Cube> faces) {}
	private final ArticulatedArmorMesh.Mesh mesh;
	private final List<ModelPart.Cube> faces;
	private ArticulatedArmorGeometry.Palette lastPalette;

	public ArticulatedAuraShellGeometry(ModelPart source, boolean slim, boolean armsOnly) {
		this(bake(source, slim, armsOnly));
	}
	private ArticulatedAuraShellGeometry(Baked baked) {
		super(baked.root(), RenderTypes::eyes);
		mesh = baked.mesh(); faces = baked.faces();
		setupAnim(ArticulatedArmorGeometry.Palette.from(joint -> new Matrix4f().set(ArticulatedCombatPose.NONE.world(joint).values())));
	}

	@Override
	public void setupAnim(ArticulatedArmorGeometry.Palette palette) {
		Objects.requireNonNull(palette);
		if (lastPalette == palette) return;
		var deformed = mesh.deform(palette::transform);
		for (int i = 0; i < mesh.faces().size(); i++) {
			var face = mesh.faces().get(i);
			ModelPart.Vertex[] vertices = new ModelPart.Vertex[4];
			for (int j = 0; j < 4; j++) {
				var corner = face.corners().get(j);
				Vec3 point = deformed.positions().get(corner.controlPoint());
				vertices[j] = new ModelPart.Vertex(point.x(), point.y(), point.z(), corner.u(), corner.v());
			}
			Vec3 normal = deformed.normals().get(i);
			faces.get(i).polygons[0] = new ModelPart.Polygon(vertices, new Vector3f(normal.x(), normal.y(), normal.z()));
		}
		lastPalette = palette;
	}

	public ArticulatedArmorMesh.Mesh mesh() { return mesh; }

	private static Baked bake(ModelPart source, boolean slim, boolean armsOnly) {
		ModelPart expectedRoot = (slim ? AuraShellLayer.createSlimShell() : AuraShellLayer.createShell()).bakeRoot();
		Map<String, Source> expected = new LinkedHashMap<>();
		expectedRoot.visit(new PoseStack(), (pose, path, index, cube) -> expected.put(path + ":" + index, new Source(new Matrix4f(pose.pose()), cube)));
		List<ArticulatedArmorMesh.SourceFace> sources = new ArrayList<>();
		source.visit(new PoseStack(), (pose, path, index, cube) -> {
			Source original = expected.remove(path + ":" + index);
			if (original == null || !original.pose().equals(pose.pose(), 1.0e-6F) || !same(original.cube(), cube))
				throw new IllegalArgumentException("Altered aura shell geometry or UVs: " + path);
			Region region = switch (path) {
				case "/head" -> Region.HEAD;
				case "/body" -> Region.BODY;
				case "/right_arm" -> Region.RIGHT_ARM;
				case "/left_arm" -> Region.LEFT_ARM;
				case "/right_leg" -> Region.RIGHT_LEG;
				case "/left_leg" -> Region.LEFT_LEG;
				default -> null; // ShellModel suppresses every skin second layer in its fallback too.
			};
			if (region == null || armsOnly && !region.arm()) return;
			for (var polygon : cube.polygons) {
				List<ArticulatedArmorMesh.SourceVertex> vertices = new ArrayList<>(4);
				for (var vertex : polygon.vertices()) {
					Vector3f point = pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f()).mul(16);
					vertices.add(new ArticulatedArmorMesh.SourceVertex(new Vec3(point.x, point.y, point.z), vertex.u(), vertex.v()));
				}
				sources.add(new ArticulatedArmorMesh.SourceFace(region, vertices));
			}
		});
		if (!expected.isEmpty()) throw new IllegalArgumentException("Incomplete aura shell geometry: " + expected.keySet());
		var mesh = ArticulatedArmorMesh.bakeAuraShell(sources);
		List<ModelPart.Cube> faces = new ArrayList<>();
		for (var face : mesh.faces()) {
			// One storage polygon per original/subdivided exterior face. setupAnim fills it.
			faces.add(new ModelPart.Cube(0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, false, 64, 64, EnumSet.of(Direction.NORTH)));
		}
		return new Baked(new ModelPart(faces, Map.of()), mesh, List.copyOf(faces));
	}
	private static boolean same(ModelPart.Cube expected, ModelPart.Cube actual) {
		if (expected.polygons.length != actual.polygons.length) return false;
		for (int i = 0; i < expected.polygons.length; i++) {
			if (!Arrays.equals(expected.polygons[i].vertices(), actual.polygons[i].vertices())
				|| !expected.polygons[i].normal().equals(actual.polygons[i].normal())) return false;
		}
		return true;
	}
}
