package dev.wildercord.client.compat;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * The input events 1.21.9 hands a screen, on 1.21.8, which still passes the raw GLFW numbers. A screen
 * here overrides the plain-number methods and forwards one of these, so the code that reads an input
 * reads the same thing on every version.
 */
public final class McInput {
	private McInput() {}

	/** A key press or release. */
	public record KeyEvent(int key, int scancode, int modifiers) {
		public boolean hasControlDown() {
			return InputConstants.isModifierKeyDown(modifiers, InputConstants.MOD_CONTROL);
		}

		public boolean hasShiftDown() {
			return InputConstants.isModifierKeyDown(modifiers, InputConstants.MOD_SHIFT);
		}

		public boolean hasAltDown() {
			return InputConstants.isModifierKeyDown(modifiers, InputConstants.MOD_ALT);
		}

		public boolean isKeyDown(int keyCode) {
			return key == keyCode;
		}

		public boolean isEscape() {
			return key == GLFW.GLFW_KEY_ESCAPE;
		}

		public boolean isEnter() {
			return key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER;
		}
	}

	/** A mouse button press or release, at a point in the screen. */
	public record MouseButtonEvent(double x, double y, int button) {
	}

	/** A character typed into a focused edit box. */
	public record CharacterEvent(char codepoint, int modifiers) {
		public String codepointAsString() {
			return String.valueOf(codepoint);
		}

		public boolean hasControlDown() {
			return InputConstants.isModifierKeyDown(modifiers, InputConstants.MOD_CONTROL);
		}
	}
}
