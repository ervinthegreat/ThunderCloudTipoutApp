package platform.graphics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import entities.Light;
import math.Mat4;
import platform.graphics.objects.FrameBuffer;
import platform.graphics.objects.Mesh;
import platform.graphics.objects.Texture;

public class Rasterizer {
	private FrameBuffer fb = null;
	private static final int FP_SHIFT = 16;
	private static final int FP_ONE = 1 << FP_SHIFT;
	private static final int TILE_SIZE = FrameBuffer.TILE_SIZE;
	// Skip extremely tiny triangles (area2 < 1 px) to reduce per-pixel overhead.
	private static final int MIN_TRI_AREA2 = 1;
	
	private int 			cullMode 	= 2;
	public static final int CULL_NONE  	= 0;
	public static final int CULL_FRONT 	= 1;
	public static final int CULL_BACK  	= 2;

	/** MVP = projection * view * model; set before drawArrays. */
	private Mat4 mvpMatrix = new Mat4();
	private Mat4 modelMatrix = new Mat4();
	private TextureManager textureManager;
	private Light light;
	private boolean lit = true;
	private static final float AMBIENT = 0.1f;

	/** Reused for transform and NDC (no per-triangle allocation). */
	private final float[] clip = new float[4];
	private final float[] ndcXYZ = new float[9];
	private final float[] tmpWorld = new float[4];
	private final float[] tmpNorm = new float[4];
	private final float[] lightRGB = new float[9];

	private static List<Mesh> vaos = new ArrayList<Mesh>();
	
	public Rasterizer(FrameBuffer fb) {
		this.fb = fb;
	}
	
	private void drawHorizontalSpanClipped(int x1, float z1, int u1f, int v1f, float r1, float g1, float b1,
			int x2, float z2, int u2f, int v2f, float r2, float g2, float b2, int y, Texture tex,
			int clipMinX, int clipMaxX) {
		if (y < 0 || y >= fb.height) return; // Vertical clipping
		int safeMinX = Math.max(0, clipMinX);
		int safeMaxX = Math.min(fb.width - 1, clipMaxX);
		if (safeMinX > safeMaxX) return;
		int z1f = (int) (z1 * FP_ONE);
		int z2f = (int) (z2 * FP_ONE);
		if (x1 == x2) {
			int x = x1;
			if (x < safeMinX || x > safeMaxX) return;
			if (!fb.hierarchicalZTest(x / TILE_SIZE, y / TILE_SIZE, z1f)) return;
			int idx = y * fb.width + x;
			int prevDepth = fb.depth[idx];
			if (z1f < prevDepth) {
				fb.depth[idx] = z1f;
				fb.updateHierarchicalZ(x, y, z1f, prevDepth);
				int base = textureManager.sampleFixed(tex, u1f, v1f);
				int tr = (base >> 16) & 0xFF;
				int tg = (base >> 8) & 0xFF;
				int tb = base & 0xFF;
				int rr = (int) (tr * r1);
				int gg = (int) (tg * g1);
				int bb = (int) (tb * b1);
				fb.pixels[idx] = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
			}
			return;
		}
		if (x1 > x2) {
			int tx = x1; x1 = x2; x2 = tx;
			int tz = z1f; z1f = z2f; z2f = tz;
			int tu = u1f; u1f = u2f; u2f = tu;
			int tv = v1f; v1f = v2f; v2f = tv;
			float tr = r1; r1 = r2; r2 = tr;
			float tg = g1; g1 = g2; g2 = tg;
			float tb = b1; b1 = b2; b2 = tb;
		}
		int start = Math.max(0, x1);
		int end = Math.min(fb.width - 1, x2);
		start = Math.max(start, safeMinX);
		end = Math.min(end, safeMaxX);
		if (start > end) return;
		int dx = x2 - x1;
		float invDx = 1.0f / dx;
		int dzdx = (int) ((z2f - z1f) * invDx);
		int dudx = (int) ((u2f - u1f) * invDx);
		int dvdx = (int) ((v2f - v1f) * invDx);
		float drdx = (r2 - r1) * invDx;
		float dgdx = (g2 - g1) * invDx;
		float dbdx = (b2 - b1) * invDx;
		int xOffset = start - x1;
		int zStart = z1f + xOffset * dzdx;
		int zEnd = z1f + (end - x1) * dzdx;
		int zMin = zStart < zEnd ? zStart : zEnd;
		if (!fb.hierarchicalZTest(start / TILE_SIZE, y / TILE_SIZE, zMin)) return;
		int uStart = u1f + xOffset * dudx;
		int vStart = v1f + xOffset * dvdx;
		float rStart = r1 + xOffset * drdx;
		float gStart = g1 + xOffset * dgdx;
		float bStart = b1 + xOffset * dbdx;
		int idx = y * fb.width + start;
		for (int x = start; x <= end; x++) {
			int prevDepth = fb.depth[idx];
			if (zStart < prevDepth) {
				fb.depth[idx] = zStart;
				fb.updateHierarchicalZ(x, y, zStart, prevDepth);
				int baseColor = textureManager.sampleFixed(tex, uStart, vStart);
				int tr = (baseColor >> 16) & 0xFF;
				int tg = (baseColor >> 8) & 0xFF;
				int tb = baseColor & 0xFF;
				int rr = (int) (tr * rStart);
				int gg = (int) (tg * gStart);
				int bb = (int) (tb * bStart);
				fb.pixels[idx] = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
			}
			zStart += dzdx;
			uStart += dudx;
			vStart += dvdx;
			rStart += drdx;
			gStart += dgdx;
			bStart += dbdx;
			idx++;
		}
	}
	
