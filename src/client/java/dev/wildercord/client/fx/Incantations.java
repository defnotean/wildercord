package dev.wildercord.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildercord.cast.Charging;
import dev.wildercord.client.CastingOptions;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Incantation;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Incantations: while a spell charges, its runes' syllables ({@link Incantation}) rise one by one from
 * behind the caster as each rune's roundel opens on the circle, and gather into a line of glowing script
 * over their shoulders: the spell's incantation, readable by anyone close enough (it fades with distance).
 * In a duel, a sharp-eyed opponent can read what's coming. Every client works it out from the synced
 * charge, as it does the circle; a tap is cast without a word, so only charging speaks.
 *
 * <p>The words are vanilla text (lit like glowing sign text), so they draw correctly under shader packs.
 * Behind the caster, they never cover a first-person view. When the spell is let go they thin away in a
 * few ticks, leaving the release to itself. A client option hides them: all, only others', or only your
 * own; hidden, they're silent too.</p>
 */
public final class Incantations {
	private Incantations() {}

	/** Text size, blocks per pixel of the font: a syllable is about a sixth of a block tall. */
	private static final float SCALE = 0.019F;
	/** Ticks a syllable takes to rise into its place in the line. */
	private static final int RISE = 14;
	/** Ticks the incantation takes to thin away once the charge ends. */
	private static final int FADE = 6;
	/** Fully legible within this many blocks, gone at the second. */
	private static final double CLEAR = 6;
	private static final double GONE = 18;
	/** Syllables to a line; a longer spell's incantation goes on a second line under the first. */
	private static final int PER_LINE = 5;

	/** One spoken syllable: what it says, its rune's colour, and when it was spoken. */
	private record Word(String text, int color, long born) {}

	/** A caster's incantation for the charge that began at {@code start}. */
	private static final class Utterance {
		final long start;
		final List<String> runes;
		final List<Word> words = new ArrayList<>();
		int fading = -1;

		Utterance(long start, List<String> runes) {
			this.start = start;
			this.runes = runes;
		}
	}

	/** Each charging caster's incantation, by entity id (kept a few ticks past the charge, while it fades). */
	private static final Map<Integer, Utterance> SPOKEN = new HashMap<>();

