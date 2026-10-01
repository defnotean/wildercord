package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.MasteryChoices;
import dev.wildercord.client.fx.MagicQuality;
import dev.wildercord.content.Inscription;
import dev.wildercord.spell.MasteryRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

/**
 * Spell mastery on the client: the toast when one of your spells reaches a rank (with its sigil for an icon), the
 * brief title by the caster when a named Adept spell is cast nearby (your own included, unless spell titles are
 * switched off in the magic settings), and an inscribed scroll's sigil under its tooltip.
 */
public final class MasteryClient {
	private MasteryClient() {}

	/** Set when a spell reaches a rank with a trait to choose: the Cord screen opens its choice the next time it opens. */
	private static boolean prompt;

	/** Whether a trait is waiting to be chosen since the last time the Cord screen offered it (cleared by reading it). */
	static boolean takePrompt() {
		boolean was = prompt;
		prompt = false;
		return was;
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(MasteryChoices.Rise.TYPE, (payload, context) -> {
			context.client().gui.toastManager().addToast(new RankToast(payload));
			if (payload.choice()) {
				prompt = true;
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(MasteryChoices.Title.TYPE, (payload, context) -> Titles.receive(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			prompt = false;
			Titles.SHOWN.clear();
		});
		ClientTooltipComponentCallback.EVENT.register(data -> data instanceof Inscription inscription ? new SigilTooltip(inscription) : null);
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Wildercord.id("mastery_titles"), Titles::extract);
	}

	// ------------------------------------------------------------------ the toast

	/** "Your spell grows: Adept", with the spell's name in its colour and its sigil where an item would sit. */
	static final class RankToast implements Toast {
		private static final Identifier BACKGROUND = Wildercord.id("toast/grimoire");
		private static final long SHOW_MS = 6000;
		private final MasteryChoices.Rise rise;
		private Visibility visibility = Visibility.SHOW;

		RankToast(MasteryChoices.Rise rise) {
			this.rise = rise;
		}

		/** The token a rank's toast carries, so it can be found among the toasts (the game tests look for it). */
		static String token(String name, int rank) {
			return "mastery:" + name + ":" + rank;
		}

		@Override
		public Visibility getWantedVisibility() {
			return visibility;
		}

		@Override
		public Object getToken() {
			return token(rise.name(), rise.rank());
		}

		@Override
		public void update(ToastManager manager, long fullyVisibleForMs) {
			visibility = fullyVisibleForMs >= SHOW_MS * manager.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
		}

		@Override
		public SoundEvent getSoundEvent() {
			return SoundEvents.AMETHYST_BLOCK_RESONATE;
		}

		@Override
		public void extractRenderState(GuiGraphicsExtractor g, Font font, long fullyVisibleForMs) {
			g.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, width(), height());
			int color = 0xFF000000 | rise.color();
			GuiSpellCircle.ring(g, 16, 16, 9, 1, color);
			GuiSpellCircle.sigil(g, 16, 16, 7, rise.seed(), 0xFF000000 | MasteryPanel.rankColor(rise.rank()), 1.1F);
			Component title = Component.translatable(rise.choice() ? "toast.wildercord.mastery.choose" : "toast.wildercord.mastery",
				MasteryPanel.rankName(rise.rank()));
			String shownTitle = font.plainSubstrByWidth(title.getString(), width() - 36);
			g.text(font, shownTitle, 30, 7, 0xFFB8A8FF, false);
			String shown = font.plainSubstrByWidth(rise.name(), width() - 36);
			g.text(font, shown, 30, 18, color, false);
		}
	}

	// ------------------------------------------------------------------ spoken names

	/**
	 * Mastered spells' names, spoken when cast: a brief title floating above the caster that drifts up and fades, or,
	 * when the caster isn't in view (or is you, in first person), a line low on the screen like a subtitle.
	 */
	public static final class Titles {
		private Titles() {}

		/** How long a title shows, in ticks. */
		static final int TICKS = 50;

		record Shown(int caster, String name, int rank, int color, long start) {}

		static final List<Shown> SHOWN = new ArrayList<>();

		static void receive(MasteryChoices.Title title) {
			Minecraft mc = Minecraft.getInstance();
			if (!MagicQuality.spellTitles || mc.level == null) {
				return;
			}
			SHOWN.removeIf(s -> s.caster() == title.caster());
			SHOWN.add(new Shown(title.caster(), title.name(), title.rank(), title.color(), mc.level.getGameTime()));
			while (SHOWN.size() > 4) {
				SHOWN.removeFirst();
			}
		}

		/** The names showing right now (for the game tests). */
		public static List<String> showing() {
			return SHOWN.stream().map(Shown::name).toList();
		}

		static void extract(GuiGraphicsExtractor g, DeltaTracker delta) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || SHOWN.isEmpty()) {
				return;
			}
			long now = mc.level.getGameTime();
			float partial = delta.getGameTimeDeltaPartialTick(false);
			SHOWN.removeIf(s -> now - s.start() > TICKS || now < s.start());
			if (!MagicQuality.spellTitles) {
				SHOWN.clear();
				return;
			}
			Font font = mc.font;
			int width = g.guiWidth();
			int height = g.guiHeight();
			int line = 0;
			for (Shown s : SHOWN) {
				float t = (now - s.start() + partial) / TICKS;
				float alpha = Math.min(Mth.clamp(t * 10, 0, 1), Mth.clamp((1 - t) * 3.5F, 0, 1));
				if (alpha <= 0.02F) {
					continue;
				}
				String mark = s.rank() >= MasteryRules.MYTHIC ? "❖" : s.rank() >= MasteryRules.MASTER ? "✦" : "✧";
				Component text = Component.literal(mark + " " + s.name() + " " + mark);
				int argb = (Mth.clamp((int) (alpha * 255), 0, 255) << 24) | (s.color() & 0xFFFFFF);
				Entity caster = mc.level.getEntity(s.caster());
				float[] at = caster == null ? null : above(mc, caster, partial, width, height);
				if (at != null) {
					// By the caster: a little larger, drifting up as it fades.
					g.pose().pushMatrix();
					g.pose().translate(at[0], at[1] - 8 * t);
					g.pose().scale(1.15F, 1.15F);
					g.centeredText(font, text, 0, -4, argb);
					g.pose().popMatrix();
				} else {
					g.centeredText(font, text, width / 2, height - 84 - line * 11, argb);
					line++;
				}
			}
		}

