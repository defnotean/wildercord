package dev.wildercord.client.fx;

import dev.wildercord.content.RitualOption;
import dev.wildercord.content.SpellCircleOption;
import dev.wildercord.content.WildercordItems;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Attunements, on each client (see {@link RitualOption}): the land's own magic circle (the circle of
 * the rune it gives) opening under the meditating player and slowly turning, and motes of its colour
 * rising off the ground and spiralling up into the Blank Rune in their hand, where a light gathers.
 * It builds through the four stages: each one flares the circle and adds to it (a rune band round
 * the rim, then a ring of rays, then an outer ring with lights at its quarters), brighter and busier
 * each time. At the finish the circle flares and rings race out from it, the gathered light bursts,
 * and the rune's emblem shines over the hand and drifts up. If the server stops sending (the
 * meditation broke), it fades away.
 */
public final class RitualCircles {
	private RitualCircles() {}

	/** The ritual showing for each player, by id. */
	private static final Map<Integer, Ritual> SHOWING = new HashMap<>();

	public static void tick(Minecraft mc) {
		if (mc.level == null) {
			SHOWING.clear();
			return;
		}
		SHOWING.values().removeIf(r -> !r.isAlive() || r.level() != mc.level);
	}

	public static class Provider implements ParticleProvider<RitualOption> {
		@Override
		public Particle createParticle(RitualOption option, ClientLevel level, double x, double y, double z, double xa, double ya, double za,
				RandomSource random) {
			Entity found = level.getEntity(option.entity());
			if (!(found instanceof LivingEntity player)) {
				return null;
			}
			Ritual showing = SHOWING.get(player.getId());
			if (showing != null && showing.isAlive() && showing.rune.equals(option.rune())) {
				showing.feed(option.progress());
				return null;
			}
			if (showing != null) {
				showing.leave();
			}
			Ritual ritual = new Ritual(level, player, option);
			SHOWING.put(player.getId(), ritual);
			return ritual;
		}
	}