	public void fillTriangle(int x1, int y1, float z1, float u1, float v1,
			int x2, int y2, float z2, float u2, float v2,
			int x3, int y3, float z3, float u3, float v3, Texture tex,
			float r1, float g1, float b1,
			float r2, float g2, float b2,
			float r3, float g3, float b3) {
		  int area2 = (x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1);
		  if (area2 > -MIN_TRI_AREA2 && area2 < MIN_TRI_AREA2) return;
		    switch (cullMode) {
		        case CULL_BACK: if (area2 >= 0) return; break;
		        case CULL_FRONT:if (area2 <= 0) return; break;
		        case CULL_NONE:default: break;
		    }
	    // 1. Sort vertices by Y (y1 <= y2 <= y3)
	    if (y1 > y2) { int tx = x1; x1 = x2; x2 = tx; int ty = y1; y1 = y2; y2 = ty; float tz = z1; z1 = z2; z2 = tz; float tu = u1; u1 = u2; u2 = tu; float tv = v1; v1 = v2; v2 = tv; float tr = r1; r1 = r2; r2 = tr; float tg = g1; g1 = g2; g2 = tg; float tb = b1; b1 = b2; b2 = tb; }
	    if (y1 > y3) { int tx = x1; x1 = x3; x3 = tx; int ty = y1; y1 = y3; y3 = ty; float tz = z1; z1 = z3; z3 = tz; float tu = u1; u1 = u3; u3 = tu; float tv = v1; v1 = v3; v3 = tv; float tr = r1; r1 = r3; r3 = tr; float tg = g1; g1 = g3; g3 = tg; float tb = b1; b1 = b3; b3 = tb; }
	    if (y2 > y3) { int tx = x2; x2 = x3; x3 = tx; int ty = y2; y2 = y3; y3 = ty; float tz = z2; z2 = z3; z3 = tz; float tu = u2; u2 = u3; u3 = tu; float tv = v2; v2 = v3; v3 = tv; float tr = r2; r2 = r3; r3 = tr; float tg = g2; g2 = g3; g3 = tg; float tb = b2; b2 = b3; b3 = tb; }
	    if (y1 == y3) return; // Zero-height triangle
	    // 2. Calculate Slopes (fixed-point dx/dy, float dz/du/dv per scanline)
	    int x1f = x1 << FP_SHIFT;
	    int x2f = x2 << FP_SHIFT;
	    int x3f = x3 << FP_SHIFT;
	    int dy13 = y3 - y1;
	    int dy12 = y2 - y1;
	    int dy23 = y3 - y2;
	    float invDy13 = dy13 != 0 ? 1.0f / dy13 : 0f;
	    float invDy12 = dy12 != 0 ? 1.0f / dy12 : 0f;
	    float invDy23 = dy23 != 0 ? 1.0f / dy23 : 0f;
	    int dx13f = (int) ((x3f - x1f) * invDy13);
	    int dx12f = (int) ((x2f - x1f) * invDy12);
	    int dx23f = (int) ((x3f - x2f) * invDy23);
	    float dz13 = (z3 - z1) * invDy13;
	    float dz12 = (z2 - z1) * invDy12;
	    float dz23 = (z3 - z2) * invDy23;
	    float du13 = (u3 - u1) * invDy13;
	    float du12 = (u2 - u1) * invDy12;
	    float du23 = (u3 - u2) * invDy23;
	    float dv13 = (v3 - v1) * invDy13;
	    float dv12 = (v2 - v1) * invDy12;
	    float dv23 = (v3 - v2) * invDy23;
	    float dr13 = (r3 - r1) * invDy13;
	    float dr12 = (r2 - r1) * invDy12;
	    float dr23 = (r3 - r2) * invDy23;
	    float dg13 = (g3 - g1) * invDy13;
	    float dg12 = (g2 - g1) * invDy12;
	    float dg23 = (g3 - g2) * invDy23;
	    float db13 = (b3 - b1) * invDy13;
	    float db12 = (b2 - b1) * invDy12;
	    float db23 = (b3 - b2) * invDy23;
	    // 3. Draw the Top Half (Flat-Bottom) with incremental stepping
	    int sxF = x1f;
	    int exF = x1f;
	    float sz = z1, ez = z1;
	    float su = u1, eu = u1;
	    float sv = v1, ev = v1;
	    float sr = r1, er = r1;
	    float sg = g1, eg = g1;
	    float sb = b1, eb = b1;
	    int startY = Math.max(y1, 0);
	    int endY = Math.min(y2, fb.height - 1);
	    if (startY <= endY) {
		    for (int tileY = startY; tileY <= endY; tileY += TILE_SIZE) {
		    	int tileEndY = Math.min(tileY + TILE_SIZE - 1, endY);
		    	int yOffset = tileY - y1;
		    	sxF = x1f + yOffset * dx13f;
		    	exF = x1f + yOffset * dx12f;
		    	sz = z1 + yOffset * dz13;
		    	ez = z1 + yOffset * dz12;
		    	su = u1 + yOffset * du13;
		    	eu = u1 + yOffset * du12;
		    	sv = v1 + yOffset * dv13;
		    	ev = v1 + yOffset * dv12;
		    	sr = r1 + yOffset * dr13;
		    	er = r1 + yOffset * dr12;
		    	sg = g1 + yOffset * dg13;
		    	eg = g1 + yOffset * dg12;
		    	sb = b1 + yOffset * db13;
		    	eb = b1 + yOffset * db12;
		    	for (int y = tileY; y <= tileEndY; y++) {
			        int sx = sxF >> FP_SHIFT;
			        int ex = exF >> FP_SHIFT;
			        int suF = (int) (su * FP_ONE);
			        int euF = (int) (eu * FP_ONE);
			        int svF = (int) (sv * FP_ONE);
			        int evF = (int) (ev * FP_ONE);
			        int minX = Math.min(sx, ex);
			        int maxX = Math.max(sx, ex);
			        if (maxX >= 0 && minX < fb.width) {
			        	int clampedMinX = Math.max(0, minX);
			        	int clampedMaxX = Math.min(fb.width - 1, maxX);
			        	int tileStartX = (clampedMinX / TILE_SIZE) * TILE_SIZE;
			        	for (int tileX = tileStartX; tileX <= clampedMaxX; tileX += TILE_SIZE) {
			        		int tileEndX = Math.min(tileX + TILE_SIZE - 1, clampedMaxX);
					        drawHorizontalSpanClipped(sx, sz, suF, svF, sr, sg, sb,
					        		ex, ez, euF, evF, er, eg, eb, y, tex, tileX, tileEndX);
			        	}
			        }
			        sxF += dx13f;
			        exF += dx12f;
			        sz += dz13;
			        ez += dz12;
			        su += du13;
			        eu += du12;
			        sv += dv13;
			        ev += dv12;
			        sr += dr13;
			        er += dr12;
			        sg += dg13;
			        eg += dg12;
			        sb += db13;
			        eb += db12;
		    	}
		    }
	    }
	    // 4. Draw the Bottom Half (Flat-Top) with incremental stepping
	    if (y2 < y3) {//avoid division by zero.
	    	startY = Math.max(y2, 0);
	    	endY = Math.min(y3, fb.height - 1);
	    	if (startY <= endY) {
			    for (int tileY = startY; tileY <= endY; tileY += TILE_SIZE) {
			    	int tileEndY = Math.min(tileY + TILE_SIZE - 1, endY);
			    	int yOffset = tileY - y1;
			    	sxF = x1f + yOffset * dx13f;
			    	exF = x2f + (tileY - y2) * dx23f;
			    	sz = z1 + yOffset * dz13;
			    	ez = z2 + (tileY - y2) * dz23;
			    	su = u1 + yOffset * du13;
			    	eu = u2 + (tileY - y2) * du23;
			    	sv = v1 + yOffset * dv13;
			    	ev = v2 + (tileY - y2) * dv23;
			    	sr = r1 + yOffset * dr13;
			    	er = r2 + (tileY - y2) * dr23;
			    	sg = g1 + yOffset * dg13;
			    	eg = g2 + (tileY - y2) * dg23;
			    	sb = b1 + yOffset * db13;
			    	eb = b2 + (tileY - y2) * db23;
			    	for (int y = tileY; y <= tileEndY; y++) {
			            int sx = sxF >> FP_SHIFT;
			            int ex = exF >> FP_SHIFT;
			            int suF = (int) (su * FP_ONE);
			            int euF = (int) (eu * FP_ONE);
			            int svF = (int) (sv * FP_ONE);
			            int evF = (int) (ev * FP_ONE);
			            int minX = Math.min(sx, ex);
			            int maxX = Math.max(sx, ex);
			            if (maxX >= 0 && minX < fb.width) {
			            	int clampedMinX = Math.max(0, minX);
			            	int clampedMaxX = Math.min(fb.width - 1, maxX);
			            	int tileStartX = (clampedMinX / TILE_SIZE) * TILE_SIZE;
			            	for (int tileX = tileStartX; tileX <= clampedMaxX; tileX += TILE_SIZE) {
			            		int tileEndX = Math.min(tileX + TILE_SIZE - 1, clampedMaxX);
					            drawHorizontalSpanClipped(sx, sz, suF, svF, sr, sg, sb,
					            		ex, ez, euF, evF, er, eg, eb, y, tex, tileX, tileEndX);
			            	}
			            }
			            sxF += dx13f;
			            exF += dx23f;
			            sz += dz13;
			            ez += dz23;
			            su += du13;
			            eu += du23;
			            sv += dv13;
			            ev += dv23;
			            sr += dr13;
			            er += dr23;
			            sg += dg13;
			            eg += dg23;
			            sb += db13;
			            eb += db23;
			    	}
			    }
	    	}
	    }
	}

