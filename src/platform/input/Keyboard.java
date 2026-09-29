package platform.input;

import java.util.HashSet;
import java.util.Set;

/** Tracks which keys are currently held. Platforms feed it through press/release. */
public class Keyboard {

	// Values match both java.awt.event.KeyEvent.VK_* and browser KeyboardEvent.keyCode.
	public static final int KEY_W = 87;
	public static final int KEY_S = 83;
	public static final int KEY_A = 65;
	public static final int KEY_D = 68;
	public static final int KEY_SPACE = 32;
	public static final int KEY_LSHIFT = 16;
	public static final int KEY_LCONTROL = 17;
	public static final int KEY_ESCAPE = 27;

	private static final Set<Integer> keysDown = new HashSet<>();

	public static boolean isKeyDown(int keyCode) {
		return keysDown.contains(keyCode);
	}

	public static void press(int keyCode) 	{keysDown.add(keyCode);}
	public static void release(int keyCode) {keysDown.remove(keyCode);}
	public static void releaseAll() 		{keysDown.clear();}
}
