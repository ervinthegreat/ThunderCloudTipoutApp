package platform.graphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import math.Mat4;
import platform.graphics.objects.FrameBuffer;
import platform.graphics.objects.Mesh;

public class Rasterizer {
	private FrameBuffer fb = null;
	private static final int FP_SHIFT = 16;
	private static final int FP_ONE = 1 << FP_SHIFT;
	// Skip extremely tiny triangles (area2 < 1 px) to reduce per-pixel overhead.
	private static final int MIN_TRI_AREA2 = 1;
	
	private int 			cullMode 	= 2;
	public static final int CULL_NONE  	= 0;
	public static final int CULL_FRONT 	= 1;
	public static final int CULL_BACK  	= 2;

	/** The camera sits at the origin looking down -Z, so model space -> view space is just the model matrix. */
	private Mat4 modelMatrix = new Mat4();
	private final float halfWidth, halfHeight, focal;
	private boolean lit = true;
	private static final float AMBIENT = 0.1f;
	/** Unit vector pointing toward the light (up, right, and toward the viewer). */
	private static final float LIGHT_X, LIGHT_Y, LIGHT_Z;
	static {
		float x = 10f, y = 10f, z = 14f;
		float inv = 1f / (float) Math.sqrt(x * x + y * y + z * z);
		LIGHT_X = x * inv; LIGHT_Y = y * inv; LIGHT_Z = z * inv;
	}
	private int colorR, colorG, colorB;

	/** Per-point results for the mesh being drawn, reused across draws and grown as needed. */
	private int[] screenX = new int[0];
	private int[] screenY = new int[0];
	private float[] depth = new float[0];
	private final float[] tmp = new float[4];

	private static List<Mesh> vaos = new ArrayList<Mesh>();
	
	public Rasterizer(FrameBuffer fb, float fovYDegrees) {
		this.fb = fb;
		halfWidth = fb.width * 0.5f;
		halfHeight = fb.height * 0.5f;
		focal = halfHeight / (float) Math.tan(Math.toRadians(fovYDegrees) * 0.5);
	}

	/** Fills a screen-space triangle with one color, one row at a time; culling is done by the caller. */
	private void fillTriangle(int x1, int y1, float z1,
			int x2, int y2, float z2,
			int x3, int y3, float z3, int pixel) {
	    // Sort vertices by Y (y1 <= y2 <= y3)
	    if (y1 > y2) { int tx = x1; x1 = x2; x2 = tx; int ty = y1; y1 = y2; y2 = ty; float tz = z1; z1 = z2; z2 = tz; }
	    if (y1 > y3) { int tx = x1; x1 = x3; x3 = tx; int ty = y1; y1 = y3; y3 = ty; float tz = z1; z1 = z3; z3 = tz; }
	    if (y2 > y3) { int tx = x2; x2 = x3; x3 = tx; int ty = y2; y2 = y3; y3 = ty; float tz = z2; z2 = z3; z3 = tz; }
	    if (y1 == y3) return; // Zero-height triangle
	    float invDy13 = 1.0f / (y3 - y1);
	    float invDy12 = y2 != y1 ? 1.0f / (y2 - y1) : 0f;
	    float invDy23 = y3 != y2 ? 1.0f / (y3 - y2) : 0f;
	    int x1f = x1 << FP_SHIFT;
	    int x2f = x2 << FP_SHIFT;
	    int dx13f = (int) (((x3 - x1) << FP_SHIFT) * invDy13);
	    int dx12f = (int) (((x2 - x1) << FP_SHIFT) * invDy12);
	    int dx23f = (int) (((x3 - x2) << FP_SHIFT) * invDy23);
	    float dz13 = (z3 - z1) * invDy13;
	    float dz12 = (z2 - z1) * invDy12;
	    float dz23 = (z3 - z2) * invDy23;
	    // Rows above y2 lie between edges 1-3 and 1-2; rows from y2 down lie between edges 1-3 and 2-3.
	    int endY = Math.min(y3, fb.height - 1);
	    for (int y = Math.max(y1, 0); y <= endY; y++) {
	    	int down1 = y - y1;
	    	int sx = (x1f + down1 * dx13f) >> FP_SHIFT;
	    	float sz = z1 + down1 * dz13;
	    	if (y < y2) {
	    		drawSpan(sx, sz, (x1f + down1 * dx12f) >> FP_SHIFT, z1 + down1 * dz12, y, pixel);
	    	} else {
	    		int down2 = y - y2;
	    		drawSpan(sx, sz, (x2f + down2 * dx23f) >> FP_SHIFT, z2 + down2 * dz23, y, pixel);
	    	}
	    }
	}

