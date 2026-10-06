package dev.wildercord.client.cosmetic;

import dev.wildercord.Wildercord;
import dev.wildercord.client.CastingPose;
import dev.wildercord.client.CordScreen;
import dev.wildercord.content.CordTier;
import dev.wildercord.cosmetic.CordCosmetics;
import dev.wildercord.cosmetic.CordStyles;
import dev.wildercord.cosmetic.CordStyles.Option;
import dev.wildercord.cosmetic.CordStyles.Style;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Circles;
import dev.wildercord.spell.Feats;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The Cord screen's Cosmetics page: how your Cord looks to everyone. Pick the beads' material, their
 * glow (your spells' own colours, or one fixed colour) and the trail your casts leave; the choice is
 * saved at once. Beside the choices your own figure turns slowly, wearing the Cord; point at any
 * option, even a locked one, to see it on your wrist. Locked options say how to earn them, and the
 * ones bought with materials are bought with a click.
 *
 * <p>Drawn like the rest of the Cord screen (same panel, header and page tabs, same scaling), so
 * it reads as one of its pages.</p>
 */
public class CordStyleScreen extends Screen implements dev.wildercord.client.CordEditorParent {
	private static final int W = 372;
	private static final int H = 292;
	private static final int CELL = 18;
	private static final int PITCH = 20;
	private static final int PREVIEW_X = 10;
	private static final int PREVIEW_Y = 26;
	private static final int PREVIEW_W = 138;
	private static final int PREVIEW_H = H - PREVIEW_Y - 12;
	private static final int RIGHT = PREVIEW_X + PREVIEW_W + 8;
	private static final int[] SECTION_Y = {28, 66, 124};
	private static final int READOUT_TOP = 164;

	private static final int GOLD = 0xFFE8C46A;
	private static final int TEXT = 0xFFE8E4F4;
	private static final int DIM = 0xFF8A84A0;
	private static final int FAINT = 0xFF5A5470;
	private static final int GREEN = 0xFF9CE08C;

	private static final Identifier SPR_PANEL = Wildercord.id("cord/panel");
	private static final Identifier SPR_INSET = Wildercord.id("cord/inset");
	private static final Identifier SPR_SOCKET = Wildercord.id("cord/socket");
	private static final Identifier SPR_SOCKET_HOVER = Wildercord.id("cord/socket_hover");
	private static final Identifier SPR_LOCK = Wildercord.id("cord/lock");

	private static final String[] GROUPS = {CordStyles.MATERIAL, CordStyles.GLOW, CordStyles.TRAIL};

	/** Glows in two rows: "spell" and eight dyes, then the other eight. */
	private static final int GLOW_ROW = 9;

	private final CordScreen parent;
	@Override public Screen cordEditorParent(){return parent;}
	private Option hovered;
	private final long opened = net.minecraft.util.Util.getMillis();

	public CordStyleScreen(CordScreen parent) {
		super(Component.translatable("screen.wildercord.page.cosmetics"));
		this.parent = parent;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean isInGameUi() {
		return true;
	}

	// ------------------------------------------------------------------ layout (as the Cord screen)

	private float scale() {
		double fit = Math.min(1.0, Math.min((width - 8) / (double) W, (height - 8) / (double) H));
		if (fit >= 1.0) {
			return 1.0F;
		}
		int guiScale = Math.max(1, minecraft.getWindow().getGuiScale());
		int physical = Math.max(1, (int) Math.floor(guiScale * fit));
		return physical / (float) guiScale;
	}

	private int left() {
		return Math.round((width - W * scale()) / 2);
	}

	private int top() {
		return Math.round((height - H * scale()) / 2);
	}

	private double localX(double screenX) {
		return (screenX - left()) / scale();
	}

	private double localY(double screenY) {
		return (screenY - top()) / scale();
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/** Where an option's cell is, in local coordinates. */
	private static int[] cell(String group, int index) {
		int section = group.equals(CordStyles.MATERIAL) ? 0 : group.equals(CordStyles.GLOW) ? 1 : 2;
		int row = group.equals(CordStyles.GLOW) ? index / GLOW_ROW : 0;
		int col = group.equals(CordStyles.GLOW) ? index % GLOW_ROW : index;
		return new int[] {RIGHT + col * PITCH, SECTION_Y[section] + 12 + row * PITCH};
	}

	private Option optionAt(double mx, double my) {
		for (String group : GROUPS) {
			List<Option> options = CordStyles.group(group);
			for (int i = 0; i < options.size(); i++) {
				int[] at = cell(group, i);
				if (inside(mx, my, at[0], at[1], CELL, CELL)) {
					return options.get(i);
				}
			}
		}
		return null;
	}

	/** The middle of an option's cell, on screen (for the game tests). */
	public double[] optionPoint(String key) {
		Option option = CordStyles.option(key);
		int[] at = cell(option.group(), CordStyles.group(option.group()).indexOf(option));
		return new double[] {left() + (at[0] + CELL / 2.0) * scale(), top() + (at[1] + CELL / 2.0) * scale()};
	}

	// ------------------------------------------------------------------ drawing

	private static void sprite(GuiGraphicsExtractor g, Identifier id, int x, int y, int w, int h) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, id, x, y, w, h);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		super.extractRenderState(g, mouseX, mouseY, partial);
		if (minecraft.player == null) {
			return;
		}
		float s = scale();
		int mx = (int) Math.floor(localX(mouseX));
		int my = (int) Math.floor(localY(mouseY));
		g.pose().pushMatrix();
		g.pose().translate(left(), top());
		g.pose().scale(s, s);
		List<Component> tooltip = draw(g, mx, my);
		g.pose().popMatrix();
		if (Spellbooks.tier(minecraft.player) != null) {
			drawPreview(g, partial);
		}
		if (tooltip != null) {
			g.setTooltipForNextFrame(font, dev.wildercord.client.Tooltips.fit(font, tooltip, width, height), mouseX, mouseY);
		}
	}

	private List<Component> draw(GuiGraphicsExtractor g, int mx, int my) {
		sprite(g, SPR_PANEL, 0, 0, W, H);
		CordTier tier = Spellbooks.tier(minecraft.player);
		Component name = tier == null ? Component.translatable("screen.wildercord.page.cosmetics") : Component.translatable(tier.itemKey());
		// The same header as the Cord screen's other pages, so the tabs don't jump when you switch.
		CordScreen.PageTabs tabs = CordScreen.pageTabs(font, name);
		g.text(font, tabs.shown(), 13, 10, GOLD, true);
		tabs.draw(g, font, 3, mx, my);
		if (tier == null) {
			g.centeredText(font, Component.translatable("screen.wildercord.no_cord"), W / 2, H / 2 - 10, TEXT);
			g.centeredText(font, Component.translatable("screen.wildercord.no_cord_hint"), W / 2, H / 2 + 4, DIM);
			return null;
		}

		sprite(g, SPR_INSET, PREVIEW_X, PREVIEW_Y, PREVIEW_W, PREVIEW_H);
		Style style = CordCosmetics.style(minecraft.player);
		CordStyles.Progress progress = CordCosmetics.progress(minecraft.player);
		hovered = optionAt(mx, my);
		List<Component> tooltip = null;
		for (int section = 0; section < GROUPS.length; section++) {
			String group = GROUPS[section];
			g.text(font, Component.translatable("screen.wildercord.cosmetics." + group), RIGHT, SECTION_Y[section], GOLD, true);
			List<Option> options = CordStyles.group(group);
			for (int i = 0; i < options.size(); i++) {
				Option option = options.get(i);
				int[] at = cell(group, i);
				boolean unlocked = CordStyles.unlocked(option, progress);
				boolean worn = style.get(group).equals(option.id());
				boolean hover = option == hovered;
				sprite(g, hover ? SPR_SOCKET_HOVER : SPR_SOCKET, at[0], at[1], CELL, CELL);
				drawSwatch(g, option, at[0] + 3, at[1] + 3);
				if (!unlocked) {
					g.fill(at[0] + 2, at[1] + 2, at[0] + CELL - 2, at[1] + CELL - 2, 0x99100C18);
					sprite(g, SPR_LOCK, at[0] + 10, at[1] + 9, 7, 8);
				}
				if (worn) {
					frame(g, at[0], at[1], CELL, CELL, GOLD);
				}
				if (hover) {
					tooltip = List.of(optionName(option).copy().withColor(option.color() | 0xFF000000), unlockLine(option, progress));
				}
			}
		}
		drawReadout(g, style, progress);
		return tooltip;
	}

	private void drawSwatch(GuiGraphicsExtractor g, Option option, int x, int y) {
		switch (option.group()) {
			case CordStyles.MATERIAL -> sprite(g, Wildercord.id("cord/bead_" + option.id()), x, y, 12, 12);
			case CordStyles.TRAIL -> sprite(g, Wildercord.id("cord/trail_" + option.id()), x, y, 12, 12);
			default -> {
				if (option.id().equals("spell")) {
					sprite(g, Wildercord.id("cord/glow_spell"), x, y, 12, 12);
				} else {
					g.fill(x + 1, y + 1, x + 11, y + 11, 0xFF0B0910);
					g.fill(x + 2, y + 2, x + 10, y + 10, 0xFF000000 | option.color());
					g.fill(x + 3, y + 3, x + 5, y + 5, 0x60FFFFFF);
				}
			}
		}
	}

	private static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x + w, y + 1, color);
		g.fill(x, y + h - 1, x + w, y + h, color);
		g.fill(x, y, x + 1, y + h, color);
		g.fill(x + w - 1, y, x + w, y + h, color);
	}

	private static Component optionName(Option option) {
		return Component.translatable("cosmetic.wildercord." + option.group() + "." + option.id());
	}

	/** How an option is earned, or that it's yours. */
	private Component unlockLine(Option option, CordStyles.Progress progress) {
		if (CordStyles.unlocked(option, progress)) {
			return Component.translatable("screen.wildercord.cosmetics.unlocked").withColor(GREEN);
		}
		CordStyles.Unlock unlock = option.unlock();
		return switch (unlock.kind()) {
			case CIRCLE -> Component.translatable("screen.wildercord.cosmetics.need_circle", Circles.ordinal(unlock.amount())).withColor(DIM);
			case FEAT -> Component.translatable("screen.wildercord.cosmetics.need_feat", Feats.feat(unlock.what()).name(),
				Feats.feat(unlock.what()).description()).withColor(DIM);
			case BOSS -> Component.translatable("screen.wildercord.cosmetics.need_boss").withColor(DIM);
			case CRAFT -> {
				Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(unlock.what()));
				int have = minecraft.player.getInventory().clearOrCountMatchingItems(stack -> stack.is(item), true, 0,
					minecraft.player.inventoryMenu.getCraftSlots());
				yield Component.translatable("screen.wildercord.cosmetics.buy", unlock.amount(), item.getName(new ItemStack(item)), have)
					.withColor(have >= unlock.amount() ? GOLD : 0xFFE06060);
			}
			default -> Component.empty();
		};
	}

	/** Under the choices: what's being pointed at (or worn), its description, and how it's earned. */
	private void drawReadout(GuiGraphicsExtractor g, Style style, CordStyles.Progress progress) {
		sprite(g, SPR_INSET, RIGHT - 4, READOUT_TOP, W - RIGHT - 6, H - 12 - READOUT_TOP);
		int x = RIGHT + 2;
		int y = READOUT_TOP + 6;
		int width = W - RIGHT - 18;
		List<FormattedCharSequence> lines = new ArrayList<>();
		if (hovered != null) {
			g.text(font, optionName(hovered), x, y, hovered.color() | 0xFF000000, true);
			y += 12;
			Component desc = Component.translatable("cosmetic.wildercord." + hovered.group() + "." + hovered.id() + ".desc");
			lines.addAll(font.split(desc, width));
			lines.add(FormattedCharSequence.EMPTY);
			lines.addAll(font.split(unlockLine(hovered, progress), width));
		} else {
			g.text(font, Component.translatable("screen.wildercord.cosmetics.wearing"), x, y, GOLD, true);
			y += 12;
			for (String group : GROUPS) {
				Option worn = CordStyles.find(group, style.get(group));
				if (worn != null) {
					lines.addAll(font.split(Component.translatable("screen.wildercord.cosmetics.worn." + group, optionName(worn)), width));
				}
			}
			lines.add(FormattedCharSequence.EMPTY);
			lines.addAll(font.split(Component.translatable("screen.wildercord.cosmetics.hint").withColor(FAINT), width));
		}
		for (FormattedCharSequence line : lines) {
			if (y + 9 > H - 16) {
				break;
			}
			g.text(font, line, x, y, TEXT, false);
			y += 10;
		}
	}

	/**
	 * Your figure, slowly turning, wearing the Cord: the style you point at (even a locked one), or
	 * the one you wear. With no spell ready the beads would be missing, so a row of them is lent.
	 */
	private void drawPreview(GuiGraphicsExtractor g, float partial) {
		LivingEntity player = minecraft.player;
		EntityRenderer<? super LivingEntity, ?> renderer = minecraft.getEntityRenderDispatcher().getRenderer(player);
		EntityRenderState state = renderer.createRenderState(player, partial);
		state.shadowPieces.clear();
		state.outlineColor = 0;
		float seconds = (net.minecraft.util.Util.getMillis() - opened) / 1000F;
		float spin = seconds * 40F;
		if (state instanceof LivingEntityRenderState living) {
			living.bodyRot = 180F + 30F + spin;
			living.yRot = 0;
			living.xRot = 0;
			living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
			living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
			living.scale = 1.0F;
		}
		Style style = CordCosmetics.style(minecraft.player);
		if (hovered != null) {
			style = style.with(hovered);
		}
		if (state instanceof AvatarRenderState avatar) {
			avatar.setData(CordStyleLook.STYLE, style);
			CastingPose pose = (CastingPose) avatar;
			WildercordAttachments.CordLook cord = pose.wildercord$cord();
			if (cord.beads().isEmpty() && !cord.tier().isEmpty()) {
				pose.wildercord$setCord(new WildercordAttachments.CordLook(cord.tier(), List.of(0xF06E32, 0x8CDCFF, 0xFFE650, 0x6EDC64, 0xE678DC)));
			}
			// The beads breathe brighter and dimmer, so the glow shows.
			pose.wildercord$setGlow(1.3F + 0.5F * Mth.sin(seconds * 2.5F));
		}
		float s = scale();
		int x0 = Math.round(left() + (PREVIEW_X + 2) * s);
		int y0 = Math.round(top() + (PREVIEW_Y + 2) * s);
		int x1 = Math.round(left() + (PREVIEW_X + PREVIEW_W - 2) * s);
		int y1 = Math.round(top() + (PREVIEW_Y + PREVIEW_H - 2) * s);
		// Close enough to see the beads: the frame shows about a block and a half, centred just above the wrist.
		float size = (y1 - y0) / 1.55F;
		g.entity(state, size, new Vector3f(0, 0.95F, 0), new Quaternionf().rotateZ((float) Math.PI), null, x0, y0, x1, y1);
		if (hovered != null && hovered.group().equals(CordStyles.TRAIL) && !hovered.id().equals("none")) {
			drawTrailPreview(g, hovered.id(), (x0 + x1) / 2, (y0 + y1) / 2 + Math.round(size * 0.1F), seconds, size);
		}
	}

	/** A few of the trail's motes drifting out from around the wrist, over the figure. */
	private void drawTrailPreview(GuiGraphicsExtractor g, String trail, int cx, int cy, float seconds, float size) {
		Identifier icon = Wildercord.id("cord/trail_" + trail);
		for (int i = 0; i < 7; i++) {
			float t = (seconds * 0.6F + i / 7F) % 1F;
			float angle = i * 2.4F;
			int x = Math.round(cx + Mth.cos(angle) * size * 0.35F * t);
			int y = Math.round(cy + (trail.equals("embers") || trail.equals("sparks") || trail.equals("stars") ? -1 : 1) * size * 0.3F * t
				+ Mth.sin(angle) * size * 0.1F * t);
			int d = Math.max(4, Math.round(8 * (1 - t) + 2));
			sprite(g, icon, x - d / 2, y - d / 2, d, d);
		}
	}

	// ------------------------------------------------------------------ clicks

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = localX(event.x());
		double my = localY(event.y());
		if (minecraft.player == null) {
			return super.mouseClicked(event, doubleClick);
		}
		CordTier tier = Spellbooks.tier(minecraft.player);
		Component name = tier == null ? Component.translatable("screen.wildercord.page.cosmetics") : Component.translatable(tier.itemKey());
		int page = CordScreen.pageTabs(font, name).at(mx, my);
		if (page >= 0) {
			if (page != 3) {
				click();
				parent.showPage(page);
				minecraft.gui.setScreen(parent);
			}
			return true;
		}
		if (tier == null) {
			return true;
		}
		Option option = optionAt(mx, my);
		if (option != null) {
			choose(option);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	/** Wears an option, or buys it first if it's bought with materials; the server checks either way. */
	public void choose(Option option) {
		CordStyles.Progress progress = CordCosmetics.progress(minecraft.player);
		if (CordStyles.unlocked(option, progress)) {
			Style next = CordCosmetics.style(minecraft.player).with(option);
			ClientPlayNetworking.send(new CordCosmetics.SetStyle(next.material(), next.glow(), next.trail()));
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(dev.wildercord.content.WildercordSounds.RUNE_THREAD, 1.0F, 1.0F));
		} else if (CordStyles.buyable(option)) {
			ClientPlayNetworking.send(new CordCosmetics.BuyStyle(option.key()));
			click();
		} else {
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS, 0.6F));
			minecraft.player.sendOverlayMessage(unlockLine(option, progress).copy().withStyle(ChatFormatting.GRAY));
		}
	}

	private void click() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
	}
}
