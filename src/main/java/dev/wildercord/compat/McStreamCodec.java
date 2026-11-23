package dev.wildercord.compat;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Variadic {@code StreamCodec.composite} for 1.21.1, which only ships composite up to 6 arguments.
 * The 1.21.2+ call sites pass the field codecs and getters plus the record constructor as trailing
 * arguments; this helper drives the same shape and is only used where the record has more than
 * six components.
 */
public final class McStreamCodec {
	private McStreamCodec() {}

	/**
	 * @param args codec, getter, codec, getter, ... at odd positions, the constructor as the last element
	 */
	@SuppressWarnings("unchecked")
	public static <B, C> StreamCodec<B, C> composite(Object... args) {
		final int arity = (args.length - 1) / 2;
		final StreamCodec<? super B, Object>[] codecs = new StreamCodec[arity];
		final Function<C, Object>[] getters = new Function[arity];
		for (int i = 0; i < arity; i++) {
			codecs[i] = (StreamCodec<? super B, Object>) args[2 * i];
			getters[i] = (Function<C, Object>) args[2 * i + 1];
		}
		final Object ctor = args[args.length - 1];

		return new StreamCodec<B, C>() {
			@Override
			public void encode(B buf, C value) {
				for (int i = 0; i < arity; i++) {
					codecs[i].encode(buf, getters[i].apply(value));
				}
			}

			@Override
			public C decode(B buf) {
				Object[] values = new Object[arity];
				for (int i = 0; i < arity; i++) {
					values[i] = codecs[i].decode(buf);
				}
				return invokeConstructor(ctor, values);
			}
		};
	}

	@SuppressWarnings("unchecked")
	private static <C> C invokeConstructor(Object ctor, Object[] values) {
		// The trailing argument is a method reference (a functional interface instance such as
		// Function6/7/8). Find its apply method and call it with the deserialized values.
		try {
			Class<?> cls = ctor.getClass();
			for (Method m : cls.getMethods()) {
				if (m.getName().equals("apply") && m.getParameterCount() == values.length) {
					return (C) m.invoke(ctor, (Object[]) values);
				}
			}
			Class<?>[] iface = cls.getInterfaces();
			for (Class<?> i : iface) {
				for (Method m : i.getMethods()) {
					if (m.getName().equals("apply") && m.getParameterCount() == values.length) {
						return (C) m.invoke(ctor, values);
					}
				}
			}
			throw new IllegalStateException("No apply method found for " + cls.getName() + " with " + values.length + " args");
		} catch (IllegalAccessException | IllegalArgumentException | java.lang.reflect.InvocationTargetException e) {
			throw new IllegalStateException("Failed to invoke constructor " + ctor.getClass().getName(), e);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to resolve constructor " + ctor.getClass().getName(), e);
		}
	}
}
