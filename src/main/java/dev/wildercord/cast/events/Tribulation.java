package dev.wildercord.cast.events;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.HeartCircles;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.Sigils;
import dev.wildercord.player.Heart;
import dev.wildercord.spell.Circles;
import dev.wildercord.spell.CoopTribulationRules;
import dev.wildercord.spell.TribulationRules;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A heart's tribulation ({@link TribulationRules}): when the 5th, 10th, 15th or 20th circle is ready and its meditation
 * completes, the sky answers. Waves of tempered Runebound come for the caster, stronger at every tier, the last led by a
 * Herald; beating them all forms the circle. Dying, fleeing or running out of time loses it (the condensed mana is kept, and
 * it can be faced again after a rest). Nothing here is saved: a restart sends its monsters away and the heart waits again.
 *
 * <p>Party members standing in the ring when it begins face it beside the caster ({@link CoopTribulationRules}): each one
 * makes every wave bigger and its monsters tougher, fixed from the start so leaving early can't shrink the storm. Only the
 * caster's fall ends it; an ally who falls or flees drops out. Allies who stand to the end take a share of the spoils.</p>
 */
public final class Tribulation {
	private static final int COLOR = 0xE8D8B0;
	private static final int STORM = 0x8C7CFF;
	private static final Identifier HEALTH = Wildercord.id("tribulation_health");
	private static final Identifier DAMAGE = Wildercord.id("tribulation_damage");
	/** On the last wave's Herald. */
	public static final String HERALD_TAG = "wildercord.tribulation_herald";
	/** On a rival tribulation's Shadow (it carries the Herald's tag too). */
	public static final String SHADOW_TAG = "wildercord.tribulation_shadow";
	private static final int SHADOW = 0x9A6CD8;

