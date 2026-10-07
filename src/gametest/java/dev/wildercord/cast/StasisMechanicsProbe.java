package dev.wildercord.cast;

import com.google.gson.JsonObject;
import dev.wildercord.api.WildercordEvents;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/** Read-only evidence for the original screenshot fixture's Stasis cases; never changes their timing or damage. */
public final class StasisMechanicsProbe implements AutoCloseable {
	private static final int MAX_EVENTS = 40;
	private static boolean installed;
	private static StasisMechanicsProbe active;
	private final ServerPlayer owner;
	private final ServerLevel level;
	private final LivingEntity target;
	private final RuneDef attack;
	private int events;
	private int suppressed;
	private int pendingSlot = -1;
	private String previousState;
	private Field stasis;
	private String reflectionProblem;
	private StasisDeliveryProbe delivery;

	private StasisMechanicsProbe(ServerPlayer owner, LivingEntity target, RuneDef attack) {
		this.owner = owner;
		this.level = owner.level();
		this.target = target;
		this.attack = attack;
		try {
			stasis = field(Wards.class, "STASIS");
		} catch (ReflectiveOperationException | RuntimeException e) {
			reflectionProblem = e.toString();
		}
	}

	/** Call and close on the fixture's server thread. Only one exact world/body/target scope may be active. */
	public static StasisMechanicsProbe open(ServerPlayer owner, LivingEntity target, RuneDef attack) {
		if (active != null) throw new IllegalStateException("Stasis fixture observation already active");
		install();
		StasisMechanicsProbe probe = new StasisMechanicsProbe(owner, target, attack);
		active = probe;
		try {
			probe.delivery = StasisDeliveryProbe.open(owner, target, attack);
			probe.checkpoint("opened");
			return probe;
		} catch (RuntimeException | Error failure) {
			if (active == probe) active = null;
			if (probe.delivery != null) probe.delivery.close();
			throw failure;
		}
	}

