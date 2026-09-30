package platform.display;

import platform.graphics.objects.FrameBuffer;
import ui.UiLayer;

/** Platform-specific output surface that shows a finished frame with the UI drawn sharp on top. */
public interface Display {
	/** ui may be null. */
	void present(FrameBuffer fb, UiLayer ui);
}
