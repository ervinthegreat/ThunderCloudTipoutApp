package platform.io;

/** Decoded image. Pixels packed as 0xAARRGGBB, row-major, top row first. */
public class Bitmap {
	public final int width;
	public final int height;
	public final int[] pixels;

	public Bitmap(int width, int height, int[] pixels) {
		this.width = width;
		this.height = height;
		this.pixels = pixels;
	}
}
