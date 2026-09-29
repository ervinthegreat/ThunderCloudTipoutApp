package math;

import platform.display.DisplayManager;

public class Maths {

	public static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
	private static final int LUT_SIZE = 4096;
	private static final float TWO_PI = (float) (Math.PI * 2.0);
	private static final float LUT_TO_RAD = TWO_PI / LUT_SIZE;
	private static final float DEG_TO_LUT = LUT_SIZE / 360.0f;
	private static final float[] SIN_LUT = new float[LUT_SIZE];
	private static final float[] COS_LUT = new float[LUT_SIZE];
	static {
		for (int i = 0; i < LUT_SIZE; i++) {
			float rad = i * LUT_TO_RAD;
			SIN_LUT[i] = (float) Math.sin(rad);
			COS_LUT[i] = (float) Math.cos(rad);
		}
	}
	// Reused scratch matrices to avoid per-frame allocations (single-threaded usage).
	private static final Mat4 VIEW_RX = new Mat4();
	private static final Mat4 VIEW_RY = new Mat4();
	private static final Mat4 VIEW_RZ = new Mat4();
	private static final Mat4 VIEW_T = new Mat4();
	private static final Mat4 VIEW_R = new Mat4();
	private static final Mat4 TRANS_S = new Mat4();
	private static final Mat4 TRANS_RX = new Mat4();
	private static final Mat4 TRANS_RY = new Mat4();
	private static final Mat4 TRANS_RZ = new Mat4();
	private static final Mat4 TRANS_T = new Mat4();
	private static final Mat4 TRANS_R = new Mat4();

	/** FOV in degrees (e.g. 70). Aspect from DisplayManager.getWidth()/getHeight(). */
	public static void perspective(Mat4 out, float fovYDegrees, float near, float far) {
		float aspect = (float) DisplayManager.getWidth() / DisplayManager.getHeight();
		float sy = 1f / (float) Math.tan((fovYDegrees * DEG_TO_RAD) * 0.5f);
		float sx = sy / aspect;
		float nf = 1f / (near - far);
		float[] m = out.m;
		out.identity();
		m[0] = sx;
		m[5] = sy;
		m[10] = (far + near) * nf;
		m[11] = -1f;
		m[14] = 2f * far * near * nf;
		m[15] = 0f;
	}

	public static void viewMatrix(Mat4 out, Vec3 position, float pitchDeg, float yawDeg, float rollDeg) {
		float pitchRad = pitchDeg * DEG_TO_RAD;
		float yawRad = yawDeg * DEG_TO_RAD;
		float rollRad = rollDeg * DEG_TO_RAD;
		VIEW_RX.setRotationX(pitchRad);
		VIEW_RY.setRotationY(yawRad);
		VIEW_RZ.setRotationZ(rollRad);
		VIEW_T.setTranslation(-position.x, -position.y, -position.z);
		// view = Rx * Ry * Rz * T(-pos) (matches OpenGL-style snippet)
		VIEW_R.set(VIEW_RX);
		out.multiply(VIEW_R, VIEW_RY);       // out = Rx * Ry
		VIEW_R.set(out);
		out.multiply(VIEW_R, VIEW_RZ);       // out = Rx * Ry * Rz
		VIEW_R.set(out);
		out.multiply(VIEW_R, VIEW_T);        // out = Rx * Ry * Rz * T
	}

	/** Mesh transform: translate * roll(Z) * yaw(Y) * pitch(X) * scale. Angles in degrees; scale is uniform. */
	public static void transform(Mat4 out, Vec3 position, float pitchDeg, float yawDeg, float rollDeg, float scale) {
		TRANS_S.setScale(scale, scale, scale);
		float pitchRad = pitchDeg * DEG_TO_RAD;
		float yawRad = yawDeg * DEG_TO_RAD;
		float rollRad = rollDeg * DEG_TO_RAD;
		TRANS_RX.setRotationX(pitchRad);
		TRANS_RY.setRotationY(yawRad);
		TRANS_RZ.setRotationZ(rollRad);
		TRANS_T.setTranslation(position.x, position.y, position.z);
		// out = T * Rz * Ry * Rx * S (same order as LWJGL: translate, roll, yaw, pitch, scale)
		TRANS_R.set(TRANS_S);
		out.multiply(TRANS_RX, TRANS_R);       // out = Rx * S
		TRANS_R.set(out);
		out.multiply(TRANS_RY, TRANS_R);       // out = Ry * Rx * S
		TRANS_R.set(out);
		out.multiply(TRANS_RZ, TRANS_R);       // out = Rz * Ry * Rx * S
		TRANS_R.set(out);
		out.multiply(TRANS_T, TRANS_R);        // out = T * Rz * Ry * Rx * S
	}

	public static float sinDeg(float deg) {
		int idx = (int) (deg * DEG_TO_LUT) & (LUT_SIZE - 1);
		return SIN_LUT[idx];
	}

	public static float cosDeg(float deg) {
		int idx = (int) (deg * DEG_TO_LUT) & (LUT_SIZE - 1);
		return COS_LUT[idx];
	}

	/** Mesh transform with no rotation (position + uniform scale only). */
	public static void transform(Mat4 out, Vec3 position, float scale) {
		transform(out, position, 0f, 0f, 0f, scale);
	}
}
