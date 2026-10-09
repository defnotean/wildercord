package dev.wildercord.cast;

import com.mojang.serialization.Codec;
import dev.wildercord.Wildercord;
import dev.wildercord.player.Mana;
import dev.wildercord.spell.HearthRules;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The hearth pack: gentle, long-lasting self and ally utility for travel, camps and survival (see
 * {@link HearthRules} for the numbers). Timers live in memory by creature, like Wards: a recast refreshes,
 * never stacks, and a restart ends them. Only the Lodestar mark and the Hollow Pocket are saved, as
 * attachments of their own, so older saves load untouched. Monsters never cast these to any end.
 */
public final class HearthEffects {
	private HearthEffects() {}

	static final Set<String> PATHS = Set.of("slowburn", "camp_ward", "warm_cloak", "softsole", "softfoot", "hollow_pocket", "lodestar",
		"homeward", "gravefinder", "skyread", "lullaby", "steedsong", "glidewind", "waymark", "ember_rest", "orbcall", "tinker_hum",
		"lantern_soul", "keenkeep", "landread", "rally_light", "dew_drink", "sunbask", "currentkin", "surefoot", "long_arm", "nightwatch",
		"trailblaze", "hearthpath", "lostfind", "stillwell", "starchart", "petward", "whistle", "luckcharm", "smoke_signal", "wayfarer_hymn",
		"steedmend", "dynamo_stride", "tarry", "clot", "heartsense", "quench", "hearthbond", "springseek", "savor", "deepwarn", "enderhush");

	/** Where Homeward takes you: a block in a world. Kept through death. */
	public static final AttachmentType<GlobalPos> LODESTAR = AttachmentRegistry.create(Wildercord.id("lodestar"),
		b -> b.persistent(GlobalPos.CODEC).copyOnDeath());
	/** The Hollow Pocket's nine slots. Kept through death, like an Ender Chest's. */
	public static final AttachmentType<List<ItemStack>> POCKET = AttachmentRegistry.create(Wildercord.id("hollow_pocket"),
		b -> b.persistent(ItemStack.OPTIONAL_CODEC.listOf()).copyOnDeath());

	private static final net.minecraft.resources.Identifier STEP_ID = Wildercord.id("surefoot");
	private static final net.minecraft.resources.Identifier REACH_ID = Wildercord.id("long_arm");

	/** creature -> rune path -> the server tick it ends. */
	private static final Map<UUID, Map<String, Long>> TIMERS = new HashMap<>();
	private static final Map<UUID, Scratch> SCRATCH = new HashMap<>();
	/** caster -> path -> a place's magic (Camp Ward, Ember Rest, Rally Light, Smoke Signal). */
	private static final Map<UUID, Map<String, Anchor>> ANCHORS = new HashMap<>();
	private static final Map<UUID, ArrayDeque<Anchor>> WAYMARKS = new HashMap<>();
	private static final Map<UUID, Pointer> POINTERS = new HashMap<>();
	private static final Map<UUID, Channel> CHANNELS = new HashMap<>();
	private static final Map<UUID, Long> HOMEWARD_READY = new HashMap<>();
	private static final Map<UUID, Bond> BONDS = new HashMap<>();
	private static final Map<UUID, Long> BOND_CALLED = new HashMap<>();
	private static long now;

	record Anchor(ResourceKey<Level> dim, Vec3 at, long until) {}
	record Pointer(ResourceKey<Level> dim, List<Vec3> to, long until, int color, String label) {}
	record Channel(Vec3 from, float health, long due) {}
	record Bond(Set<UUID> members, long until) {}

