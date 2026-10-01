package dev.wildercord.gear;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Fx;
import dev.wildercord.content.CordTier;
import dev.wildercord.spell.SpellPlan;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Casting gear in the world: what someone has in their gear slots or, where a slot is empty, in their
 * hands, and what it does to their spells. Both sides use it (the Cord screen and HUD to show costs, the
 * server to charge them), and the server always reads the slots and hands at the moment of casting.
 */
public final class Gear {
	private Gear() {}

	/** The gear an item is, or null. */
	public static GearDef defOf(ItemStack stack) {
		return stack.getItem() instanceof CastingGearItem gear ? gear.def : null;
	}

	/**
	 * What a creature's gear does: for a player, the pieces in their gear slots (which take the place of
	 * held pieces of the same kind) and the held pieces of any kind whose slot is empty; for anything else,
	 * what's in its hands.
	 */
	public static GearBonuses of(LivingEntity holder) {
		if (holder == null) {
			return GearBonuses.NONE;
		}
		GearDef main = defOf(holder.getMainHandItem());
		GearDef off = defOf(holder.getOffhandItem());
		if (holder instanceof Player player) {
			return GearBonuses.of(GearSlots.defs(player), main, off);
		}
		return GearBonuses.of(main, off);
	}

	/** Whether the Tome of the Fifth Page counts: in its slot, or held in the off-hand while the slot is empty. */
	public static boolean tome(Player player) {
		return of(player).fifthSpell();
	}

	/** Whether spell slot {@code spell} is open: one of the Cord's, or the tome's while the tome counts. */
	public static boolean spellOpen(Player player, CordTier tier, int spell) {
		return tier != null && SpellSlots.open(tier.spells, tome(player), spell);
	}

	/** Cost multiplier from the gear, for a spell (the factor {@code Heart.manaCost} folds in). */
	public static double costFactor(Player player, SpellPlan.Segment root) {
		GearBonuses gear = of(player);
		return gear.isEmpty() ? 1.0 : gear.cost(GearBonuses.elements(root));
	}

	/** Extra max mana from the gear (Focus of the Deep Well). */
	public static int extraMana(Player player) {
		return of(player).mana();
	}

	/** How much faster a charged cast fills (Focus of Haste). */
	public static double chargeSpeed(LivingEntity holder) {
		return of(holder).chargeSpeed();
	}

	/** Rolls a Focus of Echoes: true if this cast echoes. */
	public static boolean echoes(ServerPlayer player, GearBonuses gear) {
		double chance = gear.echo();
		return chance > 0 && player.getRandom().nextDouble() < chance;
	}

	// ------------------------------------------------------------------ the charged-cast flourish

