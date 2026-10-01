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

/**
 * Aura's per-player state, in three parts, like spell mastery's:
 * <ul>
 * <li>{@link #AURA}: the path itself (the method, the stage, experience and the aura held). Saved, kept through death
 * (the aura held empties on a new body), synced to its owner only;</li>
 * <li>{@link #STATE}: what is happening right now (the stance, the guard, cooldowns, a trial's progress). Not saved,
 * synced to its owner only, for the HUD's beat ring and marks;</li>
 * <li>{@link #LOOK}: what everyone else's client needs to draw it (a colour, a stage, whether the blade is lit, whether
 * they're guarding or breathing). Not saved, synced to everyone nearby.</li>
 * </ul>
 * Nothing else about a player's aura leaves the server.
 */
public final class AuraAttachments {
	private AuraAttachments() {}

	/**
	 * A player's aura.
	 *
	 * @param method   the breathing method's id, or "" before one is learned
	 * @param stage    0 (none) to {@link AuraRules#MAX_STAGE}
	 * @param xp       experience (a running total, filling toward the next stage's threshold and waiting there)
	 * @param aura     the aura held now (0 to the stage's capacity); it never decays
	 * @param practice experience learned on training dummies and in the practice arena (held to {@link AuraRules#PRACTICE_CAP})
	 */
	public record Data(String method, int stage, double xp, float aura, double practice) {
		public static final Data NONE = new Data("", 0, 0, 0, 0);

		public Data {
			method = method == null ? "" : method;
			stage = AuraRules.clampStage(stage);
			xp = Math.max(0, xp);
			aura = Math.max(0, aura);
			practice = Math.max(0, practice);
		}

		public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.optionalFieldOf("method", "").forGetter(Data::method),
			Codec.INT.optionalFieldOf("stage", 0).forGetter(Data::stage),
			Codec.DOUBLE.optionalFieldOf("xp", 0.0).forGetter(Data::xp),
			Codec.FLOAT.optionalFieldOf("aura", 0.0F).forGetter(Data::aura),
			Codec.DOUBLE.optionalFieldOf("practice", 0.0).forGetter(Data::practice)
		).apply(i, Data::new));

		public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(128), Data::method, ByteBufCodecs.VAR_INT, Data::stage, ByteBufCodecs.DOUBLE, Data::xp,
			ByteBufCodecs.FLOAT, Data::aura, ByteBufCodecs.DOUBLE, Data::practice, Data::new);

		public boolean learned() {
			return !method.isEmpty() && stage >= AuraRules.GLOW;
		}

		public Data withAura(float aura) {
			return new Data(method, stage, xp, aura, practice);
		}

		public Data withXp(double xp, double practice) {
			return new Data(method, stage, xp, aura, practice);
		}

		public Data withStage(int stage) {
			return new Data(method, stage, xp, aura, practice);
		}

		public Data withMethod(String method) {
			return new Data(method, stage, xp, aura, practice);
		}
	}

	public static final AttachmentType<Data> AURA = AttachmentRegistry.create(
		Wildercord.id("aura"),
		builder -> builder
			.initializer(() -> Data.NONE)
			.persistent(Data.CODEC)
			.syncWith(Data.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
			.copyOnDeath()
	);

	/**
	 * What a player's aura is doing now. Game times are the server's; -1 means not at all.
	 *
	 * @param settledAt    when the breathing stance took hold (the beat runs from here), or -1 while not in it
	 * @param guardRaised  when the guard went up (its perfect moment runs from here), or -1
	 * @param guardUntil   when the guard drops at the latest (it drops sooner when sneak is let go)
	 * @param slashReadyAt when Aura Slash is ready again
	 * @param backlashUntil when backlash lets go
	 * @param stillness    ticks of unbroken stillness at a place of power toward a breakthrough (0 when none is under way)
	 * @param trialUntil   when the stronger-foe trial under way runs out, or -1
	 */
	public record State(long settledAt, long guardRaised, long guardUntil, long slashReadyAt, long backlashUntil, int stillness, long trialUntil) {
		public static final State NONE = new State(-1, -1, -1, 0, 0, 0, -1);

		public static final StreamCodec<ByteBuf, State> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_LONG, State::settledAt, ByteBufCodecs.VAR_LONG, State::guardRaised, ByteBufCodecs.VAR_LONG, State::guardUntil,
			ByteBufCodecs.VAR_LONG, State::slashReadyAt, ByteBufCodecs.VAR_LONG, State::backlashUntil, ByteBufCodecs.VAR_INT, State::stillness,
			ByteBufCodecs.VAR_LONG, State::trialUntil, State::new);

		public boolean breathing() {
			return settledAt >= 0;
		}

		public boolean guarding(long now) {
			return guardRaised >= 0 && now <= guardUntil;
		}

		public State settled(long at) {
			return new State(at, guardRaised, guardUntil, slashReadyAt, backlashUntil, at < 0 ? 0 : stillness, trialUntil);
		}

		public State guard(long raised, long until) {
			return new State(settledAt, raised, until, slashReadyAt, backlashUntil, stillness, trialUntil);
		}

		public State slashReady(long at) {
			return new State(settledAt, guardRaised, guardUntil, at, backlashUntil, stillness, trialUntil);
		}

		public State backlash(long until) {
			return new State(settledAt, guardRaised, guardUntil, slashReadyAt, until, stillness, trialUntil);
		}

		public State stillness(int ticks) {
			return new State(settledAt, guardRaised, guardUntil, slashReadyAt, backlashUntil, ticks, trialUntil);
		}

		public State trial(long until) {
			return new State(settledAt, guardRaised, guardUntil, slashReadyAt, backlashUntil, stillness, until);
		}
	}

	public static final AttachmentType<State> STATE = AttachmentRegistry.create(
		Wildercord.id("aura_state"),
		builder -> builder.syncWith(State.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/**
	 * How a player's aura looks to everyone.
	 *
	 * @param color     the aura's colour now, 0xRRGGBB
	 * @param stage     its stage (the blade's look: a haze, flowing ripples, a crystal blade)
	 * @param lit       whether there's aura enough to coat a blow (an empty aura shows only faintly)
	 * @param guarding  whether the guard is up
	 * @param breathing whether they're in the breathing stance
	 */
	public record Look(int color, int stage, boolean lit, boolean guarding, boolean breathing) {
		public static final Look NONE = new Look(0, 0, false, false, false);

		public static final StreamCodec<ByteBuf, Look> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.INT, Look::color, ByteBufCodecs.VAR_INT, Look::stage, ByteBufCodecs.BOOL, Look::lit, ByteBufCodecs.BOOL, Look::guarding,
			ByteBufCodecs.BOOL, Look::breathing, Look::new);
	}

	public static final AttachmentType<Look> LOOK = AttachmentRegistry.create(
		Wildercord.id("aura_look"),
		builder -> builder.syncWith(Look.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	public static void init() {}
}
