package ui;

/**
 * An editable field drawn by the UI. The platform keeps text and focused in sync with its native
 * keyboard input; the UI sets the rectangle each frame while laying out.
 */
public class TextField {
	public enum Kind { NAME, MONEY }

	public final Kind kind;
	public String text = "";
	public boolean focused;
	public float x, y, w, h;
	/** Runs when the keyboard's Next/Return key is pressed in this field. */
	public Runnable onEnter;

	public TextField(Kind kind) {
		this.kind = kind;
	}

	public boolean contains(float px, float py) {
		return px >= x && px < x + w && py >= y && py < y + h;
	}
}
