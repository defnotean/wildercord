package dev.wildercord.client.fx;

import dev.wildercord.content.SigilOption;
import net.fabricmc.fabric.api.client.particle.v1.FabricSpriteSet;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A magic circle, tinted, facing any direction, turning around its own centre, drawn from both sides
 * at full brightness. It opens with a quick grow and fades out over the last part of its life.
 *
 * <p>The circle, rune ring, star, broken circle and reticle are drawn in thin lines of light, like a
 * spell's own circle (see {@link SpellCircleParticle}): rings, strokes and little glyphs laid down
 * as short pieces of a thin-line texture over a soft glow, so their lines stay fine and crisp however
 * big the circle is (a sigil twenty blocks across never turns into fat, blocky strokes). They draw
 * themselves in as they open, the outer rings sweeping round first. Only a small one (under
 * {@link #DRAWN_MIN} blocks) is still a single textured quad. A glow is always one.
 */
public class SigilParticle extends SingleQuadParticle implements SigilGroup.Extent {
	/** Sprites in {@link SigilOption} style order, set when the provider is created. */
	static FabricSpriteSet sprites;

	protected float yaw;
	protected float pitch;
	protected float spin;
	protected float size;
	/** Steady sigils (aim previews) skip the grow and fade: they're replaced every few ticks. */
	protected boolean steady;
	/** Drawn as darkness (void). */
	protected boolean dark;
	/** A glow faces the camera and bursts: full at once, then fading over its whole life. */
	protected final boolean glow;
	/** While drawing a glow's white-hot core, a smaller second quad over the tinted one. */
	private boolean core;
	/** A band is drawn as short straight pieces around the circle, so its line is equally thin at any size. */
	protected final boolean band;
	/** How long one piece of a band is, in blocks, at the band's full size. */
	private static final float BAND_PIECE = 0.13F;
	/** Circles, rings, stars, broken circles and reticles at least this big (in radius) are drawn in lines. */
	static final float DRAWN_MIN = 0.7F;

	protected final int style;
	/** Its colour as sent: for a dark one, the tint of the thin rim of light its lines keep. */
	private final int tint;
	/** A dark circle's rim: its tint brought up to a clear glow (void's near-black purple becomes violet). */
	private final int rim;
	/** Drawn in lines rather than one textured quad (see {@link #DRAWN_MIN}). */
	private final boolean drawn;
	/** A broken circle's breaks, each its own. */
	private final float[] breaks = new float[5];
	private final TextureAtlasSprite line;
	private final TextureAtlasSprite soft;
	private final TextureAtlasSprite spark;

	// While drawing.
	private QuadParticleRenderState drawing;
	private final Quaternionf plane = new Quaternionf();
	private final Quaternionf turnQ = new Quaternionf();
	private final Vector3f at = new Vector3f();
	private float px;
	private float py;
	private float pz;
	private float strokeAlpha;

	protected SigilParticle(ClientLevel level, double x, double y, double z, SigilOption option) {
		super(level, x, y, z, sprite(option.style()));
		this.yaw = option.yaw();
		this.pitch = option.pitch();
		this.spin = option.spin();
		this.size = option.size();
		this.quadSize = option.size();
		this.glow = option.style() == SigilOption.GLOW;
		this.band = option.style() == SigilOption.BAND;
		this.lifetime = Math.max(2, option.lifetime());
		this.gravity = 0;
		this.hasPhysics = false;
		this.xd = 0;
		this.yd = 0;
		this.zd = 0;
		this.roll = random.nextFloat() * Mth.TWO_PI;
		this.oRoll = roll;
		int color = option.color();
		if (MagicQuality.safeTelegraphs) {
			color = dev.wildercord.presentation.SafeColourRules.safe(color);
		}
		this.dark = (color & GlowLayers.DARK_FLAG) != 0;
		this.tint = color & 0xFFFFFF;
		this.rim = rimOf(tint);
		if (dark) {
			color = GlowLayers.darkColor(color);
		}
		setColor(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F);
		setAlpha(0);
		this.style = option.style();
		this.drawn = option.size() >= DRAWN_MIN && (style == SigilOption.CIRCLE || style == SigilOption.RING || style == SigilOption.STAR
			|| style == SigilOption.CRACKED || style == SigilOption.TARGET);
		float base = random.nextFloat() * Mth.TWO_PI;
		for (int i = 0; i < breaks.length; i++) {
			breaks[i] = base + Mth.TWO_PI * i / breaks.length + (random.nextFloat() - 0.5F) * 0.6F;
		}
		this.line = SpellCircleParticle.particleSprite("sigil_band");
		this.soft = SpellCircleParticle.particleSprite("sigil_soft");
		this.spark = SpellCircleParticle.particleSprite("sigil_glow");
	}

	static TextureAtlasSprite sprite(int style) {
		var list = sprites.getSprites();
		return list.get(Math.floorMod(style, list.size()));
	}

	/** A client-only sigil, e.g. the aim preview. */
	public static SigilParticle make(ClientLevel level, Vec3 at, SigilOption option, boolean steady) {
		SigilParticle particle = new SigilParticle(level, at.x, at.y, at.z, option);
		particle.steady = steady;
		if (steady) {
			// Replaced every few ticks: each one takes its turn from the world's clock, so they line up and it turns smoothly.
			particle.roll = (float) (level.getGameTime() * option.spin() % Mth.TWO_PI);
			particle.oRoll = particle.roll;
		}
		return particle;
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;
		oRoll = roll;
		roll += spin;
		if (age++ >= lifetime) {
			remove();
		}
	}

	/** Opening, then fading: 0 to 1. */
	protected float fade(float partial) {
		if (steady) {
			return 0.85F;
		}
		float t = age + partial;
		if (glow) {
			return Mth.clamp(t / 1.5F, 0, 1) * Mth.clamp((lifetime - t) / (lifetime * 0.75F), 0, 1);
		}
		float in = Mth.clamp(t / 4F, 0, 1);
		float out = Mth.clamp((lifetime - t) / (lifetime * 0.3F), 0, 1);
		return Math.min(in, out);
	}

	@Override
	public float getQuadSize(float partial) {
		if (steady) {
			return size;
		}
		if (glow) {
			float t = Mth.clamp((age + partial) / 3F, 0, 1);
			return size * (0.55F + 0.45F * (1 - (1 - t) * (1 - t))) * (core ? 0.42F : 1F);
		}
		float t = Mth.clamp((age + partial) / 6F, 0, 1);
		float ease = 1 - (1 - t) * (1 - t) * (1 - t);
		// One drawn in lines draws itself in as it opens, so it barely needs to grow.
		return size * (drawn ? 0.82F + 0.18F * ease : 0.35F + 0.65F * ease);
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partial) {
		alpha = fade(partial);
		if (alpha <= 0.01F) {
			return;
		}
		if (glow) {
			super.extract(state, camera, partial);
			float r = rCol;
			float g = gCol;
			float b = bCol;
			rCol = 1 - (1 - r) * 0.2F;
			gCol = 1 - (1 - g) * 0.2F;
			bCol = 1 - (1 - b) * 0.2F;
			core = true;
			super.extract(state, camera, partial);
			core = false;
			rCol = r;
			gCol = g;
			bCol = b;
			return;
		}
		float turn = Mth.lerp(partial, oRoll, roll);
		if (drawn) {
			Vec3 cam = camera.position();
			extractDrawn(state, (float) (Mth.lerp(partial, xo, x) - cam.x), (float) (Mth.lerp(partial, yo, y) - cam.y),
				(float) (Mth.lerp(partial, zo, z) - cam.z), turn, partial);
			return;
		}
		Quaternionf front = new Quaternionf().rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(pitch), turn);
		if (band) {
			Vec3 cam = camera.position();
			extractBand(state, front, (float) (Mth.lerp(partial, xo, x) - cam.x), (float) (Mth.lerp(partial, yo, y) - cam.y),
				(float) (Mth.lerp(partial, zo, z) - cam.z), partial);
			return;
		}
		extractRotatedQuad(state, camera, front, partial);
		Quaternionf back = new Quaternionf(front).rotateY(Mth.PI);
		extractRotatedQuad(state, camera, back, partial);
	}

	/**
	 * A band around ({@code px}, {@code py}, {@code pz}) (relative to the camera) in the plane of
	 * {@code plane}, its radius {@link #getQuadSize}: straight pieces laid end to end, each turned
	 * along the circle and drawn from both sides.
	 */
	protected void extractBand(QuadParticleRenderState state, Quaternionf plane, float px, float py, float pz, float partial) {
		float r = getQuadSize(partial);
		int pieces = Math.max(16, (int) Math.ceil(Mth.TWO_PI * size / BAND_PIECE));
		float half = Mth.PI * r / pieces * 1.06F;
		int color = ARGB.colorFromFloat(alpha, rCol, gCol, bCol);
		int light = getLightCoords(partial);
		Layer layer = getLayer();
		for (int i = 0; i < pieces; i++) {
			float a = Mth.TWO_PI * i / pieces;
			plane.transform(at.set(Mth.cos(a) * r, Mth.sin(a) * r, 0));
			Quaternionf piece = turnQ.set(plane).rotateZ(a + Mth.HALF_PI);
			for (int side = 0; side < 2; side++) {
				if (side == 1) {
					Facing.flip(piece);
				}
				state.add(layer, px + at.x, py + at.y, pz + at.z, piece.x, piece.y, piece.z, piece.w, half,
					getU0(), getU1(), getV0(), getV1(), color, light);
			}
		}
	}

	// ------------------------------------------------------------------ drawn in lines

	/**
	 * Little glyphs for the rune bands, as strokes in a box from -1 to 1 (x along the band, y outward):
	 * a stem with arms, a flagged stem, a diamond, a cross, an open peak, a zig-zag, an arrow, a
	 * crossed stem, a ladder, a chevron, a branch and a hook.
	 */
	private static final float[][] GLYPHS = {
		{0, -1, 0, 1, 0, 0.15F, -0.7F, 0.9F, 0, 0.15F, 0.7F, 0.9F},
		{0, -1, 0, 1, 0, 1, 0.7F, 0.5F, 0, 0.35F, 0.7F, -0.1F},
		{0, -1, 0.65F, 0, 0.65F, 0, 0, 1, 0, 1, -0.65F, 0, -0.65F, 0, 0, -1},
		{-0.65F, -1, 0.65F, 1, 0.65F, -1, -0.65F, 1},
		{-0.7F, -1, 0, 1, 0, 1, 0.7F, -1, -0.35F, -0.15F, 0.35F, -0.15F},
		{-0.6F, 1, 0.6F, 1, 0.6F, 1, -0.6F, -1, -0.6F, -1, 0.6F, -1},
		{0, -1, 0, 1, -0.6F, 0.4F, 0, 1, 0.6F, 0.4F, 0, 1},
		{0, -1, 0, 1, -0.7F, 0.25F, 0.7F, 0.25F},
		{-0.55F, -1, -0.55F, 1, 0.55F, -1, 0.55F, 1, -0.55F, 0.3F, 0.55F, -0.3F},
		{-0.65F, 1, 0, -0.9F, 0, -0.9F, 0.65F, 1},
		{0, -1, 0, 1, 0, -0.2F, -0.65F, 0.5F, 0, 0.35F, 0.65F, 1},
		{-0.5F, 1, -0.5F, -1, -0.5F, -1, 0.5F, -1, 0.5F, -1, 0.5F, -0.2F},
	};

	private static float part(float open, float from, float length) {
		return Mth.clamp((open - from) / length, 0, 1);
	}

	private static float ease(float t) {
		return 1 - (1 - t) * (1 - t);
	}

	private static int argb(float alpha, int rgb) {
		return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
	}

	private static int lighter(int rgb, float t) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return ((r + Math.round((255 - r) * t)) << 16) | ((g + Math.round((255 - g) * t)) << 8) | (b + Math.round((255 - b) * t));
	}

	/** {@code tint} scaled up until its brightest channel glows, then lightened a touch: the same hue, bright enough to see. */
	private static int rimOf(int tint) {
		int r = (tint >> 16) & 0xFF;
		int g = (tint >> 8) & 0xFF;
		int b = tint & 0xFF;
		int max = Math.max(1, Math.max(r, Math.max(g, b)));
		float k = Math.max(1, 220F / max);
		int scaled = (Math.min(255, Math.round(r * k)) << 16) | (Math.min(255, Math.round(g * k)) << 8) | Math.min(255, Math.round(b * k));
		return lighter(scaled, 0.15F);
	}

	/**
	 * The circle in lines, around ({@code cx}, {@code cy}, {@code cz}) relative to the camera: its soft
	 * glow, then its rings, strokes and glyphs, the outer parts turning with it and its heart turning
	 * slowly the other way.
	 */
	private void extractDrawn(QuadParticleRenderState state, float cx, float cy, float cz, float turn, float partial) {
		drawing = state;
		px = cx;
		py = cy;
		pz = cz;
		plane.rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(pitch), 0);
		float r = getQuadSize(partial);
		float open = steady ? 1 : Mth.clamp((age + partial) / 11F, 0, 1);
		strokeAlpha = alpha;
		float fine = Math.max(0.012F, r * 0.011F);
		float heavy = Math.max(0.02F, r * 0.024F);
		float inner = -turn * 0.6F;
		int main = lighter(tint, 0.15F);
		switch (style) {
			case SigilOption.CIRCLE -> {
				glowDisc(r * 1.12F, 0.16F);
				ring(r * 0.97F, heavy, 1, main, turn, Mth.TWO_PI * ease(part(open, 0, 0.5F)));
				ring(r * 0.82F, fine, 0.9F, tint, turn, Mth.TWO_PI * ease(part(open, 0.08F, 0.5F)));
				glyphs(r * 0.895F, r * 0.042F, 16, turn, fine, part(open, 0.35F, 0.35F), main);
				ring(r * 0.68F, fine, 0.85F, tint, -turn, Mth.TWO_PI * ease(part(open, 0.15F, 0.5F)));
				float star = part(open, 0.3F, 0.45F);
				for (int t = 0; t < 2; t++) {
					polygon(r * 0.68F, 3, inner + Mth.HALF_PI + t * Mth.PI, fine * 1.2F, star, main);
				}
				float knots = part(open, 0.6F, 0.3F);
				for (int k = 0; k < 6; k++) {
					float a = inner + Mth.HALF_PI + Mth.TWO_PI * k / 6;
					circle(Mth.cos(a) * r * 0.68F, Mth.sin(a) * r * 0.68F, r * 0.04F, fine, knots, lighter(tint, 0.4F));
				}
				ring(r * 0.34F, fine, 0.8F, tint, turn, Mth.TWO_PI * ease(part(open, 0.35F, 0.4F)));
				polygon(r * 0.2F, 4, inner * 2 + Mth.PI / 4, fine, part(open, 0.5F, 0.4F), main);
				ring(r * 0.09F, fine, part(open, 0.6F, 0.3F), lighter(tint, 0.3F), 0, Mth.TWO_PI);
				heart(Math.min(0.45F, r * 0.11F), part(open, 0.5F, 0.4F));
			}
			case SigilOption.RING -> {
				glowBand(r * 0.905F, r * 0.13F, 0.12F);
				ring(r * 0.97F, heavy * 0.8F, 1, main, turn, Mth.TWO_PI * ease(part(open, 0, 0.55F)));
				ring(r * 0.84F, fine, 0.85F, tint, -turn, Mth.TWO_PI * ease(part(open, 0.1F, 0.55F)));
				glyphs(r * 0.905F, r * 0.04F, 12, turn + Mth.PI / 12, fine, part(open, 0.35F, 0.4F), main);
				float dots = part(open, 0.5F, 0.4F);
				for (int k = 0; k < 12; k++) {
					float a = turn + Mth.TWO_PI * k / 12;
					dot(Mth.cos(a) * r * 0.905F, Mth.sin(a) * r * 0.905F, Math.max(0.035F, r * 0.02F), dots, lighter(tint, 0.5F));
				}
			}
			case SigilOption.STAR -> {
				glowDisc(r * 0.95F, 0.15F);
				float drawnIn = part(open, 0.05F, 0.55F);
				polygon(r * 0.93F, 4, turn, fine * 1.3F, drawnIn, main);
				polygon(r * 0.93F, 4, turn + Mth.PI / 4, fine * 1.3F, drawnIn, main);
				ring(r * 0.93F, fine, 0.35F, tint, turn, Mth.TWO_PI * ease(part(open, 0, 0.6F)));
				ring(r * 0.46F, fine, 0.85F, tint, inner, Mth.TWO_PI * ease(part(open, 0.2F, 0.5F)));
				ring(r * 0.26F, fine, 0.75F, lighter(tint, 0.2F), -inner, Mth.TWO_PI * ease(part(open, 0.3F, 0.5F)));
				float points = part(open, 0.5F, 0.4F);
				for (int k = 0; k < 8; k++) {
					float a = turn + Mth.PI / 4 * k;
					dot(Mth.cos(a) * r * 0.93F, Mth.sin(a) * r * 0.93F, Math.max(0.04F, r * 0.035F), points, lighter(tint, 0.5F));
				}
				heart(Math.min(0.4F, r * 0.09F), points);
			}
			case SigilOption.CRACKED -> {
				glowDisc(r * 1.0F, 0.12F);
				float sweep = ease(part(open, 0, 0.5F));
				for (int k = 0; k < breaks.length; k++) {
					float next = breaks[(k + 1) % breaks.length] + (k + 1 == breaks.length ? Mth.TWO_PI : 0);
					float from = breaks[k] + turn + 0.12F;
					ring(r * 0.87F, heavy, 1, main, from, (next + turn - 0.12F - from) * sweep);
					float from2 = breaks[k] + turn + 0.5F;
					ring(r * 0.7F, fine, 0.75F, tint, from2, (next + turn + 0.1F - from2) * sweep);
				}
				float run = part(open, 0.1F, 0.6F);
				for (int k = 0; k < breaks.length; k++) {
					crack(r, breaks[k] + turn, k, fine * 1.5F, run, main);
				}
			}
			case SigilOption.TARGET -> {
				glowDisc(r * 1.02F, 0.13F);
				float sweep = ease(part(open, 0, 0.5F));
				int dashes = 32;
				for (int k = 0; k < dashes; k++) {
					float a = turn + Mth.TWO_PI * k / dashes;
					ring(r * 0.96F, heavy * 0.8F, 1, main, a, Mth.TWO_PI / dashes * 0.62F * sweep);
				}
				ring(r * 0.75F, fine, 0.45F, tint, turn, Mth.TWO_PI * sweep);
				float arrows = part(open, 0.2F, 0.5F);
				for (int k = 0; k < 4; k++) {
					float a = turn + Mth.HALF_PI * k;
					float tipX = Mth.cos(a) * r * 0.6F;
					float tipY = Mth.sin(a) * r * 0.6F;
					for (int side = -1; side <= 1; side += 2) {
						float b = a + side * 0.14F;
						stroke(tipX, tipY, Mth.cos(b) * r * 0.775F, Mth.sin(b) * r * 0.775F, fine * 1.4F, arrows, main);
					}
				}
				float cross = part(open, 0.4F, 0.4F) * 0.7F;
				stroke(-r * 0.07F, 0, r * 0.07F, 0, fine, cross, tint);
				stroke(0, -r * 0.07F, 0, r * 0.07F, fine, cross, tint);
			}
			default -> { }
		}
		drawing = null;
	}

	/** A soft round glow under the whole circle, {@code rad} across its middle. */
	private void glowDisc(float rad, float strength) {
		int color = argb(strokeAlpha * strength, dark ? GlowLayers.darkColor(tint) : tint);
		piece(soft, 0, 0, 0, rad, color, dark ? GlowLayers.DARK : GlowLayers.GLOW);
	}

	/** A soft glow along a ring of radius {@code rad}, {@code width} wide: a rune ring has nothing inside it to light. */
	private void glowBand(float rad, float width, float strength) {
		int color = argb(strokeAlpha * strength, dark ? GlowLayers.darkColor(tint) : tint);
		int n = Math.max(8, Math.min(48, (int) Math.ceil(Mth.TWO_PI * rad / (width * 1.6F))));
		for (int i = 0; i < n; i++) {
			float a = Mth.TWO_PI * i / n;
			piece(soft, Mth.cos(a) * rad, Mth.sin(a) * rad, 0, width, color, dark ? GlowLayers.DARK : GlowLayers.GLOW);
		}
	}

	/** A bright heart at the centre (for a dark circle, its darkest point). */
	private void heart(float half, float shown) {
		if (shown <= 0) {
			return;
		}
		if (dark) {
			piece(soft, 0, 0, 0, half * 1.6F, argb(strokeAlpha * shown, GlowLayers.darkColor(tint)), GlowLayers.DARK);
			return;
		}
		piece(spark, 0, 0, 0, half, argb(strokeAlpha * shown * 0.9F, lighter(tint, 0.6F)), GlowLayers.GLOW);
	}

	/** A small glowing point. */
	private void dot(float u, float v, float half, float shown, int color) {
		if (shown <= 0) {
			return;
		}
		piece(spark, u, v, 0, half, argb(strokeAlpha * shown, dark ? rim : color), GlowLayers.GLOW);
	}

	/**
	 * An arc of radius {@code rad} from angle {@code from}, sweeping {@code sweep} radians (a whole ring
	 * at 2π), of line {@code width}: short pieces of the line texture laid end to end.
	 */
	private void ring(float rad, float width, float strength, int color, float from, float sweep) {
		if (sweep <= 1.0E-3F || rad <= 0 || strength <= 0) {
			return;
		}
		ringAt(0, 0, rad, width, strength, color, from, sweep);
	}

	/**
	 * An arc around (u, v): light in {@code color}, or for a dark circle a wider stroke of darkness
	 * with a thinner line of its tint running along it (the rim that makes void read as a hole).
	 */
	private void ringAt(float u, float v, float rad, float width, float strength, int color, float from, float sweep) {
		if (dark) {
			arcPieces(u, v, rad, width * 1.9F, argb(strokeAlpha * strength, GlowLayers.darkColor(tint)), GlowLayers.DARK, from, sweep);
			arcPieces(u, v, rad, width, argb(strokeAlpha * strength * 0.5F, rim), GlowLayers.GLOW, from, sweep);
			return;
		}
		arcPieces(u, v, rad, width, argb(strokeAlpha * strength, color), GlowLayers.GLOW, from, sweep);
	}

	/** Pieces of line laid end to end round an arc, as many as its width needs so the line keeps its width. */
	private void arcPieces(float u, float v, float rad, float width, int argb, Layer layer, float from, float sweep) {
		if ((argb >>> 24) < 3) {
			return;
		}
		float length = Math.abs(sweep) * rad;
		int n = Math.max(3, Math.min(600, (int) Math.ceil(length / (width * 3.2F))));
		float half = length / n / 2 * 1.08F;
		float step = sweep / n;
		for (int i = 0; i < n; i++) {
			float a = from + step * (i + 0.5F);
			piece(line, u + Mth.cos(a) * rad, v + Mth.sin(a) * rad, a + Mth.HALF_PI, half, argb, layer);
		}
	}

	/** A small ring around (u, v), drawn in as {@code shown} goes from 0 to 1. */
	private void circle(float u, float v, float rad, float width, float shown, int color) {
		if (shown <= 0) {
			return;
		}
		ringAt(u, v, rad, width, 1, color, 0, Mth.TWO_PI * shown);
	}

	/** A regular polygon's edges, corners on radius {@code rad} from angle {@code start}, each drawn in from its corner as {@code shown} goes to 1. */
	private void polygon(float rad, int corners, float start, float width, float shown, int color) {
		if (shown <= 0) {
			return;
		}
		for (int k = 0; k < corners; k++) {
			float a0 = start + Mth.TWO_PI * k / corners;
			float a1 = start + Mth.TWO_PI * (k + 1) / corners;
			float x0 = Mth.cos(a0) * rad;
			float y0 = Mth.sin(a0) * rad;
			float x1 = Mth.cos(a1) * rad;
			float y1 = Mth.sin(a1) * rad;
			stroke(x0, y0, x0 + (x1 - x0) * shown, y0 + (y1 - y0) * shown, width, 1, color);
		}
	}

	/** {@code count} glyphs round a band of radius {@code rad}, each {@code half} tall either side of it. */
	private void glyphs(float rad, float half, int count, float start, float width, float shown, int color) {
		if (shown <= 0) {
			return;
		}
		for (int k = 0; k < count; k++) {
			float a = start + Mth.TWO_PI * k / count;
			float nx = Mth.cos(a);
			float ny = Mth.sin(a);
			float tx = -ny;
			float ty = nx;
			float cu = nx * rad;
			float cv = ny * rad;
			float[] g = GLYPHS[k % GLYPHS.length];
			for (int i = 0; i + 3 < g.length; i += 4) {
				stroke(cu + (tx * g[i] + nx * g[i + 1]) * half, cv + (ty * g[i] + ny * g[i + 1]) * half,
					cu + (tx * g[i + 2] + nx * g[i + 3]) * half, cv + (ty * g[i + 2] + ny * g[i + 3]) * half, width, shown, color);
			}
		}
	}

	/** One crack running out from radius 0.38 to past the rim at angle {@code at}, jagging as it goes, with a fork off it. */
	private void crack(float r, float at, int which, float width, float run, int color) {
		if (run <= 0) {
			return;
		}
		int steps = 5;
		float r0 = r * 0.38F;
		float step = r * 0.135F;
		float x = Mth.cos(at + 0.1F) * r0;
		float y = Mth.sin(at + 0.1F) * r0;
		float shown = run * steps;
		for (int i = 0; i < steps && shown > 0; i++) {
			// A fissure wanders a little either side of its line, never the same way twice.
			float jag = Mth.sin(which * 1.7F + i * 2.3F + breaks[which]) * 0.085F;
			float rr = r0 + step * (i + 1);
			float nx = Mth.cos(at + jag) * rr;
			float ny = Mth.sin(at + jag) * rr;
			float f = Math.min(1, shown);
			stroke(x, y, x + (nx - x) * f, y + (ny - y) * f, width * (1 - i * 0.12F), 1, color);
			if (i == 2 && shown > 1) {
				// A fork off the middle of it.
				float side = which % 2 == 0 ? 0.38F : -0.38F;
				float fx = nx + Mth.cos(at + side) * step * 0.9F;
				float fy = ny + Mth.sin(at + side) * step * 0.9F;
				float g = Math.min(1, shown - 1);
				stroke(nx, ny, nx + (fx - nx) * g, ny + (fy - ny) * g, width * 0.6F, 1, color);
			}
			x = nx;
			y = ny;
			shown -= 1;
		}
	}

	/** A straight stroke of {@code width} from (u0, v0) to (u1, v1). */
	private void stroke(float u0, float v0, float u1, float v1, float width, float strength, int color) {
		if (strength <= 0) {
			return;
		}
		if (dark) {
			strokePieces(u0, v0, u1, v1, width * 1.9F, argb(strokeAlpha * strength, GlowLayers.darkColor(tint)), GlowLayers.DARK);
			strokePieces(u0, v0, u1, v1, width, argb(strokeAlpha * strength * 0.5F, rim), GlowLayers.GLOW);
			return;
		}
		strokePieces(u0, v0, u1, v1, width, argb(strokeAlpha * strength, color), GlowLayers.GLOW);
	}

	private void strokePieces(float u0, float v0, float u1, float v1, float width, int argb, Layer layer) {
		float du = u1 - u0;
		float dv = v1 - v0;
		float length = Mth.sqrt(du * du + dv * dv);
		if (length < 1.0E-4F || (argb >>> 24) < 3) {
			return;
		}
		int n = Math.max(1, Math.min(200, (int) Math.ceil(length / (width * 3.2F))));
		float half = length / n / 2 * 1.08F;
		float rot = (float) Math.atan2(dv, du);
		for (int i = 0; i < n; i++) {
			float t = (i + 0.5F) / n;
			piece(line, u0 + du * t, v0 + dv * t, rot, half, argb, layer);
		}
	}

	/** One square piece of {@code sprite} at (u, v) in the circle's plane, turned {@code rot}, drawn from both sides. */
	private void piece(TextureAtlasSprite sprite, float u, float v, float rot, float half, int argb, Layer layer) {
		if ((argb >>> 24) < 3 || half <= 0) {
			return;
		}
		int light = LightCoordsUtil.FULL_BRIGHT;
		plane.transform(at.set(u, v, 0.002F));
		turnQ.set(plane).rotateZ(rot);
		drawing.add(layer, px + at.x, py + at.y, pz + at.z, turnQ.x, turnQ.y, turnQ.z, turnQ.w, half,
			sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), argb, light);
		plane.transform(at.set(u, v, -0.002F));
		Facing.flip(turnQ);
		drawing.add(layer, px + at.x, py + at.y, pz + at.z, turnQ.x, turnQ.y, turnQ.z, turnQ.w, half,
			sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), argb, light);
	}

	@Override
	public int getLightCoords(float partial) {
		return LightCoordsUtil.FULL_BRIGHT;
	}

	@Override
	protected Layer getLayer() {
		return dark ? GlowLayers.DARK : GlowLayers.GLOW;
	}

	@Override
	public ParticleRenderType getGroup() {
		return SigilGroup.TYPE;
	}

	public float size() {
		return size;
	}

	@Override
	public double centreX() {
		return x;
	}

	@Override
	public double centreY() {
		return y;
	}

	@Override
	public double centreZ() {
		return z;
	}

	@Override
	public double reach() {
		return size + 0.5;
	}

	public double x() {
		return x;
	}

	public double y() {
		return y;
	}

	public double z() {
		return z;
	}

	public static class Provider implements ParticleProvider<SigilOption> {
		public Provider(FabricSpriteSet set) {
			sprites = set;
		}

		@Override
		public Particle createParticle(SigilOption option, ClientLevel level, double x, double y, double z, double xa, double ya, double za,
				RandomSource random) {
			return new SigilParticle(level, x, y, z, option);
		}
	}
}
