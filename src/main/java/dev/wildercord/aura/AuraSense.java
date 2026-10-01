package dev.wildercord.aura;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.Targets;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

import java.util.ArrayList;
import java.util.List;

/**
 * Aura sense (Glow): with each breath of the breathing stance, the hostile creatures within {@link #RANGE} blocks are
 * outlined, for the breather alone, briefly (a little longer than a breath, so they stay lit while the stance holds). From
 * Form it reaches further ({@link AuraRules#SENSE_RANGE_FORM}) and pulses on its own every few seconds while in a fight,
 * stance or not. The server finds them and tells only that player which; their client draws the outlines in the aura's colour.
 */
public final class AuraSense {
	private AuraSense() {}

	/** How far aura sense reaches, in blocks, before Form. */
	public static final double RANGE = AuraRules.SENSE_RANGE;
	/** How long an outline lasts (ticks): a breath and a little more. */
	public static final int TICKS = AuraRules.BREATH_PERIOD + 14;
	/** The most creatures one breath outlines. */
	private static final int MAX = 48;

	/** Server to client: outline these entities (by network id) in {@code color} for {@code ticks}. */
	public record Sensed(List<Integer> ids, int color, int ticks) implements CustomPacketPayload {
		public static final Type<Sensed> TYPE = new Type<>(Wildercord.id("aura_sense"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Sensed> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX)), Sensed::ids, ByteBufCodecs.INT, Sensed::color, ByteBufCodecs.VAR_INT, Sensed::ticks,
			Sensed::new).cast();

		@Override
		public Type<Sensed> type() {
			return TYPE;
		}
	}

	static void init() {
		PayloadTypeRegistry.clientboundPlay().register(Sensed.TYPE, Sensed.CODEC);
	}

	/** The hostile creatures a breath would sense round {@code player} now. */
	public static List<Mob> sensed(ServerPlayer player) {
		double range = AuraRules.senseRange(Aura.stage(player));
		List<Mob> found = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(range),
			m -> m.isAlive() && !m.isInvisibleTo(player) && (m instanceof Enemy || m.getTarget() == player) && !Targets.playerPet(m)
				&& m.distanceToSqr(player) <= range * range);
		return found.size() > MAX ? found.subList(0, MAX) : found;
	}

	/** Every tick: from Form, a pulse of sense every few seconds while in a fight (the stance's own breaths aside). */
	static void combat(ServerPlayer player, long now) {
		if (Aura.stage(player) >= AuraRules.FORM && (now + player.getId()) % AuraRules.SENSE_COMBAT_PERIOD == 0 && Aura.inFight(player)
				&& !Aura.state(player).breathing()) {
			pulse(player, AuraRules.SENSE_COMBAT_PERIOD + 14);
		}
	}

	/** One breath's sense: tells the player what's out there. */
	static void pulse(ServerPlayer player) {
		pulse(player, TICKS);
	}

	private static void pulse(ServerPlayer player, int ticks) {
		if (Aura.stage(player) < AuraRules.GLOW || !ServerPlayNetworking.canSend(player, Sensed.TYPE)) {
			return;
		}
		List<Integer> ids = new ArrayList<>();
		for (Mob mob : sensed(player)) {
			ids.add(mob.getId());
		}
		ServerPlayNetworking.send(player, new Sensed(ids, Aura.color(player), ticks));
	}
}
