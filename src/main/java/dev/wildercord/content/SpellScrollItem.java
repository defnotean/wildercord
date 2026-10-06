package dev.wildercord.content;

import dev.wildercord.cast.Cast;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.SecretSpells;
import dev.wildercord.cast.SpellCaster;
import dev.wildercord.cast.Vfx;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RelayRules;
import dev.wildercord.spell.ReweaveRules;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.Secrets;
import dev.wildercord.spell.SpellCompiler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A Spell Scroll: one spell, inscribed from a Cord onto paper, that anyone can cast once, with
 * or without a Cord and whether or not they know its runes. Inscribing takes a sheet of paper, an
 * ink sac and twice the spell's mana. Good for trading, and for handing a friend your best spell
 * for one fight. A spell of Adept mastery or higher carries its traits and sigil, and can be studied
 * (used while sneaking) to learn it (see {@link dev.wildercord.cast.Inscriptions}).
 */
public class SpellScrollItem extends Item {
	public SpellScrollItem(Properties properties) {
		super(properties);
	}

	public static List<RuneDef> runesOf(ScrollSpell scroll) {
		if (RelayRules.containsIds(scroll.runes()) || (ReweaveRules.containsIds(scroll.runes()) || dev.wildercord.spell.ExciseRules.containsIds(scroll.runes()))) return List.of();
		List<RuneDef> runes = new ArrayList<>();
		for (String id : scroll.runes()) {
			Runes.get(id).ifPresent(runes::add);
		}
		return runes;
	}