	// removed flat-color overloads
	
	/** Vertex assembly: read vertices by stride; transform by MVP to NDC; draw each triangle with fillTriangle. */
	public void drawArrays(int VAOID, int stride, int startIndex, int endIndex) {
		Mesh mesh = vaos.get(VAOID);
		float[] positions = mesh.positions;
		float[] texCoords = mesh.texCoords;
		float[] normals = mesh.normals;
		int[] indices = mesh.indices;
		Texture tex = textureManager.getTextureUnit0();
		int count = endIndex - startIndex;
		if (count < 3) return;
		for (int v = 0; v + 2 < count; v += 3) {
			int idx0 = indices[startIndex + v];
			int idx1 = indices[startIndex + v + 1];
			int idx2 = indices[startIndex + v + 2];
			int b0 = idx0 * stride, b1 = idx1 * stride, b2 = idx2 * stride;
			if (!vertexToNDC(positions[b0], positions[b0 + 1], positions[b0 + 2], ndcXYZ, 0)) continue;
			if (!vertexToNDC(positions[b1], positions[b1 + 1], positions[b1 + 2], ndcXYZ, 3)) continue;
			if (!vertexToNDC(positions[b2], positions[b2 + 1], positions[b2 + 2], ndcXYZ, 6)) continue;
			if (lit) {
				computeVertexLight(positions, normals, b0, lightRGB, 0);
				computeVertexLight(positions, normals, b1, lightRGB, 3);
				computeVertexLight(positions, normals, b2, lightRGB, 6);
			} else {
				lightRGB[0] = lightRGB[1] = lightRGB[2] = 1f;
				lightRGB[3] = lightRGB[4] = lightRGB[5] = 1f;
				lightRGB[6] = lightRGB[7] = lightRGB[8] = 1f;
			}
			int ax = ndcToScreenX(ndcXYZ[0]), ay = ndcToScreenY(ndcXYZ[1]);
			int bx = ndcToScreenX(ndcXYZ[3]), by = ndcToScreenY(ndcXYZ[4]);
			int cx = ndcToScreenX(ndcXYZ[6]), cy = ndcToScreenY(ndcXYZ[7]);
			int t0 = idx0 * 2;
			int t1 = idx1 * 2;
			int t2 = idx2 * 2;
			float u0 = texCoords[t0], v0 = texCoords[t0 + 1];
			float u1 = texCoords[t1], v1 = texCoords[t1 + 1];
			float u2 = texCoords[t2], v2 = texCoords[t2 + 1];
			fillTriangle(ax, ay, ndcXYZ[2], u0, v0,
					bx, by, ndcXYZ[5], u1, v1,
					cx, cy, ndcXYZ[8], u2, v2, tex,
					lightRGB[0], lightRGB[1], lightRGB[2],
					lightRGB[3], lightRGB[4], lightRGB[5],
					lightRGB[6], lightRGB[7], lightRGB[8]);
		}
	}

