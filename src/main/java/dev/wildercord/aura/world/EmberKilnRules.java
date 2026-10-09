package dev.wildercord.aura.world;

import java.util.ArrayList;
import java.util.List;

/** Fixed ground annulus: inward pocket, outward escape, or a timed jump answer one warned pulse. */
public final class EmberKilnRules {
	private EmberKilnRules() {}
	public static final int TELL = 40, RECOVERY = 48, COOLDOWN = 130, WARNING_REFRESH = 4, SECTORS = 32;
	public static final double COST = 28, DAMAGE = 26, INNER = 2.75, OUTER = 5.5, ESCAPE = 6.25;
	public static final double LOW = -.05, HIGH = .75;
	public static final int MAX_COVER_BOXES = 512;

	/** Occupies only an ordinary Ember slot; existing cast/range/Wake responses keep priority. */
	public static boolean eligible(int school, int sequence, double distance, double height, double aura, long now, long readyAt) {
		return school == MastersRules.EMBER && Math.floorMod(sequence, 4) == 1 && now >= readyAt
			&& Double.isFinite(aura) && aura >= COST && aura <= MastersRules.AURA_MAX
			&& Double.isFinite(distance) && distance >= INNER && distance <= OUTER
			&& Double.isFinite(height) && Math.abs(height) <= .15;
	}

	/** Player feet choose the boundary; bodies never enlarge the ring or consume another player's hit. */
	public static boolean hits(double x, double z, double height) {
		if (!Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(height)) return false;
		double squared = x * x + z * z;
		return squared >= INNER * INNER && squared <= OUTER * OUTER && height >= LOW && height <= HIGH;
	}

	public static int sector(double x, double z) {
		if (!Double.isFinite(x) || !Double.isFinite(z) || x == 0 && z == 0) return -1;
		double angle = Math.atan2(z, x);
		if (angle < 0) angle += Math.PI * 2;
		return Math.min(SECTORS - 1, (int) (angle * SECTORS / (Math.PI * 2)));
	}

	public static double angle(int boundary) { return boundary * (Math.PI * 2 / SECTORS); }

	/** Conservative rectangle/cone overlap clips an entire sector, including every edge of thin cover. */
	public static boolean blocksSector(double minX, double maxX, double minZ, double maxZ, int sector) {
		if (!Double.isFinite(minX) || !Double.isFinite(maxX) || !Double.isFinite(minZ) || !Double.isFinite(maxZ)
			|| minX > maxX || minZ > maxZ || sector < 0 || sector >= SECTORS) return true;
		double nearX = Math.max(minX, Math.min(0, maxX)), nearZ = Math.max(minZ, Math.min(0, maxZ));
		if (nearX * nearX + nearZ * nearZ > OUTER * OUTER) return false;
		List<Point> polygon = List.of(new Point(minX, minZ), new Point(maxX, minZ), new Point(maxX, maxZ), new Point(minX, maxZ));
		polygon = clip(polygon, Math.cos(angle(sector)), Math.sin(angle(sector)), 1);
		return !clip(polygon, Math.cos(angle(sector + 1)), Math.sin(angle(sector + 1)), -1).isEmpty();
	}

	private record Point(double x, double z) {}
	private static List<Point> clip(List<Point> input, double dx, double dz, int side) {
		var output = new ArrayList<Point>();
		if (input.isEmpty()) return output;
		Point previous = input.getLast();
		double before = side * (dx * previous.z - dz * previous.x);
		for (Point next : input) {
			double after = side * (dx * next.z - dz * next.x);
			if ((before >= -1e-9) != (after >= -1e-9)) {
				double t = before / (before - after);
				output.add(new Point(previous.x + (next.x - previous.x) * t, previous.z + (next.z - previous.z) * t));
			}
			if (after >= -1e-9) output.add(next);
			previous = next; before = after;
		}
		return output;
	}
}
