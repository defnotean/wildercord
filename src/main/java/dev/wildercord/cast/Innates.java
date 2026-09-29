package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.content.SigilOption;
import dev.wildercord.Wildercord;
import dev.wildercord.mixin.MannequinAccessor;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Innate runes: one is awakened in each caster's heart when the 1st Circle forms, chosen at
 * random. Nobody else can craft, find or learn it, and it grows with every circle (+6% power
 * each). Each does something no other rune does.
 */
public final class Innates {
	private Innates() {}

	public static final double POWER_PER_CIRCLE = 0.06;

	/** Innate power: stronger with every circle the caster holds (a cracked one gives nothing, as for everything a circle grants). */
	public static double scale(LivingEntity caster) {
		return caster instanceof ServerPlayer player ? 1 + POWER_PER_CIRCLE * Heart.active(player) : 1.0;
	}

	// ------------------------------------------------------------------ awakening

	/** At the 1st Circle: an innate rune wakes, learned and ready to thread. */
	public static void awaken(ServerPlayer player) {
		if (!Heart.innate(player).isEmpty()) {
			return;
		}
		RuneDef rune = Runes.INNATE.get(player.getRandom().nextInt(Runes.INNATE.size()));
		player.setAttached(WildercordAttachments.INNATE, rune.id());
		Spellbooks.learn(player, rune.id());
		int color = RuneColors.of(rune);
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
		player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(rune.name()).withColor(color)));
		player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("title.wildercord.innate").withColor(0xE8E0FF)));
		player.sendSystemMessage(Component.translatable("message.wildercord.innate", Component.literal(rune.name()).withColor(color)).withColor(0xE8E0FF));
		Sigils.ground(player.level(), player.position(), color, 0xFFFFFF, 2.2F, 50);
		awakenFx(player, color);
		Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 0.8F);
		Grimoire.feat(player, Feats.INNATE);
	}

	/** The rune wakes: rings of its colour race out over the ground and round the heart, and columns of light rise round you. */
	private static void awakenFx(ServerPlayer player, int color) {
		ServerLevel level = player.level();
		Vec3 feet = player.position();
		ElementFx.groundRing(level, feet, color, 0.3, 3.4, 0.09, 18);
		ElementFx.groundRing(level, feet, 0xFFFFFF, 0.2, 2.4, 0.04, 22);
		ElementFx.ring(level, feet.add(0, 1.2, 0), UP, color, 0.3, 2.6, 0.05, 12);
		for (int i = 0; i < 6; i++) {
			double a = Math.PI * 2 * i / 6;
			Vec3 base = feet.add(Math.cos(a) * 1.5, 0.05, Math.sin(a) * 1.5);
			ElementFx.ray(level, base, base.add(0, 2.4, 0), i % 2 == 0 ? color : 0xFFFFFF, 0.06, 18);
		}
		Vfx.radial(level, ParticleTypes.END_ROD, feet.add(0, 1.2, 0), 16, 0.25);
	}

	private static final Vec3 UP = new Vec3(0, 1, 0);

	// ------------------------------------------------------------------ state

	private record Thread(UUID id, long until, LivingEntity caster) {}

	private record Kindle(int stacks, long last) {}

	private static final class Gale {
		long until;
		int dashes;
		boolean jumping = true;

		Gale(long until, int dashes) {
			this.until = until;
			this.dashes = dashes;
		}
	}

	private record Debt(float left, float perSecond, long until) {}

	/** What Borrowed Time is repaid with: the sum borrowed, and a fifth again. */
	private static final float BORROW_INTEREST = 1.2F;
	/** What each afterimage has soaked so far, by its body: it bursts harder for it. */
	private static final Map<UUID, Float> SOAKED = new HashMap<>();

	private record SpellHit(SpellPlan.Segment root, long time) {}

	private record Afterimage(Mannequin body, LivingEntity caster, long until, double power) {}

	private static final Map<UUID, Thread> THREADS = new HashMap<>();
	private static final Map<UUID, Set<LivingEntity>> THREAD_MEMBERS = new HashMap<>();
	private static final Map<UUID, Kindle> KINDLING = new HashMap<>();
	private static final Map<UUID, Long> TWIN = new HashMap<>();
	private static final Map<UUID, Long> TWIN_ARMED = new HashMap<>();
	private static final Map<UUID, Deque<float[]>> HURT_HISTORY = new HashMap<>();
	private static final Map<UUID, Debt> DEBTS = new HashMap<>();
	private static final Map<UUID, Gale> GALES = new HashMap<>();
	private static final Map<UUID, Long> STONEFORM = new HashMap<>();
	private static final Map<UUID, Long> STONE_LAST = new HashMap<>();
	private static final Map<UUID, SpellHit> LAST_SPELL_ON = new HashMap<>();
	private static final Map<UUID, Long> FORTUNE = new HashMap<>();
	private static final List<Afterimage> AFTERIMAGES = new ArrayList<>();
	/** On every afterimage, so one left over from before a restart can be recognised and removed. */
	private static final String AFTERIMAGE_TAG = "wildercord.afterimage";
	private static final Map<UUID, Long> STORMHEART = new HashMap<>();
	private static final Map<UUID, Long> STORM_LAST = new HashMap<>();
	private static final Identifier STONE_KNOCKBACK = Wildercord.id("stoneform");
	/** Set while shared or bonus damage is being dealt, so it never shares or bonuses itself again. */
	private static boolean echoing;
	/** Set while Borrowed Time's debt is being paid: a payment is never borrowed again. */
	private static boolean repaying;

	public static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
			if (damage <= 0 || !(entity.level() instanceof ServerLevel level)) {
				return;
			}
			if (entity instanceof Mannequin && entity.entityTags().contains(AFTERIMAGE_TAG)) {
				SOAKED.merge(entity.getUUID(), damage, Float::sum);
			}
			long now = level.getGameTime();
			if (!repaying && entity instanceof ServerPlayer player && player.getAttachedOrElse(WildercordAttachments.INNATE, "").equals(Runes.BORROWED_TIME.id())) {
				Deque<float[]> history = HURT_HISTORY.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
				history.addLast(new float[] {now, damage});
				while (history.size() > 40 || !history.isEmpty() && now - history.peekFirst()[0] > 100) {
					history.removeFirst();
				}
			}
			if (echoing) {
				return;
			}
			SurgeArcs.onBlow(level, entity, source, blocked);
			shareThread(level, entity, damage);
			if (source.getEntity() instanceof LivingEntity blow && blow != entity) {
				stoneAftershock(level, entity, now);
			}
			stormStrike(level, entity, source, now, damage);
			fortuneMelee(level, entity, source, damage);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			// Death settles a debt: it isn't carried on to the one who respawns.
			if (entity instanceof ServerPlayer dead) {
				DEBTS.remove(dead.getUUID());
			}
			if (source.getEntity() instanceof ServerPlayer winner && entity instanceof Enemy && entity.level() instanceof ServerLevel there) {
				Long until = FORTUNE.get(winner.getUUID());
				if (until != null && there.getGameTime() <= until && there.getRandom().nextFloat() < 0.25F) {
					net.minecraft.world.entity.ExperienceOrb.award(there, entity.position(), LUCKY_XP);
					lucky(there, entity);
				}
			}
			if (source.getEntity() instanceof ServerPlayer player && entity instanceof Enemy && DEBTS.remove(player.getUUID()) != null) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.debt_forgiven").withColor(0xF2D98A));
				TimeFx.stasisRelease(player.level(), player, 0);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(Innates::tick);
		// Afterimages go before the world is saved; one saved anyway (its chunk unloaded) is gone when it loads.
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			AFTERIMAGES.forEach(a -> a.body().discard());
			AFTERIMAGES.clear();
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Mannequin && entity.entityTags().contains(AFTERIMAGE_TAG)
					&& AFTERIMAGES.stream().noneMatch(a -> a.body() == entity)) {
				entity.discard();
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			THREADS.clear();
			SurgeArcs.clear();
			THREAD_MEMBERS.clear();
			KINDLING.clear();
			TWIN.clear();
			TWIN_ARMED.clear();
			HURT_HISTORY.clear();
			DEBTS.clear();
			GALES.clear();
			STONEFORM.clear();
			STONE_LAST.clear();
			LAST_SPELL_ON.clear();
			FORTUNE.clear();
			AFTERIMAGES.clear();
			SOAKED.clear();
			STORMHEART.clear();
			STORM_LAST.clear();
		});
	}

	/** Runs one innate rune. {@code power} already includes the circle scaling. */
	static void apply(Cast cast, RuneDef rune, List<LivingEntity> helped, List<LivingEntity> harmed, double power, double duration) {
		LivingEntity caster = cast.caster;
		boolean onSelf = helped.contains(caster);
		switch (Effects.builtIn(rune) ? rune.path() : "") {
			case "blood_thread" -> bloodThread(cast, harmed, Effects.ticks(8, duration));
			case "kindling" -> harmed.forEach(t -> kindle(cast, t, power));
			case "twin_star" -> {
				if (onSelf) {
					TWIN.put(caster.getUUID(), cast.level.getGameTime() + 120);
					TWIN_ARMED.put(caster.getUUID(), cast.level.getGameTime());
					TechniqueVfx.twinStarMark(cast.level, caster);
				}
			}
			case "borrowed_time" -> {
				if (onSelf && caster instanceof ServerPlayer player) {
					borrowTime(player);
				}
			}
			case "gale_mantle" -> helped.forEach(t -> {
				GALES.put(t.getUUID(), new Gale(cast.level.getGameTime() + Effects.ticks(12, duration), 3));
				Vfx.emit(cast.level, ParticleTypes.GUST, t.position().add(0, 0.5, 0), 1, 0.0, 0.0);
				ElementFx.gustRing(cast.level, t.position(), 2.0);
				ElementFx.swirl(cast.level, t.position().add(0, 0.1, 0), 0.7, t.getBbHeight() + 0.2, 4);
				Fx.sound(cast.level, t.position(), SoundEvents.BREEZE_WIND_CHARGE_BURST, 0.8F, 1.3F);
			});
			case "stoneform" -> helped.forEach(t -> {
				int ticks = Effects.ticks(8, duration);
				STONEFORM.put(t.getUUID(), cast.level.getGameTime() + ticks);
				t.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, ticks, 0, false, true));
				t.getAttribute(Attributes.KNOCKBACK_RESISTANCE).addOrUpdateTransientModifier(
					new AttributeModifier(STONE_KNOCKBACK, 1.0, AttributeModifier.Operation.ADD_VALUE));
				// Its own stance, in deepslate: not Stoneskin's sandstone cast.
				StormEarthFx.stoneform(cast.level, t, ticks);
			});
			case "mirrorfrost" -> {
				if (onSelf && caster instanceof ServerPlayer player) {
					mirror(cast, player);
				}
			}
			case "fortune" -> helped.forEach(t -> {
				FORTUNE.put(t.getUUID(), cast.level.getGameTime() + Effects.ticks(10, duration));
				ElementFx.bloom(cast.level, t.getBoundingBox().getCenter(), t.position(), 1.3);
				ElementFx.groundRing(cast.level, t.position(), 0xF5D86A, 0.2, 1.1, 0.05, 14);
				// A gold coin spinning over the head for as long as the luck lasts.
				ElementFx.orbit(cast.level, t.position().add(0, t.getBbHeight() + 0.5, 0), 0.25, 1, Effects.ticks(10, duration), 0xF5D86A, 0xFFF4B0);
				Vfx.emit(cast.level, ParticleTypes.HAPPY_VILLAGER, t.getBoundingBox().getCenter(), 6, 0.4, 0.0);
				dev.wildercord.cast.feel.Feels.sound(cast.level, t.position(), "life_coin", 0.9F, 1.0F);
			});
			case "phantom" -> {
				if (onSelf && caster instanceof ServerPlayer player) {
					afterimage(cast, player, Effects.ticks(4, duration), power);
				}
			}
			case "stormheart" -> helped.forEach(t -> {
				STORMHEART.put(t.getUUID(), cast.level.getGameTime() + Effects.ticks(10, duration));
				StormEarthFx.stormheartStance(cast.level, t, Effects.ticks(10, duration));
				Vec3 c = t.getBoundingBox().getCenter();
				ElementFx.bolt(cast.level, c.add(0.6, 2.4, 0.3), c, 0.05, 1, 2);
				ElementFx.ring(cast.level, c, UP, ElementFx.STORM.primary(), 1.4, 0.5, 0.04, 8);
				ElementFx.groundRing(cast.level, t.position(), ElementFx.STORM.accent(), 0.3, 1.6, 0.05, 9);
				Vfx.emit(cast.level, ParticleTypes.ELECTRIC_SPARK, c, 12, 0.5, 0.05);
				dev.wildercord.cast.feel.Feels.sound(cast.level, t.position(), "storm_whine", 0.7F, 1.0F);
			});
			default -> { }
		}
	}

	// ------------------------------------------------------------------ Blood Thread

	private static void bloodThread(Cast cast, List<LivingEntity> harmed, int ticks) {
		if (harmed.isEmpty()) {
			return;
		}
		UUID id = UUID.randomUUID();
		long until = cast.level.getGameTime() + ticks;
		Set<LivingEntity> members = new HashSet<>(harmed);
		// A single target threads to everything else hostile near it.
		if (members.size() == 1) {
			LivingEntity first = harmed.getFirst();
			for (Entity e : cast.level.getEntities(first, first.getBoundingBox().inflate(6), e -> Targets.canHarm(cast.caster, e))) {
				if (members.size() < 5) {
					members.add((LivingEntity) e);
				}
			}
		}
		THREAD_MEMBERS.put(id, members);
		for (LivingEntity t : members) {
			THREADS.put(t.getUUID(), new Thread(id, until, cast.caster));
		}
		Fx.sound(cast.level, harmed.getFirst().position(), SoundEvents.CHAIN_PLACE, 1.0F, 0.6F);
		ShapeRunners.steps(cast, 1, 10, ticks - 1, t -> drawThread(cast.level, members));
	}

	private static void drawThread(ServerLevel level, Set<LivingEntity> members) {
		List<LivingEntity> alive = members.stream().filter(LivingEntity::isAlive).toList();
		for (int i = 0; i + 1 < alive.size(); i++) {
			Vec3 a = alive.get(i).getBoundingBox().getCenter();
			Vec3 b = alive.get(i + 1).getBoundingBox().getCenter();
			Vec3 d = b.subtract(a);
			// A sagging thread of crimson light, in four pieces, drawn fresh as the last fades.
			Vec3 prev = a;
			for (int k = 1; k <= 4; k++) {
				double s = k / 4.0;
				Vec3 p = a.add(d.scale(s)).add(0, -Math.sin(s * Math.PI) * 0.35, 0);
				ElementFx.ray(level, prev, p, k % 2 == 0 ? 0xFF5060 : ElementFx.BLOOD.primary(), 0.03, 11);
				prev = p;
			}
		}
	}

	private static void shareThread(ServerLevel level, LivingEntity entity, float damage) {
		Thread thread = THREADS.get(entity.getUUID());
		if (thread == null || level.getGameTime() > thread.until()) {
			return;
		}
		Set<LivingEntity> members = THREAD_MEMBERS.get(thread.id());
		if (members == null) {
			return;
		}
		echoing = true;
		try {
			DamageSource source = level.damageSources().indirectMagic(thread.caster(), thread.caster());
			for (LivingEntity other : members) {
				if (other != entity && other.isAlive() && other.level() == level && other.distanceTo(entity) < 32) {
					Effects.readyToHurt(other);
					other.hurtServer(level, source, damage * 0.5F);
					TechniqueVfx.chain(level, entity.getBoundingBox().getCenter(), other, false);
				}
			}
		} finally {
			echoing = false;
		}
	}

	// ------------------------------------------------------------------ Kindling

	private static void kindle(Cast cast, LivingEntity t, double power) {
		long now = cast.level.getGameTime();
		Kindle k = KINDLING.get(t.getUUID());
		int stacks = k == null || now - k.last() > 120 ? 1 : k.stacks() + 1;
		Effects.hurt(cast, t, cast.level.damageSources().source(DamageTypes.IN_FIRE, cast.caster), 3 * power * Reactions.fire(cast, t));
		Vec3 c = t.getBoundingBox().getCenter();
		// One flame tongue and one ember in the ring over its head for every stack.
		ElementFx.flames(cast.level, t.position(), Math.max(0.35, t.getBbWidth() * 0.6), t.getBbHeight(), stacks);
		for (int i = 0; i < stacks; i++) {
			double a = Math.PI * 2 * i / 5;
			Vfx.emit(cast.level, ParticleTypes.SMALL_FLAME, c.add(Math.cos(a) * 0.6, 0.6, Math.sin(a) * 0.6), 2, 0.02, 0.0);
		}
		Fx.sound(cast.level, c, SoundEvents.FIRECHARGE_USE, 0.4F, 1.2F + stacks * 0.15F);
		if (stacks >= 5) {
			KINDLING.remove(t.getUUID());
			ElementFx.fireImpact(cast.level, c, 2.2);
			ElementFx.flames(cast.level, t.position(), Math.max(0.4, t.getBbWidth() * 0.7), t.getBbHeight() + 0.5, 6);
			Vfx.radial(cast.level, ParticleTypes.FLAME, c, 20, 0.35);
			Vfx.shockwave(cast.level, t.position(), 3.0, Vfx.theme("fire"), 4);
			Fx.sound(cast.level, c, SoundEvents.GENERIC_EXPLODE, 0.7F, 1.4F);
			for (Entity e : cast.level.getEntities((Entity) null, new AABB(c, c).inflate(3), e -> Targets.canHarm(cast.caster, e))) {
				LivingEntity other = (LivingEntity) e;
				other.igniteForSeconds(4);
				Effects.hurt(cast, other, cast.level.damageSources().source(DamageTypes.IN_FIRE, cast.caster), 10 * power);
			}
			Reactions.callout(cast, "ignite", 0xFF9040);
		} else {
			KINDLING.put(t.getUUID(), new Kindle(stacks, now));
		}
	}

	// ------------------------------------------------------------------ Twin Star

	/** How strong Twin Star's second cast is. */
	public static final double TWIN_POWER = 0.75;

	/** The next cast after Twin Star goes off twice; the Twin Star cast itself doesn't count. */
	public static boolean consumeTwin(ServerPlayer player) {
		long now = player.level().getGameTime();
		Long until = TWIN.get(player.getUUID());
		Long armed = TWIN_ARMED.get(player.getUUID());
		if (until == null || now > until || armed != null && armed == now) {
			return false;
		}
		TWIN.remove(player.getUUID());
		TWIN_ARMED.remove(player.getUUID());
		return true;
	}

	// ------------------------------------------------------------------ Borrowed Time

	private static void borrowTime(ServerPlayer player) {
		long now = player.level().getGameTime();
		float owed = 0;
		Deque<float[]> history = HURT_HISTORY.get(player.getUUID());
		if (history != null) {
			for (float[] hurt : history) {
				if (now - hurt[0] <= 100) {
					owed += hurt[1];
				}
			}
		}
		// Only what can really be mended is borrowed: health that has already come back is nothing to repay.
		owed = Math.min(owed, Math.max(0.0F, player.getMaxHealth() - player.getHealth()));
		if (owed < 0.5F) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.nothing_borrowed"));
			return;
		}
		if (history != null) {
			history.clear();
		}
		player.heal(owed);
		// Borrowing again adds to what's still owed (it never wipes it), and the whole of it, with a fifth on top, is paid over the next ten seconds.
		Debt old = DEBTS.get(player.getUUID());
		float total = owed * BORROW_INTEREST + (old == null ? 0 : old.left());
		DEBTS.put(player.getUUID(), new Debt(total, total / 10F, now + 200));
		TechniqueVfx.rewind(player.level(), player.position(), player.position());
		ElementFx.goldenTicks(player.level(), player.getBoundingBox().getCenter(), 0.5, 8);
		ElementFx.groundRing(player.level(), player.position(), ElementFx.TIME.primary(), 1.8, 0.4, 0.06, 14);
		dev.wildercord.cast.feel.Feels.sound(player.level(), player.position(), "time_sand", 0.9F, 1.0F);
		player.sendOverlayMessage(Component.translatable("message.wildercord.borrowed", Math.round(owed)).withColor(0xF2D98A));
	}

	/** What a player still owes Borrowed Time (0 for nothing), for the tests. */
	public static float owed(ServerPlayer player) {
		Debt debt = DEBTS.get(player.getUUID());
		return debt == null ? 0 : debt.left();
	}

	// ------------------------------------------------------------------ Mirrorfrost

	/** What Mirrorfrost returns: 70% of the power, and marked (a negative rune count) so it can't be mirrored back. */
	static final double MIRROR_POWER = 0.7;
	static final int MIRRORED = -1;

	/** Remembers the last spell that hit a player, for Mirrorfrost. */
	static void spellHit(Cast cast, LivingEntity target) {
		// A mirrored spell (marked by its negative rune count) is never itself mirrored.
		if (target instanceof ServerPlayer player && cast.caster != player && cast.info.root() != null && cast.info.runes() >= 0) {
			LAST_SPELL_ON.put(player.getUUID(), new SpellHit(cast.info.root(), cast.level.getGameTime()));
		}
	}

	private static void mirror(Cast cast, ServerPlayer player) {
		SpellHit hit = LAST_SPELL_ON.remove(player.getUUID());
		if (hit == null || cast.level.getGameTime() - hit.time() > 600) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.nothing_to_mirror"));
			return;
		}
		Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(1.0));
		Sigils.telegraph(cast.level, hand, player.getLookAngle(), 0x8CDCFF, 0.8F, 16);
		Vfx.radial(cast.level, new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM, net.minecraft.world.item.Items.GLASS_PANE), hand, 10, 0.2);
		ElementFx.shatterRing(cast.level, hand, 1.4);
		ElementFx.shards(cast.level, hand, 0.9, 5);
		Feels.sound(cast.level, hand, "frost_mirror", 1.0F, 1.0F);
		Cast mirrored = new Cast(player, 1, Heart.bonuses(player), false, null, new Cast.Info(hit.root(), MIRRORED, Heart.leaning(player))).withAffinity()
			.withPower(MIRROR_POWER);
		CastEngine.cast(mirrored, hit.root());
		Grimoire.feat(player, Feats.MIRROR);
	}

	// ------------------------------------------------------------------ Fortune

	/** Spell damage multiplier from Fortune: a one-in-four chance of double (expected +25%). */
	static double fortune(Cast cast, LivingEntity target) {
		Long until = FORTUNE.get(cast.caster.getUUID());
		if (until == null || cast.level.getGameTime() > until || cast.level.getRandom().nextFloat() >= 0.25F) {
			return 1.0;
		}
		lucky(cast.level, target);
		return LUCKY_MULTIPLIER;
	}

	/** What a fortunate hit deals: double. */
	public static final double LUCKY_MULTIPLIER = 2.0;
	/** XP a fortunate kill drops on top of its own (a one in four chance). */
	public static final int LUCKY_XP = 6;

	private static void fortuneMelee(ServerLevel level, LivingEntity entity, DamageSource source, float damage) {
		// A strike by hand: a spell's own strike (Cleave, Aftershock) already rolled Fortune in Effects.hurt.
		if (!(source.getDirectEntity() instanceof ServerPlayer player) || source.getEntity() != player || Dungeons.spellLanding()) {
			return;
		}
		Long until = FORTUNE.get(player.getUUID());
		if (until == null || level.getGameTime() > until || level.getRandom().nextFloat() >= 0.25F || !entity.isAlive()) {
			return;
		}
		lucky(level, entity);
		echoing = true;
		try {
			Effects.readyToHurt(entity);
			entity.hurtServer(level, level.damageSources().playerAttack(player), (float) (damage * (LUCKY_MULTIPLIER - 1)));
		} finally {
			echoing = false;
		}
	}

	private static void lucky(ServerLevel level, LivingEntity target) {
		Vec3 c = target.getBoundingBox().getCenter();
		Sigils.flash(level, c, 0xFF9CFF7A, 2.0F);
		ElementFx.ring(level, c, UP, LUCK, 0.2, 1.6, 0.05, 8);
		ElementFx.orbit(level, c, 0.7, 2, 4, LUCK, 0xFFF4B0);
		Vfx.radial(level, ParticleTypes.HAPPY_VILLAGER, c, 8, 0.25);
		Vfx.radial(level, ParticleTypes.CRIT, c, 8, 0.4);
		dev.wildercord.cast.feel.Feels.sound(level, c, "life_coin_proc", 1.0F, 1.0F);
	}

	private static final int LUCK = 0x9CFF7A;

	// ------------------------------------------------------------------ Phantom

	private static void afterimage(Cast cast, ServerPlayer player, int ticks, double power) {
		ServerLevel level = cast.level;
		Mannequin body = Mannequin.create(EntityTypes.MANNEQUIN, level);
		if (body == null) {
			return;
		}
		body.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
		body.setYHeadRot(player.getYHeadRot());
		body.setComponent(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
		((MannequinAccessor) body).wildercord$setHideDescription(true);
		((MannequinAccessor) body).wildercord$setImmovable(true);
		body.setCustomName(Component.translatable("entity.wildercord.afterimage", player.getName()).withColor(0xB45AF0));
		body.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
		body.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60);
		body.setHealth(60);
		body.addTag(AFTERIMAGE_TAG);
		AFTERIMAGES.add(new Afterimage(body, player, level.getGameTime() + ticks, power));
		level.addFreshEntity(body);
		// You step out of it, briefly unseen.
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false));
		ElementFx.implode(level, body.getBoundingBox().getCenter(), 1.4, 8);
		ElementFx.groundRing(level, body.position(), ElementFx.VOID.primary(), 0.2, 1.6, 0.05, 12);
		Vfx.emit(level, ParticleTypes.SOUL, body.getBoundingBox().getCenter(), 8, 0.3, 0.03);
		dev.wildercord.cast.feel.Feels.sound(level, body.position(), "void_phantom_form", 1.0F, 1.0F);
		taunt(level, body);
	}

	private static void taunt(ServerLevel level, Mannequin body) {
		for (Entity e : level.getEntities(body, body.getBoundingBox().inflate(16), e -> e instanceof Mob && e instanceof Enemy && e.isAlive())) {
			((Mob) e).setTarget(body);
		}
	}

	private static void burst(ServerLevel level, Afterimage image) {
		Vec3 c = image.body().getBoundingBox().getCenter();
		Cast cast = new Cast(image.caster());
		// Bait and revenge: it pays back 1 for every 6 it soaked (up to 8 more).
		double soaked = Math.min(8.0, Math.floor(SOAKED.getOrDefault(image.body().getUUID(), 0.0F) / 6.0F));
		SOAKED.remove(image.body().getUUID());
		for (Entity e : level.getEntities(image.body(), new AABB(c, c).inflate(3), e -> Targets.canHarm(image.caster(), e))) {
			Effects.hurt(cast, (LivingEntity) e, level.damageSources().indirectMagic(image.caster(), image.caster()), (8 + soaked) * image.power());
		}
		Sigils.flash(level, c, 0xFFB45AF0, 2.0F);
		ElementFx.voidImpact(level, c, 2.0);
		ElementFx.blackCore(level, c, 0.35, 8);
		Vfx.radial(level, ParticleTypes.REVERSE_PORTAL, c, 20, 0.5);
		Vfx.radial(level, ParticleTypes.SOUL, c, 10, 0.2);
		dev.wildercord.cast.feel.Feels.sound(level, c, "void_phantom_burst", 1.0F, 1.0F);
		image.body().discard();
	}

	// ------------------------------------------------------------------ Stoneform and Stormheart

	private static void stoneAftershock(ServerLevel level, LivingEntity entity, long now) {
		Long until = STONEFORM.get(entity.getUUID());
		if (until == null || now > until || now - STONE_LAST.getOrDefault(entity.getUUID(), 0L) < 20) {
			return;
		}
		STONE_LAST.put(entity.getUUID(), now);
		Cast cast = new Cast(entity);
		double power = scale(entity);
		Vfx.shockwave(level, entity.position(), 3.0, Vfx.theme("earth"), 4);
		ElementFx.crack(level, entity.position(), 1.4, 16);
		dev.wildercord.cast.feel.Feels.sound(level, entity.position(), "earth_stomp", 0.9F, 0.84F);
		// A pet in Stoneform fights for its owner: its aftershock spares them and hits what they'd hit.
		LivingEntity side = entity instanceof net.minecraft.world.entity.OwnableEntity pet && pet.getOwner() instanceof LivingEntity owner ? owner : entity;
		echoing = true;
		try {
			// Earth damage, whatever the blow it answers was: it's the Stoneform's own aftershock, not the attacker's spell.
			Effects.asElement("earth", () -> {
				for (Entity e : level.getEntities(entity, entity.getBoundingBox().inflate(3), e -> e != side && Targets.canHarm(side, e))) {
					LivingEntity t = (LivingEntity) e;
					Effects.hurt(cast, t, level.damageSources().indirectMagic(entity, entity), 3.5 * power);
					Vec3 away = t.position().subtract(entity.position());
					Effects.push(t, (away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize()).scale(0.8).add(0, 0.35, 0));
				}
			});
		} finally {
			echoing = false;
		}
	}

	/** Blows under this (chip damage) don't call the storm down. */
	static final float STORM_MIN = 2.0F;

	private static void stormStrike(ServerLevel level, LivingEntity entity, DamageSource source, long now, float damage) {
		Long until = STORMHEART.get(entity.getUUID());
		if (until == null || now > until || !(source.getEntity() instanceof LivingEntity attacker) || attacker == entity || !attacker.isAlive()
				|| now - STORM_LAST.getOrDefault(entity.getUUID(), 0L) < 20 || damage < STORM_MIN) {
			return;
		}
		// Never a friend (a pet with it fights for its owner, as in Stoneform).
		LivingEntity side = entity instanceof net.minecraft.world.entity.OwnableEntity pet && pet.getOwner() instanceof LivingEntity owner ? owner : entity;
		if (!Targets.canHarm(side, attacker)) {
			return;
		}
		STORM_LAST.put(entity.getUUID(), now);
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.setVisualOnly(true);
			bolt.snapTo(attacker.getX(), attacker.getY(), attacker.getZ());
			level.addFreshEntity(bolt);
		}
		Vfx.shockArc(level, entity.getBoundingBox().getCenter(), attacker.getBoundingBox().getCenter());
		echoing = true;
		try {
			// As spell damage, like Stoneform's aftershock: a player it strikes takes it at the server's pvp scale, and a Shield
			// meets it. Storm damage, whatever the blow it answers was.
			Effects.asElement("storm", () -> Effects.hurt(new Cast(entity), attacker, level.damageSources().source(DamageTypes.LIGHTNING_BOLT, entity),
				5 * scale(entity)));
		} finally {
			echoing = false;
		}
	}

	// ------------------------------------------------------------------ ticking

	private static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		if (!GALES.isEmpty()) {
			for (Iterator<Map.Entry<UUID, Gale>> it = GALES.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<UUID, Gale> entry = it.next();
				ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
				Gale gale = entry.getValue();
				if (player == null || now > gale.until || gale.dashes <= 0) {
					it.remove();
					continue;
				}
				boolean jump = player.getLastClientInput().jump();
				if (jump && !gale.jumping && !player.onGround() && !player.isInWater() && !player.getAbilities().flying) {
					dash(player);
					gale.dashes--;
				}
				gale.jumping = jump;
			}
		}
		if (!AFTERIMAGES.isEmpty()) {
			for (Iterator<Afterimage> it = AFTERIMAGES.iterator(); it.hasNext(); ) {
				Afterimage image = it.next();
				Mannequin body = image.body();
				if (!(body.level() instanceof ServerLevel level)) {
					it.remove();
					continue;
				}
				if (body.isRemoved() || !body.isAlive() || level.getGameTime() >= image.until()) {
					it.remove();
					if (!body.isRemoved()) {
						burst(level, image);
					}
					continue;
				}
				long remaining = image.until() - level.getGameTime();
				// The decoy's fuse quickens: a tick every half second, then every quarter, then every tenth of the last second.
				if (level.getGameTime() % 10 == 0 || (remaining <= 40 && level.getGameTime() % 5 == 0) || (remaining <= 20 && level.getGameTime() % 2 == 0)) {
					dev.wildercord.cast.feel.Feels.sound(level, body.position(), "void_phantom_tick", 0.8F, 1.0F + (float) (0.5 * (1.0 - Math.max(0, remaining) / 80.0)));
				}
				if (level.getGameTime() % 10 == 0) {
					taunt(level, body);
					ElementFx.groundRing(level, body.position(), ElementFx.VOID.primary(), 0.9, 0.5, 0.035, 11);
					Vfx.emit(level, new DustParticleOptions(0xB45AF0, 0.9F), body.getBoundingBox().getCenter(), 3, 0.35, 0.0);
					Vfx.emit(level, ParticleTypes.SOUL_FIRE_FLAME, body.position().add(0, 0.1, 0), 2, 0.3, 0.0);
				}
			}
		}
		if (now % 20 == 0) {
			if (!DEBTS.isEmpty()) {
				Map<ServerPlayer, Float> payments = new java.util.LinkedHashMap<>();
				for (Iterator<Map.Entry<UUID, Debt>> it = DEBTS.entrySet().iterator(); it.hasNext(); ) {
					Map.Entry<UUID, Debt> entry = it.next();
					ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
					Debt debt = entry.getValue();
					if (player == null) {
						// Away: the debt waits for them (logging out doesn't forgive it).
						entry.setValue(new Debt(debt.left(), debt.perSecond(), debt.until() + 20));
						continue;
					}
					if (debt.left() <= 0.01F || now > debt.until() + 20) {
						it.remove();
						continue;
					}
					// A payment never kills: it stops a heart short and waits.
					float pay = Math.min(Math.min(debt.left(), debt.perSecond()), Math.max(0.0F, player.getHealth() - 1.0F));
					entry.setValue(new Debt(debt.left() - pay, debt.perSecond(), debt.until()));
					if (pay > 0) {
						payments.put(player, pay);
					}
				}
				// Paid after the sweep: a payment can kill, and a death (or a monster Rebirth's blast slays) changes the debts.
				payments.forEach((player, pay) -> {
					echoing = true;
					repaying = true;
					try {
						Effects.readyToHurt(player);
						player.hurtServer(player.level(), player.level().damageSources().magic(), pay);
					} finally {
						echoing = false;
						repaying = false;
					}
					// A coin of gold falls from you with a low tock: every payment is heard.
					TimeFx.coin(player.level(), player);
				});
			}
			THREADS.values().removeIf(t -> now > t.until());
			THREAD_MEMBERS.keySet().removeIf(id -> THREADS.values().stream().noneMatch(t -> t.id().equals(id)));
			KINDLING.values().removeIf(k -> now - k.last() > 200);
			TWIN.values().removeIf(until -> now > until);
			// These only matter for a moment, and would otherwise grow with every creature that ever had them on a long-running server.
			TWIN_ARMED.keySet().removeIf(id -> !TWIN.containsKey(id));
			STONE_LAST.values().removeIf(last -> now - last > 20);
			STORM_LAST.values().removeIf(last -> now - last > 20);
			FORTUNE.values().removeIf(until -> now > until);
			STORMHEART.values().removeIf(until -> now > until);
			LAST_SPELL_ON.values().removeIf(h -> now - h.time() > 600);
			for (Iterator<Map.Entry<UUID, Long>> it = STONEFORM.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<UUID, Long> entry = it.next();
				if (now > entry.getValue()) {
					it.remove();
					for (ServerLevel level : server.getAllLevels()) {
						if (level.getEntity(entry.getKey()) instanceof LivingEntity stone && stone.getAttribute(Attributes.KNOCKBACK_RESISTANCE) != null) {
							stone.getAttribute(Attributes.KNOCKBACK_RESISTANCE).removeModifier(STONE_KNOCKBACK);
							break;
						}
					}
				}
			}
		}
	}

	private static void dash(ServerPlayer player) {
		Vec3 look = player.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0, look.z);
		flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
		// Steered: the dash goes where you're pushing (forward, back, left, right), not only where you look.
		net.minecraft.world.entity.player.Input input = player.getLastClientInput();
		int ahead = (input.forward() ? 1 : 0) - (input.backward() ? 1 : 0);
		int aside = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
		if (ahead != 0 || aside != 0) {
			Vec3 left = new Vec3(flat.z, 0, -flat.x);
			flat = flat.scale(ahead).add(left.scale(aside)).normalize();
		}
		// It shoves aside whoever it passes through the moment it leaves.
		int shoved = 0;
		for (Entity e : player.level().getEntities(player, player.getBoundingBox().inflate(1.5), e -> e instanceof LivingEntity && Targets.canHarm(player, e))) {
			if (shoved++ >= 8) {
				break;
			}
			Vec3 away = Effects.horizontal(e.position().subtract(player.position()), flat);
			Statuses.windPush((LivingEntity) e, away.scale(0.6).add(0, 0.2, 0));
		}
		player.setDeltaMovement(flat.scale(1.25).add(0, 0.42, 0));
		player.needsSync = true;
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
		player.resetFallDistance();
		ServerLevel level = player.level();
		Vfx.emit(level, ParticleTypes.GUST, player.position(), 1, 0.0, 0.0);
		ElementFx.gustRing(level, player.position(), 1.4);
		ElementFx.ring(level, player.position().add(0, 0.9, 0).subtract(flat.scale(0.9)), flat, ElementFx.WIND.secondary(), 0.3, 1.3, 0.04, 7);
		Feels.sound(level, player.position(), "wind_gale", 0.9F, 1.0F);
	}
}