	/** Transform (x,y,z) by MVP, write NDC x,y,z to out[off..off+2]. Returns false if w <= 0. */
	private boolean vertexToNDC(float x, float y, float z, float[] out, int off) {
		mvpMatrix.transform(x, y, z, 1f, clip);
		float w = clip[3];
		if (w <= 0f) return false;
		out[off] = clip[0] / w;
		out[off + 1] = clip[1] / w;
		out[off + 2] = clip[2] / w;
		return true;
	}

	private void computeLightColor(float wx, float wy, float wz, float nx, float ny, float nz, float[] out, int off) {
		float lx = light.position.x - wx;
		float ly = light.position.y - wy;
		float lz = light.position.z - wz;
		float dist2 = lx * lx + ly * ly + lz * lz;
		if (dist2 < 1e-6f) dist2 = 1e-6f;
		float dist = (float) Math.sqrt(dist2);
		float invDist = 1f / dist;
		float ux = lx * invDist;
		float uy = ly * invDist;
		float uz = lz * invDist;
		float ndotl = nx * ux + ny * uy + nz * uz;
		if (ndotl < 0f) ndotl = 0f;
		float radius = Math.max(light.radius, 0.001f);
		float falloff = Math.max(light.falloff, 1f);
		float ratio = dist / radius;
		float attenuation = 1f / (1f + (float) Math.pow(ratio, falloff));
		float intensity = AMBIENT + ndotl * attenuation;
		out[off] = Math.min(1f, light.color.x * intensity);
		out[off + 1] = Math.min(1f, light.color.y * intensity);
		out[off + 2] = Math.min(1f, light.color.z * intensity);
	}

