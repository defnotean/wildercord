package dev.wildercord.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.wildercord.content.RelayLesson;
import dev.wildercord.player.MasterStudies;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** The same three pages can be read at the lectern or recovered from the Grimoire, without an item slot. */
public final class RelayLessonScreen extends Screen implements CordEditorParent {
	private static long closedNonce;
	private static long nextRequest;
	private final Screen parent;
 @Override public Screen cordEditorParent(){return parent;}
	private final long request;
	private boolean requesting;
	private boolean requestSent;
	private final long nonce;
	private final boolean studying;
	private final boolean learnedNow;
	private int page;
	private int scroll;
	private int left, top, panelWidth, panelHeight, bodyTop, bodyBottom;
	private Button next;
	private boolean waiting;

	public RelayLessonScreen(Screen parent) {
		this(parent, new RelayLesson.Show(0, 0, false, false, ++nextRequest));
		requesting = true;
	}

	private RelayLessonScreen(Screen parent, RelayLesson.Show shown) {
		super(Component.literal(RelayLesson.TITLE));
		this.parent = parent;
		this.request = shown.request();
		this.nonce = shown.nonce();
		this.page = Math.clamp(shown.page(), 0, 2);
		this.studying = shown.studying();
		this.learnedNow = shown.learnedNow();
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(RelayLesson.Show.TYPE, (shown, context) -> {
			Screen previous = context.client().gui.screen();
			if (shown.request() != 0 && (!(previous instanceof RelayLessonScreen lesson)
				|| !lesson.requesting || lesson.request != shown.request())) return;
			if (shown.nonce() != 0 && shown.nonce() == closedNonce) return;
			if (shown.page() < 0) {
				if (previous instanceof RelayLessonScreen lesson && lesson.nonce == shown.nonce()) lesson.onClose();
				return;
			}
			if (shown.page() > 0 && (!(previous instanceof RelayLessonScreen lesson) || lesson.nonce != shown.nonce())) return;
			Screen parent = previous instanceof RelayLessonScreen lesson ? lesson.parent : previous;
			context.client().gui.setScreen(new RelayLessonScreen(parent, shown));
		});
	}

	@Override public boolean isPauseScreen() { return false; }

	/** Read-only presentation state for accessibility and native protocol checks. */
	public int pageNumber() { return page; }
	public boolean isStudying() { return studying; }
	public long sessionNonce() { return nonce; }
	public double[] nextPagePoint() { return new double[] {next.getX() + next.getWidth() / 2.0, next.getY() + 10}; }

