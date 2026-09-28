package dev.wildercord.api;

import dev.wildercord.spell.EffectKind;
import dev.wildercord.spell.RuneDef;
import dev.wildercord.spell.RuneFamily;
import dev.wildercord.spell.Trait;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * An example add-on, as it would sit in another mod (see docs/API.md): an effect, a modifier, a
 * shape, a link, a Codex category, an element reaction and an event listener. The unit tests register
 * it to check that everything an add-on does goes through the API cleanly.
 *
 * <p>In a real add-on this class is listed in {@code fabric.mod.json} under
 * {@code "entrypoints": {"wildercord": ["com.example.ExampleAddon"]}}.</p>
 */
public class ExampleAddon implements WildercordAddon {
	public static final String SOAKED = "example:soaked";

	@Override
	public void onWildercordInit(WildercordApi api) {
		api.registerCategory(RuneFamily.EFFECT, "weather");

		// Drench: soaks harmed creatures (for Steam, below) and slows them.
		api.effect("example:drench", "Drench", "frost", EffectKind.HARMFUL)
			.tier(1).cost(5).traits(Trait.DURATION).category("weather")
			.description("Soaks targets for 6 seconds and slows them.")
			.onApply(ctx -> {
				for (LivingEntity target : ctx.harmed()) {
					api.mark(target, SOAKED, (int) Math.round(120 * ctx.duration()));
					target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, (int) Math.round(120 * ctx.duration()), 0));
				}
			})
			.register();

		// Steam: fire damage on a soaked creature bursts into steam for +40%.
		api.registerReaction("example:steam", "fire", (caster, target) -> {
			if (!api.hasMark(target, SOAKED)) {
				return 1.0;
			}
			api.clearMark(target, SOAKED);
			return 1.4;
		});

		// Brutal: a modifier for anything with power: x1.8 power for x1.5 cost.
		api.modifier("example:brutal", "Brutal").tier(2).multiplier(1.5).needs(Trait.POWER).numbers(1.8, 1.0, 1.0)
			.description("+80% power.").register();

		// Halo: a shape that hits everything within 3 blocks of the caster, twice, a second apart.
		RuneDef halo = api.shape("example:halo", "Halo").tier(2).cost(5).multiplier(1.8).traits(Trait.RADIUS).category("area")
			.description("Hits everything within 3 blocks of you, twice, a second apart.")
			.onDeliver(ctx -> {
				for (int i = 0; i < 2; i++) {
					ShapeContext strike = ctx.pulse();
					strike.later(1 + 20 * i, () -> {
						if (strike.alive()) {
							Vec3 centre = strike.caster().position().add(0, 1, 0);
							strike.hit(strike.near(centre, 3.0 * strike.radius()).stream().filter(e -> e != strike.caster()).toList(), centre, null, null);
						}
					});
				}
			})
			.register();

		// At Dusk: the rest fires two seconds later, where the caster then stands.
		api.link("example:at_dusk", "At Dusk").tier(2).cost(2)
			.description("The rest fires two seconds later, from wherever you are.")
			.onLink(ctx -> ctx.later(40, () -> {
				if (ctx.alive()) {
					ctx.fireAt(ctx.caster().getEyePosition(), ctx.caster().getLookAngle(), ctx.caster());
				}
			}))
			.register();

		// Listening: count the casts of anything with a Halo in it.
		WildercordEvents.AFTER_CAST.register((player, spell, runes, spent) -> {
			if (runes.contains(halo)) {
				haloCasts++;
			}
		});
	}

	static int haloCasts;
}
