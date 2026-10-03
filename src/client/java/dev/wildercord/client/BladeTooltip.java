package dev.wildercord.client;

import dev.wildercord.aura.BladeBond;
import dev.wildercord.aura.BladeCeremony;
import dev.wildercord.aura.BladeRules;
import dev.wildercord.aura.BondedBlades;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A bonded blade's tooltip tells its story. Under its name: its tier and whose it is, its resonance toward the next tier (or what that
 * waits on), its trait, and a word if it's notched or if it's someone else's. Held Shift: where and when it was bonded, when it took its
 * name, who carried it before, its deeds counted, the art it has played most, and its notable deeds by day. A blade whose bond ended
 * says whose it once was.
 */
public final class BladeTooltip {
	private BladeTooltip() {}

	private static final int GOLD = 0xFFE8C46A;
	private static final int TEXT = 0xFFC8C0D8;
	private static final int DIM = 0xFF8A84A0;

	public static void init() {
		ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
			List<Component> extra = lines(stack, Minecraft.getInstance().player, Minecraft.getInstance().hasShiftDown());
			if (!extra.isEmpty()) {
				lines.addAll(Math.min(1, lines.size()), extra);
			}
		});
	}

	/** The lines {@code stack} adds under its name for {@code viewer} ({@code story}: the whole story, Shift held). */
	public static List<Component> lines(ItemStack stack, LocalPlayer viewer, boolean story) {
		List<Component> out = new ArrayList<>();
		String etched = stack.get(dev.wildercord.aura.RuneEtchings.RUNE);
		if (etched != null) {
			var rune = dev.wildercord.spell.Runes.get(etched).orElse(null);
			out.add(Component.translatable("tooltip.wildercord.blade_rune", rune == null ? Component.literal(etched)
				: dev.wildercord.content.RuneItem.runeName(rune)).withColor(GOLD));
			out.add(Component.translatable("tooltip.wildercord.blade_rune.price", rune == null ? "?"
				: dev.wildercord.aura.RuneEtchingRules.price(rune)).withColor(DIM));
		}
		BladeBond.Former former = stack.get(BondedBlades.FORMER);
		BladeBond b = BondedBlades.bond(stack);
		if (b == null) {
			if (former != null) {
				Component tier = Component.translatable("aura.wildercord.blade.tier." + BladeRules.tierId(Math.max(1, former.tier())));
				out.add(Component.translatable(former.name().isEmpty() ? "tooltip.wildercord.bonded_blade.former_unnamed" : "tooltip.wildercord.bonded_blade.former",
					Component.literal(former.ownerName()), Component.literal(BladeRules.cleanName(former.name())), tier).withStyle(ChatFormatting.ITALIC).withColor(DIM));
			}
			return out;
		}
		int color = 0xFF000000 | (b.color() == 0 ? 0xD8D0F0 : b.color());
		boolean mine = viewer != null && b.ownedBy(viewer.getUUID());
		Component tier = Component.translatable("aura.wildercord.blade.tier." + BladeRules.tierId(b.tier())).withColor(GOLD);
		out.add(Component.translatable("tooltip.wildercord.bonded_blade.whose", tier, Component.literal(b.who().ownerName()).withColor(color)).withColor(TEXT));
		// Its resonance toward the next tier, or what that waits on.
		double resonance = b.growth().resonance();
		int bosses = b.history().count(BladeRules.BOSSES);
		if (b.tier() >= BladeRules.MAX_TIER) {
			out.add(Component.translatable("tooltip.wildercord.bonded_blade.resonance_top", (int) resonance).withColor(color));
		} else {
			int next = b.tier() + 1;
			out.add(Component.empty().append(bar(BladeRules.progress(resonance, b.tier()), color)).append(" ")
				.append(Component.translatable("tooltip.wildercord.bonded_blade.resonance", (int) resonance, (int) BladeRules.threshold(next),
					Component.translatable("aura.wildercord.blade.tier." + BladeRules.tierId(next))).withColor(TEXT)));
			if (mine) {
				BladeRules.Waiting waiting = BladeRules.waiting(b.tier(), resonance, bosses, dev.wildercord.aura.Aura.stage(viewer));
				if (waiting == BladeRules.Waiting.STAGE) {
					out.add(Component.translatable("tooltip.wildercord.bonded_blade.waits_stage", Component.translatable("aura.wildercord.stage."
						+ dev.wildercord.aura.AuraStages.id(BladeRules.gate(next)))).withColor(DIM));
				} else if (waiting == BladeRules.Waiting.BOSS) {
					out.add(Component.translatable("tooltip.wildercord.bonded_blade.waits_boss").withColor(DIM));
				}
			}
		}
		// Its trait.
		String trait = b.growth().trait();
		if (!trait.isEmpty()) {
			BladeRules.trait(trait).ifPresent(t -> {
				MutableComponent name = Component.translatable(t.nameKey()).withColor(GOLD);
				if (trait.equals(BladeRules.WELL_WORN) && !b.growth().traitArt().isEmpty()) {
					name = Component.translatable("tooltip.wildercord.bonded_blade.well_worn", name, artName(b.growth().traitArt())).withColor(GOLD);
				}
				out.add(Component.translatable("tooltip.wildercord.bonded_blade.trait", name).withColor(TEXT));
				out.add(Component.translatable(t.nameKey() + ".desc").withColor(DIM));
			});
		} else if (b.tier() >= BladeRules.AWAKENED && mine) {
			out.add(Component.translatable("tooltip.wildercord.bonded_blade.trait_waits").withColor(GOLD));
		}
		if (stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1) {
			out.add(Component.translatable("tooltip.wildercord.bonded_blade.notched").withColor(0xFFC8A0A0));
		}
		if (viewer != null && !mine) {
			out.add(Component.translatable("tooltip.wildercord.bonded_blade.not_yours", Component.literal(b.who().ownerName())).withColor(0xFFC8A0A0));
		}
		if (!story) {
			out.add(Component.translatable("tooltip.wildercord.bonded_blade.shift").withStyle(ChatFormatting.ITALIC).withColor(0xFF6A6480));
			return out;
		}
		out.addAll(story(b));
		return out;
	}

	/** The whole story: where and when, its name, who carried it, its deeds counted, its favourite art, its notable deeds. */
	static List<Component> story(BladeBond b) {
		List<Component> out = new ArrayList<>();
		BladeBond.Origin o = b.origin();
		out.add(Component.translatable("tooltip.wildercord.bonded_blade.bonded_on", o.day(), Component.translatable(BladeCeremony.biomeKey(o.biome())),
			o.x(), o.z()).withColor(TEXT));
		if (!b.shownName().isEmpty() && b.growth().namedDay() > 0) {
			out.add(Component.translatable("tooltip.wildercord.bonded_blade.named_on", Component.literal(b.shownName()), b.growth().namedDay()).withColor(TEXT));
		}
		if (!b.who().lineage().isEmpty()) {
			out.add(Component.translatable("tooltip.wildercord.bonded_blade.lineage", String.join(", ", b.who().lineage())).withColor(TEXT));
		}
		// Its deeds counted, two to a line.
		List<Component> counts = new ArrayList<>();
		for (String id : BladeRules.SHOWN_COUNTS) {
			int n = b.history().count(id);
			if (n > 0) {
				counts.add(Component.translatable("tooltip.wildercord.bonded_blade.count." + id, String.format(Locale.ROOT, "%,d", n)));
			}
		}
		for (int i = 0; i < counts.size(); i += 2) {
			MutableComponent line = Component.empty().append(counts.get(i));
			if (i + 1 < counts.size()) {
				line.append(Component.literal("  ·  ").withColor(0xFF5A5470)).append(counts.get(i + 1));
			}
			out.add(line.withColor(DIM));
		}
		String fav = BladeRules.favourite(b.history().arts());
		if (!fav.isEmpty()) {
			out.add(Component.translatable("tooltip.wildercord.bonded_blade.favourite", artName(fav), b.history().arts().getOrDefault(fav, 0)).withColor(DIM));
		}
		List<BladeBond.Deed> deeds = b.history().deeds();
		if (!deeds.isEmpty()) {
			out.add(Component.translatable("tooltip.wildercord.bonded_blade.deeds").withColor(GOLD));
			for (int i = Math.max(0, deeds.size() - 6); i < deeds.size(); i++) {
				out.add(Component.literal(" ").append(deed(deeds.get(i))).withColor(TEXT));
			}
		}
		return out;
	}

	/** A notable deed's line, by its kind. */
	public static Component deed(BladeBond.Deed d) {
		Component what = switch (d.kind()) {
			case BladeRules.DEED_BOSS -> Component.translatable("tooltip.wildercord.bonded_blade.deed.boss", Component.translatable(d.arg()));
			case BladeRules.DEED_BREAKTHROUGH -> Component.translatable("tooltip.wildercord.bonded_blade.deed.breakthrough",
				Component.translatable("aura.wildercord.stage." + d.arg()));
			case BladeRules.DEED_TIER -> Component.translatable("tooltip.wildercord.bonded_blade.deed.tier", Component.translatable("aura.wildercord.blade.tier." + d.arg()));
			case BladeRules.DEED_DUEL -> Component.translatable("tooltip.wildercord.bonded_blade.deed.duel", d.arg().isEmpty() ? Component.literal("-")
				: Component.translatable("aura.wildercord.method." + d.arg().replace(':', '.')));
			case BladeRules.DEED_PEERLESS -> Component.translatable("tooltip.wildercord.bonded_blade.deed.peerless", Component.literal(BladeRules.cleanName(d.arg())));
			case BladeRules.DEED_WAY -> Component.translatable("tooltip.wildercord.bonded_blade.deed.way", Component.translatable("aura.wildercord.way." + d.arg().replace(':', '.')));
			case BladeRules.DEED_PASSED -> {
				String[] two = d.arg().split(">", 2);
				yield Component.translatable("tooltip.wildercord.bonded_blade.deed.passed", Component.literal(two[0]), Component.literal(two.length > 1 ? two[1] : ""));
			}
			case BladeRules.DEED_NAMED -> Component.translatable("tooltip.wildercord.bonded_blade.deed.named", Component.literal(BladeRules.cleanName(d.arg())));
			default -> Component.literal(d.kind());
		};
		return Component.translatable("tooltip.wildercord.bonded_blade.deed_day", d.day(), what);
	}

	/** An art's name by its id (a technique slot's as "a technique"). */
	static Component artName(String id) {
		if (id.startsWith("technique_")) {
			return Component.translatable("aura.wildercord.banner.technique");
		}
		return Component.translatable("aura.wildercord.art." + id.replace(':', '.'));
	}

	/** A small bar of {@code share} filled, in {@code color}: ten squares. */
	static Component bar(double share, int color) {
		int filled = (int) Math.round(Math.max(0, Math.min(1, share)) * 10);
		return Component.literal("■".repeat(filled)).withColor(color).append(Component.literal("■".repeat(10 - filled)).withColor(0xFF3A3450));
	}

	/** The counts the tooltip shows, by id (for the page too). */
	static Map<String, Integer> shown(BladeBond b) {
		Map<String, Integer> out = new java.util.LinkedHashMap<>();
		for (String id : BladeRules.SHOWN_COUNTS) {
			out.put(id, b.history().count(id));
		}
		return out;
	}
}
