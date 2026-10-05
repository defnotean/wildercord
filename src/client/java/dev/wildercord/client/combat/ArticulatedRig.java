package dev.wildercord.client.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.aura.ArticulatedCombatPose;
import dev.wildercord.aura.ArticulatedCombatPose.Joint;
import dev.wildercord.aura.ArticulatedCombatPose.Transform;
import dev.wildercord.client.mixin.ModelPartChildrenAccessor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.HumanoidArm;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Original rigid-segment geometry with actual elbow, wrist, knee, ankle and torso hinges. This is
 * not smooth weighted skinning. Skin side faces are cut from the original 64x64 UV rectangles,
 * rather than shrinking a complete arm texture onto each segment. No external animation assets.
 */
public final class ArticulatedRig {
	public static final String CHILD = "wildercord_articulated";
	private final EnumMap<Joint, ModelPart> parts = new EnumMap<>(Joint.class);
	private final Map<ModelPart, String> overlays = new LinkedHashMap<>();
	public final ModelPart root;
	private final boolean slim;
	private final boolean master;

	public ArticulatedRig(boolean slim, boolean master) {
		this.slim = slim;
		this.master = master;
		Map<Joint, List<ModelPart.Cube>> cubes = new EnumMap<>(Joint.class);
		for (Joint joint : Joint.values()) cubes.put(joint, new ArrayList<>());
		body(cubes, Joint.PELVIS, 9, 12, 12);
		body(cubes, Joint.SPINE, 4, 9, 9);
		body(cubes, Joint.CHEST, 0, 4, 4);
		cubes.get(Joint.HEAD).add(cube(0, 0, -4, -8, -4, 8, 8, 8, 0, false));
		for (boolean left : new boolean[] {false, true}) {
			int u = left && !master ? 32 : 40, v = left && !master ? 48 : 16;
			float x = left ? -1 : slim ? -2 : -3, width = slim ? 3 : 4;
			boolean mirror = master && left;
			Joint upper = left ? Joint.LEFT_UPPER_ARM : Joint.RIGHT_UPPER_ARM;
			Joint lower = left ? Joint.LEFT_FOREARM : Joint.RIGHT_FOREARM;
			Joint hand = left ? Joint.LEFT_HAND : Joint.RIGHT_HAND;
			cubes.get(upper).add(slice(u, v, x, -2, -2, width, 12, 4, 0, 6, 0, 0, mirror));
			cubes.get(lower).add(slice(u, v, x, -2, -2, width, 12, 4, 6, 10, 4, 0, mirror));
			cubes.get(hand).add(slice(u, v, x, -2, -2, width, 12, 4, 10, 12, 8, 0, mirror));
			int legU = left && !master ? 16 : 0, legV = left && !master ? 48 : 16;
			Joint thigh = left ? Joint.LEFT_THIGH : Joint.RIGHT_THIGH;
			Joint shin = left ? Joint.LEFT_SHIN : Joint.RIGHT_SHIN;
			Joint foot = left ? Joint.LEFT_FOOT : Joint.RIGHT_FOOT;
			cubes.get(thigh).add(slice(legU, legV, -2, 0, -2, 4, 12, 4, 0, 6, 0, 0, mirror));
			cubes.get(shin).add(slice(legU, legV, -2, 0, -2, 4, 12, 4, 6, 10, 6, 0, mirror));
			cubes.get(foot).add(slice(legU, legV, -2, 0, -2, 4, 12, 4, 10, 12, 10, 0, mirror));
		}
		Map<Joint, Map<String, ModelPart>> children = new EnumMap<>(Joint.class);
		for (Joint joint : Joint.values()) {
			Map<String, ModelPart> childMap = new LinkedHashMap<>();
			children.put(joint, childMap);
			ModelPart part = new ModelPart(cubes.get(joint), childMap);
			parts.put(joint, part);
		}
		Map<String, ModelPart> roots = new LinkedHashMap<>();
		for (Joint joint : Joint.values()) {
			Joint parent = joint.parent();
			(parent == null ? roots : children.get(parent)).put(joint.name(), parts.get(joint));
		}
		root = new ModelPart(List.of(), roots);
		if (!master) {
			overlay(children.get(Joint.HEAD), "hat", cube(32, 0, -4, -8, -4, 8, 8, 8, .5F, false));
			for (Joint joint : new Joint[] {Joint.PELVIS, Joint.SPINE, Joint.CHEST}) {
				float start = joint == Joint.PELVIS ? 9 : joint == Joint.SPINE ? 4 : 0;
				float end = joint == Joint.PELVIS ? 12 : joint == Joint.SPINE ? 9 : 4;
				overlay(children.get(joint), "jacket", slice(16, 32, -4, 0, -2, 8, 12, 4, start, end, end, .25F, false));
			}
			for (boolean left : new boolean[] {false, true}) {
				int u = left ? 48 : 40, v = left ? 48 : 32;
				float x = left ? -1 : slim ? -2 : -3, width = slim ? 3 : 4;
				Joint[] arms = left ? new Joint[] {Joint.LEFT_UPPER_ARM, Joint.LEFT_FOREARM, Joint.LEFT_HAND}
					: new Joint[] {Joint.RIGHT_UPPER_ARM, Joint.RIGHT_FOREARM, Joint.RIGHT_HAND};
				Joint[] legs = left ? new Joint[] {Joint.LEFT_THIGH, Joint.LEFT_SHIN, Joint.LEFT_FOOT}
					: new Joint[] {Joint.RIGHT_THIGH, Joint.RIGHT_SHIN, Joint.RIGHT_FOOT};
				float[] start = {0, 6, 10}, end = {6, 10, 12}, armOrigin = {0, 4, 8}, legOrigin = {0, 6, 10};
				for (int i = 0; i < 3; i++) {
					overlay(children.get(arms[i]), left ? "left_sleeve" : "right_sleeve",
						slice(u, v, x, -2, -2, width, 12, 4, start[i], end[i], armOrigin[i], .25F, false));
					overlay(children.get(legs[i]), left ? "left_pants" : "right_pants",
						slice(0, left ? 48 : 32, -2, 0, -2, 4, 12, 4, start[i], end[i], legOrigin[i], .25F, false));
				}
			}
		}
		apply(ArticulatedCombatPose.NONE::local);
		root.visible = false;
	}

