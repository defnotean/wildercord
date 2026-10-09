package dev.wildercord.aura.world;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.aura.AuraAttachments;
import dev.wildercord.cast.LeyWalker;
import dev.wildercord.config.Config;
import dev.wildercord.content.CordTier;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbook;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Runes;
import dev.wildercord.world.LeyLines;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The same observation-only benchmark can run against baseline and corrected Mana Skin.
 * One genuine connected player receives production regeneration, and an unpaused Gale Master
 * chooses and executes all attacks through normal entity ticks. No projected(), manual AI step,
 * artificial charge, tick replay, teleport, or resource refill occurs during observation.
 *
 * This is a stationary, unguarded, non-casting survival trial, not a claim about every school or
 * player strategy. Native knockback, navigation, guard rests, breathing and projectile flight
 * remain live. Outcomes are logged, not asserted: either implementation may survive or die.
 */
public final class ManaSkinSustainChecks implements FabricClientGameTest {
	private static final long SEED = 98432026L;
	private static final int WINDOW = 400;
	private static final float HEALTH = 20;
	private static boolean registered;
	private static volatile ManaSkinSustainChecks active;
	private ServerPlayer player;
	private SwordMaster master;
	private Vec3 origin;
	private long began = -1, previousTick = -1, animationBegan = -1;
	private int nativeTicksAtStart, attacks, releases, hits, regenTicks, unexpectedDamage;
	private boolean released, done;
	private String failure;
	private Snapshot initial, previous;
	private Hit pending;
	private float spent, observedRegeneration, spentAtPreviousTick, minimumMana;
	private final List<Long> hitTicks = new ArrayList<>();

	private record Snapshot(float health, float absorption, float mana) {
		static Snapshot of(ServerPlayer player) {
			return new Snapshot(player.getHealth(), player.getAbsorptionAmount(), Spellbooks.mana(player));
		}
	}

	private static final class Hit {
		final long tick;
		final DamageSource source;
		final Snapshot before;
		final float requested;
		Snapshot earlyAfterDamage;
		Hit(long tick, DamageSource source, Snapshot before, float requested) {
			this.tick = tick; this.source = source; this.before = before; this.requested = requested;
		}
	}

	@Override
	public void runTest(ClientGameTestContext context) {
		registerObservers();
		try (var world = context.worldBuilder().adjustSettings(settings -> settings.setSeed(Long.toString(SEED))).create()) {
			context.waitTicks(40);
			for (String command : List.of("difficulty normal", "gamerule spawn_mobs false", "gamerule natural_health_regeneration false",
				"weather clear", "time set noon")) world.getServer().runCommand(command);
			world.getServer().runOnServer(server -> prepare(server.getPlayerList().getPlayers().getFirst()));
			// Native player ticks install the armour modifiers and settle gravity before enrollment.
			context.waitTicks(20);
			try {
				world.getServer().waitFor(server -> {
					if (server.getTickCount() % 5 != 0) return false;
					start();
					return true;
				}, 10);
				world.getServer().waitFor(server -> done, WINDOW + 40);
				world.getServer().runOnServer(server -> {
					check(failure == null, "Sustain benchmark fixture: " + failure);
					check(began >= 0 && attacks > 0 && releases > 0 && hits > 0,
						"The benchmark must observe a naturally started trial, real releases and native Master hits");
					check(regenTicks > 0 && observedRegeneration > 0,
						"The connected player's normal production mana regeneration must actually run");
					check(unexpectedDamage == 0, "Only this enrolled Master may damage the benchmark player");
				});
			} finally {
				world.getServer().runOnServer(server -> {
					active = null;
					if (master != null && !master.isRemoved()) master.discard();
				});
			}
		} finally { active = null; }
	}

