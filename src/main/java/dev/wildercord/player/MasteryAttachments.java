package dev.wildercord.player;

import dev.wildercord.Wildercord;
import dev.wildercord.spell.MasteryRules;
import dev.wildercord.spell.RuneDef;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Spell mastery's per-player state: the whole {@link MasteryBook} (private: saved, kept through death, synced to its
 * owner only) and the public {@link Look} of the spell a player has ready (synced to everyone nearby, who draw its rank
 * and sigil on its circle). Nothing else about a player's mastery ever leaves the server.
 */
public final class MasteryAttachments {
	private MasteryAttachments() {}

	/** Every spell's record. Kept through death. */
	public static final AttachmentType<MasteryBook> MASTERY = AttachmentRegistry.create(
		Wildercord.id("mastery"),
		builder -> builder
			.initializer(() -> MasteryBook.EMPTY)
			.persistent(MasteryBook.CODEC)
			.syncWith(MasteryBook.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/**
	 * How the spell a player has ready (selected, charging or just cast) looks to everyone: which spell (a hash of its key),
	 * its rank, its sigil's seed, and the looks its traits give it. Not saved: worked out again as they play.
	 */
	public record Look(long spell, int rank, long seed, int flags) {
		public static final Look NONE = new Look(0, 0, 0, 0);
		/** Flags: what its traits change about how it looks. */
		public static final int STARS = 1;
		public static final int HUE = 2;
		public static final int EMBERS = 4;
		public static final int FROST = 8;
		public static final int PETALS = 16;
		/** It charges faster (so everyone's circle opens with it). */
		public static final int QUICK = 32;

		public static final StreamCodec<ByteBuf, Look> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_LONG, Look::spell, ByteBufCodecs.VAR_INT, Look::rank, ByteBufCodecs.VAR_LONG, Look::seed, ByteBufCodecs.VAR_INT, Look::flags,
			Look::new);

		public boolean has(int flag) {
			return (flags & flag) != 0;
		}

		/** Whether this is the look of the spell made of {@code ids} (rune ids, Knots untied as they fire). */
		public boolean of(List<String> ids) {
			return rank > 0 && spell == MasteryRules.hash(MasteryRules.key(ids));
		}
	}

	public static final AttachmentType<Look> LOOK = AttachmentRegistry.create(
		Wildercord.id("mastery_look"),
		builder -> builder.syncWith(Look.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	public static MasteryBook book(Player player) {
		return player.getAttachedOrElse(MASTERY, MasteryBook.EMPTY);
	}

	/**
	 * The look {@code entity} gives the spell made of {@code runes}, or {@link Look#NONE} when it isn't the spell they
	 * have ready (or they're no player). Safe on both sides: every client knows everyone's ready spell's look.
	 */
	public static Look lookOf(Entity entity, List<RuneDef> runes) {
		if (!(entity instanceof Player player)) {
			return Look.NONE;
		}
		Look look = player.getAttachedOrElse(LOOK, Look.NONE);
		List<String> ids = dev.wildercord.spell.Knots.flatten(runes).stream().map(RuneDef::id).toList();
		return look.of(ids) ? look : Look.NONE;
	}

	public static void init() {}
}
