package dev.wildercord.gametest;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

/** Exact legal RNG inputs for two native Glow ticks; never installed on an entity. */
public final class AuraBodyMoteScript implements RandomSource {
	private int cursor;
	private static final String[] CALLS = {"float", "float", "double", "double", "int:14", "int:3", "float", "double"};
	private static final double[] VALUES = {0.5, 0.0, 0.25, 0.5, 0, 1, 0.5, 0.5};

	private double take(String call) {
		if (cursor >= CALLS.length || !CALLS[cursor].equals(call)) {
			throw new AssertionError("Glow script draw " + cursor + ": expected "
				+ (cursor == CALLS.length ? "end" : CALLS[cursor]) + ", got " + call);
		}
		return VALUES[cursor++];
	}

	public int consumed() { return cursor; }
	public boolean exhausted() { return cursor == CALLS.length; }
	@Override public float nextFloat() { return (float) take("float"); }
	@Override public double nextDouble() { return take("double"); }
	@Override public int nextInt(int bound) { return (int) take("int:" + bound); }
	private AssertionError forbidden(String operation) { return new AssertionError("Unexpected Glow script operation: " + operation); }
	@Override public RandomSource fork() { throw forbidden("fork"); }
	@Override public PositionalRandomFactory forkPositional() { throw forbidden("forkPositional"); }
	@Override public void setSeed(long seed) { throw forbidden("setSeed"); }
	@Override public int nextInt() { throw forbidden("nextInt"); }
	@Override public long nextLong() { throw forbidden("nextLong"); }
	@Override public boolean nextBoolean() { throw forbidden("nextBoolean"); }
	@Override public double nextGaussian() { throw forbidden("nextGaussian"); }
}
