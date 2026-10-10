package dev.wildercord.monster;

import dev.wildercord.Wildercord;
import dev.wildercord.aura.Aura;
import dev.wildercord.player.Heart;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Wakes roaming giants near travellers, and runs them: their stomps, their wandering, their bars and their hearts. See {@link GiantRules}. */
public final class Giants {
	private Giants() {}

	/** Every giant carries this tag. */
	public static final String TAG = "wildercord_giant";
	private static final Identifier SCALE = Wildercord.id("giant_scale");
	private static final Identifier STURDY = Wildercord.id("giant_sturdy");

	/** What a giant drops: a heart as big as a fist, still warm. */
	public static final Item GIANT_HEART = heart();

	private static Item heart() {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Wildercord.id("giant_heart"));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key).rarity(Rarity.RARE)));
	}

	/** The giants loaded right now, each with its bar. */
	private static final Map<UUID, Tracked> GIANTS = new HashMap<>();

	private record Tracked(Mob mob, ServerBossEvent bar) {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % GiantRules.CHECK_TICKS == 0 && server.getTickCount() > 0) check(server);
			tick(server);
		});
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mob mob && is(mob)) track(mob);
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> forget(entity.getUUID()));
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof Mob mob && is(mob) && mob.level() instanceof ServerLevel level) fall(level, mob);
		});
		CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Wildercord.id("wildercord")))
			.register(output -> output.accept(GIANT_HEART));
	}

	public static boolean is(Mob mob) {
		return mob.entityTags().contains(TAG);
	}

	// ------------------------------------------------------------------ waking

	private static void check(MinecraftServer server) {
		ServerLevel level = server.overworld();
		if (level.getDifficulty() == Difficulty.PEACEFUL) return;
		RandomSource random = level.getRandom();
		if (random.nextDouble() >= GiantRules.CHANCE) return;
		List<ServerPlayer> players = level.players().stream().filter(p -> p.isAlive() && !p.isSpectator()).toList();
		if (players.isEmpty()) return;
		wake(level, players.get(random.nextInt(players.size())));
	}

	/** Wakes a giant somewhere near {@code player}; returns it, or null with nowhere to stand or another giant close by. */
	public static @Nullable Mob wake(ServerLevel level, ServerPlayer player) {
		double apart = GiantRules.APART * GiantRules.APART;
		for (Tracked giant : GIANTS.values()) {
			if (giant.mob().level() == level && giant.mob().distanceToSqr(player) < apart) return null;
		}
		RandomSource random = level.getRandom();
		for (int attempt = 0; attempt < 12; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = GiantRules.WAKE_NEAR + random.nextDouble() * (GiantRules.WAKE_FAR - GiantRules.WAKE_NEAR);
			BlockPos column = BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY(), player.getZ() + Math.sin(angle) * distance);
			if (!level.isPositionEntityTicking(column)) continue;
			BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
			if (!level.getFluidState(ground).isEmpty() || !level.getFluidState(ground.below()).isEmpty()) continue;
			var biome = level.getBiome(ground).value();
			GiantRules.Kind kind = GiantRules.kind(biome.getBaseTemperature(), biome.coldEnoughToSnow(ground, level.getSeaLevel()));
			int threat = Math.max(GiantRules.THREAT_MIN, TemperingRules.threat(Heart.circles(player), Aura.stage(player)));
			Mob giant = make(level, kind, new Vec3(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5), threat, false);
			if (giant == null) return null;
			if (!level.noCollision(giant)) continue;
			level.addFreshEntity(giant);
			level.playSound(null, giant.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 4.0F, 0.5F);
			Direction way = Direction.getApproximateNearest(giant.getX() - player.getX(), 0, giant.getZ() - player.getZ());
			player.sendSystemMessage(Component.translatable("message.wildercord.giant.wakes", giant.getDisplayName(),
				Component.translatable("town.wildercord.way." + way.getSerializedName())).withStyle(ChatFormatting.GOLD));
			return giant;
		}
		return null;
	}

	/** Makes a giant of {@code kind} at {@code at}, tempered to {@code threat}; adds it to the world only if {@code add}. */
	public static @Nullable Mob make(ServerLevel level, GiantRules.Kind kind, Vec3 at, int threat, boolean add) {
		WildMonster mob = (kind == GiantRules.Kind.MATRIARCH ? MonsterContent.GLOOMSTALKER : MonsterContent.BRAMBLEWALKER)
			.create(level, EntitySpawnReason.EVENT);
		if (mob == null) return null;
		mob.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360, 0);
		// The matriarch leads every pack in the snow.
		mob.setVariant(kind.variant, kind == GiantRules.Kind.MATRIARCH);
		modifier(mob, Attributes.SCALE, SCALE, GiantRules.SCALE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		modifier(mob, Attributes.KNOCKBACK_RESISTANCE, STURDY, GiantRules.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE);
		TemperingRules.Elite elite = switch (kind) {
			case MATRIARCH -> TemperingRules.Elite.SWIFT;
			case COLOSSUS -> TemperingRules.Elite.BRUTAL;
			case ELDER -> TemperingRules.Elite.IRONHIDE;
		};
		Tempering.champion(mob, threat, elite, GiantRules.TOUGHNESS, Component.translatable(kind.key()).withColor(0xE8C060));
		mob.addTag(TAG);
		if (add) level.addFreshEntity(mob);
		return mob;
	}

	private static void modifier(Mob mob, Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance == null) return;
		instance.removeModifier(id);
		instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
	}

	// ------------------------------------------------------------------ running them

	private static void track(Mob mob) {
		GIANTS.computeIfAbsent(mob.getUUID(), id -> new Tracked(mob,
			new ServerBossEvent(UUID.randomUUID(), mob.getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10)));
	}

	private static void forget(UUID id) {
		Tracked gone = GIANTS.remove(id);
		if (gone != null) gone.bar().removeAllPlayers();
	}

	private static void tick(MinecraftServer server) {
		if (GIANTS.isEmpty()) return;
		long now = server.getTickCount();
		for (Tracked giant : List.copyOf(GIANTS.values())) {
			Mob mob = giant.mob();
			if (mob.isRemoved() || !mob.isAlive()) {
				forget(mob.getUUID());
				continue;
			}
			boolean fighting = mob.getTarget() != null;
			if (GiantRules.stomps(fighting, now, mob.getId())) stomp(mob);
			if (GiantRules.roams(fighting, now, mob.getId())) roam(mob);
			if (now % 10 == 0) update(mob);
		}
	}

	/** Its bar, or null if it isn't a loaded giant. */
	public static @Nullable ServerBossEvent bar(Mob mob) {
		Tracked giant = GIANTS.get(mob.getUUID());
		return giant == null ? null : giant.bar();
	}

	/** Brings its bar up to date: who's near enough to see it, and how hurt it is. */
	public static void update(Mob mob) {
		Tracked giant = GIANTS.get(mob.getUUID());
		if (giant == null || !(mob.level() instanceof ServerLevel level)) return;
		ServerBossEvent bar = giant.bar();
		double range = GiantRules.BAR_RANGE * GiantRules.BAR_RANGE;
		List<ServerPlayer> near = level.players().stream().filter(p -> p.isAlive() && p.distanceToSqr(mob) < range).toList();
		for (ServerPlayer player : near) bar.addPlayer(player);
		for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
			if (!near.contains(player)) bar.removePlayer(player);
		}
		bar.setProgress(Math.max(0F, Math.min(1F, mob.getHealth() / mob.getMaxHealth())));
	}

	/** Its stomp: the ground shakes, and everything near that isn't a monster is hurt and thrown. Returns how many it hit. */
	public static int stomp(Mob mob) {
		if (!(mob.level() instanceof ServerLevel level)) return 0;
		double radius = GiantRules.STOMP_RADIUS + mob.getBbWidth() / 2;
		int hit = 0;
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(radius, 2, radius),
				e -> e != mob && e.isAlive() && !(e instanceof Enemy) && !e.isSpectator())) {
			double distance = Math.sqrt(victim.distanceToSqr(mob.getX(), victim.getY(), mob.getZ())) - mob.getBbWidth() / 2;
			float damage = GiantRules.stompDamage(Math.max(0, distance));
			if (damage <= 0) continue;
			victim.hurtServer(level, level.damageSources().mobAttack(mob), damage);
			Vec3 away = new Vec3(victim.getX() - mob.getX(), 0, victim.getZ() - mob.getZ());
			away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(GiantRules.STOMP_THROW
				* (1 - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)));
			victim.setDeltaMovement(victim.getDeltaMovement().add(away.x, 0.45, away.z));
			victim.needsSync = true;
			hit++;
		}
		level.sendParticles(ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 0.2, mob.getZ(), 6, radius / 2, 0.1, radius / 2, 0);
		level.sendParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY() + 0.2, mob.getZ(), 30, radius / 2, 0.1, radius / 2, 0.05);
		level.playSound(null, mob.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5F, 0.55F);
		return hit;
	}

	/** Sets off somewhere new, far off. */
	private static void roam(Mob mob) {
		if (!(mob instanceof PathfinderMob walker)) return;
		RandomSource random = mob.getRandom();
		Vec3 to = LandRandomPos.getPos(walker, GiantRules.ROAM_NEAR + random.nextInt(GiantRules.ROAM_FAR - GiantRules.ROAM_NEAR + 1), 7);
		if (to != null) walker.getNavigation().moveTo(to.x, to.y, to.z, 0.8);
	}

	/** It falls: it drops its heart, and everyone near is told. */
	private static void fall(ServerLevel level, Mob mob) {
		mob.spawnAtLocation(level, new ItemStack(GIANT_HEART, GiantRules.hearts(level.getRandom().nextDouble())));
		Component message = Component.translatable("message.wildercord.giant.falls", mob.getDisplayName()).withStyle(ChatFormatting.GOLD);
		double range = GiantRules.BAR_RANGE * GiantRules.BAR_RANGE;
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(mob) < range) player.sendSystemMessage(message);
		}
		forget(mob.getUUID());
	}
}
