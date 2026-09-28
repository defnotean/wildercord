package dev.wildercord.cast.events;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Fx;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.ScreenFx;
import dev.wildercord.cast.Sigils;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordItems;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A rift siege. A tear of violet light opens beside a settled place, a spell circle turning on
 * either face, and three waves of Runebound pour out of it over about two minutes: more with more
 * players near, Adepts in the later waves, and in the last a Riftcaller, a Runebound illager with a
 * boss bar. Players close it by beating every wave, or by striking the tear with spells of three
 * different elements; either way it gives runes, Blank Runes, a Mana Crystal, experience and the
 * Riftwarden feat. Left alone, it closes itself a while after its last wave and takes its monsters
 * back with it.
 *
 * <p>Spells find the tear through an invisible, unbreakable armour stand standing in it (bolts stop
 * on it and area spells hit it); nothing here is saved, and the stand and any monster left over
 * are removed as their chunks load after a restart.</p>
 */
public final class RiftSiege {
	private static final int TEAR = 0xA060FF;
	private static final int RIM = 0xEADCFF;
	private static final double HEIGHT = 3.6;
	private static final Identifier RIFTCALLER_HEALTH = Wildercord.id("riftcaller_health");
	/** On the Riftcaller, so anything can tell it from the rest. */
	public static final String RIFTCALLER_TAG = "wildercord.riftcaller";