	private static void body(Map<Joint, List<ModelPart.Cube>> cubes, Joint joint, float start, float end, float origin) {
		cubes.get(joint).add(slice(16, 16, -4, 0, -2, 8, 12, 4, start, end, origin, 0, false));
	}

	private void overlay(Map<String, ModelPart> children, String name, ModelPart.Cube cube) {
		ModelPart part = new ModelPart(List.of(cube), new LinkedHashMap<>());
		children.put(name, part);
		overlays.put(part, name);
	}

	/** Called once per baked renderer, not per frame. Accessor is isolated to attachment. */
	public void attach(ModelPart modelRoot) {
		Map<String, ModelPart> children = ((ModelPartChildrenAccessor) (Object) modelRoot).wildercord$children();
		if (children.putIfAbsent(CHILD, root) != null) throw new IllegalStateException("Duplicate articulated root");
	}

	/** Carries the original Master's authored clothes on the new chest, pelvis and head sockets. */
	public void masterAccessories(HumanoidModel<?> model) {
		if (!master) return;
		add(parts.get(Joint.HEAD), "hood", model.hat);
		ModelPart shoulders = emptyAt(0, -4, 0);
		add(shoulders, "cloak", model.body.getChild("cloak"));
		add(shoulders, "mantle", model.body.getChild("mantle"));
		add(parts.get(Joint.CHEST), "travelling_clothes", shoulders);
		ModelPart hip = emptyAt(0, -12, 0);
		add(hip, "scabbard", model.body.getChild("scabbard"));
		add(parts.get(Joint.PELVIS), "scabbard_socket", hip);
	}

	private static void add(ModelPart parent, String name, ModelPart child) {
		((ModelPartChildrenAccessor) (Object) parent).wildercord$children().put(name, child);
	}

	private static ModelPart emptyAt(float x, float y, float z) {
		ModelPart part = new ModelPart(List.of(), new LinkedHashMap<>());
		part.setInitialPose(PartPose.offset(x, y, z));
		part.resetPose();
		return part;
	}

	public void apply(Function<Joint, Transform> palette) {
		for (Joint joint : Joint.values()) {
			Transform transform = palette.apply(joint);
			ModelPart part = parts.get(joint);
			part.setPos(transform.x(), transform.y(), transform.z());
			part.setRotation(transform.rotation().x(), transform.rotation().y(), transform.rotation().z());
			part.xScale = part.yScale = part.zScale = 1;
			part.visible = true;
			part.skipDraw = false;
		}
		root.visible = true;
	}

