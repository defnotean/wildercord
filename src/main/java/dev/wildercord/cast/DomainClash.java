package dev.wildercord.cast;

import dev.wildercord.content.SigilOption;
import dev.wildercord.player.Heart;
import dev.wildercord.spell.Feats;
import dev.wildercord.spell.SpellNumbers;
import dev.wildercord.spell.SpellPlan;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain clashes: two casters' Domains can't overlap. When a Domain opens into another one, the
 * two push against each other for a moment and the weaker one shatters. Strength is the Domain's
 * power (circles, Cord enchantments, Focus and Vow) times its size, so a bigger heart wins.
 */
public final class DomainClash {
	private DomainClash() {}

	private record Domain(Cast cast, ServerLevel level, Vec3 center, double radius, double strength, int color, long until) {
		boolean alive() {
			return cast.alive() && level.getGameTime() <= until;
		}
	}

	private static final List<Domain> ACTIVE = new ArrayList<>();

	public static void init() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
	}

	static double strength(Cast cast, SpellPlan.Group g, double radius) {
		int circles = cast.caster instanceof ServerPlayer player ? Heart.active(player) : 4;
		return cast.power * SpellNumbers.groupPower(g) * (1 + 0.1 * circles) * Math.sqrt(radius / 9.0);
	}

	/** Called as a Domain opens. */
	static void open(Cast cast, SpellPlan.Group g, Vec3 center, double radius, int ticks, int color) {
		ACTIVE.removeIf(d -> !d.alive());
		Domain mine = new Domain(cast, cast.level, center, radius, strength(cast, g, radius), color, cast.level.getGameTime() + ticks + 20);
		for (Domain other : List.copyOf(ACTIVE)) {
			if (other.level() == cast.level && other.cast().caster != cast.caster && other.center().distanceTo(center) < other.radius() + radius) {
				clash(mine, other);
			}
		}
		ACTIVE.add(mine);
	}

	private static void clash(Domain a, Domain b) {
		ServerLevel level = a.level();
		// The incumbent holds a tie.
		Domain winner = a.strength() > b.strength() ? a : b;
		Domain loser = winner == a ? b : a;
		Vec3 ab = b.center().subtract(a.center());
		double d = Math.max(0.1, ab.length());
		Vec3 dir = ab.scale(1 / d);
		// Where the two shells meet.
		double along = Math.max(0, Math.min(d, (d + a.radius() - b.radius()) / 2));
		Vec3 front = a.center().add(dir.scale(along)).add(0, 1.5, 0);
		Vec3 side = dir.cross(new Vec3(0, 1, 0)).normalize();
		Component message = Component.translatable("message.wildercord.domain_clash").withColor(0xFFF0C0).withStyle(ChatFormatting.BOLD);
		Casters.tell(a.cast().caster, message);
		Casters.tell(b.cast().caster, message);
		// Where they meet, each domain's circle stands against the other's.
		float size = (float) Math.min(4.5, Math.min(a.radius(), b.radius()) * 0.45);
		Sigils.layer(level, front.subtract(dir.scale(0.08)), dir, dev.wildercord.content.SigilOption.CIRCLE, a.color(), size, 34, 0.09F);
		Sigils.layer(level, front.add(dir.scale(0.08)), dir.scale(-1), dev.wildercord.content.SigilOption.CIRCLE, b.color(), size, 34, -0.09F);
		Sigils.layer(level, front, dir, dev.wildercord.content.SigilOption.RING, 0xFFF4D0, size * 1.3F, 34, 0.2F);
		Fx.sound(level, front, SoundEvents.WARDEN_SONIC_CHARGE, 1.4F, 1.2F);
		Fx.sound(level, front, SoundEvents.BELL_RESONATE, 1.2F, 0.6F);
		double width = Math.min(a.radius(), b.radius()) * 0.9;
		for (int t = 0; t < 30; t += 2) {
			int tick = t;
			Scheduler.later(t + 1, () -> {
				// Where the shells grind: lightning of both colours crackling up and down the front, a band of
				// light sweeping it, and each side's rings shoving at the other.
				for (int k = 0; k < 2; k++) {
					double s = level.getRandom().nextDouble() * 2 - 1;
					double drift = (level.getRandom().nextDouble() - 0.5) * 0.3;
					ElementFx.bolt(level, front.add(side.scale(s * width)).add(0, 2.5, 0), front.add(side.scale((s + drift) * width)).add(0, -1.4, 0), 0.06, 1, 1,
						k == 0 ? a.color() : b.color(), 0);
				}
				double y = Math.sin(tick * 0.4) * 2.2;
				Light.ray(level, front.subtract(side.scale(width)).add(0, y, 0), front.add(side.scale(width)).add(0, y, 0), tick % 4 == 0 ? a.color() : b.color(),
					0.08, 4);
				if (tick % 6 == 0) {
					Light.ring(level, front.subtract(dir.scale(0.3)), dir, a.color(), 0.5, size * 1.3, 0.07, 8);
					Light.ring(level, front.add(dir.scale(0.3)), dir, b.color(), 0.5, size * 1.3, 0.07, 8);
				}
				Vfx.emit(level, ParticleTypes.ELECTRIC_SPARK, front, 6, 1.2, 0.2);
				if (tick % 6 == 0) {
					Fx.sound(level, front, SoundEvents.AMETHYST_BLOCK_HIT, 1.2F, 0.5F + tick / 40F);
				}
			});
		}
		Scheduler.later(32, () -> {
			if (!loser.cast().alive()) {
				return;
			}
			loser.cast().cancel();
			shatter(level, loser);
			Casters.tell(winner.cast().caster, Component.translatable("message.wildercord.domain_holds").withColor(winner.color()).withStyle(ChatFormatting.BOLD));
			Casters.tell(loser.cast().caster, Component.translatable("message.wildercord.domain_shattered").withStyle(ChatFormatting.RED));
			Grimoire.feat(winner.cast().caster, Feats.CLASH);
		});
	}

	/** A Domain breaking like glass: shards rain from its shell. */
	private static void shatter(ServerLevel level, Domain domain) {
		Vec3 c = domain.center();
		double r = domain.radius();
		ItemParticleOption glass = new ItemParticleOption(ParticleTypes.ITEM, Items.GLASS_PANE);
		// The dome falls in on itself in light: its meridians and parallels collapse to the middle.
		double line = 0.08 + r * 0.006;
		for (int m = 0; m < 4; m++) {
			double a = Math.PI * m / 4;
			Light.ring(level, c, new Vec3(Math.cos(a), 0, Math.sin(a)), domain.color(), r, r * 0.05, line, 12);
		}
		for (double lat : new double[] {0.3, 0.6}) {
			double rr = r * Math.cos(lat * Math.PI / 2);
			Light.ring(level, c.add(0, r * Math.sin(lat * Math.PI / 2), 0), new Vec3(0, 1, 0), 0xFFFFFF, rr, rr * 0.05, line, 12);
		}
		Sigils.flash(level, c.add(0, 1.5, 0), domain.color(), (float) Math.min(8, r));
		for (int i = 0; i < 100; i++) {
			double y = level.getRandom().nextDouble();
			double a = level.getRandom().nextDouble() * Math.PI * 2;
			double rr = Math.sqrt(1 - y * y) * r;
			Vec3 p = c.add(Math.cos(a) * rr, y * r, Math.sin(a) * rr);
			Fx.sendFar(level, i % 3 == 0 ? glass : new DustParticleOptions(domain.color(), 2.0F), p);
		}
		Sigils.send(level, dev.wildercord.content.SigilOption.flat(dev.wildercord.content.SigilOption.CRACKED, domain.color(), (float) Math.min(r, 12), 40, 0.0F),
			CastEngine.ground(level, c).add(0, 0.08, 0));
		Fx.sound(level, c, dev.wildercord.content.WildercordSounds.SHIELD_BREAK, 2.0F, 1.0F);
		Fx.sound(level, c, dev.wildercord.content.WildercordSounds.DOMAIN_CLOSE, 1.5F, 1.0F);
	}
}