	private static final List<List<EntityType<? extends Mob>>> WAVE_TYPES = List.of(
		List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.ZOMBIE, EntityTypes.HUSK, EntityTypes.SKELETON),
		List.of(EntityTypes.VINDICATOR, EntityTypes.SKELETON, EntityTypes.ZOMBIE, EntityTypes.PILLAGER, EntityTypes.STRAY, EntityTypes.ZOMBIE),
		List.of(EntityTypes.VINDICATOR, EntityTypes.PILLAGER, EntityTypes.SKELETON, EntityTypes.WITCH, EntityTypes.ZOMBIE, EntityTypes.SKELETON));

	final ServerLevel level;
	/** The ground at the tear's foot, and its middle. */
	final Vec3 base;
	final Vec3 centre;
	/** The way the tear faces (level, toward whoever it opened near), and along it. */
	private final Vec3 facing;
	private final Vec3 side;
	private final long opened;
	private int wave;
	private int waveSize;
	/** Monsters of the current wave still to step out of the tear. */
	private int pending;
	private long nextWaveAt;
	private long lastWaveAt;
	private long lastNear;
	private boolean closed;
	private final List<UUID> mobs = new ArrayList<>();
	private UUID riftcaller;
	private ArmorStand anchor;
	private final Set<String> elements = new LinkedHashSet<>();
	private final Set<UUID> fighters = new LinkedHashSet<>();
	private final ServerBossEvent bar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("boss.wildercord.rift", 1, EventRules.WAVES),
		BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);

	RiftSiege(ServerLevel level, Vec3 base, Vec3 toward) {
		this.level = level;
		this.base = base;
		this.centre = base.add(0, HEIGHT / 2 + 0.2, 0);
		Vec3 f = toward.subtract(base).multiply(1, 0, 1);
		this.facing = f.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : f.normalize();
		this.side = new Vec3(-facing.z, 0, facing.x);
		this.opened = level.getGameTime();
		this.nextWaveAt = opened + EventRules.waveAt(1);
		this.lastNear = opened;
	}

	// ------------------------------------------------------------------ where

	/** Room for a rift 14 to 26 blocks from the player: loaded, level, dry ground with open air above. */
	static Vec3 spotNear(ServerLevel level, ServerPlayer player) {
		RandomSource random = level.getRandom();
		for (int i = 0; i < 16; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			double r = 14 + random.nextDouble() * 12;
			Vec3 spot = room(level, player.position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
			if (spot != null) {
				return spot;
			}
		}
		return null;
	}

	/** A few blocks in front of the player (the command's {@code here}). */
	static Vec3 spotHere(ServerLevel level, ServerPlayer player) {
		Vec3 look = player.getLookAngle().multiply(1, 0, 1);
		look = look.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : look.normalize();
		for (int d = 6; d <= 12; d++) {
			Vec3 spot = room(level, player.position().add(look.scale(d)));
			if (spot != null) {
				return spot;
			}
		}
		return null;
	}

	private static Vec3 room(ServerLevel level, Vec3 near) {
		Vec3 spot = WorldEvents.standingSpot(level, near);
		if (spot == null) {
			return null;
		}
		for (int y = 0; y < 5; y++) {
			net.minecraft.core.BlockPos p = net.minecraft.core.BlockPos.containing(spot.add(0, y, 0));
			if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
				return null;
			}
		}
		return spot;
	}

	// ------------------------------------------------------------------ opening

	void begin() {
		placeAnchor();
		WorldEvents.farSound(level, centre, EventSounds.RIFT_OPEN, 64, 1.0F);
		ScreenFx.shake(level, centre, 0.5F, 32);
		Sigils.flash(level, centre, RIM, 4.0F);
		Light.groundRing(level, base, TEAR, 0.4, 9.0, 0.25, 16);
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(centre) <= 64) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.rift_opens", WorldEvents.direction(player.position(), centre))
					.withColor(TEAR));
			}
		}
	}

	/** The invisible stand spells strike the tear through: unbreakable, silent, weightless, as tall as the tear. */
	private void placeAnchor() {
		if (anchor != null) {
			// One that went with its chunk is removed when it loads again.
			WorldEvents.forget(anchor.getUUID());
			anchor = null;
		}
		if (!level.isPositionEntityTicking(net.minecraft.core.BlockPos.containing(base))) {
			return;
		}
		ArmorStand stand = EntityTypes.ARMOR_STAND.create(level, EntitySpawnReason.EVENT);
		if (stand == null) {
			return;
		}
		stand.snapTo(base.x, base.y + 0.2, base.z);
		stand.setInvisible(true);
		stand.setNoGravity(true);
		stand.setPermanentlyInvulnerable(true);
		stand.setSilent(true);
		stand.setNoBasePlate(true);
		AttributeInstance scale = stand.getAttribute(Attributes.SCALE);
		if (scale != null) {
			scale.setBaseValue(1.7);
		}
		WorldEvents.mark(stand);
		level.addFreshEntity(stand);
		anchor = stand;
	}

	// ------------------------------------------------------------------ while it's open

	/** Every server tick; false once it has closed. */
	boolean tick(long now) {
		if (closed) {
			return false;
		}
		if (!level.isLoaded(net.minecraft.core.BlockPos.containing(base))) {
			// Its ground was unloaded: it closes rather than keep anything loaded.
			close(false, null);
			return false;
		}
		if (anchor == null || anchor.isRemoved()) {
			placeAnchor();
		}
		if (now % 4 == 0) {
			drawTear(now);
		}
		int alive = alive();
		if (now % 20 == 0) {
			boolean near = false;
			for (ServerPlayer player : level.players()) {
				double d = player.position().distanceTo(centre);
				if (d <= 48 && !player.isSpectator()) {
					bar.addPlayer(player);
					near = true;
					fighters.add(player.getUUID());
				} else if (d > 64) {
					bar.removePlayer(player);
				}
			}
			if (near) {
				lastNear = now;
			}
			updateBar(alive);
		}
		boolean beaten = wave >= 1 && pending == 0 && alive == 0;
		if (beaten && wave < EventRules.WAVES) {
			// A wave beaten early brings the next one sooner.
			nextWaveAt = Math.min(nextWaveAt, now + EventRules.WAVE_EARLY);
		}
		if (wave < EventRules.WAVES && now >= nextWaveAt) {
			spawnWave(wave + 1, now);
		} else if (wave >= EventRules.WAVES && beaten) {
			close(true, null);
			return false;
		} else if ((wave >= EventRules.WAVES && now > lastWaveAt + EventRules.RIFT_LINGER) || now - lastNear > EventRules.RIFT_ABANDONED) {
			close(false, null);
			return false;
		}
		return true;
	}

	/** How many of its monsters still stand (the Riftcaller too). */
	public int alive() {
		int n = 0;
		for (UUID id : mobs) {
			if (level.getEntity(id) instanceof Mob mob && mob.isAlive()) {
				n++;
			}
		}
		return n;
	}

	/** The ground at the tear's foot. */
	public Vec3 base() {
		return base;
	}

	public Vec3 centre() {
		return centre;
	}

	public int wave() {
		return wave;
	}

	public boolean closed() {
		return closed;
	}

	/** The Riftcaller, while it's alive. */
	public Mob riftcaller() {
		return riftcaller != null && level.getEntity(riftcaller) instanceof Mob mob && mob.isAlive() ? mob : null;
	}

	private void updateBar(int alive) {
		Mob boss = riftcaller();
		if (boss != null) {
			bar.setName(Component.translatable("entity.wildercord.riftcaller"));
			bar.setColor(BossEvent.BossBarColor.PINK);
			bar.setOverlay(BossEvent.BossBarOverlay.NOTCHED_10);
			bar.setProgress(Math.max(0, boss.getHealth() / boss.getMaxHealth()));
		} else {
			bar.setName(Component.translatable("boss.wildercord.rift", Math.max(1, wave), EventRules.WAVES));
			bar.setProgress(wave == 0 ? 1.0F : Math.max(0, Math.min(1, alive / (float) Math.max(1, waveSize))));
		}
	}

	/** The tear: a jagged lens of violet light, darkness down its middle, a spell circle turning on either face. */
	private void drawTear(long now) {
		RandomSource random = level.getRandom();
		double open = Math.min(1.0, (now - opened) / 30.0);
		int segments = 6;
		Vec3[] left = new Vec3[segments + 1];
		Vec3[] right = new Vec3[segments + 1];
		for (int i = 0; i <= segments; i++) {
			double h = HEIGHT * i / segments;
			double w = 0.55 * open * Math.sin(Math.PI * i / segments);
			double jitter = (i == 0 || i == segments) ? 0 : (random.nextDouble() - 0.5) * 0.18;
			Vec3 at = base.add(0, 0.2 + h, 0);
			left[i] = at.add(side.scale(w + jitter));
			right[i] = at.add(side.scale(-w - jitter));
		}
		for (int i = 0; i < segments; i++) {
			Light.ray(level, left[i], left[i + 1], TEAR, 0.07, 6);
			Light.ray(level, right[i], right[i + 1], TEAR, 0.07, 6);
		}
		Light.ray(level, base.add(0, 0.5, 0), base.add(0, HEIGHT - 0.1, 0), ElementFx.dark(0x2A0850), 0.3 * open + 0.02, 6);
		Light.ray(level, base.add(0, 0.4, 0), base.add(0, HEIGHT, 0), RIM, 0.03, 6);
		if (now % 12 == 0) {
			for (int s = -1; s <= 1; s += 2) {
				Vec3 n = facing.scale(s);
				Sigils.layer(level, centre.add(n.scale(0.35)), n, SigilOption.CIRCLE, TEAR, 1.9F, 16, 0.05F);
				Sigils.layer(level, centre.add(n.scale(0.37)), n, SigilOption.RING, RIM, 2.4F, 16, -0.07F);
			}
		}
		if (now % 40 == 0) {
			Sigils.send(level, SigilOption.flat(SigilOption.STAR, TEAR, 3.2F, 44, 0.03F), base.add(0, 0.07, 0));
		}
		Fx.send(level, ParticleTypes.REVERSE_PORTAL, centre.x, centre.y, centre.z, 2, 0.15, HEIGHT * 0.3, 0.15, 0.02);
	}

	private void spawnWave(int n, long now) {
		wave = n;
		lastWaveAt = now;
		if (n < EventRules.WAVES) {
			nextWaveAt = opened + EventRules.waveAt(n + 1);
		}
		int players = 0;
		ServerPlayer nearest = null;
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && player.position().distanceTo(centre) <= 48) {
				players++;
				if (nearest == null || player.distanceToSqr(centre) < nearest.distanceToSqr(centre)) {
					nearest = player;
				}
			}
		}
		int size = EventRules.waveSize(n, Math.max(1, players));
		int adepts = EventRules.adepts(n, Math.max(1, players));
		waveSize = size + (n == EventRules.WAVES ? 1 : 0);
		List<EntityType<? extends Mob>> types = WAVE_TYPES.get(Math.min(WAVE_TYPES.size(), n) - 1);
		WorldEvents.farSound(level, centre, EventSounds.RIFT_WAVE, 64, 1.0F);
		Light.ring(level, centre, facing, TEAR, 0.4, 3.0, 0.12, 10);
		Light.ring(level, centre, facing.scale(-1), TEAR, 0.4, 3.0, 0.12, 10);
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(centre) <= 64) {
				player.sendOverlayMessage(Component.translatable(n == EventRules.WAVES ? "message.wildercord.rift_last_wave" : "message.wildercord.rift_wave",
					n, EventRules.WAVES).withColor(TEAR));
			}
		}
		ServerPlayer target = nearest;
		pending += size + (n == EventRules.WAVES ? 1 : 0);
		for (int i = 0; i < size; i++) {
			boolean adept = i < adepts;
			EntityType<? extends Mob> type = types.get(i % types.size());
			Scheduler.later(1 + i * 6, () -> emerge(type, adept, target, null));
		}
		if (n == EventRules.WAVES) {
			Scheduler.later(12 + size * 6, () -> emerge(EntityTypes.VINDICATOR, true, target, List.of(Runes.BOLT, Runes.BLACKFLAME, Runes.SPLIT_MOD)));
		}
	}

	/** One monster steps out of the tear, onto one side or the other; {@code riftcaller} is the Riftcaller's spell. */
	private void emerge(EntityType<? extends Mob> type, boolean adept, ServerPlayer target, List<RuneDef> riftcallerSpell) {
		pending = Math.max(0, pending - 1);
		if (closed) {
			return;
		}
		RandomSource random = level.getRandom();
		double s = random.nextBoolean() ? 1 : -1;
		Vec3 out = facing.scale(s * (1.5 + random.nextDouble() * 1.5)).add(side.scale((random.nextDouble() - 0.5) * 2.5));
		Vec3 at = WorldEvents.standingSpot(level, base.add(out));
		if (at == null) {
			at = WorldEvents.standingSpot(level, base.add(facing.scale(s * 1.5)));
		}
		if (at == null) {
			at = base;
		}
		Mob mob = WorldEvents.spawnRunebound(level, type, at, adept, target == null || target.isRemoved() ? null : target, riftcallerSpell);
		if (mob == null) {
			return;
		}
		mobs.add(mob.getUUID());
		if (riftcallerSpell != null) {
			riftcaller = mob.getUUID();
			mob.addTag(RIFTCALLER_TAG);
			AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
			if (health != null) {
				health.addOrReplacePermanentModifier(new AttributeModifier(RIFTCALLER_HEALTH, 2.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
				mob.setHealth(mob.getMaxHealth());
			}
			AttributeInstance knockback = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
			if (knockback != null) {
				knockback.setBaseValue(0.6);
			}
			Sigils.circle(level, centre, facing, TEAR, RIM, 2.6F, 30);
			ScreenFx.shake(level, centre, 0.4F, 24);
			for (ServerPlayer player : bar.getPlayers()) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.riftcaller").withColor(0xFF7CE0));
			}
		}
		Vec3 mid = at.add(0, mob.getBbHeight() * 0.5, 0);
		Sigils.flash(level, mid, RIM, 1.6F);
		Light.ray(level, centre, mid, TEAR, 0.08, 6);
		Fx.send(level, ParticleTypes.REVERSE_PORTAL, mid.x, mid.y, mid.z, 16, 0.3, 0.5, 0.3, 0.05);
		Fx.sound(level, mid, net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 0.7F, 0.6F);
	}

	// ------------------------------------------------------------------ sealing it with spells

	/** Whether a spell's hit struck the tear: it hit the stand in it, or landed within reach of its middle. */
	boolean struckBy(Cast.Hit hit) {
		if (closed) {
			return false;
		}
		if (anchor != null && hit.entities().contains(anchor)) {
			return true;
		}
		Vec3 p = hit.point();
		double dx = p.x - centre.x;
		double dz = p.z - centre.z;
		return dx * dx + dz * dz <= 3.0 * 3.0 && p.y >= base.y - 1 && p.y <= base.y + HEIGHT + 1;
	}

	/** A player's spell of {@code element} strikes the tear: three different elements seal it. */
	public void strike(ServerPlayer player, String element) {
		if (closed || !elements.add(element)) {
			return;
		}
		fighters.add(player.getUUID());
		int color = RuneColors.element(element);
		Sigils.flash(level, centre, color, 2.4F);
		Light.ring(level, centre, facing, color, 0.3, 2.4, 0.1, 10);
		Sigils.layer(level, centre.add(facing.scale(0.4)), facing, SigilOption.STAR, color, 1.2F + 0.4F * elements.size(), 40, 0.1F);
		Fx.sound(level, centre, EventSounds.STORM_ARC, 1.0F, 0.7F + 0.15F * elements.size());
		for (ServerPlayer p : bar.getPlayers()) {
			p.sendOverlayMessage(Component.translatable("message.wildercord.rift_struck",
				Component.translatable("element.wildercord." + element), elements.size(), EventRules.RIFT_SEAL_ELEMENTS).withColor(color));
		}
		if (elements.size() >= EventRules.RIFT_SEAL_ELEMENTS) {
			close(true, player);
		}
	}

	/** The elements that have struck it so far (for the tests). */
	public Set<String> elements() {
		return Set.copyOf(elements);
	}

	// ------------------------------------------------------------------ closing

	/**
	 * It closes: {@code won} (every wave beaten, or sealed by spells: {@code sealer} is who did it)
	 * gives its rewards; otherwise it simply closes. Any monster still out goes back in.
	 */
	void close(boolean won, ServerPlayer sealer) {
		if (closed) {
			return;
		}
		closed = true;
		for (UUID id : mobs) {
			Entity e = level.getEntity(id);
			if (e != null && e.isAlive()) {
				WorldEvents.vanish(level, e);
			}
			WorldEvents.forget(id);
		}
		if (anchor != null) {
			WorldEvents.forget(anchor.getUUID());
			anchor.discard();
		}
		WorldEvents.farSound(level, centre, EventSounds.RIFT_CLOSE, 64, 1.0F);
		ElementFx.implode(level, centre, 3.0, 14);
		Sigils.flash(level, centre, RIM, won ? 5.0F : 2.5F);
		Light.ring(level, centre, facing, RIM, 3.5, 0.2, 0.12, 12);
		ScreenFx.shake(level, centre, 0.4F, 24);
		String message = won ? (sealer != null ? "message.wildercord.rift_sealed" : "message.wildercord.rift_won") : "message.wildercord.rift_fades";
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceTo(centre) <= 64) {
				player.sendOverlayMessage(Component.translatable(message).withColor(won ? RIM : 0xB8A8D8));
			}
		}
		if (won) {
			reward();
		}
		bar.removeAllPlayers();
	}

	/** Runes, Blank Runes, a Mana Crystal and experience where the tear was, and Riftwarden for everyone who stood against it. */
	private void reward() {
		RandomSource random = level.getRandom();
		Vec3 at = base.add(0, 0.8, 0);
		for (int i = 0; i < EventRules.RIFT_RUNES; i++) {
			RuneDef rune = EventRules.rewardRune("rift", EventRules.riftRuneTier(random.nextDouble()), random.nextDouble());
			drop(at, RuneItem.stack(rune));
		}
		drop(at, new ItemStack(WildercordItems.BLANK_RUNE, EventRules.riftBlanks(random.nextDouble())));
		drop(at, new ItemStack(WildercordItems.MANA_CRYSTAL));
		ExperienceOrb.award(level, at, EventRules.RIFT_XP);
		for (ServerPlayer player : level.players()) {
			if (fighters.contains(player.getUUID()) && player.position().distanceTo(centre) <= 64) {
				Grimoire.feat(player, Feats.RIFTWARDEN);
			}
		}
	}

	private void drop(Vec3 at, ItemStack stack) {
		ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
		item.setDeltaMovement(level.getRandom().nextGaussian() * 0.08, 0.3, level.getRandom().nextGaussian() * 0.08);
		item.setGlowingTag(true);
		level.addFreshEntity(item);
	}

	/** The server is stopping: let go of the boss bar (its monsters are removed as they load next time). */
	void forget() {
		bar.removeAllPlayers();
		closed = true;
	}
}
