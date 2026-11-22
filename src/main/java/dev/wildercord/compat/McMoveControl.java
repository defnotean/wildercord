package dev.wildercord.compat;

import java.lang.reflect.Field;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * Parks a mob's move control. 1.21.6 added {@code MoveControl#setWait}; 1.21.5 keeps its operation
 * enum protected, so the field is found reflectively. The field is located by its enum type rather
 * than by name so the intermediary remap cannot break it, and the constant is matched by enum name,
 * which survives remapping.
 */
public final class McMoveControl {

	private static final Field OPERATION;
	private static final Object WAIT;

	static {
		Field field = null;
		Object wait = null;
		for (Field candidate : MoveControl.class.getDeclaredFields()) {
			Class<?> type = candidate.getType();
			if (type.isEnum() && type.getDeclaringClass() == MoveControl.class) {
				candidate.setAccessible(true);
				field = candidate;
				for (Object constant : type.getEnumConstants()) {
					if (((Enum<?>) constant).name().equals("WAIT")) {
						wait = constant;
					}
				}
				break;
			}
		}
		if (field == null || wait == null) {
			throw new IllegalStateException("MoveControl lost its wait operation");
		}
		OPERATION = field;
		WAIT = wait;
	}

	private McMoveControl() {}

	/** Stops the mob walking anywhere until something gives it a new target. */
	public static void hold(MoveControl control) {
		try {
			OPERATION.set(control, WAIT);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("could not park " + control, e);
		}
	}
}
