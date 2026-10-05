package dev.wildercord.aura;

import dev.wildercord.aura.ArticulatedCombatPose.Joint;
import dev.wildercord.aura.ArticulatedCombatPose.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Original, texture-independent armor topology. Only existing exterior faces are subdivided;
 * joint cuts never add caps, another shell, or overlapping polygons. UVs remain affine samples
 * of the source face, including mirrored faces and transparent texels. Positions at UV seams
 * reference the same control point, so adjacent faces cannot separate when the palette changes.
 * Coordinates and collar widths are model pixels, not blocks.
 */
public final class ArticulatedArmorMesh {
	private ArticulatedArmorMesh() {}
	private static final float EPS = 1.0e-5F;
	// Two-pixel bands meet at one rigid ring between elbow/wrist and knee/ankle. Linear
	// weights keep the inner bend monotone; a cubic ease would fold the larger armor shell.
	public static final float COLLAR_HALF_WIDTH = 2;

	public enum Region {
		HEAD(Joint.HEAD, Joint.HEAD, Joint.HEAD, 0, 0),
		BODY(Joint.CHEST, Joint.SPINE, Joint.PELVIS, 4, 9),
		RIGHT_ARM(Joint.RIGHT_UPPER_ARM, Joint.RIGHT_FOREARM, Joint.RIGHT_HAND, 6, 10),
		LEFT_ARM(Joint.LEFT_UPPER_ARM, Joint.LEFT_FOREARM, Joint.LEFT_HAND, 6, 10),
		RIGHT_LEG(Joint.RIGHT_THIGH, Joint.RIGHT_SHIN, Joint.RIGHT_FOOT, 18, 22),
		LEFT_LEG(Joint.LEFT_THIGH, Joint.LEFT_SHIN, Joint.LEFT_FOOT, 18, 22);

		private final Joint upper, middle, lower;
		private final float firstCut, secondCut;
		Region(Joint upper, Joint middle, Joint lower, float firstCut, float secondCut) {
			this.upper = upper; this.middle = middle; this.lower = lower;
			this.firstCut = firstCut; this.secondCut = secondCut;
		}
		public boolean arm() { return this == RIGHT_ARM || this == LEFT_ARM; }
	}

	public record SourceVertex(Vec3 position, float u, float v) {
		public SourceVertex {
			Objects.requireNonNull(position);
			if (!finite(position) || !Float.isFinite(u) || !Float.isFinite(v)) throw new IllegalArgumentException("Non-finite armor vertex");
		}
	}
	public record SourceFace(Region region, List<SourceVertex> vertices) {
		public SourceFace {
			Objects.requireNonNull(region);
			vertices = List.copyOf(vertices);
			if (vertices.size() != 4) throw new IllegalArgumentException("Armor source must contain quads");
		}
	}
	public record ControlPoint(Region region, Vec3 bindPosition, Joint first, Joint second, float secondWeight) {}
	public record Corner(int controlPoint, float u, float v) {}
	public record Face(List<Corner> corners, Vec3 bindNormal, int sourceFace) {
		public Face { corners = List.copyOf(corners); }
	}

	/** The callback is the FINAL world matrix times inverse bind, with no second pose-weight blend. */
	@FunctionalInterface
	public interface SkinTransform { Vec3 transform(Joint joint, Vec3 bindPosition); }

	public record Deformed(List<Vec3> positions, List<Vec3> normals) {
		public Deformed { positions = List.copyOf(positions); normals = List.copyOf(normals); }
	}

	public static final class Mesh {
		private final List<ControlPoint> controlPoints;
		private final List<Face> faces;
		private final Map<Region, Section> sections = new EnumMap<>(Region.class);
		private Mesh(List<ControlPoint> controlPoints, List<Face> faces) {
			this.controlPoints = List.copyOf(controlPoints); this.faces = List.copyOf(faces);
			for (Region region : Region.values()) {
				float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY, minZ = minX, maxZ = maxX;
				for (var point : controlPoints) if (point.region() == region) {
					minX = Math.min(minX, point.bindPosition().x()); maxX = Math.max(maxX, point.bindPosition().x());
					minZ = Math.min(minZ, point.bindPosition().z()); maxZ = Math.max(maxZ, point.bindPosition().z());
				}
				if (minX <= maxX) sections.put(region, new Section((minX + maxX) / 2, (minZ + maxZ) / 2, (maxX - minX) / 2, (maxZ - minZ) / 2));
			}
		}
		public List<ControlPoint> controlPoints() { return controlPoints; }
		public List<Face> faces() { return faces; }