	/** Inscribes one of the player's spells onto a new scroll. */
	public static void inscribe(ServerPlayer player, int spell) {
        if (dev.wildercord.cast.ExciseCasting.blocking(player)) return;
		CordTier tier = Spellbooks.tier(player);
		if (tier == null || !dev.wildercord.gear.Gear.spellOpen(player, tier, spell)) {
			return;
		}
		Spellbook book = Spellbooks.get(player);
		if (spell >= 0 && spell < book.spells().size() && RelayRules.containsIds(book.spells().get(spell))) {
			player.sendOverlayMessage(Component.literal(RelayRules.STORAGE_PROBLEM).withStyle(ChatFormatting.RED));
			return;
		}
		if (spell >= 0 && spell < book.spells().size() && (ReweaveRules.containsIds(book.spells().get(spell)) || dev.wildercord.spell.ExciseRules.containsIds(book.spells().get(spell)))) {
			player.sendOverlayMessage(Component.literal(dev.wildercord.spell.ExciseRules.containsIds(book.spells().get(spell)) ? dev.wildercord.spell.ExciseRules.STORAGE_PROBLEM : ReweaveRules.STORAGE_PROBLEM).withStyle(ChatFormatting.RED));
			return;
		}
		List<RuneDef> runes = SpellCaster.activeRunes(book, spell, tier);
		SpellCompiler.Compiled compiled = SpellCompiler.compile(runes);
		if (runes.isEmpty() || compiled.isEmpty()) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.spell_empty", spell + 1).withStyle(ChatFormatting.RED));
			return;
		}
		// Twice what casting it would cost this player now, as the Cord screen shows it (the server's cost multiplier,
		// discounts, a found secret's price), not the bare price of its runes.
		int cost = 2 * Math.max(1, Heart.manaCost(player, compiled, Heart.secretCost(player, runes)));
		boolean creative = player.isCreative();
		if (!creative) {
			if (!has(player, Items.PAPER) || !has(player, Items.INK_SAC) && !has(player, Items.GLOW_INK_SAC)) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.scroll_needs").withStyle(ChatFormatting.RED));
				return;
			}
			float mana = Spellbooks.mana(player);
			if (mana < cost) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.scroll_mana", (int) mana, cost).withStyle(ChatFormatting.RED));
				return;
			}
			take(player, Items.PAPER);
			if (!take(player, Items.INK_SAC)) {
				take(player, Items.GLOW_INK_SAC);
			}
			Spellbooks.setMana(player, mana - cost);
		}
		ItemStack scroll = new ItemStack(WildercordItems.SPELL_SCROLL);
		scroll.set(WildercordComponents.SCROLL, new ScrollSpell(runes.stream().map(RuneDef::id).toList(), SpellCaster.nameOf(player, book, spell, runes),
			player.getGameProfile().name()));
		// An Adept spell carries its traits and sigil with it (see cast.Inscriptions).
		boolean mastered = dev.wildercord.cast.Inscriptions.inscribe(player, runes, scroll);
		if (!player.getInventory().add(scroll)) {
			player.drop(scroll, false, net.minecraft.util.Prediction.SERVER_ONLY);
		}
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 0.8F, 1.2F);
		player.sendOverlayMessage(Component.translatable(mastered ? "message.wildercord.inscribed_mastery" : "message.wildercord.inscribed",
			SpellCaster.nameOf(player, book, spell, runes)).withColor(0xE8D8B0));
		Grimoire.feat(player, Feats.SCROLL);
	}

	private static boolean has(Player player, Item item) {
		return player.getInventory().hasAnyMatching(stack -> stack.is(item));
	}

	private static boolean take(Player player, Item item) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (stack.is(item)) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer server && (dev.wildercord.cast.ExciseCasting.blocking(server) || dev.wildercord.aura.MastersArts.committed(server))) return InteractionResult.FAIL;
		ItemStack stack = player.getItemInHand(hand);
		ScrollSpell scroll = stack.get(WildercordComponents.SCROLL);
		if (scroll == null) {
			return InteractionResult.PASS;
		}
		if (RelayRules.containsIds(scroll.runes())) {
			if (player instanceof ServerPlayer server) server.sendOverlayMessage(Component.literal(RelayRules.STORAGE_PROBLEM).withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		if ((ReweaveRules.containsIds(scroll.runes()) || dev.wildercord.spell.ExciseRules.containsIds(scroll.runes()))) {
			if (player instanceof ServerPlayer server) server.sendOverlayMessage(Component.literal(dev.wildercord.spell.ExciseRules.containsIds(scroll.runes()) ? dev.wildercord.spell.ExciseRules.STORAGE_PROBLEM : ReweaveRules.STORAGE_PROBLEM).withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		if (player instanceof ServerPlayer serverPlayer && player.isShiftKeyDown() && stack.has(Inscription.TYPE)) {
			// Studied rather than read: an inscribed scroll teaches its spell (see cast.Inscriptions).
			Component refused = dev.wildercord.cast.Inscriptions.study(serverPlayer, stack);
			if (refused != null) {
				serverPlayer.sendOverlayMessage(refused);
				return InteractionResult.FAIL;
			}
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			try (var admission = dev.wildercord.cast.ActionAdmission.begin(serverPlayer)) {
				if (admission == null) return InteractionResult.FAIL;
				List<RuneDef> runes = runesOf(scroll);
				SpellCompiler.Compiled compiled = SpellCompiler.compile(runes);
				if (runes.isEmpty() || compiled.isEmpty()) {
					return InteractionResult.FAIL;
				}
				if (dev.wildercord.cast.ExciseCasting.blocking(serverPlayer) || !dev.wildercord.cast.RelayCircles.beforeOtherSpell(serverPlayer)) return InteractionResult.FAIL;
				dev.wildercord.aura.MasterForms.cancel(serverPlayer);
				Optional<Secrets.Secret> secret = Secrets.match(runes);
				// Against a Shield a secret weighs its full price, as it does cast from a Cord.
				Cast cast = new Cast(serverPlayer, 1, Heart.Bonuses.NONE, false, null, new Cast.Info(compiled.root(), runes.size(), "", List.copyOf(runes)))
					.weigh(compiled.cost() * secret.map(Secrets.Secret::power).orElse(1.0));
				dev.wildercord.cast.Inscriptions.onRead(serverPlayer, stack, runes, cast);
				if(secret.isPresent())Vfx.castCircle(serverPlayer,Vfx.themeOf(secret.get().color()),runes);
				else dev.wildercord.cast.FormationVfx.send(cast,runes);
				dev.wildercord.cast.Scheduler.later(3, () -> {
					if (!cast.alive()) return;
					if (secret.isPresent()) SecretSpells.cast(cast, secret.get());
					else CastEngine.cast(cast, compiled.root());
				});
				level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.4F);
				stack.consume(1, player);
				player.getCooldowns().addCooldown(stack, 20);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public Component getName(ItemStack stack) {
		ScrollSpell scroll = stack.get(WildercordComponents.SCROLL);
		if (scroll == null || scroll.name().isEmpty()) {
			return super.getName(stack);
		}
		return Component.translatable("item.wildercord.spell_scroll.named", scroll.name()).withColor(0xE8D8B0);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		ScrollSpell scroll = stack.get(WildercordComponents.SCROLL);
		if (scroll == null) {
			builder.accept(Component.translatable("tooltip.wildercord.scroll_blank").withStyle(ChatFormatting.GRAY));
			return;
		}
		if ((ReweaveRules.containsIds(scroll.runes()) || dev.wildercord.spell.ExciseRules.containsIds(scroll.runes()))) {
			builder.accept(Component.literal(dev.wildercord.spell.ExciseRules.containsIds(scroll.runes()) ? dev.wildercord.spell.ExciseRules.STORAGE_PROBLEM : ReweaveRules.STORAGE_PROBLEM).withStyle(ChatFormatting.RED));
		}
		List<RuneDef> runes = runesOf(scroll);
		if (!runes.isEmpty()) {
			for (String line : SpellCompiler.compile(runes).lines()) {
				builder.accept(Component.literal(line).withStyle(ChatFormatting.GRAY));
			}
		}
		if (!scroll.author().isEmpty()) {
			builder.accept(Component.translatable("tooltip.wildercord.scroll_author", scroll.author()).withStyle(ChatFormatting.DARK_GRAY));
		}
		builder.accept(Component.translatable("tooltip.wildercord.scroll_use").withStyle(ChatFormatting.DARK_AQUA));
		dev.wildercord.cast.Inscriptions.tooltip(stack, builder);
	}

	/** An inscribed scroll shows its spell's sigil under its tooltip. */
	@Override
	public Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(ItemStack stack) {
		return Optional.ofNullable(stack.get(Inscription.TYPE));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return stack.has(WildercordComponents.SCROLL);
	}
}
