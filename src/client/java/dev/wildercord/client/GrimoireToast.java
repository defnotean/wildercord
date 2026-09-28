package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.content.RuneItem;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.Secrets;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * "New in your Grimoire": shown when a reaction, secret spell, feat or riddle is first
 * discovered, with an icon for what kind it is and the condensed mana it brought.
 */
public class GrimoireToast implements Toast {
	private static final Identifier BACKGROUND = Wildercord.id("toast/grimoire");
	private static final long SHOW_MS = 5000;

	private final Component title;
	private final Component name;
	private final ItemStack icon;
	private final int color;
	private Visibility visibility = Visibility.SHOW;

	public GrimoireToast(String key) {
		this.title = Component.translatable(key.startsWith("hint:") ? "toast.wildercord.riddle" : "toast.wildercord.grimoire");
		String id = key.substring(key.indexOf(':') + 1);
		if (key.startsWith("reaction:")) {
			this.name = Component.translatable("reaction.wildercord." + id);
			this.icon = RuneItem.stack(switch (id) {
				case "shatter" -> Runes.FROST;
				case "conduct" -> Runes.SHOCK;
				case "wildfire" -> Runes.FIRE;
				case "implode" -> Runes.GRAVITY_WELL;
				default -> Runes.REPEL;
			});
			this.color = RuneColors.of(switch (id) {
				case "shatter" -> Runes.FROST;
				case "conduct" -> Runes.SHOCK;
				case "wildfire" -> Runes.FIRE;
				default -> Runes.PULL;
			});
		} else if (key.startsWith("secret:")) {
			Secrets.Secret secret = Secrets.byId(id).orElse(null);
			this.name = Component.literal(secret == null ? id : secret.name());
			this.icon = new ItemStack(Items.KNOWLEDGE_BOOK);
			this.color = secret == null ? 0xF5C46A : secret.color();
		} else if (key.startsWith(dev.wildercord.spell.Fusions.KEY_PREFIX)) {
			dev.wildercord.spell.RuneDef made = Runes.get("wildercord:" + id).orElse(Runes.HARM);
			this.name = Component.translatable("toast.wildercord.fusion", RuneItem.runeName(made));
			this.icon = RuneItem.stack(made);
			this.color = RuneColors.of(made);
		} else if (key.startsWith("attune:")) {
			// A Blank Rune attuned to a land: the rune it became.
			dev.wildercord.spell.RuneDef rune = dev.wildercord.spell.Attunements.byId(id).map(dev.wildercord.spell.Attunements.Rule::rune).orElse(Runes.GROW);
			this.name = Component.translatable("toast.wildercord.attuned").append(": ").append(RuneItem.runeName(rune));
			this.icon = RuneItem.stack(rune);
			this.color = RuneColors.of(rune);
		} else if (key.startsWith("hint:")) {
			this.name = Component.translatable("toast.wildercord.riddle_hint");
			this.icon = new ItemStack(dev.wildercord.content.WildercordItems.TORN_PAGE);
			this.color = 0xE8D8B0;
		} else {
			this.name = Component.literal(Feats.feat(id).name());
			this.icon = featIcon(id);
			this.color = 0xF5C46A;
		}
	}

	private static ItemStack featIcon(String id) {
		return switch (id) {
			case Feats.ARCHIVIST -> new ItemStack(Items.ENCHANTED_BOOK);
			case Feats.CINDER_WARDEN -> new ItemStack(dev.wildercord.content.dungeons.DungeonItems.CINDER_HEART);
			case Feats.STAR_EATER -> new ItemStack(dev.wildercord.content.dungeons.DungeonItems.ASTRAL_LENS);
			case Feats.TIDE_SCRIBE -> new ItemStack(dev.wildercord.content.dungeons.DungeonItems.DROWNED_QUILL);
			case Feats.RUNEBOUND -> RuneItem.stack(Runes.HARM);
			case Feats.LEY_LINE, Feats.WELLSTONE -> new ItemStack(dev.wildercord.content.WildercordBlocks.WELLSTONE);
			case Feats.SEAL -> new ItemStack(dev.wildercord.content.WildercordBlocks.RUNE_SEAL);
			case Feats.SCROLL -> new ItemStack(dev.wildercord.content.WildercordItems.SPELL_SCROLL);
			case Feats.OVERCAST -> new ItemStack(Items.AMETHYST_SHARD);
			case Feats.RHYTHM -> new ItemStack(Items.NOTE_BLOCK);
			case Feats.CLASH -> RuneItem.stack(Runes.DOMAIN);
			case Feats.UNISON -> RuneItem.stack(Runes.RESONANCE);
			case Feats.COLLISION -> RuneItem.stack(Runes.BOLT);
			case Feats.INNATE -> new ItemStack(Items.HEART_OF_THE_SEA);
			case Feats.UPGRADE, Feats.COMBINE -> new ItemStack(dev.wildercord.content.WildercordBlocks.FUSION_ALTAR);
			case Feats.KNOT -> new ItemStack(Items.STRING);
			default -> new ItemStack(dev.wildercord.content.WildercordItems.MANA_CRYSTAL);
		};
	}

	@Override
	public Visibility getWantedVisibility() {
		return visibility;
	}

	@Override
	public void update(ToastManager manager, long fullyVisibleForMs) {
		visibility = fullyVisibleForMs >= SHOW_MS * manager.getNotificationDisplayTimeMultiplier() ? Visibility.HIDE : Visibility.SHOW;
	}

	@Override
	public SoundEvent getSoundEvent() {
		return SoundEvents.BOOK_PAGE_TURN;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, Font font, long fullyVisibleForMs) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, width(), height());
		g.item(icon, 8, 8);
		g.text(font, title, 30, 7, 0xFFB8A8FF, false);
		String shown = font.plainSubstrByWidth(name.getString(), width() - 36);
		g.text(font, Component.literal(shown).withStyle(name.getStyle()), 30, 18, 0xFF000000 | color, false);
	}
}
