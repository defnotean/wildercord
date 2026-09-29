package dev.wildercord.cast.feel;

/** Your own presentation for a {@link Phase}. Send particles through {@code Fx}, {@code Vfx}, {@code ElementFx}, {@code Light} and {@code Sigils}, as always. */
@FunctionalInterface
public interface Hook {
	void run(FeelCtx ctx);
}
