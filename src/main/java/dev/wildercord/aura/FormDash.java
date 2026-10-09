package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.arts.ReleasedArtOwner;
import dev.wildercord.aura.world.MasterVictories;
import dev.wildercord.world.dungeons.DungeonWards;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Cinder Lunge and Reed Slip: grounded field forms in the one Master-form slot. Each accepted step sweeps the real body and
 * keeps safe footing; a wall, ledge, hazard or ward stalls the travel. No invulnerability, and every end has a recovery.
 */
public final class FormDash {
	private FormDash() {}
	public record Progress(int learned, int equipped, long readyAt, long recoveryUntil, int practiced) {
		public static final Progress NONE = new Progress(0, 0, 0, 0, 0);
		public Progress {
			learned &= FormDashRules.KNOWN; practiced &= learned; readyAt = Math.max(0, readyAt); recoveryUntil = Math.max(0, recoveryUntil);
			equipped = (learned & FormDashRules.bit(equipped)) != 0 && FormDashRules.slot(equipped) ? equipped : 0;
		}
		public boolean knows(int form) { return (learned & FormDashRules.bit(form)) != 0; }
		public boolean drilled(int form) { return (practiced & FormDashRules.bit(form)) != 0; }
		public static final Codec<Progress> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("learned", 0).forGetter(Progress::learned),
			Codec.INT.optionalFieldOf("equipped", 0).forGetter(Progress::equipped),
			Codec.LONG.optionalFieldOf("ready_at", 0L).forGetter(Progress::readyAt),
			Codec.LONG.optionalFieldOf("recovery_until", 0L).forGetter(Progress::recoveryUntil),
			Codec.INT.optionalFieldOf("practiced", 0).forGetter(Progress::practiced)
		).apply(i, Progress::new));
		public static final StreamCodec<ByteBuf, Progress> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Progress::learned, ByteBufCodecs.VAR_INT, Progress::equipped, ByteBufCodecs.VAR_LONG, Progress::readyAt,
			ByteBufCodecs.VAR_LONG, Progress::recoveryUntil, ByteBufCodecs.VAR_INT, Progress::practiced, Progress::new);
	}
	/** Absent on old saves, which read as no field form learned. */
	public static final AttachmentType<Progress> PROGRESS = AttachmentRegistry.create(Wildercord.id("field_forms"),
		builder -> builder.initializer(() -> Progress.NONE).persistent(Progress.CODEC).copyOnDeath()
			.syncWith(Progress.STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));
	/** {@code window} counts down the follow-up moment a perfect guard opened; the form key answers it while it lasts. */
	public record View(long epoch, int form, int phase, int ticks, int rest, int recovery, int window) {
		public static final View NONE = new View(0, 0, 0, 0, 0, 0, 0);
		public static final StreamCodec<ByteBuf, View> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, View::epoch,
			ByteBufCodecs.VAR_INT, View::form, ByteBufCodecs.VAR_INT, View::phase, ByteBufCodecs.VAR_INT, View::ticks,
			ByteBufCodecs.VAR_INT, View::rest, ByteBufCodecs.VAR_INT, View::recovery, ByteBufCodecs.VAR_INT, View::window, View::new);
	}
	public static final AttachmentType<View> VIEW = AttachmentRegistry.create(Wildercord.id("field_form_view"),
		builder -> builder.syncWith(View.CODEC, AttachmentSyncPredicate.targetOnly()));
	/** Shares Wall Turn's action numbers; {@code form} only matters for EQUIP. */
	public record Action(int action, int form, long epoch, long sequence) implements CustomPacketPayload {
		public static final Type<Action> TYPE = new Type<>(Wildercord.id("field_form_action"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Action::action,
			ByteBufCodecs.VAR_INT, Action::form, ByteBufCodecs.VAR_LONG, Action::epoch, ByteBufCodecs.VAR_LONG, Action::sequence, Action::new).cast();
		@Override public Type<Action> type() { return TYPE; }
	}
	/** Accepted set, travel, end and abort. Viewers play these back; nothing is predicted. */
	public record Event(int entity, long epoch, long serial, int form, int phase, int ticks, float yaw, long at) implements CustomPacketPayload {
		public static final Type<Event> TYPE = new Type<>(Wildercord.id("field_form_event"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Event> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Event::entity,
			ByteBufCodecs.VAR_LONG, Event::epoch, ByteBufCodecs.VAR_LONG, Event::serial, ByteBufCodecs.VAR_INT, Event::form,
			ByteBufCodecs.VAR_INT, Event::phase, ByteBufCodecs.VAR_INT, Event::ticks, ByteBufCodecs.FLOAT, Event::yaw, ByteBufCodecs.VAR_LONG, Event::at, Event::new).cast();
		@Override public Type<Event> type() { return TYPE; }
	}
	private static final class Session {
		final long epoch = ++epochs;
		final WallTurnRules.Input input = FormDashRules.input();
		Move active;
		long serial;
		int form, phase, phaseTicks;
		float yaw;
		// ---- moves pack
		int window;
		LivingEntity foe;
		boolean airSpent;
		long lastCounter = Long.MIN_VALUE;
	}
	private static long epochs;
	private static final Map<ServerPlayer, Session> SESSIONS = new IdentityHashMap<>();
	public static Progress data(Player player) { return player.getAttachedOrElse(PROGRESS, Progress.NONE); }
	public static View view(Player player) { return player.getAttachedOrElse(VIEW, View.NONE); }
	private static boolean original(ServerPlayer player) {
		return player.isAlive() && !player.isRemoved() && player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player;
	}
	public static String name(int form) {
		return switch (form) {
			case FormDashRules.CINDER_LUNGE -> "cinder_lunge"; case FormDashRules.AIR_STEP -> "air_step"; case FormDashRules.PLUNGE -> "plunge";
			case FormDashRules.RIPOSTE -> "ember_riposte"; case FormDashRules.SHOVE -> "gale_shove"; case FormDashRules.GUARD_BREAK -> "stone_break";
			case FormDashRules.SPELL_CUT -> "spell_cut"; default -> "reed_slip";
		};
	}
	public static boolean eligible(ServerPlayer player, int form) {
		return Aura.enabled(player) && FormDashRules.form(form)
			&& FormDashRules.eligible(form, Aura.stage(player), MasterVictories.progress(player).cleared(FormDashRules.school(form)));
	}
	/** Only a teacher's checked acceptance calls this; packets cannot submit a learned bit. */
	public static boolean learn(ServerPlayer player, int form) {
		Progress p = data(player);
		if (p.knows(form) || !original(player) || !eligible(player, form)) return false;
		player.setAttached(PROGRESS, new Progress(p.learned() | FormDashRules.bit(form), p.equipped(), p.readyAt(), p.recoveryUntil(), p.practiced()));
		return true;
	}
	/** Admission again at every accepted step; the shared body checks are Wall Turn's. */
	static boolean able(ServerPlayer player, int form) {
		return local(player, form) && !MasterFormMovement.occupied(player) && local(player, form);
	}
	private static boolean local(ServerPlayer player, int form) {
		return MasterForms.free(player) && eligible(player, form) && data(player).knows(form) && (!FormDashRules.slot(form) || data(player).equipped() == form)
			&& !MasterForms.wallCommitted(player) && !MasterForms.data(player).airborneUsed();
	}
	/** A released aerial form coming down: its own fall damage is the landing, read next tick, not an interrupt. */
	static boolean fallingFree(ServerPlayer player) { Session s = SESSIONS.get(player); return s != null && s.active instanceof AerialMove move && move.released(); }
	public static boolean ownsMotion(ServerPlayer player) { Session s = SESSIONS.get(player); return s != null && s.active != null; }
	/** Guard, Aura Step and fixed arts wait through travel and recovery (read through {@link MasterForms#committed}). */
	public static boolean committed(ServerPlayer player) { return ownsMotion(player) || data(player).recoveryUntil() > MasterForms.now(player); }
	/** No refund. A stopped form still pays its full recovery, so an interrupt is never a free cancel. */
	public static boolean cancel(ServerPlayer player) {
		Session s = SESSIONS.get(player);
		if (s == null || s.active == null) return false;
		Move move = s.active;
		s.active = null;
		move.abort();
		int recovery = FormDashRules.recovery(move.form, true);
		Progress p = data(player);
		player.setAttached(PROGRESS, new Progress(p.learned(), p.equipped(), p.readyAt(), Math.max(p.recoveryUntil(), MasterForms.now(player) + recovery), p.practiced()));
		emit(player, s, move.form, FormDashRules.ABORT, recovery, move.yaw);
		return true;
	}
	/** Wall Turn took the slot. */
	static void clearSlot(ServerPlayer player) {
		Progress p = data(player);
		if (p.equipped() != 0) player.setAttached(PROGRESS, new Progress(p.learned(), 0, p.readyAt(), p.recoveryUntil(), p.practiced()));
	}
	public static boolean request(ServerPlayer player, Action packet) {
		try (var admission = dev.wildercord.cast.ActionAdmission.begin(player)) {
			return admission != null && request(player, packet, admission);
		}
	}
	private static boolean request(ServerPlayer player, Action packet, dev.wildercord.cast.ActionAdmission admission) {
		Session s = SESSIONS.get(player);
		long now = MasterForms.now(player);
		if (!original(player) || s == null || packet.epoch() != s.epoch || !s.input.admit(packet.sequence(), packet.action(), now)) return false;
		if (packet.action() == WallTurnRules.CANCEL) { MasterForms.cancel(player); return true; }
		if (packet.action() == WallTurnRules.RELEASE) return true;
		if (packet.action() == WallTurnRules.EQUIP || packet.action() == WallTurnRules.UNEQUIP) {
			Progress p = data(player); boolean equip = packet.action() == WallTurnRules.EQUIP;
			if (equip && (!p.knows(packet.form()) || !FormDashRules.slot(packet.form()) || !eligible(player, packet.form())) || !equip && p.equipped() == 0) return refusal(player, "equip_wait");
			// One slot and one shared rest: swapping forms never skips the rest a spent form owes.
			if (MasterForms.committed(player) || MasterForms.data(player).airborneUsed() || Aura.inFight(player) || MastersArts.committed(player)
				|| dev.wildercord.cast.ExciseCasting.committed(player) || dev.wildercord.cast.RelayCircles.committed(player) || MasterFormMovement.occupied(player)
				|| player.hasAttached(dev.wildercord.player.WildercordAttachments.CHARGE) || now < p.readyAt() || now < MasterForms.data(player).readyAt()
				|| !WallTurn.safeLanding(player)) return refusal(player, "equip_wait");
			if (SESSIONS.get(player) != s || !admission.valid() || !data(player).equals(p)) return false;
			player.setAttached(PROGRESS, new Progress(p.learned(), equip ? packet.form() : 0, p.readyAt(), p.recoveryUntil(), p.practiced()));
			if (equip) MasterForms.clearSlot(player);
			return true;
		}
		if (s.window > 0 && s.active == null) return followUp(player, s, admission, now);
		if (player.isShiftKeyDown()) { MasterForms.cancel(player); return false; }
		if (s.active != null) return false;
		Progress p = data(player);
		int form = p.equipped();
		if (form == 0) return refusal(player, "unequipped");
		if (!able(player, form) || SESSIONS.get(player) != s || !admission.valid() || MasterForms.committed(player)) return refusal(player, "unavailable");
		p = data(player);
		if (!FormDashRules.canPay(form, Aura.aura(player), now, Math.max(p.readyAt(), MasterForms.data(player).readyAt()), p.recoveryUntil()))
			return refusal(player, now < p.recoveryUntil() ? "recovery" : now < Math.max(p.readyAt(), MasterForms.data(player).readyAt()) ? "rest" : "aura");
		Move move = FormDashRules.aerial(form) ? AerialMove.accept(player, form, s.airSpent) : Move.accept(player, form);
		if (move == null) return refusal(player, form == FormDashRules.CINDER_LUNGE ? "blocked" : form == FormDashRules.REED_SLIP ? "slip_blocked"
			: player.onGround() ? "air_only" : form == FormDashRules.PLUNGE ? "plunge_blocked" : "air_blocked");
		// Reserve before hooks: a spend hook cannot re-enter acceptance or waive the fixed price.
		s.active = move;
		player.setAttached(PROGRESS, new Progress(p.learned(), p.equipped(), now + FormDashRules.rest(form), p.recoveryUntil(), p.practiced()));
		if (Aura.spend(player, FormDashRules.cost(form), "master_form:" + name(form)).backlash() || !move.valid()
			|| SESSIONS.get(player) != s || s.active != move || !admission.valid()) {
			MasterForms.cancel(player); return false;
		}
		if (dev.wildercord.cast.RelayCircles.pending(player)) dev.wildercord.cast.RelayCircles.cancel(player);
		move.hold();
		if (form == FormDashRules.CINDER_LUNGE) {
			sound(player, SoundEvents.FIRECHARGE_USE, .6F, 1.3F);
			emit(player, s, form, FormDashRules.SET, FormDashRules.CINDER_SET_TICKS, move.yaw);
		} else if (form == FormDashRules.AIR_STEP) {
			s.airSpent = true;
			sound(player, SoundEvents.BREEZE_JUMP, .7F, 1.2F);
			emit(player, s, form, FormDashRules.RISE, FormDashRules.AIR_STEP_TICKS, move.yaw);
		} else if (form == FormDashRules.PLUNGE) {
			s.airSpent = true;
			sound(player, SoundEvents.MACE_SMASH_AIR, .7F, .8F);
			emit(player, s, form, FormDashRules.SET, FormDashRules.PLUNGE_SET_TICKS, move.yaw);
		} else {
			sound(player, SoundEvents.BREEZE_SLIDE, .7F, 1.4F);
			emit(player, s, form, FormDashRules.SLIP, FormDashRules.REED_TICKS, move.yaw);
		}
		return true;
	}
	private static boolean refusal(ServerPlayer player, String reason) {
		player.sendOverlayMessage(Component.translatable("message.wildercord.field_form." + reason)); return false;
	}
	private static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
	}
	private static void emit(ServerPlayer player, Session s, int form, int phase, int ticks, float yaw) {
		s.form = form; s.phase = phase; s.phaseTicks = ticks; s.yaw = yaw;
		Event event = new Event(player.getId(), s.epoch, ++s.serial, form, phase, ticks, yaw, player.level().getGameTime());
		if (player.connection == null || player.isRemoved()) return;
		for (ServerPlayer viewer : PlayerLookup.tracking(player)) if (ServerPlayNetworking.canSend(viewer, Event.TYPE)) ServerPlayNetworking.send(viewer, event);
		if (ServerPlayNetworking.canSend(player, Event.TYPE)) ServerPlayNetworking.send(player, event);
	}
	private static void tick(ServerPlayer player) {
		if (!original(player)) { retire(player); return; }
		Session s = SESSIONS.computeIfAbsent(player, ignored -> new Session());
		settle(player);
		if (s.phaseTicks > 0) s.phaseTicks--;
		if (s.active != null) {
			Move move = s.active;
			boolean wasSetting = move.setting();
			int result = move.tick();
			if (SESSIONS.get(player) != s) return;
			if (s.active == move) {
				if (result < 0) MasterForms.cancel(player);
				else if (result > 0) finish(player, s, move);
				else if (wasSetting && !move.setting()) {
					int travel = FormDashRules.travelPhase(move.form);
					if (travel != FormDashRules.IDLE) {
						sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, .8F, .8F);
						emit(player, s, move.form, travel, FormDashRules.travelSpan(move.form), move.yaw);
					}
				}
			}
		}
		if (s.active == null && s.phaseTicks == 0) s.phase = FormDashRules.IDLE;
		if (s.window > 0 && --s.window == 0) s.foe = null;
		if (!(s.active instanceof AerialMove) && (player.onGround() || player.isInWater() || player.isInLava() || player.onClimbable())) s.airSpent = false;
		long now = MasterForms.now(player);
		Progress p = data(player);
		View view = new View(s.epoch, s.phase == FormDashRules.IDLE ? p.equipped() : s.form, s.phase, s.phaseTicks,
			(int) Math.clamp(p.readyAt() - now, 0, FormDashRules.MAX_REST), (int) Math.clamp(p.recoveryUntil() - now, 0, FormDashRules.MAX_TICKS), s.window);
		if (!view.equals(view(player))) player.setAttached(VIEW, view);
	}
	/**
	 * A record from another world's clock never owes more than one use's rest and recovery from now. A save that holds both
	 * slots (only an edited or merged file can) keeps the Master form (Wall Turn or Stone Hinge) and empties the field-form slot.
	 */
	private static void settle(ServerPlayer player) {
		Progress p = data(player);
		long now = MasterForms.now(player);
		long ready = Math.min(p.readyAt(), now + FormDashRules.MAX_REST), recovery = Math.min(p.recoveryUntil(), now + FormDashRules.MAX_RECOVERY);
		int equipped = MasterForms.data(player).equipped() != 0 ? 0 : p.equipped();
		if (ready != p.readyAt() || recovery != p.recoveryUntil() || equipped != p.equipped())
			player.setAttached(PROGRESS, new Progress(p.learned(), equipped, ready, recovery, p.practiced()));
	}
	private static void finish(ServerPlayer player, Session s, Move move) {
		s.active = null;
		int phase = move.end();
		boolean clean = phase != FormDashRules.STALL && phase != FormDashRules.SPLASH;
		int recovery = FormDashRules.aerial(move.form) ? FormDashRules.landingRecovery(move.form, phase) : FormDashRules.recovery(move.form, move.stalled);
		long now = MasterForms.now(player);
		Progress p = data(player);
		boolean drilled = clean && !p.drilled(move.form);
		player.setAttached(PROGRESS, new Progress(p.learned(), p.equipped(), p.readyAt(), Math.max(p.recoveryUntil(), now + recovery),
			clean ? p.practiced() | FormDashRules.bit(move.form) : p.practiced()));
		emit(player, s, move.form, phase, recovery, move.yaw);
		move.effect(phase);
		if (drilled) player.sendSystemMessage(Component.translatable("message.wildercord.field_form.practiced." + name(move.form)));
	}
	private static void retire(ServerPlayer player) {
		cancel(player);
		SESSIONS.remove(player); player.removeAttached(VIEW);
	}
	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(Action.TYPE, Action.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Event.TYPE, Event.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Action.TYPE, (packet, context) -> request(context.player(), packet));
		ServerTickEvents.END_SERVER_TICK.register(server -> { for (ServerPlayer player : server.getPlayerList().getPlayers()) tick(player); });
		// Damage, stance breaks and knockback leases already reach cancel() through MasterForms.cancel.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> { if (entity instanceof ServerPlayer player) retire(player); });
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> retire(player));
		ServerPlayerEvents.AFTER_RESPAWN.register((before, after, alive) -> retire(before));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> retire(handler.player));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> SESSIONS.clear());
		FormDashLessons.init();
		// ---- moves pack
		PayloadTypeRegistry.serverboundPlay().register(Counter.TYPE, Counter.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Counter.TYPE, (packet, context) -> counter(context.player(), packet));
	}

	// ---- moves pack
	/** A swing while a hostile spell flies: the server judges the timing from its own look snapshot, never a client vector. */
	public record Counter(long epoch) implements CustomPacketPayload {
		public static final Type<Counter> TYPE = new Type<>(Wildercord.id("spell_cut_counter"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Counter> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_LONG, Counter::epoch, Counter::new).cast();
		@Override public Type<Counter> type() { return TYPE; }
	}
	/** A perfect guard that caught a living attacker opens the follow-up moment, if any follow-up is learned. */
	public static void parried(ServerPlayer player, LivingEntity attacker) {
		Session s = SESSIONS.get(player);
		if (s == null || attacker == null || !attacker.isAlive() || s.active != null || !original(player)
			|| FormDashRules.pick(data(player).learned(), 0, 0) == 0) return;
		s.window = FormDashRules.FOLLOW_WINDOW; s.foe = attacker;
	}
	/** Whether a follow-up moment is open now (for tests and the HUD). */
	public static boolean followOpen(ServerPlayer player) { Session s = SESSIONS.get(player); return s != null && s.window > 0; }
	/**
	 * The form key inside the moment answers with the follow-up the move keys ask for. The guard may still be held: it is
	 * lowered for the answer, which pays its price, keeps its tell and owes its recovery like any other form.
	 */
	private static boolean followUp(ServerPlayer player, Session s, dev.wildercord.cast.ActionAdmission admission, long now) {
		Progress p = data(player);
		// The client's move intent is in world space: turn it into the player's own forward and sideways keys.
		Vec3 intent = player.getLastClientMoveIntent();
		double rad = Math.toRadians(player.getYRot()), x = Double.isFinite(intent.x) ? intent.x : 0, z = Double.isFinite(intent.z) ? intent.z : 0;
		int form = FormDashRules.pick(p.learned(), x * -Math.sin(rad) + z * Math.cos(rad), x * Math.cos(rad) + z * Math.sin(rad));
		LivingEntity foe = s.foe;
		s.window = 0; s.foe = null;
		if (form == 0) return false;
		if (!FormDashRules.canPay(form, Aura.aura(player), now, 0, p.recoveryUntil()))
			return refusal(player, now < p.recoveryUntil() ? "recovery" : "aura");
		if (!eligible(player, form) || MastersArts.committed(player) || MasterForms.wallCommitted(player)) return refusal(player, "unavailable");
		AuraGuard.lowerForCounter(player);
		if (!able(player, form) || SESSIONS.get(player) != s || !admission.valid()) return refusal(player, "unavailable");
		Move move = Move.followUp(player, form, foe);
		if (move == null) return refusal(player, "follow_blocked");
		s.active = move;
		if (Aura.spend(player, FormDashRules.cost(form), "master_form:" + name(form)).backlash() || !move.valid()
			|| SESSIONS.get(player) != s || s.active != move || !admission.valid()) {
			MasterForms.cancel(player); return false;
		}
		move.hold();
		sound(player, form == FormDashRules.RIPOSTE ? SoundEvents.FIRECHARGE_USE : form == FormDashRules.SHOVE ? SoundEvents.BREEZE_CHARGE : SoundEvents.ANVIL_PLACE,
			.6F, form == FormDashRules.GUARD_BREAK ? .6F : 1.2F);
		emit(player, s, form, FormDashRules.SET, FormDashRules.setTicks(form), move.yaw);
		return true;
	}
	/** Spell Cut: a CUT severs the spell and pays the full price; a near MISS pays less but owes a longer recovery. */
	static boolean counter(ServerPlayer player, Counter packet) {
		Session s = SESSIONS.get(player);
		long now = MasterForms.now(player);
		if (!original(player) || s == null || packet.epoch() != s.epoch || s.lastCounter == now) return false;
		s.lastCounter = now;
		Progress p = data(player);
		if (!p.knows(FormDashRules.SPELL_CUT) || !eligible(player, FormDashRules.SPELL_CUT) || s.active != null || now < p.recoveryUntil()
			|| MasterForms.committed(player) || MastersArts.committed(player) || !Aura.holdsWeapon(player) || player.isSpectator()) return false;
		Vec3 facing = player.getLookAngle();
		if (!Double.isFinite(facing.lengthSqr()) || facing.lengthSqr() < 1.0E-6) return false;
		facing = facing.normalize();
		var timing = dev.wildercord.spell.SpellCutRules.Timing.NONE;
		dev.wildercord.cast.RuneBolt target = null;
		Vec3 center = player.getBoundingBox().getCenter();
		for (var bolt : player.level().getEntitiesOfClass(dev.wildercord.cast.RuneBolt.class,
				player.getBoundingBox().inflate(dev.wildercord.spell.SpellCutRules.COUNTER_ARM), bolt -> bolt.hostileSpellTo(player))) {
			Vec3 toward = bolt.position().subtract(center);
			if (toward.lengthSqr() < 1.0E-8) continue;
			Vec3 unit = toward.normalize();
			var t = dev.wildercord.spell.SpellCutRules.counter(toward.length(), facing.dot(unit), bolt.getDeltaMovement().normalize().dot(unit.scale(-1)));
			if (t == dev.wildercord.spell.SpellCutRules.Timing.CUT && target == null) target = bolt;
			timing = dev.wildercord.spell.SpellCutRules.best(timing, t);
		}
		if (timing == dev.wildercord.spell.SpellCutRules.Timing.NONE) return false;
		if (Aura.aura(player) < dev.wildercord.spell.SpellCutRules.cost(timing)) return refusal(player, "aura");
		int recovery = dev.wildercord.spell.SpellCutRules.recovery(timing);
		// Reserve before hooks: the recovery is owed before the spend can call out.
		player.setAttached(PROGRESS, new Progress(p.learned(), p.equipped(), p.readyAt(), Math.max(p.recoveryUntil(), now + recovery), p.practiced()));
		if (Aura.spend(player, dev.wildercord.spell.SpellCutRules.cost(timing), "master_form:spell_cut").backlash()) return false;
		boolean cut = target != null && target.swordCut(player, facing);
		if (!cut && timing == dev.wildercord.spell.SpellCutRules.Timing.CUT) {
			// Cover closed the line after the timing was judged: it reads and recovers as a miss, at the price already paid.
			Progress q = data(player);
			player.setAttached(PROGRESS, new Progress(q.learned(), q.equipped(), q.readyAt(),
				Math.max(q.recoveryUntil(), now + dev.wildercord.spell.SpellCutRules.COUNTER_MISS_RECOVERY), q.practiced()));
			recovery = dev.wildercord.spell.SpellCutRules.COUNTER_MISS_RECOVERY;
		}
		if (cut) {
			sound(player, SoundEvents.TRIDENT_HIT, .8F, 1.4F);
			Progress q = data(player);
			if (!q.drilled(FormDashRules.SPELL_CUT)) {
				player.setAttached(PROGRESS, new Progress(q.learned(), q.equipped(), q.readyAt(), q.recoveryUntil(), q.practiced() | FormDashRules.bit(FormDashRules.SPELL_CUT)));
				player.sendSystemMessage(Component.translatable("message.wildercord.field_form.practiced.spell_cut"));
			}
		} else sound(player, SoundEvents.PLAYER_ATTACK_NODAMAGE, .8F, .7F);
		emit(player, s, FormDashRules.SPELL_CUT, cut ? FormDashRules.SEVER : FormDashRules.MISS, recovery, player.getYRot());
		return cut;
	}

	/** One original body's grounded travel. Every stretch sweeps its standing box and keeps footing, or it stalls. */
	static class Move {
		final ServerPlayer player;
		final ServerLevel level;
		final ReleasedArtOwner owner;
		final ItemStack blade;
		final String method;
		final int form;
		final Vec3 start, direction;
		final float yaw;
		final DungeonWards.MovementWard warded;
		Vec3 last;
		int set, step;
		boolean stalled, aborted;
		double fallRisk;
		Move(ServerPlayer player, int form, Vec3 direction, float yaw) {
			this.player = player; level = player.level(); owner = ReleasedArtOwner.capture(player); blade = player.getMainHandItem();
			method = Aura.data(player).method(); this.form = form; start = player.position(); last = start; this.direction = direction; this.yaw = yaw;
			warded = DungeonWards.movementWard(level, player.blockPosition()); fallRisk = Math.max(0, player.fallDistance);
			set = FormDashRules.setTicks(form);
		}
		/** Grounded, safe support and a clear first stretch. A refused start costs nothing. */
		static Move accept(ServerPlayer player, int form) {
			if (!Double.isFinite(player.fallDistance) || player.isShiftKeyDown() || !WallTurn.safeLanding(player)) return null;
			Vec3 direction;
			float yaw;
			if (form == FormDashRules.CINDER_LUNGE) {
				Vec3 look = player.getLookAngle();
				direction = new Vec3(look.x, 0, look.z);
				// Looking almost straight up or down still lunges along the body's facing.
				if (!Double.isFinite(direction.lengthSqr()) || direction.lengthSqr() < 1.0E-4) {
					double rad = Math.toRadians(player.getYRot());
					direction = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
				}
				if (!Double.isFinite(direction.lengthSqr()) || direction.lengthSqr() < 1.0E-4) return null;
				direction = direction.normalize();
				yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
			} else {
				Vec3 intent = player.getLastClientMoveIntent();
				double[] lane = FormDashRules.slip(player.getYRot(), Double.isFinite(intent.x) ? intent.x : 0, Double.isFinite(intent.z) ? intent.z : 0);
				direction = new Vec3(lane[0], 0, lane[1]);
				yaw = player.getYRot();
			}
			Move move = new Move(player, form, direction, yaw);
			return move.next(player.position()) == null ? null : move;
		}
		/** The next accepted body position: level, one riser up, or one riser down. Null stalls. */
		private Vec3 next(Vec3 from) {
			Vec3 to = from.add(direction.scale(FormDashRules.stride(form)));
			if (WallTurn.swept(player, from, to, warded)) {
				if (WallTurn.footing(player, to, warded)) return supported(from, to) ? to : null;
				Vec3 down = to.add(0, -.5, 0);
				return WallTurn.swept(player, to, down, warded) && WallTurn.footing(player, down, warded) && supported(from, down) ? down : null;
			}
			Vec3 up = from.add(0, FormDashRules.STEP, 0), over = to.add(0, FormDashRules.STEP, 0);
			return WallTurn.swept(player, from, up, warded) && WallTurn.swept(player, up, over, warded) && WallTurn.footing(player, over, warded)
				&& supported(from, over) ? over : null;
		}
		/** Every sub-step of a stride stands on safe support at its start or end height: a one-block pit, void or lava gap stops travel. */
		private boolean supported(Vec3 from, Vec3 to) {
			int pieces = Math.max(1, (int) Math.ceil(Math.hypot(to.x - from.x, to.z - from.z) / FormDashRules.FOOTING_PROBE));
			for (int i = 1; i < pieces; i++) {
				Vec3 at = from.lerp(to, i / (double) pieces);
				if (!WallTurn.footing(player, new Vec3(at.x, from.y, at.z), warded) && !WallTurn.footing(player, new Vec3(at.x, to.y, at.z), warded)) return false;
			}
			return true;
		}
		/** The lunge ends on reaching a foe instead of passing through it, so the cut meets what it closed on. */
		private boolean reached(Vec3 from) {
			AABB body = player.getBoundingBox().move(from.subtract(player.position()));
			AABB lane = body.minmax(body.move(direction.scale(FormDashRules.stride(form))));
			return !level.getEntities(player, lane, e -> ArtKit.harmableWithoutAim(player, e)).isEmpty();
		}
		boolean valid() {
			if (aborted || !owner.valid() || player.level() != level || !able(player, form)) return false;
			return !aborted && owner.valid() && player.level() == level && player.getMainHandItem() == blade && method.equals(Aura.data(player).method())
				&& (FormDashRules.followUp(form) || !player.isShiftKeyDown()) && Double.isFinite(player.fallDistance) && player.position().distanceToSqr(last) <= .64;
		}
		boolean setting() { return set > 0; }
		void hold() { player.setDeltaMovement(Vec3.ZERO); player.needsSync = true; }
		/** -1 aborts, 0 continues, 1 ends (arrived or stalled). */
		int tick() {
			if (!valid()) return -1;
			fallRisk = Math.max(fallRisk, player.fallDistance);
			if (set > 0) { set--; return move(last) ? 0 : -1; }
			if (FormDashRules.travelTicks(form) == 0) return 1;
			if (FormDashRules.cuts(form) && reached(player.position())) return 1;
			Vec3 to = next(player.position());
			if (to == null) { stalled = true; hold(); return 1; }
			if (!move(to)) return -1;
			return ++step >= FormDashRules.travelTicks(form) ? 1 : 0;
		}
		boolean move(Vec3 to) {
			Vec3 from = player.position();
			if (!from.equals(to) && !WallTurn.swept(player, from, to, warded)) return false;
			fallRisk = Math.max(fallRisk, player.fallDistance) + Math.max(0, from.y - to.y);
			player.teleportTo(level, to.x, to.y, to.z, Relative.ROTATION, 0, 0, false);
			player.fallDistance = Math.max(player.fallDistance, fallRisk);
			player.setDeltaMovement(Vec3.ZERO);
			player.needsSync = true;
			last = to;
			return true;
		}
		/** One foe in a short lane ahead takes an art-capped cut. Guards, friends and protections answer as for any art. */
		void cut() {
			if (!owner.valid() || player.level() != level) return;
			sound(player, SoundEvents.PLAYER_ATTACK_STRONG, .9F, .9F);
			// The nearest foe the blade can actually reach: a wall or closed door between them stops the cut.
			for (LivingEntity foe : ArtKit.line(player, player.position(), direction, FormDashRules.CUT_REACH, FormDashRules.CUT_HALF, 1.5, 8)) {
				if (!visible(foe)) continue;
				AuraCombat.artStrike(player, foe, ArtKit.weapon(player) * FormDashRules.cutScale(form) * ArtKit.scale(), ArtRules.PVP_ART_CAP, false);
				return;
			}
		}
		boolean visible(LivingEntity foe) {
			Vec3 chest = player.position().add(0, Math.min(1.2, player.getBbHeight() * .65), 0), at = foe.getBoundingBox().getCenter();
			for (Vec3 from : new Vec3[] {player.getEyePosition(), chest})
				if (level.clip(new ClipContext(from, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS) return true;
			return false;
		}
		void abort() { aborted = true; if (owner.valid()) { player.fallDistance = Math.max(player.fallDistance, fallRisk); hold(); } }
		// ---- moves pack
		/** The phase this travel ends on; aerial forms override it with how they came down. */
		int end() { return FormDashRules.endPhase(form, stalled); }
		void effect(int phase) {
			if (phase == FormDashRules.CUT) cut();
			else if (phase == FormDashRules.STRIKE) strike();
		}
		/**
		 * A follow-up from a perfect guard: grounded and from safe footing. Riposte travels toward the caught attacker (or where
		 * the player looks); the shove and the break plant and strike where they stand.
		 */
		static Move followUp(ServerPlayer player, int form, LivingEntity foe) {
			if (!FormDashRules.followUp(form) || !Double.isFinite(player.fallDistance) || !WallTurn.safeLanding(player)) return null;
			Vec3 direction = null;
			if (foe != null && foe.isAlive() && !foe.isRemoved() && foe.level() == player.level() && foe.distanceToSqr(player) < 64) {
				Vec3 toward = foe.position().subtract(player.position());
				direction = new Vec3(toward.x, 0, toward.z);
			}
			if (direction == null || !Double.isFinite(direction.lengthSqr()) || direction.lengthSqr() < 1.0E-4) {
				Vec3 look = player.getLookAngle();
				direction = new Vec3(look.x, 0, look.z);
			}
			if (!Double.isFinite(direction.lengthSqr()) || direction.lengthSqr() < 1.0E-4) {
				double rad = Math.toRadians(player.getYRot());
				direction = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
			}
			direction = direction.normalize();
			Move move = new Move(player, form, direction, (float) Math.toDegrees(Math.atan2(-direction.x, direction.z)));
			move.target = foe;
			if (FormDashRules.travelTicks(form) > 0 && !move.reached(player.position()) && move.next(player.position()) == null) return null;
			return move;
		}
		LivingEntity target;
		/** The planted follow-ups: the caught attacker if still in reach and in sight, else the nearest foe in a short lane. */
		void strike() {
			if (!owner.valid() || player.level() != level) return;
			LivingEntity foe = target != null && target.isAlive() && !target.isRemoved() && target.level() == level
				&& target.distanceToSqr(player) <= (FormDashRules.FOLLOW_REACH + 1) * (FormDashRules.FOLLOW_REACH + 1)
				&& ArtKit.harmableWithoutAim(player, target) && visible(target) ? target : null;
			if (foe == null) for (LivingEntity near : ArtKit.line(player, player.position(), direction, FormDashRules.FOLLOW_REACH, FormDashRules.CUT_HALF, 1.5, 8))
				if (visible(near)) { foe = near; break; }
			if (foe == null) { sound(player, SoundEvents.PLAYER_ATTACK_NODAMAGE, .8F, .8F); return; }
			if (form == FormDashRules.SHOVE) {
				sound(player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), .8F, 1.1F);
				ArtKit.shove(foe, new Vec3(direction.x * FormDashRules.SHOVE_PUSH, .2, direction.z * FormDashRules.SHOVE_PUSH));
				foe.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, FormDashRules.SHOVE_WEAKNESS, 0), player);
			} else if (form == FormDashRules.GUARD_BREAK) {
				sound(player, SoundEvents.ANVIL_LAND, .7F, .7F);
				AuraCombat.artStrike(player, foe, ArtKit.weapon(player) * FormDashRules.GUARD_BREAK_SCALE * ArtKit.scale(), ArtRules.PVP_ART_CAP, false);
				foe.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, FormDashRules.GUARD_BREAK_SLOW, 1), player);
				if (foe instanceof ServerPlayer guard) AuraGuard.lowerForCounter(guard);
			}
		}
	}
}