	private static void install() {
		if (installed) return;
		installed = true;
		// Fabric has no listener removal API. These delegates retain no fixture objects after close().
		WildercordEvents.BEFORE_CAST.register((player, slot, runes, cost) -> {
			StasisMechanicsProbe probe = active;
			if (probe != null && probe.attempt(player, slot)) probe.log("before_cast", "slot=" + slot + " cost=" + cost + " runes=" + ids(runes), false);
			return true;
		});
		WildercordEvents.AFTER_CAST.register((player, slot, runes, spent) -> {
			StasisMechanicsProbe probe = active;
			if (probe != null && probe.attempt(player, slot)) probe.log("after_cast", "slot=" + slot + " spent=" + spent + " runes=" + ids(runes), false);
		});
		WildercordEvents.SPELL_HIT.register((caster, targets, point, effects) -> {
			StasisMechanicsProbe probe = active;
			if (probe != null && caster == probe.owner && caster.level() == probe.level && effects.size() == 1
				&& (effects.getFirst().equals(Runes.STASIS) || effects.getFirst().equals(probe.attack))) {
				probe.log("spell_hit", "effects=" + ids(effects) + " expectedTargetHit=" + targets.stream().anyMatch(entity -> entity == probe.target)
					+ " actualTargets=" + targets.stream().map(e -> e.getUUID() + "/" + e.getId()).toList() + " point=" + point, false);
			}
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, damage, blocked) -> {
			StasisMechanicsProbe probe = active;
			if (probe != null && entity == probe.target && entity.level() == probe.level) {
				probe.log("after_damage", "source=" + source.getMsgId() + " attacker=" + (source.getEntity() == null ? null : source.getEntity().getUUID())
					+ " base=" + base + " damage=" + damage + " blocked=" + blocked, false);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			StasisMechanicsProbe probe = active;
			if (probe != null && server == probe.level.getServer()) {
				String state = probe.state().toString();
				if (!state.equals(probe.previousState)) {
					probe.previousState = state;
					probe.log("state_changed", "end_server_tick", false);
				}
			}
		});
	}

	/** Wrap only the fixture's existing cast call; edit, cooldown reset and mana fill still happen exactly once. */
	public static void cast(ServerPlayer player, int slot, List<RuneDef> runes, Component editProblem, Runnable cast) {
		StasisMechanicsProbe probe = active;
		if (probe == null || player != probe.owner || player.level() != probe.level) {
			cast.run();
			return;
		}
		probe.pendingSlot = slot;
		probe.log("cast_attempt", "slot=" + slot + " requested=" + ids(runes) + " threaded=" + Spellbooks.get(player).spells().get(slot)
			+ " readyAt=" + Spellbooks.readyAt(player, slot) + " cordTier=" + Spellbooks.tier(player)
			+ " editProblem=" + (editProblem == null ? null : editProblem.getString()), false);
		try {
			cast.run();
		} finally {
			try {
				probe.log("cast_returned", "slot=" + slot + " readyAt=" + Spellbooks.readyAt(player, slot), false);
			} finally {
				probe.pendingSlot = -1;
			}
		}
	}

	/** GameTest mixin callback: bind only a Cast made inside this exact fixture attempt. */
	public static void observePaidCast(ServerPlayer player, int slot, List<RuneDef> runes, Cast cast) {
		StasisMechanicsProbe probe = active;
		if (probe != null && probe.delivery != null && probe.attempt(player, slot)) {
			try { probe.delivery.paid(player, runes, cast); } catch (Throwable ignored) { /* Observation only. */ }
		}
	}

	private boolean attempt(ServerPlayer player, int slot) {
		return player == owner && player.level() == level && slot == pendingSlot;
	}

	public void checkpoint(String name) {
		log("checkpoint", name, true);
	}

	private JsonObject state() {
		JsonObject state = new JsonObject();
		state.addProperty("targetAlive", target.isAlive());
		state.addProperty("targetRemoved", target.isRemoved());
		state.addProperty("targetInOriginalLevel", target.level() == level);
		state.addProperty("targetStillRegistered", level.getEntity(target.getUUID()) == target);
		state.addProperty("health", target.getHealth());
		state.addProperty("noGravity", target.isNoGravity());
		try {
			if (reflectionProblem != null) {
				state.addProperty("observationError", reflectionProblem);
			} else {
				Object held = ((Map<?, ?>) stasis.get(null)).get(target.getUUID());
				state.addProperty("stasisPresent", held != null);
				if (held != null) {
					state.addProperty("entryIdentity", Integer.toHexString(System.identityHashCode(held)));
					state.addProperty("sameTarget", field(held.getClass(), "target").get(held) == target);
					state.addProperty("sameCaster", field(held.getClass(), "caster").get(held) == owner);
					state.addProperty("sameLevel", field(held.getClass(), "level").get(held) == level);
					state.addProperty("until", field(held.getClass(), "until").getLong(held));
					state.addProperty("stored", field(held.getClass(), "stored").getFloat(held));
					state.addProperty("hits", field(held.getClass(), "hits").getInt(held));
					DamageSource source = (DamageSource) field(held.getClass(), "source").get(held);
					state.addProperty("storedSource", source == null ? null : source.getMsgId());
					state.addProperty("storedSourceFromOwner", source != null && source.getEntity() == owner);
				}
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			reflectionProblem = e.toString();
			state.addProperty("observationError", reflectionProblem);
		}
		return state;
	}

	private void log(String event, String detail, boolean checkpoint) {
		if (!checkpoint && events++ >= MAX_EVENTS) {
			suppressed++;
			return;
		}
		JsonObject record = new JsonObject();
		record.addProperty("case", attack.id());
		record.addProperty("event", event);
		record.addProperty("detail", detail);
		record.addProperty("gameTime", level.getGameTime());
		record.addProperty("targetGameTime", target.level().getGameTime());
		record.addProperty("serverTick", level.getServer().getTickCount());
		record.addProperty("owner", owner.getUUID().toString());
		record.addProperty("ownerConnected", level.getServer().getPlayerList().getPlayer(owner.getUUID()) == owner);
		record.addProperty("ownerAlive", owner.isAlive());
		record.addProperty("ownerRemoved", owner.isRemoved());
		record.addProperty("ownerSpectator", owner.isSpectator());
		record.addProperty("ownerInOriginalLevel", owner.level() == level);
		record.addProperty("ownerPosition", owner.position().toString());
		record.addProperty("ownerAim", owner.getLookAngle().toString());
		record.addProperty("mana", Spellbooks.mana(owner));
		record.addProperty("expectedTarget", target.getUUID().toString());
		record.addProperty("targetPosition", target.position().toString());
		record.add("state", state());
		record.addProperty("suppressed", suppressed);
		System.out.println("WILDERCORD_STASIS_OBSERVATION " + record);
	}

	private static Field field(Class<?> type, String name) throws ReflectiveOperationException {
		Field field = type.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	private static List<String> ids(List<RuneDef> runes) {
		return runes.stream().map(RuneDef::id).toList();
	}

	@Override
	public void close() {
		try {
			checkpoint("closed");
		} finally {
			if (active == this) active = null;
			pendingSlot = -1;
			if (delivery != null) delivery.close();
		}
	}
}
