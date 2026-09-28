package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.RuneItem;
import dev.wildercord.net.WildercordNetworking;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.SpellCompiler;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.List;
import java.util.Locale;

/**
 * The spell wheel: hold the switch-spell key and your spells fan out in a ring. Point the mouse at
 * one and let go to select it; let go without pointing and the wheel stays open until you click a
 * spell, press the key again, press its number, or press Esc. Each shows its name, its runes and
 * whether it's cooling down. A tap of the key still just moves to the next spell.
 *
 * <p>Opening a screen releases every key mapping, so the wheel never asks the mapping whether the
 * key is down: it waits for the key's own release event instead.
 */
public class SpellWheelScreen extends Screen {
	private static final Identifier RING = Wildercord.id("wheel/ring");
	private static final Identifier NODE = Wildercord.id("wheel/node");
	private static final Identifier NODE_HOVER = Wildercord.id("wheel/node_hover");
	private static final int RADIUS = 60;
	/** Labels sit just outside the 180-wide disc. */
	private static final int LABEL_RADIUS = 97;
	private static final int GOLD = 0xFFE8C46A;
	private static final int TEXT = 0xFFE8E4F4;
	private static final int DIM = 0xFF8A84A0;

	private final KeyMapping key;
	private int hovered = -1;
	private int opened;
	/** Whether the mouse has pointed at a spell since the wheel opened. */
	private boolean pointed;
	/** Where the mouse was when the wheel first drew, and whether it has moved since: only a move points. */
	private int startX = Integer.MIN_VALUE;
	private int startY;
	private boolean moved;
	/** Let go without pointing: the wheel stays open until a choice or Esc. */
	private boolean toggled;