	private static final List<List<EntityType<? extends Mob>>> TYPES = List.of(
		List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.HUSK, EntityTypes.STRAY),
		List.of(EntityTypes.VINDICATOR, EntityTypes.SKELETON, EntityTypes.PILLAGER, EntityTypes.HUSK, EntityTypes.WITCH),
		List.of(EntityTypes.VINDICATOR, EntityTypes.WITHER_SKELETON, EntityTypes.PILLAGER, EntityTypes.STRAY, EntityTypes.WITCH),
		List.of(EntityTypes.VINDICATOR, EntityTypes.WITHER_SKELETON, EntityTypes.EVOKER, EntityTypes.BLAZE, EntityTypes.PILLAGER));
	private static final List<EntityType<? extends Mob>> HERALDS = List.of(
		EntityTypes.VINDICATOR, EntityTypes.EVOKER, EntityTypes.RAVAGER, EntityTypes.RAVAGER);

	private static final Map<UUID, Tribulation> ACTIVE = new HashMap<>();
	private static final Map<UUID, Long> RESTING = new HashMap<>();

	private final ServerPlayer player;
	private final ServerLevel level;
	private final int circle;
	private final Vec3 centre;
	private final ServerBossEvent bar;
	/** Party members facing it beside the caster, while they stay in it. */
	private final List<ServerPlayer> allies = new ArrayList<>();
	/** How many allies it began with: the storm keeps that size even if some drop out. */
	private int party;
	private final List<Mob> mobs = new ArrayList<>();
	private int wave;
	private int pending;
	private long nextWaveAt;
	private long waveDeadline;
	/** Between a beaten wave and the next. */
	private boolean breathing;

	private Tribulation(ServerPlayer player, int circle) {
		this.player = player;
		this.level = player.level();
		this.circle = circle;
		this.centre = player.position();
		this.bar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("boss.wildercord.tribulation", Circles.ordinal(circle), 1,
			TribulationRules.waves(circle)), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);
		this.nextWaveAt = level.getGameTime() + 60;
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (!ACTIVE.isEmpty()) ACTIVE.values().removeIf(t -> !t.tick());
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			for (Tribulation t : ACTIVE.values()) t.clear();
			ACTIVE.clear();
			RESTING.clear();
		});
	}

	/** Ends {@code player}'s tribulation without a verdict or a rest (its monsters are sent away). */
	public static void cancel(ServerPlayer player) {
		Tribulation t = ACTIVE.remove(player.getUUID());
		if (t != null) t.clear();
	}

	/** Whether {@code player} is facing a tribulation now. */
	public static boolean active(ServerPlayer player) {
		return ACTIVE.containsKey(player.getUUID());
	}

	/** Whether {@code player} is in any tribulation, their own or as someone's ally. */
	public static boolean engaged(ServerPlayer player) {
		if (active(player)) return true;
		for (Tribulation t : ACTIVE.values()) if (t.allies.contains(player)) return true;
		return false;
	}

	/** How many allies {@code caster}'s tribulation began with (0 when there is none). */
	public static int party(ServerPlayer caster) {
		Tribulation t = ACTIVE.get(caster.getUUID());
		return t == null ? 0 : t.party;
	}

	/**
	 * The meditation for circle {@code n} completed: begin its tribulation. Returns false (and the circle may simply form) when
	 * there is none to face here; on Peaceful no monster can come, so the circle forms without one.
	 */
	public static boolean begin(ServerPlayer player, int n) {
		if (!TribulationRules.tribulation(n) || player.level().getDifficulty() == Difficulty.PEACEFUL) return false;
		if (active(player)) return true;
		long now = player.level().getGameTime();
		Long until = RESTING.get(player.getUUID());
		if (until != null && now < until) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.tribulation.rest", (until - now + 19) / 20).withColor(COLOR));
			return true;
		}
		Tribulation t = new Tribulation(player, n);
		t.enlist();
		ACTIVE.put(player.getUUID(), t);
		t.open();
		return true;
	}

	/** Party members in the ring and free to fight join in, as many as count. */
	private void enlist() {
		for (UUID id : dev.wildercord.party.Parties.members(player)) {
			if (allies.size() >= CoopTribulationRules.MAX_ALLIES) break;
			if (id.equals(player.getUUID())) continue;
			ServerPlayer ally = level.getServer().getPlayerList().getPlayer(id);
			if (ally != null && ally.isAlive() && !ally.isSpectator() && ally.level() == level
					&& ally.position().distanceTo(centre) <= TribulationRules.RADIUS && !engaged(ally)) allies.add(ally);
		}
		party = CoopTribulationRules.allies(allies.size());
	}

	private int size(int wave) {
		return CoopTribulationRules.waveSize(TribulationRules.waveSize(circle, wave), party);
	}

	private List<ServerPlayer> everyone() {
		List<ServerPlayer> all = new ArrayList<>(allies.size() + 1);
		all.add(player);
		all.addAll(allies);
		return all;
	}

	private void overlay(Component message) {
		for (ServerPlayer p : everyone()) p.sendOverlayMessage(message);
	}

	/** The nearest fighter for a monster without a target. */
	private ServerPlayer nearest(Mob mob) {
		ServerPlayer best = player;
		for (ServerPlayer ally : allies) if (!ally.isCreative() && ally.distanceToSqr(mob) < best.distanceToSqr(mob)) best = ally;
		return best;
	}

	private void open() {
		for (ServerPlayer ally : allies) {
			bar.addPlayer(ally);
			ally.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
			ally.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.wildercord.tribulation").withColor(COLOR)));
			ally.sendSystemMessage(Component.translatable("message.wildercord.tribulation.ally_begin", player.getDisplayName(), Circles.ordinal(circle),
				(int) TribulationRules.RADIUS).withColor(COLOR));
		}
		if (!allies.isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.wildercord.tribulation.allies", allies.size()).withColor(COLOR));
		}
		bar.addPlayer(player);
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
		player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.wildercord.tribulation").withColor(COLOR)));
		player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("title.wildercord.tribulation.sub", Circles.ordinal(circle))));
		player.sendSystemMessage(Component.translatable("message.wildercord.tribulation.begin", Circles.ordinal(circle), TribulationRules.waves(circle),
			(int) TribulationRules.RADIUS).withColor(COLOR));
		ScreenFx.shake(level, centre, 0.6F, 32);
		Sigils.ground(level, centre, STORM, COLOR, 2.4F, 80);
		Light.groundRing(level, centre, STORM, 0.4, TribulationRules.RADIUS, 0.25, 40);
		strike(centre);
		WorldEvents.farSound(level, centre, SoundEvents.LIGHTNING_BOLT_THUNDER, 96, 0.7F);
	}

	/** A bolt of light that burns nothing. */
	private void strike(Vec3 at) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
		if (bolt == null) return;
		bolt.snapTo(at.x, at.y, at.z);
		bolt.setVisualOnly(true);
		level.addFreshEntity(bolt);
	}

	/** Every server tick; false once it's over. */
	private boolean tick() {
		long now = level.getGameTime();
		if (player.isRemoved() || !player.isAlive() || player.level() != level) {
			fail("died");
			return false;
		}
		if (player.position().distanceTo(centre) > TribulationRules.RADIUS) {
			fail("fled");
			return false;
		}
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			fail("peaceful");
			return false;
		}
		allies.removeIf(ally -> {
			boolean out = ally.isRemoved() || !ally.isAlive() || ally.level() != level || ally.position().distanceTo(centre) > TribulationRules.RADIUS;
			if (out) {
				bar.removePlayer(ally);
				if (!ally.isRemoved()) ally.sendSystemMessage(Component.translatable("message.wildercord.tribulation.ally_out").withColor(0xE07A5F));
				player.sendOverlayMessage(Component.translatable("message.wildercord.tribulation.ally_lost", ally.getDisplayName()).withColor(0xE07A5F));
			}
			return out;
		});
		int alive = 0;
		for (Mob mob : mobs) {
			if (mob.isAlive() && !mob.isRemoved()) {
				alive++;
				if (mob.getTarget() == null) {
					ServerPlayer near = nearest(mob);
					if (!near.isCreative()) mob.setTarget(near);
				}
				// One that strays too far is drawn back to the circle.
				if (mob.position().distanceTo(centre) > TribulationRules.RADIUS + 8) mob.teleportTo(centre.x, centre.y, centre.z);
			}
		}
		if (now % 10 == 0) {
			int size = wave == 0 ? 1 : size(wave) + (TribulationRules.lastWave(circle, wave) ? 1 : 0);
			bar.setName(Component.translatable("boss.wildercord.tribulation", Circles.ordinal(circle), Math.max(1, wave), TribulationRules.waves(circle)));
			bar.setProgress(wave == 0 ? 1F : Math.max(0F, Math.min(1F, (alive + pending) / (float) size)));
			Light.groundRing(level, centre, STORM, 0.3, TribulationRules.RADIUS, 0.2, 12);
		}
		if (wave > 0 && !breathing && pending == 0 && alive == 0) {
			if (TribulationRules.lastWave(circle, wave)) {
				win();
				return false;
			}
			breathing = true;
			nextWaveAt = now + TribulationRules.BREATH_TICKS;
			overlay(Component.translatable("message.wildercord.tribulation.breath", wave, TribulationRules.waves(circle)).withColor(COLOR));
		}
		if (wave > 0 && !breathing && now > waveDeadline) {
			fail("time");
			return false;
		}
		if ((wave == 0 || breathing) && now >= nextWaveAt) {
			breathing = false;
			spawnWave(wave + 1, now);
		}
		return true;
	}

	private void spawnWave(int n, long now) {
		wave = n;
		waveDeadline = now + TribulationRules.WAVE_TICKS;
		int size = size(n);
		int adepts = TribulationRules.adepts(circle, n);
		int tier = TribulationRules.tier(circle);
		List<EntityType<? extends Mob>> types = TYPES.get(tier - 1);
		boolean last = TribulationRules.lastWave(circle, n);
		overlay(Component.translatable(last ? (TribulationRules.rival(circle) ? "message.wildercord.tribulation.last_rival" : "message.wildercord.tribulation.last") : "message.wildercord.tribulation.wave",
			n, TribulationRules.waves(circle)).withColor(STORM));
		WorldEvents.farSound(level, centre, SoundEvents.LIGHTNING_BOLT_THUNDER, 96, 0.9F + 0.05F * n);
		pending = size + (last ? 1 : 0);
		for (int i = 0; i < size; i++) {
			boolean adept = i < adepts;
			EntityType<? extends Mob> type = types.get((i + n) % types.size());
			dev.wildercord.cast.Scheduler.later(1 + i * 8, () -> emerge(type, adept, false));
		}
		if (last) {
			EntityType<? extends Mob> leader = TribulationRules.rival(circle) ? EntityTypes.ZOMBIE : HERALDS.get(tier - 1);
			dev.wildercord.cast.Scheduler.later(20 + size * 8, () -> emerge(leader, true, true));
		}
	}

	private void emerge(EntityType<? extends Mob> type, boolean adept, boolean herald) {
		pending = Math.max(0, pending - 1);
		if (ACTIVE.get(player.getUUID()) != this) return;
		RandomSource random = level.getRandom();
		Vec3 at = null;
		for (int i = 0; i < 8 && at == null; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			double r = 8 + random.nextDouble() * 6;
			at = WorldEvents.standingSpot(level, centre.add(Math.cos(a) * r, 0, Math.sin(a) * r));
		}
		if (at == null) at = WorldEvents.standingSpot(level, centre);
		if (at == null) at = centre;
		boolean shadow = herald && TribulationRules.rival(circle);
		Mob mob = WorldEvents.spawnRunebound(level, type, at, adept, player, shadow ? mirroredSpell() : null);
		if (mob == null) return;
		if (shadow) {
			mirror(mob);
			ScreenFx.shake(level, at, 0.5F, 30);
			overlay(Component.translatable("message.wildercord.tribulation.shadow").withColor(SHADOW));
		} else {
			double health = CoopTribulationRules.healthBonus(TribulationRules.healthBonus(circle), party);
			temper(mob, herald ? health + 2.0 : health, TribulationRules.damageBonus(circle));
		}
		if (herald && !shadow) {
			mob.addTag(HERALD_TAG);
			mob.setCustomName(Component.translatable("entity.wildercord.tribulation_herald").withColor(COLOR));
			ScreenFx.shake(level, at, 0.4F, 24);
			overlay(Component.translatable("message.wildercord.tribulation.herald").withColor(COLOR));
		}
		mobs.add(mob);
		strike(at);
	}

	/** The caster's selected spell, if it compiles: the Shadow casts it back at them. Null leaves it a Runebound spell of its own. */
	private List<dev.wildercord.spell.RuneDef> mirroredSpell() {
		dev.wildercord.player.Spellbook book = dev.wildercord.player.Spellbooks.get(player);
		if (book.selected() < 0 || book.selected() >= book.spells().size()) return null;
		List<dev.wildercord.spell.RuneDef> spell = new ArrayList<>();
		for (String id : book.spells().get(book.selected())) dev.wildercord.spell.Runes.get(id).ifPresent(spell::add);
		return spell.isEmpty() || dev.wildercord.spell.SpellCompiler.compile(spell).isEmpty() ? null : spell;
	}

	/** Dresses a Shadow as the caster: their face, a copy of their armour and blade, their health and their pace. */
	private void mirror(Mob mob) {
		mob.addTag(SHADOW_TAG);
		mob.addTag(HERALD_TAG);
		net.minecraft.world.item.ItemStack head = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD);
		head.set(net.minecraft.core.component.DataComponents.PROFILE, net.minecraft.world.item.component.ResolvableProfile.createResolved(player.getGameProfile()));
		mob.setItemSlot(EquipmentSlot.HEAD, head);
		for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			mob.setItemSlot(slot, player.getItemBySlot(slot).copy());
		}
		net.minecraft.world.item.ItemStack blade = player.getMainHandItem();
		mob.setItemSlot(EquipmentSlot.MAINHAND, blade.isEmpty() ? new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD) : blade.copy());
		for (EquipmentSlot slot : EquipmentSlot.values()) mob.setDropChance(slot, 0F);
		AttributeInstance max = mob.getAttribute(Attributes.MAX_HEALTH);
		if (max != null) {
			double target = TribulationRules.shadowHealth(circle, player.getMaxHealth());
			max.addOrReplacePermanentModifier(new AttributeModifier(HEALTH, target - max.getValue(), AttributeModifier.Operation.ADD_VALUE));
			mob.setHealth(mob.getMaxHealth());
		}
		AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) speed.setBaseValue(TribulationRules.SHADOW_SPEED);
		AttributeInstance reinforcements = mob.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
		if (reinforcements != null) reinforcements.setBaseValue(0);
		mob.setCustomName(Component.translatable("entity.wildercord.tribulation_shadow", player.getName()).withColor(SHADOW));
		mob.setCustomNameVisible(true);
	}

	private static void temper(Mob mob, double health, double damage) {
		AttributeInstance max = mob.getAttribute(Attributes.MAX_HEALTH);
		if (max != null) {
			max.addOrReplacePermanentModifier(new AttributeModifier(HEALTH, health, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
			mob.setHealth(mob.getMaxHealth());
		}
		AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
		if (attack != null) {
			attack.addOrReplacePermanentModifier(new AttributeModifier(DAMAGE, damage, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
	}

	private void win() {
		bar.removeAllPlayers();
		Sigils.flash(level, player.position().add(0, 1.2, 0), 0xFF000000 | COLOR, 3.6F);
		ScreenFx.shake(level, centre, 0.3F, 24);
		player.sendSystemMessage(Component.translatable("message.wildercord.tribulation.won", Circles.ordinal(circle)).withColor(COLOR));
		if (Heart.ready(player) && Heart.circles(player) + 1 == circle) {
			HeartCircles.form(player);
			player.sendSystemMessage(Component.translatable("message.wildercord.tribulation.scar").withColor(0xE05A4A));
		}
		spoils();
		for (ServerPlayer ally : allies) {
			ally.sendSystemMessage(Component.translatable("message.wildercord.tribulation.ally_won", player.getDisplayName(), Circles.ordinal(circle)).withColor(COLOR));
			Vec3 at = ally.position().add(0, 0.8, 0);
			drop(at, new net.minecraft.world.item.ItemStack(dev.wildercord.content.WildercordItems.MANA_CRYSTAL,
				CoopTribulationRules.allyCrystals(TribulationRules.spoilCrystals(circle))));
			net.minecraft.world.entity.ExperienceOrb.award(level, at, TribulationRules.spoilXp(circle));
		}
	}

	/** Runes, Mana Crystals and experience where the caster stood: more at every tier. */
	private void spoils() {
		RandomSource random = level.getRandom();
		Vec3 at = player.position().add(0, 0.8, 0);
		for (int i = 0; i < TribulationRules.spoilRunes(circle); i++) {
			drop(at, dev.wildercord.content.RuneItem.stack(EventRules.rewardRune("rift", TribulationRules.spoilRuneTier(circle, random.nextDouble()), random.nextDouble())));
		}
		drop(at, new net.minecraft.world.item.ItemStack(dev.wildercord.content.WildercordItems.MANA_CRYSTAL, TribulationRules.spoilCrystals(circle)));
		net.minecraft.world.entity.ExperienceOrb.award(level, at, TribulationRules.spoilXp(circle));
		if (dev.wildercord.wildlife.SkyMountRules.rewards(circle)) {
			drop(at, new net.minecraft.world.item.ItemStack(dev.wildercord.wildlife.MountContent.SKYRAY_BRIDLE));
			player.sendSystemMessage(Component.translatable("message.wildercord.tribulation.bridle").withColor(0xA8D0FF));
		}
	}

	private void drop(Vec3 at, net.minecraft.world.item.ItemStack stack) {
		net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(level, at.x, at.y, at.z, stack);
		item.setDeltaMovement(level.getRandom().nextGaussian() * 0.08, 0.3, level.getRandom().nextGaussian() * 0.08);
		item.setGlowingTag(true);
		level.addFreshEntity(item);
	}

	private void fail(String why) {
		clear();
		RESTING.put(player.getUUID(), level.getGameTime() + TribulationRules.RETRY_TICKS);
		if (!player.isRemoved()) {
			player.sendSystemMessage(Component.translatable("message.wildercord.tribulation.failed." + why, Circles.ordinal(circle),
				TribulationRules.RETRY_TICKS / 1200).withColor(0xE07A5F));
		}
		for (ServerPlayer ally : allies) {
			if (!ally.isRemoved()) ally.sendSystemMessage(Component.translatable("message.wildercord.tribulation.ally_failed", player.getDisplayName()).withColor(0xE07A5F));
		}
	}

	private void clear() {
		bar.removeAllPlayers();
		for (Mob mob : mobs) {
			if (mob.isAlive() && !mob.isRemoved()) WorldEvents.vanish(level, mob);
		}
		mobs.clear();
	}
}