		public Deformed deform(SkinTransform palette) {
			List<Vec3> positions = new ArrayList<>(controlPoints.size());
			for (ControlPoint point : controlPoints) {
				Vec3 a = palette.transform(point.first(), point.bindPosition());
				Vec3 p = point.first() == point.second() ? a
					: collar(point, a, palette.transform(point.second(), point.bindPosition()), palette, sections.get(point.region()));
				if (!finite(p)) throw new IllegalArgumentException("Non-finite resolved armor palette");
				positions.add(p);
			}
			List<Vec3> normals = new ArrayList<>(faces.size());
			for (Face face : faces) {
				var c = face.corners();
				Vec3 a = positions.get(c.get(0).controlPoint()), b = positions.get(c.get(1).controlPoint());
				Vec3 d = positions.get(c.get(3).controlPoint()), e = positions.get(c.get(2).controlPoint());
				// The sum of the two triangle normals also handles a slightly non-planar collar quad.
				Vec3 normal = b.minus(a).cross(e.minus(a)).plus(e.minus(a).cross(d.minus(a))).unit();
				if (normal.length() < EPS) throw new IllegalArgumentException("Collapsed armor face");
				normals.add(normal);
			}
			return new Deformed(positions, normals);
		}
	}

	private record Section(float x, float z, float radiusX, float radiusZ) {}

	/** A support envelope of the two adjacent cross-sections, confined to existing collar rings. */
	private static Vec3 collar(ControlPoint point, Vec3 a, Vec3 b, SkinTransform palette, Section section) {
		float t = point.secondWeight();
		// The thick, stock four-pixel arm shell already encloses both skin/sleeve sizes.
		// Adding a support envelope here would introduce an inner-elbow fold.
		if (point.region().arm()) return a.toward(b, t);
		Vec3 center = new Vec3(section.x(), point.bindPosition().y(), section.z());
		Vec3 ca = palette.transform(point.first(), center), cb = palette.transform(point.second(), center);
		Vec3 c = ca.toward(cb, t), p = a.toward(b, t);
		Vec3 ax = palette.transform(point.first(), center.plus(new Vec3(1, 0, 0))).minus(ca);
		Vec3 bx = palette.transform(point.second(), center.plus(new Vec3(1, 0, 0))).minus(cb);
		Vec3 az = palette.transform(point.first(), center.plus(new Vec3(0, 0, 1))).minus(ca);
		Vec3 bz = palette.transform(point.second(), center.plus(new Vec3(0, 0, 1))).minus(cb);
		Vec3 ay = palette.transform(point.first(), center.plus(new Vec3(0, 1, 0))).minus(ca);
		Vec3 by = palette.transform(point.second(), center.plus(new Vec3(0, 1, 0))).minus(cb);
		Vec3 x = ax.toward(bx, t).unit(), z0 = az.toward(bz, t);
		Vec3 z = z0.minus(x.times(x.dot(z0))).unit();
		float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY, minZ = minX, maxZ = maxX;
		for (int signX : new int[] {-1, 1}) for (int signZ : new int[] {-1, 1}) {
			for (Vec3 q : new Vec3[] {
				ca.plus(ax.times(signX * section.radiusX())).plus(az.times(signZ * section.radiusZ())).minus(c),
				cb.plus(bx.times(signX * section.radiusX())).plus(bz.times(signZ * section.radiusZ())).minus(c)}) {
				// Existing skin/pants/jacket slices extend their closed caps by 0.25px.
				float capX = .25F * Math.max(Math.abs(ay.dot(x)), Math.abs(by.dot(x)));
				float capZ = .25F * Math.max(Math.abs(ay.dot(z)), Math.abs(by.dot(z)));
				minX = Math.min(minX, q.dot(x) - capX); maxX = Math.max(maxX, q.dot(x) + capX);
				minZ = Math.min(minZ, q.dot(z) - capZ); maxZ = Math.max(maxZ, q.dot(z) + capZ);
			}
		}
		// At the middle ring a bent pair's projected radius shrinks by cos(half-angle).
		// Its reciprocal restores that radius without the oversized full-miter correction.
		// A conservative cap projection above accounts for the existing 0.25px skin overlays;
		// it vanishes at bind. Original shell radii keep inner/outer equipment slots separated.
		Vec3 yBlend = ay.toward(by, t);
		float volume = 1 / Math.max(.5F, yBlend.length());
		float targetX = (point.bindPosition().x() < section.x() ? minX : maxX) * volume;
		float targetZ = (point.bindPosition().z() < section.z() ? minZ : maxZ) * volume;
		float envelope = 4 * t * (1 - t);
		Vec3 local = p.minus(c);
		return p.plus(x.times((targetX - local.dot(x)) * envelope)).plus(z.times((targetZ - local.dot(z)) * envelope));
	}

