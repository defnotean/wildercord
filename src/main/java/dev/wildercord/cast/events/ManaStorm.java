package dev.wildercord.cast.events;

import dev.wildercord.Wildercord;
import dev.wildercord.cast.Cast;
import dev.wildercord.cast.CastEngine;
import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Fx;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.LeyWalker;
import dev.wildercord.cast.Light;
import dev.wildercord.cast.Scheduler;
import dev.wildercord.cast.Sigils;
import dev.wildercord.content.SigilOption;
import dev.wildercord.player.Heart;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.world.LeyLines;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A mana storm: for three to five minutes the world's mana boils up over a stretch of ley line.
 * Everyone under it regenerates mana twice as fast and casts for a quarter less, the sky takes a
 * faint violet cast, and violet arcs jump between points of the line; but every spell cast under
 * it has a small chance to surge (see {@link EventRules.Surge}).
 *
 * <p>Who is under a storm travels to their client as {@link #STORM_UNTIL} (renewed every second,
 * like a Wellstone's), which {@code Mana.of} and {@code Heart.manaCost} read on both sides and the
 * client's sky tint reads too. Nothing is saved: a restart simply ends the storm.</p>
 */
public final class ManaStorm {
	/** Game time until which a player is under a mana storm. Synced to that player only; not saved. */
	public static final AttachmentType<Long> STORM_UNTIL = AttachmentRegistry.create(
		Wildercord.id("storm_until"),
		builder -> builder.syncWith(ByteBufCodecs.VAR_LONG, AttachmentSyncPredicate.targetOnly())
	);

	/** The storm's violet, and its bright core. */
	public static final int VIOLET = 0xA070FF;
	public static final int CORE = 0xE8DCFF;

	/** Harmful effects a surge may send along with a spell, one per element. */
	private static final List<RuneDef> STRAY = List.of(Runes.FIRE, Runes.FROST, Runes.SHOCK, Runes.WINDCUT, Runes.PELT, Runes.HEX, Runes.HARM);

	final ServerLevel level;
	final Vec3 centre;
	final long start;
	long end;
	/** Players under it at the last check. */
	private final Set<UUID> inside = new HashSet<>();
	/** Casts under it per player, toward Stormcaller. */
	private final Map<UUID, Integer> casts = new HashMap<>();
	private int turn;

	ManaStorm(ServerLevel level, Vec3 centre, long start, int ticks) {
		this.level = level;
		this.centre = centre;
		this.start = start;
		this.end = start + ticks;
	}

	// ------------------------------------------------------------------ who is under one (both sides)

	/** Whether this player is under a mana storm (works on the client too, for the HUD and the sky). */
	public static boolean inside(Player player) {
		return player.getAttachedOrElse(STORM_UNTIL, 0L) > player.level().getGameTime();
	}

	/** What spells cost this player: 25% less under a storm. */
	public static double costFactor(Player player) {
		return inside(player) ? EventRules.STORM_COST : 1.0;
	}

	/** Extra mana regeneration under a storm (+100%), added to the other multipliers in {@code Mana.of}. */
	public static float regenBonus(Player player) {
		return inside(player) ? EventRules.STORM_REGEN_BONUS : 0.0F;
	}

	public boolean covers(Vec3 at) {
		double dx = at.x - centre.x;
		double dz = at.z - centre.z;
		return dx * dx + dz * dz <= EventRules.STORM_RADIUS * EventRules.STORM_RADIUS;
	}

	/** Ends it now (next tick): the command and the tests. */
	public void stop() {
		end = start;
	}

	public long ticksLeft(long now) {
		return Math.max(0, end - now);
	}

	// ------------------------------------------------------------------ finding a ley line

	/**
	 * The strongest point of a ley line within {@code search} blocks of {@code around}, at ground
	 * level, or null if no line runs that close (or it's outside the Overworld). Loaded ground only.
	 */
	public static Vec3 leyHeart(ServerLevel level, BlockPos around, int search) {
		if (level.dimension() != Level.OVERWORLD) {
			return null;
		}
		long seed = LeyWalker.seed(level);
		double best = LeyWalker.ON_LINE;
		Vec3 found = null;
		for (int dx = -search; dx <= search; dx += 8) {
			for (int dz = -search; dz <= search; dz += 8) {
				int x = around.getX() + dx;
				int z = around.getZ() + dz;
				double s = LeyLines.strength(seed, x + 0.5, z + 0.5);
				if (s > best && level.isLoaded(new BlockPos(x, around.getY(), z))) {
					best = s;
					found = new Vec3(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5);
				}
			}
		}
		return found;
	}

	// ------------------------------------------------------------------ running

	void begin() {
		// A great ring rolls out over the line from the storm's heart.
		Light.groundRing(level, centre.add(0, 0.2, 0), VIOLET, 1.0, 24.0, 0.5, 40);
		Sigils.send(level, SigilOption.flat(SigilOption.STAR, VIOLET, 6.0F, 80, 0.02F), centre.add(0, 0.1, 0));
		tick(start, true);
	}

	/** Every server tick; false once it has blown over. */
	boolean tick(long now) {
		if (now >= end) {
			finish();
			return false;
		}
		tick(now, false);
		return true;
	}

	private void tick(long now, boolean first) {
		if (first || now % 20 == 0) {
			Set<UUID> still = new HashSet<>();
			for (ServerPlayer player : level.players()) {
				if (player.isSpectator() || !covers(player.position())) {
					continue;
				}
				still.add(player.getUUID());
				player.setAttached(STORM_UNTIL, now + 50);
				if (inside.add(player.getUUID())) {
					player.sendOverlayMessage(Component.translatable("message.wildercord.storm_start").withColor(VIOLET));
					WorldEvents.soundFor(player, EventSounds.STORM_START, 1.0F, 1.0F);
				}
			}
			for (UUID id : inside) {
				if (!still.contains(id) && level.getServer().getPlayerList().getPlayer(id) instanceof ServerPlayer gone) {
					gone.removeAttached(STORM_UNTIL);
					gone.sendOverlayMessage(Component.translatable("message.wildercord.storm_left").withColor(0xB8A8D8));
				}
			}
			inside.retainAll(still);
		}
		if (now % 7 == 0 && !inside.isEmpty()) {
			arcNear(pickInside());
		}
		if (now % 13 == 0 && !inside.isEmpty()) {
			skyArc(pickInside());
		}
	}

	private ServerPlayer pickInside() {
		List<UUID> ids = new ArrayList<>(inside);
		UUID id = ids.get(Math.floorMod(turn++, ids.size()));
		return level.getServer().getPlayerList().getPlayer(id);
	}

	/** A faint violet arc jumping between two points of the ley line near a player, with motes lifting off it. */
	private void arcNear(ServerPlayer player) {
		if (player == null) {
			return;
		}
		RandomSource random = level.getRandom();
		long seed = LeyWalker.seed(level);
		Vec3 first = null;
		for (int i = 0; i < 12; i++) {
			double x = player.getX() + (random.nextDouble() - 0.5) * 40;
			double z = player.getZ() + (random.nextDouble() - 0.5) * 40;
			if (LeyLines.strength(seed, x, z) < 0.5 || !level.isLoaded(BlockPos.containing(x, player.getY(), z))) {
				continue;
			}
			Vec3 p = new Vec3(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z)) + 0.3
				+ random.nextDouble() * 1.4, z);
			if (Math.abs(p.y - player.getY()) > 16) {
				continue;
			}
			if (first == null) {
				first = p;
			} else if (first.distanceTo(p) >= 3 && first.distanceTo(p) <= 12) {
				ElementFx.bolt(level, first, p, 0.05, 2, 2, VIOLET, 0);
				Fx.send(level, ParticleTypes.END_ROD, p.x, p.y, p.z, 2, 0.2, 0.3, 0.2, 0.02);
				if (random.nextInt(3) == 0) {
					Fx.sound(level, p, EventSounds.STORM_ARC, 0.35F, 0.9F + random.nextFloat() * 0.2F);
				}
				return;
			}
		}
		// Only one point found: the line breathes a mote of light instead.
		if (first != null) {
			ElementFx.orb(level, first, CORE, 0.12, 4);
			Fx.send(level, ParticleTypes.END_ROD, first.x, first.y, first.z, 3, 0.2, 0.4, 0.2, 0.03);
		}
	}

	/** Now and then, high over a player, violet lightning crawls across the sky. */
	private void skyArc(ServerPlayer player) {
		if (player == null || !level.canSeeSky(player.blockPosition()) || level.getRandom().nextInt(3) != 0) {
			return;
		}
		RandomSource random = level.getRandom();
		double a = random.nextDouble() * Math.PI * 2;
		Vec3 mid = player.position().add(Math.cos(a) * 14, 22 + random.nextDouble() * 8, Math.sin(a) * 14);
		Vec3 along = ElementFx.flatDir(random.nextDouble() * Math.PI * 2).scale(5 + random.nextDouble() * 4);
		Light.ray(level, mid.subtract(along), mid.add(along), VIOLET, 0.06, 6);
		ElementFx.bolt(level, mid.subtract(along), mid.add(along), 0.07, 3, 2, VIOLET, 0);
	}

	/** It blows over: everyone under it is told, and their storm bonuses stop. */
	void finish() {
		for (UUID id : inside) {
			if (level.getServer().getPlayerList().getPlayer(id) instanceof ServerPlayer player) {
				player.removeAttached(STORM_UNTIL);
				player.sendOverlayMessage(Component.translatable("message.wildercord.storm_end").withColor(0xB8A8D8));
				WorldEvents.soundFor(player, EventSounds.STORM_END, 0.9F, 1.0F);
			}
		}
		inside.clear();
	}

	// ------------------------------------------------------------------ surges

	/** Rolls whether a player's cast surges: never outside a storm. Called by {@code SpellCaster} before it casts. */
	public static EventRules.Surge surge(ServerPlayer player) {
		if (!inside(player)) {
			return EventRules.Surge.NONE;
		}
		RandomSource random = player.getRandom();
		return EventRules.surge(random.nextDouble(), random.nextDouble());
	}

	/**
	 * After a cast under a storm: counts it toward Stormcaller and plays out the surge, if any.
	 * {@code again} casts the same spell again, for an echo.
	 */
	public static void afterCast(ServerPlayer player, EventRules.Surge surge, List<RuneDef> runes, Runnable again) {
		if (!inside(player)) {
			return;
		}
		ManaStorm storm = WorldEvents.stormAt(player.level(), player.position());
		if (storm != null) {
			int n = storm.casts.merge(player.getUUID(), 1, Integer::sum);
			if (n >= EventRules.STORMCALLER_CASTS) {
				Grimoire.feat(player, Feats.STORMCALLER);
			}
		}
		if (surge == EventRules.Surge.NONE) {
			return;
		}
		ServerLevel level = player.level();
		Vec3 at = player.position().add(0, 1.0, 0);
		Fx.sound(level, at, EventSounds.SURGE, 0.9F, 0.9F + player.getRandom().nextFloat() * 0.2F);
		ElementFx.ring(level, at, new Vec3(0, 1, 0), VIOLET, 0.3, 2.2, 0.08, 10);
		Sigils.flash(level, at.add(player.getLookAngle().scale(1.5)), CORE, 1.6F);
		switch (surge) {
			case BIGGER -> player.sendOverlayMessage(Component.translatable("message.wildercord.surge.bigger").withColor(VIOLET));
			case ECHO -> {
				player.sendOverlayMessage(Component.translatable("message.wildercord.surge.echo").withColor(VIOLET));
				Scheduler.later(10, () -> {
					if (!player.isRemoved() && player.isAlive()) {
						ElementFx.ring(level, player.position().add(0, 1, 0), new Vec3(0, 1, 0), CORE, 0.2, 1.6, 0.06, 8);
						again.run();
					}
				});
			}
			case ELEMENT -> stray(player, runes);
			case BACKFIRE -> backfire(player);
			default -> { }
		}
	}

	/** A stray element rides along: the spell's shape (a bolt for Self or Touch) carrying a random element. */
	private static void stray(ServerPlayer player, List<RuneDef> runes) {
		RuneDef element = STRAY.get(player.getRandom().nextInt(STRAY.size()));
		RuneDef shape = Runes.BOLT;
		for (RuneDef rune : runes) {
			if (rune.family() == RuneFamily.SHAPE) {
				shape = rune.is(Runes.SELF.id()) || rune.is(Runes.TOUCH.id()) ? Runes.BOLT : rune;
				break;
			}
		}
		List<RuneDef> spell = List.of(shape, element);
		SpellCompiler.Compiled compiled = SpellCompiler.compile(spell);
		player.sendOverlayMessage(Component.translatable("message.wildercord.surge.element",
			Component.translatable("element.wildercord." + element.element())).withColor(RuneColors.of(element)));
		if (compiled.isEmpty()) {
			return;
		}
		Cast cast = new Cast(player, 1, Heart.bonuses(player), false, null, new Cast.Info(compiled.root(), spell.size(), "", spell));
		CastEngine.cast(cast, compiled.root());
	}

	/** The storm's mana kicks back: a jolt and a shove, never enough to kill, and a little mana spilled. */
	private static void backfire(ServerPlayer player) {
		ServerLevel level = player.level();
		player.sendOverlayMessage(Component.translatable("message.wildercord.surge.backfire").withColor(0xFF8C8C));
		float hurt = Math.min(3.0F, player.getHealth() - 2.0F);
		if (hurt > 0 && !player.isCreative()) {
			player.hurtServer(level, level.damageSources().magic(), hurt);
		}
		Vec3 back = player.getLookAngle().scale(-0.6).add(0, 0.35, 0);
		player.push(back.x, back.y, back.z);
		player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(player));
		Spellbooks.setMana(player, Math.max(0, Spellbooks.mana(player) - 10));
		Vec3 at = player.position().add(0, 1, 0);
		ElementFx.bolt(level, at, at.add(player.getLookAngle().scale(2.0)), 0.06, 2, 2, VIOLET, 0);
		Fx.send(level, ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 12, 0.4, 0.5, 0.4, 0.15);
	}
}
