package dev.wildercord.client.fx;

import dev.wildercord.Wildercord;
import dev.wildercord.content.SpellCircleOption;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Secrets;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellSigil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A spell's whole magic circle, built from its runes every frame (the layout is
 * {@link SpellSigil}'s): a soft glow; a heavy frame with rays on the star's points; a band of
 * script made of the spell's rune emblems; a band in the first effect's pattern; a star polygon
 * with a roundel on each point (a rune's pattern around its emblem); an inner ring; and the seal.
 * The bands and the star turn, each its own way. It opens in stages: the frame, then the script and
 * the star's lines drawing themselves, then the roundels one by one, in casting order.
 *
 * <p>Lines are drawn as short straight pieces of a thin-line texture, and bands as tiles laid end to
 * end, so every line keeps its width and every pattern its size at any radius. Everything is drawn
 * from both sides at full brightness.
 */
public class SpellCircleParticle extends SingleQuadParticle implements SigilGroup.Extent {
	/** One rune on the circle: its ring pattern, its emblem and its colour. */
	/** A rune's ring and emblem in its colour, and a fused rune's second half ({@code band2}, {@code mark2}, null for others) in its partner's. */
	protected record Rune(TextureAtlasSprite band, TextureAtlasSprite mark, int color, TextureAtlasSprite band2, TextureAtlasSprite mark2, int color2) {
		Rune(TextureAtlasSprite band, TextureAtlasSprite mark, int color) {
			this(band, mark, color, null, null, 0);
		}

		static Rune of(String id, RuneDef def) {
			int color = def == null ? 0xFFFFFF : RuneColors.of(def);
			int second = def == null ? -1 : RuneColors.second(def);
			if (second < 0) {
				return new Rune(runeSprite(id, def, "band"), runeSprite(id, def, "mark"), color);
			}
			return new Rune(runeSprite(id, def, "band"), runeSprite(id, def, "mark"), color, runeSprite(id, def, "band2"), runeSprite(id, def, "mark2"), second);
		}
	}

	protected final List<Rune> runes = new ArrayList<>();
	protected final int color;
	protected final float radius;
	protected final float yaw;
	protected final float pitch;
	private final int points;
	/** A secret spell's id: its circle gets a centrepiece of its own instead of the star. */
	private final String secret;
	private final float roundel;
	private final Rune pattern;
	protected final TextureAtlasSprite line;
	protected final TextureAtlasSprite glow;
	/** A smooth round glow, for the big soft light behind the whole circle (the other has a sparkle and steps). */
	protected final TextureAtlasSprite soft;
	private final TextureAtlasSprite[] script;
	private final dev.wildercord.spell.CircleDisciplines.Design design;
	private final List<Integer> materials = new ArrayList<>();

	/** How far each part has turned: the script band, the pattern band (the other way) and the star. */
	private float scriptTurn;
	private float oScriptTurn;
	private float patternTurn;
	private float oPatternTurn;
	private float starTurn;
	private float oStarTurn;

	// While drawing a frame.
	private QuadParticleRenderState state;
	private final Quaternionf plane = new Quaternionf();
	private final Quaternionf turn = new Quaternionf();
	private final Vector3f at = new Vector3f();
	private float cx;
	private float cy;
	private float cz;
	private int light;

	protected SpellCircleParticle(ClientLevel level, double x, double y, double z, SpellCircleOption option) {
		super(level, x, y, z, particleSprite("sigil_band"));
		this.color = option.color() & 0xFFFFFF;
		this.radius = option.radius();
		this.yaw = option.yaw();
		this.pitch = option.pitch();
		Rune firstEffect = null;
		List<RuneDef> defs = new ArrayList<>();
		for (String id : option.runes().subList(0, Math.min(option.runes().size(), SpellSigil.MAX_RUNES))) {
			RuneDef def = Runes.get(id).orElse(null);
			if (def != null) {
				defs.add(def);
			}
			Rune rune = Rune.of(id, def);
			runes.add(rune);
			if (firstEffect == null && def != null && def.family() == RuneFamily.EFFECT) {
				firstEffect = rune;
			}
		}
		if (runes.isEmpty()) {
			runes.add(new Rune(particleSprite("circle/_shape_band"), particleSprite("circle/_shape_mark"), 0xFFFFFF));
		}
		this.pattern = firstEffect != null ? firstEffect : runes.getFirst();
		List<RuneDef> flat=dev.wildercord.spell.Knots.flatten(defs);
		this.design = dev.wildercord.spell.CircleDisciplines.design(flat);
		for(RuneDef def:flat) if(def.family()==RuneFamily.EFFECT) {
			for(RuneDef leaf:dev.wildercord.spell.WovenRunes.isWoven(def)?dev.wildercord.spell.WovenRunes.contents(def):List.of(def)) {
				int primary=RuneColors.of(leaf), secondary=RuneColors.second(leaf);
				if(!materials.contains(primary)&&materials.size()<10)materials.add(primary);
				if(secondary>=0&&!materials.contains(secondary)&&materials.size()<10)materials.add(secondary);
			}
		}
		this.secret = defs.size() == option.runes().size() ? Secrets.match(defs).map(Secrets.Secret::id).orElse(null) : null;
		this.points = SpellSigil.points(runes.size());
		this.roundel = SpellSigil.roundel(runes.size());
		this.line = particleSprite("sigil_band");
		this.glow = particleSprite("sigil_glow");
		this.soft = particleSprite("sigil_soft");
		this.script = runes.stream().map(Rune::mark).toArray(TextureAtlasSprite[]::new);
		this.starTurn = 0;
		this.scriptTurn = level.getRandom().nextFloat() * Mth.TWO_PI;
		this.patternTurn = level.getRandom().nextFloat() * Mth.TWO_PI;
		this.lifetime = Math.max(2, option.lifetime());
		this.gravity = 0;
		this.hasPhysics = false;
		this.xd = 0;
		this.yd = 0;
		this.zd = 0;
		setAlpha(0);
	}

	static TextureAtlasSprite particleSprite(String path) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(Wildercord.id(path));
	}

	/** A rune's pattern ({@code band}) or emblem ({@code mark}), drawn by tools/circle_art.py; add-ons get their family's. */
	static TextureAtlasSprite runeSprite(String rune, RuneDef def, String part) {
		TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES);
		Identifier id = Identifier.tryParse(rune);
		if (id != null && id.getNamespace().equals(Wildercord.MOD_ID)) {
			TextureAtlasSprite own = atlas.getSprite(Wildercord.id("circle/" + id.getPath() + "_" + part));
			if (own != atlas.missingSprite()) {
				return own;
			}
		}
		// A Knot is a spell tied up: it wears the links' ring and emblem.
		String family = def == null ? "effect" : def.family() == RuneFamily.KNOT ? "link" : def.family().name().toLowerCase(Locale.ROOT);
		return atlas.getSprite(Wildercord.id("circle/_" + family + "_" + part));
	}

	@Override
	public void tick() {
		xo = x;
		yo = y;
		zo = z;
		oScriptTurn = scriptTurn;
		scriptTurn += switch(design) { case GYRE -> .035F; case ANCHOR,VIGIL -> .003F; case ECLIPSE -> -.009F; default -> .012F; };
		oPatternTurn = patternTurn;
		patternTurn -= switch(design) { case TEMPEST -> .045F; case RESERVOIR,MERCY -> .008F; default -> .018F; };
		oStarTurn = starTurn;
		starTurn += switch(design) { case ANCHOR -> 0; case GYRE,PILGRIM -> .024F; case CONFLUENCE -> -.016F; default -> .006F; };
		if (age++ >= lifetime) {
			remove();
		}
	}

	/** How far the circle has opened, 0 to 1. */
	protected float opening(float partial) {
		// Short-lived cast and telegraph circles must finish drawing before their spell
		// releases. Ritual, shield and display circles keep their slower unfolding.
		float openingTicks = lifetime <= 50 ? 2.5F : 6F + runes.size();
		return Mth.clamp((age + partial) / openingTicks, 0, 1);
	}

	/** Its brightness: quickly in, and out over the last part of its life. */
	protected float fade(float partial) {
		float t = age + partial;
		return Math.min(Mth.clamp(t / 2F, 0, 1), Mth.clamp((lifetime - t) / (lifetime * 0.3F), 0, 1));
	}

	/** Its radius right now, in blocks. */
	protected float size(float partial) {
		return radius;
	}

	protected Vec3 centre(float partial) {
		return new Vec3(Mth.lerp(partial, xo, x), Mth.lerp(partial, yo, y), Mth.lerp(partial, zo, z));
	}

	/** The circle's orientation: local x and y lie in its plane, local y up the circle. */
	protected Quaternionf orientation(float partial) {
		return new Quaternionf().rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(pitch), 0);
	}

	private static float part(float open, float from, float length) {
		return Mth.clamp((open - from) / length, 0, 1);
	}

	protected static int argb(float alpha, int rgb) {
		return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
	}

	protected static int lighter(int rgb, float t) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return ((r + Math.round((255 - r) * t)) << 16) | ((g + Math.round((255 - g) * t)) << 8) | (b + Math.round((255 - b) * t));
	}

	@Override
	public void extract(QuadParticleRenderState state, Camera camera, float partial) {
		float alpha = fade(partial);
		if (alpha <= 0.01F) {
			return;
		}
		this.state = state;
		float open = opening(partial);
		plane.set(orientation(partial));
		Vec3 c = centre(partial);
		Vec3 cam = camera.position();
		cx = (float) (c.x - cam.x);
		cy = (float) (c.y - cam.y);
		cz = (float) (c.z - cam.z);
		light = LightCoordsUtil.FULL_BRIGHT;
		float grow = part(open, 0, 0.3F);
		float r = size(partial) * (0.55F + 0.45F * (1 - (1 - grow) * (1 - grow)));
		// Once it's open, a gentle pulse.
		float pulse = open >= 1 ? 0.85F + 0.15F * Mth.sin((age + partial) * 0.35F) : 1F;
		float a = alpha * pulse;
		float fine = Math.max(0.004F, SpellSigil.FINE * r);
		float heavy = Math.max(0.007F, SpellSigil.HEAVY * r);
		float star = Mth.lerp(partial, oStarTurn, starTurn);

		// The glow behind it all.
		piece(soft, 0, 0, 0, r * 1.25F, argb(a * 0.3F * (0.4F + 0.6F * open), color), 0);

		// The frame, and rays on the star's points.
		float frame = part(open, 0, 0.2F);
		ring(0, 0, r * SpellSigil.FRAME, heavy, argb(a * frame, color), 0.002F);
		ring(0, 0, r * SpellSigil.FRAME_INNER, fine, argb(a * frame * 0.9F, color), 0.002F);
		for (int k = 0; k < points; k++) {
			float ang = star + Mth.HALF_PI - Mth.TWO_PI * k / points;
			line(Mth.cos(ang) * r * SpellSigil.FRAME, Mth.sin(ang) * r * SpellSigil.FRAME,
				Mth.cos(ang) * r * (SpellSigil.FRAME + (SpellSigil.RAYS - SpellSigil.FRAME) * frame),
				Mth.sin(ang) * r * (SpellSigil.FRAME + (SpellSigil.RAYS - SpellSigil.FRAME) * frame), fine, argb(a * frame, color), 0.002F);
		}

		// The script: the spell's emblems, in order, round and round.
		float writing = part(open, 0.1F, 0.25F);
		tiles(script, 0, 0, r * SpellSigil.SCRIPT, r * SpellSigil.SCRIPT_HEIGHT, Mth.lerp(partial, oScriptTurn, scriptTurn),
			argb(a * writing * 0.95F, lighter(color, 0.25F)), 0.003F, true);
		ring(0, 0, r * SpellSigil.SCRIPT_INNER, fine, argb(a * writing, color), 0.002F);

		// The pattern band, in the first effect's own ring pattern.
		tiles(new TextureAtlasSprite[] {pattern.band}, 0, 0, r * SpellSigil.PATTERN, r * SpellSigil.PATTERN_HEIGHT,
			Mth.lerp(partial, oPatternTurn, patternTurn), argb(a * writing, pattern.color), 0.003F, false);
		if (pattern.band2 != null) {
			tiles(new TextureAtlasSprite[] {pattern.band2}, 0, 0, r * SpellSigil.PATTERN, r * SpellSigil.PATTERN_HEIGHT,
				Mth.lerp(partial, oPatternTurn, patternTurn), argb(a * writing, pattern.color2), 0.0031F, false);
		}

		// The star: its circle, then its lines drawing themselves from every point.
		float drawn = part(open, 0.2F, 0.35F);
		if (secret != null) {
			// A secret spell: its own centrepiece, and its seal; no roundels.
			secretCentre(secret, r, drawn, a, fine, heavy, star, partial);
			float middle = part(open, 0.3F, 0.3F);
			Rune first = runes.getFirst();
			piece(first.mark, 0, 0, -star * 2, r * SpellSigil.SEAL / 2 * (0.6F + 0.4F * middle), argb(a * middle, lighter(color, 0.2F)), 0.008F);
			if (first.mark2 != null) {
				piece(first.mark2, 0, 0, -star * 2, r * SpellSigil.SEAL / 2 * (0.6F + 0.4F * middle), argb(a * middle, lighter(first.color2, 0.2F)), 0.0081F);
			}
			this.state = null;
			return;
		}
		ring(0, 0, r * SpellSigil.STAR, fine, argb(a * drawn * 0.7F, color), 0.004F);
		circleCentre(r * SpellSigil.STAR, drawn, a, fine, star, age+partial);

		// The inner rings and the seal.
		float middle = part(open, 0.3F, 0.3F);
		ring(0, 0, r * SpellSigil.INNER, fine, argb(a * middle, color), 0.004F);
		ring(0, 0, r * SpellSigil.MEDALLION, fine, argb(a * middle, lighter(color, 0.3F)), 0.004F);
		Rune first = runes.getFirst();
		piece(first.mark, 0, 0, -star * 2, r * SpellSigil.SEAL / 2 * (0.6F + 0.4F * middle), argb(a * middle, first.color), 0.007F);
		if (first.mark2 != null) {
			piece(first.mark2, 0, 0, -star * 2, r * SpellSigil.SEAL / 2 * (0.6F + 0.4F * middle), argb(a * middle, first.color2), 0.0071F);
		}

		// The roundels, one by one: a rune's pattern around its emblem, on its star point.
		int n = runes.size();
		float s = r * roundel;
		for (int i = 0; i < n; i++) {
			float shown = part(open, 0.35F + 0.55F * i / n, 0.1F);
			if (shown <= 0) {
				continue;
			}
			Rune rune = runes.get(i);
			float ang = star + Mth.HALF_PI - Mth.TWO_PI * SpellSigil.pointOf(i, n) / points;
			float u = Mth.cos(ang) * r * SpellSigil.STAR;
			float v = Mth.sin(ang) * r * SpellSigil.STAR;
			float rs = s * (0.5F + 0.5F * shown);
			// A soft glow behind the roundel, so it stands out from the star's lines.
			piece(glow, u, v, 0, rs * 1.25F, argb(a * shown * 0.35F, lighter(rune.color, 0.5F)), 0.005F);
			ring(u, v, rs, fine, argb(a * shown, rune.color), 0.005F);
			tiles(new TextureAtlasSprite[] {rune.band}, u, v, rs * 0.72F, rs * 0.4F, -star * 3, argb(a * shown * 0.9F, rune.color), 0.006F, false);
			piece(rune.mark, u, v, ang - Mth.HALF_PI, rs * 0.5F, argb(a * shown, lighter(rune.color, 0.15F)), 0.007F);
			if (rune.band2 != null) {
				// A fused rune: its partner element's half, in that element's colour.
				tiles(new TextureAtlasSprite[] {rune.band2}, u, v, rs * 0.72F, rs * 0.4F, -star * 3, argb(a * shown * 0.9F, rune.color2), 0.0061F, false);
				piece(rune.mark2, u, v, ang - Mth.HALF_PI, rs * 0.5F, argb(a * shown, lighter(rune.color2, 0.15F)), 0.0071F);
			}
		}
		extras(r, a, fine, partial);
		this.state = null;
	}

	/**
	 * Anything a kind of circle draws over the ordinary one (a Shield's ripples and cracks), in the
	 * circle's own plane: {@code r} is its radius right now, {@code a} its brightness, {@code fine}
	 * its fine line width.
	 */
	protected void extras(float r, float a, float fine, float partial) {
	}

	/**
	 * The centrepiece of a secret spell's circle, drawn inside the script and pattern bands in place
	 * of the star and roundels: every secret has a design no ordinary spell can make.
	 */
	/** Use the same authored mechanism as the Cord screen preview. */
	private void circleCentre(float r,float open,float alpha,float fine,float turn,float time) {
		var colors=materials.stream().map(rgb->argb(alpha*open*.8F,rgb)).toList();
		dev.wildercord.spell.CircleGeometry.draw(design,r,open,fine,turn,time,argb(alpha*open,lighter(color,.3F)),colors,
			(x0,y0,x1,y1,width,ink)->line(x0,y0,x1,y1,width,ink,.005F));
	}

	private void secretCentre(String id, float r, float drawn, float a, float fine, float heavy, float star, float partial) {
		int main = argb(a * Math.min(1, drawn * 1.5F), lighter(color, 0.35F));
		int soft = argb(a * drawn * 0.7F, color);
		float inner = r * SpellSigil.STAR;
		switch (id) {
			case "sunfall" -> {
				// A sun: a burning disc, and long and short rays all round.
				ring(0, 0, r * 0.28F, heavy, main, 0.004F);
				ring(0, 0, r * 0.36F, fine, soft, 0.004F);
				for (int k = 0; k < 24; k++) {
					float ang = star * 2 + Mth.TWO_PI * k / 24;
					// Long and short rays grow out from the disc as the circle opens.
					float end = 0.4F + ((k % 2 == 0 ? 0.74F : 0.56F) - 0.4F) * drawn;
					line(Mth.cos(ang) * r * 0.4F, Mth.sin(ang) * r * 0.4F, Mth.cos(ang) * r * end, Mth.sin(ang) * r * end,
						k % 2 == 0 ? heavy * 0.7F : fine, main, 0.004F);
				}
			}
			case "glacial_lance" -> {
				// A snowflake: six spokes, each with branches, inside a hexagon.
				polygon(6, inner, star, fine, soft, 1);
				for (int k = 0; k < 6; k++) {
					float ang = star + Mth.TWO_PI * k / 6;
					float cx = Mth.cos(ang);
					float cy = Mth.sin(ang);
					line(0, 0, cx * inner * drawn, cy * inner * drawn, heavy * 0.6F, main, 0.004F);
					for (float along : new float[] {0.45F, 0.72F}) {
						float bx = cx * inner * along;
						float by = cy * inner * along;
						float len = inner * (0.9F - along) * 0.6F * drawn;
						for (int side = -1; side <= 1; side += 2) {
							float ba = ang + side * Mth.PI / 3;
							line(bx, by, bx + Mth.cos(ba) * len, by + Mth.sin(ba) * len, fine, main, 0.004F);
						}
					}
				}
			}
			case "horizon_cut" -> {
				// A horizon: one straight cut across the whole circle, and two crescents facing it.
				float w = inner * 1.35F * drawn;
				line(-w, 0, w, 0, heavy, main, 0.004F);
				arc(0, inner * 0.2F, inner * 0.62F, Mth.PI * 1.15F, Mth.PI * 1.85F, fine, main, 0.004F);
				arc(0, -inner * 0.2F, inner * 0.62F, Mth.PI * 0.15F, Mth.PI * 0.85F, fine, main, 0.004F);
				ring(0, 0, inner, fine, soft, 0.004F);
			}
			case "petal_storm" -> {
				// A flower: six overlapping circles around a seventh, turning slowly.
				float pr = inner * 0.5F;
				ring(0, 0, pr * drawn, fine, main, 0.004F);
				for (int k = 0; k < 6; k++) {
					float ang = star * 3 + Mth.TWO_PI * k / 6;
					ring(Mth.cos(ang) * pr, Mth.sin(ang) * pr, pr * drawn, fine, main, 0.004F);
				}
				ring(0, 0, inner, fine, soft, 0.004F);
			}
			case "tempest_step" -> {
				// A storm: jagged bolts from the centre out to five points, a chain of lightning round them.
				for (int k = 0; k < 5; k++) {
					float ang = star * 2 + Mth.TWO_PI * k / 5 + Mth.HALF_PI;
					zigzag(0, 0, Mth.cos(ang) * inner * drawn, Mth.sin(ang) * inner * drawn, 4, inner * 0.09F, fine * 1.4F, main);
				}
				for (int k = 0; k < 5; k++) {
					float a0 = star * 2 + Mth.TWO_PI * k / 5 + Mth.HALF_PI;
					float a1 = star * 2 + Mth.TWO_PI * (k + 1) / 5 + Mth.HALF_PI;
					zigzag(Mth.cos(a0) * inner, Mth.sin(a0) * inner, Mth.cos(a1) * inner, Mth.sin(a1) * inner, 3, inner * 0.06F, fine, soft);
				}
			}
			case "singularity" -> {
				// A black star: rings spiralling into a centre that swallows the light.
				for (int k = 0; k < 5; k++) {
					float rad = inner * (1 - k * 0.18F) * drawn;
					arc(0, 0, rad, star * (3 + k) + k, star * (3 + k) + k + Mth.PI * 1.5F, fine, main, 0.004F);
				}
				piece(glow, 0, 0, 0, inner * 0.55F, argb(a * drawn * 0.9F, 0x000000), 0.005F, GlowLayers.DARK, GlowLayers.darkColor(color));
			}
			case "zero_hour" -> {
				// A clock: twelve hour marks, and two hands that do not move.
				ring(0, 0, inner, fine, soft, 0.004F);
				for (int k = 0; k < 12; k++) {
					float ang = Mth.HALF_PI - Mth.TWO_PI * k / 12;
					float from = k % 3 == 0 ? 0.78F : 0.86F;
					line(Mth.cos(ang) * inner * from, Mth.sin(ang) * inner * from, Mth.cos(ang) * inner * 0.96F, Mth.sin(ang) * inner * 0.96F,
						k % 3 == 0 ? heavy * 0.7F : fine, main, 0.004F);
				}
				line(0, 0, 0, inner * 0.7F * drawn, heavy * 0.8F, main, 0.005F);
				line(0, 0, inner * 0.45F * drawn, 0, heavy * 0.8F, main, 0.005F);
			}
			case "rebirth" -> {
				// Wings: two great feathered arcs rising from the centre, a flame between them.
				for (int side = -1; side <= 1; side += 2) {
					for (int f = 0; f < 5; f++) {
						float rad = inner * (0.45F + f * 0.12F) * drawn;
						float base = side < 0 ? Mth.PI * 0.55F : Mth.PI * 0.45F;
						float span = 0.9F - f * 0.08F;
						arc(side * inner * 0.1F, -inner * 0.2F, rad, side < 0 ? base : base - span, side < 0 ? base + span : base, fine, main, 0.004F);
					}
				}
				line(0, -inner * 0.35F, 0, inner * 0.55F * drawn, heavy * 0.7F, main, 0.005F);
			}
			case "tectonic_rise" -> {
				// Stone: nested squares, each turned against the last.
				for (int k = 0; k < 4; k++) {
					polygon(4, inner * (1 - k * 0.2F) * drawn, star * (k % 2 == 0 ? 1 : -1) + k * Mth.PI / 8, k == 0 ? heavy * 0.6F : fine, main, 1);
				}
			}
			case "starlight_cascade" -> {
				// A constellation: stars joined by lines, falling across the circle.
				float[][] stars = {{-0.7F, 0.55F}, {-0.35F, 0.7F}, {0.05F, 0.45F}, {0.4F, 0.62F}, {0.25F, 0.1F}, {-0.15F, -0.25F}, {0.3F, -0.6F}};
				for (int k = 0; k < stars.length; k++) {
					float u = stars[k][0] * inner;
					float v = stars[k][1] * inner;
					if (k + 1 < stars.length && drawn * stars.length > k + 1) {
						line(u, v, stars[k + 1][0] * inner, stars[k + 1][1] * inner, fine, soft, 0.004F);
					}
					if (drawn * stars.length > k) {
						piece(glow, u, v, 0, inner * 0.14F, argb(a, lighter(color, 0.5F)), 0.006F);
					}
				}
				ring(0, 0, inner, fine, soft, 0.004F);
			}
			default -> ring(0, 0, inner, fine, main, 0.004F);
		}
	}

	/** A regular polygon of {@code sides} with corners on radius {@code rad}, turned by {@code turn}. */
	private void polygon(int sides, float rad, float turn, float width, int argb, int step) {
		for (int k = 0; k < sides; k++) {
			float a0 = turn + Mth.HALF_PI - Mth.TWO_PI * k / sides;
			float a1 = turn + Mth.HALF_PI - Mth.TWO_PI * (k + step) / sides;
			line(Mth.cos(a0) * rad, Mth.sin(a0) * rad, Mth.cos(a1) * rad, Mth.sin(a1) * rad, width, argb, 0.004F);
		}
	}

	/** An arc of a circle around (u, v) from angle {@code from} to {@code to}. */
	protected void arc(float u, float v, float rad, float from, float to, float width, int argb, float depth) {
		if ((argb >>> 24) < 3 || rad <= 0) {
			return;
		}
		float length = Math.abs(to - from) * rad;
		int n = Math.max(3, (int) Math.ceil(length / (width * 3.2F)));
		float half = length / n / 2 * 1.08F;
		for (int i = 0; i < n; i++) {
			float ang = from + (to - from) * (i + 0.5F) / n;
			piece(line, u + Mth.cos(ang) * rad, v + Mth.sin(ang) * rad, ang + Mth.HALF_PI, half, argb, depth);
		}
	}

	/** A jagged line from (u0, v0) to (u1, v1): {@code kinks} sharp turns, each up to {@code jag} off the straight. */
	private void zigzag(float u0, float v0, float u1, float v1, int kinks, float jag, float width, int argb) {
		float du = u1 - u0;
		float dv = v1 - v0;
		float length = Mth.sqrt(du * du + dv * dv);
		if (length < 1.0E-4F) {
			return;
		}
		float nu = -dv / length;
		float nv = du / length;
		float pu = u0;
		float pv = v0;
		for (int i = 1; i <= kinks + 1; i++) {
			float t = i / (float) (kinks + 1);
			float off = i <= kinks ? (i % 2 == 0 ? jag : -jag) : 0;
			float qu = u0 + du * t + nu * off;
			float qv = v0 + dv * t + nv * off;
			line(pu, pv, qu, qv, width, argb, 0.004F);
			pu = qu;
			pv = qv;
		}
	}

	/** One square piece of {@code sprite} at ({@code u}, {@code v}) in the circle's plane, drawn from both sides. */
	private void piece(TextureAtlasSprite sprite, float u, float v, float rot, float half, int argb, float depth, Layer layer, int rgb) {
		piece(sprite, u, v, rot, half, (argb & 0xFF000000) | (rgb & 0xFFFFFF), depth, layer);
	}

	protected void piece(TextureAtlasSprite sprite, float u, float v, float rot, float half, int argb, float depth) {
		piece(sprite, u, v, rot, half, argb, depth, getLayer());
	}

	private void piece(TextureAtlasSprite sprite, float u, float v, float rot, float half, int argb, float depth, Layer layer) {
		if ((argb >>> 24) < 3 || half <= 0) {
			return;
		}
		plane.transform(at.set(u, v, depth));
		turn.set(plane).rotateZ(rot);
		state.add(layer, cx + at.x, cy + at.y, cz + at.z, turn.x, turn.y, turn.z, turn.w, half,
			sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), argb, light);
		plane.transform(at.set(u, v, -depth));
		Facing.flip(turn);
		state.add(layer, cx + at.x, cy + at.y, cz + at.z, turn.x, turn.y, turn.z, turn.w, half,
			sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), argb, light);
	}

	/** A thin ring of {@code width} around (u, v): short pieces of line laid end to end. */
	protected void ring(float u, float v, float rad, float width, int argb, float depth) {
		if ((argb >>> 24) < 3 || rad <= 0) {
			return;
		}
		float pieceLength = width * 3.2F;
		int n = Math.max(12, (int) Math.ceil(Mth.TWO_PI * rad / pieceLength));
		float half = Mth.PI * rad / n * 1.08F;
		for (int i = 0; i < n; i++) {
			float a = Mth.TWO_PI * i / n;
			piece(line, u + Mth.cos(a) * rad, v + Mth.sin(a) * rad, a + Mth.HALF_PI, half, argb, depth);
		}
	}

	/** A straight line of {@code width} from (u0, v0) to (u1, v1). */
	protected void line(float u0, float v0, float u1, float v1, float width, int argb, float depth) {
		float du = u1 - u0;
		float dv = v1 - v0;
		float length = Mth.sqrt(du * du + dv * dv);
		if ((argb >>> 24) < 3 || length < 1.0E-4F) {
			return;
		}
		int n = Math.max(1, (int) Math.ceil(length / (width * 3.2F)));
		float half = length / n / 2 * 1.08F;
		float rot = (float) Math.atan2(dv, du);
		for (int i = 0; i < n; i++) {
			float t = (i + 0.5F) / n;
			piece(line, u0 + du * t, v0 + dv * t, rot, half, argb, depth);
		}
	}

	/**
	 * A band of tiles around (u, v), {@code height} tall, top edge outward, cycling through
	 * {@code sprites}; with {@code whole}, always a whole number of cycles, so the order reads true.
	 */
	protected void tiles(TextureAtlasSprite[] sprites, float u, float v, float rad, float height, float angle, int argb, float depth, boolean whole) {
		if ((argb >>> 24) < 3 || rad <= 0 || height <= 0) {
			return;
		}
		int n = Math.max(6, Math.round(Mth.TWO_PI * rad / height));
		if (whole && sprites.length > 1) {
			n = Math.max(sprites.length, Math.round(n / (float) sprites.length) * sprites.length);
		}
		float half = Mth.PI * rad / n;
		for (int i = 0; i < n; i++) {
			float a = angle + Mth.TWO_PI * i / n;
			piece(sprites[i % sprites.length], u + Mth.cos(a) * rad, v + Mth.sin(a) * rad, a - Mth.HALF_PI, half, argb, depth);
		}
	}

	@Override
	public int getLightCoords(float partial) {
		return LightCoordsUtil.FULL_BRIGHT;
	}

	@Override
	protected Layer getLayer() {
		return GlowLayers.GLOW;
	}

	@Override
	public ParticleRenderType getGroup() {
		return SigilGroup.TYPE;
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
		return radius * 1.3 + 0.5;
	}

	public static class Provider implements ParticleProvider<SpellCircleOption> {
		@Override
		public Particle createParticle(SpellCircleOption option, ClientLevel level, double x, double y, double z, double xa, double ya, double za,
				RandomSource random) {
			return new SpellCircleParticle(level, x, y, z, option);
		}
	}
}