	/** Every client tick: speaks each charging caster's next syllables as their roundels open, and lets finished ones fade. */
	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level == null) {
			SPOKEN.clear();
			return;
		}
		CastingOptions.ensureLoaded();
		long now = level.getGameTime();
		java.util.Set<Integer> charging = new java.util.HashSet<>();
		for (Player player : level.players()) {
			WildercordAttachments.Charge charge = player.getAttached(WildercordAttachments.CHARGE);
			if (charge == null || charge.runes().isEmpty() || !CastingOptions.incantations.shows(player == mc.player)) {
				continue;
			}
			charging.add(player.getId());
			Utterance u = SPOKEN.get(player.getId());
			if (u == null || u.start != charge.start() || u.fading >= 0) {
				u = new Utterance(charge.start(), List.copyOf(charge.runes()));
				SPOKEN.put(player.getId(), u);
			}
			double progress = Charging.progress(player, charge, now);
			int n = u.runes.size();
			// In step with the circle: a rune's syllable as its roundel opens (see SpellCircleParticle), with its note.
			while (u.words.size() < n && progress >= 0.35 + 0.55 * u.words.size() / n) {
				speak(level, player, u, now);
			}
		}
		for (Iterator<Map.Entry<Integer, Utterance>> it = SPOKEN.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<Integer, Utterance> e = it.next();
			Utterance u = e.getValue();
			if (!charging.contains(e.getKey()) && u.fading < 0) {
				u.fading = FADE;
			}
			if (u.fading >= 0 && u.fading-- <= 0) {
				it.remove();
			}
		}
	}

	private static void speak(ClientLevel level, Player caster, Utterance u, long now) {
		String id = u.runes.get(u.words.size());
		RuneDef rune = Runes.get(id).orElse(null);
		int color = rune == null ? 0xE8C46A : RuneColors.of(rune);
		u.words.add(new Word(Incantation.syllable(id), color, now));
		// A breath of a whisper on the rune's own degree of the scale, under its note.
		net.minecraft.sounds.SoundEvent whisper = dev.wildercord.content.WildercordSounds.kit("incant_whisper");
		if (whisper != null) {
			int degree = Math.floorMod(id.hashCode(), 5);
			level.playLocalSound(caster.getX(), caster.getEyeY(), caster.getZ(), whisper, net.minecraft.sounds.SoundSource.PLAYERS, 0.22F,
				dev.wildercord.cast.feel.Feels.step(degree), false);
		}
	}

	/** How many syllables of {@code entityId}'s incantation are showing now (0: none, or hidden by the options). For tests. */
	public static int spoken(int entityId) {
		Utterance u = SPOKEN.get(entityId);
		return u == null || u.fading >= 0 ? 0 : u.words.size();
	}

	/** {@code entityId}'s incantation as it reads now ("" for none). For tests. */
	public static String line(int entityId) {
		Utterance u = SPOKEN.get(entityId);
		if (u == null) {
			return "";
		}
		List<String> words = new ArrayList<>();
		for (Word w : u.words) {
			words.add(w.text());
		}
		return String.join(" ", words);
	}

	// ------------------------------------------------------------------ drawing

	/** Draws every incantation in the world, facing the camera, among the frame's other translucent things. */
	public static void submit(LevelRenderContext context) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || SPOKEN.isEmpty()) {
			return;
		}
		CameraRenderState camera = context.levelState().cameraRenderState;
		Vec3 cam = camera.pos;
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		double now = level.getGameTime() + partial;
		Font font = mc.font;
		PoseStack pose = context.poseStack();
		for (Map.Entry<Integer, Utterance> e : SPOKEN.entrySet()) {
			Entity entity = level.getEntity(e.getKey());
			Utterance u = e.getValue();
			if (!(entity instanceof Player caster) || u.words.isEmpty()) {
				continue;
			}
			Vec3 anchor = anchor(caster, partial);
			double distance = anchor.distanceTo(cam);
			float far = (float) Mth.clamp(1 - (distance - CLEAR) / (GONE - CLEAR), 0, 1);
			if (far <= 0.01F) {
				continue;
			}
			// Thinning away once the charge has ended, rising a little as it goes.
			float leaving = u.fading >= 0 ? Math.max(0, (u.fading - partial) / FADE) : 1;
			float lift = (1 - leaving) * 0.25F;
			int lines = (u.words.size() + PER_LINE - 1) / PER_LINE;
			for (int line = 0; line < lines; line++) {
				int from = line * PER_LINE;
				int to = Math.min(u.words.size(), from + PER_LINE);
				List<FormattedCharSequence> texts = new ArrayList<>();
				int[] widths = new int[to - from];
				int total = 0;
				int space = font.width(" ");
				for (int i = from; i < to; i++) {
					FormattedCharSequence text = Component.literal(u.words.get(i).text()).withStyle(ChatFormatting.ITALIC).getVisualOrderText();
					texts.add(text);
					widths[i - from] = font.width(text);
					total += widths[i - from] + (i > from ? space * 2 : 0);
				}
				float x = -total / 2F;
				for (int i = from; i < to; i++) {
					Word word = u.words.get(i);
					float age = (float) (now - word.born());
					float risen = Mth.clamp(age / RISE, 0, 1);
					float eased = 1 - (1 - risen) * (1 - risen);
					// It rises from shoulder height into its place; the whole line sways gently, as if on a breath.
					float y = -line * 11 + (1 - eased) * -28 + Mth.sin((float) now * 0.08F + i * 0.9F) * 0.8F;
					float alpha = far * leaving * Math.min(1, risen * 1.6F);
					if (alpha > 0.02F) {
						drawWord(context, pose, camera, anchor.add(0, lift, 0), texts.get(i - from), x, y, word.color(), alpha);
					}
					x += widths[i - from] + space * 2;
				}
			}
		}
	}

	/** Over the caster's shoulders and a little behind them, between their head and their name. */
	private static Vec3 anchor(Player caster, float partial) {
		Vec3 eye = caster.getEyePosition(partial);
		float yaw = caster.getViewYRot(partial) * Mth.DEG_TO_RAD;
		Vec3 back = new Vec3(Mth.sin(yaw), 0, -Mth.cos(yaw));
		return eye.add(back.scale(0.55)).add(0, 0.42, 0);
	}

	/**
	 * One syllable, facing the camera, {@code x} and {@code y} font pixels from the anchor (y up): its
	 * rune's colour lightened, with a darker outline, at full brightness, like glowing sign text.
	 */
	private static void drawWord(LevelRenderContext context, PoseStack pose, CameraRenderState camera, Vec3 at, FormattedCharSequence text, float x,
			float y, int color, float alpha) {
		pose.pushPose();
		pose.translate(at.x - camera.pos.x, at.y - camera.pos.y, at.z - camera.pos.z);
		pose.rotate(camera.orientation);
		pose.scale(SCALE, -SCALE, SCALE);
		int a = Mth.clamp((int) (alpha * 255), 4, 255);
		int fill = (a << 24) | lighter(color, 0.55F);
		int outline = (Mth.clamp((int) (alpha * 200), 4, 255) << 24) | darker(color, 0.35F);
		context.submitNodeCollector().submitText(pose, x, -y - 9, text, false, Font.DisplayMode.NORMAL, LightCoordsUtil.FULL_BRIGHT, fill, 0, outline);
		pose.popPose();
	}

	private static int lighter(int rgb, float t) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		return ((r + Math.round((255 - r) * t)) << 16) | ((g + Math.round((255 - g) * t)) << 8) | (b + Math.round((255 - b) * t));
	}

	private static int darker(int rgb, float t) {
		int r = Math.round(((rgb >> 16) & 0xFF) * t);
		int g = Math.round(((rgb >> 8) & 0xFF) * t);
		int b = Math.round((rgb & 0xFF) * t);
		return (r << 16) | (g << 8) | b;
	}
}
