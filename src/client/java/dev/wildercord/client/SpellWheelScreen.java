package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.RuneItem;
import dev.wildercord.gear.Gear;
import dev.wildercord.gear.SpellSlots;
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
 * whether it's cooling down. A tap of the key still just moves to the next spell. The wheel holds
 * every spell you can use: the Cord's, and the tome's fifth while the Tome of the Fifth Page is in the
 * off-hand.
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
	/** The pointed node (an index into {@link #slots()}), or -1. */
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

	/** The spell slots on the wheel, in order: the Cord's, then the tome's while it's held. */
	private List<Integer> slots() {
		CordTier tier = tier();
		return tier == null ? List.of() : SpellSlots.open(tier.spells, Gear.tome(minecraft.player));
	}

	@Override
	public void tick() {
		if (opened == 0) {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(dev.wildercord.content.WildercordSounds.WHEEL_OPEN, 1.0F, 1.0F));
		}
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
		List<Integer> slots = slots();
		if (hovered >= 0 && hovered < slots.size()) {
			ClientPlayNetworking.send(new WildercordNetworking.SelectSpell(slots.get(hovered)));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(dev.wildercord.content.WildercordSounds.WHEEL_SELECT, 1.0F, 1.0F));
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
		// A spell's number picks it: 5 is the tome's, whatever the Cord.
		int number = slots().indexOf(event.key() - InputConstants.KEY_1);
		if (number >= 0) {
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
		List<Integer> slots = slots();
		int n = slots.size();
		if (n == 0) {
			return;
		}
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
			int now = (int) Math.floorMod(Math.round(angle / step), n);
			if (now != hovered && pointed) {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(dev.wildercord.content.WildercordSounds.WHEEL_HOVER, 1.0F, 0.6F));
			}
			hovered = now;
			pointed = true;
		} else if (!pointed || toggled) {
			hovered = slots.indexOf(book.selected());
		}
		long now = minecraft.player.level().getGameTime();
		for (int i = 0; i < n; i++) {
			int slot = slots.get(i);
			double a = -Math.PI / 2 + Math.PI * 2 * i / n;
			int x = cx + (int) Math.round(Math.cos(a) * radius);
			int y = cy + (int) Math.round(Math.sin(a) * radius);
			boolean isHovered = i == hovered;
			boolean isSelected = slot == book.selected();
			List<RuneDef> runes = SpellCaster.activeRunes(book, slot, tier);
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
			long remaining = Spellbooks.readyAt(minecraft.player, slot) - now;
			if (!runes.isEmpty() && remaining > 0) {
				int total = Math.max(1, Heart.cooldownTicks(minecraft.player, SpellHud.read(runes), Heart.secretCooldown(minecraft.player, runes) * dev.wildercord.cast.Mastery.cooldownFactor(minecraft.player, runes)));
				int shade = (int) Math.ceil(24 * Math.min(1.0, remaining / (double) total));
				g.fill(x - 12, y - 12 + (24 - shade), x + 12, y + 12, 0x90000000);
			}
			String number = Integer.toString(slot + 1);
			g.text(font, number, x - font.width(number) / 2, y - 4, isHovered ? GOLD : isSelected ? TEXT : DIM, true);
			// Name and runes, just outside the disc, anchored on the side facing away from the centre.
			String name = runes.isEmpty() ? Component.translatable("screen.wildercord.wheel.empty").getString() : SpellCaster.nameOf(minecraft.player, book, slot, runes, SpellHud.read(runes));
			int lx = cx + (int) Math.round(Math.cos(a) * LABEL_RADIUS);
			int ly = cy + (int) Math.round(Math.sin(a) * LABEL_RADIUS);
			double cos = Math.cos(a);
			double sin = Math.sin(a);
			// As wide as there's room for before the edge of the screen, on the side the label grows toward.
			int room = cos > 0.3 ? width - lx - 4 : cos < -0.3 ? lx - 4 : 2 * Math.min(lx, width - lx) - 8;
			String shown = font.plainSubstrByWidth(name, Math.max(24, Math.min(150, room)));
			int icons = Math.min(runes.size(), Math.max(1, Math.min(8, room / 9)));
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
			List<RuneDef> runes = SpellCaster.activeRunes(book, slots.get(hovered), tier);
			if (!runes.isEmpty()) {
				SpellCompiler.Compiled compiled = SpellHud.read(runes);
				// A secret spell you've found costs and recharges as one (before that, as the ordinary spell).
				double secretCost = Heart.secretCost(minecraft.player, runes) * dev.wildercord.cast.Mastery.costFactor(minecraft.player, runes);
				Component cost = compiled.paysInHealth()
					? Component.literal(Heart.healthCost(minecraft.player, compiled, secretCost) + "❤")
					: Component.translatable("screen.wildercord.rune_cost", Heart.manaCost(minecraft.player, compiled, secretCost));
				String cooldown = String.format(Locale.ROOT, "%.1fs", Heart.cooldownTicks(minecraft.player, compiled, Heart.secretCooldown(minecraft.player, runes) * dev.wildercord.cast.Mastery.cooldownFactor(minecraft.player, runes)) / 20.0);
				g.centeredText(font, cost, cx, cy - 8, 0xFFB8A8FF);
				g.centeredText(font, Component.literal(cooldown), cx, cy + 2, DIM);
			}
		}
	}
}
