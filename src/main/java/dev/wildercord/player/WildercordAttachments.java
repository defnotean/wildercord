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

	/**
	 * The casting gear in the gear slots (slot id to the piece; see {@code gear.GearSlot}; a slot with
	 * nothing in it isn't listed). Synced to everyone who can see the wearer, who see it on them. Unlike the
	 * Cord it isn't kept through death: it drops like the rest of the inventory, or is carried over with it
	 * under keepInventory (see {@code gear.GearSlots}).
	 */
	public static final AttachmentType<Map<String, ItemStack>> GEAR = AttachmentRegistry.create(
		Wildercord.id("gear"),
		builder -> builder
			.initializer(Map::of)
			.persistent(Codec.unboundedMap(Codec.STRING, ItemStack.OPTIONAL_CODEC))
			.syncWith(ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ItemStack.OPTIONAL_STREAM_CODEC), AttachmentSyncPredicate.all())
	);

	/**
	 * The backpack in the Backpack slot (see {@code backpack.Backpacks}), with everything in it. Saved with the
	 * player and synced only to them: what's inside is nobody else's business. Like the gear it isn't kept
	 * through death: it drops, or is carried over under keepInventory.
	 */
	public static final AttachmentType<ItemStack> BACKPACK = AttachmentRegistry.create(
		Wildercord.id("backpack"),
		builder -> builder
			.persistent(ItemStack.OPTIONAL_CODEC)
			.syncWith(ItemStack.OPTIONAL_STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/**
	 * How the worn backpack looks: the bare item and its colour, none of its contents. Synced to everyone who
	 * can see the wearer, who see it on their back. Saved alongside {@link #BACKPACK}, and always set with it.
	 */
	public static final AttachmentType<ItemStack> BACKPACK_LOOK = AttachmentRegistry.create(
		Wildercord.id("backpack_look"),
		builder -> builder
			.persistent(ItemStack.OPTIONAL_CODEC)
			.syncWith(ItemStack.OPTIONAL_STREAM_CODEC, AttachmentSyncPredicate.all())
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

	/**
	 * Game time at which each spell is ready again. Synced for the HUD's cooldown ring. Saved and
	 * kept through death, so logging out and back in doesn't reset a long cooldown.
	 */
	public static final AttachmentType<List<Long>> COOLDOWNS = AttachmentRegistry.create(
		Wildercord.id("cooldowns"),
		builder -> builder
			.initializer(() -> List.of(0L, 0L, 0L, 0L))
			.persistent(Codec.LONG.listOf())
			.copyOnDeath()
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

	/**
	 * Casts per element, as leaning counted them before affinities existed. No longer counted: read once,
	 * when a caster first gets affinities, to give them a start (see {@code cast.PlayerAffinities}).
	 */
	public static final AttachmentType<Map<String, Integer>> ELEMENT_CASTS = AttachmentRegistry.create(
		Wildercord.id("element_casts"),
		builder -> builder
			.initializer(Map::of)
			.persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
			.syncWith(ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/**
	 * A player's affinity points with each element (element to points; see {@code spell.PlayerAffinity}).
	 * Synced for the Grimoire page, the readout and the cost the HUD shows. Kept through death.
	 */
	public static final AttachmentType<Map<String, Integer>> AFFINITY = AttachmentRegistry.create(
		Wildercord.id("affinity"),
		builder -> builder
			.persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
			.syncWith(ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/** The points each way of earning affinity has offered today, and which in-game day that is: its daily allowance. */
	public record AffinityTally(long day, Map<String, Double> earned) {
		public static final AffinityTally NONE = new AffinityTally(Long.MIN_VALUE, Map.of());
		public static final Codec<AffinityTally> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("day").forGetter(AffinityTally::day),
			Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).fieldOf("earned").forGetter(AffinityTally::earned)
		).apply(i, AffinityTally::new));

		/** What {@code key} has offered on {@code today} (nothing, if this tally is from another day). */
		public double earned(long today, String key) {
			return day == today ? earned.getOrDefault(key, 0.0) : 0.0;
		}

		/** This tally with {@code key} at {@code value} on {@code today}, starting afresh on a new day. */
		public AffinityTally with(long today, String key, double value) {
			Map<String, Double> next = new HashMap<>(day == today ? earned : Map.of());
			next.put(key, value);
			return new AffinityTally(today, Map.copyOf(next));
		}
	}

	/** The day's affinity allowances (server only: nobody else needs them). Saved and kept through death, so neither resets them. */
	public static final AttachmentType<AffinityTally> AFFINITY_TALLY = AttachmentRegistry.create(
		Wildercord.id("affinity_tally"),
		builder -> builder
			.initializer(() -> AffinityTally.NONE)
			.persistent(AffinityTally.CODEC)
			.copyOnDeath()
	);

	/**
	 * The rank each rune has reached at the Fusion Altar (rune id to 2 or 3; a rune not listed is rank I).
	 * Synced so the Cord screen and tooltips can show it. Kept through death.
	 */
	public static final AttachmentType<Map<String, Integer>> RUNE_RANKS = AttachmentRegistry.create(
		Wildercord.id("rune_ranks"),
		builder -> builder
			.initializer(Map::of)
			.persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
			.syncWith(ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/**
	 * When the player last attuned in each land (attunement id to game time), so a land gives its rune
	 * once a day. Synced so the Grimoire can show when each land is ready again. Kept through death.
	 */
	public static final AttachmentType<Map<String, Long>> ATTUNED_AT = AttachmentRegistry.create(
		Wildercord.id("attuned_at"),
		builder -> builder
			.initializer(Map::of)
			.persistent(Codec.unboundedMap(Codec.STRING, Codec.LONG))
			.syncWith(ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG), AttachmentSyncPredicate.targetOnly())
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
	 * everyone nearby, who draw the spell's readable circle growing in front of the caster, read its
	 * incantation rising from them, and see its overchannel (see {@code spell.Overchannel}): the ticks it
	 * takes to fill ({@code full}, fixed as it began), the stages the caster's heart can hold
	 * ({@code stages}, 0 when overchannel is off), the stage it has reached and when
	 * ({@code stageTime}), and whether the server counts a traced glyph ({@code traceable}).
	 */
	public record Charge(int spell, long start, List<String> runes, int full, int stages, int stage, long stageTime, boolean traceable) {
		public static final StreamCodec<ByteBuf, Charge> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Charge::spell, ByteBufCodecs.VAR_LONG, Charge::start,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), Charge::runes, ByteBufCodecs.VAR_INT, Charge::full,
			ByteBufCodecs.VAR_INT, Charge::stages, ByteBufCodecs.VAR_INT, Charge::stage, ByteBufCodecs.VAR_LONG, Charge::stageTime,
			ByteBufCodecs.BOOL, Charge::traceable, Charge::new);

		/** A plain charge: the usual time to fill, no overchannel, no tracing. */
		public Charge(int spell, long start, List<String> runes) {
			this(spell, start, runes, dev.wildercord.cast.Charging.FULL, 0, 0, start, false);
		}

		/** This charge at {@code stage}, reached at {@code time}. */
		public Charge withStage(int stage, long time) {
			return new Charge(spell, start, runes, full, stages, stage, time, traceable);
		}
	}

	/**
	 * A Shield: the mana a spell must cost to break it, the game time it ends, its colour, and the runes
	 * of the spell that raised it (its magic circle is that spell's). Synced to everyone nearby, who
	 * draw its circle; not saved (it lasts seconds).
	 */
	public record SpellShield(float strength, long until, int color, List<String> runes) {
		public static final StreamCodec<ByteBuf, SpellShield> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.FLOAT, SpellShield::strength, ByteBufCodecs.VAR_LONG, SpellShield::until, ByteBufCodecs.INT, SpellShield::color,
			ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), SpellShield::runes, SpellShield::new);

		public SpellShield {
			runes = List.copyOf(runes.size() > 16 ? runes.subList(0, 16) : runes);
		}
	}

	public static final AttachmentType<SpellShield> SPELL_SHIELD = AttachmentRegistry.create(
		Wildercord.id("spell_shield"),
		builder -> builder.syncWith(SpellShield.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	/** On an arrow fired from an imbued bow: the spell it releases where it lands, and its colour. Server only. */
	public record ImbuedShot(List<String> runes, int color) {}

	public static final AttachmentType<ImbuedShot> IMBUED_SHOT = AttachmentRegistry.create(Wildercord.id("imbued_shot"), builder -> {});

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
		// Kept through death with the Cord itself, so it doesn't vanish from a respawned player for a moment.
		builder -> builder.syncWith(CordLook.STREAM_CODEC, AttachmentSyncPredicate.all()).copyOnDeath()
	);

	/**
	 * On anything a spell holds still (Stasis, Infinity's frozen projectiles): whether it had no
	 * gravity before. Saved, so something held when it was saved (a player logging out, a server
	 * stopping) gets its gravity back when it loads, instead of floating forever.
	 */
	public static final AttachmentType<Boolean> HELD_GRAVITY = AttachmentRegistry.create(
		Wildercord.id("held_gravity"),
		builder -> builder.persistent(Codec.BOOL)
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

	/**
	 * The game time a player's spellguard last held (absent: it never has), so it recharges from then (see
	 * {@code cast.SpellDefence}). Saved, so leaving and coming back doesn't recharge it; synced, for the Cord screen's
	 * readout. Not kept through death: a player who respawns starts with it ready.
	 */
	public static final AttachmentType<Long> SPELLGUARD = AttachmentRegistry.create(
		Wildercord.id("spellguard"),
		builder -> builder
			.persistent(Codec.LONG)
			.syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.targetOnly())
);

	/**
	 * A flight from Soar (see {@code cast.Soar}): the game time it ends (or, once it has, the game time its
	 * gentle descent stops guarding the faller), the flying speed the player had before it, and whether it's
	 * over and they're coming down. Its being here before it's over is the note that Soar gave the flight, so
	 * Soar only ever takes back a flight it gave. Saved, so a flight caught by a logout, a restart or a crash is
	 * picked up or tidied away as the player logs in; not kept through death; synced to everyone nearby, who
	 * draw the wings.
	 */
	public record Soaring(long until, float speed, boolean falling) {
		public static final Codec<Soaring> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("until").forGetter(Soaring::until),
			Codec.FLOAT.fieldOf("speed").forGetter(Soaring::speed),
			Codec.BOOL.fieldOf("falling").forGetter(Soaring::falling)
		).apply(i, Soaring::new));
		public static final StreamCodec<ByteBuf, Soaring> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_LONG, Soaring::until, ByteBufCodecs.FLOAT, Soaring::speed, ByteBufCodecs.BOOL, Soaring::falling, Soaring::new);
	}

	public static final AttachmentType<Soaring> SOARING = AttachmentRegistry.create(
		Wildercord.id("soaring"),
		builder -> builder
			.persistent(Soaring.CODEC)
			.syncWith(Soaring.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	/**
	 * When a player's wings have rested after a Soar flight: the game time Soar may lift them again (a flight
	 * that ends for any reason but death rests them for 30 seconds; see {@code cast.SoarRules#REST_TICKS}).
	 * Saved, so logging out doesn't cut it short, and kept through death, so dying doesn't either. Server only.
	 */
	public static final AttachmentType<Long> SOAR_REST = AttachmentRegistry.create(
		Wildercord.id("soar_rest"),
		builder -> builder.persistent(Codec.LONG).copyOnDeath()
	);

	/**
	 * Reading runes (see {@code spell.RuneReading}): how far the player is with each rune they're still reading (rune id to
	 * progress). A rune not listed is understood, so every rune known before reading existed, and every starter rune,
	 * needs nothing here; an entry is dropped the moment its rune is understood. Saved, synced for the Codex, kept
	 * through death.
	 */
	public static final AttachmentType<Map<String, Integer>> RUNE_READING = AttachmentRegistry.create(
		Wildercord.id("rune_reading"),
		builder -> builder
			.initializer(Map::of)
			.persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
			.syncWith(ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/**
	 * What this player may know of their world's own magic (see {@code spell.ResonanceLore}, which decides it): the
	 * resonances they found, read the riddle of, or heard announced, the quirks they've met, and how many resonances the
	 * world holds. Worked out by the server from the world and the player's Grimoire whenever either changes (and at
	 * login), so it's never saved: a client is never sent anything else.
	 */
	public record WorldLore(List<dev.wildercord.spell.ResonanceLore.View> resonances, List<dev.wildercord.spell.ResonanceLore.QuirkView> quirks, int total) {
		public static final WorldLore NONE = new WorldLore(List.of(), List.of(), 0);

		public WorldLore {
			resonances = List.copyOf(resonances);
			quirks = List.copyOf(quirks);
		}

		private static final StreamCodec<ByteBuf, dev.wildercord.spell.ResonanceLore.View> VIEW = StreamCodec.of(
			(buf, view) -> {
				ByteBufCodecs.STRING_UTF8.encode(buf, view.id());
				ByteBufCodecs.STRING_UTF8.encode(buf, view.name());
				ByteBufCodecs.STRING_UTF8.encode(buf, view.riddle());
				ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(8)).encode(buf, view.runes());
				ByteBufCodecs.STRING_UTF8.encode(buf, view.twist());
				ByteBufCodecs.INT.encode(buf, view.color());
				ByteBufCodecs.BOOL.encode(buf, view.found());
				ByteBufCodecs.BOOL.encode(buf, view.hinted());
				ByteBufCodecs.STRING_UTF8.encode(buf, view.finder());
			},
			buf -> new dev.wildercord.spell.ResonanceLore.View(ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
				ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(8)).decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
				ByteBufCodecs.INT.decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf)));
		private static final StreamCodec<ByteBuf, dev.wildercord.spell.ResonanceLore.QuirkView> QUIRK = StreamCodec.composite(
			ByteBufCodecs.STRING_UTF8, dev.wildercord.spell.ResonanceLore.QuirkView::id, ByteBufCodecs.STRING_UTF8, dev.wildercord.spell.ResonanceLore.QuirkView::text,
			dev.wildercord.spell.ResonanceLore.QuirkView::new);
		public static final StreamCodec<ByteBuf, WorldLore> STREAM_CODEC = StreamCodec.composite(
			VIEW.apply(ByteBufCodecs.list(64)), WorldLore::resonances, QUIRK.apply(ByteBufCodecs.list(64)), WorldLore::quirks, ByteBufCodecs.VAR_INT, WorldLore::total,
			WorldLore::new);
	}

	public static final AttachmentType<WorldLore> WORLD_LORE = AttachmentRegistry.create(
		Wildercord.id("world_lore"),
		builder -> builder
			.initializer(() -> WorldLore.NONE)
			.syncWith(WorldLore.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	public static void init() {}
}


