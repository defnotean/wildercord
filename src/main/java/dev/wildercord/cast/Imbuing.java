package dev.wildercord.cast;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildercord.Wildercord;
import dev.wildercord.content.CordItem;
import dev.wildercord.content.Imbued;
import dev.wildercord.content.RuneItem;
import dev.wildercord.content.SpellScrollItem;
import dev.wildercord.content.WildercordComponents;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.player.Heart;
import dev.wildercord.player.WildercordAttachments;
import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellNames;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Imbue: the rest of a spell isn't cast but stored, with {@link SpellNumbers#IMBUE_CHARGES} charges,
 * in what the shape before it touched.
 * <ul>
 *   <li><b>Items</b> (Self: the item in your hand) hold it in an {@link Imbued} component and let it go
 *       through their use: a weapon at what it strikes, a bow or crossbow where its arrows land, a tool
 *       at each block it breaks (and what it strikes), armour or a shield at whatever hurts you, anything
 *       else when used, at what you're looking at.</li>
 *   <li><b>Blocks</b>, any block at all, become glyphs, kept with the world: a faint copy of the spell's
 *       circle on the face that was struck. A glyph goes off at a creature that steps on (or touches)
 *       it, uses it (opens the door, the chest), shoots it, or breaks it, or when the block is powered
 *       by redstone. It sleeps while its maker is away (it casts as them). A block item in your hand can
 *       be imbued too, and becomes a glyph where it's placed; break your own glyph and the block you get
 *       back still holds what's left of it.</li>
 * </ul>
 * The stored part was paid for up front (three times over), so releasing it costs nothing.
 */
public final class Imbuing {
	private Imbuing() {}

	/** Glyphs one caster may keep; making another lets the oldest fade. */
	public static final int MAX_GLYPHS = 12;
	/** One item releases at most this often (a flurry of strikes, all four armour pieces at once). */
	private static final int ITEM_GAP = 10;
	/** A glyph goes off at most this often. */
	private static final int GLYPH_REARM = 20;
	/** Players see glyphs from this far. */
	private static final double GLYPH_SEEN = 24.0;

	/** When each player's imbued things last released, and fired an imbued shot. */
	private static final Map<UUID, Long> LAST_RELEASE = new HashMap<>();
	private static final Map<UUID, Long> LAST_SHOT = new HashMap<>();
	/** Imbued arrows in flight, for their trail. */
	private static final Set<Projectile> SHOTS = Collections.newSetFromMap(new WeakHashMap<>());

