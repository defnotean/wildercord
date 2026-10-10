package dev.wildercord.client;

import dev.wildercord.cast.HeartCircles;
import dev.wildercord.content.CordTier;
import dev.wildercord.net.BreakthroughPayload;
import dev.wildercord.player.Heart;
import dev.wildercord.spell.CircleVows;
import dev.wildercord.spell.Circles;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.Passives;
import dev.wildercord.spell.TribulationRules;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A breakthrough's page (0.12 "Tempering"): shown when a circle forms, it says what the circle brought (its share of mana,
 * regeneration and power, any perk, passive slot, vow or lesson it opens, a tribulation's spoils) and what the next asks.
 */
public final class BreakthroughScreen extends Screen {
	/** A breakthrough that arrived while another screen was open waits for it to close. */
	private static BreakthroughPayload waiting;

	private final int circle;
	private final boolean tribulation;
	private final int color;
	private int left, top, panelWidth, panelHeight, bodyTop, bodyBottom, scroll;

	public BreakthroughScreen(int circle, boolean tribulation) {
		super(Component.translatable("screen.wildercord.breakthrough.title"));
		this.circle = circle;
		this.tribulation = tribulation;
		this.color = 0xFF000000 | HeartCircles.color(circle);
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(BreakthroughPayload.TYPE, (payload, context) -> {
			if (context.client().gui.screen() == null) open(context.client(), payload);
			else waiting = payload;
		});
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (waiting != null && client.player != null && client.gui.screen() == null) {
				BreakthroughPayload payload = waiting;
				waiting = null;
				open(client, payload);
			} else if (client.player == null) {
				waiting = null;
			}
		});
	}

	private static void open(Minecraft client, BreakthroughPayload payload) {
		client.gui.setScreen(new BreakthroughScreen(payload.circle(), payload.tribulation()));
	}

	@Override public boolean isPauseScreen() { return false; }

	@Override
	protected void init() {
		panelWidth = Math.min(380, width - 20);
		panelHeight = Math.min(300, height - 16);
		left = (width - panelWidth) / 2;
		top = (height - panelHeight) / 2;
		bodyTop = top + 46;
		bodyBottom = top + panelHeight - 34;
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
			.bounds(width / 2 - 60, top + panelHeight - 26, 120, 20).build());
	}

	private List<Component> content() {
		Player player = minecraft.player;
		List<Component> lines = new ArrayList<>();
		lines.add(Component.translatable("screen.wildercord.breakthrough.gained", Circles.MANA_PER_CIRCLE,
			String.format(Locale.ROOT, "%.1f", Circles.REGEN_PER_CIRCLE), Math.round(Circles.POWER_PER_CIRCLE * 100)).withStyle(ChatFormatting.WHITE));
		if (player != null) {
			int active = Heart.active(player);
			lines.add(Component.translatable("screen.wildercord.heart.bonus", active * Circles.MANA_PER_CIRCLE,
				String.format(Locale.ROOT, "%.1f", active * Circles.REGEN_PER_CIRCLE), Math.round(active * Circles.POWER_PER_CIRCLE * 100)).withStyle(ChatFormatting.GRAY));
		}
		lines.add(Component.empty());
		lines.add(Component.translatable("screen.wildercord.breakthrough.unlocks").withColor(color));
		int unlocks = 0;
		if (tribulation) {
			lines.add(bullet(Component.translatable("screen.wildercord.breakthrough.tribulation", TribulationRules.spoilRunes(circle), TribulationRules.spoilCrystals(circle))));
			unlocks++;
		}
		if (TribulationRules.tribulation(circle)) {
			lines.add(bullet(Component.translatable("screen.wildercord.breakthrough.scar", TribulationRules.scars(circle))));
			unlocks++;
		}
		if (Passives.slots(circle) > Passives.slots(circle - 1)) {
			lines.add(bullet(Component.translatable("message.wildercord.passive_slot", Passives.slots(circle))));
			unlocks++;
		}
		String perk = "message.wildercord.perk." + circle;
		if (net.minecraft.locale.Language.getInstance().has(perk)) {
			lines.add(bullet(Component.translatable(perk)));
			unlocks++;
		}
		String lesson = "screen.wildercord.breakthrough.lesson." + circle;
		if (net.minecraft.locale.Language.getInstance().has(lesson)) {
			lines.add(bullet(Component.translatable(lesson)));
			unlocks++;
		}
		CircleVows.Vow vow = CircleVows.at(circle);
		if (vow != null) {
			lines.add(bullet(Component.translatable("screen.wildercord.breakthrough.vow", vow.first().name(), vow.first().text(),
				vow.second().name(), vow.second().text())));
			unlocks++;
		}
		if (unlocks == 0) lines.add(bullet(Component.translatable("screen.wildercord.breakthrough.steady")));
		lines.add(Component.empty());
		if (circle >= Circles.MAX) {
			lines.add(Component.translatable("screen.wildercord.heart.complete", Circles.MAX).withStyle(ChatFormatting.GOLD));
			return lines;
		}
		int next = circle + 1;
		lines.add(Component.translatable("screen.wildercord.heart.next", Circles.ordinal(next)).withStyle(ChatFormatting.GOLD));
		lines.add(bullet(Component.translatable("screen.wildercord.heart.condense", "0",
			String.format(Locale.ROOT, "%,d", Circles.condenseNeeded(next)))));
		for (Circles.Requirement requirement : Circles.requirements(next)) {
			Component text = switch (requirement.need()) {
				case RUNES -> Component.translatable("screen.wildercord.heart.need.runes", requirement.amount(), player == null ? 0 : Heart.progress(player, requirement));
				case CORD -> Component.translatable("screen.wildercord.heart.need.cord",
					Component.translatable(CordTier.values()[Math.min(CordTier.values().length - 1, requirement.amount())].itemKey()));
				case KILLS -> Component.translatable("screen.wildercord.heart.need.kills", requirement.amount(), player == null ? 0 : Heart.progress(player, requirement));
				case BOSS -> Component.translatable("screen.wildercord.heart.need.boss");
				case REACTIONS -> Component.translatable("screen.wildercord.heart.need.reactions", requirement.amount(), player == null ? 0 : Heart.progress(player, requirement));
				case RUNEBOUND -> Component.translatable("screen.wildercord.heart.need.runebound", requirement.amount(), player == null ? 0 : Heart.progress(player, requirement));
				case SECRETS -> Component.translatable("screen.wildercord.heart.need.secrets", requirement.amount(), player == null ? 0 : Heart.progress(player, requirement));
				case FEAT -> Component.translatable("screen.wildercord.heart.need.feat", Feats.feat(requirement.feat()).name(), Feats.feat(requirement.feat()).description());
			};
			boolean met = player != null && Heart.met(player, requirement);
			lines.add(Component.literal(met ? "✔ " : "• ").append(text).withStyle(met ? ChatFormatting.GREEN : ChatFormatting.GRAY));
		}
		if (TribulationRules.tribulation(next)) {
			lines.add(bullet(Component.translatable("screen.wildercord.breakthrough.next_tribulation", TribulationRules.waves(next))).copy().withStyle(ChatFormatting.YELLOW));
		}
		return lines;
	}

	private static Component bullet(Component text) {
		return Component.literal("• ").append(text).withStyle(ChatFormatting.GRAY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF2141022);
		graphics.fill(left + 1, top + 1, left + panelWidth - 1, top + 2, color);
		graphics.fill(left + 1, top + panelHeight - 2, left + panelWidth - 1, top + panelHeight - 1, color);
		graphics.centeredText(font, title, width / 2, top + 10, 0xFFE8D8B0);
		graphics.centeredText(font, Component.translatable("title.wildercord.circle", Circles.ordinal(circle)), width / 2, top + 24, color);
		List<FormattedCharSequence> lines = new ArrayList<>();
		for (Component line : content()) {
			if (line.getString().isEmpty()) lines.add(FormattedCharSequence.EMPTY);
			else lines.addAll(font.split(line, Math.max(80, panelWidth - 32)));
		}
		int visible = Math.max(1, (bodyBottom - bodyTop) / 11);
		int maximum = Math.max(0, lines.size() - visible);
		scroll = Math.clamp(scroll, 0, maximum);
		graphics.enableScissor(left + 12, bodyTop, left + panelWidth - 12, bodyBottom);
		for (int index = scroll; index < Math.min(lines.size(), scroll + visible); index++) {
			graphics.text(font, lines.get(index), left + 16, bodyTop + (index - scroll) * 11, 0xFFE8E5D9, false);
		}
		graphics.disableScissor();
		super.extractRenderState(graphics, mouseX, mouseY, partial);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		scroll = Math.max(0, scroll - (int) Math.signum(scrollY) * 3);
		return true;
	}
}
