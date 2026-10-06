package dev.wildercord.cast;

import dev.wildercord.aura.arts.ReleasedArtOwner;
import dev.wildercord.content.CordTier;
import dev.wildercord.player.Heart;
import dev.wildercord.player.MasterStudies;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RelayInputRules;
import dev.wildercord.spell.RelayRules;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.world.dungeons.DungeonWards;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** One paid, body-bound focus. The second fresh edge commits a fixed lane, never another cast. */
public final class RelayCircles {
	private RelayCircles() {}
	private static final Map<ServerPlayer, Focus> FOCI = new java.util.IdentityHashMap<>();
	private static final Map<ServerPlayer, RelayInputRules.Edges> INPUT = new WeakHashMap<>();
	private static final Map<ServerPlayer, Long> RECOVERY = new WeakHashMap<>();
	private static final double LANE_RADIUS = 0.25;

	private static final class Focus {
		final ServerPlayer player;
		final ReleasedArtOwner owner;
		final ServerLevel level;
		final ItemStack cord;
		final int slot, selected;
		final List<String> ids;
		final List<RuneDef> runes;
		final SpellCompiler.Compiled compiled;
		final Vec3 pos, placedFrom;
		final BlockPos floor;
		final long created, expires;
		final int color;
		Cast cast;
		Vec3 end;
		long releaseAt;
		boolean retired;

		Focus(ServerPlayer player, int slot, List<RuneDef> runes, SpellCompiler.Compiled compiled, Vec3 pos, BlockPos floor) {
			this.player = player; this.owner = ReleasedArtOwner.capture(player); this.level = player.level();
			this.cord = Spellbooks.cord(player); this.slot = slot; this.selected = Spellbooks.get(player).selected();
			this.ids = List.copyOf(Spellbooks.get(player).spells().get(slot)); this.runes = List.copyOf(runes); this.compiled = compiled;
			this.pos = pos; this.floor = floor.immutable(); this.placedFrom = player.position();
			this.created = now(player); this.expires = created + RelayRules.FOCUS_TICKS;
			this.color = dev.wildercord.spell.RuneColors.of(runes.get(1));
		}

		boolean valid() {
			if (retired) return false;
			if (!owner.valid() || !available(player) || !MasterStudies.knowsRelay(player) || !MasterStudies.eligibleRelay(player)
				|| Spellbooks.cord(player) != cord || Spellbooks.tier(player) != CordTier.ECHO
				|| Spellbooks.get(player).selected() != selected || !dev.wildercord.gear.Gear.spellOpen(player, CordTier.ECHO, slot)
				|| !Spellbooks.get(player).spells().get(slot).equals(ids) || !SpellCaster.activeRunes(Spellbooks.get(player), slot, CordTier.ECHO).equals(runes)
				|| end == null && now(player) >= expires || !clear(level, player, player.getEyePosition(), pos)
				|| !level.hasChunkAt(floor) || level.getBlockState(floor).getCollisionShape(level, floor).isEmpty()
				|| end != null && (!clear(level, player, pos, end) || player.getEyePosition().distanceTo(pos) + pos.distanceTo(end) > RelayRules.MAX_PATH + 1.0e-5)) {
				retired = true;
				return false;
			}
			return true;
		}

		boolean admitsBlock(BlockPos block) {
			Vec3 at = Vec3.atCenterOf(block);
			return valid() && player.getEyePosition().distanceTo(pos) + pos.distanceTo(at) <= RelayRules.MAX_PATH
				&& clear(level, player, player.getEyePosition(), at, block) && clear(level, player, pos, at, block);
		}

		boolean admits(Entity target) {
			return valid() && target instanceof LivingEntity && target.level() == level && target.isAlive() && !target.isRemoved() && level.getEntity(target.getUUID()) == target
				&& Targets.canHarm(player, target)
				&& player.getEyePosition().distanceTo(pos) + pos.distanceTo(target.getBoundingBox().getCenter()) <= RelayRules.MAX_PATH
				&& clear(level, player, player.getEyePosition(), target.getBoundingBox().getCenter())
				&& clear(level, player, pos, target.getBoundingBox().getCenter());
		}
	}

