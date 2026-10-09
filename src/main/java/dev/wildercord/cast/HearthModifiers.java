package dev.wildercord.cast;

import dev.wildercord.cast.feel.Feels;
import dev.wildercord.spell.HearthLinkRules;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.SpellPlan;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The hearth pack's modifiers at work (see {@link HearthLinkRules}). {@link #around} wraps one effect landing: before it, it
 * picks who the effect may reach and how strongly; while it lands, it answers {@link Targets#canHarm} (who is passed
 * over), {@code Effects.hurtCapped} (each creature's share) and every block-break question (Steady, Level Ground); after
 * it, it works out what changed (blocks broken, creatures killed, items dropped) and applies the rest: drops smelted,
 * silk-touched or fortuned, carried to the pack, veins and trees followed, crops replanted, loot fetched, anger soothed.
 *
 * <p>Only what happens in the moment the effect lands is changed: a part it schedules for later (a falling star, a
 * lingering field) lands as written.
 */
public final class HearthModifiers {
	private HearthModifiers() {}

	/** The effect landing right now, while it has hearth modifiers. Nested landings stack. */
	private static final class Context {
		final Cast cast;
		final LivingEntity caster;
		final Set<String> mods;
		final Context outer;
		Predicate<Entity> keeps = e -> true;
		boolean steady;
		Integer floorY;
		int pooled;
		final Map<Entity, Integer> order = new LinkedHashMap<>();
		final Set<LivingEntity> reached = new LinkedHashSet<>();

		Context(Cast cast, Set<String> mods, Context outer) {
			this.cast = cast;
			this.caster = cast.caster;
			this.mods = mods;
			this.outer = outer;
		}

		boolean has(String path) {
			return mods.contains(path);
		}

		void keep(Predicate<Entity> rule) {
			keeps = keeps.and(rule);
		}
	}

	private static Context active;
	private static boolean registered;
	/** Creatures Cushioned has moved: no fall damage on their next landing, until the game time given. */
	private static final Map<UUID, Long> CUSHIONED = new HashMap<>();

	/** The hearth modifiers on an effect, by path (empty for nearly every effect). */
	static Set<String> of(SpellPlan.EffectNode node) {
		Set<String> out = null;
		for (RuneDef mod : node.mods) {
			if (HearthLinkRules.ownsModifier(mod)) {
				if (out == null) {
					out = new HashSet<>();
				}
				out.add(mod.path());
			}
		}
		return out == null ? Set.of() : out;
	}

	/** Lands one effect through its hearth modifiers; {@code apply} lands it as it would land without them. */
	public static void around(Cast cast, SpellPlan.EffectNode node, Cast.Hit hit, double groupPower, BiConsumer<Cast.Hit, Double> apply) {
		Set<String> mods = of(node);
		if (mods.isEmpty()) {
			apply.accept(hit, groupPower);
			return;
		}
		register();
		Context ctx = new Context(cast, mods, active);
		LivingEntity caster = cast.caster;
		ServerLevel level = cast.level;
		double power = groupPower;
		List<Entity> entities = new ArrayList<>(hit.entities());
		boolean self = hit.self();
		boolean changed = false;

		// Who it may strike (asked through Targets.canHarm while it lands).
		if (ctx.has("gentle")) {
			ctx.keep(e -> !(e instanceof Animal || e instanceof AbstractVillager || e instanceof OwnableEntity o && o.getOwner() != null));
		}
		if (ctx.has("sparing")) {
			ctx.keep(e -> !(e instanceof Player && e != caster) && !(e instanceof OwnableEntity o && o.getOwner() instanceof Player));
		}
		if (ctx.has("culling")) {
			ctx.keep(e -> e instanceof Enemy);
			power *= HearthLinkRules.CULLING_POWER;
		}
		if (ctx.has("hallowed")) {
			ctx.keep(e -> e.is(EntityTypeTags.UNDEAD));
			power *= HearthLinkRules.HALLOWED_POWER;
		}
		if (ctx.has("headhunting")) {
			Predicate<Entity> before = ctx.keeps;
			LivingEntity chosen = entities.stream().filter(e -> e instanceof LivingEntity && e != caster && before.test(e) && Targets.canHarm(caster, e))
				.map(e -> (LivingEntity) e).max(Comparator.comparingDouble(LivingEntity::getHealth)).orElse(null);
			if (chosen != null) {
				ctx.keep(e -> e == chosen);
				power *= HearthLinkRules.HEADHUNT_POWER;
			}
		}
		if (ctx.has("soothing")) {
			power *= HearthLinkRules.SOOTHING_POWER;
		}
		if (ctx.has("pooled")) {
			Predicate<Entity> keeps = ctx.keeps;
			ctx.pooled = (int) entities.stream().filter(e -> e != caster && keeps.test(e) && Targets.canHarm(caster, e)).count();
			if (ctx.pooled == 0) {
				ctx.pooled = level.getEntitiesOfClass(LivingEntity.class, new AABB(BlockPos.containing(hit.point())).inflate(4),
					e -> e != caster && keeps.test(e) && Targets.canHarm(caster, e)).size();
			}
		}
		if (ctx.has("sunlit")) {
			BlockPos at = BlockPos.containing(hit.point()).above();
			power *= HearthLinkRules.sunlit(level.isBrightOutside() && level.canSeeSky(at));
		}
		// Who it may help (helpful effects help whoever the shape reached).
		if (ctx.has("inward")) {
			entities = self || entities.contains(caster) ? new ArrayList<>(List.of(caster)) : new ArrayList<>();
			power *= HearthLinkRules.INWARD_POWER;
			changed = true;
		}
		if (ctx.has("selfless")) {
			entities.remove(caster);
			if (entities.isEmpty() && self) {
				entities.addAll(level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(4), e -> e != caster && Targets.canHelp(caster, e)));
			}
			self = false;
			power *= HearthLinkRules.SELFLESS_POWER;
			changed = true;
		}
		if (ctx.has("triage")) {
			LivingEntity worst = entities.stream().filter(e -> e instanceof LivingEntity && Targets.canHelp(caster, e)).map(e -> (LivingEntity) e)
				.max(Comparator.comparingDouble(e -> e.getMaxHealth() - e.getHealth())).orElse(null);
			entities = worst == null ? new ArrayList<>() : new ArrayList<>(List.of(worst));
			self = self && worst == caster;
			power *= HearthLinkRules.TRIAGE_POWER;
			changed = true;
		}
		Cast.Hit landed = changed ? new Cast.Hit(List.copyOf(entities), hit.point(), hit.dir(), hit.origin(), hit.block(), hit.face(), self, hit.power()) : hit;

		// Blocks: Steady and Level Ground answer the break questions; the rest look at what changed.
		ctx.steady = ctx.has("steady");
		if (ctx.has("level_ground")) {
			ctx.floorY = caster.getBlockY();
		}
		BlockPos center = hit.block() != null ? hit.block() : BlockPos.containing(hit.point());
		boolean blockRules = ctx.has("tidy") || ctx.has("replanting") || ctx.has("kilned") || ctx.has("silken") || ctx.has("windfall")
			|| ctx.has("veinfollow") || ctx.has("timbering");
		Map<BlockPos, BlockState> before = new LinkedHashMap<>();
		if (blockRules) {
			int r = HearthLinkRules.BLOCK_REACH;
			for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
				BlockState state = level.getBlockState(p);
				if (!state.isAir()) {
					before.put(p.immutable(), state);
				}
			}
		}
		AABB itemBox = new AABB(center).inflate(12, 8, 12).expandTowards(0, HearthLinkRules.TIMBER_LOGS + 4, 0);
		Set<UUID> itemsBefore = new HashSet<>();
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, itemBox)) {
			itemsBefore.add(item.getUUID());
		}
		Set<BlockPos> fireBefore = ctx.has("damp") ? fires(level, center) : Set.of();

		active = ctx;
		try {
			apply.accept(landed, power);
			after(ctx, landed, center, before, itemBox, itemsBefore, fireBefore);
		} finally {
			active = ctx.outer;
		}
	}

	// ------------------------------------------------------------------ while it lands

	/** Whether a hearth modifier on the effect landing for {@code caster} passes {@code entity} over (asked by Targets.canHarm). */
	public static boolean passesOver(LivingEntity caster, Entity entity) {
		for (Context c = active; c != null; c = c.outer) {
			if (c.caster == caster && !c.keeps.test(entity)) {
				return true;
			}
		}
		return false;
	}

	/** What a hearth modifier makes of one blow on {@code target} (Tapering, Pooled), and notes the creature reached. */
	public static double damageFactor(Cast cast, LivingEntity target) {
		Context c = active;
		if (c == null || c.caster != cast.caster) {
			return 1.0;
		}
		c.reached.add(target);
		double factor = 1.0;
		if (c.has("tapering")) {
			int index = c.order.computeIfAbsent(target, k -> c.order.size());
			factor *= HearthLinkRules.taper(index);
		}
		if (c.has("pooled")) {
			factor *= HearthLinkRules.pooled(Math.max(1, c.pooled));
		}
		return factor;
	}

	private static void register() {
		if (registered) {
			return;
		}
		registered = true;
		// Steady and Level Ground: the effect landing may not take this block.
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
			for (Context c = active; c != null; c = c.outer) {
				if (c.caster == player && (c.steady || c.floorY != null && pos.getY() < c.floorY)) {
					return false;
				}
			}
			return true;
		});
		// Cushioned: the next landing doesn't hurt.
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (CUSHIONED.isEmpty() || !source.is(DamageTypeTags.IS_FALL)) {
				return true;
			}
			Long until = CUSHIONED.remove(entity.getUUID());
			if (until == null || entity.level().getGameTime() > until) {
				return true;
			}
			if (entity.level() instanceof ServerLevel level) {
				Vfx.emit(level, ParticleTypes.CLOUD, entity.position(), 8, 0.4, 0.02);
				Feels.sound(level, entity.position(), "note_mod", 0.3F, 0.8F);
			}
			return false;
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> CUSHIONED.clear());
	}

	// ------------------------------------------------------------------ after it lands

	private static void after(Context ctx, Cast.Hit hit, BlockPos center, Map<BlockPos, BlockState> before, AABB itemBox, Set<UUID> itemsBefore,
			Set<BlockPos> fireBefore) {
		Cast cast = ctx.cast;
		ServerLevel level = cast.level;
		LivingEntity caster = ctx.caster;
		Vec3 point = hit.point();

		if (!before.isEmpty()) {
			blocks(ctx, before, itemBox, itemsBefore);
		}

		// Creatures it reached: the shape's own, and any the effect struck.
		Set<LivingEntity> reached = new LinkedHashSet<>(ctx.reached);
		for (Entity e : hit.entities()) {
			if (e instanceof LivingEntity living) {
				reached.add(living);
			}
		}
		if (ctx.has("soothing")) {
			for (LivingEntity e : reached) {
				if (e == caster || e.isDeadOrDying()) {
					continue;
				}
				if (e instanceof Mob mob && mob.getTarget() == caster) {
					mob.setTarget(null);
				}
				if (e instanceof NeutralMob neutral) {
					neutral.stopBeingAngry();
				}
				if (e.getLastHurtByMob() == caster) {
					e.setLastHurtByMob(null);
				}
				Vfx.emit(level, ParticleTypes.NOTE, e.getEyePosition().add(0, 0.4, 0), 2, 0.3, 0.0);
			}
		}
		List<LivingEntity> dead = reached.stream().filter(e -> e != caster && (e.isDeadOrDying() || !e.isAlive())).toList();
		if (ctx.has("bountiful")) {
			for (LivingEntity e : dead) {
				int xp = e.getExperienceReward(level, caster);
				if (xp > 0) {
					ExperienceOrb.award(level, e.position(), xp);
					Vfx.emit(level, ParticleTypes.WAX_ON, e.position().add(0, 0.5, 0), 6, 0.4, 0.05);
				}
			}
		}
		if (ctx.has("fetching") && !dead.isEmpty()) {
			for (ItemEntity item : newItems(level, itemBox, itemsBefore)) {
				if (dead.stream().anyMatch(e -> e.position().distanceToSqr(item.position()) < 9)) {
					toFeet(item, caster);
				}
			}
		}
		if (ctx.has("cushioned")) {
			long until = level.getGameTime() + HearthLinkRules.CUSHION_TICKS;
			if (hit.self()) {
				CUSHIONED.put(caster.getUUID(), until);
			}
			for (Entity e : hit.entities()) {
				if (e instanceof LivingEntity && (e == caster || Targets.canHarm(caster, e))) {
					CUSHIONED.put(e.getUUID(), until);
				}
			}
		}

		// Helpful extras for whoever it helped.
		List<LivingEntity> helped = new ArrayList<>();
		for (Entity e : hit.entities()) {
			if (e instanceof LivingEntity living && Targets.canHelp(caster, living)) {
				helped.add(living);
			}
		}
		if (hit.self() && !helped.contains(caster)) {
			helped.add(caster);
		}
		if (ctx.has("mending")) {
			for (LivingEntity e : helped) {
				boolean any = false;
				for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST,
					EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
					ItemStack stack = e.getItemBySlot(slot);
					if (stack.isDamageableItem() && stack.isDamaged()) {
						stack.setDamageValue(HearthLinkRules.mended(stack.getDamageValue()));
						any = true;
					}
				}
				if (any) {
					Vfx.emit(level, ParticleTypes.WAX_OFF, e.position().add(0, 1, 0), 6, 0.4, 0.05);
					Feels.sound(level, e.position(), "note_mod", 0.3F, 1.5F);
				}
			}
		}
		if (ctx.has("nourishing")) {
			for (LivingEntity e : helped) {
				if (e instanceof Player player) {
					player.getFoodData().eat(HearthLinkRules.NOURISH_FOOD, 0.6F);
					Vfx.emit(level, ParticleTypes.HAPPY_VILLAGER, player.position().add(0, 1, 0), 4, 0.3, 0.0);
				}
			}
		}
		if (ctx.has("purifying")) {
			for (LivingEntity e : helped) {
				MobEffectInstance worst = e.getActiveEffects().stream()
					.filter(i -> i.getEffect().value().getCategory() == MobEffectCategory.HARMFUL).findFirst().orElse(null);
				if (worst != null) {
					e.removeEffect(worst.getEffect());
					Vfx.emit(level, ParticleTypes.END_ROD, e.position().add(0, 1, 0), 6, 0.3, 0.03);
				}
			}
		}
		if (ctx.has("matchmaking")) {
			for (Entity e : hit.entities()) {
				if (e instanceof Animal animal && !animal.isBaby() && animal.canFallInLove()) {
					animal.setInLove(caster instanceof Player player ? player : null);
				}
			}
		}

		// The ground where it lands.
		if (ctx.has("fleecing")) {
			fleece(ctx, hit, itemBox);
		}
		if (ctx.has("furrowing")) {
			furrow(ctx, center);
		}
		if (ctx.has("sowing")) {
			sow(ctx, center);
		}
		if (ctx.has("fertile")) {
			fertilise(ctx, center);
		}
		if (ctx.has("torchset")) {
			lamp(ctx, hit);
		}
		if (ctx.has("ore_sensing")) {
			senseOres(level, center);
		}
		if (ctx.has("damp")) {
			douse(level, center, fireBefore);
			for (int delay : new int[] {1, 5, 20}) {
				Scheduler.later(delay, () -> douse(level, center, fireBefore));
			}
		}
		if (ctx.has("magnetic")) {
			pull(level, caster, point);
			Scheduler.later(5, () -> {
				if (caster.isAlive() && caster.level() == level) {
					pull(level, caster, point);
				}
			});
		}
	}

	/** Broken blocks: veins and trees followed, then their drops replanted, re-rolled, smelted and carried as the modifiers say. */
	private static void blocks(Context ctx, Map<BlockPos, BlockState> before, AABB itemBox, Set<UUID> itemsBefore) {
		Cast cast = ctx.cast;
		ServerLevel level = cast.level;
		LivingEntity caster = ctx.caster;
		Map<BlockPos, BlockState> broken = new LinkedHashMap<>();
		before.forEach((pos, old) -> {
			BlockState now = level.getBlockState(pos);
			if (now.isAir() || !now.getFluidState().isEmpty() && old.getFluidState().isEmpty() && now.getBlock() != old.getBlock()) {
				broken.put(pos, old);
			}
		});
		if (broken.isEmpty()) {
			return;
		}
		if (ctx.has("veinfollow")) {
			for (Map.Entry<BlockPos, BlockState> e : List.copyOf(broken.entrySet())) {
				if (HearthLinks.ore(e.getValue())) {
					follow(ctx, e.getKey(), e.getValue(), HearthLinkRules.VEIN_BLOCKS, false, broken);
				}
			}
		}
		if (ctx.has("timbering")) {
			for (Map.Entry<BlockPos, BlockState> e : List.copyOf(broken.entrySet())) {
				if (e.getValue().is(BlockTags.LOGS)) {
					follow(ctx, e.getKey(), e.getValue(), HearthLinkRules.TIMBER_LOGS, true, broken);
				}
			}
		}
		boolean reroll = ctx.has("silken") || ctx.has("windfall");
		boolean handle = reroll || ctx.has("kilned") || ctx.has("tidy") || ctx.has("replanting");
		if (!handle) {
			return;
		}
		// The drops of what broke: whatever new item lies by a broken block.
		List<Vec3> where = new ArrayList<>();
		List<ItemStack> stacks = new ArrayList<>();
		for (ItemEntity item : newItems(level, itemBox, itemsBefore)) {
			if (broken.keySet().stream().anyMatch(p -> Vec3.atCenterOf(p).distanceToSqr(item.position()) < 2.6)) {
				if (!reroll) {
					where.add(item.position());
					stacks.add(item.getItem().copy());
				}
				item.discard();
			}
		}
		if (reroll) {
			ItemStack tool = tool(level, ctx.has("silken"));
			broken.forEach((pos, old) -> {
				for (ItemStack drop : Block.getDrops(old, level, pos, null, caster, tool)) {
					where.add(Vec3.atCenterOf(pos));
					stacks.add(drop);
				}
			});
			Vfx.emit(level, ctx.has("silken") ? ParticleTypes.END_ROD : ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(broken.keySet().iterator().next()),
				6, 0.4, 0.03);
		}
		if (ctx.has("replanting")) {
			broken.forEach((pos, old) -> {
				if (old.getBlock() instanceof CropBlock crop && crop.isMaxAge(old) && level.getBlockState(pos).isAir()
						&& old.canSurvive(level, pos) && take(stacks, old.getBlock().asItem())) {
					level.setBlockAndUpdate(pos, crop.getStateForAge(0));
					Vfx.emit(level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(pos), 4, 0.3, 0.0);
				}
			});
		}
		if (ctx.has("kilned")) {
			boolean any = false;
			for (int i = 0; i < stacks.size(); i++) {
				ItemStack out = smelted(level, stacks.get(i));
				if (out != stacks.get(i)) {
					stacks.set(i, out);
					any = true;
				}
			}
			if (any) {
				Vfx.emit(level, ParticleTypes.FLAME, Vec3.atCenterOf(broken.keySet().iterator().next()), 8, 0.4, 0.02);
				Feels.sound(level, Vec3.atCenterOf(broken.keySet().iterator().next()), "note_mod", 0.3F, 0.9F);
			}
		}
		for (int i = 0; i < stacks.size(); i++) {
			ItemStack stack = stacks.get(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (ctx.has("tidy") && caster instanceof Player player) {
				player.getInventory().add(stack);
				if (stack.isEmpty()) {
					continue;
				}
				drop(level, caster.position(), stack);
			} else {
				drop(level, where.get(i), stack);
			}
		}
		if (ctx.has("tidy")) {
			Feels.sound(level, caster.position(), "note_mod", 0.25F, 1.3F);
		}
	}

	/** Veinfollow and Timbering: breaks up to {@code limit} more blocks of the same kind touching {@code start} (only upward for a tree). */
	private static void follow(Context ctx, BlockPos start, BlockState kind, int limit, boolean upward, Map<BlockPos, BlockState> broken) {
		ServerLevel level = ctx.cast.level;
		ArrayDeque<BlockPos> queue = new ArrayDeque<>(List.of(start));
		Set<BlockPos> seen = new HashSet<>(List.of(start));
		int taken = 0;
		while (!queue.isEmpty() && taken < limit) {
			BlockPos at = queue.poll();
			for (BlockPos next : BlockPos.betweenClosed(at.offset(-1, upward ? 0 : -1, -1), at.offset(1, 1, 1))) {
				if (taken >= limit) {
					break;
				}
				BlockPos p = next.immutable();
				if (!seen.add(p) || upward && p.getY() < start.getY()) {
					continue;
				}
				BlockState state = level.getBlockState(p);
				if (!state.is(kind.getBlock())) {
					continue;
				}
				if (!Casters.mayEdit(ctx.caster, level, p) || !ctx.cast.takeBlock()) {
					return;
				}
				level.destroyBlock(p, true, ctx.caster);
				broken.put(p, state);
				queue.add(p);
				taken++;
			}
		}
		if (taken > 0) {
			Feels.sound(level, Vec3.atCenterOf(start), "note_mod", 0.3F, upward ? 0.7F : 1.1F);
		}
	}

	private static ItemStack tool(ServerLevel level, boolean silk) {
		ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
		var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		if (silk) {
			pick.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
		} else {
			pick.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), HearthLinkRules.WINDFALL_LEVEL);
		}
		return pick;
	}

	private static ItemStack smelted(ServerLevel level, ItemStack stack) {
		SingleRecipeInput input = new SingleRecipeInput(stack);
		var recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
		if (recipe.isEmpty()) {
			return stack;
		}
		ItemStack out = recipe.get().value().assemble(input);
		if (out.isEmpty()) {
			return stack;
		}
		out.setCount(Math.min(out.getMaxStackSize(), out.getCount() * stack.getCount()));
		return out;
	}

	/** Takes one {@code item} out of the stacks, if any is there. */
	private static boolean take(List<ItemStack> stacks, Item item) {
		for (ItemStack stack : stacks) {
			if (stack.is(item) && !stack.isEmpty()) {
				stack.shrink(1);
				return true;
			}
		}
		return false;
	}

	private static List<ItemEntity> newItems(ServerLevel level, AABB box, Set<UUID> before) {
		return level.getEntitiesOfClass(ItemEntity.class, box, item -> item.isAlive() && !before.contains(item.getUUID()));
	}

	private static void drop(ServerLevel level, Vec3 at, ItemStack stack) {
		ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
		item.setDefaultPickUpDelay();
		level.addFreshEntity(item);
	}

	private static void toFeet(ItemEntity item, LivingEntity caster) {
		item.setPos(caster.getX(), caster.getY() + 0.1, caster.getZ());
		item.setDeltaMovement(Vec3.ZERO);
		item.setNoPickUpDelay();
	}

	private static void pull(ServerLevel level, LivingEntity caster, Vec3 point) {
		double r = HearthLinkRules.MAGNET_RADIUS;
		List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(point, point).inflate(r),
			item -> item.isAlive() && item.position().distanceToSqr(point) <= r * r);
		for (ItemEntity item : items) {
			Vfx.emit(level, ParticleTypes.PORTAL, item.position(), 3, 0.1, 0.05);
			toFeet(item, caster);
		}
		if (!items.isEmpty()) {
			Feels.sound(level, caster.position(), "note_mod", 0.3F, 1.6F);
		}
	}

	private static void fleece(Context ctx, Cast.Hit hit, AABB itemBox) {
		ServerLevel level = ctx.cast.level;
		LivingEntity caster = ctx.caster;
		List<net.minecraft.world.entity.animal.sheep.Sheep> sheep = new ArrayList<>();
		for (Entity e : hit.entities()) {
			if (e instanceof net.minecraft.world.entity.animal.sheep.Sheep s) {
				sheep.add(s);
			}
		}
		if (sheep.isEmpty()) {
			sheep.addAll(level.getEntitiesOfClass(net.minecraft.world.entity.animal.sheep.Sheep.class, new AABB(BlockPos.containing(hit.point())).inflate(3)));
		}
		Set<UUID> before = new HashSet<>();
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, itemBox)) {
			before.add(item.getUUID());
		}
		boolean any = false;
		for (net.minecraft.world.entity.animal.sheep.Sheep s : sheep) {
			if (s.readyForShearing()) {
				s.shear(level, SoundSource.PLAYERS, new ItemStack(Items.SHEARS));
				any = true;
			}
		}
		if (any) {
			for (ItemEntity item : newItems(level, itemBox, before)) {
				toFeet(item, caster);
			}
		}
	}

	private static void furrow(Context ctx, BlockPos center) {
		ServerLevel level = ctx.cast.level;
		int r = HearthLinkRules.FURROW_RADIUS;
		int tilled = 0;
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -1, -r), center.offset(r, 1, r))) {
			BlockState state = level.getBlockState(p);
			if ((state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.DIRT_PATH)) && level.getBlockState(p.above()).isAir()
					&& Casters.mayEdit(ctx.caster, level, p) && ctx.cast.takeBlock()) {
				level.setBlockAndUpdate(p, Blocks.FARMLAND.defaultBlockState());
				tilled++;
			}
		}
		if (tilled > 0) {
			Feels.sound(level, Vec3.atCenterOf(center), "note_mod", 0.3F, 0.8F);
			Vfx.emit(level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(center).add(0, 0.6, 0), 4, r, 0.0);
		}
	}

	private static void sow(Context ctx, BlockPos center) {
		if (!(ctx.caster instanceof Player player)) {
			return;
		}
		ServerLevel level = ctx.cast.level;
		int r = HearthLinkRules.SOW_RADIUS;
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -2, -r), center.offset(r, 1, r))) {
			BlockPos above = p.above();
			if (!level.getBlockState(p).is(Blocks.FARMLAND) || !level.getBlockState(above).isAir()) {
				continue;
			}
			ItemStack seeds = seeds(player);
			if (seeds.isEmpty()) {
				return;
			}
			Block crop = ((BlockItem) seeds.getItem()).getBlock();
			BlockState plant = crop.defaultBlockState();
			if (!plant.canSurvive(level, above) || !Casters.mayEdit(player, level, above)) {
				continue;
			}
			level.setBlockAndUpdate(above.immutable(), plant);
			if (!player.isCreative()) {
				seeds.shrink(1);
			}
			Vfx.emit(level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(above), 2, 0.2, 0.0);
		}
	}

	private static ItemStack seeds(Player player) {
		var inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (!stack.isEmpty() && stack.getItem() instanceof BlockItem item && (item.getBlock() instanceof CropBlock || item.getBlock() instanceof StemBlock)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private static void fertilise(Context ctx, BlockPos center) {
		ServerLevel level = ctx.cast.level;
		if (!Casters.mayBuild(ctx.caster)) {
			return;
		}
		int r = HearthLinkRules.FERTILE_RADIUS;
		int grown = 0;
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -2, -r), center.offset(r, 2, r))) {
			if (grown >= 24) {
				break;
			}
			BlockState state = level.getBlockState(p);
			if (state.getBlock() instanceof CropBlock crop) {
				if (!crop.isMaxAge(state)) {
					level.setBlockAndUpdate(p, crop.getStateForAge(crop.getAge(state) + 1));
					grown++;
					Vfx.emit(level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(p), 2, 0.25, 0.0);
				}
			} else if ((state.getBlock() instanceof SaplingBlock || state.getBlock() instanceof StemBlock)
					&& BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, p.immutable())) {
				grown++;
				Vfx.emit(level, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(p), 2, 0.25, 0.0);
			}
		}
	}

	private static void lamp(Context ctx, Cast.Hit hit) {
		if (!(ctx.caster instanceof Player player)) {
			return;
		}
		ServerLevel level = ctx.cast.level;
		BlockPos at = hit.block() != null && hit.face() != null ? hit.block().relative(hit.face()) : BlockPos.containing(hit.point());
		if (!level.getBlockState(at).isAir() || level.getMaxLocalRawBrightness(at) > HearthLinkRules.DARK) {
			return;
		}
		BlockState torch = Blocks.TORCH.defaultBlockState();
		if (!torch.canSurvive(level, at) || !Casters.mayEdit(player, level, at)) {
			return;
		}
		int slot = player.getInventory().findSlotMatchingItem(new ItemStack(Items.TORCH));
		if (slot < 0 && !player.isCreative()) {
			return;
		}
		level.setBlockAndUpdate(at, torch);
		if (slot >= 0 && !player.isCreative()) {
			player.getInventory().getItem(slot).shrink(1);
		}
		Vfx.emit(level, ParticleTypes.FLAME, Vec3.atCenterOf(at), 4, 0.1, 0.01);
	}

	private static void senseOres(ServerLevel level, BlockPos center) {
		int r = HearthLinkRules.ORE_RADIUS;
		List<BlockPos> ores = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
			if (HearthLinks.ore(level.getBlockState(p))) {
				ores.add(p.immutable());
				if (ores.size() >= 48) {
					break;
				}
			}
		}
		if (ores.isEmpty()) {
			return;
		}
		Feels.sound(level, Vec3.atCenterOf(center), "note_mod", 0.3F, 1.8F);
		for (int t = 0; t < HearthLinkRules.ORE_GLOW_TICKS; t += 20) {
			Scheduler.later(Math.max(1, t), () -> {
				for (BlockPos p : ores) {
					if (HearthLinks.ore(level.getBlockState(p))) {
						Vfx.emit(level, ParticleTypes.WAX_ON, Vec3.atCenterOf(p), 2, 0.35, 0.0);
					}
				}
			});
		}
	}

	private static Set<BlockPos> fires(ServerLevel level, BlockPos center) {
		Set<BlockPos> out = new HashSet<>();
		for (BlockPos p : BlockPos.betweenClosed(center.offset(-6, -4, -6), center.offset(6, 4, 6))) {
			if (level.getBlockState(p).getBlock() instanceof BaseFireBlock) {
				out.add(p.immutable());
			}
		}
		return out;
	}

	private static void douse(ServerLevel level, BlockPos center, Set<BlockPos> before) {
		for (BlockPos p : fires(level, center)) {
			if (!before.contains(p)) {
				level.removeBlock(p, false);
				Vfx.emit(level, ParticleTypes.SMOKE, Vec3.atCenterOf(p), 3, 0.2, 0.01);
			}
		}
	}
}