	/**
	 * A charged spell leaves a staff of one of its elements with that element's flourish, sized by the
	 * charge: flames, a ring of ice, a bolt from the sky, a gust, cracked ground, leaves, a collapsing
	 * dark, a star, a clock face or a pulse of blood.
	 */
	public static void flourish(ServerPlayer player, GearBonuses gear, Collection<String> elements, double charge) {
		List<GearDef> staffs = gear.staffsFor(elements);
		if (staffs.isEmpty() || charge < 0.2) {
			return;
		}
		ServerLevel level = player.level();
		double size = 0.6 + 0.8 * Math.min(1, charge);
		Vec3 look = player.getLookAngle();
		Vec3 tip = player.getEyePosition().add(look.scale(0.9)).add(0, -0.25, 0);
		Vec3 feet = player.position();
		for (GearDef staff : staffs) {
			boolean greater = staff.kind() == GearDef.GearKind.GREATER_STAFF;
			double s = greater ? size * 1.3 : size;
			switch (staff.element()) {
				case "fire" -> {
					ElementFx.flameBurst(level, tip, 0.6 * s, (int) (10 * s));
					ElementFx.heatFlare(level, tip, s);
				}
				case "frost" -> {
					ElementFx.shatterRing(level, feet.add(0, 0.1, 0), 1.6 * s);
					ElementFx.shards(level, tip, 0.8 * s, (int) (8 * s));
				}
				case "storm" -> {
					ElementFx.bolt(level, tip.add(look.scale(0.4)).add(0, 3.5, 0), tip, 0.12 * s, 2, 2);
					ElementFx.sparks(level, tip, (int) (8 * s), 0.3);
				}
				case "wind" -> {
					ElementFx.gustRing(level, feet.add(0, 0.1, 0), 1.8 * s);
					ElementFx.swirl(level, feet, 0.9 * s, 2.2, 3);
				}
				case "earth" -> {
					ElementFx.crack(level, feet, 1.5 * s, 30);
					ElementFx.stoneShards(level, feet.add(0, 0.2, 0), ElementFx.groundBlock(level, feet), (int) (8 * s), 0.25);
				}
				case "life" -> {
					ElementFx.leafSpiral(level, feet, 0.9 * s, 2.2, 10);
					ElementFx.petals(level, tip, 0.5 * s, (int) (8 * s));
				}
				case "void" -> {
					ElementFx.implode(level, tip, 1.0 * s, 12);
					ElementFx.blackCore(level, tip, 0.35 * s, 12);
				}
				case "arcane" -> {
					ElementFx.starSeal(level, tip, look, 0.9 * s, 18);
					ElementFx.shimmer(level, tip, 0.6 * s, (int) (10 * s));
				}
				case "time" -> {
					ElementFx.clock(level, tip, look, 0.8 * s, 20, false);
					ElementFx.goldenTicks(level, tip, 0.6 * s, (int) (8 * s));
				}
				case "blood" -> {
					ElementFx.pulse(level, tip, look, 0.9 * s);
					ElementFx.drip(level, tip, 0.4 * s, (int) (8 * s));
				}
				default -> { }
			}
			Fx.sound(level, tip, greater ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.AMETHYST_CLUSTER_HIT, 0.6F, 0.8F + 0.4F * (float) charge);
		}
	}

	// ------------------------------------------------------------------ words

	/** One line per piece that counts: what it's doing, for the Cord screen's mana badge. */
	public static List<Component> describe(GearBonuses gear) {
		List<Component> lines = new ArrayList<>();
		for (GearDef piece : gear.pieces()) {
			lines.add(Component.translatable("screen.wildercord.gear.piece", Component.translatable(itemKey(piece)), effect(piece)).withStyle(ChatFormatting.GRAY));
		}
		return lines;
	}

	/** What a piece does, in a few words. */
	public static Component effect(GearDef piece) {
		return switch (piece.kind()) {
			case STAFF, GREATER_STAFF -> Component.translatable("tooltip.wildercord.gear.staff", Component.translatable("element.wildercord." + piece.element()),
				percent(piece.elementPower() - 1), percent(1 - piece.elementCost()));
			case TOME -> Component.translatable("tooltip.wildercord.gear.tome");
			case FOCUS -> piece.chargeSpeed() > 1 ? Component.translatable("tooltip.wildercord.gear.haste", percent(piece.chargeSpeed() - 1))
				: piece == GearDef.REPRIEVE ? Component.translatable("tooltip.wildercord.gear.reprieve")
				: piece == GearDef.GROUNDING ? Component.translatable("tooltip.wildercord.gear.grounding")
				: piece == GearDef.RESOLVE ? Component.translatable("tooltip.wildercord.gear.resolve", percent(GearDef.RESOLVE_PROTECTION), percent(1 - piece.power()))
				: piece.cost() < 1 ? Component.translatable("tooltip.wildercord.gear.thrift", percent(1 - piece.cost()), percent(1 - piece.power()))
				: piece.mana() > 0 ? Component.translatable("tooltip.wildercord.gear.deep_well", piece.mana())
				: Component.translatable("tooltip.wildercord.gear.echoes", percent(piece.echo()));
		};
	}

	public static String itemKey(GearDef piece) {
		return "item.wildercord." + piece.path();
	}

	/** A fraction as a whole percentage, e.g. 0.2 as "20". */
	public static String percent(double fraction) {
		return String.format(Locale.ROOT, "%d", Math.round(fraction * 100));
	}
}