	@Override
	protected void init() {
		if (requesting && !requestSent) {
			requestSent = true;
			ClientPlayNetworking.send(new RelayLesson.Retrieve(request, false));
		}
		panelWidth = Math.min(390, width - 20);
		panelHeight = Math.min(320, height - 16);
		left = (width - panelWidth) / 2;
		top = (height - panelHeight) / 2;
		bodyTop = top + 60;
		bodyBottom = top + panelHeight - 49;
		int buttonY = top + panelHeight - 29;
		int buttonWidth = (panelWidth - 40) / 3;
		Button previous = addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> {
			page = Math.max(0, page - 1); scroll = 0; rebuildWidgets();
		}).bounds(left + 10, buttonY, buttonWidth, 20).build());
		previous.active = !requesting && !studying && page > 0;
		next = addRenderableWidget(Button.builder(Component.translatable(studying && page == 2
			? "screen.wildercord.relay_lesson.learn" : "screen.wildercord.relay_lesson.next"), button -> advance())
			.bounds(left + 20 + buttonWidth, buttonY, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
			.bounds(left + 30 + buttonWidth * 2, buttonY, buttonWidth, 20).build());
	}

	private void advance() {
		if (waiting || requesting) return;
		if (studying) {
			waiting = true;
			next.active = false;
			ClientPlayNetworking.send(new RelayLesson.Turn(nonce, page + 1));
		} else if (page < 2) {
			page++; scroll = 0; rebuildWidgets();
		}
	}

	private List<FormattedCharSequence> lines() {
		List<FormattedCharSequence> lines = new ArrayList<>();
		if (requesting) {
			append(lines, Component.translatable("screen.wildercord.relay_lesson.opening"));
			return lines;
		}
		append(lines, RelayLesson.page(page));
		if (page == 2) {
			append(lines, Component.empty());
			append(lines, Component.translatable("screen.wildercord.relay_lesson.controls",
				WildercordKeys.openKey(), WildercordKeys.nextKey(), WildercordKeys.castKey()));
			append(lines, Component.empty());
			append(lines, Component.translatable("screen.wildercord.relay_lesson.limits"));
			append(lines, Component.empty());
			append(lines, Component.translatable("screen.wildercord.relay_lesson.practice",
				WildercordKeys.castKey(), WildercordKeys.castKey()));
			if (minecraft.player != null && MasterStudies.practicedRelay(minecraft.player)) {
				append(lines, Component.translatable("screen.wildercord.relay_lesson.practiced"));
			}
		}
		return lines;
	}

	private void append(List<FormattedCharSequence> lines, Component text) {
		if (text.getString().isEmpty()) lines.add(FormattedCharSequence.EMPTY);
		else lines.addAll(font.split(text, Math.max(80, panelWidth - 32)));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF21B2030);
		graphics.fill(left + 1, top + 1, left + panelWidth - 1, top + 2, 0xFFD4B76D);
		graphics.centeredText(font, title, width / 2, top + 10, 0xFFE8C46A);
		graphics.centeredText(font, Component.literal(RelayLesson.AUTHOR), width / 2, top + 24, 0xFFB4BCBB);
		Component status = Component.translatable(learnedNow ? "screen.wildercord.relay_lesson.learned"
			: studying ? "screen.wildercord.relay_lesson.reading" : "screen.wildercord.relay_lesson.saved", page + 1, 3);
		graphics.centeredText(font, status, width / 2, top + 40, 0xFF7FDAD4);
		List<FormattedCharSequence> lines = lines();
		int visible = Math.max(1, (bodyBottom - bodyTop) / 11);
		int maximum = Math.max(0, lines.size() - visible);
		scroll = Math.clamp(scroll, 0, maximum);
		graphics.enableScissor(left + 12, bodyTop, left + panelWidth - 12, bodyBottom);
		for (int index = scroll; index < Math.min(lines.size(), scroll + visible); index++) {
			graphics.text(font, lines.get(index), left + 16, bodyTop + (index - scroll) * 11, 0xFFE8E5D9, false);
		}
		graphics.disableScissor();
		if (maximum > 0) graphics.centeredText(font, Component.translatable("screen.wildercord.relay_lesson.scroll", scroll + 1, maximum + 1),
			width / 2, bodyBottom + 4, 0xFFA7B8B6);
		next.active = !requesting && !waiting && (studying || page < 2) && (!studying || scroll >= maximum);
		super.extractRenderState(graphics, mouseX, mouseY, partial);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		scroll = Math.max(0, scroll - (int) Math.signum(scrollY) * 3);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_HOME || event.key() == InputConstants.KEY_END) {
			scroll = event.key() == InputConstants.KEY_HOME ? 0 : Integer.MAX_VALUE;
			return true;
		}
		if (event.key() == InputConstants.KEY_PAGEDOWN || event.key() == InputConstants.KEY_DOWN) {
			scroll += event.key() == InputConstants.KEY_PAGEDOWN ? 8 : 1;
			return true;
		}
		if (event.key() == InputConstants.KEY_PAGEUP || event.key() == InputConstants.KEY_UP) {
			scroll = Math.max(0, scroll - (event.key() == InputConstants.KEY_PAGEUP ? 8 : 1));
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		if (requesting) ClientPlayNetworking.send(new RelayLesson.Retrieve(request, true));
		if (nonce != 0) closedNonce = nonce;
		if (studying) ClientPlayNetworking.send(new RelayLesson.Turn(nonce, -1));
		minecraft.gui.setScreen(parent);
	}
}
