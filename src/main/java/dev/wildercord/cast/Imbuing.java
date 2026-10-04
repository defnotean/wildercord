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
 * The stored part was paid for up front (three times over), so releasing it costs no mana. It
 * isn't free of time, though: everything a caster has imbued, glyphs included, shares one cooldown,
 * as long as the stored spell's own (so a sword, a bow, a helmet and a row of glyphs on one redstone
 * line can't take turns to cast it faster than the Cord could, and Vow's longer cooldown still
 * counts), and a glyph re-arms only as fast as its spell could be cast (picking it up and putting it
 * down again doesn't reset that). A spell cast once can Imbue once: a storm's echo or Twin Star's
 * second go doesn't store it a second time. A caster keeps at most {@link #MAX_ITEMS} imbued items (the oldest fades), so mana
 * can't be banked into a chest of charged swords, and a release never Siphons mana back.
 */
public final class Imbuing {
	private Imbuing() {}

	/** Glyphs one caster may keep; making another lets the oldest fade. The default: a server sets imbuing.max_glyphs. */
	public static final int MAX_GLYPHS = 12;
	/** Imbued items one caster may keep charged; imbuing another lets the oldest fade. The default: a server sets imbuing.max_items. */
	public static final int MAX_ITEMS = 6;

	/** Imbued items one caster keeps on this server. */
	public static int maxItems() {
		return dev.wildercord.config.Config.get().imbueMaxItems();
	}

	/** Glyphs one caster keeps on this server. */
	public static int maxGlyphs() {
		return dev.wildercord.config.Config.get().imbueMaxGlyphs();
	}
	/** The shortest wait between a caster's releases, whatever the spell (a flurry of strikes, all four armour pieces at once). */
	private static final int ITEM_GAP = 10;
	/** The shortest time a glyph takes to re-arm, whatever its spell. */
	private static final int GLYPH_REARM = 20;
	/** How long a server-time jump may leave a stale cooldown before it's ignored (longer than any spell's). */
	private static final int STALE = 1300;
	/** Players see glyphs from this far. */
	private static final double GLYPH_SEEN = 24.0;

	/** When each player's imbued things may next release (they share one cooldown), and when they last fired an imbued shot. */
	private static final Map<UUID, Long> READY_AT = new HashMap<>();
	private static final Map<UUID, Long> LAST_SHOT = new HashMap<>();
	/** Glyphs broken by their makers while re-arming: when each may go off again, by the serial of the block item it went into. */
	private static final Map<Long, Long> CARRIED = new HashMap<>();
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
		// The shared cooldown outlasts a logout (relogging mustn't reset it); spent ones are swept in the tick.
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_SHOT.remove(handler.player.getUUID()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			READY_AT.clear();
			LAST_SHOT.clear();
			CARRIED.clear();
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
		if (old != null) {
			Ledger.of(player.level()).forget(old);
		}
		target.set(WildercordComponents.IMBUED, counted(player, stored.stream().map(RuneDef::id).toList(), SpellNumbers.IMBUE_CHARGES, color, glint));
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

	/**
	 * A new imbued item's spell, counted among its maker's: if that makes too many, the oldest one's
	 * magic fades (the next time it would release), and the maker is told.
	 */
	private static Imbued counted(ServerPlayer maker, List<String> runes, int charges, int color, boolean glint) {
		long serial = Ledger.of(maker.level()).add(maker.getUUID());
		if (Ledger.of(maker.level()).trimmed(maker.getUUID())) {
			maker.sendSystemMessage(Component.translatable("message.wildercord.imbue_oldest_faded", maxItems()).withStyle(ChatFormatting.GRAY));
		}
		return new Imbued(runes, charges, color, glint, maker.getUUID(), serial);
	}

	/**
	 * Whether an imbued item's magic still holds: false (and the magic is gone from it, and whoever
	 * holds it is told) if its maker has imbued {@link #MAX_ITEMS} newer things since.
	 */
	private static boolean holds(ServerPlayer holder, ItemStack stack, Imbued imbued) {
		if (!imbued.counted() || Ledger.of(holder.level()).has(imbued)) {
			return true;
		}
		strip(stack, imbued);
		holder.sendOverlayMessage(Component.translatable("message.wildercord.imbue_faded", stack.getHoverName()).withStyle(ChatFormatting.GRAY));
		Fx.sound(holder.level(), holder.position(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_BREAK, 0.5F, 1.4F);
		return false;
	}

	private static void strip(ItemStack stack, Imbued imbued) {
		stack.remove(WildercordComponents.IMBUED);
		if (imbued.glint()) {
			stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
		}
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

	/**
	 * Writes a glyph (replacing any there), with its flourish; the oldest of the maker's fades if they have too many,
	 * counted over every world (the game time they were made at is the same clock in all of them).
	 */
	private static void write(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face, List<String> ids, int color, int charges) {
		Glyphs glyphs = Glyphs.of(level);
		long now = level.getGameTime();
		List<Map.Entry<ServerLevel, Glyph>> mine = new ArrayList<>();
		for (ServerLevel world : level.getServer().getAllLevels()) {
			Glyphs held = world == level ? glyphs : world.getDataStorage().get(Glyphs.TYPE);
			if (held != null) {
				held.all().stream().filter(g -> g.owner().equals(player.getUUID())).forEach(g -> mine.add(Map.entry(world, g)));
			}
		}
		mine.sort(java.util.Comparator.comparingLong(e -> e.getValue().made()));
		for (int i = 0; i <= mine.size() - maxGlyphs(); i++) {
			Map.Entry<ServerLevel, Glyph> oldest = mine.get(i);
			Glyphs.of(oldest.getKey()).remove(oldest.getValue().pos());
			fade(oldest.getKey(), oldest.getValue());
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
		Ledger ledger = Ledger.of(level);
		if (imbued.counted() && !ledger.has(imbued)) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.imbue_faded", level.getBlockState(pos).getBlock().getName()).withStyle(ChatFormatting.GRAY));
			return;
		}
		ledger.forget(imbued);
		write(player, level, pos.immutable(), face, imbued.runes(), imbued.color(), imbued.charges());
		// A glyph picked up and put down again is still re-arming: breaking it doesn't reset the wait.
		Long rearm = imbued.counted() ? CARRIED.remove(imbued.serial()) : null;
		if (rearm != null && rearm > level.getGameTime()) {
			Glyphs.of(level).rearm.put(pos.immutable(), rearm);
		}
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

	/** Ticks until this player's imbued things may release again (0 = now): they share one cooldown. */
	private static long waiting(ServerPlayer player) {
		Long at = READY_AT.get(player.getUUID());
		long now = player.level().getGameTime();
		return at == null || at - now > STALE ? 0 : Math.max(0, at - now);
	}

	private static boolean ready(ServerPlayer player) {
		return waiting(player) == 0;
	}

	/** Starts the shared cooldown: as long as the stored spell's own would be for this player, and never under {@link #ITEM_GAP}. */
	private static void cool(ServerPlayer player, List<String> runes) {
		READY_AT.put(player.getUUID(), player.level().getGameTime() + cooldown(player, runes, ITEM_GAP));
	}

	/** The stored spell's cooldown as {@code caster} would cast it (Rapid, Vow and heart perks counted), at least {@code floor}. */
	private static int cooldown(ServerPlayer caster, List<String> ids, int floor) {
		SpellCompiler.Compiled compiled = SpellCompiler.compileStored(runesOf(ids));
		return compiled.isEmpty() ? floor : Math.max(floor, Heart.cooldownTicks(caster, compiled));
	}

	/**
	 * Where an item's spell lets go when its trigger is a creature or block: there, unless the spell
	 * only helps (a Heal in a sword or a helmet), which would be wasted on a foe or a stone, so it
	 * goes to whoever holds the item instead.
	 */
	private static Cast.Trigger aimed(ServerPlayer holder, List<String> runes, Cast.Trigger at) {
		return kinds(runes)[0] ? at : Cast.Trigger.self(holder);
	}

	/** Spends a charge of {@code stack} and releases its spell next tick (after whatever set it off has settled). */
	private static void release(ServerPlayer player, ItemStack stack, Imbued imbued, Cast.Trigger at) {
		spend(player, stack, imbued);
		cool(player, imbued.runes());
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
			dev.wildercord.runesmith.Contracts.onImbueSpent(player, stack);
			strip(stack, imbued);
			Ledger.of(player.level()).forget(imbued);
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
		dev.wildercord.api.WildercordEvents.IMBUE_RELEASED.invoker().onRelease(caster, List.copyOf(runes), at.pos(), at.entity());
		// Paid for when it was imbued: it can't Siphon that mana back a second time.
		Cast cast = new Cast(caster, 1, Heart.bonuses(caster), false, null, new Cast.Info(compiled.root(), runes.size(), Heart.leaning(caster), List.copyOf(runes)))
			.noSiphon().from(at).withAffinity();
		CastEngine.runSegment(cast, compiled.root(), at);
	}

	/** A strike with an imbued weapon or tool, and whatever hurts someone wearing imbued armour. */
	private static void afterDamage(LivingEntity entity, DamageSource source, float baseDamage, float damage, boolean blocked) {
		// A strike by hand, not a spell's (Cleave and Aftershock strike as the caster too).
		if (source.getEntity() instanceof ServerPlayer player && source.getDirectEntity() == player && source.is(DamageTypes.PLAYER_ATTACK) && entity != player
				&& !Dungeons.spellLanding()) {
			ItemStack weapon = player.getMainHandItem();
			Imbued imbued = weapon.get(WildercordComponents.IMBUED);
			Imbued.Release kind = imbued == null ? null : Imbued.release(weapon);
			if ((kind == Imbued.Release.WEAPON || kind == Imbued.Release.TOOL) && ready(player) && holds(player, weapon, imbued)) {
				release(player, weapon, imbued, aimed(player, imbued.runes(),
					new Cast.Trigger(entity.getBoundingBox().getCenter(), player.getLookAngle(), entity, null, null)));
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
				if (ready(wearer) && holds(wearer, worn, imbued)) {
					Vec3 c = attacker.getBoundingBox().getCenter();
					release(wearer, worn, imbued, aimed(wearer, imbued.runes(), new Cast.Trigger(c, c.subtract(wearer.getEyePosition()).normalize(), attacker, null, null)));
				}
				break;
			}
		}
	}

	/**
	 * An arrow leaving an imbued bow or crossbow carries the spell: one arrow and one charge a shot. Of a
	 * crossbow's triple shot only the first arrow carries it (the other two, fired the same tick, are
	 * plain), so Multishot can't turn one charge into three spells.
	 */
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
		Imbued imbued = bow.get(WildercordComponents.IMBUED);
		// While the shared cooldown runs, a shot is only an arrow (and keeps its charge).
		if (!ready(player) || !holds(player, bow, imbued)) {
			return;
		}
		LAST_SHOT.put(player.getUUID(), now);
		spend(player, bow, imbued);
		cool(player, imbued.runes());
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
		if (owner == null || owner.level() != level || !owner.isAlive() || level.getGameTime() < glyphs.rearm.getOrDefault(pos, 0L) || !ready(owner)) {
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
		// A probe only asks whether the block may be changed; the glyph waits for a real break.
		if (level instanceof ServerLevel server && !Casters.probing()) {
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
		long rearm = glyphs.rearm.getOrDefault(pos, 0L);
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
				one.set(WildercordComponents.IMBUED, player instanceof ServerPlayer maker
					? counted(maker, glyph.runes(), glyph.charges(), glyph.color(), true)
					: new Imbued(glyph.runes(), glyph.charges(), glyph.color(), true, glyph.owner(), 0L));
				one.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
				carry(level, one.get(WildercordComponents.IMBUED), rearm);
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

	/** Remembers a broken glyph's re-arm for the block it went back into (by its serial), until that's placed again. */
	private static void carry(ServerLevel level, Imbued imbued, long rearm) {
		long now = level.getGameTime();
		CARRIED.values().removeIf(at -> at <= now);
		if (imbued != null && imbued.counted() && rearm > now) {
			CARRIED.put(imbued.serial(), rearm);
		}
	}

	/** A block broken with an imbued tool. */
	private static void afterBreak(Level level, Player player, BlockPos pos, BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
		if (!(player instanceof ServerPlayer server)) {
			return;
		}
		keep(server.level(), player, pos, state);
		ItemStack tool = server.getMainHandItem();
		Imbued imbued = tool.get(WildercordComponents.IMBUED);
		if (imbued != null && Imbued.release(tool) == Imbued.Release.TOOL && ready(server) && holds(server, tool, imbued)) {
			release(server, tool, imbued, aimed(server, imbued.runes(),
				new Cast.Trigger(Vec3.atCenterOf(pos), server.getLookAngle(), null, pos.immutable(), Direction.UP)));
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
		if (!holds(server, stack, imbued)) {
			return InteractionResult.FAIL;
		}
		if (!ready(server)) {
			server.sendOverlayMessage(Component.translatable("message.wildercord.imbue_cooling",
				String.format(java.util.Locale.ROOT, "%.1f", waiting(server) / 20.0)).withStyle(ChatFormatting.GRAY));
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
			player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0), e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && !e.isRemoved() && e != player, reach * reach);
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
		/**
		 * Read one glyph at a time: one that no longer reads (its block came from a mod that's gone) is
		 * dropped on its own, never the whole dimension's glyphs with it.
		 */
		static final Codec<Glyphs> CODEC = Glyph.CODEC.xmap(Optional::of, Optional::get).orElse(Optional.empty()).listOf()
			.xmap(read -> new Glyphs(read.stream().flatMap(Optional::stream).toList()), glyphs -> glyphs.all().stream().map(Optional::of).toList());
		static final SavedDataType<Glyphs> TYPE = new SavedDataType<>(Wildercord.id("glyphs"), Glyphs::new, CODEC, null);

		private final Map<BlockPos, Glyph> byPos = new LinkedHashMap<>();
		/** Not saved: when each last went off, whether it was powered last time it was looked at, and whom it last went off at. */
		private final Map<BlockPos, Long> rearm = new HashMap<>();
		private final Set<BlockPos> powered = new java.util.HashSet<>();
		private final Map<BlockPos, UUID> victim = new HashMap<>();

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
				victim.remove(pos);
				setDirty();
			}
		}

		boolean isEmpty() {
			return byPos.isEmpty();
		}
	}

	/**
	 * Each caster's imbued items, by serial, oldest first (kept with the overworld, so it holds
	 * whichever world the items are in). Only the newest {@link #MAX_ITEMS} of anyone's still hold
	 * their magic; an item whose serial has dropped off fades the next time it would release.
	 */
	public static final class Ledger extends SavedData {
		// The last serial handed out is saved too, so a faded item's serial is never handed out again.
		static final Codec<Ledger> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.optionalFieldOf("next", 0L).forGetter(ledger -> ledger.next),
			Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG.listOf()).optionalFieldOf("makers", Map.of()).forGetter(ledger -> ledger.byMaker)
		).apply(i, Ledger::new));
		static final SavedDataType<Ledger> TYPE = new SavedDataType<>(Wildercord.id("imbued_items"), Ledger::new, CODEC, null);

		private final Map<UUID, List<Long>> byMaker = new HashMap<>();
		private long next;

		public Ledger() {
		}

		private Ledger(long next, Map<UUID, List<Long>> saved) {
			this.next = next;
			saved.forEach((maker, serials) -> {
				byMaker.put(maker, new ArrayList<>(serials));
				serials.forEach(s -> this.next = Math.max(this.next, s));
			});
		}

		public static Ledger of(ServerLevel level) {
			return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
		}

		/** Counts a new imbued item of {@code maker}'s and returns its serial. */
		long add(UUID maker) {
			long serial = ++next;
			byMaker.computeIfAbsent(maker, k -> new ArrayList<>()).add(serial);
			setDirty();
			return serial;
		}

		/** Lets the oldest of {@code maker}'s items fade until they have no more than {@link #MAX_ITEMS}; true if any did. */
		boolean trimmed(UUID maker) {
			List<Long> serials = byMaker.get(maker);
			boolean any = false;
			while (serials != null && serials.size() > maxItems()) {
				serials.removeFirst();
				any = true;
			}
			if (any) {
				setDirty();
			}
			return any;
		}

		public boolean has(Imbued imbued) {
			List<Long> serials = byMaker.get(imbued.maker());
			return serials != null && serials.contains(imbued.serial());
		}

		/** An item that's spent, re-imbued or became a glyph stops counting. */
		void forget(Imbued imbued) {
			List<Long> serials = imbued.counted() ? byMaker.get(imbued.maker()) : null;
			if (serials != null && serials.remove(imbued.serial())) {
				if (serials.isEmpty()) {
					byMaker.remove(imbued.maker());
				}
				setDirty();
			}
		}

		/** How many of {@code maker}'s imbued items still hold their magic. */
		public int count(UUID maker) {
			return byMaker.getOrDefault(maker, List.of()).size();
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
		if (tick % 1200 == 0 && !READY_AT.isEmpty()) {
			long now = server.overworld().getGameTime();
			READY_AT.values().removeIf(at -> at <= now || at - now > STALE);
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
				// A glyph shares its maker's imbued cooldown too: a dozen on one redstone line can't all go off at once.
				if (!awake || now < glyphs.rearm.getOrDefault(glyph.pos(), 0L) || !ready(owner)) {
					continue;
				}
				LivingEntity stepper = null;
				if (!rising) {
					boolean[] kind = kinds(glyph.runes());
					List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, zone(level, glyph),
						e -> e.isAlive() && !e.isSpectator() && (kind[0] && Targets.canHarm(owner, e) || kind[1] && Targets.canHelp(owner, e)));
					// A creature that stays on a glyph is caught once: it goes off at it again only after it steps off and back on.
					UUID last = glyphs.victim.get(glyph.pos());
					boolean stillOn = last != null && near.stream().anyMatch(e -> e.getUUID().equals(last));
					if (last != null && !stillOn) {
						glyphs.victim.remove(glyph.pos());
					}
					UUID caught = stillOn ? last : null;
					stepper = near.stream().filter(e -> !e.getUUID().equals(caught)).findFirst().orElse(null);
					if (stepper == null) {
						continue;
					}
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
		if (!ready(owner)) {
			return;
		}
		cool(owner, glyph.runes());
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
			// It re-arms no faster than its spell could be cast again (so a Vowed glyph waits its Vow out).
			glyphs.rearm.put(glyph.pos(), level.getGameTime() + cooldown(owner, glyph.runes(), GLYPH_REARM));
			if (stepper != null) {
				glyphs.victim.put(glyph.pos(), stepper.getUUID());
			}
		}
		Sigils.spell(level, at.add(n.scale(0.04)), n, runesOf(glyph.runes()), glyph.color(), Math.max(0.3F, circleSize(level, glyph) * 1.4F), 18);
		Sigils.flash(level, at.add(n.scale(0.2)), glyph.color(), 1.4F);
		Light.groundRing(level, at, glyph.color(), 0.2, 1.6, 0.06, 10);
		cast(owner, glyph.runes(), trigger);
		dev.wildercord.advancement.Advancements.moment(owner, dev.wildercord.advancement.Advancements.GLYPH);
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
