package dev.wildercord.familiar;

import dev.wildercord.cast.ElementFx;
import dev.wildercord.cast.Fx;
import dev.wildercord.cast.Grimoire;
import dev.wildercord.cast.Spirits;
import dev.wildercord.content.WellstoneBlock;
import dev.wildercord.content.WildercordBlocks;
import dev.wildercord.player.Mana;
import dev.wildercord.player.Spellbooks;
import dev.wildercord.spell.Feats;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Familiars on the server: bonding a wild wisp, calling a familiar out and sending it home,
 * telling it to stay (or to wait at a Wellstone), its name, its experience and its level. The truth
 * is kept on the player ({@link Bonds}); the wisp in the world is only the familiar's body, summoned
 * next to its owner whenever it's out and missing (after logging in, a respawn, a new dimension).
 * One familiar is out at a time.
 */
public final class Familiars {
	private Familiars() {}

	/** How near its owner a familiar must be to help: regeneration, spells and experience. */
	public static final double NEAR = 24.0;
	/** How far a Wellstone may be for a familiar told to stay to wait there instead. */
	private static final int WELL_REACH = 12;

	/** The body of each familiar that's out or waiting, by bond id. */
	private static final Map<String, Wisp> LIVE = new HashMap<>();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			int tick = server.getTickCount();
			if (tick % 10 == 0) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					tickPlayer(player, tick % 20 == 0);
				}
			}
			WispSpawner.tick(server);
		});
		// Logging off: the familiar goes back into its lantern (it's still "out", so it comes back with them).
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			Wisp wisp = live(handler.player);
			if (wisp != null) {
				// The world's entity list may only change on the server thread: a disconnect handled anywhere else waits for it.
				if (server.isSameThread()) {
					wisp.discard();
				} else {
					server.execute(wisp::discard);
				}
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LIVE.clear());
		// Fighting together: every monster its owner (or its spells) slays near it teaches a familiar a little.
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(entity instanceof Enemy)) {
				return;
			}
			ServerPlayer player = source.getEntity() instanceof ServerPlayer p ? p
				: source.getEntity() instanceof OwnableEntity pet && pet.getOwner() instanceof ServerPlayer p ? p : null;
			if (player != null) {
				gainXp(player, WispRules.killXp(Spirits.isBoss(entity)));
			}
		});
	}

	public static Bonds get(Player player) {
		return player.getAttachedOrElse(FamiliarContent.BONDS, Bonds.NONE);
	}

	static void set(ServerPlayer player, Bonds bonds) {
		player.setAttached(FamiliarContent.BONDS, bonds);
	}

	/** The body of the player's familiar that's out, if it's in the world. */
	public static Wisp live(Player player) {
		Bonds bonds = get(player);
		Wisp wisp = bonds.out().isEmpty() ? null : LIVE.get(bonds.out());
		return wisp != null && !wisp.isRemoved() ? wisp : null;
	}

	static void forget(Wisp wisp) {
		if (!wisp.bondId.isEmpty()) {
			LIVE.remove(wisp.bondId, wisp);
		}
	}

	// ------------------------------------------------------------------ taming

	/** Called for every effect of every spell as it lands: a wild wisp it touched drinks the magic, if it's of its element. */
	public static void onSpell(dev.wildercord.cast.Cast cast, dev.wildercord.cast.Cast.Hit hit, String element) {
		if (element.isEmpty() || !(cast.caster instanceof ServerPlayer player)) {
			return;
		}
		for (net.minecraft.world.entity.Entity entity : hit.entities()) {
			if (entity instanceof Wisp wisp && wisp.wild()) {
				wisp.offer(player, element, cast.identity());
			}
		}
	}

	static void tamingProgress(Wisp wisp, ServerPlayer player, int hits) {
		ServerLevel level = (ServerLevel) wisp.level();
		wisp.flare();
		Fx.sound(level, wisp.position(), FamiliarContent.WISP_CHIME, 0.9F, 0.85F + 0.15F * hits);
		ElementFx.impact(level, wisp.element(), wisp.position().add(0, 0.2, 0), 0.45);
		player.sendOverlayMessage(Component.translatable("message.wildercord.wisp.drinks", element(wisp.element()), hits, WispRules.TAMING_HITS)
			.withColor(wisp.color()));
	}

	static void spooked(Wisp wisp, ServerPlayer player, String offered) {
		ServerLevel level = (ServerLevel) wisp.level();
		Fx.sound(level, wisp.position(), SoundEvents.AMETHYST_BLOCK_BREAK, 0.6F, 1.8F);
		Fx.send(level, ParticleTypes.POOF, wisp.position().add(0, 0.2, 0), 6, 0.15, 0.02);
		player.sendOverlayMessage(Component.translatable("message.wildercord.wisp.spooked", element(offered), element(wisp.element()))
			.withStyle(ChatFormatting.GRAY));
	}

	/** Tells a player they keep all the familiars they can. */
	static void full(ServerPlayer player) {
		player.sendOverlayMessage(Component.translatable("message.wildercord.wisp.full", WispRules.MAX_BONDS).withStyle(ChatFormatting.RED));
	}

	/** A wild wisp becomes the player's familiar, and comes out at once (the one out before goes back into the lantern). */
	static void bond(ServerPlayer player, Wisp wisp) {
		Bonds bonds = get(player);
		if (!WispRules.canBond(bonds.bonds().size())) {
			full(player);
			return;
		}
		recall(player, true);
		bonds = get(player);
		String id = wisp.getStringUUID();
		Bonds.Bond bond = new Bonds.Bond(id, wisp.element(), "", 0, false);
		bonds = bonds.with(bond).withOut(id);
		set(player, bonds);
		wisp.bondId = id;
		wisp.setOwner(player.getUUID());
		wisp.setMode(Wisp.FOLLOW);
		wisp.setFamiliarLevel(1);
		wisp.setPersistenceRequired();
		wisp.updateName(player.getGameProfile().name(), "");
		LIVE.put(id, wisp);

		ServerLevel level = (ServerLevel) wisp.level();
		wisp.flare();
		Fx.sound(level, wisp.position(), FamiliarContent.WISP_BOND, 1.0F, 1.0F);
		ElementFx.impact(level, wisp.element(), wisp.position().add(0, 0.2, 0), 1.1);
		ElementFx.ring(level, wisp.position().add(0, 0.2, 0), new Vec3(0, 1, 0), wisp.color(), 0.2, 1.6, 0.06, 14);
		player.sendSystemMessage(Component.translatable("message.wildercord.wisp.bonded", element(wisp.element())).withColor(wisp.color()));
		if (bonds.bonds().size() == 1) {
			player.sendSystemMessage(Component.translatable("message.wildercord.wisp.bonded_hint").withStyle(ChatFormatting.GRAY));
		}
		Grimoire.feat(player, Feats.KINDRED);
		if (WispRules.menagerie(bonds.elements())) {
			Grimoire.feat(player, Feats.MENAGERIE);
		}
	}

	// ------------------------------------------------------------------ out and home

	/** Calls a familiar out next to its owner. */
	static Wisp summon(ServerPlayer player, Bonds.Bond bond) {
		ServerLevel level = player.level();
		Wisp wisp = FamiliarContent.WISP.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (wisp == null) {
			return null;
		}
		wisp.setElement(bond.element());
		Vec3 at = wisp.shoulder(player);
		wisp.snapTo(at.x, at.y, at.z, player.getYRot(), 0);
		wisp.bondId = bond.id();
		wisp.setOwner(player.getUUID());
		wisp.setMode(Wisp.FOLLOW);
		wisp.setFamiliarLevel(bond.level());
		wisp.setPersistenceRequired();
		wisp.updateName(player.getGameProfile().name(), bond.name());
		level.addFreshEntity(wisp);
		LIVE.put(bond.id(), wisp);
		wisp.flare();
		Fx.send(level, Wisp.elementParticle(bond.element()), at.add(0, 0.2, 0), 8, 0.2, 0.02);
		return wisp;
	}

	/** Sends the familiar that's out back into the lantern. Returns whether one was out. */
	static boolean recall(ServerPlayer player, boolean quiet) {
		Bonds bonds = get(player);
		Bonds.Bond out = bonds.outBond();
		if (out == null) {
			return false;
		}
		Wisp wisp = live(player);
		if (wisp != null) {
			if (!quiet) {
				Fx.sound(player.level(), wisp.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 1.6F);
			}
			Fx.send((ServerLevel) wisp.level(), Wisp.elementParticle(out.element()), wisp.position().add(0, 0.2, 0), 8, 0.15, 0.02);
			wisp.discard();
		}
		set(player, bonds.withOut(""));
		if (!quiet) {
			player.sendOverlayMessage(Component.translatable("message.wildercord.wisp.home", displayName(out)).withColor(color(out)));
		}
		return true;
	}

	/** Calls a familiar out by its bond (a waiting one leaves its Wellstone). */
	static void callOut(ServerPlayer player, Bonds.Bond bond) {
		recall(player, true);
		Bonds bonds = get(player);
		if (bond.waiting()) {
			bond = bond.withWaiting(false);
			bonds = bonds.with(bond);
		}
		set(player, bonds.withOut(bond.id()));
		summon(player, bond);
		Fx.sound(player.level(), player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 1.4F);
		player.sendOverlayMessage(Component.translatable("message.wildercord.wisp.out", displayName(bond), bond.level()).withColor(color(bond)));
	}

	/** Sneak-use on your own familiar: follow becomes stay (or waiting, near an awake Wellstone), and back. */
	static void toggleStay(ServerPlayer player, Wisp wisp) {
		Bonds bonds = get(player);
		Bonds.Bond bond = bonds.get(wisp.bondId);
		if (bond == null) {
			return;
		}
		switch (wisp.mode()) {
			case Wisp.FOLLOW -> {
				if (wellstoneNear((ServerLevel) wisp.level(), wisp.blockPosition())) {
					wisp.setMode(Wisp.WAITING);
					set(player, bonds.with(bond.withWaiting(true)).withOut(""));
					player.sendOverlayMessage(Component.translatable("message.wildercord.wisp.waiting", displayName(bond)).withColor(color(bond)));
				} else {
					wisp.setMode(Wisp.STAY);
					player.sendOverlayMessage(Component.translatable("message.wildercord.wisp.stay", displayName(bond)).withColor(color(bond)));
				}
			}
			case Wisp.STAY -> {
				wisp.setMode(Wisp.FOLLOW);
				player.sendOverlayMessage(Component.translatable("message.wildercord.wisp.follow", displayName(bond)).withColor(color(bond)));
			}
			case Wisp.WAITING -> {
				// Collected from its Wellstone: it follows again, and whoever was out goes home.
				Wisp current = live(player);
				if (current != null && current != wisp) {
					recall(player, true);
					bonds = get(player);
				}
				wisp.setMode(Wisp.FOLLOW);
				set(player, bonds.with(bond.withWaiting(false)).withOut(bond.id()));
				LIVE.put(bond.id(), wisp);
				player.sendOverlayMessage(Component.translatable("message.wildercord.wisp.follow", displayName(bond)).withColor(color(bond)));
			}
			default -> { }
		}
		wisp.flare();
		Fx.sound(player.level(), wisp.position(), FamiliarContent.WISP_CHIME, 0.7F, wisp.mode() == Wisp.FOLLOW ? 1.3F : 0.9F);
	}

	private static boolean wellstoneNear(ServerLevel level, BlockPos at) {
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-WELL_REACH, -4, -WELL_REACH), at.offset(WELL_REACH, 4, WELL_REACH))) {
			BlockState state = level.getBlockState(pos);
			if (state.is(WildercordBlocks.WELLSTONE) && state.getValue(WellstoneBlock.ACTIVE)) {
				return true;
			}
		}
		return false;
	}

	/** A name tag on a familiar: kept on its bond, and shown with its owner's name. */
	static void rename(Wisp wisp, String name) {
		ServerPlayer owner = wisp.ownerPlayer();
		if (owner == null) {
			return;
		}
		Bonds bonds = get(owner);
		Bonds.Bond bond = bonds.get(wisp.bondId);
		if (bond == null) {
			return;
		}
		String clean = name.strip();
		clean = clean.length() > 32 ? clean.substring(0, 32) : clean;
		set(owner, bonds.with(bond.withName(clean)));
		wisp.updateName(owner.getGameProfile().name(), clean);
	}

	/** A wild wisp fading away (day came, or it has drifted long enough). */
	static void fade(Wisp wisp) {
		ServerLevel level = (ServerLevel) wisp.level();
		Fx.send(level, Wisp.elementParticle(wisp.element()), wisp.position().add(0, 0.2, 0), 6, 0.2, 0.01);
		Fx.send(level, ParticleTypes.END_ROD, wisp.position().add(0, 0.2, 0), 3, 0.1, 0.02);
		wisp.discard();
	}

	// ------------------------------------------------------------------ ticking

	/** Every 10 ticks, per player: calls out a familiar that's out but missing, and gives its mana. */
	private static void tickPlayer(ServerPlayer player, boolean second) {
		Bonds bonds = get(player);
		Bonds.Bond out = bonds.outBond();
		if (out == null) {
			if (!bonds.out().isEmpty()) {
				set(player, bonds.withOut(""));
			}
			return;
		}
		Wisp wisp = live(player);
		if (wisp == null || wisp.level() != player.level()) {
			if (wisp != null) {
				wisp.discard();
			}
			if (player.isAlive() && !player.isSpectator()) {
				summon(player, out);
			}
			return;
		}
		// A little more mana, twice a second, while it's near.
		if (Spellbooks.tier(player) != null && wisp.distanceTo(player) <= NEAR) {
			Mana.restore(player, Mana.of(player).regen() * WispRules.regenBonus(out.level()) / 2);
		}
	}

	/** Every tick, for each familiar in the world: is it still wanted here, and its magic. */
	static void tickFamiliar(ServerLevel level, Wisp wisp) {
		if (wisp.tickCount % 20 == 0 && !stillWanted(wisp)) {
			wisp.discard();
			return;
		}
		if (wisp.mode() == Wisp.WAITING) {
			return;
		}
		ServerPlayer owner = wisp.ownerPlayer();
		if (owner == null || owner.level() != level) {
			return;
		}
		LIVE.putIfAbsent(wisp.bondId, wisp);
		if (owner.isAlive() && !owner.isSpectator() && wisp.distanceTo(owner) <= NEAR) {
			Bonds.Bond bond = get(owner).get(wisp.bondId);
			FamiliarMagic.tick(level, wisp, owner, bond == null ? 1 : bond.level());
		}
	}

	/**
	 * Whether this body still belongs: its owner is online and still has its bond, and it's the one
	 * out (or it's waiting at its Wellstone, and no other body of it is about). A waiting familiar
	 * whose owner is offline stays put.
	 */
	private static boolean stillWanted(Wisp wisp) {
		ServerPlayer owner = wisp.ownerPlayer();
		if (owner == null) {
			return wisp.mode() == Wisp.WAITING;
		}
		Bonds bonds = get(owner);
		Bonds.Bond bond = bonds.get(wisp.bondId);
		if (bond == null) {
			return false;
		}
		Wisp other = LIVE.get(bond.id());
		if (other != null && other != wisp && !other.isRemoved()) {
			return false;
		}
		if (wisp.mode() == Wisp.WAITING) {
			return bond.waiting();
		}
		return bonds.out().equals(bond.id()) && owner.level() == wisp.level();
	}

	/** Experience for the familiar that's out, if it's near; a new level is announced. */
	static void gainXp(ServerPlayer player, int xp) {
		Bonds bonds = get(player);
		Bonds.Bond out = bonds.outBond();
		Wisp wisp = live(player);
		if (out == null || wisp == null || wisp.level() != player.level() || wisp.distanceTo(player) > NEAR) {
			return;
		}
		int before = out.level();
		Bonds.Bond grown = out.withXp(out.xp() + xp);
		set(player, bonds.with(grown));
		if (grown.level() > before) {
			wisp.setFamiliarLevel(grown.level());
			wisp.flare();
			ServerLevel level = (ServerLevel) wisp.level();
			Fx.sound(level, wisp.position(), FamiliarContent.WISP_LEVEL, 1.0F, 1.0F);
			ElementFx.impact(level, wisp.element(), wisp.position().add(0, 0.2, 0), 0.9);
			player.sendSystemMessage(Component.translatable("message.wildercord.wisp.level", displayName(grown), grown.level(),
				Math.round(WispRules.regenBonus(grown.level()) * 100), WispRules.helpInterval(grown.level()) / 20).withColor(color(grown)));
		}
	}

	// ------------------------------------------------------------------ text

	public static Component element(String element) {
		return Component.translatable("element.wildercord." + element);
	}

	/** Its name from a name tag, or "Fire Wisp". */
	public static Component displayName(Bonds.Bond bond) {
		return bond.name().isEmpty() ? Component.translatable("entity.wildercord.wisp.of", element(bond.element())) : Component.literal(bond.name());
	}

	public static int color(Bonds.Bond bond) {
		return dev.wildercord.spell.RuneColors.element(bond.element());
	}

	/** The owner of a familiar, for other systems (never a wild wisp's). */
	public static LivingEntity owner(Wisp wisp) {
		return wisp.wild() ? null : wisp.getOwner();
	}
}
