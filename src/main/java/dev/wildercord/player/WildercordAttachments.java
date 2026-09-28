package dev.wildercord.player;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Per-player state. Saved with the player and synced only to that player. */
public final class WildercordAttachments {
	private WildercordAttachments() {}

	/** The Cord worn in the Cord slot. Kept through death. */
	public static final AttachmentType<ItemStack> CORD = AttachmentRegistry.create(
		Wildercord.id("cord"),
		builder -> builder
			.initializer(() -> ItemStack.EMPTY)
			.persistent(ItemStack.OPTIONAL_CODEC)
			.syncWith(ItemStack.OPTIONAL_STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** Learned runes and threaded spells. Kept through death. */
	public static final AttachmentType<Spellbook> SPELLBOOK = AttachmentRegistry.create(
		Wildercord.id("spellbook"),
		builder -> builder
			.initializer(() -> Spellbook.EMPTY)
			.persistent(Spellbook.CODEC)
			.syncWith(Spellbook.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** Current mana. Refills from zero after a respawn. */
	public static final AttachmentType<Float> MANA = AttachmentRegistry.create(
		Wildercord.id("mana"),
		builder -> builder
			.initializer(() -> 0.0F)
			.persistent(Codec.FLOAT)
			.syncWith(ByteBufCodecs.FLOAT, AttachmentSyncPredicate.targetOnly())
	);

	/** Game time at which each spell is ready again. Synced for the HUD's cooldown ring. */
	public static final AttachmentType<List<Long>> COOLDOWNS = AttachmentRegistry.create(
		Wildercord.id("cooldowns"),
		builder -> builder
			.initializer(() -> List.of(0L, 0L, 0L, 0L))
			.syncWith(ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), AttachmentSyncPredicate.targetOnly())
	);

	/** Mana Crystals used: each is +10 max mana, forever. */
	public static final AttachmentType<Integer> CRYSTALS = AttachmentRegistry.create(
		Wildercord.id("crystals"),
		builder -> builder
			.initializer(() -> 0)
			.persistent(Codec.INT)
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** Heart Circles formed (0 to 8). Kept through death. */
	public static final AttachmentType<Integer> CIRCLES = AttachmentRegistry.create(
		Wildercord.id("circles"),
		builder -> builder
			.initializer(() -> 0)
			.persistent(Codec.INT)
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** Mana spent on spells, in total: it condenses toward the next circle. Kept through death. */
	public static final AttachmentType<Integer> CONDENSED = AttachmentRegistry.create(
		Wildercord.id("condensed"),
		builder -> builder
			.initializer(() -> 0)
			.persistent(Codec.INT)
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** Monsters defeated with spells, in total (a breakthrough for the 4th, 6th, 7th and 8th Circles). Kept through death. */
	public static final AttachmentType<Integer> SPELL_KILLS = AttachmentRegistry.create(
		Wildercord.id("spell_kills"),
		builder -> builder
			.initializer(() -> 0)
			.persistent(Codec.INT)
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** Has helped slay a boss (the 7th Circle's breakthrough). Kept through death. */
	public static final AttachmentType<Boolean> BOSS_SLAIN = AttachmentRegistry.create(
		Wildercord.id("boss_slain"),
		builder -> builder
			.initializer(() -> false)
			.persistent(Codec.BOOL)
			.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** Whether the player is meditating (sneaking and still). Worked out by the server. */
	public static final AttachmentType<Boolean> MEDITATING = AttachmentRegistry.create(
		Wildercord.id("meditating"),
		builder -> builder
			.initializer(() -> false)
			.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly())
	);

	/** On a summoned spirit wolf: the game time it fades. Saved, so a reload can't make it permanent. */
	public static final AttachmentType<Long> SPIRIT_UNTIL = AttachmentRegistry.create(
		Wildercord.id("spirit_until"),
		builder -> builder.persistent(Codec.LONG)
	);

	/** On a mob frozen by Freeze: the game time its AI comes back. Saved, so a reload can't leave it frozen. */
	public static final AttachmentType<Long> FROZEN_UNTIL = AttachmentRegistry.create(
		Wildercord.id("frozen_until"),
		builder -> builder.persistent(Codec.LONG)
	);

	/** The Grimoire: every reaction, secret spell, feat and hint discovered (see {@code spell.Feats}). Kept through death. */
	public static final AttachmentType<List<String>> GRIMOIRE = AttachmentRegistry.create(
		Wildercord.id("grimoire"),
		builder -> builder
			.initializer(List::of)
			.persistent(Codec.STRING.listOf())
			.syncWith(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** The innate rune awakened at the 1st Circle (a rune id), or empty before then. Kept through death. */
	public static final AttachmentType<String> INNATE = AttachmentRegistry.create(
		Wildercord.id("innate"),
		builder -> builder
			.initializer(() -> "")
			.persistent(Codec.STRING)
			.syncWith(ByteBufCodecs.STRING_UTF8, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** Casts per element, for elemental leaning. Kept through death. */
	public static final AttachmentType<Map<String, Integer>> ELEMENT_CASTS = AttachmentRegistry.create(
		Wildercord.id("element_casts"),
		builder -> builder
			.initializer(Map::of)
			.persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
			.syncWith(ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** Runebound slain (a breakthrough for the 6th Circle). Kept through death. */
	public static final AttachmentType<Integer> RUNEBOUND_SLAIN = AttachmentRegistry.create(
		Wildercord.id("runebound_slain"),
		builder -> builder
			.initializer(() -> 0)
			.persistent(Codec.INT)
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** Circles cracked by overcasting, and the game time they mend. Kept through death. */
	public record Cracks(int count, long until) {
		public static final Cracks NONE = new Cracks(0, 0);
		public static final Codec<Cracks> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("count").forGetter(Cracks::count),
			Codec.LONG.fieldOf("until").forGetter(Cracks::until)
		).apply(i, Cracks::new));
		public static final StreamCodec<ByteBuf, Cracks> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Cracks::count, ByteBufCodecs.VAR_LONG, Cracks::until, Cracks::new);

		public int active(long now) {
			return now < until ? count : 0;
		}
	}

	public static final AttachmentType<Cracks> CRACKS = AttachmentRegistry.create(
		Wildercord.id("cracks"),
		builder -> builder
			.initializer(() -> Cracks.NONE)
			.persistent(Cracks.CODEC)
			.syncWith(Cracks.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** Rhythm: casts chained on the beat, and the window in which the next one counts. */
	public record Rhythm(int stacks, long windowStart, long windowEnd) {
		public static final Rhythm NONE = new Rhythm(0, 0, 0);
		public static final StreamCodec<ByteBuf, Rhythm> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Rhythm::stacks, ByteBufCodecs.VAR_LONG, Rhythm::windowStart, ByteBufCodecs.VAR_LONG, Rhythm::windowEnd, Rhythm::new);
	}

	public static final AttachmentType<Rhythm> RHYTHM = AttachmentRegistry.create(
		Wildercord.id("rhythm"),
		builder -> builder
			.initializer(() -> Rhythm.NONE)
			.syncWith(Rhythm.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/**
	 * A spell being charged: which one, when charging began, and its runes' ids in order. Synced to
	 * everyone nearby, who draw the spell's readable circle growing in front of the caster.
	 */
	public record Charge(int spell, long start, List<String> runes) {
		public static final StreamCodec<ByteBuf, Charge> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Charge::spell, ByteBufCodecs.VAR_LONG, Charge::start,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), Charge::runes, Charge::new);
	}

	public static final AttachmentType<Charge> CHARGE = AttachmentRegistry.create(
		Wildercord.id("charge"),
		builder -> builder.syncWith(Charge.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	/**
	 * How a player's worn Cord looks to others: its tier ("" for none) and a colour per rune of the
	 * selected spell, for the beads. Kept up to date by {@code cast.CordLook}; synced to everyone nearby.
	 */
	public record CordLook(String tier, List<Integer> beads) {
		public static final CordLook NONE = new CordLook("", List.of());
		public static final StreamCodec<ByteBuf, CordLook> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, CordLook::tier, ByteBufCodecs.INT.apply(ByteBufCodecs.list(12)), CordLook::beads, CordLook::new);
	}

	public static final AttachmentType<CordLook> CORD_LOOK = AttachmentRegistry.create(
		Wildercord.id("cord_look"),
		builder -> builder.syncWith(CordLook.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	/** The spell just cast, for its casting pose: the shape's id and the game time it went off. Synced to everyone nearby. */
	public record CastPose(String shape, long start) {
		public static final StreamCodec<ByteBuf, CastPose> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, CastPose::shape, ByteBufCodecs.VAR_LONG, CastPose::start, CastPose::new);
		/** How long a casting pose lasts, in ticks. */
		public static final int TICKS = 12;
	}

	public static final AttachmentType<CastPose> CAST_POSE = AttachmentRegistry.create(
		Wildercord.id("cast_pose"),
		builder -> builder.syncWith(CastPose.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	/** Standing on a ley line right now (worked out by the server every few ticks). */
	public static final AttachmentType<Boolean> ON_LEY = AttachmentRegistry.create(
		Wildercord.id("on_ley"),
		builder -> builder
			.initializer(() -> false)
			.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly())
	);

	/** Near an awake Wellstone: the game time that stops counting unless it's renewed. */
	public static final AttachmentType<Long> WELL_UNTIL = AttachmentRegistry.create(
		Wildercord.id("well_until"),
		builder -> builder
			.initializer(() -> 0L)
			.syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.targetOnly())
	);

	/** On a Runebound monster: the runes of the spell it casts. Saved with the monster. */
	public static final AttachmentType<List<String>> RUNEBOUND = AttachmentRegistry.create(
		Wildercord.id("runebound"),
		builder -> builder.persistent(Codec.STRING.listOf())
	);

	/**
	 * How a Runebound's rune marks look: its spell's colour, whether it's an Adept, and the game
	 * time its telegraphed cast lands (0 when none is coming), so the marks flare while it telegraphs.
	 */
	public record RuneMarks(int color, boolean adept, long castAt) {
		public static final StreamCodec<ByteBuf, RuneMarks> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, RuneMarks::color, ByteBufCodecs.BOOL, RuneMarks::adept, ByteBufCodecs.VAR_LONG, RuneMarks::castAt, RuneMarks::new);

		public RuneMarks casting(long at) {
			return new RuneMarks(color, adept, at);
		}
	}

	/**
	 * On a Runebound monster: its rune marks. Not saved (worked out again from its spell as it
	 * loads); synced to everyone tracking it, who draw the glowing marks on its body.
	 */
	public static final AttachmentType<RuneMarks> RUNE_MARKS = AttachmentRegistry.create(
		Wildercord.id("rune_marks"),
		builder -> builder.syncWith(RuneMarks.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	public static void init() {}
}