	private void prepare(ServerPlayer target) {
		player = target;
		check(player.level().getSeed() == SEED, "The benchmark's synthetic world uses its fixed seed");
		origin = offLeyOrigin();
		for (int x = -32; x <= 32; x++) for (int z = -32; z <= 32; z++)
			player.level().setBlockAndUpdate(BlockPos.containing(origin).offset(x, -1, z), Blocks.STONE.defaultBlockState());
		player.setGameMode(GameType.SURVIVAL);
		player.removeAllEffects();
		player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
		player.setHealth(HEALTH); player.setAbsorptionAmount(0);
		player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
		player.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
		player.setAttached(AuraAttachments.AURA, AuraAttachments.Data.NONE);
		player.setAttached(AuraAttachments.STATE, AuraAttachments.State.NONE);
		player.setAttached(WildercordAttachments.CIRCLES, 20);
		player.setAttached(WildercordAttachments.CRACKS, WildercordAttachments.Cracks.NONE);
		player.setAttached(WildercordAttachments.CRYSTALS, 0);
		// An uncast, harmless known innate prevents a random first-Circle awakening during the trial.
		player.setAttached(WildercordAttachments.INNATE, Runes.TWIN_STAR.id());
		Spellbooks.set(player, new Spellbook(List.of(), List.of(), 0, true));
		Spellbooks.setCord(player, new ItemStack(WildercordItems.ECHO_CORD));
		var protection = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
		Item[] armour = {Items.NETHERITE_BOOTS, Items.NETHERITE_LEGGINGS, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_HELMET};
		EquipmentSlot[] slots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
		for (int i = 0; i < armour.length; i++) {
			ItemStack stack = new ItemStack(armour[i]); stack.enchant(protection, 4); player.setItemSlot(slots[i], stack);
		}
		player.teleportTo(player.level(), origin.x, origin.y, origin.z + 3, Set.of(), 180, 0, false);
		player.setDeltaMovement(Vec3.ZERO);
	}

	/** Deterministic location selection; normal LeyWalker still owns the live ON_LEY attachment. */
	private Vec3 offLeyOrigin() {
		long leySeed = LeyWalker.seed(player.level());
		for (int x = -256; x <= 256; x += 32) for (int z = -256; z <= 256; z += 32) {
			boolean clear = true;
			for (int dx = -24; dx <= 24 && clear; dx += 2) for (int dz = -24; dz <= 24; dz += 2) {
				if (LeyLines.strength(leySeed, x + dx + .5, z + dz + .5) >= LeyWalker.ON_LINE) { clear = false; break; }
			}
			if (clear) return new Vec3(x + .5, 181, z + .5);
		}
		throw new AssertionError("No fixed off-ley arena was found for the benchmark seed");
	}

	private void start() {
		check(player.onGround() && !player.isShiftKeyDown() && player.getHealth() == HEALTH, "The fresh player is healthy, grounded and not meditating");
		check(player.getArmorValue() == 20 && player.getAttributeValue(Attributes.ARMOR_TOUGHNESS) == 12,
			"All four real Protection IV netherite pieces have installed their native modifiers");
		check(standardMana(), "Echo/Circle20 must provide the unboosted default 600 mana / 18 mana per second: " + Mana.of(player));
		Spellbooks.setMana(player, Mana.max(player)); // The only refill; observation has not begun.
		master = AuraWorld.SWORD_MASTER.create(player.level(), EntitySpawnReason.COMMAND);
		check(master != null, "The native sustain Master is constructible");
		master.setDiscipline(MastersRules.GALE);
		master.getRandom().setSeed(SEED);
		master.snapTo(origin.x, origin.y, origin.z, 0, 0);
		check(player.level().addFreshEntity(master), "The Master is inserted into the fresh native world");
		master.mobInteract(player, InteractionHand.MAIN_HAND); master.mobInteract(player, InteractionHand.MAIN_HAND);
		check(SwordMaster.ready(player) == 1 && master.challengerCount() == 1, "The sole genuine challenger explicitly enrolls and closes preparation");
		initial = previous = Snapshot.of(player); minimumMana = initial.mana();
		active = this;
		log("settings seed=" + SEED + " windowTicks=" + WINDOW + " school=gale difficulty=" + player.level().getDifficulty()
			+ " initial=" + initial + " manaStats=" + Mana.of(player) + " regenMultiplier=" + Config.regenMultiplier(player)
			+ " defence=" + Config.defence(player) + " startDistance=" + master.distanceTo(player)
			+ " arena=" + origin + " naturalHealthRegen=false strategy=stationary_no_cast_no_guard"
			+ " armour=P4_netherite nativeKnockback=true initialServerTickModulo5=" + player.level().getServer().getTickCount() % 5);
	}