	private void computeVertexLight(float[] positions, float[] normals, int base, float[] out, int off) {
		float px = positions[base];
		float py = positions[base + 1];
		float pz = positions[base + 2];
		modelMatrix.transform(px, py, pz, 1f, tmpWorld);
		float wx = tmpWorld[0];
		float wy = tmpWorld[1];
		float wz = tmpWorld[2];

		float nx = normals[base];
		float ny = normals[base + 1];
		float nz = normals[base + 2];
		modelMatrix.transform(nx, ny, nz, 0f, tmpNorm);
		float nwx = tmpNorm[0];
		float nwy = tmpNorm[1];
		float nwz = tmpNorm[2];
		float nLen2 = nwx * nwx + nwy * nwy + nwz * nwz;
		if (nLen2 > 1e-12f) {
			float inv = 1f / (float) Math.sqrt(nLen2);
			nwx *= inv; nwy *= inv; nwz *= inv;
		}
		computeLightColor(wx, wy, wz, nwx, nwy, nwz, out, off);
	}

	public void setMVP(Mat4 mvp) { this.mvpMatrix.set(mvp); }
	public void setModelMatrix(Mat4 model) { this.modelMatrix.set(model); }
	public void setTextureManager(TextureManager textureManager) { this.textureManager = textureManager; }
	public void setLight(Light light) { this.light = light; }
	public void setLit(boolean lit) { this.lit = lit; }
	
	public void clearColor(int color) {
        Arrays.fill(fb.pixels, color);
        if (fb.depth != null) fb.clearDepth(Integer.MAX_VALUE);
	}
	
	// removed flat-color drawTriangle
	
	public void setCullMode(int mode) {this.cullMode = mode;}
	private int ndcToScreenX(float ndcX) {return (int) ((ndcX + 1f) * 0.5f * fb.width);}
	private int ndcToScreenY(float ndcY) {return (int) ((1f - ndcY) * 0.5f * fb.height);}


	public static int createVAO(Mesh mesh) {
		vaos.add(mesh);
		return vaos.size() - 1;
	}

	public int getVertexCount(int vaoId) {
		Mesh mesh = vaos.get(vaoId);
		if (mesh.indices != null) return mesh.indices.length;
		return mesh.positions == null ? 0 : mesh.positions.length;
	}
}
