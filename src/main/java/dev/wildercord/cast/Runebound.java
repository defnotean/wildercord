package dev.wildercord.cast;

import dev.wildercord.Wildercord;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.player.Heart;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellNames;
import dev.wildercord.spell.SpellSigil;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.illager.Vindicator;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The Runebound: monsters that carry a Cord and cast real spells, built and run by exactly the
 * same engine as a player's. Their spell floats above them as rune icons and a name, so you can
 * read what's coming; before each cast a magic circle opens in front of them for a second, long
 * enough to dodge, block or shoot the bolt out of the air. Slay one for a chance at a rune from
 * its Cord.
 *
 * <p>A few percent of zombies, skeletons, witches and illagers spawn Runebound (more on harder
 * difficulties); one in six of those is an Adept with a stronger, modified spell.</p>
 */
public final class Runebound {
	private Runebound() {}

	private static final Identifier HEALTH = Wildercord.id("runebound_health");
	private static final int TELEGRAPH = 22;

	/** A Runebound's state between ticks: when it may cast again, and the cast it's preparing. */
	private static final class State {
		long readyAt;
		long castAt;
		LivingEntity target;
	}

	private static final Map<UUID, Mob> LOADED = new HashMap<>();
	private static final Map<UUID, State> STATES = new HashMap<>();
	/** Monsters already rolled for, so a reload never rolls again. */
	private static final String ROLLED_TAG = "wildercord.rolled";
	private static final String ADEPT_TAG = "wildercord.adept";

