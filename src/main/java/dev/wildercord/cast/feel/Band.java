package dev.wildercord.cast.feel;

/** How big a cast should read: S, M (today's sizes), L, XL. From cost, charge and tier. */
public enum Band {
	S(0.7F), M(1.0F), L(1.15F), XL(1.3F);

	/** Volume factor for sounds of this band. */
	public final float volume;

	Band(float volume) {
		this.volume = volume;
	}

	public static Band of(double scale) {
		return scale < 0.85 ? S : scale < 1.2 ? M : scale < 1.6 ? L : XL;
	}
}