	public static Mesh bake(List<SourceFace> source) {
		List<ControlPoint> points = new ArrayList<>();
		List<Face> faces = new ArrayList<>();
		Map<PointKey, Integer> welded = new LinkedHashMap<>();
		for (int faceIndex = 0; faceIndex < source.size(); faceIndex++) {
			SourceFace face = source.get(faceIndex);
			float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
			for (SourceVertex vertex : face.vertices()) { min = Math.min(min, vertex.position().y()); max = Math.max(max, vertex.position().y()); }
			if (max - min < EPS || face.region() == Region.HEAD) {
				addFace(face.region(), face.vertices(), faceIndex, points, faces, welded);
				continue;
			}
			TreeSet<Float> levels = new TreeSet<>();
			levels.add(min); levels.add(max);
			for (float cut : new float[] {face.region().firstCut, face.region().secondCut}) {
				for (int step = -2; step <= 2; step++) {
					float y = cut + step * COLLAR_HALF_WIDTH / 2;
					if (y > min + EPS && y < max - EPS) levels.add(y);
				}
			}
			List<Float> rings = List.copyOf(levels);
			for (int ring = 0; ring < rings.size() - 1; ring++) {
				List<SourceVertex> corners = new ArrayList<>(4);
				for (SourceVertex vertex : face.vertices()) {
					float y = vertex.position().y();
					if (Math.abs(y - min) >= EPS && Math.abs(y - max) >= EPS)
						throw new IllegalArgumentException("Armor source must be unposed, axis-aligned vanilla geometry");
					SourceVertex opposite = null;
					for (SourceVertex candidate : face.vertices()) {
						if (candidate.position().x() == vertex.position().x() && candidate.position().z() == vertex.position().z()
							&& Math.abs(candidate.position().y() - y) >= EPS) { opposite = candidate; break; }
					}
					if (opposite == null) throw new IllegalArgumentException("Armor source edge is not vertical");
					SourceVertex top = y == min ? vertex : opposite, bottom = y == min ? opposite : vertex;
					float target = y == min ? rings.get(ring) : rings.get(ring + 1);
					corners.add(interpolate(top, bottom, target));
				}
				addFace(face.region(), corners, faceIndex, points, faces, welded);
			}
		}
		return new Mesh(points, faces);
	}

	private record PointKey(Region region, Vec3 position) {}
	private static void addFace(Region region, List<SourceVertex> source, int sourceIndex,
			List<ControlPoint> points, List<Face> faces, Map<PointKey, Integer> welded) {
		List<Corner> corners = new ArrayList<>(4);
		for (SourceVertex vertex : source) {
			PointKey key = new PointKey(region, vertex.position());
			Integer index = welded.get(key);
			if (index == null) {
				index = points.size(); points.add(weight(region, vertex.position())); welded.put(key, index);
			}
			corners.add(new Corner(index, vertex.u(), vertex.v()));
		}
		Vec3 a = source.get(0).position(), b = source.get(1).position(), c = source.get(2).position();
		Vec3 normal = b.minus(a).cross(c.minus(a)).unit();
		if (normal.length() < EPS) throw new IllegalArgumentException("Degenerate armor source face");
		faces.add(new Face(corners, normal, sourceIndex));
	}

	private static SourceVertex interpolate(SourceVertex top, SourceVertex bottom, float y) {
		if (y == top.position().y()) return top;
		if (y == bottom.position().y()) return bottom;
		float t = (y - top.position().y()) / (bottom.position().y() - top.position().y());
		return new SourceVertex(new Vec3(top.position().x(), y, top.position().z()),
			top.u() + (bottom.u() - top.u()) * t, top.v() + (bottom.v() - top.v()) * t);
	}

	private static ControlPoint weight(Region region, Vec3 position) {
		float y = position.y(), width = COLLAR_HALF_WIDTH;
		if (region == Region.HEAD || y <= region.firstCut - width) return rigid(region, position, region.upper);
		if (y < region.firstCut + width) return blend(region, position, region.upper, region.middle, region.firstCut);
		if (y <= region.secondCut - width) return rigid(region, position, region.middle);
		if (y < region.secondCut + width) return blend(region, position, region.middle, region.lower, region.secondCut);
		return rigid(region, position, region.lower);
	}
	private static ControlPoint rigid(Region region, Vec3 position, Joint joint) { return new ControlPoint(region, position, joint, joint, 0); }
	private static ControlPoint blend(Region region, Vec3 position, Joint first, Joint second, float cut) {
		float t = (position.y() - cut + COLLAR_HALF_WIDTH) / (COLLAR_HALF_WIDTH * 2);
		return new ControlPoint(region, position, first, second, t);
	}
	private static boolean finite(Vec3 p) { return Float.isFinite(p.x()) && Float.isFinite(p.y()) && Float.isFinite(p.z()); }
}