	public static long now(ServerPlayer player) { return player.level().getServer().overworld().getGameTime(); }
	public static boolean pending(ServerPlayer player) { return FOCI.containsKey(player); }
	/** Reached through Charging.interrupt, so Statuses keeps its existing shared interruption immunity. */
	public static boolean interrupt(ServerPlayer player) {
		if (!pending(player)) return false;
		cancel(player);
		player.sendOverlayMessage(Component.literal("The Relay margin closes. Its payment and rest remain."));
		return true;
	}
	/** Other user-triggered spell routes cancel an uncommitted focus, and cannot overlap warning/recovery. */
	public static boolean beforeOtherSpell(ServerPlayer player) {
		if (committed(player)) return false;
		if (pending(player)) cancel(player);
		return true;
	}
	public static boolean recovering(ServerPlayer player) { return now(player) < RECOVERY.getOrDefault(player, 0L); }
	public static boolean committed(ServerPlayer player) {
		Focus focus = FOCI.get(player);
		return recovering(player) || focus != null && focus.end != null;
	}
	public static boolean contains(ServerPlayer player, int requested) {
		var book = Spellbooks.get(player);
		int slot = requested < 0 ? book.selected() : requested;
		return slot >= 0 && slot < book.spells().size() && RelayRules.containsIds(book.spells().get(slot));
	}

	/** Original row is checked before activeRunes can silently omit an unlearned or over-tier shape. */
	public static String problem(ServerPlayer player, int slot) {
		if (slot < 0 || slot >= dev.wildercord.gear.SpellSlots.ALL) return "Choose an open spell slot.";
		List<String> ids = Spellbooks.get(player).spells().get(slot);
		if (!RelayRules.containsIds(ids)) return "Thread Relay Circle followed by Harm, Frost or Shock.";
		List<RuneDef> runes = ids.stream().map(Runes::get).flatMap(java.util.Optional::stream).toList();
		if (runes.size() != ids.size() || !RelayRules.valid(runes)) return "Relay needs exactly Relay Circle then Harm, Frost or Shock. No links, Knots or modifiers.";
		if (!MasterStudies.knowsRelay(player)) return MasterStudies.hasRelayLesson(player) ? "Finish the copied Relay lesson in your Grimoire; keep eight active Heart Circles." : "Copy The Margin Between Places at the quiet Archive lectern, then study it in your Grimoire.";
		if (!MasterStudies.eligibleRelay(player)) return "Relay Circle needs eight active Heart Circles and The Last Page.";
		if (Spellbooks.tier(player) != CordTier.ECHO || !dev.wildercord.gear.Gear.spellOpen(player, CordTier.ECHO, slot)) return "Relay Circle needs your Echo Cord and an open ordinary spell slot.";
		if (!SpellCaster.activeRunes(Spellbooks.get(player), slot, CordTier.ECHO).equals(runes)) return "Learn both Relay runes before casting this whole spell.";
		return null;
	}

	public static void input(ServerPlayer player, int action, int requested, long nonce) {
		try (var admission = ActionAdmission.begin(player)) {
			if (admission != null) input(player, action, requested, nonce, admission);
		}
	}
	private static void input(ServerPlayer player, int action, int requested, long nonce, ActionAdmission admission) {
		if (requested < -1 || requested >= dev.wildercord.gear.SpellSlots.ALL) return;
		if (!INPUT.computeIfAbsent(player, ignored -> new RelayInputRules.Edges()).accept(action, nonce, now(player))) return;
		if (action == RelayInputRules.CANCEL) { cancel(player); return; }
		if (action != RelayInputRules.DOWN) return;
		Focus focus = FOCI.get(player);
		if (player.isShiftKeyDown()) { cancel(player); return; }
		int slot = requested < 0 ? Spellbooks.get(player).selected() : requested;
		if (focus != null) {
			if (slot != focus.slot) return;
			if (!focus.valid()) { cancel(player); return; }
			if (focus.end == null) commit(focus);
			return;
		}
		place(player, slot, admission);
	}