	public SpellWheelScreen(KeyMapping key) {
		super(Component.translatable("screen.wildercord.wheel"));
		this.key = key;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean isInGameUi() {
		return true;
	}

	private CordTier tier() {
		return minecraft.player == null ? null : Spellbooks.tier(minecraft.player);
	}

	@Override
	public void tick() {
		opened++;
	}

	/** Whether it's waiting for a click or a key rather than for the key to be let go. */
	public boolean toggled() {
		return toggled;
	}

	/** The key was let go: choose what's pointed at, or stay open if nothing was. */
	private void released() {
		if (pointed) {
			choose();
		} else {
			toggled = true;
		}
	}

	private void choose() {
		CordTier tier = tier();
		if (tier != null && hovered >= 0 && hovered < tier.spells) {
			ClientPlayNetworking.send(new WildercordNetworking.SelectSpell(hovered));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F));
		}
		onClose();
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			onClose();
		} else {
			choose();
		}
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (!toggled && key.matchesMouse(event)) {
			released();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyReleased(KeyEvent event) {
		if (!toggled && key.matches(event)) {
			released();
			return true;
		}
		return super.keyReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		CordTier tier = tier();
		int number = event.key() - InputConstants.KEY_1;
		if (tier != null && number >= 0 && number < tier.spells) {
			hovered = number;
			pointed = true;
			choose();
			return true;
		}
		if (toggled && (key.matches(event) || event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER)) {
			choose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		super.extractRenderState(g, mouseX, mouseY, partial);
		CordTier tier = tier();
		if (tier == null) {
			return;
		}
		Spellbook book = Spellbooks.get(minecraft.player);
		int n = tier.spells;
		int cx = width / 2;
		int cy = height / 2;
		float open = Math.min(1F, (opened + partial) / 3F);
		int radius = Math.round(RADIUS * (0.6F + 0.4F * open));
		g.blitSprite(RenderPipelines.GUI_TEXTURED, RING, cx - 90, cy - 90, 180, 180);
		double dx = mouseX - cx;
		double dy = mouseY - cy;
		if (startX == Integer.MIN_VALUE) {
			startX = mouseX;
			startY = mouseY;
		}
		moved |= Math.abs(mouseX - startX) + Math.abs(mouseY - startY) > 4;
		if (moved && dx * dx + dy * dy > 14 * 14) {
			double angle = Math.atan2(dy, dx) + Math.PI / 2;
			double step = Math.PI * 2 / n;
			hovered = (int) Math.floorMod(Math.round(angle / step), n);
			pointed = true;
		} else if (!pointed || toggled) {
			hovered = book.selected() < n ? book.selected() : -1;
		}
		long now = minecraft.player.level().getGameTime();
		for (int i = 0; i < n; i++) {
			double a = -Math.PI / 2 + Math.PI * 2 * i / n;
			int x = cx + (int) Math.round(Math.cos(a) * radius);
			int y = cy + (int) Math.round(Math.sin(a) * radius);
			boolean isHovered = i == hovered;
			boolean isSelected = i == book.selected();
			List<RuneDef> runes = SpellCaster.activeRunes(book, i, tier);
			g.blitSprite(RenderPipelines.GUI_TEXTURED, isHovered ? NODE_HOVER : NODE, x - 16, y - 16, 32, 32);
			// The node's colour: the spell's first element.
			int color = 0xFF40C8BE;
			for (RuneDef rune : runes) {
				if (rune.family() == RuneFamily.EFFECT) {
					color = 0xFF000000 | RuneColors.of(rune);
					break;
				}
			}
			g.fill(x - 9, y + 10, x + 9, y + 11, color);
			long remaining = Spellbooks.readyAt(minecraft.player, i) - now;
			if (!runes.isEmpty() && remaining > 0) {
				int total = Math.max(1, Heart.cooldownTicks(minecraft.player, SpellCompiler.compile(runes)));
				int shade = (int) Math.ceil(24 * Math.min(1.0, remaining / (double) total));
				g.fill(x - 12, y - 12 + (24 - shade), x + 12, y + 12, 0x90000000);
			}
			String number = Integer.toString(i + 1);
			g.text(font, number, x - font.width(number) / 2, y - 4, isHovered ? GOLD : isSelected ? TEXT : DIM, true);
			// Name and runes, just outside the disc, anchored on the side facing away from the centre.
			String name = runes.isEmpty() ? Component.translatable("screen.wildercord.wheel.empty").getString() : SpellCaster.nameOf(book, i, runes);
			String shown = font.plainSubstrByWidth(name, 150);
			int icons = Math.min(runes.size(), 8);
			int lx = cx + (int) Math.round(Math.cos(a) * LABEL_RADIUS);
			int ly = cy + (int) Math.round(Math.sin(a) * LABEL_RADIUS);
			double cos = Math.cos(a);
			double sin = Math.sin(a);
			int blockW = Math.max(font.width(shown), icons * 9);
			int nx = cos > 0.3 ? lx : cos < -0.3 ? lx - blockW : lx - font.width(shown) / 2;
			int ny = sin > 0.3 ? ly : sin < -0.3 ? ly - 20 : ly - 10;
			g.text(font, shown, cos < -0.3 ? lx - font.width(shown) : nx, ny, isHovered ? GOLD : TEXT, true);
			int ix = cos > 0.3 ? lx : cos < -0.3 ? lx - icons * 9 : lx - icons * 9 / 2;
			g.pose().pushMatrix();
			g.pose().translate(ix, ny + 10);
			g.pose().scale(0.5F, 0.5F);
			for (int k = 0; k < icons; k++) {
				g.item(RuneItem.stack(runes.get(k)), k * 18, 0);
			}
			g.pose().popMatrix();
		}
		// The centre: what you're pointing at.
		if (hovered >= 0 && hovered < n) {
			List<RuneDef> runes = SpellCaster.activeRunes(book, hovered, tier);
			if (!runes.isEmpty()) {
				SpellCompiler.Compiled compiled = SpellCompiler.compile(runes);
				String cost = compiled.paysInHealth()
					? Heart.healthCost(minecraft.player, compiled) + "❤"
					: Heart.manaCost(minecraft.player, compiled) + " mana";
				String cooldown = String.format(Locale.ROOT, "%.1fs", Heart.cooldownTicks(minecraft.player, compiled) / 20.0);
				g.centeredText(font, Component.literal(cost), cx, cy - 8, 0xFFB8A8FF);
				g.centeredText(font, Component.literal(cooldown), cx, cy + 2, DIM);
			}
		}
	}
}
