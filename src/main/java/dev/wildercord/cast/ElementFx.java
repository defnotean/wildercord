package dev.wildercord.cast;

import dev.wildercord.content.LightOption;
import dev.wildercord.content.SigilOption;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The element visual languages. Every effect is drawn from its element's own rules (a palette, a
 * signature motif and a signature impact), so a hit reads as fire or frost at a glance, whatever
 * the spell:
 * <ul>
 *   <li><b>Fire</b>: embers rising, a heat flare (a gold core in an orange bloom) and flame tongues,
 *       short crescents licking upward; its impact bursts into whirling flame slashes.</li>
 *   <li><b>Frost</b>: crystal shards (short spikes of light), a hard white shatter ring and frost
 *       creeping over the ground as a glowing frost seal.</li>
 *   <li><b>Storm</b>: branching lightning built from short rays, jagged and forking, flickering
 *       over two or three ticks; its impact throws forks out over the ground.</li>
 *   <li><b>Wind</b>: swirling crescents, each on its own tilt, spiralling up an axis, and rings of
 *       air running out along the ground.</li>
 *   <li><b>Earth</b>: the ground cracking (a cracked seal and a ring of dust), chips of the ground
 *       itself thrown up, stone spires.</li>
 *   <li><b>Life</b>: petals and leaves, a leaf spiral of soft green arcs, and a bloom: a gentle
 *       glow, a flower seal and a slow ring.</li>
 *   <li><b>Void</b>: darkness, not light: rings of it imploding and a black core, each with a
 *       thin violet rim, so void reads as a hole in the world.</li>
 *   <li><b>Arcane</b>: star seals, comets of pink light orbiting on tilted paths, and a shimmer of
 *       glyphs drawn in.</li>
 *   <li><b>Time</b>: clock faces of light, their hands crescents sweeping round like hands, the
 *       quarter hours ticked in white, and golden flecks.</li>
 *   <li><b>Blood</b>: crimson cuts, heartbeat rings pulsing in pairs, and drops falling.</li>
 * </ul>
 * Light and circles from here go through {@link Fx#send} like every other particle: left out for
 * a player whose eyes they would sit in front of (so a buff on yourself never fills your view),
 * and silent while a passive renews itself quietly. Shapes use {@link Light} instead, which
 * reaches 128 blocks.
 */
public final class ElementFx {
	private ElementFx() {}

	/**
	 * Or'd into a light's colour, draws it as darkness (subtracted from the world) instead of a
	 * glow; the colour is the darkness's tint. The same bit as {@code Light.DARK}.
	 */
	public static final int DARK = Light.DARK;

	private static final Vec3 UP = new Vec3(0, 1, 0);

	/**
	 * An element's colours: its own (the rune colour), a lighter one for highlights and cores, and a
	 * contrast. Void's contrast is the tint of its darkness.
	 */
	public record Palette(int primary, int secondary, int accent) {}

	public static final Palette FIRE = new Palette(0xF06E32, 0xFFD060, 0xFF3A1A);
	public static final Palette FROST = new Palette(0x8CDCFF, 0xE6FAFF, 0x3A8CFF);
	public static final Palette STORM = new Palette(0xFFE650, 0xFFFBE0, 0xA8C8FF);
	public static final Palette WIND = new Palette(0xC8F0DC, 0xFFFFFF, 0x7FE0C0);
	public static final Palette EARTH = new Palette(0xB48C5A, 0xE8C890, 0x6E5436);
	public static final Palette LIFE = new Palette(0x6EDC64, 0xE8FFB0, 0xFFA8D8);
	public static final Palette VOID = new Palette(0xB45AF0, 0xE0B0FF, 0x1A0830);
	public static final Palette ARCANE = new Palette(0xE678DC, 0xFFD8FA, 0x9A7CFF);
	public static final Palette TIME = new Palette(0xF2D98A, 0xFFF8E0, 0xC8962E);
	public static final Palette BLOOD = new Palette(0xD2283C, 0xFF6474, 0x5A0A14);
	public static final Palette NEUTRAL = new Palette(0x40C8BE, 0xC8FFF8, 0xFFFFFF);

	public static Palette palette(String element) {
		return switch (element) {
			case "fire" -> FIRE;
			case "frost" -> FROST;
			case "storm" -> STORM;
			case "wind" -> WIND;
			case "earth" -> EARTH;
			case "life" -> LIFE;
			case "void" -> VOID;
			case "arcane" -> ARCANE;
			case "time" -> TIME;
			case "blood" -> BLOOD;
			default -> NEUTRAL;
		};
	}

	private static final String[] ELEMENTS = {"fire", "frost", "storm", "wind", "earth", "life", "void", "arcane", "time", "blood"};

	/** The element a theme belongs to (themes share their element's rune colour), or "" for a shape's or a tinted one. */
	public static String elementOf(Vfx.Theme theme) {
		for (String element : ELEMENTS) {
			if (palette(element).primary() == theme.primary()) {
				return element;
			}
		}
		return "";
	}

	/** A theme's palette: its element's, or one made from its own colours. */
	public static Palette palette(Vfx.Theme theme) {
		String element = elementOf(theme);
		return element.isEmpty() ? new Palette(theme.primary(), theme.secondary(), 0xFFFFFF) : palette(element);
	}

	/** {@code color} drawn as darkness. */
	public static int dark(int color) {
		return color | DARK;
	}

	// ------------------------------------------------------------------ light and circles, through Fx.send

	/** A shockwave racing out from {@code from} to {@code to} blocks (or falling in, if {@code to} is smaller) in the plane facing {@code normal}. */
	public static void ring(ServerLevel level, Vec3 at, Vec3 normal, int color, double from, double to, double width, int lifetime) {
		send(level, ringOption(normal, color, from, to, width, lifetime), at);
	}

	/** The particle behind {@link #ring}, for sending another way (to everyone but one player, say). */
	public static LightOption ringOption(Vec3 normal, int color, double from, double to, double width, int lifetime) {
		Vec3 n = safe(normal);
		return new LightOption(LightOption.RING, color, (float) from, (float) to, 0, (float) width, Sigils.yaw(n), Sigils.pitch(n), 0, lifetime);
	}

	public static void groundRing(ServerLevel level, Vec3 at, int color, double from, double to, double width, int lifetime) {
		ring(level, at.add(0, 0.08, 0), UP, color, from, to, width, lifetime);
	}

	/** A beam from {@code from} to {@code to}: it shoots out in two ticks, holds and thins away. */
	public static void ray(ServerLevel level, Vec3 from, Vec3 to, int color, double width, int lifetime) {
		Vec3 d = to.subtract(from);
		send(level, new LightOption(LightOption.RAY, color, (float) d.x, (float) d.y, (float) d.z, (float) width, 0, 0, 0, lifetime), from);
	}

	/** A crescent round {@code centre} in the plane facing {@code normal}, its middle toward {@code toward} (see {@link Light#slash}). */
	public static void slash(ServerLevel level, Vec3 centre, Vec3 normal, Vec3 toward, int color, double radius, double span, double width,
			int sweep, int lifetime) {
		send(level, slashOption(normal, toward, color, radius, span, width, sweep, lifetime), centre);
	}

	/** The particle behind {@link #slash}, for sending another way. */
	public static LightOption slashOption(Vec3 normal, Vec3 toward, int color, double radius, double span, double width, int sweep, int lifetime) {
		Vec3 n = safe(normal);
		float yaw = Sigils.yaw(n);
		float pitch = Sigils.pitch(n);
		// The same plane the client builds, so "toward" lands where it should.
		Quaternionf plane = new Quaternionf().rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(pitch), 0);
		Vector3f x = plane.transform(new Vector3f(1, 0, 0));
		Vector3f y = plane.transform(new Vector3f(0, 1, 0));
		float mid = (float) Math.atan2(toward.x * y.x + toward.y * y.y + toward.z * y.z, toward.x * x.x + toward.y * x.y + toward.z * x.z);
		return new LightOption(LightOption.SLASH, color, (float) radius, (float) span, sweep, (float) width, yaw, pitch, mid, lifetime);
	}

	/** A glowing orb wrapped in turning rings; four ticks or less, and it doesn't fade (for one redrawn as it moves). */
	public static void orb(ServerLevel level, Vec3 at, int color, double radius, int lifetime) {
		send(level, new LightOption(LightOption.ORB, color, (float) radius, 0, 0, 0.02F, 0, 0, 0, lifetime), at);
	}

	/** A magic circle layer facing {@code normal}. */
	public static void sigil(ServerLevel level, Vec3 at, Vec3 normal, int style, int color, double size, int lifetime, double spin) {
		Vec3 n = safe(normal);
		send(level, new SigilOption(style, color & 0xFFFFFF, (float) size, Sigils.yaw(n), Sigils.pitch(n), lifetime, (float) spin), at);
	}

	/** A circle lying on the ground at {@code feet}. */
	public static void flatSigil(ServerLevel level, Vec3 feet, int style, int color, double size, int lifetime, double spin) {
		send(level, SigilOption.flat(style, color & 0xFFFFFF, (float) size, lifetime, (float) spin), feet.add(0, 0.07, 0));
	}

	private static void send(ServerLevel level, ParticleOptions light, Vec3 at) {
		Fx.send(level, light, at.x, at.y, at.z, 1, 0, 0, 0, 0);
	}

	// ------------------------------------------------------------------ geometry

	private static Vec3 safe(Vec3 normal) {
		return normal.lengthSqr() < 1.0E-8 ? UP : normal;
	}

	/** A unit vector at right angles to {@code n}. */
	public static Vec3 perp(Vec3 n) {
		Vec3 p = n.cross(UP);
		if (p.lengthSqr() < 1.0E-6) {
			p = n.cross(new Vec3(1, 0, 0));
		}
		return p.normalize();
	}

	/** The direction at {@code angle} within the plane facing {@code normal}. */
	public static Vec3 inPlane(Vec3 normal, double angle) {
		Vec3 n = safe(normal).normalize();
		Vec3 u = perp(n);
		Vec3 v = n.cross(u);
		return u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
	}

	/** Straight up, tipped over by {@code tilt} radians toward compass angle {@code toward}. */
	public static Vec3 tilted(double tilt, double toward) {
		return new Vec3(Math.cos(toward) * Math.sin(tilt), Math.cos(tilt), Math.sin(toward) * Math.sin(tilt));
	}

	/** A level direction at compass angle {@code angle}. */
	public static Vec3 flatDir(double angle) {
		return new Vec3(Math.cos(angle), 0, Math.sin(angle));
	}

	public static Vec3 randomDir(RandomSource random) {
		double y = random.nextDouble() * 2 - 1;
		double a = random.nextDouble() * Math.PI * 2;
		double s = Math.sqrt(1 - y * y);
		return new Vec3(Math.cos(a) * s, y, Math.sin(a) * s);
	}

	/** The ground under {@code at}, if it's within {@code drop} blocks; otherwise null. */
	public static Vec3 floor(ServerLevel level, Vec3 at, double drop) {
		Vec3 ground = CastEngine.ground(level, at);
		// With nothing under it, CastEngine.ground hands back the very point it was given.
		return ground != at && at.y - ground.y <= drop ? ground : null;
	}

	// ------------------------------------------------------------------ fire: embers, a heat flare, flame slashes

	/** Embers: flames and sparks rising off a patch {@code spread} blocks around {@code at}. */
	public static void embers(ServerLevel level, Vec3 at, double spread, int count) {
		RandomSource r = level.getRandom();
		for (int i = 0; i < count; i++) {
			Vec3 p = at.add((r.nextDouble() - 0.5) * 2 * spread, (r.nextDouble() - 0.5) * spread, (r.nextDouble() - 0.5) * 2 * spread);
			Vec3 up = new Vec3((r.nextDouble() - 0.5) * 0.4, 1, (r.nextDouble() - 0.5) * 0.4);
			Vfx.fling(level, i % 3 == 2 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME, p, up, 0.05 + r.nextDouble() * 0.07);
		}
		if (count >= 6) {
			Vfx.emit(level, ParticleTypes.LAVA, at, count / 6, spread * 0.5, 0.0);
		}
	}

	/** A heat flare: a gold core in an orange bloom, {@code size} blocks across. */
	public static void heatFlare(ServerLevel level, Vec3 at, double size) {
		Sigils.flash(level, at, FIRE.primary, (float) (size * 1.5));
		Sigils.flash(level, at, FIRE.secondary, (float) size);
	}

	/** Flame tongues licking up round a column {@code radius} wide, from {@code base} up to {@code height}. */
	public static void flames(ServerLevel level, Vec3 base, double radius, double height, int count) {
		tongues(level, base, radius, height, count, FIRE.primary, FIRE.secondary, 2, 7);
	}

	/**
	 * Tongues: short crescents on the sides of a column, curling up and over like flames, in two
	 * alternating colours. The arcs sweep in {@code sweep} ticks (one more for every other one, so
	 * they flicker).
	 */
	public static void tongues(ServerLevel level, Vec3 base, double radius, double height, int count, int color, int second, int sweep,
			int lifetime) {
		RandomSource r = level.getRandom();
		double phase = r.nextDouble() * Math.PI * 2;
		for (int i = 0; i < count; i++) {
			double a = phase + Math.PI * 2 * i / count + (r.nextDouble() - 0.5) * 0.6;
			Vec3 centre = base.add(0, height * (0.15 + 0.7 * r.nextDouble()), 0);
			// In the upright plane through the axis: the arc bulges out and curls up.
			Vec3 toward = flatDir(a).scale(0.8).add(0, 0.6, 0);
			slash(level, centre, new Vec3(-Math.sin(a), 0, Math.cos(a)), toward, i % 2 == 0 ? color : second, radius + 0.12, 1.2 + r.nextDouble() * 0.4,
				0.13 + r.nextDouble() * 0.06, sweep + i % 2, lifetime);
		}
	}

	/** A burst of flame slashes: crescents whirling round {@code centre} on every tilt. */
	public static void flameBurst(ServerLevel level, Vec3 centre, double radius, int count) {
		RandomSource r = level.getRandom();
		for (int i = 0; i < count; i++) {
			Vec3 normal = randomDir(r);
			int color = i % 3 == 0 ? FIRE.secondary : i % 3 == 1 ? FIRE.primary : FIRE.accent;
			slash(level, centre, normal, inPlane(normal, r.nextDouble() * Math.PI * 2), color, radius * (0.7 + 0.3 * r.nextDouble()),
				1.6 + r.nextDouble() * 0.6, 0.1 + radius * 0.06, 2, 7);
		}
	}

	public static void fireImpact(ServerLevel level, Vec3 at, double size) {
		heatFlare(level, at, 1.1 * size);
		ring(level, at, UP, FIRE.primary, 0.15 * size, 1.2 * size, 0.06 * size, 8);
		flameBurst(level, at, 0.55 * size, 3);
		embers(level, at, 0.25 * size, (int) Math.max(3, 6 * size));
	}

	// ------------------------------------------------------------------ frost: shards, a shatter ring, frost creeping

	/** Crystal shards: short spikes of ice light bursting out of {@code at} (up to {@code reach} long), and chips of ice. */
	public static void shards(ServerLevel level, Vec3 at, double reach, int count) {
		RandomSource r = level.getRandom();
		for (int i = 0; i < count; i++) {
			Vec3 dir = randomDir(r).add(0, 0.25, 0).normalize();
			double length = reach * (0.55 + 0.45 * r.nextDouble());
			ray(level, at.add(dir.scale(0.12)), at.add(dir.scale(length)), i % 2 == 0 ? FROST.primary : 0xCFF4FF, 0.035 + 0.02 * r.nextDouble(),
				7 + r.nextInt(3));
		}
		Vfx.radial(level, new ItemParticleOption(ParticleTypes.ITEM, Items.BLUE_ICE), at, count, 0.14);
	}

	/** The shatter ring: a hard white ring snapping out, a blue one on a tilt behind it. */
	public static void shatterRing(ServerLevel level, Vec3 at, double radius) {
		ring(level, at, UP, FROST.secondary, 0.2, radius, 0.05, 7);
		ring(level, at, tilted(0.6, level.getRandom().nextDouble() * Math.PI * 2), FROST.accent, 0.15, radius * 0.8, 0.035, 8);
	}

	/** Frost creeping over the ground: a glowing frost seal and a slow ring spreading out from it. */
	public static void frostCreep(ServerLevel level, Vec3 feet, double radius, int lifetime) {
		flatSigil(level, feet, SigilOption.STAR, FROST.primary, radius, lifetime, 0.01);
		groundRing(level, feet, FROST.secondary, 0.2, radius * 1.15, 0.035, Math.max(10, lifetime - 4));
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, feet.add(0, 0.2, 0), (int) Math.max(3, radius * 4), radius * 0.4, 0.01);
	}

	public static void frostImpact(ServerLevel level, Vec3 at, double size) {
		Sigils.flash(level, at, FROST.primary, (float) (1.2 * size));
		shatterRing(level, at, 1.2 * size);
		shards(level, at, 0.8 * size, (int) Math.max(3, 5 * size));
		Vfx.emit(level, ParticleTypes.SNOWFLAKE, at, (int) Math.max(2, 4 * size), 0.3 * size, 0.02);
	}

	// ------------------------------------------------------------------ storm: branching lightning, sparks

	/**
	 * Lightning from {@code from} to {@code to}: a jagged run of short rays with {@code forks}
	 * branching off it, flickering {@code flickers} times (a fresh path each tick).
	 */
	public static void bolt(ServerLevel level, Vec3 from, Vec3 to, double width, int forks, int flickers) {
		boltOnce(level, from, to, width, forks, 5, STORM.primary, STORM.accent, 0);
		for (int f = 1; f < flickers; f++) {
			Scheduler.later(f, () -> boltOnce(level, from, to, width * 0.8, Math.max(0, forks - 1), 3, STORM.primary, STORM.accent, 0));
		}
	}

	/** Lightning drawn in {@code color} over a wider stroke of {@code under} (a darkness, say): the same path, twice. */
	public static void bolt(ServerLevel level, Vec3 from, Vec3 to, double width, int forks, int flickers, int color, int under) {
		boltOnce(level, from, to, width, forks, 5, color, color, under);
		for (int f = 1; f < flickers; f++) {
			Scheduler.later(f, () -> boltOnce(level, from, to, width * 0.8, Math.max(0, forks - 1), 3, color, color, under));
		}
	}

	private static void boltOnce(ServerLevel level, Vec3 from, Vec3 to, double width, int forks, int lifetime, int color, int forkColor, int under) {
		Vec3 d = to.subtract(from);
		double length = d.length();
		if (length < 0.05) {
			return;
		}
		RandomSource r = level.getRandom();
		Vec3 dir = d.scale(1 / length);
		Vec3 u = perp(dir);
		Vec3 v = dir.cross(u);
		int segments = (int) Math.max(3, Math.min(10, Math.round(length / 0.6)));
		double jag = Math.min(0.4, 0.1 + length * 0.06);
		Vec3[] points = new Vec3[segments + 1];
		points[0] = from;
		for (int i = 1; i <= segments; i++) {
			Vec3 p = from.add(d.scale(i / (double) segments));
			if (i < segments) {
				p = p.add(u.scale((r.nextDouble() - 0.5) * 2 * jag)).add(v.scale((r.nextDouble() - 0.5) * 2 * jag));
			}
			points[i] = p;
			if (under != 0) {
				ray(level, points[i - 1], p, under, width * 2.6, lifetime + 1);
			}
			ray(level, points[i - 1], p, color, width, lifetime);
		}
		for (int k = 0; k < forks && segments > 2; k++) {
			Vec3 start = points[1 + r.nextInt(segments - 2)];
			Vec3 side = u.scale(r.nextDouble() - 0.5).add(v.scale(r.nextDouble() - 0.5)).normalize();
			Vec3 forkDir = dir.scale(0.6).add(side.scale(0.8)).normalize();
			double forkLength = length * (0.18 + 0.15 * r.nextDouble());
			Vec3 mid = start.add(forkDir.scale(forkLength * 0.5)).add(side.scale(jag * (r.nextDouble() - 0.5)));
			Vec3 end = start.add(forkDir.scale(forkLength));
			ray(level, start, mid, forkColor, width * 0.6, lifetime - 1);
			ray(level, mid, end, forkColor, width * 0.45, lifetime - 1);
		}
	}

	public static void sparks(ServerLevel level, Vec3 at, int count, double speed) {
		Vfx.radial(level, ParticleTypes.ELECTRIC_SPARK, at, count, speed);
	}

	public static void stormImpact(ServerLevel level, Vec3 at, double size) {
		Sigils.flash(level, at, STORM.secondary, (float) (1.4 * size));
		ring(level, at, UP, STORM.primary, 0.2 * size, 1.3 * size, 0.045 * size, 6);
		RandomSource r = level.getRandom();
		for (int i = 0; i < 3; i++) {
			bolt(level, at, at.add(randomDir(r).scale((0.8 + 0.5 * r.nextDouble()) * size)), 0.04 * size, 0, 2);
		}
		sparks(level, at, (int) Math.max(3, 8 * size), 0.35 * size);
	}

	// ------------------------------------------------------------------ wind: swirling arcs, spiralling gusts

	/**
	 * A gust: crescents swirling round an axis from {@code base} up {@code height}, each on its own
	 * tilt, spiralling as they climb. All sent at once: each arc takes a tick longer to sweep than
	 * the one below it, so the spiral winds upward.
	 */
	public static void swirl(ServerLevel level, Vec3 base, double radius, double height, int arcs) {
		swirl(level, base, radius, height, arcs, WIND.primary, WIND.secondary);
	}

	public static void swirl(ServerLevel level, Vec3 base, double radius, double height, int arcs, int color, int second) {
		double phase = level.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < arcs; i++) {
			double k = arcs == 1 ? 0 : i / (double) (arcs - 1);
			double a = phase + i * 2.1;
			slash(level, base.add(0, height * k, 0), tilted(0.25, a + Math.PI / 2), flatDir(a), i % 2 == 0 ? color : second, radius * (1 - 0.25 * k),
				2.3, 0.07, 1 + i, 6 + i);
		}
	}

	/** A ring of wind running out along the ground and a puff of air. */
	public static void gustRing(ServerLevel level, Vec3 feet, double radius) {
		groundRing(level, feet, WIND.secondary, 0.3, radius, 0.05, 9);
		groundRing(level, feet, WIND.accent, 0.2, radius * 0.7, 0.03, 11);
		Vfx.emit(level, ParticleTypes.SMALL_GUST, feet.add(0, 0.3, 0), 2, radius * 0.3, 0.0);
	}

	public static void windImpact(ServerLevel level, Vec3 at, double size) {
		Sigils.flash(level, at, WIND.secondary, (float) (0.9 * size));
		RandomSource r = level.getRandom();
		for (int i = 0; i < 3; i++) {
			Vec3 normal = randomDir(r);
			slash(level, at, normal, inPlane(normal, r.nextDouble() * Math.PI * 2), i == 1 ? WIND.accent : WIND.primary, 0.6 * size, 2.6, 0.07 * size,
				2, 6);
		}
		Vfx.emit(level, ParticleTypes.SMALL_GUST, at, 1, 0.0, 0.0);
		Vfx.radial(level, ParticleTypes.CLOUD, at, (int) Math.max(2, 4 * size), 0.12 * size);
	}

	// ------------------------------------------------------------------ earth: cracks, chips of the ground, stone

	/** The block under {@code at} (for chips and dust that match the ground), or dirt over air. */
	public static BlockState groundBlock(ServerLevel level, Vec3 at) {
		BlockState state = level.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
		return state.isAir() ? Blocks.DIRT.defaultBlockState() : state;
	}

	/** The ground cracks: a cracked seal, a ring of dust racing out over it and chips of it thrown up. */
	public static void crack(ServerLevel level, Vec3 feet, double radius, int lifetime) {
		flatSigil(level, feet, SigilOption.CRACKED, EARTH.secondary, radius, lifetime, 0.0);
		groundRing(level, feet, EARTH.primary, 0.3, radius * 1.15, 0.09, 10);
		BlockState ground = groundBlock(level, feet);
		BlockParticleOption chip = new BlockParticleOption(ParticleTypes.BLOCK, ground);
		RandomSource r = level.getRandom();
		int chips = (int) Math.max(5, Math.min(20, radius * 5));
		for (int i = 0; i < chips; i++) {
			double a = r.nextDouble() * Math.PI * 2;
			double rr = Math.sqrt(r.nextDouble()) * radius;
			Vfx.fling(level, chip, feet.add(Math.cos(a) * rr, 0.1, Math.sin(a) * rr), new Vec3(Math.cos(a) * 0.3, 1, Math.sin(a) * 0.3),
				0.22 + r.nextDouble() * 0.15);
		}
		Vfx.emit(level, new BlockParticleOption(ParticleTypes.DUST_PILLAR, ground), feet.add(0, 0.1, 0), (int) Math.max(3, Math.min(14, radius * 3)),
			radius * 0.4, 0.08);
	}

	/** Chips of {@code state} thrown out of {@code at}. */
	public static void stoneShards(ServerLevel level, Vec3 at, BlockState state, int count, double speed) {
		Vfx.radial(level, new BlockParticleOption(ParticleTypes.BLOCK, state), at, count, speed);
	}

	public static void earthImpact(ServerLevel level, Vec3 at, double size) {
		Sigils.flash(level, at, EARTH.secondary, (float) (1.1 * size));
		ring(level, at, UP, EARTH.primary, 0.2 * size, 1.2 * size, 0.08 * size, 9);
		ring(level, at, tilted(0.5, level.getRandom().nextDouble() * Math.PI * 2), EARTH.secondary, 0.15 * size, 0.8 * size, 0.04 * size, 7);
		stoneShards(level, at, groundBlock(level, at), (int) Math.max(4, 8 * size), 0.22 * size);
	}

	// ------------------------------------------------------------------ life: petals and leaves, a leaf spiral, a bloom

	private static final ColorParticleOption LEAF = ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, 0xFF000000 | LIFE.primary);

	/** Petals and leaves drifting down from {@code at}. */
	public static void petals(ServerLevel level, Vec3 at, double spread, int count) {
		Vfx.emit(level, ParticleTypes.CHERRY_LEAVES, at, (count + 1) / 2, spread, 0.0);
		Vfx.emit(level, LEAF, at, count / 2, spread, 0.0);
	}

	/**
	 * A leaf spiral: two strands of soft green arcs climbing round an axis from {@code base} up
	 * {@code height}. All sent at once: each arc takes a tick longer to sweep than the one below.
	 */
	public static void leafSpiral(ServerLevel level, Vec3 base, double radius, double height, int steps) {
		double phase = level.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < steps; i++) {
			double y = height * (i + 0.5) / steps;
			for (int strand = 0; strand < 2; strand++) {
				double a = phase + i * 1.0 + strand * Math.PI;
				slash(level, base.add(0, y, 0), tilted(0.3, a + Math.PI / 2), flatDir(a), strand == 0 ? LIFE.primary : LIFE.secondary,
					radius * (1 - 0.15 * i / steps), 1.1, 0.07, 1 + i, 7 + i);
			}
		}
	}

	/** A bloom: a gentle glow at {@code at}, a flower seal opening at {@code feet} and a slow ring spreading from it. */
	public static void bloom(ServerLevel level, Vec3 at, Vec3 feet, double size) {
		Sigils.flash(level, at, LIFE.secondary, (float) (1.1 * size));
		flatSigil(level, feet, SigilOption.STAR, LIFE.accent, 0.45 * size, 22, 0.04);
		groundRing(level, feet, LIFE.primary, 0.25, 1.1 * size, 0.045, 16);
	}

	public static void lifeImpact(ServerLevel level, Vec3 at, double size) {
		Sigils.flash(level, at, LIFE.secondary, (float) (1.1 * size));
		ring(level, at, UP, LIFE.primary, 0.15 * size, 1.0 * size, 0.05 * size, 12);
		double a = level.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < 2; i++) {
			Vec3 normal = tilted(0.9, a + i * Math.PI);
			slash(level, at, normal, inPlane(normal, a), i == 0 ? LIFE.primary : LIFE.accent, 0.45 * size, 2.4, 0.07, 3, 9);
		}
		petals(level, at, 0.3 * size, (int) Math.max(2, 4 * size));
	}

	// ------------------------------------------------------------------ void: darkness imploding, a black core

	/** Void implodes: dark rings falling in on {@code at} from {@code radius}, a thin violet rim running ahead of them. */
	public static void implode(ServerLevel level, Vec3 at, double radius, int lifetime) {
		double spin = level.getRandom().nextDouble() * Math.PI * 2;
		ring(level, at, UP, dark(VOID.accent), radius, 0.08, 0.1, lifetime);
		ring(level, at, tilted(1.1, spin), dark(VOID.accent), radius * 0.9, 0.08, 0.08, lifetime);
		ring(level, at, UP, VOID.primary, radius * 1.1, 0.12, 0.03, lifetime);
		// Portal particles fly in to where they're spawned.
		Vfx.emit(level, ParticleTypes.PORTAL, at, (int) Math.max(4, Math.min(16, radius * 5)), 0.1, radius * 0.6);
	}

	/** A black core: a hole in the world wrapped in thin violet rims. */
	public static void blackCore(ServerLevel level, Vec3 at, double radius, int lifetime) {
		orb(level, at, dark(VOID.accent), radius, lifetime);
		double a = level.getRandom().nextDouble() * Math.PI;
		ring(level, at, tilted(1.2, a), VOID.primary, radius * 1.4, radius * 1.3, 0.025, lifetime);
		ring(level, at, tilted(1.2, a + Math.PI / 2), VOID.secondary, radius * 1.4, radius * 1.3, 0.02, lifetime);
	}

	public static void voidImpact(ServerLevel level, Vec3 at, double size) {
		implode(level, at, 1.1 * size, 6);
		Scheduler.later(5, () -> {
			blackCore(level, at, 0.22 * size, 6);
			ring(level, at, UP, VOID.primary, 0.1, 0.9 * size, 0.035, 6);
			Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, at, (int) Math.max(3, 6 * size), 0.12 * size);
		});
	}

	// ------------------------------------------------------------------ arcane: star seals, orbiting motes, a shimmer

	/** A star seal: a star turning inside a rune ring that turns the other way. */
	public static void starSeal(ServerLevel level, Vec3 at, Vec3 normal, double size, int lifetime) {
		sigil(level, at, normal, SigilOption.STAR, ARCANE.primary, size, lifetime, 0.12);
		sigil(level, at.add(safe(normal).normalize().scale(0.01)), normal, SigilOption.RING, ARCANE.accent, size * 1.4, lifetime, -0.08);
	}

	/** Orbiting motes: comets of light tracing tilted orbits round {@code centre} over {@code ticks}. */
	public static void orbit(ServerLevel level, Vec3 centre, double radius, int count, int ticks) {
		orbit(level, centre, radius, count, ticks, ARCANE.primary, ARCANE.secondary);
	}

	public static void orbit(ServerLevel level, Vec3 centre, double radius, int count, int ticks, int color, int second) {
		double phase = level.getRandom().nextDouble() * Math.PI * 2;
		for (int i = 0; i < count; i++) {
			double a = phase + Math.PI * 2 * i / count;
			Vec3 normal = tilted(0.55, a);
			slash(level, centre, normal, inPlane(normal, a * 2), i % 2 == 0 ? color : second, radius, Math.PI * 1.5, 0.06, ticks, ticks + 4);
		}
	}

	/** A shimmer: glyphs drawn in to {@code at} and pink sparkles. */
	public static void shimmer(ServerLevel level, Vec3 at, double spread, int count) {
		// Enchanting glyphs fly in to where they're spawned.
		Vfx.emit(level, ParticleTypes.ENCHANT, at, count, 0.05, spread * 1.5);
		Vfx.emit(level, SpellParticleOption.create(ParticleTypes.INSTANT_EFFECT, 0xFF000000 | ARCANE.primary, 1.0F), at, (count + 1) / 2, spread, 0.0);
	}

	public static void arcaneImpact(ServerLevel level, Vec3 at, double size) {
		Sigils.flash(level, at, ARCANE.primary, (float) (1.3 * size));
		starSeal(level, at, UP, 0.5 * size, 14);
		orbit(level, at, 0.55 * size, 2, 5);
		shimmer(level, at, 0.3 * size, (int) Math.max(2, 5 * size));
	}

	// ------------------------------------------------------------------ time: clock faces, hands, golden ticks

	/**
	 * A clock face of light round {@code centre}: a gold ring, its hands sweeping round it over
	 * {@code ticks} (a long and a short crescent, turning like hands) and the quarter hours ticked
	 * in white. {@code backward} turns the hands the other way.
	 */
	public static void clock(ServerLevel level, Vec3 centre, Vec3 normal, double radius, int ticks, boolean backward) {
		Vec3 n = safe(normal).normalize();
		Vec3 facing = backward ? n.scale(-1) : n;
		Vec3 u = perp(n);
		Vec3 v = n.cross(u);
		ring(level, centre, n, TIME.primary, radius, radius, 0.03, ticks + 6);
		slash(level, centre, facing, u, TIME.secondary, radius * 0.85, Math.PI * 1.9, 0.07, ticks, ticks + 3);
		slash(level, centre, facing, v, TIME.accent, radius * 0.55, Math.PI * 1.2, 0.09, ticks, ticks + 3);
		for (int q = 0; q < 4; q++) {
			double a = Math.PI / 2 * q;
			Vec3 dir = u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
			ray(level, centre.add(dir.scale(radius * 0.82)), centre.add(dir.scale(radius * 1.12)), TIME.secondary, 0.035, ticks + 6);
		}
	}

	/** A stopped clock: the face, and both hands standing still at {@code hand} radians. */
	public static void stoppedClock(ServerLevel level, Vec3 centre, Vec3 normal, double radius, double hand, int lifetime) {
		Vec3 n = safe(normal).normalize();
		Vec3 u = perp(n);
		Vec3 v = n.cross(u);
		ring(level, centre, n, TIME.primary, radius, radius, 0.03, lifetime);
		ray(level, centre, centre.add(u.scale(Math.cos(hand) * radius * 0.8)).add(v.scale(Math.sin(hand) * radius * 0.8)), TIME.secondary, 0.04, lifetime);
		ray(level, centre, centre.add(u.scale(Math.cos(hand + 2.1) * radius * 0.5)).add(v.scale(Math.sin(hand + 2.1) * radius * 0.5)), TIME.accent,
			0.05, lifetime);
	}

	/** Golden ticks: flecks of gold and white drifting off {@code at}. */
	public static void goldenTicks(ServerLevel level, Vec3 at, double spread, int count) {
		Vfx.emit(level, ParticleTypes.WAX_ON, at, count, spread, 0.0);
		Vfx.emit(level, ParticleTypes.END_ROD, at, Math.max(1, count / 3), spread, 0.01);
	}

	public static void timeImpact(ServerLevel level, Vec3 at, double size) {
		Sigils.flash(level, at, TIME.secondary, (float) (1.2 * size));
		clock(level, at, UP, 0.6 * size, 6, false);
		goldenTicks(level, at, 0.3 * size, (int) Math.max(2, 4 * size));
	}

	// ------------------------------------------------------------------ blood: cuts, heartbeat rings, drops

	private static final ItemParticleOption DROP = new ItemParticleOption(ParticleTypes.ITEM, Items.REDSTONE);

	/** Crimson drops falling from a patch {@code spread} blocks round {@code at}. */
	public static void drip(ServerLevel level, Vec3 at, double spread, int count) {
		RandomSource r = level.getRandom();
		for (int i = 0; i < count; i++) {
			Vec3 p = at.add((r.nextDouble() - 0.5) * 2 * spread, (r.nextDouble() - 0.5) * spread, (r.nextDouble() - 0.5) * 2 * spread);
			Vfx.fling(level, DROP, p, new Vec3((r.nextDouble() - 0.5) * 0.2, -0.3, (r.nextDouble() - 0.5) * 0.2), 0.12);
		}
		Vfx.emit(level, new DustParticleOptions(BLOOD.primary, 0.9F), at, Math.max(1, count / 2), spread, 0.0);
	}

	/** A heartbeat: two rings pulsing out of {@code at}, the second a beat behind the first. */
	public static void pulse(ServerLevel level, Vec3 at, Vec3 normal, double radius) {
		ring(level, at, normal, BLOOD.primary, 0.15, radius, 0.06, 7);
		ring(level, at, normal, BLOOD.secondary, 0.05, radius * 0.7, 0.04, 11);
	}

	/** A cut: a crimson crescent round {@code centre}, bulging toward {@code toward}, a paler one just inside it. */
	public static void cut(ServerLevel level, Vec3 centre, Vec3 normal, Vec3 toward, double radius, double width) {
		slash(level, centre, normal, toward, BLOOD.primary, radius, 2.0, width, 1, 6);
		slash(level, centre.subtract(safe(toward).normalize().scale(0.15)), normal, toward, BLOOD.secondary, radius * 0.9, 1.7, width * 0.45, 1, 5);
	}

	public static void bloodImpact(ServerLevel level, Vec3 at, double size) {
		Sigils.flash(level, at, BLOOD.primary, (float) (1.1 * size));
		RandomSource r = level.getRandom();
		Vec3 normal = randomDir(r);
		Vec3 toward = inPlane(normal, r.nextDouble() * Math.PI * 2);
		double radius = 0.6 * size;
		cut(level, at.subtract(toward.scale(radius)), normal, toward, radius, 0.14 * size);
		pulse(level, at, UP, 0.9 * size);
		drip(level, at, 0.2 * size, (int) Math.max(2, 4 * size));
	}

	// ------------------------------------------------------------------ every element

	public static void neutralImpact(ServerLevel level, Vec3 at, double size) {
		Sigils.flash(level, at, NEUTRAL.primary, (float) (1.1 * size));
		ring(level, at, UP, NEUTRAL.primary, 0.15 * size, 1.0 * size, 0.05 * size, 8);
		Vfx.radial(level, ParticleTypes.END_ROD, at, (int) Math.max(2, 4 * size), 0.1 * size);
	}

	/** An element's signature impact, {@code size} about a block. */
	public static void impact(ServerLevel level, String element, Vec3 at, double size) {
		switch (element) {
			case "fire" -> fireImpact(level, at, size);
			case "frost" -> frostImpact(level, at, size);
			case "storm" -> stormImpact(level, at, size);
			case "wind" -> windImpact(level, at, size);
			case "earth" -> earthImpact(level, at, size);
			case "life" -> lifeImpact(level, at, size);
			case "void" -> voidImpact(level, at, size);
			case "arcane" -> arcaneImpact(level, at, size);
			case "time" -> timeImpact(level, at, size);
			case "blood" -> bloodImpact(level, at, size);
			default -> neutralImpact(level, at, size);
		}
	}
}