	private static boolean available(ServerPlayer player) {
		return player.isAlive() && !player.isRemoved() && !player.isSpectator() && !player.isSleeping() && !player.isPassenger()
			&& player.level().getServer().getPlayerList().getPlayer(player.getUUID()) == player
			&& !CastLock.locked(player) && !dev.wildercord.aura.arts.ArtWards.silenced(player) && !VoidTime.hushed(player) && !FusedFrostWards.sealed(player)
			&& !dev.wildercord.aura.MastersArts.committed(player)
			&& Float.isFinite(player.getXRot()) && Float.isFinite(player.getYRot()) && finite(player.getEyePosition())
			&& player.containerMenu == player.inventoryMenu
			&& !player.hasAttached(dev.wildercord.player.WildercordAttachments.CHARGE);
	}

	private static void place(ServerPlayer player, int slot, ActionAdmission admission) {
		String problem = problem(player, slot);
		if (problem != null) { fail(player, problem); return; }
		if (!available(player) || recovering(player)) return;
		long now = now(player), rest = player.getAttachedOrElse(RelayState.REST, 0L);
		if (now < rest || player.level().getGameTime() < Spellbooks.readyAt(player, slot)) {
			fail(player, "Relay Circle is resting; its eight-second rest is shared by every slot."); return;
		}
		Vec3 eye = player.getEyePosition();
		Vec3 aim = eye.add(player.getLookAngle().scale(RelayRules.PLACE_RANGE));
		if (!loaded(player.level(), eye, aim)) { fail(player, "The focus must stay in loaded ground."); return; }
		var hit = player.level().clip(new ClipContext(eye, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (hit.getType() != HitResult.Type.BLOCK || hit.getDirection() != Direction.UP) { fail(player, "Aim at visible floor within eight blocks to place the focus."); return; }
		Vec3 at = hit.getLocation().add(0, 0.12, 0);
		if (!clear(player.level(), player, eye, at)) { fail(player, "Both sides of the margin need clear sight; wards close it."); return; }
		List<RuneDef> runes = SpellCaster.activeRunes(Spellbooks.get(player), slot, CordTier.ECHO);
		SpellCompiler.Compiled compiled = SpellCompiler.compile(runes);
		if (compiled.isEmpty()) return;
		int cost = Heart.manaCost(player, compiled, Mastery.costFactor(player, runes));
		if (WildSurge.freeRecast(player, player.level().getGameTime())) { fail(player, "Relay Circle cannot use a free recast. Spend it with another spell first."); return; }
		if (!player.isCreative() && (!Float.isFinite(Spellbooks.mana(player)) || Spellbooks.mana(player) < cost)) { fail(player, "Relay Circle needs " + cost + " mana. It cannot overcast."); return; }
		Focus focus = new Focus(player, slot, runes, compiled, at, hit.getBlockPos());
		if (!dev.wildercord.api.WildercordEvents.BEFORE_CAST.invoker().allow(player, slot, runes, cost)) return;
		// An add-on callback may change body, loadout, mana, position or progression. No receipt survives that change.
		if (!admission.valid() || !focus.valid() || !clear(player.level(), player, player.getEyePosition(), at)
			|| player.getEyePosition().distanceTo(at) > RelayRules.PLACE_RANGE + .13
			|| !player.isCreative() && (!Float.isFinite(Spellbooks.mana(player)) || Spellbooks.mana(player) < cost) || FOCI.containsKey(player)
			|| now < player.getAttachedOrElse(RelayState.REST, 0L)) return;
		float mana = Spellbooks.mana(player);
		Heart.Bonuses bonuses = Heart.bonuses(player, mana >= dev.wildercord.player.Mana.max(player) - .5F);
		bonuses = bonuses.withPower(bonuses.power() * Mastery.powerFactor(player, runes));
		focus.cast = new Cast(player, 1, bonuses, false, focus::valid, new Cast.Info(compiled.root(), runes.size(), Heart.leaning(player), runes))
			.weigh(compiled.cost()).damagePrice(cost).gear(dev.wildercord.gear.Gear.of(player)).withAffinity()
			.admission(focus::admits).blockAdmission(focus::admitsBlock).lifetime(focus::valid).incoming(at);
		// Reserve payment, rest and the exact receipt before any payment/progression callback can run.
		if (!player.isCreative()) Spellbooks.setMana(player, mana - cost);
		player.setAttached(RelayState.REST, now + RelayRules.REST_TICKS);
		Spellbooks.setReadyAt(player, slot, player.level().getGameTime() + RelayRules.REST_TICKS);
		FOCI.put(player, focus);
		dev.wildercord.aura.MasterForms.cancel(player);
		if (!player.isCreative()) dev.wildercord.aura.Unity.manaSpent(player, cost);
		if (!focus.valid()) { cancel(player); return; }
		Mastery.onCast(player, slot, runes, focus.cast, cost);
		HeartCircles.condense(player, cost);
		PlayerAffinities.onCast(player, compiled.root(), cost, 0);
		HeartCircles.onCast(player);
		if (!focus.valid()) { cancel(player); return; }
		player.setAttached(RelayState.VIEW, new RelayState(RelayState.PLACED, slot, focus.created, focus.expires, at, at, focus.color));
		Fx.sound(player.level(), at, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, .55F, 1.1F);
		dev.wildercord.api.WildercordEvents.AFTER_CAST.invoker().afterCast(player, slot, runes, cost);
		if (!focus.valid()) cancel(player);
	}

	private static void commit(Focus focus) {
		Vec3 eye = focus.player.getEyePosition(), look = focus.player.getLookAngle();
		Vec3 to = eye.add(look.scale(RelayRules.MAX_PATH));
		if (!loaded(focus.level, eye, to)) return;
		var hit = focus.level.clip(new ClipContext(eye, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, focus.player));
		Vec3 aim = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
		// The crosshair's first visible living body is a server-observed aim point, not a homing target.
		// A displaced origin aimed only at the distant sky point would otherwise miss the body under the crosshair.
		List<LivingEntity> observed = new ArrayList<>();
		Vec3 limit = aim;
		focus.level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class), new AABB(eye, limit).inflate(.15),
			e -> e != focus.player && e.isAlive() && e.getBoundingBox().inflate(.15).clip(eye, limit).isPresent(), observed, Cast.MAX_ENTITIES + 1);
		if (observed.size() > Cast.MAX_ENTITIES) return;
		double nearest = eye.distanceToSqr(aim);
		for (LivingEntity target : observed) {
			Vec3 point = target.getBoundingBox().inflate(.15).clip(eye, limit).orElse(limit);
			double distance = eye.distanceToSqr(point);
			if (distance < nearest) { nearest = distance; aim = point; }
		}
		Vec3 direction = aim.subtract(focus.pos);
		double remaining = RelayRules.MAX_PATH - eye.distanceTo(focus.pos);
		if (!finite(direction) || remaining <= .25 || direction.lengthSqr() < .01) return;
		Vec3 end = focus.pos.add(direction.normalize().scale(Math.min(remaining, direction.length()) - .02));
		if (!clear(focus.level, focus.player, focus.pos, end)
			|| eye.distanceTo(focus.pos) + focus.pos.distanceTo(end) > RelayRules.MAX_PATH + 1.0e-5) return;
		if (!focus.valid()) { cancel(focus.player); return; }
		focus.end = end;
		focus.releaseAt = now(focus.player) + RelayRules.WARN_TICKS;
		dev.wildercord.aura.MasterForms.cancel(focus.player);
		focus.player.setAttached(RelayState.VIEW, new RelayState(RelayState.WARNING, focus.slot, now(focus.player), focus.releaseAt, focus.pos, focus.end, focus.color));
	}

