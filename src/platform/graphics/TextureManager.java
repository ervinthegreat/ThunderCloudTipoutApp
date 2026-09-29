package platform.graphics;

import java.util.HashMap;
import java.util.Map;

import platform.graphics.objects.Texture;
import platform.io.Assets;
import platform.io.Bitmap;

/**
 * Owns texture creation, binding to units, and sampling.
 * Single concern: texture resource and binding state.
 */
public class TextureManager {
	public static final int MAX_TEXTURE_UNITS = 8;

	private final Map<Integer, Texture> textures = new HashMap<>();
	private final int[] textureUnits = new int[MAX_TEXTURE_UNITS]; // unit -> texture id, 0 = none
	private int activeTextureUnit = 0;
	private int nextTextureId = 1;

	public void activeTexture(int unit) {
		if (unit >= 0 && unit < MAX_TEXTURE_UNITS) {
			activeTextureUnit = unit;
		}
	}

	public void bindTexture(int textureId) {
		if (activeTextureUnit >= 0 && activeTextureUnit < MAX_TEXTURE_UNITS) {
			textureUnits[activeTextureUnit] = textureId;
		}
	}

	public int createTexture(int texWidth, int texHeight, int[] texPixels) {
		if (texPixels == null || texPixels.length < texWidth * texHeight) return 0;
		int id = nextTextureId++;
		textures.put(id, new Texture(texWidth, texHeight, texPixels));
		return id;
	}

	public int loadTexture(String path) {
		Bitmap img = Assets.loadImage(path);
		if (img == null) return 0;
		return createTexture(img.width, img.height, img.pixels);
	}

	/** Returns the texture bound to unit 0 (used by rasterizer). */
	public Texture getTextureUnit0() {
		int id = textureUnits[0];
		return id != 0 ? textures.get(id) : null;
	}

	/** Nearest-neighbor sample. u,v in [0,1]; v=0 is bottom (OpenGL-style). */
	public int sample(Texture t, float u, float v) {
		if (t == null || t.pixels == null) return 0;
		float uu = u - (float) Math.floor(u);
		float vv = v - (float) Math.floor(v);
		if (uu < 0f) uu += 1f;
		if (vv < 0f) vv += 1f;
		int tx = (int) (uu * (t.width - 1e-5f));
		int ty = (int) ((1f - vv) * (t.height - 1e-5f));
		tx = Math.max(0, Math.min(t.width - 1, tx));
		ty = Math.max(0, Math.min(t.height - 1, ty));
		int p = t.pixels[ty * t.width + tx];
		return p & 0x00FFFFFF;
	}

	/** Fixed-point (16.16) nearest-neighbor sample. v=0 is bottom (OpenGL-style). */
	public int sampleFixed(Texture t, int uFixed, int vFixed) {
		if (t == null || t.pixels == null) return 0;
		final int FP_SHIFT = 16;
		final int FP_ONE = 1 << FP_SHIFT;
		int uu = uFixed % FP_ONE;
		int vv = vFixed % FP_ONE;
		if (uu < 0) uu += FP_ONE;
		if (vv < 0) vv += FP_ONE;
		int tx = (uu * t.width) >> FP_SHIFT;
		int ty = ((FP_ONE - 1 - vv) * t.height) >> FP_SHIFT;
		tx = Math.max(0, Math.min(t.width - 1, tx));
		ty = Math.max(0, Math.min(t.height - 1, ty));
		int p = t.pixels[ty * t.width + tx];
		return p & 0x00FFFFFF;
	}
}