	public static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register(Imbuing::afterDamage);
		ServerEntityEvents.ENTITY_LOAD.register(Imbuing::onEntityLoad);
		PlayerBlockBreakEvents.AFTER.register(Imbuing::afterBreak);
		PlayerBlockBreakEvents.BEFORE.register(Imbuing::beforeBreak);
		net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register(Imbuing::onUseBlock);
		UseItemCallback.EVENT.register(Imbuing::onUse);
		ServerTickEvents.END_SERVER_TICK.register(Imbuing::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			LAST_RELEASE.remove(handler.player.getUUID());
			LAST_SHOT.remove(handler.player.getUUID());
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			LAST_RELEASE.clear();
			LAST_SHOT.clear();
			SHOTS.clear();
		});
	}

	// ------------------------------------------------------------------ imbuing

	/** Called when a shape anchoring an Imbue hits: stores the rest in the item in hand (Self) or the block it touched. */
	static void imbue(Cast cast, Cast.Hit hit) {
		if (!(cast.caster instanceof ServerPlayer player) || !cast.once("imbue")) {
			return;
		}
		List<RuneDef> stored = SpellCompiler.stored(cast.info.spell());
		if (stored.isEmpty() || stored.stream().anyMatch(r -> r.is(Runes.IMBUE.id())) || SpellCompiler.compileStored(stored).isEmpty()) {
			fail(player, "message.wildercord.imbue_nothing");
			return;
		}
		int color = colorOf(stored);
		if (hit.self()) {
			if (player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty()) {
				// Nothing in hand: the block you're looking at, whatever it is.
				Vec3 eye = player.getEyePosition();
				BlockHitResult looked = cast.level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(player.blockInteractionRange())),
					ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
				if (looked.getType() != HitResult.Type.MISS) {
					imbueBlock(player, cast.level, looked.getBlockPos().immutable(), looked.getDirection(), stored, color, SpellNumbers.IMBUE_CHARGES);
					return;
				}
			}
			imbueItem(player, stored, color);
			return;
		}
		BlockPos pos = hit.block();
		Direction face = hit.face();
		if (pos == null && !hit.entities().isEmpty()) {
			Entity first = hit.entities().getFirst();
			if (first == player) {
				imbueItem(player, stored, color);
				return;
			}
			// A creature was struck: the glyph goes into the ground under it.
			pos = first.blockPosition().below();
			face = Direction.UP;
		}
		if (pos == null || cast.level.getBlockState(pos).isAir() || !cast.level.getFluidState(pos).isEmpty() && cast.level.getBlockState(pos).canBeReplaced()) {
			fail(player, "message.wildercord.imbue_no_target");
			return;
		}
		imbueBlock(player, cast.level, pos.immutable(), face == null ? Direction.UP : face, stored, color, SpellNumbers.IMBUE_CHARGES);
	}

	private static void imbueItem(ServerPlayer player, List<RuneDef> stored, int color) {
		InteractionHand hand = player.getMainHandItem().isEmpty() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		ItemStack held = player.getItemInHand(hand);
		if (held.isEmpty()) {
			fail(player, "message.wildercord.imbue_empty_hand");
			return;
		}
		if (held.getItem() instanceof CordItem || held.getItem() instanceof RuneItem || held.getItem() instanceof SpellScrollItem) {
			fail(player, "message.wildercord.imbue_cannot");
			return;
		}
		// One item holds the spell: from a stack, one is taken off and imbued (so charges can't be copied by splitting).
		ItemStack target = held.getCount() > 1 ? held.split(1) : held;
		Imbued old = target.get(WildercordComponents.IMBUED);
		boolean glint = old != null ? old.glint() : !target.has(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
		target.set(WildercordComponents.IMBUED, new Imbued(stored.stream().map(RuneDef::id).toList(), SpellNumbers.IMBUE_CHARGES, color, glint));
		if (glint) {
			target.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
		if (target != held && !player.getInventory().add(target)) {
			player.drop(target, false, net.minecraft.util.Prediction.SERVER_ONLY);
		}
		ServerLevel level = player.level();
		Vec3 at = hand(player, hand);
		Sigils.spell(level, at, player.getLookAngle().scale(-1), stored, color, 0.32F, 30);
		Sigils.flash(level, at, color, 1.0F);
		ElementFx.implode(level, at, 0.9, 6);
		Fx.sound(level, at, WildercordSounds.IMBUE, 0.9F, 1.0F);
		Imbued.Release release = Imbued.release(target);
		player.sendOverlayMessage(Component.translatable("message.wildercord.imbued_item", target.getHoverName(),
			Component.literal(SpellNames.auto(stored)).withColor(color), SpellNumbers.IMBUE_CHARGES,
			Component.translatable("tooltip.wildercord.imbued." + release.name().toLowerCase(java.util.Locale.ROOT))).withColor(0xE8E0FF));
		Grimoire.feat(player, dev.wildercord.spell.Feats.IMBUE);
	}

	private static void imbueBlock(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face, List<RuneDef> stored, int color, int charges) {
		if (level.getBlockState(pos).isAir()) {
			fail(player, "message.wildercord.imbue_no_target");
			return;
		}
		if (!Casters.mayEdit(player, level, pos)) {
			fail(player, "message.wildercord.imbue_protected");
			return;
		}
		write(player, level, pos, face, stored.stream().map(RuneDef::id).toList(), color, charges);
		player.sendOverlayMessage(Component.translatable("message.wildercord.imbued_block", Component.literal(SpellNames.auto(stored)).withColor(color),
			charges).withColor(0xE8E0FF));
		Grimoire.feat(player, dev.wildercord.spell.Feats.IMBUE);
	}

	/** Writes a glyph (replacing any there), with its flourish; the oldest of the maker's fades if they have too many. */
	private static void write(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face, List<String> ids, int color, int charges) {
		Glyphs glyphs = Glyphs.of(level);
		long now = level.getGameTime();
		List<Glyph> mine = glyphs.all().stream().filter(g -> g.owner().equals(player.getUUID())).sorted(java.util.Comparator.comparingLong(Glyph::made)).toList();
		for (int i = 0; i <= mine.size() - MAX_GLYPHS; i++) {
			Glyph oldest = mine.get(i);
			glyphs.remove(oldest.pos());
			fade(level, oldest);
		}
		Glyph glyph = new Glyph(pos, face, ids, charges, player.getUUID(), color, now, level.getBlockState(pos).getBlock());
		glyphs.put(glyph);
		Vec3 at = faceCentre(level, glyph);
		Vec3 n = Vec3.atLowerCornerOf(face.getUnitVec3i());
		Sigils.spell(level, at.add(n.scale(0.03)), n, runesOf(ids), color, circleSize(level, glyph) * 1.1F, 40);
		Light.groundRing(level, at, color, 0.1, 1.1, 0.05, 12);
		Sigils.flash(level, at, color, 1.2F);
		Fx.sound(level, at, WildercordSounds.IMBUE, 0.9F, 1.0F);
		// Its circle shows straight away for everyone near.
		show(level, glyph, true);
	}

	/** From the block item mixin: an imbued block item was placed, and the block becomes a glyph holding what the item held. */
	public static void placed(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face, Imbued imbued) {
		if (imbued == null || imbued.charges() <= 0 || level.getBlockState(pos).isAir()) {
			return;
		}
		write(player, level, pos.immutable(), face, imbued.runes(), imbued.color(), imbued.charges());
		player.sendOverlayMessage(Component.translatable("message.wildercord.imbued_block",
			Component.literal(SpellNames.auto(runesOf(imbued.runes()))).withColor(imbued.color()), imbued.charges()).withColor(0xE8E0FF));
	}

	/** Where a glyph's circle lies: on the block's own shape (a slab's top, a button's face), not its whole cube. */
	static Vec3 faceCentre(ServerLevel level, Glyph glyph) {
		AABB box = shape(level, glyph.pos());
		Vec3 c = box.getCenter();
		return switch (glyph.face()) {
			case UP -> new Vec3(c.x, box.maxY, c.z);
			case DOWN -> new Vec3(c.x, box.minY, c.z);
			case NORTH -> new Vec3(c.x, c.y, box.minZ);
			case SOUTH -> new Vec3(c.x, c.y, box.maxZ);
			case WEST -> new Vec3(box.minX, c.y, c.z);
			case EAST -> new Vec3(box.maxX, c.y, c.z);
		};
	}

	/** The block's outline in the world (its whole cube if it has none). */
	private static AABB shape(ServerLevel level, BlockPos pos) {
		net.minecraft.world.phys.shapes.VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
		return shape.isEmpty() ? new AABB(pos) : shape.bounds().move(pos);
	}

	/** A glyph's circle fits the face it's on: smaller on a button than on a floor. */
	private static float circleSize(ServerLevel level, Glyph glyph) {
		AABB box = shape(level, glyph.pos());
		double across = switch (glyph.face().getAxis()) {
			case Y -> Math.min(box.getXsize(), box.getZsize());
			case X -> Math.min(box.getYsize(), box.getZsize());
			case Z -> Math.min(box.getXsize(), box.getYsize());
		};
		return (float) Math.max(0.12, Math.min(0.42, across * 0.45));
	}

	/** Where a creature must be to set a glyph off: on it, or touching the face it's on. */
	static AABB zone(ServerLevel level, Glyph glyph) {
		Vec3 n = Vec3.atLowerCornerOf(glyph.face().getUnitVec3i());
		return shape(level, glyph.pos()).expandTowards(n.scale(0.5)).inflate(0.02);
	}

	private static void fail(ServerPlayer player, String key) {
		player.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
	}

	/** The stored spell's colour: its first effect's element, or its first rune's. */
	static int colorOf(List<RuneDef> runes) {
		for (RuneDef rune : runes) {
			if (rune.family() == dev.wildercord.spell.RuneFamily.EFFECT) {
				return RuneColors.of(rune);
			}
		}
		return runes.isEmpty() ? 0xE678DC : RuneColors.of(runes.getFirst());
	}

	/** Roughly where a player's hand is, for the flourishes. */
	private static Vec3 hand(ServerPlayer player, InteractionHand hand) {
		Vec3 look = player.getLookAngle();
		Vec3 side = new Vec3(-look.z, 0, look.x).normalize().scale(hand == InteractionHand.MAIN_HAND ? 0.35 : -0.35);
		return player.getEyePosition().add(look.scale(0.55)).add(side).add(0, -0.35, 0);
	}

	// ------------------------------------------------------------------ releasing from items

	/** Whether this player's imbued things may release now (see {@link #ITEM_GAP}). */
	private static boolean ready(ServerPlayer player) {
		Long last = LAST_RELEASE.get(player.getUUID());
		long now = player.level().getGameTime();
		return last == null || now - last >= ITEM_GAP || last > now;
	}

	/** Spends a charge of {@code stack} and releases its spell next tick (after whatever set it off has settled). */
	private static void release(ServerPlayer player, ItemStack stack, Imbued imbued, Cast.Trigger at) {
		spend(player, stack, imbued);
		LAST_RELEASE.put(player.getUUID(), player.level().getGameTime());
		List<String> runes = imbued.runes();
		int color = imbued.color();
		Scheduler.later(1, () -> {
			if (player.isAlive() && !player.isRemoved()) {
				Sigils.flash(player.level(), at.pos(), color, 1.2F);
				cast(player, runes, at);
			}
		});
	}

	private static void spend(ServerPlayer player, ItemStack stack, Imbued imbued) {
		int left = imbued.charges() - 1;
		if (left <= 0) {
			stack.remove(WildercordComponents.IMBUED);
			if (imbued.glint()) {
				stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
			}
			player.sendOverlayMessage(Component.translatable("message.wildercord.imbue_spent", stack.getHoverName()).withStyle(ChatFormatting.GRAY));
			Fx.sound(player.level(), player.position(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK, 0.5F, 1.4F);
		} else {
			stack.set(WildercordComponents.IMBUED, imbued.withCharges(left));
		}
	}

	/** Casts stored runes as {@code caster}, set off at {@code at}. */
	static void cast(ServerPlayer caster, List<String> ids, Cast.Trigger at) {
		List<RuneDef> runes = new ArrayList<>();
		for (String id : ids) {
			Runes.get(id).ifPresent(runes::add);
		}
		SpellCompiler.Compiled compiled = SpellCompiler.compileStored(runes);
		if (compiled.isEmpty() || runes.stream().anyMatch(r -> r.is(Runes.IMBUE.id()))) {
			return;
		}
		Cast cast = new Cast(caster, 1, Heart.bonuses(caster), false, null, new Cast.Info(compiled.root(), runes.size(), Heart.leaning(caster), List.copyOf(runes)));
		CastEngine.runSegment(cast, compiled.root(), at);
	}

	/** A strike with an imbued weapon or tool, and whatever hurts someone wearing imbued armour. */
	private static void afterDamage(LivingEntity entity, DamageSource source, float baseDamage, float damage, boolean blocked) {
		if (source.getEntity() instanceof ServerPlayer player && source.getDirectEntity() == player && source.is(DamageTypes.PLAYER_ATTACK) && entity != player) {
			ItemStack weapon = player.getMainHandItem();
			Imbued imbued = weapon.get(WildercordComponents.IMBUED);
			Imbued.Release kind = imbued == null ? null : Imbued.release(weapon);
			if ((kind == Imbued.Release.WEAPON || kind == Imbued.Release.TOOL) && ready(player)) {
				release(player, weapon, imbued, new Cast.Trigger(entity.getBoundingBox().getCenter(), player.getLookAngle(), entity, null, null));
			}
		}
		if (entity instanceof ServerPlayer wearer && source.getEntity() instanceof LivingEntity attacker && attacker != wearer && (damage > 0 || blocked)) {
			for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
					EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
				ItemStack worn = wearer.getItemBySlot(slot);
				Imbued imbued = worn.get(WildercordComponents.IMBUED);
				if (imbued == null || Imbued.release(worn) != Imbued.Release.WORN) {
					continue;
				}
				if (ready(wearer)) {
					Vec3 c = attacker.getBoundingBox().getCenter();
					release(wearer, worn, imbued, new Cast.Trigger(c, c.subtract(wearer.getEyePosition()).normalize(), attacker, null, null));
				}
				break;
			}
		}
	}

	/** An arrow leaving an imbued bow or crossbow carries the spell (one charge a shot, even a triple one). */
	private static void onEntityLoad(Entity entity, ServerLevel level) {
		if (!(entity instanceof AbstractArrow arrow) || arrow.hasAttached(WildercordAttachments.IMBUED_SHOT) || arrow.tickCount > 0
				|| !(arrow.getOwner() instanceof ServerPlayer player) || player.distanceToSqr(arrow) > 64) {
			return;
		}
		ItemStack fired = arrow.getWeaponItem();
		if (fired == null || !fired.has(WildercordComponents.IMBUED) || arrow.getDeltaMovement().lengthSqr() < 0.01) {
			return;
		}
		ItemStack bow = null;
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack held = player.getItemInHand(hand);
			if (held.is(fired.getItem()) && held.has(WildercordComponents.IMBUED)) {
				bow = held;
				break;
			}
		}
		if (bow == null) {
			return;
		}
		long now = level.getGameTime();
		Long last = LAST_SHOT.get(player.getUUID());
		if (last != null && last == now) {
			return;
		}
		LAST_SHOT.put(player.getUUID(), now);
		Imbued imbued = bow.get(WildercordComponents.IMBUED);
		spend(player, bow, imbued);
		arrow.setAttached(WildercordAttachments.IMBUED_SHOT, new WildercordAttachments.ImbuedShot(imbued.runes(), imbued.color()));
		SHOTS.add(arrow);
	}

	/** From the projectile mixin: an imbued arrow struck something, and its spell goes off there. */
	public static void onShotHit(Projectile projectile, HitResult result) {
		WildercordAttachments.ImbuedShot shot = projectile.removeAttached(WildercordAttachments.IMBUED_SHOT);
		SHOTS.remove(projectile);
		if (shot == null || !(projectile.getOwner() instanceof ServerPlayer owner) || owner.level() != projectile.level()) {
			return;
		}
		Vec3 dir = projectile.getDeltaMovement().lengthSqr() > 1.0E-6 ? projectile.getDeltaMovement().normalize() : owner.getLookAngle();
		Cast.Trigger at;
		if (result instanceof EntityHitResult hit) {
			at = new Cast.Trigger(hit.getEntity().getBoundingBox().getCenter(), dir, hit.getEntity(), null, null);
		} else if (result instanceof BlockHitResult hit && hit.getType() != HitResult.Type.MISS) {
			at = new Cast.Trigger(hit.getLocation(), dir, null, hit.getBlockPos(), hit.getDirection());
		} else {
			return;
		}
		Scheduler.later(1, () -> {
			if (owner.isAlive() && !owner.isRemoved() && owner.level() == projectile.level()) {
				Sigils.flash(owner.level(), at.pos(), shot.color(), 1.3F);
				cast(owner, shot.runes(), at);
			}
		});
	}

	/**
	 * Sets a glyph off from outside (someone used, shot or is breaking its block): at {@code by} if the
	 * spell is for them, or from the block itself for {@code by == null}. Returns whether it went off.
	 */
	private static boolean trigger(ServerLevel level, BlockPos pos, LivingEntity by) {
		Glyphs glyphs = level.getDataStorage().get(Glyphs.TYPE);
		Glyph glyph = glyphs == null ? null : glyphs.at(pos).orElse(null);
		if (glyph == null) {
			return false;
		}
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(glyph.owner());
		if (owner == null || owner.level() != level || !owner.isAlive() || level.getGameTime() < glyphs.rearm.getOrDefault(pos, 0L)) {
			return false;
		}
		if (by != null) {
			boolean[] kind = kinds(glyph.runes());
			if (!(kind[0] && Targets.canHarm(owner, by) || kind[1] && Targets.canHelp(owner, by))) {
				return false;
			}
		}
		fire(level, glyphs, glyph, owner, by);
		return true;
	}

	/** Using a glyph's block (a door, a chest, a lever, a button) sets it off at whoever it's for. */
	private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (level instanceof ServerLevel server && hand == InteractionHand.MAIN_HAND) {
			trigger(server, hit.getBlockPos(), player);
		}
		return InteractionResult.PASS;
	}

	/** From the projectile mixin: something shot a glyph's block, and it goes off. */
	public static void onBlockShot(ServerLevel level, BlockPos pos) {
		trigger(level, pos, null);
	}

	/** Someone else breaking a glyph's block sets it off at them first (if it's for them). */
	private static boolean beforeBreak(Level level, Player player, BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity be) {
		if (level instanceof ServerLevel server) {
			Glyphs glyphs = server.getDataStorage().get(Glyphs.TYPE);
			Glyph glyph = glyphs == null ? null : glyphs.at(pos).orElse(null);
			if (glyph != null && !glyph.owner().equals(player.getUUID())) {
				trigger(server, pos, player);
			}
		}
		return true;
	}

	/**
	 * Its maker breaking a glyph's block gets the block back still holding what's left of it (if the
	 * block drops itself); anyone else's break, or a block that drops something else, lets it fade.
	 */
	private static void keep(ServerLevel level, Player player, BlockPos pos, BlockState state) {
		Glyphs glyphs = level.getDataStorage().get(Glyphs.TYPE);
		Glyph glyph = glyphs == null ? null : glyphs.at(pos).orElse(null);
		if (glyph == null) {
			return;
		}
		glyphs.remove(pos);
		if (!glyph.owner().equals(player.getUUID())) {
			fade(level, glyph);
			return;
		}
		// The block's drops appear just after the break is reported: the magic goes into one next tick.
		net.minecraft.world.item.Item item = state.getBlock().asItem();
		Scheduler.later(1, () -> {
			for (net.minecraft.world.entity.item.ItemEntity drop : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					new AABB(pos).inflate(1.5), e -> e.getAge() <= 3 && e.getItem().is(item) && !e.getItem().has(WildercordComponents.IMBUED))) {
				ItemStack stack = drop.getItem();
				ItemStack one = stack.getCount() > 1 ? stack.split(1) : stack;
				one.set(WildercordComponents.IMBUED, new Imbued(glyph.runes(), glyph.charges(), glyph.color(), true));
				one.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
				if (one != stack) {
					level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(), one));
				} else {
					drop.setItem(one);
				}
				Sigils.flash(level, drop.position(), glyph.color(), 0.8F);
				return;
			}
			fade(level, glyph);
		});
	}

	/** A block broken with an imbued tool. */
	private static void afterBreak(Level level, Player player, BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
		if (!(player instanceof ServerPlayer server)) {
			return;
		}
		keep(server.level(), player, pos, state);
		ItemStack tool = server.getMainHandItem();
		Imbued imbued = tool.get(WildercordComponents.IMBUED);
		if (imbued != null && Imbued.release(tool) == Imbued.Release.TOOL && ready(server)) {
			release(server, tool, imbued, new Cast.Trigger(Vec3.atCenterOf(pos), server.getLookAngle(), null, pos.immutable(), Direction.UP));
		}
	}

	/** Using an imbued item that has no other way to let go: at what you're looking at. */
	private static InteractionResult onUse(Player player, Level level, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		Imbued imbued = stack.get(WildercordComponents.IMBUED);
		if (imbued == null || Imbued.release(stack) != Imbued.Release.USE) {
			return InteractionResult.PASS;
		}
		if (!(player instanceof ServerPlayer server)) {
			return InteractionResult.SUCCESS;
		}
		if (!ready(server)) {
			return InteractionResult.FAIL;
		}
		Cast.Trigger at = aim(server, imbued.runes());
		if (at == null) {
			server.sendOverlayMessage(Component.translatable("message.wildercord.imbue_no_aim").withStyle(ChatFormatting.GRAY));
			return InteractionResult.FAIL;
		}
		server.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
		release(server, stack, imbued, at);
		return InteractionResult.SUCCESS;
	}

	/**
	 * Where a used item's spell goes: a spell that starts with a shape leaves from you like any cast;
	 * one that starts with an effect lands on what you're looking at (null if that's nothing).
	 */
	private static Cast.Trigger aim(ServerPlayer player, List<String> ids) {
		List<RuneDef> runes = new ArrayList<>();
		ids.forEach(id -> Runes.get(id).ifPresent(runes::add));
		SpellCompiler.Compiled compiled = SpellCompiler.compileStored(runes);
		if (!compiled.isEmpty() && !compiled.root().groups.isEmpty() && !compiled.root().groups.getFirst().implicit) {
			return Cast.Trigger.self(player);
		}
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		Vec3 end = eye.add(look.scale(CastEngine.AIM_RANGE));
		BlockHitResult block = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
		double reach = block.getType() == HitResult.Type.MISS ? CastEngine.AIM_RANGE : block.getLocation().distanceTo(eye);
		EntityHitResult creature = ProjectileUtil.getEntityHitResult(player, eye, eye.add(look.scale(reach)),
			player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0), e -> e instanceof LivingEntity && e.isAlive() && e != player, reach * reach);
		if (creature != null) {
			return new Cast.Trigger(creature.getEntity().getBoundingBox().getCenter(), look, creature.getEntity(), null, null);
		}
		if (block.getType() != HitResult.Type.MISS) {
			return new Cast.Trigger(block.getLocation(), look, null, block.getBlockPos(), block.getDirection());
		}
		return null;
	}

	// ------------------------------------------------------------------ glyphs

	/** A spell imbued into a block: where, on which face, what it holds, and whose it is. */
	public record Glyph(BlockPos pos, Direction face, List<String> runes, int charges, UUID owner, int color, long made, Block block) {
		static final Codec<Glyph> CODEC = RecordCodecBuilder.create(i -> i.group(
			BlockPos.CODEC.fieldOf("pos").forGetter(Glyph::pos),
			Direction.CODEC.optionalFieldOf("face", Direction.UP).forGetter(Glyph::face),
			Codec.STRING.listOf().fieldOf("runes").forGetter(Glyph::runes),
			Codec.INT.fieldOf("charges").forGetter(Glyph::charges),
			UUIDUtil.CODEC.fieldOf("owner").forGetter(Glyph::owner),
			Codec.INT.optionalFieldOf("color", 0xE678DC).forGetter(Glyph::color),
			Codec.LONG.optionalFieldOf("made", 0L).forGetter(Glyph::made),
			BuiltInRegistries.BLOCK.byNameCodec().fieldOf("block").forGetter(Glyph::block)
		).apply(i, Glyph::new));

		Glyph withCharges(int left) {
			return new Glyph(pos, face, runes, left, owner, color, made, block);
		}
	}

	/** The glyphs of one dimension, saved with it. */
	public static final class Glyphs extends SavedData {
		static final Codec<Glyphs> CODEC = Glyph.CODEC.listOf().xmap(Glyphs::new, Glyphs::all);
		static final SavedDataType<Glyphs> TYPE = new SavedDataType<>(Wildercord.id("glyphs"), Glyphs::new, CODEC, null);

		private final Map<BlockPos, Glyph> byPos = new LinkedHashMap<>();
		/** Not saved: when each last went off, and whether it was powered last time it was looked at. */
		private final Map<BlockPos, Long> rearm = new HashMap<>();
		private final Set<BlockPos> powered = new java.util.HashSet<>();

		public Glyphs() {
		}

		private Glyphs(List<Glyph> glyphs) {
			glyphs.forEach(g -> byPos.put(g.pos(), g));
		}

		public static Glyphs of(ServerLevel level) {
			return level.getDataStorage().computeIfAbsent(TYPE);
		}

		public List<Glyph> all() {
			return List.copyOf(byPos.values());
		}

		public Optional<Glyph> at(BlockPos pos) {
			return Optional.ofNullable(byPos.get(pos));
		}

		void put(Glyph glyph) {
			byPos.put(glyph.pos(), glyph);
			setDirty();
		}

		void remove(BlockPos pos) {
			if (byPos.remove(pos) != null) {
				rearm.remove(pos);
				powered.remove(pos);
				setDirty();
			}
		}

		boolean isEmpty() {
			return byPos.isEmpty();
		}
	}

	private static void tick(MinecraftServer server) {
		int tick = server.getTickCount();
		if (!SHOTS.isEmpty()) {
			// Imbued arrows trail motes of their spell's colour.
			for (Iterator<Projectile> it = SHOTS.iterator(); it.hasNext(); ) {
				Projectile p = it.next();
				if (p.isRemoved() || !p.hasAttached(WildercordAttachments.IMBUED_SHOT) || !(p.level() instanceof ServerLevel level)) {
					it.remove();
				} else {
					Fx.send(level, Fx.dust(p.getAttached(WildercordAttachments.IMBUED_SHOT).color(), 0.8F), p.position(), 1, 0.02, 0.0);
				}
			}
		}
		if (tick % 2 != 0) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			Glyphs glyphs = level.getDataStorage().get(Glyphs.TYPE);
			if (glyphs == null || glyphs.isEmpty()) {
				continue;
			}
			long now = level.getGameTime();
			for (Glyph glyph : glyphs.all()) {
				if (!level.isLoaded(glyph.pos())) {
					continue;
				}
				if (!level.getBlockState(glyph.pos()).is(glyph.block())) {
					// The block it was written on is gone (or changed): so is the glyph.
					glyphs.remove(glyph.pos());
					fade(level, glyph);
					continue;
				}
				ServerPlayer owner = server.getPlayerList().getPlayer(glyph.owner());
				boolean awake = owner != null && owner.level() == level && owner.isAlive() && !owner.isSpectator();
				if (tick % 100 == 0) {
					show(level, glyph, awake);
				}
				boolean power = level.hasNeighborSignal(glyph.pos());
				boolean rising = power && !glyphs.powered.contains(glyph.pos());
				if (power) {
					glyphs.powered.add(glyph.pos());
				} else {
					glyphs.powered.remove(glyph.pos());
				}
				if (!awake || now < glyphs.rearm.getOrDefault(glyph.pos(), 0L)) {
					continue;
				}
				LivingEntity stepper = null;
				if (!rising) {
					boolean[] kind = kinds(glyph.runes());
					List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, zone(level, glyph),
						e -> e.isAlive() && !e.isSpectator() && (kind[0] && Targets.canHarm(owner, e) || kind[1] && Targets.canHelp(owner, e)));
					if (near.isEmpty()) {
						continue;
					}
					stepper = near.getFirst();
				}
				fire(level, glyphs, glyph, owner, stepper);
			}
		}
	}

	/** Whom a glyph's spell is for: [0] creatures its maker would harm, [1] ones it would help. */
	private static boolean[] kinds(List<String> ids) {
		boolean harm = false;
		boolean help = false;
		for (String id : ids) {
			RuneDef rune = Runes.get(id).orElse(null);
			if (rune == null || rune.family() != dev.wildercord.spell.RuneFamily.EFFECT) {
				continue;
			}
			help |= rune.kind() == EffectKind.HELPFUL;
			harm |= rune.kind() != EffectKind.HELPFUL;
		}
		return new boolean[] {harm || !help, help};
	}

	private static void fire(ServerLevel level, Glyphs glyphs, Glyph glyph, ServerPlayer owner, LivingEntity stepper) {
		Vec3 at = faceCentre(level, glyph);
		Vec3 n = Vec3.atLowerCornerOf(glyph.face().getUnitVec3i());
		Cast.Trigger trigger;
		if (stepper != null) {
			Vec3 c = stepper.getBoundingBox().getCenter();
			Vec3 dir = c.subtract(at).lengthSqr() > 1.0E-4 ? c.subtract(at).normalize() : n;
			trigger = new Cast.Trigger(c, dir, stepper, glyph.pos(), glyph.face());
		} else {
			trigger = new Cast.Trigger(at.add(n.scale(0.1)), n, null, glyph.pos(), glyph.face());
		}
		int left = glyph.charges() - 1;
		if (left <= 0) {
			glyphs.remove(glyph.pos());
		} else {
			glyphs.put(glyph.withCharges(left));
			glyphs.rearm.put(glyph.pos(), level.getGameTime() + GLYPH_REARM);
		}
		Sigils.spell(level, at.add(n.scale(0.04)), n, runesOf(glyph.runes()), glyph.color(), Math.max(0.3F, circleSize(level, glyph) * 1.4F), 18);
		Sigils.flash(level, at.add(n.scale(0.2)), glyph.color(), 1.4F);
		Light.groundRing(level, at, glyph.color(), 0.2, 1.6, 0.06, 10);
		cast(owner, glyph.runes(), trigger);
		if (left <= 0) {
			fade(level, glyph);
		}
	}

	/** The glyph's circle, faint, for the players near it (fainter still while its maker is away). */
	private static void show(ServerLevel level, Glyph glyph, boolean awake) {
		Vec3 n = Vec3.atLowerCornerOf(glyph.face().getUnitVec3i());
		Vec3 at = faceCentre(level, glyph).add(n.scale(0.02));
		int color = dim(glyph.color(), awake ? 0.55F : 0.22F);
		dev.wildercord.content.SpellCircleOption circle = new dev.wildercord.content.SpellCircleOption(
			glyph.runes().stream().limit(dev.wildercord.spell.SpellSigil.MAX_RUNES).toList(), color, circleSize(level, glyph), Sigils.yaw(n), Sigils.pitch(n), 120);
		for (ServerPlayer player : level.players()) {
			if (player.position().distanceToSqr(at) <= GLYPH_SEEN * GLYPH_SEEN) {
				level.sendParticles(player, circle, true, true, at.x, at.y, at.z, 1, 0, 0, 0, 0);
			}
		}
	}

	private static void fade(ServerLevel level, Glyph glyph) {
		Vec3 at = Vec3.atCenterOf(glyph.pos()).add(Vec3.atLowerCornerOf(glyph.face().getUnitVec3i()).scale(0.5));
		Fx.send(level, net.minecraft.core.particles.ParticleTypes.ENCHANT, at, 12, 0.3, 0.3);
		Fx.sound(level, at, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK, 0.4F, 1.5F);
	}

	private static List<RuneDef> runesOf(List<String> ids) {
		List<RuneDef> runes = new ArrayList<>();
		ids.forEach(id -> Runes.get(id).ifPresent(runes::add));
		return runes;
	}

	private static int dim(int rgb, float f) {
		int r = Math.round(((rgb >> 16) & 0xFF) * f);
		int g = Math.round(((rgb >> 8) & 0xFF) * f);
		int b = Math.round((rgb & 0xFF) * f);
		return (r << 16) | (g << 8) | b;
	}
}
