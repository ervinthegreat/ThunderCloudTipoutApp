package platform.display;

import platform.graphics.objects.FrameBuffer;

/** Platform-specific output surface that shows a finished frame. */
public interface Display {
	void present(FrameBuffer fb);
}
