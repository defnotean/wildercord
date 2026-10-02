package dev.wildercord.client.fx;

import dev.wildercord.aura.AuraFx;
import dev.wildercord.aura.WayRules;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;

/** Woven, double-sided standards on bronze poles. Cloth flexes along its own plane; it never faces the camera. */
public final class AuraStandard extends SingleQuadParticle implements SigilGroup.Extent {
	private record Key(int owner, String way, Vec3 at) {}
	private static final Map<Key, AuraStandard> LIVE = new HashMap<>();
	private final Key key;
	private final Vec3 facing;
	private final TextureAtlasSprite cloth;
	private final TextureAtlasSprite pole;
	private final TextureAtlasSprite finial;
	private final float scale;
	private final int clothTint;
	private float emphasis;
	private int quiet;
	private int keep;

	public static void receive(AuraFx.Standard cue) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		LIVE.values().removeIf(p -> !p.isAlive() || p.level != mc.level);
		Key key = new Key(cue.owner(), cue.way(), cue.at());
		AuraStandard existing = LIVE.get(key);
		if (existing != null) {
			existing.quiet = 0;
			existing.keep = Mth.clamp(cue.ticks(), 4, 100);
			existing.emphasis = Mth.clamp(cue.emphasis(), 0.1F, 1.6F);
			return;
		}
		if (LIVE.size() >= 48) return;
		AuraStandard made = new AuraStandard(mc.level, cue, key);
		LIVE.put(key, made);
		mc.particleEngine.add(made);
	}

	public static void clear() { LIVE.values().forEach(AuraStandard::remove); LIVE.clear(); }
	public static int showing() { return LIVE.size(); }

	private AuraStandard(ClientLevel level, AuraFx.Standard cue, Key key) {
		super(level, cue.at().x, cue.at().y, cue.at().z, SpellCircleParticle.particleSprite("aura_standard_pole"));
		this.key = key;
		Vec3 flat = new Vec3(cue.facing().x, 0, cue.facing().z);
		this.facing = flat.lengthSqr() < 1.0E-5 ? new Vec3(0, 0, 1) : flat.normalize();
		String way = WayRules.BUILT_IN.contains(cue.way()) ? cue.way() : "unknown";
		this.cloth = SpellCircleParticle.particleSprite("aura_standard_" + way);
		this.pole = SpellCircleParticle.particleSprite("aura_standard_pole");
		this.finial = SpellCircleParticle.particleSprite("aura_standard_finial");
		this.scale = Mth.clamp(cue.scale(), 0.25F, 1.5F);
		this.clothTint = way.equals("unknown") ? (cue.color() & 0xFFFFFF) : 0xFFFFFF;
		this.emphasis = Mth.clamp(cue.emphasis(), 0.1F, 1.6F);
		this.keep = Mth.clamp(cue.ticks(), 4, 100);
		this.hasPhysics = false;
		this.gravity = 0;
	}

	@Override public void tick() {
		age++;
		if (++quiet > keep + 8) { remove(); LIVE.remove(key, this); }
	}

	@Override public void extract(QuadParticleRenderState state, Camera camera, float partial) {
		Minecraft mc = Minecraft.getInstance();
		// A rally standard above your own head stays outside first-person sight.
		if (scale < 0.9F && mc.player != null && key.owner() == mc.player.getId() && mc.getCameraEntity() == mc.player
			&& mc.options.getCameraType().isFirstPerson()) return;
		float fade = Math.min(1, (age + partial) / 8F) * Mth.clamp((keep + 8 - quiet - partial) / 8F, 0, 1);
		if (fade <= 0.01F) return;
		Vec3 base = new Vec3(x, y, z).subtract(camera.position());
		// The outer rally standard flies inward, so its hem stays in the choice's first-person view.
		boolean inward = scale >= 0.9F && key.way().equals(WayRules.BANNER);
		Vec3 side = new Vec3(facing.z, 0, -facing.x).scale(inward ? -1 : 1);
		float rise = Mth.clamp((age + partial) / 12F, 0, 1);
		float time = (float) ((level.getGameTime() % 10000) + partial);
		float yaw = (float) Math.atan2(facing.x, facing.z) + (inward ? Mth.PI : 0);
		int tone = emphasis < 0.8F ? 0xFF9C96AA : 0xFFFFFFFF;
		int tint = ((Math.round(fade * 255) & 255) << 24) | (tone & 0xFFFFFF);
		// Four narrow faces give the pole volume even when seen edge-on.
		for (int face = 0; face < 4; face++) {
			Quaternionf q = new Quaternionf().rotationY(yaw + face * Mth.HALF_PI);
			for (int i = 0; i < 26; i++) {
				Vec3 at = base.add(0, (i + 0.5) * 0.1 * scale * rise, 0);
				quad(state, pole, at, q, 0.052F * scale, 0, 0, 1, 1, tint);
			}
		}
		// Preserve the cloth silhouette while reducing its folds for calm settings or distant viewers.
		int cols = base.lengthSqr() > 24 * 24 || MagicQuality.bodyAura == MagicQuality.BodyAura.OFF ? 2
			: MagicQuality.bodyAura == MagicQuality.BodyAura.CALM ? 4 : 6;
		int rows = cols * 3 / 2;
		float tile = 0.9F * scale / cols;
		for (int c = 0; c < cols; c++) {
			float u = (c + 0.5F) / cols;
			float bend = (float) Math.sin(time * 0.12F - u * 4 + key.at().x) * 0.13F * u * scale;
			float turn = (float) Math.cos(time * 0.12F - u * 4 + key.at().x) * 0.26F * u;
			Quaternionf front = new Quaternionf().rotationY(yaw + turn);
			Quaternionf back = new Quaternionf(front).rotateY(Mth.PI);
			for (int r = 0; r < rows; r++) {
				float droop = 0.06F * u * u * scale;
				Vec3 at = base.add(side.scale((c + 0.5) * tile + 0.035 * scale))
					.add(facing.scale(bend)).add(0, (2.45F * scale - (r + 0.5F) * tile - droop) * rise, 0);
				float shade = 0.9F + 0.1F * (float) Math.cos(time * 0.12F - u * 4);
				float dim = emphasis < 0.8F ? 0.6F : shade;
				int rgb = (Math.round(((clothTint >> 16) & 255) * dim) << 16)
					| (Math.round(((clothTint >> 8) & 255) * dim) << 8) | Math.round((clothTint & 255) * dim);
				int ink = (tint & 0xFF000000) | rgb;
				// A narrow overlap seals the joints while neighbouring cloth sections turn in the wind.
				quad(state, cloth, at, front, tile * 0.535F, c / (float) cols, r / (float) rows, (c+1F)/cols, (r+1F)/rows, ink);
				quad(state, cloth, at, back, tile * 0.535F, (c+1F)/cols, r/(float)rows, c/(float)cols, (r+1F)/rows, ink);
			}
		}
		// Bronze finial and collar, with a small pressure streamer above the crest.
		Quaternionf q = new Quaternionf().rotationY(yaw);
		quad(state, finial, base.add(0, 2.66 * scale * rise, 0), q, 0.12F * scale, 0, 0, 1, 1, tint);
		quad(state, finial, base.add(0, 2.66 * scale * rise, 0), new Quaternionf(q).rotateY(Mth.PI), 0.12F * scale, 1, 0, 0, 1, tint);
		if (emphasis > 1.1F && MagicQuality.bodyAura != MagicQuality.BodyAura.OFF) {
			LightStrokes paint = new LightStrokes(state, GlowLayers.GLOW);
			for (int i = 0; i < 3; i++) {
				Vec3 a = base.add(side.scale((0.18 + i * 0.23) * scale)).add(0, 2.55 * scale * rise, 0);
				Vec3 b = a.add(facing.scale(0.08 * Math.sin(time * 0.16 + i))).add(side.scale(0.18 * scale)).add(0, 0.16 * scale, 0);
				paint.segment(true, (float)a.x, (float)a.y, (float)a.z, (float)b.x, (float)b.y, (float)b.z, 0.025F * scale,
					LightStrokes.argb(fade * 0.6F, 0xF0D99B));
			}
		}
	}

	private static void quad(QuadParticleRenderState state, TextureAtlasSprite sprite, Vec3 at, Quaternionf q, float half,
		float u0, float v0, float u1, float v1, int color) {
		state.add(Layer.TRANSLUCENT, (float)at.x, (float)at.y, (float)at.z, q.x, q.y, q.z, q.w, half,
			Mth.lerp(u0, sprite.getU0(), sprite.getU1()), Mth.lerp(u1, sprite.getU0(), sprite.getU1()),
			Mth.lerp(v0, sprite.getV0(), sprite.getV1()), Mth.lerp(v1, sprite.getV0(), sprite.getV1()), color, LightCoordsUtil.FULL_BRIGHT);
	}
	@Override protected Layer getLayer() { return Layer.TRANSLUCENT; }
	@Override public ParticleRenderType getGroup() { return SigilGroup.TYPE; }
	@Override public double centreX() { return x; }
	@Override public double centreY() { return y + 1.4 * scale; }
	@Override public double centreZ() { return z; }
	@Override public double reach() { return 1.7 * scale; }
}
