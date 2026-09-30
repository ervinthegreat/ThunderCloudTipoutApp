package ui;

import java.util.List;

/** Platform keyboard input for TextFields (the phone's own keyboard on the web). */
public interface TextInputHost {
	/** Called after each layout with the fields currently on screen; others are removed. */
	void sync(List<TextField> fields);

	void focus(TextField field);

	void blurAll();
}
