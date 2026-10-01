package dev.wildercord.client;

import dev.wildercord.Wildercord;
import dev.wildercord.content.RuneItem;
import dev.wildercord.spell.Bestiary;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.Secrets;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import java.util.Optional;

/**
 * "New in your Grimoire": shown when a reaction, secret spell, feat, riddle or a creature's
 * weakness is first discovered, with an icon for what kind it is and the condensed mana it brought.
 * An affinity reaching a new level has one too ({@link #affinity}), with the element's mark for its icon.
 */
public class GrimoireToast implements Toast {
	private static final Identifier BACKGROUND = Wildercord.id("toast/grimoire");
	private static final long SHOW_MS = 5000;

	private final Component title;
	private final Component name;
	private final ItemStack icon;
	private final int color;
	/** The element whose mark is drawn in place of an item (an affinity's toast), or null. */
	private final String glyph;
	private final Object token;
	private Visibility visibility = Visibility.SHOW;

	private GrimoireToast(Component title, Component name, int color, String glyph, Object token) {
		this(title, name, ItemStack.EMPTY, color, glyph, token);
	}

	private GrimoireToast(Component title, Component name, ItemStack icon, int color, String glyph, Object token) {
		this.title = title;
		this.name = name;
		this.icon = icon;
		this.color = color;
		this.glyph = glyph;
		this.token = token;
	}

	/** An affinity reached {@code level}: "An affinity awakens: Frost I", or "Your affinity deepens: Frost III". */
	public static GrimoireToast affinity(String element, int level) {
		Component title = Component.translatable(level <= 1 ? "toast.wildercord.affinity_new" : "toast.wildercord.affinity");
		Component name = Component.translatable("toast.wildercord.affinity_level", Component.translatable("element.wildercord." + element), RuneItem.roman(level));
		return new GrimoireToast(title, name, RuneColors.element(element), element, token(element, level));
	}

	/**
	 * Something of this world's magic revealed: "A resonance answers: the Glasswind Rite" (with a book for its icon), or
	 * "This world's nature: Here, Shock cracks harder in the rain." The token carries the kind and name, for the tests.
	 */
	public static GrimoireToast revealed(String kind, String name, int color) {
		boolean quirk = dev.wildercord.cast.WorldResonances.Revealed.QUIRK.equals(kind);
		Component title = Component.translatable(quirk ? "toast.wildercord.quirk" : "toast.wildercord.resonance");
		String shown = name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
		ItemStack icon = new ItemStack(quirk ? Items.COMPASS : Items.AMETHYST_CLUSTER);
		return new GrimoireToast(title, Component.literal(shown), icon, color, null, kind + ":" + name);
	}

	/** The token an affinity's toast carries, so it can be found among the toasts (the game tests look for it). */
	public static String token(String element, int level) {
		return "affinity:" + element + ":" + level;
	}

