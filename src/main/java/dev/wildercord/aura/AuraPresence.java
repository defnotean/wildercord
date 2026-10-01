package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * What the top stages and the spellblade keep about a player, beside {@link AuraAttachments} (which stays as wave one left
 * it, so the duelists and aura knights that read it see no change):
 * <ul>
 * <li>{@link #LOOK}: what everyone else's client needs to draw it (aura armour's shell and when it last took a blow, a spell
 * held on the blade). Not saved, synced to everyone nearby;</li>
 * <li>{@link #TIMERS}: when Aura Step and Dominion are ready again and when a Dominion ends. Saved (game time carries over a
 * restart, so leaving and coming back never resets a long cooldown) and kept through death, synced to its owner only.</li>
 * </ul>
 */
public final class AuraPresence {
	private AuraPresence() {}

	/**
	 * How a player's top-stage aura looks to everyone.
	 *
	 * @param shell         whether aura armour is up (Form, with aura enough): a faint shell of aura round the body
	 * @param shellStruckAt when the shell last took a blow (it flares), or -1
	 * @param bladeSpell    the colour of a spell riding the blade (0xRRGGBB), or 0 while none is
	 * @param bladeUntil    when that spell slips off the blade, or -1
	 */
	public record Look(boolean shell, long shellStruckAt, int bladeSpell, long bladeUntil) {
		public static final Look NONE = new Look(false, -1, 0, -1);

		public static final StreamCodec<ByteBuf, Look> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, Look::shell, ByteBufCodecs.VAR_LONG, Look::shellStruckAt, ByteBufCodecs.INT, Look::bladeSpell,
			ByteBufCodecs.VAR_LONG, Look::bladeUntil, Look::new);

		/** Whether a spell rides the blade at {@code now}. */
		public boolean spellHeld(long now) {
			return bladeSpell != 0 && now <= bladeUntil;
		}

		public Look withShell(boolean on) {
			return new Look(on, shellStruckAt, bladeSpell, bladeUntil);
		}

		public Look struck(long at) {
			return new Look(shell, at, bladeSpell, bladeUntil);
		}

		public Look blade(int color, long until) {
			return new Look(shell, shellStruckAt, color, until);
		}
	}

	public static final AttachmentType<Look> LOOK = AttachmentRegistry.create(
		Wildercord.id("aura_presence"),
		builder -> builder.syncWith(Look.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	/**
	 * A player's top-stage timers, in the server's game time.
	 *
	 * @param stepReadyAt     when Aura Step is ready again
	 * @param dominionReadyAt when Dominion is ready again
	 * @param dominionUntil   when the Dominion raised last ends (past: none is up)
	 */
	public record Timers(long stepReadyAt, long dominionReadyAt, long dominionUntil) {
		public static final Timers NONE = new Timers(0, 0, 0);

		public static final Codec<Timers> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.optionalFieldOf("step_ready_at", 0L).forGetter(Timers::stepReadyAt),
			Codec.LONG.optionalFieldOf("dominion_ready_at", 0L).forGetter(Timers::dominionReadyAt),
			Codec.LONG.optionalFieldOf("dominion_until", 0L).forGetter(Timers::dominionUntil)
		).apply(i, Timers::new));

		public static final StreamCodec<ByteBuf, Timers> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_LONG, Timers::stepReadyAt, ByteBufCodecs.VAR_LONG, Timers::dominionReadyAt, ByteBufCodecs.VAR_LONG, Timers::dominionUntil,
			Timers::new);

		public Timers stepReady(long at) {
			return new Timers(at, dominionReadyAt, dominionUntil);
		}

		public Timers dominion(long readyAt, long until) {
			return new Timers(stepReadyAt, readyAt, until);
		}
	}

	public static final AttachmentType<Timers> TIMERS = AttachmentRegistry.create(
		Wildercord.id("aura_timers"),
		builder -> builder
			.initializer(() -> Timers.NONE)
			.persistent(Timers.CODEC)
			.syncWith(Timers.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	public static Look look(Player player) {
		return player.getAttachedOrElse(LOOK, Look.NONE);
	}

	public static Timers timers(Player player) {
		return player.getAttachedOrElse(TIMERS, Timers.NONE);
	}

	/** Sets the look (only when it changes, so nothing is sent while it doesn't). */
	static void look(ServerPlayer player, Look look) {
		if (!look.equals(look(player))) {
			if (look.equals(Look.NONE)) {
				player.removeAttached(LOOK);
			} else {
				player.setAttached(LOOK, look);
			}
		}
	}

	static void timers(ServerPlayer player, Timers timers) {
		if (!timers.equals(timers(player))) {
			player.setAttached(TIMERS, timers);
		}
	}

	public static void init() {}
}
