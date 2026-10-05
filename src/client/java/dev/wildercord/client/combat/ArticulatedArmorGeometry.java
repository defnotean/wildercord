package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.ArticulatedArmorMesh;
import dev.wildercord.aura.ArticulatedArmorMesh.Region;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.ArticulatedCombatPose.Joint;
import dev.wildercord.aura.ArticulatedCombatPose.Vec3;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Renderer-local, reload-owned armor model. Runtime vanilla baked quads supply every position,
 * mirror and UV, so 64x32 armor and trim mappings never pass through the player's 64x64 skin
 * layout. The supplied armor bake also retains vanilla's four-pixel arms for slim players.
 * All material passes render this same welded, open-at-internal-cuts mesh and immutable palette.
 * This class owns no texture, GPU resource, skin/sleeve visibility, or entity render state.
 */
public final class ArticulatedArmorGeometry extends Model<ArticulatedArmorGeometry.Palette> {
	/** Immutable final joint-world snapshot. A deferred draw never reads a live player or rig. */
	public static final class Palette {
		// Bind transforms are pose-, player- and reload-independent. Only read these matrices;
		// every captured world and skin matrix below still belongs to its own palette.
		private static final Matrix4fc[] INVERSE_BIND = inverseBind();
		private final Matrix4f[] world, skin;
		private final boolean hat;
		private Palette(Function<Joint, Matrix4f> matrices) {
			hat = true;
			world = new Matrix4f[Joint.values().length];
			skin = new Matrix4f[world.length];
			for (Joint joint : Joint.values()) {
				Matrix4f matrix = new Matrix4f(Objects.requireNonNull(matrices.apply(joint), joint.name()));
				if (!matrix.isFinite()) throw new IllegalArgumentException("Non-finite armor palette: " + joint);
				world[joint.ordinal()] = matrix;
				skin[joint.ordinal()] = new Matrix4f(matrix).mul(INVERSE_BIND[joint.ordinal()]);
			}
		}
		private static Matrix4fc[] inverseBind() {
			Matrix4fc[] matrices = new Matrix4fc[Joint.values().length];
			for (Joint joint : Joint.values())
				matrices[joint.ordinal()] = new Matrix4f().set(ArticulatedCombatPose.NONE.world(joint).values()).invert();
			return matrices;
		}
		private Palette(Palette source, boolean hat) { world = source.world; skin = source.skin; this.hat = hat; }
		public Palette withHat(boolean visible) { return hat == visible ? this : new Palette(this, visible); }
		public boolean hatVisible() { return hat; }
		public static Palette from(Function<Joint, Matrix4f> worldPixelMatrices) { return new Palette(worldPixelMatrices); }
		public Matrix4f matrix(Joint joint) { return new Matrix4f(world[joint.ordinal()]); }

		/** Call only after the rig has received baseline, free-look and any other final corrections. */
		public static Palette capture(ArticulatedRig rig) {
			return from(joint -> {
				PoseStack stack = new PoseStack();
				rig.transformTo(joint, stack);
				Matrix4f matrix = new Matrix4f(stack.last().pose());
				return matrix.m30(matrix.m30() * 16).m31(matrix.m31() * 16).m32(matrix.m32() * 16);
			});
		}
		Vec3 transform(Joint joint, Vec3 point) {
			Vector3f transformed = skin[joint.ordinal()].transformPosition(point.x(), point.y(), point.z(), new Vector3f());
			return new Vec3(transformed.x, transformed.y, transformed.z);
		}
	}

	private record Baked(ArticulatedArmorMesh.Mesh mesh, ModelPart root, ModelPart hat, List<ModelPart.Cube> faces, boolean armsOnly) {}
	private final ArticulatedArmorMesh.Mesh mesh;
	private final List<ModelPart.Cube> faces;
	private final boolean armsOnly;
	private final ModelPart hat;
	private Palette lastPalette;

