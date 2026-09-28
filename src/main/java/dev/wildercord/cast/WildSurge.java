package dev.wildercord.cast;

import dev.wildercord.content.SigilOption;
import dev.wildercord.content.WildercordSounds;
import dev.wildercord.player.Mana;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.RuneColors;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.Runes;
import dev.wildercord.spell.SpellCompiler;
import dev.wildercord.spell.SpellPlan;
import dev.wildercord.spell.WildMagic;
import dev.wildercord.spell.WildMagic.Surge;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Wild magic: an overcast spell (paid for by cracking a Heart Circle) sometimes twists into
 * something unexpected. {@link WildMagic} holds the chance and the table; this makes each outcome
 * happen, with a swirl of its colour around the caster and a line above their hotbar.
 *
 * <p>Nothing here changes a block (the spell itself still asks {@link Casters#mayEdit} for every one
 * it would), and nothing here can kill the caster: a Backfire stops at half a heart, a Blink only lands
 * on safe, solid ground no more than 3 blocks down, and floating comes with a slow fall.</p>
 */
public final class WildSurge {
	private WildSurge() {}

	private static final Vec3 UP = new Vec3(0, 1, 0);
	/** The Wisps surge's wisps: the violet of the Runebound's marks. */
	private static final int WISP_COLOR = 0xB06CFF;

	/** The outcome the next overcast of each player surges into, whatever the dice say (tests, commands). */
	private static final Map<UUID, Surge> FORCED = new HashMap<>();
	/** Free Recasts waiting, by player: until when. */
	private static final Map<UUID, Long> FREE = new HashMap<>();

	/** Makes the next overcast of {@code player} surge into {@code surge} (null: back to chance). */
	public static void force(ServerPlayer player, Surge surge) {
		if (surge == null) {
			FORCED.remove(player.getUUID());
		} else {
			FORCED.put(player.getUUID(), surge);
		}
	}

	/** Whether {@code player} has a Free Recast waiting. */
	public static boolean freeRecast(ServerPlayer player, long now) {
		Long until = FREE.get(player.getUUID());
		return until != null && now <= until && until - now <= WildMagic.FREE_RECAST_TICKS;
	}

	/** Spends a waiting Free Recast. */
	static void useFreeRecast(ServerPlayer player) {
		FREE.remove(player.getUUID());
		player.sendOverlayMessage(Component.translatable("message.wildercord.surge.free_used").withColor(Surge.FREE_RECAST.color));
	}

	static void forget(UUID player) {
		FORCED.remove(player);
		FREE.remove(player);
	}

	static void clear() {
		FORCED.clear();
		FREE.clear();
	}

	/**
	 * An overcast spell is about to go off: rolls for a surge, and makes it happen if one comes up.
	 *
	 * @param mana    the mana the caster had
	 * @param cost    what the spell cost
	 * @param release casts the spell as it was written, for a given cast
	 * @return true if it surged (and the surge decided what, if anything, was cast); false to cast it as usual
	 */
	static boolean overcast(Cast cast, List<RuneDef> runes, boolean secret, double mana, double cost, Consumer<Cast> release) {
		if (!(cast.caster instanceof ServerPlayer player)) {
			return false;
		}
		RandomSource random = player.getRandom();
		Surge surge = FORCED.remove(player.getUUID());
		if (surge == null) {
			if (random.nextDouble() >= WildMagic.chance(mana, cost, Mana.max(player))) {
				return false;
			}
			surge = WildMagic.pick(random.nextDouble(), !secret);
		}
		if (secret && surge.rewrites) {
			surge = Surge.TWICE;
		}
		surge(player, cast, runes, surge, release);
		Grimoire.feat(player, Feats.WILD_SURGE);
		return true;
	}

	private static void surge(ServerPlayer player, Cast cast, List<RuneDef> runes, Surge surge, Consumer<Cast> release) {
		ServerLevel level = player.level();
		RandomSource random = player.getRandom();
		Vec3 heart = player.position().add(0, 1.1, 0);
		switch (surge) {
			case TWICE -> twice(player, cast, release);
			case ELEMENT -> {
				String from = WildMagic.elementOf(runes);
				List<String> others = new ArrayList<>(WildMagic.elements());
				others.remove(from);
				SpellCompiler.Compiled swapped = null;
				String to = "";
				if (!from.isEmpty() && !others.isEmpty()) {
					to = others.get(random.nextInt(others.size()));
					swapped = SpellCompiler.compile(WildMagic.swapElement(runes, to, random.nextInt(8)));
				}
				if (swapped == null || swapped.isEmpty()) {
					// Nothing in it has an element to swap: it goes off twice instead.
					twice(player, cast, release);
					surge = Surge.TWICE;
				} else {
					announce(player, surge, Component.translatable("element.wildercord." + to).withColor(RuneColors.element(to)));
					CastEngine.cast(cast, swapped.root());
					return;
				}
			}
			case GRAND -> {
				List<RuneDef> grand = WildMagic.grand(runes);
				SpellCompiler.Compiled widened = grand == null ? null : SpellCompiler.compile(grand);
				if (widened == null || widened.isEmpty()) {
					// Nothing in it has a size: it comes out half as strong again instead.
					release.accept(cast.withPower(1.5));
				} else {
					CastEngine.cast(cast, widened.root());
				}
				ElementFx.ring(level, heart, UP, surge.color, 0.5, 4.5, 0.1, 12);
			}
			case BUTTERFLIES -> butterflies(level, heart, random);
			case HEAL_ALL -> {
				for (LivingEntity t : near(player, WildMagic.NEAR, true)) {
					t.heal(6.0F);
					Vfx.heal(level, t);
				}
				ElementFx.groundRing(level, player.position(), surge.color, 0.4, WildMagic.NEAR, 0.08, 14);
			}
			case BLINK -> {
				blink(player, random);
				release.accept(cast);
			}
			case WISPS -> {
				release.accept(cast);
				SpellCompiler.Compiled wisp = SpellCompiler.compile(List.of(Runes.WISP, Runes.HARM));
				if (!wisp.isEmpty() && !wisp.root().groups.isEmpty()) {
					SpellPlan.Group group = wisp.root().groups.getFirst();
					Cast weak = cast.withPower(0.5);
					Vfx.Theme theme = Vfx.themeOf(WISP_COLOR);
					for (int i = 0; i < 4; i++) {
						double a = Math.PI * 2 * i / 4 + random.nextDouble() * 0.5;
						ShapeRunners.wisp(weak.child(), group, null, heart, new Vec3(Math.cos(a), 0.35, Math.sin(a)), theme);
					}
				}
			}
			case LEVITATE -> {
				for (LivingEntity t : near(player, 6.0, true)) {
					if (Spirits.isBoss(t)) {
						continue;
					}
					t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 24, 1, false, true));
					if (t instanceof ServerPlayer) {
						// Players come down gently: a flipped moment of gravity is never a fall to your death.
						t.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, true));
					}
				}
				ElementFx.swirl(level, player.position(), 2.2, 3.0, 5, 0xE0F4FF, 0xFFFFFF);
				release.accept(cast);
			}
			case STRAY -> {
				List<LivingEntity> near = near(player, WildMagic.NEAR * 2, false);
				near.removeIf(t -> !player.hasLineOfSight(t));
				if (near.isEmpty()) {
					twice(player, cast, release);
					surge = Surge.TWICE;
				} else {
					LivingEntity target = near.get(random.nextInt(near.size()));
					// The spell tugs your arm round to whoever it has chosen.
					player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getBoundingBox().getCenter());
					announce(player, surge, target.getDisplayName());
					release.accept(cast);
					return;
				}
			}
			case SLOW_TIME -> {
				for (LivingEntity t : near(player, WildMagic.NEAR, true)) {
					t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2, false, true));
				}
				ElementFx.groundRing(level, player.position(), ElementFx.TIME.primary(), WildMagic.NEAR, 0.5, 0.08, 16);
				Fx.sound(level, heart, SoundEvents.BELL_BLOCK, 0.8F, 0.5F);
				release.accept(cast);
			}
			case BACKFIRE -> {
				float damage = Math.min(WildMagic.BACKFIRE, player.getHealth() - 1.0F);
				if (damage > 0 && !player.isCreative()) {
					player.hurtServer(level, level.damageSources().magic(), damage);
				}
				Vfx.radial(level, ParticleTypes.SMOKE, heart, 16, 0.12);
				Vfx.radial(level, new DustParticleOptions(surge.color, 1.3F), heart, 14, 0.25);
				Fx.sound(level, heart, SoundEvents.GENERIC_EXPLODE, 0.5F, 1.6F);
			}
			case FREE_RECAST -> {
				release.accept(cast);
				FREE.put(player.getUUID(), level.getGameTime() + WildMagic.FREE_RECAST_TICKS);
			}
			case WARD -> {
				release.accept(cast);
				// Never trades a stronger Shield already up for this one.
				if (Shields.strength(player) < WildMagic.WARD_STRENGTH) {
					Shields.give(player, WildMagic.WARD_STRENGTH, WildMagic.WARD_TICKS, List.of(Runes.SHIELD.id()));
				}
				Fx.sound(level, heart, WildercordSounds.SHIELD_UP, 0.8F, 1.2F);
			}
		}
		announce(player, surge);
	}

	/** It goes off, and again a moment later. */
	private static void twice(ServerPlayer player, Cast cast, Consumer<Cast> release) {
		release.accept(cast);
		Scheduler.later(8, () -> {
			if (player.isAlive() && !player.isRemoved()) {
				TechniqueVfx.twinStar(player.level(), player);
				release.accept(cast.withPower(1.0));
			}
		});
	}

	/** Blinks the caster 3 to 6 blocks away, onto safe, solid ground; nowhere safe, nowhere at all. */
	private static void blink(ServerPlayer player, RandomSource random) {
		ServerLevel level = player.level();
		for (int attempt = 0; attempt < 12; attempt++) {
			double a = random.nextDouble() * Math.PI * 2;
			double d = WildMagic.BLINK_MIN + random.nextDouble() * (WildMagic.BLINK_MAX - WildMagic.BLINK_MIN);
			Vec3 spot = CastEngine.ground(level, player.position().add(Math.cos(a) * d, 1.5, Math.sin(a) * d));
			if (spot.y < player.getY() - 3 || spot.y > player.getY() + 2.5) {
				continue;
			}
			AABB box = player.getDimensions(player.getPose()).makeBoundingBox(spot);
			BlockPos below = BlockPos.containing(spot.x, spot.y - 0.5, spot.z);
			boolean solid = level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && !level.getBlockState(below).is(Blocks.MAGMA_BLOCK);
			if (solid && level.noCollision(player, box) && !level.containsAnyLiquid(box.inflate(0, 0.5, 0)) && level.getWorldBorder().isWithinBounds(spot.x, spot.z)) {
				Vec3 from = player.position();
				player.teleportTo(level, spot.x, spot.y, spot.z, Set.<Relative>of(), player.getYRot(), player.getXRot(), false);
				player.resetFallDistance();
				Vfx.blink(level, from, spot);
				Fx.sound(level, spot, WildercordSounds.BLINK, 0.9F, 1.1F);
				return;
			}
		}
	}

	/** A harmless shower: butterflies of light fluttering up and away, and fireworks bursting overhead. */
	private static void butterflies(ServerLevel level, Vec3 heart, RandomSource random) {
		int[] colors = {0xFFA8E8, 0x9AE0FF, 0xFFE08A, 0xB8FFB0, 0xD0A8FF};
		for (int wave = 0; wave < 6; wave++) {
			int w = wave;
			Scheduler.later(1 + wave * 4, () -> {
				for (int i = 0; i < 5; i++) {
					int color = colors[(w + i) % colors.length];
					Vec3 dir = new Vec3(random.nextDouble() - 0.5, 0.6 + random.nextDouble() * 0.5, random.nextDouble() - 0.5).normalize();
					Vec3 at = heart.add(dir.scale(0.4 + w * 0.25));
					// Two wings of light, and the glow of the body between them.
					Light.orb(level, at, color, 0.07, 14);
					Vfx.fling(level, new DustParticleOptions(color, 0.9F), at.add(dir.cross(UP).normalize().scale(0.12)), dir, 0.12);
					Vfx.fling(level, new DustParticleOptions(color, 0.9F), at.subtract(dir.cross(UP).normalize().scale(0.12)), dir, 0.12);
				}
				Vec3 burst = heart.add((random.nextDouble() - 0.5) * 3, 2.5 + random.nextDouble() * 1.5, (random.nextDouble() - 0.5) * 3);
				Vfx.radial(level, ParticleTypes.FIREWORK, burst, 18, 0.18);
				Sigils.flash(level, burst, 0xFF000000 | colors[w % colors.length], 1.0F);
				Fx.sound(level, burst, w % 2 == 0 ? SoundEvents.FIREWORK_ROCKET_BLAST : SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.7F, 1.0F + 0.1F * w);
			});
		}
	}

	/** Every living creature within {@code range} of the caster (the caster too, with {@code self}). */
	private static List<LivingEntity> near(ServerPlayer player, double range, boolean self) {
		List<LivingEntity> found = new ArrayList<>(player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
			e -> e.isAlive() && !e.isSpectator() && e.distanceTo(player) <= range && (self || e != player)));
		if (!self) {
			found.remove(player);
		}
		return found;
	}

	/** The swirl of its colour round the caster, and (a moment later, after the crack's own line) what it became. */
	private static void announce(ServerPlayer player, Surge surge, Object... args) {
		ServerLevel level = player.level();
		Vec3 feet = player.position();
		Vec3 heart = feet.add(0, 1.1, 0);
		ElementFx.swirl(level, feet.add(0, 0.1, 0), 1.4, 2.4, 7, surge.color, 0xFFFFFF);
		ElementFx.flatSigil(level, feet.add(0, 0.05, 0), SigilOption.STAR, surge.color, 2.8, 30, 0.3);
		ElementFx.ring(level, heart, UP, surge.color, 0.3, 2.6, 0.06, 10);
		Vfx.radial(level, new DustParticleOptions(surge.color, 1.2F), heart, 18, 0.3);
		Vfx.radial(level, ParticleTypes.WITCH, heart, 10, 0.2);
		Fx.sound(level, heart, WildercordSounds.CIRCLE_OPEN, 0.9F, 1.4F);
		Fx.sound(level, heart, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9F, 1.5F);
		Component line = Component.translatable("message.wildercord.wild_surge", Component.translatable(surge.key(), args)).withColor(surge.color);
		Scheduler.later(18, () -> {
			if (!player.isRemoved()) {
				player.sendOverlayMessage(line);
			}
		});
	}
}
