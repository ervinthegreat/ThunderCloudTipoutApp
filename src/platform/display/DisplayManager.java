package platform.display;

import platform.graphics.objects.FrameBuffer;
import ui.UiLayer;

public class DisplayManager {
	/** Framebuffer downscale used by every platform, so desktop and web render at the same resolution. */
	public static final int DEFAULT_SCALE = 3;

	private static int width = 800;
	private static int height = 600;
	private static FrameBuffer fb;
	private static Display display;
	private static UiLayer uiLayer;

	/** width/height are the logical window size; the framebuffer is that size divided by scale. */
	public static void createDisplay(int windowWidth, int windowHeight, int scale) {
		width 	= windowWidth;
		height 	= windowHeight;
		fb 		= new FrameBuffer(width, height, scale);
	}

	public static void setDisplay(Display target) {display = target;}
	public static void setUiLayer(UiLayer layer) {uiLayer = layer;}

	public static void startFrame() {
	}

	public static void endFrame() {
		if (display != null) display.present(fb, uiLayer);
	}

	public static FrameBuffer getFramebuffer() {return fb;}
	public static int getWidth() {return width;}
	public static int getHeight() {return height;}
}
