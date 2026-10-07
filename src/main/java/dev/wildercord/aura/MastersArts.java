package dev.wildercord.aura;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtKit;
import dev.wildercord.aura.world.MasterHitReceipt;
import dev.wildercord.cast.CastLock;
import dev.wildercord.cast.Effects;
import dev.wildercord.cast.RuneBolt;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Statuses;
import dev.wildercord.net.PacketThrottle;
import dev.wildercord.player.WildercordAttachments;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Rebindable combat moves with server-owned aim, eligibility, payment and persistent cooldowns. */
public final class MastersArts {
	private MastersArts() {}

	public record Activate(int move) implements CustomPacketPayload {
		public static final Type<Activate> TYPE = new Type<>(Wildercord.id("masters_art"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Activate> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Activate::move, Activate::new).cast();
		@Override public Type<Activate> type() { return TYPE; }
	}

	/** An accepted action's server timeline, sent to its owner and nearby viewers. */
	public record Performed(int entity, int move, long startTick, int windup, int recovery, float yaw, float pitch) implements CustomPacketPayload {
		public static final Type<Performed> TYPE = new Type<>(Wildercord.id("masters_art_performed"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Performed> CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, Performed::entity, ByteBufCodecs.VAR_INT, Performed::move, ByteBufCodecs.VAR_LONG, Performed::startTick,
			ByteBufCodecs.VAR_INT, Performed::windup, ByteBufCodecs.VAR_INT, Performed::recovery,
			ByteBufCodecs.FLOAT, Performed::yaw, ByteBufCodecs.FLOAT, Performed::pitch, Performed::new).cast();
		@Override public Type<Performed> type() { return TYPE; }
	}

	/** Stored on the player and measured on the overworld clock, so dimension swaps and death cannot clear rests. */
	public record Rest(long spellcut, long rising, long driving, long shared) {
		public static final Rest NONE = new Rest(0, 0, 0, 0);
		public static final Codec<Rest> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.optionalFieldOf("spellcut", 0L).forGetter(Rest::spellcut),
			Codec.LONG.optionalFieldOf("rising", 0L).forGetter(Rest::rising),
			Codec.LONG.optionalFieldOf("driving", 0L).forGetter(Rest::driving),
			Codec.LONG.optionalFieldOf("shared", 0L).forGetter(Rest::shared)
		).apply(i, Rest::new));
		long ready(int move) { return Math.max(shared, switch (move) { case 0 -> spellcut; case 1 -> rising; default -> driving; }); }
		Rest begin(int move, long now, int ticks, int recovery) {
			return new Rest(move == 0 ? now + ticks : spellcut, move == 1 ? now + ticks : rising,
				move == 2 ? now + ticks : driving, now + Math.max(MastersArtRules.SHARED_REST, recovery));
		}
	}

	private static final AttachmentType<Rest> REST = AttachmentRegistry.create(Wildercord.id("masters_art_rests"),
		builder -> builder.initializer(() -> Rest.NONE).persistent(Rest.CODEC).copyOnDeath());
	private record Aim(ServerPlayer player, Vec3 direction, Vec3 view) {}
	private static Aim activeAim;
	private record Targets(ServerPlayer player, dev.wildercord.aura.arts.ArtReleaseTargets.Release receipt) {}
	private static Targets activeTargets;
	private static EarnedCounters.Release activeCounter;
	private record Pending(ServerPlayer player, java.util.function.BooleanSupplier valid) {}
	private static final Map<UUID, Pending> PENDING = new HashMap<>();
	/** A short physical continuation, separate from already-launched fields, wounds and afterimages. */
	private static final class Continuation {
		final ServerPlayer player;
		final net.minecraft.server.level.ServerLevel level;
		final net.minecraft.world.item.ItemStack blade;
		final String method;
		final long until;
		boolean cancelled;

		Continuation(ServerPlayer player, int recovery) {
			this.player = player;
			this.level = player.level();
			this.blade = player.getMainHandItem();
			this.method = Aura.data(player).method();
			this.until = level.getGameTime() + recovery;
		}