	/** What a player's running effects remember between ticks. */
	static final class Scratch {
		float total = -1, burnCarry;
		int lastFood = -1;
		ItemStack tool;
		int toolDamage, keenCarry;
		Vec3 last;
		int still;
		float stillGiven;
		double walked;
		int dynamoGiven, tinkerGiven;
		Vec3 trailLast;
		final ArrayDeque<Vec3> crumbs = new ArrayDeque<>();
		ResourceKey<Level> trailDim;
		BlockPos light;
		ResourceKey<Level> lightDim;
		final Set<Integer> warned = new HashSet<>();
		long warnedAt = -1000;
		float sunHealth = -1;
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(HearthEffects::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.player));
		// Like a potion's, a hearth rune's gifts end with you.
		net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				forget(oldPlayer);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				forget(p);
			}
			SCRATCH.keySet().forEach(id -> unlight(server, SCRATCH.get(id)));
			TIMERS.clear();
			SCRATCH.clear();
			ANCHORS.clear();
			WAYMARKS.clear();
			POINTERS.clear();
			CHANNELS.clear();
			HOMEWARD_READY.clear();
			BONDS.clear();
			BOND_CALLED.clear();
		});
		// Wayfarer's Hymn is for walking, not fighting: striking anything ends it.
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (source.getEntity() instanceof ServerPlayer attacker && active(attacker, "wayfarer_hymn")) {
				timers(attacker).remove("wayfarer_hymn");
				drop(attacker, MobEffects.SPEED);
				drop(attacker, MobEffects.JUMP_BOOST);
			}
		});
	}

	/** Whether the rune is one of this pack's. */
	public static boolean owns(RuneDef rune) {
		return Effects.builtIn(rune) && PATHS.contains(rune.path());
	}

	/** Runs a hearth rune; does nothing for anyone else's. */
	static void apply(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, List<LivingEntity> helped, List<LivingEntity> harmed, double power,
		double duration) {
		RuneDef rune = node.effect;
		if (!owns(rune) || !(cast.caster instanceof ServerPlayer caster)) {
			return;
		}
		ServerLevel level = cast.level;
		now = level.getServer().getTickCount();
		String path = rune.path();
		boolean loud = !cast.passive;
		Vec3 point = hit.point() != null ? hit.point() : caster.position();
		List<ServerPlayer> players = new ArrayList<>();
		for (LivingEntity t : helped) {
			if (t instanceof ServerPlayer p) {
				players.add(p);
			}
		}
		switch (path) {
			// ---- timed buffs on whoever they help
			case "slowburn", "warm_cloak", "softsole", "softfoot", "orbcall", "keenkeep", "dew_drink", "sunbask", "nightwatch", "tarry", "clot",
				"savor", "deepwarn", "enderhush", "tinker_hum", "stillwell", "dynamo_stride", "glidewind", "heartsense", "lantern_soul", "quench" -> {
				int seconds = switch (path) {
					case "slowburn", "warm_cloak", "softsole", "dew_drink", "sunbask", "nightwatch", "enderhush" -> HearthRules.LONG;
					case "clot", "quench", "glidewind" -> path.equals("glidewind") ? 120 : HearthRules.SHORT;
					case "heartsense" -> 30;
					default -> HearthRules.MID;
				};
				for (ServerPlayer p : players) {
					start(p, path, Effects.ticks(seconds, duration));
					if (path.equals("quench")) {
						p.clearFire();
					}
					if (path.equals("warm_cloak")) {
						p.setTicksFrozen(0);
					}
					if (loud) {
						glow(level, p, path);
					}
				}
			}
			case "currentkin", "luckcharm" -> {
				Holder<MobEffect> effect = path.equals("currentkin") ? MobEffects.DOLPHINS_GRACE : MobEffects.LUCK;
				int ticks = Effects.ticks(path.equals("currentkin") ? HearthRules.SHORT : HearthRules.MID, duration);
				for (LivingEntity t : helped) {
					t.addEffect(new MobEffectInstance(effect, ticks, 0, true, true));
					if (loud) {
						glow(level, t, path);
					}
				}
			}
			case "surefoot", "long_arm" -> {
				for (ServerPlayer p : players) {
					start(p, path, Effects.ticks(HearthRules.MID, duration));
					modifier(p, path, true);
					if (loud) {
						glow(level, p, path);
					}
				}
			}
			case "steedsong", "steedmend" -> {
				List<LivingEntity> steeds = new ArrayList<>();
				if (caster.getVehicle() instanceof LivingEntity mount) {
					steeds.add(mount);
				}
				for (LivingEntity t : helped) {
					if (t instanceof Animal && !steeds.contains(t)) {
						steeds.add(t);
					}
				}
				if (steeds.isEmpty()) {
					Casters.tell(caster, Component.translatable("message.wildercord.hearth.no_mount"));
					return;
				}
				int ticks = Effects.ticks(HearthRules.SHORT, duration);
				for (LivingEntity s : steeds) {
					if (path.equals("steedsong")) {
						s.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 1, false, true));
						s.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, ticks, 1, false, true));
					} else {
						s.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 0, false, true));
					}
					glow(level, s, path);
				}
			}
			case "wayfarer_hymn" -> {
				int ticks = Effects.ticks(HearthRules.SHORT, duration);
				for (LivingEntity t : allies(caster, level, caster.position(), 10)) {
					t.addEffect(new MobEffectInstance(MobEffects.SPEED, ticks, 0, false, true));
					t.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, ticks, 0, false, true));
					if (t instanceof ServerPlayer p) {
						start(p, path, ticks);
					}
					glow(level, t, path);
				}
				Fx.sound(level, caster.position(), SoundEvents.NOTE_BLOCK_FLUTE, 0.8f, 1.2f);
			}
			case "petward" -> {
				int ticks = Effects.ticks(HearthRules.SHORT, duration);
				int count = 0;
				for (TamableAnimal pet : level.getEntitiesOfClass(TamableAnimal.class, caster.getBoundingBox().inflate(16),
					a -> a.isTame() && a.getOwner() == caster)) {
					pet.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 0, false, true));
					glow(level, pet, path);
					count++;
				}
				if (count == 0) {
					Casters.tell(caster, Component.translatable("message.wildercord.hearth.no_pets"));
				}
			}
			case "whistle" -> {
				int count = 0;
				for (TamableAnimal pet : level.getEntitiesOfClass(TamableAnimal.class, caster.getBoundingBox().inflate(48),
					a -> a.isTame() && a.getOwner() == caster && !a.isOrderedToSit() && !a.isLeashed() && !a.isPassenger())) {
					Fx.send(level, ParticleTypes.POOF, pet.position().add(0, 0.5, 0), 6, 0.3, 0.02);
					pet.teleportTo(caster.getX(), caster.getY(), caster.getZ());
					pet.getNavigation().stop();
					count++;
				}
				Fx.sound(level, caster.position(), SoundEvents.NOTE_BLOCK_FLUTE, 1f, 1.8f);
				Casters.tell(caster, count == 0 ? Component.translatable("message.wildercord.hearth.no_pets")
					: Component.translatable("message.wildercord.hearth.pets", count));
			}
			case "lullaby" -> {
				for (LivingEntity t : allies(caster, level, caster.position(), 8)) {
					if (t instanceof ServerPlayer p) {
						p.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
						glow(level, p, path);
					}
				}
				int asleep = 0;
				for (ServerPlayer p : level.players()) {
					asleep += p.isSleeping() ? 1 : 0;
				}
				Fx.sound(level, caster.position(), SoundEvents.NOTE_BLOCK_CHIME, 0.7f, 0.6f);
				Casters.tell(caster, Component.translatable("message.wildercord.hearth.lullaby", asleep, level.players().size()));
			}
			case "hearthbond" -> {
				Set<UUID> members = new HashSet<>();
				for (LivingEntity t : allies(caster, level, caster.position(), 12)) {
					if (t instanceof ServerPlayer p) {
						members.add(p.getUUID());
						glow(level, p, path);
					}
				}
				members.add(caster.getUUID());
				Bond old = BONDS.get(caster.getUUID());
				BONDS.put(caster.getUUID(), new Bond(members, HearthRules.refreshed(old == null ? 0 : old.until(), now,
					Effects.ticks(HearthRules.SHORT, duration))));
			}
			// ---- places
			case "camp_ward", "ember_rest", "rally_light", "smoke_signal" -> {
				int seconds = switch (path) {
					case "camp_ward", "ember_rest" -> HearthRules.MID;
					case "rally_light" -> HearthRules.SHORT;
					default -> 120;
				};
				Vec3 at = ground(level, hit, caster);
				ANCHORS.computeIfAbsent(caster.getUUID(), k -> new HashMap<>())
					.put(path, new Anchor(level.dimension(), at, now + Effects.ticks(seconds, duration)));
				glowAt(level, at, path);
			}
			case "waymark" -> {
				ArrayDeque<Anchor> marks = WAYMARKS.computeIfAbsent(caster.getUUID(), k -> new ArrayDeque<>());
				while (marks.size() >= HearthRules.WAYMARKS) {
					marks.removeFirst();
				}
				Vec3 at = ground(level, hit, caster);
				marks.addLast(new Anchor(level.dimension(), at, now + Effects.ticks(HearthRules.LONG, duration)));
				glowAt(level, at, path);
				Casters.tell(caster, Component.translatable("message.wildercord.hearth.waymark", marks.size(), HearthRules.WAYMARKS));
			}
			case "lodestar" -> {
				Vec3 at = ground(level, hit, caster);
				BlockPos pos = BlockPos.containing(at);
				caster.setAttached(LODESTAR, GlobalPos.of(level.dimension(), pos));
				glowAt(level, at, path);
				Fx.sound(level, at, SoundEvents.LODESTONE_COMPASS_LOCK, 1f, 1f);
				Casters.tell(caster, Component.translatable("message.wildercord.hearth.lodestar_set", pos.getX(), pos.getY(), pos.getZ()));
			}
			case "homeward" -> homeward(caster, level);
			case "hollow_pocket" -> pocket(caster);
			// ---- reading the world
			case "skyread" -> skyread(caster, level);
			case "landread" -> landread(caster, level, BlockPos.containing(ground(level, hit, caster)));
			case "starchart" -> {
				BlockPos spawn = level.getRespawnData().pos();
				int far = (int) Math.sqrt(caster.blockPosition().distSqr(spawn));
				BlockPos me = caster.blockPosition();
				Casters.tell(caster, Component.translatable("message.wildercord.hearth.star", me.getX(), me.getY(), me.getZ(),
					caster.getDirection().getName(), far));
				glow(level, caster, path);
			}
			case "gravefinder" -> {
				GlobalPos death = caster.getLastDeathLocation().orElse(null);
				if (death == null) {
					Casters.tell(caster, Component.translatable("message.wildercord.hearth.no_death"));
				} else {
					point(caster, death.dimension(), List.of(Vec3.atCenterOf(death.pos())), Effects.ticks(HearthRules.BRIEF, duration), 0xB0B0C8,
						"message.wildercord.hearth.death_far");
				}
			}
			case "hearthpath" -> {
				ServerPlayer.RespawnConfig respawn = caster.getRespawnConfig();
				ResourceKey<Level> dim = respawn != null ? respawn.respawnData().dimension() : Level.OVERWORLD;
				BlockPos pos = respawn != null ? respawn.respawnData().pos() : level.getServer().overworld().getRespawnData().pos();
				point(caster, dim, List.of(Vec3.atCenterOf(pos)), Effects.ticks(HearthRules.BRIEF, duration), 0xFFB060,
					"message.wildercord.hearth.home_far");
			}
			case "springseek" -> {
				BlockPos water = nearestWater(level, caster.blockPosition(), 24);
				if (water == null) {
					Casters.tell(caster, Component.translatable("message.wildercord.hearth.no_water"));
				} else {
					point(caster, level.dimension(), List.of(Vec3.atCenterOf(water)), Effects.ticks(30, duration), 0x4FA8FF,
						"message.wildercord.hearth.water_far");
				}
			}
			case "lostfind" -> {
				List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, caster.getBoundingBox().inflate(32), e -> true);
				items.sort((a, b) -> Double.compare(a.distanceToSqr(caster), b.distanceToSqr(caster)));
				if (items.isEmpty()) {
					Casters.tell(caster, Component.translatable("message.wildercord.hearth.no_items"));
				} else {
					List<Vec3> to = new ArrayList<>();
					for (int i = 0; i < Math.min(5, items.size()); i++) {
						to.add(items.get(i).position());
					}
					point(caster, level.dimension(), to, Effects.ticks(30, duration), 0xF0E070, null);
				}
			}
			case "trailblaze" -> {
				start(caster, path, Effects.ticks(HearthRules.LONG, duration));
				Scratch s = scratch(caster);
				if (s.trailDim != level.dimension()) {
					s.crumbs.clear();
				}
				s.trailDim = level.dimension();
				s.trailLast = caster.position();
				s.crumbs.addLast(caster.position());
				glow(level, caster, path);
			}
			default -> {
			}
		}
	}

	// ------------------------------------------------------------------ the tick

	private static void tick(MinecraftServer server) {
		now = server.getTickCount();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			Map<String, Long> t = TIMERS.get(p.getUUID());
			if (t != null && !t.isEmpty()) {
				tickPlayer(p, t);
			}
		}
		tickChannels(server);
		if (now % 20 == 0) {
			tickAnchors(server);
			tickWaymarks(server);
			tickBonds(server);
		}
		if (now % 10 == 0) {
			tickPointers(server);
		}
	}

	private static void tickPlayer(ServerPlayer p, Map<String, Long> timers) {
		ServerLevel level = p.level();
		Scratch s = scratch(p);
		// Ended ones go first, and take their leftovers with them.
		Iterator<Map.Entry<String, Long>> it = timers.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Long> e = it.next();
			if (e.getValue() <= now) {
				it.remove();
				end(p, e.getKey(), s);
			}
		}
		Vec3 pos = p.position();
		double moved = s.last == null ? 0 : Math.min(1.0, horizontal(pos, s.last));
		boolean stood = s.last != null && pos.distanceToSqr(s.last) < 1.0E-4;
		s.last = pos;
		FoodData food = p.getFoodData();
		int foodNow = food.getFoodLevel();
		if (timers.containsKey("savor") && s.lastFood >= 0 && foodNow > s.lastFood) {
			food.setSaturation(HearthRules.savor(foodNow - s.lastFood, foodNow, food.getSaturationLevel()));
		}
		s.lastFood = foodNow;
		if (timers.containsKey("slowburn")) {
			float total = food.getFoodLevel() + food.getSaturationLevel();
			if (s.total >= 0 && total < s.total) {
				float[] r = HearthRules.slowburn(s.total - total, s.burnCarry);
				float room = Math.max(0, food.getFoodLevel() - food.getSaturationLevel());
				float back = Math.min(r[0], room);
				food.setSaturation(food.getSaturationLevel() + back);
				s.burnCarry = Math.min(4, r[1] + (r[0] - back));
			}
			s.total = food.getFoodLevel() + food.getSaturationLevel();
		} else {
			s.total = -1;
		}
		if (timers.containsKey("warm_cloak") && p.getTicksFrozen() > 0) {
			p.setTicksFrozen(0);
		}
		if (timers.containsKey("softsole") && p.fallDistance > 0 && p.fallDistance < 1.5 && overFarmland(level, p.blockPosition())) {
			p.resetFallDistance();
		}
		if (timers.containsKey("quench") && p.getRemainingFireTicks() > 0 && !p.isInLava()) {
			p.setRemainingFireTicks(Math.max(0, p.getRemainingFireTicks() - 2));
		}
		if (timers.containsKey("keenkeep")) {
			keenkeep(p, s);
		} else {
			s.tool = null;
		}
		if (timers.containsKey("lantern_soul")) {
			lantern(p, level, s);
		}
		if (timers.containsKey("stillwell")) {
			s.still = stood ? s.still + 1 : 0;
			if (now % 20 == 0) {
				float gain = HearthRules.stillwell(s.still, s.stillGiven);
				if (gain > 0) {
					Mana.restore(p, gain);
					s.stillGiven += gain;
					if (now % 60 == 0) {
						Fx.sendParticles(level, p, ParticleTypes.ENCHANT, false, false, p.getX(), p.getY() + 1, p.getZ(), 4, 0.4, 0.4, 0.4, 0.2);
					}
				}
			}
		}
		if (timers.containsKey("dynamo_stride") && p.onGround() && p.getVehicle() == null && !p.isFallFlying()) {
			s.walked += moved;
			int gain = HearthRules.dynamo(s.walked, s.dynamoGiven);
			if (s.walked >= HearthRules.DYNAMO_STEP) {
				s.walked -= HearthRules.DYNAMO_STEP;
				if (gain > 0) {
					int one = Math.min(gain, HearthRules.DYNAMO_MANA);
					Mana.restore(p, one);
					s.dynamoGiven += one;
					Fx.sendParticles(level, p, ParticleTypes.ELECTRIC_SPARK, false, false, p.getX(), p.getY() + 0.2, p.getZ(), 6, 0.3, 0.1, 0.3, 0.05);
				}
			}
		}
		if (timers.containsKey("trailblaze") && s.trailLast != null && s.trailDim == level.dimension()
			&& HearthRules.crumb(pos.distanceToSqr(s.trailLast))) {
			s.trailLast = pos;
			s.crumbs.addLast(pos);
			while (s.crumbs.size() > HearthRules.TRAIL_CRUMBS) {
				s.crumbs.removeFirst();
			}
		}
		if (timers.containsKey("glidewind") && now % 10 == 0 && p.isFallFlying()) {
			Vec3 v = p.getKnownMovement();
			double push = HearthRules.push(v.length(), HearthRules.GLIDE_CAP, HearthRules.GLIDE_PUSH * 3);
			if (push > 0) {
				p.setDeltaMovement(v.add(p.getLookAngle().scale(push)));
				p.connection.send(new ClientboundSetEntityMotionPacket(p));
				Fx.send(level, ParticleTypes.CLOUD, p.position(), 2, 0.2, 0.01);
			}
		}
		if (timers.containsKey("tinker_hum") && now % HearthRules.TINKER_EVERY == 0 && s.tinkerGiven < HearthRules.TINKER_MAX) {
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				ItemStack stack = p.getItemBySlot(slot);
				if (stack.isDamageableItem() && stack.isDamaged()) {
					stack.setDamageValue(stack.getDamageValue() - 1);
					s.tinkerGiven++;
					Fx.sendParticles(level, p, ParticleTypes.WAX_ON, false, false, p.getX(), p.getY() + 1, p.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
					break;
				}
			}
		}
		if (now % 300 == 0 && timers.containsKey("dew_drink") && p.isInWaterOrRain() && food.getFoodLevel() < 20) {
			food.eat(1, 0f);
			Fx.sendParticles(level, p, ParticleTypes.SPLASH, false, false, p.getX(), p.getY() + 1.6, p.getZ(), 5, 0.2, 0.1, 0.2, 0.05);
		}
		if (now % 120 == 0 && timers.containsKey("sunbask")) {
			BlockPos head = BlockPos.containing(p.getEyePosition());
			boolean sun = level.isBrightOutside() && level.canSeeSky(head) && !level.isRainingAt(head);
			if (sun && p.getHealth() >= s.sunHealth && p.getHealth() < p.getMaxHealth()) {
				p.heal(1);
				Fx.send(level, ParticleTypes.WAX_OFF, p.position().add(0, 1.2, 0), 3, 0.3, 0.02);
			}
			s.sunHealth = p.getHealth();
		}
		if (now % 5 == 0 && timers.containsKey("orbcall")) {
			for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, p.getBoundingBox().inflate(10), o -> true)) {
				Vec3 to = p.position().add(0, 0.5, 0).subtract(orb.position());
				if (to.lengthSqr() > 0.25) {
					orb.setDeltaMovement(orb.getDeltaMovement().add(to.normalize().scale(0.12)));
				}
			}
		}
		if (now % 40 == 0 && timers.containsKey("tarry")) {
			for (MobEffectInstance e : new ArrayList<>(p.getActiveEffects())) {
				int extra = e.getEffect().value().isBeneficial() ? HearthRules.tarry(e.getDuration(), e.isInfiniteDuration()) : 0;
				if (extra > 0) {
					p.addEffect(new MobEffectInstance(e.getEffect(), e.getDuration() + extra, e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon()));
				}
			}
		}
		if (now % 20 == 0 && timers.containsKey("clot")) {
			for (Holder<MobEffect> bad : List.of(MobEffects.POISON, MobEffects.WITHER)) {
				MobEffectInstance e = p.getEffect(bad);
				if (e != null && !e.isInfiniteDuration() && e.getDuration() > 1) {
					p.removeEffect(bad);
					p.addEffect(new MobEffectInstance(bad, HearthRules.clot(e.getDuration()), e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon()));
				}
			}
		}
		if (now % 20 == 0 && timers.containsKey("heartsense")) {
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(24), e -> e != p && e.isAlive())) {
				Fx.sendParticles(level, p, Fx.dust(0xD03040, 1.2f), false, true, e.getX(), e.getY() + e.getBbHeight() * 0.7, e.getZ(), 2, 0.1, 0.1, 0.1, 0);
			}
		}
		if (now % 20 == 0 && (timers.containsKey("surefoot") || timers.containsKey("long_arm"))) {
			modifier(p, "surefoot", timers.containsKey("surefoot"));
			modifier(p, "long_arm", timers.containsKey("long_arm"));
		}
		if (now % 10 == 0) {
			watch(p, level, s, timers);
		}
		if (now % 20 == 0 && timers.containsKey("deepwarn")) {
			deepwarn(p, level, s);
		}
		if (now % 20 == 0 && timers.containsKey("trailblaze") && s.trailDim == level.dimension()) {
			for (Vec3 c : s.crumbs) {
				if (c.distanceToSqr(pos) < 48 * 48) {
					Fx.sendParticles(level, p, Fx.dust(0x8FD86A, 1.0f), true, true, c.x, c.y + 0.2, c.z, 1, 0.05, 0.05, 0.05, 0);
				}
			}
		}
	}

	/** Softfoot, Nightwatch and Enderhush: what the monsters around you are doing about you. */
	private static void watch(ServerPlayer p, ServerLevel level, Scratch s, Map<String, Long> timers) {
		boolean softfoot = timers.containsKey("softfoot"), nightwatch = timers.containsKey("nightwatch"), hush = timers.containsKey("enderhush");
		if (!softfoot && !nightwatch && !hush) {
			return;
		}
		Set<Integer> hunting = new HashSet<>();
		for (Mob mob : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32), m -> m instanceof Enemy && m.isAlive())) {
			if (Spirits.isBoss(mob)) {
				continue;
			}
			int since = mob.getLastHurtByMob() == p ? mob.tickCount - mob.getLastHurtByMobTimestamp() : Integer.MAX_VALUE;
			boolean targeting = mob.getTarget() == p;
			if (hush && mob instanceof Enderman man && (targeting || man.isAngryAt(p, level)) && since > HearthRules.GRUDGE_TICKS) {
				man.stopBeingAngry();
				man.setTarget(null);
				continue;
			}
			if (!targeting) {
				continue;
			}
			if (softfoot && HearthRules.loses(mob.distanceToSqr(p), since)) {
				mob.setTarget(null);
				continue;
			}
			if (nightwatch && mob.distanceToSqr(p) < 16 * 16) {
				hunting.add(mob.getId());
				if (s.warned.add(mob.getId()) && HearthRules.ready(s.warnedAt, now, 40)) {
					s.warnedAt = now;
					Fx.sound(level, p.position(), SoundEvents.NOTE_BLOCK_BELL, 0.6f, 1.6f);
					Casters.tell(p, Component.translatable("message.wildercord.hearth.hunted"));
				}
			}
		}
		if (nightwatch) {
			s.warned.retainAll(hunting);
		}
	}

	private static void deepwarn(ServerPlayer p, ServerLevel level, Scratch s) {
		if (!HearthRules.ready(s.warnedAt, now, 60)) {
			return;
		}
		BlockPos feet = p.blockPosition();
		String warn = null;
		search:
		for (int dx = -2; dx <= 2; dx++) {
			for (int dz = -2; dz <= 2; dz++) {
				for (int dy = 1; dy <= 6; dy++) {
					if (level.getFluidState(feet.offset(dx, -dy, dz)).is(FluidTags.LAVA)) {
						warn = "message.wildercord.hearth.lava";
						break search;
					}
				}
			}
		}
		if (warn == null && p.onGround()) {
			Vec3 look = Effects.horizontal(p.getLookAngle(), new Vec3(1, 0, 0));
			BlockPos ahead = BlockPos.containing(p.position().add(look.scale(1.5)));
			int air = 0;
			for (int dy = 1; dy <= 8 && level.getBlockState(ahead.below(dy)).getCollisionShape(level, ahead.below(dy)).isEmpty()
				&& level.getFluidState(ahead.below(dy)).isEmpty(); dy++) {
				air++;
			}
			if (level.getBlockState(ahead).getCollisionShape(level, ahead).isEmpty() && HearthRules.drop(air)) {
				warn = "message.wildercord.hearth.drop";
			}
		}
		if (warn != null) {
			s.warnedAt = now;
			Fx.sound(level, p.position(), SoundEvents.NOTE_BLOCK_BASS, 0.7f, 0.6f);
			Casters.tell(p, Component.translatable(warn));
		}
	}

	private static void keenkeep(ServerPlayer p, Scratch s) {
		ItemStack tool = p.getMainHandItem();
		if (!tool.isDamageableItem()) {
			s.tool = null;
			return;
		}
		if (tool == s.tool && tool.getDamageValue() > s.toolDamage) {
			int[] kept = HearthRules.keenkeep(tool.getDamageValue() - s.toolDamage, s.keenCarry);
			s.keenCarry = kept[1];
			if (kept[0] > 0) {
				tool.setDamageValue(tool.getDamageValue() - kept[0]);
			}
		}
		s.tool = tool;
		s.toolDamage = tool.getDamageValue();
	}

	/** Lantern Soul: an unseen light that moves with your head, taken down behind you. */
	private static void lantern(ServerPlayer p, ServerLevel level, Scratch s) {
		BlockPos head = BlockPos.containing(p.getEyePosition());
		if (head.equals(s.light) && level.dimension() == s.lightDim) {
			if (now % 100 == 0 && level.getBlockState(head).is(Blocks.LIGHT)) {
				TemporaryBlocks.put(level, head, level.getBlockState(head), Blocks.AIR.defaultBlockState(), level.getGameTime() + 200);
			}
			return;
		}
		unlight(level.getServer(), s);
		if (level.getBlockState(head).isAir() && Casters.mayBuild(p) && level.mayInteract(p, head)) {
			BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 14);
			level.setBlock(head, light, 3);
			TemporaryBlocks.put(level, head, light, Blocks.AIR.defaultBlockState(), level.getGameTime() + 200);
			s.light = head;
			s.lightDim = level.dimension();
		}
	}

	private static void unlight(MinecraftServer server, Scratch s) {
		if (s == null || s.light == null) {
			return;
		}
		ServerLevel level = server.getLevel(s.lightDim);
		if (level != null && level.isLoaded(s.light) && level.getBlockState(s.light).is(Blocks.LIGHT)) {
			TemporaryBlocks.remove(level, s.light);
			level.setBlock(s.light, Blocks.AIR.defaultBlockState(), 3);
		}
		s.light = null;
	}

	private static void end(ServerPlayer p, String path, Scratch s) {
		switch (path) {
			case "lantern_soul" -> unlight(p.level().getServer(), s);
			case "surefoot", "long_arm" -> modifier(p, path, false);
			case "trailblaze" -> s.crumbs.clear();
			case "stillwell" -> s.stillGiven = 0;
			case "dynamo_stride" -> {
				s.dynamoGiven = 0;
				s.walked = 0;
			}
			case "tinker_hum" -> s.tinkerGiven = 0;
			default -> {
			}
		}
	}

	private static void tickChannels(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Channel>> it = CHANNELS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Channel> e = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			Channel c = e.getValue();
			if (p == null || !p.isAlive()) {
				it.remove();
				continue;
			}
			ServerLevel level = p.level();
			if (p.position().distanceToSqr(c.from()) > 0.25 || p.getHealth() < c.health()) {
				it.remove();
				Fx.sound(level, p.position(), SoundEvents.CANDLE_EXTINGUISH, 0.8f, 1f);
				Casters.tell(p, Component.translatable("message.wildercord.hearth.homeward_broken"));
				continue;
			}
			if (now % 5 == 0) {
				Fx.send(level, ParticleTypes.REVERSE_PORTAL, p.position().add(0, 1, 0), 6, 0.4, 0.05);
			}
			if (now < c.due()) {
				continue;
			}
			it.remove();
			GlobalPos home = p.getAttached(LODESTAR);
			if (home == null || !HearthRules.homeward(home.dimension() == level.dimension(), home.pos().distSqr(p.blockPosition()))) {
				Casters.tell(p, Component.translatable("message.wildercord.hearth.lodestar_far"));
				continue;
			}
			Vec3 to = Vec3.atBottomCenterOf(home.pos());
			if (!level.noCollision(p, p.getBoundingBox().move(to.subtract(p.position())))) {
				Casters.tell(p, Component.translatable("message.wildercord.hearth.homeward_blocked"));
				continue;
			}
			Fx.send(level, ParticleTypes.PORTAL, p.position().add(0, 1, 0), 24, 0.5, 0.2);
			p.teleportTo(to.x, to.y, to.z);
			p.resetFallDistance();
			HOMEWARD_READY.put(p.getUUID(), now + HearthRules.HOMEWARD_REST);
			Fx.send(level, ParticleTypes.REVERSE_PORTAL, to.add(0, 1, 0), 24, 0.5, 0.2);
			Fx.sound(level, to, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.2f);
		}
	}

	private static void tickAnchors(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Map<String, Anchor>>> owners = ANCHORS.entrySet().iterator();
		while (owners.hasNext()) {
			Map.Entry<UUID, Map<String, Anchor>> o = owners.next();
			ServerPlayer owner = server.getPlayerList().getPlayer(o.getKey());
			o.getValue().entrySet().removeIf(a -> a.getValue().until() <= now || owner == null);
			if (o.getValue().isEmpty()) {
				owners.remove();
				continue;
			}
			for (Map.Entry<String, Anchor> e : o.getValue().entrySet()) {
				Anchor a = e.getValue();
				ServerLevel level = server.getLevel(a.dim());
				if (level == null || !level.isLoaded(BlockPos.containing(a.at()))) {
					continue;
				}
				anchor(owner, level, e.getKey(), a.at());
			}
		}
	}

	private static void anchor(ServerPlayer owner, ServerLevel level, String path, Vec3 at) {
		switch (path) {
			case "camp_ward" -> {
				AABB ward = new AABB(at, at).inflate(HearthRules.WARD_RADIUS);
				for (Mob mob : level.getEntitiesOfClass(Mob.class, ward, m -> m instanceof Enemy && !Spirits.isBoss(m)
					&& HearthRules.freshSpawn(m.tickCount, m.isPersistenceRequired() || m.requiresCustomPersistence(), m.hasCustomName()))) {
					Fx.send(level, ParticleTypes.POOF, mob.position().add(0, 0.5, 0), 8, 0.3, 0.02);
					mob.discard();
				}
				for (Phantom phantom : level.getEntitiesOfClass(Phantom.class, new AABB(at, at).inflate(24, 48, 24), m -> true)) {
					phantom.setTarget(null);
					phantom.setDeltaMovement(phantom.getDeltaMovement().add(0, 0.6, 0));
				}
				if (now % 40 == 0) {
					for (int i = 0; i < 16; i++) {
						double ang = i * Math.PI / 8;
						Fx.send(level, Fx.dust(0x9A7CFF, 1.0f), at.add(Math.cos(ang) * HearthRules.WARD_RADIUS, 0.3, Math.sin(ang) * HearthRules.WARD_RADIUS),
							1, 0.05, 0);
					}
				}
			}
			case "ember_rest" -> {
				Fx.send(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, at.add(0, 0.3, 0), 1, 0.1, 0.01);
				Fx.send(level, ParticleTypes.FLAME, at.add(0, 0.2, 0), 3, 0.2, 0.01);
				if (now % 80 == 0) {
					for (LivingEntity t : allies(owner, level, at, HearthRules.EMBER_RADIUS)) {
						t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, true));
						t.setTicksFrozen(0);
					}
					Fx.sound(level, at, SoundEvents.CAMPFIRE_CRACKLE, 0.8f, 1f);
				}
			}
			case "rally_light" -> {
				for (int y = 0; y < 8; y++) {
					Fx.send(level, ParticleTypes.END_ROD, at.add(0, y, 0), 1, 0.05, 0.01);
				}
				if (now % 80 == 0) {
					for (LivingEntity t : allies(owner, level, at, HearthRules.RALLY_RADIUS)) {
						t.addEffect(new MobEffectInstance(MobEffects.HASTE, 120, 0, true, true));
						t.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 0, true, true));
					}
					Fx.sound(level, at, SoundEvents.BEACON_AMBIENT, 0.6f, 1.4f);
				}
			}
			case "smoke_signal" -> Fx.sendFar(level, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, at.add(0, 0.5, 0));
			default -> {
			}
		}
	}

	private static void tickWaymarks(MinecraftServer server) {
		Iterator<Map.Entry<UUID, ArrayDeque<Anchor>>> it = WAYMARKS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, ArrayDeque<Anchor>> e = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			e.getValue().removeIf(a -> a.until() <= now);
			if (p == null || e.getValue().isEmpty()) {
				it.remove();
				continue;
			}
			for (Anchor a : e.getValue()) {
				if (a.dim() == p.level().dimension() && a.at().distanceToSqr(p.position()) < HearthRules.WAYMARK_SIGHT * HearthRules.WAYMARK_SIGHT) {
					for (int y = 0; y < 12; y += 2) {
						Fx.sendParticles(p.level(), p, ParticleTypes.END_ROD, true, true, a.at().x, a.at().y + y, a.at().z, 1, 0.02, 0.3, 0.02, 0);
					}
				}
			}
		}
	}

	private static void tickPointers(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Pointer>> it = POINTERS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Pointer> e = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			Pointer ptr = e.getValue();
			if (p == null || ptr.until() <= now) {
				it.remove();
				continue;
			}
			if (ptr.dim() != p.level().dimension()) {
				continue;
			}
			Vec3 eye = p.getEyePosition().add(0, -0.3, 0);
			for (Vec3 to : ptr.to()) {
				Vec3 dir = to.subtract(eye);
				if (dir.lengthSqr() < 1) {
					continue;
				}
				dir = dir.normalize();
				for (double d = 1.2; d <= 3.2; d += 0.5) {
					Vec3 at = eye.add(dir.scale(d));
					Fx.sendParticles(p.level(), p, Fx.dust(ptr.color(), 0.8f), false, true, at.x, at.y, at.z, 1, 0, 0, 0, 0);
				}
			}
			if (ptr.label() != null && now % 40 == 0) {
				Casters.tell(p, Component.translatable(ptr.label(), (int) Math.sqrt(ptr.to().get(0).distanceToSqr(p.position()))));
			}
		}
	}

	private static void tickBonds(MinecraftServer server) {
		Iterator<Map.Entry<UUID, Bond>> it = BONDS.entrySet().iterator();
		while (it.hasNext()) {
			Bond b = it.next().getValue();
			if (b.until() <= now) {
				it.remove();
				continue;
			}
			for (UUID id : b.members()) {
				ServerPlayer hurt = server.getPlayerList().getPlayer(id);
				if (hurt == null || !hurt.isAlive() || hurt.getHealth() >= HearthRules.BOND_LOW
					|| !HearthRules.ready(BOND_CALLED.getOrDefault(id, -100000L), now, HearthRules.BOND_REST)) {
					continue;
				}
				BOND_CALLED.put(id, now);
				hurt.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, true));
				Fx.send(hurt.level(), ParticleTypes.HEART, hurt.position().add(0, 1.8, 0), 4, 0.3, 0.02);
				for (UUID other : b.members()) {
					ServerPlayer ally = server.getPlayerList().getPlayer(other);
					if (ally != null && ally != hurt) {
						Fx.sound(ally.level(), ally.position(), SoundEvents.NOTE_BLOCK_BELL, 0.8f, 0.8f);
						Casters.tell(ally, Component.translatable("message.wildercord.hearth.bond_low", hurt.getDisplayName()));
						if (ally.level() == hurt.level()) {
							Fx.sendParticles(hurt.level(), ally, ParticleTypes.END_ROD, true, true, hurt.getX(), hurt.getY() + 2.5, hurt.getZ(), 1, 0, 0, 0, 0);
						}
					}
				}
			}
		}
	}

	// ------------------------------------------------------------------ pieces

	private static void homeward(ServerPlayer caster, ServerLevel level) {
		long ready = HOMEWARD_READY.getOrDefault(caster.getUUID(), 0L);
		if (ready > now) {
			Casters.tell(caster, Component.translatable("message.wildercord.hearth.homeward_rest", (ready - now + 19) / 20));
			return;
		}
		GlobalPos home = caster.getAttached(LODESTAR);
		if (home == null) {
			Casters.tell(caster, Component.translatable("message.wildercord.hearth.no_lodestar"));
			return;
		}
		if (!HearthRules.homeward(home.dimension() == level.dimension(), home.pos().distSqr(caster.blockPosition()))) {
			Casters.tell(caster, Component.translatable("message.wildercord.hearth.lodestar_far"));
			return;
		}
		CHANNELS.put(caster.getUUID(), new Channel(caster.position(), caster.getHealth(), now + HearthRules.HOMEWARD_CHANNEL));
		Fx.sound(level, caster.position(), SoundEvents.PORTAL_TRIGGER, 0.3f, 1.6f);
		Casters.tell(caster, Component.translatable("message.wildercord.hearth.homeward_start"));
	}

	private static void pocket(ServerPlayer caster) {
		List<ItemStack> saved = caster.getAttachedOrElse(POCKET, List.of());
		SimpleContainer box = new SimpleContainer(HearthRules.POCKET_SLOTS) {
			@Override
			public void setChanged() {
				super.setChanged();
				List<ItemStack> out = new ArrayList<>(getContainerSize());
				for (int i = 0; i < getContainerSize(); i++) {
					out.add(getItem(i).copy());
				}
				caster.setAttached(POCKET, out);
			}
		};
		for (int i = 0; i < Math.min(HearthRules.POCKET_SLOTS, saved.size()); i++) {
			box.setItem(i, saved.get(i).copy());
		}
		caster.openMenu(new SimpleMenuProvider((id, inv, p) -> new ChestMenu(MenuType.GENERIC_9x1, id, inv, box, 1),
			Component.translatable("container.wildercord.hollow_pocket")));
		Fx.sound(caster.level(), caster.position(), SoundEvents.BUNDLE_DROP_CONTENTS, 0.7f, 0.8f);
	}

	private static void skyread(ServerPlayer caster, ServerLevel level) {
		ServerLevel sky = level.getServer().overworld();
		var weather = sky.getWeatherData();
		String key;
		int ticks;
		if (weather.isThundering()) {
			key = "message.wildercord.hearth.weather_storm";
			ticks = weather.getThunderTime();
		} else if (weather.isRaining()) {
			key = "message.wildercord.hearth.weather_rain";
			ticks = weather.getRainTime();
		} else {
			key = "message.wildercord.hearth.weather_clear";
			ticks = weather.getClearWeatherTime() > 0 ? weather.getClearWeatherTime() : weather.getRainTime();
		}
		long time = sky.getOverworldClockTime();
		caster.sendSystemMessage(Component.translatable(key, HearthRules.minutes(ticks)));
		caster.sendSystemMessage(Component.translatable("message.wildercord.hearth.time", HearthRules.hour(time), HearthRules.moon(time) + 1));
		Fx.send(level, ParticleTypes.CLOUD, caster.position().add(0, 2.2, 0), 6, 0.4, 0.01);
	}

	private static void landread(ServerPlayer caster, ServerLevel level, BlockPos pos) {
		String biome = level.getBiome(pos).unwrapKey().map(k -> k.identifier().getPath().replace('_', ' ')).orElse("?");
		int light = level.getMaxLocalRawBrightness(pos);
		caster.sendSystemMessage(Component.translatable("message.wildercord.hearth.land", biome, pos.getY(), light));
		if (level.dimension() == Level.OVERWORLD
			&& WorldgenRandom.seedSlimeChunk(pos.getX() >> 4, pos.getZ() >> 4, level.getSeed(), 987234911L).nextInt(10) == 0) {
			caster.sendSystemMessage(Component.translatable("message.wildercord.hearth.slime"));
		}
		Fx.send(level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(pos), 6, 0.4, 0.01);
	}

	private static BlockPos nearestWater(ServerLevel level, BlockPos from, int radius) {
		BlockPos best = null;
		double bestSqr = Double.MAX_VALUE;
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int dy = -8; dy <= 8; dy++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					at.set(from.getX() + dx, from.getY() + dy, from.getZ() + dz);
					double d = dx * dx + dy * dy + dz * dz;
					if (d < bestSqr && level.isLoaded(at) && level.getFluidState(at).is(FluidTags.WATER) && level.getFluidState(at).isSource()) {
						best = at.immutable();
						bestSqr = d;
					}
				}
			}
		}
		return best;
	}

	private static void point(ServerPlayer p, ResourceKey<Level> dim, List<Vec3> to, int ticks, int color, String label) {
		if (dim != p.level().dimension()) {
			Casters.tell(p, Component.translatable("message.wildercord.hearth.other_world"));
			return;
		}
		POINTERS.put(p.getUUID(), new Pointer(dim, List.copyOf(to), now + ticks, color, label));
		Fx.sound(p.level(), p.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.2f);
	}

	private static boolean overFarmland(ServerLevel level, BlockPos feet) {
		for (int dy = 0; dy <= 3; dy++) {
			if (level.getBlockState(feet.below(dy)).getBlock() instanceof FarmlandBlock) {
				return true;
			}
		}
		return false;
	}

	private static void modifier(ServerPlayer p, String path, boolean on) {
		boolean step = path.equals("surefoot");
		AttributeInstance attr = p.getAttribute(step ? Attributes.STEP_HEIGHT : Attributes.BLOCK_INTERACTION_RANGE);
		if (attr == null) {
			return;
		}
		net.minecraft.resources.Identifier id = step ? STEP_ID : REACH_ID;
		if (on) {
			if (!attr.hasModifier(id)) {
				attr.addTransientModifier(new AttributeModifier(id, step ? 0.45 : 2.0, AttributeModifier.Operation.ADD_VALUE));
			}
		} else {
			attr.removeModifier(id);
		}
	}

	/** Where a place's magic sits: the face of the block struck, else the caster's feet. */
	private static Vec3 ground(ServerLevel level, Cast.Hit hit, LivingEntity caster) {
		if (hit.self() || hit.point() == null) {
			return caster.position();
		}
		if (hit.block() != null && hit.face() != null) {
			return Vec3.atBottomCenterOf(hit.block().relative(hit.face()));
		}
		return hit.point();
	}

	private static List<LivingEntity> allies(LivingEntity owner, ServerLevel level, Vec3 at, double radius) {
		return level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
			e -> e.isAlive() && e.distanceToSqr(at) <= radius * radius && (e == owner || Targets.canHelp(owner, e)));
	}

	private static void drop(ServerPlayer p, Holder<MobEffect> effect) {
		MobEffectInstance e = p.getEffect(effect);
		if (e != null && e.getAmplifier() == 0 && e.getDuration() <= HearthRules.SHORT * 20) {
			p.removeEffect(effect);
		}
	}

	private static double horizontal(Vec3 a, Vec3 b) {
		double dx = a.x - b.x, dz = a.z - b.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	private static void glow(ServerLevel level, LivingEntity t, String path) {
		ParticleOptions particle = particle(path);
		Fx.send(level, particle, t.position().add(0, t.getBbHeight() * 0.6, 0), 10, 0.4, 0.02);
		if (HearthFeels.LIFE.contains(path)) {
			// The Life hearth runes land with their own authored voice (tools/feel/life_outcomes_audio.py).
			dev.wildercord.cast.feel.Feels.sound(level, t.position(), "life_auth_" + path + "_outcome", 0.5f, 1.0f);
		} else {
			Fx.sound(level, t.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5f, 1.4f);
		}
	}

	private static void glowAt(ServerLevel level, Vec3 at, String path) {
		Fx.send(level, particle(path), at.add(0, 0.5, 0), 16, 0.5, 0.02);
		Fx.sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.0f);
	}

	private static ParticleOptions particle(String path) {
		return switch (path) {
			case "warm_cloak", "sunbask", "ember_rest", "smoke_signal", "quench" -> ParticleTypes.SMALL_FLAME;
			case "currentkin", "dew_drink", "springseek" -> ParticleTypes.SPLASH;
			case "softfoot", "enderhush", "hollow_pocket", "homeward" -> ParticleTypes.REVERSE_PORTAL;
			case "slowburn", "lullaby", "trailblaze", "hearthbond", "steedmend", "petward" -> ParticleTypes.HAPPY_VILLAGER;
			case "softsole", "steedsong", "glidewind", "wayfarer_hymn", "whistle" -> ParticleTypes.CLOUD;
			case "dynamo_stride", "skyread" -> ParticleTypes.ELECTRIC_SPARK;
			case "clot", "heartsense" -> Fx.dust(0xB02030, 1.0f);
			case "tarry", "savor" -> ParticleTypes.ENCHANT;
			default -> ParticleTypes.END_ROD;
		};
	}

	private static void start(ServerPlayer p, String path, int ticks) {
		timers(p).merge(path, HearthRules.refreshed(0, now, ticks), Math::max);
	}

	private static Map<String, Long> timers(LivingEntity e) {
		return TIMERS.computeIfAbsent(e.getUUID(), k -> new HashMap<>());
	}

	private static Scratch scratch(LivingEntity e) {
		return SCRATCH.computeIfAbsent(e.getUUID(), k -> new Scratch());
	}

	/** Whether a timed hearth rune is running on this creature (for tests and the Hymn's end). */
	public static boolean active(LivingEntity e, String path) {
		Map<String, Long> t = TIMERS.get(e.getUUID());
		Long until = t == null ? null : t.get(path);
		return until != null && until > now;
	}

	/** The waymarks a player has standing (for tests). */
	public static int waymarks(LivingEntity e) {
		ArrayDeque<Anchor> marks = WAYMARKS.get(e.getUUID());
		return marks == null ? 0 : marks.size();
	}

	/** Whether this caster has a place's magic standing (for tests). */
	public static boolean anchored(LivingEntity e, String path) {
		Map<String, Anchor> a = ANCHORS.get(e.getUUID());
		return a != null && a.containsKey(path);
	}

	/** Whether this player has motes pointing somewhere (for tests). */
	public static boolean pointing(LivingEntity e) {
		return POINTERS.containsKey(e.getUUID());
	}

	/** Whether this player is channelling Homeward (for tests). */
	public static boolean channelling(LivingEntity e) {
		return CHANNELS.containsKey(e.getUUID());
	}

	/** Ends every hearth rune this player has going, places and marks too (for tests that share one player). */
	public static void reset(ServerPlayer p) {
		forget(p);
		ANCHORS.remove(p.getUUID());
		WAYMARKS.remove(p.getUUID());
		BONDS.remove(p.getUUID());
		HOMEWARD_READY.remove(p.getUUID());
	}

	private static void forget(ServerPlayer p) {
		Scratch s = SCRATCH.remove(p.getUUID());
		if (s != null && p.level().getServer() != null) {
			unlight(p.level().getServer(), s);
		}
		Map<String, Long> t = TIMERS.remove(p.getUUID());
		if (t != null) {
			modifier(p, "surefoot", false);
			modifier(p, "long_arm", false);
		}
		CHANNELS.remove(p.getUUID());
		POINTERS.remove(p.getUUID());
	}
}
