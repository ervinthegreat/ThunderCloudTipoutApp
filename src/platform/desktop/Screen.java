package platform.desktop;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import platform.display.Display;
import platform.graphics.objects.FrameBuffer;

public class Screen extends Canvas implements Display {
	private static final long serialVersionUID = 1L;
	private final BufferedImage image;

	public Screen(int width, int height, FrameBuffer fb) {
		this.image = wrap(fb);
		Dimension size = new Dimension(width, height);
		setPreferredSize(size);
		setIgnoreRepaint(true);
	}

	/** BufferedImage that shares the framebuffer's pixel array, so no per-frame copy is needed. */
	private static BufferedImage wrap(FrameBuffer fb) {
		DirectColorModel cm = new DirectColorModel(32, 0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000);
		DataBufferInt buffer = new DataBufferInt(fb.pixels, fb.pixels.length);
		WritableRaster raster = Raster.createPackedRaster(buffer, fb.width, fb.height, fb.width,
				cm.getMasks(), null);
		return new BufferedImage(cm, raster, false, null);
	}

	@Override
	public void present(FrameBuffer fb) {
		BufferStrategy bs = getBufferStrategy();
		if (bs == null) {
			createBufferStrategy(2);
			return;
		}
		Graphics g = bs.getDrawGraphics();
		try {
			g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
		} finally {
			g.dispose();
		}
		bs.show();
	}
}