		boolean alive() {
			return !cancelled && level.getGameTime() < until && eligible(player) && player.level() == level
				&& player.getMainHandItem() == blade && method.equals(Aura.data(player).method())
				&& level.getServer().getPlayerList().getPlayer(player.getUUID()) == player;
		}
	}
	private static final Map<UUID, Continuation> CONTINUATIONS = new HashMap<>();
	private static Continuation performingContinuation;
	private static final PacketThrottle REQUESTS = new PacketThrottle(6, 4);

	public static void init() {
		dev.wildercord.aura.arts.ReleasedArtOwner.init();
		PayloadTypeRegistry.serverboundPlay().register(Activate.TYPE, Activate.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(Performed.TYPE, Performed.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Activate.TYPE, (payload, context) -> {
			if (REQUESTS.allow(context.player().getUUID(), context.server().getTickCount())) activate(context.player(), payload.move());
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			REQUESTS.forget(handler.player.getUUID());
			forget(handler.player.getUUID());
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			REQUESTS.clear(); PENDING.clear(); activeAim = null; activeTargets = null; activeCounter = null; performingContinuation = null;
			CONTINUATIONS.values().forEach(sequence -> sequence.cancelled = true);
			CONTINUATIONS.clear();
		});
		// Packet-driven held-slot changes cancel synchronously through AuraSelectionMixin. This additionally catches
		// persistent equipment/method/world changes made directly by server systems before another active frame.
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.START_SERVER_TICK.register(server -> {
			for (Pending pending : java.util.List.copyOf(PENDING.values())) {
				if (!valid(pending)) cancel(pending.player());
			}
			for (Continuation sequence : java.util.List.copyOf(CONTINUATIONS.values())) {
				if (sequence.level.getGameTime() >= sequence.until) CONTINUATIONS.remove(sequence.player.getUUID(), sequence);
				else if (!sequence.alive()) cancel(sequence.player);
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (MastersArtRules.interrupts(base, taken, blocked) && entity instanceof ServerPlayer player) cancel(player);
		});
	}

	/** The same validated entrypoint is available to tests; a client cannot supply a victim or bypass payment. */
	public static boolean activate(ServerPlayer player, int ordinal) {
		if (dev.wildercord.cast.ActionAdmission.busy(player) || dev.wildercord.cast.ExciseCasting.blocking(player)) return false;
		MastersArtRules.Move move = MastersArtRules.move(ordinal);
		if (MasterForms.committed(player) || move == null || !Float.isFinite(player.getYRot()) || !Float.isFinite(player.getXRot()) || !player.isAlive() || player.isSpectator() || !Aura.enabled(player) || !Aura.holdsWeapon(player)
			|| Awakening.spent(player) || AuraGuard.guarding(player) || Clashes.holding(player) || player.isSleeping()
			|| player.isPassenger() || dev.wildercord.aura.arts.ArtWards.silenced(player) || CastLock.locked(player) || Stance.opened(player)) return false;
		long now = player.level().getServer().overworld().getGameTime();
		Rest rests = player.getAttachedOrElse(REST, Rest.NONE);
		if (!MastersArtRules.canPay(move, Aura.stage(player), Aura.aura(player), now, rests.ready(ordinal))) {
			String key = Aura.stage(player) < move.stage() ? "stage" : now < rests.ready(ordinal) ? "rest" : "aura";
			Object detail = key.equals("stage") ? Component.translatable("aura.wildercord.stage." + AuraStages.id(move.stage()))
				: key.equals("rest") ? String.format(java.util.Locale.ROOT, "%.1f", (rests.ready(ordinal) - now) / 20.0) : move.cost();
			player.sendOverlayMessage(Component.translatable("message.wildercord.masters_art." + key, detail));
			return false;
		}
		// Payment and rest are committed before effects, even if a foe moves or every projectile is friendly.
		if (Aura.spend(player, move.cost(), "masters:" + move.id()).backlash()) return false;
		player.setAttached(REST, rests.begin(ordinal, now, move.rest(), move.windup() + move.recovery()));
		Vec3 aim = ArtKit.flat(player);
		schedule(player, ordinal, move.windup(), move.recovery(),
			() -> Aura.stage(player) >= move.stage(), () -> perform(player, ordinal, move, aim));
		return true;
	}

	/**
	 * Starts one selected style's physical windup. SwordStrings commits its own price and individual rest once this accepts;
	 * the performer and its success hooks run only at the active frame. Other arts keep their existing timing.
	 */
	static boolean beginStyle(ServerPlayer player, AuraApi.StringArt art, AuraApi.StringContext context,
			EarnedCounters.Attempt counter, double cost, Runnable payment, Runnable impact) {
		try (var admission = dev.wildercord.cast.ActionAdmission.begin(player)) {
			return admission != null && beginStyle(player, art, context, counter, cost, payment, impact, admission);
		}
	}

	private static boolean beginStyle(ServerPlayer player, AuraApi.StringArt art, AuraApi.StringContext context,
			EarnedCounters.Attempt counter, double cost, Runnable payment, Runnable impact, dev.wildercord.cast.ActionAdmission admission) {
		MastersStyleRules.Style style = MastersStyleRules.of(art.id());
		if (dev.wildercord.cast.ExciseCasting.blocking(player) || MasterForms.committed(player) || style == null || committed(player) || !eligible(player, counter != null && counter.canLowerGuard(player)) || Aura.stage(player) < art.stage()
			|| !art.available().test(player) || !dev.wildercord.config.Config.get().aura().strings().enabled()) return false;
		if ((style.targets() == MastersStyleRules.TargetPolicy.EARNED_COUNTER) != (counter != null)
			|| counter != null && !counter.ownerValid(player)) return false;
		Vec3 aim = ArtKit.flat(player);
		Vec3 view = player.getViewVector(1.0F);
		boolean targetBearing = style.targets() == MastersStyleRules.TargetPolicy.HAILFALL_RECEIPT
			|| style.targets() == MastersStyleRules.TargetPolicy.SKYFALL_RECEIPT;
		var accepted = targetBearing ? dev.wildercord.aura.arts.ArtReleaseTargets.accept(player, context, aim, view) : null;
		if (targetBearing && accepted == null) return false;
		if (!art.condition().met(player) || !admission.valid() || !SwordStrings.rested(player, art)
			|| !eligible(player, counter != null && counter.canLowerGuard(player))
			|| counter != null && !counter.ownerValid(player) || Aura.aura(player) < cost - 1.0E-4) return false;
		long now = player.level().getServer().overworld().getGameTime();
		Rest rest = player.getAttachedOrElse(REST, Rest.NONE);
		player.setAttached(REST, new Rest(rest.spellcut(), rest.rising(), rest.driving(), now + style.windup() + style.recovery()));
		schedule(player, style.animation(), style.windup(), style.recovery(),
			() -> Aura.stage(player) >= art.stage() && art.available().test(player)
				&& dev.wildercord.config.Config.get().aura().strings().enabled()
				&& (accepted == null || accepted.ownerValid(player)) && (counter == null || counter.ownerValid(player)), () -> {
				Aim outer = activeAim;
				Targets outerTargets = activeTargets;
				EarnedCounters.Release outerCounter = activeCounter;
				activeAim = new Aim(player, aim, view);
				try {
					var release = accepted == null ? null : accepted.release(player);
					var counterRelease = counter == null ? null : counter.release(player);
					// A lost selected target is a paid whiff: keep the readable accepted motion/recovery, with no effects or hooks.
					if (targetBearing && release == null || counter != null && counterRelease == null) return;
					activeTargets = release == null ? null : new Targets(player, release);
					activeCounter = counterRelease;
					impact.run();
				} finally { activeAim = outer; activeTargets = outerTargets; activeCounter = outerCounter; }
			}, payment, counter);
		return true;
	}

	/** The authoritative facing of a style's active frame, scoped only while its performer chooses hits and launch vectors. */
	public static Vec3 committedAim(net.minecraft.world.entity.Entity entity) {
		return activeAim != null && activeAim.player() == entity ? activeAim.direction() : null;
	}

	/** Full locked view for the two first forms whose projectiles retain their original upward/downward pitch. */
	public static Vec3 committedView(net.minecraft.world.entity.Entity entity) {
		return activeAim != null && activeAim.player() == entity ? activeAim.view() : null;
	}

	/** The validated release receipt exists only inside its own performer; delayed effects retain their copied release point. */
	public static dev.wildercord.aura.arts.ArtReleaseTargets.Release releaseTargets(ServerPlayer player, String art) {
		return activeTargets != null && activeTargets.player() == player && activeTargets.receipt().art().equals(art)
			? activeTargets.receipt() : null;
	}

	public static EarnedCounters.Release earnedCounter(ServerPlayer player) {
		return activeAim != null && activeAim.player() == player ? activeCounter : null;
	}

	private static boolean eligible(ServerPlayer player) { return eligible(player, false); }
	private static boolean eligible(ServerPlayer player, boolean earnedGuard) {
		return Float.isFinite(player.getYRot()) && Float.isFinite(player.getXRot()) && player.isAlive() && !player.isRemoved() && !player.isSpectator() && Aura.enabled(player) && Aura.holdsWeapon(player)
			&& !Awakening.spent(player) && !player.isPassenger() && !player.isSleeping() && (earnedGuard || !AuraGuard.guarding(player))
			&& !Clashes.holding(player) && !dev.wildercord.aura.arts.ArtWards.silenced(player) && !CastLock.locked(player) && !Stance.opened(player);
	}

	/** One shared cancellation and recovery contract for both the new keys and the authored fixed-release style forms. */
	private static void schedule(ServerPlayer player, int animation, int windup, int recovery,
			java.util.function.BooleanSupplier stillEligible, Runnable impact) {
		schedule(player, animation, windup, recovery, stillEligible, impact, null, null);
	}

	private static void schedule(ServerPlayer player, int animation, int windup, int recovery,
			java.util.function.BooleanSupplier stillEligible, Runnable impact, Runnable payment, EarnedCounters.Attempt counter) {
		var level = player.level();
		var blade = player.getMainHandItem();
		String method = Aura.data(player).method();
		dev.wildercord.cast.Charging.interrupt(player);
		Pending token = new Pending(player, () -> eligible(player) && player.level() == level && player.getMainHandItem() == blade
			&& level.getServer().getPlayerList().getPlayer(player.getUUID()) == player
			&& method.equals(Aura.data(player).method()) && stillEligible.getAsBoolean());
		PENDING.put(player.getUUID(), token);
		// Install the cancellation token before the payment hook. A callback cannot erase and then revive this windup.
		if (payment != null) payment.run();
		// Payment is final even when its callback cancels the hit: the original guard still lowers for exposed recovery.
		if (counter != null) counter.lowerAfterPaid(player);
		if (PENDING.get(player.getUUID()) != token) return;
		if (counter != null && (!counter.ownerValid(player) || !counter.canLowerGuard(player))) { cancel(player); return; }
		if (!valid(token)) { cancel(player); return; }
		broadcast(player, new Performed(player.getId(), animation, level.getGameTime(), windup, recovery,
			net.minecraft.util.Mth.wrapDegrees(player.getYRot()), MastersStyleRules.attackPitch(animation, player.getXRot())));
		AuraFx.bodyAuraFlare(player, windup, 0.45F);
		// Damage, a cast interruption, a different player body or a changed blade/method cancels the pending hit, never its paid recovery.
		Effects.withSource(player, () -> Scheduler.later(windup, () -> {
			if (PENDING.get(player.getUUID()) != token) return;
			if (!valid(token)) {
				cancel(player);
				return;
			}
			PENDING.remove(player.getUUID(), token);
			Continuation sequence = new Continuation(player, recovery);
			CONTINUATIONS.put(player.getUUID(), sequence);
			Scheduler.later(recovery, () -> CONTINUATIONS.remove(player.getUUID(), sequence));
			Continuation outer = performingContinuation;
			performingContinuation = sequence;
			try { impact.run(); } finally { performingContinuation = outer; }
		}));
	}

	private static boolean valid(Pending pending) {
		try { return pending.valid().getAsBoolean(); }
		catch (RuntimeException failure) {
			Wildercord.LOGGER.warn("Cancelling an Aura action whose eligibility check failed", failure);
			return false;
		}
	}

	/** The accepted move owns its windup and recovery; ordinary swings, other arts and casts wait. */
	public static boolean committed(ServerPlayer player) {
		return player.getAttachedOrElse(REST, Rest.NONE).shared() > player.level().getServer().overworld().getGameTime();
	}

	/** Hostile damage or a cast interruption cancels the pending active frame, retaining paid recovery. */
	public static boolean cancel(ServerPlayer player) {
		boolean cancelled = forget(player.getUUID());
		if (cancelled) broadcast(player, new Performed(player.getId(), 0, player.level().getGameTime(), 0, 0, 0, 0));
		return cancelled;
	}

	private static boolean forget(UUID player) {
		boolean pending = PENDING.remove(player) != null;
		Continuation sequence = CONTINUATIONS.remove(player);
		boolean continuing = sequence != null && !sequence.cancelled && sequence.level.getGameTime() < sequence.until;
		if (sequence != null) sequence.cancelled = true;
		return pending || continuing;
	}

	/** Capture at release for additional physical cuts; persistent fields, wounds and afterimages deliberately do not use it. */
	public static java.util.function.BooleanSupplier continuation(ServerPlayer player) {
		Continuation sequence = performingContinuation;
		return sequence != null && sequence.player == player ? sequence::alive : () -> player.isAlive() && !player.isRemoved();
	}

	private static void broadcast(ServerPlayer player, Performed animation) {
		if (player.connection == null || player.isRemoved()) return;
		for (ServerPlayer viewer : PlayerLookup.tracking(player)) {
			if (ServerPlayNetworking.canSend(viewer, Performed.TYPE)) ServerPlayNetworking.send(viewer, animation);
		}
		if (ServerPlayNetworking.canSend(player, Performed.TYPE)) ServerPlayNetworking.send(player, animation);
	}

	private static boolean perform(ServerPlayer player, int ordinal, MastersArtRules.Move move, Vec3 forward) {
		AuraFx.Art fx = AuraFx.art(player).trail(ordinal == 1 ? AuraFxRules.Stroke.RISING : ordinal == 2 ? AuraFxRules.Stroke.THRUST : AuraFxRules.Stroke.CROSS, false, 1.15F).flare(12, 0.6F);
		ArtKit.Hits hits = ArtKit.hits(player, fx).wearing(ordinal == 1 ? MastersArtRules.BREAK_STANCE : 1);
		if (ordinal == 0) {
			int cut = 0;
			for (RuneBolt bolt : player.level().getEntitiesOfClass(RuneBolt.class, player.getBoundingBox().inflate(move.reach())).stream()
				.sorted(Comparator.comparingDouble(player::distanceToSqr)).toList()) {
				Vec3 to = bolt.position().subtract(player.getEyePosition());
				if (to.lengthSqr() > move.reach() * move.reach() || to.normalize().dot(forward) < 0.25
					|| bolt.getDeltaMovement().dot(to) >= 0 || !player.hasLineOfSight(bolt)) continue;
				if (bolt.swordCut(player, forward) && ++cut >= move.targets()) break;
			}
		}
		var foes = ordinal == 2
			? ArtKit.line(player, player.position(), forward, move.reach(), 0.8, 2.4, move.targets())
			: ArtKit.arcFrom(player, player.position(), forward, move.reach(), ordinal == 0 ? 110 : 75, move.targets());
		for (LivingEntity foe : foes) {
			if (!player.hasLineOfSight(foe)) continue;
			if (ordinal == 2 && foe instanceof ServerPlayer caster) {
				// Snapshot before the entire hit: reactions and art callbacks may replace a held spell.
				var charge = caster.getAttached(WildercordAttachments.CHARGE);
				MasterHitReceipt.Result hit = MasterHitReceipt.measure(player, caster, () -> hits.strike(caster, move.damage()));
				if (ArtKit.harmable(player, caster) && CastLock.canLock(caster)) {
					var response = CastHitRules.response(hit.damaging(), charge, caster.getAttached(WildercordAttachments.CHARGE));
					if (response == CastHitRules.Response.SEAL_IDLE || response == CastHitRules.Response.INTERRUPT && Statuses.interrupt(caster))
						CastLock.lock(caster, MastersArtRules.INTERRUPT_TICKS);
				}
			} else {
				float taken = hits.strike(foe, move.damage());
				if (taken > 0 && ordinal == 2 && ArtKit.harmable(player, foe)) CastLock.lock(foe, MastersArtRules.INTERRUPT_TICKS);
				if (taken > 0 && ordinal == 1 && ArtKit.harmable(player, foe)) ArtKit.lift(foe, 0.32, 8);
			}
		}
		AuraFx.sound(player, AuraFx.Sound.SWING, 0.8F, ordinal == 1 ? 0.85F : 1.15F);
		player.sendOverlayMessage(Component.translatable("aura.wildercord.art." + move.id()).withColor(Aura.color(player)));
		return true;
	}
}