		/** Where above {@code caster}'s head is on the screen, or null when they're out of view (or the camera itself). */
		private static float[] above(Minecraft mc, Entity caster, float partial, int width, int height) {
			var camera = mc.gameRenderer.mainCamera();
			if (caster == mc.getCameraEntity() && !camera.isDetached()) {
				return null;
			}
			Vec3 head = caster.getEyePosition(partial).add(0, 0.8, 0);
			Vec3 offset = head.subtract(camera.position());
			Vector3fc forward = camera.forwardVector();
			if (offset.x * forward.x() + offset.y * forward.y() + offset.z * forward.z() < 0.5 || offset.lengthSqr() > 48 * 48) {
				return null;
			}
			Vec3 ndc = mc.gameRenderer.projectPointToScreen(head);
			if (!Double.isFinite(ndc.x) || !Double.isFinite(ndc.y) || Math.abs(ndc.x) > 0.95 || Math.abs(ndc.y) > 0.95) {
				return null;
			}
			return new float[] {(float) ((ndc.x + 1) / 2 * width), (float) ((1 - ndc.y) / 2 * height)};
		}
	}

	// ------------------------------------------------------------------ an inscribed scroll's sigil

	/** Under an inscribed scroll's tooltip: its spell's sigil in a ring of its rank's colour. */
	record SigilTooltip(Inscription inscription) implements ClientTooltipComponent {
		@Override
		public int getHeight(Font font) {
			return 30;
		}

		@Override
		public int getWidth(Font font) {
			return 28;
		}

		@Override
		public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor g) {
			int color = 0xFF000000 | MasteryPanel.rankColor(inscription.rank());
			GuiSpellCircle.ring(g, x + 13, y + 14, 12, 1, color);
			GuiSpellCircle.sigil(g, x + 13, y + 14, 9, inscription.seed(), 0xFFE8D8B0, 1.2F);
		}
	}
}
