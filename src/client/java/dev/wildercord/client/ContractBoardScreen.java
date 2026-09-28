package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.runesmith.ContractRules;
import dev.wildercord.runesmith.Contracts;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * The Runesmith's contract board, opened from a Scribing Desk: today's three contracts as cards on
 * the Cord screen's panel, each with what it wants, how far along it is, and what it pays; handed-in
 * ones stamped, finished ones glowing gold. What was just handed in rises along the top for a moment.
 */
public class ContractBoardScreen extends Screen {
	private static final int W = 248;
	private static final int H = 176;
	private static final int CARD_X = 10;
	private static final int CARD_Y = 36;
	private static final int CARD_W = W - 20;
	private static final int CARD_H = 40;
	private static final int CARD_GAP = 4;

	private static final int GOLD = 0xFFE8C46A;
	private static final int TEXT = 0xFFE8E4F4;
	private static final int DIM = 0xFF8A84A0;
	private static final int FAINT = 0xFF5A5470;
	private static final int DONE = 0xFF7BD88F;

	private static final Identifier SPR_PANEL = Wildercord.id("cord/panel");
	private static final Identifier SPR_INSET = Wildercord.id("cord/inset");
	private static final Identifier SPR_ROW = Wildercord.id("cord/row");
	private static final Identifier SPR_ROW_SELECTED = Wildercord.id("cord/row_selected");
	private static final Identifier SPR_SOCKET = Wildercord.id("cord/socket");

	private final List<ContractRules.Contract> contracts;
	private final int ticksToDawn;
	private final List<ContractRules.Reward> handedIn;
	private final long opened = System.currentTimeMillis();

	public ContractBoardScreen(Contracts.ShowBoard board) {
		super(Component.translatable("screen.wildercord.contracts"));
		this.contracts = board.contracts();
		this.ticksToDawn = board.ticksToDawn();
		List<ContractRules.Reward> rewards = new ArrayList<>();
		for (String r : board.handedIn()) {
			int colon = r.indexOf(':');
			rewards.add(new ContractRules.Reward(r.substring(0, colon), Integer.parseInt(r.substring(colon + 1))));
		}
		this.handedIn = rewards;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private int left() {
		return (width - W) / 2;
	}

	private int top() {
		return (height - H) / 2;
	}

	private static void sprite(GuiGraphicsExtractor g, Identifier id, int x, int y, int w, int h) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		super.extractRenderState(g, mouseX, mouseY, partial);
		int x0 = left();
		int y0 = top();
		g.pose().pushMatrix();
		g.pose().translate(x0, y0);
		sprite(g, SPR_PANEL, 0, 0, W, H);
		// The title, and a quill line under it.
		g.text(font, title, 12, 10, GOLD, true);
		String refresh = Component.translatable("screen.wildercord.contracts.refresh", minutes(ticksToDawn)).getString();
		g.text(font, refresh, W - 12 - font.width(refresh), 10, DIM, false);
		g.fill(12, 22, W - 12, 23, 0x40E8C46A);
		g.fill(12, 23, W - 12, 24, 0x20000000);
		long now = System.currentTimeMillis();
		float t = (now - opened) / 1000F;
		List<Component> tooltip = null;
		int mx = mouseX - x0;
		int my = mouseY - y0;
		for (int i = 0; i < contracts.size(); i++) {
			ContractRules.Contract c = contracts.get(i);
			int cy = CARD_Y + i * (CARD_H + CARD_GAP);
			// Cards slide in one after another as the board opens.
			float slide = Mth.clamp((t - i * 0.08F) / 0.25F, 0, 1);
			int dx = Math.round((1 - ease(slide)) * 24);
			g.pose().pushMatrix();
			g.pose().translate(dx, 0);
			List<Component> tip = card(g, c, CARD_X, cy, now, mx - dx, my);
			g.pose().popMatrix();
			if (tip != null) {
				tooltip = tip;
			}
		}
		if (contracts.isEmpty()) {
			Component none = Component.translatable("screen.wildercord.contracts.none");
			g.text(font, none, (W - font.width(none)) / 2, CARD_Y + 40, DIM, false);
		}
		// Just handed in: the rewards rise and fade along the bottom.
		if (!handedIn.isEmpty() && t < 3.5F) {
			float fade = t < 2.5F ? 1 : 1 - (t - 2.5F);
			int alpha = Math.round(255 * Mth.clamp(fade, 0, 1));
			int rise = Math.round(Math.min(t, 0.6F) / 0.6F * 6);
			Component got = Component.translatable("screen.wildercord.contracts.handed_in");
			int gx = 12;
			int gy = H - 18 - rise;
			g.text(font, got, gx, gy + 4, (alpha << 24) | (GOLD & 0xFFFFFF), true);
			gx += font.width(got) + 6;
			for (ContractRules.Reward reward : handedIn) {
				g.item(icon(reward), gx, gy);
				g.itemDecorations(font, icon(reward), gx, gy);
				gx += 20;
			}
		} else {
			Component hint = Component.translatable("screen.wildercord.contracts.hint");
			g.text(font, hint, 12, H - 14, FAINT, false);
		}
		g.pose().popMatrix();
		if (tooltip != null) {
			g.setComponentTooltipForNextFrame(font, tooltip, mouseX, mouseY);
		}
	}

