package web;

/**
 * Canvas ImageData stores bytes as R,G,B,A, which read as a little-endian int is 0xAABBGGRR.
 * The engine uses 0xAARRGGBB, so converting either way is a red/blue swap.
 */
final class WebColor {
	private WebColor() {}

	static int argbToAbgr(int p) {
		return (p & 0xFF00FF00) | ((p >> 16) & 0xFF) | ((p & 0xFF) << 16);
	}
}
