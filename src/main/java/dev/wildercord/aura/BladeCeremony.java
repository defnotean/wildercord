package dev.wildercord.aura;

import dev.wildercord.api.AuraApi;
import dev.wildercord.aura.arts.ArtLight;
import dev.wildercord.cast.LeyWalker;
import dev.wildercord.cast.Motes;
import dev.wildercord.cast.PowerPlaces;
import dev.wildercord.config.Config;
import dev.wildercord.content.SigilOption;
import dev.wildercord.world.LeyLines;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The bonded blade's two ceremonies, and how its moments look and sound.
 *
 * <p><b>The bond.</b> A swordsman from Edge kneels in the breathing stance at a ley crossing with an unbonded blade in hand. Once the
 * stance has settled the ceremony runs ten seconds in three parts: <i>kindling</i> (motes of their aura rising out of the ground along
 * the crossing's two lines into the blade), <i>joining</i> (the two ley lines lighting up across the ground and running into them, a ring
 * turning under them), and <i>sealing</i> (a ring closing in, the blade blazing), then the seal: a burst at the blade, a column of light,
 * a ring racing out, the banner. Anything that breaks the stance (moving, a blow, letting the blade go) breaks the ceremony. Everyone near
 * sees the whole of it; through the swordsman's own eyes the lines lie low on the ground ahead and everything round the body is left out.</p>
 *
 * <p><b>The passing.</b> A master holding their bonded blade in the breathing stance, and a disciple kneeling before them (sneaking within
 * {@link BladeRules#PASS_REACH} blocks, the two facing each other), whom the passing rules allow ({@link AuraApi#allowBladePassing}: step
 * 10's masters and disciples): eight seconds of their two colours braiding between them, then the blade passes into the disciple's hands
 * ({@link BondedBlades#pass}). Either one moving away, standing up or looking away breaks it.</p>
 */
public final class BladeCeremony {
	private BladeCeremony() {}

	/** A ceremony under way: which, when it began, the blade it began with, the crossing's two lines, and the disciple (a passing). */
	private record Rite(int kind, long start, ItemStack blade, double lineA, double lineB, UUID disciple) {}

	private static final Map<UUID, Rite> RITES = new HashMap<>();
	/** When each swordsman's last ceremony ended (the crossroads waits a moment after one). */
	private static final Map<UUID, Long> ENDED = new HashMap<>();
	/** The stance (by when it settled) a refusal was last said in, so it's said once a stance. */
	private static final Map<UUID, Long> TOLD = new HashMap<>();

	/** Whether a ceremony of {@code player}'s is under way, or only just ended (the crossroads waits). */
	public static boolean busy(ServerPlayer player) {
		if (RITES.containsKey(player.getUUID())) {
			return true;
		}
		Long ended = ENDED.get(player.getUUID());
		return ended != null && player.level().getGameTime() - ended < BladeRules.AFTER_CEREMONY;
	}

	/** Whether a ceremony of {@code player}'s is under way now (the game tests ask). */
	public static boolean underWay(ServerPlayer player) {
		return RITES.containsKey(player.getUUID());
	}

	// ------------------------------------------------------------------ the stance

	/** Called every tick {@code player} holds the breathing stance (from {@code Aura}'s stance). */
	static void breathing(ServerPlayer player, AuraAttachments.State state, long now) {
		Rite rite = RITES.get(player.getUUID());
		if (rite != null) {
			advance(player, rite, now);
			return;
		}
		if (now - state.settledAt() < BladeRules.SETTLE_BEFORE || !BondedBlades.on(player) || Aura.stage(player) < AuraRules.GLOW) {
			return;
		}
		ItemStack hand = player.getMainHandItem();
		BladeBond held = BondedBlades.bond(hand);
		if (held != null) {
			// Their own blade in hand: a disciple kneeling before them, whom the rules allow, begins the passing.
			if (held.ownedBy(player.getUUID()) && BondedBlades.standing(player)) {
				ServerPlayer disciple = kneeling(player);
				if (disciple != null && BondedBlades.passRefusal(player, disciple, true) == null) {
					begin(player, new Rite(BondedBlades.Rite.PASS, now, hand, 0, 0, disciple.getUUID()), now);
				}
			}
			return;
		}
		if (Aura.stage(player) < BladeRules.FROM || !BondedBlades.canBond(hand) || Crossroads.standing(player)) {
			return;
		}
		boolean power = !Config.get().aura().bonds().bondAtPower() || PowerPlaces.isPlaceOfPower(player.level(), player.blockPosition());
		if (!power) {
			return;
		}
		if (BondedBlades.standing(player)) {
			// Already bonded to another: said once a stance, here where it would have begun.
			Long told = TOLD.get(player.getUUID());
			if (told == null || told != state.settledAt()) {
				TOLD.put(player.getUUID(), state.settledAt());
				player.sendOverlayMessage(Component.translatable("message.wildercord.aura.blade.already").withColor(0xA89CC8));
			}
			return;
		}
		long seed = LeyWalker.seed(player.level());
		double[] lines = LeyLines.directions(seed, player.getX(), player.getZ());
		begin(player, new Rite(BondedBlades.Rite.BOND, now, hand, lines[0], lines[1], null), now);
	}

	/** The stance broke: so does any ceremony under way. */
	static void broken(ServerPlayer player) {
		Rite rite = RITES.get(player.getUUID());
		if (rite != null) {
			cancel(player, rite, "message.wildercord.aura.blade.slipped");
		}
	}

	/** Each tick: a passing whose disciple has gone, or whose master has stopped breathing, ends. */
	static void tick(MinecraftServer server) {
		if (RITES.isEmpty()) {
			return;
		}
		for (Map.Entry<UUID, Rite> e : Map.copyOf(RITES).entrySet()) {
			ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
			if (player == null || !player.isAlive()) {
				RITES.remove(e.getKey());
				continue;
			}
			// The stance keeps a ceremony going; one that went a tick without it (a stance lost some other way) ends.
			if (!Aura.state(player).breathing()) {
				cancel(player, e.getValue(), "message.wildercord.aura.blade.slipped");
			}
		}
	}

	private static void begin(ServerPlayer player, Rite rite, long now) {
		RITES.put(player.getUUID(), rite);
		int ticks = rite.kind() == BondedBlades.Rite.BOND ? BladeRules.BOND_TICKS : BladeRules.PASS_TICKS;
		int color = color(player);
		player.setAttached(BondedBlades.RITE, new BondedBlades.Rite(rite.kind(), now, ticks, color));
		ServerLevel level = player.level();
		if (rite.kind() == BondedBlades.Rite.BOND) {
			BondedBlades.sound(level, hand(player), "aura_bond_kindle", 0.9F, 1.0F);
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.blade.kindling").withColor(0xFF000000 | color));
		} else {
			ServerPlayer disciple = disciple(player, rite);
			if (disciple != null) {
				disciple.setAttached(BondedBlades.RITE, new BondedBlades.Rite(rite.kind(), now, ticks, color(disciple)));
				disciple.sendOverlayMessage(Component.translatable("message.wildercord.aura.blade.passing_to", Component.literal(player.getGameProfile().name()))
					.withColor(0xFF000000 | color));
			}
			BondedBlades.sound(level, hand(player), "aura_bond_kindle", 0.8F, 1.2F);
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.blade.passing").withColor(0xFF000000 | color));
		}
	}

	private static void end(ServerPlayer player, Rite rite) {
		RITES.remove(player.getUUID());
		ENDED.put(player.getUUID(), player.level().getGameTime());
		player.removeAttached(BondedBlades.RITE);
		ServerPlayer disciple = disciple(player, rite);
		if (disciple != null) {
			disciple.removeAttached(BondedBlades.RITE);
		}
	}

	private static void cancel(ServerPlayer player, Rite rite, String why) {
		end(player, rite);
		BondedBlades.sound(player.level(), hand(player), "aura_bond_fail", 0.7F, 1.0F);
		player.sendOverlayMessage(Component.translatable(why).withColor(0xC8A0A0));
		Motes.glows(player.level(), hand(player), 6, 0.2, AuraRules.mix(color(player), 0x6A6478, 0.5), 0.1, 20, new Vec3(0, -0.02, 0), 0.02);
	}

	private static void advance(ServerPlayer player, Rite rite, long now) {
		int t = (int) (now - rite.start());
		ItemStack hand = player.getMainHandItem();
		if (hand != rite.blade() || hand.isEmpty()) {
			cancel(player, rite, "message.wildercord.aura.blade.left_hand");
			return;
		}
		if (rite.kind() == BondedBlades.Rite.BOND) {
			if (!BondedBlades.canBond(hand) || BondedBlades.standing(player)) {
				cancel(player, rite, "message.wildercord.aura.blade.slipped");
				return;
			}
			bondLook(player, rite, t);
			if (t >= BladeRules.BOND_TICKS) {
				end(player, rite);
				if (BondedBlades.bond(player, net.minecraft.world.InteractionHand.MAIN_HAND, "ceremony")) {
					sealed(player);
				}
			}
		} else {
			ServerPlayer disciple = disciple(player, rite);
			if (disciple == null || !kneelsBefore(disciple, player) || BondedBlades.passRefusal(player, disciple, true) != null) {
				cancel(player, rite, "message.wildercord.aura.blade.pass_broken");
				if (disciple != null) {
					disciple.removeAttached(BondedBlades.RITE);
				}
				return;
			}
			passLook(player, disciple, t);
			if (t >= BladeRules.PASS_TICKS) {
				end(player, rite);
				BondedBlades.pass(player, disciple, true);
			}
		}
	}

	// ------------------------------------------------------------------ who kneels

	/** The player kneeling before {@code master} to receive their blade, or null. */
	private static ServerPlayer kneeling(ServerPlayer master) {
		double reach = BladeRules.PASS_REACH;
		ServerPlayer best = null;
		double nearest = Double.MAX_VALUE;
		for (ServerPlayer other : master.level().getEntitiesOfClass(ServerPlayer.class, master.getBoundingBox().inflate(reach))) {
			if (other == master || !kneelsBefore(other, master)) {
				continue;
			}
			double d = other.distanceToSqr(master);
			if (d < nearest) {
				nearest = d;
				best = other;
			}
		}
		return best;
	}

	/** Whether {@code disciple} kneels before {@code master}: alive, sneaking, near, and the two facing each other. */
	static boolean kneelsBefore(ServerPlayer disciple, ServerPlayer master) {
		if (!disciple.isAlive() || disciple.isSpectator() || !disciple.isShiftKeyDown() || disciple.level() != master.level()
				|| disciple.distanceToSqr(master) > BladeRules.PASS_REACH * BladeRules.PASS_REACH) {
			return false;
		}
		Vec3 toMaster = master.position().subtract(disciple.position()).multiply(1, 0, 1);
		if (toMaster.lengthSqr() < 1.0E-4) {
			return false;
		}
		toMaster = toMaster.normalize();
		Vec3 discipleLook = flat(disciple.getViewVector(1.0F));
		Vec3 masterLook = flat(master.getViewVector(1.0F));
		return discipleLook.dot(toMaster) >= BladeRules.PASS_FACING && masterLook.dot(toMaster.scale(-1)) >= BladeRules.PASS_FACING;
	}

	private static Vec3 flat(Vec3 v) {
		Vec3 f = new Vec3(v.x, 0, v.z);
		return f.lengthSqr() < 1.0E-6 ? Vec3.ZERO : f.normalize();
	}

	private static ServerPlayer disciple(ServerPlayer master, Rite rite) {
		if (rite.disciple() == null) {
			return null;
		}
		for (ServerPlayer other : master.level().getEntitiesOfClass(ServerPlayer.class, master.getBoundingBox().inflate(16))) {
			if (other.getUUID().equals(rite.disciple())) {
				return other;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------ how the bond looks

	/** Where a swordsman's blade is, near enough: their main hand, low in the stance. */
	static Vec3 hand(ServerPlayer player) {
		double yaw = Math.toRadians(player.yBodyRot);
		Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
		Vec3 right = new Vec3(-forward.z, 0, forward.x);
		double side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
		double up = player.isCrouching() ? 0.78 : 1.05;
		return player.position().add(0, up, 0).add(forward.scale(0.45)).add(right.scale(0.36 * side));
	}

	private static int color(ServerPlayer player) {
		int c = Aura.color(player);
		return c == 0 ? 0xD8D0F0 : c;
	}

	private static Vec3 line(double angle) {
		return new Vec3(Math.cos(angle), 0, Math.sin(angle));
	}

	/**
	 * The bond ceremony as it goes: kindling (motes of the aura rising out of the ground along the two lines into the blade), joining (the
	 * two ley lines lighting across the ground, running in to the swordsman, a ring turning under them), sealing (a ring closing in, the
	 * blade blazing). Redrawn every few ticks; what lies round the body is spectacle (the swordsman sees it in third person only).
	 */
	private static void bondLook(ServerPlayer player, Rite rite, int t) {
		ServerLevel level = player.level();
		int color = color(player);
		int hot = AuraVfx.hot(color, 0.45);
		Vec3 feet = player.position();
		Vec3 blade = hand(player);
		Vec3 a = line(rite.lineA());
		Vec3 b = line(rite.lineB());
		int phase = BladeRules.phase(t);
		ArtLight world = ArtLight.world(player);
		ArtLight show = ArtLight.spectacle(player);
		if (t % 20 == 0) {
			// Each second the stance draws a breath: the body's aura swells a little more as it goes.
			AuraFx.bodyAuraFlare(player, 22, Math.min(1.0F, 0.25F + 0.004F * t));
		}
		// ---- motes rising out of the ground along the lines into the blade, faster as it goes.
		int every = phase == 0 ? 3 : phase == 1 ? 2 : 1;
		if (t % every == 0) {
			Vec3 dir = level.getRandom().nextBoolean() ? a : b;
			double sign = level.getRandom().nextBoolean() ? 1 : -1;
			double out = phase == 0 ? 2.2 + level.getRandom().nextDouble() * 2.4 : 3.0 + level.getRandom().nextDouble() * (phase == 1 ? 4.0 : 2.0);
			Vec3 from = feet.add(dir.scale(out * sign)).add(0, 0.1, 0);
			Motes.seek(level, from, blade, level.getRandom().nextInt(3) == 0 ? hot : color, 0.1 + 0.03 * phase, 18 + level.getRandom().nextInt(8), 0.4);
		}
		if (phase == 0 && t % 10 == 0) {
			show.groundRing(feet, color, 0.6, 1.8, 0.05, 14);
		}
		// ---- joining: the two lines lit across the ground, reaching further, running in to the swordsman.
		if (phase >= 1 && t % 4 == 0) {
			double grown = Math.min(1, (t - BladeRules.KINDLE_END) / 40.0);
			double reach = 1.6 + 5.4 * grown;
			for (Vec3 dir : new Vec3[] {a, b}) {
				int lit = dir == a ? color : AuraRules.mix(color, 0xFFFFFF, 0.3);
				for (int sign = -1; sign <= 1; sign += 2) {
					// The swordsman sees the lines from a little way out (the ground under their own eyes stays clear); everyone else
					// sees them run right in to the body.
					Vec3 near = feet.add(dir.scale(1.2 * sign)).add(0, 0.06, 0);
					Vec3 mid = feet.add(dir.scale(Math.min(reach, 2.6) * sign)).add(0, 0.06, 0);
					Vec3 far = feet.add(dir.scale(reach * sign)).add(0, 0.06, 0);
					if (reach > 2.6) {
						world.ray(far, mid, lit, 0.09 + 0.03 * phase, 6);
					}
					show.ray(mid, near, lit, 0.09 + 0.03 * phase, 6);
				}
			}
			if (t % 8 == 0) {
				show.ground(feet, SigilOption.BAND, color, 2.6 + 0.4 * phase, 10, 0.4);
			}
		}
		if (phase == 1 && t == BladeRules.KINDLE_END) {
			BondedBlades.sound(level, blade, "aura_bond_join", 0.9F, 1.0F);
			player.sendOverlayMessage(Component.translatable("message.wildercord.aura.blade.joining").withColor(0xFF000000 | color));
		}
		// ---- sealing: a ring closing in, the blade blazing.
		if (phase == 2) {
			if (t % 10 == 0) {
				show.groundRing(feet, hot, 4.6, 0.7, 0.08, 10);
				world.groundRing(feet, color, 5.2, 2.4, 0.06, 10);
				AuraFx.burst(level, player, blade, Vec3.ZERO, color, 0.5F + 0.004F * (t - BladeRules.JOIN_END), AuraFx.Burst.FLASH);
			}
			if (t == BladeRules.JOIN_END) {
				player.sendOverlayMessage(Component.translatable("message.wildercord.aura.blade.sealing").withColor(0xFF000000 | color));
			}
		}
	}

	/** The seal: a burst at the blade, a column of light, a ring racing out, the banner, and what it means said in chat. */
	private static void sealed(ServerPlayer player) {
		ServerLevel level = player.level();
		int color = color(player);
		int hot = AuraVfx.hot(color, 0.5);
		Vec3 feet = player.position();
		Vec3 blade = hand(player);
		BondedBlades.sound(level, blade, "aura_bond_seal", 1.0F, 1.0F);
		AuraFx.burst(level, player, blade, Vec3.ZERO, color, 1.5F, AuraFx.Burst.FLASH | AuraFx.Burst.STAR | AuraFx.Burst.SPARKS);
		ArtLight.spectacle(player).ray(feet.add(0, 0.1, 0), feet.add(0, 7.5, 0), color, 0.32, 18).ray(feet.add(0, 0.1, 0), feet.add(0, 6.0, 0), hot, 0.12, 16)
			.groundRing(feet, color, 0.8, 6.5, 0.18, 14);
		ArtLight.world(player).ray(feet.add(0, 3.0, 0), feet.add(0, 9.0, 0), color, 0.22, 16).groundRing(feet, hot, 2.4, 7.5, 0.1, 14);
		Motes.burst(level, blade, 18, color, 0.14, 30, 0.12);
		AuraFx.bodyAuraFlare(player, 50, 1.0F);
		ItemStack stack = player.getMainHandItem();
		BladeBond b = BondedBlades.bond(stack);
		Component name = b == null ? Component.translatable(stack.getItem().getDescriptionId()) : BondedBlades.name(stack, b);
		AuraFx.banner(player, Component.translatable("aura.wildercord.blade.bonded"), name, color, AuraFxRules.BannerKind.GRAND);
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.blade.bonded", name.copy().withColor(0xFF000000 | color),
			Component.translatable(biomeKey(b == null ? "" : b.origin().biome())), b == null ? 1 : b.origin().day()).withColor(0xE8D8B0));
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.blade.bonded_how").withColor(0xB8A8D8));
	}

	/** A biome's language key from its id ("minecraft:birch_forest": biome.minecraft.birch_forest). */
	public static String biomeKey(String biome) {
		if (biome == null || biome.isEmpty() || !biome.contains(":")) {
			return "aura.wildercord.blade.somewhere";
		}
		return "biome." + biome.replace(':', '.');
	}

	// ------------------------------------------------------------------ how the passing looks

	/** The passing as it goes: the master's colour flowing to the disciple and the disciple's back, braided, a ring under them. */
	private static void passLook(ServerPlayer master, ServerPlayer disciple, int t) {
		ServerLevel level = master.level();
		int mc = color(master);
		int dc = color(disciple);
		Vec3 from = hand(master);
		Vec3 to = disciple.position().add(0, disciple.isCrouching() ? 0.9 : 1.2, 0);
		if (t % 3 == 0) {
			Motes.seek(level, from, to, mc, 0.11, 20, 0.6);
			Motes.seek(level, to, from, dc, 0.09, 20, -0.6);
		}
		Vec3 middle = master.position().add(disciple.position()).scale(0.5);
		int both = AuraRules.mix(mc, dc, 0.5);
		float grown = Math.min(1.0F, t / (float) BladeRules.PASS_TICKS);
		if (t % 8 == 0) {
			ArtLight.world(master).groundRing(middle, both, 0.9, 1.7, 0.06, 10);
		}
		if (t % 10 == 0) {
			// A circle drawn round the two of them, widening as the passing goes.
			ArtLight.spectacle(master).ground(middle, SigilOption.BAND, both, 2.4 + 1.2 * grown, 12, 0.3);
		}
		if (t % 20 == 0) {
			AuraFx.bodyAuraFlare(master, 22, 0.5F);
			AuraFx.bodyAuraFlare(disciple, 22, 0.3F + 0.4F * grown);
		}
		if (t >= BladeRules.PASS_TICKS / 2 && t % 4 == 0) {
			// The blade's light leaving the master's hand for the disciple's.
			ArtLight.spectacle(master).ray(from, to, mc, 0.07, 6).ray(from, to, AuraVfx.hot(mc, 0.5), 0.025, 6);
		}
		if (t == BladeRules.PASS_TICKS / 2) {
			BondedBlades.sound(level, middle.add(0, 1, 0), "aura_bond_join", 0.8F, 1.2F);
		}
	}

	// ------------------------------------------------------------------ the blade's moments

	/** {@code b} reached {@code tier}: a burst at the blade, the body's aura flaring, its voice, the banner, and what it means. */
	static void tierMoment(ServerPlayer player, BladeBond b, int tier) {
		ServerLevel level = player.level();
		int color = b.color() == 0 ? color(player) : b.color();
		Vec3 blade = hand(player);
		ItemStack stack = BondedBlades.carried(player);
		Component name = BondedBlades.name(stack.isEmpty() ? player.getMainHandItem() : stack, b);
		Component tierName = Component.translatable("aura.wildercord.blade.tier." + BladeRules.tierId(tier));
		BondedBlades.sound(level, blade, "aura_bond_tier", 1.0F, 0.85F + 0.08F * tier);
		AuraFx.burst(level, player, blade, Vec3.ZERO, color, 0.9F + 0.25F * tier, AuraFx.Burst.FLASH | AuraFx.Burst.RING | AuraFx.Burst.STAR);
		AuraFx.bodyAuraFlare(player, 30 + 10 * tier, Math.min(1.0F, 0.5F + 0.15F * tier));
		Motes.burst(level, blade, 8 + 4 * tier, color, 0.12, 26, 0.1);
		if (tier >= BladeRules.SOULFORGED) {
			Vec3 feet = player.position();
			ArtLight.spectacle(player).ray(feet.add(0, 0.1, 0), feet.add(0, 8.0, 0), color, 0.36, 20).groundRing(feet, AuraVfx.hot(color, 0.4), 0.8, 7.0, 0.2, 16);
			ArtLight.world(player).ray(feet.add(0, 3.0, 0), feet.add(0, 10.0, 0), color, 0.24, 18);
		}
		AuraFx.banner(player, name, tierName, color, tier >= BladeRules.AWAKENED ? AuraFxRules.BannerKind.GRAND : AuraFxRules.BannerKind.ART);
		Component coloured = name.copy().withColor(0xFF000000 | color);
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.blade.tier." + BladeRules.tierId(tier), coloured).withColor(0xE8D8B0));
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.blade.tier." + BladeRules.tierId(tier) + ".how").withColor(0xB8A8D8));
	}

	/** A name given on the page. */
	static void namedMoment(ServerPlayer player, BladeBond b) {
		int color = b.color() == 0 ? color(player) : b.color();
		Vec3 blade = hand(player);
		BondedBlades.sound(player.level(), blade, "aura_bond_name", 0.8F, 1.0F);
		Motes.glows(player.level(), blade, 8, 0.2, color, 0.1, 26, new Vec3(0, 0.02, 0), 0.02);
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.blade.named", Component.literal(b.shownName()).withColor(0xFF000000 | color))
			.withColor(0xE8D8B0));
	}

	/** A trait chosen on the page. */
	static void traitMoment(ServerPlayer player, BladeBond b) {
		int color = b.color() == 0 ? color(player) : b.color();
		Vec3 blade = hand(player);
		BondedBlades.sound(player.level(), blade, "aura_bond_trait", 0.9F, 1.0F);
		AuraFx.burst(player.level(), player, blade, Vec3.ZERO, color, 0.9F, AuraFx.Burst.FLASH | AuraFx.Burst.RING);
		AuraFx.bodyAuraFlare(player, 26, 0.6F);
		ItemStack stack = BondedBlades.carried(player);
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.blade.traited", BondedBlades.name(stack.isEmpty() ? player.getMainHandItem() : stack, b)
			.copy().withColor(0xFF000000 | color), Component.translatable(BladeRules.trait(b.growth().trait()).map(BladeRules.Trait::nameKey).orElse(""))
			.withColor(0xFFE8C46A)).withColor(0xE8D8B0));
	}

	/** A bond released. */
	static void releaseMoment(ServerPlayer player, Component name, int color) {
		Vec3 at = player.position().add(0, 1.0, 0);
		BondedBlades.sound(player.level(), at, "aura_bond_release", 0.9F, 1.0F);
		Motes.glows(player.level(), at, 10, 0.4, AuraRules.mix(color == 0 ? 0xC8C0E0 : color, 0x6A6478, 0.4), 0.12, 30, new Vec3(0, -0.015, 0), 0.02);
		player.sendSystemMessage(Component.translatable("message.wildercord.aura.blade.released", name.copy().withColor(0xFF000000 | (color == 0 ? 0xC8C0E0 : color)))
			.withColor(0xC8B89A));
	}

	/** A blade passed from {@code from} to {@code to}. */
	static void passedMoment(ServerPlayer from, ServerPlayer to, BladeBond b) {
		ServerLevel level = to.level();
		int color = b.color() == 0 ? color(to) : b.color();
		Vec3 at = hand(to);
		BondedBlades.sound(level, at, "aura_bond_pass", 1.0F, 1.0F);
		AuraFx.burst(level, to, at, Vec3.ZERO, color, 1.2F, AuraFx.Burst.FLASH | AuraFx.Burst.STAR | AuraFx.Burst.RING);
		Motes.seek(level, hand(from), at, color(from), 0.14, 14, 0.2);
		AuraFx.bodyAuraFlare(to, 40, 0.9F);
		Vec3 feet = to.position();
		ArtLight.spectacle(to).ray(feet.add(0, 0.1, 0), feet.add(0, 5.5, 0), color, 0.22, 16).groundRing(feet, AuraVfx.hot(color, 0.4), 0.6, 4.0, 0.12, 14);
		ItemStack stack = to.getMainHandItem();
		Component name = BondedBlades.name(BondedBlades.bonded(stack) ? stack : BondedBlades.carried(to), b).copy().withColor(0xFF000000 | color);
		AuraFx.banner(to, Component.translatable("aura.wildercord.blade.passed_to_you"), name, color, AuraFxRules.BannerKind.GRAND);
		AuraFx.banner(from, Component.translatable("aura.wildercord.blade.passed_on"), name, color(from), AuraFxRules.BannerKind.ART);
		to.sendSystemMessage(Component.translatable("message.wildercord.aura.blade.passed_to_you", name, Component.literal(from.getGameProfile().name()))
			.withColor(0xE8D8B0));
		from.sendSystemMessage(Component.translatable("message.wildercord.aura.blade.passed_on", name, Component.literal(to.getGameProfile().name()))
			.withColor(0xE8D8B0));
		if (BladeRules.effective(b.tier(), Aura.stage(to)) < b.tier()) {
			to.sendSystemMessage(Component.translatable("message.wildercord.aura.blade.passed_sleeps").withColor(0xB8A8D8));
		}
	}

	/** A blade going home: a puff where it left, a streak toward its swordsman if they're near, and its voice at both ends. */
	static void homeFx(ServerLevel level, Vec3 from, ServerPlayer owner, int color) {
		int c = color == 0 ? 0xC8C0E0 : color;
		Motes.burst(level, from, 10, c, 0.12, 22, 0.08);
		BondedBlades.sound(level, from, "aura_bond_home", 0.8F, 0.85F);
		if (owner != null && owner.level() == level) {
			Vec3 to = owner.position().add(0, 1.0, 0);
			if (to.distanceToSqr(from) < 48 * 48 && to.distanceToSqr(from) > 1.0) {
				Motes.seek(level, from, to, c, 0.14, 16, 0.3);
				ArtLight.world(owner).ray(from, to, AuraVfx.hot(c, 0.3), 0.05, 6);
			}
			Motes.glows(level, to, 8, 0.25, c, 0.1, 22, new Vec3(0, 0.02, 0), 0.02);
			BondedBlades.sound(level, to, "aura_bond_home", 0.8F, 1.15F);
		}
	}

	static void forget(UUID id) {
		RITES.remove(id);
		ENDED.remove(id);
		TOLD.remove(id);
	}

	static void clear() {
		RITES.clear();
		ENDED.clear();
		TOLD.clear();
	}
}
