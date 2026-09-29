package math;

public class Mat4 {
	public final float[] m = new float[16];

	public Mat4() {
		identity();
	}

	public void identity() {
		for (int i = 0; i < 16; i++) m[i] = 0;
		m[0] = m[5] = m[10] = m[15] = 1f;
	}

	public void set(Mat4 other) {
		System.arraycopy(other.m, 0, m, 0, 16);
	}

	public void multiply(Mat4 a, Mat4 b) {
		float[] A = a.m, B = b.m;
		for (int col = 0; col < 4; col++) {
			for (int row = 0; row < 4; row++) {
				m[row + col * 4] =
					A[row + 0] * B[0 + col * 4] +
					A[row + 4] * B[1 + col * 4] +
					A[row + 8] * B[2 + col * 4] +
					A[row + 12] * B[3 + col * 4];
			}
		}
	}

	public void transform(float x, float y, float z, float w, float[] out) {
		out[0] = m[0] * x + m[4] * y + m[8] * z + m[12] * w;
		out[1] = m[1] * x + m[5] * y + m[9] * z + m[13] * w;
		out[2] = m[2] * x + m[6] * y + m[10] * z + m[14] * w;
		out[3] = m[3] * x + m[7] * y + m[11] * z + m[15] * w;
	}

	public void setRotationX(float rad) {
		identity();
		float c = (float) Math.cos(rad), s = (float) Math.sin(rad);
		m[5] = c;  m[6] = s;
		m[9] = -s; m[10] = c;
	}

	public void setRotationY(float rad) {
		identity();
		float c = (float) Math.cos(rad), s = (float) Math.sin(rad);
		m[0] = c;  m[2] = -s;
		m[8] = s;  m[10] = c;
	}

	public void setRotationZ(float rad) {
		identity();
		float c = (float) Math.cos(rad), s = (float) Math.sin(rad);
		m[0] = c;  m[1] = s;
		m[4] = -s; m[5] = c;
	}

	public void setTranslation(float x, float y, float z) {
		identity();
		m[12] = x;
		m[13] = y;
		m[14] = z;
	}

	public void setScale(float sx, float sy, float sz) {
		identity();
		m[0] = sx;
		m[5] = sy;
		m[10] = sz;
	}
}