	private static void release(Focus focus) {
		if (!focus.valid()) { cancel(focus.player); return; }
		var group = focus.compiled.root().groups.getFirst();
		Vec3 direction = focus.end.subtract(focus.pos).normalize();
		// A ray has one primary hit; Shock's ordinary bounded secondary arc must pass the same current admission.
		List<Entity> targets = new ArrayList<>();
		focus.level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class),
			new AABB(focus.pos, focus.end).inflate(LANE_RADIUS), e -> e != focus.player && e.isAlive()
				&& (e.getBoundingBox().inflate(LANE_RADIUS).contains(focus.pos) || e.getBoundingBox().inflate(LANE_RADIUS).clip(focus.pos, focus.end).isPresent()), targets, Cast.MAX_ENTITIES + 1);
		// Refuse pathological density as one bounded release rather than admitting an arbitrary partial crowd.
		if (targets.size() > Cast.MAX_ENTITIES) { cancel(focus.player); return; }
		targets.sort(Comparator.comparingDouble((Entity e) -> e.getBoundingBox().inflate(LANE_RADIUS).contains(focus.pos) ? 0
			: e.getBoundingBox().inflate(LANE_RADIUS).clip(focus.pos, focus.end).orElse(e.getBoundingBox().getCenter()).distanceToSqr(focus.pos)).thenComparingInt(Entity::getId));
		Entity first = targets.isEmpty() ? null : targets.getFirst();
		Vec3 stop = first == null ? focus.end : first.getBoundingBox().getCenter();
		Light.ray(focus.level, focus.pos, stop, focus.color, .07, 5);
		if (first != null && focus.admits(first)) {
			long before = first instanceof TrainingDummy dummy ? dummy.hitSequence() : -1;
			CastEngine.onHit(focus.cast, group, new Cast.Hit(List.of(first), first.getBoundingBox().getCenter(), direction, focus.pos, null, null, false), null);
			if (first instanceof TrainingDummy dummy && dummy.hitSequence() > before && focus.valid() && focus.player.position().distanceToSqr(focus.placedFrom) >= 1)
				MasterStudies.completeRelayPractice(focus.player);
		}
		// Impact callbacks may disconnect, replace, move or unequip the owner. Never recreate a retired body's HUD/recovery.
		if (!focus.valid()) { cancel(focus.player); return; }
		FOCI.remove(focus.player, focus);
		focus.retired = true;
		focus.cast.cancel();
		dev.wildercord.aura.ResonantStrikes.retire(focus.cast);
		long until = now(focus.player) + RelayRules.RECOVERY_TICKS;
		RECOVERY.put(focus.player, until);
		focus.player.setAttached(RelayState.VIEW, new RelayState(RelayState.RECOVERING, focus.slot, now(focus.player), until, focus.pos, focus.end, focus.color));
	}

	/** No mana/rest refund, and the original receipt is permanently unusable. */
	public static void cancel(ServerPlayer player) {
		Focus focus = FOCI.remove(player);
		if (focus != null) { focus.retired = true; if (focus.cast != null) { focus.cast.cancel(); dev.wildercord.aura.ResonantStrikes.retire(focus.cast); } }
		player.removeAttached(RelayState.VIEW);
	}
	private static void retire(ServerPlayer player) { cancel(player); INPUT.remove(player); RECOVERY.remove(player); }
	private static boolean finite(Vec3 p) { return Double.isFinite(p.x) && Double.isFinite(p.y) && Double.isFinite(p.z); }

	private static List<dev.wildercord.spell.RelayGeometry.Cell> cells(Vec3 from, Vec3 to) {
		return dev.wildercord.spell.RelayGeometry.cells(from.x, from.y, from.z, to.x, to.y, to.z);
	}
	private static boolean loaded(ServerLevel level, Vec3 from, Vec3 to) {
		var cells = cells(from, to);
		if (cells.isEmpty()) return false;
		for (var cell : cells) {
			BlockPos pos = new BlockPos(cell.x(), cell.y(), cell.z());
			if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) return false;
		}
		return true;
	}

	/** Every intersected cell is checked, including a grazed corner that point-sampling could skip. */
	static boolean clear(ServerLevel level, ServerPlayer caster, Vec3 from, Vec3 to) {
		return clear(level, caster, from, to, null);
	}
	private static boolean clear(ServerLevel level, ServerPlayer caster, Vec3 from, Vec3 to, BlockPos endpointBlock) {
		var cells = cells(from, to);
		if (cells.isEmpty()) return false;
		boolean ward = DungeonWards.warded(level, caster.blockPosition());
		for (var cell : cells) {
			BlockPos pos = new BlockPos(cell.x(), cell.y(), cell.z());
			if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos) || DungeonWards.warded(level, pos) != ward) return false;
		}
		var hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(to) < .0004
			|| endpointBlock != null && hit.getBlockPos().equals(endpointBlock);
	}

	/** Optional bounded collateral query for the ordinary elemental hooks reached by a Relay payload. */
	static List<LivingEntity> collateral(Cast cast, Vec3 origin, AABB area, java.util.function.Predicate<LivingEntity> matches) {
		List<LivingEntity> found = new ArrayList<>();
		cast.level.getEntities(net.minecraft.world.level.entity.EntityTypeTest.forClass(LivingEntity.class), area,
			e -> e != cast.caster && e.isAlive(), found, Cast.MAX_ENTITIES + 1);
		if (found.size() > Cast.MAX_ENTITIES) return List.of();
		found.removeIf(e -> !matches.test(e) || !cast.admits(e) || !(cast.caster instanceof ServerPlayer owner)
			|| !clear(cast.level, owner, origin, e.getBoundingBox().getCenter()));
		found.sort(Comparator.comparingDouble((LivingEntity e) -> e.distanceToSqr(origin)).thenComparingInt(Entity::getId));
		return found;
	}

	/** Current secondary origin, without widening the original paid victim policy. */
	static boolean admitsFrom(Cast cast, Vec3 at, LivingEntity target) {
		return cast.admits(target) && (!cast.guardedImpact() || cast.caster instanceof ServerPlayer owner
			&& clear(cast.level, owner, at, target.getBoundingBox().getCenter()));
	}

	/** A defensive answer keeps the original lifetime and payment while its defender supplies the new direction. */
	static Cast reflected(Cast cast, LivingEntity defender, LivingEntity attacker, Vec3 at) {
		Cast turned = cast.reflected(defender);
		if (cast.guardedImpact()) turned.admission(target -> target == attacker && cast.alive()
			&& defender.isAlive() && !defender.isRemoved() && defender.level() == cast.level
			&& cast.level.getEntity(defender.getUUID()) == defender && attacker.isAlive() && !attacker.isRemoved()
			&& attacker.level() == cast.level && cast.level.getEntity(attacker.getUUID()) == attacker
			&& Targets.canHarm(defender, target) && attacker instanceof ServerPlayer owner
			&& clear(cast.level, owner, at, target.getBoundingBox().getCenter())).incoming(at);
		return turned;
	}

	static void from(Cast cast, Vec3 at, Runnable impact) {
		Vec3 previous = cast.incoming();
		if (cast.guardedImpact()) cast.incoming(at);
		try { impact.run(); } finally { if (cast.guardedImpact()) cast.incoming(previous); }
	}

	private static void fail(ServerPlayer player, String message) { player.sendOverlayMessage(Component.literal(message).withStyle(net.minecraft.ChatFormatting.RED)); }
	public static void init() {
		dev.wildercord.net.RelayInput.init();
		dev.wildercord.net.RelayEditorReply.init();
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			for (Focus focus : List.copyOf(FOCI.values())) {
				if (!focus.valid()) { cancel(focus.player); continue; }
				if (focus.end != null && now(focus.player) >= focus.releaseAt) release(focus);
			}
			RECOVERY.entrySet().removeIf(entry -> {
				if (now(entry.getKey()) < entry.getValue()) return false;
				entry.getKey().removeAttached(RelayState.VIEW); return true;
			});
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (entity instanceof ServerPlayer player && taken > 0 && FOCI.containsKey(player)) cancel(player);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> { if (entity instanceof ServerPlayer p) retire(p); });
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((p, from, to) -> retire(p));
		ServerPlayerEvents.AFTER_RESPAWN.register((old, fresh, alive) -> retire(old));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> retire(handler.player));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			for (ServerPlayer player : List.copyOf(FOCI.keySet())) cancel(player);
			INPUT.clear(); RECOVERY.clear();
		});
	}
}
