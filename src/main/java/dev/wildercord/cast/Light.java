package dev.wildercord.cast;

import dev.wildercord.content.LightOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Shaped light for spells (see {@link LightOption}): shockwaves, beams, crescent slashes and orbs,
 * sent to everyone within 128 blocks like a magic circle. Colours are 0xRRGGBB; add {@link #DARK}
 * to a colour and it darkens what's behind it instead of lighting it (void magic).
 */
public final class Light {
	private Light() {}

	/** Or'd into a colour: the light is drawn as darkness, taking light away instead of adding it. */
	public static final int DARK = 0x01000000;

	private static final Vec3 UP = new Vec3(0, 1, 0);

	/** A shockwave racing out from {@code from} to {@code to} blocks in the plane facing {@code normal}. */
	public static void ring(ServerLevel level, Vec3 at, Vec3 normal, int color, double from, double to, double width, int lifetime) {
		Sigils.send(level, new LightOption(LightOption.RING, color, (float) from, (float) to, 0, (float) width,
			Sigils.yaw(normal), Sigils.pitch(normal), 0, lifetime), at);
	}

	/** A shockwave along the ground. */
	public static void groundRing(ServerLevel level, Vec3 at, int color, double from, double to, double width, int lifetime) {
		ring(level, at.add(0, 0.08, 0), UP, color, from, to, width, lifetime);
	}

	/** A beam from {@code from} to {@code to}. */
	public static void ray(ServerLevel level, Vec3 from, Vec3 to, int color, double width, int lifetime) {
		Vec3 d = to.subtract(from);
		Sigils.send(level, new LightOption(LightOption.RAY, color, (float) d.x, (float) d.y, (float) d.z, (float) width, 0, 0, 0, lifetime), from);
	}

	/**
	 * A crescent slash of {@code radius} around {@code centre}, spanning {@code span} radians, in the
	 * plane facing {@code normal}, its middle pointing along {@code toward} (which should lie in that
	 * plane); it sweeps across in {@code sweep} ticks.
	 */
	public static void slash(ServerLevel level, Vec3 centre, Vec3 normal, Vec3 toward, int color, double radius, double span, double width,
			int sweep, int lifetime) {
		float yaw = Sigils.yaw(normal);
		float pitch = Sigils.pitch(normal);
		// Where "toward" points, as an angle in the plane's own x/y (the same plane the client builds).
		Quaternionf plane = new Quaternionf().rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(pitch), 0);
		Vector3f x = plane.transform(new Vector3f(1, 0, 0));
		Vector3f y = plane.transform(new Vector3f(0, 1, 0));
		float mid = (float) Math.atan2(toward.x * y.x + toward.y * y.y + toward.z * y.z, toward.x * x.x + toward.y * x.y + toward.z * x.z);
		Sigils.send(level, new LightOption(LightOption.SLASH, color, (float) radius, (float) span, sweep, (float) width, yaw, pitch, mid, lifetime), centre);
	}

	/** A glowing orb wrapped in turning rings. */
	public static void orb(ServerLevel level, Vec3 at, int color, double radius, int lifetime) {
		Sigils.send(level, new LightOption(LightOption.ORB, color, (float) radius, 0, 0, 0.02F, 0, 0, 0, lifetime), at);
	}
}