	public GrimoireToast(String key) {
		this.glyph = null;
		this.token = NO_TOKEN;
		this.title =Component.translatable(key.startsWith("hint:") ? "toast.wildercord.riddle" : "toast.wildercord.grimoire");
		String id = key.substring(key.indexOf(':') + 1);
		if (key.startsWith("reaction:")) {
			this.name = Component.translatable("reaction.wildercord." + id);
			this.icon = RuneItem.stack(switch (id) {
				case "shatter" -> Runes.FROST;
				case "conduct" -> Runes.SHOCK;
				case "wildfire" -> Runes.FIRE;
				case "implode" -> Runes.GRAVITY_WELL;
				case "overload" -> Runes.PLASMA;
				case "fracture" -> Runes.PELT;
				case "blight" -> Runes.VENOM;
				case "unweave" -> Runes.PRISMATIC_BURST;
				case "rupture" -> Runes.BLEED;
				case "elapse" -> Runes.COUNTDOWN;
				default -> Runes.REPEL;
			});
			this.color = RuneColors.of(switch (id) {
				case "shatter" -> Runes.FROST;
				case "conduct" -> Runes.SHOCK;
				case "wildfire" -> Runes.FIRE;
				case "overload" -> Runes.PLASMA;
				case "fracture" -> Runes.PELT;
				case "blight" -> Runes.VENOM;
				case "unweave" -> Runes.HARM;
				case "rupture" -> Runes.BLEED;
				case "elapse" -> Runes.COUNTDOWN;
				default -> Runes.PULL;
			});
		} else if (key.startsWith("secret:")) {
			Secrets.Secret secret = Secrets.byId(id).orElse(null);
			this.name = Component.literal(secret == null ? id : secret.name());
			this.icon = new ItemStack(Items.KNOWLEDGE_BOOK);
			this.color = secret == null ? 0xF5C46A : secret.color();
		} else if (key.startsWith(dev.wildercord.spell.Fusions.KEY_PREFIX)) {
			dev.wildercord.spell.RuneDef made = Runes.get("wildercord:" + id).orElse(Runes.HARM);
			// A signature fusion (two particular runes) says so.
			this.name = Component.translatable(dev.wildercord.spell.Fusions.isSignature(made) ? "toast.wildercord.signature" : "toast.wildercord.fusion",
				RuneItem.runeName(made));
			this.icon = RuneItem.stack(made);
			this.color = RuneColors.of(made);
		} else if (key.startsWith("attune:")) {
			// A Blank Rune attuned to a land: the rune it became.
			dev.wildercord.spell.RuneDef rune = dev.wildercord.spell.Attunements.byId(id).map(dev.wildercord.spell.Attunements.Rule::rune).orElse(Runes.GROW);
			this.name = Component.translatable("toast.wildercord.attuned").append(": ").append(RuneItem.runeName(rune));
			this.icon = RuneItem.stack(rune);
			this.color = RuneColors.of(rune);
		} else if (key.startsWith(Bestiary.PREFIX)) {
			// A weakness found (the Bestiary's only announced entries): the creature, its spawn egg, the element's colour.
			Bestiary.Entry entry = Bestiary.parse(key).orElse(new Bestiary.Entry(id, Bestiary.Kind.MET, ""));
			EntityType<?> type = Optional.ofNullable(Identifier.tryParse(entry.type())).flatMap(BuiltInRegistries.ENTITY_TYPE::getOptional).orElse(null);
			Component creature = type == null ? Component.literal(entry.type()) : type.getDescription();
			this.name = Component.translatable("toast.wildercord.bestiary", creature, Component.translatable("element.wildercord." + entry.element()));
			this.icon = type == null ? new ItemStack(Items.BOOK) : SpawnEggItem.byId(type).map(ItemStack::new).orElseGet(() -> new ItemStack(Items.BOOK));
			this.color = RuneColors.element(entry.element());
		} else if (dev.wildercord.spell.FieldGuide.isKey(key)) {
			// A creature met for the first time: its name, its spawn egg, its colour in the field guide.
			String typeId = key.substring(dev.wildercord.spell.FieldGuide.PREFIX.length());
			EntityType<?> type = Optional.ofNullable(Identifier.tryParse(typeId)).flatMap(BuiltInRegistries.ENTITY_TYPE::getOptional).orElse(null);
			Component creature = type == null ? Component.literal(typeId) : type.getDescription();
			this.name = Component.translatable("toast.wildercord.creature", creature);
			this.icon = type == null ? new ItemStack(Items.BOOK) : SpawnEggItem.byId(type).map(ItemStack::new).orElseGet(() -> new ItemStack(Items.BOOK));
			this.color = dev.wildercord.spell.FieldGuide.byType(typeId).map(dev.wildercord.spell.FieldGuide.Entry::color).orElse(0xF5C46A);
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
	public Object getToken() {
		return token;
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
		if (glyph != null) {
			// The element's mark at twice its size, where an item would sit.
			g.pose().pushMatrix();
			g.pose().translate(9, 9);
			g.pose().scale(2, 2);
			ElementGlyphs.draw(g, glyph, 0, 0);
			g.pose().popMatrix();
		} else {
			g.item(icon, 8, 8);
		}
		g.text(font, title, 30, 7, 0xFFB8A8FF, false);
		String shown = font.plainSubstrByWidth(name.getString(), width() - 36);
		g.text(font, Component.literal(shown).withStyle(name.getStyle()), 30, 18, 0xFF000000 | color, false);
	}
}
