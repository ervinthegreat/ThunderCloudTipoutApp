package ui;

/** The interface drawn over the 3D scene, plus tap handling in the same UI units. */
public interface UiLayer {
	void paint(UiCanvas canvas, float width, float height);

	/** True when the next paint would look different from the last one; platforms skip paint otherwise. */
	boolean needsRepaint();

	/** Called synchronously from the platform's tap event, so it may open or close the keyboard. */
	void tap(float x, float y);
}
