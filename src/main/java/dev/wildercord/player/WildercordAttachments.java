package dev.wildercord.player;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.List;

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

	public static void init() {}
}