	private boolean standardMana() {
		Mana.Stats stats = Mana.of(player);
		return Heart.active(player) == 20 && stats.tier() == CordTier.ECHO && stats.max() == 600
			&& close(stats.regen(), 18) && close(stats.regenMultiplier(), 1) && !stats.meditating() && !stats.ley() && !stats.well();
	}

	private static void registerObservers() {
		if (registered) return;
		registered = true;
		var before = Wildercord.id("mana_skin_sustain_before");
		var after = Wildercord.id("mana_skin_sustain_after");
		ServerLivingEntityEvents.ALLOW_DAMAGE.addPhaseOrdering(before, Event.DEFAULT_PHASE);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(before, (target, source, amount) -> {
			ManaSkinSustainChecks fixture = active;
			if (fixture != null && !fixture.done && target == fixture.player) fixture.beforeHit(source, amount);
			return true; // Observation only: never veto or alter a damage request.
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.addPhaseOrdering(before, Event.DEFAULT_PHASE);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(before, (target, source, base, taken, blocked) -> {
			ManaSkinSustainChecks fixture = active;
			if (fixture != null && fixture.matches(target, source)) fixture.pending.earlyAfterDamage = Snapshot.of(fixture.player);
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.addPhaseOrdering(Event.DEFAULT_PHASE, after);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(after, (target, source, base, taken, blocked) -> {
			ManaSkinSustainChecks fixture = active;
			if (fixture != null && fixture.matches(target, source)) fixture.finishHit("after_damage", base, taken, blocked);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((target, source) -> {
			ManaSkinSustainChecks fixture = active;
			if (fixture != null && fixture.matches(target, source)) fixture.finishHit("death", Float.NaN, Float.NaN, false);
		});
		ServerTickEvents.END_SERVER_TICK.addPhaseOrdering(Event.DEFAULT_PHASE, after);
		ServerTickEvents.END_SERVER_TICK.register(after, server -> {
			ManaSkinSustainChecks fixture = active;
			if (fixture != null && !fixture.done && fixture.player.level().getServer() == server) fixture.observeTick();
		});
	}

	private boolean matches(Object target, DamageSource source) {
		return !done && target == player && pending != null && pending.source == source;
	}

	private void beforeHit(DamageSource source, float amount) {
		if (source.getEntity() != master || source.getDirectEntity() != master || !source.is(Aura.DAMAGE)) {
			unexpectedDamage++; log("unexpected_damage source=" + source + " amount=" + amount); return;
		}
		if (pending != null) { failure = "An earlier Master damage request did not complete"; return; }
		pending = new Hit(player.level().getGameTime(), source, Snapshot.of(player), amount);
	}

	private void finishHit(String boundary, float base, float taken, boolean blocked) {
		Snapshot result = Snapshot.of(player);
		float paid = pending.before.mana() - result.mana();
		spent += paid; minimumMana = Math.min(minimumMana, result.mana()); hits++;
		hitTicks.add(pending.tick - began);
		// Name the exact observation boundary rather than interpreting Fabric's callbackTaken as
		// mitigated health damage. Both compared implementations run Skin in DEFAULT_PHASE.
		log("hit=" + hits + " tick=" + (pending.tick - began) + " gameTime=" + pending.tick + " boundary=" + boundary
			+ " allowDamageAmount=" + pending.requested + " callbackBase=" + base + " callbackTaken=" + taken + " blocked=" + blocked
			+ " beforeAllow=" + pending.before + " earlyAfterDamage=" + pending.earlyAfterDamage + " final=" + result
			+ " netHealthLost=" + (pending.before.health() - result.health()) + " absorptionLost=" + (pending.before.absorption() - result.absorption())
			+ " manaSpent=" + paid + " alive=" + player.isAlive() + " masterAura=" + master.auraRemaining()
			+ " distance=" + master.distanceTo(player));
		pending = null;
	}

	private void observeTick() {
		long now = player.level().getGameTime();
		if (began < 0) {
			if (!master.started()) return;
			began = now; nativeTicksAtStart = master.tickCount;
			log("trial_started gameTime=" + began + " serverTickModulo5=" + player.level().getServer().getTickCount() % 5);
		}
		long elapsed = now - began;
		if (previousTick >= 0 && now != previousTick + 1) failure = "The observer skipped a server tick";
		previousTick = now;
		if (!standardMana()) failure = "Mana settings changed during observation: " + Mana.of(player);
		if (master.isNoAi() || master.isRemoved()) failure = "The Master stopped naturally ticking inside the observation window";
		if (master.tickCount - nativeTicksAtStart != elapsed) failure = "Native Master entity ticks did not match the observation clock";
		if (player.isAlive() && !master.canHarmParticipant(player)) failure = "The stationary challenger left the live trial boundary";
		if (pending != null) failure = "A Master damage request did not reach AFTER_DAMAGE or AFTER_DEATH";
		observeAttack(now);
		Snapshot current = Snapshot.of(player);
		float paidThisTick = spent - spentAtPreviousTick;
		float restoredThisTick = current.mana() - previous.mana() + paidThisTick;
		if (restoredThisTick < -.001F || restoredThisTick > 4.501F
			|| restoredThisTick > .001F && player.level().getServer().getTickCount() % 5 != 0)
			failure = "Observed mana changed outside the production five-tick regeneration cadence";
		if (restoredThisTick > .001F) { observedRegeneration += restoredThisTick; regenTicks++; }
		minimumMana = Math.min(minimumMana, current.mana());
		if (!current.equals(previous) || elapsed % 20 == 0)
			log("sample tick=" + elapsed + " state=" + current + " manaRestoredThisTick=" + restoredThisTick
				+ " manaSpentThisTick=" + paidThisTick + " masterAura=" + master.auraRemaining() + " guarding=" + master.guarding()
				+ " distance=" + master.distanceTo(player) + " playerPosition=" + player.position().subtract(origin)
				+ " masterPosition=" + master.position().subtract(origin));
		previous = current; spentAtPreviousTick = spent;
		if (failure != null || !player.isAlive() || elapsed >= WINDOW) {
			done = true;
			String result = failure != null ? "INVALID" : player.isAlive() ? "SURVIVED_WINDOW" : "DIED";
			log("result=" + result + " elapsedTicks=" + elapsed + " elapsedSeconds=" + elapsed / 20.0
				+ " observationLimitTicks=" + WINDOW + " nativeMasterTicks=" + (master.tickCount - nativeTicksAtStart)
				+ " attackStarts=" + attacks + " releases=" + releases + " acceptedHits=" + hits + " hitTicks=" + hitTicks
				+ " initial=" + initial + " final=" + current + " minimumMana=" + minimumMana + " totalManaSpent=" + spent
				+ " observedRegeneration=" + observedRegeneration + " regenerationTicks=" + regenTicks + " failure=" + failure);
		}
	}

	private void observeAttack(long now) {
		int animation = master.attackAnimation();
		if (animation == 0) return;
		long start = now - (long) master.attackElapsed(0);
		MastersRules.Move move = MastersRules.Move.values()[animation - 1];
		if (animationBegan != start) {
			animationBegan = start; released = false; attacks++;
			log("attack_start=" + attacks + " tick=" + (start - began) + " move=" + move + " tell=" + master.attackTellTicks()
				+ " recovery=" + move.recovery + " nominalRawDamage=" + MastersRules.damage(1, MastersRules.GALE, move)
				+ " distance=" + master.distanceTo(player) + " masterAura=" + master.auraRemaining());
		}
		if (!released && !master.state(AuraFighter.WINDUP) && master.attackElapsed(0) >= master.attackTellTicks()) {
			released = true; releases++;
			log("attack_release=" + releases + " tick=" + (now - began) + " move=" + move + " attackElapsed=" + master.attackElapsed(0));
		}
	}

	private static void log(String value) { Wildercord.LOGGER.info("[mana-skin-sustain] {}", value); }
	private static boolean close(float a, float b) { return Math.abs(a - b) < .001F; }
	private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
