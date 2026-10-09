package dev.wildercord.cast;

import dev.wildercord.spell.HearthLinkRules;
import dev.wildercord.spell.SpellPlan;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The hearth pack's links at cast time (see {@link HearthLinkRules}): ten conditions, which let the rest of the spell fire
 * now or give its mana back, and seven watchers, which wait for something the caster does (mines a block, picks a ripe
 * crop, reels in a catch, breaks into a sprint, splashes into water, mounts up, wakes from a bed) and then fire the rest
 * from where it happened. A watcher sets its seal at the caster's feet and rings when it goes off, as On Land does; one
 * whose time runs out simply lapses, and one caster keeps {@link HearthLinkRules#MAX_WATCHES} waiting at most.
 */
public final class HearthLinks {
	private HearthLinks() {}

	/** One watcher waiting on its caster. */
	private static final class Watch {
		final String kind;
		final LivingEntity caster;
		final Cast child;
		final SpellPlan.Link link;
		final long deadline;
		int baseline;
		boolean was;
		Entity lastVehicle;
		Vec3 lastHook;

		Watch(String kind, LivingEntity caster, Cast child, SpellPlan.Link link, long deadline) {
			this.kind = kind;
			this.caster = caster;
			this.child = child;
			this.link = link;
			this.deadline = deadline;
		}
	}

	private static final List<Watch> WATCHES = new ArrayList<>();
	private static boolean registered;

	/** Whether {@code id} is one of the hearth pack's links. */
	public static boolean handles(String id) {
		return HearthLinkRules.ownsLink(id);
	}

	/** Watchers waiting right now, for the tests. */
	public static int waiting() {
		return WATCHES.size();
	}

	/**
	 * Arms a watcher and returns true, or returns false for a condition (which {@link #met} then decides).
	 */
	static boolean watch(Cast cast, SpellPlan.Link link) {
		String id = link.link.id();
		if (!HearthLinkRules.isWatcher(id)) {
			return false;
		}
		register();
		String kind = id.substring(id.indexOf(':') + 1);
		LivingEntity caster = cast.caster;
		Watch watch = new Watch(kind, caster, cast.child(), link, cast.level.getGameTime() + HearthLinkRules.watchTicks(kind));
		watch.was = state(watch);
		watch.lastVehicle = caster.getVehicle();
		if (caster instanceof ServerPlayer player) {
			watch.baseline = player.getStats().getValue(Stats.CUSTOM.get(Stats.FISH_CAUGHT));
		}
		// One caster keeps a handful waiting at most: the oldest gives way.
		long mine = WATCHES.stream().filter(w -> w.caster == caster).count();
		if (mine >= HearthLinkRules.MAX_WATCHES) {
			for (Iterator<Watch> it = WATCHES.iterator(); it.hasNext(); ) {
				if (it.next().caster == caster) {
					it.remove();
					break;
				}
			}
		}
		WATCHES.add(watch);
		dev.wildercord.cast.feel.Tells.armed(cast);
		return true;
	}

	/** Whether a hearth condition holds for the cast's caster right now. */
	static boolean met(Cast cast, String id) {
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		BlockPos feet = caster.blockPosition();
		BlockPos head = BlockPos.containing(caster.getEyePosition());
		return switch (id.substring(id.indexOf(':') + 1)) {
			case "if_night" -> !level.isBrightOutside();
			case "if_day" -> level.isBrightOutside();
			case "if_raining" -> falling(level, head);
			case "if_underground" -> !level.canSeeSky(head);
			case "if_alone" -> level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(HearthLinkRules.ALONE_RADIUS),
				e -> e != caster && e.isAlive() && (e instanceof Player p && !p.isSpectator() || e instanceof net.minecraft.world.entity.monster.Enemy)).isEmpty();
			case "if_near_ally" -> !level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(HearthLinkRules.ALLY_RADIUS),
				e -> e != caster && e.isAlive() && !(e instanceof Player p && p.isSpectator()) && Targets.isAlly(caster, e)
					&& (e instanceof Player || e instanceof net.minecraft.world.entity.OwnableEntity)).isEmpty();
			case "if_unhurt" -> caster.getHealth() >= caster.getMaxHealth() - 0.01F;
			case "if_holding_tool" -> tool(caster.getMainHandItem());
			case "if_brimming" -> !(caster instanceof Player player)
				|| HearthLinkRules.brimming(dev.wildercord.player.Spellbooks.mana(player), dev.wildercord.player.Mana.max(player));
			case "if_in_fields" -> fields(level, feet);
			default -> false;
		};
	}

	/** A held condition's own touch on top of the gate: a few motes of what it saw. */
	static void passed(Cast cast, String id) {
		Vec3 at = cast.caster.position().add(0, 0.2, 0);
		var particle = switch (id.substring(id.indexOf(':') + 1)) {
			case "if_night", "if_underground" -> ParticleTypes.END_ROD;
			case "if_raining" -> ParticleTypes.SPLASH;
			case "if_in_fields", "if_near_ally" -> ParticleTypes.HAPPY_VILLAGER;
			default -> ParticleTypes.WAX_ON;
		};
		Vfx.emit(cast.level, particle, at, 6, 0.6, 0.02);
	}

	static boolean falling(ServerLevel level, BlockPos head) {
		if (!level.isRaining() || !level.canSeeSky(head)) {
			return false;
		}
		Biome biome = level.getBiome(head).value();
		return biome.hasPrecipitation() && biome.getPrecipitationAt(head, level.getSeaLevel()) != Biome.Precipitation.NONE;
	}

	static boolean tool(ItemStack stack) {
		return stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.AXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES) || stack.is(Items.SHEARS);
	}

	static boolean fields(ServerLevel level, BlockPos feet) {
		int r = HearthLinkRules.FIELDS_RADIUS;
		for (BlockPos p : BlockPos.betweenClosed(feet.offset(-r, -2, -r), feet.offset(r, 1, r))) {
			if (level.getBlockState(p).is(Blocks.FARMLAND)) {
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ watching

	private static void register() {
		if (registered) {
			return;
		}
		registered = true;
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (WATCHES.isEmpty() || !(level instanceof ServerLevel)) {
				return;
			}
			boolean ripe = state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
			fire(w -> w.caster == player && (w.kind.equals("on_mine") || w.kind.equals("on_harvest") && ripe),
				w -> new Cast.Trigger(Vec3.atCenterOf(pos), player.getLookAngle(), null, pos.immutable(), Direction.UP));
		});
		ServerTickEvents.END_SERVER_TICK.register(HearthLinks::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> WATCHES.clear());
	}

	private static void tick(MinecraftServer server) {
		if (WATCHES.isEmpty()) {
			return;
		}
		List<Watch> due = new ArrayList<>();
		List<Cast.Trigger> at = new ArrayList<>();
		for (Iterator<Watch> it = WATCHES.iterator(); it.hasNext(); ) {
			Watch w = it.next();
			LivingEntity caster = w.caster;
			if (!w.child.alive() || caster.level().getGameTime() > w.deadline) {
				it.remove();
				continue;
			}
			if (caster instanceof Player player && player.fishing != null) {
				w.lastHook = player.fishing.position();
			}
			Cast.Trigger trigger = switch (w.kind) {
				case "on_catch" -> caster instanceof ServerPlayer player
					&& player.getStats().getValue(Stats.CUSTOM.get(Stats.FISH_CAUGHT)) > w.baseline
					? new Cast.Trigger(w.lastHook != null ? w.lastHook : caster.position(), caster.getLookAngle(), null, null, null) : null;
				case "on_sprint", "on_splash", "on_wake" -> rose(w) ? new Cast.Trigger(where(w), caster.getLookAngle(), caster, null, null) : null;
				case "on_mount" -> mounted(w);
				default -> null;
			};
			if (trigger != null) {
				it.remove();
				due.add(w);
				at.add(trigger);
			}
		}
		for (int i = 0; i < due.size(); i++) {
			spring(due.get(i), at.get(i));
		}
	}

	/** Sprinting, in water, or asleep: what each watcher waits to see begin (or, for On Wake, end). */
	private static boolean state(Watch w) {
		return switch (w.kind) {
			case "on_sprint" -> w.caster.isSprinting();
			case "on_splash" -> w.caster.isInWater();
			case "on_wake" -> w.caster.isSleeping();
			default -> false;
		};
	}

	/** Whether the watched state just began (On Wake: just ended), and remembers it for the next tick. */
	private static boolean rose(Watch w) {
		boolean now = state(w);
		boolean fired = w.kind.equals("on_wake") ? w.was && !now : !w.was && now;
		w.was = now;
		return fired;
	}

	private static Vec3 where(Watch w) {
		return w.kind.equals("on_splash") ? w.caster.position() : w.caster.position().add(0, 1, 0);
	}

	private static Cast.Trigger mounted(Watch w) {
		Entity vehicle = w.caster.getVehicle();
		Entity before = w.lastVehicle;
		w.lastVehicle = vehicle;
		if (vehicle == null || vehicle == before) {
			return null;
		}
		return new Cast.Trigger(vehicle.getBoundingBox().getCenter(), w.caster.getLookAngle(), vehicle, null, null);
	}

	private static void fire(java.util.function.Predicate<Watch> matches, java.util.function.Function<Watch, Cast.Trigger> at) {
		List<Watch> due = new ArrayList<>();
		WATCHES.removeIf(w -> {
			if (matches.test(w)) {
				due.add(w);
				return true;
			}
			return false;
		});
		for (Watch w : due) {
			spring(w, at.apply(w));
		}
	}

	private static void spring(Watch w, Cast.Trigger trigger) {
		if (!w.child.alive()) {
			return;
		}
		ServerLevel level = w.child.level;
		Vfx.emit(level, ParticleTypes.WAX_ON, trigger.pos(), 10, 0.4, 0.05);
		dev.wildercord.cast.feel.Tells.sprung(w.child, trigger.pos());
		CastEngine.runSegment(w.child, w.link.next, trigger);
	}

	/** Whether {@code state} is an ore of any kind (vanilla's tag and the common one). */
	static boolean ore(BlockState state) {
		return state.is(BlockTags.ORES) || state.is(net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags.ORES);
	}
}
