package ui;

/** The interface drawn over the 3D scene each frame, plus tap handling in the same UI units. */
public interface UiLayer {
	void paint(UiCanvas canvas, float width, float height);

	/** Called synchronously from the platform's tap event, so it may open or close the keyboard. */
	void tap(float x, float y);
}
