package dev.wildercord.cast;

import dev.wildercord.config.Config;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.Inscription;
import dev.wildercord.content.ScrollSpell;
import dev.wildercord.content.SpellScrollItem;
import dev.wildercord.content.WildercordComponents;
import dev.wildercord.player.MasteryAttachments;
import dev.wildercord.player.MasteryBook;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.MasteryRules;
import dev.wildercord.spell.MasteryTraits;
import dev.wildercord.spell.RuneDef;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Inscribing a mastered spell for someone else. A spell of Adept rank or higher, inscribed onto a Spell Scroll from the
 * Cord screen, carries its traits (the ones its caster earned, not borrowed ones) and its sigil along with its runes.
 *
 * <ul>
 *   <li><b>Read aloud</b> (used), the scroll casts once, as any scroll does, with those traits and that sigil. It teaches
 *       nobody anything: a scroll's cast never earns experience.</li>
 *   <li><b>Studied</b> (used while sneaking) by someone who knows all its runes, it threads the spell into an empty row
 *       of their Cord with its name, and starts their own record of it at rank I with the traits <em>borrowed</em>:
 *       they work at once, and as the reader's own spell reaches each rank they may keep the borrowed trait or choose
 *       another.</li>
 * </ul>
 *
 * <p>Nothing can be duplicated or stacked: experience never travels; a reader who already has their own progress with
 * the spell (or who studied it already) can't study it again, so borrowed traits only ever fill the slots of a fresh
 * record, one per slot; borrowed traits can't be inscribed again; and an inscription needs Adept rank earned with the
 * spell itself.</p>
 */
public final class Inscriptions {
	private Inscriptions() {}

	static void init() {
		Inscription.init();
	}

	/**
	 * Adds the inscriber's mastery to a scroll just inscribed from Cord slot {@code spell}, if the spell is Adept or higher.
	 *
	 * @return whether it was added
	 */
	public static boolean inscribe(ServerPlayer player, List<RuneDef> runes, ItemStack scroll) {
        if (dev.wildercord.spell.ExciseRules.contains(runes)) return false;
		var settings = Config.get().mastery();
		if (!settings.enabled() || !settings.inscription() || runes.isEmpty()) {
			return false;
		}
		MasteryBook.Entry entry = MasteryAttachments.book(player).entry(Mastery.keyOf(runes)).orElse(null);
		if (entry == null || entry.rank() < MasteryRules.ADEPT) {
			return false;
		}
		scroll.set(Inscription.TYPE, new Inscription(entry.key(), entry.own(), entry.seed(), entry.rank(), player.getGameProfile().name()));
		return true;
	}

	/** A scroll being read aloud: its inscribed traits and sigil go with the cast. */
	public static void onRead(ServerPlayer player, ItemStack stack, List<RuneDef> runes, Cast cast) {
		Inscription inscription = stack.get(Inscription.TYPE);
		if (inscription == null || !Config.get().mastery().enabled() || !inscription.key().equals(Mastery.keyOf(runes))) {
			return;
		}
		Mastery.onScrollCast(player, runes, cast, inscription.traits(), inscription.rank(), inscription.seed());
	}