	public static void init() {
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (!(entity instanceof Mob mob)) {
				return;
			}
			if (mob.hasAttached(WildercordAttachments.RUNEBOUND)) {
				// Its extra health, for a Runebound saved before that was kept.
				AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
				if (health != null && !health.hasModifier(HEALTH)) {
					health.addPermanentModifier(healthBonus(mob.entityTags().contains(ADEPT_TAG)));
				}
				LOADED.put(mob.getUUID(), mob);
				showMarks(mob);
				return;
			}
			if (mob.entityTags().contains(ROLLED_TAG) || pool(mob).isEmpty()) {
				return;
			}
			mob.addTag(ROLLED_TAG);
			if (mob.hasCustomName()) {
				// Someone named it: its name isn't ours to write over.
				return;
			}
			double chance = 0.02 + 0.006 * level.getCurrentDifficultyAt(mob.blockPosition()).getEffectiveDifficulty();
			// Inside an Archive, the monsters are the Archive's: a third of them carry Cords.
			if (level.structureManager().getStructureWithPieceAt(mob.blockPosition(), dev.wildercord.world.WildercordWorldgen.ARCHIVES).isValid()
					|| level.structureManager().getStructureWithPieceAt(mob.blockPosition(), dev.wildercord.world.dungeons.DungeonWorldgen.DUNGEONS).isValid()) {
				chance = 0.35;
			}
			chance *= dev.wildercord.config.Config.get().runeboundChance();
			if (level.getDifficulty() != Difficulty.PEACEFUL && level.getRandom().nextDouble() < chance) {
				bind(mob, level.getRandom().nextInt(6) == 0);
			}
		});
		ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
			LOADED.remove(entity.getUUID());
			STATES.remove(entity.getUUID());
		});
		ServerLivingEntityEvents.AFTER_DEATH.register(Runebound::onDeath);
		// A Runebound that turns into something else (a zombie drowning, a skeleton freezing) keeps its
		// Cord if the new creature could carry one, and loses the nameplate if it couldn't.
		ServerLivingEntityEvents.MOB_CONVERSION.register((previous, converted, params) -> {
			List<RuneDef> spell = spellOf(previous);
			if (spell.isEmpty()) {
				return;
			}
			if (!pool(converted).isEmpty()) {
				bind(converted, spell, previous.entityTags().contains(ADEPT_TAG));
			} else {
				converted.removeAttached(WildercordAttachments.RUNEBOUND);
				converted.removeAttached(WildercordAttachments.RUNE_MARKS);
				converted.removeTag(ADEPT_TAG);
				AttributeInstance health = converted.getAttribute(Attributes.MAX_HEALTH);
				if (health != null) {
					health.removeModifier(HEALTH);
				}
				converted.setCustomName(null);
				converted.setCustomNameVisible(false);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (LOADED.isEmpty()) {
				return;
			}
			for (Mob mob : new ArrayList<>(LOADED.values())) {
				if (mob.isRemoved() || !mob.isAlive()) {
					LOADED.remove(mob.getUUID());
					STATES.remove(mob.getUUID());
				} else if (mob.level() instanceof ServerLevel level && level.isPositionEntityTicking(mob.blockPosition())) {
					// Only where the world is running: a monster frozen at the edge of the loaded area doesn't cast.
					tick(level, mob);
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			LOADED.clear();
			STATES.clear();
		});
	}

	// ------------------------------------------------------------------ spells

	/** The spells a monster of this kind may carry. Empty for monsters that never become Runebound. */
	static List<List<RuneDef>> pool(Mob mob) {
		// The monsters of the wilds each say which spells suit them (a Bramblewalker's vines, a harpy's lightning...).
		if (mob instanceof dev.wildercord.monster.RuneboundKin kin) {
			return kin.runeboundSpells();
		}
		if (mob instanceof AbstractSkeleton) {
			return List.of(List.of(Runes.BOLT, Runes.FROST), List.of(Runes.BOLT, Runes.SHOCK), List.of(Runes.ARC, Runes.FIRE),
				List.of(Runes.BEAM, Runes.HARM), List.of(Runes.BOLT, Runes.VENOM));
		}
		if (mob instanceof Witch) {
			return List.of(List.of(Runes.ZONE, Runes.VENOM), List.of(Runes.MINE, Runes.CHILL), List.of(Runes.ORB, Runes.HARM),
				List.of(Runes.RAIN, Runes.SHOCK), List.of(Runes.BOLT, Runes.BLIND));
		}
		if (mob instanceof Pillager || mob instanceof Vindicator) {
			return List.of(List.of(Runes.CRESCENT, Runes.HARM), List.of(Runes.BLITZ, Runes.HARM), List.of(Runes.CONE, Runes.FIRE),
				List.of(Runes.BARRAGE, Runes.HARM), List.of(Runes.BOLT, Runes.FIRE));
		}
		if (mob instanceof Zombie || mob instanceof Husk || mob instanceof Drowned || mob instanceof ZombieVillager) {
			return List.of(List.of(Runes.BURST, Runes.PUSH, Runes.HARM), List.of(Runes.TOUCH, Runes.VENOM), List.of(Runes.WAVE, Runes.FROST),
				List.of(Runes.RING, Runes.SHOCK), List.of(Runes.SELF, Runes.SWIFT, Runes.EMPOWER));
		}
		return List.of();
	}

	/** Makes a monster Runebound (an Adept carries a modified, stronger spell). */
	public static void bind(Mob mob, boolean adept) {
		bind(mob, pick(mob, adept), adept);
	}

	/**
	 * For a monster placed while the world generates (on a worker thread, before it's in the
	 * world): marks it without touching the live table. It joins when its chunk loads.
	 */
	public static void bindAtGeneration(Mob mob, boolean adept) {
		mark(mob, pick(mob, adept), adept);
	}

	/** As {@link #bindAtGeneration(Mob, boolean)}, with a spell chosen for it (a dungeon's guards carry spells that suit it). */
	public static void bindAtGeneration(Mob mob, List<RuneDef> spell, boolean adept) {
		mark(mob, spell, adept);
	}

	private static List<RuneDef> pick(Mob mob, boolean adept) {
		List<List<RuneDef>> pool = pool(mob);
		if (pool.isEmpty()) {
			pool = List.of(List.of(Runes.BOLT, Runes.HARM));
		}
		List<RuneDef> spell = new ArrayList<>(pool.get(mob.getRandom().nextInt(pool.size())));
		if (adept) {
			RuneDef shape = spell.getFirst();
			spell.add(shape.has(dev.wildercord.spell.Trait.SPLIT) ? Runes.SPLIT_MOD : Runes.AMPLIFY);
		}
		return spell;
	}

	public static void bind(Mob mob, List<RuneDef> spell, boolean adept) {
		mark(mob, spell, adept);
		showMarks(mob);
		LOADED.put(mob.getUUID(), mob);
	}

	/**
	 * Writes its Cord on its body: the synced rune marks everyone around draws, glowing in its
	 * spell's colour. Not done by {@link #mark} (a monster placed during world generation isn't in
	 * the world yet, so there's nobody to sync to); it happens as it loads instead.
	 */
	private static void showMarks(Mob mob) {
		List<RuneDef> spell = spellOf(mob);
		if (!spell.isEmpty()) {
			mob.setAttached(WildercordAttachments.RUNE_MARKS,
				new WildercordAttachments.RuneMarks(elementColor(spell), mob.entityTags().contains(ADEPT_TAG), 0));
		}
	}

	private static void mark(Mob mob, List<RuneDef> spell, boolean adept) {
		mob.setAttached(WildercordAttachments.RUNEBOUND, spell.stream().map(RuneDef::id).toList());
		mob.addTag(ROLLED_TAG);
		if (adept) {
			mob.addTag(ADEPT_TAG);
		} else {
			mob.removeTag(ADEPT_TAG);
		}
		// Permanent, so it's saved: a guard placed with the Archive keeps it once its chunk loads.
		mob.getAttribute(Attributes.MAX_HEALTH).addOrReplacePermanentModifier(healthBonus(adept));
		mob.setHealth(mob.getMaxHealth());
		mob.setCustomName(nameplate(spell, adept, false));
		mob.setCustomNameVisible(true);
	}

	private static AttributeModifier healthBonus(boolean adept) {
		return new AttributeModifier(HEALTH, adept ? 1.2 : 0.6, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
	}

	public static List<RuneDef> spellOf(Mob mob) {
		List<RuneDef> runes = new ArrayList<>();
		for (String id : mob.getAttachedOrElse(WildercordAttachments.RUNEBOUND, List.<String>of())) {
			Runes.get(id).ifPresent(runes::add);
		}
		return runes;
	}

	/** Rune icons and the spell's name, in its element's colour. Bright and bold while it's being cast. */
	static MutableComponent nameplate(List<RuneDef> spell, boolean adept, boolean casting) {
		MutableComponent name = Component.empty();
		if (casting) {
			name.append(Component.literal("» ").withColor(0xFFFFFF));
		}
		for (RuneDef rune : spell) {
			name.append(Component.object(new AtlasSprite(AtlasIds.ITEMS, Wildercord.id("item/rune/" + rune.path())),
				Component.literal("◆").withColor(RuneColors.of(rune))));
		}
		int color = elementColor(spell);
		MutableComponent title = Component.literal(" " + (adept ? "Adept · " : "") + SpellNames.auto(spell)).withColor(casting ? 0xFFFFFF : color);
		if (casting) {
			title.withStyle(ChatFormatting.BOLD);
		}
		name.append(title);
		if (casting) {
			name.append(Component.literal(" «").withColor(0xFFFFFF));
		}
		return name;
	}

	static int elementColor(List<RuneDef> spell) {
		for (RuneDef rune : spell) {
			if (rune.family() == RuneFamily.EFFECT) {
				return RuneColors.of(rune);
			}
		}
		return RuneColors.SHAPE;
	}

	/** How close a monster wants to be before casting this spell. */
	static double range(List<RuneDef> spell) {
		String shape = spell.isEmpty() ? "" : spell.getFirst().path();
		return switch (shape) {
			case "touch" -> 3.5;
			case "burst", "ring", "barrage", "imprint" -> 5.0;
			case "glaive" -> 12.0;
			case "latch" -> 16.0;
			case "cone" -> 6.0;
			case "blitz" -> 8.0;
			case "self" -> 12.0;
			case "wave" -> 12.0;
			case "crescent" -> 16.0;
			case "zone", "rain", "mine", "pillar", "totem", "domain" -> 18.0;
			default -> 22.0;
		};
	}

	static double power(ServerLevel level) {
		return switch (level.getDifficulty()) {
			case PEACEFUL, EASY -> 0.6;
			case NORMAL -> 0.8;
			case HARD -> 1.0;
		};
	}

	// ------------------------------------------------------------------ casting

	/** Cuts a monster's telegraphed cast short and holds its next one back {@code delay} ticks (Silence, Manaburn). */
	public static void interrupt(Mob mob, int delay) {
		State state = STATES.get(mob.getUUID());
		List<RuneDef> spell = spellOf(mob);
		if (state == null || spell.isEmpty()) {
			return;
		}
		state.castAt = 0;
		state.readyAt = Math.max(state.readyAt, mob.level().getGameTime() + delay);
		mob.setCustomName(nameplate(spell, mob.entityTags().contains("wildercord.adept"), false));
	}

	private static void tick(ServerLevel level, Mob mob) {
		long now = level.getGameTime();
		State state = STATES.computeIfAbsent(mob.getUUID(), k -> {
			State s = new State();
			s.readyAt = now + 40 + mob.getRandom().nextInt(40);
			return s;
		});
		List<RuneDef> spell = spellOf(mob);
		if (spell.isEmpty()) {
			return;
		}
		boolean adept = mob.entityTags().contains("wildercord.adept");
		int color = elementColor(spell);
		if (now % 20 == mob.getId() % 20) {
			// A faint rune circle turns at its feet.
			Vfx.ring(level, new DustParticleOptions(color, 0.7F), mob.position().add(0, 0.08, 0), 0.7, 10);
			Vfx.emit(level, ParticleTypes.ENCHANT, mob.position().add(0, mob.getBbHeight() + 0.3, 0), 2, 0.2, 0.3);
		}
		if (CastLock.locked(mob)) {
			// A silenced monster loses its spell: what it was winding up goes out, and it can't begin another.
			interrupt(mob);
			return;
		}
		if (state.castAt > 0) {
			LivingEntity target = state.target;
			mob.getNavigation().stop();
			if (target != null && target.isAlive()) {
				aimAt(mob, target);
			}
			if (now >= state.castAt) {
				state.castAt = 0;
				mob.setCustomName(nameplate(spell, adept, false));
				if (target != null && target.isAlive() && target.level() == level && mob.hasLineOfSight(target)) {
					aimAt(mob, target);
					cast(level, mob, spell, power(level) * (adept ? 1.15 : 1.0));
				}
				state.readyAt = now + 70 + mob.getRandom().nextInt(50);
			}
			return;
		}
		LivingEntity target = mob.getTarget();
		if (now < state.readyAt || target == null || !target.isAlive() || !Targets.canHarm(mob, target)) {
			return;
		}
		double distance = mob.distanceTo(target);
		if (distance > range(spell) || !mob.hasLineOfSight(target)) {
			return;
		}
		state.target = target;
		state.castAt = now + TELEGRAPH;
		telegraph(level, mob, spell, target, TELEGRAPH);
		mob.setCustomName(nameplate(spell, adept, true));
		// Its marks flare until the spell leaves its hand.
		WildercordAttachments.RuneMarks marks = mob.getAttached(WildercordAttachments.RUNE_MARKS);
		if (marks != null) {
			mob.setAttached(WildercordAttachments.RUNE_MARKS, marks.casting(state.castAt));
		}
	}

	/** Whether a Runebound is telegraphing a cast right now (for the tests, and for what can break one). */
	public static boolean casting(Mob mob) {
		State state = STATES.get(mob.getUUID());
		return state != null && state.castAt > 0;
	}

	/** Breaks the cast a Runebound is telegraphing (its next comes a little later). Returns whether it had one. */
	public static boolean interrupt(Mob mob) {
		State state = STATES.get(mob.getUUID());
		if (state == null || state.castAt <= 0) {
			return false;
		}
		state.castAt = 0;
		state.readyAt = mob.level().getGameTime() + 40;
		List<RuneDef> spell = spellOf(mob);
		if (!spell.isEmpty()) {
			mob.setCustomName(nameplate(spell, mob.entityTags().contains("wildercord.adept"), false));
		}
		WildercordAttachments.RuneMarks marks = mob.getAttached(WildercordAttachments.RUNE_MARKS);
		if (marks != null) {
			mob.setAttached(WildercordAttachments.RUNE_MARKS, marks.casting(0));
		}
		return true;
	}

	/** Turns a monster to face a point: body, head and look all agree, so the spell flies true. */
	static void aimAt(Mob mob, LivingEntity target) {
		Vec3 d = target.getBoundingBox().getCenter().subtract(mob.getEyePosition());
		double flat = Math.sqrt(d.x * d.x + d.z * d.z);
		float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, flat));
		mob.setYRot(yaw);
		mob.setYHeadRot(yaw);
		mob.setYBodyRot(yaw);
		mob.setXRot(pitch);
		mob.getLookControl().setLookAt(target, 60, 60);
	}

	/** A telegraph circle's radius, in blocks: a small seal held in the hand. */
	static final float TELEGRAPH_RADIUS = 0.4F;

	/**
	 * Where a caster holds its telegraph: out in the right hand, below the eyes, far enough to the
	 * side that a circle of {@code radius} leaves the caster's face in view.
	 */
	static Vec3 hands(Mob mob, Vec3 look, float radius) {
		Vec3 right = look.cross(new Vec3(0, 1, 0));
		right = right.lengthSqr() < 1.0E-4 ? Vec3.ZERO : right.normalize();
		return mob.getEyePosition().add(look.scale(0.9)).add(right.scale(mob.getBbWidth() * 0.5 + radius * 0.8))
			.add(0, -0.3 - mob.getBbHeight() * 0.18, 0);
	}

	/** The warning: the spell's circle opens in the caster's hand, and where an area spell will land is marked. */
	static void telegraph(ServerLevel level, Mob mob, List<RuneDef> spell, LivingEntity target, int ticks) {
		int color = elementColor(spell);
		Vec3 look = target.getBoundingBox().getCenter().subtract(mob.getEyePosition()).normalize();
		// The spell's own circle, readable ring by ring: learn the runes and you know what's coming.
		Sigils.spell(level, mob.getEyePosition().subtract(look.scale(0.7)).add(0, -0.3, 0), look, spell, color, TELEGRAPH_RADIUS, ticks + 4);
		String shape = spell.getFirst().path();
		if (shape.equals("zone") || shape.equals("rain") || shape.equals("mine") || shape.equals("domain")) {
			Sigils.target(level, CastEngine.ground(level, target.position().add(0, 1, 0)), color, 3.0F, ticks + 10);
		}
		Fx.sound(level, mob.position(), SoundEvents.EVOKER_PREPARE_ATTACK, 0.9F, 1.2F);
		Fx.sound(level, mob.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7F, 1.4F);
	}

	public static void cast(ServerLevel level, Mob mob, List<RuneDef> spell, double power) {
		SpellCompiler.Compiled compiled = SpellCompiler.compile(spell);
		if (compiled.isEmpty()) {
			return;
		}
		Heart.Bonuses bonuses = new Heart.Bonuses(power, 1.0, 1.0, 1.0);
		Cast cast = new Cast(mob, 1, bonuses, false, null, new Cast.Info(compiled.root(), spell.size(), "", List.copyOf(spell)));
		CastEngine.cast(cast, compiled.root());
	}

	// ------------------------------------------------------------------ slaying one

	private static void onDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
		if (!(entity instanceof Mob mob) || !mob.hasAttached(WildercordAttachments.RUNEBOUND) || !(mob.level() instanceof ServerLevel level)) {
			return;
		}
		ServerPlayer killer = source.getEntity() instanceof ServerPlayer p ? p : null;
		if (killer == null) {
			return;
		}
		killer.setAttached(WildercordAttachments.RUNEBOUND_SLAIN, Heart.runeboundSlain(killer) + 1);
		Grimoire.feat(killer, Feats.RUNEBOUND);
		List<RuneDef> spell = spellOf(mob);
		boolean adept = mob.entityTags().contains("wildercord.adept");
		if (adept) {
			dev.wildercord.advancement.Advancements.moment(killer, dev.wildercord.advancement.Advancements.RUNEBOUND_ADEPT);
		}
		Vec3 at = mob.position().add(0, 0.5, 0);
		// The Cord it carried breaks: sometimes a rune survives.
		if (!spell.isEmpty() && level.getRandom().nextFloat() < (adept ? 0.6F : 0.35F)) {
			RuneDef rune = spell.get(level.getRandom().nextInt(spell.size()));
			level.addFreshEntity(new ItemEntity(level, at.x, at.y, at.z, RuneItem.stack(rune)));
		}
		if (level.getRandom().nextFloat() < (adept ? 0.2F : 0.06F)) {
			level.addFreshEntity(new ItemEntity(level, at.x, at.y, at.z, new ItemStack(WildercordItems.TORN_PAGE)));
		}
		// Now and then an Adept's Cord was threaded with a rune of the world, found far from here.
		if (adept && level.getRandom().nextInt(100) < dev.wildercord.content.WildercordLoot.adeptFindChance()) {
			level.addFreshEntity(new ItemEntity(level, at.x, at.y, at.z,
				dev.wildercord.content.WildercordLoot.foundRune(dev.wildercord.spell.RuneSources.RUNEBOUND_ADEPT.id(), level.getRandom())));
		}
		ExperienceOrb.award(level, at, adept ? 20 : 10);
		int color = elementColor(spell);
		Vfx.radial(level, new DustParticleOptions(color, 1.2F), at.add(0, 0.5, 0), 24, 0.25);
		Vfx.radial(level, ParticleTypes.ENCHANT, at.add(0, 0.5, 0), 20, 0.5);
		Fx.sound(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 0.8F);
	}

	/** The nearest Runebound to a point, for the command and the tests. */
	public static Optional<Mob> nearest(ServerLevel level, Vec3 at, double range) {
		Mob best = null;
		for (Mob mob : LOADED.values()) {
			if (mob.level() == level && mob.position().distanceTo(at) <= range && (best == null || mob.distanceToSqr(at) < best.distanceToSqr(at))) {
				best = mob;
			}
		}
		return Optional.ofNullable(best);
	}
}
