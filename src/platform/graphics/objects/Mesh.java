package platform.graphics.objects;

public class Mesh {
	/** x,y,z per point. */
	public final float[] positions;
	/** Three point indices per triangle, counter-clockwise when seen from the front. */
	public final int[] indices;
	/** Unit x,y,z per triangle. */
	public final float[] faceNormals;

	public Mesh(float[] positions, int[] indices) {
		this.positions = positions;
		this.indices = indices;
		this.faceNormals = computeFaceNormals(positions, indices);
	}

	private static float[] computeFaceNormals(float[] p, int[] indices) {
		float[] normals = new float[indices.length];
		for (int i = 0; i + 2 < indices.length; i += 3) {
			int a = indices[i] * 3, b = indices[i + 1] * 3, c = indices[i + 2] * 3;
			float e1x = p[b] - p[a], e1y = p[b + 1] - p[a + 1], e1z = p[b + 2] - p[a + 2];
			float e2x = p[c] - p[a], e2y = p[c + 1] - p[a + 1], e2z = p[c + 2] - p[a + 2];
			float nx = e1y * e2z - e1z * e2y;
			float ny = e1z * e2x - e1x * e2z;
			float nz = e1x * e2y - e1y * e2x;
			float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
			if (len > 1e-12f) {
				nx /= len; ny /= len; nz /= len;
			}
			normals[i] = nx; normals[i + 1] = ny; normals[i + 2] = nz;
		}
		return normals;
	}
}
