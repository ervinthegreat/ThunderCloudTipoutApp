package platform.input;

/** Tracks grab state and accumulated pointer delta. Platforms feed it through addDelta. */
public class Mouse {

	/** Notified when grab state changes, so the platform can hide/show and recenter the cursor. */
	public interface GrabListener {
		void grabChanged(boolean grabbed);
	}

	private static boolean grabbed = false;
	private static float deltaX = 0f;
	private static float deltaY = 0f;
	private static GrabListener grabListener;

	public static boolean isGrabbed() {
		return grabbed;
	}

	public static void setGrabbed(boolean value) {
		if (grabbed == value) return;
		grabbed = value;
		if (grabListener != null) grabListener.grabChanged(value);
	}

	public static void setGrabListener(GrabListener listener) {grabListener = listener;}

	public static void addDelta(float dx, float dy) {
		deltaX += dx;
		deltaY += dy;
	}

	/** Delta since last getDX/getDY (call once per frame). */
	public static float getDX() {
		float d = deltaX;
		deltaX = 0f;
		return d;
	}

	public static float getDY() {
		float d = deltaY;
		deltaY = 0f;
		return d;
	}

	/** Call once per frame if you use Escape to release grab. */
	public static void update() {
		if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE))
			setGrabbed(false);
	}
}
