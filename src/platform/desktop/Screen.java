package platform.desktop;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import platform.display.Display;
import platform.graphics.objects.FrameBuffer;
import ui.UiLayer;

/**
 * Shows the framebuffer scaled up with hard pixel edges, then draws the UI sharp on top.
 * zoom shrinks the whole window (for short monitors) without changing the UI layout size.
 */
public class Screen extends Canvas implements Display {
	private static final long serialVersionUID = 1L;
	private final BufferedImage image;
	private final int uiWidth, uiHeight;
	private final float zoom;
	private final Graphics2DCanvas uiCanvas = new Graphics2DCanvas();

	public Screen(int uiWidth, int uiHeight, float zoom, FrameBuffer fb) {
		this.image = wrap(fb);
		this.uiWidth = uiWidth;
		this.uiHeight = uiHeight;
		this.zoom = zoom;
		setPreferredSize(new Dimension(Math.round(uiWidth * zoom), Math.round(uiHeight * zoom)));
		setIgnoreRepaint(true);
	}

	public float getZoom() {return zoom;}

	/** BufferedImage that shares the framebuffer's pixel array, so no per-frame copy is needed. */
	private static BufferedImage wrap(FrameBuffer fb) {
		DirectColorModel cm = new DirectColorModel(32, 0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000);
		DataBufferInt buffer = new DataBufferInt(fb.pixels, fb.pixels.length);
		WritableRaster raster = Raster.createPackedRaster(buffer, fb.width, fb.height, fb.width,
				cm.getMasks(), null);
		return new BufferedImage(cm, raster, false, null);
	}

	@Override
	public void present(FrameBuffer fb, UiLayer ui) {
		BufferStrategy bs = getBufferStrategy();
		if (bs == null) {
			createBufferStrategy(2);
			return;
		}
		Graphics2D g = (Graphics2D) bs.getDrawGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
			if (ui != null) {
				g.scale(zoom, zoom);
				uiCanvas.begin(g);
				ui.paint(uiCanvas, uiWidth, uiHeight);
			}
		} finally {
			g.dispose();
		}
		bs.show();
	}
}