	/**
	 * Studies an inscribed scroll: see the class description.
	 *
	 * @return why it couldn't be studied, or null when it was (and the scroll is spent)
	 */
	public static Component study(ServerPlayer player, ItemStack stack) {
		Inscription inscription = stack.get(Inscription.TYPE);
		ScrollSpell scroll = stack.get(WildercordComponents.SCROLL);
		var settings = Config.get().mastery();
		if (inscription == null || scroll == null) {
			return refuse("message.wildercord.inscription.none");
		}
		if (!settings.enabled() || !settings.inscription()) {
			return refuse("message.wildercord.mastery.off");
		}
		List<RuneDef> runes = SpellScrollItem.runesOf(scroll);
		if (runes.size() != scroll.runes().size() || runes.isEmpty() || !inscription.key().equals(Mastery.keyOf(runes))) {
			return refuse("message.wildercord.inscription.unreadable");
		}
		CordTier tier = Spellbooks.tier(player);
		if (tier == null) {
			return refuse("message.wildercord.no_cord");
		}
		Spellbook book = Spellbooks.get(player);
		int missing = 0;
		for (RuneDef rune : runes) {
			if (!book.knows(rune.id())) {
				missing++;
			}
		}
		if (missing > 0) {
			return refuse("message.wildercord.inscription.unknown", missing);
		}
		for (RuneDef rune : runes) {
			if (!tier.holds(rune.tier())) {
				return refuse("message.wildercord.too_strong", dev.wildercord.content.RuneItem.runeName(rune),
					Component.translatable(CordTier.forRuneTier(rune.tier()).itemKey()));
			}
		}
		if (runes.size() > tier.sockets) {
			return refuse("message.wildercord.sockets_full", Component.translatable(tier.itemKey()), tier.sockets);
		}
		MasteryBook mastery = MasteryAttachments.book(player);
		MasteryBook.Entry mine = mastery.entry(inscription.key()).orElse(null);
		if (mine != null) {
			// Their own way with it stands (or a scroll already taught it): a scroll can't add to it, or stack traits on it.
			return refuse("message.wildercord.inscription.own");
		}
		int row = -1;
		for (int i = 0; i < tier.spells; i++) {
			if (book.spells().get(i).isEmpty()) {
				row = i;
				break;
			}
		}
		if (row < 0) {
			return refuse("message.wildercord.inscription.no_row");
		}
		List<String> ids = runes.stream().map(RuneDef::id).toList();
		Component problem = SpellCaster.edit(player, row, ids);
		if (problem != null) {
			return problem.copy().withStyle(ChatFormatting.RED);
		}
		if (!scroll.name().isEmpty()) {
			SpellCaster.rename(player, row, scroll.name());
		}
		long now = player.level().getGameTime();
		MasteryBook.Entry entry = MasteryBook.Entry.fresh(inscription.key(), inscription.seed(), now).taught(inscription.seed(), inscription.author());
		int slot = 0;
		for (String trait : inscription.traits()) {
			if (slot >= MasteryRules.SLOTS || MasteryTraits.get(trait).isEmpty()) {
				continue;
			}
			entry = entry.withTrait(slot++, trait, true);
		}
		player.setAttached(MasteryAttachments.MASTERY, mastery.with(entry, Mastery.threaded(player)));
		stack.consume(1, player);
		Fx.sound(player.level(), player.position(), SoundEvents.BOOK_PAGE_TURN, 1.0F, 0.8F);
		Fx.sound(player.level(), player.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.7F, 1.2F);
		String name = scroll.name().isEmpty() ? dev.wildercord.spell.SpellNames.auto(runes) : scroll.name();
		player.sendSystemMessage(Component.translatable("message.wildercord.inscription.learned", Component.literal(name).withColor(0xE8D8B0),
			inscription.author().isEmpty() ? Component.translatable("message.wildercord.inscription.someone") : Component.literal(inscription.author()),
			row + 1).withColor(0xC8B8E8));
		return null;
	}

	private static Component refuse(String key, Object... args) {
		return Component.translatable(key, args).withStyle(ChatFormatting.RED);
	}

	/** The lines an inscribed scroll's tooltip adds: its rank, its traits and how to study it. */
	public static void tooltip(ItemStack stack, Consumer<Component> lines) {
		Inscription inscription = stack.get(Inscription.TYPE);
		if (inscription == null) {
			return;
		}
		lines.accept(Component.translatable("tooltip.wildercord.inscription.rank", Component.translatable("mastery.wildercord.rank." + inscription.rank()))
			.withColor(0xE8C46A));
		List<Component> names = new ArrayList<>();
		for (String id : inscription.traits()) {
			MasteryTraits.get(id).ifPresent(t -> names.add(MasteryChoices.traitName(t)));
		}
		if (!names.isEmpty()) {
			Component joined = Component.empty();
			for (int i = 0; i < names.size(); i++) {
				if (i > 0) {
					joined = joined.copy().append(", ");
				}
				joined = joined.copy().append(names.get(i));
			}
			lines.accept(Component.translatable("tooltip.wildercord.inscription.traits", joined).withColor(0xC8B8E8));
		}
		lines.accept(Component.translatable("tooltip.wildercord.inscription.study").withStyle(ChatFormatting.DARK_AQUA));
	}
}
