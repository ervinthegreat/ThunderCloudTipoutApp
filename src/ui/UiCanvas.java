package ui;

/**
 * Sharp 2D drawing on top of the rasterized scene, in UI units (CSS pixels on the web).
 * Each platform draws with its native text renderer so text stays crisp at full screen resolution.
 */
public interface UiCanvas {
	int ALIGN_LEFT = 0;
	int ALIGN_CENTER = 1;
	int ALIGN_RIGHT = 2;

	void fillRect(float x, float y, float w, float h, int argb);
	void fillRoundRect(float x, float y, float w, float h, float radius, int argb);
	void strokeRoundRect(float x, float y, float w, float h, float radius, float lineWidth, int argb);

	/** y is the text baseline. */
	void drawText(String text, float x, float y, float size, boolean bold, int argb, int align);
	float measureText(String text, float size, boolean bold);
}