	/** The source must be a fresh, unposed vanilla equipment-slot layer bake. It is never retained. */
	public ArticulatedArmorGeometry(ModelPart bakedVanillaSlotRoot, EquipmentSlot slot) { this(bakedVanillaSlotRoot, slot, false); }
	public ArticulatedArmorGeometry(ModelPart bakedVanillaSlotRoot, EquipmentSlot slot, boolean armsOnly) { this(bake(bakedVanillaSlotRoot, slot, armsOnly)); }
	private ArticulatedArmorGeometry(Baked baked) {
		super(baked.root(), RenderTypes::armorCutoutNoCull);
		mesh = baked.mesh(); faces = baked.faces(); armsOnly = baked.armsOnly(); hat = baked.hat();
		setupAnim(Palette.from(joint -> new Matrix4f().set(ArticulatedCombatPose.NONE.world(joint).values())));
	}

	@Override
	public void setupAnim(Palette palette) {
		Objects.requireNonNull(palette);
		hat.visible = palette.hatVisible();
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
	public boolean armsOnly() { return armsOnly; }

	private static Baked bake(ModelPart sourceRoot, EquipmentSlot slot, boolean armsOnly) {
		Set<String> required = switch (Objects.requireNonNull(slot)) {
			case HEAD -> Set.of("/head", "/head/hat");
			case CHEST -> Set.of("/body", "/right_arm", "/left_arm");
			case LEGS -> Set.of("/body", "/right_leg", "/left_leg");
			case FEET -> Set.of("/right_leg", "/left_leg");
			default -> throw new IllegalArgumentException("Unsupported armor slot: " + slot);
		};
		if (armsOnly && slot != EquipmentSlot.CHEST) throw new IllegalArgumentException("Arm-only armor requires a complete chest slot bake");
		Set<String> visited = new HashSet<>();
		List<ArticulatedArmorMesh.SourceFace> source = new ArrayList<>();
		Set<Integer> hatSources = new HashSet<>();
		// Public visitor avoids accessors and copies vertices; neither ModelParts nor polygons escape.
		sourceRoot.visit(new PoseStack(), (pose, path, index, cube) -> {
			if (!required.contains(path) || !visited.add(path)) throw new IllegalArgumentException("Unexpected armor geometry for " + slot + ": " + path);
			Region region = region(path);
			verifyVanillaCube(pose, path, index, cube, region, slot);
			if (armsOnly && !region.arm()) return;
			for (ModelPart.Polygon polygon : cube.polygons) {
				List<ArticulatedArmorMesh.SourceVertex> vertices = new ArrayList<>(4);
				for (ModelPart.Vertex vertex : polygon.vertices()) {
					Vector3f root = pose.pose().transformPosition(vertex.worldX(), vertex.worldY(), vertex.worldZ(), new Vector3f()).mul(16);
					vertices.add(new ArticulatedArmorMesh.SourceVertex(new Vec3(root.x, root.y, root.z), vertex.u(), vertex.v()));
				}
				if (path.equals("/head/hat")) hatSources.add(source.size());
				source.add(new ArticulatedArmorMesh.SourceFace(region, vertices));
			}
		});
		// Validate the complete source before using the view mask. An unequipped slot is a runtime
		// item choice; a missing baked limb is unsupported geometry and must keep the whole fallback.
		if (!visited.equals(required)) throw new IllegalArgumentException("Incomplete armor geometry for " + slot + ": expected " + required + ", found " + visited);
		ArticulatedArmorMesh.Mesh mesh = ArticulatedArmorMesh.bake(source);
		List<ModelPart.Cube> cubes = new ArrayList<>(mesh.faces().size());
		List<ModelPart.Cube> mainCubes = new ArrayList<>(), hatCubes = new ArrayList<>();
		for (var face : mesh.faces()) {
			float minX = Float.POSITIVE_INFINITY, minY = minX, minZ = minX;
			float maxX = Float.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
			for (var corner : face.corners()) {
				Vec3 p = mesh.controlPoints().get(corner.controlPoint()).bindPosition();
				minX = Math.min(minX, p.x()); minY = Math.min(minY, p.y()); minZ = Math.min(minZ, p.z());
				maxX = Math.max(maxX, p.x()); maxY = Math.max(maxY, p.y()); maxZ = Math.max(maxZ, p.z());
			}
			// A one-face storage cube participates in the ordinary Model/VertexConsumer path. Its
			// placeholder polygon is replaced above, never rendered as a second or capped cuboid.
			ModelPart.Cube cube = new ModelPart.Cube(0, 0, minX, minY, minZ, maxX - minX, maxY - minY, maxZ - minZ,
				0, 0, 0, false, 64, 32, EnumSet.of(Direction.NORTH));
			cubes.add(cube);
			(hatSources.contains(face.sourceFace()) ? hatCubes : mainCubes).add(cube);
		}
		ModelPart hat = new ModelPart(hatCubes, Map.of());
		return new Baked(mesh, new ModelPart(mainCubes, Map.of("hat", hat)), hat, List.copyOf(cubes), armsOnly);
	}

	/** Texture-only packs remain valid; unknown altered geometry must keep the original renderer. */
	private static void verifyVanillaCube(PoseStack.Pose pose, String path, int index, ModelPart.Cube cube, Region region, EquipmentSlot slot) {
		if (index != 0) throw new IllegalArgumentException("Additional armor cube: " + path);
		boolean hat = path.equals("/head/hat"), left = region == Region.LEFT_ARM || region == Region.LEFT_LEG;
		int u = region == Region.HEAD ? hat ? 32 : 0 : region == Region.BODY ? 16 : region.arm() ? 40 : 0;
		int v = region == Region.HEAD ? 0 : 16;
		float x = region == Region.HEAD || region == Region.BODY ? -4 : region.arm() ? left ? -1 : -3 : -2;
		float y = region == Region.HEAD ? -8 : region.arm() ? -2 : 0;
		float z = region == Region.HEAD ? -4 : -2, width = region == Region.HEAD || region == Region.BODY ? 8 : 4;
		float height = region == Region.HEAD ? 8 : 12, depth = region == Region.HEAD ? 8 : 4;
		float px = region.arm() ? left ? 5 : -5 : region == Region.LEFT_LEG ? 1.9F : region == Region.RIGHT_LEG ? -1.9F : 0;
		float py = region.arm() ? 2 : region == Region.LEFT_LEG || region == Region.RIGHT_LEG ? 12 : 0;
		if (!new Matrix4f().translation(px / 16, py / 16, 0).equals(pose.pose(), 1.0e-6F))
			throw new IllegalArgumentException("Posed or altered armor bind transform: " + path);
		float grow = slot == EquipmentSlot.HEAD ? hat ? 1.5F : 1
			: slot == EquipmentSlot.CHEST ? 1 : slot == EquipmentSlot.FEET ? .9F : region == Region.BODY ? .5F : .4F;
		ModelPart.Cube expected = new ModelPart.Cube(u, v, x, y, z, width, height, depth, grow, grow, grow,
			left, 64, 32, EnumSet.allOf(Direction.class));
		boolean same = cube.polygons.length == expected.polygons.length;
		for (int i = 0; same && i < cube.polygons.length; i++) {
			same = Arrays.equals(cube.polygons[i].vertices(), expected.polygons[i].vertices())
				&& cube.polygons[i].normal().equals(expected.polygons[i].normal());
		}
		if (same) return;
		throw new IllegalArgumentException("Altered armor geometry or UV layout: " + path);
	}

	private static Region region(String path) {
		String name = path.startsWith("/") ? path.substring(1) : path;
		return switch (name) {
			case "head", "head/hat" -> Region.HEAD;
			case "body" -> Region.BODY;
			case "right_arm" -> Region.RIGHT_ARM;
			case "left_arm" -> Region.LEFT_ARM;
			case "right_leg" -> Region.RIGHT_LEG;
			case "left_leg" -> Region.LEFT_LEG;
			default -> throw new IllegalArgumentException("Unsupported armor part: " + path);
		};
	}
}