	/** One contract's card; returns a tooltip if the mouse is over its reward. */
	private List<Component> card(GuiGraphicsExtractor g, ContractRules.Contract c, int x, int y, long now, int mx, int my) {
		boolean done = c.done() && !c.claimed();
		int accent = accent(c);
		sprite(g, done ? SPR_ROW_SELECTED : SPR_ROW, x, y, CARD_W, CARD_H);
		// A stripe of the contract's colour down the left edge; finished ones pulse gold.
		int stripe = done ? pulse(GOLD, now) : c.claimed() ? 0xFF3C5A44 : accent;
		g.fill(x + 2, y + 3, x + 4, y + CARD_H - 3, stripe);
		// What it's about.
		sprite(g, SPR_SOCKET, x + 8, y + 11, 18, 18);
		g.item(kindIcon(c), x + 9, y + 12);
		int textX = x + 32;
		int textColor = c.claimed() ? FAINT : TEXT;
		Component what = Contracts.describe(c);
		drawClipped(g, what, textX, y + 7, CARD_W - 32 - 34, textColor);
		// Progress: a bar in the contract's colour, then gold once it's done.
		int barX = textX;
		int barY = y + 22;
		int barW = CARD_W - 32 - 70;
		g.fill(barX, barY, barX + barW, barY + 5, 0xFF1A1526);
		g.fill(barX, barY, barX + barW, barY + 1, 0xFF0C0A12);
		float frac = c.target() <= 0 ? 1 : Math.min(1F, c.progress() / (float) c.target());
		int fill = Math.round(barW * frac);
		int barColor = c.claimed() ? 0xFF3C5A44 : done ? pulse(GOLD, now) : accent;
		if (fill > 0) {
			g.fill(barX, barY, barX + fill, barY + 5, barColor);
			g.fill(barX, barY, barX + fill, barY + 1, lighten(barColor));
		}
		String count = Math.min(c.progress(), c.target()) + " / " + c.target();
		g.text(font, count, barX + barW + 6, barY - 1, c.claimed() ? FAINT : done ? GOLD : DIM, false);
		// Its status, under the bar.
		Component status = c.claimed() ? Component.translatable("screen.wildercord.contracts.claimed")
			: done ? Component.translatable("screen.wildercord.contracts.ready") : Component.empty();
		g.text(font, status, textX, y + 30, c.claimed() ? DONE : GOLD, false);
		// The reward, in its own socket on the right.
		ContractRules.Reward reward = ContractRules.Reward.parse(c.reward());
		ItemStack stack = icon(reward);
		int rx = x + CARD_W - 28;
		int ry = y + 11;
		if (done) {
			g.fill(rx - 3, ry - 3, rx + 21, ry + 21, (pulse(GOLD, now) & 0x00FFFFFF) | 0x50000000);
		}
		sprite(g, SPR_SOCKET, rx, ry, 18, 18);
		g.item(stack, rx + 1, ry + 1);
		g.itemDecorations(font, stack, rx + 1, ry + 1);
		if (c.claimed()) {
			g.fill(rx + 1, ry + 1, rx + 17, ry + 17, 0xA0100C18);
			g.text(font, "✔", rx + 5, ry + 5, DONE, true);
		}
		if (mx >= rx && mx < rx + 18 && my >= ry && my < ry + 18) {
			return List.of(Component.translatable("screen.wildercord.contracts.reward").withColor(GOLD), Contracts.rewardLabel(reward));
		}
		return null;
	}

