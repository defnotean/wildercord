package dev.wildercord.compat;

import com.mojang.datafixers.util.Function7;
import com.mojang.datafixers.util.Function8;
import net.minecraft.network.codec.StreamCodec;

import java.util.function.Function;

/**
 * Variadic {@code StreamCodec.composite} for records with more than six components.
 * 1.21.1 only ships composite overloads up to six components; the 1.21.2+ call sites pass the field
 * codecs and getters plus the record constructor as trailing arguments and use this helper instead.
 */
public final class McStreamCodec {
	private McStreamCodec() {}

	/**
	 * @param args codec, getter, codec, getter, e.g. at odd positions, the record constructor as the last element
	 */
	@SuppressWarnings("unchecked")
	public static <B, C, T1, T2, T3, T4, T5, T6, T7> StreamCodec<B, C> composite7(
		StreamCodec<? super B, T1> c1, Function<C, T1> g1,
		StreamCodec<? super B, T2> c2, Function<C, T2> g2,
		StreamCodec<? super B, T3> c3, Function<C, T3> g3,
		StreamCodec<? super B, T4> c4, Function<C, T4> g4,
		StreamCodec<? super B, T5> c5, Function<C, T5> g5,
		StreamCodec<? super B, T6> c6, Function<C, T6> g6,
		StreamCodec<? super B, T7> c7, Function<C, T7> g7,
		Function7<T1, T2, T3, T4, T5, T6, T7, C> ctor) {
		return new StreamCodec<B, C>() {
			@Override
			public void encode(B buf, C value) {
				c1.encode(buf, g1.apply(value));
				c2.encode(buf, g2.apply(value));
				c3.encode(buf, g3.apply(value));
				c4.encode(buf, g4.apply(value));
				c5.encode(buf, g5.apply(value));
				c6.encode(buf, g6.apply(value));
				c7.encode(buf, g7.apply(value));
			}
			@Override
			public C decode(B buf) {
				return ctor.apply(
					c1.decode(buf), c2.decode(buf), c3.decode(buf), c4.decode(buf),
					c5.decode(buf), c6.decode(buf), c7.decode(buf));
			}
		};
	}

	/**
	 * @param args codec, getter, codec, getter, e.g. at odd positions, the record constructor as the last element
	 */
	@SuppressWarnings("unchecked")
	public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8> StreamCodec<B, C> composite8(
		StreamCodec<? super B, T1> c1, Function<C, T1> g1,
		StreamCodec<? super B, T2> c2, Function<C, T2> g2,
		StreamCodec<? super B, T3> c3, Function<C, T3> g3,
		StreamCodec<? super B, T4> c4, Function<C, T4> g4,
		StreamCodec<? super B, T5> c5, Function<C, T5> g5,
		StreamCodec<? super B, T6> c6, Function<C, T6> g6,
		StreamCodec<? super B, T7> c7, Function<C, T7> g7,
		StreamCodec<? super B, T8> c8, Function<C, T8> g8,
		Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> ctor) {
		return new StreamCodec<B, C>() {
			@Override
			public void encode(B buf, C value) {
				c1.encode(buf, g1.apply(value));
				c2.encode(buf, g2.apply(value));
				c3.encode(buf, g3.apply(value));
				c4.encode(buf, g4.apply(value));
				c5.encode(buf, g5.apply(value));
				c6.encode(buf, g6.apply(value));
				c7.encode(buf, g7.apply(value));
				c8.encode(buf, g8.apply(value));
			}
			@Override
			public C decode(B buf) {
				return ctor.apply(
					c1.decode(buf), c2.decode(buf), c3.decode(buf), c4.decode(buf),
					c5.decode(buf), c6.decode(buf), c7.decode(buf), c8.decode(buf));
			}
		};
	}
}