	/** Depth-tested horizontal run from xa to xb (inclusive) on row y, which must be on screen. */
	private void drawSpan(int xa, float za, int xb, float zb, int y, int pixel) {
		if (xa > xb) {
			int tx = xa; xa = xb; xb = tx;
			float tz = za; za = zb; zb = tz;
		}
		if (xb < 0 || xa >= fb.width) return;
		int zaf = (int) (za * FP_ONE);
		int zbf = (int) (zb * FP_ONE);
		int dzdx = xb > xa ? (zbf - zaf) / (xb - xa) : 0;
		int start = Math.max(0, xa);
		int end = Math.min(fb.width - 1, xb);
		int z = zaf + (start - xa) * dzdx;
		int[] depthBuffer = fb.depth;
		int[] pixels = fb.pixels;
		int row = y * fb.width;
		for (int idx = row + start, last = row + end; idx <= last; idx++, z += dzdx) {
			if (z < depthBuffer[idx]) {
				depthBuffer[idx] = z;
				pixels[idx] = pixel;
			}
		}
	}

	/** Transforms each point once, then culls, lights and fills each triangle using the stored results. */
	public void drawMesh(int vaoId) {
		Mesh mesh = vaos.get(vaoId);
		float[] positions = mesh.positions;
		int[] indices = mesh.indices;
		float[] faceNormals = mesh.faceNormals;
		int pointCount = positions.length / 3;
		ensureCapacity(pointCount);

		for (int p = 0; p < pointCount; p++) {
			project(positions[p * 3], positions[p * 3 + 1], positions[p * 3 + 2], p);
		}

		// The model matrix scales uniformly, so a rotated normal's length is that scale for every face.
		float[] m = modelMatrix.m;
		float invScale = 1f / (float) Math.sqrt(m[0] * m[0] + m[1] * m[1] + m[2] * m[2]);
		int unlitPixel = toPixel(colorR, colorG, colorB);
		for (int t = 0; t + 2 < indices.length; t += 3) {
			int a = indices[t], b = indices[t + 1], c = indices[t + 2];
			int area2 = (screenX[b] - screenX[a]) * (screenY[c] - screenY[a])
					- (screenY[b] - screenY[a]) * (screenX[c] - screenX[a]);
			if (area2 > -MIN_TRI_AREA2 && area2 < MIN_TRI_AREA2) continue;
			if (cullMode == CULL_BACK && area2 >= 0) continue;
			if (cullMode == CULL_FRONT && area2 <= 0) continue;

			int pixel = lit ? faceColor(faceNormals, t, invScale) : unlitPixel;
			fillTriangle(screenX[a], screenY[a], depth[a],
					screenX[b], screenY[b], depth[b],
					screenX[c], screenY[c], depth[c], pixel);
		}
	}

	private void ensureCapacity(int pointCount) {
		if (screenX.length >= pointCount) return;
		screenX = new int[pointCount];
		screenY = new int[pointCount];
		depth = new float[pointCount];
	}

	/**
	 * Model space -> screen pixel for point p. Every model stays well in front of the camera,
	 * so z is always negative and there is no near-plane clipping.
	 * Depth is 1/z: it interpolates linearly across the screen and is smaller for nearer points.
	 */
	private void project(float x, float y, float z, int p) {
		modelMatrix.transform(x, y, z, 1f, tmp);
		float wx = tmp[0], wy = tmp[1], wz = tmp[2];
		float invZ = 1f / wz;
		screenX[p] = (int) (halfWidth - focal * wx * invZ);
		screenY[p] = (int) (halfHeight + focal * wy * invZ);
		depth[p] = invZ;
	}

	/** Lights triangle t with the fixed directional light and returns the finished pixel. */
	private int faceColor(float[] faceNormals, int t, float invScale) {
		modelMatrix.transform(faceNormals[t], faceNormals[t + 1], faceNormals[t + 2], 0f, tmp);
		float ndotl = (tmp[0] * LIGHT_X + tmp[1] * LIGHT_Y + tmp[2] * LIGHT_Z) * invScale;
		float intensity = Math.min(1f, AMBIENT + Math.max(0f, ndotl));
		return toPixel((int) (colorR * intensity), (int) (colorG * intensity), (int) (colorB * intensity));
	}

	/** Framebuffer pixels are 0xAABBGGRR: canvas ImageData's R,G,B,A bytes read as a little-endian int. */
	private static int toPixel(int r, int g, int b) {
		return 0xFF000000 | (b << 16) | (g << 8) | r;
	}

	public void setModelMatrix(Mat4 model) { this.modelMatrix.set(model); }
	public void setLit(boolean lit) { this.lit = lit; }
	public void setColor(int argb) {
		colorR = (argb >> 16) & 0xFF;
		colorG = (argb >> 8) & 0xFF;
		colorB = argb & 0xFF;
	}
	
	/** color is 0xAARRGGBB. */
	public void clearColor(int color) {
        Arrays.fill(fb.pixels, toPixel((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF));
        Arrays.fill(fb.depth, Integer.MAX_VALUE);
	}
	
	public void setCullMode(int mode) {this.cullMode = mode;}

	public static int createVAO(Mesh mesh) {
		vaos.add(mesh);
		return vaos.size() - 1;
	}
}