	private void drawClipped(GuiGraphicsExtractor g, Component text, int x, int y, int maxWidth, int color) {
		if (font.width(text) <= maxWidth) {
			g.text(font, text, x, y, color, false);
			return;
		}
		String plain = font.plainSubstrByWidth(text.getString(), Math.max(0, maxWidth - font.width("…"))) + "…";
		g.text(font, Component.literal(plain).withStyle(text.getStyle()), x, y, color, false);
	}

	/** The colour a contract is drawn in: its element's, or a colour for its kind. */
	private static int accent(ContractRules.Contract c) {
		return 0xFF000000 | switch (c.kind()) {
			case ContractRules.RUNEBOUND, ContractRules.ELEMENT_CASTS -> RuneColors.element(c.arg());
			case ContractRules.REACTION -> 0xE07BD8;
			case ContractRules.LEY -> 0xB38CFF;
			case ContractRules.IMBUE -> 0xE678DC;
			default -> 0xD9D2B8;
		};
	}

	/** An item that says what a contract is about. */
	private static ItemStack kindIcon(ContractRules.Contract c) {
		return switch (c.kind()) {
			case ContractRules.RUNEBOUND -> new ItemStack(Items.ZOMBIE_HEAD);
			case ContractRules.ELEMENT_CASTS -> RuneItem.stack(elementRune(c.arg()));
			case ContractRules.REACTION -> RuneItem.stack(Runes.SHOCK);
			case ContractRules.LEY -> new ItemStack(dev.wildercord.content.WildercordBlocks.WELLSTONE);
			case ContractRules.IMBUE -> {
				ItemStack sword = new ItemStack(Items.IRON_SWORD);
				sword.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
				yield sword;
			}
			default -> new ItemStack(Items.SKELETON_SKULL);
		};
	}

	private static RuneDef elementRune(String element) {
		return switch (element) {
			case "fire" -> Runes.FIRE;
			case "frost" -> Runes.FROST;
			case "storm" -> Runes.LIGHTNING;
			case "wind" -> Runes.WINDCUT;
			case "earth" -> Runes.TREMOR;
			default -> Runes.HARM;
		};
	}

	/** A reward as an item (a rune reward shows a Blank Rune marked with its tier). */
	private static ItemStack icon(ContractRules.Reward reward) {
		return switch (reward.type()) {
			case "rune" -> new ItemStack(WildercordItems.BLANK_RUNE);
			case "blank_rune" -> new ItemStack(WildercordItems.BLANK_RUNE, reward.amount());
			case "mana_crystal" -> new ItemStack(WildercordItems.MANA_CRYSTAL, reward.amount());
			default -> new ItemStack(Items.EMERALD, reward.amount());
		};
	}

	private static String minutes(int ticks) {
		int minutes = Math.max(1, Math.round(ticks / 1200F));
		return minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "m" : minutes + "m";
	}

	private static float ease(float x) {
		return 1 - (1 - x) * (1 - x) * (1 - x);
	}

	private static int pulse(int argb, long now) {
		float k = 0.75F + 0.25F * (float) Math.sin(now / 250.0);
		int r = Math.round(((argb >> 16) & 0xFF) * k);
		int gr = Math.round(((argb >> 8) & 0xFF) * k);
		int b = Math.round((argb & 0xFF) * k);
		return 0xFF000000 | (r << 16) | (gr << 8) | b;
	}

	private static int lighten(int argb) {
		int r = Math.min(255, ((argb >> 16) & 0xFF) + 60);
		int gr = Math.min(255, ((argb >> 8) & 0xFF) + 60);
		int b = Math.min(255, (argb & 0xFF) + 60);
		return 0xFF000000 | (r << 16) | (gr << 8) | b;
	}
}