	public void skinLayers(AvatarRenderState state) {
		overlays.forEach((part, name) -> part.visible = switch (name) {
			case "hat" -> state.showHat;
			case "jacket" -> state.showJacket;
			case "left_sleeve" -> state.showLeftSleeve;
			case "right_sleeve" -> state.showRightSleeve;
			case "left_pants" -> state.showLeftPants;
			case "right_pants" -> state.showRightPants;
			default -> false;
		});
	}

	public void viewLayers(boolean left, boolean right) {
		overlays.forEach((part, name) -> part.visible = name.equals("left_sleeve") ? left : name.equals("right_sleeve") && right);
	}

	public void armsOnly() {
		for (Joint joint : new Joint[] {Joint.PELVIS, Joint.SPINE, Joint.CHEST}) {
			parts.get(joint).skipDraw = true;
			for (var entry : overlays.entrySet()) if (entry.getValue().equals("jacket")) entry.getKey().visible = false;
		}
		for (Joint joint : new Joint[] {Joint.HEAD, Joint.LEFT_THIGH, Joint.RIGHT_THIGH}) parts.get(joint).visible = false;
	}

	/** A palette is already blended; multiplying it by weight a second time is incorrect. */
	public void transformTo(Joint joint, PoseStack stack) {
		if (joint.parent() != null) transformTo(joint.parent(), stack);
		parts.get(joint).translateAndRotate(stack);
	}

	public void socket(HumanoidArm arm, PoseStack stack) {
		transformTo(arm == HumanoidArm.LEFT ? Joint.LEFT_HAND : Joint.RIGHT_HAND, stack);
		stack.translate((arm == HumanoidArm.LEFT ? 1 : -1) * (slim ? .5F : 1F) / 16, 0, 0);
		parts.get(arm == HumanoidArm.LEFT ? Joint.LEFT_SOCKET : Joint.RIGHT_SOCKET).translateAndRotate(stack);
	}

	public ModelPart part(Joint joint) { return parts.get(joint); }
	public boolean slim() { return slim; }

	public void restoreRigid(HumanoidModel<?> model) {
		// Leave other renderers' visibility decisions alone unless our subtree owned the last pass.
		if (root.visible) for (ModelPart part : rigid(model)) part.visible = true;
		root.visible = false;
	}

	public void hideRigid(HumanoidModel<?> model) {
		for (ModelPart part : rigid(model)) part.visible = false;
		root.visible = true;
	}

	private static ModelPart[] rigid(HumanoidModel<?> model) {
		return new ModelPart[] {model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
	}

	private static ModelPart.Cube cube(int u, int v, float x, float y, float z, float w, float h, float d, float grow, boolean mirror) {
		return new ModelPart.Cube(u, v, x, y, z, w, h, d, grow, grow, grow, mirror, 64, 64, EnumSet.allOf(Direction.class));
	}

	/** Public for native geometry checks and source-driven offline evidence, never a texture asset cache. */
	public static ModelPart.Cube slice(int u, int v, float x, float y, float z, float w, float h, float d,
			float start, float end, float origin, float grow, boolean mirror) {
		if (!(0 <= start && start < end && end <= h)) throw new IllegalArgumentException("Invalid skin slice");
		ModelPart.Cube source = cube(u, v, x, y, z, w, h, d, grow, mirror);
		ModelPart.Cube result = cube(u, v, x, y + start - origin, z, w, end - start, d, grow, mirror);
		for (int face = 0; face < source.polygons.length; face++) {
			var polygon = source.polygons[face];
			var old = polygon.vertices();
			var vertices = new ModelPart.Vertex[old.length];
			for (int i = 0; i < old.length; i++) {
				var vertex = old[i];
				boolean top = vertex.y() < y + h / 2;
				float fullY = y + (top ? start - grow : end + grow);
				float vv = vertex.v();
				if (Math.abs(polygon.normal().y()) < .5F) {
					for (var opposite : old) {
						if (opposite.x() == vertex.x() && opposite.z() == vertex.z() && opposite.y() != vertex.y()) {
							float t = (fullY - vertex.y()) / (opposite.y() - vertex.y());
							vv += (opposite.v() - vertex.v()) * t;
							break;
						}
					}
				}
				vertices[i] = new ModelPart.Vertex(vertex.x(), fullY - origin, vertex.z(), vertex.u(), vv);
			}
			result.polygons[face] = new ModelPart.Polygon(vertices, polygon.normal());
		}
		return result;
	}
}
