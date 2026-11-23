package dev.wildercord.compat;

import java.lang.reflect.Field;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 1.21.1 has no {@code Input} record and no {@code getLastClientInput()}. The server keeps the
 * movement intent in the public {@code zza}/{@code xxa} fields on LivingEntity and the client's
 * jump key in the protected {@code jumping} field, read here once through reflection.
 */
public final class McPlayerInput {
	private static final Field JUMPING = findJumpingField();

	private final Player player;

	private McPlayerInput(Player player) {
		this.player = player;
	}

	public static McPlayerInput of(Player player) {
		return new McPlayerInput(player);
	}

	public boolean forward() {
		return player.zza > 0;
	}

	public boolean backward() {
		return player.zza < 0;
	}

	public boolean left() {
		return player.xxa < 0;
	}

	public boolean right() {
		return player.xxa > 0;
	}

	public boolean sprint() {
		return player.isSprinting();
	}

	public boolean jump() {
		if (JUMPING != null) {
			try {
				return JUMPING.getBoolean(player);
			} catch (ReflectiveOperationException ignored) {
				// fall through to the intent approximation below
			}
		}
		return player.zza > 0.8F;
	}

	private static Field findJumpingField() {
		try {
			Field field = LivingEntity.class.getDeclaredField("jumping");
			field.setAccessible(true);
			return field;
		} catch (ReflectiveOperationException ignored) {
			return null;
		}
	}
}