	/** Where the Blank Rune is held: out in front of your own view in first person, else in the player's hand. */
	static Vec3 hand(LivingEntity player, float partial) {
		Minecraft mc = Minecraft.getInstance();
		if (player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) {
			Vec3 eye = player.getEyePosition(partial);
			Vec3 look = player.getViewVector(partial);
			Vec3 right = look.cross(new Vec3(0, 1, 0));
			right = right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
			Vec3 down = right.cross(look).normalize().scale(-1);
			boolean main = player.getMainHandItem().is(WildercordItems.BLANK_RUNE) || !player.getOffhandItem().is(WildercordItems.BLANK_RUNE);
			boolean rightHand = main == (player.getMainArm() == HumanoidArm.RIGHT);
			return eye.add(look.scale(1.05)).add(right.scale(rightHand ? 0.36 : -0.36)).add(down.scale(0.3));
		}
		float yaw = Mth.lerp(partial, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
		Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
		Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
		boolean main = player.getMainHandItem().is(WildercordItems.BLANK_RUNE) || !player.getOffhandItem().is(WildercordItems.BLANK_RUNE);
		boolean rightHand = main == (player.getMainArm() == HumanoidArm.RIGHT);
		Vec3 feet = new Vec3(Mth.lerp(partial, player.xo, player.getX()), Mth.lerp(partial, player.yo, player.getY()), Mth.lerp(partial, player.zo, player.getZ()));
		return feet.add(0, player.isCrouching() ? 0.68 : 0.8, 0).add(forward.scale(0.28)).add(right.scale(rightHand ? 0.36 : -0.36));
	}

	/** How many motes a tick, for how busy the player has asked particles to be. */
	private static float busy() {
		ParticleStatus status = Minecraft.getInstance().options.particles().get();
		return status == ParticleStatus.ALL ? 1F : status == ParticleStatus.DECREASED ? 0.5F : 0.2F;
	}

	/** The land's circle under one player. */
	static final class Ritual extends SpellCircleParticle {
		/** The circle's radius as it opens; it grows a little as the ritual builds. */
		private static final float RADIUS = 1.45F;
		/** Ticks without word from the server before it fades; how long it takes to fade; how long the finish lasts. */
		private static final int QUIET = 12;
		private static final int FADE = 10;
		private static final int FINISH = 26;

		final String rune;
		private final LivingEntity player;
		private float progress;
		private float shown;
		private float oShown;
		private int sinceFed;
		private int leaving = -1;
		private int finishing = -1;
		/** When each stage (1 to 3) was reached, in this circle's age; -1 until then. */
		private final int[] reached = {-1, -1, -1, -1};
		private float motes;
		private float turn;
		private float oTurn;

		Ritual(ClientLevel level, LivingEntity player, RitualOption option) {
			super(level, player.getX(), player.getY() + 0.07, player.getZ(),
				new SpellCircleOption(List.of(option.rune()), option.color(), RADIUS, 0, -90, 20 * 60 * 5));
			this.rune = option.rune();
			this.player = player;
			this.progress = option.progress();
			this.shown = progress;
			this.oShown = progress;
			if (progress >= 1) {
				finish();
			}
		}

		ClientLevel level() {
			return level;
		}

		void feed(float p) {
			progress = Math.max(progress, p);
			sinceFed = 0;
			if (progress >= 1 && finishing < 0) {
				finish();
			}
		}

		void leave() {
			if (leaving < 0 && finishing < 0) {
				leaving = 0;
			}
		}

		private int stage() {
			return Math.min(4, (int) (shown * 4 + 1.0E-3F));
		}

		@Override
		public void tick() {
			super.tick();
			oShown = shown;
			shown += (progress - shown) * 0.25F;
			oTurn = turn;
			turn += 0.01F + 0.02F * shown;
			sinceFed++;
			if (player.isRemoved()) {
				leave();
			}
			for (int s = 1; s <= 3; s++) {
				if (reached[s] < 0 && stage() >= s) {
					reached[s] = age;
				}
			}
			if (finishing >= 0) {
				if (++finishing > FINISH) {
					remove();
				}
			} else {
				if (sinceFed > QUIET) {
					leave();
				}
				if (leaving >= 0 && ++leaving > FADE) {
					remove();
				}
			}
			xo = x;
			yo = y;
			zo = z;
			x = player.getX();
			y = player.getY() + 0.07;
			z = player.getZ();
			if (leaving < 0 && finishing < 0) {
				rise();
			}
		}

		/** Motes of the land drifting up off the circle and spiralling into the hand, more as it builds. */
		private void rise() {
			Minecraft mc = Minecraft.getInstance();
			motes += (0.55F + 2.6F * shown) * busy();
			Vec3 into = hand(player, 1);
			float r = RADIUS * (1 + 0.12F * shown);
			while (motes >= 1) {
				motes -= 1;
				float a = random.nextFloat() * Mth.TWO_PI;
				float d = r * (0.45F + 0.6F * random.nextFloat());
				Vec3 from = new Vec3(x + Mth.cos(a) * d, y + 0.02 + random.nextFloat() * 0.1, z + Mth.sin(a) * d);
				int tint = random.nextInt(4) == 0 ? lighter(color, 0.65F) : color;
				mc.particleEngine.add(MoteParticle.seek(level, from, into, tint, 0.09F + 0.06F * shown + random.nextFloat() * 0.04F,
					26 + random.nextInt(14), 0.45F + random.nextFloat() * 0.5F));
			}
		}

		/** The finish: the gathered light bursts, and the rune's emblem shines over the hand. */
		private void finish() {
			finishing = 0;
			progress = 1;
			Minecraft mc = Minecraft.getInstance();
			Vec3 at = hand(player, 1);
			boolean own = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
			float speck = own ? 0.07F : 0.12F;
			int n = Math.max(6, Math.round(26 * busy()));
			for (int i = 0; i < n; i++) {
				float yy = 1 - (i + 0.5F) * 2F / n;
				float s = Mth.sqrt(Math.max(0, 1 - yy * yy));
				float a = i * 2.39996F;
				float speed = 0.11F + random.nextFloat() * 0.06F;
				Vec3 push = new Vec3(Mth.cos(a) * s * speed, yy * speed * 0.7 + 0.03, Mth.sin(a) * s * speed);
				mc.particleEngine.add(MoteParticle.glow(level, at, i % 3 == 0 ? 0xFFFFFF : color, speck, 22 + random.nextInt(10), new Vec3(0, 0.008, 0),
					push, 0.004F));
			}
			mc.particleEngine.add(new Emblem(level, player, runes.getFirst().mark(), color));
		}

		@Override
		protected float opening(float partial) {
			return finishing >= 0 ? 1 : Mth.clamp((age + partial) / 28F, 0, 1);
		}

		@Override
		protected float fade(float partial) {
			float in = Mth.clamp((age + partial) / 4F, 0, 1);
			float bright = 0.55F + 0.45F * Mth.lerp(partial, oShown, shown);
			// A flare as each stage is reached.
			float flare = 0;
			for (int s = 1; s <= 3; s++) {
				if (reached[s] >= 0) {
					float since = age + partial - reached[s];
					flare = Math.max(flare, Mth.clamp(1 - since / 10F, 0, 1));
				}
			}
			float out = 1;
			if (leaving >= 0) {
				out = Mth.clamp(1 - (leaving + partial) / FADE, 0, 1);
			} else if (finishing >= 0) {
				float f = finishing + partial;
				flare = Math.max(flare, Mth.clamp(1 - f / 8F, 0, 1));
				out = Mth.clamp(1 - (f - 8) / (FINISH - 8), 0, 1);
			}
			return in * out * Math.min(1.35F, bright + 0.35F * flare);
		}

		@Override
		protected float size(float partial) {
			float grow = 1 + 0.12F * Mth.lerp(partial, oShown, shown);
			if (finishing >= 0) {
				float f = Mth.clamp((finishing + partial) / 10F, 0, 1);
				grow *= 1 + 0.18F * (1 - (1 - f) * (1 - f));
			}
			return RADIUS * grow;
		}

		@Override
		protected Vec3 centre(float partial) {
			return new Vec3(Mth.lerp(partial, xo, x), Mth.lerp(partial, yo, y), Mth.lerp(partial, zo, z));
		}

		/** How far into view the stage's own parts have come: 0 until it's reached, then up to 1 over half a second. */
		private float stageIn(int s, float partial) {
			return reached[s] < 0 ? 0 : Mth.clamp((age + partial - reached[s]) / 10F, 0, 1);
		}

		@Override
		protected void extras(float r, float a, float fine, float partial) {
			float spin = Mth.lerp(partial, oTurn, turn);
			Rune own = runes.getFirst();
			// Stage 1: the rune's own band round the rim, turning against the circle, and a fine ring outside it.
			float one = stageIn(1, partial);
			if (one > 0) {
				tiles(new TextureAtlasSprite[] {own.band()}, 0, 0, r * 1.13F, r * 0.075F, -spin * 1.5F, argb(a * one * 0.85F, color), 0.003F, false);
				ring(0, 0, r * 1.19F, fine, argb(a * one, lighter(color, 0.3F)), 0.002F);
			}
			// Stage 2: a ring of rays, and arcs between them.
			float two = stageIn(2, partial);
			if (two > 0) {
				int rays = 12;
				for (int k = 0; k < rays; k++) {
					float ang = spin + Mth.TWO_PI * k / rays;
					float out = r * (1.24F + 0.12F * two * (k % 3 == 0 ? 1.4F : 1F));
					line(Mth.cos(ang) * r * 1.24F, Mth.sin(ang) * r * 1.24F, Mth.cos(ang) * out, Mth.sin(ang) * out, fine * (k % 3 == 0 ? 1.6F : 1F),
						argb(a * two, lighter(color, 0.2F)), 0.002F);
					float gap = Mth.TWO_PI / rays * 0.18F;
					arc(0, 0, r * 1.3F, ang + gap, ang + Mth.TWO_PI / rays - gap, fine, argb(a * two * 0.7F, color), 0.002F);
				}
			}
			// Stage 3: an outer ring, heavier, with lights at its quarters.
			float three = stageIn(3, partial);
			if (three > 0) {
				ring(0, 0, r * 1.48F, fine * 1.8F, argb(a * three, color), 0.002F);
				ring(0, 0, r * 1.53F, fine, argb(a * three * 0.6F, lighter(color, 0.4F)), 0.002F);
				float beat = 0.7F + 0.3F * Mth.sin((age + partial) * 0.4F);
				for (int k = 0; k < 4; k++) {
					float ang = -spin * 0.7F + Mth.HALF_PI * k;
					piece(glow, Mth.cos(ang) * r * 1.48F, Mth.sin(ang) * r * 1.48F, 0, r * 0.1F, argb(a * three * beat, lighter(color, 0.5F)), 0.004F);
				}
			}
			// The finish: rings racing out from the rim.
			if (finishing >= 0) {
				float f = Mth.clamp((finishing + partial) / 16F, 0, 1);
				float ease = 1 - (1 - f) * (1 - f);
				for (int k = 0; k < 2; k++) {
					float rad = r * (1.1F + (0.9F + 0.5F * k) * ease);
					ring(0, 0, rad, fine * (2.2F - k), argb(a * (1 - f) * (k == 0 ? 1 : 0.7F), k == 0 ? lighter(color, 0.5F) : color), 0.002F);
				}
			}
		}

		@Override
		public void extract(QuadParticleRenderState state, Camera camera, float partial) {
			super.extract(state, camera, partial);
			float a = fade(partial);
			if (a <= 0.01F) {
				return;
			}
			// The light gathering in the hand.
			float gathered = Mth.lerp(partial, oShown, shown);
			if (finishing >= 0) {
				float f = (finishing + partial) / 8F;
				gathered = f < 1 ? 1 + f * 0.8F : Math.max(0, 1.8F - (f - 1) * 0.6F);
			}
			Vec3 at = hand(player, partial);
			Vec3 cam = camera.position();
			float beat = 0.85F + 0.15F * Mth.sin((age + partial) * 0.5F);
			Minecraft mc = Minecraft.getInstance();
			// In your own first-person view it's right in front of you: smaller, so it never fills the screen.
			boolean own = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
			float size = (0.06F + 0.12F * gathered) * beat * (own ? 0.45F : 1F);
			billboard(state, soft, (float) (at.x - cam.x), (float) (at.y - cam.y), (float) (at.z - cam.z), size * 2.2F,
				argb(Math.min(1, a) * 0.55F, color));
			billboard(state, glow, (float) (at.x - cam.x), (float) (at.y - cam.y), (float) (at.z - cam.z), size,
				argb(Math.min(1, a), lighter(color, 0.6F)));
		}

		@Override
		public double reach() {
			return RADIUS * 2.2 + 1.5;
		}
	}

	/** A soft glow at ({@code px}, {@code py}, {@code pz}) (camera-relative), facing the camera. */
	static void billboard(QuadParticleRenderState state, TextureAtlasSprite sprite, float px, float py, float pz, float half, int argb) {
		if ((argb >>> 24) < 2 || half <= 0) {
			return;
		}
		Vector3f z = new Vector3f(-px, -py, -pz);
		if (z.lengthSquared() < 1.0E-6F) {
			return;
		}
		z.normalize();
		Vector3f up = Math.abs(z.y) > 0.95F ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
		Vector3f xAxis = new Vector3f(up).cross(z).normalize();
		Vector3f yAxis = new Vector3f(z).cross(xAxis).normalize();
		Quaternionf q = new Quaternionf().setFromNormalized(new Matrix3f(xAxis, yAxis, z));
		state.add(GlowLayers.GLOW, px, py, pz, q.x, q.y, q.z, q.w, half, sprite.getU0(), sprite.getU1(), sprite.getV0(), sprite.getV1(), argb,
			LightCoordsUtil.FULL_BRIGHT);
	}

	/** The rune's emblem shining over the hand at the finish, then drifting up and fading. */
	static final class Emblem extends SingleQuadParticle implements SigilGroup.Extent {
		private static final int LIFE = 46;
		private final LivingEntity player;
		private final TextureAtlasSprite mark;
		private final TextureAtlasSprite soft;
		private final TextureAtlasSprite glow;
		private final int color;

		Emblem(ClientLevel level, LivingEntity player, TextureAtlasSprite mark, int color) {
			super(level, player.getX(), player.getY() + 1, player.getZ(), mark);
			this.player = player;
			this.mark = mark;
			this.soft = particleSprite("sigil_soft");
			this.glow = particleSprite("sigil_glow");
			this.color = color & 0xFFFFFF;
			this.lifetime = LIFE;
			this.gravity = 0;
			this.hasPhysics = false;
		}

		private static TextureAtlasSprite particleSprite(String path) {
			return SpellCircleParticle.particleSprite(path);
		}

		@Override
		public void tick() {
			xo = x;
			yo = y;
			zo = z;
			Vec3 at = hand(player, 1).add(0, 0.3 + age * 0.012, 0);
			x = at.x;
			y = at.y;
			z = at.z;
			if (age++ >= lifetime || player.isRemoved()) {
				remove();
			}
		}

		@Override
		public void extract(QuadParticleRenderState state, Camera camera, float partial) {
			float t = age + partial;
			float grow = Mth.clamp(t / 6F, 0, 1);
			float a = Math.min(Mth.clamp(t / 2F, 0, 1), Mth.clamp((lifetime - t) / (lifetime * 0.5F), 0, 1));
			if (a <= 0.01F) {
				return;
			}
			Minecraft mc = Minecraft.getInstance();
			boolean own = player == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson();
			float half = (own ? 0.16F : 0.24F) * (0.4F + 0.6F * (1 - (1 - grow) * (1 - grow)));
			Vec3 cam = camera.position();
			float px = (float) (Mth.lerp(partial, xo, x) - cam.x);
			float py = (float) (Mth.lerp(partial, yo, y) - cam.y);
			float pz = (float) (Mth.lerp(partial, zo, z) - cam.z);
			// A burst of light behind it, then the emblem itself, white-hot at first and cooling to the rune's colour.
			float heat = Mth.clamp(1 - t / 14F, 0, 1);
			billboard(state, glow, px, py, pz, half * (1.8F + 1.4F * heat), argb(a * (0.5F + 0.5F * heat), color));
			billboard(state, soft, px, py, pz, half * 2.4F, argb(a * 0.45F, color));
			billboard(state, mark, px, py, pz, half, argb(a, lighter(color, 0.35F + 0.6F * heat)));
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
		public int getLightCoords(float partial) {
			return LightCoordsUtil.FULL_BRIGHT;
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
			return 1.2;
		}
	}

	static int argb(float alpha, int rgb) {
		return (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
	}

	static int lighter(int rgb, float t) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return ((r + Math.round((255 - r) * t)) << 16) | ((g + Math.round((255 - g) * t)) << 8) | (b + Math.round((255 - b) * t));
	}
}
