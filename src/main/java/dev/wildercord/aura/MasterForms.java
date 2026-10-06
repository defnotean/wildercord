package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.world.MasterVictories;
import dev.wildercord.aura.world.MastersRules;
import dev.wildercord.cast.CastLock;
import dev.wildercord.cast.Reactions;
import dev.wildercord.cast.VoidTime;
import dev.wildercord.player.WildercordAttachments;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

import java.util.IdentityHashMap;
import java.util.Map;

/** One learned/equipped Master-form slot, separate from written techniques and fixed-release combat arts. */
public final class MasterForms {
	private MasterForms() {}
	public static final int WALL_TURN = 1;
	public record Progress(boolean learned, int equipped, long readyAt, boolean airborneUsed, boolean practiced, long recoveryUntil, long landingUntil) {
		public static final Progress NONE = new Progress(false, 0, 0, false, false);
		public Progress(boolean learned, int equipped, long readyAt, boolean airborneUsed, boolean practiced) { this(learned, equipped, readyAt, airborneUsed, practiced, 0, 0); }
		public Progress { recoveryUntil = Math.max(0, recoveryUntil); landingUntil = Math.clamp(landingUntil, 0, Math.max(0, readyAt)); equipped = learned && equipped == WALL_TURN ? WALL_TURN : 0; readyAt = Math.max(0, readyAt); }
		public static final Codec<Progress> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("learned", false).forGetter(Progress::learned),
			Codec.INT.optionalFieldOf("equipped", 0).forGetter(Progress::equipped),
			Codec.LONG.optionalFieldOf("ready_at", 0L).forGetter(Progress::readyAt),
			Codec.BOOL.optionalFieldOf("airborne_used", false).forGetter(Progress::airborneUsed),
			Codec.BOOL.optionalFieldOf("practiced", false).forGetter(Progress::practiced),
			Codec.LONG.optionalFieldOf("recovery_until", 0L).forGetter(Progress::recoveryUntil),
			Codec.LONG.optionalFieldOf("landing_until", 0L).forGetter(Progress::landingUntil)
		).apply(i, Progress::new));
		public static final StreamCodec<ByteBuf, Progress> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, Progress::learned, ByteBufCodecs.VAR_INT, Progress::equipped, ByteBufCodecs.VAR_LONG, Progress::readyAt,
			ByteBufCodecs.BOOL, Progress::airborneUsed, ByteBufCodecs.BOOL, Progress::practiced,
			ByteBufCodecs.VAR_LONG, Progress::recoveryUntil, ByteBufCodecs.VAR_LONG, Progress::landingUntil, Progress::new);
	}
	public static final AttachmentType<Progress> PROGRESS = AttachmentRegistry.create(Wildercord.id("master_forms"),
		builder -> builder.initializer(() -> Progress.NONE).persistent(Progress.CODEC).copyOnDeath()
			.syncWith(Progress.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));
	/** Remaining times are relative ticks, so the HUD is correct in worlds with different clocks. */
	public record View(long epoch, int phase, int ticks, int rest, int recovery, int commitment) {
		public static final View NONE = new View(0, 0, 0, 0, 0, 0);
		public static final StreamCodec<ByteBuf, View> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, View::epoch,
			ByteBufCodecs.VAR_INT, View::phase, ByteBufCodecs.VAR_INT, View::ticks, ByteBufCodecs.VAR_INT, View::rest,
			ByteBufCodecs.VAR_INT, View::recovery, ByteBufCodecs.VAR_INT, View::commitment, View::new);
	}
	public static final AttachmentType<View> VIEW = AttachmentRegistry.create(Wildercord.id("master_form_view"),
		builder -> builder.syncWith(View.CODEC, AttachmentSyncPredicate.targetOnly()));
	public record Action(int action, long epoch, long sequence) implements CustomPacketPayload {
		public static final Type<Action> TYPE = new Type<>(Wildercord.id("master_form_action"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Action::action,
			ByteBufCodecs.VAR_LONG, Action::epoch, ByteBufCodecs.VAR_LONG, Action::sequence, Action::new).cast();
		@Override public Type<Action> type() { return TYPE; }
	}
	/** Actual brace, accepted movement step, landing and abort events. No predicted fixed-release timeline. */
	public record Event(int entity, long epoch, long serial, int phase, int ticks, float yaw, long at) implements CustomPacketPayload {
		public static final Type<Event> TYPE = new Type<>(Wildercord.id("master_form_event"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Event> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Event::entity,
			ByteBufCodecs.VAR_LONG, Event::epoch, ByteBufCodecs.VAR_LONG, Event::serial, ByteBufCodecs.VAR_INT, Event::phase,
			ByteBufCodecs.VAR_INT, Event::ticks, ByteBufCodecs.FLOAT, Event::yaw, ByteBufCodecs.VAR_LONG, Event::at, Event::new).cast();
		@Override public Type<Event> type() { return TYPE; }
	}
	private static final class Session {
		final long epoch = ++epochs;
		final WallTurnRules.Input input = new WallTurnRules.Input();
		WallTurn active;
		long serial;
		int phase, phaseTicks, supportTicks;
		boolean completedKick;
		float yaw;
	}
	private static long epochs;
	private static final Map<ServerPlayer, Session> SESSIONS = new IdentityHashMap<>();
	public static Progress data(Player player) { return player.getAttachedOrElse(PROGRESS, Progress.NONE); }
	public static View view(Player player) { return player.getAttachedOrElse(VIEW, View.NONE); }
	public static long now(ServerPlayer player) { return player.level().getServer().overworld().getGameTime(); }
	private static boolean original(ServerPlayer player) {
		return player.isAlive() && !player.isRemoved() && player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player;
	}
	public static boolean eligibleLesson(ServerPlayer player) {
		return Aura.enabled(player) && WallTurnRules.eligible(Aura.stage(player), MasterVictories.progress(player).cleared(MastersRules.GALE));
	}
	/** Only the teacher's checked acceptance calls this; packets cannot submit a learned bit. */
	public static boolean learn(ServerPlayer player) {
		Progress p = data(player);
		if (p.learned() || !original(player) || !eligibleLesson(player)) return false;
		player.setAttached(PROGRESS, new Progress(true, p.equipped(), p.readyAt(), p.airborneUsed(), p.practiced(), p.recoveryUntil(), p.landingUntil()));
		return true;
	}
	/** Admission used again at every accepted movement step. A loss of control never creates a free escape. */
	static boolean able(ServerPlayer player) {
		return locallyAble(player) && !MasterFormMovement.occupied(player) && locallyAble(player);
	}
	/** Re-read local eligibility after external movement predicates, without invoking those predicates twice. */
	private static boolean locallyAble(ServerPlayer player) {
		var slow = player.getEffect(MobEffects.SLOWNESS);
		return original(player) && !player.isSpectator() && !player.isSleeping() && Aura.enabled(player) && eligibleLesson(player)
			&& data(player).learned() && data(player).equipped() == WALL_TURN && Aura.holdsWeapon(player)
			&& !player.isPassenger() && !player.isFallFlying() && !player.getAbilities().flying && !player.getAbilities().mayfly
			&& !player.isNoGravity() && !player.isInWater() && !player.isInLava() && !player.onClimbable() && !player.isUsingItem()
			&& !player.hasAttached(WildercordAttachments.SOARING) && player.getAttached(WildercordAttachments.CHARGE) == null
			&& !VoidTime.anchored(player) && !CastLock.locked(player) && !dev.wildercord.aura.arts.ArtWards.silenced(player)
			&& !Reactions.has(player, Reactions.Mark.FROZEN) && !Reactions.has(player, Reactions.Mark.PULLED) && !Reactions.has(player, Reactions.Mark.AIRBORNE)
			&& (slow == null || slow.getAmplifier() < 4) && !player.hasEffect(MobEffects.LEVITATION)
			&& !Stance.opened(player) && !Clashes.holding(player) && !AuraGuard.guarding(player) && !Awakening.spent(player)
			&& !MastersArts.committed(player) && !dev.wildercord.cast.ExciseCasting.committed(player) && !dev.wildercord.cast.RelayCircles.committed(player)
			&& player.containerMenu == player.inventoryMenu && Float.isFinite(player.getYRot()) && Float.isFinite(player.getXRot());
	}
	/** Only the physical brace/kick owns motion. An admitted ordinary swing/cast can retire it. */
	public static boolean ownsMotion(ServerPlayer player) { Session s = SESSIONS.get(player); return s != null && s.active != null; }
	/** Fixed Aura arts wait through descent/recovery; ordinary movement, swings and spells do not use this gate. */
	public static boolean committed(ServerPlayer player) {
		Progress p = data(player);
		return ownsMotion(player) || p.landingUntil() > 0 || p.recoveryUntil() > now(player);
	}
	/** No refund and no airborne-use reset. Damage and equipment/world lifecycle call this synchronously. */
	public static boolean cancel(ServerPlayer player) {
		Session s = SESSIONS.get(player);
		if (s == null || s.active == null) return false;
		s.active.abort();
		s.active = null;
		emit(player, s, WallTurnRules.ABORT, 5, s.yaw);
		return true;
	}
	public static boolean request(ServerPlayer player, Action packet) {
		try (var admission = dev.wildercord.cast.ActionAdmission.begin(player)) {
			return admission != null && request(player, packet, admission);
		}
	}
	private static boolean request(ServerPlayer player, Action packet, dev.wildercord.cast.ActionAdmission admission) {
		Session s = SESSIONS.get(player);
		if (!original(player) || s == null || packet.epoch() != s.epoch || !s.input.admit(packet.sequence(), packet.action(), now(player))) return false;
		if (packet.action() == WallTurnRules.CANCEL) { cancel(player); return true; }
		if (packet.action() == WallTurnRules.RELEASE) return true;
		if (packet.action() == WallTurnRules.EQUIP || packet.action() == WallTurnRules.UNEQUIP) {
			Progress p = data(player);
			if (!p.learned() || packet.action() == WallTurnRules.EQUIP && !eligibleLesson(player) || committed(player) || p.airborneUsed() || Aura.inFight(player)
				|| MastersArts.committed(player) || dev.wildercord.cast.ExciseCasting.committed(player) || dev.wildercord.cast.RelayCircles.committed(player) || MasterFormMovement.occupied(player) || player.hasAttached(WildercordAttachments.CHARGE)
				|| now(player) < p.readyAt() || !WallTurn.safeLanding(player)) return refusal(player, "equip_wait");
			if (SESSIONS.get(player) != s || !admission.valid() || !data(player).equals(p)) return false;
			player.setAttached(PROGRESS, new Progress(true, packet.action() == WallTurnRules.EQUIP ? WALL_TURN : 0,
				p.readyAt(), p.airborneUsed(), p.practiced(), p.recoveryUntil(), p.landingUntil()));
			return true;
		}
		if (player.isShiftKeyDown()) { cancel(player); return false; }
		if (s.active != null) {
			WallTurn move = s.active;
			if (move.kick() && SESSIONS.get(player) == s && s.active == move && admission.valid()) {
				emit(player, s, WallTurnRules.KICK, WallTurnRules.KICK_TICKS, move.yaw()); return true;
			}
			return false;
		}
		Progress p = data(player);
		if (!able(player) || SESSIONS.get(player) != s || !admission.valid() || committed(player)) return refusal(player, p.learned() ? "unavailable" : "locked");
		p = data(player);
		if (!WallTurnRules.canPay(Aura.aura(player), now(player), p.readyAt(), p.airborneUsed()))
			return refusal(player, p.airborneUsed() ? "landing" : now(player) < p.readyAt() ? "rest" : "aura");
		WallTurn move = WallTurn.accept(player);
		if (move == null) return refusal(player, "wall");
		// Reserve before hooks: a spend hook cannot re-enter acceptance or waive the awakened form's fixed price.
		s.active = move; s.completedKick = false; s.supportTicks = 0;
		player.setAttached(PROGRESS, new Progress(p.learned(), p.equipped(), now(player) + WallTurnRules.REST_TICKS, true, p.practiced(), p.recoveryUntil(), now(player) + WallTurnRules.REST_TICKS));
		if (Aura.spend(player, WallTurnRules.COST, "master_form:wall_turn").backlash() || !move.valid()
			|| SESSIONS.get(player) != s || s.active != move || !admission.valid()) {
			cancel(player); return false;
		}
		// Only accepted, paid motion replaces an uncommitted focus. Its original mana price and rest remain.
		if (dev.wildercord.cast.RelayCircles.pending(player)) dev.wildercord.cast.RelayCircles.cancel(player);
		move.brace();
		emit(player, s, WallTurnRules.BRACE, WallTurnRules.BRACE_TICKS, move.yaw());
		return true;
	}
	private static boolean refusal(ServerPlayer player, String reason) {
		player.sendOverlayMessage(Component.translatable("message.wildercord.wall_turn." + reason)); return false;
	}
	private static void emit(ServerPlayer player, Session s, int phase, int ticks, float yaw) {
		s.phase = phase; s.phaseTicks = ticks; s.yaw = yaw;
		Event event = new Event(player.getId(), s.epoch, ++s.serial, phase, ticks, yaw, player.level().getGameTime());
		if (player.connection == null || player.isRemoved()) return;
		for (ServerPlayer viewer : PlayerLookup.tracking(player)) if (ServerPlayNetworking.canSend(viewer, Event.TYPE)) ServerPlayNetworking.send(viewer, event);
		if (ServerPlayNetworking.canSend(player, Event.TYPE)) ServerPlayNetworking.send(player, event);
	}
	private static void tick(ServerPlayer player) {
		if (!original(player)) { retire(player); return; }
		Session s = SESSIONS.computeIfAbsent(player, ignored -> new Session());
		if (s.phaseTicks > 0) s.phaseTicks--;
		if (s.active != null) {
			WallTurn move = s.active;
			boolean advanced = move.tick();
			if (SESSIONS.get(player) != s) return;
			if (s.active == move) {
				if (!advanced) cancel(player);
				else if (move.finished()) {
					emit(player, s, WallTurnRules.KICK, 0, move.yaw());
					s.active = null; s.completedKick = true; emit(player, s, WallTurnRules.FALL, 0, s.yaw);
				}
				else if (move.kicking()) emit(player, s, WallTurnRules.KICK, move.remaining(), move.yaw());
			}
		}
		Progress p = data(player);
		boolean supported = p.airborneUsed() && s.active == null && WallTurn.safeLanding(player);
		if (SESSIONS.get(player) != s) return;
		p = data(player);
		if (supported && p.airborneUsed()) {
			if (++s.supportTicks >= 2) {
				player.setAttached(PROGRESS, new Progress(p.learned(), p.equipped(), p.readyAt(), false, p.practiced() || s.completedKick,
					now(player) + WallTurnRules.RECOVERY_TICKS, 0));
				emit(player, s, WallTurnRules.LAND, WallTurnRules.RECOVERY_TICKS, s.yaw);
				if (s.completedKick && !p.practiced()) player.sendSystemMessage(Component.translatable("message.wildercord.wall_turn.practiced"));
				s.completedKick = false;
			}
		} else s.supportTicks = 0;
		p = data(player);
		// A vehicle/flight takeover or impossible landing must not lock other Aura arts forever. The form itself
		// remains spent until real safe support, and the original paid rest is never shortened.
		if (p.landingUntil() > 0 && (now(player) >= p.landingUntil() || player.isPassenger() || player.isNoGravity()
			|| player.getAbilities().flying || player.isFallFlying())) settleRecovery(player);
		if (s.active == null && s.phaseTicks == 0 && s.phase != WallTurnRules.FALL) s.phase = WallTurnRules.IDLE;
		View view = new View(s.epoch, s.phase, s.phaseTicks, (int) Math.clamp(data(player).readyAt() - now(player), 0, WallTurnRules.REST_TICKS),
			(int) Math.clamp(data(player).recoveryUntil() - now(player), 0, WallTurnRules.RECOVERY_TICKS),
			(int) Math.clamp(data(player).landingUntil() - now(player), 0, WallTurnRules.REST_TICKS));
		if (!view.equals(view(player))) player.setAttached(VIEW, view);
	}
	private static void settleRecovery(ServerPlayer player) {
		cancel(player);
		Progress p = data(player);
		player.setAttached(PROGRESS, new Progress(p.learned(), p.equipped(), p.readyAt(), p.airborneUsed(), p.practiced(),
			Math.max(p.recoveryUntil(), now(player) + WallTurnRules.RECOVERY_TICKS), 0));
	}
	private static void retire(ServerPlayer player) {
		if (ownsMotion(player) || data(player).landingUntil() > 0) settleRecovery(player);
		else cancel(player);
		SESSIONS.remove(player); player.removeAttached(VIEW);
	}
	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(Action.TYPE, Action.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Event.TYPE, Event.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Action.TYPE, (packet, context) -> request(context.player(), packet));
		ServerTickEvents.END_SERVER_TICK.register(server -> { for (ServerPlayer player : server.getPlayerList().getPlayers()) tick(player); });
		dev.wildercord.api.AuraApi.onStanceBroken((attacker, target) -> { if (target instanceof ServerPlayer player) cancel(player); });
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (entity instanceof ServerPlayer player && MastersArtRules.interrupts(base, taken, blocked)) cancel(player);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> { if (entity instanceof ServerPlayer player) retire(player); });
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> retire(player));
		ServerPlayerEvents.AFTER_RESPAWN.register((before, after, alive) -> retire(before));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> retire(handler.player));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> { SESSIONS.clear(); MasterFormMovement.clear(); });
	}
}
