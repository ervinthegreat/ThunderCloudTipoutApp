package platform.graphics.objects;

/** RGBA pixel data for sampling. Pixels packed as 0xAARRGGBB; use lower 24 bits for RGB. */
public class Texture {
	public final int width;
	public final int height;
	public final int[] pixels;

	public Texture(int width, int height, int[] pixels) {
		this.width = width;
		this.height = height;
		this.pixels = pixels != null && pixels.length >= width * height ? pixels : null;
	}
}
