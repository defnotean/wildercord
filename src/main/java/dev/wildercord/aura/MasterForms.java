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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.IdentityHashMap;
import java.util.Map;

/** One learned/equipped Master-form slot, separate from written techniques and fixed-release combat arts. */
public final class MasterForms {
	private MasterForms() {}
	public static final int WALL_TURN = 1, STONE_HINGE = 2;
	/** {@code learned} stays Wall Turn's bit so old saves keep their form; Stone Hinge has its own, absent in old saves. */
	public record Progress(boolean learned, int equipped, long readyAt, boolean airborneUsed, boolean practiced, long recoveryUntil, long landingUntil, boolean hingeLearned) {
		public static final Progress NONE = new Progress(false, 0, 0, false, false);
		public Progress(boolean learned, int equipped, long readyAt, boolean airborneUsed, boolean practiced) { this(learned, equipped, readyAt, airborneUsed, practiced, 0, 0, false); }
		public Progress { recoveryUntil = Math.max(0, recoveryUntil); landingUntil = Math.clamp(landingUntil, 0, Math.max(0, readyAt));
			equipped = learned && equipped == WALL_TURN ? WALL_TURN : hingeLearned && equipped == STONE_HINGE ? STONE_HINGE : 0; readyAt = Math.max(0, readyAt); }
		public boolean knows(int form) { return form == WALL_TURN ? learned : form == STONE_HINGE && hingeLearned; }
		Progress equip(int form) { return new Progress(learned, form, readyAt, airborneUsed, practiced, recoveryUntil, landingUntil, hingeLearned); }
		public static final Codec<Progress> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.BOOL.optionalFieldOf("learned", false).forGetter(Progress::learned),
			Codec.INT.optionalFieldOf("equipped", 0).forGetter(Progress::equipped),
			Codec.LONG.optionalFieldOf("ready_at", 0L).forGetter(Progress::readyAt),
			Codec.BOOL.optionalFieldOf("airborne_used", false).forGetter(Progress::airborneUsed),
			Codec.BOOL.optionalFieldOf("practiced", false).forGetter(Progress::practiced),
			Codec.LONG.optionalFieldOf("recovery_until", 0L).forGetter(Progress::recoveryUntil),
			Codec.LONG.optionalFieldOf("landing_until", 0L).forGetter(Progress::landingUntil),
			Codec.BOOL.optionalFieldOf("hinge_learned", false).forGetter(Progress::hingeLearned)
		).apply(i, Progress::new));
		public static final StreamCodec<ByteBuf, Progress> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, Progress::learned, ByteBufCodecs.VAR_INT, Progress::equipped, ByteBufCodecs.VAR_LONG, Progress::readyAt,
			ByteBufCodecs.BOOL, Progress::airborneUsed, ByteBufCodecs.BOOL, Progress::practiced,
			ByteBufCodecs.VAR_LONG, Progress::recoveryUntil, ByteBufCodecs.VAR_LONG, Progress::landingUntil,
			ByteBufCodecs.BOOL, Progress::hingeLearned, Progress::new);
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
		final WallTurnRules.Input input = new WallTurnRules.Input(StoneHingeRules.EQUIP);
		WallTurn active;
		long serial;
		int phase, phaseTicks, supportTicks;
		boolean completedKick;
		float yaw;
		/** Stone Hinge: BRACE or CATCH while planted, else 0. The lateral side and yaw are fixed at the paid press. */
		int hinge, hingeLeft, hingeSide;
		float hingeYaw;
		Vec3 plant;
	}
	private static long epochs;
	private static final Map<ServerPlayer, Session> SESSIONS = new IdentityHashMap<>();
	public static Progress data(Player player) { return player.getAttachedOrElse(PROGRESS, Progress.NONE); }
	public static View view(Player player) { return player.getAttachedOrElse(VIEW, View.NONE); }
	public static long now(ServerPlayer player) { return player.level().getServer().overworld().getGameTime(); }
	private static boolean original(ServerPlayer player) {
		return player.isAlive() && !player.isRemoved() && player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player;
	}
	public static boolean eligibleLesson(ServerPlayer player) { return eligibleLesson(player, WALL_TURN); }
	/** Sovereign plus a recorded clear of the form's Master: Gale for Wall Turn, Stone for Stone Hinge. Old clears count.
	 *  Stone Hinge also needs the server's experimental switch: off, it is neither taught nor usable (a learned bit is kept). */
	public static boolean eligibleLesson(ServerPlayer player, int form) {
		var record = MasterVictories.progress(player);
		return Aura.enabled(player) && (form == WALL_TURN ? WallTurnRules.eligible(Aura.stage(player), record.cleared(MastersRules.GALE))
			: form == STONE_HINGE && testedHinge(player) && StoneHingeRules.eligible(Aura.stage(player), record.cleared(MastersRules.STONE)));
	}
	/** Stone Hinge stays gated until its peer and latency checks pass: an operator enables {@code aura.experimental_stone_hinge}. */
	public static boolean testedHinge(net.minecraft.world.entity.player.Player player) { return dev.wildercord.config.Config.stoneHinge(player); }
	public static boolean learn(ServerPlayer player) { return learn(player, WALL_TURN); }
	/** Only the teacher's checked acceptance calls this; packets cannot submit a learned bit. */
	public static boolean learn(ServerPlayer player, int form) {
		Progress p = data(player);
		if (p.knows(form) || !original(player) || !eligibleLesson(player, form)) return false;
		player.setAttached(PROGRESS, new Progress(p.learned() || form == WALL_TURN, p.equipped(), p.readyAt(), p.airborneUsed(), p.practiced(),
			p.recoveryUntil(), p.landingUntil(), p.hingeLearned() || form == STONE_HINGE));
		return true;
	}
	/** Admission used again at every accepted movement step. A loss of control never creates a free escape. */
	static boolean able(ServerPlayer player) { return able(player, WALL_TURN); }
	private static boolean able(ServerPlayer player, int form) {
		return locallyAble(player, form) && !MasterFormMovement.occupied(player) && locallyAble(player, form);
	}
	/** Re-read local eligibility after external movement predicates, without invoking those predicates twice. */
	private static boolean locallyAble(ServerPlayer player, int form) {
		return free(player) && eligibleLesson(player, form) && data(player).knows(form) && data(player).equipped() == form;
	}
	/** Body and action state shared by every equipped form: no mount, flight, fluid, lock, guard, art or open menu. */
	static boolean free(ServerPlayer player) {
		var slow = player.getEffect(MobEffects.SLOWNESS);
		return original(player) && !player.isSpectator() && !player.isSleeping() && Aura.enabled(player) && Aura.holdsWeapon(player)
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
	public static boolean ownsMotion(ServerPlayer player) { Session s = SESSIONS.get(player); return s != null && (s.active != null || s.hinge != 0); }
	/** A paid Stone Hinge catch is open: only then does the exact-hit receipt arm. */
	public static boolean catching(ServerPlayer player) { Session s = SESSIONS.get(player); return s != null && s.hinge == StoneHingeRules.CATCH; }
	/** Whether {@code at} is in front of the paid plant, measured against the facing locked at payment, never the live camera. */
	static boolean hingeFrontal(ServerPlayer player, Vec3 at) {
		Session s = SESSIONS.get(player);
		return s != null && s.hinge == StoneHingeRules.CATCH && StoneHingeRules.frontal(s.hingeYaw, at.x - player.getX(), at.z - player.getZ());
	}
	/** Fixed Aura arts wait through descent/recovery; ordinary movement, swings and spells do not use this gate. */
	public static boolean committed(ServerPlayer player) { return wallCommitted(player) || FormDash.committed(player); }
	static boolean wallCommitted(ServerPlayer player) {
		Progress p = data(player);
		return ownsMotion(player) || p.landingUntil() > 0 || p.recoveryUntil() > now(player);
	}
	/** A field form took the one slot. Rest, landing and recovery stay as they are. */
	static void clearSlot(ServerPlayer player) {
		Progress p = data(player);
		if (p.equipped() != 0) player.setAttached(PROGRESS, p.equip(0));
	}
	/** No refund and no airborne-use reset. Damage and equipment/world lifecycle call this synchronously. */
	public static boolean cancel(ServerPlayer player) {
		boolean field = FormDash.cancel(player);
		Session s = SESSIONS.get(player);
		if (s != null && s.hinge != 0) {
			s.hinge = 0; plant(player, false);
			// Like every other ending: no refund, and the normal recovery.
			Progress p = data(player);
			player.setAttached(PROGRESS, new Progress(p.learned(), p.equipped(), p.readyAt(), p.airborneUsed(), p.practiced(),
				Math.max(p.recoveryUntil(), now(player) + StoneHingeRules.RECOVERY_TICKS), p.landingUntil(), p.hingeLearned()));
			emit(player, s, WallTurnRules.ABORT, 5, s.hingeYaw);
			return true;
		}
		if (s == null || s.active == null) return field;
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
		if (packet.action() == WallTurnRules.EQUIP || packet.action() == WallTurnRules.UNEQUIP || packet.action() == StoneHingeRules.EQUIP) {
			Progress p = data(player);
			int form = packet.action() == WallTurnRules.EQUIP ? WALL_TURN : packet.action() == StoneHingeRules.EQUIP ? STONE_HINGE : 0;
			if (form == STONE_HINGE && !testedHinge(player)) return hingeRefusal(player, "testing");
			if (form == 0 ? !p.learned() && !p.hingeLearned() : !p.knows(form) || !eligibleLesson(player, form)) return refusal(player, "equip_wait");
			if (committed(player) || p.airborneUsed() || Aura.inFight(player)
				|| MastersArts.committed(player) || dev.wildercord.cast.ExciseCasting.committed(player) || dev.wildercord.cast.RelayCircles.committed(player) || MasterFormMovement.occupied(player) || player.hasAttached(WildercordAttachments.CHARGE)
				|| now(player) < p.readyAt() || now(player) < FormDash.data(player).readyAt() || !WallTurn.safeLanding(player)) return refusal(player, "equip_wait");
			if (SESSIONS.get(player) != s || !admission.valid() || !data(player).equals(p)) return false;
			player.setAttached(PROGRESS, p.equip(form));
			if (form != 0) FormDash.clearSlot(player);
			return true;
		}
		if (player.isShiftKeyDown()) { cancel(player); return false; }
		if (data(player).equipped() == STONE_HINGE) return hinge(player, s, admission);
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
		player.setAttached(PROGRESS, new Progress(p.learned(), p.equipped(), now(player) + WallTurnRules.REST_TICKS, true, p.practiced(), p.recoveryUntil(), now(player) + WallTurnRules.REST_TICKS, p.hingeLearned()));
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
	/** Grounded press plus exactly one strafe. Known ward, border and hazard problems refuse before payment. */
	private static boolean hinge(ServerPlayer player, Session s, dev.wildercord.cast.ActionAdmission admission) {
		if (s.hinge != 0 || s.active != null) return false;
		if (!testedHinge(player)) return hingeRefusal(player, "testing");
		Progress p = data(player);
		if (!able(player, STONE_HINGE) || SESSIONS.get(player) != s || !admission.valid() || committed(player))
			return hingeRefusal(player, p.hingeLearned() ? "unavailable" : "locked");
		p = data(player);
		if (!StoneHingeRules.canPay(Aura.aura(player), now(player), p.readyAt(), p.airborneUsed()))
			return hingeRefusal(player, p.airborneUsed() ? "ground" : now(player) < p.readyAt() ? "rest" : "aura");
		if (!WallTurn.safeLanding(player) || player.isShiftKeyDown()) return hingeRefusal(player, "ground");
		var input = player.getLastClientInput();
		int side = StoneHingeRules.side(input.left(), input.right());
		if (side == 0) return hingeRefusal(player, "strafe");
		float yaw = player.getYRot();
		double[] lateral = StoneHingeRules.lateral(yaw, side);
		if (!WallTurn.hingeLane(player, new Vec3(lateral[0], 0, lateral[1]))) return hingeRefusal(player, "lane");
		// Reserve before hooks: the shared rest begins at the paid brace, and no hit, late hit or refusal refunds it.
		s.hinge = StoneHingeRules.BRACE; s.hingeLeft = StoneHingeRules.BRACE_TICKS; s.hingeSide = side; s.hingeYaw = yaw; s.plant = player.position();
		player.setAttached(PROGRESS, new Progress(p.learned(), p.equipped(), now(player) + WallTurnRules.REST_TICKS, false, p.practiced(),
			p.recoveryUntil(), p.landingUntil(), p.hingeLearned()));
		if (Aura.spend(player, StoneHingeRules.COST, "master_form:stone_hinge").backlash() || SESSIONS.get(player) != s
			|| s.hinge != StoneHingeRules.BRACE || !admission.valid()) {
			cancel(player); return false;
		}
		if (dev.wildercord.cast.RelayCircles.pending(player)) dev.wildercord.cast.RelayCircles.cancel(player);
		plant(player, true);
		dev.wildercord.cast.Fx.sound(player.level(), player.position(), net.minecraft.sounds.SoundEvents.STONE_HIT, .8F, .6F);
		emit(player, s, StoneHingeRules.BRACE, StoneHingeRules.BRACE_TICKS, yaw);
		return true;
	}
	private static boolean hingeRefusal(ServerPlayer player, String reason) {
		player.sendOverlayMessage(Component.translatable("message.wildercord.stone_hinge." + reason)); return false;
	}
	/** The planted body may shuffle, never walk off, jump, or lose the form's requirements. Losing them forfeits without refund. */
	private static void tickHinge(ServerPlayer player, Session s) {
		if (!able(player, STONE_HINGE) || SESSIONS.get(player) != s || s.hinge == 0 || !player.onGround() || s.plant == null
			|| player.position().subtract(s.plant).horizontalDistanceSqr() > StoneHingeRules.PLANT * StoneHingeRules.PLANT) {
			if (SESSIONS.get(player) == s) cancel(player);
			return;
		}
		if (--s.hingeLeft > 0) return;
		if (s.hinge == StoneHingeRules.BRACE) {
			s.hinge = StoneHingeRules.CATCH; s.hingeLeft = StoneHingeRules.CATCH_TICKS;
			emit(player, s, StoneHingeRules.CATCH, StoneHingeRules.CATCH_TICKS, s.hingeYaw);
		} else settleHinge(player, s, StoneHingeRules.SPENT);
	}
	/** Called by the exact-hit receipt after the native melee completed. Only the native impulse turns; Y is kept. */
	static boolean hinge(ServerPlayer player, LivingEntity attacker, Vec3 before, Vec3 after) {
		Session s = SESSIONS.get(player);
		if (s == null || s.hinge != StoneHingeRules.CATCH || before == null || after == null || !original(player) || VoidTime.anchored(player)
			|| !able(player, STONE_HINGE) || SESSIONS.get(player) != s || s.hinge != StoneHingeRules.CATCH) return false;
		double[] turned = StoneHingeRules.turn(before.x, before.z, after.x, after.z, s.hingeYaw, s.hingeSide);
		// A drop, ward, border or hazard that appeared during the catch refuses the turn; the native hit and impulse stand.
		if (turned == null || !WallTurn.hingeLane(player, new Vec3(turned[0] - before.x * .5, 0, turned[1] - before.z * .5))) return false;
		player.setDeltaMovement(new Vec3(turned[0], player.getDeltaMovement().y, turned[1]));
		var level = player.level();
		dev.wildercord.cast.Fx.sound(level, player.position(), net.minecraft.sounds.SoundEvents.SHIELD_BLOCK, .9F, .7F);
		dev.wildercord.cast.Fx.sound(level, player.position(), net.minecraft.sounds.SoundEvents.STONE_BREAK, .8F, .6F);
		var ground = level.getBlockState(player.getOnPos());
		if (!ground.isAir()) dev.wildercord.cast.Vfx.emit(level, new net.minecraft.core.particles.BlockParticleOption(
			net.minecraft.core.particles.ParticleTypes.BLOCK, ground), player.position().add(0, .1, 0), 14, .45, .12);
		settleHinge(player, s, StoneHingeRules.TURN);
		return true;
	}
	private static void settleHinge(ServerPlayer player, Session s, int phase) {
		s.hinge = 0; plant(player, false);
		Progress p = data(player);
		player.setAttached(PROGRESS, new Progress(p.learned(), p.equipped(), p.readyAt(), p.airborneUsed(), p.practiced(),
			Math.max(p.recoveryUntil(), now(player) + StoneHingeRules.RECOVERY_TICKS), p.landingUntil(), p.hingeLearned()));
		emit(player, s, phase, StoneHingeRules.RECOVERY_TICKS, s.hingeYaw);
	}
	private static final net.minecraft.resources.Identifier PLANT = Wildercord.id("stone_hinge_plant");
	/** The plant: a heavy, transient slow while braced and catching. Never saved, always removed on any exit. */
	private static void plant(ServerPlayer player, boolean on) {
		var speed = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
		if (speed == null) return;
		if (on) speed.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(PLANT, -StoneHingeRules.SLOW,
			net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		else speed.removeModifier(PLANT);
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
		settle(player);
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
		if (s.hinge != 0) tickHinge(player, s);
		if (SESSIONS.get(player) != s) return;
		Progress p = data(player);
		boolean supported = p.airborneUsed() && s.active == null && WallTurn.safeLanding(player);
		if (SESSIONS.get(player) != s) return;
		p = data(player);
		if (supported && p.airborneUsed()) {
			if (++s.supportTicks >= 2) {
				player.setAttached(PROGRESS, new Progress(p.learned(), p.equipped(), p.readyAt(), false, p.practiced() || s.completedKick,
					now(player) + WallTurnRules.RECOVERY_TICKS, 0, p.hingeLearned()));
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
		if (s.active == null && s.hinge == 0 && s.phaseTicks == 0 && s.phase != WallTurnRules.FALL) s.phase = WallTurnRules.IDLE;
		View view = new View(s.epoch, s.phase, s.phaseTicks, (int) Math.clamp(data(player).readyAt() - now(player), 0, WallTurnRules.REST_TICKS),
			(int) Math.clamp(data(player).recoveryUntil() - now(player), 0, WallTurnRules.RECOVERY_TICKS),
			(int) Math.clamp(data(player).landingUntil() - now(player), 0, WallTurnRules.REST_TICKS));
		if (!view.equals(view(player))) player.setAttached(VIEW, view);
	}
	/** A record from another world's clock never owes more than one use's rest and recovery from now (landing follows the rest). */
	private static void settle(ServerPlayer player) {
		Progress p = data(player);
		long ready = Math.min(p.readyAt(), now(player) + WallTurnRules.REST_TICKS), recovery = Math.min(p.recoveryUntil(), now(player) + WallTurnRules.RECOVERY_TICKS);
		if (ready != p.readyAt() || recovery != p.recoveryUntil())
			player.setAttached(PROGRESS, new Progress(p.learned(), p.equipped(), ready, p.airborneUsed(), p.practiced(), recovery, p.landingUntil(), p.hingeLearned()));
	}
	private static void settleRecovery(ServerPlayer player) {
		cancel(player);
		Progress p = data(player);
		player.setAttached(PROGRESS, new Progress(p.learned(), p.equipped(), p.readyAt(), p.airborneUsed(), p.practiced(),
			Math.max(p.recoveryUntil(), now(player) + WallTurnRules.RECOVERY_TICKS), 0, p.hingeLearned()));
	}
	private static void retire(ServerPlayer player) {
		plant(player, false);
		if (ownsMotion(player) || data(player).landingUntil() > 0) settleRecovery(player);
		else cancel(player);
		SESSIONS.remove(player); player.removeAttached(VIEW); MasterFormMovement.forget(player);
	}
	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(Action.TYPE, Action.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Event.TYPE, Event.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Action.TYPE, (packet, context) -> request(context.player(), packet));
		ServerTickEvents.END_SERVER_TICK.register(server -> { for (ServerPlayer player : server.getPlayerList().getPlayers()) tick(player); });
		dev.wildercord.api.AuraApi.onStanceBroken((attacker, target) -> { if (target instanceof ServerPlayer player) cancel(player); });
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			// Only the exact first hit inside a paid Stone Hinge catch waits for its receipt; a refused receipt still cancels.
			// A released aerial form's own fall damage is its landing; the form reads the ground next tick.
			if (entity instanceof ServerPlayer player && MastersArtRules.interrupts(base, taken, blocked) && !StoneHingeReceipt.defer(player, source)
				&& !(source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) && FormDash.fallingFree(player))) cancel(player);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> { if (entity instanceof ServerPlayer player) retire(player); });
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> retire(player));
		ServerPlayerEvents.AFTER_RESPAWN.register((before, after, alive) -> retire(before));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> retire(handler.player));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> { SESSIONS.clear(); MasterFormMovement.clear(); });
	}
}
